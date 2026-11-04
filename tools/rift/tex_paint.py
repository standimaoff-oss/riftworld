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
def pegasus_coat(face, x, y, fw, fh, base, seed):
    """Белая шкура с серыми яблоками; низ темнее, круп светлее."""
    c = rim(base, y, fh, 1.06, 0.86)
    c = fur(c, seed, x, y, 0.06, 2.6)
    # яблоки: редкие мягкие пятна на боках и крупе
    if face in ("east", "west", "up", "north", "south"):
        n = smooth_noise(seed + 7, x, y, 4.0)
        if n > 0.72:
            c = shade(c, 0.88 - (0.08 if n > 0.85 else 0.0))
        elif n < 0.22:
            c = shade(c, 1.05)
    return c


def pegasus_leg(face, x, y, fw, fh, base, seed):
    """Нога: сверху белая, к копыту темнеет (грязь и шерсть)."""
    t = y / max(1, fh - 1)
    c = mixc(shade(base, 1.04), shade(hx("#B9B2A6"), 0.96), max(0.0, t - 0.45) / 0.55)
    return fur(c, seed, x, y, 0.07, 1.8)


def pegasus_skull(face, x, y, fw, fh, base, seed):
    """Голова: глаза на боках, тёмная переносица сверху, белая морда."""
    c = pegasus_coat(face, x, y, fw, fh, base, seed)
    if fw < 3 or fh < 3:
        return c
    if face in ("east", "west"):
        # глаз: миндалина с бликом — на 45 % по длине и 38 % по высоте грани
        ex, ey = fw * 0.42, fh * 0.38
        if ellipse(x, y, ex, ey, max(1.2, fw * 0.16), max(1.0, fh * 0.13)):
            c = hx("#1B1F26")
            if ellipse(x, y, ex - fw * 0.05, ey - fh * 0.04, 0.9, 0.9):
                c = hx("#EDF3FA")
            elif ellipse(x, y, ex, ey, max(0.9, fw * 0.09), max(0.8, fh * 0.07)):
                c = hx("#3A4652")
        # скула: чуть темнее под глазом
        elif y > fh * 0.55 and smooth_noise(seed, x, y, 3.0) > 0.6:
            c = shade(c, 0.93)
    if face == "up":
        # тёмная полоса по переносице
        if abs(x - (fw - 1) / 2.0) < max(1.0, fw * 0.16):
            c = mixc(c, hx("#C9C2B6"), 0.55)
    if face == "north":
        # морда: розоватый нос и ноздри
        if y > fh * 0.55:
            c = mixc(c, hx("#C79E96"), 0.45)
        if y < fh * 0.45 and ellipse(x, y, fw * 0.30, fh * 0.55, 1.0, 1.0):
            c = hx("#6B4A45")
        if y < fh * 0.45 and ellipse(x, y, fw * 0.70, fh * 0.55, 1.0, 1.0):
            c = hx("#6B4A45")
    return c


def pegasus_mane(face, x, y, fw, fh, base, seed):
    """Грива: вертикальные пряди с тёмными прядями и светлыми бликами."""
    c = rim(base, y, fh, 1.12, 0.80)
    strand = (x * 5 + (seed >> 3)) % 7
    if strand == 0:
        c = shade(c, 0.78)
    elif strand == 3:
        c = shade(c, 1.08)
    if y > fh * 0.75:
        c = shade(c, 0.86)          # концы прядей темнее
    return fur(c, seed, x, y, 0.05, 1.4)


def pegasus_feather(face, x, y, fw, fh, base, seed):
    """Маховое перо: ряды чешуек, светлая кромка сверху, тёмный кончик."""
    row = y // 2
    c = rim(base, y, fh, 1.08, 0.84)
    if (y % 2) == 0:
        c = shade(c, 0.95)
    if (x + row * 3) % 5 == 0:
        c = shade(c, 0.88)                          # стержень пера
    if y >= fh - 1:
        c = shade(c, 0.80)                          # тёмный кончик
    if y == 0:
        c = shade(c, 1.10)                          # светлая кромка
    return c


def pegasus_feather_edge(face, x, y, fw, fh, base, seed):
    c = pegasus_feather(face, x, y, fw, fh, base, seed)
    return shade(c, 0.92)


def pegasus_hoof(face, x, y, fw, fh, base, seed):
    """Копыто: почти чёрное с блеском сверху и светлой полосой у венчика."""
    c = shade(base, 1.0 + (0.20 if y == 0 else 0.0))
    if y <= max(1, fh // 4):
        c = mixc(c, hx("#6E6A62"), 0.35)
    if smooth_noise(seed, x, y, 1.6) > 0.75:
        c = shade(c, 0.72)
    return c


def pegasus_saddle(face, x, y, fw, fh, base, seed):
    """Седло: кожа с прошивкой по краю и золотой пряжкой."""
    c = rim(base, y, fh, 1.12, 0.82)
    if face in ("north", "south", "east", "west"):
        if x in (0, fw - 1) or y in (0, fh - 1):
            c = hx("#E3D6A8")                       # прошивка
        if ellipse(x, y, fw * 0.5, fh * 0.5, fw * 0.18, fh * 0.16):
            c = hx("#C9A24A")                       # пряжка
            if ellipse(x, y, fw * 0.5, fh * 0.5, fw * 0.08, fh * 0.07):
                c = hx("#8C6B21")
    else:
        c = shade(c, 0.9)
    return c


PEGASUS = {
    "coat": pegasus_coat,
    "leg": pegasus_leg,
    "skull": pegasus_skull,
    "mane": pegasus_mane,
    "tail": pegasus_mane,
    "feather": pegasus_feather,
    "feather_edge": pegasus_feather_edge,
    "hoof": pegasus_hoof,
    "saddle": pegasus_saddle,
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
def morok_veil(face, x, y, fw, fh, base, seed):
    """Рваная вуаль: тёмная ткань с неровным нижним краем."""
    c = rim(base, y, fh, 1.14, 0.74)
    if y > fh * 0.7 and smooth_noise(seed, x, y, 2.0) > 0.5:
        c = shade(c, 0.62)
    return c


def morok_skin(face, x, y, fw, fh, base, seed):
    """Кожа Морока: пепельная, с прожилками и грязными пятнами."""
    c = rim(base, y, fh, 1.08, 0.80)
    if smooth_noise(seed, x, y, 2.6) > 0.74:
        c = shade(c, 0.84)
    if (x * 2 + y * 3) % 13 == 0:
        c = shade(c, 1.10)
    return c


def morok_skull(face, x, y, fw, fh, base, seed):
    """Лицо: два уголька глаз и щель пасти спереди, тени по бокам."""
    c = morok_skin(face, x, y, fw, fh, base, seed)
    if face == "north":
        if ellipse(x, y, fw * 0.28, fh * 0.36, max(1.2, fw * 0.15), max(1.0, fh * 0.14)):
            c = hx("#0D0F11")
        if ellipse(x, y, fw * 0.72, fh * 0.36, max(1.2, fw * 0.15), max(1.0, fh * 0.14)):
            c = hx("#0D0F11")
        if y > fh * 0.62 and abs(x - (fw - 1) / 2.0) < max(1.0, fw * 0.30):
            c = hx("#12141A")                       # пасть
    if face in ("east", "west") and y > fh * 0.5 and smooth_noise(seed, x, y, 2.0) > 0.6:
        c = shade(c, 0.88)
    return c


MOROK = {
    "veil": morok_veil,
    "skin": morok_skin,
    "skin_dark": morok_skin,
    "skull": morok_skull,
    "bone": wendigo_bone,
    "claw": wendigo_claw,
}


if __name__ == "__main__":
    print("художники текстур: PEGASUS, WENDIGO, MOROK")
    print({k: len(v) for k, v in (("pegasus", PEGASUS), ("wendigo", WENDIGO), ("morok", MOROK))})
