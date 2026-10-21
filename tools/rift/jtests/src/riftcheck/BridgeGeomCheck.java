package riftcheck;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.bridge.BridgeDeckGeom;
import ru.aiforge.riftworld.bridge.BridgeStyle;

/**
 * Real (executed) tests for the Rev 7 part 1 bridge geometry — the same maths the renderer and the collision use.
 * Runs without Minecraft (stubs for Vec3/BlockPos/AABB live in tools/rift/jtests/stubs).
 *
 * <pre>java -cp out riftcheck.BridgeGeomCheck</pre>
 */
public final class BridgeGeomCheck {

    private static int failures = 0;

    public static void main(String[] args) {
        // ---- spans at many angles, lengths and height differences: nothing may depend on a cardinal axis
        for (int deg = 0; deg < 360; deg += 15) {
            for (double len = 4; len <= BridgeDeckGeom.MAX_LEN; len += 3) {
                double rad = Math.toRadians(deg);
                int ax = 10, az = -7, ay = 64;
                int bx = ax + (int) Math.round(Math.cos(rad) * len);
                int bz = az + (int) Math.round(Math.sin(rad) * len);
                for (int dy = -3; dy <= 3; dy += 3) {
                    BridgeDeckGeom.Deck d = deck(ax, ay, az, bx, ay + dy, bz, 2, BridgeStyle.GARDEN.id);
                    checkDeck("deg=" + deg + " len=" + len + " dy=" + dy, d);
                }
            }
        }

        // ---- styles
        for (BridgeStyle st : BridgeStyle.values()) {
            BridgeDeckGeom.Deck d = deck(0, 64, 0, 12, 64, 5, 2, st.id);
            expect(d.style() == st, "style id " + st.id + " не восстановился: " + d.style());
            expect(st.plankTex != null && st.cableTex != null, "стиль " + st.key + ": нет текстур");
            expect(st.light > 0 && st.light <= 15, "стиль " + st.key + ": свет " + st.light + " вне 1..15");
            expect(st.holes >= 0 && st.holes < 1, "стиль " + st.key + ": доля дыр " + st.holes);
            expect(st.sagMul >= 1.0F, "стиль " + st.key + ": провис меньше базового");
        }
        expect(BridgeStyle.of(99) == BridgeStyle.PLAINS, "неизвестный style id должен падать в PLAINS");

        // ---- biome -> style mapping (per-dimension look: ад сгоревший, сады садовые)
        expect(BridgeDeckGeom.styleFor("violet_hills") == BridgeStyle.GARDEN.id, "04 должен быть садовым");
        expect(BridgeDeckGeom.styleFor("sizo_grove") == BridgeStyle.GROVE.id, "05 должен быть рощей");
        expect(BridgeDeckGeom.styleFor("waterfall_gardens") == BridgeStyle.GARDEN.id, "07 должен быть садовым");
        expect(BridgeDeckGeom.styleFor("ashen_lands") == BridgeStyle.BURNT.id, "ад должен быть сгоревшим");
        expect(BridgeDeckGeom.styleFor("mist_keep") == BridgeStyle.MIST.id, "туман — туманным");
        expect(BridgeDeckGeom.styleFor("elisar_city") == BridgeStyle.PLAINS.id, "город — обычный");
        expect(BridgeDeckGeom.styleFor(null) == BridgeStyle.PLAINS.id, "null — обычный");

        // ---- sag grows with the span and stays bounded
        double prev = 0;
        for (int len = 2; len <= BridgeDeckGeom.MAX_LEN; ++len) {
            BridgeDeckGeom.Deck d = deck(0, 64, 0, len, 64, 0, 2, BridgeStyle.PLAINS.id);
            double s = d.sag();
            expect(s >= 0.16 && s <= 1.25, "провис " + s + " вне [0.16, 1.25] при длине " + len);
            expect(s >= prev - 1e-9, "провис уменьшается с длиной: " + s + " < " + prev);
            prev = s;
        }

        // ---- a collapsed span really has a hole (both in planks and in collision)
        BridgeDeckGeom.Deck broken = new BridgeDeckGeom.Deck(new BlockPos(0, 64, 0), new BlockPos(16, 64, 0),
                new BlockPos(0, 64, 2), new BlockPos(16, 64, 2), BridgeStyle.BURNT.id, 0.4F, 0.6F);
        expect(broken.isBroken(), "мост с провалом не считается broken");
        Map<BlockPos, Integer> bc = broken.collisionCells();
        for (int i = 42; i <= 58; ++i) {
            double u = i / 100.0;
            expect(!broken.plankAt(u), "доска осталась в провале (u=" + u + ")");
            Vec3 p = broken.pt(u);
            BlockPos cell = new BlockPos((int) Math.floor(p.x), (int) Math.floor(p.y) - 1, (int) Math.floor(p.z));
            expect(!bc.containsKey(cell) && !bc.containsKey(cell.above()), "коллизия осталась в провале (u=" + u + ")");
        }
        expect(broken.sag() > deck(0, 64, 0, 16, 64, 0, 2, BridgeStyle.BURNT.id).sag() - 1e-9,
                "у сгоревшего моста провис должен быть не меньше обычного");
        // the very edges of the gap are still walkable - the hole starts strictly inside it
        expect(broken.open(0.5) && !broken.open(0.3) && !broken.open(0.7), "границы провала считаются неверно");

        // ---- wide bridge (MAX_WID) still works
        BridgeDeckGeom.Deck wide = deck(0, 64, 0, 14, 64, 9, BridgeDeckGeom.MAX_WID, BridgeStyle.MIST.id);
        checkDeck("wide MAX_WID", wide);

        if (failures > 0) {
            System.out.println("BridgeGeomCheck: FAILED (" + failures + " проблем)");
            System.exit(1);
        }
        System.out.println("BridgeGeomCheck: OK — под любым углом и с перепадом высот: настил провисает, поручень выше настила, "
                + "коллизия непрерывна, провал действительно провал");
    }

    private static BridgeDeckGeom.Deck deck(int ax, int ay, int az, int bx, int by, int bz, int halfWidth, int style) {
        int[] off = BridgeDeckGeom.perpOffset(bx - ax, bz - az, halfWidth);
        int px = off[0], pz = off[1];
        if (px == 0 && pz == 0) px = halfWidth;
        BlockPos a = new BlockPos(ax, ay, az), b = new BlockPos(bx, by, bz);
        return new BridgeDeckGeom.Deck(a, b, a.offset(px, 0, pz), b.offset(px, 0, pz), style, 0F, 0F);
    }

    private static void checkDeck(String tag, BridgeDeckGeom.Deck d) {
        tag = "[" + tag + "] ";
        expect(d.len() > 0.5, tag + "нулевая длина");
        expect(d.width() > 0.5, tag + "нулевая ширина");
        expect(d.sag() > 0 && d.cableSag() < d.sag(), tag + "кабель должен быть натянут сильнее настила");

        // ends sit on the posts, the middle sags
        expect(near(d.pt(0), d.tipA()), tag + "u=0 не на опоре A");
        expect(near(d.pt(1), d.tipB()), tag + "u=1 не на опоре B");
        // the deck must hang BELOW the straight chord between the two posts (works with different end heights too)
        for (double u = 0.05; u < 1.0; u += 0.05) {
            double chord = d.tipA().y + (d.tipB().y - d.tipA().y) * u;
            expect(d.pt(u).y < chord + 1e-9, tag + "настил не провисает (u=" + u + ")");
        }
        // both edges sag equally, the deck is not twisted
        for (double u = 0; u <= 1.0001; u += 0.1) {
            expect(Math.abs(d.pt(u, 0).y - d.pt(u, 1).y) < 1e-9, tag + "настил перекошен (u=" + u + ")");
            // the hand cable is always above the walking surface
            for (int side = 0; side < 2; ++side) {
                Vec3 from = side == 0 ? d.cableA() : d.cableA2();
                Vec3 to = side == 0 ? d.cableB() : d.cableB2();
                double chord = from.y + (to.y - from.y) * u;
                expect(d.railTop(u, side).y < chord + 1e-9,
                        tag + "трос не провисает (u=" + u + ", side=" + side + ")");
                expect(d.railTop(u, side).y > d.pt(u, side).y + 0.5,
                        tag + "поручень ниже настила (u=" + u + ", side=" + side + ")");
                expect(d.railMid(u, side).y > d.pt(u, side).y,
                        tag + "средний трос ниже настила (u=" + u + ", side=" + side + ")");
            }
        }
        // the width direction is horizontal and perpendicular to the span
        Vec3 w = d.widthDir(), s = d.tipB().subtract(d.tipA());
        expect(Math.abs(w.y) < 1e-9, tag + "направление ширины не горизонтально");
        double dot = w.x * s.x + w.z * s.z;
        double n = Math.sqrt(s.x * s.x + s.z * s.z) * Math.sqrt(w.x * w.x + w.z * w.z);
        // posts sit on whole blocks, so a couple of degrees of skew is unavoidable; more than ~10 is a broken deck
        expect(n < 1e-9 || Math.abs(dot / n) < 0.27, tag + "направление ширины не перпендикулярно пролёту: " + (dot / n));
        expect(d.width() >= halfWidthOf(d) * 0.55 && d.width() <= halfWidthOf(d) * 1.65,
                tag + "ширина настила далека от заданной: " + d.width());

        // collision: continuous along the span, always within [2,16] sixteenths, and it really covers the deck
        Map<BlockPos, Integer> cells = d.collisionCells();
        expect(!cells.isEmpty(), tag + "нет клеток коллизии");
        for (Map.Entry<BlockPos, Integer> e : cells.entrySet()) {
            int frac = e.getValue();
            expect(frac >= 2 && frac <= 16, tag + "высота клетки " + frac + "/16 вне 2..16");
        }
        for (double u = 0; u <= 1.0001; u += 0.02) {
            Vec3 p = d.pt(u);
            BlockPos cell = new BlockPos((int) Math.floor(p.x), (int) Math.floor(p.y + 1e-6) - 1, (int) Math.floor(p.z));
            boolean found = false;
            for (int ox = -1; ox <= 1 && !found; ++ox)
                for (int oz = -1; oz <= 1 && !found; ++oz)
                    for (int oy = -1; oy <= 1 && !found; ++oy)
                        found = cells.containsKey(cell.offset(ox, oy, oz));
            expect(found, tag + "дыра в коллизии на u=" + String.format("%.2f", u));
            if (!found) break;
        }
        // hangers are spaced regularly and cover the whole span
        double[] us = d.hangerUs();
        expect(us.length >= 3 && Math.abs(us[0]) < 1e-9 && Math.abs(us[us.length - 1] - 1) < 1e-9,
                tag + "подвесы не покрывают пролёт");
        for (int i = 1; i < us.length; ++i) {
            expect(us[i] > us[i - 1], tag + "подвесы не по порядку");
            expect((us[i] - us[i - 1]) * d.len() <= 1.6, tag + "слишком редкие подвесы");
        }
        // planks: dense enough to walk on, and the count matches the span
        expect(d.plankCount() >= 2, tag + "мало досок");
        int present = 0;
        for (int i = 0; i < d.plankCount(); ++i) if (d.plankAt((i + 0.5) / d.plankCount())) ++present;
        // holes are per plank, so allow three sigma of scatter
        double n0 = d.plankCount();
        double expected = n0 * (1 - d.style().holes);
        double sigma = Math.sqrt(n0 * d.style().holes * (1 - d.style().holes));
        expect(present >= expected - 3 * sigma - 1,
                tag + "досок меньше, чем допускает износ: " + present + "/" + (int) n0
                        + " (ожидалось ~" + Math.round(expected) + ")");

        // the owner post is the smallest corner, the stirring box contains the whole span
        BlockPos master = d.master();
        for (BlockPos c : d.corners()) expect(master.compareTo(c) <= 0, tag + "master() не минимальный угол");
        AABB box = d.stirBox();
        for (double u = 0; u <= 1.0001; u += 0.05)
            for (int side = 0; side < 2; ++side) {
                expect(box.contains(d.railTop(u, side)), tag + "коробка качания не содержит поручень (u=" + u + ")");
                expect(box.contains(d.pt(u, side)), tag + "коробка качания не содержит настил (u=" + u + ")");
            }
    }

    /** The requested width offset of the deck (a -> a2), recovered from the corners. */
    private static double halfWidthOf(BridgeDeckGeom.Deck d) {
        return Math.hypot(d.a2().getX() - d.a().getX(), d.a2().getZ() - d.a().getZ());
    }

    private static boolean near(Vec3 a, Vec3 b) {
        return a.distanceToSqr(b) < 1e-9;
    }

    private static void expect(boolean ok, String msg) {
        if (!ok) {
            ++failures;
            if (failures <= 25) System.out.println("  FAIL " + msg);
        }
    }
}
