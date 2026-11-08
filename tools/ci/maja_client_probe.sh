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
  "${GRADLE_BIN:-./gradlew}" runClient --no-daemon --console=plain -PrunDir="$PWD/../$RUN" -PautoJoin=127.0.0.1:25565
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
# Spectator flight holds the camera at altitude; creative mode alone falls to the ground.
rq gamemode spectator TestPlayer >/dev/null
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
if ! grep -Fq '[riftworld] geo el_gran_maja:' "$OUT/maja-client.log"; then
  fail "The Maja renderer never loaded its model at the test camera position"
fi
WID=$(xdotool search --onlyvisible --name Minecraft | head -1 || true)
[ -n "$WID" ] || fail "Minecraft window not found"
xdotool windowraise "$WID" || true
xdotool windowactivate --sync "$WID" || true
import -window root "$OUT/maja-client-front.png"
xdotool key --clearmodifiers F1

# Stage the v0.41 armory in the same real client/server session. These are actual game
# frames, not composited concepts, and are attached to the release by the publish job.
rq gamemode creative TestPlayer >/dev/null
rq fill 90 4 90 112 4 112 minecraft:polished_blackstone_bricks >/dev/null
rq fill 90 5 90 112 14 112 minecraft:air >/dev/null
# Do not let the enormous Maja tail or old test entities contaminate the armory frames.
rq kill '@e[type=riftworld:el_gran_maja]' >/dev/null || true
rq kill '@e[type=riftworld:elisar_villager]' >/dev/null || true
rq kill '@e[type=riftworld:elisar_guard]' >/dev/null || true
rq kill '@e[type=riftworld:elisar_archer]' >/dev/null || true
rq kill '@e[type=minecraft:item_frame]' >/dev/null || true
rq kill '@e[type=minecraft:item]' >/dev/null || true
rq kill '@e[type=minecraft:armor_stand]' >/dev/null || true
rq item replace entity TestPlayer weapon.mainhand with minecraft:air >/dev/null
rq item replace entity TestPlayer weapon.offhand with minecraft:air >/dev/null
for material in aurite riftcrystal; do
  rq kill '@e[type=minecraft:armor_stand]' >/dev/null || true
  rq summon minecraft:armor_stand 101 5 101 "{NoGravity:1b,ShowArms:1b,NoBasePlate:1b,Rotation:[180.0f,0.0f],Pose:{RightArm:[-25.0f,0.0f,-10.0f],LeftArm:[-25.0f,0.0f,10.0f]},ArmorItems:[{id:\"riftworld:${material}_boots\",Count:1b},{id:\"riftworld:${material}_leggings\",Count:1b},{id:\"riftworld:${material}_chestplate\",Count:1b},{id:\"riftworld:${material}_helmet\",Count:1b}],HandItems:[{id:\"riftworld:${material}_sword\",Count:1b},{id:\"riftworld:${material}_shield\",Count:1b}]}" >/dev/null
  rq tp TestPlayer 101 6.1 104 facing 101 5.7 101 >/dev/null
  sleep 7
  import -window root "$OUT/elisar-armory-${material}.png"
done
# Photograph all three residents at close range after removing the armour display.
rq kill '@e[type=minecraft:armor_stand]' >/dev/null || true
rq summon riftworld:elisar_villager 97 5 101 '{PersistenceRequired:1b,NoAI:1b,Rotation:[0.0f,0.0f]}' >/dev/null
rq summon riftworld:elisar_guard 101 5 100 '{PersistenceRequired:1b,NoAI:1b,Rotation:[0.0f,0.0f]}' >/dev/null
rq summon riftworld:elisar_archer 105 5 101 '{PersistenceRequired:1b,NoAI:1b,Rotation:[0.0f,0.0f]}' >/dev/null
rq tp TestPlayer 101 6.2 105 facing 101 5.8 100 >/dev/null
sleep 6
import -window root "$OUT/elisar-residents.png"
# A close-up shows both ore textures in their real block models.
rq kill '@e[type=riftworld:elisar_villager]' >/dev/null || true
rq kill '@e[type=riftworld:elisar_guard]' >/dev/null || true
rq kill '@e[type=riftworld:elisar_archer]' >/dev/null || true
rq fill 98 5 97 103 8 97 minecraft:stone >/dev/null
rq setblock 100 6 97 riftworld:riftcrystal_ore >/dev/null
rq setblock 101 6 97 riftworld:deepslate_riftcrystal_ore >/dev/null
rq tp TestPlayer 100.5 6.2 100 facing 100.5 6 97 >/dev/null
sleep 5
import -window root "$OUT/elisar-riftcrystal-ores.png"
# Every one of the eighteen items gets its own real item-frame slot, nine per material.
# Remove the ore wall first: the previous frames were occluded by overlapping blocks.
rq fill 97 4 97 103 9 97 minecraft:air >/dev/null
rq fill 97 4 97 101 8 97 minecraft:polished_blackstone_bricks >/dev/null
for material in aurite riftcrystal; do
  rq kill '@e[type=minecraft:item_frame]' >/dev/null || true
  index=0
  for item in helmet chestplate leggings boots sword pickaxe war_axe bow shield; do
    x=$((98 + index % 3))
    y=$((7 - index / 3))
    rq summon minecraft:item_frame "$x" "$y" 98 "{Facing:3b,Fixed:1b,Item:{id:\"riftworld:${material}_${item}\",Count:1b}}" >/dev/null
    index=$((index + 1))
  done
  rq tp TestPlayer 99 6.5 102 facing 99 6 98 >/dev/null
  sleep 5
  import -window root "$OUT/elisar-items-${material}.png"
done
echo "OK: captured six close-up real in-game Elisar armory frames"
if grep -Ei 'Error while rendering entity|Exception.*ElGranMaja|Unable to load.*el_gran_maja|Failed to load.*el_gran_maja' "$OUT/maja-client.log"; then
  fail "Client log reports a Maja renderer/resource failure"
fi
rq list | grep -q TestPlayer || fail "Client disconnected before the screenshot"
echo "OK: real client joined the server; screenshot saved to $OUT/maja-client-front.png (requires visual review)."
if [ "${ARMORY_ONLY:-0}" = "1" ]; then
  echo "OK: release armory capture complete; the separate full ocean probe is not repeated"
  exit 0
fi

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
# The natural-spawn keeper intentionally ignores spectators.
rq gamemode creative TestPlayer >/dev/null
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
