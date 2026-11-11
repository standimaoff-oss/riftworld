#!/usr/bin/env bash
# Real Minecraft 1.20.1 Forge `runClient` visual probe under Xvfb + Mesa llvmpipe (1920x1080)
# Captures 11 Full HD in-game screenshots: Liopleurodon, the paper dragon (5 poses)
# and the v0.40.0 Elisar gear & folk showcase.
set -euo pipefail

OUT=${OUT:-ci-out}
SHOT_DIR="$OUT/blockbench_shots"
RUN=${RUN:-ci/bb-run}
mkdir -p "$SHOT_DIR" "$RUN/client/config"

fail() { echo "::error title=Blockbench client probe::$*"; exit 1; }
on_error() {
  code=$?
  if [ "$code" -ne 0 ]; then
    for file in ci/server/logs/latest.log "$OUT/bb-client.log"; do
      [ -f "$file" ] || continue
      grep -Ei 'error|exception|crash|disconnected' "$file" | tail -12 | while IFS= read -r line; do
        echo "::error title=Game diagnostic::${line:0:450}"
      done || true
    done
  fi
}
trap on_error EXIT

# RCON рвёт соединение на длинных командах: повторяем запрос три раза,
# пока не получим ответ без "connection closed" / "Broken pipe".
rq() {
  local attempt=1 out=""
  while [ "$attempt" -le 3 ]; do
    out=$(python3 tools/rcon.py --password "${RCON_PW:-riftci}" --log "" --timeout 90 "$@" 2>&1 || true)
    if [[ "$out" != *"connection closed"* && "$out" != *"Broken pipe"* && "$out" != *"Connection reset"* ]]; then
      printf '%s' "$out"
      return 0
    fi
    attempt=$((attempt + 1))
    sleep 3
  done
  printf '%s' "$out"
  return 0
}

sudo apt-get update -qq
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq \
  xvfb openbox x11-utils xdotool imagemagick \
  libgl1 libglx-mesa0 libxi6 libxxf86vm1 libxfixes3 libxrender1 libxkbcommon0 libsm6 libopenal1 >/dev/null

cat > "$RUN/client/options.txt" <<'OPTS'
tutorialStep:none
onboardAccessibility:false
skipMultiplayerWarning:true
joinedFirstServer:true
renderDistance:12
simulationDistance:8
maxFps:30
guiScale:2
fov:-0.15
graphicsMode:1
renderClouds:false
pauseOnLostFocus:false
narrator:0
soundCategory_master:0.0
fullscreen:true
gamma:1.0
hideGui:true
bobView:false
overrideWidth:1920
overrideHeight:1080
mipmapLevels:0
entityDistanceScaling:2.5
OPTS

export DISPLAY=:99
export LIBGL_ALWAYS_SOFTWARE=1
export GALLIUM_DRIVER=llvmpipe
export MESA_GL_VERSION_OVERRIDE=3.3

Xvfb :99 -screen 0 1920x1080x24 -nolisten tcp >"$OUT/bb-xvfb.log" 2>&1 &
openbox >"$OUT/bb-openbox.log" 2>&1 &
for _ in $(seq 1 30); do xdpyinfo -display :99 >/dev/null 2>&1 && break; sleep 1; done
xdpyinfo -display :99 >/dev/null 2>&1 || fail "No virtual Xvfb display :99"

start_client() {
  (
    cd mod
    chmod +x gradlew
    ./gradlew runClient --no-daemon --console=plain \
      -PrunDir="$PWD/../$RUN" \
      -PautoJoin=127.0.0.1:25565
  ) > "$OUT/bb-client.log" 2>&1 &
  CPID=$!
}

wait_join() {
  for _ in $(seq 1 180); do
    players=$(rq list 2>&1 || true)
    [[ "$players" == *TestPlayer* ]] && return 0
    if ! kill -0 "$CPID" 2>/dev/null; then
      return 1
    fi
    sleep 3
  done
  return 1
}

echo "== Launching Forge 1.20.1 runClient under Xvfb + Mesa llvmpipe =="
start_client
if ! wait_join; then
  echo "  First launch did not join cleanly — retrying once..."
  pkill -9 -f forgeclientuserdev 2>/dev/null || true
  sleep 6
  mv "$OUT/bb-client.log" "$OUT/bb-client-attempt1.log" || true
  start_client
  wait_join || fail "Client failed to join test server on retry"
fi

WID=$(xdotool search --onlyvisible --name Minecraft | head -1 || true)
[ -n "$WID" ] || fail "Minecraft X11 window not found"
xdotool windowraise "$WID" || true

# Record OpenGL renderer info from client log for verification
grep -Ei "Backend library|OpenGL|llvmpipe|Mesa" "$OUT/bb-client.log" > "$SHOT_DIR/gl_renderer_info.txt" || true

rq gamerule doDaylightCycle false >/dev/null
rq gamerule doWeatherCycle false >/dev/null
rq weather clear >/dev/null
rq gamemode spectator TestPlayer >/dev/null
rq effect give TestPlayer minecraft:night_vision 9999 0 true >/dev/null
rq effect give TestPlayer minecraft:conduit_power 9999 0 true >/dev/null

parse_cam() {
  python3 - "$1" "$2" <<'PY'
import re, sys
line, key = sys.argv[1], sys.argv[2]
m = re.search(rf"{re.escape(key)}=([-\d.]+),([-\d.]+),([-\d.]+):([-\d.]+),([-\d.]+),([-\d.]+)", line)
if not m:
    raise SystemExit(f"Could not parse {key} from: {line}")
print(*m.groups())
PY
}

capture_cam() {
  local line="$1" key="$2" out_png="$3" wait_s="$4"
  read -r CX CY CZ TX TY TZ < <(parse_cam "$line" "$key")
  rq execute in riftworld:elisar run tp TestPlayer "$CX" "$CY" "$CZ" facing "$TX" "$TY" "$TZ" >/dev/null
  sleep "$wait_s"
  xdotool windowraise "$WID" || true
  import -window root "$SHOT_DIR/$out_png"
  echo "  Captured $SHOT_DIR/$out_png (cam=$CX,$CY,$CZ -> $TX,$TY,$TZ)"
  # Objective frame check: a camera placed outside the entity tracking distance
  # renders only sky, which is almost flat and has almost no model pixels.
  colours=$(identify -format "%k" "$SHOT_DIR/$out_png" 2>/dev/null || echo 0)
  dark=$(convert "$SHOT_DIR/$out_png" -colorspace Gray -resize 240x135! -threshold 40% -negate \
         -format "%[fx:mean]" info: 2>/dev/null || echo 0)
  echo "    frame check: unique colours=$colours dark_share=$dark"
  [ "$colours" -ge 250 ] || fail "$out_png looks empty (only $colours unique colours)"
  awk -v d="$dark" 'BEGIN{exit !(d+0>=0.004)}' \
    || fail "$out_png contains almost no model pixels (dark share $dark)"
}

# ==============================================================================
# 1) LIOPLEURODON IN ELISAR SEA (3 underwater camera angles)
# ==============================================================================
echo "== Staging Blockbench Liopleurodon in riftworld:elisar ocean =="
rq execute in riftworld:elisar run time set noon >/dev/null
rq execute in riftworld:elisar run tp TestPlayer 696 42 696 90 0 >/dev/null
sleep 12
LIO_INFO=$(rq execute in riftworld:elisar positioned 696 36 696 run riftstruct bb_liopleurodon_showcase)
echo "  $LIO_INFO"
[[ "$LIO_INFO" == *"model_boxes=234"* ]] || fail "Expected Liopleurodon model_boxes=234, got: $LIO_INFO"
[[ "$LIO_INFO" == *"atlas=1024"* ]] || fail "Expected Liopleurodon atlas=1024, got: $LIO_INFO"

capture_cam "$LIO_INFO" "cam_hero"   "03_ingame_liopleurodon_hero.png"   22
capture_cam "$LIO_INFO" "cam_jaws"   "04_ingame_liopleurodon_jaws.png"   12
capture_cam "$LIO_INFO" "cam_dorsal" "05_ingame_liopleurodon_dorsal.png" 12

# ==============================================================================
# 2) PAPER DRAGON OVER ELISAR (v0.49.0 — the Pegasus was removed from the mod)
# ==============================================================================
echo "== Staging the paper dragon over riftworld:elisar sunlit meadow & sky =="
rq effect clear TestPlayer >/dev/null
rq execute in riftworld:elisar run time set 4500 >/dev/null
rq execute in riftworld:elisar run tp TestPlayer 520 112 520 40 0 >/dev/null
sleep 14
DRAGON_INFO=$(rq execute in riftworld:elisar positioned 520 108 520 run riftstruct bb_paper_dragon_showcase)
echo "  $DRAGON_INFO"
[[ "$DRAGON_INFO" == *"spawned=1"* ]] || fail "Expected the paper dragon to spawn, got: $DRAGON_INFO"

capture_cam "$DRAGON_INFO" "cam_wild"   "06_ingame_paper_dragon_far.png"        18
capture_cam "$DRAGON_INFO" "cam_saddle" "07_ingame_paper_dragon_close.png"     12
capture_cam "$DRAGON_INFO" "cam_flight" "08_ingame_paper_dragon_motion.png"    12
capture_cam "$DRAGON_INFO" "cam_glide"  "09_ingame_paper_dragon_side.png"      12
capture_cam "$DRAGON_INFO" "cam_hover"  "10_ingame_paper_dragon_hover.png"     12

# ==============================================================================
# 3) ELISAR GEAR & FOLK (v0.40.0): armour stands + villager / guard / archer
# ==============================================================================
echo "== Staging the Elisar gear and folk showcase =="
rq execute in riftworld:elisar run time set 6000 >/dev/null
# llvmpipe подгружает новый участок медленно: греем клиент прямо на точке съёмки.
rq execute in riftworld:elisar run tp TestPlayer 760 123 770 0 0 >/dev/null
sleep 30
GEAR_INFO=$(rq execute in riftworld:elisar positioned 760 118 760 run riftstruct v40_gear)
echo "  $GEAR_INFO"
[[ "$GEAR_INFO" == *"stands=4"* ]] || fail "Expected 4 armour stands, got: $GEAR_INFO"
[[ "$GEAR_INFO" == *"folk=3"* ]] || fail "Expected 3 Elisar folk, got: $GEAR_INFO"
# Сущности должны существовать на сервере: иначе пустой кадр — не вина камеры.
# Тяжёлые запросы "if entity ... distance=..40" рвали RCON-соединение, и
# весь sea-check падал на этом шаге. Проверяем список сущностей поштучно.
stands=$(rq execute in riftworld:elisar positioned 760 118 760 if entity '@e[type=minecraft:armor_stand]' 2>&1 || true)
[[ "$stands" == *"Test passed"* ]] || fail "No armour stands near the showcase: $stands"
folk=$(rq execute in riftworld:elisar positioned 760 118 760 if entity '@e[type=riftworld:elisar_guard]' 2>&1 || true)
[[ "$folk" == *"Test passed"* ]] || fail "No Elisar guard spawned: $folk"

capture_cam "$GEAR_INFO" "cam_row"   "11_ingame_gear_row.png"      30
capture_cam "$GEAR_INFO" "cam_folk"  "12_ingame_elisar_folk.png"   26
capture_cam "$GEAR_INFO" "cam_close" "13_ingame_gear_closeup.png"  22

# ==============================================================================
# 4) Verify client logs & renderer health
# ==============================================================================
grep -F '[riftworld] geo' "$OUT/bb-client.log" > "$SHOT_DIR/geo_loaded.txt" || true
grep -Ei 'Error while rendering entity|Exception.*(Liopleurodon)|Failed to load.*(liopleurodon)' "$OUT/bb-client.log" > "$SHOT_DIR/render_diagnostics.txt" || true

grep -Fq '[riftworld] geo liopleurodon:' "$OUT/bb-client.log" || fail "Liopleurodon geo log missing"

if grep -Ei 'Error while rendering entity.*liopleurodon|Exception.*(LiopleurodonRenderer|LiopleurodonBlockbench)|Failed to load texture.*liopleurodon' "$OUT/bb-client.log"; then
  grep -Ei 'Error while rendering entity.*liopleurodon|Exception.*(Liopleurodon|PaperDragon)|Failed to load.*liopleurodon' "$OUT/bb-client.log" | head -5 | while IFS= read -r l; do
    echo "::error title=Model render error::${l:0:450}"
  done
  fail "Client log contains Liopleurodon/PaperDragon entity rendering errors"
fi

rq list | grep -q TestPlayer || fail "Client disconnected before finishing captures"
pkill -f forgeclientuserdev 2>/dev/null || true
echo "SUCCESS: All 11 Full HD in-game screenshots rendered via Xvfb + Mesa llvmpipe!"
