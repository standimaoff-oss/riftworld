#!/usr/bin/env bash
# Forge 1.20.1-47.4.23 dedicated test server.
# Usage: setup_server.sh {install|start|run|stop|restart|status|mods|deploy}
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT_PATH="$(readlink -f "${BASH_SOURCE[0]}")"
MC_ROOT=${MC_ROOT:-/home/user/.cache/mc}
SRV="$MC_ROOT/server"
FORGE=1.20.1-47.4.23
MODID=${MODID:-riftworld}
MOD_DIR=${MOD_DIR:-$ROOT/mod}
LOGS=${LOGS:-$ROOT/reports/session_temple/logs}
RCON_PW=${RCON_PASSWORD:-bm21test}
JAVA_HOME=${JAVA_HOME:-$(find "$MC_ROOT/jdk" -maxdepth 1 -type d -name 'jdk-17*' -print -quit 2>/dev/null || true)}
if [[ -z "$JAVA_HOME" ]]; then JAVA_HOME=$(find /usr/lib/jvm -maxdepth 1 -type d -name 'java-17*' -print -quit 2>/dev/null || true); fi
if [[ -z "$JAVA_HOME" ]] && command -v java >/dev/null 2>&1; then JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")"; fi
[[ -n "$JAVA_HOME" && -x "$JAVA_HOME/bin/java" ]] && export JAVA_HOME PATH="$JAVA_HOME/bin:$PATH"

_dl() {
  local url=$1 out=$2
  for i in 1 2 3; do curl -fsSL "$url" -o "$out" && return 0; sleep 5; done
  return 1
}

install_java() {
  if [[ -n "$JAVA_HOME" && -x "$JAVA_HOME/bin/java" ]] && "$JAVA_HOME/bin/java" -version 2>&1 | grep -q 'version "17'; then return 0; fi
  if sudo apt-get install -y openjdk-17-jdk-headless >/dev/null 2>&1; then
    JAVA_HOME="$(find /usr/lib/jvm -maxdepth 1 -type d -name 'java-17*' -print -quit 2>/dev/null || true)"
  else
    mkdir -p "$MC_ROOT/jdk"
    _dl "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse" "$MC_ROOT/jdk17.tgz"
    tar xzf "$MC_ROOT/jdk17.tgz" -C "$MC_ROOT/jdk" && rm -f "$MC_ROOT/jdk17.tgz"
    JAVA_HOME="$(find "$MC_ROOT/jdk" -maxdepth 1 -type d -name 'jdk-17*' -print -quit)"
  fi
  if [[ -z "$JAVA_HOME" || ! -x "$JAVA_HOME/bin/java" ]] || ! "$JAVA_HOME/bin/java" -version 2>&1 | grep -q 'version "17'; then
    echo "Java 17 JDK installation failed." >&2
    return 1
  fi
  export JAVA_HOME PATH="$JAVA_HOME/bin:$PATH"
}

install() {
  install_java
  mkdir -p "$SRV/mods" "$LOGS"
  if [[ ! -f "$SRV/libraries/net/minecraftforge/forge/$FORGE/unix_args.txt" ]]; then
    _dl "https://maven.minecraftforge.net/net/minecraftforge/forge/$FORGE/forge-$FORGE-installer.jar" "$SRV/installer.jar"
    (cd "$SRV" && java -jar installer.jar --installServer >"$SRV/installer.log" 2>&1)
    rm -f "$SRV/installer.jar" "$SRV/installer.jar.log"
  fi
  echo "eula=true" > "$SRV/eula.txt"
  cat > "$SRV/server.properties" <<P
online-mode=false
enable-rcon=true
rcon.password=$RCON_PW
rcon.port=25575
server-ip=127.0.0.1
level-type=minecraft\\:flat
level-seed=21122
generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}
view-distance=4
simulation-distance=4
max-players=5
spawn-protection=0
spawn-monsters=false
gamemode=survival
motd=Riftworld temple headless test
P
  deploy
}

deploy() {
  mkdir -p "$SRV/mods"
  shopt -s nullglob
  local candidates=("$MOD_DIR"/build/libs/"$MODID"-*.jar) jar
  rm -f "$SRV/mods/$MODID"-*.jar
  for jar in "${candidates[@]}"; do
    [[ "$jar" == *-sources.jar || "$jar" == *-javadoc.jar ]] && continue
    cp "$jar" "$SRV/mods/"
    echo "deployed $(basename "$jar")"
    return 0
  done
  echo "no built $MODID jar found in $MOD_DIR/build/libs" >&2
  return 1
}

pid() { pgrep -f "forge/$FORGE/unix_args.txt" || true; }

run() {
  if [[ -z "$JAVA_HOME" || ! -x "$JAVA_HOME/bin/java" ]] || ! "$JAVA_HOME/bin/java" -version 2>&1 | grep -q 'version "17'; then
    echo "Minecraft 1.20.1 requires Java 17; run tools/setup_server.sh install first." >&2
    return 1
  fi
  mkdir -p "$SRV" "$LOGS"
  cd "$SRV"
  ln -sfn "$SRV/logs/latest.log" "$LOGS/$MODID-server-live.log"
  exec java -Xmx768M -Xms512M -XX:+UseSerialGC -Dfml.earlyprogresswindow=false \
    @libraries/net/minecraftforge/forge/$FORGE/unix_args.txt nogui
}

start() {
  [[ -n "$(pid)" ]] && { echo "server already running"; return 0; }
  rm -rf "$SRV/crash-reports"
  mkdir -p "$SRV" "$LOGS"
  setsid nohup "$SCRIPT_PATH" run >"$SRV/console.log" 2>&1 </dev/null &
  for _ in $(seq 1 180); do
    if grep -q 'Done (' "$SRV/logs/latest.log" 2>/dev/null; then
      grep 'Done (' "$SRV/logs/latest.log" | tail -1
      return 0
    fi
    if find "$SRV/crash-reports" -maxdepth 1 -type f -name '*.txt' -print -quit 2>/dev/null | grep -q .; then break; fi
    sleep 2
  done
  echo "server did not reach Done; inspect $SRV/console.log and $SRV/logs/latest.log" >&2
  return 1
}

stop() {
  local p
  p=$(pid)
  [[ -z "$p" ]] && return 0
  python3 "$ROOT/tools/rcon.py" --log "" stop >/dev/null 2>&1 || kill $p
  for _ in $(seq 1 30); do [[ -z "$(pid)" ]] && return 0; sleep 1; done
  pkill -9 -f "forge/$FORGE/unix_args.txt" 2>/dev/null || true
}

case "${1:-status}" in
  install) install ;;
  deploy) deploy ;;
  run) run ;;
  start) start ;;
  stop) stop ;;
  restart) stop; deploy; start ;;
  status) [[ -n "$(pid)" ]] ;;
  mods) grep -E 'riftworld|Found mod file' "$SRV/logs/debug.log" 2>/dev/null | tail -40 || true; python3 "$ROOT/tools/rcon.py" --log "" forge mods 2>/dev/null || true ;;
  *) echo "usage: $0 {install|start|run|stop|restart|status|mods|deploy}"; exit 1 ;;
esac
