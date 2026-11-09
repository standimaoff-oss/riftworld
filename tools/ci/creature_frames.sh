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
renderDistance:16
simulationDistance:8
maxFps:30
guiScale:2
fov:0.35
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
shot frame-pegasus-three-quarter 205.5 101.7 -3.6 208 101 0 10
shot frame-pegasus-side 211.6 101.7 0 208 101 0 7
shot frame-pegasus-head 208 102.3 -2.3 208 102.1 -0.6 7

# ---------------------------------------------------------------- liopleurodon
# locate может вернуть пустоту, пока чанки прогреваются — пробуем несколько раз
# и в крайнем случае берём известный центр глубокого океана (его же использует
# maja_client_probe). Падать из-за поиска биома тут незачем: это всего лишь кадры.
OX=""; OZ=""
for attempt in 1 2 3 4 5 6; do
  LOC=$(rq execute in riftworld:elisar positioned 0 54 0 run locate biome riftworld:maja_ocean 2>&1 | tr -d '\n' || true)
  echo "ocean attempt $attempt: $LOC"
  read -r OX OZ < <(python3 - "$LOC" <<'PY'
import re,sys
m=re.search(r'\[\s*(-?\d+)\s*,\s*(?:~|-?\d+)\s*,\s*(-?\d+)\s*\]',sys.argv[1])
print((' '.join(m.groups())) if m else '')
PY
) || true
  if [[ "${OX:-}" =~ ^-?[0-9]+$ && "${OZ:-}" =~ ^-?[0-9]+$ ]]; then break; fi
  OX=""; OZ=""
  sleep 10
done
if [[ ! "${OX:-}" =~ ^-?[0-9]+$ ]]; then
  echo "::warning title=In-game frames::locate biome не нашёл океан, берём известный центр"
  OX=1715; OZ=2126
fi
echo "ocean centre: $OX $OZ"
rq execute in riftworld:elisar run forceload add "$OX" "$OZ" >/dev/null 2>&1 || true
rq kill '@e[type=riftworld:liopleurodon]' >/dev/null 2>&1 || true
rq execute in riftworld:elisar run summon riftworld:liopleurodon "$OX" 50 "$OZ" \
  '{NoAI:1b,PersistenceRequired:1b}' >/dev/null
sleep 6
rq execute in riftworld:elisar run tp TestPlayer "$((OX + 5))" 51 "$((OZ - 6))" facing "$OX" 50 "$OZ" >/dev/null
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
rq execute in riftworld:elisar run tp TestPlayer "$OX" 51 "$((OZ - 9))" facing "$OX" 50 "$OZ" >/dev/null
sleep 6
capture_underwater frame-liopleurodon-side
rq execute in riftworld:elisar run tp TestPlayer "$OX" 51.4 "$((OZ - 3))" facing "$OX" 50.6 "$OZ" >/dev/null
sleep 6
capture_underwater frame-liopleurodon-head

# ---------------------------------------------------------------- El Gran Maja
rq kill '@e[type=riftworld:el_gran_maja]' >/dev/null 2>&1 || true
rq forceload add -80 -176 80 176 >/dev/null 2>&1 || true
rq summon riftworld:el_gran_maja 0 200 0 '{NoAI:1b,PersistenceRequired:1b}' >/dev/null
for _ in $(seq 1 80); do
  state=$(rq data get entity '@e[type=riftworld:el_gran_maja,limit=1]' majaReady 2>&1 || true)
  [[ "$state" == *1b* ]] && break
  sleep 3
done
sleep 6
# whole body from the side (the titan is ~322 blocks long) and the head up close
shot frame-maja-body 150 228 70 0 202 -45 14
shot frame-maja-head 0 214 188 0 200 115 12

# ---------------------------------------------------------------- v0.48.0 Armory & Elisar Folk
rq execute in riftworld:elisar run time set noon >/dev/null
rq execute in riftworld:elisar run forceload add 744 744 776 776 >/dev/null 2>&1 || true
rq execute in riftworld:elisar run tp TestPlayer 760 123 770 0 0 >/dev/null
sleep 14
rq execute in riftworld:elisar positioned 760 118 760 run riftstruct v40_gear >/dev/null
sleep 8
rq execute in riftworld:elisar run tp TestPlayer 755.25 118.0 764.2 facing 755.25 119.0 760.5 >/dev/null
sleep 10
capture_underwater frame-v049-armors-closeup

# Pitch-dark rift-slate vault to verify full-bright emissive Riftcrystal armor glow at zero sky/block light
rq execute in riftworld:elisar run time set midnight >/dev/null
rq execute in riftworld:elisar run fill 750 117 758 761 123 765 riftworld:rift_slate hollow >/dev/null
rq execute in riftworld:elisar run tp TestPlayer 757.3 118.0 763.3 facing 757.5 119.0 760.5 >/dev/null
sleep 8
capture_underwater frame-v049-riftcrystal-night-glow

# Restore open daylight showcase for tools & settlement folk closeups
rq execute in riftworld:elisar run fill 750 118 758 761 123 765 minecraft:air >/dev/null
rq execute in riftworld:elisar run time set noon >/dev/null
rq execute in riftworld:elisar positioned 760 118 760 run riftstruct v40_gear >/dev/null
sleep 6
rq execute in riftworld:elisar run tp TestPlayer 764.25 118.0 763.0 facing 764.25 119.0 760.5 >/dev/null
sleep 8
capture_underwater frame-v049-tools-closeup

# Dropped-item showcase: lay the volumetric weapon/tool/armor models out on the
# showcase floor and look straight down at them. This is the most direct proof
# that the element models sample their palette textures correctly (the v0.49.0
# regression rendered them as black noise).
rq execute in riftworld:elisar run fill 754 117 756 768 117 762 minecraft:air >/dev/null
for pair in \
  "752 762.0 757.5 riftworld:aurite_sword" \
  "753 762.0 757.5 riftworld:aurite_pickaxe" \
  "754 762.0 757.5 riftworld:aurite_axe" \
  "755 762.0 757.5 riftworld:aurite_shovel" \
  "756 762.0 757.5 riftworld:aurite_hoe" \
  "757 762.0 757.5 riftworld:aurite_bow" \
  "758 762.0 757.5 riftworld:elisar_shield" \
  "759 762.0 757.5 riftworld:riftcrystal_sword" \
  "760 762.0 757.5 riftworld:riftcrystal_pickaxe" \
  "761 762.0 757.5 riftworld:riftcrystal_axe" \
  "762 762.0 757.5 riftworld:elisar_bow_pulling_0" \
  "752 762.0 758.5 riftworld:aurite_helmet" \
  "753 762.0 758.5 riftworld:aurite_chestplate" \
  "754 762.0 758.5 riftworld:aurite_leggings" \
  "755 762.0 758.5 riftworld:aurite_boots" \
  "756 762.0 758.5 riftworld:riftcrystal_helmet" \
  "757 762.0 758.5 riftworld:riftcrystal_chestplate" \
  "758 762.0 758.5 riftworld:riftcrystal_leggings" \
  "759 762.0 758.5 riftworld:riftcrystal_boots"; do
  set -- $pair
  rq execute in riftworld:elisar positioned "$1" 118 "$2" run summon item "$3" >/dev/null 2>&1 || true
done
sleep 6
rq execute in riftworld:elisar run tp TestPlayer 757.5 121.0 763.6 facing 757.5 117.0 757.9 >/dev/null
sleep 8
capture_underwater frame-v049-items-dropped

rq execute in riftworld:elisar run tp TestPlayer 760.5 118.0 774.2 facing 760.5 119.0 767.6 >/dev/null
sleep 10
capture_underwater frame-v049-elisar-folk

echo "captured $SHOT in-game frames"
[ "$SHOT" -ge 7 ] || fail "Only $SHOT frames captured"
ls -l "$FRAMES"
if grep -Ei 'Render thread/FATAL|Minecraft Crash Report|Error while rendering entity' "$OUT/frames-client.log"; then
  fail "Client reported a rendering failure while capturing frames"
fi
echo "OK: real client frames saved to $FRAMES"
