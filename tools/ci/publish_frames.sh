#!/usr/bin/env bash
# Кладём кадры настоящего клиента, логи probe-скриптов и собранные jar прямо
# в рабочую ветвь: артефакты CI извне недоступны, а по коммиту видно и кадры,
# и текстовый отчёт probe-скрипта (в нём ровно та проверка, которая упала).
set -Eeuo pipefail
BRANCH=${BRANCH:-${GITHUB_REF_NAME:-}}
OUT=${OUT:-ci-out}
[ -n "$BRANCH" ] || { echo 'BRANCH is empty, нечего публиковать'; exit 1; }

echo "== содержимое $OUT =="
ls -la "$OUT" || true

changed=0
if ls "$OUT"/*.png >/dev/null 2>&1; then
    mkdir -p reports/ci_frames
    cp -a "$OUT"/*.png reports/ci_frames/
    changed=1
fi
# Текстовые отчёты probe-скриптов: единственный способ прочитать, какая именно
# проверка не прошла, когда логи джобы недоступны.
if ls "$OUT"/*.txt "$OUT"/*.log >/dev/null 2>&1; then
    mkdir -p reports/ci_diag
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

commit() {
    git add -A reports/ci_frames reports/ci_diag release
    git diff --cached --quiet && return 1
    git commit -m "Add in-game screenshots and jars [skip ci]"
    return 0
}

git fetch origin "$BRANCH"
# Рабочая ветвь обновляется параллельными джобами: принимаем её состояние как
# есть (наши свежескопированные файлы лежат в рабочем каталоге и не пропадут).
git reset --mixed "origin/$BRANCH"
commit || { echo 'нечего публиковать после синхронизации'; exit 0; }

for try in 1 2 3 4 5; do
    if git push origin "HEAD:$BRANCH"; then
        echo "кадры и jar опубликованы в $BRANCH (попытка $try)"
        exit 0
    fi
    sleep 10
    git fetch origin "$BRANCH"
    git reset --mixed "origin/$BRANCH"
    commit || { echo 'нечего публиковать после синхронизации'; exit 0; }
done

echo 'НЕ УДАЛОСЬ запушить кадры за 5 попыток'
exit 1
