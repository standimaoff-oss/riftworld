#!/usr/bin/env python3
"""Rev 3 / C-2 (session 7): stills + short video of the furnished castle rooms.
usage: rooms_tour.py stills [names] | video"""
import os, subprocess, sys, time
sys.path.insert(0, os.path.dirname(__file__))
import castle_tour as ct  # noqa: E402

ct.SHOTS = "/home/user/release/riftworld_screens/session7"
os.makedirs(ct.SHOTS, exist_ok=True)
Wp, aim, move, R = ct.Wp, ct.aim, ct.move, ct.R

# name: (camera, target, warm seconds)
STILLS = [
    ("01_armoury", Wp(21, 1, 26), Wp(8, 1.5, 15), 30),
    ("02_refectory", Wp(21, 1.2, 44), Wp(10, 0.5, 32), 10),
    ("03_kitchen", Wp(21, 1.2, 57), Wp(7, 1.5, 51), 8),
    ("04_barracks", Wp(12, 7.2, 18.5), Wp(21, 7, 13), 8),
    ("05_bedchamber", Wp(19, 7.4, 35.5), Wp(8, 7.6, 24), 8),
    ("06_alchemy", Wp(12, 7.4, 50), Wp(6, 7.8, 43), 8),
    ("07_storage_loft", Wp(13, 7.4, 53), Wp(22, 7, 59), 8),
    ("08_prison", Wp(8, 1.2, 59), Wp(3, 1, 64), 10),
    ("09_library", Wp(67, 1.2, 13), Wp(55, 1, 23), 20),
    ("10_chapel", Wp(61.5, 3, 30), Wp(61.5, 2, 46), 8),
    ("11_chapel_gallery", Wp(54, 7.2, 30), Wp(63, 1.5, 44), 6),
    ("12_treasury", Wp(61.5, 1.2, 49), Wp(55, 0, 58), 8),
    ("13_keep", Wp(66, 1.2, 55), Wp(71, 1, 59.5), 8),
    ("14_scriptorium", Wp(55, 7.4, 26.5), Wp(65, 7, 15), 8),
    ("15_guest_room", Wp(54, 7.4, 49), Wp(59, 7.5, 58), 8),
    ("16_storeroom", Wp(36, 1.2, 58), Wp(26, 1, 53), 30),
    ("17_wine_cellar", Wp(39.5, 1.2, 56), Wp(48, 1, 54), 6),
    ("18_trophy_gallery", Wp(49, 7.4, 53), Wp(31, 7.5, 57), 8),
    ("19_gatehouse_guards", Wp(41, 8.4, 6), Wp(34, 8, 10), 14),
    ("20_watch_tower", Wp(8, 1.2, 13), Wp(4, 1, 8), 12),
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


SHOTS_V = [  # name, caption, [(p0, p1, t0, t1, secs), ...], warm
    ("armoury", "Оружейная стражи: стойки с глефами, доспехи, наковальня", [(Wp(21, 1, 26), Wp(20, 1.2, 14), Wp(8, 1.5, 18), Wp(8, 1.5, 22), 6)], 25),
    ("refectory", "Трапезная: пиршественные столы, кресла с высокими спинками", [(Wp(21, 1.2, 43), Wp(21, 1.2, 31), Wp(10, 0.5, 32), Wp(10, 0.5, 42), 6)], 6),
    ("kitchen", "Кухня: очаг с котлом, камин, разделочные столы, травы", [(Wp(21, 1.2, 57), Wp(18, 1.5, 47), Wp(7, 1.5, 51), Wp(8, 1.5, 55), 6)], 6),
    ("bedchamber", "Спальня лорда: кровать с балдахином, камин, гобелены", [(Wp(19, 7.4, 35.5), Wp(16, 7.6, 27), Wp(8, 7.6, 24), Wp(22, 7.8, 29), 6)], 8),
    ("alchemy", "Алхимическая: колбы, перегонный куб, котёл, книги", [(Wp(12, 7.4, 50), Wp(18, 7.4, 44), Wp(6, 7.8, 43), Wp(6, 7.8, 47), 6)], 6),
    ("library", "Библиотека: книжные стены, аналои с цепями", [(Wp(67, 1.2, 13), Wp(63, 1.2, 25), Wp(55, 1, 23), Wp(55, 1, 14), 6)], 18),
    ("chapel", "Часовня: скамьи, алтарь, хоры вдоль стены зала", [(Wp(61.5, 3, 30), Wp(61.5, 2.6, 40), Wp(61.5, 2, 46), Wp(61.5, 2.5, 46), 6)], 6),
    ("treasury", "Сокровищница: золото, окованные сундуки с добычей", [(Wp(61.5, 1.2, 49), Wp(56, 1.2, 51), Wp(55, 0, 58), Wp(58, 0, 60), 6)], 6),
    ("prison", "Темница в северо-западной башне: решётки, кандалы, кости",
     [(Wp(12, 1.2, 54), Wp(9, 1.2, 58), Wp(5, 1, 61), Wp(4, 1, 63), 4), (Wp(9, 1.2, 58), Wp(7, 1.2, 59.5), Wp(4, 1, 63), Wp(3, 1, 64), 3)], 10),
]


def video():
    parts = []
    for name, cap, moves, warm in SHOTS_V:
        secs = sum(m[4] for m in moves)
        ct.record("r_" + name, cap, secs, lambda moves=moves: [move(*m) for m in moves], (moves[0][0], moves[0][2]), warm=warm)
        parts.append("r_" + name)
    lst = "%s/s7_list.txt" % ct.VID
    open(lst, "w").write("".join("file '%s/s6_%s.mp4'\n" % (ct.VID, p) for p in parts))
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-f", "concat", "-safe", "0", "-i", lst, "-c", "copy",
                    "/home/user/release/riftworld_session7.mp4"])
    print("VIDEO DONE", flush=True)


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "stills"
    if cmd == "stills":
        stills(sys.argv[2:] or None)
    elif cmd == "video":
        video()
    print("TOUR DONE", flush=True)
