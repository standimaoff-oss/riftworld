"""City «Странники» on planet Elisar: building templates (NBT) + city plan (plan.json) for ElisarChunkGenerator.

Terrain math (island shape / hills / rim) is mirrored 1:1 in ElisarTerrain.java - keep both in sync.
Template frame: x = 0..W-1, z = 0..D-1, front (door side) = +z (south); y = 0 is the ground layer (replaces turf).
"""
import json, math, os, random, re, sys
sys.path.insert(0, os.path.dirname(__file__))
from nbtio import write_template

MOD = "/home/user/mods/riftworld"
OUT = MOD + "/src/main/resources/data/riftworld"
JAVA_KINDS = MOD + "/src/main/java/ru/aiforge/riftworld/block/ModelKind.java"
KINDS = {m.group(1): tuple(int(m.group(i)) for i in (2, 3, 4, 5))
         for m in re.finditer(r'[A-Z_]+\("([a-z_]+)", (-?\d+), (-?\d+), (\d+), (\d+),', open(JAVA_KINDS).read())}
PASSABLE = {"sign_shop", "sign_tavern", "sign_forge", "sign_fish", "sign_police", "sign_hunter", "sign_market", "sign_temple",
            "fishing_net", "trophy_antlers", "tool_wall", "rug", "wanted_wall", "sacred_poles", "lantern_hanging"}
LIT = {"lantern_hanging", "street_lamp", "fire_bowl", "fireplace", "cooking_pot", "altar_stone", "table_wood"}


def S(name, **p):
    if ":" not in name:
        name = "riftworld:" + name
    return (name, tuple(sorted((k, str(v).lower()) for k, v in p.items())))


AIR = S("minecraft:air")


def rot(x, y, z, f):
    return {"south": (x, y, z), "north": (-x, y, -z), "west": (-z, y, x), "east": (z, y, -x)}[f]


def cells(kind, f):
    x0, x1, back, h = KINDS[kind]
    return [rot(x, y, -b, f) for x in range(x0, x1 + 1) for b in range(back + 1) for y in range(h)]


# =============================================================================================== templates
class Tpl:
    def __init__(self, name, wx, wz, floors=1, wall_h=4, seed=0):
        self.name, self.wx, self.wz, self.floors, self.wall_h = name, wx, wz, floors, wall_h
        self.W, self.D = wx + 2, wz + 2
        self.B, self.NBT, self.MC = {}, {}, set()
        self.rng = random.Random(hash(name) & 0xffff ^ seed)
        self.top = floors * (wall_h + 1) - 1          # last wall row (floors separated by a beam/floor row)
        self.door_x = None

    # -- primitives
    def put(self, x, y, z, st, nbt=None, force=False):
        assert 0 <= x < self.W and 0 <= z < self.D and y >= 0, (self.name, x, y, z)
        if (x, y, z) in self.MC and not force:
            return
        self.B[(x, y, z)] = st
        if nbt is not None:
            self.NBT[(x, y, z)] = nbt
        else:
            self.NBT.pop((x, y, z), None)

    def get(self, x, y, z):
        return self.B.get((x, y, z))

    def fill(self, x0, y0, z0, x1, y1, z1, st):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.put(x, y, z, st)

    def model(self, kind, x, y, z, f, **props):
        cs = [(x + dx, y + dy, z + dz) for dx, dy, dz in cells(kind, f)]
        for c in cs:
            assert c not in self.MC, (self.name, kind, (x, y, z), c, "overlap")
            assert 0 <= c[0] < self.W and 0 <= c[2] < self.D, (self.name, kind, c, "out of template")
            cur = self.B.get(c)
            assert cur is None or cur == AIR, (self.name, kind, c, "cell occupied by", cur)
        st = dict(facing=f, powered=kind in LIT, broken=False)
        st.update(props)
        for c in cs:
            if c == (x, y, z):
                self.B[c] = S(kind, **st)
                self.NBT[c] = {"id": "riftworld:model"}
            else:
                self.B[c] = S("part", open=kind in PASSABLE)
                self.NBT.pop(c, None)
        self.MC.update(cs)

    # -- shell
    def is_corner(self, x, z):
        return x in (1, self.wx) and z in (1, self.wz)

    def is_wall(self, x, z):
        return (x in (1, self.wx) and 1 <= z <= self.wz) or (z in (1, self.wz) and 1 <= x <= self.wx)

    def shell(self, styles, floor="sizo_planks", corner="sizo_log", found="elisar_cobble", beam="sizo_log"):
        """styles: wall style per floor ('timber', 'brick', 'plaster', 'sizo', 'ambra', 'log', 'stone')."""
        wx, wz, wh = self.wx, self.wz, self.wall_h
        for x in range(1, wx + 1):
            for z in range(1, wz + 1):
                self.put(x, 0, z, S(found) if self.is_wall(x, z) else S(floor))
        for fi, style in enumerate(styles):
            y0 = fi * (wh + 1) + 1
            for y in range(y0, y0 + wh):
                for x in range(1, wx + 1):
                    for z in range(1, wz + 1):
                        if not self.is_wall(x, z):
                            self.put(x, y, z, AIR)
                            continue
                        if self.is_corner(x, z):
                            self.put(x, y, z, S(corner, axis="y"))
                            continue
                        self.put(x, y, z, self.wall_block(style, x, y - y0, z, wh))
            if fi + 1 < len(styles):          # floor/beam row between storeys
                yb = y0 + wh
                for x in range(1, wx + 1):
                    for z in range(1, wz + 1):
                        if self.is_corner(x, z):
                            self.put(x, yb, z, S(corner, axis="y"))
                        elif self.is_wall(x, z):
                            self.put(x, yb, z, S(beam, axis="x" if z in (1, wz) else "z"))
                        else:
                            self.put(x, yb, z, S(floor))

    def wall_block(self, style, x, ly, z, wh):
        along = x if z in (1, self.wz) else z
        r = self.rng.random()
        if style == "timber":
            if ly == 0 or ly == wh - 1:
                return S("timber_frame_post")
            return S("timber_frame_cross") if along % 3 == 1 else S("elisar_plaster")
        if style == "brick":
            return S("elisar_bricks_mossy") if r < 0.12 else S("elisar_bricks")
        if style == "stone":
            return S("elisar_stone_mossy") if r < 0.15 else S("elisar_cobble") if ly == 0 else S("elisar_bricks")
        if style == "plaster":
            return S("timber_frame_post") if ly == 0 else S("elisar_plaster")
        if style in ("sizo", "ambra"):
            return S(style + "_planks")
        if style == "log":
            return S("sizo_log", axis="x" if z in (1, self.wz) else "z")
        raise ValueError(style)

    def window(self, x, y, z, h=1):
        axis_x = z in (1, self.wz)
        for yy in range(y, y + h):
            self.put(x, yy, z, S("elisar_window", east=axis_x, west=axis_x, north=not axis_x, south=not axis_x, waterlogged=False))

    def door(self, x, z=None, y=1):
        z = self.wz if z is None else z
        self.door_x = x
        for half, yy in (("lower", y), ("upper", y + 1)):
            self.put(x, yy, z, S("elisar_door", facing="north", half=half, hinge="left", open=False, powered=False))
        self.put(x, 0, z + 1, S("elisar_path"))

    def opening(self, x0, x1, y0, y1, z=None):
        z = self.wz if z is None else z
        self.fill(x0, y0, z, x1, y1, z, AIR)

    def roof(self, mat, axis="x", gable="sizo_planks", base=None):
        """Gable roof with 1-block overhang. axis = ridge direction."""
        top = self.top if base is None else base
        st, sl = mat + "_stairs", mat + "_slab"
        L = (self.wz + 2) if axis == "x" else (self.wx + 2)
        k = 0
        while True:
            a, b = k, L - 1 - k
            y = top + 1 + k
            if a > b:
                break
            span = range(0, self.wx + 2) if axis == "x" else range(0, self.wz + 2)
            for u in span:
                if a < b:
                    if axis == "x":
                        self.put(u, y, a, S(st, facing="south", half="bottom", shape="straight", waterlogged=False))
                        self.put(u, y, b, S(st, facing="north", half="bottom", shape="straight", waterlogged=False))
                    else:
                        self.put(a, y, u, S(st, facing="east", half="bottom", shape="straight", waterlogged=False))
                        self.put(b, y, u, S(st, facing="west", half="bottom", shape="straight", waterlogged=False))
                else:
                    if axis == "x":
                        self.put(u, y, a, S(sl, type="bottom", waterlogged=False))
                    else:
                        self.put(a, y, u, S(sl, type="bottom", waterlogged=False))
            # gable triangles + attic air
            for c in range(a + 1, b):
                if axis == "x":
                    for gx in (1, self.wx):
                        if 1 <= c <= self.wz:
                            self.put(gx, y, c, S(gable))
                    for ix in range(2, self.wx):
                        if 1 <= c <= self.wz:
                            self.put(ix, y, c, AIR)
                else:
                    for gz in (1, self.wz):
                        if 1 <= c <= self.wx:
                            self.put(c, y, gz, S(gable))
                    for iz in range(2, self.wz):
                        if 1 <= c <= self.wx:
                            self.put(c, y, iz, AIR)
            k += 1
        self.ridge_y = top + k

    def chimney(self, x, z, y0, mat="elisar_bricks"):
        for y in range(y0, getattr(self, "ridge_y", self.top + 4) + 2):
            self.put(x, y, z, S(mat), force=True)

    def stairs_up(self, x, z0, dirn, y0=1, rail_dx=None):
        """Straight staircase in column x rising 'wall_h' blocks along +z (dirn=1) or -z (dirn=-1)."""
        rise = self.wall_h
        f = "south" if dirn > 0 else "north"
        for i in range(rise):
            z = z0 + i * dirn
            y = y0 + i
            self.put(x, y, z, S("sizo_planks_stairs", facing=f, half="bottom", shape="straight", waterlogged=False))
            for yy in range(y0, y):
                self.put(x, yy, z, S("sizo_planks"))
            self.put(x, y0 + rise, z, AIR)          # opening in the floor above
            if rail_dx is not None:
                self.put(x + rail_dx, y0 + rise + 1, z, S("elisar_fence", north=i > 0 if dirn > 0 else True, south=True if dirn > 0 else i > 0,
                                                       east=False, west=False, waterlogged=False))

    def sign(self, kind, x, y=3):
        self.model(kind, x, y, self.wz + 1, "south")

    def lights(self, per=28):
        """Hang candle chandeliers in every storey: below the first solid block above the room, over a clear column."""
        if self.name in ("forge", "temple"):
            return 0
        placed = []
        wh = self.wall_h
        for fi in range(self.floors):
            y0 = fi * (wh + 1) + 1
            cands = []
            for x in range(2, self.wx):
                for z in range(2, self.wz):
                    yc = None
                    for y in range(y0, y0 + wh + 7):
                        b = self.B.get((x, y, z))
                        if (x, y, z) in self.MC or (b is not None and b != AIR):
                            yc = y
                            break
                    if yc is None or yc - 1 < y0 + 2 or (x, yc, z) in self.MC:
                        continue
                    if any(self.B.get((x, yy, z)) not in (None, AIR) for yy in range(y0, yc)):
                        continue
                    if any(self.B.get((x + dx, y0 + dy, z + dz), AIR)[0].endswith("stairs")
                           for dx in (-1, 0, 1) for dz in (-1, 0, 1) for dy in range(0, wh + 1)):
                        continue
                    cx, cz = (self.wx + 1) / 2, (self.wz + 1) / 2
                    cands.append((abs(x - cx) + abs(z - cz), x, yc - 1, z))
            want = max(1, round((self.wx - 2) * (self.wz - 2) / per))
            got = []
            for _, x, y, z in sorted(cands):
                if all(abs(x - a) + abs(z - c) >= 5 for a, _, c in got):
                    got.append((x, y, z))
                if len(got) >= want:
                    break
            for x, y, z in got:
                self.model("lantern_hanging", x, y, z, "south")
            placed += got
        return len(placed)

    def save(self):
        self.nlights = self.lights()
        H = max(p[1] for p in self.B) + 1
        n, pal = write_template(OUT + "/structures/city/%s.nbt" % self.name, (self.W, H, self.D), self.B, self.NBT)
        return {"name": self.name, "W": self.W, "H": H, "D": self.D, "door_x": self.door_x, "blocks": n, "lights": self.nlights}


def fix_fences(t):
    """Recompute fence connections inside a template (neighbour fence or full block)."""
    for (x, y, z), st in list(t.B.items()):
        if st[0] == "riftworld:elisar_fence":
            con = {}
            for d, dx, dz in (("north", 0, -1), ("south", 0, 1), ("west", -1, 0), ("east", 1, 0)):
                nb = t.B.get((x + dx, y, z + dz))
                con[d] = nb is not None and nb != AIR and ("fence" in nb[0] or not any(k in nb[0] for k in ("door", "part", "window", "stairs", "slab"))
                                                         and nb[0].split(":")[1] not in KINDS)
            t.B[(x, y, z)] = S("elisar_fence", waterlogged=False, **con)


TPL = {}


def T_house_a():
    t = Tpl("house_a", 7, 6)
    t.shell(["timber"])
    t.door(4)
    for x in (2, 6):
        t.window(x, 2, 6)
    t.window(1, 2, 3); t.window(1, 2, 4); t.window(7, 2, 3); t.window(4, 2, 1)
    t.model("bed_wood", 2, 1, 3, "south")
    t.model("table_wood", 4, 1, 2, "south")
    t.model("chair_wood", 4, 1, 3, "north")
    t.model("stool", 5, 1, 3, "north")
    t.model("wardrobe", 6, 1, 5, "west")
    t.model("rug", 3, 1, 5, "south")
    t.model("crate_stack", 6, 1, 2, "south")
    t.roof("thatch", "x", gable="timber_frame_post")
    return t


def T_house_b():
    t = Tpl("house_b", 8, 7, floors=2)
    t.shell(["brick", "timber"])
    t.door(4)
    for x in (2, 6):
        t.window(x, 2, 7); t.window(x, 7, 7)
    for z in (3, 5):
        t.window(8, 7, z)
    t.window(1, 7, 5); t.window(4, 2, 1); t.window(4, 7, 1); t.window(6, 7, 1)
    t.stairs_up(7, 2, 1, rail_dx=-1)
    t.model("fireplace", 2, 1, 4, "east")
    t.model("table_wood", 4, 1, 3, "south")
    t.model("chair_wood", 4, 1, 4, "north"); t.model("chair_wood", 5, 1, 4, "north"); t.model("chair_wood", 4, 1, 2, "south")
    t.model("wardrobe", 6, 1, 6, "north")
    t.model("crate_stack", 2, 1, 6, "south")
    t.model("bed_wood", 2, 6, 3, "south"); t.model("bed_wood", 4, 6, 3, "south")
    t.model("rug", 3, 6, 6, "south")
    t.model("wardrobe", 5, 6, 2, "south")
    t.roof("roof_red", "x", gable="timber_frame_post")
    t.chimney(1, 4, 5)
    return t


def T_house_c():
    t = Tpl("house_c", 9, 7, floors=2)
    t.shell(["ambra", "plaster"], floor="ambra_planks", corner="ambra_log", beam="ambra_log")
    t.door(6)
    for x in (4, 8):
        t.window(x, 2, 7)
    for x in (3, 5, 7):
        t.window(x, 7, 7); t.window(x, 7, 1)
    t.window(9, 2, 4); t.window(9, 7, 4); t.window(1, 7, 4)
    t.stairs_up(2, 2, 1, rail_dx=1)
    t.model("table_wood", 5, 1, 3, "south")
    t.model("chair_wood", 5, 1, 4, "north"); t.model("chair_wood", 6, 1, 4, "north")
    t.model("shop_shelf", 7, 1, 2, "south")
    t.model("cooking_pot", 8, 1, 5, "west")
    t.model("bed_wood", 5, 6, 3, "south"); t.model("bed_wood", 7, 6, 3, "south")
    t.model("wardrobe", 8, 6, 6, "north")
    t.model("rug", 5, 6, 6, "south")
    t.roof("roof_blue", "x", gable="ambra_planks")
    return t


def T_house_d():
    t = Tpl("house_d", 6, 7, floors=2)
    t.shell(["plaster", "timber"])
    t.door(3)
    t.window(5, 2, 7); t.window(2, 7, 7); t.window(4, 7, 7)
    t.window(1, 2, 3); t.window(1, 7, 3); t.window(1, 7, 5); t.window(6, 7, 3)
    t.stairs_up(5, 2, 1, rail_dx=-1)
    t.model("table_wood", 2, 1, 4, "south")
    t.model("chair_wood", 2, 1, 5, "north"); t.model("chair_wood", 3, 1, 5, "north")
    t.model("wardrobe", 2, 1, 2, "south")
    t.model("crate_stack", 3, 1, 2, "south")
    t.model("bed_wood", 2, 6, 3, "south"); t.model("bed_wood", 3, 6, 3, "south")
    t.model("rug", 2, 6, 6, "south")
    t.roof("roof_red", "z", gable="elisar_plaster")
    return t


def T_shop(name="shop", roof="roof_red", styles=("brick",), alt=False):
    t = Tpl(name, 8, 7, wall_h=5)
    t.shell(list(styles))
    t.door(4)
    for x in (2, 3, 6, 7):
        t.window(x, 2, 7, h=2)
    t.window(1, 3, 4); t.window(8, 3, 4)
    if not alt:
        t.model("shop_counter", 4, 1, 4, "south")
        t.model("stool", 4, 1, 3, "south")
        t.model("shop_shelf", 2, 1, 2, "south"); t.model("shop_shelf", 6, 1, 2, "south")
        t.model("crate_stack", 7, 1, 5, "west"); t.model("crate_stack", 2, 1, 5, "east")
        t.model("table_wood", 4, 1, 2, "south")
    else:
        t.model("shop_counter", 4, 1, 3, "south")
        t.model("shop_shelf", 2, 1, 4, "east"); t.model("shop_shelf", 7, 1, 3, "west")
        t.model("crate_stack", 2, 1, 6, "east"); t.model("crate_stack", 7, 1, 6, "west")
        t.model("fish_barrel", 2, 1, 2, "south")
        t.model("table_wood", 5, 1, 2, "south")
    t.sign("sign_shop", 6)
    t.roof(roof, "x", gable="sizo_planks")
    return t


def T_tavern():
    t = Tpl("tavern", 13, 9, floors=2)
    t.shell(["brick", "timber"])
    t.door(7)
    for x in (3, 5, 10, 11):
        t.window(x, 2, 9)
    for x in (3, 5, 7, 9, 11):
        t.window(x, 7, 9); t.window(x, 7, 1)
    t.window(13, 2, 4); t.window(13, 7, 4); t.window(13, 7, 7); t.window(1, 7, 3); t.window(1, 7, 7)
    t.stairs_up(12, 8, -1, rail_dx=-1)
    t.model("bar_counter", 9, 1, 4, "south")
    t.model("keg_rack", 9, 1, 2, "south")
    for x in (8, 9, 10):
        t.model("stool", x, 1, 5, "north")
    t.model("fireplace", 2, 1, 5, "east")
    t.model("table_wood", 3, 1, 3, "south")
    t.model("chair_wood", 3, 1, 4, "north"); t.model("chair_wood", 4, 1, 4, "north"); t.model("chair_wood", 3, 1, 2, "south")
    t.model("table_wood", 4, 1, 6, "south")
    t.model("chair_wood", 4, 1, 7, "north"); t.model("chair_wood", 5, 1, 7, "north"); t.model("stool", 6, 1, 6, "west")
    t.model("table_wood", 7, 1, 2, "south")
    t.model("bed_wood", 3, 6, 3, "south"); t.model("bed_wood", 5, 6, 3, "south"); t.model("bed_wood", 7, 6, 3, "south")
    t.model("wardrobe", 9, 6, 2, "south")
    t.model("rug", 4, 6, 6, "south")
    t.model("table_wood", 8, 6, 6, "south")
    t.model("chair_wood", 8, 6, 7, "north")
    t.sign("sign_tavern", 9)
    t.roof("roof_red", "x", gable="timber_frame_post")
    t.chimney(1, 5, 5)
    fix_fences(t)
    return t


def T_police():
    t = Tpl("police", 11, 9, floors=2)
    t.shell(["stone", "brick"], floor="elisar_cobble")
    t.door(5)
    t.window(3, 2, 9); t.window(8, 2, 9)
    for x in (3, 5, 7, 9):
        t.window(x, 7, 9); t.window(x, 7, 1)
    t.window(11, 2, 3); t.window(11, 7, 6); t.window(1, 7, 3)
    # jail cell (bars) in the back-right corner
    for y in range(1, 5):
        for x in (7, 8, 9, 10):
            t.put(x, y, 5, S("elisar_fence"))
        for z in (2, 3, 4):
            t.put(7, y, z, S("elisar_fence"))
    t.put(9, 1, 5, S("elisar_door", facing="south", half="lower", hinge="left", open=False, powered=False))
    t.put(9, 2, 5, S("elisar_door", facing="south", half="upper", hinge="left", open=False, powered=False))
    t.model("bed_wood", 10, 1, 3, "south")
    t.model("stool", 8, 1, 2, "south")
    t.stairs_up(2, 8, -1, rail_dx=1)
    t.model("table_wood", 3, 1, 4, "south")
    t.model("chair_wood", 3, 1, 3, "south")
    t.model("weapon_rack", 2, 1, 2, "south"); t.model("weapon_rack", 4, 1, 2, "south")
    t.model("wanted_wall", 6, 1, 2, "south")
    t.model("wanted_wall", 10, 1, 8, "west")
    t.model("crate_stack", 8, 1, 8, "north")
    t.model("bounty_board", 2, 1, 10, "south")
    t.model("table_wood", 5, 6, 3, "south")
    t.model("chair_wood", 5, 6, 4, "north")
    t.model("wardrobe", 9, 6, 2, "south")
    t.model("bed_wood", 8, 6, 7, "south"); t.model("bed_wood", 10, 6, 7, "south")
    t.model("weapon_rack", 7, 6, 2, "south")
    t.sign("sign_police", 7)
    t.roof("roof_blue", "x", gable="elisar_bricks")
    fix_fences(t)
    return t


def T_fisher():
    t = Tpl("fisher_house", 7, 6)
    t.shell(["sizo"])
    t.door(4)
    t.window(2, 2, 6); t.window(1, 2, 3); t.window(7, 2, 3); t.window(4, 2, 1)
    t.model("bed_wood", 2, 1, 3, "south")
    t.model("table_wood", 4, 1, 2, "south")
    t.model("chair_wood", 4, 1, 3, "north")
    t.model("cooking_pot", 6, 1, 4, "west")
    t.model("fish_barrel", 6, 1, 2, "south")
    t.model("fishing_net", 2, 1, 5, "east")
    t.model("fish_barrel", 1, 1, 7, "south"); t.model("fish_barrel", 2, 1, 7, "south")
    t.sign("sign_fish", 6)
    t.roof("thatch", "x", gable="sizo_planks")
    return t


def T_hunter():
    t = Tpl("hunter_house", 8, 7)
    t.shell(["log"], corner="ambra_log")
    t.door(4)
    t.window(2, 2, 7); t.window(8, 2, 3); t.window(5, 2, 1); t.window(1, 2, 6)
    t.model("trophy_antlers", 3, 1, 2, "south"); t.model("trophy_antlers", 6, 1, 2, "south")
    t.model("hide_rack", 7, 1, 4, "west")
    t.model("weapon_rack", 7, 1, 6, "west")
    t.model("bed_wood", 2, 1, 6, "south")
    t.model("fireplace", 2, 1, 3, "east")
    t.model("table_wood", 4, 1, 4, "south")
    t.model("stool", 4, 1, 5, "north")
    t.model("rug", 5, 1, 6, "south")
    t.model("hide_rack", 1, 1, 8, "south")
    t.sign("sign_hunter", 6)
    t.roof("thatch", "x", gable="sizo_log")
    t.chimney(1, 3, 5)
    return t


def T_forge():
    t = Tpl("forge", 9, 8)
    t.shell(["brick"], floor="elisar_cobble")
    t.opening(2, 8, 1, 4)                     # open front ...
    for x in (4, 7):                            # ... with log posts
        t.fill(x, 1, 8, x, 4, 8, S("sizo_log", axis="y"))
    for x in (2, 3, 5, 6, 8):
        t.put(x, 4, 8, S("sizo_log", axis="x"))
    t.put(5, 0, 9, S("elisar_path"))
    t.door_x = 5
    t.window(1, 2, 5); t.window(9, 2, 5)
    t.model("forge_hearth", 5, 1, 3, "south")
    t.model("anvil_stump", 5, 1, 5, "south")
    t.model("quench_barrel", 7, 1, 5, "south")
    t.model("tool_wall", 2, 1, 5, "east")
    t.model("crate_stack", 8, 1, 2, "south")
    t.model("weapon_rack", 2, 1, 2, "south")
    t.model("sign_forge", 7, 3, 9, "south")
    t.roof("roof_red", "x", gable="elisar_bricks")
    t.chimney(5, 2, 5)
    return t


def T_temple():
    t = Tpl("temple", 11, 15, wall_h=7)
    t.shell(["stone"], floor="elisar_bricks", corner="ambra_log", found="elisar_stone")
    t.opening(5, 7, 1, 4)
    t.put(6, 0, 16, S("elisar_path"))
    t.door_x = 6
    for z in (4, 7, 10, 13):
        t.window(1, 3, z, h=2); t.window(11, 3, z, h=2)
    t.model("idol", 6, 1, 3, "south")
    t.model("altar_stone", 5, 1, 5, "south")
    for x, z in ((3, 4), (9, 4), (3, 9), (9, 9)):
        t.model("fire_bowl", x, 1, z, "south")
    t.model("sacred_poles", 2, 1, 2, "south"); t.model("sacred_poles", 10, 1, 2, "south")
    for z in (8, 11, 14):
        t.model("rug", 5, 1, z, "south")
    t.model("sacred_poles", 3, 1, 16, "south"); t.model("sacred_poles", 9, 1, 16, "south")
    t.model("sign_temple", 8, 3, 16, "south")
    t.roof("roof_blue", "z", gable="elisar_bricks")
    return t


for fn in (T_house_a, T_house_b, T_house_c, T_house_d, T_tavern, T_police, T_fisher, T_hunter, T_forge, T_temple):
    tp = fn()
    TPL[tp.name] = tp
TPL["shop"] = T_shop()
TPL["shop2"] = T_shop("shop2", "roof_blue", ("timber",), alt=True)
os.makedirs(OUT + "/structures/city", exist_ok=True)
META = {}
for name, tp in TPL.items():
    META[name] = tp.save()
    print("tpl", name, META[name]["W"], "x", META[name]["D"], "h", META[name]["H"], "blocks", META[name]["blocks"], "lights", META[name]["lights"])


# =============================================================================================== plan
OCEAN = 62
rng = random.Random(20261005)


class Island:
    def __init__(self, idx, name, cx, cz, R, base, depth):
        self.idx, self.name, self.cx, self.cz, self.R, self.base, self.depth = idx, name, cx, cz, R, base, depth
        r = random.Random(idx * 7919 + 13)
        self.p1, self.p2, self.p3 = (r.uniform(0, 6.283) for _ in range(3))

    def shapeR(self, th):
        return self.R * (1 + 0.07 * math.sin(3 * th + self.p1) + 0.045 * math.sin(5 * th + self.p2) + 0.025 * math.sin(11 * th + self.p3))

    def t(self, x, z):
        dx, dz = x - self.cx, z - self.cz
        return 1 - math.hypot(dx, dz) / self.shapeR(math.atan2(dz, dx))

    def raw(self, x, z):
        h = self.base + 1.6 * math.sin(x * 0.043 + self.p1) * math.cos(z * 0.037 + self.p2) + 0.9 * math.sin((x - z) * 0.071 + self.p3)
        t = self.t(x, z)
        if t < 0.12:
            h -= ((1 - max(t, 0) / 0.12) ** 2) * 4
        return h

    def rim(self, th, inset=0.0):
        r = self.shapeR(th) - inset
        return self.cx + math.cos(th) * r, self.cz + math.sin(th) * r


def polar(ang_deg, d):
    a = math.radians(ang_deg)
    return round(math.cos(a) * d), round(math.sin(a) * d)


I0 = Island(0, "main", 0, 0, 90, 104, 44)
SAT = [("temple", -38, 56, 112, 28, 34), ("fishers", 14, 56, 99, 26, 30), ("homes", 84, 62, 106, 30, 30),
       ("forge", 162, 58, 108, 28, 32), ("farms", 236, 52, 103, 26, 30)]
ISL = [I0]
for k, (nm, ang, R, base, depth, gap) in enumerate(SAT):
    cx, cz = polar(ang, I0.R + R + gap)
    ISL.append(Island(k + 1, nm, cx, cz, R, base, depth))
BY = {i.name: i for i in ISL}


def island_at(x, z):
    for i in ISL:
        if i.t(x, z) > 0:
            return i
    return None


# occupancy grid ------------------------------------------------------------------------------
N0 = 420
SZ = 2 * N0
occ = bytearray(SZ * SZ)         # 0 free, 1 road, 2 plot, 3 prop, 4 water, 5 tree, 6 keep-clear
FREE, ROAD, PLOT, PROP, WATER, TREE, KEEP = range(7)


def oget(x, z):
    x, z = int(math.floor(x)) + N0, int(math.floor(z)) + N0
    if 0 <= x < SZ and 0 <= z < SZ:
        return occ[z * SZ + x]
    return KEEP


def oset(x, z, v):
    x, z = int(math.floor(x)) + N0, int(math.floor(z)) + N0
    if 0 <= x < SZ and 0 <= z < SZ:
        occ[z * SZ + x] = v


def seg_dist(px, pz, ax, az, bx, bz):
    vx, vz = bx - ax, bz - az
    L2 = vx * vx + vz * vz
    t = 0 if L2 == 0 else max(0, min(1, ((px - ax) * vx + (pz - az) * vz) / L2))
    return math.hypot(px - ax - vx * t, pz - az - vz * t), t


ROADS, PLAZAS, PLOTS, PROPS, BRIDGES, TREES, SPRINGS = [], [], [], [], [], [], []
RIVER = {}


def add_road(pts, w, kind, mark=True):
    """kind: 0 cobble street, 1 dirt path, 2 plank walkway."""
    for a, b in zip(pts, pts[1:]):
        ROADS.append({"a": [round(a[0], 2), round(a[1], 2)], "b": [round(b[0], 2), round(b[1], 2)], "w": w, "k": kind})
        if mark:
            x0, x1 = int(min(a[0], b[0]) - w), int(max(a[0], b[0]) + w) + 1
            z0, z1 = int(min(a[1], b[1]) - w), int(max(a[1], b[1]) + w) + 1
            for x in range(x0, x1):
                for z in range(z0, z1):
                    d, _ = seg_dist(x + 0.5, z + 0.5, a[0], a[1], b[0], b[1])
                    if d <= w / 2 + 0.3 and oget(x, z) in (FREE, KEEP, TREE):
                        oset(x, z, ROAD)


def add_plaza(x, z, r, kind=0):
    PLAZAS.append({"x": x, "z": z, "r": r, "k": kind})
    for xx in range(int(x - r) - 1, int(x + r) + 2):
        for zz in range(int(z - r) - 1, int(z + r) + 2):
            if math.hypot(xx + 0.5 - x, zz + 0.5 - z) <= r:
                oset(xx, zz, ROAD)


# --- the laboratory twin on the main island (template 63 x 70, ground floor at template y 30)
LAB_W, LAB_D = 63, 70
lab_x0, lab_z0 = -31, -35
lab_h = round(sum(math.floor(I0.raw(lab_x0 + i * 6, lab_z0 + j * 7)) for i in range(11) for j in range(11)) / 121)
PLOTS.append({"t": "laboratory", "x0": lab_x0, "z0": lab_z0, "w": LAB_W, "d": LAB_D, "f": "south", "h": lab_h, "yo": -30, "m": 6})
for x in range(lab_x0 - 3, lab_x0 + LAB_W + 3):
    for z in range(lab_z0 - 3, lab_z0 + LAB_D + 3):
        oset(x, z, PLOT)

# --- bridges (main -> every satellite, plus temple <-> fishers)
def bridge(ia, ib):
    th = math.atan2(ib.cz - ia.cz, ib.cx - ia.cx)
    ax, az = ia.rim(th, 5)
    bx, bz = ib.rim(th + math.pi, 5)
    ya, yb = math.floor(ia.raw(ax, az)), math.floor(ib.raw(bx, bz))
    L = math.hypot(bx - ax, bz - az)
    BRIDGES.append({"a": [round(ax, 2), round(az, 2)], "b": [round(bx, 2), round(bz, 2)], "ya": ya, "yb": yb, "sag": round(max(1.5, L / 14), 2), "hw": 1.6})
    for k in range(int(L) + 1):
        x, z = ax + (bx - ax) * k / L, az + (bz - az) * k / L
        for dx in range(-3, 4):
            for dz in range(-3, 4):
                if oget(x + dx, z + dz) == FREE:
                    oset(x + dx, z + dz, KEEP)
    return (ax, az), (bx, bz)


ENDS = {}
for i in ISL[1:]:
    a, b = bridge(I0, i)
    ENDS[i.name] = (a, b)
TF = bridge(BY["temple"], BY["fishers"])


# --- main island streets: rounded ring road around the lab + radial streets to the bridges
ring = []
RX, RZ = 44, 47
for k in range(41):
    a = 2 * math.pi * k / 40
    c, s = math.cos(a), math.sin(a)
    # superellipse ring (rounded rectangle)
    ring.append((RX * math.copysign(abs(c) ** 0.45, c), RZ * math.copysign(abs(s) ** 0.45, s)))
add_road(ring, 5, 0)


def radial_from_ring(end):
    th = math.atan2(end[1], end[0])
    best = min(ring, key=lambda p: abs(math.atan2(p[1], p[0]) - th) if abs(math.atan2(p[1], p[0]) - th) < math.pi else 9)
    add_road([best, end], 5, 0)
    return best


for nm in ("temple", "fishers", "homes", "forge", "farms"):
    radial_from_ring(ENDS[nm][0])
# market square (south-west quarter of the main island, between two radials)
mth = math.radians(120)
MARKET = (round(math.cos(mth) * 68, 1), round(math.sin(mth) * 68, 1))
add_plaza(MARKET[0], MARKET[1], 11, 0)
add_road([MARKET, (math.cos(mth) * 50, math.sin(mth) * 50)], 5, 0)
# side lanes on the main island
for ang in (30, 210, 300, 160):
    a = math.radians(ang)
    p0 = (math.cos(a) * 52, math.sin(a) * 52)
    p1 = (math.cos(a + 0.12) * 74, math.sin(a + 0.12) * 74)
    add_road([p0, p1], 3, 1)

# --- satellite islands: bridge end -> plaza at the centre -> lanes
for i in ISL[1:]:
    b = ENDS[i.name][1]
    c = (i.cx + rng.uniform(-4, 4), i.cz + rng.uniform(-4, 4))
    mid = ((b[0] + c[0]) / 2 + rng.uniform(-5, 5), (b[1] + c[1]) / 2 + rng.uniform(-5, 5))
    add_road([b, mid, c], 4 if i.name != "farms" else 3, 0 if i.name in ("forge", "homes") else 1)
    add_plaza(c[0], c[1], 7 if i.name != "temple" else 9, 0 if i.name in ("forge", "homes", "temple") else 1)
    i.centre = c
    th0 = math.atan2(c[1] - b[1], c[0] - b[0])
    for dth in (-1.9, 0.0, 1.9):
        th = th0 + dth + rng.uniform(-0.3, 0.3)
        e = i.rim(th, 12)
        m2 = ((c[0] + e[0]) / 2 + rng.uniform(-4, 4), (c[1] + e[1]) / 2 + rng.uniform(-4, 4))
        add_road([c, m2, e], 3, 1)
tf_a, tf_b = TF
add_road([tf_a, BY["temple"].centre], 3, 1)
add_road([tf_b, BY["fishers"].centre], 3, 1)

# --- the river on the "homes" island: pond -> meander -> waterfall over the far rim
H = BY["homes"]
far = math.atan2(H.cz, H.cx) + 0.5
pond = (H.cx + math.cos(far + 2.4) * H.R * 0.32, H.cz + math.sin(far + 2.4) * H.R * 0.32)
pts = [pond]
for k in range(1, 7):
    f = k / 6
    base = (pond[0] + (H.rim(far, -4)[0] - pond[0]) * f, pond[1] + (H.rim(far, -4)[1] - pond[1]) * f)
    nx, nz = -(H.rim(far)[1] - pond[1]), H.rim(far)[0] - pond[0]
    ln = math.hypot(nx, nz)
    off = math.sin(f * 7.0) * 5 * (1 - f * 0.6)
    pts.append((base[0] + nx / ln * off, base[1] + nz / ln * off))
riv_samples = []
for a, b in zip(pts, pts[1:]):
    L = math.hypot(b[0] - a[0], b[1] - a[1])
    for k in range(int(L) + 1):
        x, z = a[0] + (b[0] - a[0]) * k / L, a[1] + (b[1] - a[1]) * k / L
        if H.t(x, z) > 0.14:
            riv_samples.append(math.floor(H.raw(x, z)))
RL = min(riv_samples) - 1
RIVER = {"pts": [[round(p[0], 2), round(p[1], 2)] for p in pts], "hw": 2.2, "level": RL, "pond": [round(pond[0], 2), round(pond[1], 2)], "pr": 6.0}
for x in range(-N0, N0):
    pass
for a, b in zip(pts, pts[1:]):
    for x in range(int(min(a[0], b[0]) - 8), int(max(a[0], b[0]) + 9)):
        for z in range(int(min(a[1], b[1]) - 8), int(max(a[1], b[1]) + 9)):
            d, _ = seg_dist(x + 0.5, z + 0.5, a[0], a[1], b[0], b[1])
            if d <= RIVER["hw"] + 0.5:
                if oget(x, z) != ROAD:
                    oset(x, z, WATER)
            elif d <= RIVER["hw"] + 3 and oget(x, z) == FREE:
                oset(x, z, KEEP)
for x in range(int(pond[0] - 9), int(pond[0] + 10)):
    for z in range(int(pond[1] - 9), int(pond[1] + 10)):
        d = math.hypot(x + 0.5 - pond[0], z + 0.5 - pond[1])
        if d <= RIVER["pr"] and oget(x, z) != ROAD:
            oset(x, z, WATER)
        elif d <= RIVER["pr"] + 3 and oget(x, z) == FREE:
            oset(x, z, KEEP)

# --- small springs (waterfalls) on the rims
for i in ISL:
    for k in range(2 if i.idx else 3):
        for _ in range(40):
            th = rng.uniform(0, 2 * math.pi)
            sx, sz = i.rim(th, 2.5)
            ox, oz = i.rim(th, -1.6)
            if oget(sx, sz) == FREE and all(math.hypot(sx - s["s"][0], sz - s["s"][1]) > 30 for s in SPRINGS):
                if all(seg_dist(sx, sz, b["a"][0], b["a"][1], b["b"][0], b["b"][1])[0] > 14 for b in BRIDGES):
                    y = math.floor(i.raw(math.floor(sx) + 0.5, math.floor(sz) + 0.5)) - 1
                    SPRINGS.append({"s": [math.floor(sx), math.floor(sz)], "o": [math.floor(ox), math.floor(oz)], "y": y})
                    for dx in range(-3, 4):
                        for dz in range(-3, 4):
                            if oget(sx + dx, sz + dz) == FREE:
                                oset(sx + dx, sz + dz, KEEP)
                    break


# --- plots along the streets ------------------------------------------------------------------
def tpl_dims(name, f):
    m = META[name]
    return (m["W"], m["D"]) if f in ("south", "north") else (m["D"], m["W"])


def door_world(name, f, x0, z0):
    m = META[name]
    W, D, tx, tz = m["W"], m["D"], m["door_x"], m["D"] - 1
    if f == "south":
        return x0 + tx, z0 + tz
    if f == "north":
        return x0 + W - 1 - tx, z0 + D - 1 - tz
    if f == "west":
        return x0 + D - 1 - tz, z0 + tx
    return x0 + tz, z0 + W - 1 - tx


def rect_free(x0, z0, w, d, isl, margin=2):
    for x in range(x0 - margin, x0 + w + margin):
        for z in range(z0 - margin, z0 + d + margin):
            v = oget(x, z)
            if v in (ROAD, PLOT, WATER, PROP):
                return False
    for x in range(x0, x0 + w, 2):
        for z in range(z0, z0 + d, 2):
            if isl.t(x, z) < 0.16:
                return False
    for x, z in ((x0, z0), (x0 + w - 1, z0), (x0, z0 + d - 1), (x0 + w - 1, z0 + d - 1)):
        if isl.t(x, z) < 0.16:
            return False
    hs = [math.floor(isl.raw(x + 0.5, z + 0.5)) for x in range(x0, x0 + w, 2) for z in range(z0, z0 + d, 2)]
    return max(hs) - min(hs) <= 4


def place_building(name, isl, tries=900, near=None):
    segs = [r for r in ROADS if island_at(*r["a"]) is isl or island_at(*r["b"]) is isl]
    best = None
    for _ in range(tries):
        r = rng.choice(segs)
        u = rng.uniform(0.1, 0.9)
        px, pz = r["a"][0] + (r["b"][0] - r["a"][0]) * u, r["a"][1] + (r["b"][1] - r["a"][1]) * u
        vx, vz = r["b"][0] - r["a"][0], r["b"][1] - r["a"][1]
        ln = math.hypot(vx, vz) or 1
        side = rng.choice((-1, 1))
        nx, nz = -vz / ln * side, vx / ln * side
        # building faces the road: facing = cardinal closest to (-n)
        fx, fz = -nx, -nz
        f = ("east" if fx > 0 else "west") if abs(fx) > abs(fz) else ("south" if fz > 0 else "north")
        w, d = tpl_dims(name, f)
        depth = d if f in ("south", "north") else w
        off = r["w"] / 2 + 2.5 + depth / 2 + rng.uniform(0, 3)
        cx, cz = px + nx * off, pz + nz * off
        x0, z0 = int(round(cx - w / 2)), int(round(cz - d / 2))
        if not rect_free(x0, z0, w, d, isl):
            continue
        score = rng.random()
        if near is not None:
            score += math.hypot(cx - near[0], cz - near[1]) / 30
        if best is None or score < best[0]:
            best = (score, x0, z0, w, d, f, r)
            if near is None:
                break
    if best is None:
        print("  !! no room for", name, "on", isl.name)
        return None
    _, x0, z0, w, d, f, r = best
    hs = sorted(math.floor(isl.raw(x + 0.5, z + 0.5)) for x in range(x0, x0 + w) for z in range(z0, z0 + d))
    h = hs[len(hs) // 2]
    p = {"t": name, "x0": x0, "z0": z0, "w": w, "d": d, "f": f, "h": h, "yo": 0, "m": 4}
    PLOTS.append(p)
    for x in range(x0 - 1, x0 + w + 1):
        for z in range(z0 - 1, z0 + d + 1):
            oset(x, z, PLOT)
    # front path: door step -> straight out until it meets the street
    dx, dz = door_world(name, f, x0, z0)
    step = {"south": (0, 1), "north": (0, -1), "east": (1, 0), "west": (-1, 0)}[f]
    sx, sz = dx + step[0], dz + step[1]
    ex, ez = sx, sz
    for k in range(1, 14):
        ex, ez = sx + step[0] * k, sz + step[1] * k
        if oget(ex, ez) == ROAD:
            break
    add_road([(sx + 0.5, sz + 0.5), (ex + 0.5, ez + 0.5)], 2, 1)
    p["door"] = [dx, dz]
    return p


ORDER = {
    "main": ["police", "tavern", "shop", "shop2", "house_b", "house_c", "house_d", "house_b", "house_a", "house_d", "house_c", "house_a"],
    "temple": ["temple", "house_a", "house_d"],
    "fishers": ["fisher_house", "fisher_house", "fisher_house", "house_a", "shop2"],
    "homes": ["hunter_house", "house_b", "house_c", "house_a", "house_d", "house_c"],
    "forge": ["forge", "house_b", "shop", "house_a", "house_d"],
    "farms": ["house_a", "house_c", "house_d", "house_a", "house_b"],
}
NEAR = {"police": MARKET, "tavern": MARKET, "shop": MARKET}
for i in ISL:
    for name in ORDER[i.name]:
        near = NEAR.get(name) if i is I0 else getattr(i, "centre", None) if name in ("temple", "forge", "hunter_house") else None
        place_building(name, i, near=near)


# --- props ---------------------------------------------------------------------------------------
def prop_ok(kind, x, z, f, allow=(FREE, KEEP, TREE)):
    isl = island_at(x + 0.5, z + 0.5)
    if isl is None:
        return False
    for dx, _, dz in cells(kind, f):
        if dx == 0 and dz == 0 or True:
            v = oget(x + dx, z + dz)
            if v not in allow or (isl.t(x + dx + 0.5, z + dz + 0.5) < 0.06):
                return False
    return True


def add_prop(kind, x, z, f, allow=(FREE, KEEP, TREE), lit=None):
    x, z = int(math.floor(x)), int(math.floor(z))
    if not prop_ok(kind, x, z, f, allow):
        return False
    PROPS.append({"k": kind, "x": x, "z": z, "f": f, "lit": (kind in LIT) if lit is None else lit})
    for dx, _, dz in cells(kind, f):
        oset(x + dx, z + dz, PROP)
    for dx in range(-1, 2):
        for dz in range(-1, 2):
            if oget(x + dx, z + dz) == FREE:
                oset(x + dx, z + dz, KEEP)
    return True


def face_to(dx, dz):
    return ("east" if dx > 0 else "west") if abs(dx) > abs(dz) else ("south" if dz > 0 else "north")


# street lamps along streets (both kinds), alternating sides
for r in ROADS:
    if r["w"] < 3:
        continue
    ax, az = r["a"]; bx, bz = r["b"]
    L = math.hypot(bx - ax, bz - az)
    if L < 6:
        continue
    vx, vz = (bx - ax) / L, (bz - az) / L
    n = int(L // 15)
    for k in range(n + 1):
        u = (k + 0.5) / (n + 1)
        side = 1 if k % 2 == 0 else -1
        px, pz = ax + (bx - ax) * u - vz * side * (r["w"] / 2 + 1.2), az + (bz - az) * u + vx * side * (r["w"] / 2 + 1.2)
        if any(math.hypot(px - p["x"], pz - p["z"]) < 10 for p in PROPS if p["k"] == "street_lamp"):
            continue
        add_prop("street_lamp", px, pz, face_to(vz * side, -vx * side))
# lamps at both ends of every bridge
for b in BRIDGES:
    for (ex, ez), (ox, oz) in ((b["a"], b["b"]), (b["b"], b["a"])):
        L = math.hypot(ox - ex, oz - ez)
        vx, vz = (ox - ex) / L, (oz - ez) / L
        for side in (-1, 1):
            add_prop("street_lamp", ex - vx * 1.5 - vz * side * 3.2, ez - vz * 1.5 + vx * side * 3.2, face_to(vx, vz), allow=(FREE, KEEP, TREE, ROAD))
# market: stalls in a ring facing the centre, well in the middle, benches, crates and the market sign post
mx, mz = MARKET
add_prop("well", mx - 0.5, mz - 0.5, "south", allow=(ROAD,))
for k, ang in enumerate(range(0, 360, 45)):
    a = math.radians(ang + 10)
    x, z = mx + math.cos(a) * 8.0, mz + math.sin(a) * 8.0
    kind = "market_stall_fish" if k % 3 == 1 else "market_stall"
    if not add_prop(kind, x, z, face_to(mx - x, mz - z), allow=(ROAD, FREE, KEEP)):
        add_prop("crate_stack", x, z, "south", allow=(ROAD, FREE, KEEP))
for ang in (30, 140, 250):
    a = math.radians(ang)
    add_prop("bench", mx + math.cos(a) * 4.5, mz + math.sin(a) * 4.5, face_to(math.cos(a), math.sin(a)), allow=(ROAD,))
PROPS.append({"k": "signpost", "sign": "sign_market", "x": int(mx + math.cos(mth + math.pi) * 12.5), "z": int(mz + math.sin(mth + math.pi) * 12.5), "f": face_to(-math.cos(mth), -math.sin(mth)), "lit": False})
# temple sanctuary: idols in a ring around the temple-island plaza, fire bowls between them
tc = BY["temple"].centre
for k in range(6):
    a = 2 * math.pi * k / 6 + 0.3
    add_prop("idol", tc[0] + math.cos(a) * 7.5, tc[1] + math.sin(a) * 7.5, face_to(-math.cos(a), -math.sin(a)), allow=(ROAD, FREE, KEEP))
    a2 = a + math.pi / 6
    add_prop("fire_bowl", tc[0] + math.cos(a2) * 6.5, tc[1] + math.sin(a2) * 6.5, "south", allow=(ROAD, FREE, KEEP))
add_prop("altar_stone", tc[0] - 1, tc[1], "south", allow=(ROAD,))
for k in range(5):
    a = rng.uniform(0, 6.28)
    add_prop("sacred_poles", tc[0] + math.cos(a) * 12, tc[1] + math.sin(a) * 12, "south")


def near_plot(kind, p, n=1, dist=(2, 7)):
    placed = 0
    for _ in range(80):
        if placed >= n:
            break
        side = rng.choice(("w", "e", "n", "s"))
        if side in ("w", "e"):
            x = p["x0"] - rng.randint(*dist) if side == "w" else p["x0"] + p["w"] + rng.randint(*dist) - 1
            z = rng.randint(p["z0"], p["z0"] + p["d"] - 1)
        else:
            z = p["z0"] - rng.randint(*dist) if side == "n" else p["z0"] + p["d"] + rng.randint(*dist) - 1
            x = rng.randint(p["x0"], p["x0"] + p["w"] - 1)
        if add_prop(kind, x, z, rng.choice(("south", "north", "east", "west"))):
            placed += 1


for p in PLOTS:
    t = p["t"]
    if t == "fisher_house":
        near_plot("fish_rack", p); near_plot("rowboat", p); near_plot("fish_barrel", p, 2)
    elif t == "hunter_house":
        near_plot("hide_rack", p); near_plot("crate_stack", p)
    elif t == "forge":
        near_plot("crate_stack", p, 2); near_plot("quench_barrel", p)
    elif t in ("shop", "shop2", "tavern"):
        near_plot("crate_stack", p, 1, (1, 3)); near_plot("bench", p, 1, (1, 3))
    elif t.startswith("house"):
        if rng.random() < 0.5:
            near_plot("bench", p, 1, (1, 3))
        if rng.random() < 0.35:
            near_plot("cooking_pot", p, 1, (2, 5))
# fish stalls on the fishers' plaza
fc = BY["fishers"].centre
for ang in (20, 160, 260):
    a = math.radians(ang)
    add_prop("market_stall_fish", fc[0] + math.cos(a) * 9, fc[1] + math.sin(a) * 9, face_to(-math.cos(a), -math.sin(a)), allow=(ROAD, FREE, KEEP))
# abandoned overgrown wagons: on grass verges next to streets
wag = 0
for _ in range(400):
    if wag >= 7:
        break
    r = rng.choice([r for r in ROADS if r["w"] >= 3])
    u = rng.uniform(0.15, 0.85)
    ax, az = r["a"]; bx, bz = r["b"]
    L = math.hypot(bx - ax, bz - az) or 1
    side = rng.choice((-1, 1))
    px = ax + (bx - ax) * u - (bz - az) / L * side * (r["w"] / 2 + 3.5)
    pz = az + (bz - az) * u + (bx - ax) / L * side * (r["w"] / 2 + 3.5)
    f = face_to((bx - ax), (bz - az)) if rng.random() < 0.5 else face_to(-(bx - ax), -(bz - az))
    if add_prop("wagon_overgrown", px, pz, f):
        wag += 1
# benches + wells on satellite plazas
for i in ISL[1:]:
    c = i.centre
    if i.name in ("farms", "homes", "forge"):
        add_prop("well", c[0] + 3, c[1] - 0.5, "south", allow=(ROAD, FREE, KEEP))
    for ang in (60, 200):
        a = math.radians(ang + i.idx * 30)
        add_prop("bench", c[0] + math.cos(a) * 5.5, c[1] + math.sin(a) * 5.5, face_to(math.cos(a), math.sin(a)), allow=(ROAD, FREE, KEEP))

# --- trees --------------------------------------------------------------------------------------
DENS = {"main": 0.35, "temple": 0.85, "fishers": 0.45, "homes": 0.6, "forge": 0.5, "farms": 0.4}
for i in ISL:
    R = int(i.R * 1.2)
    for gx in range(int(i.cx - R), int(i.cx + R), 6):
        for gz in range(int(i.cz - R), int(i.cz + R), 6):
            x, z = gx + rng.randint(0, 5), gz + rng.randint(0, 5)
            t = i.t(x + 0.5, z + 0.5)
            if t < 0.07:
                continue
            dens = DENS[i.name] * (1.4 if t < 0.3 else 1.0)
            if rng.random() > dens:
                continue
            ok = all(oget(x + dx, z + dz) in (FREE, TREE) for dx in range(-3, 4) for dz in range(-3, 4))
            if not ok:
                continue
            kind = 0 if rng.random() < (0.65 if i.name in ("temple", "homes") else 0.45) else 1
            h = rng.randint(8, 13) if kind == 0 else rng.randint(5, 8)
            TREES.append([x, z, kind, h, rng.randint(0, 1 << 30)])
            for dx in range(-1, 2):
                for dz in range(-1, 2):
                    oset(x + dx, z + dz, TREE)

plan = {"ocean": OCEAN,
        "islands": [{"cx": i.cx, "cz": i.cz, "R": i.R, "base": i.base, "depth": i.depth, "p": [i.p1, i.p2, i.p3], "name": i.name} for i in ISL],
        "roads": ROADS, "plazas": PLAZAS, "plots": PLOTS, "props": PROPS, "bridges": BRIDGES, "river": RIVER, "springs": SPRINGS,
        "trees": TREES, "templates": {k: [v["W"], v["H"], v["D"]] for k, v in META.items()},
        "spawn": [int(MARKET[0]), int(MARKET[1])]}
os.makedirs(OUT + "/city", exist_ok=True)
json.dump(plan, open(OUT + "/city/plan.json", "w"), separators=(",", ":"))
print("islands", [(i.name, i.cx, i.cz, i.R) for i in ISL])
print("plots", len(PLOTS), "props", len(PROPS), "roads", len(ROADS), "bridges", len(BRIDGES), "trees", len(TREES), "springs", len(SPRINGS), "riverL", RL)
from collections import Counter
print(Counter(p["t"] for p in PLOTS)); print(Counter(p["k"] for p in PROPS))

# --- debug map ------------------------------------------------------------------------------------
if "--map" in sys.argv:
    from PIL import Image
    S2 = 2
    img = Image.new("RGB", (SZ, SZ), (40, 70, 120))
    px = img.load()
    COL = {FREE: (70, 140, 110), ROAD: (150, 150, 150), PLOT: (190, 120, 70), PROP: (240, 230, 80), WATER: (60, 160, 230), TREE: (20, 90, 60), KEEP: (90, 160, 120)}
    for z in range(SZ):
        for x in range(SZ):
            if island_at(x - N0 + 0.5, z - N0 + 0.5):
                px[x, z] = COL[occ[z * SZ + x]]
    for b in BRIDGES:
        ax, az = b["a"]; bx, bz = b["b"]
        L = int(math.hypot(bx - ax, bz - az))
        for k in range(L + 1):
            x, z = ax + (bx - ax) * k / L, az + (bz - az) * k / L
            px[int(x) + N0, int(z) + N0] = (160, 110, 60)
    for s in SPRINGS:
        px[s["o"][0] + N0, s["o"][1] + N0] = (255, 255, 255)
    img.save("/home/user/.cache/prev/city_map.png")
