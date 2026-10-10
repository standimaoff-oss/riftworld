package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import ru.aiforge.riftworld.block.DemonGateBE;

/**
 * ЧЕРНЫЙ ПРОЛОМ ПРОСТРАНСТВА (Black Rift in Space):
 * The portal is rendered as a torn rupture in reality:
 * - A deep pitch-black void core that swallows light and occludes space behind it
 * - Concentric cosmic distortion rings swirling inward toward the singularity
 * - An agitated, burning, flickering rift rim (горящая кромка пролома)
 * - Jagged spatial cracks and lightning branching outward through the air
 * - Floating shards of shattered reality orbiting and being drawn into the event horizon
 */
public class DemonGateRenderer implements BlockEntityRenderer<DemonGateBE> {
    private static final int SEGS = 24;
    private static final int RINGS = 6;
    private static final int CRACKS = 10;
    private static final int SHARDS = 16;

    public DemonGateRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(DemonGateBE be, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
        if (be.getLevel() == null) return;
        double t = be.getLevel().getGameTime() + partial;
        long p = be.getBlockPos().asLong();
        double phase = ((p * 0x9E3779B97F4A7C15L) & 0xFFFF) / 65535.0 * Math.PI * 2;

        VertexConsumer vc = buf.getBuffer(RenderType.debugQuads());
        drawRift(vc, pose, 0.5, 0.5, 0.48, phase, t, 1);    // south-facing sheet
        drawRift(vc, pose, 0.5, 0.5, 0.52, -phase, t, -1); // north-facing sheet
    }

    private static void drawRift(VertexConsumer vc, PoseStack pose, double cx, double cy, double cz,
                                 double phase, double t, int dir) {
        double twist = t * 1.1 * dir + phase;

        // 1) Swirling dark event-horizon rings (cosmic dark violet / obsidian gradient)
        for (int k = 0; k < RINGS; ++k) {
            double r0 = 0.08 + 0.095 * k, r1 = 0.08 + 0.095 * (k + 1);
            float dk = 1.0F - k / (float) RINGS;
            // Dark abyssal gradient: from deep cosmic violet/obsidian to near-black
            int cr = (int) (15 + 45 * dk);
            int cg = (int) (5 + 18 * dk);
            int cb = (int) (25 + 65 * dk);
            float alpha = 0.55F + 0.35F * dk;

            for (int i = 0; i < SEGS; ++i) {
                double a0 = i * Math.PI * 2 / SEGS + twist + k * 0.28;
                double a1 = (i + 1) * Math.PI * 2 / SEGS + twist + k * 0.28;
                double wob = Math.sin(a0 * 4 + t * 2.8) * 0.025;
                double x0 = r0 * Math.cos(a0), y0 = r0 * Math.sin(a0);
                double x1 = r0 * Math.cos(a1), y1 = r0 * Math.sin(a1);
                double x2 = r1 * Math.cos(a1), y2 = r1 * Math.sin(a1);
                double x3 = r1 * Math.cos(a0), y3 = r1 * Math.sin(a0);
                quad(vc, pose, cx + x0, cy + y0, cz + wob, cx + x1, cy + y1, cz - wob,
                        cx + x2, cy + y2, cz + wob, cx + x3, cy + y3, cz - wob, cr, cg, cb, alpha);
            }
        }

        // 2) The pitch-black void singularity (ЧЕРНЫЙ ПРОЛОМ: непроглядная тьма)
        double pulse = 0.22 + 0.04 * Math.sin(t * 1.6 + phase);
        // Outer absorption layer
        for (int i = 0; i < SEGS; ++i) {
            double a0 = i * Math.PI * 2 / SEGS, a1 = (i + 1) * Math.PI * 2 / SEGS;
            double x0 = pulse * Math.cos(a0), y0 = pulse * Math.sin(a0);
            double x1 = pulse * Math.cos(a1), y1 = pulse * Math.sin(a1);
            quad(vc, pose, cx, cy, cz, cx + x0, cy + y0, cz,
                    cx + x1, cy + y1, cz, cx, cy, cz, 4, 2, 8, 0.95F);
            quad(vc, pose, cx, cy, cz - dir * 0.003, cx + x1, cy + y1, cz - dir * 0.003,
                    cx + x0, cy + y0, cz - dir * 0.003, cx, cy, cz - dir * 0.003, 4, 2, 8, 0.95F);
        }

        // Inner absolute black core (pure pitch-black singularity)
        double coreR = pulse * 0.72;
        for (int i = 0; i < SEGS; ++i) {
            double a0 = i * Math.PI * 2 / SEGS, a1 = (i + 1) * Math.PI * 2 / SEGS;
            double x0 = coreR * Math.cos(a0), y0 = coreR * Math.sin(a0);
            double x1 = coreR * Math.cos(a1), y1 = coreR * Math.sin(a1);
            quad(vc, pose, cx, cy, cz + dir * 0.003, cx + x0, cy + y0, cz + dir * 0.003,
                    cx + x1, cy + y1, cz + dir * 0.003, cx, cy, cz + dir * 0.003, 0, 0, 0, 1.0F);
            quad(vc, pose, cx, cy, cz - dir * 0.005, cx + x1, cy + y1, cz - dir * 0.005,
                    cx + x0, cy + y0, cz - dir * 0.005, cx, cy, cz - dir * 0.005, 0, 0, 0, 1.0F);
        }

        // 3) Burning, restless rift rim (горящая кромка горизонта событий)
        for (int i = 0; i < SEGS; ++i) {
            double a0 = i * Math.PI * 2 / SEGS, a1 = (i + 1) * Math.PI * 2 / SEGS;
            double flick0 = 0.5 + 0.5 * Math.sin(a0 * 7 - t * 4.2 + phase);
            double flick1 = 0.5 + 0.5 * Math.sin(a1 * 7 - t * 4.2 + phase);
            double w0 = 0.04 + 0.025 * flick0;
            double w1 = 0.04 + 0.025 * flick1;
            int r = (int) (180 + 75 * flick0);
            int g = (int) (60 + 80 * flick0);
            int b = (int) (200 + 55 * flick0);
            double x0 = (pulse - w0 * 0.5) * Math.cos(a0), y0 = (pulse - w0 * 0.5) * Math.sin(a0);
            double x1 = (pulse - w1 * 0.5) * Math.cos(a1), y1 = (pulse - w1 * 0.5) * Math.sin(a1);
            double x2 = (pulse + w1 * 0.5) * Math.cos(a1), y2 = (pulse + w1 * 0.5) * Math.sin(a1);
            double x3 = (pulse + w0 * 0.5) * Math.cos(a0), y3 = (pulse + w0 * 0.5) * Math.sin(a0);
            quad(vc, pose, cx + x0, cy + y0, cz + dir * 0.004, cx + x1, cy + y1, cz + dir * 0.004,
                    cx + x2, cy + y2, cz + dir * 0.004, cx + x3, cy + y3, cz + dir * 0.004, r, g, b, 0.88F);
        }

        // 4) Jagged spatial cracks (разломы пространства в воздухе)
        for (int c = 0; c < CRACKS; ++c) {
            double baseA = c * Math.PI * 2 / CRACKS + phase * 0.3;
            double crackLen = 0.25 + 0.18 * Math.sin(c * 3.7 + t * 0.8);
            double segLen = crackLen / 3.0;
            double curR = pulse * 0.95;
            double curA = baseA;
            for (int s = 0; s < 3; ++s) {
                double nextR = curR + segLen;
                double zig = Math.sin(s * 2.1 + c * 5.3 + t * 1.5) * 0.12;
                double nextA = curA + zig;
                double w = (0.025 * (1.0 - s / 3.0));
                double px0 = curR * Math.cos(curA), py0 = curR * Math.sin(curA);
                double px1 = nextR * Math.cos(nextA), py1 = nextR * Math.sin(nextA);
                double nx = -(py1 - py0) * w, ny = (px1 - px0) * w;
                int crkR = s == 0 ? 255 : 200;
                int crkG = s == 0 ? 220 : 120;
                int crkB = 255;
                float crkA = (float) (0.85 - s * 0.22);
                quad(vc, pose, cx + px0 - nx, cy + py0 - ny, cz + dir * 0.005,
                        cx + px1 - nx, cy + py1 - ny, cz + dir * 0.005,
                        cx + px1 + nx, cy + py1 + ny, cz + dir * 0.005,
                        cx + px0 + nx, cy + py0 + ny, cz + dir * 0.005,
                        crkR, crkG, crkB, crkA);
                curR = nextR;
                curA = nextA;
            }
        }

        // 5) Infalling reality shards (осколки пространства, падающие в сингулярность)
        for (int i = 0; i < SHARDS; ++i) {
            double speed = 0.04 + (i % 5) * 0.015;
            double u = ((t * speed + i * 0.23 + phase) % 1.0);
            double fallR = pulse * (1.1 + 2.4 * (1.0 - u));
            double shardA = i * 2.4 + t * 0.03 * (1.0 + u);
            double sz = 0.025 + 0.02 * (1.0 - u);
            double sx = fallR * Math.cos(shardA), sy = fallR * Math.sin(shardA);
            float shardA_val = (float) (0.75 * (1.0 - u * 0.5));
            int sr = (i % 3 == 0) ? 255 : 180;
            int sg = (i % 3 == 0) ? 240 : 80;
            int sb = 255;
            quad(vc, pose, cx + sx - sz, cy + sy, cz + dir * 0.006,
                    cx + sx, cy + sy + sz, cz + dir * 0.006,
                    cx + sx + sz, cy + sy, cz + dir * 0.006,
                    cx + sx, cy + sy - sz, cz + dir * 0.006,
                    sr, sg, sb, shardA_val);
        }
    }

    private static void quad(VertexConsumer vc, PoseStack pose,
                             double x0, double y0, double z0, double x1, double y1, double z1,
                             double x2, double y2, double z2, double x3, double y3, double z3,
                             int r, int g, int b, float a) {
        var m = pose.last().pose();
        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        vc.vertex(m, (float) x0, (float) y0, (float) z0).color(rf, gf, bf, a).normal(pose.last().normal(), 0, 0, 1).endVertex();
        vc.vertex(m, (float) x1, (float) y1, (float) z1).color(rf, gf, bf, a).normal(pose.last().normal(), 0, 0, 1).endVertex();
        vc.vertex(m, (float) x2, (float) y2, (float) z2).color(rf, gf, bf, a).normal(pose.last().normal(), 0, 0, 1).endVertex();
        vc.vertex(m, (float) x3, (float) y3, (float) z3).color(rf, gf, bf, a).normal(pose.last().normal(), 0, 0, 1).endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(DemonGateBE be) {
        return true;
    }
}
