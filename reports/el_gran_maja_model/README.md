# El Gran Maja — модель для Riftworld

Модель встроена в Forge-мод Riftworld для Minecraft 1.20.1. Исходник Bedrock/Blockbench и воспроизводимый генератор находятся в этой папке; игровые геометрия, границы и атлас копируются в `mod/src/main/resources/assets/riftworld/`. Изображения здесь — **программные рендеры модели, а не кадры игры**.

- 13 связанных костей, 3206 кубоидов; длина около **322 блоков**, ширина **116 блоков**, высота **44,6 блока**.
- Атлас 4096×4096, на каждую ось поверхности — 16 пикселей на блок. Общие проекционные листы намеренно переиспользуют UV.
- 62 отдельных зуба: 32 сверху и 30 снизу; каждый собран из четырёх сужающихся, смещённых секций. Формы и длина различаются; зубы не сводятся к двум ровным рядам прямых иголок.
- Нижняя челюсть — отдельная кость; изображения `04_...` и `08_...` показывают её разные углы. Тело и хвост связаны цепью костей.
- `build_collision_bounds.py` строит 13 консервативных ограничивающих объёмов по крайним положениям анимации; `validate_collision_bounds.py` дополнительно проверяет случайные фазы, вращение и идентичность игровых ресурсов. Это проверка геометрии, а не замена испытанию в игре.

## Воспроизведение

Требуются Python 3, Pillow и NumPy:

```bash
python reports/el_gran_maja_model/build_el_gran_maja_model.py
python reports/el_gran_maja_model/validate_el_gran_maja_model.py
python reports/el_gran_maja_model/render_el_gran_maja_shots.py
python reports/el_gran_maja_model/build_collision_bounds.py
python reports/el_gran_maja_model/validate_collision_bounds.py
```

Референс и модель сверены в `01_model_front_and_side_ortho.png` и `06_reference_mouth_vs_model_mouth.png`; масштабный вид — `03_model_three_quarter_scale_with_player.png`. Внешний вид в настоящем клиенте может отличаться из-за освещения, воды и тумана.
