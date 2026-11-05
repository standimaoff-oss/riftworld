#!/usr/bin/env bash
# Real in-game frames of the rebuilt creatures.
#
# Requires the disposable Forge server from server_check.sh (KEEP=1, RCON on 25575)
# and a working Xvfb + software GL stack, exactly like maja_client_probe.sh.
# Saves ci-out/frames/*.png — these are actual Minecraft client screenshots.
set -euo pipefail
OUT=${OUT:-ci-out}
RUN=${RUN:-ci/frames-run}
FRAMES="$OUT/frames"
mkdir -p "$OUT" "$FRAMES" "$RUN/client/config"
fail() { echo "::error title=In-game frames::$*"; exit 1; }
rq() { python3 tools/rcon.py --password "${RCON_PW:-riftci}" --log '' --timeout 90 "$@"; }

sudo apt-get update -qq
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq xvfb openbox x11-utils xdotool imagemagick \
  libgl1 libglx-mesa0 libxi6 libxxf86vm1 libxfixes3 libxrender1 libxkbcommon0 libsm6 libopenal1 >/dev/null

cat > "$RUN/client/options.txt" <<'OPTS'
tutorialStep:none
onboardAccessibility:false
skipMultiplayerWarning:true
joinedFirstServer:true
renderDistance:10
simulationDistance:8
maxFps:30
guiScale:2
fov:1.0
graphicsMode:1
renderClouds:false
pauseOnLostFocus:false
narrator:0
soundCategory_master:0.0
fullscreen:true
gamma:1.0
hideGui:true
bobView:false
overrideWidth:1600
overrideHeight:900
mipmapLevels:4
entityDistanceScaling:2.0
OPTS

export DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe MESA_GL_VERSION_OVERRIDE=3.3
Xvfb :99 -screen 0 1600x900x24 -nolisten tcp >"$OUT/frames-xvfb.log" 2>&1 &
openbox >"$OUT/frames-openbox.log" 2>&1 &
for _ in $(seq 1 30); do xdpyinfo -display :99 >/dev/null 2>&1 && break; sleep 1; done
xdpyinfo -display :99 >/dev/null 2>&1 || fail "No virtual display"

(
  cd mod
  chmod +x gradlew
  ./gradlew runClient --no-daemon --console=plain -PrunDir="$PWD/../$RUN" -PautoJoin=127.0.0.1:25565
) > "$OUT/frames-client.log" 2>&1 &
client=$!

for _ in $(seq 1 200); do
  players=$(rq list 2>&1 || true)
  [[ "$players" == *TestPlayer* ]] && break
  if ! kill -0 "$client" 2>/dev/null; then
    tail -40 "$OUT/frames-client.log" | while IFS= read -r line; do echo "::error title=client::${line:0:350}"; done
    fail "Client exited before joining"
  fi
  sleep 3
done
[[ "${players:-}" == *TestPlayer* ]] || fail "Client did not join the test server"

rq gamemode spectator TestPlayer >/dev/null
rq weather clear >/dev/null
rq time set noon >/dev/null
rq gamerule doDaylightCycle false >/dev/null || true
rq difficulty peaceful >/dev/null || true
rq effect give TestPlayer minecraft:night_vision 999 0 true >/dev/null || true

SHOT=0
# shot <name> <camX> <camY> <camZ> <lookX> <lookY> <lookZ> [wait]
shot() {
  local name=$1 cx=$2 cy=$3 cz=$4 lx=$5 ly=$6 lz=$7 wait=${8:-6}
  rq tp TestPlayer "$cx" "$cy" "$cz" facing "$lx" "$ly" "$lz" >/dev/null
  sleep "$wait"
  local wid
  wid=$(xdotool search --onlyvisible --name Minecraft | head -1 || true)
  [ -n "$wid" ] || fail "Minecraft window not found"
  xdotool windowraise "$wid" >/dev/null 2>&1 || true
  import -window root "$FRAMES/$name.png"
  SHOT=$((SHOT + 1))
  echo "captured $name"
}

# ---------------------------------------------------------------- pegasus
rq fill 190 99 -24 226 99 24 minecraft:grass_block >/dev/null
rq kill '@e[type=riftworld:pegasus]' >/dev/null 2>&1 || true
rq summon riftworld:pegasus 208 100 0 '{NoAI:1b,PersistenceRequired:1b}' >/dev/null
shot frame-pegasus-three-quarter 201 102 -13 208 101 0 10
shot frame-pegasus-side 208 102 -14 208 101 0 6
shot frame-pegasus-head 206 102 -6 208 103 -1 6

# ---------------------------------------------------------------- liopleurodon
LOC=$(rq execute in riftworld:elisar positioned 0 54 0 run locate biome riftworld:maja_ocean | tr -d '\n')
echo "ocean: $LOC"
read -r OX OZ < <(python3 - "$LOC" <<'PY'
import re,sys
m=re.search(r'\[\s*(-?\d+)\s*,\s*(?:~|-?\d+)\s*,\s*(-?\d+)\s*\]',sys.argv[1])
if not m: raise SystemExit('no ocean coordinates in '+sys.argv[1])
print(*m.groups())
PY
)
[[ "${OX:-}" =~ ^-?[0-9]+$ ]] || fail "No ocean coordinates: $LOC"
rq execute in riftworld:elisar run forceload add "$OX" "$OZ" >/dev/null 2>&1 || true
rq kill '@e[type=riftworld:liopleurodon]' >/dev/null 2>&1 || true
rq execute in riftworld:elisar run summon riftworld:liopleurodon "$OX" 40 "$OZ" \
  '{NoAI:1b,PersistenceRequired:1b}' >/dev/null
sleep 6
rq execute in riftworld:elisar run tp TestPlayer "$((OX + 8))" 40 "$((OZ - 10))" facing "$OX" 40 "$OZ" >/dev/null
sleep 10
capture_underwater() {
  local name=$1
  local wid
  wid=$(xdotool search --onlyvisible --name Minecraft | head -1 || true)
  [ -n "$wid" ] || fail "Minecraft window not found"
  xdotool windowraise "$wid" >/dev/null 2>&1 || true
  import -window root "$FRAMES/$name.png"
  SHOT=$((SHOT + 1))
  echo "captured $name"
}
capture_underwater frame-liopleurodon-swim
rq execute in riftworld:elisar run tp TestPlayer "$((OX + 13))" 41 "$OZ" facing "$OX" 40 "$OZ" >/dev/null
sleep 6
capture_underwater frame-liopleurodon-side
rq execute in riftworld:elisar run tp TestPlayer "$((OX + 5))" 42 "$((OZ + 1))" facing "$OX" 40 "$OZ" >/dev/null
sleep 6
capture_underwater frame-liopleurodon-head

# ---------------------------------------------------------------- El Gran Maja
rq kill '@e[type=riftworld:el_gran_maja]' >/dev/null 2>&1 || true
rq forceload add -80 -176 240 176 >/dev/null 2>&1 || true
rq summon riftworld:el_gran_maja "$((OX - 120))" 40 "$OZ" '{NoAI:1b,PersistenceRequired:1b}' >/dev/null
for _ in $(seq 1 60); do
  state=$(rq execute in riftworld:elisar run data get entity \
    '@e[type=riftworld:el_gran_maja,limit=1]' majaReady 2>&1 || true)
  [[ "$state" == *1b* ]] && break
  sleep 3
done
sleep 6
rq execute in riftworld:elisar run tp TestPlayer "$((OX - 120))" 60 "$((OZ - 150))" \
  facing "$((OX - 120))" 30 "$((OZ + 60))" >/dev/null
sleep 12
capture_underwater frame-maja-body
rq execute in riftworld:elisar run tp TestPlayer "$((OX - 120))" 46 "$((OZ + 40))" \
  facing "$((OX - 120))" 34 "$((OZ + 120))" >/dev/null
sleep 10
capture_underwater frame-maja-head

echo "captured $SHOT in-game frames"
[ "$SHOT" -ge 6 ] || fail "Only $SHOT frames captured"
ls -l "$FRAMES"
if grep -Ei 'Render thread/FATAL|Minecraft Crash Report|Error while rendering entity' "$OUT/frames-client.log"; then
  fail "Client reported a rendering failure while capturing frames"
fi
echo "OK: real client frames saved to $FRAMES"
