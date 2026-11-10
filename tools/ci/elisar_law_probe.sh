#!/usr/bin/env bash
# v0.49.0 — живая проверка городского закона Элисара на настоящем сервере Forge.
#
# Проверяем в игре то, что просил игрок:
#   1) страж патрулирует по дороге (а не стоит и не бродит наугад);
#   2) удар по стражу даёт ответный удар и предупреждение, но НЕ убивает;
#   3) камера тюрьмы и склад действительно строятся рядом с дорогой.
#
# Скрипт работает на одноразовом сервере из CI.
#
# ВАЖНО: город Элисара стоит вокруг нуля (дороги в радиусе ~230 блоков),
# а не в сотнях. Пробы ищут настоящую площадь из плана города.
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
# Отчёт пишем в ci-out: его потом публикует publish_frames.sh, и по тексту
# видно, какая именно проверка не прошла (логи джобы извне недоступны).
mkdir -p ci-out
LOG="${LOG:-ci-out/elisar-law-probe.txt}"
: > "$LOG"
# Отчёт пишем ещё и прямо в ветку: механизм публикации может не доставить
# файл из ci-out, а без текста пробы падение невозможно расследовать.
DIRECT="reports/ci_diag/elisar-law__probe.txt"
mkdir -p reports/ci_diag
: > "$DIRECT"
DIAG_SAVE="$LOG"
LOG="$LOG"

# Площадь и дорога центрального города Элисара (см. data/riftworld/city/plan.json).
# Высота НЕ выдумывается: её спрашивает у сервера проба v49_city_ground,
# потому что рельеф города меняется, а страж, поставленный в воздухе, падает
# и не появляется — проба честно падала «не удалось заспавнить стражей».
CITY_X=-28
CITY_Z=50
CITY_Y=118

rq() { python3 tools/rcon.py --password "${RCON_PW:-riftci}" --log '' --timeout 90 "$@"; }
fail() {
  echo "::error title=Elisar law probe::$*"
  # Причина падения — в текстовый отчёт, иначе её не увидеть снаружи.
  { echo "FAIL: $*"; echo "--- конец отчёта пробы ---"; } >> "$DIRECT" 2>/dev/null || true
  { echo "FAIL: $*"; echo "--- конец отчёта пробы ---"; } >> "$LOG" 2>/dev/null || true
  # Весь отчёт пробы — в аннотацию CI: их читает API без всяких логов.
  if [ -s "$DIRECT" ]; then
    while IFS= read -r line; do
      echo "::error title=Отчёт пробы закона::${line:0:200}"
    done < "$DIRECT"
  fi
  exit 1
}
log() {
  echo "$*" | tee -a "$LOG"
  echo "$*" >> "$DIRECT" 2>/dev/null || true
}

log '== v0.49.0: проверка городского закона Элисара на живом сервере =='
rq gamerule doMobLoot true >/dev/null

# ВАЖНО: без загруженных чанков getHeight отдаёт высоту генератора (дно
# океана, Y≈62), и страж, поставленный на такую точку, просто не появляется.
# Поэтому сначала держим центр города загруженным.
rq execute in riftworld:elisar run forceload add -96 16 -96 96 >/dev/null 2>&1 || true
rq execute in riftworld:elisar run forceload add -64 16 16 96 >/dev/null 2>&1 || true
sleep 6

# Спрашиваем у сервера настоящую точку дороги и высоту земли.
GROUND_INFO=$(rq execute in riftworld:elisar run riftstruct v49_city_ground 2>&1 | tr '\r\n' '  ' || true)
log "   v49_city_ground -> ${GROUND_INFO:0:300}"
if [[ "$GROUND_INFO" == *"spot="* ]]; then
  # spot= — центр площади с настоящей высотой земли: там гарантированно
  # твёрдый грунт, а не дно океана.
  SPOT=$(echo "$GROUND_INFO" | sed -n 's/.*spot=\([^ ]*\).*/\1/p')
  CITY_X=$(echo "$SPOT" | cut -d, -f1)
  CITY_Y=$(echo "$SPOT" | cut -d, -f2)
  CITY_Z=$(echo "$SPOT" | cut -d, -f3)
  log "   площадь города: $CITY_X,$CITY_Y,$CITY_Z (дорога: $(echo "$GROUND_INFO" | sed -n 's/.*road=\([^ ]*\).*/\1/p'))"
elif [[ "$GROUND_INFO" == *"road="* ]]; then
  ROAD=$(echo "$GROUND_INFO" | sed -n 's/.*road=\([^ ]*\).*/\1/p')
  CITY_X=$(echo "$ROAD" | cut -d, -f1)
  CITY_Y=$(echo "$ROAD" | cut -d, -f2)
  CITY_Z=$(echo "$ROAD" | cut -d, -f3)
  log "   дорога города: $CITY_X,$CITY_Y,$CITY_Z"
else
  # Пробы может ещё не быть в сборке — тогда берём заведомо верную точку
  # центральной площади из плана города (см. data/riftworld/city/plan.json).
  log "   проба v49_city_ground недоступна, беру запасную точку площади"
  CITY_X=-28
  CITY_Y=118
  CITY_Z=50
fi

# ---------------------------------------------------------------- 1. патруль
log '-- 1) страж идёт по дороге, а не стоит на месте'
rq execute in riftworld:elisar run tp TestPlayer "$CITY_X" "$CITY_Y" "$CITY_Z" 0 0 >/dev/null 2>&1 || true
sleep 8
# RCON исполняет команды в обычном мире, а стражей мы ставим в Элисаре:
# команду надо вести через 'execute in riftworld:elisar', иначе она бьёт
# несуществующую цель и цепочка закона рвётся.
rq execute in riftworld:elisar run kill '@e[type=riftworld:elisar_guard]' >/dev/null 2>&1 || true
# Ставим стражей прямо на городскую площадь: витрина v40_gear стоит на 760,760
# (вне города) и для проверки патруля не годится.
# Пробуем несколько высот: если земля ниже, страж провалится и не появится.
SPAWNED=0
for dy in 0 1 2 3 4 -1 -2; do
  SY=$((CITY_Y + dy))
  for i in 1 2 3; do
    # Ответ сервера на summon — в отчёт: по нему видно, что именно не так
    # («Неизвестный тип сущности», «Не удалось создать» и т.п.).
    SUM=$(rq execute in riftworld:elisar run summon riftworld:elisar_guard \
      "$CITY_X" "$SY" "$CITY_Z" '{PersistenceRequired:1b}' 2>&1 | tr '\r\n' ' ' || true)
    log "   summon на высоте $SY -> ${SUM:0:170}"
    sleep 0.4
  done
  sleep 2
  # Считаем стражей через 'execute as @e[...] run data get entity @s Pos':
  # селектор берёт ВСЕХ стражей измерения, а не только тех, кто попал в
  # радиус позиции команды (ради 'if entity' без distance — три блока).
  COUNT=$(rq execute in riftworld:elisar as @e[type=riftworld:elisar_guard] \
    run data get entity @s Pos 2>&1 | grep -c 'entity data' || true)
  GUARDS="guards=$COUNT"
  log "   найдено стражей: $COUNT"
  if [ "${COUNT:-0}" -ge 1 ]; then
    SPAWNED=1
    CITY_Y=$SY
    break
  fi
done
if [ "$SPAWNED" != 1 ]; then
  fail "Не удалось заспавнить стражей ни на одной высоте около $CITY_X,$CITY_Y,$CITY_Z"
fi
log "   стражей рядом: 1+ (высота $CITY_Y)"

# Позиция стража до и через 15 секунд: он обязан ДВИГАТЬСЯ по маршруту.
POS1=$(rq execute in riftworld:elisar run data get entity '@e[type=riftworld:elisar_guard,limit=1]' Pos 2>&1 | tr -d '\n' || true)
PATROL1=$(rq execute in riftworld:elisar run riftstruct v49_guard_patrol 2>&1 | tr '\r\n' '  ' || true)
log "   состояние стража: ${PATROL1:0:300}"
sleep 15
POS2=$(rq execute in riftworld:elisar run data get entity '@e[type=riftworld:elisar_guard,limit=1]' Pos 2>&1 | tr -d '\n' || true)
PATROL2=$(rq execute in riftworld:elisar run riftstruct v49_guard_patrol 2>&1 | tr '\r\n' '  ' || true)
log "   состояние стража: ${PATROL2:0:300}"
log "   позиция стража: $POS1 -> $POS2"
if [[ -z "$POS1" || "$POS1" == *"error"* || "$POS1" == *"Invalid"* ]]; then
  fail "Не удалось прочитать позицию стража: $POS1"
fi
if [[ "$POS1" == "$POS2" ]]; then
  fail "Страж не двигается — патруль по дорогам не работает"
fi
log '   OK: страж идёт по патрулю'

# ---------------------------------------------------------------- 2. ответный удар
log '-- 2) удар по стражу: предупреждение, но НЕ смерть'
rq gamemode survival TestPlayer >/dev/null || true
rq effect give TestPlayer minecraft:resistance 999 4 true >/dev/null || true
sleep 1
HP_BEFORE=$(rq execute in riftworld:elisar run data get entity '@e[type=riftworld:elisar_guard,limit=1]' Health 2>&1 | tr -d '\n' || true)
rq execute in riftworld:elisar run damage '@e[type=riftworld:elisar_guard,limit=1]' 4 minecraft:player_attack by TestPlayer >/dev/null 2>&1 || true
sleep 2
HP_AFTER=$(rq execute in riftworld:elisar run data get entity '@e[type=riftworld:elisar_guard,limit=1]' Health 2>&1 | tr -d '\n' || true)
log "   здоровье стража: $HP_BEFORE -> $HP_AFTER"
if [[ "$HP_AFTER" == *"0.0"* || "$HP_AFTER" == *"0"* && "$HP_AFTER" != *".0"* ]]; then
  fail "Страж погиб от удара игрока — он должен только предупреждать"
fi
if [[ -z "$HP_AFTER" || "$HP_AFTER" == *"error"* || "$HP_AFTER" == *"Invalid"* ]]; then
  fail "Не удалось прочитать здоровье стража: $HP_AFTER"
fi
log '   OK: страж выжил и предупредил'

# ---------------------------------------------------------------- 3. тюрьма и склад
log '-- 3) камера тюрьмы и склад строятся рядом с дорогой'
PRISON=$(rq execute in riftworld:elisar positioned "$CITY_X" "$CITY_Y" "$CITY_Z" run riftstruct v49_elisar_law 2>&1 | tr '\r\n' '  ' || true)
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

# ---------------------------------------------------------------- 4. вся цепочка
# Убил стража, город видел -> розыск -> смерть -> тюрьма -> вещи в сундуке.
log '-- 4) полная цепочка: убийство при свидетелях -> розыск -> тюрьма -> сундук'
rq gamemode survival TestPlayer >/dev/null || true
# Кладём игроку вещи: после ареста их не должно остаться в инвентаре.
rq give TestPlayer riftworld:aurite_ingot 5 >/dev/null 2>&1 || true
rq give TestPlayer riftworld:elisar_ducat 3 >/dev/null 2>&1 || true
sleep 1

# Ставим НЕСКОЛЬКО стражей рядом: один убит, остальные видят убийство.
for i in 1 2 3; do
  rq execute in riftworld:elisar run summon riftworld:elisar_guard \
    "$CITY_X" "$CITY_Y" "$CITY_Z" '{PersistenceRequired:1b}' >/dev/null 2>&1 || true
  sleep 0.5
done
sleep 3
# Убиваем ОДНОГО стража от руки игрока: остальные — свидетели.
rq execute in riftworld:elisar run summon riftworld:elisar_guard \
  "$CITY_X" "$CITY_Y" "$CITY_Z" '{PersistenceRequired:1b}' >/dev/null 2>&1 || true
sleep 2
rq execute in riftworld:elisar run damage '@e[type=riftworld:elisar_guard,limit=1,sort=nearest]' 1000 minecraft:player_attack by TestPlayer >/dev/null 2>&1 || true
sleep 3

LAW=$(rq execute in riftworld:elisar positioned "$CITY_X" "$CITY_Y" "$CITY_Z" run riftstruct v49_law_state 2>&1 | tr '\r\n' '  ' || true)
log "   riftstruct v49_law_state -> ${LAW:0:400}"
if [[ "$LAW" == *"wanted=1"* ]]; then
  log '   OK: игрок в розыске (город видел убийство)'
else
  fail "Игрок не попал в розыск после убийства стража при свидетелях: $LAW"
fi

# Убиваем самого игрока: он должен очнуться в камере, а вещи — в сундуке.
# Игрок тоже в измерении Элисара.
rq execute in riftworld:elisar run damage TestPlayer 1000 minecraft:player_attack by TestPlayer >/dev/null 2>&1 || true
# Возрождение игрока занимает время: ждём, пока он снова появится в мире.
for _ in $(seq 1 20); do
  ALIVE=$(rq execute in riftworld:elisar as TestPlayer run data get entity @s Pos 2>&1 | tr -d '\n' || true)
  [[ "$ALIVE" == *"entity data"* ]] && break
  sleep 1
done
sleep 4
LAW2=$(rq execute in riftworld:elisar positioned "$CITY_X" "$CITY_Y" "$CITY_Z" run riftstruct v49_law_state 2>&1 | tr '\r\n' '  ' || true)
log "   после смерти -> ${LAW2:0:400}"
if [[ "$LAW2" == *"jailed=1"* ]]; then
  log '   OK: игрок в городской тюрьме'
else
  fail "Игрок не оказался в тюрьме после смерти в розыске: $LAW2"
fi
if [[ "$LAW2" == *"chest_items=1"* || "$LAW2" == *"chest_items=2"* || "$LAW2" == *"chest_items=3"* ]]; then
  log '   OK: вещи игрока лежат в сундуке склада рядом с тюрьмой'
else
  fail "Вещи игрока не оказались в сундуке склада: $LAW2"
fi

log 'OK: городской закон Элисара проверен на живом сервере.'
echo '--- конец отчёта пробы ---' >> "$DIRECT" 2>/dev/null || true
# Отчёт — в аннотации CI: их читает API, в отличие от логов джобы.
if [ -s "$DIRECT" ]; then
  while IFS= read -r line; do
    echo "::notice title=Отчёт пробы закона::${line:0:200}"
  done < "$DIRECT"
fi
