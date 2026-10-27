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
# Рев 12: если выделенный сервер упал, его лог — единственный источник причины падения.
# Раньше в артефактах был только снимок лога из шага 1, и «Connection Lost» приходилось гадать.
keep_server_logs() {
  local srv=${SRV:-ci/server}
  [ -f "$srv/logs/latest.log" ] && cp "$srv/logs/latest.log" "$OUT/server_latest.log" 2>/dev/null || true
  [ -f "$srv/console.log" ] && cp "$srv/console.log" "$OUT/server_console.log" 2>/dev/null || true
  if [ -d "$srv/crash-reports" ]; then
    mkdir -p "$OUT/crash"
    cp "$srv"/crash-reports/*.txt "$OUT/crash/" 2>/dev/null || true
  fi
}

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
      -PautoJoin=127.0.0.1:25565 \
      ${WITH_DT:+-PwithDT} 2>&1
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

echo "== телепорт в Элисар и прогрузка области =="
if ! RCON list >/dev/null 2>&1; then
  echo "FATAL: выделенный сервер не отвечает по RCON до начала проб"
  keep_server_logs
  tail -40 "$OUT/server_latest.log" 2>/dev/null || true
  exit 1
fi
EL time set noon >/dev/null 2>&1 || true        # в Элисаре всегда полдень (ElisarDaylight), но фиксируем сразу
EL tp TestPlayer "$RX" "$((RF + 6))" "$RZ" 90 40 | tail -1
EL forceload add "$((RX - 80))" "$((RZ - 80))" "$((RX + 100))" "$((RZ + 140))" | tail -1
EL forceload add "$((HX - 40))" "$((HZ - 40))" "$((HX + 40))" "$((HZ + 40))" | tail -1
EL forceload add "$((CX - 40))" "$((CZ - 40))" "$((CX + 40))" "$((CZ + 40))" | tail -1
sleep 60

if ! RCON list >/dev/null 2>&1; then
  echo "FAIL: сервер упал на прогрузке Элисара (RCON молчит). Причина — в server_latest.log / crash:"
  keep_server_logs
  grep -nE "Exception|Caused by|crash" "$OUT/server_latest.log" 2>/dev/null | tail -20 || true
  FAILS=$((FAILS + 1))
fi

# ---------------------------------------------------------------- журнал проб (нужен уже здесь)
PROBES="$OUT/probes.txt"
: > "$PROBES"
log() { echo "$*" | tee -a "$PROBES"; }

# ---------------------------------------------------------------- самопроверка моря из самой игры
# riftsea_field <поле> <радиус>: гоняет /riftsea в позиции игрока и отдаёт одно поле ответа
riftsea_field() {
  local raw
  raw=$(RCON execute in riftworld:elisar at TestPlayer run riftsea 2>&1 | tr -d '\r' | tr '\n' ' ')
  log "  /riftsea raw: ${raw:0:400}"
  python3 - "$raw" "$1" <<'PYEOF'
import re, sys
s, k = sys.argv[1], sys.argv[2]
m = re.search(k + r"=(-?\d+)", s)
print(m.group(1) if m else "0")
PYEOF
}
# Рев 10: /riftsea считает по реальным блокам (глубина дна, вода и воздух в пещерах, кораллы),
# поэтому координаты камер больше не захардкожены — их отдаёт мод.
EL tp TestPlayer "$RX" "$((RF + 6))" "$RZ" 90 20 >/dev/null 2>&1
sleep 8
RREEF=$(riftsea_field floor)             # настоящая кромка дна в рифовой точке (мир стал глубже)
[ "$RREEF" != "0" ] && RF=$RREEF
SEA_RAW=$(RCON execute in riftworld:elisar at TestPlayer run riftsea 2>&1 | tr -d '\r' | tr '\n' ' ')
echo "  /riftsea -> ${SEA_RAW:0:400}"
read -r SEA_FLOOR SEA_WATER SEA_MIN SEA_CAVES SEA_WET SEA_AIR SEA_FLOAT SEA_CEIL SEA_PLANTS SEA_CORALS CAVE_X CAVE_Y CAVE_Z < <(python3 - "$SEA_RAW" <<'PYEOF'
import re, sys
s = sys.argv[1] if len(sys.argv) > 1 else ""
def g(k, d="0"):
    m = re.search(k + r"=(-?\d+)", s)
    return m.group(1) if m else d
c = re.search(r"caveat=(-?\d+),(-?\d+),(-?\d+)", s)
cx, cy, cz = c.groups() if c else ("0", "0", "0")
print(g("floor"), g("water"), g("min"), g("caves"), g("wet"), g("air"), g("float"), g("ceilair"), g("plants"), g("corals"), cx, cy, cz)
PYEOF
)
log "  /riftsea raw: ${SEA_RAW:0:600}"
echo "  дно=$SEA_FLOOR вода=$SEA_WATER min=$SEA_MIN | пещеры: колонок=$SEA_CAVES воды=$SEA_WET воздуха=$SEA_AIR (у свода $SEA_CEIL, висячего $SEA_FLOAT) | растения=$SEA_PLANTS кораллы=$SEA_CORALS | пещера=$CAVE_X,$CAVE_Y,$CAVE_Z"

# ---------------------------------------------------------------- пробы (реальная генерация)
FAILS=${FAILS:-0}
WATER=${WATER:-0}
write_report() {
  {
    echo "# CI, шаг 2: настоящий клиент Minecraft в Элисаре"
    echo
    echo "- клиент: ForgeGradle runClient под Xvfb (llvmpipe, без GPU)"
    echo "- воды в колонке (${KX:-?},y,${KZ:-?}): **$WATER** блоков"
    echo "- море: дно y=$SEA_FLOOR, вода $SEA_WATER блоков, глубочайшее дно y=$SEA_MIN"
    echo "- пещеры: $SEA_CAVES колонок, воды $SEA_WET / воздуха $SEA_AIR блоков (у свода $SEA_CEIL, висячего $SEA_FLOAT)"
    echo "- провалов проб: **$FAILS**"
    echo "- строк Exception/ERROR в логе клиента (без звука/наратора): **$(grep -E "Exception|ERROR" "$OUT/client.log" 2>/dev/null | grep -viE "Narrator|flite|text2speech|OpenAL|SoundSystem|SoundEngine" | wc -l)**"
    echo
    echo "## пробы"; echo '```'; cat "$PROBES" 2>/dev/null; echo '```'
    echo "## колонка (y; W=вода, S=донный песок, a=воздух/пещера, .=прочее)"; echo '```'; cat "$OUT/column.txt" 2>/dev/null; echo '```'
    echo "## скриншоты"; ls -1 "$OUT"/*.jpg 2>/dev/null | sed 's/^/- /'
  } > "$OUT/report_client.md"
}
trap write_report EXIT

trap 'keep_server_logs; write_report' EXIT

# проба «под игроком»: телепортируем игрока в точку и смотрим блок прямо под ним
at_block() { # at_block <x> <y> <z> <block> <label>
  local ans
  EL tp TestPlayer "${1:-0}" "${2:-0}" "${3:-0}" 0 0 >/dev/null 2>&1
  sleep 2
  ans=$(EL execute at TestPlayer run execute if block \~ \~ \~ "riftworld:$4" 2>&1)
  if [[ "$ans" == *"Test passed"* ]]; then log "  OK   $5 ($1,$2,$3) = $4"
  else log "  FAIL $5 ($1,$2,$3) != $4 | ответ: ${ans:0:120} | игрок: $(EL data get entity TestPlayer Pos 2>&1 | head -1)"; FAILS=$((FAILS + 1)); fi
}

echo "== пробы моря (под игроком) =="
at_block "$KX" "$((KT - 6))" "$KZ" elisar_water "вода на глубине"
seabed_probe() { # seabed_probe <x> <y> <z> <label>: твёрдый донный блок с перебором по высоте
  # ровно в точке может быть устье пещеры (дно пробито водой) или сад над дном — это нормально,
  # поэтому смотрим несколько колонок и несколько высот вокруг замеренной кромки
  local x=$1 y=$2 z=$3 label=$4 b dy dx dz
  for dy in 0 1 -1 2 -2; do
    for dx in 0 1 -1 2 -2; do
      for dz in 0 1 -1; do
        EL tp TestPlayer "$((x + dx))" "$((y + dy))" "$((z + dz))" 0 0 >/dev/null 2>&1
        sleep 1                              # телепорт применяется на тике
        for b in elisar_seabed elisar_silt reef_stone coral_brain coral_cap_pink coral_cap_teal coral_cap_violet cobble; do
          if [[ "$(EL execute at TestPlayer run execute if block \~ \~ \~ "riftworld:$b" 2>&1)" == *"Test passed"* ]]; then
            log "  OK   $label: $b на $((x + dx)),$((y + dy)),$((z + dz))"; return 0
          fi
        done
      done
    done
  done
  log "  FAIL $label ($x,$y,$z): твёрдого донного блока нет рядом"; FAILS=$((FAILS + 1))
}
EL tp TestPlayer "$KX" 60 "$KZ" 0 0 >/dev/null 2>&1
sleep 3
KFLOOR=$(riftsea_field floor)
[ "$KFLOOR" = "0" ] && KFLOOR=$SEA_FLOOR
seabed_probe "$KX" "$KFLOOR" "$KZ" "донный блок"

echo "== пробы Рев 10: глубокое дно и затопленные пещеры =="
probe_ok() { log "  OK   $1"; }
probe_fail() { log "  FAIL $1"; FAILS=$((FAILS + 1)); }
[ "$SEA_FLOOR" != "0" ] && probe_ok "кромка дна из /riftsea: y=$SEA_FLOOR" \
  || probe_fail "команда /riftsea не ответила (сырой ответ в probes.txt) — проба глубины не выполнена"
[ "$SEA_WATER" -ge 44 ] && probe_ok "толщина воды под рифом: $SEA_WATER блоков (нужно >=44)" \
  || probe_fail "море мелкое: $SEA_WATER блоков"
[ "$SEA_CAVES" -ge 20 ] && probe_ok "колонок с пещерами в радиусе проб: $SEA_CAVES" \
  || probe_fail "пещер слишком мало: $SEA_CAVES колонок"
if [ "$SEA_WET" -gt 0 ]; then
  PCT=$((SEA_WET * 100 / (SEA_WET + SEA_AIR + 1)))
  [ "$PCT" -ge 65 ] && probe_ok "пещеры ЗАТОПЛЕНЫ: воды ${PCT}% объёма полостей (остальное — воздушные карманы)" \
    || probe_fail "пещеры сухие: воды только ${PCT}% (баг Рев 8 вернулся)"
fi
# Рев 12: воздух в пещерах должен стоять У СВОДА, а не висеть пузырями в воде
AIRN=$((SEA_AIR + 1))
FPCT=$((SEA_FLOAT * 100 / AIRN))
CPCT=$((SEA_CEIL * 100 / AIRN))
[ "$FPCT" -le 12 ] && probe_ok "воздух в пещерах прижат к своду: висячих пузырей ${FPCT}% (нужно <=12%)" \
  || probe_fail "в толще воды висят воздушные пузыри: ${FPCT}% воздуха (было видно как «странная дырка»)"
[ "$CPCT" -ge 25 ] && probe_ok "воздух под сводами (под камнем): ${CPCT}% всего воздуха пещер" \
  || probe_fail "воздух размазан по пещере, у сводов только ${CPCT}%"
if [ "$CAVE_X" != "0" ]; then
  at_block "$CAVE_X" "$CAVE_Y" "$CAVE_Z" elisar_water "вода внутри пещеры"
  DEPTH=$((KT - CAVE_Y))
  probe_ok "пещера найдена на y=$CAVE_Y (в $DEPTH блоках под уровнем моря)"
else
  probe_fail "затопленная пещера не найдена в радиусе проб"
fi
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

echo "== Рев 12: коралловые деревья (растения из многих блоков) =="
EL tp TestPlayer "$RX" "$((RF + 8))" "$RZ" 90 10 >/dev/null 2>&1
sleep 6
CORAL_RAW=$(EL execute at TestPlayer run riftcoral 2>&1 | tr -d '\r' | tr '\n' ' ')
log "  /riftcoral raw: ${CORAL_RAW:0:400}"
read -r CORAL_AT CORAL_H CORAL_BLOCKS CORAL_COLS CORAL_TIPS CORAL_CAM CORAL_CAMLOW < <(python3 - "$CORAL_RAW" <<'PYEOF'
import re, sys
s = sys.argv[1] if len(sys.argv) > 1 else ""
def g(k, d="0"):
    m = re.search(k + r"=(-?\d+)", s)
    return m.group(1) if m else d
at = re.search(r"at=(-?\d+),(-?\d+),(-?\d+)", s)
at = ",".join(at.groups()) if at else "0,0,0"
cam = re.search(r"cam=(-?\d+),(-?\d+),(-?\d+)", s)
cam = ",".join(cam.groups()) if cam else "0,0,0"
low = re.search(r"camlow=(-?\d+),(-?\d+),(-?\d+)", s)
low = ",".join(low.groups()) if low else "0,0,0"
print(at, g("h"), g("blocks"), g("cols"), g("tips"), cam, low)
PYEOF
)
log "  коралл: at=$CORAL_AT h=$CORAL_H блоков=$CORAL_BLOCKS колонок=$CORAL_COLS украшений=$CORAL_TIPS"
if [ "$CORAL_AT" != "0,0,0" ]; then
  [ "$CORAL_H" -ge 3 ] && probe_ok "коралл — ВЫСОКОЕ растение: $CORAL_H блоков в высоту" \
    || probe_fail "коралл ниже 3 блоков (h=$CORAL_H) — всё ещё одиночный кустик"
  [ "$CORAL_BLOCKS" -ge 12 ] && probe_ok "коралл из МНОГИХ блоков: $CORAL_BLOCKS блоков, $CORAL_COLS колонок" \
    || probe_fail "коралл слишком маленький: $CORAL_BLOCKS блоков"
  [ "$CORAL_COLS" -ge 3 ] && probe_ok "у растения есть ветви/плита: $CORAL_COLS колонок" \
    || probe_fail "растение в одну колонку — нет силуэта"
  [ "$CORAL_TIPS" -ge 1 ] && probe_ok "на ветвях есть кораллы-украшения: $CORAL_TIPS" \
    || probe_fail "верхушки ветвей пустые"
  CX=$(printf '%s' "$CORAL_AT" | cut -d, -f1); CY=$(printf '%s' "$CORAL_AT" | cut -d, -f2); CZ=$(printf '%s' "$CORAL_AT" | cut -d, -f3)
  log "  камера коралла: cam=$CORAL_CAM camlow=$CORAL_CAMLOW"
else
  probe_fail "/riftcoral не нашла коралловое дерево"
fi

echo "== профиль колонки $KX,y,$KZ =="
PROFILE="$OUT/column.txt"
: > "$PROFILE"
for y in $(seq $((SEA_FLOOR - 4)) 66); do
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
[ "$CAVE_X" != "0" ] && EL tp TestPlayer "$CAVE_X" "$CAVE_Y" "$CAVE_Z" 0 0 >/dev/null 2>&1
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
lookat() { # lookat <cx> <cy> <cz> <tx> <ty> <tz> <pause> <name> — камера смотрит ровно на цель
  local yp
  yp=$(python3 tools/ci/lookat_angles.py "$1" "$2" "$3" "$4" "$5" "$6")
  view "$1" "$2" "$3" "${yp% *}" "${yp#* }" "$7" "$8"
}
view "$VX" 66 "$VZ" 90 42 14 "02_reef_from_above"        # рифовый сад сверху сквозь воду
view "$((VX + 10))" "$((VF + 6))" "$VZ" 270 18 14 "03_reef_underwater"   # риф вплотную под водой
view "$((VX + 4))" "$((VF + 9))" "$VZ" 210 22 14 "03b_reef_hills"         # рифовые холмы и палитры
view "$((VX - 18))" "$((VF + 3))" "$VZ" 45 4 14 "03c_reef_level"          # риф на уровне глаз рыбы
view "$((VX + 2))" "$((VF + 2))" "$VZ" 100 8 14 "03d_corals_3d"            # новые кораллы-растения (3D)
# Рев 12: КОРАЛЛОВЫЕ ДЕРЕВЬЯ — кадры сбоку, снизу и общий план рощи
if [ -n "${CX:-}" ] && [ "${CORAL_AT:-0,0,0}" != "0,0,0" ]; then
  EL gamemode spectator TestPlayer >/dev/null 2>&1 || true
  lookat "${CORAL_CAM%%,*}" "$(printf '%s' "$CORAL_CAM" | cut -d, -f2)" "${CORAL_CAM##*,}" "$CX" "$((CY + CORAL_H / 2))" "$CZ" 16 "16_coral_tree"
  lookat "${CORAL_CAMLOW%%,*}" "$(printf '%s' "$CORAL_CAMLOW" | cut -d, -f2)" "${CORAL_CAMLOW##*,}" "$CX" "$((CY + CORAL_H / 2))" "$CZ" 14 "16b_coral_tree_low"
  lookat "$((CX + 16))" "$((CY + 14))" "$((CZ + 5))" "$CX" "$((CY + 2))" "$CZ" 16 "16c_coral_grove"
  EL gamemode creative TestPlayer >/dev/null 2>&1 || true
else
  log "  ПРОПУСК кадров коралловых деревьев: /riftcoral не нашла растение"
fi
view "$((LX + 15))" "$((LF + 11))" "$((LZ + 15))" 225 14 14 "04_kelp_forest" # ламинария
view "$VX" 64 "$((VZ + 26))" 180 2 14 "05_open_sea"          # поверхность: открытое море
view "$DX" 65 "$DZ" 30 55 14 "06_trench"                     # каньон сверху
view "$HX" "$((HF + 14))" "$HZ" 0 58 14 "07_blue_hole"             # синяя дыра: взгляд в колодец сверху
if [ "$CAVE_X" != "0" ]; then
  view "$CAVE_X" "$((CAVE_Y + 1))" "$CAVE_Z" 0 6 16 "08_cave"          # затопленная пещера (координаты от /riftsea)
else
  log "  ПРОПУСК кадров пещеры: /riftsea не нашёл затопленную полость"
fi
[ "$CAVE_X" != "0" ] && view "$CAVE_X" "$CAVE_Y" "$CAVE_Z" 180 0 12 "09_cave_back"   # пещера, другой ракурс
# витрина: камера над коралловым садом, рыбы в 3-4 блоках по курсу
EL tp TestPlayer "$((VX + 10))" "$((VF + 6))" "$VZ" 270 8 >/dev/null 2>&1
sleep 2
SUM_LOCAL 3 0 0 '{NoAI:1b,Silent:1b,sf_variant:0}'
SUM_LOCAL 3.4 0.8 0.7 '{NoAI:1b,Silent:1b,sf_variant:1}'
SUM_LOCAL 4.2 -0.9 -0.3 '{NoAI:1b,Silent:1b,sf_variant:2}'
SUM_LOCAL 4.8 0.4 0.9 '{NoAI:1b,Silent:1b,sf_variant:3}'
sleep 6
shot "10_fish_reef"
# Рев 10: новые виды — мирные на рифу, хищники в затопленной пещере
EL tp TestPlayer "$((VX + 10))" "$((VF + 5))" "$VZ" 270 4 >/dev/null 2>&1
sleep 2
SUM_LOCAL 3 0 0 '{NoAI:1b,Silent:1b,sf_variant:7}'      # рыба-хирург
SUM_LOCAL 3.6 1.0 0.9 '{NoAI:1b,Silent:1b,sf_variant:8}' # морская черепаха
SUM_LOCAL 4.4 -0.9 -0.5 '{NoAI:1b,Silent:1b,sf_variant:0}'
sleep 6
shot "10b_fish_tropical"
if [ "$CAVE_X" != "0" ]; then
  EL tp TestPlayer "$CAVE_X" "$((CAVE_Y + 1))" "$CAVE_Z" 0 0 >/dev/null 2>&1
  sleep 2
  SUM_LOCAL 2.8 0 0 '{NoAI:1b,Silent:1b,sf_variant:9}'    # глубинный глот
  SUM_LOCAL 3.4 1.1 0.6 '{NoAI:1b,Silent:1b,sf_variant:10}'  # жгучая медуза
  SUM_LOCAL 4.0 -1.0 -0.4 '{NoAI:1b,Silent:1b,sf_variant:5}'
  sleep 6
  shot "11b_fish_cave_rare"
fi
# крупный план: рыба в трёх блоках прямо по центру кадра — видно даже мелкие детали рендера
EL tp TestPlayer "$KX" 44 "$KZ" 180 0 >/dev/null 2>&1
sleep 2
SUM_LOCAL 3 0 0 '{NoAI:1b,Silent:1b,sf_variant:0}'
SUM_LOCAL 3.6 0.7 0.9 '{NoAI:1b,Silent:1b,sf_variant:1}'
SUM_LOCAL 4.4 -0.9 -0.5 '{NoAI:1b,Silent:1b,sf_variant:4}'
sleep 6
shot "12_fish_closeup"
view "$KX" 22 "$KZ" 180 65 14 "13_reef_floor"                  # дно рифа вплотную: разбор блоков
[ "$CAVE_X" != "0" ] && view "$CAVE_X" "$CAVE_Y" "$CAVE_Z" 0 0 12 "11_fish_cave"     # хищники пещеры
RCON effect clear TestPlayer >/dev/null 2>&1 || true

# ---------------------------------------------------------------- Рев 11: каждая модель рыбы — отдельным кадром
echo "== модели рыб: JSON + текстура, каждый вид отдельно =="
MODEL_NAMES=(silver gold striped jelly ray angler eel surgeon turtle gloom sting)
MISSING=0
for n in "${MODEL_NAMES[@]}"; do
  [ -f "mod/src/main/resources/assets/riftworld/geo/$n.json" ] || { log "  FAIL нет geo/$n.json"; MISSING=$((MISSING + 1)); }
  [ -f "mod/src/main/resources/assets/riftworld/textures/entity/$n.png" ] || { log "  FAIL нет textures/entity/$n.png"; MISSING=$((MISSING + 1)); }
done
if [ "$MISSING" -eq 0 ]; then log "  OK   у всех ${#MODEL_NAMES[@]} видов есть JSON-модель и текстура"
else log "  FAIL моделей/текстур не хватает: $MISSING"; FAILS=$((FAILS + 1)); fi

# чистая толща воды над дном: рыба в 2.4 блоках по курсу занимает половину кадра
EL tp TestPlayer "$KX" 40 "$KZ" 180 2 >/dev/null 2>&1
sleep 3
RCON effect give TestPlayer minecraft:night_vision 900 0 true >/dev/null 2>&1 || true
for i in "${!MODEL_NAMES[@]}"; do
  EL execute at TestPlayer run kill @e[type=riftworld:sea_fish,distance=..48] >/dev/null 2>&1
  sleep 1
  # Rotation 90: рыба стоит к камере БОКОМ — видно профиль, плавники и хвост, а не «торец»
  SUM_LOCAL 2.2 0 0 "{NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[90f,0f],sf_variant:$i}"
  sleep 3
  shot "model_$(printf '%02d' "$i")_${MODEL_NAMES[$i]}"
done
EL execute at TestPlayer run kill @e[type=riftworld:sea_fish,distance=..48] >/dev/null 2>&1
RCON effect clear TestPlayer >/dev/null 2>&1 || true
# клиент ругается в лог, если JSON не распарсился — это отдельная проба
GEOERR=$(grep -c "не удалось загрузить geo-модель" "$OUT/client.log" 2>/dev/null || echo 0)
if [ "$(grep -c "не удалось загрузить geo-модель" "$OUT/client.log" 2>/dev/null || true)" -eq 0 ]; then
  log "  OK   клиент не жаловался на geo-модели"
else
  log "  FAIL клиент не смог загрузить geo-модели: $(grep -c "не удалось загрузить geo-модель" "$OUT/client.log")"
  FAILS=$((FAILS + 1))
fi

# ---------------------------------------------------------------- Рев 11: деревья с ветками (не квадрат)
echo "== деревья Элисара: ствол + ветки =="
EL tp TestPlayer -30 120 58 180 0 >/dev/null 2>&1
sleep 4
# сизо ярче амбры: на кадре видно и крону, и ветки (амбра почти белая на светлом грунте)
TREE_RAW=$(EL execute at TestPlayer run rifttree sizo 2>&1)
printf '%s' "$TREE_RAW" | grep -q "at=" || TREE_RAW=$(EL execute at TestPlayer run rifttree 2>&1)
log "  /rifttree raw: ${TREE_RAW:0:240}"
TREE_AT=$(printf '%s' "$TREE_RAW" | sed -n 's/.*at=\([-0-9]*,[-0-9]*,[-0-9]*\).*/\1/p' | head -1)
TREE_BR=$(printf '%s' "$TREE_RAW" | sed -n 's/.*branches=\([0-9]*\).*/\1/p' | head -1)
if [ -n "$TREE_AT" ]; then
  TX=$(printf '%s' "$TREE_AT" | cut -d, -f1); TY=$(printf '%s' "$TREE_AT" | cut -d, -f2); TZ=$(printf '%s' "$TREE_AT" | cut -d, -f3)
  log "  OK   дерево найдено: $TREE_AT (ветвей: ${TREE_BR:-?})"
  if [ "${TREE_BR:-0}" -ge 3 ]; then log "  OK   у дерева есть ветки: ${TREE_BR} блоков вне ствола"
  else log "  FAIL ветвей слишком мало: ${TREE_BR:-0} — дерево выглядит квадратом"; FAILS=$((FAILS + 1)); fi
  # телепорт к дереву, чтобы чанк прогрузился, и повторный счёт уже по настоящим блокам
  EL tp TestPlayer "$((TX + 10))" "$((TY + 10))" "$((TZ + 10))" 225 20 >/dev/null 2>&1
  sleep 8
  TREE2=$(EL execute at TestPlayer run rifttree sizo 2>&1)
  printf '%s' "$TREE2" | grep -q "at=" || TREE2=$(EL execute at TestPlayer run rifttree 2>&1)
  log "  /rifttree повтор: ${TREE2:0:200}"
  BR2=$(printf '%s' "$TREE2" | sed -n 's/.*branches=\([0-9]*\).*/\1/p' | head -1)
  LV2=$(printf '%s' "$TREE2" | sed -n 's/.*leaves=\([0-9]*\).*/\1/p' | head -1)
  if [ "${BR2:-0}" -ge 5 ] && [ "${LV2:-0}" -ge 20 ]; then
    log "  OK   дерево с ветками и кроной: ветвей $BR2, листвы $LV2"
  else
    log "  FAIL дерево неполное: ветвей ${BR2:-0}, листвы ${LV2:-0}"; FAILS=$((FAILS + 1))
  fi
  # камера смотрит из точки, которую нашла сама команда (cam/camtop): дерево может
  # стоять между домами, и «наугад +14 по диагонали» упирается в стену
  CAM=$(printf '%s' "$TREE2" | sed -n 's/.*cam=\([-0-9]*,[-0-9]*,[-0-9]*\).*/\1/p' | head -1)
  CAMTOP=$(printf '%s' "$TREE2" | sed -n 's/.*camtop=\([-0-9]*,[-0-9]*,[-0-9]*\).*/\1/p' | head -1)
  [ -n "$CAM" ] || CAM="$((TX + 16)),$((TY + 10)),$((TZ + 16))"
  [ -n "$CAMTOP" ] || CAMTOP="$((TX + 4)),$((TY + 22)),$((TZ + 4))"
  log "  камера дерева: cam=$CAM camtop=$CAMTOP"
  # без spectator игрок падает с высоты камеры (проверено: tp на y=117 -> в кадре земля с y=107)
  EL gamemode spectator TestPlayer >/dev/null 2>&1 || true
  lookat "${CAM%%,*}" "$(printf '%s' "$CAM" | cut -d, -f2)" "${CAM##*,}" \
         "$TX" "$((TY + 3))" "$TZ" 16 "14_tree_branches"
  lookat "${CAMTOP%%,*}" "$(printf '%s' "$CAMTOP" | cut -d, -f2)" "${CAMTOP##*,}" \
         "$TX" "$((TY + 2))" "$TZ" 16 "14b_tree_top"
  EL gamemode creative TestPlayer >/dev/null 2>&1 || true
else
  log "  FAIL /rifttree не ответила"; FAILS=$((FAILS + 1))
fi

# ---------------------------------------------------------------- Рев 11: аддон к Dynamic Trees (если включён)
if [ "${WITH_DT:-0}" = "1" ]; then
  echo "== Dynamic Trees: аддон riftworlddt (живой рост деревьев и кораллов) =="
  EL execute at TestPlayer run gamemode creative >/dev/null 2>&1 || true
  DTSEED=$(EL execute at TestPlayer run give TestPlayer riftworlddt:sizo_seed 1 2>&1)
  log "  give riftworlddt:sizo_seed -> ${DTSEED:0:120}"
  # 1.20.1 отвечает "Gave 1 [Sizo Seed] to TestPlayer" (старое "Given" — это 1.16)
  if [[ "$DTSEED" == *"Gave"* || "$DTSEED" == *"Given"* ]]; then log "  OK   DT видит семена аддона (riftworlddt:sizo_seed)"
  else log "  FAIL аддон к Dynamic Trees не загрузился (ответ give: ${DTSEED:0:160})"; FAILS=$((FAILS + 1)); fi
  DTCORAL=$(EL execute at TestPlayer run give TestPlayer riftworlddt:coral_staghorn_seed 1 2>&1)
  if [[ "$DTCORAL" == *"Gave"* || "$DTCORAL" == *"Given"* ]]; then log "  OK   DT видит коралловые семена (riftworlddt:coral_staghorn_seed)"
  else log "  FAIL коралловые виды DT не зарегистрированы (ответ give: ${DTCORAL:0:160})"; FAILS=$((FAILS + 1)); fi
  DTHIT=$(grep -cE "riftworlddt:sizo" "$OUT/client.log" 2>/dev/null || echo 0)
  log "  упоминаний riftworlddt:sizo в логе: $DTHIT"

  # семьи в регистрах DT (JSON должны быть расчитаны и зарегистрированы)
  # NB: у DT подкоманда требует явный "list"; иначе она молчит, и проба ложно падала.
  # Имя подкоманды = имя реестра в нижнем регистре: Family -> family (не families!).
  # "true" = RAW-режим, печатает registry name (riftworlddt:sizo), а не переведённое имя.
  DTFAM=$(EL execute in riftworld:elisar run dynamictrees registry family list true 2>&1 | tr '\r\n' '  ')
  log "  /dynamictrees registry family list -> ${DTFAM:0:220}"
  DTSP=$(EL execute in riftworld:elisar run dynamictrees registry species list true 2>&1 | tr '\r\n' '  ')
  log "  /dynamictrees registry species list -> ${DTSP:0:220}"
  for f in riftworlddt:sizo riftworlddt:ambra riftworlddt:coral; do
    if [[ "$DTFAM" == *"$f"* ]]; then log "  OK   у DT есть семья $f"
    else log "  FAIL у DT нет семьи $f (ответ DT: ${DTFAM:0:180})"; FAILS=$((FAILS + 1)); fi
  done
  for f in riftworlddt:sizo riftworlddt:ambra riftworlddt:giant_sizo riftworlddt:coral_staghorn \
           riftworlddt:coral_table riftworlddt:coral_whip; do
    if [[ "$DTSP" == *"$f"* ]]; then log "  OK   у DT есть вид $f"
    else log "  FAIL у DT нет вида $f (ответ DT: ${DTSP:0:180})"; FAILS=$((FAILS + 1)); fi
  done

  # ---------------------------------------------------------------- дерево через DT: /dynamictrees settree
  # Рев 13.6: раньше посадка шла «на глазок» — в точке, где могло не быть грунта, и проба падала
  # без объяснений. Теперь готовим площадку: ставим наш грунт (elisar_soil), расчищаем воздух,
  # сажаем на блок выше грунта и проверяем, что DT поставил ветви НАШЕГО аддона.
  ELP_dt() { EL execute in riftworld:elisar run "$@" 2>&1 | tr '\r\n' '  '; }
  dt_grow() {  # dt_grow <x> <y> <z> <species> [jo_code] — пробует settree, потом старое имя set
    local x=$1 y=$2 z=$3 sp=$4 code=${5:-} out qcode=''
    [ -n "$code" ] && qcode="\"$code\""   # JoCode должен прийти в кавычках (StringArgumentType.string())
    for cmd in settree set; do
      if [ -n "$code" ]; then
        # JoCode у DT — StringArgumentType.string(): нужны КАВЫЧКИ, иначе Brigadier ругается
        # «Expected whitespace to end one argument, but found trailing data».
        out=$(ELP_dt dynamictrees $cmd "$x" "$y" "$z" "$sp" "$qcode")
      else
        out=$(ELP_dt dynamictrees $cmd "$x" "$y" "$z" "$sp")
      fi
      case "$out" in
        *"Incorrect argument"*|*"Unknown or incomplete"*|*"Unknown command"*|*"Unknown or incomplete command"*) continue ;;
      esac
      printf '%s' "$out"; return 0
    done
    printf '%s' "$out"
  }
  dt_pad() {  # dt_pad <x> <y> <z> — грунт под саженец + столб воздуха над ним
    ELP_dt setblock "$1" "$2" "$3" riftworld:elisar_soil >/dev/null 2>&1 || true
    ELP_dt fill "$1" "$(( $2 + 1 ))" "$3" "$1" "$(( $2 + 12 ))" "$3" minecraft:air >/dev/null 2>&1 || true
  }
  # Рев 13.6: количество блоков считает мод одной командой (/riftdtat) — раньше было
  # несколько сотен «execute if block» по RCON, и шаг клиента заметно растягивался.
  dt_at() { EL execute in riftworld:elisar run riftdtat "$1" "$2" "$3" 2>&1 | grep -m1 "riftdtat" || true; }
  # Рев 13.7: settree ставит только «затравку» (JoCode JP = пенёк), DT растит её импульсами.
  # Раньше из-за этого в мире было 3 блока, и проба честно падала.
  dt_grow_pulses() {  # dt_grow_pulses <x> <y> <z> <min> — импульсы роста, пока блоков не станет >= min
    local x=$1 y=$2 z=$3 want=$4 tot=0 n
    for n in 4 8 16 32 64; do
      EL execute in riftworld:elisar run dynamictrees growpulse "$x" "$y" "$z" "$n" >/dev/null 2>&1 || true
      sleep 2
      tot=$(dt_at "$x" "$y" "$z" | sed -n 's/.* dt=\([0-9]*\).*/\1/p' | head -1)
      tot=${tot:-0}
      [ "$tot" -ge "$want" ] && break
    done
    printf '%s' "$tot"
  }
  echo "  [DT] секция дерева" >> "$PROBES"
  if [ -n "$TREE_AT" ]; then
    # площадка рядом с найденным деревом sizo: там точно суша и загруженные чанки
    DTX=$((TX + 9)); DTZ=$((TZ + 3)); DTY=$TY
    EL tp TestPlayer "$((DTX + 6))" "$((DTY + 8))" "$((DTZ + 6))" 225 25 >/dev/null 2>&1
    sleep 3
    dt_pad "$DTX" "$DTY" "$DTZ"
    # JoCode берём из jo_codes/sizo.txt (сизо = стройное дерево, коды от берёзы DT)
    DTS=$(dt_grow "$DTX" "$((DTY + 1))" "$DTZ" riftworlddt:sizo "2:JJJ0JP1JJ+ZJOOPvnRJOOPvn")
    log "  dynamictrees settree $DTX,$((DTY + 1)),$DTZ riftworlddt:sizo -> ${DTS:0:160}"
    sleep 6
    dt_grow_pulses "$DTX" "$((DTY + 1))" "$DTZ" 30 >/dev/null
    DTAT=$(dt_at "$DTX" "$((DTY + 1))" "$DTZ")
    log "  /riftdtat у посадки дерева -> ${DTAT:0:300}"
    DT_BR=$(printf '%s' "$DTAT" | sed -n 's/.*_branch=\([0-9]*\).*/\1/p' | head -1)
    DT_TOT=$(printf '%s' "$DTAT" | sed -n 's/.* dt=\([0-9]*\).*/\1/p' | head -1)
    if [ "${DT_TOT:-0}" -ge 20 ] && [ -n "$DT_BR" ]; then
      log "  OK   DT вырастил дерево аддона: блоков $DT_TOT (ветвей $DT_BR)"
    else
      log "  FAIL DT не вырастил дерево (блоков $DT_TOT, ответ: ${DTS:0:160})"; FAILS=$((FAILS + 1))
    fi
    EL gamemode spectator TestPlayer >/dev/null 2>&1 || true
    lookat "$((DTX + 12))" "$((DTY + 11))" "$((DTZ + 12))" "$DTX" "$((DTY + 6))" "$DTZ" 14 "15_dt_tree"
    EL gamemode creative TestPlayer >/dev/null 2>&1 || true
  else
    log "  FAIL нет TREE_AT — DT-дерево не проверено"; FAILS=$((FAILS + 1))
  fi

  # ---------------------------------------------------------------- коралл через DT: подводный JoCode
  echo "  [DT] секция коралла" >> "$PROBES"
  # Рев 13.6: подводный саженец ставим на наш грунт, который кладём сами на дно рифа —
  # иначе DT отказывается расти (под точкой посадки может быть рифовая скала, а не грунт).
  DEX=$((RX + 6)); DEZ=$((RZ + 6)); DEY=$((RF + 1))
  ELP_dt setblock "$DEX" "$RF" "$DEZ" riftworld:elisar_soil >/dev/null 2>&1 || true
  ELP_dt fill "$DEX" "$DEY" "$DEZ" "$DEX" "$((DEY + 10))" "$DEZ" minecraft:air >/dev/null 2>&1 || true
  ELP_dt setblock "$DEX" "$((DEY - 1))" "$DEZ" riftworld:elisar_soil >/dev/null 2>&1 || true
  log "  координаты DT-коралла: $DEX,$DEY,$DEZ (риф $RX,$RF,$RZ)"
  DTS2=$(dt_grow "$DEX" "$DEY" "$DEZ" riftworlddt:coral_staghorn "8:P")
  log "  dynamictrees settree $DEX,$DEY,$DEZ riftworlddt:coral_staghorn -> ${DTS2:0:160}"
  sleep 6
  dt_grow_pulses "$DEX" "$DEY" "$DEZ" 12 >/dev/null
  DCAT=$(dt_at "$DEX" "$DEY" "$DEZ")
  log "  /riftdtat у посадки коралла -> ${DCAT:0:300}"
  DC_TOT=$(printf '%s' "$DCAT" | sed -n 's/.* dt=\([0-9]*\).*/\1/p' | head -1)
  DC_BR=$(printf '%s' "$DCAT" | sed -n 's/.*_branch=\([0-9]*\).*/\1/p' | head -1)
  if [ "${DC_TOT:-0}" -ge 8 ] && [ -n "$DC_BR" ]; then
    log "  OK   DT вырастил ПОДВОДНЫЙ коралл: блоков $DC_TOT (ветвей $DC_BR)"
  else
    log "  FAIL DT не вырастил подводный коралл (блоков $DC_TOT, ответ: ${DTS2:0:160})"; FAILS=$((FAILS + 1))
  fi
  DC=$(EL execute in riftworld:elisar run riftcoral 2>&1)
  log "  /riftcoral -> ${DC:0:200}"
  # кадр рощи: смотрим на риф (наша роща уже в воде)
  lookat "$((RX + 16))" "$((RF + 14))" "$((RZ + 5))" "$RX" "$((RF + 2))" "$RZ" 16 "16c_coral_grove"
fi

RCON effect clear TestPlayer >/dev/null 2>&1 || true
RCON effect clear TestPlayer >/dev/null 2>&1 || true

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
# Рев 13: «Exception loading blockstate definition ... missing model for variant» — это WARN,
# а не ошибка клиента, но именно им заканчивалась история с namespace аддона DT.
# Считаем отдельно, чтобы причина была видна в отчёте одной строкой, а не тонула в общем ERRS.
MODELWARN=$(grep -c "missing model for variant" "$OUT/client.log" 2>/dev/null || true)
MODELWARN=${MODELWARN:-0}
ERRS=$(grep -E "Exception|ERROR" "$OUT/client.log" 2>/dev/null \
       | grep -viE "Narrator|flite|text2speech|OpenAL|SoundSystem|SoundEngine|no sound|libflite" \
       | grep -viE "missing model for variant|loading blockstate definition|ModelBakery" | wc -l)
echo "строк Exception/ERROR в логе клиента (без звука/наратора и без missing-model): $ERRS"
echo "missing model for variant в логе клиента: $MODELWARN"
if [ "$MODELWARN" -gt 0 ]; then
  echo "::error title=модели::missing model for variant: $MODELWARN строк — блоки регистрируются не в своём namespace"
  grep "missing model for variant" "$OUT/client.log" | head -3 \
    | while IFS= read -r line; do echo "::error title=модели::${line:0:300}"; done
fi

{
  echo "# CI, шаг 2: настоящий клиент Minecraft в Элисаре"
  echo
  echo "- клиент: ForgeGradle runClient под Xvfb (llvmpipe, без GPU)"
  echo "- воды в колонке ($KX,y,$KZ): **$WATER** блоков"
  echo "- провалов проб: **$FAILS**"
  echo "- моделей рыб (JSON + текстура) в кадре: **${#MODEL_NAMES[@]}**"
  echo "- строк Exception/ERROR в логе клиента (без звука/наратора и без missing-model): **$ERRS**"
  echo "- missing model for variant в логе клиента: **$MODELWARN**"
  echo
  echo "## пробы"; echo '```'; cat "$PROBES"; echo '```'
  echo "## колонка (y=0..66; W=вода, S=донный песок, a=воздух/пещера, .=прочее)"; echo '```'; cat "$PROFILE"; echo '```'
  echo "## скриншоты"; ls -1 "$OUT"/*.jpg 2>/dev/null | sed 's/^/- /'
} > "$OUT/report_client.md"
cat "$OUT/report_client.md"

python3 tools/rcon.py --password "$RCON_PW" --log "" stop >/dev/null 2>&1 || true

# Рев 13: шаг падал «молча» — в логе прогона не было видно ни одной причины, а артефакты
# скачать нельзя. Теперь каждая провалившаяся проба и каждая строка Exception/ERROR
# выводится АННОТАЦИЕЙ GitHub (::error::), она видна через API прогона без артефактов.
if [ "$FAILS" -gt 0 ]; then
  n=0
  while IFS= read -r line; do
    n=$((n + 1)); [ "$n" -gt 12 ] && break
    echo "::error title=проба клиента::${line#  }"
  done < <(grep -E "^  FAIL" "$PROBES" 2>/dev/null | head -12)
  echo "::error title=итог::провалов проб: $FAILS (см. probes.txt)"
fi
if [ "$ERRS" -gt 0 ]; then
  grep -E "Exception|ERROR" "$OUT/client.log" 2>/dev/null \
    | grep -viE "Narrator|flite|text2speech|OpenAL|SoundSystem|SoundEngine|no sound|libflite" \
    | head -10 | while IFS= read -r line; do
        echo "::error title=ошибка в логе клиента::${line:0:300}"
      done
  echo "::error title=итог::строк Exception/ERROR в логе клиента: $ERRS"
fi
echo "== итог: провалов $FAILS, ошибок в логе $ERRS, снимков $(ls -1 "$OUT"/*.jpg 2>/dev/null | wc -l) =="

[ "$FAILS" -eq 0 ] && [ "$ERRS" -eq 0 ] && [ "$MODELWARN" -eq 0 ]
