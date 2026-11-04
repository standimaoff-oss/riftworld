#!/usr/bin/env bash
# A focused visual smoke check; server + NoAI test titan come from maja_server_probe.sh.
set -euo pipefail
OUT=${OUT:-ci-out}
RUN=${RUN:-ci/maja-run}
mkdir -p "$OUT" "$RUN/client/config"
fail() { echo "::error title=Maja client probe::$*"; exit 1; }
on_error() {
  code=$?
  if [ "$code" -ne 0 ]; then
    for file in ci/server/logs/latest.log "$OUT/maja-client.log"; do
      [ -f "$file" ] || continue
      grep -Ei 'error|exception|crash|disconnected' "$file" | tail -8 | while IFS= read -r line; do
        echo "::error title=Game diagnostic::${line:0:450}"
      done || true
    done
  fi
}
trap on_error EXIT
rq() { python3 tools/rcon.py --password "${RCON_PW:-riftci}" --log "" --timeout 90 "$@"; }

sudo apt-get update -qq
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq xvfb openbox x11-utils xdotool imagemagick libgl1 libglx-mesa0 libxi6 libxxf86vm1 libxfixes3 libxrender1 libxkbcommon0 libsm6 libopenal1 >/dev/null
cat > "$RUN/client/options.txt" <<'OPTS'
tutorialStep:none
onboardAccessibility:false
skipMultiplayerWarning:true
joinedFirstServer:true
renderDistance:12
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
hideGui:false
bobView:false
overrideWidth:1920
overrideHeight:1080
mipmapLevels:0
entityDistanceScaling:1.5
OPTS
export DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe MESA_GL_VERSION_OVERRIDE=3.3
Xvfb :99 -screen 0 1920x1080x24 -nolisten tcp >"$OUT/maja-xvfb.log" 2>&1 &
openbox >"$OUT/maja-openbox.log" 2>&1 &
for _ in $(seq 1 30); do xdpyinfo -display :99 >/dev/null 2>&1 && break; sleep 1; done
xdpyinfo -display :99 >/dev/null 2>&1 || fail "No virtual display"
(
  cd mod
  chmod +x gradlew
  ./gradlew runClient --no-daemon --console=plain -PrunDir="$PWD/../$RUN" -PautoJoin=127.0.0.1:25565
) > "$OUT/maja-client.log" 2>&1 &
client=$!
for _ in $(seq 1 180); do
  players=$(rq list 2>&1 || true)
  [[ "$players" == *TestPlayer* ]] && break
  if ! kill -0 "$client" 2>/dev/null; then
    tail -30 "$OUT/maja-client.log" | while IFS= read -r line; do echo "::error title=client::${line:0:350}"; done
    fail "Client exited before joining"
  fi
  sleep 3
done
[[ "${players:-}" == *TestPlayer* ]] || fail "Client did not join the test server"
rq gamemode creative TestPlayer >/dev/null
rq weather clear >/dev/null
rq time set noon >/dev/null
POS=$(rq data get entity '@e[type=riftworld:el_gran_maja,limit=1]' Pos)
read -r X Y Z < <(python3 - "$POS" <<'PY'
import re,sys
m=re.search(r'\[\s*([-\d.eE+]+)d?\s*,\s*([-\d.eE+]+)d?\s*,\s*([-\d.eE+]+)d?\s*\]',sys.argv[1])
if not m: raise SystemExit('Could not parse '+sys.argv[1])
print(*m.groups())
PY
)
# Head is about 156 blocks forward (+Z). Camera is just in front, within tracking distance.
rq tp TestPlayer "$X" "$(( ${Y%.*} + 14 ))" "$(( ${Z%.*} + 188 ))" facing "$X" "$Y" "$(( ${Z%.*} + 115 ))" >/dev/null
sleep 35
WID=$(xdotool search --onlyvisible --name Minecraft | head -1 || true)
[ -n "$WID" ] || fail "Minecraft window not found"
xdotool windowraise "$WID" || true
import -window root "$OUT/maja-client-front.png"
if grep -Ei 'Error while rendering entity|Exception.*ElGranMaja|Unable to load.*el_gran_maja|Failed to load.*el_gran_maja' "$OUT/maja-client.log"; then
  fail "Client log reports a Maja renderer/resource failure"
fi
rq list | grep -q TestPlayer || fail "Client disconnected before the screenshot"
echo "OK: real client joined the server; screenshot saved to $OUT/maja-client-front.png (requires visual review)."

# Exercise the actual custom dimension and the new biome, not just overworld registration.
LOC=$(rq execute in riftworld:elisar positioned 0 54 0 run locate biome riftworld:maja_ocean)
echo "Biome locate: $LOC"
read -r OX OZ < <(python3 - "$LOC" <<'PY'
import re,sys
m=re.search(r'\[\s*(-?\d+)\s*,\s*(?:~|-?\d+)\s*,\s*(-?\d+)\s*\]',sys.argv[1])
if not m: raise SystemExit('Could not parse biome location: '+sys.argv[1])
print(*m.groups())
PY
)
[[ "${OX:-}" =~ ^-?[0-9]+$ && "${OZ:-}" =~ ^-?[0-9]+$ ]] || fail "No ocean coordinates"
rq execute in riftworld:elisar run tp TestPlayer "$OX" 95 "$OZ" 0 0
sleep 60
BIOME=''
for _ in $(seq 1 24); do
  BIOME=$(rq execute in riftworld:elisar if biome "$OX" 54 "$OZ" riftworld:maja_ocean 2>&1 || true)
  [[ "$BIOME" == *"Test passed"* ]] && break
  [[ "$BIOME" == *"Test failed"* ]] && break
  sleep 5
done
echo "Ocean biome check: $BIOME"
[[ "$BIOME" == *"Test passed"* ]] || fail "Ocean at $OX,54,$OZ; locate=$LOC; check=$BIOME"
# Natural spawning is probabilistic and clearance-sensitive: report the observation without
# equating a timeout to an absence of support. The collision and build gates remain mandatory.
seen=0
for _ in $(seq 1 24); do
  result=$(rq execute in riftworld:elisar if entity '@e[type=riftworld:el_gran_maja]' 2>&1 || true)
  if [[ "$result" == *"Test passed"* ]]; then seen=1; break; fi
  sleep 5
done
if [ "$seen" = 1 ]; then echo "::notice title=Natural Maja spawn::Observed in a real new Elisar ocean."
else echo "::warning title=Natural Maja spawn::Not observed within two minutes at this ocean location."; fi
if grep -Ei 'Render thread/FATAL|Minecraft Crash Report|Error while rendering|NullPointerException' "$OUT/maja-client.log"; then
  fail "Client crashed or reported a rendering exception after entering the ocean"
fi
rq list | grep -q TestPlayer || fail "Client disconnected in the ocean"
echo "OK: client remained connected after entering the generated ocean."
