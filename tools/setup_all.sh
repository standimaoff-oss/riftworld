#!/usr/bin/env bash
# Recreate the headless Forge test toolchain (Minecraft 1.20.1 / Forge 47.4.23).
# No physical GPU is required: setup_client.sh uses Xvfb + Mesa llvmpipe software rendering.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MC_ROOT=${MC_ROOT:-/home/user/.cache/mc}
JDK_DIR="$MC_ROOT/jdk"
mkdir -p "$JDK_DIR" "$MC_ROOT/gradle-home" "$MC_ROOT/runs"

java17_ready() {
  command -v java >/dev/null 2>&1 && command -v javac >/dev/null 2>&1 && java -version 2>&1 | grep -q 'version "17'
}
JDK_HOME=$(find "$JDK_DIR" -maxdepth 1 -type d -name 'jdk-17*' -print -quit 2>/dev/null || true)
if [[ -z "$JDK_HOME" ]]; then
  JDK_HOME=$(find /usr/lib/jvm -maxdepth 1 -type d -name 'java-17*' -print -quit 2>/dev/null || true)
fi
if [[ -n "$JDK_HOME" ]]; then export JAVA_HOME="$JDK_HOME" PATH="$JDK_HOME/bin:$PATH"; fi
if ! java17_ready; then
  if sudo apt-get update -qq && sudo apt-get install -y -qq openjdk-17-jdk-headless; then
    echo "OPENJDK17_APT_OK"
  else
    mkdir -p "$JDK_DIR"
    curl -fsSL -o "$MC_ROOT/jdk17.tgz" "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse"
    tar xzf "$MC_ROOT/jdk17.tgz" -C "$JDK_DIR"
    rm -f "$MC_ROOT/jdk17.tgz"
    echo "OPENJDK17_TEMURIN_OK"
  fi
  JDK_HOME=$(find "$JDK_DIR" -maxdepth 1 -type d -name 'jdk-17*' -print -quit 2>/dev/null || true)
  if [[ -z "$JDK_HOME" ]]; then JDK_HOME=$(find /usr/lib/jvm -maxdepth 1 -type d -name 'java-17*' -print -quit 2>/dev/null || true); fi
  if [[ -n "$JDK_HOME" ]]; then export JAVA_HOME="$JDK_HOME" PATH="$JDK_HOME/bin:$PATH"; fi
fi
java17_ready || { echo "Java 17 JDK is required (java + javac)." >&2; exit 1; }

sudo apt-get update -qq
sudo apt-get install -y -qq \
  xvfb xdotool x11-apps x11-utils imagemagick \
  libgl1 libgl1-mesa-dri libglx-mesa0 mesa-utils libxi6 libxxf86vm1 libxfixes3 libxrender1 libxkbcommon0 libsm6 \
  vorbis-tools rsync python3-pil >/dev/null

echo "HEADLESS_GRAPHICS_OK"
# Blender is only needed when regenerating source GLB assets; it is intentionally optional.
if [[ ${INSTALL_BLENDER:-0} == 1 ]] && [[ ! -x /home/user/.cache/blender/blender-5.2.2-linux-x64/blender ]]; then
  mkdir -p /home/user/.cache/blender
  curl -fsSL -o /home/user/.cache/blender/blender.tar.xz https://download.blender.org/release/Blender5.2/blender-5.2.2-linux-x64.tar.xz
  tar xJf /home/user/.cache/blender/blender.tar.xz -C /home/user/.cache/blender
  rm -f /home/user/.cache/blender/blender.tar.xz
fi

chmod +x "$ROOT"/tools/*.sh "$ROOT"/mod/gradlew
java -version
echo "ALL_DONE ROOT=$ROOT MC_ROOT=$MC_ROOT"
