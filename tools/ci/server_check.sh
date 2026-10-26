#!/usr/bin/env bash
# Headless verification of the riftworld jar: installs a Forge 1.20.1 dedicated server,
# boots it with the freshly built mod and checks the boot/mod/tps/log health.
# Оставляет сервер ЖИВЫМ (KEEP=1 по умолчанию) — следующим шагом его использует client_check.sh,
# который и делает настоящие пробы измерения Элисара (dedicated-сервер сам измерение не генерирует,
# пока в нём нет игрока).
# usage: tools/ci/server_check.sh
set -uo pipefail
FORGE=${FORGE:-1.20.1-47.4.23}
SRV=${SRV:-ci/server}
OUT=${OUT:-ci-out}
RCON_PW=${RCON_PW:-riftci}
KEEP=${KEEP:-1}
JAR=$(ls mod/build/libs/riftworld-*.jar 2>/dev/null | grep -v -e sources -e javadoc | head -1)
mkdir -p "$SRV/mods" "$OUT"
if [ -z "$JAR" ]; then echo "FATAL: no mod jar built"; exit 1; fi
echo "jar: $JAR"
cp -f "$JAR" "$SRV/mods/"

# Рев 12: WITH_DT=1 — сервер тоже должен видеть Dynamic Trees и аддон (client_check проверяет
# рост деревьев через /dynamictrees). Без DT-jar аддон не загрузится, а без аддона нет смысла.
if [ "${WITH_DT:-0}" = "1" ]; then
  echo "== WITH_DT: Dynamic Trees + аддон в mods сервера =="
  DTJAR=$(ls mod/build/libs/riftworld-dt-*.jar 2>/dev/null | grep -v -e sources -e javadoc | head -1)
  [ -n "$DTJAR" ] && { cp -f "$DTJAR" "$SRV/mods/"; echo "  аддон: $(basename "$DTJAR")"; } \
    || echo "  WARN: аддон не собран (dtJar) — проверка DT будет пропущена"
  DTVER=$(sed -n 's/^dynamic_trees_version=//p' mod/gradle.properties 2>/dev/null | head -1)
  DTMAIN=$(ls ~/.gradle/caches/modules-2/files-2.1/com.ferreusveritas.dynamictrees/DynamicTrees-*/*/DynamicTrees-*.jar 2>/dev/null \
             | grep -v -e sources | head -1)
  if [ -z "$DTMAIN" ]; then
    curl -fsSL -o "$SRV/mods/dynamictrees.jar" \
      "https://harleyoconnor.com/maven/com/ferreusveritas/dynamictrees/DynamicTrees-1.20.1/DynamicTrees-1.20.1-${DTVER}.jar" \
      && DTMAIN="$SRV/mods/dynamictrees.jar" || echo "  WARN: Dynamic Trees jar не скачался"
  fi
  [ -n "$DTMAIN" ] && [ -f "$DTMAIN" ] && echo "  Dynamic Trees: $(basename "$DTMAIN")"
fi

if [ ! -f "$SRV/libraries/net/minecraftforge/forge/$FORGE/unix_args.txt" ]; then
  curl -fsSL -o "$SRV/installer.jar" \
    "https://maven.minecraftforge.net/net/minecraftforge/forge/$FORGE/forge-$FORGE-installer.jar" || { echo "FATAL: installer download"; exit 1; }
  ( cd "$SRV" && java -jar installer.jar --installServer ) > "$OUT/installer.log" 2>&1 || { echo "FATAL: installer"; tail -40 "$OUT/installer.log"; exit 1; }
  rm -f "$SRV/installer.jar" "$SRV/installer.jar.log"
fi

printf 'eula=true\n' > "$SRV/eula.txt"
cat > "$SRV/server.properties" <<P
online-mode=false
enable-rcon=true
rcon.password=$RCON_PW
rcon.port=25575
server-port=25565
server-ip=
level-type=minecraft\:normal
level-seed=21122
view-distance=6
simulation-distance=5
max-players=5
spawn-protection=0
spawn-monsters=false
gamemode=creative
allow-nether=false
max-tick-time=300000
sync-chunk-writes=false
motd=riftworld CI
P

( cd "$SRV" && nohup java -Xmx2G -Dfml.earlyprogresswindow=false @libraries/net/minecraftforge/forge/$FORGE/unix_args.txt nogui > console.log 2>&1 & echo $! > server.pid )

echo "waiting for server boot..."
ok=0
for i in $(seq 1 300); do
  grep -q 'Done (' "$SRV/logs/latest.log" 2>/dev/null && { ok=1; break; }
  grep -qE 'A fatal error|Exception in thread "main"' "$SRV/console.log" 2>/dev/null && break
  sleep 2
done
if [ "$ok" != 1 ]; then
  echo "FATAL: server did not reach Done in 10 min"; tail -60 "$SRV/console.log"; cp "$SRV/console.log" "$OUT/console.log"; exit 1
fi
grep 'Done (' "$SRV/logs/latest.log" | tail -1 | tee "$OUT/done.txt"

Rq() { python3 tools/rcon.py --password "$RCON_PW" --log "" --timeout 120 "$@"; }
FAILS=0
bad() { echo "CHECK FAILED: $*"; FAILS=$((FAILS+1)); }
say() { echo; echo "== $*"; }

say "mods loaded"
Rq forge mods > "$OUT/mods.txt" 2>&1
grep -qi 'riftworld' "$OUT/mods.txt" && echo "  riftworld present" || bad "mod not listed by forge mods"

say "tps"
Rq forge tps > "$OUT/tps.txt" 2>&1
grep -q "Mean TPS: 20.000" "$OUT/tps.txt" && echo "  tps ok" || bad "tps below 20"
cat "$OUT/tps.txt"

say "log sweep"
grep -nE 'Exception|\bERROR\b' "$SRV/logs/latest.log" | grep -viE 'Registry|codec' > "$OUT/errors.txt" || true
ERRS=$(grep -c . "$OUT/errors.txt" || true)
echo "error lines: $ERRS"
[ "$ERRS" -eq 0 ] || bad "$ERRS exception/error lines in latest.log"

cp "$SRV/logs/latest.log" "$OUT/latest.log" 2>/dev/null || true

if [ "$KEEP" != 1 ]; then
  Rq stop >/dev/null 2>&1
  for i in $(seq 1 60); do pid=$(cat "$SRV/server.pid" 2>/dev/null || echo); [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null || break; sleep 2; done
fi

{
  echo "# CI, шаг 1: сборка jar + запуск Forge-сервера"
  echo
  echo "- jar: \`$JAR\`"
  echo "- сервер: \`$(cat "$OUT/done.txt" 2>/dev/null | tail -1)\`"
  echo "- провалов проверок: **$FAILS**"
  echo "- строк Exception/ERROR в latest.log: **$ERRS**"
  echo "- сервер оставлен живым для шага клиента: **$KEEP**"
  echo
  echo "## forge mods"; echo '```'; sed -n '1,25p' "$OUT/mods.txt"; echo '```'
  echo "## tps"; echo '```'; cat "$OUT/tps.txt"; echo '```'
  echo "## ошибки"; echo '```'; sed -n '1,40p' "$OUT/errors.txt"; echo '```'
} > "$OUT/report_server.md"
cat "$OUT/report_server.md"
[ "$FAILS" -eq 0 ] && [ "$ERRS" -eq 0 ]
