#!/usr/bin/env bash
# Disposable real-server test: the first deterministic ocean region is centred
# at (1715,2126). Wait for both a mast and a hull block in newly generated chunks.
set -euo pipefail
mkdir -p ci-out
rq() { python3 tools/rcon.py --password "${RCON_PW:-riftci}" --log '' --timeout 240 "$@"; }
rq execute in riftworld:elisar run forceload add 2288 2112 2335 2143 | tee ci-out/ocean-wreck.txt
for spec in '2324 82 2126 minecraft:dark_oak_log' '2303 64 2133 minecraft:spruce_planks'; do
  ready=0
  for _ in $(seq 1 90); do
    result=$(rq execute in riftworld:elisar if block $spec run time query gametime 2>&1 || true)
    if [[ "$result" == *'The time is'* ]]; then ready=1; break; fi
    sleep 2
  done
  echo "$spec: $ready ($result)" | tee -a ci-out/ocean-wreck.txt
  [ "$ready" = 1 ] || { echo '::error::Ocean wreck block missing'; exit 1; }
done
