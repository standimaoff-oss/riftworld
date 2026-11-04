#!/usr/bin/env bash
# Integration probe on a real Forge server, in the disposable superflat overworld.
# Requires the server from server_check.sh with KEEP=1 and RCON enabled.
set -euo pipefail
OUT=${OUT:-ci-out}
mkdir -p "$OUT"
LOG="$OUT/maja-probes.txt"
: > "$LOG"
rq() { python3 tools/rcon.py --password "${RCON_PW:-riftci}" --log "" --timeout 90 "$@"; }
fail() { echo "::error title=Maja server probe::$*"; exit 1; }
log() { echo "$*" | tee -a "$LOG"; }
on_error() {
  code=$?
  if [ "$code" -ne 0 ]; then
    echo "::error title=Maja server probe::Unexpected error at line ${BASH_LINENO[0]} (exit $code)"
    tail -15 ci/server/logs/latest.log 2>/dev/null | while IFS= read -r line; do echo "::error title=server::${line:0:300}"; done
  fi
}
trap on_error EXIT

log "Force-loading a 253-chunk rectangle for the 322-block body..."
rq forceload add -80 -176 80 176 | tee -a "$LOG"
rq summon riftworld:el_gran_maja 0 200 0 '{NoAI:1b,PersistenceRequired:1b}' | tee -a "$LOG"
SEL='@e[type=riftworld:el_gran_maja,limit=1]'
wait_ready() {
  local expected=$1 state
  for _ in $(seq 1 100); do
    state=$(rq data get entity "$SEL" majaReady 2>&1 || true)
    if [[ "$state" == *"$expected"* ]]; then log "majaReady=$expected"; return 0; fi
    sleep 2
  done
  fail "Expected majaReady=$expected; got: $state"
}
wait_ready 1b
log "Placing a solid block inside the forward head envelope..."
rq setblock 0 200 155 minecraft:stone | tee -a "$LOG"
wait_ready 0b
rq setblock 0 200 155 minecraft:air | tee -a "$LOG"
wait_ready 1b
log "A world border smaller than the body must also reject it..."
rq worldborder set 300 | tee -a "$LOG"
wait_ready 0b
rq worldborder set 59999968 | tee -a "$LOG"
wait_ready 1b
log "Allowing AI: the anchor must actually move while the area is clear."
BEFORE=$(rq data get entity "$SEL" Pos)
rq data merge entity "$SEL" '{NoAI:0b}' | tee -a "$LOG"
sleep 5
AFTER=$(rq data get entity "$SEL" Pos)
python3 - "$BEFORE" "$AFTER" <<'PY' || fail "Anchor did not move in empty, loaded space"
import re,sys
vectors=[]
for text in sys.argv[1:]:
    m=re.search(r'\[\s*([-\d.eE+]+)d?\s*,\s*([-\d.eE+]+)d?\s*,\s*([-\d.eE+]+)d?\s*\]',text)
    if not m: raise SystemExit('Cannot parse Pos: '+text)
    vectors.append(tuple(float(x) for x in m.groups()))
d=sum((a-b)**2 for a,b in zip(*vectors))**.5
print('before:',vectors[0],'after:',vectors[1],'distance:',round(d,3))
if not .1<d<25: raise SystemExit('unexpected motion')
PY
rq data merge entity "$SEL" '{NoAI:1b}' | tee -a "$LOG"
log "OK: live server tested readiness, solid-block collision, border containment and movement."
