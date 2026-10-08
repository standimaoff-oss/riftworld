#!/usr/bin/env bash
# Build a mod with all Gradle state kept under ~/.cache (excluded from workspace snapshots).
# Usage: MODID=riftworld bash tools/build.sh [gradle tasks...]   (default MODID=tubombers, task build); copies the jar to ~/release/
#
# The sandbox layout keeps the sources in ~/mods/<id>; the repository itself keeps them in <repo>/mod.
# Both are supported: the repository copy is used whenever ~/mods/<id> is absent.
set -euo pipefail
MODID=${MODID:-tubombers}
# JDK + Gradle caches of the sandbox (skipped when they are not there, e.g. on a normal machine)
JDK=/home/user/.cache/mc/jdk/jdk-17.0.20.1+1
if [ -x "$JDK/bin/javac" ]; then
  export JAVA_HOME=$JDK PATH=$JDK/bin:$PATH
fi
export GRADLE_USER_HOME=${GRADLE_USER_HOME:-/home/user/.cache/mc/gradle-home}   # wrapper distribution goes here too, not ~/.gradle
SRC=/home/user/mods/$MODID
if [ ! -d "$SRC" ]; then
  SRC=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/mod
fi
if ! command -v javac >/dev/null 2>&1; then
  echo "build.sh: no javac found - install a JDK 17 (Forge 1.20.1 needs it) or set JAVA_HOME" >&2
  exit 2
fi
cd "$SRC"
[ "$MODID" = aiarsenal ] && [ -x /home/user/tools/glb_stash.sh ] && /home/user/tools/glb_stash.sh restore
chmod +x gradlew
./gradlew --project-cache-dir /home/user/.cache/mc/gradle --gradle-user-home "$GRADLE_USER_HOME" "${@:-build}" -q -Dorg.gradle.jvmargs="-Xmx700m"
V=$(grep '^mod_version' gradle.properties | cut -d= -f2 | tr -d ' ')
mkdir -p /home/user/release && cp build/libs/$MODID-$V.jar /home/user/release/
ls -la /home/user/release/$MODID-$V.jar
