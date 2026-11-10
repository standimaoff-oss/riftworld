#!/usr/bin/env bash
# Кладём кадры настоящего клиента, отчёты probe-скриптов и собранные jar прямо
# в рабочую ветвь: артефакты CI извне недоступны, а по коммиту видно и кадры,
# и текстовый отчёт probe-скрипта (в нём ровно та проверка, которая упала).
set -Eeuo pipefail
BRANCH=${BRANCH:-${GITHUB_REF_NAME:-}}
OUT=${OUT:-ci-out}
[ -n "$BRANCH" ] || { echo 'BRANCH is empty, нечего публиковать'; exit 1; }

# Журнал публикации — на каждую джобу свой: иначе параллельные джобы
# затирают друг друга и в ветке виден только журнал последней.
DIAG="reports/ci_diag/publish-${GITHUB_JOB:-job}.log"
mkdir -p reports/ci_diag
{
    echo "run ${GITHUB_RUN_ID:-?} / job ${GITHUB_JOB:-?} / ветвь $BRANCH"
    echo "== содержимое $OUT =="
    ls -la "$OUT" || true
} > "$DIAG"

changed=0

# Копирование свежих кадров, отчётов и jar в ветвь. Вынесено в функцию,
# чтобы можно было повторить копирование после синхронизации с ветвью:
# чужие коммиты не должны затирать наши свежие отчёты.
publish_files() {
    if ls "$OUT"/*.png >/dev/null 2>&1; then
        mkdir -p reports/ci_frames reports/ci_diag
        cp -a "$OUT"/*.png reports/ci_frames/
        changed=1
    fi
    # Текстовые отчёты probe-скриптов: единственный способ прочитать, какая
    # именно проверка не прошла, когда логи джобы недоступны.
    # Имя файла начинается с имени джобы — иначе параллельные джобы затирают
    # отчёты друг друга, и падение одной пробы невозможно расследовать.
    # ВАЖНО: 'ls a b' возвращает ненулевой код, если НЕ найден хотя бы один
    # из шаблонов. У джобы закона есть .txt, но нет .log — и весь блок
    # пропускался, из-за чего отчёт пробы никогда не попадал в ветвь.
    # Поэтому проверяем каждый шаблон отдельно.
    local has_report=0
    for pat in "$OUT"/*.txt "$OUT"/*.log; do
        [ -e "$pat" ] || continue
        has_report=1
        break
    done
    if [ "$has_report" = 1 ]; then
        job=${GITHUB_JOB:-job}
        for f in "$OUT"/*.txt "$OUT"/*.log; do
            [ -e "$f" ] || continue
            base=$(basename "$f")
            # Собственный журнал публикации не затираем старой копией из ci-out.
            [ "$base" = "publish.log" ] && continue
            cp -a "$f" "reports/ci_diag/${job}__${base}" 2>/dev/null || true
            cp -a "$f" "reports/ci_diag/${base}" 2>/dev/null || true
        done
        changed=1
    fi
    if ls mod/build/libs/*.jar >/dev/null 2>&1; then
        mkdir -p release
        cp -a mod/build/libs/*.jar release/
        changed=1
    fi
    return 0
}

publish_files
[ "$changed" = 1 ] || { echo 'нечего публиковать'; exit 0; }

git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"

# git add падает целиком, если любой pathspec не существует, поэтому добавляем
# только реально существующие каталоги.
add_all() {
    for d in reports/ci_frames reports/ci_diag release; do
        [ -d "$d" ] && git add -A "$d"
    done
    # Отчёт пробы пишется прямо в reports/ci_diag — добавляем его явно,
    # чтобы он не потерялся из-за чужих коммитов в ветке.
    [ -f reports/ci_diag/elisar-law__probe.txt ] && git add -f reports/ci_diag/elisar-law__probe.txt
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
    # Ветвь могли обновить параллельные джобы: забираем их коммиты, но НЕ
    # трогаем наши файлы (reset --mixed стирает рабочие изменения).
    # Сначала сохраняем свежие копии отчётов, потом подтягиваем чужое, потом
    # возвращаем наши файлы поверх — так ничего не теряется.
    # checkout затирает наши свежие отчёты, поэтому сначала копируем их в
    # отдельную папку, а после синхронизации кладём обратно.
    rm -rf /tmp/publish_keep && mkdir -p /tmp/publish_keep
    if [ -d reports/ci_diag ]; then cp -a reports/ci_diag /tmp/publish_keep/; fi
    git checkout FETCH_HEAD -- . >>"$DIAG" 2>&1 || true
    git reset --soft FETCH_HEAD >>"$DIAG" 2>&1 || echo "reset не удался" >>"$DIAG"
    if [ -d /tmp/publish_keep/ci_diag ]; then
        cp -a /tmp/publish_keep/ci_diag/. reports/ci_diag/ 2>/dev/null || true
    fi
    publish_files
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
