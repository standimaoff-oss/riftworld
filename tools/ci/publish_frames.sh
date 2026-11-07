#!/usr/bin/env bash
# Кладём кадры настоящего клиента, отчёты probe-скриптов и собранные jar прямо
# в рабочую ветвь: артефакты CI извне недоступны, а по коммиту видно и кадры,
# и текстовый отчёт probe-скрипта (в нём ровно та проверка, которая упала).
set -Eeuo pipefail
BRANCH=${BRANCH:-${GITHUB_REF_NAME:-}}
OUT=${OUT:-ci-out}
[ -n "$BRANCH" ] || { echo 'BRANCH is empty, нечего публиковать'; exit 1; }

DIAG=reports/ci_diag/publish.log
mkdir -p reports/ci_diag
{
    echo "run ${GITHUB_RUN_ID:-?} / job ${GITHUB_JOB:-?} / ветвь $BRANCH"
    echo "== содержимое $OUT =="
    ls -la "$OUT" || true
} > "$DIAG"

changed=0
if ls "$OUT"/*.png >/dev/null 2>&1; then
    mkdir -p reports/ci_frames
    cp -a "$OUT"/*.png reports/ci_frames/
    changed=1
fi
# Текстовые отчёты probe-скриптов: единственный способ прочитать, какая именно
# проверка не прошла, когда логи джобы недоступны.
if ls "$OUT"/*.txt "$OUT"/*.log >/dev/null 2>&1; then
    cp -a "$OUT"/*.txt reports/ci_diag/ 2>/dev/null || true
    cp -a "$OUT"/*.log reports/ci_diag/ 2>/dev/null || true
    changed=1
fi
if ls mod/build/libs/*.jar >/dev/null 2>&1; then
    mkdir -p release
    cp -a mod/build/libs/*.jar release/
    changed=1
fi
[ "$changed" = 1 ] || { echo 'нечего публиковать'; exit 0; }

git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"

# git add падает целиком, если любой pathspec не существует, поэтому добавляем
# только реально существующие каталоги.
add_all() {
    for d in reports/ci_frames reports/ci_diag release; do
        [ -d "$d" ] && git add -A "$d"
    done
    return 0
}

add_all
git diff --cached --quiet && { echo 'нечего публиковать после копирования'; exit 0; }
git commit -q -m "Add in-game screenshots and jars [skip ci]"

for try in 1 2 3 4 5; do
    if err=$(git push origin "HEAD:$BRANCH" 2>&1); then
        echo "кадры и jar опубликованы в $BRANCH (попытка $try)"
        exit 0
    fi
    echo "попытка $try: $err" >> "$DIAG"
    sleep 10
    # Параллельные джобы пушат в ту же ветвь: принимаем её состояние как есть
    # (наши свежескопированные файлы лежат на диске и не пропадают) и
    # пересоздаём коммит уже на новой базе.
    # Полный refspec и FETCH_HEAD: короткое имя ветки не обновляет
    # refs/remotes/origin/*, и reset ушёл бы на устаревший коммит.
    git fetch origin "refs/heads/$BRANCH" >>"$DIAG" 2>&1 || echo "fetch не удался" >>"$DIAG"
    git reset --mixed FETCH_HEAD >>"$DIAG" 2>&1 || echo "reset не удался" >>"$DIAG"
    add_all
    if git diff --cached --quiet; then
        echo "после синхронизации нечего публиковать" >>"$DIAG"
        exit 0
    fi
    git commit -q -m "Add in-game screenshots and jars [skip ci]"
done

echo "НЕ УДАЛОСЬ запушить кадры за 5 попыток; отчёт:"
cat "$DIAG"
exit 1
