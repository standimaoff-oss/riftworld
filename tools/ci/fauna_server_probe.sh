#!/usr/bin/env bash
# Actual mob deaths and drops, in the disposable Forge server's forced-loaded overworld.
set -euo pipefail
OUT=${OUT:-ci-out}
mkdir -p "$OUT"
LOG="$OUT/fauna-server-probes.txt"
: > "$LOG"
rq() { python3 tools/rcon.py --password "${RCON_PW:-riftci}" --log '' --timeout 90 "$@"; }
fail() { echo "::error title=Fauna server probe::$*"; exit 1; }
log() { echo "$*" | tee -a "$LOG"; }
log 'Summoning the three creatures on the disposable server and verifying their actual death drops.'
rq gamerule doMobLoot true >/dev/null
for entry in 'liopleurodon:liopleurodon_tooth' 'pegasus:pegasus_feather' 'wendigo:wendigo_antler'; do
  mob=${entry%%:*}; item=${entry##*:}
  rq summon "riftworld:$mob" 40 200 0 '{NoAI:1b,NoGravity:1b,PersistenceRequired:1b,Tags:["loot_probe"]}' | tee -a "$LOG"
  rq kill "@e[type=riftworld:$mob,tag=loot_probe]" | tee -a "$LOG"
  found=''
  for _ in $(seq 1 20); do
    result=$(rq execute if entity "@e[type=minecraft:item,nbt={Item:{id:\"riftworld:$item\"}}]" 2>&1 || true)
    if [[ "$result" == *'Test passed'* ]]; then found=1; break; fi
    sleep 1
  done
  [ -n "$found" ] || fail "$mob did not drop $item"
  log "Observed $item drop from $mob."
  rq kill "@e[type=minecraft:item,nbt={Item:{id:\"riftworld:$item\"}}]" >/dev/null || true
done
log 'OK: all three creature trophies dropped in a real Forge server.'
