#!/usr/bin/env python3
"""Camera director for the riftworld video: smooth spectator camera paths over RCON + scripted clicks.

usage: director.py <scene> [...]   (scenes listed in SCENES; 'all' runs the full film)
World origin of the test lab: template (0,0,0) -> world (0,-63,0).
"""
import math, os, socket, struct, subprocess, sys, time

OY = -63
HZ = 20


class Rcon:
    def __init__(self, host="127.0.0.1", port=25575, pw="bm21test"):
        for _ in range(5):
            try:
                self.s = socket.create_connection((host, port), timeout=5)
                self._send(3, pw)
                self._read()
                return
            except Exception:
                time.sleep(1)
        raise SystemExit("rcon unreachable")

    def _send(self, kind, body):
        data = struct.pack("<ii", 7, kind) + body.encode() + b"\0\0"
        self.s.sendall(struct.pack("<i", len(data)) + data)

    def _read(self):
        n = struct.unpack("<i", self._x(4))[0]
        d = self._x(n)
        return d[8:-2].decode("utf-8", "replace")

    def _x(self, n):
        b = b""
        while len(b) < n:
            c = self.s.recv(n - len(b))
            if not c:
                raise ConnectionError
            b += c
        return b

    def __call__(self, cmd):
        self._send(2, cmd)
        return self._read()


R = Rcon()


def W(x, y, z):
    """template -> world"""
    return x, y + OY, z


def look(frm, to):
    dx, dy, dz = to[0] - frm[0], to[1] - frm[1], to[2] - frm[2]
    yaw = math.degrees(math.atan2(-dx, dz))
    pitch = -math.degrees(math.atan2(dy, math.hypot(dx, dz)))
    return yaw, pitch


def tp(p, yaw, pitch):
    R("tp @a %.3f %.3f %.3f %.2f %.2f" % (p[0], p[1], p[2], yaw, pitch))


def ease(t):
    return t * t * (3 - 2 * t)


def lerp(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def bez(pts, t):
    while len(pts) > 1:
        pts = [lerp(pts[i], pts[i + 1], t) for i in range(len(pts) - 1)]
    return pts[0]


def move(points, targets, secs, smooth=True):
    """Camera along a bezier through template-space points, looking along a bezier of targets."""
    pts = [W(*p) for p in points]
    tgs = [W(*p) for p in targets]
    n = max(2, int(secs * HZ))
    t0 = time.time()
    for i in range(n + 1):
        t = i / n
        if smooth:
            t = ease(t)
        p = bez(pts, t)
        g = bez(tgs, t)
        yaw, pitch = look(p, g)
        tp(p, yaw, pitch)
        dt = t0 + (i + 1) / HZ - time.time()
        if dt > 0:
            time.sleep(dt)


def hold(point, target, secs):
    move([point, point], [target, target], secs)


def key(k):
    subprocess.run(["xdotool", "keydown", k], env=dict(os.environ, DISPLAY=":99"))
    time.sleep(0.15)
    subprocess.run(["xdotool", "keyup", k], env=dict(os.environ, DISPLAY=":99"))


def rclick():
    subprocess.run(["xdotool", "click", "3"], env=dict(os.environ, DISPLAY=":99"))


def focus():
    R("gamemode spectator @a")
    tp(W(31, 60, 31), 0, -90)
    time.sleep(0.5)
    subprocess.run(["xdotool", "mousemove", "400", "300", "click", "1"], env=dict(os.environ, DISPLAY=":99"))
    time.sleep(0.3)


def use_block(stand, target):
    """Creative player standing at `stand` right-clicks the block centre `target` (template coords)."""
    R("gamemode creative @a")
    s = W(*stand)
    yaw, pitch = look((s[0], s[1] + 1.62, s[2]), W(*target))
    tp(s, yaw, pitch)
    time.sleep(0.6)
    rclick()
    time.sleep(0.3)


def setup():
    for c in ("gamerule sendCommandFeedback false", "gamerule doDaylightCycle false", "gamerule doMobSpawning false",
              "gamerule commandBlockOutput false", "gamerule logAdminCommands false", "weather clear", "kill @e[type=item]"):
        R(c)


def reset():
    R("place template riftworld:laboratory 0 -63 0")
    R("kill @e[type=item]")


# ------------------------------------------------------------------------------------------------ scenes
def s_exterior():
    R("time set 1000")
    move([(31, 52, 110), (31, 46, 92), (31, 40, 78)], [(31, 34, 50), (31, 34, 50), (31, 33, 62)], 9)
    move([(-12, 50, 75), (-14, 54, 30), (2, 58, -12)], [(31, 36, 35), (31, 36, 31), (31, 36, 28)], 10)


def s_hall():
    move([(31, 44, 4), (31, 46, 31), (31, 41, 52)], [(31, 31, 30), (31, 30, 31), (31, 31, 30)], 8)
    move([(43, 40, 44), (44, 39, 31), (43, 40, 19)], [(31, 31, 31), (31, 31, 31), (31, 31, 31)], 6)
    # siren + chandeliers on the gallery
    hold((37.5, 34.6, 41.5), (37.5, 35.4, 44.5), 2.5)


def s_decor():
    R("effect give @a minecraft:night_vision 600 0 true")
    # lobby: desks, notes, extinguishers, chandeliers
    move([(31.5, 33.6, 52.5), (28.5, 33, 56.5), (22.5, 33, 57.5)], [(26.5, 32, 61), (24.5, 32.5, 61), (20.5, 32.2, 61)], 7)
    # north labs: flipped table, fallen shelf, fallen chair
    move([(26.5, 33.5, 11), (24.5, 33, 8.5), (19.5, 33, 9.5)], [(31.5, 31.5, 4), (25, 31.5, 4), (20.5, 31.3, 4.5)], 6)
    # basement warehouse: fallen shelf, barrels, boxes, trash bin
    move([(15.5, 27.5, 36.5), (18.5, 27.3, 38), (20.5, 27, 39)], [(20.5, 25.5, 32), (19, 25.4, 33), (10.5, 25.5, 40.5)], 6)
    move([(46.5, 27.3, 41.5), (45.5, 27.3, 45.5)], [(44.5, 25.4, 46.5), (52.5, 25.4, 40.5)], 4)
    # 2nd floor security room + offices
    move([(31.5, 41.5, 52), (30, 41.3, 56), (33, 41.4, 57)], [(33.5, 39.6, 60.5), (37.5, 40.5, 60.5), (28.5, 39.4, 57.5)], 6)
    move([(15.5, 41.3, 10), (16.5, 41.2, 7.5)], [(15.5, 39.8, 6.5), (17.5, 42, 5.5)], 4)
    R("effect clear @a")


def s_note():
    R("time set 18000")
    R("clear @a")
    hold((31.5, 34.0, 36.5), (31.5, 33, 39.5), 1.5)
    use_block((30.5, 32, 40.5), (31.5, 32.4, 39.5))
    R("gamemode spectator @a")
    move([(31.5, 34.0, 36.5), (33.5, 33.6, 37.0)], [(31.5, 33.5, 39.5), (31.5, 32.5, 39.5)], 4.5)


def s_power():
    R("time set 18000")
    R("data merge block 31 -31 41 {Stones:15,Stage:0}")
    R("clear @a")
    R("give @a riftworld:resonance_stone 1")
    hold((31.5, 34.2, 47.5), (31.5, 33, 41.5), 2)
    use_block((30.5, 32, 40.5), (31.5, 32.6, 41.5))
    R("gamemode spectator @a")
    move([(31.5, 34.0, 45.0), (31.5, 41, 50)], [(31.5, 33, 41.5), (31.5, 33, 26)], 4)
    hold((31.5, 41, 50), (31.5, 33, 26), 2)
    move([(31.5, 41, 50), (20, 41, 44)], [(31.5, 33, 26), (31, 32, 31)], 3)
    # basement flicker
    move([(20.5, 27.3, 50.5), (30.5, 27.3, 50.5)], [(20.5, 28.5, 40.5), (35.5, 28.5, 40.5)], 5)


def s_launch():
    use_block((30.5, 32, 40.5), (31.5, 32.4, 39.5))
    R("gamemode spectator @a")
    move([(31.5, 34.0, 41.0), (34, 41.5, 46)], [(31.5, 33, 39.5), (31.5, 30, 31.5)], 2.5)
    hold((34, 41.5, 46), (31.5, 30, 31.5), 1.5)                   # hatch opening
    move([(34, 41.5, 46), (40, 40, 40), (39, 40, 22)], [(31.5, 28, 31.5), (31.5, 30, 31.5), (31.5, 31, 31.5)], 6)
    move([(39, 40, 22), (33, 37, 18)], [(31.5, 32, 31.5), (31.5, 34, 31.5)], 4)   # capsule arrives (rise ends ~13 s)
    # cleaning wave: hall, then basement, then 2nd floor
    move([(33, 37, 18), (22, 36, 20)], [(31.5, 32, 31.5), (40, 31, 40)], 3)
    move([(20.5, 27.4, 46.5), (25.5, 27.4, 50.5)], [(30, 25.5, 40), (40, 25.5, 45)], 3)
    move([(15.5, 41.3, 14), (24.5, 41.3, 14)], [(30.5, 39, 14), (40.5, 39, 14)], 3)


def s_capsule():
    R("clear @a")
    move([(31.5, 36, 52), (31.5, 33.2, 41.0), (31.5, 32.7, 38.6)], [(31.5, 33, 35.5), (31.5, 33, 35), (31.5, 32.6, 35)], 4)
    R("gamemode creative @a")
    for i in range(25):
        z = 38.6 - i * 0.06
        tp(W(31.5, 31, z), 180, 5)
        time.sleep(0.05)
    time.sleep(1.2)
    for i in range(45):
        z = 37.1 - i * 0.12
        tp(W(31.5, 31, z), 180, 8)
        time.sleep(0.05)
    use_block((31.5, 31, 31.5), (31.5, 31.8, 29.5))
    time.sleep(3)
    R("gamemode spectator @a")
    move([(31.5, 33.8, 31.5), (33.5, 34.5, 34.5)], [(31.5, 33.0, 29.0), (30.5, 32.5, 28)], 4)
    move([(31.5, 36, 44), (31.5, 44, 50)], [(31.5, 34, 31.5), (31.5, 35, 31.5)], 5)


SCENES = {"note": s_note, "exterior": s_exterior, "hall": s_hall, "decor": s_decor, "power": s_power, "launch": s_launch, "capsule": s_capsule}

if __name__ == "__main__":
    setup()
    for a in sys.argv[1:]:
        if a == "reset":
            reset()
        elif a == "focus":
            focus()
        elif a == "all":
            for f in ("exterior", "hall", "decor", "note", "power", "launch", "capsule"):
                SCENES[f]()
        else:
            SCENES[a]()
