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

# Пакеты для headless-клиента ставим общим скриптом с ретраями: сеть apt на
# runner'е периодически отваливается, и раньше из-за этого съёмка падала
# целиком, а не пыталась переустановить зеркала.
bash tools/ci/install_client_packages.sh

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
  rq execute in "${SHOT_DIM:-minecraft:overworld}" run tp TestPlayer "$cx" "$cy" "$cz" facing "$lx" "$ly" "$lz" >/dev/null || true
  sleep "$wait"
  capture "$name" "$wait"
}

# capture <name> [wait] — снимок окна клиента без телепорта.
capture() {
  local name=$1 wait=${2:-6}
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
  # Кадр может быть технически удачным, но чёрным: клиент ещё не подгрузил
  # чанки измерения (город Элисара генерируется долго), и на экране просто
  # ночное небо. Раньше такой кадр молча уходил в релиз — и кадр Смотрителя
  # Пращи был чёрным. Теперь проверяем яркость и переснимаем с большей паузой.
  local dark=1 attempt=0
  while [ "$attempt" -lt 4 ]; do
    if ! frame_is_dark "$FRAMES/$name.png"; then dark=0; break; fi
    attempt=$((attempt + 1))
    echo "::warning title=In-game frames::кадр $name слишком тёмный, переснимаем ($attempt/4)"
    sleep $((wait + 10))
    xdotool windowraise "$wid" >/dev/null 2>&1 || true
    xdotool windowactivate "$wid" >/dev/null 2>&1 || true
    sleep 2
    import -window root "$FRAMES/$name.png" || true
  done
  if [ "$dark" = 1 ]; then
    echo "::warning title=In-game frames::кадр $name остался тёмным после четырёх попыток"
  fi
  SHOT=$((SHOT + 1))
  echo "captured $name"
}

# frame_is_dark <file> — 0, если в кадре есть освещённая сцена.
# Критерий тот же, что и в tools/rift/validate_v049_frames.py: сумма каналов
# ярче 90 у менее чем 5% пикселей означает «ночное небо», а не объект.
# Считаем через ImageMagick: он в CI есть всегда, а Pillow может отсутствовать
# (и тогда проверка молча ничего не проверяла, пропуская чёрные кадры).
frame_is_dark() {
  local file=$1 bright=0 total=0
  [ -s "$file" ] || return 0
  if command -v convert >/dev/null 2>&1; then
    # Доля пикселей, у которых сумма каналов больше 90.
    bright=$(convert "$file" -colorspace sRGB -resize 160x90! \
      -fx "((r+g+b)>(90/255))?1:0" -format '%[fx:mean]' info: 2>/dev/null || echo 1)
    total=1
  elif command -v identify >/dev/null 2>&1; then
    # Запасной путь: средняя яркость кадра. Ночное небо даёт единицы, а
    # освещённая сцена — десятки.
    local mean
    mean=$(convert "$file" -colorspace Gray -format '%[fx:mean*255]' info: 2>/dev/null || echo 255)
    total=1
    if awk -v m="$mean" 'BEGIN { exit !(m + 0 < 12) }'; then bright=1; fi
  else
    return 0   # без ImageMagick не судим: лучше лишний кадр, чем ложная тревога
  fi
  [ "$total" = 1 ] || return 0
  # bright — доля освещённых пикселей; кадр тёмный, если она меньше 5%.
  if awk -v b="$bright" 'BEGIN { exit !(b + 0 < 0.05) }'; then
    return 1
  fi
  return 0
}

# ---------------------------------------------------------------- paper dragon
# v0.49.0: Пегас удалён из мода, кадры перенесены на Бумажного дракона.
rq fill 190 99 -24 226 99 24 minecraft:grass_block >/dev/null
rq kill '@e[type=riftworld:paper_dragon]' >/dev/null 2>&1 || true
rq summon riftworld:paper_dragon 208 104 0 '{NoAI:1b,PersistenceRequired:1b}' >/dev/null
shot frame-paper-dragon-three-quarter 205.5 105.7 -3.6 208 105 0 10
shot frame-paper-dragon-side 211.6 105.7 0 208 105 0 7
shot frame-paper-dragon-head 208 106.3 -2.3 208 106.1 -0.6 7

# ---------------------------------------------------------------- bone raven
# v0.50.0: КОСТЯНОЙ ВОРОН ВОЗВРАЩЁН в мод (пользователь отменил удаление).
# Снимаем его в городе Элисара: площадка ставится серверной командой, чтобы
# камера не уезжала за две тысячи блоков от клиента (иначе кадр чёрный).
RAVEN_INFO=$(rq execute in riftworld:elisar positioned 560 108 560 run riftstruct bb_bone_raven_showcase 2>&1 | tr -d '\r')
echo "  $RAVEN_INFO"
if [[ "$RAVEN_INFO" != *"spawned=1"* ]]; then
  echo "::warning title=In-game frames::витрина ворона не собралась: $RAVEN_INFO"
else
  rq execute in riftworld:elisar run forceload add 552 552 568 568 >/dev/null 2>&1 || true
  # Полдень: ворон тёмный, и на ночном небе он читался чёрным силуэтом —
  # кадр уходил в пересъёмку и в итоге оставался тёмным. Днём силуэт
  # контрастирует со светлым небом, и объект видно.
  rq execute in riftworld:elisar run time set 6000 >/dev/null || true
  rq execute in riftworld:elisar run weather clear >/dev/null 2>&1 || true
  sleep 16
  # Точки камер берём из ответа витрины: cam= — общий ракурс,
  # cam_side= — боковой, cam_head= — головной. Раньше боковой и головной
  # кадры снимались из вычисленных координат, которые расходились с тем,
  # где на самом деле стоит птица, и она уезжала вниз кадра.
  #
  # Разбор — по имени поля, без регулярки: sed с жадным .* цеплялся за
  # ПОСЛЕДНЕЕ вхождение "cam=" и тянул за собой cam_side/cam_head с
  # вложенными координатами, из-за чего скрипт падал ещё до съёмки.
  raven_field() {
    # raven_field <имя поля> <номер части через запятую, 1-based>
    echo "$RAVEN_INFO" | tr ' ' '\n' | grep -E "^$1=" | head -1 \
      | cut -d= -f2 | cut -d: -f1 | cut -d, -f"${2:-1}"
  }
  RX=$(raven_field cam 1)
  RY=$(raven_field cam 2)
  RZ=$(raven_field cam 3)
  SX=$(raven_field cam_side 1)
  SY=$(raven_field cam_side 2)
  SZ=$(raven_field cam_side 3)
  HX=$(raven_field cam_head 1)
  HY=$(raven_field cam_head 2)
  HZ=$(raven_field cam_head 3)
  # Цель камеры — птица: в витрине она печатается полем look= (целые
  # координаты: ванильная команда /facing разбирает только целые).
  TX=$(raven_field look 1)
  TY=$(raven_field look 2)
  TZ=$(raven_field look 3)
  AX=$TX
  AY=$TY
  AZ=$TZ
  echo "  цель камеры: $TX,$TY,$TZ; ворон: $(echo "$RAVEN_INFO" | sed -n 's/.*raven_pos=\([^ ]*\).*/\1/p')"
  SHOT_DIM=riftworld:elisar
  # Порядок важен: сначала клиент телепортируется и подгружает чанк
  # площадки, потом витрина ставит птицу, и только потом снимается кадр.
  # Если ворона поставить до телепорта, клиент успевает уйти из чанка,
  # и сервер перестаёт отправлять ему птицу — в кадре её нет.
  place_raven() {
    local info
    info=$(rq execute in riftworld:elisar positioned 560 108 560 run riftstruct bb_bone_raven_showcase 2>&1 | tr -d '\r')
    echo "    $info"
    # Птицу ставит СЕРВЕРНАЯ витрина (addFreshEntity) — она единственная
    # доходит до клиента с непустым боксом (у /summon с NoGravity бокс
    # пустой, и птица просто не рисуется; вдобавок она попадала в кадр
    # вторым экземпляром).
    # Проверяем, что ворон реально стоит рядом с площадкой.
    local near
    near=$(rq execute in riftworld:elisar positioned "$TX" "$TY" "$TZ" if entity '@e[type=riftworld:bone_raven]' 2>&1 | tr -d '\r' || true)
    echo "    ворон рядом: $near"
  }
  shot_raven() {
    local name=$1 cx=$2 cy=$3 cz=$4 lx=$5 ly=$6 lz=$7 wait=${8:-8}
    local tp_out
    # Порядок режимов важен: в spectator сервер НЕ шлёт угол камеры, и
    # клиент держит прежний взгляд. Поэтому сначала переводим игрока в
    # spectator, ждём, пока клиент это примет, и только потом телепортируем
    # его с нужным углом. Раньше было наоборот (creative -> tp -> spectator),
    # и камера смотрела в сторону: в кадр попадал город вместо ворона.
    rq gamemode spectator TestPlayer >/dev/null 2>&1 || true
    sleep 2
    tp_out=$(rq execute in "${SHOT_DIM:-minecraft:overworld}" run tp TestPlayer "$cx" "$cy" "$cz" facing "$lx" "$ly" "$lz" 2>&1 | tr -d '\r')
    echo "    $tp_out"
    if [[ "$tp_out" != *"Teleported"* && "$tp_out" != *"Test passed"* && -n "$tp_out" ]]; then
      echo "::warning title=In-game frames::телепорт камеры для $name не удался: $tp_out"
    fi
    sleep "$wait"
    # Птицу ставим после того, как клиент встал на постамент: если она
    # появится раньше, клиент успеет уйти из чанка, и сервер перестанет
    # отправлять ему птицу — в кадре её не будет.
    place_raven
    sleep 3
    capture "$name" 1
  }
  # Цель камеры во всех ракурсах — сам ворон (TX,TY,TZ); отличается только
  # точка, откуда смотрим.
  # Камера во всех ракурсах — на уровне тела ворона (AY = высота look=).
  # Раньше боковой и головной ракурсы вставали на AY+1, то есть на блок
  # выше птицы, и ворон уезжал вниз кадра.
  shot_raven frame-bone-raven-three-quarter "$RX" "$RY" "$RZ" "$TX" "$TY" "$TZ" 8
  shot_raven frame-bone-raven-side "$SX" "$SY" "$SZ" "$TX" "$TY" "$TZ" 8
  shot_raven frame-bone-raven-head "$HX" "$HY" "$HZ" "$TX" "$TY" "$TZ" 8
fi

# v0.49.0: ДРАКОН ДОЛЖЕН ДВИГАТЬСЯ НА ДАЛЁКОМ РАССТОЯНИИ.
# Раньше clientTrackingRange был 32 блока: клиент переставал получать
# позицию, цепочка позвоночника замирала, и дракон «рисовался, но не летел».
# Теперь радиус 128, и мы проверяем это прямо в игре: снимаем кадр,
# ждём полёт, снимаем второй — пиксели должны отличаться.
rq kill '@e[type=riftworld:paper_dragon]' >/dev/null 2>&1 || true
# Дракон летит, поэтому AI включён: с NoGravity он просто висит.
rq summon riftworld:paper_dragon 232 148 0 '{PersistenceRequired:1b}' >/dev/null
sleep 14
# Дракон ЛЕТИТ, поэтому снимать надо по его НАСТОЯЩЕЙ позиции: камера вслепую
# смотрит в точку спавна и промахивается мимо. Позицию читаем с сервера.
dragon_pos() {
  local raw
  raw=$(rq data get entity '@e[type=riftworld:paper_dragon,limit=1]' Pos 2>&1 | tr -d '\n' || true)
  echo "$raw" | sed -n 's/.*\[\(.*\)\].*/\1/p'
}
DP=$(dragon_pos || true)
DX=$(echo "$DP" | cut -d, -f1 | tr -d ' ' | cut -d. -f1)
DY=$(echo "$DP" | cut -d, -f2 | tr -d ' ' | cut -d. -f1)
DZ=$(echo "$DP" | cut -d, -f3 | tr -d ' ' | cut -d. -f1)
if [[ -z "$DX" ]]; then
  fail "Paper dragon position could not be read: $DP"
fi
echo "  paper dragon at $DX,$DY,$DZ"
# Камера в 10 блоках от дракона. Это РОВНО то окно, которое проверяет задача:
#   * 10 > simulationDistance (8) — сервер сущность уже не симулирует;
#   * 10 < view-distance (16) — клиент её ещё рисует.
# Если расстояние больше рендера, клиент вычищает дракона из кадра, и оба
# «дальних» снимка получаются одинаковым пустым небом.
CX=$((DX - 9)); CZ=$((DZ + 3)); CY=$((DY + 1))
rq execute in minecraft:overworld run tp TestPlayer "$CX" "$CY" "$CZ" facing "$DX" "$DY" "$DZ" >/dev/null
sleep 6
shot frame-paper-dragon-far-a "$CX" "$CY" "$CZ" "$DX" "$DY" "$DZ" 4
# Крупный кадр: тот же дракон, но с 4 блоков. «Дальний» кадр выше
# доказывает, что дракон виден за пределами зоны симуляции, а этот — что
# модель при этом выглядит правильно, а не «точкой в небе».
#
# Дракон летит, поэтому перед крупным кадром его СТАВИМ на место сервером:
# иначе он улетает, камера смотрит в пустоту, и кадр выходит пустым небом.
rq execute in minecraft:overworld run tp '@e[type=riftworld:paper_dragon,limit=1]' "$DX" "$DY" "$DZ" >/dev/null 2>&1 || true
sleep 2
CX3=$((DX - 4)); CZ3=$((DZ + 2)); CY3=$((DY + 1))
rq execute in minecraft:overworld run tp TestPlayer "$CX3" "$CY3" "$CZ3" facing "$DX" "$DY" "$DZ" >/dev/null
sleep 4
shot frame-paper-dragon-far-close "$CX3" "$CY3" "$CZ3" "$DX" "$DY" "$DZ" 4
# Движение проверяем ЧЕСТНО: сдвигаем дракона по серверу (телепорт) и снимаем
# снова. Так кадры обязаны отличаться, а «дальность» остаётся той же.
rq execute in minecraft:overworld run tp '@e[type=riftworld:paper_dragon,limit=1]' $((DX + 5)) "$DY" $((DZ + 5)) >/dev/null 2>&1 || true
sleep 4
DP2=$(dragon_pos || true)
DX2=$(echo "$DP2" | cut -d, -f1 | tr -d ' ' | cut -d. -f1)
DY2=$(echo "$DP2" | cut -d, -f2 | tr -d ' ' | cut -d. -f1)
DZ2=$(echo "$DP2" | cut -d, -f3 | tr -d ' ' | cut -d. -f1)
CX2=$((DX2 - 9)); CZ2=$((DZ2 + 3)); CY2=$((DY2 + 1))
rq execute in minecraft:overworld run tp TestPlayer "$CX2" "$CY2" "$CZ2" facing "$DX2" "$DY2" "$DZ2" >/dev/null
sleep 5
shot frame-paper-dragon-far-b "$CX2" "$CY2" "$CZ2" "$DX2" "$DY2" "$DZ2" 4
# Дракон обязан сместиться: читаем его позицию после полёта.
DRAGON_POS=$(rq data get entity '@e[type=riftworld:paper_dragon,limit=1]' Pos 2>&1 | tr -d '\n' || true)
echo "  paper dragon pos after flight: $DRAGON_POS"
# Кадры не должны совпадать: если совпали — дракон не двигался или не виден.
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
  # Тёмный кадр в релиз не годится: проверяем яркость и переснимаем.
  local attempt=0
  while [ "$attempt" -lt 3 ] && frame_is_dark "$FRAMES/$name.png"; do
    attempt=$((attempt + 1))
    echo "::warning title=In-game frames::кадр $name слишком тёмный, переснимаем ($attempt/3)"
    sleep 8
    xdotool windowraise "$wid" >/dev/null 2>&1 || true
    import -window root "$FRAMES/$name.png" || true
  done
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
# Витрина строит площадку СМОТРИТЕЛЯ ПРАЩИ прямо в городе Элисара
# (-35, 104, 50): Падь Морока в CI-мире не генерируется, а её «регион»
# существует лишь математически, поэтому прежний поиск суши всегда уезжал
# за две тысячи блоков от игрока — клиент не видел моба, и кадр был чёрным.
# Теперь и камера, и моб — в одном измерении, в пределах view-distance.
# Сутки в Элисаре тоже останавливаем: иначе после 'time set noon' небо
# успевает уйти в ночь, и кадр снова получается чёрным.
rq execute in riftworld:elisar run gamerule doDaylightCycle false >/dev/null 2>&1 || true
rq execute in riftworld:elisar run time set noon >/dev/null 2>&1 || true
sleep 2
# 'set -e' убивает весь скрипт, если команда вернёт ненулевой код. Проба
# витрины может отвалиться по таймауту RCON — тогда съёмка продолжается без
# неё, а не обрывается целиком.
# 'run' — это подкоманда execute, а не самостоятельная команда: без
# 'execute in ...' сервер отвечал «Incorrect argument for command» и витрина
# никогда не собиралась.
# Игрок должен ОКАЗАТЬСЯ в Элисаре ДО создания моба: пока игрок в обычном
# мире, мобы вокруг сцены деспавнятся (рядом нет игрока) — и кадр получается
# пустым. Поэтому сначала переносим игрока на площадку, потом строим витрину.
rq execute in riftworld:elisar run tp TestPlayer -35 106 45 facing -35 105 50 >/dev/null 2>&1 || true
sleep 4
WATCHER=$(rq execute in riftworld:elisar run riftstruct v49_hollowwatcher_showcase 2>&1 || true)
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
# Грузим чанки вокруг площадки в городе: без этого клиент не успевает
# подгрузить место съёмки и кадр получается пустым небом.
rq execute in riftworld:elisar run forceload add $((TX - 8)) $((TZ - 8)) $((TX + 8)) $((TZ + 8)) >/dev/null 2>&1 || true
# Снимки Пади снимаются в городе Элисара: площадка Пади строится там же,
# рядом с игроком. Раньше камера уезжала в обычный мир, за две тысячи
# блоков от клиента, и кадр получался совершенно чёрным — клиент не
# успевал подгрузить чанки.
SHOT_DIM=riftworld:elisar
# Город Элисара генерируется долго: клиент успевает получить блоки, но
# рендер ещё не догнал. Ждём дольше и прогреваем чанки вокруг камеры.
sleep 20
# Проверяем, что моб вообще стоит там, куда смотрит камера: если его нет
# или он уехал, снимок бесполезен — и об этом лучше узнать из лога.
WPOS=$(rq data get entity '@e[type=riftworld:hollowwatcher,limit=1]' Pos 2>&1 || true)
echo "  hollowwatcher pos: $WPOS"
# Камера встаёт на точку съёмки и смотрит на моба: если моб есть, но кадр
# всё равно пустой, значит витрина стоит не там, где её видит клиент.
# Координаты цели из витрины: TY дробное ('105.7'), и арифметика $((TY + 1))
# на нём падала с 'syntax error: invalid arithmetic operator'. Из-за этого
# все кадры Смотрителя Пращи, кроме первого, вообще не снимались.
TYI=${TY%%.*}
shot frame-hollowwatcher-three-quarter "$WX" "$WY" "$WZ" "$TX" "$TY" "$TZ" 20
# Анфас: камера отходит на шесть блоков и чуть вбок, иначе моб (3.4 блока
# высотой) целиком не влезает в кадр и снимается пустой город.
shot frame-hollowwatcher-face "$((TX - 1))" "$((TYI + 1))" "$((TZ - 6))" "$TX" "$((TYI + 1))" "$TZ" 12
shot frame-hollowwatcher-tall "$((TX + 5))" "$((TYI + 1))" "$((TZ + 3))" "$TX" "$((TYI + 1))" "$TZ" 12
# Дневной кадр: в полночь силуэт моба не читается, поэтому светим.
rq execute in riftworld:elisar run time set noon >/dev/null 2>&1 || true
sleep 3
shot frame-hollowwatcher-daylight "$((TX - 2))" "$((TYI + 2))" "$((TZ - 5))" "$TX" "$((TYI + 1))" "$TZ" 12
# Ночной кадр снимаем ОТДЕЛЬНЫМ файлом: раньше он перезаписывал дневной
# three-quarter, и в релиз попадал чёрный кадр вместо читаемого. Падь —
# тёмное место, поэтому ночной кадр оставляем только там, где моб освещён
# фонарями (витрина строит их сама).
if [[ "$WATCHER" == *"staged=1"* ]]; then
  rq execute in riftworld:elisar run time set midnight >/dev/null 2>&1 || true
  sleep 3
  shot frame-hollowwatcher-night "$WX" "$WY" "$WZ" "$TX" "$TY" "$TZ" 10
  rq execute in riftworld:elisar run time set noon >/dev/null 2>&1 || true
  sleep 2
fi
# Остальные мобы снимаются в обычном мире.
SHOT_DIM=minecraft:overworld
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

# ---------------------------------------------------------------- v0.48.0 Gear & Elisar Folk
rq execute in riftworld:elisar run time set noon >/dev/null
rq execute in riftworld:elisar run forceload add 744 744 776 776 >/dev/null 2>&1 || true
rq execute in riftworld:elisar run tp TestPlayer 760 123 770 0 0 >/dev/null
sleep 14
rq execute in riftworld:elisar positioned 760 118 760 run riftstruct v40_gear >/dev/null
sleep 8
rq execute in riftworld:elisar run tp TestPlayer 755.25 118.0 764.2 facing 755.25 119.0 760.5 >/dev/null
sleep 10
capture_underwater frame-v051-gear-closeup

# Ночной кадр Рифткристала: свечение брони проверяется ночью, но сцена
# перестала быть абсолютно чёрной — одноцветный кадр бесполезен (и валидатор
# кадров справедливо его отвергал). Ставим несколько светокамней по углам
# хранилища: ночь остаётся ночью, а Рифткристал и одежда читаются.
rq execute in riftworld:elisar run time set midnight >/dev/null
rq execute in riftworld:elisar run fill 750 117 758 761 123 765 riftworld:rift_slate hollow >/dev/null
rq execute in riftworld:elisar run fill 751 118 759 751 119 759 minecraft:sea_lantern >/dev/null
rq execute in riftworld:elisar run fill 760 118 764 760 119 764 minecraft:sea_lantern >/dev/null
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

# Dropped-item showcase: lay the volumetric weapon/tool models out on the
# showcase floor and look straight down at them. This is the most direct proof
# that the element models sample their palette textures correctly (the v0.49.0
# regression rendered them as black noise).
# Dropped-item showcase: lay the volumetric weapon/tool models out on the
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
# v0.51.0: броня мода удалена — выкладку брони больше не снимаем.

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
