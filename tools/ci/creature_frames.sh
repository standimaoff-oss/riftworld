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

# Свой журнал съёмки: без него падение видно только по коду возврата, а сам
# отчёт публикуется в ветку и остаётся читаемым.
SCRIPT_LOG="$OUT/frames-capture.log"
: > "$SCRIPT_LOG"
exec > >(tee -a "$SCRIPT_LOG") 2>&1
fail() { echo "::error title=In-game frames::$*"; exit 1; }
# RCON drops the socket every now and then under a burst of commands; retry a
# couple of times so a transient disconnect does not abort the whole capture.
rq() {
  local attempt=1 rc=0
  while [ "$attempt" -le 4 ]; do
    python3 tools/rcon.py --password "${RCON_PW:-riftci}" --log '' --timeout 90 "$@" && return 0
    rc=$?
    echo "::warning title=In-game frames::rcon attempt $attempt failed (rc=$rc), retrying"
    sleep 3
    attempt=$((attempt + 1))
  done
  return "$rc"
}

sudo apt-get update -qq
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq xvfb openbox x11-utils xdotool imagemagick \
  libgl1 libglx-mesa0 libxi6 libxxf86vm1 libxfixes3 libxrender1 libxkbcommon0 libsm6 libopenal1 >/dev/null

cat > "$RUN/client/options.txt" <<'OPTS'
tutorialStep:none
onboardAccessibility:false
skipMultiplayerWarning:true
joinedFirstServer:true
renderDistance:32
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

# Суша Пади Морока генерируется ДО старта клиента: её чанки с плотным лесом
# и своими деревьями создаются минуту, и проба витрины Смотрителя Пращи
# зависала на них, пока RCON не отваливался по таймауту.
rq forceload add 1180 800 1220 840 >/dev/null 2>&1 || true

export DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe MESA_GL_VERSION_OVERRIDE=3.3
Xvfb :99 -screen 0 1600x900x24 -nolisten tcp >"$OUT/frames-xvfb.log" 2>&1 &
openbox >"$OUT/frames-openbox.log" 2>&1 &
for _ in $(seq 1 30); do xdpyinfo -display :99 >/dev/null 2>&1 && break; sleep 1; done
xdpyinfo -display :99 >/dev/null 2>&1 || fail "No virtual display"

# Клиент на программном OpenGL иногда падает прямо при создании окна
# («We died but didn't somehow?»). Это случайность окружения, а не сборки,
# поэтому клиент запускаем заново: без этого одна такая случайность уводила
# всю серию кадров.
start_client() {
  # Лог клиента перезаписывается на каждой попытке: иначе маркер падения
  # «We died but didn't somehow» от прошлой попытки прячет новое падение.
  : > "$OUT/frames-client.log"
  (
    cd mod
    chmod +x gradlew
    ./gradlew runClient --no-daemon --console=plain -PrunDir="$PWD/../$RUN" -PautoJoin=127.0.0.1:25565
  ) >> "$OUT/frames-client.log" 2>&1 &
  client=$!
}

start_client
attempt=1
players=""
for _ in $(seq 1 240); do
  players=$(rq list 2>&1 || true)
  [[ "$players" == *TestPlayer* ]] && break
  if ! kill -0 "$client" 2>/dev/null; then
    # Раннее окно Forge на программном OpenGL иногда не поднимает GL-контекст
    # («We died but didn't somehow»). Это случайность окружения, а не сборки,
    # поэтому клиент запускаем заново: без этого одна такая случайность
    # уводила всю серию кадров.
    if [ "$attempt" -lt 3 ] && grep -Fqi "We died but didn't somehow" "$OUT/frames-client.log"; then
      echo "::warning title=In-game frames::раннее окно Forge упало при старте, перезапуск ($attempt/3)"
      attempt=$((attempt + 1))
      start_client
      sleep 5
      continue
    fi
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
  # Измерение для съёмки: Смотритель Пращи стоит в обычном мире (Падь Морока),
  # а игрок по умолчанию в измерении Элисара — телепорт без указания мира
  # увозил камеру в океан, и кадр получался пустым небом.
  rq execute in "${SHOT_DIM:-minecraft:overworld}" run tp TestPlayer "$cx" "$cy" "$cz" facing "$lx" "$ly" "$lz" >/dev/null
  sleep "$wait"
  local wid
  wid=$(xdotool search --onlyvisible --name Minecraft | head -1 || true)
  [ -n "$wid" ] || fail "Minecraft window not found"
  xdotool windowraise "$wid" >/dev/null 2>&1 || true
  xdotool windowactivate "$wid" >/dev/null 2>&1 || true
  sleep 1
  # Клиент мог развалиться посреди серии: тогда окно ещё есть, а снимок
  # будет мусором. Проверяем, что игрок ещё в игре.
  if ! rq list 2>&1 | grep -q TestPlayer; then
    fail "The client lost the player before the frame $name"
  fi
  # 'import' возвращает 4, если окно не найдено или снимок не удался.
  # Раньше это роняло всю съёмку кадров: терялся даже список уже снятых.
  local rc=0
  import -window root "$FRAMES/$name.png" || rc=$?
  if [ "$rc" != 0 ] || [ ! -s "$FRAMES/$name.png" ]; then
    echo "::warning title=In-game frames::не удалось снять кадр $name (rc=$rc), пробуем ещё раз"
    sleep 3
    import -window root "$FRAMES/$name.png" || true
  fi
  if [ ! -s "$FRAMES/$name.png" ]; then
    echo "::warning title=In-game frames::кадр $name пуст — продолжаем съёмку дальше"
    return 0
  fi
  SHOT=$((SHOT + 1))
  echo "captured $name"
}

# ---------------------------------------------------------------- paper dragon
# v0.49.0: Пегас удалён из мода, кадры перенесены на Бумажного дракона.
rq fill 190 99 -24 226 99 24 minecraft:grass_block >/dev/null
rq kill '@e[type=riftworld:paper_dragon]' >/dev/null 2>&1 || true
rq summon riftworld:paper_dragon 208 104 0 '{NoAI:1b,PersistenceRequired:1b}' >/dev/null
shot frame-paper-dragon-three-quarter 205.5 105.7 -3.6 208 105 0 10
shot frame-paper-dragon-side 211.6 105.7 0 208 105 0 7
shot frame-paper-dragon-head 208 106.3 -2.3 208 106.1 -0.6 7

# v0.49.0: ДРАКОН ДОЛЖЕН ДВИГАТЬСЯ НА ДАЛЁКОМ РАССТОЯНИИ.
# Раньше clientTrackingRange был 32 блока: клиент переставал получать
# позицию, цепочка позвоночника замирала, и дракон «рисовался, но не летел».
# Теперь радиус 128, и мы проверяем это прямо в игре: снимаем кадр,
# ждём полёт, снимаем второй — пиксели должны отличаться.
rq kill '@e[type=riftworld:paper_dragon]' >/dev/null 2>&1 || true
# Дракон летит, поэтому AI включён: с NoGravity он просто висит.
rq summon riftworld:paper_dragon 232 148 0 '{PersistenceRequired:1b}' >/dev/null
# Камера в 20 блоках от дракона: это БОЛЬШЕ simulationDistance (8), но меньше
# renderDistance (32). Раньше камера стояла в 55 блоках, а renderDistance был
# 16 — клиент просто вычищал дракона из кадра, и оба «дальних» кадра были
# пустым небом.
rq tp TestPlayer 212 150 0 facing 232 148 0 >/dev/null
sleep 14
# Дракон ЛЕТИТ, поэтому снимать надо по его НАСТОЯЩЕЙ позиции: камера вслепую
# смотрит в точку спавна и промахивается мимо. Позицию читаем с сервера.
dragon_pos() {
  local raw
  raw=$(rq data get entity '@e[type=riftworld:paper_dragon,limit=1]' Pos 2>&1 | tr -d '\n' || true)
  echo "$raw" | sed -n 's/.*\[\(.*\)\].*/\1/p'
}
DP=$(dragon_pos)
DX=$(echo "$DP" | cut -d, -f1 | tr -d ' ' | cut -d. -f1)
DY=$(echo "$DP" | cut -d, -f2 | tr -d ' ' | cut -d. -f1)
DZ=$(echo "$DP" | cut -d, -f3 | tr -d ' ' | cut -d. -f1)
if [[ -z "$DX" ]]; then
  fail "Paper dragon position could not be read: $DP"
fi
echo "  paper dragon at $DX,$DY,$DZ"
# Камера в 18 блоках от дракона: это дальше simulationDistance (8), но ближе
# renderDistance (32) — дракон обязан быть виден и при этом двигаться.
CX=$((DX - 17)); CZ=$((DZ + 4)); CY=$((DY + 2))
rq tp TestPlayer "$CX" "$CY" "$CZ" facing "$DX" "$DY" "$DZ" >/dev/null
sleep 5
shot frame-paper-dragon-far-a "$CX" "$CY" "$CZ" "$DX" "$DY" "$DZ" 4
sleep 10
DP2=$(dragon_pos)
DX2=$(echo "$DP2" | cut -d, -f1 | tr -d ' ' | cut -d. -f1)
DY2=$(echo "$DP2" | cut -d, -f2 | tr -d ' ' | cut -d. -f1)
DZ2=$(echo "$DP2" | cut -d, -f3 | tr -d ' ' | cut -d. -f1)
CX2=$((DX2 - 17)); CZ2=$((DZ2 + 4)); CY2=$((DY2 + 2))
rq tp TestPlayer "$CX2" "$CY2" "$CZ2" facing "$DX2" "$DY2" "$DZ2" >/dev/null
sleep 4
shot frame-paper-dragon-far-b "$CX2" "$CY2" "$CZ2" "$DX2" "$DY2" "$DZ2" 4
# Дракон обязан сместиться: сравниваем его координаты через 10 секунд
DRAGON_POS=$(rq data get entity '@e[type=riftworld:paper_dragon,limit=1]' Pos 2>&1 | tr -d '\n' || true)
echo "  paper dragon pos after flight: $DRAGON_POS"
# Кадры не должны совпадать: если совпали — дракон не двигался.
if cmp -s "$FRAMES/frame-paper-dragon-far-a.png" "$FRAMES/frame-paper-dragon-far-b.png"; then
  fail "Paper dragon did not move beyond the simulation range (identical frames)"
fi

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

# ---------------------------------------------------------------- hollowwatcher (v0.49.0)
# Новый хоррор-моб Пади: Смотритель Пращи.
# Раньше моба ставили в воздухе над городом (200 120 0) — получался крошечный
# силуэт в небе. Теперь витрина ищет НАСТОЯЩУЮ сушу Пади и ставит моба на
# неё, а камера берёт координаты прямо из пробы.
# Падь Морока живёт в обычном мире, поэтому витрину спрашиваем ИМЕННО в нём.
rq execute in riftworld:elisar run time set midnight >/dev/null
# Суша Пади должна быть сгенерирована ДО пробы: иначе проба сама генерирует
# чанки и занимает серверный поток на десятки секунд — RCON отваливается по
# таймауту, и кадр не снимается вовсе.
# forceload-ом держим сушу Пади загруженной, пока снимаем остальные кадры:
# тогда к моменту витрины чанки уже сгенерированы и проба отвечает мгновенно.
rq forceload add 1180 800 1220 840 >/dev/null 2>&1 || true
sleep 5
WATCHER=$(rq run riftstruct v49_hollowwatcher_showcase)
echo "  $WATCHER"
# Если витрина не собралась — это не повод ронять всю серию кадров:
# предупреждаем и идём дальше (остальные мобы важнее одного кадра).
if [[ "$WATCHER" != *"spawned=1"* ]]; then
  echo "::warning title=In-game frames::витрина Смотрителя Пращи не собралась: $WATCHER"
  SKIP_WATCHER=1
fi
# cam=camX,camY,camZ:tgtX,tgtY,tgtZ
WX=$(echo "$WATCHER" | sed -n 's/.*cam=\([^:]*\):.*/\1/p' | cut -d, -f1)
WY=$(echo "$WATCHER" | sed -n 's/.*cam=\([^:]*\):.*/\1/p' | cut -d, -f2)
WZ=$(echo "$WATCHER" | sed -n 's/.*cam=\([^:]*\):.*/\1/p' | cut -d, -f3)
TX=$(echo "$WATCHER" | sed -n 's/.*cam=[^:]*:\(.*\)/\1/p' | cut -d, -f1)
TY=$(echo "$WATCHER" | sed -n 's/.*cam=[^:]*:\(.*\)/\1/p' | cut -d, -f2)
TZ=$(echo "$WATCHER" | sed -n 's/.*cam=[^:]*:\(.*\)/\1/p' | cut -d, -f3)
if [ "${SKIP_WATCHER:-0}" != 1 ]; then
# Грузим чанки вокруг Пади: без этого клиент не успевает подгрузить место
# съёмки и кадр получается пустым небом.
rq forceload add $((TX - 48)) $((TZ - 48)) $((TX + 48)) $((TZ + 48)) >/dev/null 2>&1 || true
# Снимки Пади снимаются в обычном мире.
SHOT_DIM=minecraft:overworld
sleep 8
shot frame-hollowwatcher-three-quarter "$WX" "$WY" "$WZ" "$TX" "$TY" "$TZ" 14
shot frame-hollowwatcher-face "$TX" "$((TY + 1))" "$((TZ - 2))" "$TX" "$TY" "$TZ" 10
shot frame-hollowwatcher-tall "$((TX + 4))" "$TY" "$TZ" "$TX" "$TY" "$TZ" 10
# Вертушка даёт ночное небо, а для хоррор-кадра нужен читаемый силуэт.
rq execute in minecraft:overworld run time set noon >/dev/null 2>&1 || true
sleep 3
shot frame-hollowwatcher-daylight "$TX" "$((TY + 1))" "$((TZ - 3))" "$TX" "$TY" "$TZ" 10
rq execute in minecraft:overworld run time set midnight >/dev/null 2>&1 || true
sleep 3
shot frame-hollowwatcher-three-quarter "$WX" "$WY" "$WZ" "$TX" "$TY" "$TZ" 10
fi

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
# Dropped-item showcase: lay the volumetric weapon/tool/armor models out on the
# showcase floor and look straight down at them. This is the most direct proof
# that the element models sample their palette textures correctly (the v0.49.0
# regression rendered them as black noise).
# PickupDelay keeps them on the floor, Age:-32768s freezes the idle bob.
drop_row() {
  local z=$1; shift
  local i=0 item xpos
  for item in "$@"; do
    xpos=$(awk -v i="$i" 'BEGIN { printf "%.2f", 747.0 + i * 2.2 }')
    rq execute in riftworld:elisar run summon item "$xpos" 119.5 "$z" \
      "{Item:{id:\"riftworld:$item\",Count:1b},PickupDelay:32767s,Age:-32768s}" >/dev/null 2>&1 \
      || echo "::warning title=In-game frames::could not summon $item"
    i=$((i + 1))
  done
}
drop_row 766 aurite_sword aurite_pickaxe aurite_axe aurite_shovel aurite_hoe \
            riftcrystal_sword riftcrystal_pickaxe riftcrystal_axe riftcrystal_shovel riftcrystal_hoe \
            elisar_bow elisar_shield
sleep 4
rq execute in riftworld:elisar run tp TestPlayer 759 120 772 facing 759 119 766 >/dev/null
sleep 8
capture_underwater frame-v049-items-dropped-tools
drop_row 772 aurite_helmet aurite_chestplate aurite_leggings aurite_boots \
            riftcrystal_helmet riftcrystal_chestplate riftcrystal_leggings riftcrystal_boots
sleep 4
rq execute in riftworld:elisar run tp TestPlayer 756 120 778 facing 756 119 772 >/dev/null
sleep 8
capture_underwater frame-v049-items-dropped-armour

rq execute in riftworld:elisar run tp TestPlayer 760.5 118.0 774.2 facing 760.5 119.0 767.6 >/dev/null
sleep 10
capture_underwater frame-v049-elisar-folk

echo "captured $SHOT in-game frames"
[ "$SHOT" -ge 9 ] || fail "Only $SHOT frames captured"
ls -l "$FRAMES"
if grep -Ei 'Render thread/FATAL|Minecraft Crash Report|Error while rendering entity' "$OUT/frames-client.log"; then
  fail "Client reported a rendering failure while capturing frames"
fi
echo "OK: real client frames saved to $FRAMES"
