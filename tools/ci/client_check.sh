#!/usr/bin/env bash
# Рев 8: проверка «как в жизни» — поднимаем НАСТОЯЩИЙ клиент Minecraft (ForgeGradle runClient)
# в виртуальном дисплее, заходим в мир, уходим в измерение Элисара и снимаем экран.
#
# Почему клиент: dedicated-сервер не генерирует измерение, в котором нет игрока
# (проверено: чанки Элисара остаются воздухом, сущности в них не спавнятся).
#
# usage: tools/ci/client_check.sh     (из корня репозитория, JDK 17 в PATH, DISPLAY свободен)
set -uo pipefail
OUT=${OUT:-ci-shots}
RUN=${RUN:-ci/runs}
FORGE=${FORGE:-1.20.1-47.4.23}
DISP=${DISP:-:99}
mkdir -p "$OUT" "$RUN/client"

# ---------------------------------------------------------------- зависимости
sudo apt-get update -qq >/dev/null 2>&1 || true
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq xvfb x11-utils imagemagick libgl1 libglx-mesa0 \
  libxi6 libxxf86vm1 libxfixes3 libxrender1 libxkbcommon0 libsm6 libopenal1 >/dev/null 2>&1 || true
command -v Xvfb >/dev/null || { echo "FATAL: нет Xvfb"; exit 1; }
command -v import >/dev/null || { echo "FATAL: нет imagemagick"; exit 1; }

# ---------------------------------------------------------------- настройки клиента
python3 - <<'PY'
import json, os
run = os.environ.get("RUN", "ci/runs")
opts = {
    "tutorialStep": "none", "onboardAccessibility": "false", "skipMultiplayerWarning": "true",
    "joinedFirstServer": "true", "renderDistance": "6", "simulationDistance": "5", "maxFps": "20",
    "guiScale": "2", "fov": "0.0", "graphicsMode": "1", "renderClouds": "false",
    "pauseOnLostFocus": "false", "narrator": "0", "soundCategory_master": "0.0",
    "fullscreen": "false", "showSubtitles": "false", "autoJump": "false",
    "darkMojangStudiosBackground": "true", "skipRealms32bitWarning": "true",
}
os.makedirs(run + "/client", exist_ok=True)
with open(run + "/client/options.txt", "w", encoding="utf-8") as f:
    for k, v in opts.items():
        f.write(f"{k}:{v}\n")
print("client options ->", run + "/client/options.txt")
PY

# вечный «новый мир» + сразу в Элисар (быстрый вход без меню)
cat > "$RUN/client/run.sh" <<'EOS'
#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
exec ../jdk/bin/java -Xmx1500M -XX:+UseSerialGC \
  -Djava.awt.headless=false -Dorg.lwjgl.util.Debug=false -Dfml.earlyprogresswindow=false \
  -cp "$(dirname "$0")/../client/forge-client.jar" net.minecraft.launchwrapper.Launch \
  --username TestPlayer --gameDir . --assetsDir ../client/assets --assetIndex 5 --version 1.20.1 2>/dev/null || true
EOS
echo "(fallback launcher не используется, если запуск идёт через Gradle)"

# ---------------------------------------------------------------- запуск клиента
xvfb_run() {
  if ! pgrep -f "Xvfb $DISP" >/dev/null; then
    Xvfb "$DISP" -screen 0 1280x800x24 -nolisten tcp >/tmp/xvfb.log 2>&1 &
    sleep 3
  fi
}
xvfb_run
export DISPLAY=$DISP LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe MESA_GL_VERSION_OVERRIDE=3.3

shot() { # shot <имя>: снять экран и уменьшить
  import -window root "/tmp/$1_raw.png" 2>/dev/null || return 1
  convert "/tmp/$1_raw.png" -resize 1280x800 -quality 88 "$OUT/$1.jpg" 2>/dev/null || cp "/tmp/$1_raw.png" "$OUT/$1.png"
  echo "  shot: $OUT/$1.jpg"
}

echo "== старт клиента через Gradle (runClient) =="
(
  cd mod
  chmod +x gradlew
  ./gradlew runClient --no-daemon --console=plain \
    -PrunDir="$PWD/../$RUN" \
    -PautoJoin=127.0.0.1:25565 \
    2>&1
) > "$OUT/client.log" 2>&1 &
CPID=$!

# ждём, пока клиент дойдёт до главного меню (по логу) или упадёт
for i in $(seq 1 240); do
  grep -qiE "Setting user:|Created: .*minecraft:textures|OpenAL initialized|LWJGL Version" "$OUT/client.log" && break
  grep -qiE "FATAL|A fatal error|Exception in thread \"main\"" "$OUT/client.log" && break
  sleep 3
done
sleep 20
shot "client_boot" || true
echo "== клиент поднят, состояние лога: =="
tail -5 "$OUT/client.log" || true

# ---------------------------------------------------------------- сценарий в игре
RCON_PW=${RCON_PW:-riftci}
R() { python3 tools/rcon.py --password "$RCON_PW" --log "" --timeout 120 "$@" 2>&1; }
step() { echo; echo "== $*"; sleep "${2:-3}"; }

echo "== ждём игрока на сервере =="
for i in $(seq 1 120); do
  ans=$(R list 2>/dev/null || true)
  case "$ans" in *TestPlayer*) echo "  игрок в игре: $ans"; break ;; esac
  sleep 5
done

R gamerule doDaylightCycle false >/dev/null 2>&1 || true
R time set noon >/dev/null 2>&1 || true
R weather clear >/dev/null 2>&1 || true
R gamemode creative TestPlayer >/dev/null 2>&1 || true

echo "== телепорт в измерение Элисара (море) =="
R execute in riftworld:elisar run tp TestPlayer 600 64 666 2>&1 | tail -2
sleep 25                      # даём клиенту догрузить чанки

echo "== генерация области для съёмки =="
R execute in riftworld:elisar run forceload add 576 576 704 704 >/dev/null 2>&1 || true
sleep 45

# --- пробы: вода, дно, кораллы
probe() { # probe <x> <y> <z> <block> <label>
  local ans
  ans=$(R execute in riftworld:elisar positioned "$1" "$2" "$3" run execute if block ~ ~ ~ "riftworld:$4" 2>/dev/null || true)
  case "$ans" in *"Test passed"*) echo "  OK   $5 ($1,$2,$3) = $4" ;; *) echo "  FAIL $5 ($1,$2,$3) != $4"; return 1 ;; esac
}
echo "== пробы моря (300,300) =="
probe 300 40 300 elisar_water "вода на глубине"
probe 300 13 300 elisar_seabed "донный песок"
probe 600 66 666 elisar_water "вода над рифом"

# --- скриншоты
echo "== скриншоты =="
tp_shot() { # tp_shot <x> <y> <z> <yaw> <pitch> <имя> <пауза>
  R execute in riftworld:elisar run tp TestPlayer "$1" "$2" "$3" "$4" "$5" >/dev/null 2>&1
  sleep "${7:-12}"
  shot "$6" || true
}
tp_shot 600 20 666 90 35 sea_reef       14     # риф сверху-сбоку
tp_shot 600 58 666 90 25 sea_surface    12     # поверхность воды
tp_shot 626 16 770 45 5  sea_trench     14     # ущелье / глубина
tp_shot 612 16 786 90 40 sea_blue_hole  14     # синяя дыра
tp_shot 659 9 613 0 0    sea_cave       16     # внутри подводной пещеры

# --- инвентарь/творческая вкладка: блоки моря
R gamemode creative TestPlayer >/dev/null 2>&1 || true

echo "== итог =="
R list || true
# гасим клиент и собираем лог
pkill -f "GradleWrapperMain runClient" 2>/dev/null || true
pkill -f forgeclientuserdev 2>/dev/null || true
sleep 5
kill $CPID 2>/dev/null || true

grep -cE "Exception|ERROR" "$OUT/client.log" > "$OUT/client_errors.txt" 2>/dev/null || echo 0 > "$OUT/client_errors.txt"
echo "строк Exception/ERROR в логе клиента: $(cat "$OUT/client_errors.txt")"
ls -la "$OUT"
