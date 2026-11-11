#!/usr/bin/env bash
# Real Forge world, new deterministic ocean; never run against a player's world.
set -euo pipefail
mkdir -p ci-out
rq() { python3 tools/rcon.py --password "${RCON_PW:-riftci}" --log '' --timeout 240 "$@"; }
report=ci-out/deep-ocean.txt
rq execute in riftworld:elisar run forceload add 1645 1956 1785 2296 | tee "$report"
# Wait for actual full chunks, not just the forceload ticket.
# Сервер только что поднялся, и иногда генератор не успевает за отведённое
# время: колонка остаётся пустой, и проба падала вслепую. Поэтому ждём дольше
# и повторяем форсированную загрузку — ticket мог не дойти до генератора.
for _ in $(seq 1 180); do
  result=$(rq execute in riftworld:elisar if block 1715 -64 2126 minecraft:bedrock run time query gametime 2>&1 || true)
  [[ "$result" == *'The time is'* ]] && break
  rq execute in riftworld:elisar run forceload add 1645 1956 1785 2296 >/dev/null 2>&1 || true
  sleep 2
done
[[ "$result" == *'The time is'* ]] || { echo '::error::Deep ocean chunk did not load'; exit 1; }
for point in '1715 2126' '1680 2090' '1750 2160'; do
  read -r x z <<< "$point"
  for start in -57 -27 3 33; do
    cmd=(execute in riftworld:elisar)
    for y in $(seq "$start" $((start+29))); do cmd+=(if block "$x" "$y" "$z" riftworld:elisar_water); done
    cmd+=(run time query gametime)
    result=$(rq "${cmd[@]}")
    [[ "$result" == *'The time is'* ]] || { echo "::error::Missing water at $point y=$start..$((start+29)): $result"; exit 1; }
  done
  result=$(rq execute in riftworld:elisar unless block "$x" -58 "$z" minecraft:air unless block "$x" -58 "$z" riftworld:elisar_water run time query gametime)
  [[ "$result" == *'The time is'* ]] || { echo "::error::Missing seabed at $point y=-58"; exit 1; }
  echo "$point: 120 water blocks y=-57..62, seabed y=-58" | tee -a "$report"
done
rq execute in riftworld:elisar run summon riftworld:el_gran_maja 1715 30 2126 '{Tags:["deep_test"],majaMotionTick:600}' | tee -a "$report"
sleep 22
selector='@e[type=riftworld:el_gran_maja,tag=deep_test,limit=1]'
rq execute in riftworld:elisar run data get entity "$selector" | tee -a "$report" | grep -q 'majaReady: 1b' || { echo '::error::Submerged Maja is not collision-ready'; exit 1; }
rq execute in riftworld:elisar run data get entity "$selector" Pos | tee -a "$report"
# Forge custom-fluid breathing is separate from vanilla water. Wait beyond
# the 300-tick air supply and check both air and health, not just readiness.
rq execute in riftworld:elisar run data get entity "$selector" Air | tee -a "$report" | grep -q '300s' || { echo '::error::Maja is consuming air in Elisar water'; exit 1; }
rq execute in riftworld:elisar run data get entity "$selector" Health | tee -a "$report" | grep -q '1024.0f' || { echo '::error::Submerged Maja lost health'; exit 1; }
# At this phase she should swim at y=30, wholly under the surface.
rq execute in riftworld:elisar if entity '@e[type=riftworld:el_gran_maja,tag=deep_test,y=28,dy=4,x=1600,dx=230,z=1900,dz=400]' run time query gametime | grep -q 'The time is' || { echo '::error::Maja left the submerged cruising depth'; exit 1; }
rq execute in riftworld:elisar run kill "$selector" | tee -a "$report"
echo 'OK: fresh deep-ocean columns and submerged collision-ready motion.' | tee -a "$report"
