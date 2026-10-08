#!/usr/bin/env bash
# Publishes riftworld sources to the private repo standimaoff-oss/riftworld (token only in ~/.git-credentials).
set -euo pipefail
H=/home/user; R=$H/.cache/riftworld-git; URL=https://github.com/standimaoff-oss/riftworld.git; MSG=${1:-update}
# Rev 5: the sources live in the repository itself (this checkout, branch arena/3e6d6463-riftworld) and are
# published with git push; this script is the legacy publisher of the sandbox layout ~/mods/riftworld and only
# runs when that layout is present.
if [ ! -d $H/mods/riftworld ]; then
  echo "gh_push_rift.sh: $H/mods/riftworld not found - publish with: git push origin $(git rev-parse --abbrev-ref HEAD)" >&2
  exit 2
fi
if [ ! -d $R/.git ]; then
  rm -rf $R; git clone -q $URL $R 2>/dev/null || { mkdir -p $R; cd $R; git init -q -b main; git remote add origin $URL; }
fi
cd $R; git config user.name "riftworld-bot"; git config user.email "riftworld-bot@users.noreply.github.com"
git pull -q --rebase origin main 2>/dev/null || true
mkdir -p mod tools blender reports
rsync -a --delete --exclude build/ --exclude .gradle/ --exclude 'run*/' --exclude src/generated/ $H/mods/riftworld/ mod/
rsync -a --delete --exclude __pycache__/ $H/tools/rift/ tools/rift/
for f in build.sh setup_all.sh setup_server.sh setup_client.sh rcon.py snap.sh src_zip.sh gh_push_rift.sh client_options.txt; do [ -f $H/tools/$f ] && cp $H/tools/$f tools/; done
rsync -a --delete $H/models/rift/ blender/
cp $H/reports/riftworld.md reports/; cp $H/reports/riftworld.md README.md
git add -A
if git diff --cached --quiet; then echo "nothing to commit"; else git commit -qm "$MSG"; fi
git push -q -u origin main
echo "pushed $(git rev-parse --short HEAD) $(git ls-files | wc -l) files"
