#!/usr/bin/env python3
"""Собрать .glb из .obj без Blender.

Нужен для моделей, которые правились вручную (OBJ) и должны попасть и в
item-иконку (GLB). Blender в песочнице нет, а формат glTF 2.0 простой:
достаточно JSON-чанка с буферами и BIN-чанка с данными.

Поддерживается ровно то, что нужно нашим пропсам:
  * четырехугольники (f с 4 вершинами) и треугольники;
  * материалы из .mtl (map_Kd -> текстура), каждый материал = свой примитив;
  * UV из .obj.

Запуск:
    python3 tools/rift/obj_to_glb.py models/block/obj/painting_islands.obj
"""
import json
import math
import os
import struct
import sys

# glTF: 5126 = float, 5123 = ushort, 5125 = uint
GL_FLOAT = 5126
GL_USHORT = 5123
GL_UINT = 5125

BIN_CHUNK = 0x004E4942
JSON_CHUNK = 0x4E4F534A


def read_mtl(path):
    """Разбор .mtl: имя материала -> путь текстуры (без расширения)."""
    materials = {}
    if not os.path.exists(path):
        return materials
    cur = None
    for line in open(path, encoding='utf-8'):
        s = line.strip()
        if s.startswith('newmtl '):
            cur = s[7:].strip()
            materials[cur] = None
        elif s.startswith('map_Kd ') and cur:
            tex = s[7:].strip().split()[-1]
            # riftworld:block/obj/m_wood -> m_wood
            materials[cur] = tex.split('/')[-1]
    return materials


def read_obj(path):
    """Разбор .obj: вершины, UV, материал -> список граней (индексы v/vt)."""
    verts, uvs = [], []
    groups = []
    cur = None
    for line in open(path, encoding='utf-8'):
        s = line.strip()
        if s.startswith('v '):
            verts.append(tuple(float(x) for x in s.split()[1:4]))
        elif s.startswith('vt '):
            uvs.append(tuple(float(x) for x in s.split()[1:3]))
        elif s.startswith('usemtl '):
            cur = s[7:].strip()
            groups.append([cur, []])
        elif s.startswith('f ') and groups:
            groups[-1][1].append(s.split()[1:])
    return verts, uvs, groups


def load_png_rgba(path):
    """Читает PNG в RGBA-байты (только 8-битные PNG без палитры)."""
    import struct as st
    import zlib
    data = open(path, 'rb').read()
    assert data[:8] == b'\x89PNG\r\n\x1a\n', '%s: не PNG' % path
    pos = 8
    width = height = 0
    bitdepth = colortype = 0
    idat = b''
    while pos < len(data):
        length = st.unpack('>I', data[pos:pos + 4])[0]
        ctype = data[pos + 4:pos + 8]
        chunk = data[pos + 8:pos + 8 + length]
        if ctype == b'IHDR':
            width, height, bitdepth, colortype = st.unpack('>IIBB', chunk[:10])
        elif ctype == b'IDAT':
            idat += chunk
        elif ctype == b'IEND':
            break
        pos += 12 + length
    assert bitdepth == 8, '%s: только 8 бит' % path
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[colortype]
    raw = zlib.decompress(idat)
    stride = width * channels
    out = bytearray(width * height * 4)
    prev = bytearray(stride)
    p = 0
    for y in range(height):
        filt = raw[p]
        p += 1
        line = bytearray(raw[p:p + stride])
        p += stride
        for i in range(stride):
            a = line[i - channels] if i >= channels else 0
            b = prev[i]
            c = prev[i - channels] if i >= channels else 0
            x = line[i]
            if filt == 1:
                x += a
            elif filt == 2:
                x += b
            elif filt == 3:
                x += (a + b) // 2
            elif filt == 4:
                pp = a + b - c
                pa, pb, pc = abs(pp - a), abs(pp - b), abs(pp - c)
                x += a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
            line[i] = x & 0xFF
        for xx in range(width):
            src = xx * channels
            dst = (y * width + xx) * 4
            if colortype == 6:
                out[dst:dst + 4] = line[src:src + 4]
            elif colortype == 2:
                out[dst:dst + 3] = line[src:src + 3]
                out[dst + 3] = 255
            elif colortype == 0:
                v = line[src]
                out[dst:dst + 4] = bytes((v, v, v, 255))
            elif colortype == 4:
                v = line[src]
                out[dst:dst + 4] = bytes((v, v, v, line[src + 1]))
            else:  # palette
                v = line[src]
                out[dst:dst + 4] = bytes((v, v, v, 255))
        prev = line
    return width, height, bytes(out)


def build_glb(obj_path, out_path, texture_dir):
    verts, uvs, groups = read_obj(obj_path)
    base = os.path.splitext(os.path.basename(obj_path))[0]
    materials = read_mtl(os.path.splitext(obj_path)[0] + '.mtl')

    buffers = bytearray()
    buffer_views = []
    accessors = []
    meshes = []
    gltf_materials = []
    images = []
    textures = []
    samplers = []

    def add_view(data, target=None):
        while len(buffers) % 4:
            buffers.append(0)
        offset = len(buffers)
        buffers.extend(data)
        view = {'buffer': 0, 'byteOffset': offset, 'byteLength': len(data)}
        if target:
            view['target'] = target
        buffer_views.append(view)
        return len(buffer_views) - 1

    def add_float_accessor(values, comps):
        # values — плоский список компонентов (len = count * comps).
        count = len(values) // comps
        data = struct.pack('<%df' % len(values), *values)
        view = add_view(data, 34962)
        acc = {'bufferView': view, 'componentType': GL_FLOAT, 'count': count, 'type': {1: 'SCALAR', 2: 'VEC2', 3: 'VEC3'}[comps]}
        if comps == 3:
            xs = values[0::3]
            ys = values[1::3]
            zs = values[2::3]
            acc['min'] = [min(xs), min(ys), min(zs)]
            acc['max'] = [max(xs), max(ys), max(zs)]
        accessors.append(acc)
        return len(accessors) - 1

    def add_index_accessor(indices):
        if max(indices) < 65536:
            data = struct.pack('<%dH' % len(indices), *indices)
            view = add_view(data, 34963)
            comp = GL_USHORT
        else:
            data = struct.pack('<%dI' % len(indices), *indices)
            view = add_view(data, 34963)
            comp = GL_UINT
        accessors.append({'bufferView': view, 'componentType': comp, 'count': len(indices), 'type': 'SCALAR'})
        return len(accessors) - 1

    for mat_name, faces in groups:
        if not faces:
            continue
        # Разворачиваем грани в треугольники, дублируя вершины (у каждой
        # грани свой набор, чтобы UV не «сшивались» между гранями).
        pos, uvs_out, idx = [], [], []
        for f in faces:
            toks = f
            vi = []
            ti = []
            for tok in toks:
                parts = tok.split('/')
                vi.append(int(parts[0]) - 1)
                ti.append(int(parts[1]) - 1 if len(parts) > 1 and parts[1] else 0)
            # веер: (0, i, i+1)
            for k in range(1, len(vi) - 1):
                for corner in (0, k, k + 1):
                    v = verts[vi[corner]]
                    pos.extend(v)
                    t = uvs[ti[corner]] if ti[corner] < len(uvs) else (0.0, 0.0)
                    uvs_out.extend(t)
                    idx.append(len(pos) // 3 - 1)

        tex_name = materials.get(mat_name)
        mat_index = len(gltf_materials)
        if tex_name:
            tex_path = os.path.join(texture_dir, tex_name + '.png')
            if os.path.exists(tex_path):
                w, h, rgba = load_png_rgba(tex_path)
                view = add_view(rgba)
                images.append({'bufferView': view, 'mimeType': 'image/png', 'name': tex_name})
                img_index = len(images) - 1
                if not samplers:
                    samplers.append({'magFilter': 9729, 'minFilter': 9987, 'wrapS': 10497, 'wrapT': 10497})
                textures.append({'sampler': 0, 'source': img_index})
                tex_index = len(textures) - 1
                gltf_materials.append({
                    'name': mat_name,
                    'pbrMetallicRoughness': {
                        'baseColorTexture': {'index': tex_index},
                        'metallicFactor': 0.0,
                        'roughnessFactor': 0.9,
                    },
                })
            else:
                gltf_materials.append({'name': mat_name,
                                       'pbrMetallicRoughness': {'baseColorFactor': [0.7, 0.55, 0.4, 1.0]}})
        else:
            gltf_materials.append({'name': mat_name,
                                   'pbrMetallicRoughness': {'baseColorFactor': [0.7, 0.55, 0.4, 1.0]}})

        a_pos = add_float_accessor(pos, 3)
        a_uv = add_float_accessor(uvs_out, 2)
        a_idx = add_index_accessor(idx)
        meshes.append({
            'name': '%s_%d' % (mat_name, mat_index),
            'primitives': [{
                'attributes': {'POSITION': a_pos, 'TEXCOORD_0': a_uv},
                'indices': a_idx,
                'material': mat_index,
                'mode': 4,
            }],
        })

    gltf = {
        'asset': {'version': '2.0', 'generator': 'riftworld obj_to_glb'},
        'scene': 0,
        'scenes': [{'nodes': [0]}],
        'nodes': [{'name': base, 'mesh': 0}],
        'meshes': meshes,
        'materials': gltf_materials,
        'textures': textures,
        'images': images,
        'samplers': samplers,
        'accessors': accessors,
        'bufferViews': buffer_views,
        'buffers': [{'byteLength': len(buffers)}],
    }

    jsb = json.dumps(gltf, separators=(',', ':')).encode('utf-8')
    jsb += b' ' * ((4 - len(jsb) % 4) % 4)
    binchunk = bytes(buffers)
    binchunk += b'\x00' * ((4 - len(binchunk) % 4) % 4)
    total = 12 + 8 + len(jsb) + 8 + len(binchunk)
    out = bytearray()
    out += struct.pack('<III', 0x46546C67, 2, total)
    out += struct.pack('<II', len(jsb), JSON_CHUNK) + jsb
    out += struct.pack('<II', len(binchunk), BIN_CHUNK) + binchunk
    with open(out_path, 'wb') as f:
        f.write(out)
    return len(meshes), len(images)


def main():
    if len(sys.argv) < 2:
        raise SystemExit('usage: obj_to_glb.py <file.obj> [out.glb]')
    obj = sys.argv[1]
    out = sys.argv[2] if len(sys.argv) > 2 else os.path.splitext(obj)[0] + '.glb'
    # obj лежит в <assets>/<namespace>/models/block/obj,
    # текстуры — в <assets>/<namespace>/textures/block/obj.
    # Ищем корень namespace, поднимаясь вверх до папки с textures/block/obj.
    obj_dir = os.path.dirname(os.path.abspath(obj))
    namespace = None
    probe = obj_dir
    for _ in range(6):
        probe = os.path.dirname(probe)
        if os.path.isdir(os.path.join(probe, 'textures', 'block', 'obj')):
            namespace = probe
            break
    if namespace is None:
        raise SystemExit('не нашёл textures/block/obj рядом с %s' % obj)
    tex_dir = os.path.join(namespace, 'textures', 'block', 'obj')
    meshes, images = build_glb(obj, out, tex_dir)
    print('%s -> %s: примитивов=%d текстур=%d' % (obj, out, meshes, images))


if __name__ == '__main__':
    main()
