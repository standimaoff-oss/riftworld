#!/usr/bin/env python3
"""Починить winding (порядок вершин) у граней в .glb без Blender.

Некоторые плоскости в моделях экспортированы с обратным обходом: верхняя
грань смотрит вниз, и рендерер рисует её изнанкой (лицевая сторона
отсекается — крыша «вывернута»).

Скрипт берёт готовый .glb, находит примитивы нужного материала и
переворачивает треугольники, у которых нормаль смотрит строго вниз,
после чего пересобирает GLB-контейнер на месте.

Запуск:
    python3 tools/rift/fix_glb_winding.py models/glb/market_stall.glb e_stripe
"""
import json
import math
import struct
import sys

COMP = {5120: 'b', 5121: 'B', 5122: 'h', 5123: 'H', 5125: 'I', 5126: 'f'}
# Размер компонента в байтах по enum-значению glTF (5126 — это GL_FLOAT, 4 байта).
ESZ = {5120: 1, 5121: 1, 5122: 2, 5123: 2, 5125: 4, 5126: 4}
NCOMP = {'SCALAR': 1, 'VEC2': 2, 'VEC3': 3, 'VEC4': 4, 'MAT4': 16}
BIN_CHUNK = 0x004E4942
JSON_CHUNK = 0x4E4F534A


def load_glb(path):
    """Возвращает (bytearray с данными, json, смещение BIN, длина BIN)."""
    data = bytearray(open(path, 'rb').read())
    if data[:4] != b'glTF':
        raise SystemExit('%s: не GLB' % path)
    json_len = struct.unpack_from('<I', data, 12)[0]
    js = json.loads(bytes(data[20:20 + json_len]).decode('utf-8'))
    off = 20 + json_len
    while off + 8 <= len(data):
        clen, ctype = struct.unpack_from('<II', data, off)
        if ctype == BIN_CHUNK:
            return data, js, off + 8, clen
        off += 8 + clen
    raise SystemExit('%s: нет BIN-чанка' % path)


def save_glb(path, data, js, bin_off, bin_len):
    jsb = json.dumps(js, separators=(',', ':')).encode('utf-8')
    jsb += b' ' * ((4 - len(jsb) % 4) % 4)
    chunk = bytes(data[bin_off:bin_off + bin_len])
    total = 12 + 8 + len(jsb) + 8 + len(chunk)
    out = bytearray()
    out += struct.pack('<III', 0x46546C67, 2, total)
    out += struct.pack('<II', len(jsb), JSON_CHUNK) + jsb
    out += struct.pack('<II', len(chunk), BIN_CHUNK) + chunk
    open(path, 'wb').write(out)


def accessor_layout(js, index):
    """(accessor, bufferView, число компонент, размер компонента, шаг)."""
    acc = js['accessors'][index]
    view = js['bufferViews'][acc['bufferView']]
    n = NCOMP[acc['type']]
    esz = ESZ[acc['componentType']]
    stride = view.get('byteStride') or n * esz
    return acc, view, n, esz, stride


def read_accessor(data, bin_off, js, index):
    acc, view, n, esz, stride = accessor_layout(js, index)
    fmt = COMP[acc['componentType']]
    base = bin_off + view.get('byteOffset', 0) + acc.get('byteOffset', 0)
    return [struct.unpack_from('<%d%s' % (n, fmt), data, base + i * stride)
            for i in range(acc['count'])]


def write_accessor(data, bin_off, js, index, values):
    acc, view, n, esz, stride = accessor_layout(js, index)
    fmt = COMP[acc['componentType']]
    base = bin_off + view.get('byteOffset', 0) + acc.get('byteOffset', 0)
    for i, v in enumerate(values):
        struct.pack_into('<%d%s' % (n, fmt), data, base + i * stride, *v)


def normal_of(pos, a, b, c):
    ax, ay, az = pos[b][0] - pos[a][0], pos[b][1] - pos[a][1], pos[b][2] - pos[a][2]
    bx, by, bz = pos[c][0] - pos[a][0], pos[c][1] - pos[a][1], pos[c][2] - pos[a][2]
    nx = ay * bz - az * by
    ny = az * bx - ax * bz
    nz = ax * by - ay * bx
    length = math.sqrt(nx * nx + ny * ny + nz * nz) or 1.0
    return nx / length, ny / length, nz / length


def fix(path, material, threshold=0.5):
    data, js, bin_off, bin_len = load_glb(path)
    triangles = 0
    flipped = 0
    for mesh in js['meshes']:
        for prim in mesh['primitives']:
            name = js['materials'][prim['material']]['name']
            if name != material:
                continue
            pos = read_accessor(data, bin_off, js, prim['attributes']['POSITION'])
            idx = [v[0] for v in read_accessor(data, bin_off, js, prim['indices'])]
            for t in range(0, len(idx), 3):
                triangles += 1
                _, ny, _ = normal_of(pos, idx[t], idx[t + 1], idx[t + 2])
                if ny < -threshold:
                    idx[t + 1], idx[t + 2] = idx[t + 2], idx[t + 1]
                    flipped += 1
            write_accessor(data, bin_off, js, prim['indices'], [(v,) for v in idx])
    save_glb(path, data, js, bin_off, bin_len)
    return triangles, flipped


def main():
    if len(sys.argv) < 3:
        raise SystemExit('usage: fix_glb_winding.py <file.glb> <material>')
    path, material = sys.argv[1], sys.argv[2]
    triangles, flipped = fix(path, material)
    print('%s: треугольников=%d перевёрнуто=%d' % (path, triangles, flipped))
    if flipped == 0:
        raise SystemExit('ничего не перевёрнуто — проверьте материал/модель')


if __name__ == '__main__':
    main()
