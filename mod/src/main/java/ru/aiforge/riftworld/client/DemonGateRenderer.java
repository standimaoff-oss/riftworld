package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import ru.aiforge.riftworld.block.DemonGateBE;

/**
 * ЧЕРНЫЙ ПРОЛОМ ПРОСТРАНСТВА (Уникальный пространственный разлом — НЕ ИЗ БЛОКОВ):
 * A majestic, free-floating, organic 3D rift in the fabric of space:
 * - Soaring vertical organic spatial tear (~4.5 blocks high, ~2.5 blocks wide) with jagged torn edges.
 * - Pitch-black void singularity core that swallows light and occludes reality.
 * - 3D tilted accretion rings orbiting the fissure at complex spatial angles.
 * - Restless burning plasma rim with animated harmonic turbulence.
 * - Branching 3D spatial lightning fractures tearing through the cathedral air.
 * - Orbiting, tumbling 3D crystalline reality shards being drawn into the event horizon.
 */
public class DemonGateRenderer implements BlockEntityRenderer<DemonGateBE> {
    private static final int TEAR_SEGS = 48;
    private static final int ACCRETION_SEGS = 36;
    private static final int CRACK_COUNT = 14;
    private static final int SHARD_COUNT = 24;

    public DemonGateRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(DemonGateBE be, float partial, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null) return;
        double time = be.getLevel().getGameTime() + partial;
        long seed = be.getBlockPos().asLong();
        double phase = ((seed * 0x9E3779B97F4A7C15L) & 0xFFFF) / 65535.0 * Math.PI * 2.0;

        ps.pushPose();
        ps.translate(0.5, 1.2, 0.5); // elevate to hover freely in mid-air above the pedestal

        VertexConsumer vcSolid = buffers.getBuffer(RenderType.debugQuads());
        VertexConsumer vcGlow = buffers.getBuffer(RenderType.lightning());
        Matrix4f m = ps.last().pose();

        // 1. ORGANIC JAGGED SPATIAL TEAR (Вертикальный пролом пространства)
        // Both south-facing and north-facing double-sided rendering
        drawSpatialTear(vcSolid, vcGlow, m, time, phase, 1.0);
        drawSpatialTear(vcSolid, vcGlow, m, time, phase, -1.0);

        // 2. 3D ACCRETION RINGS (Кольца космического горизонта, наклоненные в 3D)
        ps.pushPose();
        ps.mulPose(Axis.XP.rotationDegrees(28.0F));
        ps.mulPose(Axis.YP.rotationDegrees((float) (time * 1.8 + phase * 10.0)));
        Matrix4f rm1 = ps.last().pose();
        drawAccretionRing(vcGlow, rm1, 1.8, 2.2, 0.04, time, 40, 15, 80, 180);
        ps.popPose();

        ps.pushPose();
        ps.mulPose(Axis.ZP.rotationDegrees(-35.0F));
        ps.mulPose(Axis.YP.rotationDegrees((float) (-time * 2.4 - phase * 15.0)));
        Matrix4f rm2 = ps.last().pose();
        drawAccretionRing(vcGlow, rm2, 1.3, 1.6, 0.03, time * 1.3, 80, 20, 140, 200);
        ps.popPose();

        // 3. JAGGED 3D SPATIAL LIGHTNING FRACTURES (Молнии разлома в 3D)
        drawSpatialCracks(vcGlow, m, time, phase);

        // 4. ORBITING 3D SHATTERS OF REALITY (Падающие осколки реальности)
        drawRealityShards(vcGlow, ps, time, phase);

        ps.popPose();
    }

    /** Computes the organic, jagged boundary radius of the spatial tear at angle theta. */
    private static double getTearRadius(double theta, double time, double phase) {
        // Vertical eye / lancet profile: tall along Y (theta = +/- PI/2), narrower along X (theta = 0, PI)
        double cos = Math.cos(theta);
        double sin = Math.sin(theta);
        // Base profile: vertical tear (height ~ 2.2m from center = 4.4m total; width ~ 1.1m from center)
        double base = Math.sqrt(0.85 * cos * cos + 4.2 * sin * sin);

        // Harmonic turbulence ripples
        double turb = 0.12 * Math.sin(theta * 5.0 + time * 0.15 + phase)
                + 0.07 * Math.sin(theta * 9.0 - time * 0.28)
                + 0.04 * Math.cos(theta * 13.0 + time * 0.42);

        return Math.max(0.2, base + turb);
    }

    private static void drawSpatialTear(VertexConsumer vcSolid, VertexConsumer vcGlow, Matrix4f m,
                                        double time, double phase, double dir) {
        double zOff = dir * 0.02;

        // A. PITCH-BLACK VOID SINGULARITY (Центральная бездна)
        for (int i = 0; i < TEAR_SEGS; ++i) {
            double a0 = i * Math.PI * 2.0 / TEAR_SEGS;
            double a1 = (i + 1) * Math.PI * 2.0 / TEAR_SEGS;

            double r0 = getTearRadius(a0, time, phase) * 0.85;
            double r1 = getTearRadius(a1, time, phase) * 0.85;

            double x0 = r0 * Math.cos(a0), y0 = r0 * Math.sin(a0);
            double x1 = r1 * Math.cos(a1), y1 = r1 * Math.sin(a1);

            // Absolute black void core
            quad(vcSolid, m,
                    0, 0, zOff,
                    x0, y0, zOff,
                    x1, y1, zOff,
                    0, 0, zOff,
                    2, 0, 5, 255);
        }

        // B. BURNING FLARING EVENT-HORIZON RIM (Горящая кромка)
        for (int i = 0; i < TEAR_SEGS; ++i) {
            double a0 = i * Math.PI * 2.0 / TEAR_SEGS;
            double a1 = (i + 1) * Math.PI * 2.0 / TEAR_SEGS;

            double rCore0 = getTearRadius(a0, time, phase) * 0.82;
            double rCore1 = getTearRadius(a1, time, phase) * 0.82;
            double rRim0 = getTearRadius(a0, time, phase) * 1.15;
            double rRim1 = getTearRadius(a1, time, phase) * 1.15;

            double x0 = rCore0 * Math.cos(a0), y0 = rCore0 * Math.sin(a0);
            double x1 = rCore1 * Math.cos(a1), y1 = rCore1 * Math.sin(a1);
            double x2 = rRim1 * Math.cos(a1), y2 = rRim1 * Math.sin(a1);
            double x3 = rRim0 * Math.cos(a0), y3 = rRim0 * Math.sin(a0);

            double flick = 0.5 + 0.5 * Math.sin(a0 * 7.0 + time * 0.35 + phase);
            int r = (int) (140 + 100 * flick);
            int g = (int) (30 + 80 * flick);
            int b = (int) (190 + 65 * flick);
            int al = (int) (180 + 70 * flick);

            quad(vcGlow, m,
                    x0, y0, zOff + dir * 0.01,
                    x1, y1, zOff + dir * 0.01,
                    x2, y2, zOff + dir * 0.01,
                    x3, y3, zOff + dir * 0.01,
                    r, g, b, al);
        }
    }

    private static void drawAccretionRing(VertexConsumer vc, Matrix4f m, double r0, double r1, double h,
                                          double time, int cr, int cg, int cb, int al) {
        for (int i = 0; i < ACCRETION_SEGS; ++i) {
            double a0 = i * Math.PI * 2.0 / ACCRETION_SEGS;
            double a1 = (i + 1) * Math.PI * 2.0 / ACCRETION_SEGS;

            double c0 = Math.cos(a0), s0 = Math.sin(a0);
            double c1 = Math.cos(a1), s1 = Math.sin(a1);

            double wob0 = Math.sin(a0 * 3.0 + time * 0.2) * 0.04;
            double wob1 = Math.sin(a1 * 3.0 + time * 0.2) * 0.04;

            quad(vc, m,
                    r0 * c0, wob0 - h, r0 * s0,
                    r1 * c0, wob0 + h, r1 * s0,
                    r1 * c1, wob1 + h, r1 * s1,
                    r0 * c1, wob1 - h, r0 * s1,
                    cr, cg, cb, al);
        }
    }

    private static void drawSpatialCracks(VertexConsumer vc, Matrix4f m, double time, double phase) {
        for (int c = 0; c < CRACK_COUNT; ++c) {
            double baseAngle = c * Math.PI * 2.0 / CRACK_COUNT + phase * 0.5;
            double tearR = getTearRadius(baseAngle, time, phase);

            double cx = tearR * Math.cos(baseAngle);
            double cy = tearR * Math.sin(baseAngle);
            double cz = 0;

            int segments = 4;
            double len = 0.35 + 0.25 * Math.sin(c * 4.3 + time * 0.2);
            double segLen = len / segments;

            double curX = cx, curY = cy, curZ = cz;
            for (int s = 0; s < segments; ++s) {
                double branchAngle = baseAngle + Math.sin(s * 3.1 + c * 5.7 + time * 0.4) * 0.45;
                double zZig = Math.cos(s * 2.8 + c * 3.3 + time * 0.3) * 0.25;

                double nextX = curX + Math.cos(branchAngle) * segLen;
                double nextY = curY + Math.sin(branchAngle) * segLen;
                double nextZ = curZ + zZig * segLen;

                double w = 0.04 * (1.0 - s / (double) segments);
                quad(vc, m,
                        curX - w, curY, curZ,
                        curX + w, curY, curZ,
                        nextX + w, nextY, nextZ,
                        nextX - w, nextY, nextZ,
                        210, 180, 255, 220);
                quad(vc, m,
                        curX, curY - w, curZ,
                        curX, curY + w, curZ,
                        nextX, nextY + w, nextZ,
                        nextX, nextY - w, nextZ,
                        255, 230, 255, 240);

                curX = nextX;
                curY = nextY;
                curZ = nextZ;
            }
        }
    }

    private static void drawRealityShards(VertexConsumer vc, PoseStack ps, double time, double phase) {
        for (int i = 0; i < SHARD_COUNT; ++i) {
            double speed = 0.035 + (i % 6) * 0.012;
            double cycle = (time * speed + i * 0.17 + phase) % 1.0;
            double orbitR = 0.4 + 2.4 * (1.0 - cycle); // falls towards singularity
            double angle = i * 2.1 + time * 0.04 * (1.0 + cycle);
            double yPos = (Math.sin(i * 3.7 + time * 0.05) * 1.5) * (1.0 - cycle * 0.4);
            double zPos = Math.sin(angle * 2.0 + i) * 0.45;

            double sx = orbitR * Math.cos(angle);
            double sz = orbitR * Math.sin(angle) * 0.5 + zPos;

            ps.pushPose();
            ps.translate(sx, yPos, sz);
            ps.mulPose(Axis.XP.rotationDegrees((float) (time * 2.5 + i * 40.0)));
            ps.mulPose(Axis.YP.rotationDegrees((float) (time * 3.1 + i * 60.0)));

            Matrix4f sm = ps.last().pose();
            float szModel = (float) (0.05 + 0.035 * (1.0 - cycle));

            int sr = (i % 3 == 0) ? 255 : ((i % 3 == 1) ? 140 : 80);
            int sg = (i % 3 == 0) ? 220 : ((i % 3 == 1) ? 60 : 200);
            int sb = 255;
            int sal = (int) (200 * (1.0 - cycle * 0.3));

            drawShardBox(vc, sm, szModel, sr, sg, sb, sal);
            ps.popPose();
        }
    }

    private static void drawShardBox(VertexConsumer vc, Matrix4f m, float s, int r, int g, int b, int a) {
        float h = s * 1.8F;
        // 4 triangular facets
        quad(vc, m, 0, h, 0, s, 0, 0, 0, 0, s, 0, h, 0, r, g, b, a);
        quad(vc, m, 0, h, 0, 0, 0, s, -s, 0, 0, 0, h, 0, (int) (r * 0.8), (int) (g * 0.8), (int) (b * 0.8), a);
        quad(vc, m, 0, h, 0, -s, 0, 0, 0, 0, -s, 0, h, 0, r, g, b, a);
        quad(vc, m, 0, h, 0, 0, 0, -s, s, 0, 0, 0, h, 0, (int) (r * 0.8), (int) (g * 0.8), (int) (b * 0.8), a);

        quad(vc, m, 0, -h, 0, 0, 0, s, s, 0, 0, 0, -h, 0, (int) (r * 0.7), (int) (g * 0.7), (int) (b * 0.7), a);
        quad(vc, m, 0, -h, 0, -s, 0, 0, 0, 0, s, 0, -h, 0, (int) (r * 0.6), (int) (g * 0.6), (int) (b * 0.6), a);
        quad(vc, m, 0, -h, 0, 0, 0, -s, -s, 0, 0, 0, -h, 0, (int) (r * 0.7), (int) (g * 0.7), (int) (b * 0.7), a);
        quad(vc, m, 0, -h, 0, s, 0, 0, 0, 0, -s, 0, -h, 0, (int) (r * 0.6), (int) (g * 0.6), (int) (b * 0.6), a);
    }

    private static void quad(VertexConsumer vc, Matrix4f m,
                             double x0, double y0, double z0,
                             double x1, double y1, double z1,
                             double x2, double y2, double z2,
                             double x3, double y3, double z3,
                             int r, int g, int b, int a) {
        vc.vertex(m, (float) x0, (float) y0, (float) z0).color(r, g, b, a).endVertex();
        vc.vertex(m, (float) x1, (float) y1, (float) z1).color(r, g, b, a).endVertex();
        vc.vertex(m, (float) x2, (float) y2, (float) z2).color(r, g, b, a).endVertex();
        vc.vertex(m, (float) x3, (float) y3, (float) z3).color(r, g, b, a).endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(DemonGateBE be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 160;
    }
}
