package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import ru.aiforge.riftworld.block.MosaicBlock;
import ru.aiforge.riftworld.block.MosaicCoreBE;

/**
 * 3D RITUAL MOSAIC (Уникальная 3D Мозаика на полу):
 * Renders a full 3D celestial astrolabe / sacred sigil dial on the floor of the Great Temple:
 * - 3D raised bronze and gold concentric astrolabe rings (radii 2.3m, 1.6m, 0.9m).
 * - 8 raised candle-socket shrines around the perimeter with carved sigil bases.
 * - Rotating astronomical dial rings that counter-rotate as the ritual charges.
 * - Inset liquid light energy channels connecting each candle socket to the central dais.
 * - In the center: an elevated octagonal gold altar dais with a hovering, spinning multi-faceted Rift Core crystal.
 */
public class MosaicCoreRenderer implements BlockEntityRenderer<MosaicCoreBE> {
    private static final int SEGS = 32;

    public MosaicCoreRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(MosaicCoreBE be, float partial, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null) return;
        float time = be.getLevel().getGameTime() + partial;
        int stage = be.getBlockState().hasProperty(MosaicBlock.STAGE) ? be.getBlockState().getValue(MosaicBlock.STAGE) : 0;
        boolean open = be.isOpen();

        ps.pushPose();
        ps.translate(0.5, 0.005, 0.5); // align to block center, just above floor plane

        VertexConsumer vc = buffers.getBuffer(RenderType.debugQuads());

        // 1. OUTER HEAVY BRONZE & GOLD RIM (R = 2.4 to 2.2)
        drawRing3D(vc, ps, 2.22, 2.42, 0.02, 0.05, 180, 140, 45, 1.0F);
        drawRing3D(vc, ps, 2.40, 2.46, 0.00, 0.025, 120, 95, 35, 1.0F);

        // 8 CANDLE-SOCKET ALCOVES around the perimeter
        for (int i = 0; i < 8; ++i) {
            double a = i * Math.PI / 4.0;
            double sx = Math.cos(a) * 2.0;
            double sz = Math.sin(a) * 2.0;
            drawShrinePedestal(vc, ps, sx, sz, 0.28, 0.045, 210, 170, 60);
        }

        // 2. CONCENTRIC ASTRONOMICAL DIAL RINGS
        // Outer runic band (R = 1.95 to 1.65)
        float spinOuter = (stage > 0 || open) ? time * 0.015F : 0.0F;
        drawSegmentedDial(vc, ps, 1.65, 1.95, 0.015, 0.035, 16, spinOuter, 190, 155, 55, 140, 110, 35);

        // Counter-rotating inner calendar ring (R = 1.15 to 1.55)
        float spinInner = (stage > 0 || open) ? -time * 0.025F : 0.0F;
        drawSegmentedDial(vc, ps, 1.15, 1.55, 0.018, 0.038, 24, spinInner, 220, 185, 75, 160, 130, 45);

        // 3. EIGHT RADIATING ENERGY CHANNELS (Connecting candle shrines to the core)
        float glowPulse = 0.5F + 0.5F * Mth.sin(time * 0.2F);
        int chR = (stage >= 3 || open) ? 255 : (stage == 2 ? 240 : (stage == 1 ? 120 : 70));
        int chG = (stage >= 3 || open) ? 230 : (stage == 2 ? 160 : (stage == 1 ? 200 : 90));
        int chB = (stage >= 3 || open) ? 140 : (stage == 2 ? 50 : (stage == 1 ? 255 : 180));
        float alphaCh = (stage > 0 || open) ? (0.65F + 0.35F * glowPulse) : 0.35F;

        for (int i = 0; i < 8; ++i) {
            double a = i * Math.PI / 4.0;
            double cos = Math.cos(a), sin = Math.sin(a);
            drawChannel(vc, ps, cos * 0.55, sin * 0.55, cos * 1.95, sin * 1.95, 0.06, 0.02, 35, 25, 20, 1.0F);
            // Glowing energy flow inside the channel
            drawChannel(vc, ps, cos * 0.55, sin * 0.55, cos * 1.95, sin * 1.95, 0.035, 0.026, chR, chG, chB, alphaCh);
        }

        // 4. CENTRAL OCTAGONAL ALTAR DAIS
        drawOctagonDais(vc, ps, 0.65, 0.065, 225, 185, 70);
        drawOctagonDais(vc, ps, 0.45, 0.095, 245, 210, 90);

        // 5. HOVERING 3D RIFT CORE CRYSTAL
        ps.pushPose();
        float bob = Mth.sin(time * 0.08F) * 0.06F;
        float rotY = time * 1.4F;
        ps.translate(0, 0.45 + bob, 0);
        ps.mulPose(Axis.YP.rotationDegrees(rotY));
        ps.mulPose(Axis.XP.rotationDegrees(22.5F));

        float crSize = 0.22F + (stage * 0.03F);
        if (open) crSize = 0.32F;

        // Outer crystal facets (faceted octahedron)
        drawCrystalFacet(vc, ps, crSize, chR, chG, chB, 0.85F);

        // Inner glowing core
        ps.mulPose(Axis.YP.rotationDegrees(-rotY * 2.0F));
        drawCrystalFacet(vc, ps, crSize * 0.55F, 255, 255, 255, 0.95F);

        ps.popPose();

        ps.popPose();
    }

    private static void drawRing3D(VertexConsumer vc, PoseStack ps, double r0, double r1, double y0, double y1,
                                   int r, int g, int b, float a) {
        for (int i = 0; i < SEGS; ++i) {
            double a0 = i * Math.PI * 2.0 / SEGS;
            double a1 = (i + 1) * Math.PI * 2.0 / SEGS;
            double cos0 = Math.cos(a0), sin0 = Math.sin(a0);
            double cos1 = Math.cos(a1), sin1 = Math.sin(a1);

            // Top surface
            quad(vc, ps,
                    r0 * cos0, y1, r0 * sin0,
                    r1 * cos0, y1, r1 * sin0,
                    r1 * cos1, y1, r1 * sin1,
                    r0 * cos1, y1, r0 * sin1,
                    r, g, b, a);
            // Outer wall
            quad(vc, ps,
                    r1 * cos0, y0, r1 * sin0,
                    r1 * cos0, y1, r1 * sin0,
                    r1 * cos1, y1, r1 * sin1,
                    r1 * cos1, y0, r1 * sin1,
                    (int) (r * 0.8), (int) (g * 0.8), (int) (b * 0.8), a);
            // Inner wall
            quad(vc, ps,
                    r0 * cos0, y0, r0 * sin0,
                    r0 * cos1, y0, r0 * sin1,
                    r0 * cos1, y1, r0 * sin1,
                    r0 * cos0, y1, r0 * sin0,
                    (int) (r * 0.7), (int) (g * 0.7), (int) (b * 0.7), a);
        }
    }

    private static void drawSegmentedDial(VertexConsumer vc, PoseStack ps, double r0, double r1, double y0, double y1,
                                          int count, float spin, int rA, int gA, int bA, int rB, int gB, int bB) {
        for (int i = 0; i < count; ++i) {
            double a0 = i * Math.PI * 2.0 / count + spin;
            double a1 = (i + 0.85) * Math.PI * 2.0 / count + spin; // gap between teeth
            double cos0 = Math.cos(a0), sin0 = Math.sin(a0);
            double cos1 = Math.cos(a1), sin1 = Math.sin(a1);

            int cr = (i % 2 == 0) ? rA : rB;
            int cg = (i % 2 == 0) ? gA : gB;
            int cb = (i % 2 == 0) ? bA : bB;

            // Top plate
            quad(vc, ps,
                    r0 * cos0, y1, r0 * sin0,
                    r1 * cos0, y1, r1 * sin0,
                    r1 * cos1, y1, r1 * sin1,
                    r0 * cos1, y1, r0 * sin1,
                    cr, cg, cb, 1.0F);
            // Edge step
            quad(vc, ps,
                    r1 * cos0, y0, r1 * sin0,
                    r1 * cos0, y1, r1 * sin0,
                    r1 * cos1, y1, r1 * sin1,
                    r1 * cos1, y0, r1 * sin1,
                    (int) (cr * 0.75), (int) (cg * 0.75), (int) (cb * 0.75), 1.0F);
        }
    }

    private static void drawShrinePedestal(VertexConsumer vc, PoseStack ps, double cx, double cz, double r, double h,
                                           int cr, int cg, int cb) {
        int sides = 8;
        for (int i = 0; i < sides; ++i) {
            double a0 = i * Math.PI * 2.0 / sides;
            double a1 = (i + 1) * Math.PI * 2.0 / sides;
            double x0 = cx + Math.cos(a0) * r, z0 = cz + Math.sin(a0) * r;
            double x1 = cx + Math.cos(a1) * r, z1 = cz + Math.sin(a1) * r;

            // Top
            quad(vc, ps, cx, h, cz, x0, h, z0, x1, h, z1, cx, h, cz, cr, cg, cb, 1.0F);
            // Wall
            quad(vc, ps, x0, 0, z0, x0, h, z0, x1, h, z1, x1, 0, z1,
                    (int) (cr * 0.8), (int) (cg * 0.8), (int) (cb * 0.8), 1.0F);
        }
    }

    private static void drawChannel(VertexConsumer vc, PoseStack ps, double x0, double z0, double x1, double z1,
                                    double width, double y, int cr, int cg, int cb, float al) {
        double dx = x1 - x0, dz = z1 - z0;
        double len = Math.hypot(dx, dz);
        if (len < 1e-4) return;
        double nx = -dz / len * (width * 0.5);
        double nz = dx / len * (width * 0.5);

        quad(vc, ps,
                x0 - nx, y, z0 - nz,
                x0 + nx, y, z0 + nz,
                x1 + nx, y, z1 + nz,
                x1 - nx, y, z1 - nz,
                cr, cg, cb, al);
    }

    private static void drawOctagonDais(VertexConsumer vc, PoseStack ps, double r, double h, int cr, int cg, int cb) {
        int sides = 8;
        for (int i = 0; i < sides; ++i) {
            double a0 = i * Math.PI * 2.0 / sides + (Math.PI / 8.0);
            double a1 = (i + 1) * Math.PI * 2.0 / sides + (Math.PI / 8.0);
            double x0 = Math.cos(a0) * r, z0 = Math.sin(a0) * r;
            double x1 = Math.cos(a1) * r, z1 = Math.sin(a1) * r;

            // Top
            quad(vc, ps, 0, h, 0, x0, h, z0, x1, h, z1, 0, h, 0, cr, cg, cb, 1.0F);
            // Side
            quad(vc, ps, x0, 0, z0, x0, h, z0, x1, h, z1, x1, 0, z1,
                    (int) (cr * 0.85), (int) (cg * 0.85), (int) (cb * 0.85), 1.0F);
        }
    }

    private static void drawCrystalFacet(VertexConsumer vc, PoseStack ps, float s, int cr, int cg, int cb, float al) {
        float h = s * 1.5F;
        float[][] pts = {
                {s, 0, 0}, {0, 0, s}, {-s, 0, 0}, {0, 0, -s}
        };
        for (int i = 0; i < 4; ++i) {
            float[] p0 = pts[i];
            float[] p1 = pts[(i + 1) % 4];
            // Top pyramid
            quad(vc, ps, 0, h, 0, p0[0], 0, p0[2], p1[0], 0, p1[2], 0, h, 0, cr, cg, cb, al);
            // Bottom pyramid
            quad(vc, ps, 0, -h, 0, p1[0], 0, p1[2], p0[0], 0, p0[2], 0, -h, 0,
                    (int) (cr * 0.8), (int) (cg * 0.8), (int) (cb * 0.8), al);
        }
    }

    private static void quad(VertexConsumer vc, PoseStack ps,
                             double x0, double y0, double z0,
                             double x1, double y1, double z1,
                             double x2, double y2, double z2,
                             double x3, double y3, double z3,
                             int r, int g, int b, float a) {
        var m = ps.last().pose();
        var n = ps.last().normal();
        float rf = r / 255.0F, gf = g / 255.0F, bf = b / 255.0F;
        vc.vertex(m, (float) x0, (float) y0, (float) z0).color(rf, gf, bf, a).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, (float) x1, (float) y1, (float) z1).color(rf, gf, bf, a).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, (float) x2, (float) y2, (float) z2).color(rf, gf, bf, a).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, (float) x3, (float) y3, (float) z3).color(rf, gf, bf, a).normal(n, 0, 1, 0).endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(MosaicCoreBE be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
