package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.arena.BossColumnBlock;
import ru.aiforge.riftworld.arena.BossColumnBlockEntity;
import ru.aiforge.riftworld.glb.Pose;

/** Healing column: GLB model (intact / shattered nodes, cracks by HP, turning crystal), rise / sink offset, ember beam. */
public class BossColumnRenderer implements BlockEntityRenderer<BossColumnBlockEntity> {
    private static final ResourceLocation GLB = new ResourceLocation(RiftWorld.MODID, "models/glb/boss_column.glb");
    private static final String[] INTACT = {"shaft", "runes", "crystal", "crack_1", "crack_2"};
    private static final String[] BROKEN = {"stub", "rubble", "shards"};
    private static final String[] SPIN = {"crystal", "crack_1", "crack_2"};
    private static final float SUNK = -2.95F;

    public BossColumnRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    private static float ease(float u) {
        return u * u * (3 - 2 * u);
    }

    @Override
    public void render(BossColumnBlockEntity be, float partial, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null) return;
        int phase = be.getBlockState().getValue(BossColumnBlock.PHASE);
        float time = be.getLevel().getGameTime() + partial;
        float off;
        if (be.anim == 1) off = SUNK * (1 - ease(Mth.clamp((time - be.animStart) / BossColumnBlockEntity.RISE_T, 0, 1)));
        else if (be.anim == 2) off = SUNK * ease(Mth.clamp((time - be.animStart) / BossColumnBlockEntity.SINK_T, 0, 1));
        else if (phase == 0) return;
        else off = 0;
        GlbAssets.Loaded lm = GlbAssets.get(GLB);
        if (lm == null) return;
        boolean broken = phase == 2;
        Pose pose = lm.scratch();
        for (String n : INTACT) pose.hide(n, broken);
        for (String n : BROKEN) pose.hide(n, !broken);
        pose.hide("crack_1", be.hp >= 3);
        pose.hide("crack_2", be.hp >= 2);
        float spin = time * 0.03F, bob = Mth.sin(time * 0.07F) * 0.035F;
        float hit = Mth.clamp(1 - (time - be.flash) / 6F, 0, 1);
        float jolt = hit * Mth.sin(time * 3.1F) * 0.04F;
        for (String n : SPIN) {
            pose.rotate(n, spin, 0, 1, 0);
            pose.translate(n, jolt, bob, 0);
        }
        pose.compute();
        float shake = be.anim == 1 ? Mth.sin(time * 2.3F) * 0.015F : 0;
        ps.pushPose();
        ps.translate(0.5 + shake, off, 0.5);
        GlbRenderer.render(lm, pose, ps, buffers, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
        if (!broken && be.anim == 0) {
            if (be.beam) beam(be, ps, buffers, time);
            if (hit > 0) flare(ps, buffers, hit, time);
        }
    }

    // ------------------------------------------------------------------------------------------------ beam
    private static void quad(VertexConsumer vc, Matrix4f m, Vector3f a, Vector3f b, Vector3f s0, Vector3f s1, float w0, float w1,
                             int r, int g, int bl, int a0, int a1) {
        float[][] p = {{a.x - s0.x * w0, a.y - s0.y * w0, a.z - s0.z * w0}, {a.x + s0.x * w0, a.y + s0.y * w0, a.z + s0.z * w0},
                {b.x + s1.x * w1, b.y + s1.y * w1, b.z + s1.z * w1}, {b.x - s1.x * w1, b.y - s1.y * w1, b.z - s1.z * w1}};
        int[] al = {a0, a0, a1, a1};
        for (int k = 0; k < 4; ++k) vc.vertex(m, p[k][0], p[k][1], p[k][2]).color(r, g, bl, al[k]).endVertex();
        for (int k = 3; k >= 0; --k) vc.vertex(m, p[k][0], p[k][1], p[k][2]).color(r, g, bl, al[k]).endVertex();
    }

    private static void beam(BossColumnBlockEntity be, PoseStack ps, MultiBufferSource buffers, float time) {
        Vector3f a = new Vector3f(0.5F, (float) BossColumnBlockEntity.CRYSTAL_Y, 0.5F);
        Vector3f b = new Vector3f((float) (be.tx - be.getBlockPos().getX()), (float) (be.ty - be.getBlockPos().getY()),
                (float) (be.tz - be.getBlockPos().getZ()));
        Vector3f d = new Vector3f(b).sub(a);
        float len = d.length();
        if (len < 0.5F) return;
        Vector3f dn = new Vector3f(d).div(len);
        Vector3f s1 = new Vector3f(dn).cross(0, 1, 0);
        if (s1.lengthSquared() < 1e-4F) s1.set(1, 0, 0);
        s1.normalize();
        Vector3f s2 = new Vector3f(dn).cross(s1).normalize();
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        Matrix4f m = ps.last().pose();
        int n = Math.max(8, (int) (len * 2.5F));
        Vector3f p0 = new Vector3f(), p1 = new Vector3f();
        for (int i = 0; i < n; ++i) {
            float u0 = i / (float) n, u1 = (i + 1) / (float) n;
            // gentle sag + flicker; brightness pulses flow from the column towards the throne
            float sag0 = Mth.sin(u0 * Mth.PI) * 0.35F, sag1 = Mth.sin(u1 * Mth.PI) * 0.35F;
            a.lerp(b, u0, p0).add(0, -sag0, 0);
            a.lerp(b, u1, p1).add(0, -sag1, 0);
            float f0 = 0.5F + 0.5F * Mth.sin(u0 * len * 1.6F - time * 0.45F), f1 = 0.5F + 0.5F * Mth.sin(u1 * len * 1.6F - time * 0.45F);
            float w0 = 0.05F + 0.035F * f0, w1 = 0.05F + 0.035F * f1;
            int a0 = (int) (70 + 110 * f0), a1 = (int) (70 + 110 * f1);
            quad(vc, m, p0, p1, s1, s1, w0, w1, 255, 80, 20, a0, a1);
            quad(vc, m, p0, p1, s2, s2, w0, w1, 255, 80, 20, a0, a1);
            quad(vc, m, p0, p1, s1, s1, w0 * 0.35F, w1 * 0.35F, 255, 210, 120, a0, a1);
            quad(vc, m, p0, p1, s2, s2, w0 * 0.35F, w1 * 0.35F, 255, 210, 120, a0, a1);
        }
    }

    /** Short star-shaped flash around the crystal when it is struck. */
    private static void flare(PoseStack ps, MultiBufferSource buffers, float k, float time) {
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        Matrix4f m = ps.last().pose();
        Vector3f c = new Vector3f(0.5F, (float) BossColumnBlockEntity.CRYSTAL_Y, 0.5F);
        int al = (int) (200 * k);
        float r = 0.25F + 0.45F * (1 - k);
        for (int i = 0; i < 6; ++i) {
            float ang = i * Mth.PI / 3 + time * 0.1F;
            Vector3f dir = new Vector3f(Mth.cos(ang), (i % 2 == 0 ? 0.5F : -0.5F), Mth.sin(ang)).normalize();
            Vector3f s = new Vector3f(dir).cross(0, 1, 0);
            if (s.lengthSquared() < 1e-4F) s.set(1, 0, 0);
            s.normalize();
            Vector3f tip = new Vector3f(dir).mul(r).add(c);
            quad(vc, m, c, tip, s, s, 0.06F, 0.0F, 255, 150, 60, al, 0);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(BossColumnBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
