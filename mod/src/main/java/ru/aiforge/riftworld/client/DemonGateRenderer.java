package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import ru.aiforge.riftworld.block.DemonGateBE;

/**
 * Part 12: the demonic teleport is no longer a flat pane - client-side it is a slowly WHIRLING crimson vortex:
 * twisted rings of ember-red that darken toward a pulsing hungry core. Pure visuals (debug quads), zero packets.
 * The swirl phase is per-block, so the 3x3 pane of the ruin arch shows one continuous maelstrom.
 */
public class DemonGateRenderer implements BlockEntityRenderer<DemonGateBE> {
    private static final int SEGS = 14, RINGS = 5;

    public DemonGateRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(DemonGateBE be, float partial, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
        if (be.getLevel() == null) return;
        double t = be.getLevel().getGameTime() + partial;
        // per-block phase so adjacent panes shimmer in one flow
        long p = be.getBlockPos().asLong();
        double phase = ((p * 0x9E3779B97F4A7C15L) & 0xFFFF) / 65535.0 * Math.PI * 2;

        VertexConsumer vc = buf.getBuffer(RenderType.debugQuads());
        drawSwirl(vc, pose, 0.5, 0.5, 0.48, phase, t, 1);      // south-facing sheet
        drawSwirl(vc, pose, 0.5, 0.5, 0.52, -phase, t, -1);   // north-facing sheet
    }

    private static void drawSwirl(VertexConsumer vc, PoseStack pose, double cx, double cy, double cz,
                                  double phase, double t, int dir) {
        double twist = t * 0.9 * dir + phase;
        for (int k = 0; k < RINGS; ++k) {
            double r0 = 0.06 + 0.115 * k, r1 = 0.06 + 0.115 * (k + 1);
            float dk = 1.0F - k / (float) RINGS;
            int cr = (int) (150 + 105 * dk), cg = (int) (18 + 26 * dk), cb = (int) (26 + 14 * dk);
            float alpha = 0.62F + 0.28F * dk;
            for (int i = 0; i < SEGS; ++i) {
                double a0 = i * Math.PI * 2 / SEGS + twist + k * 0.35;
                double a1 = (i + 1) * Math.PI * 2 / SEGS + twist + k * 0.35;
                double wob = Math.sin(a0 * 3 + t * 2.3) * 0.03;
                double x0 = r0 * Math.cos(a0), y0 = r0 * Math.sin(a0);
                double x1 = r0 * Math.cos(a1), y1 = r0 * Math.sin(a1);
                double x2 = r1 * Math.cos(a1), y2 = r1 * Math.sin(a1);
                double x3 = r1 * Math.cos(a0), y3 = r1 * Math.sin(a0);
                quad(vc, pose, cx + x0, cy + y0, cz + wob, cx + x1, cy + y1, cz - wob,
                        cx + x2, cy + y2, cz + wob, cx + x3, cy + y3, cz - wob, cr, cg, cb, alpha);
            }
        }
        // the hungry core: bigger when the vortex breathes in
        double pulse = 0.10 + 0.03 * Math.sin(t * 1.7 + phase);
        float coreA = 0.82F + 0.15F * (float) Math.sin(t * 1.7 + phase);
        for (int i = 0; i < SEGS; ++i) {
            double a0 = i * Math.PI * 2 / SEGS, a1 = (i + 1) * Math.PI * 2 / SEGS;
            quad(vc, pose, cx, cy, cz, cx + pulse * Math.cos(a0), cy + pulse * Math.sin(a0), cz,
                    cx + pulse * Math.cos(a1), cy + pulse * Math.sin(a1), cz, cx, cy, cz, 46, 8, 14, coreA);
            quad(vc, pose, cx, cy, cz - dir * 0.004, cx + pulse * Math.cos(a1), cy + pulse * Math.sin(a1), cz - dir * 0.004,
                    cx + pulse * Math.cos(a0), cy + pulse * Math.sin(a0), cz - dir * 0.004, cx, cy, cz - dir * 0.004, 46, 8, 14, coreA);
        }
        // inner blazing heart
        double h = pulse * 0.45;
        quad(vc, pose, cx - h, cy - h, cz + dir * 0.006, cx - h, cy + h, cz + dir * 0.006,
                cx + h, cy + h, cz + dir * 0.006, cx + h, cy - h, cz + dir * 0.006, 255, 64, 24, 0.9F);
        quad(vc, pose, cx + h, cy - h, cz + dir * 0.006, cx + h, cy + h, cz + dir * 0.006,
                cx - h, cy + h, cz + dir * 0.006, cx - h, cy - h, cz + dir * 0.006, 255, 64, 24, 0.9F);
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
