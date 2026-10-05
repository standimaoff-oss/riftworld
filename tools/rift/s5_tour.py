#!/usr/bin/env python3
"""Session-5 video segments: smooth spectator camera moves in Elisar over RCON, recorded with ffmpeg x11grab.
usage: s5_tour.py [segment ...]   -> /home/user/.cache/vid/s5_<name>.mp4 (captioned)"""
import math, os, subprocess, sys, time
sys.path.insert(0, os.path.dirname(__file__))
from director import Rcon  # noqa: E402

OUT = "/home/user/.cache/vid"
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
os.makedirs(OUT, exist_ok=True)
R = Rcon()
HZ = 20


def tp(p, yaw, pitch):
    R("execute in riftworld:elisar run tp TestPlayer %.3f %.3f %.3f %.2f %.2f" % (p[0], p[1], p[2], yaw, pitch))


def look(frm, to):
    dx, dy, dz = to[0] - frm[0], to[1] - frm[1], to[2] - frm[2]
    return math.degrees(math.atan2(-dx, dz)), -math.degrees(math.atan2(dy, math.hypot(dx, dz)))


def ease(t):
    return t * t * (3 - 2 * t)


def lerp3(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def move(p0, p1, t0, t1, secs):
    """camera p0->p1 while looking from target t0 to t1"""
    n = max(2, int(secs * HZ))
    start = time.time()
    for i in range(n + 1):
        u = ease(i / n)
        p = lerp3(p0, p1, u)
        yaw, pitch = look(p, lerp3(t0, t1, u))
        tp(p, yaw, pitch)
        dt = start + (i + 1) / HZ - time.time()
        if dt > 0:
            time.sleep(dt)


def orbit(c, r, y, a0, a1, target, secs):
    n = max(2, int(secs * HZ))
    start = time.time()
    for i in range(n + 1):
        a = math.radians(a0 + (a1 - a0) * ease(i / n))
        p = (c[0] + math.cos(a) * r, y, c[1] + math.sin(a) * r)
        yaw, pitch = look(p, target)
        tp(p, yaw, pitch)
        dt = start + (i + 1) / HZ - time.time()
        if dt > 0:
            time.sleep(dt)


SEG = {
    "village": ("Деревня: светлый грунт, мох, заборы у края террас, фонари", (6, 121, 140), (24, 113, 168), (20, 104, 175), (30, 104, 200), 8),
    "fields": ("Пшеничные поля, бочки, ящики, фонари на цепях под карнизами", (55, 112, -40), (66, 109, -52), (75, 103, -62), (78, 103, -70), 7),
    "bridge": ("Мосты: поперечный настил, двойные цепи, столбы с фонарями по краям", (92, 105, 15), (112, 104, 22), (124, 99, 31), (150, 100, 36), 8),
    "under": ("Корка 1 блок, сразу серый камень; низ островов и обломки", (128, 86, -16), (118, 84, 14), (95, 92, 0), (95, 92, 25), 8),
    "eye": ("Глаз в небе: моргает, следит за игроком, ночью спит", (-20, 120, 60), (-22, 121, 62), (40, 160, 6), (40, 160, 6), 11),
}


def record(name):
    cap = SEG[name][0] if name in SEG else {"tower": "Перевёрнутая башня над разломом", "night": "Ночью глаз закрыт",
                                             "wild": "Дикие острова: большие, средние, малые и островки"}[name]
    # warm up the first pose
    if name in SEG:
        _, p0, p1, t0, t1, secs = SEG[name]
        yaw, pitch = look(p0, t0)
        tp(p0, yaw, pitch)
    elif name == "tower":
        secs = 10
        tp((-104 + 34, 186, 22), 90, 0)
    elif name == "night":
        secs = 6
        R("time set 18000")
        tp((-20, 120, 60), -131, -20)
    elif name == "wild":
        secs = 9
        tp((330, 140, -60), 90, 10)
    time.sleep(14)
    raw = "%s/s5_%s_raw.mp4" % (OUT, name)
    ff = subprocess.Popen(["ffmpeg", "-loglevel", "error", "-y", "-f", "x11grab", "-framerate", "20", "-video_size", "800x480",
                           "-i", ":99.0+0,60", "-t", str(secs + 3), "-c:v", "libx264", "-preset", "veryfast", "-crf", "21", "-pix_fmt", "yuv420p", raw])
    time.sleep(0.6)
    t0 = time.time()
    if name in SEG:
        _, p0, p1, ta, tb, secs = SEG[name]
        if name == "eye":
            time.sleep(secs)
        else:
            move(p0, p1, ta, tb, secs)
    elif name == "tower":
        orbit((-104, 22), 34, 186, 0, 110, (-104, 182, 22), secs)
    elif name == "night":
        time.sleep(secs)
        R("time set 6000")
    elif name == "wild":
        move((330, 140, -60), (380, 128, -20), (420, 110, -60), (430, 105, 20), secs)
    dur = time.time() - t0
    time.sleep(0.3)
    subprocess.run(["pkill", "-INT", "-f", "[f]fmpeg -loglevel error -y -f x11grab"])
    ff.wait()
    tf = "%s/s5_%s.txt" % (OUT, name)
    open(tf, "w", encoding="utf-8").write(cap)
    vf = ("drawtext=fontfile=%s:textfile=%s:fontcolor=white:fontsize=20:borderw=2:bordercolor=black@0.8:x=20:y=h-40:"
          "alpha='if(lt(t,0.4),t/0.4,1)'" % (FONT, tf))
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-ss", "0.6", "-i", raw, "-t", "%.2f" % (dur + 0.2), "-vf", vf,
                    "-c:v", "libx264", "-preset", "veryfast", "-crf", "21", "-pix_fmt", "yuv420p", "-r", "20", "%s/s5_%s.mp4" % (OUT, name)])
    print("seg", name, "%.1fs" % dur, flush=True)


if __name__ == "__main__":
    for s in sys.argv[1:]:
        record(s)
    print("TOUR DONE", flush=True)
