#!/usr/bin/env python3
"""Rev 3 / C-2b (session 8): spiral stairs + furnished tower storeys.
usage: towers_tour.py stills [names] | video"""
import json, os, subprocess, sys, time
sys.path.insert(0, os.path.dirname(__file__))
import castle_tour as ct  # noqa: E402

ct.SHOTS = "/home/user/release/riftworld_screens/session8"
os.makedirs(ct.SHOTS, exist_ok=True)
Wp, aim, move = ct.Wp, ct.aim, ct.move
STEPS = json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "tower_steps.json")))

STILLS = [
    ("01_nw_stair_ground", Wp(7, 1.4, 60), Wp(2.5, 4, 57.5), 10),
    ("02_nw_jailers_room", Wp(8, 9.3, 64), Wp(4, 9, 60), 10),
    ("03_nw_torture_chamber", Wp(8, 17.3, 58), Wp(3, 17, 62), 10),
    ("04_nw_watch_room", Wp(8, 25.3, 63), Wp(3, 25, 59), 10),
    ("05_sw_stair_ground", Wp(4, 1.2, 12), Wp(8, 4, 9), 12),
    ("06_sw_store", Wp(7, 9.3, 12), Wp(4, 9, 9), 8),
    ("07_sw_archers_room", Wp(7, 17.3, 8), Wp(4, 17, 11), 8),
    ("08_sw_lookout", Wp(5, 27.5, 10), Wp(38, 12, 36), 10),
    ("09_keep_store", Wp(70, 1.2, 55), Wp(65, 3, 59), 25),
    ("10_keep_guards_hall", Wp(70, 9.2, 59), Wp(66, 9, 55), 8),
    ("11_keep_armoury", Wp(70, 17.3, 60), Wp(66, 17, 55), 8),
    ("12_keep_commander", Wp(70, 25.3, 55), Wp(67, 25, 58), 8),
    ("13_keep_loft", Wp(70, 33.3, 59), Wp(66, 33, 55), 8),
    ("14_keep_upper_hall", Wp(70, 41.3, 55), Wp(66, 41, 59), 8),
    ("15_keep_roof", Wp(64, 50, 61.5), Wp(30, 32, 30), 12),
]


def stills(names=None):
    for name, cam, tgt, warm in STILLS:
        if names and name not in names:
            continue
        aim(cam, tgt)
        time.sleep(warm)
        aim(cam, tgt)
        time.sleep(1.5)
        ct.snap(name)


CENTRE = {"NW": (5, 61), "SW": (5, 10), "KEEP": (68, 57)}


def climb(key, a, b, per=0.55):
    st = STEPS[key][a:b + 1]
    pts = [Wp(x, y + 1.05, z) for (x, y, z) in st]

    def tgt(i):
        j = min(i + 3, len(pts) - 1)
        x, y, z = pts[j]
        if j == i:  # end of flight: look ahead along the last direction
            px, py, pz = pts[i - 1]
            x, z = x + (x - px) * 3, z + (z - pz) * 3
        c = Wp(CENTRE[key][0], 0, CENTRE[key][1])
        return (0.45 * x + 0.55 * c[0], y + 1.6, 0.45 * z + 0.55 * c[2])
    return [(pts[i], pts[i + 1], tgt(i), tgt(i + 1), per) for i in range(len(pts) - 1)]


SHOTS_V = [  # name, caption, moves, warm
    ("nw_climb", "Северо-западная башня: винтовая лестница через все этажи", climb("NW", 0, 23), 30),
    ("nw_torture", "Пыточная: клетка, кандалы, жаровня с котлом",
     [(Wp(8, 17.3, 58), Wp(7, 17.3, 63), Wp(3, 17, 62), Wp(3, 17, 59), 5)], 8),
    ("sw_climb", "Юго-западная башня: склад, комната лучников, смотровая площадка", climb("SW", 0, 20), 14),
    ("sw_lookout", "Смотровая площадка разрушенной башни",
     [(Wp(5, 26, 10), Wp(5, 28, 10), Wp(38, 12, 30), Wp(30, 12, 50), 5)], 4),
    ("keep_climb", "Донжон: кладовая и зал стражи", climb("KEEP", 0, 16), 25),
    ("keep_commander", "Покои коменданта: кровать, стол, кресло, аналой",
     [(Wp(70, 25.3, 55), Wp(70, 25.3, 59), Wp(66, 25, 58), Wp(66, 25, 55), 5)], 8),
    ("keep_roof", "Крыша донжона: вид на замок и Адские угодья",
     [(Wp(66, 48, 60), Wp(64, 50, 61.5), Wp(30, 32, 22), Wp(26, 32, 44), 6)], 12),
]


def video():
    parts = []
    for name, cap, moves, warm in SHOTS_V:
        secs = sum(m[4] for m in moves)
        ct.record("t_" + name, cap, secs, lambda moves=moves: [move(*m) for m in moves], (moves[0][0], moves[0][2]), warm=warm)
        parts.append("t_" + name)
    lst = "%s/s8_list.txt" % ct.VID
    open(lst, "w").write("".join("file '%s/s6_%s.mp4'\n" % (ct.VID, p) for p in parts))
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-f", "concat", "-safe", "0", "-i", lst,
                    "-c:v", "libx264", "-preset", "slow", "-crf", "27", "-pix_fmt", "yuv420p",
                    "/home/user/release/riftworld_session8.mp4"])
    print("VIDEO DONE", flush=True)


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "stills"
    if cmd == "stills":
        stills(sys.argv[2:] or None)
    elif cmd == "video":
        video()
    print("TOUR DONE", flush=True)
