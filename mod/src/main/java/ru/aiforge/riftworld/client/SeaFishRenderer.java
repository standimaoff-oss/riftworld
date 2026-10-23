package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
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

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("riftworld/seafish");
    private static int seen;

    public SeaFishRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(SeaFishEntity e) {
        return new ResourceLocation("minecraft", "textures/misc/white.png");
    }

    @Override
    public void render(SeaFishEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
        int n = seen++;
        if (n < 3 || n % 2000 == 0) {
            LOG.info("[sea_fish] render #{} variant={} pos={} light={} yaw={} dist={}", n, e.variant(),
                    e.blockPosition(), light, yaw, e.distanceToSqr(Minecraft.getInstance().player == null
                            ? e.position() : Minecraft.getInstance().player.position()));
        }
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
