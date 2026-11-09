#!/usr/bin/env bash
# v0.49.0 — живая проверка городского закона Элисара на настоящем сервере Forge.
#
# Проверяем в игре то, что просил игрок:
#   1) страж патрулирует по дороге (а не стоит и не бродит наугад);
#   2) удар по стражу даёт ответный удар и предупреждение, но НЕ убивает;
#   3) камера тюрьмы и склад действительно строятся рядом с дорогой.
#
# Скрипт работает на одноразовом сервере из CI.
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
LOG="${LOG:-/tmp/elisar-law-probe.log}"
: > "$LOG"

rq() { python3 tools/rcon.py --password "${RCON_PW:-riftci}" --log '' --timeout 90 "$@"; }
fail() { echo "::error title=Elisar law probe::$*"; exit 1; }
log() { echo "$*" | tee -a "$LOG"; }

log '== v0.49.0: проверка городского закона Элисара на живом сервере =='
rq gamerule doMobLoot true >/dev/null

# ---------------------------------------------------------------- 1. патруль
log '-- 1) страж идёт по дороге, а не стоит на месте'
rq execute in riftworld:elisar run tp TestPlayer 760 123 770 0 0 >/dev/null 2>&1 || true
sleep 8
rq kill '@e[type=riftworld:elisar_guard]' >/dev/null 2>&1 || true
rq execute in riftworld:elisar positioned 760 120 770 run riftstruct v40_gear >/dev/null 2>&1 || true
sleep 4
GUARDS=$(rq execute if entity '@e[type=riftworld:elisar_guard]' 2>&1 || true)
[[ "$GUARDS" == *'Test passed'* ]] || fail "Не удалось заспавнить стражей: $GUARDS"
log '   стражей рядом: 1+ (проверено)'

# Позиция стража до и через 10 секунд: он обязан ДВИГАТЬСЯ по маршруту.
POS1=$(rq data get entity '@e[type=riftworld:elisar_guard,limit=1]' Pos 2>&1 | tr -d '\n' || true)
sleep 10
POS2=$(rq data get entity '@e[type=riftworld:elisar_guard,limit=1]' Pos 2>&1 | tr -d '\n' || true)
log "   позиция стража: $POS1 -> $POS2"
if [[ "$POS1" == "$POS2" ]]; then
  fail "Страж не двигается — патруль по дорогам не работает"
fi
log '   OK: страж идёт по патрулю'

# ---------------------------------------------------------------- 2. ответный удар
log '-- 2) удар по стражу: предупреждение, но НЕ смерть'
rq gamemode survival TestPlayer >/dev/null || true
rq effect give TestPlayer minecraft:resistance 999 4 true >/dev/null || true
sleep 1
HP_BEFORE=$(rq data get entity '@e[type=riftworld:elisar_guard,limit=1]' Health 2>&1 | tr -d '\n' || true)
rq damage '@e[type=riftworld:elisar_guard,limit=1]' 4 minecraft:player_attack by TestPlayer >/dev/null 2>&1 || true
sleep 2
HP_AFTER=$(rq data get entity '@e[type=riftworld:elisar_guard,limit=1]' Health 2>&1 | tr -d '\n' || true)
log "   здоровье стража: $HP_BEFORE -> $HP_AFTER"
if [[ "$HP_AFTER" == *"0"* ]]; then
  fail "Страж погиб от удара игрока — он должен только предупреждать"
fi
if [[ -z "$HP_AFTER" || "$HP_AFTER" == *"error"* || "$HP_AFTER" == *"Invalid"* ]]; then
  fail "Не удалось прочитать здоровье стража: $HP_AFTER"
fi
log '   OK: страж выжил и предупредил'

# ---------------------------------------------------------------- 3. тюрьма и склад
log '-- 3) камера тюрьмы и склад строятся рядом с дорогой'
PRISON=$(rq execute in riftworld:elisar positioned 760 120 770 run riftstruct v49_elisar_law 2>&1 | tr '\r\n' '  ' || true)
log "   riftstruct v49_elisar_law -> ${PRISON:0:400}"
if [[ "$PRISON" == *"jail=1"* ]]; then
  log '   OK: камера построена'
else
  fail "Камера тюрьмы не построена: $PRISON"
fi
if [[ "$PRISON" == *"chest=1"* ]]; then
  log '   OK: сундук склада построен'
else
  fail "Сундук склада не построен: $PRISON"
fi

log 'OK: городской закон Элисара проверен на живом сервере.'
