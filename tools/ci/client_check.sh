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

# ---------------------------------------------------------------- запуск клиента
echo "== старт клиента через Gradle (runClient) =="
(
  cd mod
  chmod +x gradlew
  ./gradlew runClient --no-daemon --console=plain \
    -PrunDir="$PWD/../$RUN" \
    -PautoJoin=127.0.0.1:25565 2>&1
) > "$OUT/client.log" 2>&1 &
CPID=$!

for i in $(seq 1 160); do
  grep -qi "Connecting to 127.0.0.1" "$OUT/client.log" && break
  grep -qiE "A fatal error|Exception in thread \"main\"" "$OUT/client.log" && break
  sleep 3
done
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
echo "  reef=$RX,$RZ floor=$RF | deep=$DX,$DZ floor=$DF | hole=$HX,$HZ floor=$HF | cave=$CX,$CZ $CLOW..$CHIGH | column=$KX,$KZ floor=$KF"

echo "== телепорт в Элисар и прогрузка области =="
EL time set noon >/dev/null 2>&1 || true        # в Элисаре всегда полдень (ElisarDaylight), но фиксируем сразу
EL tp TestPlayer "$RX" "$((RF + 6))" "$RZ" 90 40 | tail -1
EL forceload add "$((RX - 80))" "$((RZ - 80))" "$((RX + 80))" "$((RZ + 80))" | tail -1
sleep 60

# ---------------------------------------------------------------- пробы (реальная генерация)
FAILS=0
PROBES="$OUT/probes.txt"
: > "$PROBES"
probe() { # probe <x> <y> <z> <block> <label>
  local ans
  ans=$(ELP "$1" "$2" "$3" execute if block ~ ~ ~ "riftworld:$4")
  if [[ "$ans" == *"Test passed"* ]]; then echo "  OK   $5 ($1,$2,$3) = $4" | tee -a "$PROBES"
  else echo "  FAIL $5 ($1,$2,$3) != $4" | tee -a "$PROBES"; FAILS=$((FAILS + 1)); fi
}
echo "== пробы моря =="
probe "$KX" "$((KT - 3))" "$KZ" elisar_water "вода на глубине"
probe "$KX" "$KF" "$KZ" elisar_seabed "донный песок"
probe "$RX" "$((RF + 1))" "$RZ" elisar_water "вода над рифом"

echo "== профиль колонки $KX,y,$KZ =="
PROFILE="$OUT/column.txt"
: > "$PROFILE"
for y in $(seq 0 66); do
  w="."
  [[ "$(ELP "$KX" "$y" "$KZ" execute if block ~ ~ ~ riftworld:elisar_water)" == *"Test passed"* ]] && w="W"
  [[ "$(ELP "$KX" "$y" "$KZ" execute if block ~ ~ ~ riftworld:elisar_seabed)" == *"Test passed"* ]] && w="S"
  [[ "$(ELP "$KX" "$y" "$KZ" execute if block ~ ~ ~ minecraft:air)" == *"Test passed"* ]] && w="a"
  printf '%s' "$w" >> "$PROFILE"
done
echo >> "$PROFILE"
WATER=$(tr -cd 'W' < "$PROFILE" | wc -c)
echo "  воды в колонке: $WATER блоков"
[ "$WATER" -ge 20 ] || { echo "  FAIL: море не сгенерировалось"; FAILS=$((FAILS + 1)); }

echo "== рыбы: естественный спавн смотрителем =="
sleep 25
FISH=$(EL if entity @e[type=riftworld:sea_fish,distance=..80] 2>&1)
if [[ "$FISH" == *"Test passed"* ]]; then echo "  OK   рыбы заспавнились сами (SeaLifeKeeper)" | tee -a "$PROBES"
else echo "  FAIL рыб нет рядом с игроком" | tee -a "$PROBES"; FAILS=$((FAILS + 1)); fi
EL execute run kill @e[type=riftworld:sea_fish] >/dev/null 2>&1 || true

echo "== рыбы: витрина всех видов (summon с NBT) =="
SUM() { EL summon riftworld:sea_fish "$1" "$2" "$3" "$4" 2>&1 | tail -1; }
# витрина в открытой воде на рифе: 6 видов перед камерой
SUM "$VX" "$((VF + 5))" "$VZ" '{sf_variant:0}' >/dev/null
SUM "$((VX + 2))" "$((VF + 5))" "$((VZ - 1))" '{sf_variant:0}' >/dev/null
SUM "$((VX + 3))" "$((VF + 4))" "$((VZ - 2))" '{sf_variant:1}' >/dev/null
SUM "$((VX - 2))" "$((VF + 4))" "$((VZ - 2))" '{sf_variant:2}' >/dev/null
SUM "$((VX + 1))" "$((VF + 6))" "$((VZ - 4))" '{sf_variant:3}' >/dev/null
SUM "$((VX - 3))" "$((VF + 6))" "$((VZ - 5))" '{sf_variant:4}' >/dev/null
# хищники — в пещере
SUM "$CX" "$((CLOW + 2))" "$((CZ - 3))" '{sf_variant:5}' >/dev/null
SUM "$((CX + 2))" "$((CLOW + 3))" "$((CZ - 2))" '{sf_variant:6}' >/dev/null
sleep 6
CNT=$(EL if entity @e[type=riftworld:sea_fish] 2>&1)
[[ "$CNT" == *"Test passed"* ]] && echo "  OK   витрина рыб создана" | tee -a "$PROBES" || { echo "  FAIL не удалось создать рыб" | tee -a "$PROBES"; FAILS=$((FAILS + 1)); }

# ---------------------------------------------------------------- скриншоты
echo "== скриншоты =="
key F1                       # убрать HUD — «красивые» кадры
sleep 2
view() { # view <x> <y> <z> <yaw> <pitch> <pause> <name>
  EL tp TestPlayer "$1" "$2" "$3" "$4" "$5" >/dev/null 2>&1
  sleep "$6"
  shot "$7"
}
view "$VX" "$((VF + 7))" "$VZ" 90 30 16 "02_reef_vista"           # рифовый сад: панорама с воды
view "$VX" "$((VF + 3))" "$VZ" 200 12 14 "03_reef_closeup"          # риф вплотную
view "$((LX + 6))" "$((LF + 6))" "$((LZ + 6))" 225 15 14 "04_kelp_forest"   # ламинария
view "$VX" 59 "$VZ" 90 -28 14 "05_looking_up"                       # из-под воды на поверхность
view "$DX" "$((DF + 3))" "$DZ" 30 0 14 "06_trench"                  # ущелье
view "$HX" "$((HF + 5))" "$HZ" 90 60 14 "07_blue_hole"              # синяя дыра сверху
view "$CX" "$((CLOW + 3))" "$CZ" 0 0 16 "08_cave"                   # подводная пещера
view "$CX" "$((CLOW + 3))" "$CZ" 180 0 12 "09_cave_back"            # пещера, другой ракурс
view "$VX" "$((VF + 5))" "$((VZ + 6))" 0 0 10 "10_fish_reef"        # витрина мирных рыб
view "$CX" "$((CLOW + 3))" "$((CZ + 5))" 0 0 10 "11_fish_cave"      # хищники пещеры (удильщик/угорь)
key F1
sleep 1

# ---------------------------------------------------------------- итог
echo "== итог =="
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
