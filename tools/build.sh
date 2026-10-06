#!/usr/bin/env bash
# Build the checked-out Riftworld mod. Gradle caches stay in ~/.cache so test builds are disposable.
# Usage: MODID=riftworld tools/build.sh [gradle tasks...]
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODID=${MODID:-riftworld}
MOD_DIR=${MOD_DIR:-$ROOT/mod}
MC_ROOT=${MC_ROOT:-/home/user/.cache/mc}
JAVA_HOME=${JAVA_HOME:-$(find "$MC_ROOT/jdk" -maxdepth 1 -type d -name 'jdk-17*' -print -quit 2>/dev/null || true)}
if [[ -z "$JAVA_HOME" ]]; then JAVA_HOME=$(find /usr/lib/jvm -maxdepth 1 -type d -name 'java-17*' -print -quit 2>/dev/null || true); fi
if [[ -n "$JAVA_HOME" && -x "$JAVA_HOME/bin/java" ]]; then export JAVA_HOME PATH="$JAVA_HOME/bin:$PATH"; fi
java_bin="${JAVA_HOME:+$JAVA_HOME/bin/java}"
javac_bin="${JAVA_HOME:+$JAVA_HOME/bin/javac}"
[[ -x "$java_bin" ]] || java_bin=$(command -v java || true)
[[ -x "$javac_bin" ]] || javac_bin=$(command -v javac || true)
if [[ -z "$java_bin" || -z "$javac_bin" ]] || ! "$java_bin" -version 2>&1 | grep -q 'version "17'; then
  echo "Minecraft 1.20.1 requires a Java 17 JDK; run tools/setup_all.sh first." >&2
  exit 1
fi
export GRADLE_USER_HOME=${GRADLE_USER_HOME:-$MC_ROOT/gradle-home}
TASKS=("${@:-build}")
chmod +x "$MOD_DIR/gradlew"
cd "$MOD_DIR"
"$MOD_DIR/gradlew" --project-cache-dir "$MC_ROOT/gradle" --gradle-user-home "$GRADLE_USER_HOME" \
  "${TASKS[@]}" -q -Dorg.gradle.jvmargs="-Xmx1100m -XX:+UseSerialGC"

VERSION=$(grep '^mod_version' "$MOD_DIR/gradle.properties" | cut -d= -f2 | tr -d ' ')
JAR="$MOD_DIR/build/libs/$MODID-$VERSION.jar"
[[ -f "$JAR" ]]
echo "BUILT $JAR ($(du -h "$JAR" | cut -f1))"
