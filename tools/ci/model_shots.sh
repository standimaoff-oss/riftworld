#!/usr/bin/env bash
# Настоящие игровые кадры переработанных Пегаса и Лиоплевродона.
# Forge-клиент под Xvfb :99, программный OpenGL Mesa llvmpipe, 1920x1080.
# Dedicated-сервер уже поднят tools/ci/server_check.sh (KEEP=1).
#
# В отличие от камер /riftstruct здесь сцены ставятся в контролируемых координатах:
# чистая платформа для Пегаса и песчаный аквариум для Лиоплевродона.
#
# usage: OUT=ci-out/model-shots RUN=ci/model-run bash tools/ci/model_shots.sh
set -o pipefail

OUT=${OUT:-ci-out/model-shots}
RUN=${RUN:-ci/model-run}
DISP=${DISP:-:99}
RCON_PW=${RCON_PW:-riftci}
REPO_SHOTS=reports/hero_models/game
mkdir -p "$OUT" "$RUN/client/config" "$REPO_SHOTS"

: > "$OUT/shots_report.txt"
log() { echo "$*" | tee -a "$OUT/shots_report.txt"; }
fail() { echo "FATAL: $*" | tee -a "$OUT/shots_report.txt"; exit 1; }
rq() { python3 tools/rcon.py --password "$RCON_PW" --log "" --timeout 90 "$@"; }
EL() { rq execute in riftworld:elisar run "$@" 2>&1; }

# ------------------------------------------------------------------ настройки клиента
cat > "$RUN/client/options.txt" <<'OPTS'
tutorialStep:none
onboardAccessibility:false
skipMultiplayerWarning:true
joinedFirstServer:true
renderDistance:12
simulationDistance:8
maxFps:30
guiScale:2
fov:0.0
graphicsMode:1
renderClouds:false
pauseOnLostFocus:false
narrator:0
soundCategory_master:0.0
fullscreen:true
hideGui:true
showSubtitles:false
autoJump:false
darkMojangStudiosBackground:true
skipRealms32bitWarning:true
gamma:1.0
bobView:false
overrideWidth:1920
overrideHeight:1080
mipmapLevels:0
entityDistanceScaling:2.0
biomeBlendRadius:2
OPTS
cat > "$RUN/client/config/fml.toml" <<'FML'
maxThreads = -1
versionCheck = false
defaultConfigPath = "defaultconfigs"
disableOptimizedDFU = true
earlyWindowControl = true
earlyWindowWidth = 1920
earlyWindowHeight = 1080
earlyWindowFBScale = 1
earlyWindowMaximized = true
earlyWindowProvider = "fmlearlywindow"
earlyWindowSkipGLVersions = ["4.6", "4.5", "4.4", "4.3", "4.2", "4.1", "4.0"]
earlyWindowLogHelpMessage = false
earlyWindowSquir = false
earlyWindowShowCPU = false
FML

# ------------------------------------------------------------------ Xvfb + Mesa llvmpipe
if ! pgrep -f "Xvfb $DISP" >/dev/null 2>&1; then
  Xvfb "$DISP" -screen 0 1920x1080x24 -nolisten tcp >"$OUT/xvfb.log" 2>&1 &
  sleep 3
fi
export DISPLAY=$DISP
export LIBGL_ALWAYS_SOFTWARE=1
export GALLIUM_DRIVER=llvmpipe
export MESA_LOADER_DRIVER_OVERRIDE=llvmpipe
export MESA_GL_VERSION_OVERRIDE=3.3
if command -v openbox >/dev/null 2>&1 && ! pgrep -x openbox >/dev/null 2>&1; then
  openbox >"$OUT/openbox.log" 2>&1 &
  sleep 2
fi
for _ in $(seq 1 30); do
  xdpyinfo -display "$DISP" >/dev/null 2>&1 && break
  sleep 1
done
xdpyinfo -display "$DISP" >/dev/null 2>&1 || fail "Xvfb $DISP не поднялся"
if command -v glxinfo >/dev/null 2>&1; then
  glxinfo -B >"$OUT/llvmpipe.txt" 2>&1 || true
else
  echo "glxinfo отсутствует; LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe" >"$OUT/llvmpipe.txt"
fi
log "== Xvfb $DISP / Mesa llvmpipe =="
grep -Ei 'OpenGL renderer|OpenGL version|direct rendering' "$OUT/llvmpipe.txt" | head -5 | tee -a "$OUT/shots_report.txt" || true

# ------------------------------------------------------------------ клиент Forge
CPID=""
start_client() {
  (
    cd mod
    chmod +x gradlew
    ./gradlew runClient --no-daemon --console=plain --max-workers=2 \
      -PrunDir="$PWD/../$RUN" -PautoJoin=127.0.0.1:25565 2>&1
  ) >"$OUT/client.log" 2>&1 &
  CPID=$!
}

wait_connect() {
  for _ in $(seq 1 240); do
    grep -qi "Connecting to 127.0.0.1" "$OUT/client.log" && return 0
    grep -qE "We died but didn't somehow|A fatal error|Exception in thread \"main\"" "$OUT/client.log" && return 1
    if [ -n "$CPID" ] && ! kill -0 "$CPID" 2>/dev/null; then return 1; fi
    sleep 3
  done
  return 1
}

log "== старт Forge-клиента =="
start_client
if ! wait_connect; then
  log "первый запуск не удался — повтор (early display под Xvfb)"
  pkill -9 -f forgeclientuserdev 2>/dev/null || true
  pkill -9 -x xmessage zenity gxmessage kdialog 2>/dev/null || true
  sleep 8
  mv "$OUT/client.log" "$OUT/client_attempt1.log" 2>/dev/null || true
  start_client
  wait_connect || { tail -40 "$OUT/client.log"; fail "клиент не подключился со второй попытки"; }
fi

PLAYER=0
for _ in $(seq 1 90); do
  players=$(rq list 2>/dev/null || true)
  case "$players" in *TestPlayer*) PLAYER=1; break ;; esac
  sleep 4
done
[ "$PLAYER" = 1 ] || fail "TestPlayer не зашёл на сервер"
log "игрок в игре: $players"
sleep 20

RCON_gamerule() { rq gamerule "$@" >/dev/null 2>&1 || true; }
RCON_gamerule doDaylightCycle false
RCON_gamerule doWeatherCycle false
RCON_gamerule doMobSpawning false
rq weather clear >/dev/null 2>&1 || true
rq time set noon >/dev/null 2>&1 || true
rq difficulty peaceful >/dev/null 2>&1 || true
rq gamemode spectator TestPlayer >/dev/null 2>&1 || true
rq effect clear TestPlayer >/dev/null 2>&1 || true
rq effect give TestPlayer minecraft:night_vision 999 0 true >/dev/null 2>&1 || true

# ------------------------------------------------------------------ X11 съёмка
WID=""
geom() {
  WID=$(xdotool search --onlyvisible --name "Minecraft" 2>/dev/null | head -1)
  [ -z "$WID" ] && WID=$(xdotool search --onlyvisible --class "Minecraft" 2>/dev/null | head -1)
  [ -z "$WID" ] && WID=$(xdotool search --onlyvisible --classname ".*" 2>/dev/null | head -1)
  [ -n "$WID" ] || return 1
  eval "$(xdotool getwindowgeometry --shell "$WID")"
  return 0
}

key() {
  if geom; then
    xdotool windowactivate --sync "$WID" 2>/dev/null || true
    xdotool key --window "$WID" "$1" 2>/dev/null || xdotool key "$1" 2>/dev/null || true
  fi
}

shot() {
  pkill -9 -x xmessage zenity gxmessage kdialog 2>/dev/null || true
  WID=""; X=0; Y=0; WIDTH=1920; HEIGHT=1080
  if geom; then
    xdotool windowraise "$WID" 2>/dev/null || true
    if [ "${WIDTH:-0}" -lt 1800 ] || [ "${HEIGHT:-0}" -lt 1000 ]; then
      xdotool windowsize "$WID" 1920 1080 2>/dev/null || true
      xdotool windowmove "$WID" 0 0 2>/dev/null || true
      sleep 2
      geom || true
    fi
  fi
  import -window root /tmp/rift-model-full.png 2>/dev/null || return 1
  convert /tmp/rift-model-full.png -crop "${WIDTH}x${HEIGHT}+${X}+${Y}" +repage \
    -sampling-factor 4:4:4 -quality 93 "$OUT/$1.jpg" 2>/dev/null \
    || cp /tmp/rift-model-full.png "$OUT/$1.png"
  log "  снято: $OUT/$1.jpg (${WIDTH}x${HEIGHT})"
}

face() {
  EL tp TestPlayer "$1" "$2" "$3" facing "$4" "$5" "$6" >/dev/null 2>&1 || true
}

view_face() { # name cx cy cz tx ty tz pause
  local name=$1 cx=$2 cy=$3 cz=$4 tx=$5 ty=$6 tz=$7 pause=${8:-8}
  log "  камера $cx,$cy,$cz → $tx,$ty,$tz"
  face "$cx" "$cy" "$cz" "$tx" "$ty" "$tz"
  sleep "$pause"
  shot "$name"
}

key F1
sleep 2
shot "00_boot"

# ------------------------------------------------------------------ пробы: только статистика моделей
probe_stats() {
  local kind=$1 out
  log "== /riftstruct $kind (статистика) =="
  out=$(rq execute at TestPlayer in riftworld:elisar run riftstruct "$kind" 2>&1 | tr '\r\n' '  ')
  sleep 5
  log "  ${out:0:500}"
}

EL tp TestPlayer 0 110 0 90 0 >/dev/null 2>&1 || true
EL forceload add -20 -20 20 20 >/dev/null 2>&1 || true
EL forceload add 180 180 220 220 >/dev/null 2>&1 || true
probe_stats "v34_pegasus"
probe_stats "v32_liopleurodon"
probe_stats "v38_liopleurodon_jaws"

# Чистим всё, что наспавнили пробы, чтобы кадры были одиночными и чистыми.
EL kill '@e[type=riftworld:pegasus]' >/dev/null 2>&1 || true
EL kill '@e[type=riftworld:liopleurodon]' >/dev/null 2>&1 || true

# ------------------------------------------------------------------ сцена 1: Пегас
log "== сцена: Пегас на чистой платформе =="
EL tp TestPlayer 0 105 0 0 0 >/dev/null 2>&1 || true
sleep 8
EL fill -14 99 -14 14 99 14 minecraft:grass_block >/dev/null 2>&1 || true
EL fill -14 100 -14 14 109 14 minecraft:air >/dev/null 2>&1 || true
EL fill -14 98 -14 14 98 14 minecraft:dirt >/dev/null 2>&1 || true
EL summon riftworld:pegasus 0.5 100 0.5 \
  '{NoAI:1b,PersistenceRequired:1b,Invulnerable:1b,Silent:1b,NoGravity:1b,Rotation:[0f,0f]}' >/dev/null 2>&1 || true
sleep 8

view_face "01_pegasus_front" 0 103 12 0 101.5 0 10
view_face "02_pegasus_side" 12 103 0 0 101.5 0 10
view_face "03_pegasus_three_quarter" 9 104 9 0 101.5 0 10
view_face "04_pegasus_wingspan_top" 0 112 0.5 0 101.5 0 10

# ------------------------------------------------------------------ сцена 2: Лиоплевродон
log "== сцена: Лиоплевродон в чистом аквариуме =="
EL kill '@e[type=riftworld:pegasus]' >/dev/null 2>&1 || true
EL tp TestPlayer 200 45 200 0 0 >/dev/null 2>&1 || true
sleep 8
EL fill 188 20 188 212 20 212 minecraft:sand >/dev/null 2>&1 || true
EL fill 188 21 188 212 50 212 minecraft:water >/dev/null 2>&1 || true
EL fill 188 51 188 212 70 212 minecraft:air >/dev/null 2>&1 || true
EL summon riftworld:liopleurodon 200.5 35 200.5 \
  '{NoAI:1b,PersistenceRequired:1b,Invulnerable:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f]}' >/dev/null 2>&1 || true
sleep 10

view_face "05_liopleurodon_side" 200 35.5 215 200.5 35.5 200.5 12
view_face "06_liopleurodon_three_quarter" 212 37.5 212 200.5 35.5 200.5 12
view_face "07_liopleurodon_head" 190 36 203 196 35.5 200.5 12
view_face "08_liopleurodon_top" 200 48 200 200 35 200 12

# ------------------------------------------------------------------ проверка загрузки моделей
{
  echo "# Загрузка geo-моделей в реальном клиенте"
  grep -F '[riftworld] geo ' "$OUT/client.log" | grep -E 'pegasus|liopleurodon' || true
  echo
  echo "# Возможные ошибки рендера"
  grep -Ei 'Unable to load.*(pegasus|liopleurodon)|Error while rendering|Exception.*(pegasus|liopleurodon)' "$OUT/client.log" | tail -20 || true
} >"$OUT/model_load.txt"
cat "$OUT/model_load.txt" | tee -a "$OUT/shots_report.txt"
if ! grep -q 'geo pegasus:' "$OUT/client.log" || ! grep -q 'geo liopleurodon:' "$OUT/client.log"; then
  log "WARN: в логе клиента не видно обе загрузки geo; кадры требуют визуальной проверки"
fi

# ------------------------------------------------------------------ контактный лист и публикация
rm -f "$REPO_SHOTS"/0[1-9]_*.jpg "$REPO_SHOTS"/00_in_game_contact_sheet.jpg
if compgen -G "$OUT/0[1-9]_*.jpg" >/dev/null; then
  montage -label '%f' "$OUT"/0[1-9]_*.jpg -tile 2x4 -geometry 800x450+10+10 \
    -background '#101820' -font DejaVu-Sans -pointsize 18 \
    "$OUT/00_in_game_contact_sheet.jpg" 2>/dev/null || true
fi
cp "$OUT"/0[1-9]_*.jpg "$REPO_SHOTS"/ 2>/dev/null || true
cp "$OUT/00_boot.jpg" "$REPO_SHOTS"/ 2>/dev/null || true
cp "$OUT/00_in_game_contact_sheet.jpg" "$REPO_SHOTS"/ 2>/dev/null || true
cp "$OUT/llvmpipe.txt" "$REPO_SHOTS"/ 2>/dev/null || true
cp "$OUT/model_load.txt" "$REPO_SHOTS"/ 2>/dev/null || true
cp "$OUT/shots_report.txt" "$REPO_SHOTS"/ 2>/dev/null || true
log "готово: $OUT и $REPO_SHOTS"
echo "OK: игровые кадры сняты под Xvfb $DISP / Mesa llvmpipe"
