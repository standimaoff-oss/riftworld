#!/usr/bin/env bash
# Build a mod with all Gradle state kept under ~/.cache (excluded from workspace snapshots).
# Usage: MODID=tubombers tools/build.sh [gradle tasks...]   (default MODID=tubombers, task build); copies the jar to ~/release/
set -euo pipefail
MODID=${MODID:-tubombers}
export JAVA_HOME=/home/user/.cache/mc/jdk/jdk-17.0.20.1+1 PATH=/home/user/.cache/mc/jdk/jdk-17.0.20.1+1/bin:$PATH
export GRADLE_USER_HOME=/home/user/.cache/mc/gradle-home   # wrapper distribution goes here too, not ~/.gradle
cd /home/user/mods/$MODID
[ "$MODID" = aiarsenal ] && /home/user/tools/glb_stash.sh restore
chmod +x gradlew
./gradlew --project-cache-dir /home/user/.cache/mc/gradle --gradle-user-home "$GRADLE_USER_HOME" "${@:-build}" -q -Dorg.gradle.jvmargs="-Xmx700m"
V=$(grep '^mod_version' gradle.properties | cut -d= -f2 | tr -d ' ')
mkdir -p /home/user/release && cp build/libs/$MODID-$V.jar /home/user/release/
ls -la /home/user/release/$MODID-$V.jar
