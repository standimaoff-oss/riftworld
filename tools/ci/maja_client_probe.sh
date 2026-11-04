#!/usr/bin/env bash
# A focused visual smoke check; server + NoAI test titan come from maja_server_probe.sh.
set -euo pipefail
OUT=${OUT:-ci-out}
RUN=${RUN:-ci/maja-run}
mkdir -p "$OUT" "$RUN/client/config"
fail() { echo "::error title=Maja client probe::$*"; exit 1; }
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
fov:0.0
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
rq tp TestPlayer "$X" "$(( ${Y%.*} + 14 ))" "$(( ${Z%.*} + 176 ))" facing "$X" "$Y" "$(( ${Z%.*} + 115 ))" >/dev/null
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
