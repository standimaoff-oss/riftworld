#!/usr/bin/env python3
"""Сгенерировать звуки мода Riftworld (OGG Vorbis).

Звуков немного — четыре, ровно столько, сколько просил пользователь
(«добавь звуки переплавки и другим вещам, но не много»):

  * elisar_smelting    — гудение горна/переплавки;
  * elisar_coin        — звон монет при торговле;
  * elisar_market_bell — колокол рынка;
  * elisar_hammer      — удар молота кузнеца.

Все звуки синтезируются: шум + фильтры + огибающие. Формат — моно, 44.1 кГц,
Ogg Vorbis (именно его ждёт Minecraft).

Запуск:
    python3 tools/rift/gen_sounds.py
"""
import math
import os
import struct
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '.pylibs'))

OUT = 'mod/src/main/resources/assets/riftworld/sounds'
RATE = 44100


def write_ogg(path, samples):
    """Пишет моно OGG Vorbis через soundfile (libsndfile)."""
    import numpy as np
    import soundfile as sf
    data = np.asarray(samples, dtype=np.float32)
    peak = float(max(abs(x) for x in data)) or 1.0
    data = data / peak * 0.9
    sf.write(path, data, RATE, format='OGG', subtype='VORBIS')
    return len(data)


def env(i, n, attack=0.01, release=0.6):
    """Огибающая: быстрый подъём и экспоненциальное затухание."""
    t = i / n
    a = min(1.0, t / attack) if attack > 0 else 1.0
    r = math.exp(-t / release)
    return a * r


def noise(rng):
    return rng.uniform(-1.0, 1.0)


class Rng:
    """Простой детерминированный ГПСЧ (xorshift) — звуки должны быть
    одинаковыми при каждой генерации."""

    def __init__(self, seed=0x2545F491):
        self.s = seed & 0xFFFFFFFF

    def next(self):
        x = self.s
        x ^= (x << 13) & 0xFFFFFFFF
        x ^= x >> 17
        x ^= (x << 5) & 0xFFFFFFFF
        self.s = x & 0xFFFFFFFF
        return (self.s / 0xFFFFFFFF) * 2.0 - 1.0

    def uniform(self, a, b):
        return a + (b - a) * (self.next() * 0.5 + 0.5)


def lowpass(samples, cutoff):
    """Однополюсный фильтр низких частот."""
    dt = 1.0 / RATE
    rc = 1.0 / (2 * math.pi * cutoff)
    alpha = dt / (rc + dt)
    out = []
    prev = 0.0
    for x in samples:
        prev += alpha * (x - prev)
        out.append(prev)
    return out


def highpass(samples, cutoff):
    """Однополюсный фильтр высоких частот."""
    dt = 1.0 / RATE
    rc = 1.0 / (2 * math.pi * cutoff)
    alpha = rc / (rc + dt)
    out = []
    prev_in = 0.0
    prev_out = 0.0
    for x in samples:
        prev_out = alpha * (prev_out + x - prev_in)
        prev_in = x
        out.append(prev_out)
    return out


def smelting(duration=1.6):
    """Гудение горна: низкий тон + шипение огня."""
    n = int(RATE * duration)
    rng = Rng(0x51A7)
    raw = [noise(rng) for _ in range(n)]
    hiss = lowpass(raw, 2600.0)
    # треск огня
    crackle = []
    for i in range(n):
        t = i / RATE
        # редкие всплески шума
        burst = 0.0
        phase = (t * 9.0) % 1.0
        if phase < 0.02:
            burst = raw[i] * (1.0 - phase / 0.02) * 0.5
        crackle.append(burst)
    out = []
    for i in range(n):
        t = i / RATE
        e = env(i, n, 0.06, 0.55)
        # низкий гул с лёгкой расстройкой
        hum = 0.55 * math.sin(2 * math.pi * 96.0 * t) + 0.25 * math.sin(2 * math.pi * 143.0 * t)
        out.append((hum * 0.7 + hiss[i] * 0.35 + crackle[i]) * e)
    return out


def coin(duration=0.55):
    """Звон монет: несколько коротких металлических отзвуков."""
    n = int(RATE * duration)
    rng = Rng(0xC01A)
    out = [0.0] * n
    # три «монеты», каждая — сумма гармоник с быстрым затуханием
    for k, (start, gain) in enumerate(((0.0, 1.0), (0.075, 0.7), (0.17, 0.45))):
        base = (2100.0, 2650.0, 3180.0)[k]
        s0 = int(start * RATE)
        length = n - s0
        for i in range(length):
            t = i / RATE
            e = math.exp(-t * 26.0)
            v = 0.0
            for h, amp in ((1, 1.0), (2.41, 0.55), (3.83, 0.3), (5.1, 0.15)):
                v += amp * math.sin(2 * math.pi * base * h * t)
            out[s0 + i] += v * e * gain * 0.18
    # лёгкий шум удара
    for i in range(min(n, int(0.02 * RATE))):
        out[i] += noise(rng) * 0.12 * (1.0 - i / (0.02 * RATE))
    return out


def market_bell(duration=2.2):
    """Колокол рынка: три тона с медленным затуханием."""
    n = int(RATE * duration)
    out = [0.0] * n
    for freq, amp in ((523.25, 1.0), (1046.5, 0.45), (1567.98, 0.22)):
        for i in range(n):
            t = i / RATE
            e = math.exp(-t * (1.6 if freq < 600 else 3.4))
            # лёгкая «детонация» в начале
            strike = 1.0 + 0.6 * math.exp(-t * 90.0) * math.sin(2 * math.pi * 7.0 * t)
            out[i] += amp * math.sin(2 * math.pi * freq * t) * e * strike
    return out


def hammer(duration=0.7):
    """Удар молота: резкий щелчок по металлу с коротким хвостом."""
    n = int(RATE * duration)
    rng = Rng(0x4A44)
    raw = [noise(rng) for _ in range(n)]
    ring = highpass(raw, 900.0)
    out = []
    for i in range(n):
        t = i / RATE
        e = math.exp(-t * 34.0)
        body = 0.35 * math.sin(2 * math.pi * 320.0 * t) + 0.2 * math.sin(2 * math.pi * 780.0 * t)
        out.append((body * math.exp(-t * 16.0) + ring[i] * 0.5) * e)
    return out


SOUNDS = {
    'elisar_smelting': smelting,
    'elisar_coin': coin,
    'elisar_market_bell': market_bell,
    'elisar_hammer': hammer,
}


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, fn in SOUNDS.items():
        path = os.path.join(OUT, name + '.ogg')
        data = fn()
        write_ogg(path, data)
        print('%-22s %6.2f с  %7d байт' % (name, len(data) / RATE, os.path.getsize(path)))


if __name__ == '__main__':
    main()
