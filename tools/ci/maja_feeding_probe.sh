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
read -r IX IZ R AX AY AZ < <(python3 - "$SITE" <<'PY'
import re,sys,math
v=dict(re.findall(r'(x|z|radius|reach|base|anchor_x|anchor_y|anchor_z)=(-?[\d.]+)',sys.argv[1]))
print(int(v['x']),int(v['z']),math.ceil(float(v['reach']))+10,int(v['anchor_x']),int(v['anchor_y']),int(v['anchor_z']))
PY
)
# Clipped rectangles overlap; do not force more than the vanilla 256-chunk cap.
A=$(rq execute in riftworld:elisar run forceload add $((AX-70)) $((AZ-170)) $((AX+70)) $((AZ+170)))
log "body: $A"; [[ "$A" == *'Marked'* ]] || fail 'body forceload failed'
B=$(rq execute in riftworld:elisar run forceload add $((IX-R)) $((IZ-R)) $((IX+R)) $((IZ+R)))
log "island: $B"; [[ "$B" == *'Marked'* || "$B" == *'already'* ]] || fail 'island forceload failed'
rq execute in riftworld:elisar run gamerule mobGriefing false >/dev/null
rq execute in riftworld:elisar run summon riftworld:el_gran_maja "$AX" "$AY" "$AZ" '{PersistenceRequired:1b,majaMotionTick:500}' | tee -a "$LOG"
SEL='@e[type=riftworld:el_gran_maja,limit=1]'
sleep 15
READY=$(rq execute in riftworld:elisar run data get entity "$SEL" majaReady)
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
