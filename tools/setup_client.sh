#!/usr/bin/env bash
# Headless Minecraft 1.20.1 + Forge 47.4.23 dev client. No physical GPU is required.
# Usage: setup_client.sh {install|start|run|stop|status}
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT_PATH="$(readlink -f "${BASH_SOURCE[0]}")"
MC_ROOT=${MC_ROOT:-/home/user/.cache/mc}
MOD_DIR=${MOD_DIR:-$ROOT/mod}
DISPLAY_NUM=${DISPLAY_NUM:-99}
JAVA_HOME=${JAVA_HOME:-$(find "$MC_ROOT/jdk" -maxdepth 1 -type d -name 'jdk-17*' -print -quit 2>/dev/null || true)}
if [[ -z "$JAVA_HOME" ]]; then JAVA_HOME=$(find /usr/lib/jvm -maxdepth 1 -type d -name 'java-17*' -print -quit 2>/dev/null || true); fi
export GRADLE_USER_HOME=${GRADLE_USER_HOME:-$MC_ROOT/gradle-home}
if [[ -n "$JAVA_HOME" && -x "$JAVA_HOME/bin/java" ]]; then export JAVA_HOME PATH="$JAVA_HOME/bin:$PATH"; fi
GRADLE=("$MOD_DIR/gradlew" --project-cache-dir "$MC_ROOT/gradle" --gradle-user-home "$GRADLE_USER_HOME")

require_java17() {
  local java_bin javac_bin
  java_bin="${JAVA_HOME:+$JAVA_HOME/bin/java}"
  javac_bin="${JAVA_HOME:+$JAVA_HOME/bin/javac}"
  [[ -x "$java_bin" ]] || java_bin=$(command -v java || true)
  [[ -x "$javac_bin" ]] || javac_bin=$(command -v javac || true)
  if [[ -z "$java_bin" || -z "$javac_bin" ]] || ! "$java_bin" -version 2>&1 | grep -q 'version "17'; then
    echo "Minecraft 1.20.1 requires a Java 17 JDK; run tools/setup_all.sh first." >&2
    return 1
  fi
}
CLIENT_LOG="$MC_ROOT/runs/client/console.log"

install() {
  require_java17
  sudo apt-get install -y xvfb xdotool x11-apps x11-utils imagemagick libgl1 libgl1-mesa-dri libglx-mesa0 mesa-utils >/dev/null
  mkdir -p "$MC_ROOT/runs/client"
  (cd "$MOD_DIR" && "${GRADLE[@]}" downloadAssets -q)
}

xvfb() {
  if ! pgrep -f "Xvfb :$DISPLAY_NUM([[:space:]]|$)" >/dev/null; then
    Xvfb ":$DISPLAY_NUM" -screen 0 1280x800x24 +extension GLX -nolisten tcp >"$MC_ROOT/runs/xvfb.log" 2>&1 &
    for _ in $(seq 1 50); do
      [[ -S "/tmp/.X11-unix/X$DISPLAY_NUM" ]] && break
      sleep 0.1
    done
    [[ -S "/tmp/.X11-unix/X$DISPLAY_NUM" ]]
  fi
}

run() {
  require_java17
  mkdir -p "$MC_ROOT/runs/client"
  xvfb
  export DISPLAY=":$DISPLAY_NUM" LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe
  glxinfo -B >"$MC_ROOT/runs/client/renderer.log" 2>&1 || { cat "$MC_ROOT/runs/client/renderer.log" >&2; return 1; }
  grep -qi 'llvmpipe' "$MC_ROOT/runs/client/renderer.log" || {
    echo "Expected Mesa llvmpipe software renderer; see $MC_ROOT/runs/client/renderer.log" >&2
    return 1
  }
  cd "$MOD_DIR"
  exec "${GRADLE[@]}" runClient -PautoJoin="${JOIN:-127.0.0.1:25565}" -Dorg.gradle.jvmargs="-Xmx900m -XX:+UseSerialGC"
}

start() {
  mkdir -p "$(dirname "$CLIENT_LOG")"
  if pgrep -f 'net.minecraftforge.bootstrap|forgeclientuserdev|GradleStart|runClient' >/dev/null; then
    echo "client already running"
    return 0
  fi
  setsid nohup "$SCRIPT_PATH" run >"$CLIENT_LOG" 2>&1 </dev/null &
  echo "client starting; log: $CLIENT_LOG"
}

stop() {
  pkill -f 'net.minecraftforge.bootstrap|forgeclientuserdev|GradleStart|runClient' || true
}

status() {
  pgrep -af 'net.minecraftforge.bootstrap|forgeclientuserdev|GradleStart|runClient' || true
}

case "${1:-status}" in
  install) install ;;
  start) start ;;
  run) run ;;
  stop) stop ;;
  status) status ;;
  *) echo "usage: $0 {install|start|run|stop|status}"; exit 1 ;;
esac
