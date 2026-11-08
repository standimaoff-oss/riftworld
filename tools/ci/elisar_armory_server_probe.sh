#!/usr/bin/env bash
# Run while tools/ci/server_check.sh is keeping its flat Forge server alive (KEEP=1).
set -uo pipefail
OUT=${OUT:-ci-out}
RCON_PW=${RCON_PW:-riftci}
mkdir -p "$OUT"
FAILS=0
Rq() { python3 tools/rcon.py --password "$RCON_PW" --log "" --timeout 30 "$@"; }
check_output() {
  local name=$1 pattern=$2
  shift 2
  echo "== $name"
  Rq "$@" > "$OUT/elisar_armory_${name}.txt" 2>&1
  local rc=$?
  cat "$OUT/elisar_armory_${name}.txt"
  if [ "$rc" -ne 0 ] || ! grep -q "$pattern" "$OUT/elisar_armory_${name}.txt"; then
    echo "::error title=Elisar armory::$name failed (rc=$rc, expected $pattern)"
    FAILS=$((FAILS+1))
  fi
}

check_output summon_villager 'Summoned new' summon riftworld:elisar_villager 4 6 4
check_output summon_guard 'Summoned new' summon riftworld:elisar_guard 6 6 4
check_output summon_archer 'Summoned new' summon riftworld:elisar_archer 8 6 4
check_output guard_equipment 'aurite_sword' data get entity '@e[type=riftworld:elisar_guard,limit=1,sort=nearest]' HandItems
check_output guard_shield 'aurite_shield' data get entity '@e[type=riftworld:elisar_guard,limit=1,sort=nearest]' HandItems
check_output archer_equipment 'aurite_bow' data get entity '@e[type=riftworld:elisar_archer,limit=1,sort=nearest]' HandItems

check_output place_ore 'Changed the block' setblock 12 6 12 riftworld:riftcrystal_ore
check_output place_deepslate_ore 'Changed the block' setblock 14 6 12 riftworld:deepslate_riftcrystal_ore
check_output place_loot_chest 'Changed the block' setblock 12 6 14 minecraft:chest
check_output mine_with_diamond_pickaxe '' loot insert 12 6 14 mine 12 6 12 minecraft:diamond_pickaxe
check_output shard_loot 'riftcrystal_shard' data get block 12 6 14 Items

# Silk Touch must return the block itself instead of shards.
check_output place_silk_chest 'Changed the block' setblock 14 6 14 minecraft:chest
Rq loot insert 14 6 14 mine 14 6 12 'minecraft:diamond_pickaxe{Enchantments:[{id:"minecraft:silk_touch",lvl:1s}]}' > "$OUT/elisar_armory_mine_silk.txt" 2>&1 || FAILS=$((FAILS+1))
check_output silk_ore_loot 'deepslate_riftcrystal_ore' data get block 14 6 14 Items

# A representative of each equipment family proves item registry/NBT creation on the live server.
for item in aurite_sword aurite_pickaxe aurite_war_axe aurite_bow aurite_shield aurite_helmet \
            riftcrystal_sword riftcrystal_pickaxe riftcrystal_war_axe riftcrystal_bow riftcrystal_shield riftcrystal_helmet; do
  check_output "item_${item}" 'Summoned new' summon minecraft:item 18 7 18 "{Item:{id:\"riftworld:${item}\",Count:1b}}"
done

if [ "$FAILS" -ne 0 ]; then
  echo "::error title=Elisar armory::$FAILS live checks failed"
  exit 1
fi
echo "Elisar armory: all live registry, NPC equipment and ore-loot checks passed"
