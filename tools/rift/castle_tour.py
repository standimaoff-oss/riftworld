#!/usr/bin/env python3
"""Rev 3 / C-1 (session 6): stills + video of the castle «Чертог Тлеющего Рога» on its temporary site in Elisar.
usage: castle_tour.py stills|video|light|dark
Template (tx,ty,tz) -> world (X0+tx, Y+ty, Z0+tz) with X0=2, Y=104, Z0=436 (CastleSite.java)."""
import json, math, os, subprocess, sys, time
sys.path.insert(0, os.path.dirname(__file__))
from director import Rcon  # noqa: E402

X0, Y0, Z0 = 2, 104, 436
SHOTS = "/home/user/release/riftworld_screens/session6"
VID = "/home/user/.cache/vid"
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
os.makedirs(SHOTS, exist_ok=True); os.makedirs(VID, exist_ok=True)
META = json.load(open("/home/user/mods/riftworld/src/main/resources/data/riftworld/structures/castle/castle_meta.json"))
R = Rcon()
HZ = 20


def Wp(tx, ty, tz):
    return (X0 + tx + 0.5, Y0 + ty, Z0 + tz + 0.5)


def tp(p, yaw, pitch):
    R("execute in riftworld:elisar run tp TestPlayer %.3f %.3f %.3f %.2f %.2f" % (p[0], p[1], p[2], yaw, pitch))


def look(frm, to):
    dx, dy, dz = to[0] - frm[0], to[1] - frm[1], to[2] - frm[2]
    return math.degrees(math.atan2(-dx, dz)), -math.degrees(math.atan2(dy, math.hypot(dx, dz)))


def ease(t):
    return t * t * (3 - 2 * t)


def lerp3(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def aim(p, t):
    yaw, pitch = look(p, t)
    tp(p, yaw, pitch)


def snap(name):
    subprocess.run(["bash", "/home/user/tools/snap.sh", os.path.join(SHOTS, name + ".jpg")])
    print("shot", name, flush=True)


def candles(on):
    for (x, y, z) in META["candles"]:
        R("execute in riftworld:elisar run setblock %d %d %d riftworld:candle_stand[facing=north,powered=%s]" % (X0 + x, Y0 + y, Z0 + z, "true" if on else "false"))


# name: (camera, target, warm seconds)
STILLS = [
    ("01_front_from_city", (40, 126, 394), (40, 116, 468), 35),
    ("02_ne_oblique", (104, 134, 418), (40, 114, 472), 22),
    ("03_ruined_east_side", (108, 118, 490), (62, 112, 474), 18),
    ("04_back_rose_keep", (2, 134, 546), (40, 118, 480), 22),
    ("05_gatehouse", (40, 112, 416), (40, 112, 440), 10),
    ("06_hall_from_entrance_dark", Wp(38, 2, 17), Wp(38, 5, 46), 12),
    ("07_hall_candles_lit", Wp(38, 2, 17), Wp(38, 5, 46), 6),
    ("08_hall_from_gallery", Wp(38, 11.6, 17), Wp(38, 3, 44), 8),
    ("09_side_gallery_jump_point", Wp(27.5, 12.2, 36), Wp(46, 9, 26), 8),
    ("10_throne", Wp(38, 4.6, 38), Wp(38, 6, 47), 8),
    ("11_collapsed_roof", Wp(40, 3, 30), Wp(47, 26, 40), 8),
    ("12_sockets_candles", Wp(28, 6, 24), Wp(38, 1, 32), 8),
    ("13_site_dead_trees", (-22, 114, 430), (20, 110, 470), 20),
]


def stills(names=None):
    for name, cam, tgt, warm in STILLS:
        if names and name not in names:
            continue
        if name == "06_hall_from_entrance_dark":
            candles(False)
        if name == "07_hall_candles_lit":
            candles(True)
        aim(cam, tgt)
        time.sleep(warm)
        aim(cam, tgt)
        time.sleep(1.5)
        snap(name)


def move(p0, p1, t0, t1, secs):
    n = max(2, int(secs * HZ))
    start = time.time()
    for i in range(n + 1):
        u = ease(i / n)
        p = lerp3(p0, p1, u)
        aim(p, lerp3(t0, t1, u))
        dt = start + (i + 1) / HZ - time.time()
        if dt > 0:
            time.sleep(dt)


def record(name, cap, secs, fn, warm_pose, warm=14):
    aim(*warm_pose)
    time.sleep(warm)
    raw = "%s/s6_%s_raw.mp4" % (VID, name)
    ff = subprocess.Popen(["ffmpeg", "-loglevel", "error", "-y", "-f", "x11grab", "-framerate", "20", "-video_size", "800x480",
                           "-i", ":99.0+0,60", "-t", str(secs + 3), "-c:v", "libx264", "-preset", "veryfast", "-crf", "21", "-pix_fmt", "yuv420p", raw])
    time.sleep(0.6)
    t0 = time.time()
    fn()
    dur = time.time() - t0
    time.sleep(0.3)
    subprocess.run(["pkill", "-INT", "-f", "[f]fmpeg -loglevel error -y -f x11grab"])
    ff.wait()
    tf = "%s/s6_%s.txt" % (VID, name)
    open(tf, "w", encoding="utf-8").write(cap)
    vf = ("drawtext=fontfile=%s:textfile=%s:fontcolor=white:fontsize=18:borderw=2:bordercolor=black@0.8:x=20:y=h-38:"
          "alpha='if(lt(t,0.4),t/0.4,1)'" % (FONT, tf))
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-ss", "0.6", "-i", raw, "-t", "%.2f" % (dur + 0.2), "-vf", vf,
                    "-c:v", "libx264", "-preset", "veryfast", "-crf", "21", "-pix_fmt", "yuv420p", "-r", "20", "%s/s6_%s.mp4" % (VID, name)])
    print("seg", name, "%.1fs" % dur, flush=True)


def lightup():
    """Candles light one by one from the entrance to the throne (preview of the boss entry scene lighting)."""
    candles(False)
    time.sleep(1.0)
    for (x, y, z) in META["candles"]:
        R("execute in riftworld:elisar run setblock %d %d %d riftworld:candle_stand[facing=north,powered=true]" % (X0 + x, Y0 + y, Z0 + z))
        time.sleep(0.45)
    time.sleep(1.5)


def video():
    record("approach", "«Чертог Тлеющего Рога»: красный кирпич, серый камень, редкая латунь", 10,
           lambda: move((40, 134, 372), (40, 116, 414), (40, 118, 470), (40, 113, 450), 10), ((40, 134, 372), (40, 118, 470)), warm=25)
    record("orbit", "Асимметрия: башни разной формы, часть стен обрушена", 12,
           lambda: move((104, 134, 418), (106, 120, 512), (40, 114, 472), (52, 112, 476), 12), ((104, 134, 418), (40, 114, 472)), warm=25)
    record("hall", "Главный зал: 2 этажа, балконы, 6 гнёзд под колонны, кресло босса", 11,
           lambda: move(Wp(38, 2, 15), Wp(38, 3, 33), Wp(38, 6, 46), Wp(38, 5, 46), 11), (Wp(38, 2, 15), Wp(38, 6, 46)), warm=10)
    record("candles", "Свечи загораются одна за другой до кресла (свет для сцены входа)", 12,
           lightup, (Wp(38, 2, 17), Wp(38, 5, 46)), warm=4)
    record("gallery", "Галереи 2-го этажа, точки прыжка стражей, обрушенная кровля", 10,
           lambda: move(Wp(27, 11.6, 20), Wp(27, 11.6, 40), Wp(44, 4, 30), Wp(48, 20, 42), 10), (Wp(27, 11.6, 20), Wp(44, 4, 30)), warm=8)
    parts = ["approach", "orbit", "hall", "candles", "gallery"]
    lst = "%s/s6_list.txt" % VID
    open(lst, "w").write("".join("file '%s/s6_%s.mp4'\n" % (VID, p) for p in parts))
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-f", "concat", "-safe", "0", "-i", lst, "-c", "copy", "/home/user/release/riftworld_session6.mp4"])
    print("VIDEO DONE", flush=True)


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "stills"
    if cmd == "stills":
        stills(sys.argv[2:] or None)
    elif cmd == "video":
        video()
    elif cmd == "light":
        candles(True)
    elif cmd == "dark":
        candles(False)
    print("TOUR DONE", flush=True)
