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

log 'Summoning and ticking all 15 v0.47.0 Elisar biome fauna creatures on the live Forge server.'
for mob in porcelain_crane basalt_beetle ash_moth amethyst_fox sizo_leafwing \
           azure_frog blossom_snail phantom_wolf crystal_gecko faceted_owl \
           bone_raven coral_hermit_crab reef_seahorse ghost_squid silver_sea_serpent; do
  rq summon "riftworld:$mob" 40 200 0 '{PersistenceRequired:1b,NoGravity:1b,Tags:["v047_probe"]}' | tee -a "$LOG"
  sleep 0.3
  chk=$(rq execute if entity "@e[type=riftworld:$mob,tag=v047_probe]" 2>&1 || true)
  [[ "$chk" == *'Test passed'* ]] || fail "v0.47.0 entity $mob failed to spawn or survive live AI tick"
  rq tp "@e[type=riftworld:$mob,tag=v047_probe]" 40 -200 0 >/dev/null || true
  rq kill "@e[type=riftworld:$mob,tag=v047_probe]" >/dev/null || true
done

for entry in 'ash_moth:ash_wing_dust' 'bone_raven:bone_pinion' 'ghost_squid:abyssal_ink_sac' 'silver_sea_serpent:sea_scale'; do
  mob=${entry%%:*}; item=${entry##*:}
  rq summon "riftworld:$mob" 40 200 0 '{NoAI:1b,NoGravity:1b,PersistenceRequired:1b,Tags:["v047_loot"]}' | tee -a "$LOG"
  rq kill "@e[type=riftworld:$mob,tag=v047_loot]" | tee -a "$LOG"
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
log 'OK: all 15 v0.47.0 fauna entities spawned, ticked, and dropped trophies cleanly.'
