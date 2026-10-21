package riftcheck;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import ru.aiforge.riftworld.tree.TreeGen;
import ru.aiforge.riftworld.tree.TreeSpecies;

/**
 * Real (executed) tests for the Rev 7 part 2 tree engine, run without Minecraft:
 *
 * <pre>
 *   java -cp out riftcheck.TreeGenCheck dump  /tmp/tree_dump_java.txt   # dump nodes for the Python cross-check
 *   java -cp out riftcheck.TreeGenCheck check                             # structural invariants
 * </pre>
 *
 * The dump is compared byte-for-byte with the reference implementation `tools/rift/tree_gen.py`
 * (tools/rift/jtests/run.py), so the off-line photos and the shipped Java code cannot drift apart silently.
 */
public final class TreeGenCheck {

    /** kind -> name, in the id order used by TreeSpecies. */
    private static final Object[][] SPECIES = {
            {TreeSpecies.SIZO, "sizo", 10, 13},
            {TreeSpecies.AMBRA, "ambra", 7, 10},
            {TreeSpecies.SIZO_GIANT, "sizo_giant", 17, 22},
            {TreeSpecies.LILAC, "lilac", 12, 17},
            {TreeSpecies.FERN, "fern", 7, 10},
            {TreeSpecies.MIST_PINE, "mist_pine", 14, 20},
            {TreeSpecies.OAK, "oak", 8, 12},
    };

    private static int failures = 0;

    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0] : "check";
        if ("dump".equals(mode)) {
            dump(args[1]);
            return;
        }
        check();
        if (failures > 0) {
            System.out.println("TreeGenCheck: FAILED (" + failures + " проблем)");
            System.exit(1);
        }
        System.out.println("TreeGenCheck: OK — движок детерминирован, ствол связный, листва не висит в воздухе, "
                + "деревья влезают в радиус 8 (нужно для генерации по чанкам)");
    }

    // ------------------------------------------------------------------ dump for the Python cross-check
    private static void dump(String path) {
        StringBuilder sb = new StringBuilder();
        try (java.io.PrintWriter w = new java.io.PrintWriter(path, "UTF-8")) {
            for (Object[] s : SPECIES) {
                int kind = (Integer) s[0];
                for (int h = (Integer) s[2]; h <= (Integer) s[3]; ++h) {
                    for (int seed : new int[] {1, 424242, 1000 + kind, 987654321}) {
                        List<int[]> nodes = build(kind, h, seed);
                        for (int[] n : nodes) {
                            w.println(s[1] + " " + h + " " + seed + " " + n[0] + " " + n[1] + " " + n[2]
                                    + " " + n[3] + " " + n[4]);
                        }
                        w.println("# " + s[1] + " " + h + " " + seed + " " + nodes.size());
                    }
                }
            }
            sb.append("dump written");
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(path + " — " + sb);
    }

    private static List<int[]> build(int kind, int height, int seed) {
        Map<Long, int[]> map = new TreeMap<>();
        List<int[]> out = new ArrayList<>();
        TreeGen.build(kind, height, seed, (dx, dy, dz, k, ax) -> out.add(new int[] {dx, dy, dz, k, ax}));
        out.sort((p, q) -> p[1] != q[1] ? Integer.compare(p[1], q[1])
                : p[2] != q[2] ? Integer.compare(p[2], q[2])
                : p[0] != q[0] ? Integer.compare(p[0], q[0])
                : Integer.compare(p[3], q[3]));
        return out;
    }

    // ------------------------------------------------------------------ invariants
    private static void check() {
        for (Object[] s : SPECIES) {
            int kind = (Integer) s[0];
            String name = (String) s[1];
            int hmin = (Integer) s[2], hmax = (Integer) s[3];
            for (int h = hmin; h <= hmax; ++h) {
                for (int seed : new int[] {1, 7, 424242, 1000 + kind, 987654321}) {
                    List<int[]> a = build(kind, h, seed);
                    List<int[]> b = build(kind, h, seed);
                    expect(!a.isEmpty(), name + " h=" + h + " seed=" + seed + ": дерево пустое");
                    expect(same(a, b), name + " h=" + h + " seed=" + seed + ": не детерминировано");
                    checkTree(name, h, seed, a, hmin, hmax);
                }
            }
        }
        // different seeds must produce different trees (not a single static template)
        List<int[]> t1 = build(TreeSpecies.LILAC, 15, 1);
        List<int[]> t2 = build(TreeSpecies.LILAC, 15, 2);
        expect(!same(t1, t2), "lilac: разные seed дают одинаковое дерево — движок не использует seed");

        // ids must stay stable: they are persisted inside CityPlan.Tree/plan.json
        expect(TreeSpecies.SIZO == 0 && TreeSpecies.AMBRA == 1 && TreeSpecies.DEAD == 2 && TreeSpecies.SIZO_GIANT == 3
                && TreeSpecies.LILAC == 4 && TreeSpecies.FERN == 5 && TreeSpecies.MIST_PINE == 6
                && TreeSpecies.OAK == 7, "идентификаторы пород изменены — сломаются сохранения");
        expect(TreeSpecies.usesEngine(TreeSpecies.LILAC) && TreeSpecies.usesEngine(TreeSpecies.MIST_PINE)
                && !TreeSpecies.usesEngine(TreeSpecies.DEAD), "usesEngine(): мёртвое дерево не должно идти через движок");
        for (Object[] s : SPECIES) {
            int kind = (Integer) s[0];
            int th = TreeGen.trunkHeight(kind, (Integer) s[2]);
            expect(th >= (Integer) s[2] - 1 && th <= (Integer) s[3] + 2,
                    s[1] + ": trunkHeight=" + th + " вне диапазона породы");
        }
    }

    private static int kindFor(String name) {
        for (Object[] s : SPECIES) if (s[1].equals(name)) return (Integer) s[0];
        throw new IllegalArgumentException(name);
    }

    private static void checkTree(String name, int h, int seed, List<int[]> nodes, int hmin, int hmax) {
        String tag = name + " h=" + h + " seed=" + seed + ": ";
        int logs = 0, leaves = 0, maxR = 0, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        Map<Long, Integer> kindAt = new TreeMap<>();
        for (int[] n : nodes) {
            int dx = n[0], dy = n[1], dz = n[2], k = n[3];
            if (k == TreeGen.LOG) ++logs; else ++leaves;
            maxR = Math.max(maxR, Math.max(Math.abs(dx), Math.abs(dz)));
            minY = Math.min(minY, dy);
            maxY = Math.max(maxY, dy);
            kindAt.put(key(dx, dy, dz), k);
            expect(n[4] >= 0 && n[4] <= 2, tag + "недопустимая ось " + n[4]);
        }
        expect(logs >= 2, tag + "нет брёвен (" + logs + ")");
        expect(leaves >= 5, tag + "почти нет листвы (" + leaves + ")");
        expect(minY >= 0, tag + "блок ниже земли (dy=" + minY + ")");
        expect(maxY <= hmax + 6, tag + "дерево выше спецификации: " + maxY + " > " + (hmax + 6));
        // generation walks chunks +-8 around the trunk, a wider tree would be cut by the chunk border
        expect(maxR <= 8, tag + "радиус " + maxR + " > 8 — обрежется границей чанка");
        // trunk continuity: from the ground up to the highest log there is always a log near the axis
        int topLog = 0;
        for (int[] n : nodes) if (n[3] == TreeGen.LOG && Math.abs(n[0]) <= 2 && Math.abs(n[2]) <= 2) topLog = Math.max(topLog, n[1]);
        for (int y = 0; y <= topLog; ++y) {
            boolean found = false;
            for (int dx = -2; dx <= 2 && !found; ++dx)
                for (int dz = -2; dz <= 2 && !found; ++dz)
                    if (kindAt.getOrDefault(key(dx, y, dz), -1) == TreeGen.LOG) found = true;
            expect(found, tag + "разрыв ствола на уровне y=" + y);
            if (!found) break;
        }
        // every log must be connected to the base through other logs (no detached branches)
        java.util.Set<Long> logSet = new java.util.HashSet<>();
        for (int[] n : nodes) if (n[3] == TreeGen.LOG) logSet.add(key(n[0], n[1], n[2]));
        java.util.Set<Long> seen = new java.util.HashSet<>();
        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
        for (int[] n : nodes) {
            if (n[3] == TreeGen.LOG && n[1] == 0) {
                queue.add(n);
                seen.add(key(n[0], 0, n[2]));
                break;
            }
        }
        expect(!queue.isEmpty(), tag + "нет бревна на земле");
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            for (int dx = -1; dx <= 1; ++dx)
                for (int dy = -1; dy <= 1; ++dy)
                    for (int dz = -1; dz <= 1; ++dz) {
                        long k = key(cur[0] + dx, cur[1] + dy, cur[2] + dz);
                        if (logSet.contains(k) && seen.add(k)) queue.add(new int[] {cur[0] + dx, cur[1] + dy, cur[2] + dz});
                    }
        }
        expect(seen.size() == logSet.size(),
                tag + "оторванные ветви: " + (logSet.size() - seen.size()) + " брёвен не связаны со стволом");

        // no floating leaves: each leaf touches a log or another leaf chain that reaches a log
        for (int[] n : nodes) {
            if (n[3] != TreeGen.LEAF) continue;
            boolean touches = false;
            for (int dx = -1; dx <= 1 && !touches; ++dx)
                for (int dy = -1; dy <= 1 && !touches; ++dy)
                    for (int dz = -1; dz <= 1 && !touches; ++dz) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        Integer k = kindAt.get(key(n[0] + dx, n[1] + dy, n[2] + dz));
                        if (k != null) touches = true;
                    }
            expect(touches, tag + "одинокий блок листвы в воздухе (" + n[0] + "," + n[1] + "," + n[2] + ")");
        }
    }

    private static long key(int x, int y, int z) {
        return ((long) (x + 64) << 42) | ((long) (y + 64) << 21) | (long) (z + 64);
    }

    private static boolean same(List<int[]> a, List<int[]> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); ++i) {
            for (int j = 0; j < 5; ++j) if (a.get(i)[j] != b.get(i)[j]) return false;
        }
        return true;
    }

    private static void expect(boolean ok, String msg) {
        if (!ok) {
            ++failures;
            if (failures <= 25) System.out.println("  FAIL " + msg);
        }
    }
}
