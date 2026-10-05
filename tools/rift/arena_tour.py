#!/usr/bin/env python3
"""Rev 3 / C-3 (session 9): throne-hall sequence recorded in SURVIVAL first person (the mechanics only react to real players):
entry -> candles light one by one -> approach the throne -> awakening, columns rise and beam into the throne ->
crystal hits / pickaxe / wrong hit -> shattering -> all columns down -> reset (spectator view: columns sink).
usage: arena_tour.py run"""
import math, os, subprocess, sys, time
sys.path.insert(0, os.path.dirname(__file__))
import castle_tour as ct  # noqa: E402

ct.SHOTS = "/home/user/release/riftworld_screens/session9"
os.makedirs(ct.SHOTS, exist_ok=True)
Wp, R = ct.Wp, ct.R
EYE = 1.62
COLS = [(31, 23), (31, 30), (31, 37), (45, 23), (45, 30), (45, 37)]
PARTS = []


def E(cmd):
    return R("execute in riftworld:elisar run " + cmd)


def aim(feet, tgt):
    yaw, pitch = ct.look((feet[0], feet[1] + EYE, feet[2]), tgt)
    ct.tp(feet, yaw, pitch)


ct.aim = aim  # record() warms up with the same eye-corrected aim


def move(p0, p1, t0, t1, secs):
    n = max(2, int(secs * 20))
    start = time.time()
    for i in range(n + 1):
        u = ct.ease(i / n)
        aim(ct.lerp3(p0, p1, u), ct.lerp3(t0, t1, u))
        dt = start + (i + 1) / 20 - time.time()
        if dt > 0:
            time.sleep(dt)


def click(n=1, gap=0.9):
    for _ in range(n):
        subprocess.run(["xdotool", "click", "1"], env=dict(os.environ, DISPLAY=":99"))
        time.sleep(gap)


def snap(name):
    ct.snap(name)


def seg(name, cap, secs, fn, pose, warm=3):
    ct.record("a_" + name, cap, secs, fn, pose, warm=warm)
    PARTS.append("a_" + name)


def crystal(c):
    return Wp(c[0], 4.6, c[1])


def run():
    E("gamerule sendCommandFeedback false")
    # prepare: survival, protected, night, arena dormant, player outside the hall
    E("tp TestPlayer %.1f %.1f %.1f 0 0" % Wp(38, 1, 8))
    R("gamemode survival TestPlayer")
    R("effect give TestPlayer resistance 99999 4 true")
    R("effect give TestPlayer saturation 99999 1 true")
    R("item replace entity TestPlayer weapon.mainhand with minecraft:air")
    E("time set 18000")
    E("riftarena reset")
    time.sleep(6)
    aim(Wp(38, 1, 10), Wp(38, 3, 46))
    time.sleep(25)

    # 1. entry: step into the dark hall, candles light pair by pair towards the throne
    def entry():
        move(Wp(38, 1, 11), Wp(38, 1, 17), Wp(38, 3, 46), Wp(38, 3, 46), 3.0)
        time.sleep(2.0)
        snap("01_candles_lighting")
        move(Wp(38, 1, 17), Wp(38, 1, 22), Wp(38, 3, 46), Wp(38, 3.5, 46), 5.0)
        time.sleep(1.0)
        snap("02_candles_lit")
    seg("entry", "Вход: в темноте свечи загораются по одной — от двери к трону", 11, entry, (Wp(38, 1, 11), Wp(38, 3, 46)), warm=2)

    # 2. approach the throne -> awakening: roar, the six columns rise from their sockets (free first-person movement)
    def awaken():
        move(Wp(38, 1, 22), Wp(38, 1, 39), Wp(38, 3.5, 46), Wp(38, 5, 46), 5.0)
        move(Wp(38, 1, 39), Wp(38, 3, 42.5), Wp(38, 5, 46), Wp(38, 5.5, 46), 1.2)
        time.sleep(1.0)
        move(Wp(38, 3, 42.5), Wp(38, 3, 42.5), Wp(38, 5.5, 46), Wp(38, 3, 30), 1.6)
        time.sleep(2.5)
        snap("03_columns_rising")
        time.sleep(3.5)
        snap("04_columns_beams")
        move(Wp(38, 3, 42.5), Wp(38, 3, 42.5), Wp(38, 3, 30), Wp(31, 4, 36), 2.0)
    seg("awaken", "У трона: Хозяин Чертога пробуждается — из гнёзд поднимаются 6 колонн", 17, awaken, (Wp(38, 1, 22), Wp(38, 3.5, 46)), warm=1)

    # 3. healing beams converge into the throne (where the boss will stand - part A)
    def beams():
        move(Wp(34, 1, 27), Wp(36, 1, 35), Wp(38, 4, 46), Wp(38, 4, 46), 6.0)
        snap("05_healing_beams")
    seg("beams", "Колонны лучами передают целительную силу на трон (босс — следующий этап)", 6, beams, (Wp(34, 1, 27), Wp(38, 4, 46)), warm=3)

    # 4. a hit on the shaft does nothing (hint), three hits on the crystal shatter the column
    c = COLS[4]
    feet = Wp(c[0] - 2, 1, c[1])

    def crystal_hits():
        aim(feet, Wp(c[0], 3.3, c[1]))
        time.sleep(0.6)
        click(1, 1.2)
        snap("06_wrong_hit_hint")
        aim(feet, crystal(c))
        time.sleep(0.5)
        click(1, 1.0)
        click(1, 0.4)
        snap("07_crystal_cracked")
        time.sleep(0.5)
        click(1, 0.35)
        snap("08_column_shattered")
        time.sleep(1.5)
    seg("crystal", "Удар по стволу не действует. Три удара по кристаллу — и колонна разрушена", 7, crystal_hits, (feet, Wp(c[0], 3.3, c[1])), warm=4)

    # 5. a pickaxe breaks a column by hitting any part
    R("item replace entity TestPlayer weapon.mainhand with minecraft:iron_pickaxe")
    c = COLS[1]
    feet2 = Wp(c[0] + 2, 1, c[1])

    def pickaxe():
        aim(feet2, Wp(c[0], 2.6, c[1]))
        time.sleep(0.6)
        click(3, 1.0)
        time.sleep(1.2)
        snap("09_pickaxe_stump")
    seg("pickaxe", "Кирка ломает колонну ударом в любую часть", 5.5, pickaxe, (feet2, Wp(c[0], 2.6, c[1])), warm=4)

    # break the remaining ones off camera except the last
    for c in (COLS[0], COLS[2], COLS[3]):
        f = Wp(c[0] + (2 if c[0] < 38 else -2), 1, c[1])
        aim(f, Wp(c[0], 2.6, c[1]))
        time.sleep(0.8)
        click(3, 1.0)
    c = COLS[5]
    feet3 = Wp(c[0] - 2, 1, c[1])

    def last():
        aim(feet3, Wp(c[0], 2.6, c[1]))
        time.sleep(0.6)
        click(3, 1.0)
        time.sleep(0.3)
        snap("10_all_columns_down")
        move(feet3, feet3, Wp(c[0], 2.6, c[1]), Wp(38, 5, 46), 2.5)
        time.sleep(1.0)
    seg("last", "Последняя колонна: трон больше ничто не лечит", 7.5, last, (feet3, Wp(c[0], 2.6, c[1])), warm=4)
    print(R("execute in riftworld:elisar run riftarena status"), flush=True)

    # 6. reset (hall empty a minute -> here forced): candles go out, the stumps sink back into their sockets
    R("item replace entity TestPlayer weapon.mainhand with minecraft:air")
    R("gamemode spectator TestPlayer")
    R("effect give TestPlayer night_vision 60 0 true")
    cam = Wp(37, 4, 19)

    def reset():
        time.sleep(0.8)
        E("riftarena reset")
        time.sleep(1.5)
        snap("11_reset_sinking")
        time.sleep(2.5)
    seg("reset", "Если зал пуст минуту — свечи гаснут, колонны уходят в гнёзда и восстанавливаются", 5, reset, (cam, Wp(31, 2.5, 28)), warm=4)

    lst = "%s/s9_list.txt" % ct.VID
    open(lst, "w").write("".join("file '%s/s6_%s.mp4'\n" % (ct.VID, p) for p in PARTS))
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-f", "concat", "-safe", "0", "-i", lst,
                    "-c:v", "libx264", "-preset", "slow", "-crf", "27", "-pix_fmt", "yuv420p",
                    "/home/user/release/riftworld_session9.mp4"])
    print("VIDEO DONE", flush=True)


if __name__ == "__main__":
    run()
    print("TOUR DONE", flush=True)
