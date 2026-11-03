# RiftWorld `v0.30.0` — часть 1

Сборка проверена живым клиентом Minecraft (Forge 1.20.1, CPU-рендер Xvfb + llvmpipe):
**0 провалов проб**, CI-прогон [#348](https://github.com/standimaoff-oss/riftworld/actions/runs/41637466951) — success.

## Файлы
- `riftworld-v0.30.0.jar` — мод (Forge 1.20.1)
- `riftworld-dt-v0.30.0.jar` — аддон Dynamic Trees (ставится вместе с модом и DT 1.4.10)

## Что исправлено (по списку владельца)

| № | Требование | Как сделано |
|---|---|---|
| 1 | Нет защитных стен у замка | Залиты все бреши куртин: `wall_breaches=0` (7316 блоков), 4 линии стен герметичны |
| 2 | Крафты практические всем блокам | 413 рецептов, покрытие **304/343** блоков, 35 лабораторных/редких удалено, 0 коллизий |
| 3 | Круг портит вид у пепельных структур | Круг убран — остались только футпринты колокольни/кургана + тропа |
| 4 | Летающая башня чаще | Шанс `0.62 → 0.78`, радиус-буфер `26 → 18` |
| 5 | Волны у мелких островов | `MIN_WAKE_ISLAND_R = 16` — острова меньше 16 блоков волн не оставляют |
| 6 | Никаких надписей про дракона | Удалены все `displayClientMessage` в драконе, полёте и свистке |
| 7 | Честный рандом структур | Убраны `pinned`-ячейки; у всех структур только шанс |
| 8 | Управление драконом мышкой | Полёт считается на клиенте (`travel()`), всадник садится на седло, мышь = поворот/тангаж |
| 9 | Меньше руды | `count 7 → 3`, `size 8 → 5`, порог `0.895`, выход `0.68 → 0.24` |
| 10 | Круговые лестницы проходимы | Убраны 813 мешающих блоков, `stairs_jammed=0` |
| 11 | Лестницы не обрываются | Пандусы перевырезаются после мостовой; все 28 лестниц проходимы от низа до верха |
| 12 | Дыры в замке | Залито 434 дыры оболочки, `sub_air=0` |
| 13 | Нет входа в Ротонду | Сквозной проход с юга и севера: `rotunda_door=48` |
| 14 | Мост в никуда | Каузей доведён до замка, западные ворота прорезаны: `sub_bridge=2079`, `above_far_island=0` |
| 15 | Двери башен наружу | Двери 28 башен развёрнуты внутрь королевства |

## Пробы живого мира
```
blacktide_citadel: ocean_only=true bridge_seabed=true sub_bridge=220 lvl1_east=227
                   wall_breaches=0 tower_stairs=1027 stairs_jammed=0 rotunda_door=48
                   blocks=149968 sub_air=0
blacktide_cathedral_hall: carpets=575 thrones=1 chandeliers=14 pillars=1721 gilded=0
blacktide_second_floor:   floor2_blocks=1316 warm_lights=452 floor2_books=144
blacktide_rotunda_sanctum: carpets=124 altars=1 books=434 gilded=0
blacktide_tower_walkability: climbed_towers=28 total_stairs=1027
v29_blacktide_seabed_bridge: sub_bridge=2079 water_above=297 above_far_island=0
v29_crafting_and_coin_ore: passed_crafts=8/8 gilded_3x3=true ducat_smelt=true
v29_sync_ocean_waves_perf: r2=28.0 r6=92.0 r12=188.0 fish_despawn=true
```

## Кадры и отчёты
- Папка кадров: [`reports/iteration-39/shots`](https://github.com/standimaoff-oss/riftworld/tree/arena/68e37ffc-riftworld/reports/iteration-39/shots)
- Отчёт клиента: [`report_client.md`](https://github.com/standimaoff-oss/riftworld/blob/arena/68e37ffc-riftworld/reports/iteration-39/report_client.md)
- Все пробы: [`probes.txt`](https://github.com/standimaoff-oss/riftworld/blob/arena/68e37ffc-riftworld/reports/iteration-39/probes.txt)
- Лог клиента: [`client.log`](https://github.com/standimaoff-oss/riftworld/blob/arena/68e37ffc-riftworld/reports/iteration-39/client.log) · ошибки: [`client_errors.txt`](https://github.com/standimaoff-oss/riftworld/blob/arena/68e37ffc-riftworld/reports/iteration-39/client_errors.txt)

## Концепты на будущее
- Новый биом **«Мороковая Падь»** (`riftworld:morok_hollow`) — 5 AI-референсов в стиле Minecraft: [`reports/biome_concept_morok_hollow`](https://github.com/standimaoff-oss/riftworld/tree/arena/68e37ffc-riftworld/reports/biome_concept_morok_hollow)
- Роадмап новых мобов: **Лиоплевродон** (море), **Пегас** (земля/воздух), **Вендиго** (хозяин Пади): [`reports/ROADMAP_v0.31_plus.md`](https://github.com/standimaoff-oss/riftworld/blob/arena/68e37ffc-riftworld/reports/ROADMAP_v0.31_plus.md)
