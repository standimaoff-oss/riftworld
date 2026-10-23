#!/usr/bin/env bash
# Headless verification of the riftworld jar: installs a Forge 1.20.1 dedicated server,
# boots it with the freshly built mod, drives it over RCON, probes the Elisar ocean column
# and writes ci-out/report.md. Runs on a GitHub runner (full internet, no GPU needed).
# usage: tools/ci/server_check.sh
set -uo pipefail
FORGE=${FORGE:-1.20.1-47.4.23}
SRV=${SRV:-ci/server}
OUT=${OUT:-ci-out}
RCON_PW=${RCON_PW:-riftci}
JAR=$(ls mod/build/libs/riftworld-*.jar 2>/dev/null | grep -v -e sources -e javadoc | head -1)
mkdir -p "$SRV/mods" "$OUT"
if [ -z "$JAR" ]; then echo "FATAL: no mod jar built"; exit 1; fi
echo "jar: $JAR"
cp -f "$JAR" "$SRV/mods/"

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
level-type=minecraft\:normal
level-seed=21122
view-distance=4
simulation-distance=4
max-players=5
spawn-protection=0
spawn-monsters=false
gamemode=creative
allow-nether=false
max-tick-time=300000
sync-chunk-writes=false
motd=riftworld CI
P

( cd "$SRV" && nohup java -Xmx3G -Dfml.earlyprogresswindow=false @libraries/net/minecraftforge/forge/$FORGE/unix_args.txt nogui > console.log 2>&1 & echo $! > server.pid )

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

R="python3 tools/rcon.py --password $RCON_PW --log $OUT/rcon.txt --timeout 240"
Rq() { python3 tools/rcon.py --password "$RCON_PW" --log "" --timeout 240 "$@"; }
FAILS=0
bad() { echo "CHECK FAILED: $*"; FAILS=$((FAILS+1)); }
say() { echo; echo "== $*"; }
expect() { local want="$1"; shift; say "$*"; local ans; ans=$($R --expect "$want" "$@" 2>&1) || { echo "$ans"; bad "$*"; }; }

say "mods loaded"
$R forge mods > "$OUT/mods.txt" 2>&1
grep -qi 'riftworld' "$OUT/mods.txt" && echo "  riftworld present" || bad "mod not listed by forge mods"

# --- Elisar dimension: force-generate 10x10 chunks of open ocean around (300,300)
expect "Loaded" execute in riftworld:elisar run forceload add 224 224 384 384
expect "Summoned new" execute in riftworld:elisar run summon riftworld:paper_dragon 0 150 0
expect "Test passed" execute in riftworld:elisar if entity @e[type=riftworld:paper_dragon]
Rq execute in riftworld:elisar run kill @e[type=riftworld:paper_dragon] >/dev/null 2>&1

# --- vertical probe of the open-ocean column at (300, 300): what is at each y
say "ocean column probe (300, y, 300)"
: > "$OUT/column.txt"
for y in $(seq 0 66); do
  ans=$(Rq execute in riftworld:elisar positioned 300 "$y" 300 run execute if block ~ ~ ~ riftworld:elisar_water 2>/dev/null || true)
  case "$ans" in *"Test passed"*) echo "$y W" >> "$OUT/column.txt" ;; *) echo "$y ." >> "$OUT/column.txt" ;; esac
  ans=$(Rq execute in riftworld:elisar positioned 300 "$y" 300 run execute if block ~ ~ ~ riftworld:elisar_seabed 2>/dev/null || true)
  case "$ans" in *"Test passed"*) echo "$y S" >> "$OUT/column.txt" ;; esac
done
WATER=$(grep -c 'W$' "$OUT/column.txt" || true)
SEABED=$(grep -c 'S$' "$OUT/column.txt" || true)
echo "water blocks in column: $WATER, seabed blocks: $SEABED"
[ "$WATER" -ge 30 ] || bad "sea column is not deep (water=$WATER)"
[ "$SEABED" -ge 1 ] || bad "no elisar_seabed under the water"

expect "Mean TPS" forge tps

# --- crash / error sweep of the server log
say "log sweep"
grep -nE 'Exception|\bERROR\b' "$SRV/logs/latest.log" | grep -viE 'Registry|codec' > "$OUT/errors.txt" || true
ERRS=$(grep -c . "$OUT/errors.txt" || true)
echo "error lines: $ERRS"
[ "$ERRS" -eq 0 ] || bad "$ERRS exception/error lines in latest.log"

Rq stop >/dev/null 2>&1
for i in $(seq 1 60); do pid=$(cat "$SRV/server.pid" 2>/dev/null || echo); [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null || break; sleep 2; done
cp "$SRV/logs/latest.log" "$OUT/latest.log" 2>/dev/null || true
cp "$SRV/console.log" "$OUT/console.log" 2>/dev/null || true

{
  echo "# CI: сборка jar + живой запуск Minecraft (Forge $FORGE)"
  echo
  echo "- jar: \`$JAR\`"
  echo "- сервер: \`$(cat "$OUT/done.txt" 2>/dev/null | tail -1)\`"
  echo "- вода в колонке (300,y,300): **$WATER** блоков, дно \`elisar_seabed\`: **$SEABED** блоков"
  echo "- провалов проверок: **$FAILS**"
  echo "- строк Exception/ERROR в latest.log: **$ERRS**"
  echo
  echo "## колонка (y: блок)"
  echo '```'; tr '\n' ' ' < "$OUT/column.txt"; echo; echo '```'
  echo "## forge mods"; echo '```'; sed -n '1,25p' "$OUT/mods.txt"; echo '```'
  echo "## rcon"; echo '```'; cat "$OUT/rcon.txt" 2>/dev/null; echo '```'
  echo "## ошибки"; echo '```'; sed -n '1,40p' "$OUT/errors.txt"; echo '```'
} > "$OUT/report.md"
cat "$OUT/report.md"
[ "$FAILS" -eq 0 ] && [ "$ERRS" -eq 0 ]
