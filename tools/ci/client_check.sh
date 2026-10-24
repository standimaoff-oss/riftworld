#!/usr/bin/env bash
# Рев 8: проверка «как в жизни» — поднимаем НАСТОЯЩИЙ клиент Minecraft (ForgeGradle runClient)
# в виртуальном дисплее, заходим в мир, уходим в измерение Элисара и снимаем экран.
#
# Почему клиент: dedicated-сервер не генерирует измерение, в котором нет игрока
# (проверено: чанки Элисара остаются воздухом, сущности в них не спавнятся).
#
# usage: tools/ci/client_check.sh     (из корня репозитория, JDK 17 в PATH)
set -uo pipefail
OUT=${OUT:-ci-shots}
RUN=${RUN:-ci/runs}
DISP=${DISP:-:99}
RCON_PW=${RCON_PW:-riftci}
SPOTS=tools/ci/spots.json
mkdir -p "$OUT" "$RUN/client"

# ---------------------------------------------------------------- зависимости
sudo apt-get update -qq >/dev/null 2>&1 || true
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq xvfb x11-utils xdotool imagemagick libgl1 libglx-mesa0 \
  libxi6 libxxf86vm1 libxfixes3 libxrender1 libxkbcommon0 libsm6 libopenal1 >/dev/null 2>&1 || true
command -v Xvfb >/dev/null || { echo "FATAL: нет Xvfb"; exit 1; }
command -v import >/dev/null || { echo "FATAL: нет imagemagick"; exit 1; }

# ---------------------------------------------------------------- настройки клиента
python3 - <<'PY'
import os
run = os.environ.get("RUN", "ci/runs")
opts = {
    "tutorialStep": "none", "onboardAccessibility": "false", "skipMultiplayerWarning": "true",
    "joinedFirstServer": "true", "renderDistance": "8", "simulationDistance": "6", "maxFps": "30",
    "guiScale": "2", "fov": "0.0", "graphicsMode": "1", "renderClouds": "false",
    "pauseOnLostFocus": "false", "narrator": "0", "soundCategory_master": "0.0",
    "fullscreen": "false", "showSubtitles": "false", "autoJump": "false",
    "darkMojangStudiosBackground": "true", "skipRealms32bitWarning": "true",
    "gamma": "1.0", "hideGui": "false", "bobView": "false",
}
os.makedirs(run + "/client", exist_ok=True)
with open(run + "/client/options.txt", "w", encoding="utf-8") as f:
    for k, v in opts.items():
        f.write(f"{k}:{v}\n")
print("client options ->", run + "/client/options.txt")
PY

# ---------------------------------------------------------------- дисплей
if ! pgrep -f "Xvfb $DISP" >/dev/null; then
  Xvfb "$DISP" -screen 0 1280x800x24 -nolisten tcp >/tmp/xvfb.log 2>&1 &
  sleep 3
fi
export DISPLAY=$DISP LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe MESA_GL_VERSION_OVERRIDE=3.3

WID=""
geom() {  # найти окно игры и его геометрию (x y w h)
  WID=$(xdotool search --onlyvisible --name "Minecraft" 2>/dev/null | head -1)
  [ -z "$WID" ] && WID=$(xdotool search --onlyvisible --class "Minecraft" 2>/dev/null | head -1)
  [ -z "$WID" ] && WID=$(xdotool search --onlyvisible --classname ".*" 2>/dev/null | head -1)
  [ -n "$WID" ] || return 1
  eval "$(xdotool getwindowgeometry --shell "$WID")"    # X Y WIDTH HEIGHT
  return 0
}

shot() {  # shot <имя>
  if geom; then
    import -window root /tmp/full.png 2>/dev/null || return 1
    convert /tmp/full.png -crop "${WIDTH}x${HEIGHT}+${X}+${Y}" +repage -quality 90 "$OUT/$1.jpg" 2>/dev/null \
      || cp /tmp/full.png "$OUT/$1.png"
  else
    import -window root -quality 90 "$OUT/$1.jpg" 2>/dev/null || return 1
  fi
  echo "  shot: $OUT/$1.jpg"
}

key() { # key <F1>
  if geom; then xdotool windowactivate --sync "$WID" 2>/dev/null || true; xdotool key --window "$WID" "$1" 2>/dev/null || xdotool key "$1"; fi
}

RCON() { python3 tools/rcon.py --password "$RCON_PW" --log "" --timeout 120 "$@" 2>&1; }
if ! RCON list >/dev/null 2>&1; then
  echo "FATAL: сервер не отвечает по RCON — клиенту некуда заходить (см. отчёт шага сервера)"
  exit 1
fi
EL() { RCON execute in riftworld:elisar run "$@" 2>&1; }
ELP() { local x=$1 y=$2 z=$3; shift 3; RCON execute in riftworld:elisar positioned "$x" "$y" "$z" run "$@" 2>&1; }

# ---------------------------------------------------------------- дисплей должен быть готов
export DISPLAY=$DISP LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe MESA_GL_VERSION_OVERRIDE=3.3
for i in $(seq 1 30); do
  xdpyinfo -display "$DISP" >/dev/null 2>&1 && break
  sleep 1
done
xdpyinfo -display "$DISP" >/dev/null 2>&1 || { echo "FATAL: Xvfb не поднялся"; exit 1; }
echo "  дисплей $DISP готов"

# ---------------------------------------------------------------- запуск клиента (с одним повтором)
start_client() {
  (
    cd mod
    chmod +x gradlew
    ./gradlew runClient --no-daemon --console=plain \
      -PrunDir="$PWD/../$RUN" \
      -PautoJoin=127.0.0.1:25565 2>&1
  ) > "$OUT/client.log" 2>&1 &
  CPID=$!
}

wait_connect() {   # 0 — клиент подключился, 1 — умер/не дождались
  for i in $(seq 1 160); do
    grep -qi "Connecting to 127.0.0.1" "$OUT/client.log" && return 0
    grep -qE "We died but didn't somehow|A fatal error|Exception in thread \"main\"" "$OUT/client.log" && return 1
    kill -0 "$CPID" 2>/dev/null || return 1
    sleep 3
  done
  return 1
}

echo "== старт клиента через Gradle (runClient) =="
start_client
if ! wait_connect; then
  echo "  клиент не поднялся — повторный запуск (капризы early-display под Xvfb)"
  pkill -f forgeclientuserdev 2>/dev/null || true
  sleep 8
  mv "$OUT/client.log" "$OUT/client_attempt1.log"
  start_client
  wait_connect || { echo "FATAL: клиент не поднялся со второй попытки"; tail -30 "$OUT/client.log"; exit 1; }
fi
sleep 25
shot "01_boot"

echo "== ждём игрока на сервере =="
PLAYER=0
for i in $(seq 1 60); do
  ans=$(RCON list 2>/dev/null || true)
  case "$ans" in *TestPlayer*) PLAYER=1; echo "  игрок в игре: $ans"; break ;; esac
  sleep 5
done
[ "$PLAYER" = 1 ] || { echo "FATAL: клиент не зашёл в мир"; tail -40 "$OUT/client.log"; exit 1; }

RCON gamerule doDaylightCycle false >/dev/null 2>&1 || true
RCON gamerule doWeatherCycle false >/dev/null 2>&1 || true
RCON weather clear >/dev/null 2>&1 || true
RCON gamemode creative TestPlayer >/dev/null 2>&1 || true

# ---------------------------------------------------------------- координаты сцен
read -r RX RZ RF < <(python3 -c "import json;d=json.load(open('$SPOTS'));print(d['reef']['x'],d['reef']['z'],d['reef']['floor'])")
read -r DX DZ DF < <(python3 -c "import json;d=json.load(open('$SPOTS'));print(d['deep']['x'],d['deep']['z'],d['deep']['floor'])")
read -r HX HZ HF < <(python3 -c "import json;d=json.load(open('$SPOTS'));print(d['hole']['x'],d['hole']['z'],d['hole']['floor'])")
read -r CX CZ CLOW CHIGH < <(python3 -c "import json;d=json.load(open('$SPOTS'));print(d['cave']['x'],d['cave']['z'],d['cave']['low'],d['cave']['high'])")
read -r KX KZ KF KB KT < <(python3 -c "import json;d=json.load(open('$SPOTS'));c=d['column'];print(c['x'],c['z'],c['floor'],c['water_bottom'],c['water_top'])")
read -r VX VZ VF < <(python3 -c "import json;d=json.load(open('$SPOTS'));print(d['reef_vista']['x'],d['reef_vista']['z'],d['reef_vista']['floor'])")
read -r LX LZ LF < <(python3 -c "import json;d=json.load(open('$SPOTS'));print(d['kelp']['x'],d['kelp']['z'],d['kelp']['floor'])")
FY=$((RF + 22))          # «витринный» горизонт над рифом: толща воды, хорошо освещена
echo "  reef=$RX,$RZ floor=$RF | deep=$DX,$DZ floor=$DF | hole=$HX,$HZ floor=$HF | cave=$CX,$CZ $CLOW..$CHIGH | column=$KX,$KZ floor=$KF"

# ---------------------------------------------------------------- пятна биомов (rev 9)
# координаты берём у самого мода (/riftbiomes), а не хардкодим — камеры всегда попадают в биом
BIOMES_RAW=$(RCON riftbiomes 2>&1 | tr -d '\r' | tr '\n' ' ')
echo "  /riftbiomes -> $BIOMES_RAW"
read -r VHX VHZ VHY TMX TMZ TMY OCEAN < <(python3 - "$BIOMES_RAW" <<'PYEOF'
import re, sys
s = sys.argv[1] if len(sys.argv) > 1 else ""
def spot(tag):
    m = re.search(tag + r"=(-?\d+),(-?\d+),(-?\d+)", s)
    return m.groups() if m else ("0", "0", "0")
v, t = spot("violet"), spot("terraced")
o = re.search(r"ocean=(-?\d+)", s)
print(v[0], v[1], v[2], t[0], t[1], t[2], o.group(1) if o else "45")
PYEOF
)
echo "  violet=$VHX,$VHZ top=$VHY | terraced=$TMX,$TMZ top=$TMY | ocean=$OCEAN"
printf '%s\n' "violet $VHX $VHY $VHZ" "terraced $TMX $TMY $TMZ" > "$OUT/biome_spots.txt"

echo "== телепорт в Элисар и прогрузка области =="
EL time set noon >/dev/null 2>&1 || true        # в Элисаре всегда полдень (ElisarDaylight), но фиксируем сразу
EL tp TestPlayer "$RX" "$((RF + 6))" "$RZ" 90 40 | tail -1
EL forceload add "$((RX - 80))" "$((RZ - 80))" "$((RX + 100))" "$((RZ + 140))" | tail -1
EL forceload add "$((HX - 40))" "$((HZ - 40))" "$((HX + 40))" "$((HZ + 40))" | tail -1
EL forceload add "$((CX - 40))" "$((CZ - 40))" "$((CX + 40))" "$((CZ + 40))" | tail -1
EL forceload add "$((VHX - 200))" "$((VHZ - 200))" "$((VHX + 200))" "$((VHZ + 200))" | tail -1
EL forceload add "$((TMX - 220))" "$((TMZ - 220))" "$((TMX + 220))" "$((TMZ + 220))" | tail -1
sleep 60

# ---------------------------------------------------------------- пробы (реальная генерация)
FAILS=${FAILS:-0}
WATER=${WATER:-0}
PROBES="$OUT/probes.txt"
write_report() {
  {
    echo "# CI, шаг 2: настоящий клиент Minecraft в Элисаре"
    echo
    echo "- клиент: ForgeGradle runClient под Xvfb (llvmpipe, без GPU)"
    echo "- воды в колонке (${KX:-?},y,${KZ:-?}): **$WATER** блоков"
    echo "- провалов проб: **$FAILS**"
    echo "- строк Exception/ERROR в логе клиента (без звука/наратора): **$(grep -E "Exception|ERROR" "$OUT/client.log" 2>/dev/null | grep -viE "Narrator|flite|text2speech|OpenAL|SoundSystem|SoundEngine" | wc -l)**"
    echo
    echo "## пробы"; echo '```'; cat "$PROBES" 2>/dev/null; echo '```'
    echo "## колонка (y; W=вода, S=донный песок, a=воздух/пещера, .=прочее)"; echo '```'; cat "$OUT/column.txt" 2>/dev/null; echo '```'
    echo "## скриншоты"; ls -1 "$OUT"/*.jpg 2>/dev/null | sed 's/^/- /'
  } > "$OUT/report_client.md"
}
trap write_report EXIT
: > "$PROBES"
log() { echo "$*" | tee -a "$PROBES"; }

# проба «под игроком»: телепортируем игрока в точку и смотрим блок прямо под ним
at_block() { # at_block <x> <y> <z> <block> <label>
  local ans
  EL tp TestPlayer "${1:-0}" "${2:-0}" "${3:-0}" 0 0 >/dev/null 2>&1
  sleep 2
  ans=$(EL execute at TestPlayer run execute if block \~ \~ \~ "riftworld:$4" 2>&1)
  if [[ "$ans" == *"Test passed"* ]]; then log "  OK   $5 ($1,$2,$3) = $4"
  else log "  FAIL $5 ($1,$2,$3) != $4 | ответ: ${ans:0:120}"; FAILS=$((FAILS + 1)); fi
}

echo "== пробы моря (под игроком) =="
at_block "$KX" "$((KT - 6))" "$KZ" elisar_water "вода на глубине"
at_block "$KX" "$KF" "$KZ" elisar_seabed "донный песок"
# на рифе растёт сад: проверяем цветок/коралл прямо на дне, а воду — на два блока выше
garden_probe() { # garden_probe <x> <z> <floor> <label>: ищем любое морское растение рядом с точкой
  local x=$1 z=$2 f=$3 label=$4 found=""
  for d in 0 1 -1 2 -2 3 -3 4 -4; do
    for e in 0 1 -1 2 -2 3 -3; do
      for b in sea_grass coral_branch coral_fan coral_tube coral_glow coral_brain sea_kelp sea_kelp_top sea_shells; do
        EL tp TestPlayer "$((x + d))" "$((f + 1))" "$((z + e))" 0 0 >/dev/null 2>&1
        if [[ "$(EL execute at TestPlayer run execute if block \~ \~ \~ "riftworld:$b" 2>&1)" == *"Test passed"* ]]; then
          found="$b at $((x + d)),$((f + 1)),$((z + e))"; break 3
        fi
      done
    done
  done
  if [ -n "$found" ]; then log "  OK   $label: $found"
  else log "  FAIL $label: рифовый сад не найден рядом с ($x,$z)"; FAILS=$((FAILS + 1)); fi
}
garden_probe "$RX" "$RZ" "$RF" "рифовый сад"
at_block "$RX" "$((RF + 4))" "$RZ" elisar_water "вода над рифом"

echo "== профиль колонки $KX,y,$KZ =="
PROFILE="$OUT/column.txt"
: > "$PROFILE"
for y in $(seq 2 66); do
  EL tp TestPlayer "$KX" "$y" "$KZ" 0 0 >/dev/null 2>&1
  w="."
  [[ "$(EL execute at TestPlayer run execute if block \~ \~ \~ riftworld:elisar_water 2>&1)" == *"Test passed"* ]] && w="W"
  [[ "$(EL execute at TestPlayer run execute if block \~ \~ \~ riftworld:elisar_seabed 2>&1)" == *"Test passed"* ]] && w="S"
  [[ "$(EL execute at TestPlayer run execute if block \~ \~ \~ minecraft:water 2>&1)" == *"Test passed"* ]] && w="v"
  printf '%s' "$w" >> "$PROFILE"
done
echo >> "$PROFILE"
WATER=$(tr -cd 'W' < "$PROFILE" | wc -c)
log "  воды в колонке: $WATER блоков"
[ "$WATER" -ge 20 ] || { log "  FAIL: море не сгенерировалось"; FAILS=$((FAILS + 1)); }

# ---------------------------------------------------------------- рыбы
echo "== рыбы: естественный спавн смотрителем =="
EL tp TestPlayer "$RX" "$((RF + 6))" "$RZ" 90 20 >/dev/null 2>&1
sleep 45
FISH=$(EL execute at TestPlayer run execute if entity @e[type=riftworld:sea_fish,distance=..64] 2>&1)
if [[ "$FISH" == *"Test passed"* ]]; then log "  OK   рыбы заспавнились сами (SeaLifeKeeper)"
else log "  FAIL рыб нет рядом с игроком | ${FISH:0:100}"; FAILS=$((FAILS + 1)); fi

echo "== рыбы: витрина всех видов (summon с NBT) =="
# summon_local <вперёд> <вбок> <вверх> <NBT>: summoned строго перед игроком (локальные координаты)
SUM_LOCAL() {
  local out
  out=$(EL execute at TestPlayer rotated as TestPlayer run summon riftworld:sea_fish ^"$2" ^"$3" ^"$1" "$4" 2>&1)
  log "  summon ^$2 ^$3 ^$1 $4 -> ${out:0:80}"
}

# камера-витрина: игрок в толще воды над рифом, смотрит на север
EL tp TestPlayer "$KX" 46 "$((KZ + 6))" 180 6 >/dev/null 2>&1
sleep 3
SUM_LOCAL 4 -2 0 '{NoAI:1b,Silent:1b,sf_variant:0}'
SUM_LOCAL 5 -0.6 0.6 '{NoAI:1b,Silent:1b,sf_variant:0}'
SUM_LOCAL 6 0.8 -0.6 '{NoAI:1b,Silent:1b,sf_variant:1}'
SUM_LOCAL 7 2.2 0.2 '{NoAI:1b,Silent:1b,sf_variant:2}'
SUM_LOCAL 8 -3.2 1.2 '{NoAI:1b,Silent:1b,sf_variant:3}'
SUM_LOCAL 9 3.4 1.6 '{NoAI:1b,Silent:1b,sf_variant:4}'
# хищники — в пещере, тоже перед камерой
EL tp TestPlayer "$CX" "$((CLOW + 3))" "$CZ" 0 0 >/dev/null 2>&1
sleep 3
SUM_LOCAL 2.6 0 0 '{NoAI:1b,Silent:1b,sf_variant:5}'
SUM_LOCAL 3 1.1 0.2 '{NoAI:1b,Silent:1b,sf_variant:6}'
SUM_LOCAL 2.4 -1.2 0.1 '{NoAI:1b,Silent:1b,sf_variant:5}'
sleep 8
CNT=$(EL execute at TestPlayer run execute if entity @e[type=riftworld:sea_fish,distance=..96] 2>&1)
if [[ "$CNT" == *"Test passed"* ]]; then log "  OK   рыбы в мире есть и тикают"
else log "  FAIL рыб в мире нет"; FAILS=$((FAILS + 1)); fi

# ---------------------------------------------------------------- скриншоты
echo "== скриншоты =="
key F1                       # убрать HUD — «красивые» кадры
RCON effect give TestPlayer minecraft:night_vision 600 0 true >/dev/null 2>&1 || true
sleep 2
view() { # view <x> <y> <z> <yaw> <pitch> <pause> <name>
  EL tp TestPlayer "$1" "$2" "$3" "$4" "$5" >/dev/null 2>&1
  sleep "$6"
  shot "$7"
}
view "$VX" 66 "$VZ" 90 42 14 "02_reef_from_above"        # рифовый сад сверху сквозь воду
view "$((VX + 10))" "$((VF + 6))" "$VZ" 270 18 14 "03_reef_underwater"   # риф вплотную под водой
view "$((LX + 15))" "$((LF + 11))" "$((LZ + 15))" 225 14 14 "04_kelp_forest" # ламинария
view "$VX" 64 "$((VZ + 26))" 180 2 14 "05_open_sea"          # поверхность: открытое море
view "$DX" 65 "$DZ" 30 55 14 "06_trench"                     # каньон сверху
view "$HX" "$((HF + 14))" "$HZ" 0 58 14 "07_blue_hole"             # синяя дыра: взгляд в колодец сверху
view "$CX" "$((CLOW + 3))" "$CZ" 0 0 16 "08_cave"            # подводная пещера
view "$CX" "$((CLOW + 3))" "$CZ" 180 0 12 "09_cave_back"     # пещера, другой ракурс
# витрина: камера над коралловым садом, рыбы в 3-4 блоках по курсу
EL tp TestPlayer "$((VX + 10))" "$((VF + 6))" "$VZ" 270 8 >/dev/null 2>&1
sleep 2
SUM_LOCAL 3 0 0 '{NoAI:1b,Silent:1b,sf_variant:0}'
SUM_LOCAL 3.4 0.8 0.7 '{NoAI:1b,Silent:1b,sf_variant:1}'
SUM_LOCAL 4.2 -0.9 -0.3 '{NoAI:1b,Silent:1b,sf_variant:2}'
SUM_LOCAL 4.8 0.4 0.9 '{NoAI:1b,Silent:1b,sf_variant:3}'
sleep 6
shot "10_fish_reef"
# крупный план: рыба в трёх блоках прямо по центру кадра — видно даже мелкие детали рендера
EL tp TestPlayer "$KX" 44 "$KZ" 180 0 >/dev/null 2>&1
sleep 2
SUM_LOCAL 3 0 0 '{NoAI:1b,Silent:1b,sf_variant:0}'
SUM_LOCAL 3.6 0.7 0.9 '{NoAI:1b,Silent:1b,sf_variant:1}'
SUM_LOCAL 4.4 -0.9 -0.5 '{NoAI:1b,Silent:1b,sf_variant:4}'
sleep 6
shot "12_fish_closeup"
view "$KX" 22 "$KZ" 180 65 14 "13_reef_floor"                  # дно рифа вплотную: разбор блоков
view "$CX" "$((CLOW + 3))" "$CZ" 0 0 12 "11_fish_cave"    # хищники пещеры
# --- rev 9: биом «Фиолетовые холмы» (новый рельеф) и «Многоуровневые горы»
put() { EL spreadplayers "$1" "$2" 1 6 false TestPlayer >/dev/null 2>&1; }   # поставить на поверхность
ROT() { RCON execute in riftworld:elisar at TestPlayer run tp TestPlayer ~ ~ ~ "$1" "$2" >/dev/null 2>&1; }   # только поворот
view "$VHX" "$((VHY + 46))" "$((VHZ - 235))" 0 16 24 "14_violet_hills_air"      # холмы с воздуха, силуэт на фоне неба
put "$VHX" "$VHZ"
ROT 0 8
sleep 10
shot "15_violet_hills_ground"
put "$((VHX + 55))" "$((VHZ + 40))"
ROT 215 12
sleep 10
shot "16_violet_hills_fields"
view "$((TMX + 40))" "$((TMY + 75))" "$((TMZ - 300))" 350 22 24 "17_terraced_air" # массив целиком
view "$((TMX + 175))" "$((TMY - 6))" "$((TMZ + 175))" 45 12 20 "18_terraced_side"     # ярусы вблизи
view "$((TMX - 170))" "$((TMY + 8))" "$((TMZ - 170))" 315 10 20 "19_terraced_shelf"    # массив в профиль: ярусы-полки
RCON effect clear TestPlayer >/dev/null 2>&1 || true
key F1
sleep 1

# ---------------------------------------------------------------- итог
echo "== диагностика положения =="
log "  игрок: $(EL data get entity TestPlayer Pos 2>&1 | head -1)"
log "  рыбы:  $(EL data get entity @e[type=riftworld:sea_fish,limit=1] Pos 2>&1 | head -1)"

echo "== итог ==="
RCON list || true
pkill -f "GradleWrapperMain runClient" 2>/dev/null || true
pkill -f forgeclientuserdev 2>/dev/null || true
sleep 5
kill $CPID 2>/dev/null || true

# шум окружения (нет звуковой карты / нет flite для наратора) в ошибки не считаем
ERRS=$(grep -E "Exception|ERROR" "$OUT/client.log" 2>/dev/null \
       | grep -viE "Narrator|flite|text2speech|OpenAL|SoundSystem|SoundEngine|no sound|libflite" | wc -l)
echo "строк Exception/ERROR в логе клиента (без звука/наратора): $ERRS"

{
  echo "# CI, шаг 2: настоящий клиент Minecraft в Элисаре"
  echo
  echo "- клиент: ForgeGradle runClient под Xvfb (llvmpipe, без GPU)"
  echo "- воды в колонке ($KX,y,$KZ): **$WATER** блоков"
  echo "- провалов проб: **$FAILS**"
  echo "- строк Exception/ERROR в логе клиента (без звука/наратора): **$ERRS**"
  echo
  echo "## пробы"; echo '```'; cat "$PROBES"; echo '```'
  echo "## колонка (y=0..66; W=вода, S=донный песок, a=воздух/пещера, .=прочее)"; echo '```'; cat "$PROFILE"; echo '```'
  echo "## скриншоты"; ls -1 "$OUT"/*.jpg 2>/dev/null | sed 's/^/- /'
} > "$OUT/report_client.md"
cat "$OUT/report_client.md"

python3 tools/rcon.py --password "$RCON_PW" --log "" stop >/dev/null 2>&1 || true
[ "$FAILS" -eq 0 ] && [ "$ERRS" -eq 0 ]
