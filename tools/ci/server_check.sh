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
if [ -z "$JAR" ]; then echo "::error title=сервер::jar не собран (mod/build/libs пуст)"; exit 1; fi
echo "jar: $JAR"
cp -f "$JAR" "$SRV/mods/"

write_report_server() {
  {
    echo "# CI, шаг 1: сборка jar + запуск Forge-сервера"
    echo
    echo "- jar: \`$JAR\`"
    echo "- сервер: \`$(tail -1 "$OUT/done.txt" 2>/dev/null)\`"
    echo "- провалов проверок: **${FAILS:-0}**"
    echo "- строк Exception/ERROR в latest.log: **${ERRS:-0}**"
    echo "- сервер оставлен живым для шага клиента: **$KEEP**"
    echo
    echo "## forge mods"; echo '```'; sed -n '1,25p' "$OUT/mods.txt" 2>/dev/null; echo '```'
    echo "## tps"; echo '```'; cat "$OUT/tps.txt" 2>/dev/null; echo '```'
    echo "## аддон DT"; echo '```'; cat "$OUT/riftdt.txt" 2>/dev/null; echo '```'
    echo "## ошибки"; echo '```'; sed -n '1,40p' "$OUT/errors.txt" 2>/dev/null; echo '```'
  } > "$OUT/report_server.md" 2>/dev/null || true
}
on_exit() {
  local rc=$?
  write_report_server
  if [ "$rc" -ne 0 ]; then
    echo "::error title=сервер::скрипт завершился с кодом $rc (провалов проверок: ${FAILS:-0}, ошибок в логе: ${ERRS:-0})"
    if [ -f "$SRV/console.log" ]; then
      tail -40 "$SRV/console.log" | while IFS= read -r line; do echo "::error title=console::${line:0:400}"; done
    fi
    if [ -f "$OUT/installer.log" ]; then
      tail -20 "$OUT/installer.log" | while IFS= read -r line; do echo "::error title=installer::${line:0:400}"; done
    fi
  fi
}
trap on_exit EXIT

# Рев 12: WITH_DT=1 — сервер тоже должен видеть Dynamic Trees и аддон (client_check проверяет
# рост деревьев через /dynamictrees). Без DT-jar аддон не загрузится, а без аддона нет смысла.
if [ "${WITH_DT:-0}" = "1" ]; then
  echo "== WITH_DT: Dynamic Trees + аддон в mods сервера =="
  DTJAR=$(ls mod/build/libs/riftworld-dt-*.jar 2>/dev/null | grep -v -e sources -e javadoc | head -1)
  [ -n "$DTJAR" ] && { cp -f "$DTJAR" "$SRV/mods/"; echo "  аддон: $(basename "$DTJAR")"; } \
    || echo "  WARN: аддон не собран (dtJar) — проверка DT будет пропущена"
  DTVER=$(sed -n 's/^dynamic_trees_version=//p' mod/gradle.properties 2>/dev/null | head -1)
  # Рев 15.2: приоритет 0 — jar, скачанный шагом CI (DT_JAR), тот же файл идёт в dev-клиент
  DTMAIN=""
  if [ -n "${DT_JAR:-}" ] && [ -f "${DT_JAR:-/nonexistent}" ]; then
    cp -f "$DT_JAR" "$SRV/mods/dynamictrees.jar" && DTMAIN="$SRV/mods/dynamictrees.jar" && echo "  DT jar из CI-скачивания: $DT_JAR"
  fi
  # Приоритет 1: gradle copyDTJar — кладёт в build/dtlib/dynamictrees.jar
  DTMAIN=${DTMAIN:-$(ls mod/build/dtlib/dynamictrees.jar 2>/dev/null | head -1)}
  [ -n "$DTMAIN" ] && echo "  DT jar из build/dtlib (copyDTJar)"
  # Приоритет 2: gradle cache — ищем ВСЮду (deobf-jar может лежать в любом подкаталоге)
  if [ -z "$DTMAIN" ]; then
    DTMAIN=$(find ~/.gradle -name 'DynamicTrees-*.jar' 2>/dev/null | grep -v -e sources -e javadoc | head -1)
    [ -n "$DTMAIN" ] && echo "  DT jar из gradle cache"
  fi
  # Приоритет 3: harleyoconnor.com maven (correct URL: /DynamicTrees-1.20.1/1.4.10/DynamicTrees-1.20.1-1.4.10.jar)
  if [ -z "$DTMAIN" ]; then
    echo "  DT jar не найден локально — скачиваем из harleyoconnor.com maven (DT ${DTVER})..."
    curl -fsSL -L -o "$SRV/mods/dynamictrees.jar" \
      "https://harleyoconnor.com/maven/com/ferreusveritas/dynamictrees/DynamicTrees-1.20.1/${DTVER}/DynamicTrees-1.20.1-${DTVER}.jar" \
      && DTMAIN="$SRV/mods/dynamictrees.jar" || echo "  WARN: Dynamic Trees jar не скачался из maven"
  fi
  # Приоритет 4: CurseForge (последний — api.cfwidget.com часто отдаёт HTML вместо jar)
  if [ -z "$DTMAIN" ]; then
    echo "  maven не сработал — пробуем CurseForge..."
    curl -fsSL -L -o "$SRV/mods/dynamictrees.jar" \
      "https://api.cfwidget.com/mods/mc-mods/dynamic-trees/versions/${DTVER}/download" \
      && DTMAIN="$SRV/mods/dynamictrees.jar" || echo "  WARN: Dynamic Trees jar не скачался с CurseForge"
  fi
  if [ -n "$DTMAIN" ] && [ -f "$DTMAIN" ]; then
    cp -f "$DTMAIN" "$SRV/mods/" 2>/dev/null || true
    echo "  Dynamic Trees: $(basename "$DTMAIN")"
  else
    echo "  FATAL: Dynamic Trees jar недоступен — сервер не запустится с аддоном"
  fi
fi

if [ ! -f "$SRV/libraries/net/minecraftforge/forge/$FORGE/unix_args.txt" ]; then
  curl -fsSL -o "$SRV/installer.jar" \
    "https://maven.minecraftforge.net/net/minecraftforge/forge/$FORGE/forge-$FORGE-installer.jar" || { echo "::error title=сервер::не скачался установщик Forge"; exit 1; }
  ( cd "$SRV" && java -jar installer.jar --installServer ) > "$OUT/installer.log" 2>&1 || { echo "::error title=сервер::установщик Forge упал"; tail -40 "$OUT/installer.log"; exit 1; }
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
# Рев 12.2: superflat — убираем ванильные биомы (deep_dark), в которых DT создаёт
# feature order cycle. Elisar — кастомная размерность, работает независимо от overworld.
level-type=minecraft\:flat
generator-settings={"biome":"minecraft:plains","layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"lakes":false,"features":false,"structure_overrides":[]}
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
  echo "::error title=сервер::сервер не дошёл до Done за 10 минут"; cp "$SRV/console.log" "$OUT/console.log" 2>/dev/null; tail -60 "$SRV/console.log"; exit 1
fi
grep 'Done (' "$SRV/logs/latest.log" | tail -1 | tee "$OUT/done.txt"

Rq() { python3 tools/rcon.py --password "$RCON_PW" --log "" --timeout 120 "$@"; }
FAILS=0
ERRS=0
bad() { echo "CHECK FAILED: $*"; FAILS=$((FAILS+1)); }
say() { echo; echo "== $*"; }

say "mods loaded"
Rq forge mods > "$OUT/mods.txt" 2>&1
grep -qi 'riftworld' "$OUT/mods.txt" && echo "  riftworld present" || bad "mod not listed by forge mods"

say "tps"
Rq forge tps > "$OUT/tps.txt" 2>&1
python3 - "$OUT/tps.txt" <<'PYEOF' || bad "tps ниже 19.5"
import re, sys
t = open(sys.argv[1], encoding="utf-8", errors="ignore").read()
m = re.search(r"Mean TPS: ([0-9.]+)", t)
sys.exit(0 if m and float(m.group(1)) >= 19.5 else 1)
PYEOF
cat "$OUT/tps.txt"

if [ -n "${WITH_DT:-}" ]; then
  say "Dynamic Trees: namespace аддона"
  # Рев 13: главный симптом сломанного аддона — блоки уехали в namespace dynamictrees
  # (тогда в клиенте 255 warning'ов missing model). Проверяем из самой игры, а не по логу.
  Rq riftdt > "$OUT/riftdt.txt" 2>&1 || true
  # Рев 13.8: у DT подкомандой registry доступны только family/species/leaves_properties;
  # jo_codes проверяем по нашим ресурсам (файлы trees/riftworlddt/jo_codes/*.txt в jar).
  JC=$(unzip -l "${DTJAR:-$JAR}" 2>/dev/null | grep -c "trees/riftworlddt/jo_codes/.*[.]txt" || true)
  echo "  jo_codes аддона в jar: ${JC:-0}"
  cat "$OUT/riftdt.txt"
  LINE=$(tr -d '\r' < "$OUT/riftdt.txt" | grep -m1 "riftdt" || true)
  # Гейт — по фактическому состоянию реестров (это и есть симптом бага): блоки на месте,
  # ничего не утекло в namespace dynamictrees, семьи/виды/листва видны.
  # handler=... печатаем для справки: его рефлексия не должна ронять проверку.
  if echo "$LINE" | grep -q "wrong_ns=0" && echo "$LINE" | grep -q "missing=0" \
     && echo "$LINE" | grep -q "fam=riftworlddt\|fam=-.*sizo\|fam=sizo" \
     && echo "$LINE" | grep -q "sp="; then
    echo "  аддон DT зарегистрировался в своём namespace"
  else
    echo "::error title=аддон DT::$(echo "$LINE" | head -c 900)"
    bad "аддон DT: $LINE"
  fi
fi

say "log sweep"
grep -nE 'Exception|\bERROR\b' "$SRV/logs/latest.log" | grep -viE 'Registry|codec|dynamictrees:guide|RecipeManager.*Parsing|Missing conditions|DedicatedServerProperties' > "$OUT/errors.txt" || true
ERRS=$(grep -c . "$OUT/errors.txt" || true)
echo "error lines: $ERRS"
[ "$ERRS" -eq 0 ] || bad "$ERRS exception/error lines in latest.log"

cp "$SRV/logs/latest.log" "$OUT/latest.log" 2>/dev/null || true

if [ "$KEEP" != 1 ]; then
  Rq stop >/dev/null 2>&1
  for i in $(seq 1 60); do pid=$(cat "$SRV/server.pid" 2>/dev/null || echo); [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null || break; sleep 2; done
fi

write_report_server
cat "$OUT/report_server.md"

# Рев 13: причины провала — аннотациями, чтобы их было видно без артефактов
if [ "$FAILS" -gt 0 ]; then
  echo "::error title=сервер::провалов проверок: $FAILS"
fi
if [ "$ERRS" -gt 0 ]; then
  head -5 "$OUT/errors.txt" | while IFS= read -r line; do
    echo "::error title=лог сервера::${line:0:600}"
  done
fi
[ "$FAILS" -eq 0 ] && [ "$ERRS" -eq 0 ]
