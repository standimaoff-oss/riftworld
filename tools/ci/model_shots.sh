#!/usr/bin/env bash
# Настоящие игровые кадры переработанных Пегаса и Лиоплевродона.
# Forge-клиент под Xvfb :99, программный OpenGL Mesa llvmpipe, 1920x1080,
# dedicаted-сервер уже поднят tools/ci/server_check.sh (KEEP=1).
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
showSubtitles:false
autoJump:false
darkMojangStudiosBackground:true
skipRealms32bitWarning:true
gamma:1.0
hideGui:false
bobView:false
overrideWidth:1920
overrideHeight:1080
mipmapLevels:0
entityDistanceScaling:1.5
biomeBlendRadius:3
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
rq weather clear >/dev/null 2>&1 || true
rq gamemode creative TestPlayer >/dev/null 2>&1 || true
rq effect give TestPlayer minecraft:night_vision 900 0 true >/dev/null 2>&1 || true
rq effect give TestPlayer minecraft:conduit_power 900 0 true >/dev/null 2>&1 || true

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

view() { # view <x> <y> <z> <yaw> <pitch> <pause> <name>
  EL tp TestPlayer "$1" "$2" "$3" "$4" "$5" >/dev/null 2>&1 || true
  sleep "$6"
  shot "$7"
}

lookat() { # lookat <cx> <cy> <cz> <tx> <ty> <tz> <pause> <name>
  local yp
  yp=$(python3 tools/ci/lookat_angles.py "$1" "$2" "$3" "$4" "$5" "$6" 2>/dev/null || echo "0 0")
  view "$1" "$2" "$3" "${yp% *}" "${yp#* }" "$7" "$8"
}

key F1                         # убрать HUD: чистая витрина модели
sleep 2
shot "00_boot"

# ------------------------------------------------------------------ сцены /riftstruct
EL tp TestPlayer 0 110 0 90 0 >/dev/null 2>&1 || true
EL time set noon >/dev/null 2>&1 || true
EL forceload add 670 670 730 730 >/dev/null 2>&1 || true
EL forceload add 1140 780 1180 820 >/dev/null 2>&1 || true
EL tp TestPlayer 1160 145 800 0 0 >/dev/null 2>&1 || true
sleep 12
EL tp TestPlayer 696 50 696 0 0 >/dev/null 2>&1 || true
sleep 12

PROBE_OUT=""; PROBE_AT=""; PROBE_CAM=""
probe_cam() {
  local kind=$1 out=""
  log "== /riftstruct $kind =="
  for _ in 1 2; do
    out=$(rq execute at TestPlayer in riftworld:elisar run riftstruct "$kind" 2>&1 | tr '\r\n' '  ')
    sleep 5
  done
  log "  ${out:0:500}"
  PROBE_OUT="$out"
  PROBE_AT=$(printf '%s' "$out" | sed -n 's/.* at=\([-0-9.]*,[-0-9.]*,[-0-9.]*\).*/\1/p' | tail -1)
  PROBE_CAM=$(printf '%s' "$out" | grep -o 'cam=[-0-9.]*,[-0-9.]*,[-0-9.]*:[-0-9.]*,[-0-9.]*,[-0-9.]*' | tail -1 | sed 's/^cam=//')
  [ -n "$PROBE_AT" ] || PROBE_AT="0,110,0"
}

use_cam() { # use_cam <name> <pause> <factor>
  local vals
  vals=$(python3 tools/ci/model_shot_camera.py "$PROBE_CAM" "$3" "$PROBE_AT" 2>/dev/null || true)
  [ -n "$vals" ] || vals="0 110 0 0 110 0"
  read -r CX CY CZ TX TY TZ <<< "$vals"
  log "  камера $CX,$CY,$CZ → $TX,$TY,$TZ (factor=$3)"
  lookat "$CX" "$CY" "$CZ" "$TX" "$TY" "$TZ" "$2" "$1"
}

# Пегас — ПЕРВЫМ, чтобы новые модели точно попали в кадры.
probe_cam "v34_pegasus"
use_cam "01_pegasus_hero" 12 1.0
use_cam "02_pegasus_wingspan" 10 1.34

# Лиоплевродон — общий план, широкий план и крупная пасть.
probe_cam "v32_liopleurodon"
use_cam "03_liopleurodon_full" 14 1.0
use_cam "04_liopleurodon_wide" 10 1.32
probe_cam "v38_liopleurodon_jaws"
use_cam "05_liopleurodon_jaws" 12 1.0

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
if compgen -G "$OUT/0[1-9]_*.jpg" >/dev/null; then
  montage -label '%f' "$OUT"/0[1-9]_*.jpg -tile 2x3 -geometry 800x450+10+10 \
    -background '#101820' -font DejaVu-Sans -pointsize 18 \
    "$OUT/00_in_game_contact_sheet.jpg" 2>/dev/null || true
fi
cp "$OUT"/*.jpg "$REPO_SHOTS"/ 2>/dev/null || true
cp "$OUT/llvmpipe.txt" "$REPO_SHOTS"/ 2>/dev/null || true
cp "$OUT/model_load.txt" "$REPO_SHOTS"/ 2>/dev/null || true
cp "$OUT/shots_report.txt" "$REPO_SHOTS"/ 2>/dev/null || true
log "готово: $OUT и $REPO_SHOTS"
echo "OK: игровые кадры сняты под Xvfb $DISP / Mesa llvmpipe"
