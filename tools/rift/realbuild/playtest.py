#!/usr/bin/env python3
"""Играет с живым сервером через RCON и проверяет мод в настоящем мире.

Это не юнит-тест и не стенд-заглушка: команды выполняет настоящий выделенный сервер Forge 1.20.1 с загруженным
`riftworld-<version>.jar`, поэтому проверяются реальная регистрация блоков/сущностей, реальная генерация
измерения `riftworld:elisar` и реальные NBT-данные блок-энтити.

Usage:
    python3 tools/rift/realbuild/playtest.py [--host 127.0.0.1] [--port 25575] [--password rift] [--json out.json]

Требования: сервер уже запущен (`run_server.py --start`) с включённым RCON.
"""
import argparse
import json
import re
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from rcon import Rcon  # noqa: E402

DIM = "riftworld:elisar"

ROOT = Path(__file__).resolve().parents[3]
BLOCKSTATES = ROOT / "mod/src/main/resources/assets/riftworld/blockstates"


def all_blocks():
    """Все блоки мода по его же blockstate-файлам: сервер должен знать каждый из них."""
    if not BLOCKSTATES.is_dir():
        return []
    return sorted(p.stem for p in BLOCKSTATES.glob("*.json"))


# блоки, которые обязательно проверяем поимённо (мосты, деревья, якоря арены)
KEY_BLOCKS = ["bridge_post", "bridge_walk", "ambra_log", "lilac_log", "mist_pine_log", "fern_log",
              "ambra_leaves", "lilac_leaves", "mist_pine_leaves", "fern_leaves", "temple_floor",
              "arena_core", "boss_column", "demon_gate", "mosaic_core", "elisar_water"]


class Play:
    def __init__(self, rcon):
        self.r = rcon
        self.results = []

    def cmd(self, c):
        return self.r.cmd(c).strip()

    def ok(self, name, passed, detail=""):
        self.results.append({"test": name, "ok": bool(passed), "detail": detail})
        print(("  [OK]   " if passed else "  [FAIL] ") + name + (f" — {detail}" if detail else ""))
        return passed

    def put(self, x, y, z, block):
        """Ставит блок «с нуля»: сначала воздух, потом блок. Повторная установка того же блока на то же
        место возвращает «Could not set the block», хотя мир не меняется — это мешало повторным прогонам."""
        self.cmd(f"execute in {DIM} run setblock {x} {y} {z} minecraft:air")
        return self.cmd(f"execute in {DIM} run setblock {x} {y} {z} {block}")

    def run(self, name, cmd, expect="Changed"):
        out = self.cmd(cmd)
        return self.ok(name, expect in out, out[:120])

    # ----- проверки -------------------------------------------------------------------------------------
    def server_alive(self):
        out = self.cmd("list")
        return self.ok("сервер отвечает по RCON", "players online" in out, out)

    def dimension(self):
        out = self.cmd("forge tps")
        return self.ok(f"измерение {DIM} загружено", DIM in out,
                       ", ".join(l.split(":")[0] for l in out.splitlines() if "Dim " in l))

    def commands(self):
        out = self.cmd("riftarena status")
        return self.ok("команда мода /riftarena отвечает", "arena" in out.lower() or "loaded" in out.lower(), out)

    def forceload(self):
        """Без форс-лоча execute in другие измерения отвечает «That position is not loaded»."""
        out = self.cmd(f"execute in {DIM} run forceload add -48 -48 112 112")
        # «No chunks were marked» = зона уже под форс-лочем с прошлого прогона, это тоже успех
        loaded = "Marked" in out or "No chunks were marked" in out
        self.ok("тестовая зона форс-лочится", loaded, out[:110])
        time.sleep(4)
        return loaded

    def place_blocks(self):
        """Ставит каждый блок мода в воздух, окружив его камнем: лианам/растениям нужна опора, иначе
        ванильный setblock честно отвечает «Could not set the block» (это поведение мира, а не баг мода)."""
        names = all_blocks()
        if not names:
            return self.ok("список блоков из resources", False, f"нет {BLOCKSTATES}")
        self.ok("blockstates мода найдены", len(names) > 200, f"{len(names)} блоков")
        missing, unsupported = [], []
        for i, name in enumerate(names):
            x, z = 4 + (i % 30) * 4, 4 + (i // 30) * 6
            y = 110
            # опора только СНИЗУ и СЗАДИ: шаг 4 по x, чтобы «обвес» одного блока не портил соседний
            for dx, dy, dz in ((0, -1, 0), (0, 0, 1)):
                self.cmd(f"execute in {DIM} run setblock {x + dx} {y + dy} {z + dz} minecraft:stone")
            out = self.put(x, y, z, f"riftworld:{name}")
            if "Changed" in out:
                continue
            # опора не помогла: пробуем в воздухе (часть блоков не требует опоры, но не любит соседей)
            if "Could not set" in out:
                unsupported.append(name)
                continue
            missing.append(f"{name}: {out[:60]}")
        # «Could not set» при наличии опоры — тоже сигнал, но отделяем: помечаем как «нужна другая опора»
        self.ok("ни один блок не падает с ошибкой регистрации", not missing, "; ".join(missing[:4]))
        self.ok("все блоки ставятся в мире", not unsupported,
                ("не встали: " + ", ".join(unsupported[:6])) if unsupported else "")

    def bridge_be(self):
        """Мост: опора + настил, у опоры должен появиться блок-энтити с данными."""
        a = self.put(20, 92, 20, "riftworld:bridge_post")
        self.ok("опора моста ставится", "Changed" in a, a[:80])
        out = self.cmd(f"execute in {DIM} run data get block 20 92 20")
        has_nbt = "{" in out and "has the following block data" in out.lower() or "{" in out
        self.ok("у опоры моста есть блок-энтити", has_nbt, out[:160])

    def tree_blocks(self):
        """Деревья: ставим ствол и листву всех 4 пород — именно их использует DT-аддон."""
        species = ["lilac", "sizo", "fern", "mist_pine"]
        for i, sp in enumerate(species):
            x = 40 + i * 4
            a = self.put(x, 90, 40, f"riftworld:{sp}_log")
            b = self.put(x, 91, 40, f"riftworld:{sp}_leaves")
            self.ok(f"порода {sp}: ствол+листва", "Changed" in a and "Changed" in b, f"{a[:40]} / {b[:40]}")

    def tags(self):
        out = self.put(60, 90, 60, "riftworld:lilac_log")
        if "Changed" not in out:
            return self.ok("теги брёвен", False, out[:80])
        check = self.cmd(f"execute in {DIM} if block 60 90 60 #minecraft:logs")
        return self.ok("бревно мода попадает в #minecraft:logs", "Test passed" in check, check)

    # каждая порода живёт в своём биоме; код — это короткое имя в /rifttreeplan
    BIOMES = [
        ("riftworld:sizo_grove", "sg", "sizo_log"),
        ("riftworld:violet_hills", "vh", "lilac_log"),
        ("riftworld:waterfall_gardens", "wg", "fern_log"),
        ("riftworld:mist_keep", "mk", "mist_pine_log"),
    ]

    def biomes(self):
        """Биом = своя логика генерации. Тут ловится класс ошибок «биом есть, но это не тот биом».

        `locate biome` берёт биом из источника, а /riftprobe спрашивает сам планировщик биома: если они
        расходятся, значит перепутаны Holder-ы в ElisarBiomeSource (так и было: «Висячие сады» и Туманный
        дозор молча поменялись биомами из-за порядка полей в RecordCodecBuilder).
        """
        for biome, code, _ in self.BIOMES:
            out = self.cmd(f"execute in {DIM} run locate biome {biome}")
            m = re.search(r"\[(-?\d+), (-?\d+), (-?\d+)\]", out)
            if not m:
                self.ok(f"биом {biome} существует", False, out[:120])
                continue
            x, y, z = int(m.group(1)), int(m.group(2)), int(m.group(3))
            self.cmd(f"execute in {DIM} run forceload add {x} {z} {x} {z}")   # чанк должен существовать
            time.sleep(1.5)
            saved = self.cmd(f"execute in {DIM} if biome {x} {y} {z} {biome}")
            probe = self.cmd(f"execute in {DIM} positioned {x} 100 {z} run riftprobe")
            mine = f"{code}.inside=true" in probe
            self.ok(f"{biome}: биом в чанке совпадает с планировщиком",
                    "Test passed" in saved and mine,
                    f"locate {x},{y},{z}; if_biome={'да' if 'Test passed' in saved else 'НЕТ'}; "
                    f"probe={'да' if mine else 'НЕТ'} | {probe[:150]}")

    def biome_trees(self):
        """Деревья по-настоящему: планировщик говорит, что сажает, а мир должен это содержать.

        Раньше здесь был перебор точек вокруг `locate biome` — и он честно показывал ноль, потому что деревья
        стоят на островах, а `locate` возвращает любую точку биома (до 170 блоков от острова). Теперь:
        /rifttreeplan сверяет план генератора с блоками в мире, /rifttreescan считает стволы и листву.
        """
        for biome, code, log in self.BIOMES:
            out = self.cmd(f"execute in {DIM} run locate biome {biome}")
            m = re.search(r"\[(-?\d+), (-?\d+), (-?\d+)\]", out)
            if not m:
                self.ok(f"деревья в {biome}", False, out[:120])
                continue
            cx, cz = int(m.group(1)), int(m.group(3))
            plan = self.cmd(f"execute in {DIM} positioned {cx} 100 {cz} run rifttreeplan {code} 160")
            planned = int((re.search(r"planned=(\d+)", plan) or [0, 0])[1])
            if planned == 0:
                self.ok(f"деревья в {biome}: планировщик сажает деревья", False, plan[:160])
                continue
            # центр региона из плана — именно там стоят деревья; грузим квадрат вокруг него
            rm = re.search(r"region_at_center=(-?\d+),(-?\d+) R=(\d+)", plan)
            rx, rz = (int(rm.group(1)), int(rm.group(2))) if rm else (cx, cz)
            rad = min(96, int(rm.group(3))) if rm else 96
            self.cmd(f"execute in {DIM} run forceload add {rx - rad} {rz - rad} {rx + rad} {rz + rad}")
            time.sleep(4)
            again = self.cmd(f"execute in {DIM} positioned {rx} 100 {rz} run rifttreeplan {code} 96")
            in_world = int((re.search(r"in_world=(\d+)", again) or [0, 0])[1])
            total = int((re.search(r"planned=(\d+)", again) or [0, 0])[1])
            scan = self.cmd(f"execute in {DIM} positioned {rx} 100 {rz} run rifttreescan {rad} 60 170")
            self.ok(f"деревья в {biome}: план {total} → в мире {in_world}", in_world > 0 and in_world == total,
                    f"{again[:120]} | {scan[:220]}")

    def biome_lookup(self):
        out = self.cmd(f"execute in {DIM} run locate biome riftworld:sizo_grove")
        found = "nearest" in out
        return self.ok("биом riftworld:sizo_grove находится в мире", found, out[:120])

    def entities(self):
        """Сущности: summon реального моба и чтение его NBT (значит EntityType зарегистрирован и тикается)."""
        for eid in ("morgrath", "ember_guard", "paper_dragon"):
            out = self.cmd(f"execute in {DIM} run summon riftworld:{eid} 30 100 30")
            spawned = "Summoned new" in out
            data = self.cmd(f"execute in {DIM} run data get entity @e[type=riftworld:{eid},limit=1]")
            self.ok(f"сущность riftworld:{eid} призывается и имеет NBT", spawned and "{" in data,
                    f"{out[:60]} | {data[:90]}")
            self.cmd(f"execute in {DIM} run kill @e[type=riftworld:{eid}]")

    def items(self):
        """Предметы: кладём их в мир как entity item — сервер сам проверит, что id предмета существует."""
        for iid in ("bridge_rope", "resonance_stone", "elisar_ducat", "morgrath_scythe",
                    "guard_sickle", "morgrath_spawn_egg"):
            self.cmd(f"execute in {DIM} run kill @e[type=minecraft:item,tag=riftprobe]")
            out = self.cmd(f'execute in {DIM} run summon minecraft:item 34 100 34 '
                           f'{{Item:{{id:"riftworld:{iid}",Count:1b}},Tags:["riftprobe"]}}')
            # «Коса Морграта» падает в мир как СВОЯ сущность (ScytheItemEntity: не исчезает, светит, горит),
            # поэтому ищем предмет и среди обычных item, и среди riftworld:scythe_item
            back = ""
            for sel in ("type=minecraft:item,tag=riftprobe", "type=riftworld:scythe_item,tag=riftprobe",
                        "type=riftworld:scythe_item"):
                back = self.cmd(f"execute in {DIM} run data get entity @e[{sel},limit=1] Item.id")
                if "No entity was found" not in back:
                    break
            self.ok(f"предмет riftworld:{iid} существует", "Summoned new" in out and iid in back,
                    f"{out[:50]} | {back[:70]}")
            self.cmd(f"execute in {DIM} run kill @e[type=minecraft:item,tag=riftprobe]")
            self.cmd(f"execute in {DIM} run kill @e[type=riftworld:scythe_item,tag=riftprobe]")

    def world_bridges(self):
        """Мосты в настоящем мире: генератор планирует настил (`WorldBridges`), блоки должны стоять в чанках.

        `/riftbridgeplan` прогоняет ту же математику, что и генератор чанков (без записи), и говорит, где настил;
        `/riftblocks` считает блоки мода в загруженных чанках вокруг найденного моста.
        """
        found_any = 0
        for biome, origin in (("riftworld:violet_hills", (0, 0)),
                              ("riftworld:waterfall_gardens", (3000, 0)),
                              ("riftworld:waterfall_gardens", (-3000, 2000)),
                              ("riftworld:waterfall_gardens", (5000, -4000))):
            out = self.cmd(f"execute in {DIM} positioned {origin[0]} 100 {origin[1]} run locate biome {biome}")
            m = re.search(r"\[(-?\d+), (-?\d+), (-?\d+)\]", out)
            if not m:
                continue
            cx, cz = int(m.group(1)), int(m.group(3))
            plan = self.cmd(f"execute in {DIM} positioned {cx} 100 {cz} run riftbridgeplan 160")
            decks = int((re.search(r"decks=(\d+)", plan) or [0, 0])[1])
            if decks == 0:
                # мост в «Висячих садах» редкий (гуляж через промоину с шансом ~0.6 на регион) — это не дефект
                self.ok(f"{biome} ({cx},{cz}): моста в регионе нет (редкий мост, допустимо)", True, plan[:120])
                continue
            self.ok(f"генератор планирует мост в {biome} ({cx},{cz})", True, plan[:200])
            dm = re.search(r"-->(\-?\d+),(\-?\d+)", plan) or re.search(r" (\-?\d+),(\-?\d+)->", plan)
            bx, bz = (int(dm.group(1)), int(dm.group(2))) if dm else (cx, cz)
            self.cmd(f"execute in {DIM} run forceload add {bx - 48} {bz - 48} {bx + 48} {bz + 48}")
            time.sleep(4)
            scan = self.cmd(f"execute in {DIM} positioned {bx} 100 {bz} run riftblocks 48")
            posts = int((re.search(r"bridge_post=(\d+)", scan) or [0, 0])[1])
            walks = int((re.search(r"bridge_walk=(\d+)", scan) or [0, 0])[1])
            self.ok(f"мост стоит в мире ({biome} {cx},{cz}): 4 опоры и настил", posts >= 4 and walks > 0,
                    f"bridge_post={posts} bridge_walk={walks} | {scan[:150]}")
            found_any += 1 if (posts >= 4 and walks > 0) else 0
        self.ok("мосты генерируются хотя бы в двух проверенных местах", found_any >= 2, f"подтверждено {found_any}")

    def worldgen(self):
        """Настоящая генерация: форс-лочим регион и смотрим, что поверхность не пустая."""
        out = self.cmd(f"execute in {DIM} run forceload add 1200 176 1240 216")
        self.ok("чанки форс-лочатся", "Marked" in out or "No chunks were marked" in out, out[:100])
        time.sleep(6)
        hit = self.cmd(f"execute in {DIM} if block 1200 60 176 minecraft:air")
        self.ok("генератор выдаёт непустую колонку на y=60", "Test failed" in hit,
                "блок на глубине есть (не океан пустоты)" if "Test failed" in hit else hit[:90])

    def no_crash(self):
        out = self.cmd("forge tps")
        self.ok("TPS держится (20.0)", "TPS: 20.0" in out,
               " ".join(l.strip() for l in out.splitlines() if "Overall" in l)[:80]) if "Overall" in out else None
        return True


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--host", default="127.0.0.1")
    ap.add_argument("--port", type=int, default=25575)
    ap.add_argument("--password", default="rift")
    ap.add_argument("--json", default=None)
    args = ap.parse_args()

    rcon = Rcon(args.host, args.port, args.password)
    p = Play(rcon)
    print("== живой сервер Forge 1.20.1 + мод riftworld ==")
    p.server_alive()
    p.dimension()
    p.forceload()
    p.commands()
    p.place_blocks()
    p.bridge_be()
    p.tree_blocks()
    p.tags()
    p.biome_lookup()
    p.biomes()
    p.biome_trees()
    p.world_bridges()
    p.entities()
    p.items()
    p.worldgen()
    p.no_crash()
    rcon.close()

    failed = [r for r in p.results if not r["ok"]]
    print(f"\nитог: {len(p.results) - len(failed)}/{len(p.results)} проверок пройдено")
    if args.json:
        Path(args.json).parent.mkdir(parents=True, exist_ok=True)
        Path(args.json).write_text(json.dumps(p.results, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"результаты: {args.json}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
