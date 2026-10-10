package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.entity.PaperDragonEntity;

/**
 * Рев 5: реалистичный модульный оригами-дракон (по референсу-фото): тело - цепь сложенных треугольных бумажных
 * модулей (пирамиды), зубчатый спинной хребет из стоячих треугольных складок, бумажная голова с отогнутыми рогами,
 * открытой пастью и глазами-чернилами, две пары маленьких лапок с когтями, тонкий хвост с алой меткой.
 * Всё процедурно: фальшивый свет по нормали каждого треугольника = объёмные складки без освещения движка.
 */
public class PaperDragonRenderer extends EntityRenderer<PaperDragonEntity> {
    private static final int PAPER = 0xF4F0E4, PAPER_D = 0xD8D3C2, PAPER_L = 0xFBF8EE, INK = 0x33343E, RED = 0xD22E28;
    private static final int SEGS = 15;
    private static final double STEP = 0.55;
    /** фальшевый свет: как дневное солнце сверху-сбоку */
    private static final Vec3 LIGHT = new Vec3(0.35, 0.86, 0.30).normalize();

    public PaperDragonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(PaperDragonEntity e, float yaw, float partial, PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buf, int light) {
        pose.pushPose();
        VertexConsumer vc = buf.getBuffer(RenderType.debugQuads());
        double t = e.tickCount + partial;
        float yRot = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
        float xRot = Mth.lerp(partial, e.xRotO, e.getXRot());
        float bank = Mth.lerp(partial, e.bank0, e.bank);
        double f = Mth.lerp(partial, e.flap0, e.flap);

        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - yRot));
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(xRot));
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(bank));                   // крен в повороте - "реалистичные выходы"

        // позвоночник: S-образная волна, нарастает к хвосту; хребет чуть дышит волной взмаха
        Vec3[] c = new Vec3[SEGS + 1];
        for (int i = 0; i <= SEGS; ++i) {
            double grow = Math.min(1.0, 0.15 + i * 0.16);
            double lat = Math.sin(t * 0.10 - i * 0.52) * 0.16 * (0.3 + grow * 1.4);
            double lift = Math.sin(t * 0.13 - i * 0.45) * 0.15 * grow
                    + Math.sin(f * 6.283 - i * 0.75) * 0.05 * grow;
            if (i > SEGS - 3) lift += Math.sin(t * 0.07) * (i - SEGS + 3) * 0.10;        // хвостовой завиток
            c[i] = new Vec3(lat, lift, -i * STEP);
        }
        // ширина/высота сегментов: тонкая морда - плечи - постепенное сужение к хвосту
        double[] w = new double[SEGS + 1];
        for (int i = 0; i <= SEGS; ++i) {
            double base = i < 3 ? 0.16 + i * 0.09 : Math.max(0.04, 0.34 * (1.0 - (i - 3) / (double) (SEGS - 1)));
            w[i] = base;
        }
        // тело: цепь бумажных пирамид-модулей
        for (int i = 0; i < SEGS; ++i) module(vc, pose, c[i], c[i + 1], w[i], w[i + 1], i);
        // спинные складки-плавники между третьими модулями
        for (int i = 1; i < SEGS - 1; ++i) ridge(vc, pose, c, w, i, t);
        head(vc, pose, c, w, t, f);
        legs(vc, pose, c, w, t, f);
        tailTag(vc, pose, c, w, t);
        pose.popPose();
    }

    /* ------------------------------------------------ геометрия модуля */

    /** Бумажный модуль между двумя точками позвоночника: 4 пирамиды (верх/низ/лево/право), фальш-свет по нормалям. */
    private static void module(VertexConsumer vc, PoseStack pose, Vec3 a, Vec3 b, double wa, double wb, int i) {
        double wgt = 1.0;
        int top = shade(PAPER, wgt), topD = shade(PAPER_D, wgt), side = shade(PAPER, 0.92), under = shade(PAPER_D, 0.86);
        Vec3 mid = a.add(b).scale(0.5);
        double wm = (wa + wb) * 0.5;
        Vec3 up = new Vec3(0, wm * 0.85, 0), dn = new Vec3(0, -wm * 0.85, 0);
        Vec3 sideV = side(a, b).scale(wm);
        Vec3 aL = a.subtract(sideV), aR = a.add(sideV), bL = b.subtract(sideV), bR = b.add(sideV);
        Vec3 aT = a.add(up), bT = b.add(up), aB = a.add(dn), bB = b.add(dn);
        // верхняя пирамидка (главная световая грань)
        Vec3 apex = mid.add(up.scale(0.55));
        tri(vc, pose, aT, bT, apex, top);
        // нижняя
        Vec3 apexB = mid.add(dn.scale(0.55));
        tri(vc, pose, bB, aB, apexB, under);
        // боковые
        Vec3 apexL = mid.subtract(sideV.scale(0.5));
        tri(vc, pose, bL, aL, apexL, i % 2 == 0 ? side : topD);
        Vec3 apexR = mid.add(sideV.scale(0.5));
        tri(vc, pose, aR, bR, apexR, i % 2 == 0 ? topD : side);
        // торцевые грани кольца (между сегментами почти не видны)
        tri(vc, pose, aT, aL, aB, topD);
        tri(vc, pose, aT, aB, aR, topD);
    }

    /** Стоячий треугольный плавник-сгиб на хребте (как на фото: зубчатый гребень). */
    private static void ridge(VertexConsumer vc, PoseStack pose, Vec3[] c, double[] w, int i, double t) {
        if (i < 2 || i > SEGS - 2) return;
        double wave = Math.sin(t * 0.10 - i * 0.55) * 0.05;
        Vec3 base = c[i].add(0, w[i] * 0.80, 0);
        Vec3 next = c[i + 1].add(0, w[i + 1] * 0.80, 0);
        Vec3 tip = base.add(next).scale(0.5).add(new Vec3(wave, w[i] * 1.15, -w[i] * 0.45));
        int col = i % 2 == 0 ? PAPER_L : PAPER;
        tri(vc, pose, base, next, tip, col);
    }

    /* ------------------------------------------------ голова оригами */

    private static void head(VertexConsumer vc, PoseStack pose, Vec3[] c, double[] w, double t, double f) {
        Vec3 h = c[0];
        // челюсть мягко открывается-закрывается на взмахе
        double jaw = 0.02 + Math.sin(f * 6.283) * 0.01;
        // череп: два скрещенных модуля поперёк спины, морда вытянута вперёд
        double hw = w[0] * 1.15;
        Vec3 snout = h.add(0, 0.0, hw * 1.9);                       // кончик морды
        Vec3 snoutD = h.add(0, -hw * 0.35 - jaw * 2, hw * 1.6);     // нижняя челюсть
        Vec3 topL = h.add(new Vec3(-hw * 0.7, hw * 0.75, -hw * 0.2));
        Vec3 topR = h.add(new Vec3(hw * 0.7, hw * 0.75, -hw * 0.2));
        Vec3 botL = h.add(new Vec3(-hw * 0.7, -hw * 0.4, -hw * 0.2));
        Vec3 botR = h.add(new Vec3(hw * 0.7, -hw * 0.4, -hw * 0.2));
        // верх морды: 2 треугольника от бровей к носу
        tri(vc, pose, topL, topR, snout, PAPER_L);
        // щёки
        tri(vc, pose, topL, botL, snout, PAPER);
        tri(vc, pose, botR, topR, snout, PAPER_D);
        // нижняя челюсть (открыта чуть)
        tri(vc, pose, botL, botR, snoutD, shade(PAPER_D, 0.9));
        tri(vc, pose, botR, botL, snoutD, shade(PAPER_D, 0.85));
        // горло тёмное в пасти
        tri(vc, pose, botL, botR, h.add(0, -hw * 0.15, hw * 0.7), INK);
        // глаза-чернила на боках
        quad(vc, pose, topL.x - 0.001, topL.y - hw * 0.18, topL.z + hw * 0.5, topL.x - 0.001, topL.y - hw * 0.30, topL.z + hw * 0.72,
                topL.x - 0.001, topL.y - hw * 0.34, topL.z + hw * 0.86, topL.x - 0.001, topL.y - hw * 0.24, topL.z + hw * 0.95, INK);
        quad(vc, pose, topR.x + 0.001, topR.y - hw * 0.18, topR.z + hw * 0.95, topR.x + 0.001, topR.y - hw * 0.24, topR.z + hw * 0.86,
                topR.x + 0.001, topR.y - hw * 0.34, topR.z + hw * 0.72, topR.x + 0.001, topR.y - hw * 0.30, topR.z + hw * 0.5, INK);
        // рога: два сгибающихся назад треугольных луча с чернильными кончиками (над затылком)
        for (int s = -1; s <= 1; s += 2) {
            Vec3 horn0 = h.add(new Vec3(s * hw * 0.55, hw * 0.75, -hw * 0.25));
            Vec3 horn1 = horn0.add(new Vec3(s * hw * 0.35, hw * 0.55, -hw * 1.0));
            Vec3 hornT = horn1.add(new Vec3(s * hw * 0.15, hw * 0.05, -hw * 0.9));
            tri(vc, pose, horn0, horn1, hornT, PAPER);
            tri(vc, pose, horn1, hornT, horn1.add(0, hw * 0.3, -hw * 0.2), INK);   // чернильный кончик
        }
        // уши-складки за рогами (как на фото - два бумажных крыла-уха)
        for (int s = -1; s <= 1; s += 2) {
            Vec3 ear0 = h.add(new Vec3(s * hw * 0.75, hw * 0.55, -hw * 0.35));
            Vec3 earT = ear0.add(new Vec3(s * hw * 0.75, hw * 0.15, -hw * 0.9));
            Vec3 ear1 = ear0.add(new Vec3(s * hw * 0.2, -hw * 0.25, -hw * 1.1));
            tri(vc, pose, ear0, ear1, earT, PAPER_D);
        }
        // усы: тонкие чернильные полоски вперёд-вниз
        for (int s = -1; s <= 1; s += 2) {
            Vec3 w0 = h.add(new Vec3(s * hw * 0.35, -hw * 0.05, hw * 1.3));
            Vec3 w1 = w0.add(new Vec3(s * hw * 1.3, -hw * 0.4, hw * 1.0));
            whisker(vc, pose, w0.x, w0.y, w0.z, w1.x, w1.y, w1.z, INK);
        }
    }

    /* ------------------------------------------------ лапки */

    /** Две пары крошечных лапок (как на фото): бедро+голень+веер из трёх когтей; мягкое гребком в полёте. */
    private static void legs(VertexConsumer vc, PoseStack pose, Vec3[] c, double[] w, double t, double f) {
        int[] segIdx = {3, 8};
        for (int li = 0; li < 2; ++li) {
            int i = segIdx[li];
            double paddle = Math.sin(f * 6.283 + li * 2.2) * 0.35;                      // гребок лапами
            for (int s = -1; s <= 1; s += 2) {
                Vec3 hip = c[i].add(new Vec3(s * w[i] * 0.9, -w[i] * 0.4, 0));
                Vec3 knee = hip.add(new Vec3(s * w[i] * 1.1, -w[i] * 1.15, paddle * w[i] * 1.4));
                Vec3 foot = knee.add(new Vec3(s * w[i] * 0.35, -w[i] * 0.85, w[i] * 0.55 + paddle * w[i]));
                // бедро+голень: 2 треугольные складки
                tri(vc, pose, hip, knee, hip.add(new Vec3(s * 0.03, 0.02, 0.05)), PAPER);
                tri(vc, pose, knee, foot, knee.add(new Vec3(s * 0.03, 0.01, 0.05)), PAPER_D);
                // когти: веер из 3 мелких треугольников
                for (int k = -1; k <= 1; ++k) {
                    Vec3 clawT = foot.add(new Vec3(s * (0.02 + k * 0.025), -w[i] * 0.42, 0.05 + Math.abs(k) * 0.03));
                    tri(vc, pose, foot, foot.add(new Vec3(s * 0.015, -0.02, -0.01)), clawT, PAPER_L);
                }
            }
        }
    }

    /* ------------------------------------------------ хвостовая метка */

    private static void tailTag(VertexConsumer vc, PoseStack pose, Vec3[] c, double[] w, double t) {
        Vec3 tip = c[SEGS];
        // тонкое хвостовое сопло
        Vec3 end = tip.add(new Vec3(Math.sin(t * 0.10 + 7.8) * 0.10, Math.sin(t * 0.05) * 0.06, -STEP * 1.2));
        tri(vc, pose, tip.add(0, w[SEGS], 0), tip.subtract(0, w[SEGS], 0), end, PAPER_D);
        // алая метка: маленькая бумажная полоска трепещет
        double flap = Math.sin(t * 0.45) * 0.10;
        Vec3 tag0 = end.add(new Vec3(0, 0, 0));
        Vec3 tag1 = end.add(new Vec3(0.0, 0.03 + flap, -0.22));
        quad(vc, pose, tag0.x - 0.035, tag0.y, tag0.z, tag0.x + 0.035, tag0.y, tag0.z, tag1.x + 0.035, tag1.y, tag1.z, tag1.x - 0.035, tag1.y, tag1.z, RED);
        quad(vc, pose, tag1.x - 0.035, tag1.y - 0.03, tag1.z, tag1.x + 0.035, tag1.y - 0.03, tag1.z, tag0.x + 0.035, tag0.y, tag0.z, tag0.x - 0.035, tag0.y, tag0.z, shade(RED, 0.85));
    }

    /* ------------------------------------------------ примитивы */

    private static Vec3 side(Vec3 a, Vec3 b) {
        Vec3 d = b.subtract(a);
        Vec3 s = new Vec3(-d.z, 0, d.x).normalize();
        if (s.lengthSqr() < 1e-8) s = new Vec3(1, 0, 0);
        return s;
    }

    /** Треугольник, двусторонняя отрисовка (внутренняя сторона темнее - бумага). Фальш-свет по нормали. */
    private static void tri(VertexConsumer vc, PoseStack pose, Vec3 p1, Vec3 p2, Vec3 p3, int color) {
        Vec3 n = p2.subtract(p1).cross(p3.subtract(p1));
        if (n.lengthSqr() < 1e-10) return;
        n = n.normalize();
        double k = 0.60 + 0.40 * Math.max(0, n.dot(LIGHT));
        int col = shade(color, k);
        putQuad(vc, pose, p1, p2, p3, p3, col);
        int colB = shade(color, 0.70);                                                  // изнанка чуть темнее
        putQuad(vc, pose, p3, p2, p1, p1, colB);
    }

    private static void quad(VertexConsumer vc, PoseStack pose,
                             double x0, double y0, double z0, double x1, double y1, double z1,
                             double x2, double y2, double z2, double x3, double y3, double z3, int color) {
        putQuad(vc, pose, new Vec3(x0, y0, z0), new Vec3(x1, y1, z1), new Vec3(x2, y2, z2), new Vec3(x3, y3, z3), color);
    }

    private static void putQuad(VertexConsumer vc, PoseStack pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
        float r = ((color >> 16) & 255) / 255f, g = ((color >> 8) & 255) / 255f, bl = (color & 255) / 255f;
        var m = pose.last().pose();
        vc.vertex(m, (float) a.x, (float) a.y, (float) a.z).color(r, g, bl, 1).normal(pose.last().normal(), 0, 1, 0).endVertex();
        vc.vertex(m, (float) b.x, (float) b.y, (float) b.z).color(r, g, bl, 1).normal(pose.last().normal(), 0, 1, 0).endVertex();
        vc.vertex(m, (float) c.x, (float) c.y, (float) c.z).color(r, g, bl, 1).normal(pose.last().normal(), 0, 1, 0).endVertex();
        vc.vertex(m, (float) d.x, (float) d.y, (float) d.z).color(r, g, bl, 1).normal(pose.last().normal(), 0, 1, 0).endVertex();
    }

    private static void whisker(VertexConsumer vc, PoseStack pose, double x0, double y0, double z0, double x1, double y1, double z1, int color) {
        double w = 0.018;
        quad(vc, pose, x0 - w, y0, z0, x0 + w, y0, z0, x1 + w, y1, z1, x1 - w, y1, z1, color);
        quad(vc, pose, x1 - w, y1, z1, x1 + w, y1, z1, x0 + w, y0, z0, x0 - w, y0, z0, shade(color, 0.9));
    }

    private static int shade(int color, double k) {
        int r = (int) Math.min(255, ((color >> 16) & 255) * k);
        int g = (int) Math.min(255, ((color >> 8) & 255) * k);
        int b = (int) Math.min(255, (color & 255) * k);
        return r << 16 | g << 8 | b;
    }

    @Override
    public ResourceLocation getTextureLocation(PaperDragonEntity e) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
