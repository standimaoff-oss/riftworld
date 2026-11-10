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

CONFIG=ci/server/config/riftworld-maja.toml
[ -f "$CONFIG" ] || fail "Maja config missing"
# v0.49.0: island eating is ENABLED by default (the titan is a rare, always-on event).
grep -Eq 'allowIslandEating[[:space:]]*=[[:space:]]*true' "$CONFIG" || fail "Island eating must be enabled by default in v0.49.0"
log "Island eating is enabled by default (v0.49.0 rare-event titan)."
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
log "A waterlogged solid must still block the full model..."
rq setblock 0 200 155 'minecraft:oak_slab[type=bottom,waterlogged=true]' | tee -a "$LOG"
wait_ready 0b
rq setblock 0 200 155 minecraft:air | tee -a "$LOG"
wait_ready 1b
log "A waterlogged, no-collision sea decal must remain passable..."
rq setblock 0 200 155 'riftworld:sea_shells[waterlogged=true]' | tee -a "$LOG"
rq execute if block 0 200 155 riftworld:sea_shells | grep -q 'Test passed' || fail "Sea decal was not placed"
wait_ready 1b
rq execute if block 0 200 155 riftworld:sea_shells | grep -q 'Test passed' || fail "Sea decal disappeared before the check"
rq setblock 0 200 155 minecraft:air | tee -a "$LOG"
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
log "Before motion: $BEFORE"
log "After motion: $AFTER"
python3 - "$BEFORE" "$AFTER" <<'PY' || fail "Motion probe: before=$BEFORE after=$AFTER"
import re,sys
vectors=[]
for text in sys.argv[1:]:
    m=re.search(r'\[\s*([-\d.eE+]+)d?\s*,\s*([-\d.eE+]+)d?\s*,\s*([-\d.eE+]+)d?\s*\]',text)
    if not m: raise SystemExit('Cannot parse Pos: '+text)
    vectors.append(tuple(float(x) for x in m.groups()))
d=sum((a-b)**2 for a,b in zip(*vectors))**.5
print('before:',vectors[0],'after:',vectors[1],'distance:',round(d,3))
# v0.49.0: Гран Маджа стал БЫСТРЕЕ (это требование владельца), и за окно в
# 5 секунд он проходит ~57 блоков. Граница проверки должна ловить только
# «замороженный якорь» или телепорт, а не полировать скорость крейсера.
# (Рев 16.4: здесь стояло d<40, потом d<60 — после ускорения Маджа оба
# варианта валили сборку.)
if not .1<d<120: raise SystemExit('unexpected motion')
# Мгновенный телепорт не считается «движением»: за 5 секунд крейсер Маджа
# проходит десятки блоков, но не сотни. Отсекаем явный телепорт.
if d>110: raise SystemExit('teleport, not motion')
print('speed:',round(d/5.0,2),'blocks/s')
PY
# Force the beginning of a breach on the disposable flat world. The Y
# displacement is measured on the server, not inferred from screenshots.
rq data merge entity "$SEL" '{NoAI:1b}' >/dev/null
rq tp "$SEL" 0 60 0 0 0 >/dev/null
rq data merge entity "$SEL" '{NoAI:0b,majaMotionTick:125}' >/dev/null
BEFORE=$(rq data get entity "$SEL" Pos)
sleep 5
AFTER=$(rq data get entity "$SEL" Pos)
log "Breach before: $BEFORE"
log "Breach after: $AFTER"
python3 - "$BEFORE" "$AFTER" <<'PYBREACH' || fail "Maja failed to rise during its breach"
import re,sys
ys=[]
for text in sys.argv[1:]:
    m=re.search(r'\[\s*([-\d.eE+]+)d?\s*,\s*([-\d.eE+]+)d?\s*,\s*([-\d.eE+]+)d?\s*\]',text)
    if not m: raise SystemExit('bad position '+text)
    ys.append(float(m.group(2)))
print('vertical rise:', ys[1]-ys[0])
if ys[1]-ys[0] < 3: raise SystemExit('breach did not rise')
PYBREACH
rq data merge entity "$SEL" '{NoAI:1b}' | tee -a "$LOG"
rq tp "$SEL" 0 200 0 0 0 | tee -a "$LOG"
wait_ready 1b
log "OK: live server tested readiness, solid-block collision, border containment and movement."
