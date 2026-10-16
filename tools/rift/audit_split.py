#!/usr/bin/env python3
"""Аудит п.5 «разделение блоков»: для всех структурных NBT считаем пары владелец/верхушка.
Если верхушка (декор) встречается БЕЗ владельца или чаще него — декор всё ещё затирает хозяина."""
import glob, gzip, io, os, struct, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from collections import Counter

def rd(f, t):
    if t == 1: return struct.unpack(">b", f.read(1))[0]
    if t == 2: return struct.unpack(">h", f.read(2))[0]
    if t == 3: return struct.unpack(">i", f.read(4))[0]
    if t == 4: return struct.unpack(">q", f.read(8))[0]
    if t == 5: return struct.unpack(">f", f.read(4))[0]
    if t == 6: return struct.unpack(">d", f.read(8))[0]
    if t == 8:
        n = struct.unpack(">H", f.read(2))[0]; return f.read(n).decode()
    if t == 9:
        et, n = struct.unpack(">bi", f.read(5)); return [rd(f, et) for _ in range(n)]
    if t == 10:
        o = {}
        while True:
            tt = struct.unpack(">b", f.read(1))[0]
            if tt == 0: return o
            n = struct.unpack(">H", f.read(2))[0]; k = f.read(n).decode(); o[k] = rd(f, tt)

PAIRS = [("alchemy_table", "alchemy_set"), ("kitchen_counter", "kitchen_set"),
         ("chained_lectern", "chained_books"), ("castle_bookshelf", "paper_pile")]
bad = 0
for path in sorted(glob.glob("mod/src/main/resources/data/riftworld/structures/**/*.nbt", recursive=True)):
    f = io.BytesIO(gzip.open(path).read()); f.read(1); f.read(struct.unpack(">H", f.read(2))[0])
    root = rd(f, 10)
    c = Counter(p["Name"].split(":")[-1] for p in root["palette"])
    for owner, topper in PAIRS:
        no, nt = c.get(owner, 0), c.get(topper, 0)
        if nt > no:
            print("  SUSPECT %s: %s x%d без хозяина %s x%d" % (os.path.basename(path), topper, nt, owner, no)); bad += 1
print("audit split: suspect pairs =", bad)
