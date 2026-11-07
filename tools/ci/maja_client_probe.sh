#!/usr/bin/env bash
# A focused visual smoke check; server + NoAI test titan come from maja_server_probe.sh.
set -Eeuo pipefail
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
trap 'code=$?; echo "::error title=Client test failed::line $LINENO, exit $code, command: $BASH_COMMAND"' ERR
rq() { python3 tools/rcon.py --password "${RCON_PW:-riftci}" --log "$OUT/client-commands.log" --timeout 90 "$@"; }

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
export ALSOFT_DRIVERS=null DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe MESA_GL_VERSION_OVERRIDE=3.3
Xvfb :99 -screen 0 1920x1080x24 -nolisten tcp >"$OUT/maja-xvfb.log" 2>&1 &
openbox >"$OUT/maja-openbox.log" 2>&1 &
for _ in $(seq 1 30); do xdpyinfo -display :99 >/dev/null 2>&1 && break; sleep 1; done
xdpyinfo -display :99 >/dev/null 2>&1 || fail "No virtual display"
launch_client() {
  ( cd mod
    chmod +x gradlew
    ./gradlew runClient --no-daemon --console=plain -PrunDir="$PWD/../$RUN" -PautoJoin=127.0.0.1:25565
  ) > "$OUT/maja-client.log" 2>&1 &
  client=$!
}
# Раннее окно Forge на раннере иногда не поднимает GL-контекст («We died but
# didn't somehow»). Это не про игру, а про драйвер в контейнере — пробуем снова,
# вместо того чтобы ронять весь визуальный прогон.
launch_client
attempt=1
for _ in $(seq 1 240); do
  players=$(rq list 2>&1 || true)
  [[ "$players" == *TestPlayer* ]] && break
  if ! kill -0 "$client" 2>/dev/null; then
    if [ "$attempt" -lt 3 ] && grep -Fqi "We died but didn't somehow" "$OUT/maja-client.log"; then
      echo "::warning title=client::раннее окно Forge упало при старте, перезапуск ($attempt/3)"
      attempt=$((attempt + 1))
      launch_client
      sleep 5
      continue
    fi
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
import -window root "$OUT/maja-client-front.png"
sleep 3
import -window root "$OUT/maja-client-swim-motion.png"
if grep -Ei 'Error while rendering entity|Exception.*ElGranMaja|Unable to load.*el_gran_maja|Failed to load.*el_gran_maja' "$OUT/maja-client.log"; then
  fail "Client log reports a Maja renderer/resource failure"
fi
rq list | grep -q TestPlayer || fail "Client disconnected before the screenshot"
echo "OK: real client joined the server; screenshot saved to $OUT/maja-client-front.png (requires visual review)."

# Controlled visual scene in a disposable flat world. This shallow water
# surface is staged; it is not a screenshot of the natural Elisar ocean.
MAJA='@e[type=riftworld:el_gran_maja,limit=1]'
rq fill -70 59 20 70 59 170 minecraft:water >/dev/null
rq tp "$MAJA" 0 60 0 0 0 >/dev/null
# A side/front angle avoids the moving head overtaking the camera. Wait for
# the teleport and chunks before capturing the first phase.
rq tp TestPlayer 130 100 100 facing 0 67 140 >/dev/null
sleep 10
import -window root "$OUT/maja-breach-start.png"
rq data merge entity "$MAJA" '{NoAI:0b,majaMotionTick:125}' >/dev/null
sleep 5
import -window root "$OUT/maja-breach-rise.png"
echo 'OK: captured controlled water-surface breach frames for visual review.'

# The rebuilt creatures must load and render in the real client, not just the
# software preview. Clear the titan so it cannot obscure the much smaller rigs.
rq kill '@e[type=riftworld:el_gran_maja]' >/dev/null
rq summon riftworld:liopleurodon 0 200 0 '{NoAI:1b,NoGravity:1b,PersistenceRequired:1b}' >/dev/null
rq tp TestPlayer -5 202 7 facing 0 200 0 >/dev/null
sleep 15
grep -Fq '[riftworld] geo liopleurodon:' "$OUT/maja-client.log" || fail 'Liopleurodon model was not loaded by the real client'
import -window root "$OUT/fauna-liopleurodon.png"
sleep 1
import -window root "$OUT/fauna-liopleurodon-motion.png"
rq tp TestPlayer -12 201 0 facing 0 200 0 >/dev/null
sleep 4
import -window root "$OUT/fauna-liopleurodon-side.png"
rq summon riftworld:pegasus 32 200 0 '{NoAI:1b,NoGravity:1b,PersistenceRequired:1b}' >/dev/null
rq tp TestPlayer 27 203 6 facing 32 202 0 >/dev/null
sleep 15
grep -Fq '[riftworld] geo pegasus:' "$OUT/maja-client.log" || fail 'Pegasus model was not loaded by the real client'
import -window root "$OUT/fauna-pegasus-unsaddled.png"
sleep 1
import -window root "$OUT/fauna-pegasus-motion.png"
rq tp TestPlayer 20 202 0 facing 32 202 0 >/dev/null
sleep 4
import -window root "$OUT/fauna-pegasus-side.png"
rq tp TestPlayer 27 203 6 facing 32 202 0 >/dev/null
rq data merge entity '@e[type=riftworld:pegasus,limit=1]' '{Tamed:1b,Saddle:1b}' >/dev/null
rq data get entity '@e[type=riftworld:pegasus,limit=1]' Saddle | grep -q '1b' || fail 'Pegasus saddle was not synchronized'
sleep 5
import -window root "$OUT/fauna-pegasus-saddled.png"
# Photograph a trophy in hand and the creative inventory. Pixel-level visual
# review is still required; the server probe separately verifies actual drops.
rq fill 44 199 7 50 199 13 minecraft:stone >/dev/null
rq gamemode creative TestPlayer >/dev/null
rq tp TestPlayer 47 200 10 facing 32 202 0 >/dev/null
for item in wendigo_antler pegasus_feather liopleurodon_tooth; do rq give TestPlayer "riftworld:$item" 1 >/dev/null; done
rq item replace entity TestPlayer weapon.mainhand with riftworld:wendigo_antler >/dev/null
sleep 5
import -window root "$OUT/fauna-trophy-held.png"
xdotool key --window "$WID" e
sleep 3
import -window root "$OUT/fauna-trophy-inventory.png"
xdotool key --window "$WID" e
if grep -Ei 'не удалось загрузить geo-модель (pegasus|liopleurodon)|Error while rendering entity|Unable to load.*(pegasus|liopleurodon)|Failed to load.*(pegasus|liopleurodon)' "$OUT/maja-client.log"; then
  fail 'Client log reports a rebuilt fauna renderer/resource failure'
fi
rq list | grep -q TestPlayer || fail 'Client disconnected during the fauna visual test'
echo "OK: real client loaded both fauna rigs and photographed unsaddled/saddled Pegasus plus trophies (screenshots require review)."

# A real survival player in Elisar water must take damage from the predator.
# These are disposable coordinates; never run this script in a player world.
rq kill '@e[type=riftworld:liopleurodon]' >/dev/null || true
rq difficulty normal >/dev/null
rq fill 40 199 3 54 207 17 riftworld:elisar_water >/dev/null
rq gamemode survival TestPlayer >/dev/null
rq effect give TestPlayer minecraft:water_breathing 60 0 true >/dev/null
rq tp TestPlayer 47 202 10 >/dev/null
rq summon riftworld:liopleurodon 51 202 10 '{PersistenceRequired:1b}' >/dev/null
hit=0
for _ in $(seq 1 15); do
  health=$(rq data get entity TestPlayer Health 2>&1 || true)
  echo "Predator test: $health" | tee -a "$OUT/predator-test.txt"
  if echo "$health" | grep -Eq ': ([0-9](\.[0-9]+)?|1[0-9](\.[0-9]+)?)f$'; then hit=1; break; fi
  sleep 1
done
[ "$hit" = 1 ] || fail 'Liopleurodon did not damage the survival player in Elisar water'
rq kill '@e[type=riftworld:liopleurodon]' >/dev/null || true
rq gamemode creative TestPlayer >/dev/null
echo 'OK: Liopleurodon attacked a survival player in Elisar water.'

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
# First deep-ocean centre, rather than the nearest biome edge (which can
# overlap islands). Natural spawning must still use only already-loaded chunks.
OX=1715; OZ=2126
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
rq execute as TestPlayer at TestPlayer run riftmajacheck | tee "$OUT/maja-natural-diagnostic.txt" || true
if [ "$seen" = 1 ]; then echo "::notice title=Natural Maja spawn::Observed in a real new Elisar ocean."
else fail "Natural Maja did not spawn in the open core within two minutes; see maja-natural-diagnostic.txt"; fi
if grep -Ei 'Render thread/FATAL|Minecraft Crash Report|Error while rendering|NullPointerException' "$OUT/maja-client.log"; then
  fail "Client crashed or reported a rendering exception after entering the ocean"
fi
rq list | grep -q TestPlayer || fail "Client disconnected in the ocean"
echo "OK: client remained connected after entering the generated ocean."

# Stage the naturally spawned Maja inside the actual generated deep ocean.
# Freeze translation only; the client still animates the validated swimming bones.
rq execute in riftworld:elisar run tp '@e[type=riftworld:el_gran_maja,limit=1]' 1715 30 2126 0 0 >/dev/null
rq execute in riftworld:elisar run data merge entity '@e[type=riftworld:el_gran_maja,limit=1]' '{NoAI:1b}' >/dev/null
rq gamemode spectator TestPlayer >/dev/null
rq execute in riftworld:elisar run tp TestPlayer 1780 35 2145 facing 1715 30 2126 >/dev/null
sleep 15
import -window root "$OUT/maja-deep-ocean.png"
rq execute in riftworld:elisar run tp TestPlayer 1765 35 2200 facing 1715 30 2225 >/dev/null
sleep 10
import -window root "$OUT/maja-deep-ocean-swim.png"

# Inspect the actual generated wreck from two sides. Screenshot review is manual;
# the server probe independently asserts hull and mast blocks.
rq gamemode spectator TestPlayer >/dev/null
rq execute in riftworld:elisar run tp TestPlayer 2315 69 2172 facing 2315 64 2126 >/dev/null
sleep 35
import -window root "$OUT/ocean-wreck-overview.png"
rq execute in riftworld:elisar run tp TestPlayer 2280 72 2155 facing 2315 64 2126 >/dev/null
sleep 10
import -window root "$OUT/ocean-wreck-bow.png"
rq list | grep -q TestPlayer || fail 'Client disconnected while viewing the wreck'
echo 'OK: captured real-client wreck views for manual review.'

# ---------------------------------------------------------------------------
# Снаряжение и люди Элисара. Витрину ставит команда /riftstruct v40_gear: она
# сама чистит площадку, одевает четыре стойки и призывает жителя, стража и
# лучника. Кадры снимает настоящий клиент — их нужно смотреть глазами.
# ---------------------------------------------------------------------------
rq gamemode creative TestPlayer >/dev/null
rq execute in riftworld:elisar run time set noon >/dev/null
# llvmpipe подгружает новый участок медленно: греем клиент прямо на точке съёмки.
rq execute in riftworld:elisar run tp TestPlayer 760 123 770 0 0 >/dev/null
sleep 30
GEAR_INFO=$(rq execute in riftworld:elisar positioned 760 118 760 run riftstruct v40_gear)
echo "gear showcase: $GEAR_INFO"
[[ "$GEAR_INFO" == *"stands=4"* ]] || fail "Expected 4 armour stands, got: $GEAR_INFO"
[[ "$GEAR_INFO" == *"folk=3"* ]] || fail "Expected 3 Elisar folk, got: $GEAR_INFO"
STANDS=$(rq execute in riftworld:elisar positioned 760 118 760 if entity '@e[type=minecraft:armor_stand,distance=..40]' 2>&1 || true)
[[ "$STANDS" == *"Test passed"* ]] || fail "No armour stands near the showcase: $STANDS"

# gear_shot <имя файла> <имя камеры> [ожидание]
gear_shot() {
  local name=$1 cam=$2 wait=${3:-24} pose
  pose=$(python3 - "$GEAR_INFO" "$cam" <<'PY'
import re, sys
m = re.search(sys.argv[2] + r'=(-?[\d.]+),(-?[\d.]+),(-?[\d.]+):(-?[\d.]+),(-?[\d.]+),(-?[\d.]+)', sys.argv[1])
if not m:
    raise SystemExit('no camera pose')
print(' '.join(m.groups()))
PY
)
  [ -n "$pose" ] || fail "No camera pose '$cam' in: $GEAR_INFO"
  set -- $pose
  rq execute in riftworld:elisar run tp TestPlayer "$1" "$2" "$3" facing "$4" "$5" "$6" >/dev/null
  sleep "$wait"
  import -window root "$OUT/$name.png"
  echo "captured $name"
}
gear_shot gear-armour-row   cam_row   30
gear_shot gear-elisar-folk  cam_folk  26
gear_shot gear-closeup      cam_close 22
rq list | grep -q TestPlayer || fail 'Client disconnected during the gear showcase'
echo 'OK: captured the Elisar gear and folk showcase for manual review.'
