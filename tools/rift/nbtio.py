"""Minimal gzip-NBT writer for structure templates (shared by gen_city)."""
import gzip, io, struct

TAG = {"b": 1, "s": 2, "i": 3, "l": 4, "f": 5, "d": 6, "str": 8, "list": 9, "comp": 10}


def tag_type(v):
    if isinstance(v, tuple):
        return TAG[v[0]]
    if isinstance(v, bool):
        return 1
    if isinstance(v, int):
        return 3
    if isinstance(v, float):
        return 6
    if isinstance(v, str):
        return 8
    if isinstance(v, list):
        return 9
    if isinstance(v, dict):
        return 10
    raise TypeError(v)


def wstr(o, s):
    b = s.encode("utf-8"); o.write(struct.pack(">H", len(b))); o.write(b)


def wpayload(o, v):
    t = tag_type(v)
    if isinstance(v, tuple) and v[0] in "bsilfd":
        v = v[1]
    if t == 1:
        o.write(struct.pack(">b", int(v)))
    elif t == 2:
        o.write(struct.pack(">h", v))
    elif t == 3:
        o.write(struct.pack(">i", v))
    elif t == 4:
        o.write(struct.pack(">q", v))
    elif t == 5:
        o.write(struct.pack(">f", v))
    elif t == 6:
        o.write(struct.pack(">d", v))
    elif t == 8:
        wstr(o, v)
    elif t == 9:
        et = tag_type(v[0]) if v else 0
        o.write(struct.pack(">bi", et, len(v)))
        for e in v:
            wpayload(o, e)
    elif t == 10:
        for k, e in v.items():
            o.write(struct.pack(">b", tag_type(e))); wstr(o, k); wpayload(o, e)
        o.write(b"\x00")


def write_template(path, size, B, NBT):
    """B: (x,y,z)->(name, props-tuple); NBT: (x,y,z)->dict."""
    palette, pidx, blocks = [], {}, []
    for pos in sorted(B, key=lambda p: (p[1], p[2], p[0])):
        st = B[pos]
        if st not in pidx:
            pidx[st] = len(palette)
            e = {"Name": st[0]}
            if st[1]:
                e["Properties"] = {k: v for k, v in st[1]}
            palette.append(e)
        be = {"pos": list(pos), "state": pidx[st]}
        if pos in NBT:
            be["nbt"] = dict(NBT[pos])
        blocks.append(be)
    root = {"DataVersion": 3465, "size": list(size), "palette": palette, "blocks": blocks, "entities": []}
    buf = io.BytesIO()
    buf.write(struct.pack(">b", 10)); wstr(buf, ""); wpayload(buf, root)
    with gzip.open(path, "wb") as f:
        f.write(buf.getvalue())
    return len(blocks), len(palette)
