# Great Temple — проверка перед запуском

Дата: 2026-10-06 (UTC)

## Что проверено

- `python3 -m py_compile tools/rift/*.py` — успешно.
- `python3 tools/rift/textures_temple.py` и `python3 tools/rift/gen_resources_temple.py` — успешно.
- `python3 tools/rift/gen_temple.py` — успешно: шаблон `63×96×70`, 130 603 блока, 52 состояния палитры, 3 уровня лабиринта, 35 ловушек и 10 сундуков с лут-таблицами; `vanilla []`, `skips 0`. Встроенные проверки также подтвердили 8 ритуальных свечей, одно мозаичное ядро и отсутствие launch core.
- Все 1 160 JSON-файлов ресурсов разбираются; 31 текстура храма/ритуала проверена как PNG `16×16`.
- `bash -n tools/*.sh` и `git diff --check` — успешно.

## Что не выполнено

Сборка Forge, запуск выделенного сервера и клиента, игровое прохождение/проверка ритуала и ловушек, игровые логи и скриншоты **ещё не получены**. В среде нет `java`/`javac`, локальных Gradle/Forge-кэшей тоже нет; `tools/build.sh` корректно остановился с сообщением, что для Minecraft 1.20.1 нужен JDK 17.

Повторно проверил загрузки: официальный Temurin JDK 17.0.20.1+1 найден через GitHub API, но сам архив GitHub перенаправляет на `release-assets.githubusercontent.com`; реальные попытки `curl` и `wget` завершаются ошибкой TLS (curl, код 35). Adoptium API, Debian и российские Debian mirrors, Forge/Maven-хосты также не устанавливают TLS-соединение; Debian HTTP возвращает пустой ответ. Кэш `/home/user/.cache/mc` пуст, `java`, `javac`, Gradle и Maven отсутствуют. Поэтому установить JDK и Forge зависимости в этой среде пока не удалось.

Поэтому храм не объявляется готовым: его поведение в игре пока не подтверждено.

## Следующие шаги после восстановления доступа к загрузкам

```bash
bash tools/setup_all.sh
bash tools/build.sh
bash tools/setup_server.sh install
bash tools/setup_server.sh start
bash tools/setup_client.sh install
bash tools/setup_client.sh start
```

Затем проверить стартовые и игровые логи, ритуал (зажечь все свечи и дождаться мозаики/моста), все этажи лабиринта, сундуки и ловушки; сохранить игровые логи в `reports/session_temple/logs/`, скриншоты — в `reports/session_temple/screenshots/`.
