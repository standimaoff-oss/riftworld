#!/usr/bin/env bash
# Destructive test on an isolated, newly created CI world only.
set -Eeuo pipefail
OUT=${OUT:-ci-out}; mkdir -p "$OUT"
LOG="$OUT/maja-feeding.txt"; : > "$LOG"
rq() { python3 tools/rcon.py --password "${RCON_PW:-riftci}" --log '' --timeout 240 "$@"; }
log() { echo "$*" | tee -a "$LOG"; }
fail() { log "FAILED: $*"; tail -25 ci/server/logs/latest.log | tee -a "$LOG" || true; exit 1; }
grep -Eq 'allowIslandEating[[:space:]]*=[[:space:]]*true' ci/server/config/riftworld-maja.toml || fail 'test config is not enabled'
SITE=$(rq execute in riftworld:elisar positioned 1715 70 2126 run riftmajaisland)
log "$SITE"
read -r IX IZ R AX AY AZ BASE < <(python3 - "$SITE" <<'PY'
import re,sys,math
v=dict(re.findall(r'(x|z|radius|reach|base|anchor_x|anchor_y|anchor_z)=(-?[\d.]+)',sys.argv[1]))
print(int(v['x']),int(v['z']),math.ceil(float(v['reach']))+10,int(v['anchor_x']),int(v['anchor_y']),int(v['anchor_z']),int(float(v['base'])))
PY
)
# Clipped rectangles overlap; do not force more than the vanilla 256-chunk cap.
A=$(rq execute in riftworld:elisar run forceload add $((AX-70)) $((AZ-170)) $((AX+70)) $((AZ+170)))
log "body: $A"; [[ "$A" == *'Marked'* ]] || fail 'body forceload failed'
B=$(rq execute in riftworld:elisar run forceload add $((IX-R)) $((IZ-R)) $((IX+R)) $((IZ+R)))
log "island: $B"; [[ "$B" == *'Marked'* || "$B" == *'already'* ]] || fail 'island forceload failed'
# Find a genuinely clear pose without weakening the 322-block collision check.
# Nearby islands can obstruct the first analytic anchor at one elevation.
found=0
# Кандидатов больше и они привязаны к BASE: мир каждый прогон новый, и при
# старом списке (100/110/120/130) почти все высоты отсекались условием
# Y <= BASE+8 — прогон падал на случайном рельефе, а не на настоящей ошибке.
CANDIDATES="$AY $((AY+6)) $((AY-6)) $((AY+12)) $((AY-12)) $((BASE+5)) $((BASE+2)) $((BASE-5)) 100 110 120 130"
for Y in $CANDIDATES; do
  (( Y >= BASE - 75 && Y <= BASE + 8 )) || continue
  for _ in $(seq 1 8); do
    POSE=$(rq execute in riftworld:elisar positioned "$AX" "$Y" "$AZ" rotated 0 0 run riftmajapose 2>&1 || true)
    [[ "$POSE" != *'unloaded('* ]] && break
    sleep 3
  done
  log "candidate y=$Y: $POSE"
  if [[ "$POSE" == *'clear=true'* ]]; then AY=$Y; found=1; break; fi
done
[ "$found" = 1 ] || fail 'no collision-safe pose near selected island'
rq execute in riftworld:elisar run gamerule mobGriefing false >/dev/null
rq execute in riftworld:elisar run summon riftworld:el_gran_maja "$AX" "$AY" "$AZ" '{PersistenceRequired:1b,majaMotionTick:500}' | tee -a "$LOG"
SEL='@e[type=riftworld:el_gran_maja,limit=1]'
READY=''
for _ in $(seq 1 30); do
  READY=$(rq execute in riftworld:elisar run data get entity "$SEL" majaReady 2>&1 || true)
  [[ "$READY" == *'1b'* ]] && break
  sleep 2
done
# Wait for candidate-search opportunities rather than checking immediately after spawn.
TICKS=0
for _ in $(seq 1 90); do
  TICK_OUTPUT=$(rq execute in riftworld:elisar run data get entity "$SEL" majaTicks 2>&1 || true)
  TICKS=$(echo "$TICK_OUTPUT" | sed -n 's/.*: \([0-9][0-9]*\)$/\1/p')
  [[ "${TICKS:-0}" -ge 120 ]] && break
  sleep 2
done
rq execute in riftworld:elisar as "$SEL" at @s run riftmajapose | tee -a "$LOG" || true
log "griefing=false active ticks: ${TICKS:-0}"
[[ "${TICKS:-0}" -ge 120 ]] || fail 'not enough active ticks to exercise the mobGriefing gate'
COUNT=$(rq execute in riftworld:elisar run data get entity "$SEL" majaBlocksEaten)
log "griefing=false: $READY; $COUNT"
[[ "$READY" == *'1b'* && "$COUNT" == *'0' ]] || fail 'unready or ate with mobGriefing disabled'
rq execute in riftworld:elisar run tp "$SEL" "$AX" "$AY" "$AZ" 0 0 >/dev/null
rq execute in riftworld:elisar run gamerule mobGriefing true >/dev/null
for _ in $(seq 1 90); do
  STATE=$(rq execute in riftworld:elisar run data get entity "$SEL" majaFeedStatus 2>&1 || true)
  COUNT=$(rq execute in riftworld:elisar run data get entity "$SEL" majaBlocksEaten 2>&1 || true)
  READY=$(rq execute in riftworld:elisar run data get entity "$SEL" majaReady 2>&1 || true)
  log "$STATE; $COUNT; $READY"
  [[ "$STATE" == *'finished'* ]] && break
  # v0.49.0: 'breaching' is the transient water-breach surge between the last bite
  # and the end of the meal — it is NOT a cancellation.
  [[ "$STATE" == *'breaching'* ]] && { log 'breach surge observed'; sleep 4; continue; }
  [[ "$STATE" == *'scan_blocked'* || "$STATE" == *'changed_or_denied'* || "$STATE" == *'unsafe'* ]] && fail 'feeding cancelled'
  sleep 4
done
[[ "$STATE" == *'finished'* ]] || fail 'medium island eating did not finish'
python3 - "$COUNT" <<'PY' || fail 'no blocks were eaten'
import re,sys
m=re.search(r':\s*(\d+)\s*$',sys.argv[1])
assert m and int(m.group(1)) > 1000, sys.argv[1]
PY
log 'OK: full medium island job completed with mobGriefing enabled; disabled gate preserved blocks.'

# v0.49.0: a SINGLE recorded player placement must abort the meal (fail-closed gate).
log 'Testing the player-placement gate (one placed block must stop the titan)...'
rq execute in riftworld:elisar run kill "$SEL" >/dev/null 2>&1 || true
sleep 3
rq execute in riftworld:elisar run summon riftworld:el_gran_maja "$AX" "$AY" "$AZ" '{PersistenceRequired:1b,majaMotionTick:500}' | tee -a "$LOG"
sleep 10
# One single block placed by the operator on the island must be journalled and block the meal.
rq execute in riftworld:elisar run setblock "$IX" "$BASE" "$IZ" minecraft:stone | tee -a "$LOG"
PLACED=0
for _ in $(seq 1 60); do
  STATE=$(rq execute in riftworld:elisar run data get entity "$SEL" majaFeedStatus 2>&1 || true)
  COUNT=$(rq execute in riftworld:elisar run data get entity "$SEL" majaBlocksEaten 2>&1 || true)
  log "placement gate: $STATE; $COUNT"
  if [[ "$STATE" == *'unsafe'* || "$STATE" == *'empty'* || "$STATE" == *'changed_or_denied'* || "$STATE" == *'scan_blocked'* ]]; then
    PLACED=1; break
  fi
  sleep 4
done
[[ "$PLACED" = 1 ]] || fail 'the titan kept eating after a single player placement'
log 'OK: a single player placement aborted the meal.'
