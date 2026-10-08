#!/usr/bin/env python3
"""Camera director for the riftworld video: smooth spectator camera paths over RCON + scripted clicks.

usage: director.py <scene> [...]   (scenes listed in SCENES; 'all' runs the full film)

Rev 5: the laboratory is gone, the film is about the Great Temple and its rift. `reset` places
`structures/city/temple_great.nbt` (63 x 71 x 70, ritual floor at template y = 30) with its origin at
(0, -91, 0), so template (31, 30, 34) - the rift altar - lands on world (31, -61, 34); W() maps the
rest. The camera paths below are a first pass in template space and are meant to be tuned in game.
"""
import math, os, socket, struct, subprocess, sys, time

OY = -91   # template origin of `place template riftworld:city/temple_great 0 -91 0` (ground = template y 30)
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
    R("kill @e[type=item]")
    R("fill -40 -95 -40 110 -20 110 air")
    R("place template riftworld:city/temple_great 0 -91 0")
    R("riftrift close")


# ------------------------------------------------------------------------------------------------ scenes
def s_exterior():
    R("time set 1000")
    move([(31.5, 44, 76), (31.5, 36, 62), (31.5, 32, 50)], [(31, 34, 40), (31, 32, 36), (31, 31, 34)], 9)
    move([(-8, 42, 68), (12, 48, 30), (44, 54, 6)], [(31, 36, 35), (31, 36, 31), (31, 36, 28)], 10)
    move([(44, 54, 6), (44, 46, 44), (31.5, 40, 62)], [(31, 36, 28), (31, 34, 34), (31, 33, 36)], 8)


def s_hall():
    """The ritual hall: the hexagram on the floor, the altar, the pillars."""
    R("time set 1000")
    move([(31.5, 33, 54), (31.5, 33, 46), (31.5, 32, 39)], [(31, 31, 34), (31, 30.5, 34), (31, 30.5, 34)], 8)
    move([(31.5, 32, 39), (24, 33, 30), (31, 33, 24), (38, 33, 30), (38, 33, 39)],
         [(31, 30.5, 34)] * 5, 12)                                     # round the sign
    move([(38, 33, 39), (34, 30.5, 44), (26, 30.5, 44)], [(31, 31, 30), (31, 31, 30), (31, 31, 30)], 6)
    move([(26, 30.5, 44), (24, 31, 38), (26, 31, 27)], [(31, 31, 34)] * 3, 6)   # the 4 pillars
    move([(26, 31, 27), (31.5, 30.5, 22), (31.5, 30.5, 20)], [(31, 30.5, 34), (31, 30.5, 34), (31, 30, 34)], 5)


def s_candles():
    """Light the eight candles of the ritual (flint and steel, right click)."""
    R("time set 1000")
    R("gamemode creative @a")
    R("give @a minecraft:flint_and_steel 1")
    R("effect give @a minecraft:night_vision 600 0 true")
    pairs = (((27, 30, 30), (26, 30, 30)), ((35, 30, 30), (36, 30, 30)),
             ((27, 30, 38), (26, 30, 38)), ((35, 30, 38), (36, 30, 38)),
             ((31, 30, 29), (31, 30, 28)), ((31, 30, 39), (31, 30, 40)),
             ((26, 30, 34), (25, 30, 34)), ((36, 30, 34), (37, 30, 34)))
    for (cand, stand) in pairs:
        use_block(stand, (cand[0] + 0.5, cand[1] + 0.6, cand[2] + 0.5))
    R("effect clear @a")


def s_ritual():
    """Stand inside the sign: the altar charges, space breaks, the rift opens."""
    R("time set 18000")
    hold((31.5, 30, 40), (31.5, 31.5, 34), 4)
    R("gamemode survival @a")
    hold((31.5, 30, 38), (31.5, 31.5, 34), 12)      # in the sign box -> CHARGE -> BREAK -> OPEN
    move([(31.5, 30, 38), (27, 30.5, 30), (27, 33, 27)], [(31.5, 31.5, 34), (31, 32, 34), (31, 33, 34)], 8)
    hold((27, 33, 27), (31, 33, 34), 4)


def s_rift():
    """The rift itself: a black hole that swallows the light - and the step through it."""
    R("time set 18000")
    R("riftrift status")
    move([(31.5, 33, 44), (31.5, 33.5, 39)], [(31, 33.5, 34), (31, 33.5, 34)], 5)
    hold((31.5, 33.5, 39), (31, 33.5, 34), 3)
    move([(31.5, 33.5, 39), (33.5, 32, 36.5), (31.5, 33.5, 34.2)], [(31, 33.5, 34)] * 3, 6)   # into the rift
    R("gamemode spectator @a")
    hold((31.5, 36, 42), (31, 33.5, 34), 3)          # ... and back, from the other side


SCENES = {"exterior": s_exterior, "hall": s_hall, "candles": s_candles, "ritual": s_ritual, "rift": s_rift}

if __name__ == "__main__":
    setup()
    for a in sys.argv[1:]:
        if a == "reset":
            reset()
        elif a == "focus":
            focus()
        elif a == "all":
            reset()
            for f in ("exterior", "hall", "candles", "ritual", "rift"):
                SCENES[f]()
        else:
            SCENES[a]()
