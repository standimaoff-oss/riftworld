#!/usr/bin/env bash
# Reproducible, non-destructive profiler for the disposable CI server.
# Run after server_check.sh (KEEP=1), never in a player's world.
set -euo pipefail
OUT=${OUT:-ci-out}
mkdir -p "$OUT"
LOG="$OUT/performance.txt"
: > "$LOG"
rq() { python3 tools/rcon.py --password "${RCON_PW:-riftci}" --log '' --timeout 240 "$@"; }
log() { echo "$*" | tee -a "$LOG"; }
# The dedicated server is the largest JVM here. jps ignores shells/grep and
# avoids relying on the shell's background-pid bookkeeping in server_check.sh.
PID=$(ps -C java -o pid=,rss= | sort -k2,2nr | awk 'NR==1 {print $1}')
[ -n "$PID" ] || { echo '::error title=Profiler::No server JVM'; exit 1; }
log "Server JVM: $PID"
profile_start() { jcmd "$PID" JFR.start name="$1" settings=profile maxsize=96m | tee -a "$LOG"; }
profile_stop() {
  local name=$1
  jcmd "$PID" JFR.stop name="$name" filename="$PWD/$OUT/$name.jfr" | tee -a "$LOG"
  jfr print --events jdk.ExecutionSample --stack-depth 64 --json "$OUT/$name.jfr" > "$OUT/$name.json"
  python3 - "$name" "$OUT/$name.json" <<'PY' | tee -a "$LOG"
import collections, json, sys
name, path = sys.argv[1:]
events = json.load(open(path))['recording']['events']
inclusive, top = collections.Counter(), collections.Counter()
for event in events:
    frames = (event['values'].get('stackTrace') or {}).get('frames') or []
    names = []
    for frame in frames:
        method = frame.get('method') or {}
        typ = method.get('type') or {}
        cls = typ.get('name', '').replace('/', '.')
        if cls.startswith('ru.aiforge.riftworld.'):
            names.append(cls + '.' + method.get('name', ''))
    if names:
        top[names[0]] += 1
        inclusive.update(set(names))
print(f'{name}: total samples={len(events)}; samples containing mod code={sum(top.values())}')
for method, count in inclusive.most_common(12):
    print(f'  inclusive {count:4d}  {method}')
print('  top mod frames:')
for method, count in top.most_common(8):
    print(f'    {count:4d}  {method}')
summary = ', '.join(f'{n}:{c}' for n,c in inclusive.most_common(5)) or 'no mod samples'
print(f'::notice title=Profile {name}::{summary}')
PY
}

# First region centre of the deterministic deep ocean. Same chunks/seed on
# baseline and candidate; /forceload never touches the player's saves in CI.
log 'Generating/loading a 6x6 chunk square in a new Elisar ocean at 1715,2126.'
profile_start generation
START=$(date +%s)
set +e
rq execute in riftworld:elisar run forceload add 1664 2064 1759 2159 > "$OUT/force-load.txt" 2>&1
RC=$?
set -e
ELAPSED=$(( $(date +%s) - START ))
cat "$OUT/force-load.txt" | tee -a "$LOG"
log "Force-load command: exit=$RC, wall=${ELAPSED}s (ticket creation, not full chunk completion)."
# Let async generation run after ticket creation; do not present this as TPS.
sleep 30
rq forge tps | tee -a "$LOG" || true
profile_stop generation
[ "$RC" = 0 ] || { echo '::error title=Profiler::Elisar forceload failed/timed out'; exit 1; }

log 'Testing the full-size titan on a loaded flat world, then sampling its tick cost.'
bash tools/ci/maja_server_probe.sh | tee -a "$LOG"
profile_start titan
sleep 35
rq forge tps | tee -a "$LOG" || true
profile_stop titan
log 'Profiler finished. JFR samples are diagnostic, not an FPS or speedup claim.'
