# City «Странники» video tour: per segment warm-up (chunk meshes), then a scripted spectator camera + x11grab.
# usage: python3 tools/rift/city_tour.py [seg ...]   -> .cache/vid/<seg>.mp4, then concat -> release/riftworld_part3.mp4
import math, os, subprocess, sys, time

sys.path.insert(0, os.path.dirname(__file__))
import director as D  # noqa: E402  (Rcon, look, ease, bez)

R, HZ = D.R, 20
OUT = "/home/user/.cache/vid"
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
os.makedirs(OUT, exist_ok=True)


def tp(p, yaw, pitch):
    global R
    cmd = "execute in riftworld:elisar run tp @a %.3f %.3f %.3f %.2f %.2f" % (p[0], p[1], p[2], yaw, pitch)
    for _ in range(3):
        try:
            R(cmd)
            return
        except OSError:
            time.sleep(1.0)
            R = D.Rcon()


def path(points, targets, secs):
    n = max(2, int(secs * HZ))
    t0 = time.time()
    for i in range(n + 1):
        t = D.ease(i / n)
        p, g = D.bez(points, t), D.bez(targets, t)
        yaw, pitch = D.look(p, g)
        tp(p, yaw, pitch)
        dt = t0 + (i + 1) / HZ - time.time()
        if dt > 0:
            time.sleep(dt)


def pan(p, yaw0, yaw1, pitch, secs):
    n = max(2, int(secs * HZ))
    t0 = time.time()
    for i in range(n + 1):
        t = D.ease(i / n)
        tp(p, yaw0 + (yaw1 - yaw0) * t, pitch)
        dt = t0 + (i + 1) / HZ - time.time()
        if dt > 0:
            time.sleep(dt)


def bridge_pts(a, b, ya, yb, sag, n=5, eye=1.62):
    pts = []
    for k in range(n):
        u = k / (n - 1)
        x, z = a[0] + (b[0] - a[0]) * u, a[1] + (b[1] - a[1]) * u
        y = ya + (yb - ya) * u - sag * 4 * u * (1 - u)
        pts.append((x, math.floor(y) + 1 + eye, z))
    return pts


BA, BB = (-73.9, 24.2), (-114.14, 37.38)
BR = bridge_pts(BA, BB, 101, 107, 3.02)
# name: (time, warm points, caption, action)
SEGS = {
    "approach": (6000, [((-88, 134, 116), (-24, 102, 36)), ((-108, 146, 136), (-10, 96, 10))], "Элисар · город «Странники»",
                 lambda: path([(-108, 146, 136), (-88, 134, 116), (-62, 124, 96)], [(-10, 96, 10), (-24, 102, 36), (-34, 104, 59)], 10)),
    "market": (6000, [(-48, 112, 80)], "Рынок: прилавки, колодец, указатель",
               lambda: path([(-62, 124, 96), (-50, 112, 82), (-44, 108, 66), (-34, 108, 50)], [(-34, 104, 59), (-34, 103, 59), (-30, 103, 56), (-14, 105, 52)], 10)),
    "police": (6000, [(-22, 108, 46)], "Стража: доска розыска с наградами",
               lambda: path([(-30, 108, 44), (-20, 107, 46), (-15, 106.5, 47.5)], [(-12, 105, 52), (-14, 104.5, 52), (-16, 104, 53)], 8)),
    "tavern": (6000, [(-54, 104, 38)], "Трактир: стойка, бочки, камин, люстры",
               lambda: pan((-53.5, 104.0, 38.5), -150, 10, 12, 9)),
    "bridge": (6000, [BR[2]], "Подвесной мост к острову кузнецов",
               lambda: path(BR, [(p[0] + (BB[0] - BA[0]) * 0.4, p[1] - 0.8, p[2] + (BB[1] - BA[1]) * 0.4) for p in BR], 10)),
    "forge": (6000, [(-180, 117, 43)], "Кузница: горн, наковальня, бочка для закалки",
              lambda: path([(-176, 119, 40), (-184, 115, 47), (-189, 111.5, 53)], [(-189, 109, 60), (-189, 109, 61), (-189, 108.5, 62)], 7)),
    "fishers": (6000, [(140, 104, 28)], "Остров рыбаков: бочки с рыбой Элисара",
                lambda: path([(150, 108, 20), (143, 104, 27), (137, 103, 31)], [(137, 100, 39), (137, 100, 39), (137, 100, 40)], 7)),
    "temple": (6000, [(154, 128, -141)], "Языческий храм: идолы и огненные чаши",
               lambda: path([(146, 133, -147), (156, 127, -140), (162, 118, -132)], [(141, 112, -113), (150, 113, -118), (162, 114, -116)], 9)),
    "falls": (6000, [(-25, 82, 280)], "Водопады в зеркальный океан",
              lambda: path([(-45, 90, 305), (-30, 80, 285), (-18, 76, 268)], [(-10, 100, 246), (-10, 96, 246), (-12, 80, 250)], 9)),
    "dusk": (13400, [(-48, 110, 76)], "Сумерки: фонари загораются",
             lambda: path([(-56, 112, 84), (-46, 109, 74), (-38, 107.5, 68)], [(-34, 104, 59), (-34, 104, 59), (-26, 104, 54)], 9)),
}
ORDER = ["approach", "market", "police", "tavern", "bridge", "forge", "fishers", "temple", "falls", "dusk"]


def record(name):
    tod, warm, cap, act = SEGS[name]
    tp(warm[0][0] if len(warm[0]) == 2 else warm[0], 0, 0)
    R("time set %d" % tod)
    for w in warm:
        if len(w) == 2:          # (point, look-at) -> chunk meshes are only built for sections in view
            yaw, pitch = D.look(w[0], w[1])
            for k in range(6):
                tp(w[0], yaw + (k - 2.5) * 12, pitch)
                time.sleep(10)
        else:
            tp(w, 0, 20)
            time.sleep(40)
    # start pose = first pose of the action: run the action once silently for 0.3 s is overkill -> just start recording
    raw = "%s/%s_raw.mp4" % (OUT, name)
    ff = subprocess.Popen(["ffmpeg", "-loglevel", "error", "-y", "-f", "x11grab", "-framerate", "15", "-video_size", "800x480",
                           "-i", ":99.0+0,60", "-t", "14", "-c:v", "libx264", "-preset", "veryfast", "-crf", "22", "-pix_fmt", "yuv420p", raw])
    time.sleep(0.6)
    t0 = time.time()
    act()
    dur = time.time() - t0
    time.sleep(0.3)
    subprocess.run(["pkill", "-INT", "-f", "[f]fmpeg -loglevel error -y -f x11grab"])
    ff.wait()
    # trim the first 0.6 s (static) and add the caption
    tf = "%s/%s.txt" % (OUT, name)
    open(tf, "w", encoding="utf-8").write(cap)
    vf = ("drawtext=fontfile=%s:textfile=%s:fontcolor=white:fontsize=22:borderw=2:bordercolor=black@0.8:x=20:y=h-44:"
          "alpha='if(lt(t,0.4),t/0.4,1)'" % (FONT, tf))
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-ss", "0.6", "-i", raw, "-t", "%.2f" % (dur + 0.2), "-vf", vf,
                    "-c:v", "libx264", "-preset", "veryfast", "-crf", "22", "-pix_fmt", "yuv420p", "%s/%s.mp4" % (OUT, name)])
    print("seg", name, "%.1fs" % dur, flush=True)


if __name__ == "__main__":
    for s in (sys.argv[1:] or ORDER):
        record(s)
    print("TOUR DONE", flush=True)
