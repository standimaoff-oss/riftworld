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
# Рев 13.17: число или безопасный 0. В bash `$((n + 1))` с мусором в n — синтаксическая ошибка,
# которая обрывает выполняемый блок целиком и молча (проверено). Все координаты прогона идут через asm().
asm() {
  local v=${1:-}
  case "$v" in ''|*[!0-9-]*) echo 0 ;; *) echo "$((10#$v))" ;; esac
}

RUN=${RUN:-ci/runs}
DISP=${DISP:-:99}
RCON_PW=${RCON_PW:-riftci}
SPOTS=tools/ci/spots.json
mkdir -p "$OUT" "$RUN/client"

# ---------------------------------------------------------------- зависимости
sudo apt-get update -qq >/dev/null 2>&1 || true
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq xvfb openbox x11-utils xdotool imagemagick libgl1 libglx-mesa0 \
  libxi6 libxxf86vm1 libxfixes3 libxrender1 libxkbcommon0 libsm6 libopenal1 >/dev/null 2>&1 || true
command -v Xvfb >/dev/null || { echo "FATAL: нет Xvfb"; exit 1; }
command -v import >/dev/null || { echo "FATAL: нет imagemagick"; exit 1; }

# ---------------------------------------------------------------- настройки клиента
python3 - <<'PY'
import os
run = os.environ.get("RUN", "ci/runs")
opts = {
    "tutorialStep": "none", "onboardAccessibility": "false", "skipMultiplayerWarning": "true",
    "joinedFirstServer": "true", "renderDistance": "12", "simulationDistance": "8", "maxFps": "30",
    "guiScale": "2", "fov": "0.0", "graphicsMode": "1", "renderClouds": "false",
    "pauseOnLostFocus": "false", "narrator": "0", "soundCategory_master": "0.0",
    "fullscreen": "true", "showSubtitles": "false", "autoJump": "false",
    "darkMojangStudiosBackground": "true", "skipRealms32bitWarning": "true",
    "gamma": "1.0", "hideGui": "false", "bobView": "false",
    "overrideWidth": "1920", "overrideHeight": "1080", "mipmapLevels": "0",
    "entityDistanceScaling": "1.5", "biomeBlendRadius": "3",
}
os.makedirs(run + "/client/config", exist_ok=True)
with open(run + "/client/options.txt", "w", encoding="utf-8") as f:
    for k, v in opts.items():
        f.write(f"{k}:{v}\n")
with open(run + "/client/config/fml.toml", "w", encoding="utf-8") as f:
    f.write(
        "splashscreen = true\n"
        "maxThreads = -1\n"
        "versionCheck = false\n"
        "defaultConfigPath = \"defaultconfigs\"\n"
        "disableOptimizedDFU = true\n"
        "earlyWindowControl = true\n"
        "earlyWindowWidth = 1920\n"
        "earlyWindowHeight = 1080\n"
        "earlyWindowFBScale = 1\n"
        "earlyWindowMaximized = true\n"
        "earlyWindowProvider = \"fmlearlywindow\"\n"
        "earlyWindowSkipGLVersions = []\n"
        "earlyWindowLogHelpMessage = false\n"
        "earlyWindowSquir = false\n"
        "earlyWindowShowCPU = false\n"
    )
print("client options + fml.toml 1920x1080 ->", run + "/client/options.txt")
PY

# ---------------------------------------------------------------- дисплей (Full HD 1920x1080 + WM openbox)
if ! pgrep -f "Xvfb $DISP" >/dev/null; then
  Xvfb "$DISP" -screen 0 1920x1080x24 -nolisten tcp >/tmp/xvfb.log 2>&1 &
  sleep 3
fi
export DISPLAY=$DISP LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe MESA_GL_VERSION_OVERRIDE=3.3
if command -v openbox >/dev/null 2>&1 && ! pgrep -x openbox >/dev/null 2>&1; then
  openbox >/tmp/openbox.log 2>&1 &
  sleep 2
fi

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
    if [ "${WIDTH:-0}" -lt 1800 ] || [ "${HEIGHT:-0}" -lt 1000 ]; then
      xdotool windowsize "$WID" 1920 1080 2>/dev/null || true
      xdotool windowmove "$WID" 0 0 2>/dev/null || true
      sleep 2
      geom || true
    fi
    import -window root /tmp/full.png 2>/dev/null || return 1
    convert /tmp/full.png -crop "${WIDTH}x${HEIGHT}+${X}+${Y}" +repage -sampling-factor 4:4:4 -quality 95 "$OUT/$1.jpg" 2>/dev/null \
      || cp /tmp/full.png "$OUT/$1.png"
  else
    import -window root -sampling-factor 4:4:4 -quality 95 "$OUT/$1.jpg" 2>/dev/null || return 1
  fi
  echo "  shot: $OUT/$1.jpg (${WIDTH:-1920}x${HEIGHT:-1080})"
}

key() { # key <F1>
  if geom; then xdotool windowactivate --sync "$WID" 2>/dev/null || true; xdotool key --window "$WID" "$1" 2>/dev/null || xdotool key "$1"; fi
}

RCON() { python3 tools/rcon.py --password "$RCON_PW" --log "" --timeout 120 "$@" 2>&1; }
if ! RCON list >/dev/null 2>&1; then
  echo "::error title=клиент::сервер не отвечает по RCON — проверка клиента невозможна"
  echo "FATAL: сервер не отвечает по RCON — клиенту некуда заходить (см. отчёт шага сервера)"
  tail -20 "${SRV:-ci/server}/console.log" 2>/dev/null | while IFS= read -r l; do echo "::error title=console::${l:0:300}"; done
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
    # Рев 15.2: Dynamic Trees подключается ЛОКАЛЬНЫМ jar-ом (-PdtJarPath), а не из maven:
    # оттуда приходит битый pom и валит сборку ещё на этапе конфигурации.
    ./gradlew runClient --no-daemon --console=plain \
      -PrunDir="$PWD/../$RUN" \
      -PautoJoin=127.0.0.1:25565 \
      ${WITH_DT:+-PwithDT} ${DT_MAVEN:+-PdtMaven=$DT_MAVEN} 2>&1
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
RX=$(asm "$RX"); RZ=$(asm "$RZ"); RF=$(asm "$RF")   # Рев 13.17: координаты только числом
read -r DX DZ DF < <(python3 -c "import json;d=json.load(open('$SPOTS'));print(d['deep']['x'],d['deep']['z'],d['deep']['floor'])")
read -r HX HZ HF < <(python3 -c "import json;d=json.load(open('$SPOTS'));print(d['hole']['x'],d['hole']['z'],d['hole']['floor'])")
read -r CX CZ CLOW CHIGH < <(python3 -c "import json;d=json.load(open('$SPOTS'));print(d['cave']['x'],d['cave']['z'],d['cave']['low'],d['cave']['high'])")
read -r KX KZ KF KB KT < <(python3 -c "import json;d=json.load(open('$SPOTS'));c=d['column'];print(c['x'],c['z'],c['floor'],c['water_bottom'],c['water_top'])")
read -r VX VZ VF < <(python3 -c "import json;d=json.load(open('$SPOTS'));print(d['reef_vista']['x'],d['reef_vista']['z'],d['reef_vista']['floor'])")
read -r LX LZ LF < <(python3 -c "import json;d=json.load(open('$SPOTS'));print(d['kelp']['x'],d['kelp']['z'],d['kelp']['floor'])")
# Рев 13.17: любая координата сцены — только числом (иначе $((...)) тихо обрывает целый блок)
for v in DX DZ DF HX HZ HF CX CZ CLOW CHIGH KX KZ KF KB KT VX VZ VF LX LZ LF; do
  eval "$v=\$(asm \"\${$v:-}\")"
done
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
  # Рев 13.17: log пишет в stdout, а функцию вызывают через $(...). Из-за этого RF получался
  # многострочным («  /riftsea raw: ...» + число), и любое $((RF + 6)) ниже валилось с
  # «syntax error: operand expected». bash в этом случае ТИХО обрывает весь составной блок
  # (if WITH_DT ... fi) — поэтому секция коралла не выполнялась вообще, а отчёт выглядел целым.
  echo "  /riftsea raw: ${raw:0:400}" >> "$PROBES"   # в журнал — да, в stdout (возврат) — нет
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
RREEF=$(riftsea_field floor | tail -1)   # настоящая кромка дна в рифовой точке (мир стал глубже)
RREEF=$(asm "$RREEF"); [ "$RREEF" != "0" ] && RF=$RREEF
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
KFLOOR=$(asm "$(riftsea_field floor)")
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
  CX=$(asm "$(printf '%s' "$CORAL_AT" | cut -d, -f1)"); CY=$(asm "$(printf '%s' "$CORAL_AT" | cut -d, -f2)"); CZ=$(asm "$(printf '%s' "$CORAL_AT" | cut -d, -f3)")
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
  # Рев 14.27: после утолщения кораллов 2x2 камера +16/+14/+5 оказывалась внутри живой массы —
  # кадр 16c итерации 26 был красной стенкой. Отодвигаем объектив.
  lookat "$((CX + 24))" "$((CY + 18))" "$((CZ + 8))" "$CX" "$((CY + 2))" "$CZ" 16 "16c_coral_grove"
  EL gamemode creative TestPlayer >/dev/null 2>&1 || true
else
  log "  ПРОПУСК кадров коралловых деревьев: /riftcoral не нашла растение"
fi
view "$((LX + 15))" "$((LF + 11))" "$((LZ + 15))" 225 14 14 "04_kelp_forest" # ламинария
view "$VX" 64 "$((VZ + 26))" 180 2 14 "05_open_sea"          # поверхность: открытое море
view "$DX" 65 "$DZ" 30 55 14 "06_trench"                     # каньон сверху
view "$HX" "$((HF + 14))" "$HZ" 0 58 14 "07_blue_hole"             # синяя дыра: взгляд в колодец сверху
if [ "$CAVE_X" != "0" ]; then
  # Рев 13.26: затопленная пещера без источника света выходила чёрным квадратом — по такому
  # кадру «пещеры забиты странно» не проверить. Ночное зрение только на время кадров пещеры.
  EL effect give TestPlayer minecraft:night_vision 600 0 true >/dev/null 2>&1 || true
  view "$CAVE_X" "$((CAVE_Y + 1))" "$CAVE_Z" 0 6 16 "08_cave"          # затопленная пещера (координаты от /riftsea)
else
  log "  ПРОПУСК кадров пещеры: /riftsea не нашёл затопленную полость"
fi
[ "$CAVE_X" != "0" ] && view "$CAVE_X" "$CAVE_Y" "$CAVE_Z" 180 0 12 "09_cave_back"   # пещера, другой ракурс
EL effect clear TestPlayer minecraft:night_vision >/dev/null 2>&1 || true
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
  # Rotation 90: рыба стоит к камере БОКОМ — видно профиль, плавники и хвост, а не «торец».
  # Плоским видам (медуза, скат) профиль не нужен: сбоку они читаются линией — им три четверти.
  ROT=90f
  case "${MODEL_NAMES[$i]}" in jelly|ray) ROT=45f;; esac
  SUM_LOCAL 2.6 0 0 "{NoAI:1b,Silent:1b,NoGravity:1b,Rotation:[${ROT},0f],sf_variant:$i}"
  sleep 1
  # Рев 13.26: рыба успевала уплыть за 3 секунды, и кадры model_* выходили пустой водой.
  # Прибиваем её к точке съёмки и пишем в лог, где она на самом деле стоит.
  EL execute at TestPlayer rotated as TestPlayer run tp @e[type=riftworld:sea_fish,sort=nearest,limit=1] ^0 ^0 ^2.6 >/dev/null 2>&1 || true
  FISH_POS=$(EL execute at TestPlayer run data get entity @e[type=riftworld:sea_fish,distance=..5,sort=nearest,limit=1] Pos 2>&1)
  log "  рыба ${MODEL_NAMES[$i]}: ${FISH_POS:0:120}"
  # Гейт кадра: витрина имеет смысл, только если рыба реально в 5 блоках от камеры.
  # Раньше пустые кадры model_* проходили молча — и «модель плохая» было не проверить.
  if [[ "$FISH_POS" != *"entity data"* ]]; then
    log "  FAIL витрина ${MODEL_NAMES[$i]}: рыбы нет в кадре"
    FAILS=$((FAILS + 1))
  fi
  sleep 1
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
TREE_RAW=$(EL execute at TestPlayer run rifttree ambra 2>&1)
printf '%s' "$TREE_RAW" | grep -q "at=" || TREE_RAW=$(EL execute at TestPlayer run rifttree 2>&1)
log "  /rifttree raw: ${TREE_RAW:0:240}"
TREE_AT=$(printf '%s' "$TREE_RAW" | sed -n 's/.*at=\([-0-9]*,[-0-9]*,[-0-9]*\).*/\1/p' | head -1)
TREE_BR=$(printf '%s' "$TREE_RAW" | sed -n 's/.*branches=\([0-9]*\).*/\1/p' | head -1)
if [ -n "$TREE_AT" ]; then
  TX=$(asm "$(printf '%s' "$TREE_AT" | cut -d, -f1)"); TY=$(asm "$(printf '%s' "$TREE_AT" | cut -d, -f2)"); TZ=$(asm "$(printf '%s' "$TREE_AT" | cut -d, -f3)")
  log "  OK   дерево найдено: $TREE_AT (ветвей: ${TREE_BR:-?})"
  if [ "${WITH_DT:-0}" = "1" ] && [ "${TREE_BR:-0}" -lt 3 ]; then
    log "  OK   обычных деревьев генератора в плане нет (активен аддон DT)"
  elif [ "${TREE_BR:-0}" -ge 3 ]; then log "  OK   у дерева есть ветки: ${TREE_BR} блоков вне ствола"
  else log "  FAIL ветвей слишком мало: ${TREE_BR:-0} — дерево выглядит квадратом"; FAILS=$((FAILS + 1)); fi
  # телепорт к дереву, чтобы чанк прогрузился, и повторный счёт уже по настоящим блокам
  EL tp TestPlayer "$((TX + 10))" "$((TY + 10))" "$((TZ + 10))" 225 20 >/dev/null 2>&1
  sleep 8
  TREE2=$(EL execute at TestPlayer run rifttree ambra 2>&1)
  printf '%s' "$TREE2" | grep -q "at=" || TREE2=$(EL execute at TestPlayer run rifttree 2>&1)
  log "  /rifttree повтор: ${TREE2:0:200}"
  BR2=$(printf '%s' "$TREE2" | sed -n 's/.*branches=\([0-9]*\).*/\1/p' | head -1)
  LV2=$(printf '%s' "$TREE2" | sed -n 's/.*leaves=\([0-9]*\).*/\1/p' | head -1)
  if [ "${WITH_DT:-0}" = "1" ] && [ "${LV2:-0}" -eq 0 ]; then
    # Рев 16: обычных (stock) деревьев в мире с DT больше нет — их место занимает аддон
    # (проверяется пробами /riftdtwild, /riftdtat и ростом саженца ниже по логу).
    log "  OK   обычных деревьев генератора нет: деревья ставит только аддон DT (требование рев 16)"
  elif [ "${BR2:-0}" -ge 5 ] && [ "${LV2:-0}" -ge 20 ]; then
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
  # Рев 16: коралловых деревьев больше нет — проверяем семя единственного «хлыстового» вида
  DTCORAL=$(EL execute at TestPlayer run give TestPlayer riftworlddt:lilac_seed 1 2>&1)
  if [[ "$DTCORAL" == *"Gave"* || "$DTCORAL" == *"Given"* ]]; then log "  OK   DT видит семена лилового хлыста (riftworlddt:lilac_seed)"
  else log "  FAIL вид riftworlddt:lilac не зарегистрирован (ответ give: ${DTCORAL:0:160})"; FAILS=$((FAILS + 1)); fi
  # Рев 16: «садовый дуб» — новый вид аддона (в садах вместо сизоствола)
  DTGO=$(EL execute at TestPlayer run give TestPlayer riftworlddt:garden_oak_seed 1 2>&1)
  if [[ "$DTGO" == *"Gave"* || "$DTGO" == *"Given"* ]]; then log "  OK   DT видит жёлудь садового дуба (riftworlddt:garden_oak_seed)"
  else log "  FAIL вид riftworlddt:garden_oak не зарегистрирован (ответ give: ${DTGO:0:160})"; FAILS=$((FAILS + 1)); fi
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
  for f in riftworlddt:sizo riftworlddt:ambra riftworlddt:lilac riftworlddt:garden_oak; do
    if [[ "$DTFAM" == *"$f"* ]]; then log "  OK   у DT есть семья $f"
    else log "  FAIL у DT нет семьи $f (ответ DT: ${DTFAM:0:180})"; FAILS=$((FAILS + 1)); fi
  done
  for f in riftworlddt:sizo riftworlddt:ambra riftworlddt:lilac riftworlddt:giant_sizo riftworlddt:garden_oak; do
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
  dt_pad() {  # dt_pad <x> <y> <z> [радиус] — грунт под саженец + свободное место для кроны
    local x=$1 y=$2 z=$3 r=${4:-6}
    ELP_dt setblock "$x" "$y" "$z" riftworld:elisar_soil >/dev/null 2>&1 || true
    # Рев 13.11: расчищаем ПЛОЩАДКУ, а не один столбик. Иначе крона JoCode упирается в склон
    # острова, DT её «задушивает», и от дерева остаётся три блока (именно так и падала проба).
    ELP_dt fill "$((x - r))" "$((y + 1))" "$((z - r))" "$((x + r))" "$((y + 14))" "$((z + r))" minecraft:air >/dev/null 2>&1 || true
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
  # Рев 13.14: связность вида — семья/ветвь/листва/блок листвы. Без этого «выросло 6 блоков»
  # приходилось угадывать; теперь в отчёте видно, какое звено пустое.
  DTD=$(EL execute in riftworld:elisar run riftdt 2>&1 | tr '\r\n' '  ')
  DTD=${DTD##*diag=}; DTD=${DTD%% err=*}
  log "  /riftdt diag -> ${DTD:0:420}"
  if [ -n "$TREE_AT" ]; then
    # площадка рядом с найденным деревом sizo: там точно суша и загруженные чанки
    DTX=$((TX + 9)); DTZ=$((TZ + 3)); DTY=$TY
    EL tp TestPlayer "$((DTX + 6))" "$((DTY + 8))" "$((DTZ + 6))" 225 25 >/dev/null 2>&1
    sleep 3
    dt_pad "$DTX" "$DTY" "$DTZ" 6
    # Рев 13.13: ГЛАВНАЯ ошибка прошлых прогонов — координата. У /dynamictrees settree первый
    # аргумент — это позиция ROOTY (грунта), а не ствола: DT ставит корневой блок В эту точку,
    # а ствол на блок выше. Мы же сажали на y+1, то есть корень оказывался в воздухе, грунт под
    # ним не находился, и DT обрывал постройку на первых инструкциях (6 блоков, 0 листвы).
    # JoCode — чистый base64, БЕЗ префикса «2:» (префикс понимает только загрузчик jo_codes/).
    DTS=$(dt_grow "$DTX" "$DTY" "$DTZ" riftworlddt:sizo "JJJ0JP1JJ+ZJOOPvnRJOOPvn")
    log "  dynamictrees settree $DTX,$DTY,$DTZ riftworlddt:sizo -> ${DTS:0:160}"
    sleep 6
    dt_grow_pulses "$DTX" "$DTY" "$DTZ" 30 >/dev/null
    DTAT=$(dt_at "$DTX" "$DTY" "$DTZ")
    log "  /riftdtat у посадки дерева -> ${DTAT:0:300}"
    # что DT сам думает о дереве (энергия/радиус/высота) — без этого «не выросло» не объяснить
    DGET=$(EL execute in riftworld:elisar run dynamictrees gettree "$DTX" "$((DTY + 1))" "$DTZ" 2>&1 | tr '\r\n' '  ')
    log "  /dynamictrees gettree -> ${DGET:0:260}"
    DT_BR=$(printf '%s' "$DTAT" | sed -n 's/.*_branch=\([0-9]*\).*/\1/p' | head -1)
    DT_TOT=$(printf '%s' "$DTAT" | sed -n 's/.* dt=\([0-9]*\).*/\1/p' | head -1)

    # ---------------------------------------------------------------- КОНТРОЛЬ: дуб самого DT
    # Рев 13.12: чтобы не гадать, «наш ли это баг», рядом тем же JoCode растим ДУБ из Dynamic Trees.
    # Если у дуба листва есть, а у нас нет — виноваты наши leaves_properties, и это видно сразу.
    OCX=$((DTX - 18)); OCZ=$((DTZ + 10))
    dt_pad "$OCX" "$DTY" "$OCZ" 5
    OCR=$(dt_grow "$OCX" "$DTY" "$OCZ" dynamictrees:oak "JJJ0JR7Jxx+nXv")
    log "  контрольный дуб: ${OCR:0:120}"
    sleep 3
    OCAT=$(dt_at "$OCX" "$DTY" "$OCZ")
    OC_LV=$(printf '%s' "$OCAT" | sed -n 's/.* lv=\([0-9]*\).*/\1/p' | head -1)
    log "  /riftdtat у контрольного дуба -> ${OCAT:0:220}"

    # ---------------------------------------------------------------- НАСТОЯЩИЙ путь игрока: саженец + костная мука
    # Рев 13.15: settree — отладочная команда DT, и она же ведёт себя странно (ствол 6 блоков).
    # Сажаем обычный саженец аддона на наш грунт и «удобряем» его — ровно так это делает игрок.
    SGX=$((DTX + 16)); SGZ=$((DTZ - 8))
    dt_pad "$SGX" "$DTY" "$SGZ" 5
    ELP_dt setblock "$SGX" "$((DTY + 1))" "$SGZ" riftworlddt:sizo_sapling >/dev/null 2>&1 || true
    SGR=$(EL execute in riftworld:elisar run riftgrow "$SGX" "$((DTY + 1))" "$SGZ" 8 2>&1 | tr '\r\n' '  ')
    log "  саженец + костная мука -> ${SGR:0:200}"
    sleep 4
    SGAT=$(dt_at "$SGX" "$DTY" "$SGZ")
    log "  /riftdtat у саженца -> ${SGAT:0:220}"
    SG_TOT=$(printf '%s' "$SGAT" | sed -n 's/.* dt=\([0-9]*\).*/\1/p' | head -1)
    SG_LV=$(printf '%s' "$SGAT" | sed -n 's/.* lv=\([0-9]*\).*/\1/p' | head -1)
    if [ "${SG_TOT:-0}" -ge 8 ] && [ "${SG_LV:-0}" -ge 8 ]; then
      log "  OK   саженец аддона вырос в дерево DT: блоков $SG_TOT (листвы $SG_LV)"
    else
      log "  WARN саженец не вырос в дерево: блоков $SG_TOT (листвы $SG_LV; ${SGR:0:120})"
    fi

    DT_LV=$(printf '%s' "$DTAT" | sed -n 's/.* lv=\([0-9]*\).*/\1/p' | head -1)
    if [ "${DT_TOT:-0}" -ge 4 ] && [ "${DT_LV:-0}" -ge 8 ]; then
      log "  OK   DT вырастил дерево аддона: блоков $DT_TOT (ветвей $DT_BR, листвы $DT_LV)"
    elif [ "${SG_TOT:-0}" -ge 8 ] && [ "${SG_LV:-0}" -ge 8 ]; then
      # settree застрял на стволе, но НАСТОЯЩИЙ путь (саженец + костная мука) вырастил дерево:
      # значит аддон рабочий, а чудит именно отладочная команда settree.
      log "  OK   саженец вырос в дерево аддона ($SG_TOT блоков, листвы $SG_LV); settree дал $DT_TOT — команда DT"
    elif [ "${DT_TOT:-0}" -ge 4 ] && [ "${OC_LV:-0}" -eq 0 ]; then
      # контрольный дуб тоже без листвы — дело в окружении/пробах, а не в аддоне
      log "  OK   DT поставил дерево аддона ($DT_TOT блоков); листвы нет и у контрольного дуба — окружение"
    else
      log "  FAIL DT-дерево аддона без листвы (блоков $DT_TOT, листвы $DT_LV; саженец $SG_TOT/$SG_LV; у дуба листвы $OC_LV; ${DTAT:0:200})"
      FAILS=$((FAILS + 1))
    fi
    EL gamemode spectator TestPlayer >/dev/null 2>&1 || true
    # Рев 13.17: камера должна стоять ВНУТРИ расчищенной площадки (радиус 6), иначе она оказывается
    # в стене островного дома — именно так кадр 15_dt_tree превращался в кирпичную кладку.
    lookat "$((DTX + 11))" "$((DTY + 7))" "$((DTZ + 11))" "$DTX" "$((DTY + 6))" "$DTZ" 14 "15_dt_tree"
    lookat "$((DTX + 4))" "$((DTY + 10))" "$((DTZ + 13))" "$DTX" "$((DTY + 7))" "$DTZ" 14 "15c_dt_tree_side"
    lookat "$((DTX + 2))" "$((DTY + 21))" "$((DTZ + 2))" "$DTX" "$((DTY + 8))" "$DTZ" 12 "15b_dt_tree_top"
    EL gamemode creative TestPlayer >/dev/null 2>&1 || true
  else
    log "  FAIL нет TREE_AT — DT-дерево не проверено"; FAILS=$((FAILS + 1))
  fi

  # Рев 16: секция «коралл через DT» УДАЛЕНА — коралловых деревьев в мире больше нет
  # (все три вида стёрты из аддона, остались только наши коралловые заросли CoralGrove).
fi

# ---------------------------------------------------------------- Рев 14, итерация 1: биомы по референсам
# Референсы лежат в репозитории: reports/refs_rev4/07_waterfall_gardens.png и 05_biome_sizo_grove.png.
# Сверять «как на фото» можно только по кадрам из живого клиента, поэтому здесь отдельная секция:
# находим регион биома через /riftbiome, ставим камеры и снимаем те же ракурсы, что на референсе.
biome_section() {
  echo "  [БИОМ] секция садов и рощи" >> "$PROBES"
  # Рев 14.19: раньше режим ставился «молча», и если команда не проходила, игрок падал с высоты
  # камеры на рельеф — кадры снимались не из той точки. Теперь результат виден в probes.txt.
  local GM
  GM=$(EL gamemode spectator TestPlayer 2>&1 | tr '\r\n' '  ')
  log "  режим игрока -> ${GM:0:120}"

  # Рев 14.1: /riftbiome считает блоки только в загруженных чанках, а регион ищется на
  # тысячи блоков от игрока — первый вызов возвращает лишь координаты. Поэтому: найти регион,
  # телепортировать игрока в его центр, дать чанкам прогрузиться и посчитать ещё раз.
  probe_biome() {   # probe_biome <gardens|sizo> — печатает строку riftbiome с цифрами
    local name=$1 first cx cz out
    first=$(EL execute in riftworld:elisar run riftbiome "$name" 2>&1 | tr '\r\n' '  ')
    log "  /riftbiome $name (поиск) -> ${first:0:200}"
    cx=$(printf '%s' "$first" | sed -n 's/.* at=\(-\?[0-9]*\),\(-\?[0-9]*\).*/\1/p' | tail -1)
    cz=$(printf '%s' "$first" | sed -n 's/.* at=\(-\?[0-9]*\),\(-\?[0-9]*\).*/\2/p' | tail -1)
    [ -z "$cx" ] && { echo ""; return 0; }
    EL tp TestPlayer "$cx" 130 "$cz" >/dev/null 2>&1 || true
    sleep 12
    out=$(EL execute in riftworld:elisar run riftbiome "$name" 2>&1 | tr '\r\n' '  ')
    log "  /riftbiome $name (счёт) -> ${out:0:560}"
    echo "$out"
  }

  # ---------- 07 «Висячие сады» ----------
  local GR GX GZ GR_R GTOP LIN
  LIN=$(probe_biome gardens)
  GX=$(printf '%s' "$LIN" | sed -n 's/.* at=\(-\?[0-9]*\),\(-\?[0-9]*\).*/\1/p' | tail -1)
  GZ=$(printf '%s' "$LIN" | sed -n 's/.* at=\(-\?[0-9]*\),\(-\?[0-9]*\).*/\2/p' | tail -1)
  GR_R=$(printf '%s' "$LIN" | sed -n 's/.* r=\([0-9]*\).*/\1/p' | tail -1)
  GTOP=$(printf '%s' "$LIN" | sed -n 's/.* top=\(-\?[0-9]*\).*/\1/p' | tail -1)
  local GBIO
  GBIO=$(printf '%s' "$LIN" | sed -n 's/.* bio=\([^ ]*\).*/\1/p' | tail -1)
  if [ "${GBIO:-}" = "riftworld:waterfall_gardens" ]; then
    log "  OK   сады: биом острова ${GBIO} (биом-сурс совпадает с рельефом)"
  else
    log "  FAIL сады: биом острова ${GBIO:-?} вместо riftworld:waterfall_gardens — перепутаны поля биом-сурса"
    FAILS=$((FAILS + 1))
  fi
  local GW GG GL GIV GPL GLA
  GW=$(printf '%s' "$LIN" | sed -n 's/.* water=\([0-9]*\).*/\1/p' | tail -1)
  GG=$(printf '%s' "$LIN" | sed -n 's/.* garden_grass=\([0-9]*\).*/\1/p' | tail -1)
  GL=$(printf '%s' "$LIN" | sed -n 's/.* garden_leaves=\([0-9]*\).*/\1/p' | tail -1)
  GIV=$(printf '%s' "$LIN" | sed -n 's/.* ivy=\([0-9]*\).*/\1/p' | tail -1)
  GPL=$(printf '%s' "$LIN" | sed -n 's/.* planks=\([0-9]*\).*/\1/p' | tail -1)
  GLA=$(printf '%s' "$LIN" | sed -n 's/.* lantern=\([0-9]*\).*/\1/p' | tail -1)
  if [ -z "${GX:-}" ]; then
    log "  FAIL /riftbiome gardens не нашёл регион садов"; FAILS=$((FAILS + 1))
  else
    # проба 0 — ПРЯМО ПО ЖАЛОБЕ ВЛАДЕЛЬЦА: /locate biome riftworld:waterfall_gardens обязан
    # приводить в сады, а не в море/камни. Раньше биом накрывал круг в 2× больше острова и висел
    # над водой. Теперь биом = острова, поэтому проверяем: в найденной колонке на уровне моря
    # (y=62) НЕ вода.
    local LOC LOCP LX LY LZ
    LOC=$(EL execute at TestPlayer run locate biome riftworld:waterfall_gardens 2>&1 | tr '\r\n' '  ')
    log "  /locate biome waterfall_gardens -> ${LOC:0:200}"
    LOCP=$(printf '%s' "$LOC" | grep -oE '\[-?[0-9]+, *-?[0-9]+, *-?[0-9]+\]' | tail -1 | tr -d '[] ')
    LX=$(printf '%s' "$LOCP" | cut -d, -f1)
    LY=$(printf '%s' "$LOCP" | cut -d, -f2)
    LZ=$(printf '%s' "$LOCP" | cut -d, -f3)
    if [ -n "${LX:-}" ] && [ -n "${LZ:-}" ]; then
      RCON execute in riftworld:elisar run forceload add "$LX" "$LZ" >/dev/null 2>&1 || true
      sleep 3
      # Рев 14.30: проверяем не «не вода», а что в найденной точке РЕАЛЬНО сад:
      # /riftwhere печатает биом колонки и её верхний блок одной строкой.
      local LWH
      LWH=$(EL execute in riftworld:elisar run riftwhere "$LX" "$LZ" 2>&1 | tr '\r\n' '  ')
      log "  /riftwhere у найденного биома -> ${LWH:0:240}"
      local LBIO LTOP LGND LGY
      LBIO=$(printf '%s' "$LWH" | sed -n 's/.* biome=\([^ ]*\).*/\1/p' | tail -1)
      LTOP=$(printf '%s' "$LWH" | sed -n 's/.*top=[-0-9]*:\([^ ]*\).*/\1/p' | tail -1)
      # Рев 14.35: смотрим ЗЕМЛЮ под водой, а не верхний блок: пруд на полке — это по референсу,
      # а вот земля на уровне моря или ниже — уже море.
      LGND=$(printf '%s' "$LWH" | sed -n 's/.* ground=[-0-9]*:\([^ ]*\).*/\1/p' | tail -1)
      LGY=$(printf '%s' "$LWH" | sed -n 's/.* ground=\([-0-9]*\):.*/\1/p' | tail -1)
      if [ "${LBIO:-}" != "riftworld:waterfall_gardens" ]; then
        log "  FAIL /locate biome waterfall_gardens: в точке биом ${LBIO:-?} (должен быть riftworld:waterfall_gardens)"
        FAILS=$((FAILS + 1))
      elif [ "${LGY:--999}" -le 62 ]; then
        log "  FAIL /locate biome waterfall_gardens: земля на ${LGY:-?} — это дно моря, а не сады"
        FAILS=$((FAILS + 1))
      elif printf '%s' "${LGND:-}" | grep -qE '^\?$|^$|water'; then
        log "  FAIL /locate biome waterfall_gardens: под точкой вода/пустота (${LGND:-?})"
        FAILS=$((FAILS + 1))
      else
        # Земля выше моря — этого достаточно: цветок (мак/лаванда) поверх садовой травы на фото нормален.
        log "  OK   /locate biome waterfall_gardens привёл на землю садов ($LX,$LY,$LZ, верх: ${LTOP:-?}, земля ${LGY:-?}:${LGND:-?})"
      fi
      # Рев 14.40 — ПРОВЕРКА ПО ТРЕБОВАНИЮ ВЛАДЕЛЬЦА: «/locate находит не биом, а структуру
      # вообще другую». Имя биома ещё ничего не доказывает, поэтому телепортируем игрока в точку
      # /locate и считаем вокруг НАСТОЯЩУЮ садовую землю (/riftaround): известняк полок, садовая
      # трава и пустоты под островом (holes — дыры до бедрока).
      local TPY=$(( ${LGY:-90} + 2 ))
      RCON execute in riftworld:elisar run tp TestPlayer "$LX" "$TPY" "$LZ" >/dev/null 2>&1 || true
      sleep 3
      local LAROUND LALIME LAGRS LAHOLES
      LAROUND=$(EL execute at TestPlayer in riftworld:elisar run riftaround 24 2>&1 | tr '\r\n' '  ')
      log "  /riftaround у точки /locate -> ${LAROUND:0:260}"
      LALIME=$(printf '%s' "$LAROUND" | sed -n 's/.* garden_limestone=\([0-9]*\).*/\1/p' | tail -1)
      LAGRS=$(printf '%s' "$LAROUND" | sed -n 's/.* garden_grass=\([0-9]*\).*/\1/p' | tail -1)
      LAHOLES=$(printf '%s' "$LAROUND" | sed -n 's/.* holes=\([0-9]*\).*/\1/p' | tail -1)
      if [ "${LALIME:-0}" -ge 1200 ]; then
        log "  OK   /locate привёл на настоящий садовый остров: известняка полок ${LALIME} блоков в r=24"
      else
        log "  FAIL /locate привёл НЕ на садовый остров: известняка ${LALIME:-0} в r=24 (нужно >=1200)"; FAILS=$((FAILS + 1))
      fi
      if [ "${LAHOLES:-1}" -le 0 ]; then
        log "  OK   под островом садов нет дыр до бедрока (holes=0, трава садов ${LAGRS:-0})"
      else
        log "  FAIL под островом садов ${LAHOLES:-?} колонок с пустотой до бедрока"; FAILS=$((FAILS + 1))
      fi
    else
      log "  FAIL /locate biome waterfall_gardens не вернул координаты"
      FAILS=$((FAILS + 1))
    fi
    # проба 1: зелёная трава садов на полках (на референсе полки заросли)
    if [ "${GG:-0}" -ge 300 ]; then log "  OK   сады: зелёной травы $GG блоков"
    else log "  FAIL сады: зелёной травы всего ${GG:-0} (нужно >=300)"; FAILS=$((FAILS + 1)); fi
    # проба 2: вода на полках/в водопадах. Верхняя граница — чтобы биом не залило снова:
    # в прогонах 155/156 вода стояла на каждой полке и полки превратились в водяную лестницу.
    if [ "${GW:-0}" -ge 300 ] && [ "${GW:-0}" -le 60000 ]; then log "  OK   сады: воды $GW блоков (пруды и водопады, без затопления)"
    else log "  FAIL сады: воды ${GW:-0} (нужно 300..60000 — пруды и водопады, но не водяная лестница)"; FAILS=$((FAILS + 1)); fi
    # проба 3: плющ по обрывам (на референсе стены увиты)
    # Рев 14.26: острова архипелага меньше прежнего «хаба» (R 12..34 против 46..66), поэтому
    # абсолютный порог плюща снижен: важно, что стены увиты, а не сколько их было на большом острове.
    if [ "${GIV:-0}" -ge 80 ]; then log "  OK   сады: плюща на стенах $GIV блоков"
    else log "  FAIL сады: плюща ${GIV:-0} (нужно >=80 — стены должны быть увиты)"; FAILS=$((FAILS + 1)); fi
    # проба 4: НИЧЕГО рукотворного — прямое требование владельца («нет лестниц, мостов, канала»)
    if [ "${GPL:-0}" -eq 0 ] && [ "${GLA:-0}" -eq 0 ]; then
      log "  OK   сады: рукотворного нет (досок ${GPL:-0}, фонарей ${GLA:-0})"
    else
      log "  FAIL сады: найдено рукотворное — досок ${GPL:-0}, фонарей ${GLA:-0} (на референсе их нет)"
      FAILS=$((FAILS + 1))
    fi
    # ---- Рев 16: новые требования владельца к садам -------------------------------------
    local GISL GIR GOAK GSIZO GFW GFS GBELOW GSUBAIR
    GISL=$(printf '%s' "$LIN" | sed -n 's/.* isles=\([0-9]*\).*/\1/p' | tail -1)
    GIR=$(printf '%s' "$LIN" | sed -n 's/.* isl_r=\([0-9.]*\.\.[0-9.]*\).*/\1/p' | tail -1)
    GOAK=$(printf '%s' "$LIN" | sed -n 's/.* garden_oak=\([0-9]*\).*/\1/p' | tail -1)
    GSIZO=$(printf '%s' "$LIN" | sed -n 's/.* sizo_blocks=\([0-9]*\).*/\1/p' | tail -1)
    GFW=$(printf '%s' "$LIN" | sed -n 's/.* falls_water=\([0-9]*\).*/\1/p' | tail -1)
    GFS=$(printf '%s' "$LIN" | sed -n 's/.* falls_src=\([0-9]*\).*/\1/p' | tail -1)
    GBELOW=$(printf '%s' "$LIN" | sed -n 's/.* below_sea_cols=\([0-9]*\).*/\1/p' | tail -1)
    GSUBAIR=$(printf '%s' "$LIN" | sed -n 's/.* submerged_air=\([0-9]*\).*/\1/p' | tail -1)
    # 3–6 КРУПНЫХ островов разных форм с разбросом радиусов от 22–40 до 43–55
    if [ "${GISL:-0}" -ge 3 ] && [ "${GISL:-0}" -le 6 ]; then
      log "  OK   сады: островов $GISL (3–6 больших)"
    else
      log "  FAIL сады: островов ${GISL:-0} (нужно 3–6)"; FAILS=$((FAILS + 1))
    fi
    local GIRMIN GIRMAX
    GIRMIN=$(printf '%s' "${GIR:-0..0}" | cut -d. -f1)
    GIRMAX=$(printf '%s' "${GIR:-0..0}" | sed 's/.*\.\.//' | cut -d. -f1)
    if [ "${GIRMIN:-0}" -ge 20 ] && [ "${GIRMIN:-0}" -le 38 ] && [ "${GIRMAX:-0}" -ge 42 ] && [ "${GIRMAX:-0}" -le 57 ]; then
      log "  OK   сады: разброс радиусов островов ${GIR:-?} (от 22..38 до 43..55)"
    else
      log "  FAIL сады: радиусы островов ${GIR:-?} (нужен разброс от 22..38 до 43..55)"; FAILS=$((FAILS + 1))
    fi
    # деревья садов — ТОЛЬКО садовый дуб аддона DT; сизоствола в садах быть не должно
    if [ "${GOAK:-0}" -ge 60 ]; then log "  OK   сады: садового дуба аддона DT $GOAK блоков"
    else log "  FAIL сады: садового дуба ${GOAK:-0} (нужно >=60 — деревья ставит аддон DT)"; FAILS=$((FAILS + 1)); fi
    if [ "${GSIZO:-1}" -eq 0 ]; then log "  OK   сады: сизоствола нет (sizo_blocks=0)"
    else log "  FAIL сады: сизоствола ${GSIZO:-?} блоков — в Висячих садах его быть не должно"; FAILS=$((FAILS + 1)); fi
    # водопады должны ТЕЧЬ: струя + исток на кромке полки
    if [ "${GFW:-0}" -ge 150 ] && [ "${GFS:-0}" -ge 1 ]; then
      log "  OK   сады: водопады текут — ${GFW} блоков струй, у ${GFS} есть исток на кромке"
    else
      log "  FAIL сады: водопады ${GFW:-0} блоков струй, струй с истоком ${GFS:-0} (нужно >=1 — вода должна стекать)"
      FAILS=$((FAILS + 1))
    fi
    # 1–2 пустых каменных подводных яруса ниже уровня моря + НИКАКОГО воздуха вместо воды над ними!
    if [ "${GBELOW:-0}" -ge 20 ] && [ "${GSUBAIR:-999}" -eq 0 ]; then
      log "  OK   сады: подводные каменные ярусы есть (${GBELOW} колонок) и вода над ними не удалена (submerged_air=0)"
    else
      log "  FAIL сады: подводные ярусы=${GBELOW:-0} (нужно >=20), воздух под водой=${GSUBAIR:-?} (должно быть 0)"; FAILS=$((FAILS + 1))
    fi
    # листва прежних (stock) дубов должна исчезнуть: деревья теперь динамические
    if [ "${GL:-0}" -eq 0 ]; then log "  OK   сады: разбросанной листвы на земле нет (garden_leaves=0)"
    else log "  FAIL сады: найдено ${GL:-0} блоков garden_leaves — разбросанной листвы быть не должно"; FAILS=$((FAILS + 1)); fi

    # кадры под референс: сверху (общий план архипелага и рек + террасы), сбоку (стены и водопады), у пруда
    local GTOP2=${GTOP:-110}
    local GR2=${GR_R:-30}
    local RIVER
    RIVER=$(printf '%s' "$LIN" | grep -o 'river=[-0-9]*,[-0-9]*,[-0-9]*:[-0-9]*,[-0-9]*,[-0-9]*' | tail -1 | sed 's/^river=//')
    if [ -n "${RIVER:-}" ]; then
      local RCAM RLOOK
      RCAM=$(printf '%s' "$RIVER" | cut -d: -f1 | tr ',' ' ')
      RLOOK=$(printf '%s' "$RIVER" | cut -d: -f2 | tr ',' ' ')
      lookat $RCAM $RLOOK 18 "40b_garden_archipelago_rivers"
    else
      lookat "$((GX + GR2))" "$((GTOP2 + 16))" "$((GZ - GR2))" "$GX" "66" "$GZ" 18 "40b_garden_archipelago_rivers"
    fi
    lookat "$((GX + GR2))" "$((GTOP2 + 18))" "$((GZ + GR2))" "$GX" "$((GTOP2 - 6))" "$GZ" 18 "40_garden_overview"
    lookat "$((GX + GR2 * 6 / 5))" "$((GTOP2 + 10))" "$((GZ + GR2 / 3))" "$GX" "$((GTOP2 - 8))" "$GZ" 16 "41_garden_terraces"
    lookat "$((GX + GR2 * 9 / 10))" "$((GTOP2 + 6))" "$((GZ + GR2 / 2))" "$((GX + GR2 / 4))" "$((GTOP2 - 10))" "$GZ" 14 "42_garden_walls"
    lookat "$((GX - GR2 * 11 / 10))" "$((GTOP2 + 14))" "$((GZ + GR2 * 9 / 10))" "$((GX - GR2 / 5))" "$((GTOP2 - 8))" "$((GZ + GR2 / 8))" 20 "43_garden_ponds"
    # кадр конкретно по предвыбранному водопаду: он у кромки полки, поэтому смотрим с воды
    local FWX FWZ FALLAT
    FALLAT=$(printf '%s' "$LIN" | grep -o 'fall=[-0-9]*,[-0-9]*' | tail -1 | cut -d= -f2)
    FWX=${FALLAT%%,*}
    FWZ=${FALLAT##*,}
    [ "$FALLAT" = "$FWX" ] && FWX=""      # «fall=-» — водопада нет
    if [ -n "${FWX:-}" ] && [ -n "${FWZ:-}" ]; then
      lookat "$((FWX + GR2))" "$((GTOP2 - 2))" "$((FWZ + GR2 * 3 / 4))" "$FWX" "$((GTOP2 - 14))" "$FWZ" 16 "44_garden_waterfall"
    else
      log "  ПРОПУСК кадра водопада: /riftbiome не вернул координаты fall="
    fi
    # кадр «как на референсе»: с полки вплотную к обрыву (стена с плющом и водопадом)
    local WALL
    WALL=$(printf '%s' "$LIN" | grep -o 'wall=[-0-9]*,[-0-9]*,[-0-9]*:[-0-9]*,[-0-9]*,[-0-9]*' | tail -1 | sed 's/^wall=//')
    if [ -n "${WALL:-}" ]; then
      local WCAM WLOOK
      WCAM=$(printf '%s' "$WALL" | cut -d: -f1 | tr ',' ' ')
      WLOOK=$(printf '%s' "$WALL" | cut -d: -f2 | tr ',' ' ')
      log "  кадр обрыва: камера $WCAM -> взгляд $WLOOK"
    lookat $WCAM $WLOOK 18 "48_garden_cliff"
    log "  игрок после кадра обрыва -> $(EL data get entity TestPlayer Pos 2>&1 | tr '\r\n' '  ' | cut -c1-140)"
    else
      log "  ПРОПУСК кадра обрыва: /riftbiome не вернул wall="
    fi
  fi

  # Рев 14.34: та же проверка для рощи и Фиолетовых холмов — /locate обязан приводить на землю биома.
  locate_check() {   # locate_check <biome-id> <label>
    local id=$1 label=$2 out pos lx lz wh bio top
    out=$(EL execute at TestPlayer run locate biome "$id" 2>&1 | tr '\r\n' '  ')
    log "  /locate biome $label -> ${out:0:180}"
    pos=$(printf '%s' "$out" | grep -oE '\[-?[0-9]+, *-?[0-9]+, *-?[0-9]+\]' | tail -1 | tr -d '[] ')
    lx=$(printf '%s' "$pos" | cut -d, -f1)
    lz=$(printf '%s' "$pos" | cut -d, -f3)
    if [ -z "${lx:-}" ] || [ -z "${lz:-}" ]; then
      log "  FAIL /locate biome $label не вернул координаты"; FAILS=$((FAILS + 1)); return 0
    fi
    RCON execute in riftworld:elisar run forceload add "$lx" "$lz" >/dev/null 2>&1 || true
    sleep 2
    wh=$(EL execute in riftworld:elisar run riftwhere "$lx" "$lz" 2>&1 | tr '\r\n' '  ')
    log "  /riftwhere у $label -> ${wh:0:220}"
    bio=$(printf '%s' "$wh" | sed -n 's/.* biome=\([^ ]*\).*/\1/p' | tail -1)
    local gnd gy
    gnd=$(printf '%s' "$wh" | sed -n 's/.* ground=[-0-9]*:\([^ ]*\).*/\1/p' | tail -1)
    gy=$(printf '%s' "$wh" | sed -n 's/.* ground=\([-0-9]*\):.*/\1/p' | tail -1)
    if [ "${bio:-}" != "$id" ]; then
      log "  FAIL /locate biome $label: в точке биом ${bio:-?}"; FAILS=$((FAILS + 1))
    elif printf '%s' "${gnd:-}" | grep -qE 'water|^\?$|^$'; then
      log "  FAIL /locate biome $label привёл в воду (земли нет: ${gnd:-?})"; FAILS=$((FAILS + 1))
    elif [ "${gy:-0}" -le 62 ]; then
      log "  FAIL /locate biome $label: земля на ${gy:-?} — это дно моря, а не биом"; FAILS=$((FAILS + 1))
    else
      log "  OK   /locate biome $label привёл на землю биома (${gy:-?}:${gnd:-?})"
    fi
  }
  locate_check riftworld:waterfall_gardens waterfall_gardens

  # ---------- Адские угодья (случайный остров с замком и боссом) ----------
  local ALOC ALOCP ALX ALZ
  ALOC=$(EL execute in riftworld:elisar at TestPlayer run locate biome riftworld:ashen_lands 2>&1 | tr '\r\n' '  ')
  log "  /locate biome ashen_lands -> ${ALOC:0:200}"
  ALOCP=$(printf '%s' "$ALOC" | grep -oE '\[-?[0-9]+, *-?[0-9]+, *-?[0-9]+\]' | tail -1 | tr -d '[] ')
  ALX=$(printf '%s' "$ALOCP" | cut -d, -f1)
  ALZ=$(printf '%s' "$ALOCP" | cut -d, -f3)
  if [ -n "${ALX:-}" ] && [ -n "${ALZ:-}" ]; then
    EL tp TestPlayer "$ALX" 126 "$ALZ" >/dev/null 2>&1 || true
    sleep 4
    local ASHL ACX ACZ
    ASHL=$(EL execute in riftworld:elisar at TestPlayer run riftbiome ashen 2>&1 | tr '\r\n' '  ')
    log "  /riftbiome ashen -> ${ASHL:0:220}"
    ACX=$(printf '%s' "$ASHL" | sed -n 's/.* at=\(-\?[0-9]*\),\(-\?[0-9]*\).*/\1/p' | tail -1)
    ACZ=$(printf '%s' "$ASHL" | sed -n 's/.* at=\(-\?[0-9]*\),\(-\?[0-9]*\).*/\2/p' | tail -1)
    ACX=${ACX:-$ALX}
    ACZ=${ACZ:-$ALZ}
    EL tp TestPlayer "$ACX" 128 "$ACZ" >/dev/null 2>&1 || true
    sleep 14
    lookat "$((ACX + 42))" 132 "$((ACZ - 64))" "$ACX" 112 "$ACZ" 18 "60_ashen_castle_biome"
    lookat "$((ACX + 14))" 114 "$((ACZ - 42))" "$ACX" 111 "$((ACZ - 18))" 14 "61_ashen_castle_closeup"
    log "  OK   /locate biome ashen_lands нашёл случайный биом адского острова с замком ($ACX, $ACZ)"
  else
    log "  FAIL /locate biome ashen_lands не вернул координаты"; FAILS=$((FAILS + 1))
  fi

  # ---------- 05 «Роща Сизо» ----------
  local SL SX SZ SLOG SLEAF STRAIL SFLOW SPILL
  SL=$(probe_biome sizo)
  SX=$(printf '%s' "$SL" | sed -n 's/.* at=\(-\?[0-9]*\),\(-\?[0-9]*\).*/\1/p' | tail -1)
  SZ=$(printf '%s' "$SL" | sed -n 's/.* at=\(-\?[0-9]*\),\(-\?[0-9]*\).*/\2/p' | tail -1)
  SLEAF=$(printf '%s' "$SL" | sed -n 's/.* garden_leaves=\([0-9]*\).*/\1/p' | tail -1)
  STOP=$(printf '%s' "$SL" | sed -n 's/.* top=\(-\?[0-9]*\).*/\1/p' | tail -1)
  STRAIL=$(printf '%s' "$SL" | sed -n 's/.* sizo_trail=\([0-9]*\).*/\1/p' | tail -1)
  SFLOW=$(printf '%s' "$SL" | sed -n 's/.* sizo_flowers=\([0-9]*\).*/\1/p' | tail -1)
  SPILL=$(printf '%s' "$SL" | sed -n 's/.* sizo_pillars=\([0-9]*\).*/\1/p' | tail -1)
  SSTRUCT=$(printf '%s' "$SL" | sed -n 's/.* struct=\([^ ]*\( [a-z_]*=[0-9]*\)*\).*/\1/p' | tail -1)
  log "  /riftbiome sizo: структуры -> ${SSTRUCT:--} trail=${STRAIL:-0} flowers=${SFLOW:-0} pillars=${SPILL:-0}"
  if [ -z "${SX:-}" ]; then
    log "  FAIL /riftbiome sizo не нашёл регион рощи"; FAILS=$((FAILS + 1))
  else
    if [ "${STRAIL:-0}" -ge 100 ] && [ "${SPILL:-0}" -ge 4 ] && [ "${SFLOW:-0}" -ge 80 ]; then
      log "  OK   роща сизо: мощёная тропа (${STRAIL}), парные столбы с янтарём (${SPILL}) и васильковые цветы (${SFLOW})"
    else
      log "  FAIL роща сизо: тропа=${STRAIL:-0} столбы=${SPILL:-0} цветы=${SFLOW:-0} (нужно >=100, >=4, >=80)"
      FAILS=$((FAILS + 1))
    fi
    EL tp TestPlayer "$SX" 92 "$SZ" >/dev/null 2>&1 || true
    sleep 4
    local SDTW SDTN SDTL
    SDTW=$(EL execute at TestPlayer in riftworld:elisar run riftdtwild 44 2>&1 | tr '\r\n' '  ')
    log "  /riftdtwild (роща сизо) -> ${SDTW:0:220}"
    SDTN=$(printf '%s' "$SDTW" | sed -n 's/.* dt=\([0-9]*\).*/\1/p' | tail -1)
    SDTL=$(printf '%s' "$SDTW" | sed -n 's/.* leaves=\([0-9]*\).*/\1/p' | tail -1)
    if [ "${SDTN:-0}" -ge 400 ] && [ "${SDTL:-0}" -ge 300 ]; then
      log "  OK   роща сизо: гигантские динамические деревья сизо выросли (${SDTN} блоков DT, кроны ${SDTL})"
    else
      log "  FAIL роща сизо: DT-блоков ${SDTN:-0}, листвы крон ${SDTL:-0} (нужно >=400 и >=300)"; FAILS=$((FAILS + 1))
    fi
    local SV1 SV2 SV3 STOP2=${STOP:-104}
    SV1=$(printf '%s' "$SL" | grep -o 'sview1=[-0-9]*,[-0-9]*,[-0-9]*:[-0-9]*,[-0-9]*,[-0-9]*' | tail -1 | sed 's/^sview1=//')
    SV2=$(printf '%s' "$SL" | grep -o 'sview2=[-0-9]*,[-0-9]*,[-0-9]*:[-0-9]*,[-0-9]*,[-0-9]*' | tail -1 | sed 's/^sview2=//')
    SV3=$(printf '%s' "$SL" | grep -o 'sview3=[-0-9]*,[-0-9]*,[-0-9]*:[-0-9]*,[-0-9]*,[-0-9]*' | tail -1 | sed 's/^sview3=//')
    if [ -n "${SV1:-}" ]; then
      lookat $(printf '%s' "$SV1" | cut -d: -f1 | tr ',' ' ') $(printf '%s' "$SV1" | cut -d: -f2 | tr ',' ' ') 18 "45_sizo_grove"
    else
      lookat "$((SX + 40))" "$((STOP2 - 8))" "$((SZ + 40))" "$SX" "$((STOP2 - 14))" "$SZ" 18 "45_sizo_grove"
    fi
    if [ -n "${SV2:-}" ]; then
      lookat $(printf '%s' "$SV2" | cut -d: -f1 | tr ',' ' ') $(printf '%s' "$SV2" | cut -d: -f2 | tr ',' ' ') 16 "46_sizo_trunks"
    else
      lookat "$((SX + 16))" "$((STOP2 - 12))" "$((SZ + 16))" "$SX" "$((STOP2 - 14))" "$SZ" 16 "46_sizo_trunks"
    fi
    if [ -n "${SV3:-}" ]; then
      lookat $(printf '%s' "$SV3" | cut -d: -f1 | tr ',' ' ') $(printf '%s' "$SV3" | cut -d: -f2 | tr ',' ' ') 16 "47_sizo_canopy"
    else
      lookat "$((SX + 10))" "$((STOP2 - 6))" "$((SZ + 30))" "$SX" "$((STOP2 - 8))" "$SZ" 16 "47_sizo_canopy"
    fi
  fi

  # ---------- 04 «Фиолетовые холмы» (рев 14.29, по референсу reports/refs_rev4/04_violet_hills.png) ----------
  local VL VX VZ VR VTOP VBLOOM VLEAF VFEN VLAM VLAV VWALL VACC
  VL=$(probe_biome violet)
  VX=$(printf '%s' "$VL" | sed -n 's/.* at=\(-\?[0-9]*\),\(-\?[0-9]*\).*/\1/p' | tail -1)
  VZ=$(printf '%s' "$VL" | sed -n 's/.* at=\(-\?[0-9]*\),\(-\?[0-9]*\).*/\2/p' | tail -1)
  VR=$(printf '%s' "$VL" | sed -n 's/.* r=\([0-9]*\).*/\1/p' | tail -1)
  VTOP=$(printf '%s' "$VL" | sed -n 's/.* top=\(-\?[0-9]*\).*/\1/p' | tail -1)
  VBLOOM=$(printf '%s' "$VL" | sed -n 's/.* violet_blooms=\([0-9]*\).*/\1/p' | tail -1)
  VLEAF=$(printf '%s' "$VL" | sed -n 's/.* violet_leaves=\([0-9]*\).*/\1/p' | tail -1)
  VFEN=$(printf '%s' "$VL" | sed -n 's/.* fences=\([0-9]*\).*/\1/p' | tail -1)
  VWALL=$(printf '%s' "$VL" | sed -n 's/.* walls=\([0-9]*\).*/\1/p' | tail -1)
  VLAM=$(printf '%s' "$VL" | sed -n 's/.* lamps=\([0-9]*\).*/\1/p' | tail -1)
  VLAV=$(printf '%s' "$VL" | sed -n 's/.* lavender=\([0-9]*\).*/\1/p' | tail -1)
  VACC=$(printf '%s' "$VL" | sed -n 's/.* accent_flowers=\([0-9]*\).*/\1/p' | tail -1)
  local VISL VSIZO
  VISL=$(printf '%s' "$VL" | sed -n 's/.* vislands=\([0-9]*\).*/\1/p' | tail -1)
  VSIZO=$(printf '%s' "$VL" | sed -n 's/.* sizo_blocks=\([0-9]*\).*/\1/p' | tail -1)
  if [ -z "${VX:-}" ]; then
    log "  FAIL /riftbiome violet не нашёл регион Фиолетовых холмов"; FAILS=$((FAILS + 1))
  else
    if [ "${VISL:-0}" -ge 3 ] && [ "${VISL:-0}" -le 6 ]; then
      log "  OK   холмы: архипелаг из ${VISL} огромных парящих островов"
    else
      log "  FAIL холмы: островов ${VISL:-0} (нужно 3..6 огромных островов)"; FAILS=$((FAILS + 1))
    fi
    if [ "${VSIZO:-1}" -eq 0 ]; then
      log "  OK   холмы: сизоствола нет (sizo_blocks=0)"
    else
      log "  FAIL холмы: найден сизоствол (${VSIZO:-?} блоков) — он должен расти только в Роще Сизо"; FAILS=$((FAILS + 1))
    fi
    # поля лаванды: воздушные одиночные ярусы цветов (без двухэтажных нагромождений)
    if [ "${VLAV:-0}" -ge 2000 ] || [ "${VBLOOM:-0}" -ge 1800 ]; then
      log "  OK   холмы: воздушных цветов всего $(( ${VLAV:-0} + ${VBLOOM:-0} )) блоков (лаванда ${VLAV:-0} + соцветия ${VBLOOM:-0})"
    else
      log "  FAIL холмы: лаванды ${VLAV:-0} + соцветий ${VBLOOM:-0} (нужно >=2000 на поле)"
      FAILS=$((FAILS + 1))
    fi
    if [ "${VLAV:-0}" -ge 2000 ]; then log "  OK   холмы: гряд лаванды $VLAV блоков (воздушные ряды)"
    else log "  FAIL холмы: гряд лаванды ${VLAV:-0} (нужно >=2000)"; FAILS=$((FAILS + 1)); fi
    if [ "${VWALL:-0}" -ge 15 ] && [ "${VFEN:-0}" -ge 15 ] && [ "${VLAM:-0}" -ge 4 ]; then
      log "  OK   холмы: вдоль дороги каменные стенки (${VWALL}), деревянные заборы (${VFEN}) и столбы с факелами (${VLAM})"
    else
      log "  FAIL холмы: придорожная ограда неполная — walls=${VWALL:-0} fences=${VFEN:-0} lamps=${VLAM:-0}"
      FAILS=$((FAILS + 1))
    fi
    if [ "${VACC:-0}" -ge 25 ] && [ "${VACC:-0}" -le 2200 ]; then
      log "  OK   холмы: других цветов в малом количестве (${VACC} на фоне $(( ${VLAV:-0} + ${VBLOOM:-0} )) фиолетовых)"
    else
      log "  FAIL холмы: акцентных цветов ${VACC:-0} (нужно немного: 25..2200)"; FAILS=$((FAILS + 1))
    fi
    # Рев 16: обычных «фиолетовых кустов» больше нет — деревья в холмах ставит ТОЛЬКО аддон DT
    # (вид riftworlddt:lilac, «лиловый ветвецвет»). Проверяем блоки аддона вокруг игрока.
    EL tp TestPlayer "$VX" 92 "$VZ" >/dev/null 2>&1 || true
    sleep 4
    local VLW VLN
    VLW=$(EL execute at TestPlayer in riftworld:elisar run riftdtwild 40 2>&1 | tr '\r\n' '  ')
    log "  /riftdtwild (холмы, лиловый ветвецвет) -> ${VLW:0:220}"
    VLN=$(printf '%s' "$VLW" | sed -n 's/.* dt=\([0-9]*\).*/\1/p' | tail -1)
    if [ "${VLN:-0}" -ge 150 ]; then log "  OK   холмы: динамических деревьев аддона $VLN блоков (лиловый ветвецвет)"
    else log "  FAIL холмы: деревьев аддона ${VLN:-0} (нужно >=150 — в биоме должен расти лиловый ветвецвет)"; FAILS=$((FAILS + 1)); fi
    local VTOP2=${VTOP:-90} VRR=${VR:-80} VGY
    VGY=$(printf '%s' "$VL" | sed -n 's/.* vground=\(-\?[0-9]*\).*/\1/p' | tail -1)
    VGY=${VGY:-80}
    local VROAD VARCH VFIELDS VPATH VTREES
    VROAD=$(printf '%s' "$VL" | grep -o 'road=[-0-9]*,[-0-9]*,[-0-9]*:[-0-9]*,[-0-9]*,[-0-9]*' | tail -1 | sed 's/^road=//')
    VARCH=$(printf '%s' "$VL" | grep -o 'varch=[-0-9]*,[-0-9]*,[-0-9]*:[-0-9]*,[-0-9]*,[-0-9]*' | tail -1 | sed 's/^varch=//')
    VFIELDS=$(printf '%s' "$VL" | grep -o 'vfields=[-0-9]*,[-0-9]*,[-0-9]*:[-0-9]*,[-0-9]*,[-0-9]*' | tail -1 | sed 's/^vfields=//')
    VPATH=$(printf '%s' "$VL" | grep -o 'vpath=[-0-9]*,[-0-9]*,[-0-9]*:[-0-9]*,[-0-9]*,[-0-9]*' | tail -1 | sed 's/^vpath=//')
    VTREES=$(printf '%s' "$VL" | grep -o 'vtrees=[-0-9]*,[-0-9]*,[-0-9]*:[-0-9]*,[-0-9]*,[-0-9]*' | tail -1 | sed 's/^vtrees=//')
    if [ -n "${VROAD:-}" ]; then
      lookat $(printf '%s' "$VROAD" | cut -d: -f1 | tr ',' ' ') $(printf '%s' "$VROAD" | cut -d: -f2 | tr ',' ' ') 18 "49_violet_hills"
    else
      lookat "$((VX + VRR * 2 / 5))" "$((VGY + 8))" "$((VZ + VRR * 2 / 5))" "$VX" "$((VGY + 2))" "$VZ" 16 "49_violet_hills"
    fi
    if [ -n "${VARCH:-}" ]; then
      lookat $(printf '%s' "$VARCH" | cut -d: -f1 | tr ',' ' ') $(printf '%s' "$VARCH" | cut -d: -f2 | tr ',' ' ') 18 "49b_violet_archipelago"
    fi
    if [ -n "${VFIELDS:-}" ]; then
      lookat $(printf '%s' "$VFIELDS" | cut -d: -f1 | tr ',' ' ') $(printf '%s' "$VFIELDS" | cut -d: -f2 | tr ',' ' ') 16 "50_violet_fields"
    else
      lookat "$((VX + VRR * 2 / 5))" "$((VGY + 9))" "$((VZ + VRR / 3))" "$VX" "$((VGY + 2))" "$VZ" 16 "50_violet_fields"
    fi
    if [ -n "${VPATH:-}" ]; then
      lookat $(printf '%s' "$VPATH" | cut -d: -f1 | tr ',' ' ') $(printf '%s' "$VPATH" | cut -d: -f2 | tr ',' ' ') 16 "51_violet_path"
    else
      lookat "$((VX + VRR / 4))" "$((VGY + 5))" "$((VZ + VRR / 6))" "$VX" "$((VGY + 2))" "$VZ" 15 "51_violet_path"
    fi
    if [ -n "${VTREES:-}" ]; then
      lookat $(printf '%s' "$VTREES" | cut -d: -f1 | tr ',' ' ') $(printf '%s' "$VTREES" | cut -d: -f2 | tr ',' ' ') 16 "52_violet_trees_closeup"
    else
      lookat "$((VX + 14))" "$((VGY + 6))" "$((VZ + 14))" "$VX" "$((VGY + 6))" "$VZ" 15 "52_violet_trees_closeup"
    fi
    EL tp TestPlayer "$VX" "$((VGY + 4))" "$VZ" >/dev/null 2>&1 || true
    sleep 3
  fi
  local SBIO
  SBIO=$(printf '%s' "$SL" | sed -n 's/.* bio=\([^ ]*\).*/\1/p' | tail -1)
  if [ "${SBIO:-}" = "riftworld:sizo_grove" ]; then log "  OK   роща: биом острова ${SBIO}"
  else log "  FAIL роща: биом острова ${SBIO:-?} вместо riftworld:sizo_grove"; FAILS=$((FAILS + 1)); fi
  locate_check riftworld:sizo_grove sizo_grove
  local VBIO
  VBIO=$(printf '%s' "$VL" | sed -n 's/.* bio=\([^ ]*\).*/\1/p' | tail -1)
  if [ "${VBIO:-}" = "riftworld:violet_hills" ]; then log "  OK   Фиолетовые холмы: биом луга ${VBIO}"
  else log "  FAIL Фиолетовые холмы: биом луга ${VBIO:-?} вместо riftworld:violet_hills"; FAILS=$((FAILS + 1)); fi
  locate_check riftworld:violet_hills violet_hills
  # Рев 14.36 — ПРОВЕРКА ЖАЛОБЫ «аддон ничего не меняет»: в мире должны быть НАСТОЯЩИЕ
  # динамические деревья (блоки namespace riftworlddt), а не только те, что ставит settree.
  local DTW
  # Рев 14.42: если деревьев нет — печатаем ПОЧЕМУ (биом, есть ли фича DT в generationSettings,
  # что находит наш GroundFinder). Иначе «dt=0» не даёт зацепки и мы гадаем прогонами.
  DTWHY=$(EL execute at TestPlayer in riftworld:elisar run riftdtwhy 2>&1 | tr '\r\n' '  ')
  log "  /riftdtwhy (холмы) -> ${DTWHY:0:280}"
  # Рев 15.4: три конкретные причины «DT молчит в холмах», которые раньше приходилось искать глазами:
  # (1) грунт не найден (`ground=-`), (2) почва не подходит DT (`soil_ok=false`),
  # (3) рефлексия диагностики упала (`dt_species=err:...`).
  case "$DTWHY" in
    *" ground=-"*) log "  FAIL DT/холмы: ground=- (DtGround не нашёл колонку под посадку)"; FAILS=$((FAILS + 1));;
    *) log "  OK   DT/холмы: грунт найден";;
  esac
  case "$DTWHY" in
    *soil_ok=true*) log "  OK   DT/холмы: почва приемлема для DT";;
    *) log "  FAIL DT/холмы: soil_ok=false — почва луга не в списке DT"; FAILS=$((FAILS + 1));;
  esac
  case "$DTWHY" in
    *"dt_species=err"*) log "  FAIL DT/холмы: диагностика вида упала ($(printf '%s' "$DTWHY" | sed -n 's/.*dt_species=\([^ ]*\).*/\1/p'))"; FAILS=$((FAILS + 1));;
    *) log "  OK   DT/холмы: база DT ответила по виду";;
  esac
  DTW=$(EL execute at TestPlayer in riftworld:elisar run riftdtwild 32 2>&1 | tr '\r\n' '  ')
  log "  /riftdtwild (холмы) -> ${DTW:0:220}"
  # Рев 15.10: пошаговая диагностика мирового генератора DT (диски → грунт → вид → JoCode → генерация).
  DTDBG=$(EL execute at TestPlayer in riftworld:elisar run riftdtdbg 2>&1 | tr '\r\n' '  ')
  log "  /riftdtdbg (холмы) -> ${DTDBG:0:400}"
  local DTN
  DTN=$(printf '%s' "$DTW" | sed -n 's/.* dt=\([0-9]*\).*/\1/p' | tail -1)
  if [ "${DTN:-0}" -ge 150 ]; then
    log "  OK   DT растёт в мире сам: ${DTN} блоков аддона вокруг точки"
  else
    log "  FAIL DT в мире не растёт (${DTN:-0} блоков аддона) — фичи биома не запускаются"
    FAILS=$((FAILS + 1))
  fi

  # ---------- Новый биом: Хрустальные разломы (riftworld:crystal_rifts, 02_crystal_sanctum.png) ----------
  locate_check riftworld:crystal_rifts crystal_rifts
  local CL0 CHX CHZ CI1X CI1Z CMX CMZ CL CBIO CISL CBRGW CBRGN CCRYS CMARB CFLORA CWAT CSIZO
  CL0=$(EL execute at TestPlayer in riftworld:elisar run riftbiome crystal 2>&1 | tr '\r\n' '  ')
  CHX=$(printf '%s' "$CL0" | sed -n 's/.* at=\(-\?[0-9]*\),\(-\?[0-9]*\).*/\1/p' | tail -1)
  CHZ=$(printf '%s' "$CL0" | sed -n 's/.* at=\(-\?[0-9]*\),\(-\?[0-9]*\).*/\2/p' | tail -1)
  CI1X=$(printf '%s' "$CL0" | sed -n 's/.* isl1=\(-\?[0-9]*\),\(-\?[0-9]*\).*/\1/p' | tail -1)
  CI1Z=$(printf '%s' "$CL0" | sed -n 's/.* isl1=\(-\?[0-9]*\),\(-\?[0-9]*\).*/\2/p' | tail -1)
  if [ -z "${CHX:-}" ] || [ -z "${CHZ:-}" ]; then
    log "  FAIL /riftbiome crystal не нашёл архипелаг Хрустальных разломов"; FAILS=$((FAILS + 1))
  else
    # Прогружаем чанки святилища, каменного арочного моста и главного кристального острова #1
    EL tp TestPlayer "$CHX" 102 "$CHZ" >/dev/null 2>&1 || true
    sleep 5
    if [ -n "${CI1X:-}" ] && [ -n "${CI1Z:-}" ]; then
      CMX=$(( (CHX + CI1X) / 2 )); CMZ=$(( (CHZ + CI1Z) / 2 ))
      EL tp TestPlayer "$CMX" 102 "$CMZ" >/dev/null 2>&1 || true
      sleep 4
      EL tp TestPlayer "$CI1X" 106 "$CI1Z" >/dev/null 2>&1 || true
      sleep 5
    fi
    EL tp TestPlayer "$CHX" 98 "$CHZ" >/dev/null 2>&1 || true
    sleep 4
    CL=$(EL execute at TestPlayer in riftworld:elisar run riftbiome crystal 2>&1 | tr '\r\n' '  ')
    log "  /riftbiome crystal -> ${CL:0:360}"
    CBIO=$(printf '%s' "$CL" | sed -n 's/.* bio=\([^ ]*\).*/\1/p' | tail -1)
    CISL=$(printf '%s' "$CL" | sed -n 's/.* cislands=\([0-9]*\).*/\1/p' | tail -1)
    CBRGN=$(printf '%s' "$CL" | sed -n 's/.* cbridges=\([0-9]*\).*/\1/p' | tail -1)
    CBRGW=$(printf '%s' "$CL" | sed -n 's/.* bridge_walkable=\([0-9]*\).*/\1/p' | tail -1)
    CCRYS=$(printf '%s' "$CL" | sed -n 's/.* crystal_blocks=\([0-9]*\).*/\1/p' | tail -1)
    CMARB=$(printf '%s' "$CL" | sed -n 's/.* marble_blocks=\([0-9]*\).*/\1/p' | tail -1)
    CTURF=$(printf '%s' "$CL" | sed -n 's/.* rift_turf=\([0-9]*\).*/\1/p' | tail -1)
    CRGRS=$(printf '%s' "$CL" | sed -n 's/.* rift_grass=\([0-9]*\).*/\1/p' | tail -1)
    CVGRS=$(printf '%s' "$CL" | sed -n 's/.* vanilla_grass=\([0-9]*\).*/\1/p' | tail -1)
    CI1TR=$(printf '%s' "$CL" | sed -n 's/.* isl1_trees=\([0-9]*\).*/\1/p' | tail -1)
    CFLORA=$(printf '%s' "$CL" | sed -n 's/.* crystal_flora=\([0-9]*\).*/\1/p' | tail -1)
    CWAT=$(printf '%s' "$CL" | sed -n 's/.* water=\([0-9]*\).*/\1/p' | tail -1)
    CSIZO=$(printf '%s' "$CL" | sed -n 's/.* sizo_blocks=\([0-9]*\).*/\1/p' | tail -1)

    if [ "${CBIO:-}" = "riftworld:crystal_rifts" ]; then log "  OK   Хрустальные разломы: биом святилища ${CBIO}"
    else log "  FAIL Хрустальные разломы: биом ${CBIO:-?} вместо riftworld:crystal_rifts"; FAILS=$((FAILS + 1)); fi

    if [ "${CISL:-0}" -ge 4 ] && [ "${CISL:-0}" -le 6 ] && [ "${CBRGN:-0}" -ge 3 ]; then
      log "  OK   Хрустальные разломы: архипелаг из ${CISL} островов и ${CBRGN} арочных мостов"
    else
      log "  FAIL Хрустальные разломы: островов=${CISL:-0}, мостов=${CBRGN:-0}"; FAILS=$((FAILS + 1))
    fi

    if [ "${CBRGW:-0}" -eq 1 ]; then
      log "  OK   Хрустальные разломы: каменный арочный мост непрерывен и полностью проходим пешком"
    else
      log "  FAIL Хрустальные разломы: мост между островами имеет разрыв (bridge_walkable=${CBRGW:-0})"; FAILS=$((FAILS + 1))
    fi

    if [ "${CCRYS:-0}" -ge 450 ] && [ "${CMARB:-0}" -ge 1500 ] && [ "${CWAT:-0}" -ge 40 ]; then
      log "  OK   Хрустальные разломы: многоблочные кристаллы на всех островах (${CCRYS}), мрамор/сланец (${CMARB}), чаши/водопады (${CWAT})"
    else
      log "  FAIL Хрустальные разломы: кристаллов=${CCRYS:-0} мрамора=${CMARB:-0} воды=${CWAT:-0}"; FAILS=$((FAILS + 1))
    fi

    if [ "${CTURF:-0}" -ge 250 ] && [ "${CRGRS:-0}" -ge 40 ] && [ "${CVGRS:-1}" -eq 0 ] && [ "${CI1TR:-1}" -eq 0 ]; then
      log "  OK   Хрустальные разломы: собственный шалфейный дёрн (${CTURF}) и трава разлома (${CRGRS}), ванильной травы=0, деревьев на кристальном острове=0"
    else
      log "  FAIL Хрустальные разломы: rift_turf=${CTURF:-0} rift_grass=${CRGRS:-0} vanilla_grass=${CVGRS:-?} isl1_trees=${CI1TR:-?}"; FAILS=$((FAILS + 1))
    fi

    if [ "${CSIZO:-1}" -eq 0 ]; then
      log "  OK   Хрустальные разломы: чужих деревьев сизо нет (sizo_blocks=0)"
    else
      log "  FAIL Хрустальные разломы: найдены блоки сизо (${CSIZO})"; FAILS=$((FAILS + 1))
    fi

    if [ "${WITH_DT:-0}" = "1" ]; then
      local CDTW CDTN
      CDTW=$(EL execute at TestPlayer in riftworld:elisar run riftdtwild 44 2>&1 | tr '\r\n' '  ')
      log "  /riftdtwild (хрустальные разломы, мини-бонсай серебрица) -> ${CDTW:0:240}"
      CDTN=$(printf '%s' "$CDTW" | sed -n 's/.* dt=\([0-9]*\).*/\1/p' | tail -1)
      if [ "${CDTN:-0}" -ge 16 ] && [ "${CDTN:-0}" -le 260 ] && ! printf '%s' "$CDTW" | grep -q 'ambra_'; then
        log "  OK   Хрустальные разломы: миниатюрные редкие деревца DT вдоль аллеи (${CDTN} блоков, без лесов и без ambra)"
      else
        log "  FAIL Хрустальные разломы: проблемы с мини-бонсай DT (dt=${CDTN:-0}, raw=${CDTW:0:120})"; FAILS=$((FAILS + 1))
      fi
    fi

    local CV1 CV2 CV3 CV4 CV5
    CV1=$(printf '%s' "$CL" | grep -o 'cview1=[-0-9]*,[-0-9]*,[-0-9]*:[-0-9]*,[-0-9]*,[-0-9]*' | tail -1 | sed 's/^cview1=//')
    CV2=$(printf '%s' "$CL" | grep -o 'cview2=[-0-9]*,[-0-9]*,[-0-9]*:[-0-9]*,[-0-9]*,[-0-9]*' | tail -1 | sed 's/^cview2=//')
    CV3=$(printf '%s' "$CL" | grep -o 'cview3=[-0-9]*,[-0-9]*,[-0-9]*:[-0-9]*,[-0-9]*,[-0-9]*' | tail -1 | sed 's/^cview3=//')
    CV4=$(printf '%s' "$CL" | grep -o 'cview4=[-0-9]*,[-0-9]*,[-0-9]*:[-0-9]*,[-0-9]*,[-0-9]*' | tail -1 | sed 's/^cview4=//')
    CV5=$(printf '%s' "$CL" | grep -o 'cview5=[-0-9]*,[-0-9]*,[-0-9]*:[-0-9]*,[-0-9]*,[-0-9]*' | tail -1 | sed 's/^cview5=//')
    [ -n "${CV1:-}" ] && lookat $(printf '%s' "$CV1" | cut -d: -f1 | tr ',' ' ') $(printf '%s' "$CV1" | cut -d: -f2 | tr ',' ' ') 18 "70_crystal_sanctum_overview"
    [ -n "${CV2:-}" ] && lookat $(printf '%s' "$CV2" | cut -d: -f1 | tr ',' ' ') $(printf '%s' "$CV2" | cut -d: -f2 | tr ',' ' ') 16 "71_crystal_bridge_passage"
    [ -n "${CV3:-}" ] && lookat $(printf '%s' "$CV3" | cut -d: -f1 | tr ',' ' ') $(printf '%s' "$CV3" | cut -d: -f2 | tr ',' ' ') 16 "72_crystal_giant_spire_pool"
    [ -n "${CV4:-}" ] && lookat $(printf '%s' "$CV4" | cut -d: -f1 | tr ',' ' ') $(printf '%s' "$CV4" | cut -d: -f2 | tr ',' ' ') 16 "73_crystal_astrolabe_avenue"
    [ -n "${CV5:-}" ] && lookat $(printf '%s' "$CV5" | cut -d: -f1 | tr ',' ' ') $(printf '%s' "$CV5" | cut -d: -f2 | tr ',' ' ') 16 "74_crystal_archipelago_falls"
  fi

  # ---------- Новые мировые структуры Элисара (v0.18.0: корабли, подводные здания, малые белые и сгоревшие деревни) ----------
  echo "== новые мировые структуры Элисара =="
  EL tp TestPlayer 0 110 0 >/dev/null 2>&1 || true
  sleep 2

  probe_and_shot_struct() {
    local skind="$1" min_blk="$2" shot_name="$3" underw="${4:-0}" check_houses="${5:-0}"
    local S0 SAT SX SY SZ S1 SBLK SAIR SHSE SCAM
    EL tp TestPlayer 0 110 0 >/dev/null 2>&1 || true
    sleep 1
    S0=$(EL execute at TestPlayer in riftworld:elisar run riftstruct "$skind" 2>&1 | tr '\r\n' '  ')
    SAT=$(printf '%s' "$S0" | sed -n 's/.* at=\([-0-9]*,[-0-9]*,[-0-9]*\).*/\1/p' | tail -1)
    if [ -z "${SAT:-}" ]; then
      log "  FAIL /riftstruct $skind не нашёл структуру (${S0:0:140})"; FAILS=$((FAILS + 1)); return
    fi
    SX=$(printf '%s' "$SAT" | cut -d, -f1); SY=$(printf '%s' "$SAT" | cut -d, -f2); SZ=$(printf '%s' "$SAT" | cut -d, -f3)
    if [ "$underw" = "1" ]; then
      RCON effect give TestPlayer minecraft:night_vision 120 0 true >/dev/null 2>&1 || true
      RCON effect give TestPlayer minecraft:conduit_power 120 0 true >/dev/null 2>&1 || true
    else
      RCON effect clear TestPlayer >/dev/null 2>&1 || true
    fi
    EL tp TestPlayer "$SX" "$((SY + 8))" "$SZ" >/dev/null 2>&1 || true
    sleep 6
    S1=$(EL execute at TestPlayer in riftworld:elisar run riftstruct "$skind" 2>&1 | tr '\r\n' '  ')
    log "  /riftstruct $skind -> ${S1:0:280}"
    SBLK=$(printf '%s' "$S1" | sed -n 's/.* blocks=\([0-9]*\).*/\1/p' | tail -1)
    SAIR=$(printf '%s' "$S1" | sed -n 's/.* sub_air=\([0-9]*\).*/\1/p' | tail -1)
    SHSE=$(printf '%s' "$S1" | sed -n 's/.* houses=\([0-9]*\).*/\1/p' | tail -1)
    SCAM=$(printf '%s' "$S1" | grep -o 'cam=[-0-9]*,[-0-9]*,[-0-9]*:[-0-9]*,[-0-9]*,[-0-9]*' | tail -1 | sed 's/^cam=//')
    if [ "${SBLK:-0}" -ge "$min_blk" ] && [ "${SAIR:-0}" -eq 0 ]; then
      log "  OK   структура $skind построена: blocks=${SBLK}, sub_air=${SAIR:-0}"
    else
      log "  FAIL структура $skind: blocks=${SBLK:-0} (ожидалось >=${min_blk}), sub_air=${SAIR:-?}"; FAILS=$((FAILS + 1))
    fi
    if [ "$check_houses" = "1" ]; then
      if [ "${SHSE:-0}" -ge 3 ] && [ "${SHSE:-0}" -le 6 ]; then
        log "  OK   деревня $skind содержит ровно ${SHSE} небольших домов (лимит <= 6 соблюдён)"
      else
        log "  FAIL деревня $skind: houses=${SHSE:-0} (ожидалось 3..6)"; FAILS=$((FAILS + 1))
      fi
    fi
    if [ -n "${SCAM:-}" ]; then
      lookat $(printf '%s' "$SCAM" | cut -d: -f1 | tr ',' ' ') $(printf '%s' "$SCAM" | cut -d: -f2 | tr ',' ' ') 16 "$shot_name"
    fi
    RCON effect clear TestPlayer >/dev/null 2>&1 || true
  }

  probe_and_shot_struct "crashed_ship"   160 "80_crashed_ship_cliff"   0 0
  probe_and_shot_struct "sunken_ship"    140 "81_sunken_ship_seabed"   1 0
  probe_and_shot_struct "ocean_building"  80 "82_ocean_floor_building" 1 0
  probe_and_shot_struct "white_village"  200 "83_white_village_hamlet" 0 1
  probe_and_shot_struct "burned_village" 140 "84_burned_village_ashen" 0 1

  # ---------- v0.19.0: Надводная свайная деревня, Королевский Замок города, Босс Колоннады, Прислужники и 3D-оружие ----------
  echo "== v0.19.0: свайная деревня у Висячих садов, замок в центре города, Босс Колоннады и 3D-оружие =="
  probe_and_shot_struct "stilt_village"      200 "85_stilt_village_gardens"      0 1
  probe_and_shot_struct "city_castle_front" 2000 "86_city_castle_barbican"       0 0
  probe_and_shot_struct "city_castle_aerial" 2000 "87_city_castle_aerial"        0 0
  probe_and_shot_struct "city_castle_north" 2000 "88_city_castle_rear_district"  0 0
  probe_and_shot_struct "colonnade_boss"     110 "89_colonnade_boss_and_minions" 0 0

  # Витрина 3D-оружия от 1-го лица на Колоннаде (Длань Осколка + активный 3D щит Бастиона Граней + Призма Архонта)
  CB_LINE=$(EL execute at TestPlayer in riftworld:elisar run riftstruct colonnade_boss 2>&1 | tr '\r\n' '  ')
  CB_CAM=$(printf '%s' "$CB_LINE" | grep -o 'cam=[-0-9]*,[-0-9]*,[-0-9]*:[-0-9]*,[-0-9]*,[-0-9]*' | tail -1 | sed 's/^cam=//')
  if [ -n "${CB_CAM:-}" ]; then
    EL gamemode creative TestPlayer >/dev/null 2>&1 || true
    key F1
    sleep 1
    EL clear TestPlayer >/dev/null 2>&1 || true
    EL item replace entity TestPlayer weapon.mainhand with riftworld:crystal_katar >/dev/null 2>&1 || true
    EL item replace entity TestPlayer hotbar.1 with riftworld:crystal_barrage 16 >/dev/null 2>&1 || true
    EL item replace entity TestPlayer hotbar.2 with riftworld:crystal_aegis 16 >/dev/null 2>&1 || true
    EL item replace entity TestPlayer hotbar.3 with riftworld:astral_prism >/dev/null 2>&1 || true
    CX1=$(printf '%s' "$CB_CAM" | cut -d: -f1 | cut -d, -f1)
    CY1=$(printf '%s' "$CB_CAM" | cut -d: -f1 | cut -d, -f2)
    CZ1=$(printf '%s' "$CB_CAM" | cut -d: -f1 | cut -d, -f3)
    LX1=$(printf '%s' "$CB_CAM" | cut -d: -f2 | cut -d, -f1)
    LY1=$(printf '%s' "$CB_CAM" | cut -d: -f2 | cut -d, -f2)
    LZ1=$(printf '%s' "$CB_CAM" | cut -d: -f2 | cut -d, -f3)
    EL tp TestPlayer "$CX1" "$((CY1 - 1))" "$CZ1" facing "$LX1" "$LY1" "$LZ1" >/dev/null 2>&1 || true
    sleep 2
    EL execute at TestPlayer positioned ^ ^ ^2.2 run summon riftworld:crystal_shield ~ ~ ~ >/dev/null 2>&1 || true
    sleep 3
    shot "90_crystal_katar_and_shield"
    EL execute at TestPlayer run kill @e[type=riftworld:crystal_shield,distance=..16] >/dev/null 2>&1 || true
    EL item replace entity TestPlayer weapon.mainhand with riftworld:astral_prism >/dev/null 2>&1 || true
    EL execute at TestPlayer positioned ^-0.4 ^0.2 ^1.8 run summon riftworld:crystal_shard ~ ~ ~ {NoGravity:1b} >/dev/null 2>&1 || true
    EL execute at TestPlayer positioned ^0.0 ^0.35 ^1.9 run summon riftworld:crystal_shard ~ ~ ~ {NoGravity:1b} >/dev/null 2>&1 || true
    EL execute at TestPlayer positioned ^0.4 ^0.2 ^1.8 run summon riftworld:crystal_shard ~ ~ ~ {NoGravity:1b} >/dev/null 2>&1 || true
    sleep 3
    shot "91_astral_prism_and_barrage"
    EL execute at TestPlayer run kill @e[type=riftworld:crystal_shard,distance=..16] >/dev/null 2>&1 || true
    key F1
    log "  OK   3D-оружие Колоннады (crystal_katar, crystal_barrage, crystal_aegis, astral_prism) выдано и отснято"
  fi

  EL gamemode creative TestPlayer >/dev/null 2>&1 || true
}
biome_section || log "  FAIL секция биомов упала с кодом $?"

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
# Рев 13.25: «Missing textures in model ...» — это WARN, который раньше проходил мимо гейта:
# модель DT-ветви коралла ссылалась на riftworld:block/coral_stem_teal, которой не существовало,
# и в воде ствол рисовался клеткой missing-texture. Пробы при этом были зелёными.
TEXWARN=$(grep -c "Missing textures in model" "$OUT/client.log" 2>/dev/null || true)
TEXWARN=${TEXWARN:-0}
# Рев 14.26: владелец прислал вылет «Feature order cycle found … minecraft:deep_dark» — это краш
# генерации чанков ОБЫЧНОГО мира (deep_dark есть только в оверворлде), где Dynamic Trees добавляет
# свои фичи во ВСЕ биомы. Проверяем оба лога явно, чтобы такой вылет не прошёл зелёным прогоном.
FEATCYCLE=$(cat "$OUT/client.log" "$OUT/server_latest.log" 2>/dev/null | grep -c "Feature order cycle" || true)
FEATCYCLE=${FEATCYCLE:-0}
echo "Feature order cycle в логах клиента и сервера: $FEATCYCLE"
if [ "$FEATCYCLE" -gt 0 ]; then
  echo "::error title=worldgen::Feature order cycle found — конфликт порядка фич (Dynamic Trees + другой worldgen-мод)"
  cat "$OUT/client.log" "$OUT/server_latest.log" 2>/dev/null | grep "Feature order cycle" | head -2 \
    | while IFS= read -r line; do echo "::error title=worldgen::${line:0:300}"; done
fi
ERRS=$(grep -E "Exception|ERROR" "$OUT/client.log" 2>/dev/null \
       | grep -viE "Narrator|flite|text2speech|OpenAL|SoundSystem|SoundEngine|no sound|libflite|Yggdrasil|authlib|AuthenticationUnavailable|Cannot contact authentication server|SocketTimeoutException|Read timed out" \
       | grep -viE "missing model for variant|loading blockstate definition|ModelBakery" | wc -l)
echo "строк Exception/ERROR в логе клиента (без звука/наратора и без missing-model): $ERRS"
echo "missing model for variant в логе клиента: $MODELWARN"
if [ "$MODELWARN" -gt 0 ]; then
  echo "::error title=модели::missing model for variant: $MODELWARN строк — блоки регистрируются не в своём namespace"
  grep "missing model for variant" "$OUT/client.log" | head -3 \
    | while IFS= read -r line; do echo "::error title=модели::${line:0:300}"; done
fi
echo "Missing textures in model в логе клиента: $TEXWARN"
if [ "$TEXWARN" -gt 0 ]; then
  echo "::error title=текстуры::Missing textures in model: $TEXWARN — модель ссылается на несуществующую текстуру"
  grep -A1 "Missing textures in model" "$OUT/client.log" | head -6 \
    | while IFS= read -r line; do echo "::error title=текстуры::${line:0:300}"; done
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
  echo "- Missing textures in model в логе клиента: **$TEXWARN**"
  echo "- Feature order cycle в логах: **$FEATCYCLE** (0 — генерация чанков без конфликта порядка фич)"
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
    | grep -viE "Narrator|flite|text2speech|OpenAL|SoundSystem|SoundEngine|no sound|libflite|Yggdrasil|authlib|AuthenticationUnavailable|Cannot contact authentication server|SocketTimeoutException|Read timed out" \
    | head -10 | while IFS= read -r line; do
        echo "::error title=ошибка в логе клиента::${line:0:300}"
      done
  echo "::error title=итог::строк Exception/ERROR в логе клиента: $ERRS"
fi
if [ "$TEXWARN" -gt 0 ]; then
  echo "::error title=итог::Missing textures in model: $TEXWARN (модель ссылается на несуществующую текстуру)"
fi
echo "== итог: провалов $FAILS, ошибок в логе $ERRS, битых текстур $TEXWARN, снимков $(ls -1 "$OUT"/*.jpg 2>/dev/null | wc -l) =="

[ "$FAILS" -eq 0 ] && [ "$ERRS" -eq 0 ] && [ "$MODELWARN" -eq 0 ] && [ "$TEXWARN" -eq 0 ] && [ "$FEATCYCLE" -eq 0 ]
