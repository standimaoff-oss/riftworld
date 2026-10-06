"""Tiny standard-library-only raster/PNG helper for the project's 16x16 texture scripts."""
from __future__ import annotations

import math
import os
import struct
import zlib


class Canvas:
    def __init__(self, mode: str, size: tuple[int, int], color):
        if mode not in ("RGB", "RGBA"):
            raise ValueError(f"unsupported texture mode: {mode}")
        self.mode = mode
        self.width, self.height = size
        self.channels = 3 if mode == "RGB" else 4
        rgba = tuple(color)
        if len(rgba) == 3 and self.channels == 4:
            rgba += (255,)
        if len(rgba) != self.channels:
            raise ValueError(f"expected {self.channels} color channels, got {rgba}")
        self.pixels = bytearray(rgba * (self.width * self.height))

    def _set(self, x: int, y: int, color):
        if not (0 <= x < self.width and 0 <= y < self.height):
            return
        c = tuple(color)
        if len(c) == 3 and self.channels == 4:
            c += (255,)
        elif len(c) == 4 and self.channels == 3:
            c = c[:3]
        i = (y * self.width + x) * self.channels
        self.pixels[i:i + self.channels] = bytes(max(0, min(255, int(v))) for v in c)

    def save(self, path: str):
        os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
        color_type = 2 if self.channels == 3 else 6
        scanlines = bytearray()
        stride = self.width * self.channels
        for y in range(self.height):
            scanlines.append(0)  # PNG filter: None
            start = y * stride
            scanlines.extend(self.pixels[start:start + stride])

        def chunk(kind: bytes, data: bytes) -> bytes:
            return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xffffffff)

        png = (b"\x89PNG\r\n\x1a\n"
               + chunk(b"IHDR", struct.pack(">IIBBBBB", self.width, self.height, 8, color_type, 0, 0, 0))
               + chunk(b"IDAT", zlib.compress(bytes(scanlines), 9))
               + chunk(b"IEND", b""))
        with open(path, "wb") as f:
            f.write(png)


class Image:
    @staticmethod
    def new(mode: str, size: tuple[int, int], color):
        return Canvas(mode, size, color)


class _Draw:
    def __init__(self, image: Canvas):
        self.image = image

    def point(self, xy, fill):
        x, y = xy
        self.image._set(int(round(x)), int(round(y)), fill)

    def _line(self, p0, p1, fill, width=1):
        x0, y0 = map(int, map(round, p0))
        x1, y1 = map(int, map(round, p1))
        dx, dy = abs(x1 - x0), abs(y1 - y0)
        sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
        err = dx - dy
        radius = max(0, int(width) - 1) // 2
        while True:
            for ox in range(-radius, radius + 1):
                for oy in range(-radius, radius + 1):
                    self.image._set(x0 + ox, y0 + oy, fill)
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * err
            if e2 > -dy:
                err -= dy
                x0 += sx
            if e2 < dx:
                err += dx
                y0 += sy

    def line(self, xy, fill, width=1):
        points = list(xy)
        for a, b in zip(points, points[1:]):
            self._line(a, b, fill, width)
        if len(points) == 1:
            self.point(points[0], fill)

    def rectangle(self, xy, fill=None, outline=None, width=1):
        x0, y0, x1, y1 = map(int, xy)
        x0, x1 = sorted((x0, x1))
        y0, y1 = sorted((y0, y1))
        if fill is not None:
            for y in range(y0, y1 + 1):
                for x in range(x0, x1 + 1):
                    self.image._set(x, y, fill)
        if outline is not None:
            for w in range(max(1, int(width))):
                self._line((x0 + w, y0 + w), (x1 - w, y0 + w), outline)
                self._line((x1 - w, y0 + w), (x1 - w, y1 - w), outline)
                self._line((x1 - w, y1 - w), (x0 + w, y1 - w), outline)
                self._line((x0 + w, y1 - w), (x0 + w, y0 + w), outline)

    def ellipse(self, xy, fill=None, outline=None, width=1):
        x0, y0, x1, y1 = map(float, xy)
        cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
        rx, ry = max(0.5, (x1 - x0) / 2), max(0.5, (y1 - y0) / 2)
        for y in range(int(math.floor(y0)), int(math.ceil(y1)) + 1):
            for x in range(int(math.floor(x0)), int(math.ceil(x1)) + 1):
                d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2
                if fill is not None and d <= 1:
                    self.image._set(x, y, fill)
                if outline is not None and 0.72 <= d <= 1.25:
                    self.image._set(x, y, outline)

    def polygon(self, xy, fill=None, outline=None):
        points = [(int(round(x)), int(round(y))) for x, y in xy]
        if not points:
            return
        if fill is not None:
            ymin = min(y for _, y in points)
            ymax = max(y for _, y in points)
            for y in range(ymin, ymax + 1):
                xs = []
                for (x1, y1), (x2, y2) in zip(points, points[1:] + points[:1]):
                    if y1 == y2:
                        continue
                    if min(y1, y2) <= y < max(y1, y2):
                        xs.append(x1 + (y - y1) * (x2 - x1) / (y2 - y1))
                xs.sort()
                for left, right in zip(xs[::2], xs[1::2]):
                    for x in range(int(math.ceil(left)), int(math.floor(right)) + 1):
                        self.image._set(x, y, fill)
        if outline is not None:
            self.line(points + [points[0]], outline)

    def arc(self, xy, start, end, fill):
        x0, y0, x1, y1 = map(float, xy)
        cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
        rx, ry = (x1 - x0) / 2, (y1 - y0) / 2
        steps = max(12, int(abs(end - start) * max(rx, ry) / 45))
        points = []
        for i in range(steps + 1):
            angle = math.radians(start + (end - start) * i / steps)
            points.append((cx + math.cos(angle) * rx, cy + math.sin(angle) * ry))
        self.line(points, fill)


class ImageDraw:
    @staticmethod
    def Draw(image: Canvas):
        return _Draw(image)
