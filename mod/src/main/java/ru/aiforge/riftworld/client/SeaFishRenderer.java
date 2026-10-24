package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.entity.SeaFishEntity;

/**
 * Рев 8: процедурная ихтиофауна Элисара — ни одной текстуры и модели, только квады/треугольники
 * с собственной «фальшивой» подсветкой (как бумажный дракон). Поэтому рыбы дёшевы и не грузят атлас.
 */
public class SeaFishRenderer extends EntityRenderer<SeaFishEntity> {
    private static final int SILVER_B = 0xC6D3DC, SILVER_T = 0x9AAEBD, SILVER_L = 0xEDF4F8, EYE = 0x16202A;
    private static final int GOLD_B = 0xE9C25A, GOLD_T = 0xC29A38, GOLD_L = 0xF7E3A0;
    private static final int BAND_D = 0x39404C, BAND_O = 0xDD8B4C, PALE = 0xEAE4D4;
    private static final int JELLY_B = 0x9FE6FF, JELLY_L = 0xD8F7FF, JELLY_T = 0x7FC8E8;
    private static final int RAY_T = 0x68798C, RAY_B = 0xDCE6EC, RAY_S = 0x93A5B4;
    private static final int ANGLER_B = 0x2C3947, ANGLER_T = 0x1A2430, LURE = 0xDFFFA8, LURE_CORE = 0xF6FFD6;
    private static final int EEL_B = 0xD6CFBF, EEL_T = 0xB2A996;
    // Рев 10: тропические мирные и «злые и редкие» пещерные
    private static final int SURGEON_B = 0x2F6FD0, SURGEON_T = 0x1E4C96, SURGEON_L = 0xF2D24C, SURGEON_D = 0x152F5E;
    private static final int TURTLE_B = 0x4E7A4A, TURTLE_T = 0x37582F, TURTLE_L = 0xB8C48A, TURTLE_S = 0xE3DCC0;
    private static final int GLOOM_B = 0x2A1F36, GLOOM_T = 0x140D1E, GLOOM_F = 0x8A2F6E, GLOOM_L = 0xC46BFF;
    private static final int STING_B = 0xB05AD6, STING_L = 0xE6B4FF, STING_C = 0xFFE9FF;

    public SeaFishRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(SeaFishEntity e) {
        return new ResourceLocation("minecraft", "textures/misc/white.png");
    }

    @Override
    public void render(SeaFishEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
        pose.pushPose();
        VertexConsumer vc = buf.getBuffer(RenderType.debugQuads());
        float yRot = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
        float xRot = Mth.lerp(partial, e.xRotO, e.getXRot());
        double t = e.tickCount + partial;
        double tail = Mth.lerp(partial, e.tail0, e.tail);

        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - yRot));
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(xRot));

        switch (e.variant()) {
            case SeaFishEntity.JELLY -> jelly(vc, pose, t);
            case SeaFishEntity.RAY -> ray(vc, pose, t, tail);
            case SeaFishEntity.ANGLER -> angler(vc, pose, t, tail);
            case SeaFishEntity.EEL -> eel(vc, pose, t, tail);
            case SeaFishEntity.SURGEON -> fish(vc, pose, t, tail, 0.15, 0.64, SURGEON_B, SURGEON_T, SURGEON_L, 2);
            case SeaFishEntity.TURTLE -> turtle(vc, pose, t, tail);
            case SeaFishEntity.GLOOM -> gloom(vc, pose, t, tail);
            case SeaFishEntity.STING -> sting(vc, pose, t);
            case SeaFishEntity.GOLD -> fish(vc, pose, t, tail, 0.17, 0.72, GOLD_B, GOLD_T, GOLD_L, 0);
            case SeaFishEntity.STRIPED -> fish(vc, pose, t, tail, 0.12, 0.60, PALE, BAND_D, 0xFFF6E6, 1);
            default -> fish(vc, pose, t, tail, 0.13, 0.72, SILVER_B, SILVER_T, SILVER_L, 0);
        }
        pose.popPose();
    }

    /* ================================================================ обычная рыба */

    /** Тело-веретено из 8-гранных колец + хвост-плавник, спинной и грудные плавники, глаза. */
    private static void fish(VertexConsumer vc, PoseStack pose, double t, double tail, double r0, double len,
                             int body, int top, int belly, int stripes) {
        int segs = 8;
        double step = len / segs;
        Vec3[] c = new Vec3[segs + 1];
        double[] r = new double[segs + 1];
        for (int i = 0; i <= segs; i++) {
            double f = i / (double) segs;
            double sway = Math.sin(tail * 0.55 - i * 0.65) * 0.055 * (f * f) * len;
            c[i] = new Vec3(sway, Math.sin(tail * 0.4 - i * 0.5) * 0.012 * f, -i * step);
            r[i] = r0 * Math.sin(Math.PI * Math.min(1.0, 0.16 + f * 0.95)) * (f > 0.8 ? 0.55 : 1.0);
        }
        for (int i = 0; i < segs; i++) ring(vc, pose, c[i], c[i + 1], r[i], r[i + 1], body, top, belly);
        // хвост: два треугольника, машут в противофазе к телу
        Vec3 tailA = c[segs];
        double wag = Math.sin(tail * 0.55 - segs * 0.65) * 0.16;
        Vec3 finT = tailA.add(new Vec3(wag, 0.02, -len * 0.26));
        tri(vc, pose, tailA.add(new Vec3(0, r0 * 0.55, 0)), tailA.add(new Vec3(0, -r0 * 0.55, 0)), finT, top);
        tri(vc, pose, tailA.add(new Vec3(0, 0, 0)), finT.add(new Vec3(0, r0 * 0.5, 0)), finT, belly);
        // спинной плавник
        Vec3 d0 = c[2].add(new Vec3(0, r[2] * 0.9, 0)), d1 = c[5].add(new Vec3(0, r[5] * 0.9, 0));
        tri(vc, pose, d0, d1, d1.add(new Vec3(0, r0 * 0.7, -len * 0.05)), top);
        // грудные плавники + брюшко
        for (int s = -1; s <= 1; s += 2) {
            Vec3 f0 = c[2].add(new Vec3(s * r[2] * 0.8, -r[2] * 0.2, 0));
            tri(vc, pose, f0, f0.add(new Vec3(s * r0 * 0.85, -r0 * 0.35, -len * 0.10)), f0.add(new Vec3(s * r0 * 0.30, -r0 * 0.10, -len * 0.22)),
                    Math.abs(s) > 0 ? belly : belly);
            // глазик
            Vec3 ep = c[0].add(new Vec3(s * r[0] * 0.55, r[0] * 0.25, -step * 0.55));
            quadEye(vc, pose, ep, EYE);
        }
        if (stripes == 1) {                       // полосатая: тёмные и оранжевые ленты поперёк тела
            for (int i = 1; i < segs - 1; i += 2) {
                int col = (i % 4 == 1) ? BAND_D : BAND_O;
                ringStripe(vc, pose, c[i], r[i], col);
            }
        } else if (stripes == 2) {                // синий хирург: чёрные и жёлтые полосы (как у рыбы-хирурга)
            for (int i = 1; i < segs - 1; i += 2) {
                ringStripe(vc, pose, c[i], r[i], i % 4 == 1 ? BAND_D : SURGEON_L);
            }
            tri(vc, pose, c[segs].add(-0.10, 0.02, 0), c[segs].add(0.10, 0.02, 0), c[segs].add(0, -0.02, -0.22), SURGEON_L);
        }
    }

    /** 8-гранное кольцо-сечение и мостик к следующему (верх/низ/бока разного тона). */
    private static void ring(VertexConsumer vc, PoseStack pose, Vec3 a, Vec3 b, double ra, double rb, int body, int top, int belly) {
        int n = 8;
        Vec3[] pa = new Vec3[n], pb = new Vec3[n];
        for (int k = 0; k < n; k++) {
            double ang = Math.PI * 2 * k / n;
            double x = Math.cos(ang), y = Math.sin(ang);
            pa[k] = a.add(x * ra, y * ra * 1.15, 0);
            pb[k] = b.add(x * rb, y * rb * 1.15, 0);
        }
        for (int k = 0; k < n; k++) {
            double ang = Math.PI * 2 * (k + 0.5) / n;
            int col = Math.sin(ang) > 0.45 ? top : (Math.sin(ang) < -0.45 ? belly : body);
            quad(vc, pose, pa[k], pb[k], pb[(k + 1) % n], pa[(k + 1) % n], col);
        }
    }

    private static void ringStripe(VertexConsumer vc, PoseStack pose, Vec3 c, double r, int col) {
        int n = 8;
        for (int k = 0; k < n; k++) {
            double a0 = Math.PI * 2 * k / n, a1 = Math.PI * 2 * (k + 1) / n;
            Vec3 p0 = c.add(Math.cos(a0) * (r + 0.004), Math.sin(a0) * (r + 0.004) * 1.15, 0);
            Vec3 p1 = c.add(Math.cos(a1) * (r + 0.004), Math.sin(a1) * (r + 0.004) * 1.15, 0);
            Vec3 q0 = p0.add(0, 0, -0.035), q1 = p1.add(0, 0, -0.035);
            quad(vc, pose, p0, p1, q1, q0, col);
        }
    }

    /* ================================================================ медуза */

    private static void jelly(VertexConsumer vc, PoseStack pose, double t) {
        double pulse = 0.55 + Math.sin(t * 0.08) * 0.10;
        // купол из треугольного веера
        Vec3 apex = new Vec3(0, 0.38, 0);
        int n = 10;
        for (int k = 0; k < n; k++) {
            double a0 = Math.PI * 2 * k / n, a1 = Math.PI * 2 * (k + 1) / n;
            Vec3 p0 = new Vec3(Math.cos(a0) * pulse, 0.0, Math.sin(a0) * pulse);
            Vec3 p1 = new Vec3(Math.cos(a1) * pulse, 0.0, Math.sin(a1) * pulse);
            tri(vc, pose, apex, p0, p1, (k % 2 == 0) ? JELLY_L : JELLY_B);
            tri(vc, pose, apex, p1, p0, JELLY_T);                    // изнанка (купол виден с любой стороны)
        }
        // щупальца — тонкие извивающиеся полоски
        for (int k = 0; k < 12; k++) {
            double a = Math.PI * 2 * k / 12;
            double ph = t * 0.12 + k;
            Vec3 prev = new Vec3(Math.cos(a) * pulse * 0.85, 0, Math.sin(a) * pulse * 0.85);
            for (int s = 1; s <= 5; s++) {
                double y = -s * 0.16;
                double sway = Math.sin(ph + s * 0.7) * 0.05;
                Vec3 nxt = new Vec3(prev.x + sway, y, prev.z + Math.cos(ph + s * 0.5) * 0.05);
                quad(vc, pose, prev.add(-0.012, 0, 0), prev.add(0.012, 0, 0), nxt.add(0.012, 0, 0), nxt.add(-0.012, 0, 0),
                        (s % 2 == 0) ? JELLY_B : JELLY_L);
                prev = nxt;
            }
        }
    }

    /* ================================================================ манта */

    private static void ray(VertexConsumer vc, PoseStack pose, double t, double tail) {
        double flap = Math.sin(t * 0.07) * 0.14 + 0.03;
        Vec3 head = new Vec3(0, 0, 0.62);
        Vec3 lTip = new Vec3(-1.35, flap * 0.7, -0.15);
        Vec3 rTip = new Vec3(1.35, flap * 0.7, -0.15);
        Vec3 back = new Vec3(0, 0, -0.62);
        Vec3 midL = new Vec3(-0.55, flap * 0.25 + 0.05, 0.12);
        Vec3 midR = new Vec3(0.55, flap * 0.25 + 0.05, 0.12);
        // сверху и снизу — «крылья» из четырёх треугольников
        tri(vc, pose, head, midL, midR, RAY_T);
        tri(vc, pose, midL, lTip, midR, RAY_T);
        tri(vc, pose, midR, lTip, back, RAY_S);
        tri(vc, pose, head, midR, midL, RAY_B);
        tri(vc, pose, midL, midR, lTip, RAY_B);
        tri(vc, pose, midR, back, lTip, RAY_B);
        // «рожки» и хвост-хлыст
        for (int s = -1; s <= 1; s += 2) tri(vc, pose, head, head.add(new Vec3(s * 0.16, 0.02, 0.22)), head.add(new Vec3(s * 0.05, -0.02, 0.05)), RAY_S);
        Vec3 prev = back;
        for (int s = 1; s <= 8; s++) {
            Vec3 nxt = back.add(Math.sin(tail * 0.2 + s * 0.5) * 0.01 * s, -0.004 * s, -s * 0.22);
            quad(vc, pose, prev.add(0, 0.012, 0), prev.add(0, -0.012, 0), nxt.add(0, -0.012, 0), nxt.add(0, 0.012, 0), RAY_S);
            prev = nxt;
        }
        quadEye(vc, pose, new Vec3(0.16, 0.07, 0.5), 0x1A1E24);
        quadEye(vc, pose, new Vec3(-0.16, 0.07, 0.5), 0x1A1E24);
    }

    /* ================================================================ удильщик */

    private static void angler(VertexConsumer vc, PoseStack pose, double t, double tail) {
        // приплюснутая голова-капкан и короткое тело
        Vec3[] c = new Vec3[5];
        double[] r = new double[5];
        for (int i = 0; i < 5; i++) {
            double f = i / 4.0;
            c[i] = new Vec3(Math.sin(tail * 0.5 - i * 0.6) * 0.03 * f * f, 0, -i * 0.19);
            r[i] = 0.30 * Math.sin(Math.PI * Math.min(1.0, 0.25 + f * 0.9));
        }
        for (int i = 0; i < 4; i++) ring(vc, pose, c[i], c[i + 1], r[i], r[i + 1], ANGLER_B, ANGLER_T, 0x3E4E5E);
        // пасть: две половинки, зубы-треугольники
        Vec3 mouth = c[0].add(0, -0.02, 0.30);
        tri(vc, pose, c[0].add(-0.22, 0.10, 0.12), c[0].add(0.22, 0.10, 0.12), mouth.add(0, 0.06, 0.16), ANGLER_B);
        tri(vc, pose, c[0].add(-0.20, -0.16, 0.12), c[0].add(0.20, -0.16, 0.12), mouth.add(0, -0.02, 0.14), 0x1B1218);
        for (int k = -2; k <= 2; k++) {
            Vec3 b0 = mouth.add(k * 0.07, 0.08, -0.02);
            tri(vc, pose, b0, b0.add(0.05, 0, 0), b0.add(0.025, -0.09, 0.02), 0xF2EEDA);
        }
        // светящаяся приманка на «удочке»
        Vec3 stalk = c[0].add(0.0, 0.30, 0.10);
        Vec3 bend = stalk.add(0.0, 0.30, 0.26);
        Vec3 tip = bend.add(0.0, -0.06, 0.20);
        quad(vc, pose, stalk, stalk.add(0.02, 0, 0.02), bend.add(0.02, 0, 0.02), bend, ANGLER_T);
        quad(vc, pose, bend, bend.add(0.02, 0, 0.02), tip.add(0.02, 0, 0.02), tip, ANGLER_T);
        float g = (float) (0.06 + Math.sin(t * 0.12) * 0.02);
        Vec3 centre = tip;
        for (int k = 0; k < 6; k++) {
            double a0 = Math.PI * 2 * k / 6, a1 = Math.PI * 2 * (k + 1) / 6;
            Vec3 p0 = centre.add(Math.cos(a0) * g, Math.sin(a0) * g, 0);
            Vec3 p1 = centre.add(Math.cos(a1) * g, Math.sin(a1) * g, 0);
            tri(vc, pose, centre.add(0, 0, g), p0, p1, LURE);
            tri(vc, pose, centre.add(0, 0, -g), p1, p0, LURE_CORE);
        }
        quadEye(vc, pose, new Vec3(0.19, 0.12, 0.16), 0xF2EEDA);
        quadEye(vc, pose, new Vec3(-0.19, 0.12, 0.16), 0xF2EEDA);
        // хвост
        Vec3 tailA = c[4];
        tri(vc, pose, tailA.add(0, 0.10, 0), tailA.add(0, -0.10, 0), tailA.add(Math.sin(tail * 0.5) * 0.14, 0, -0.24), ANGLER_T);
    }

    /* ================================================================ угорь */

    private static void eel(VertexConsumer vc, PoseStack pose, double t, double tail) {
        int segs = 14;
        Vec3[] c = new Vec3[segs + 1];
        double[] r = new double[segs + 1];
        for (int i = 0; i <= segs; i++) {
            double f = i / (double) segs;
            double sway = Math.sin(tail * 0.35 - i * 0.55) * 0.16 * f;
            c[i] = new Vec3(sway, Math.sin(tail * 0.3 - i * 0.4) * 0.03, -i * 0.16);
            r[i] = 0.11 * (1.0 - f * 0.75) * (i < 2 ? 1.15 : 1.0);
        }
        for (int i = 0; i < segs; i++) ring(vc, pose, c[i], c[i + 1], r[i], r[i + 1], EEL_B, EEL_T, 0xF0EBDE);
        // спинная складка по всей длине
        for (int i = 1; i < segs - 1; i++) {
            tri(vc, pose, c[i].add(0, r[i], 0), c[i + 1].add(0, r[i + 1], 0), c[i].add(0, r[i] + 0.09, -0.05), EEL_T);
        }
        // пасть с мелкими зубами и слепые глаза
        Vec3 head = c[0];
        tri(vc, pose, head.add(-0.09, 0.02, 0.03), head.add(0.09, 0.02, 0.03), head.add(0, -0.02, 0.16), 0x2A2220);
        for (int k = -1; k <= 1; k++) {
            Vec3 b0 = head.add(k * 0.05, 0.03, 0.10);
            tri(vc, pose, b0, b0.add(0.03, 0, 0), b0.add(0.015, -0.05, 0.03), 0xF6F2E6);
        }
        quadEye(vc, pose, head.add(0.075, 0.06, 0.03), 0xC9C2B0);
        quadEye(vc, pose, head.add(-0.075, 0.06, 0.03), 0xC9C2B0);
    }

    /* ================================================================ черепаха */

    /** Мирная черепаха: плоский панцирь с шестиугольными пластинами, ласты, голова на шее. */
    private static void turtle(VertexConsumer vc, PoseStack pose, double t, double tail) {
        double shell = 0.42, len = 0.62;
        Vec3 c0 = new Vec3(0, 0, 0), c1 = new Vec3(0, 0.02, -len);
        // панцирь: 8-гранная «карапакс»-призма
        int n = 8;
        Vec3[] lo = new Vec3[n], hi = new Vec3[n], lo2 = new Vec3[n], hi2 = new Vec3[n];
        for (int k = 0; k < n; k++) {
            double a = Math.PI * 2 * k / n;
            double sx = Math.cos(a) * shell, sz = Math.sin(a) * shell * 0.9;
            lo[k] = c0.add(sx * 0.92, -0.06, sz);
            hi[k] = c0.add(sx * 0.86, 0.10, sz * 0.92);
            lo2[k] = c1.add(sx * 0.80, -0.05, sz);
            hi2[k] = c1.add(sx * 0.72, 0.08, sz * 0.92);
        }
        for (int k = 0; k < n; k++) {
            int k2 = (k + 1) % n;
            quad(vc, pose, hi[k], hi[k2], hi2[k2], hi2[k], TURTLE_T);            // верх панциря
            quad(vc, pose, lo[k], lo[k2], hi[k2], hi[k], TURTLE_B);              // борт
            quad(vc, pose, lo[k], lo[k2], lo2[k2], lo2[k], TURTLE_L);            // брюхо
        }
        // «пластины»: тёмные рёбра поверх панциря
        for (int k = 0; k < n; k += 2) {
            int k2 = (k + 1) % n;
            quad(vc, pose, hi[k], hi[k2], hi2[k2], hi2[k], TURTLE_B);
        }
        // голова на шее
        Vec3 head = c0.add(0, 0.02, 0.34);
        quad(vc, pose, head.add(-0.11, 0.09, 0.10), head.add(0.11, 0.09, 0.10), head.add(0.09, -0.07, 0.10), head.add(-0.09, -0.07, 0.10), TURTLE_S);
        quad(vc, pose, head.add(-0.09, 0.07, 0.10), head.add(0.09, 0.07, 0.10), head.add(0.07, -0.05, 0.10), head.add(-0.07, -0.05, 0.10), TURTLE_L);
        quadEye(vc, pose, head.add(0.10, 0.05, 0.10), EYE);
        quadEye(vc, pose, head.add(-0.10, 0.05, 0.10), EYE);
        // ласты, гребут в противофазе
        for (int s2 = -1; s2 <= 1; s2 += 2) {
            double flap = Math.sin(tail * 0.30 + (s2 > 0 ? 0 : Math.PI)) * 0.16;
            Vec3 f0 = c0.add(s2 * shell * 0.9, -0.03, 0.10);
            Vec3 f1 = f0.add(s2 * 0.30, flap, 0.14);
            quad(vc, pose, f0, f1, f1.add(0, -0.02, 0.26), f0.add(0, -0.01, 0.24), TURTLE_L);
            Vec3 b0 = c1.add(s2 * shell * 0.8, -0.03, -0.02);
            Vec3 b1 = b0.add(s2 * 0.20, -flap, -0.10);
            quad(vc, pose, b0, b1, b1.add(0, -0.02, -0.16), b0.add(0, -0.01, -0.16), TURTLE_L);
        }
    }

    /* ================================================================ глубинный глот */

    /** «Злая и редкая» рыба глубин: раздутое тело, огромная пасть с зубами и призрачное свечение. */
    private static void gloom(VertexConsumer vc, PoseStack pose, double t, double tail) {
        int segs = 6;
        double len = 0.9, r0 = 0.24;
        Vec3[] c = new Vec3[segs + 1];
        double[] r = new double[segs + 1];
        for (int i = 0; i <= segs; i++) {
            double f = i / (double) segs;
            c[i] = new Vec3(Math.sin(tail * 0.30 - i * 0.5) * 0.05 * f, 0, i * (len / segs) * -1.0);
            r[i] = r0 * (0.55 + 0.75 * Math.sin(Math.PI * (0.22 + f * 0.72)));
        }
        for (int i = 0; i < segs; i++) ring(vc, pose, c[i], c[i + 1], r[i], r[i + 1], GLOOM_B, GLOOM_T, GLOOM_F);
        // пасть: разинута, зубы в два ряда
        Vec3 jawTop = c[0].add(0, 0.16, 0.20), jawBot = c[0].add(0, -0.16, 0.16);
        tri(vc, pose, c[0].add(-0.24, 0.06, 0.02), c[0].add(0.24, 0.06, 0.02), jawTop, GLOOM_T);
        tri(vc, pose, c[0].add(-0.22, -0.06, 0.02), c[0].add(0.22, -0.06, 0.02), jawBot, GLOOM_F);
        for (int k = -2; k <= 2; k++) {
            Vec3 b0 = c[0].add(k * 0.085, 0.05, 0.06);
            tri(vc, pose, b0, b0.add(0.05, 0, 0), b0.add(0.025, 0.13, 0.14), 0xF4F0E2);
            Vec3 b1 = c[0].add(k * 0.08, -0.05, 0.06);
            tri(vc, pose, b1, b1.add(0.045, 0, 0), b1.add(0.02, -0.11, 0.11), 0xEDE8D8);
        }
        // светящиеся призрачные полосы по бокам (биолюминесценция)
        for (int k = 0; k < 4; k++) {
            float pulse = (float) (0.6 + 0.4 * Math.sin(t * 0.08 + k));
            int col = shade(GLOOM_L, pulse);
            Vec3 p0 = c[1].add(0, r[1] * 0.2, -k * 0.18);
            quad(vc, pose, p0.add(-r[1], 0, 0), p0.add(-r[1] * 0.2, 0, 0), p0.add(-r[1] * 0.2, 0, -0.10), p0.add(-r[1], 0, -0.10), col);
            quad(vc, pose, p0.add(r[1], 0, 0), p0.add(r[1] * 0.2, 0, 0), p0.add(r[1] * 0.2, 0, -0.10), p0.add(r[1], 0, -0.10), col);
        }
        // удочка-приманка над головой
        Vec3 stalk = c[0].add(0, 0.28, -0.04), tip = stalk.add(0, 0.20, 0.26);
        quad(vc, pose, stalk, stalk.add(0.02, 0, 0.02), tip.add(0.02, 0, 0.02), tip, GLOOM_T);
        float g = (float) (0.05 + Math.sin(t * 0.16) * 0.015);
        for (int k = 0; k < 4; k++) {
            double a0 = Math.PI * 2 * k / 4, a1 = Math.PI * 2 * (k + 1) / 4;
            tri(vc, pose, tip.add(0, 0, g), tip.add(Math.cos(a0) * g, Math.sin(a0) * g, 0), tip.add(Math.cos(a1) * g, Math.sin(a1) * g, 0), GLOOM_L);
        }
        // хвост-веер
        Vec3 tailA = c[segs];
        tri(vc, pose, tailA.add(0, r[segs] * 0.8, 0), tailA.add(0, -r[segs] * 0.8, 0), tailA.add(0, 0, -0.30), GLOOM_F);
    }

    /* ================================================================ жгучая медуза */

    /** «Злая и редкая» медуза: фиолетовый купол с бахромой, ядовитые щупальца вьются. */
    private static void sting(VertexConsumer vc, PoseStack pose, double t) {
        int n = 8;
        double rad = 0.34, h = 0.26;
        Vec3 apex = new Vec3(0, h, 0);
        for (int k = 0; k < n; k++) {
            double a0 = Math.PI * 2 * k / n, a1 = Math.PI * 2 * (k + 1) / n;
            Vec3 p0 = new Vec3(Math.cos(a0) * rad, 0, Math.sin(a0) * rad);
            Vec3 p1 = new Vec3(Math.cos(a1) * rad, 0, Math.sin(a1) * rad);
            tri(vc, pose, apex, p0, p1, STING_B);
            // бахрома по краю купола
            Vec3 m = p0.add(p1).scale(0.5).add(0, -0.02, 0);
            Vec3 inner = m.scale(0.7).add(0, -0.06, 0);
            tri(vc, pose, p0, p1, inner, STING_L);
            // светящееся кольцо
            tri(vc, pose, p0.add(0, 0.03, 0), p1.add(0, 0.03, 0), m.add(0, 0.03, 0).scale(0.86), STING_C);
        }
        // щупальца
        for (int k = 0; k < 7; k++) {
            double a = Math.PI * 2 * k / 7;
            double x0 = Math.cos(a) * rad * 0.8, z0 = Math.sin(a) * rad * 0.8;
            Vec3 prev = new Vec3(x0, -0.02, z0);
            for (int s = 1; s <= 5; s++) {
                double sway = Math.sin(t * 0.12 + k + s * 0.6) * 0.05;
                Vec3 next = new Vec3(x0 * (1 - s * 0.08) + sway, -0.02 - s * 0.15, z0 * (1 - s * 0.08) + sway * 0.6);
                quad(vc, pose, prev, next, next.add(0.025, 0, 0), prev.add(0.025, 0, 0), s % 2 == 0 ? STING_L : STING_B);
                prev = next;
            }
        }
    }

    /* ================================================================ примитивы */

    /** Треугольник с обеих сторон (debugQuads отсекает обратные грани — рыба должна быть видна с любого ракурса). */
    private static void tri(VertexConsumer vc, PoseStack pose, Vec3 a, Vec3 b, Vec3 c, int col) {
        vertex(vc, pose, a, col);
        vertex(vc, pose, b, col);
        vertex(vc, pose, c, col);
        vertex(vc, pose, c, col);
        vertex(vc, pose, c, shade(col, 0.82));
        vertex(vc, pose, b, shade(col, 0.82));
        vertex(vc, pose, a, shade(col, 0.82));
        vertex(vc, pose, a, shade(col, 0.82));
    }

    private static void quad(VertexConsumer vc, PoseStack pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int col) {
        vertex(vc, pose, a, col);
        vertex(vc, pose, b, col);
        vertex(vc, pose, c, col);
        vertex(vc, pose, d, col);
        vertex(vc, pose, d, shade(col, 0.82));
        vertex(vc, pose, c, shade(col, 0.82));
        vertex(vc, pose, b, shade(col, 0.82));
        vertex(vc, pose, a, shade(col, 0.82));
    }

    /** Затемнить/подсветить цвет (изнанка). */
    private static int shade(int col, double k) {
        int r = (int) Math.min(255, ((col >> 16) & 0xFF) * k);
        int g = (int) Math.min(255, ((col >> 8) & 0xFF) * k);
        int b = (int) Math.min(255, (col & 0xFF) * k);
        return (r << 16) | (g << 8) | b;
    }

    /** Маленький глазик — квадратик, повёрнутый к камере под небольшим углом. */
    private static void quadEye(VertexConsumer vc, PoseStack pose, Vec3 p, int col) {
        double s = 0.035;
        quad(vc, pose, p.add(0, s, s), p.add(0, s, -s), p.add(0, -s, -s), p.add(0, -s, s), col);
        quad(vc, pose, p.add(0, -s, s), p.add(0, -s, -s), p.add(0, s, -s), p.add(0, s, s), col);
    }

    private static void vertex(VertexConsumer vc, PoseStack pose, Vec3 p, int col) {
        var m = pose.last().pose();
        vc.vertex(m, (float) p.x, (float) p.y, (float) p.z)
                .color((col >> 16) & 0xFF, (col >> 8) & 0xFF, col & 0xFF, 255)
                .endVertex();
    }
}
