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
log 'Summoning the creatures on the disposable server and verifying their actual death drops.'
rq gamerule doMobLoot true >/dev/null
for entry in 'liopleurodon:liopleurodon_tooth' 'silver_sea_serpent:sea_scale' 'ghost_squid:abyssal_ink_sac'; do
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
log 'OK: all creature trophies dropped in a real Forge server.'

log 'Summoning and ticking all 14 v0.49.0 Elisar biome fauna creatures on the live Forge server.'
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

log 'Verifying v0.48.0 armory recipes, Villager ducat trades, Guard & Archer equipment, and live AI.'
ARMORY_PROBE=$(rq execute in minecraft:overworld run riftstruct v48_armory_probe)
log "v48_armory_probe: $ARMORY_PROBE"
[[ "$ARMORY_PROBE" == *"recipes_ok=true"* ]] || fail "v0.48.0 recipes check failed: $ARMORY_PROBE"
[[ "$ARMORY_PROBE" == *"guard_equipped=true"* ]] || fail "v0.48.0 guard equipment failed: $ARMORY_PROBE"
[[ "$ARMORY_PROBE" == *"archer_equipped=true"* ]] || fail "v0.48.0 archer equipment failed: $ARMORY_PROBE"
# v0.54.0: торговец должен и продавать, и покупать; монетная цепочка — двухступенчатая.
[[ "$ARMORY_PROBE" == *"chain_ok=true"* ]] || fail "v0.54.0 coin chain (ingot -> coin) failed: $ARMORY_PROBE"
[[ "$ARMORY_PROBE" == *"sell_offers="* ]] || fail "v0.54.0 sell offers missing: $ARMORY_PROBE"
[[ "$ARMORY_PROBE" == *"buy_offers="* ]] || fail "v0.54.0 buy offers missing: $ARMORY_PROBE"
SELL_OFFERS=$(sed -n 's/.*sell_offers=\([0-9]*\).*/\1/p' <<<"$ARMORY_PROBE")
BUY_OFFERS=$(sed -n 's/.*buy_offers=\([0-9]*\).*/\1/p' <<<"$ARMORY_PROBE")
[[ -n "$SELL_OFFERS" && "$SELL_OFFERS" -ge 3 ]] || fail "v0.54.0 villager sells too little: $ARMORY_PROBE"
[[ -n "$BUY_OFFERS" && "$BUY_OFFERS" -ge 3 ]] || fail "v0.54.0 villager buys too little: $ARMORY_PROBE"

# v0.54.0: монетная цепочка и экономика на живом сервере.
ECON_PROBE=$(rq execute in minecraft:overworld run riftstruct v29_crafting_and_coin_ore)
log "v29_crafting_and_coin_ore: $ECON_PROBE"
[[ "$ECON_PROBE" == *"passed_crafts=11/11"* ]] || fail "v0.54.0 coin chain / economy probe failed: $ECON_PROBE"
[[ "$ECON_PROBE" == *"ducat_ingot_smelt=true"* ]] || fail "v0.54.0 ore -> ingot smelting failed: $ECON_PROBE"
[[ "$ECON_PROBE" == *"ducat_coin_smelt=true"* ]] || fail "v0.54.0 ingot -> coin smelting failed: $ECON_PROBE"
[[ "$ECON_PROBE" == *"no_direct_coin=true"* ]] || fail "v0.54.0 direct ore -> coin shortcut still exists: $ECON_PROBE"

# v0.54.0: точные профессии — команда показывает состав и назначает профессию.
PROF_PROBE=$(rq execute in minecraft:overworld run riftprofession)
log "riftprofession: $PROF_PROBE"
[[ "$PROF_PROBE" == *"MASON"* && "$PROF_PROBE" == *"MERCHANT"* ]] || fail "v0.54.0 profession list failed: $PROF_PROBE"
for folk in elisar_villager elisar_guard elisar_archer; do
  rq summon "riftworld:$folk" 40 200 0 '{PersistenceRequired:1b,NoGravity:1b,Tags:["v048_folk"]}' | tee -a "$LOG"
  sleep 0.3
  chk=$(rq execute if entity "@e[type=riftworld:$folk,tag=v048_folk]" 2>&1 || true)
  [[ "$chk" == *'Test passed'* ]] || fail "v0.48.0 folk $folk failed to spawn or tick"
  rq tp "@e[type=riftworld:$folk,tag=v048_folk]" 40 -200 0 >/dev/null || true
  rq kill "@e[type=riftworld:$folk,tag=v048_folk]" >/dev/null || true
done
log 'OK: v0.48.0 armory, dual tool sets, and 3 Elisar settlement folk verified on live Forge server.'
