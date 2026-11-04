#!/usr/bin/env python3
"""
v0.39.11 — «художники» текстур для моб-мобов.

Зачем: до этого атлас моба заливался одним цветом с мелким шумом, поэтому в игре шкура Пегаса
выглядела как пластик, а Вендиго — как чёрный силуэт. Здесь каждый материал получает функцию
`painter(face, x, y, fw, fh, base, seed)`, которая рисует НАСТОЯЩИЙ рисунок по грани коробки:

  * `face` — 'up' / 'down' / 'north' (перед) / 'south' (зад) / 'east' / 'west' (бока);
  * `x, y` — пиксель внутри грани, `fw, fh` — её размер (рисунок позиционный, а не «на глаз»);
  * возврат `None` — оставить базовый пиксель, кортеж — заменить цвет.

Так на морде появляются глаза и ноздри ровно там, где надо, на крыле — ряды маховых перьев,
на шкуре — яблоки и затенение снизу, на костях — прожилки и трещины.
"""
from geo_model_lib import hx, mixc, shade, smooth_noise, noise2


# ------------------------------------------------------------------ вспомогательное
def rim(base, y, fh, k_top=1.10, k_bot=0.78):
    """Объём: верхняя кромка светлее, нижняя темнее — коробка перестаёт быть плоской."""
    t = y / max(1, fh - 1)
    return shade(base, k_top - (k_top - k_bot) * t)


def ellipse(x, y, cx, cy, rx, ry):
    return ((x - cx) / max(0.001, rx)) ** 2 + ((y - cy) / max(0.001, ry)) ** 2 <= 1.0


def fur(base, seed, x, y, k=0.10, scale=2.2):
    """Мелкая «шерсть»: сглаженный шум вместо соли-с-перцем."""
    n = smooth_noise(seed, x, y, scale) - 0.5
    return shade(base, 1.0 + n * 2 * k)


# ------------------------------------------------------------------ ПЕГАС
# v0.40.0: масть серая с тёмными «отметинами», поэтому рисунок строится на КОНТРАСТЕ:
# светлый хребет, тёмное брюхо и низ ног, почти белые маховые с тёмными кончиками.
def pegasus_coat(face, x, y, fw, fh, base, seed):
    """Шкура: яблоки (мягкие пятна), светлая спина, тёмное брюхо, волоски."""
    c = rim(base, y, fh, 1.08, 0.80)
    # яблоки: пятна по гладкому шуму, а не «соль с перцем»
    n = smooth_noise(seed + 7, x, y, 3.6)
    if n > 0.70:
        c = shade(c, 0.86)
    elif n < 0.26:
        c = shade(c, 1.06)
    # брюхо и низ корпуса — темнее (объём)
    if face in ("east", "west", "north", "south") and fh > 4 and y > fh * 0.66:
        c = shade(c, 0.88)
    return fur(c, seed, x, y, 0.05, 2.0)


def pegasus_leg(face, x, y, fw, fh, base, seed):
    """Нога: от серой шкуры к почти чёрной «чулке» у копыта (так у настоящих коней)."""
    t = y / max(1, fh - 1)                     # 0 — верх коробки, 1 — низ
    c = mixc(shade(base, 1.05), hx("#3E3830"), min(1.0, max(0.0, t - 0.18) / 0.82))
    c = fur(c, seed, x, y, 0.06, 1.6)
    if t > 0.86:
        c = shade(c, 1.12)                     # венчик светлее чулки
    return c


def pegasus_skull(face, x, y, fw, fh, base, seed):
    """Голова: тёмная переносица сверху, скула в тени, блик на лбу."""
    c = pegasus_coat(face, x, y, fw, fh, base, seed)
    if face == "up" and fw > 2:
        if abs(x - (fw - 1) / 2.0) < max(1.0, fw * 0.22):
            c = mixc(c, hx("#8A8172"), 0.45)    # тёмная полоса по переносице
    if face in ("east", "west") and fh > 3:
        if y < fh * 0.30:
            c = shade(c, 1.08)                  # блик на лбу
        elif y > fh * 0.62:
            c = shade(c, 0.88)                  # скула в тени
    return c


def pegasus_muzzle(face, x, y, fw, fh, base, seed):
    """Морда: тёмный нос, ноздри и линия рта — только на передней грани."""
    c = pegasus_coat(face, x, y, fw, fh, base, seed)
    if face == "north":
        if y > fh * 0.42:
            c = mixc(c, hx("#4A443C"), 0.55)
        if ellipse(x, y, fw * 0.26, fh * 0.30, max(0.9, fw * 0.15), max(0.8, fh * 0.14)):
            c = hx("#241F1B")
        if ellipse(x, y, fw * 0.74, fh * 0.30, max(0.9, fw * 0.15), max(0.8, fh * 0.14)):
            c = hx("#241F1B")
        if y >= fh - 1:
            c = hx("#241F1B")                   # рот
    if face in ("east", "west") and y > fh * 0.6:
        c = mixc(c, hx("#4A443C"), 0.35)
    return c


def pegasus_mane(face, x, y, fw, fh, base, seed):
    """Грива и хвост: вертикальные пряди, светлые блики, тёмные концы."""
    c = rim(base, y, fh, 1.16, 0.72)
    strand = (x * 5 + (seed >> 3)) % 7
    if strand == 0:
        c = shade(c, 0.72)
    elif strand == 3:
        c = shade(c, 1.16)
    elif strand == 5:
        c = shade(c, 0.86)
    if y > fh * 0.72:
        c = shade(c, 0.84)
    return fur(c, seed, x, y, 0.05, 1.3)


def pegasus_feather(face, x, y, fw, fh, base, seed):
    """Маховое перо: стержень вдоль длинной оси, бородки рядами, тёмная КАЙМА по краю.

    Кайма обязательна: перья лежат вплотную, и без неё веер сбоку сливается в «доску»
    (именно так крыло и выглядело в кадрах 147 прогонов #396–#423).
    """
    long_axis_x = fw >= fh                      # перо вытянуто вдоль X текстуры
    c = rim(base, y, fh, 1.04, 0.90)
    shaft = fh // 2
    if y == shaft:                              # стержень
        c = shade(c, 1.08)
    if (x + y * 2) % 5 == 0:                    # бородки
        c = shade(c, 0.94)
    # тёмная кайма по контуру грани — перья перестают сливаться
    if x == 0 or y == 0 or y == fh - 1 or x == fw - 1:
        c = mixc(c, hx("#6A635A"), 0.40)
    # кончик пера почти чёрный
    if long_axis_x and x >= fw - 2:
        c = mixc(c, hx("#3A352F"), 0.72)
    if long_axis_x and x >= fw - 3:
        c = mixc(c, hx("#6A635A"), 0.35)
    if not long_axis_x and y == fh - 1:
        c = mixc(c, hx("#6A635A"), 0.45)
    return c


def pegasus_feather_edge(face, x, y, fw, fh, base, seed):
    """Кроющее перо / костяная основа крыла — темнее махового."""
    c = pegasus_feather(face, x, y, fw, fh, base, seed)
    return shade(c, 0.86)


def pegasus_hoof(face, x, y, fw, fh, base, seed):
    """Копыто: чёрное, со светлой полосой у венчика и блеском."""
    c = shade(base, 1.0 + (0.22 if y == 0 else 0.0))
    if y <= max(1, fh // 4):
        c = mixc(c, hx("#6E6A62"), 0.40)
    if smooth_noise(seed, x, y, 1.6) > 0.74:
        c = shade(c, 0.74)
    return c


def pegasus_saddle(face, x, y, fw, fh, base, seed):
    """Седло: кожа с прошивкой по краю и золотой пряжкой."""
    c = rim(base, y, fh, 1.14, 0.78)
    if face in ("north", "south", "east", "west"):
        if x in (0, fw - 1) or y in (0, fh - 1):
            c = hx("#E3D6A8")                       # прошивка
        if ellipse(x, y, fw * 0.5, fh * 0.5, fw * 0.18, fh * 0.16):
            c = hx("#D9B24C")                       # пряжка
            if ellipse(x, y, fw * 0.5, fh * 0.5, fw * 0.08, fh * 0.07):
                c = hx("#8C6B21")
    else:
        c = shade(c, 0.9)
    return c


def pegasus_eye(face, x, y, fw, fh, base, seed):
    """Глаз коня: янтарная радужка, чёрный зрачок, белый блик."""
    cx, cy = (fw - 1) / 2.0, (fh - 1) / 2.0
    c = hx("#E9A93C")
    if x == 0 and y == 0:
        c = hx("#6B4A16")                           # тень в углу
    if abs(x - cx) < 1.0 and abs(y - cy) < 1.0:
        c = hx("#120E0A")                           # зрачок
        if x == int(round(cx)) and y == int(round(cy)) - 1:
            c = hx("#FFF6E2")                       # блик
    return c


PEGASUS = {
    "coat": pegasus_coat,
    "leg": pegasus_leg,
    "skull": pegasus_skull,
    "muzzle": pegasus_muzzle,
    "nose": lambda face, x, y, fw, fh, base, seed: hx("#241F1B"),
    "mane": pegasus_mane,
    "tail": pegasus_mane,
    "feather": pegasus_feather,
    "feather_edge": pegasus_feather_edge,
    "feather_tip": pegasus_feather_edge,
    "hoof": pegasus_hoof,
    "saddle": pegasus_saddle,
    "gold": lambda face, x, y, fw, fh, base, seed: shade(hx("#D9B24C"), 1.0 + (0.15 if y == 0 else 0.0)),
    "eye": pegasus_eye,
}


# ------------------------------------------------------------------ ВЕНДИГО
def wendigo_hide(face, x, y, fw, fh, base, seed):
    """Шкура: кора — вертикальные трещины, тёмные волокна, редкий мох."""
    c = rim(base, y, fh, 1.10, 0.82)
    crack = smooth_noise(seed, x // 1, y, 3.2)
    if crack > 0.76:
        c = shade(c, 0.62)                          # трещина
    fine = smooth_noise(seed + 3, x, y, 1.4)
    if fine > 0.68:
        c = shade(c, 1.12)                          # волокно коры
    if fine < 0.18:
        c = mixc(c, hx("#2A2118"), 0.35)
    moss = smooth_noise(seed + 11, x, y, 2.4)
    if moss > 0.86 and y > fh * 0.4:
        c = mixc(c, hx("#4B5A3C"), 0.30)            # мох в трещинах
    return c


def wendigo_bone(face, x, y, fw, fh, base, seed):
    """Кость: светлая, с прожилками и тёмным основанием."""
    c = rim(base, y, fh, 1.08, 0.80)
    if (x * 3 + y * 2) % 9 == 0:
        c = shade(c, 0.86)                          # прожилка
    if y == 0:
        c = shade(c, 1.10)
    if smooth_noise(seed, x, y, 1.8) > 0.80:
        c = shade(c, 0.92)
    return c


def wendigo_antler(face, x, y, fw, fh, base, seed):
    """Рог: кольцевые сегменты роста."""
    c = rim(base, y, fh, 1.06, 0.84)
    if (y % 3) == 1:
        c = shade(c, 0.88)                          # кольцо
    if (x + y) % 11 == 0:
        c = shade(c, 1.06)
    return c


def wendigo_skull(face, x, y, fw, fh, base, seed):
    """Череп лося: костяная масть, тёмные глазницы и носовая впадина спереди."""
    c = wendigo_bone(face, x, y, fw, fh, base, seed)
    if face == "north":
        if ellipse(x, y, fw * 0.28, fh * 0.42, max(1.2, fw * 0.16), max(1.0, fh * 0.16)):
            c = hx("#0B0908")                       # глазница
        if ellipse(x, y, fw * 0.72, fh * 0.42, max(1.2, fw * 0.16), max(1.0, fh * 0.16)):
            c = hx("#0B0908")
        if ellipse(x, y, fw * 0.5, fh * 0.78, max(1.0, fw * 0.12), max(1.0, fh * 0.12)):
            c = hx("#171310")                       # нос
    if face in ("east", "west"):
        if ellipse(x, y, fw * 0.45, fh * 0.40, max(1.0, fw * 0.22), max(1.0, fh * 0.16)):
            c = hx("#0B0908")
    return c


def wendigo_claw(face, x, y, fw, fh, base, seed):
    c = shade(base, 1.0)
    if y == 0:
        c = mixc(c, hx("#8C8578"), 0.45)            # блеск на кончике
    return c


WENDIGO = {
    "hide": wendigo_hide,
    "hide_dark": wendigo_hide,
    "bone": wendigo_bone,
    "antler": wendigo_antler,
    "skull": wendigo_skull,
    "socket": lambda *a: hx("#0B0908"),
    "claw": wendigo_claw,
}


# ------------------------------------------------------------------ МОРОК
# v0.40.0: кожа пепельная с прожилками и РЁБРАМИ, нарисованными на груди; костяная маска
# с провалами глазниц; саван — почти чёрная ткань с изорванным низом.
def morok_skin(face, x, y, fw, fh, base, seed):
    """Пепельная кожа: тёмные прожилки, грязные пятна, светлые натянутые сухожилия."""
    c = rim(base, y, fh, 1.10, 0.74)
    vein = smooth_noise(seed + 3, x, y, 2.4)
    if vein > 0.74:
        c = shade(c, 0.78)                       # прожилка
    elif vein < 0.16:
        c = shade(c, 1.12)                       # сухожилие
    fleece = smooth_noise(seed + 9, x, y, 1.5)
    if fleece > 0.70:
        c = shade(c, 1.06)
    if smooth_noise(seed + 13, x, y, 3.2) > 0.84:
        c = mixc(c, hx("#2A3038"), 0.30)         # грязное пятно
    return c


def morok_chest(face, x, y, fw, fh, base, seed):
    """Грудь: на передней грани проступают РЁБРА (тёмные дуги под кожей)."""
    c = morok_skin(face, x, y, fw, fh, base, seed)
    if face == "north" and fh > 5:
        # три ребра: дуги, которые уходят к бокам вниз
        for i, yy in enumerate((fh * 0.30, fh * 0.52, fh * 0.74)):
            band = abs(y - yy)
            if band < 1.0:
                c = shade(c, 0.74)
            elif band < 2.0:
                c = shade(c, 0.90)
            if abs(x - (fw - 1) / 2.0) < 1.0 and band < 1.0:
                c = shade(c, 0.62)               # грудина
    return c


def morok_cloak(face, x, y, fw, fh, base, seed):
    """Саван: почти чёрная ткань, светлые рваные нити, изорванный нижний край."""
    c = rim(base, y, fh, 1.28, 0.66)
    thread = (x * 3 + (seed >> 4)) % 9
    if thread == 0:
        c = shade(c, 1.35)                       # нить основы
    elif thread == 4:
        c = shade(c, 0.72)
    if y > fh * 0.72 and smooth_noise(seed, x, y, 1.8) > 0.46:
        c = shade(c, 0.55)                       # рваный низ
    return c


def morok_bone(face, x, y, fw, fh, base, seed):
    """Кость маски: тёплая, с трещинами и тёмными швами между костями."""
    c = rim(base, y, fh, 1.06, 0.84)
    crack = smooth_noise(seed, x, y, 2.0)
    if crack > 0.80:
        c = shade(c, 0.80)
    elif crack < 0.18:
        c = shade(c, 1.06)
    if y == 0 or y == fh - 1:
        c = shade(c, 0.86)                       # шов по краю
    return c


def morok_face(face, x, y, fw, fh, base, seed):
    """Череп: на передней грани тёмная переносица и линия челюсти, щёки в тени."""
    c = morok_bone(face, x, y, fw, fh, base, seed)
    if face == "north":
        if abs(x - (fw - 1) / 2.0) < 1.0 and y < fh * 0.45:
            c = shade(c, 0.72)                   # переносица
        if y > fh - 2:
            c = shade(c, 0.66)                   # линия челюсти
    if face in ("east", "west") and y > fh * 0.62:
        c = shade(c, 0.88)
    return c


MOROK = {
    "skin": morok_skin,
    "skin_dark": morok_skin,
    "chest": morok_chest,
    "bone": morok_face,
    "bone_dark": morok_bone,
    "socket": lambda face, x, y, fw, fh, base, seed: hx("#070809"),
    "cloak": morok_cloak,
    "claw": lambda face, x, y, fw, fh, base, seed: shade(hx("#0A0B0D"), 1.0 + (0.25 if y == 0 else 0.0)),
    "eye": lambda face, x, y, fw, fh, base, seed: hx("#FF7A18"),
}


if __name__ == "__main__":
    print("художники текстур: PEGASUS, WENDIGO, MOROK")
    print({k: len(v) for k, v in (("pegasus", PEGASUS), ("wendigo", WENDIGO), ("morok", MOROK))})
