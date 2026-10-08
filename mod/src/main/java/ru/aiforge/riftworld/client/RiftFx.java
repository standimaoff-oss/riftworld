package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.block.RiftAltarBlockEntity;
import ru.aiforge.riftworld.net.RiftFxMsg;
import ru.aiforge.riftworld.world.RiftZone;

/**
 * Rev 5, client side: the black rift, the darkness of the hall and the breaking space.
 * <ul>
 * <li>The rift is <b>not</b> made of blocks: it is drawn as a hole in space in front of the altar - a black core that
 * occludes the world behind it, a halo that eats the light around it, a burning rim, cracks running through the air and
 * shards of the world slowly spiralling in;</li>
 * <li>while the ritual runs (or the rift is open) the world inside the temple is darker - and only inside it: the
 * overlay follows the volume of the hall, so stepping out of the door brings the light back;</li>
 * <li>the shatter moment rumble: the camera shakes for a moment when the space breaks.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID, value = Dist.CLIENT)
public final class RiftFx {
    private record Altar(ResourceKey<Level> dim, BlockPos pos, int state, int stage, int size, long expire) {
    }

    private static final Map<Long, Altar> ALTARS = new HashMap<>();
    private static int shake, flash;
    private static final java.util.Random RND = new java.util.Random(0x1217);

    private RiftFx() {
    }

    // ------------------------------------------------------------------------------------------------ from the server
    public static void onMsg(RiftFxMsg m) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        int size = m.size();
        if (m.state() == RiftAltarBlockEntity.OPEN && size == 0) size = 100;
        ALTARS.put(m.pos().asLong(), new Altar(mc.level.dimension(), m.pos(), m.state(), m.stage(), Math.max(size, 0), now + 90));
        if (m.shake() > 0) shake = Math.max(shake, m.shake());
        if (m.flash() > 0) flash = 8;
    }

    private static void prune(long now) {
        ALTARS.values().removeIf(a -> now > a.expire());
    }

    /** The strongest rift the camera sits in: darkness, slow motion and the light drain all read it. */
    private static Altar active(Vec3 cam) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        long now = mc.level.getGameTime();
        prune(now);
        Altar best = null;
        float bestW = 0;
        for (Altar a : ALTARS.values()) {
            if (!a.dim().equals(mc.level.dimension())) continue;
            float w = RiftZone.weight(a.pos(), cam.x, cam.y, cam.z);
            w = Math.max(w, near(a, cam) * 0.02F);
            if (w > bestW) {
                bestW = w;
                best = a;
            }
        }
        return best;
    }

    private static float near(Altar a, Vec3 cam) {
        double d = Math.sqrt(cam.distanceToSqr(RiftZone.centerX(a.pos()), RiftZone.centerY(a.pos()), RiftZone.centerZ(a.pos())));
        return d > 96 ? 0 : 1;
    }

    /** 0 = untouched world, 1 = the rift swallowed everything. */
    public static float darkness(Vec3 cam) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 0;
        float dark = 0;
        long now = mc.level.getGameTime();
        prune(now);
        for (Altar a : ALTARS.values()) {
            if (!a.dim().equals(mc.level.dimension())) continue;
            if (a.state() == RiftAltarBlockEntity.IDLE) continue;      // the hall only darkens while the ritual runs
            float inside = RiftZone.weight(a.pos(), cam.x, cam.y, cam.z);
            if (inside <= 0) continue;
            double d = Math.sqrt(cam.distanceToSqr(RiftZone.centerX(a.pos()), RiftZone.centerY(a.pos()), RiftZone.centerZ(a.pos())));
            float pull = (float) Mth.clamp(1.0 - d / 14.0, 0, 1);
            float base = a.state() == RiftAltarBlockEntity.OPEN ? 0.55F : 0.30F + 0.08F * a.stage();
            dark = Math.max(dark, inside * Math.min(0.88F, base + pull * pull * 0.45F));
        }
        return dark;
    }

    /** The world inside the hall is slow: 1 = normal time, 0.25 = barely moving. */
    public static float timeScale(Vec3 pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 1;
        long now = mc.level.getGameTime();
        for (Altar a : ALTARS.values()) {
            if (!a.dim().equals(mc.level.dimension())) continue;
            if (now > a.expire() || a.state() == RiftAltarBlockEntity.IDLE) continue;
            if (RiftZone.weight(a.pos(), pos.x, pos.y, pos.z) > 0) return 0.25F;
        }
        return 1;
    }

    // ------------------------------------------------------------------------------------------------ events
    @SubscribeEvent
    public static void render(RenderLevelStageEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            rifts(e);
        } else if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            overlay(e);
        }
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog e) {
        float dark = darkness(e.getCamera().getPosition());
        if (dark <= 0.02F) return;
        if (e.getMode() != net.minecraft.client.renderer.FogRenderer.FogMode.FOG_TERRAIN) return;
        if (e.getCamera().getFluidInCamera() != net.minecraft.world.level.material.FogType.NONE) return;
        float far = e.getFarPlaneDistance();
        e.setNearPlaneDistance(far * Mth.lerp(dark, 0.30F, 0.12F));
        e.setFarPlaneDistance(far * Mth.lerp(dark, 0.80F, 0.35F));
        e.setCanceled(true);
    }

    @SubscribeEvent
    public static void fogColor(ViewportEvent.ComputeFogColor e) {
        float dark = darkness(e.getCamera().getPosition());
        if (dark <= 0.02F) return;
        e.setRed((float) Mth.lerp(dark, e.getRed(), 0.055F));
        e.setGreen((float) Mth.lerp(dark, e.getGreen(), 0.020F));
        e.setBlue((float) Mth.lerp(dark, e.getBlue(), 0.030F));
    }

    @SubscribeEvent
    public static void angles(ViewportEvent.ComputeCameraAngles e) {
        if (shake <= 0) return;
        float t = shake * 0.35F;
        e.setYaw(e.getYaw() + (float) Math.sin(t * 5.1F) * t * 0.55F);
        e.setPitch(e.getPitch() + (float) Math.sin(t * 7.3F + 1.1F) * t * 0.42F);
        e.setRoll(e.getRoll() + (float) Math.sin(t * 3.7F + 0.4F) * t * 0.6F);
    }

    @SubscribeEvent
    public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent e) {
        if (e.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (shake > 0) shake--;
        if (flash > 0) flash--;
    }

    // ------------------------------------------------------------------------------------------------ the rift
    private static void rifts(RenderLevelStageEvent e) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = e.getCamera().getPosition();
        long now = mc.level.getGameTime();
        prune(now);
        if (ALTARS.isEmpty()) return;
        Camera camera = mc.gameRenderer.getMainCamera();
        Vector3f right = new Vector3f(camera.getLeftVector()).negate();
        Vector3f up = new Vector3f(camera.getUpVector());
        Matrix4f m = e.getPoseStack().last().pose();
        float time = (now % 100000L) + e.getPartialTick();

        for (Altar a : ALTARS.values()) {
            if (!a.dim().equals(mc.level.dimension())) continue;
            if (a.state() == RiftAltarBlockEntity.IDLE || a.size() <= 0) continue;
            Vector3f c = new Vector3f((float) (RiftZone.centerX(a.pos()) - cam.x), (float) (RiftZone.centerY(a.pos()) - cam.y),
                    (float) (RiftZone.centerZ(a.pos()) - cam.z));
            if (c.lengthSquared() > 96 * 96) continue;
            float open = Mth.clamp(a.size() / 100F, 0F, 1F);
            float R = 0.35F + 2.15F * open;
            halo(m, c, right, up, R * 3.1F, 0.42F * open, time);
            core(m, c, right, up, R, time);
            rim(m, c, right, up, R, open, time);
            cracks(m, c, right, up, R, open, a.state() == RiftAltarBlockEntity.OPEN, time);
            shards(m, c, right, up, R, open, time);
        }
        restore();
    }

    /** The world behind the rift loses its light: a soft black disc with no depth write. */
    private static void halo(Matrix4f m, Vector3f c, Vector3f right, Vector3f up, float r, float alpha, float t) {
        if (alpha <= 0.01F) return;
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        int ca = rgb((int) (235 * alpha), 0, 0, 0);
        vtx(b, m, c, right, up, 0, 0, ca, 0);
        int seg = 48;
        for (int i = 0; i <= seg; ++i) {
            float th = i * Mth.TWO_PI / seg;
            float rr = r * (1.0F + 0.05F * Mth.sin(th * 3 + t * 0.02F));
            vtx(b, m, c, right, up, th, rr, ca, 0);
        }
        Tesselator.getInstance().end();
    }

    /** The hole itself: opaque black, depth written, so it swallows whatever stands behind it. */
    private static void core(Matrix4f m, Vector3f c, Vector3f right, Vector3f up, float r, float t) {
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        int black = rgb(255, 0, 0, 0);
        vtx(b, m, c, right, up, 0, 0, black, 0);
        int seg = 56;
        for (int i = 0; i <= seg; ++i) {
            float th = i * Mth.TWO_PI / seg;
            float rr = r * (0.92F + 0.08F * Mth.sin(th * 5 - t * 0.05F));
            vtx(b, m, c, right, up, th, rr, black, 0);
        }
        Tesselator.getInstance().end();
    }

    /** Burning edge of the hole: additive, restless. */
    private static void rim(Matrix4f m, Vector3f c, Vector3f right, Vector3f up, float r, float open, float t) {
        additive();
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int seg = 64;
        for (int i = 0; i < seg; ++i) {
            float a0 = i * Mth.TWO_PI / seg, a1 = (i + 1) * Mth.TWO_PI / seg;
            float w0 = 0.06F + 0.05F * Mth.sin(a0 * 7 + t * 0.09F);
            float w1 = 0.06F + 0.05F * Mth.sin(a1 * 7 + t * 0.09F);
            float flick = 0.55F + 0.45F * Mth.sin(a0 * 11 - t * 0.13F);
            int col = rgb(255, (int) (70 + 150 * flick), (int) (40 + 110 * flick), (int) (200 * open));
            vtx(b, m, c, right, up, a0, r * 0.97F, col, 1);
            vtx(b, m, c, right, up, a1, r * 0.97F, col, 1);
            vtx(b, m, c, right, up, a1, r * 0.97F + w1, col, 1);
            vtx(b, m, c, right, up, a0, r * 0.97F + w0, col, 1);
        }
        Tesselator.getInstance().end();
    }

    /** Cracked space: jagged glowing lines running away from the hole. */
    private static void cracks(Matrix4f m, Vector3f c, Vector3f right, Vector3f up, float r, float open, boolean persistent, float t) {
        additive();
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int n = 9;
        for (int i = 0; i < n; ++i) {
            RND.setSeed(i * 7919L);
            float base = (float) (i * Mth.TWO_PI / n) + RND.nextFloat() * 0.35F;
            float len = r * (1.4F + 1.9F * RND.nextFloat()) * open;
            float grow = persistent ? 1F : Mth.clamp(open * 1.4F, 0, 1);
            float prevA = base, prevR = r * 0.9F;
            float wob = (float) (RND.nextDouble() - 0.5);
            int steps = 4;
            for (int s = 1; s <= steps; ++s) {
                float u = s / (float) steps;
                float ang = base + wob * u * 1.2F + 0.08F * Mth.sin(t * 0.03F + s);
                float rad = r * 0.9F + len * u;
                if (rad * grow < prevR) continue;
                float w = (0.05F + 0.05F * (1 - u)) * grow;
                int col = rgb(255, 200, 190, (int) (170 * grow));
                float p0 = prevR, p1 = rad;
                vtx(b, m, c, right, up, prevA, p0 - w * 0.5F, col, 1);
                vtx(b, m, c, right, up, ang, p1 - w * 0.5F, col, 1);
                vtx(b, m, c, right, up, ang, p1 + w * 0.5F, col, 1);
                vtx(b, m, c, right, up, prevA, p0 + w * 0.5F, col, 1);
                prevA = ang;
                prevR = p1;
            }
        }
        Tesselator.getInstance().end();
    }

    /** Shards of the broken space slowly falling into the hole. */
    private static void shards(Matrix4f m, Vector3f c, Vector3f right, Vector3f up, float r, float open, float t) {
        additive();
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int n = 26;
        for (int i = 0; i < n; ++i) {
            RND.setSeed(i * 104729L);
            float phase = RND.nextFloat();
            float speed = 0.06F + 0.10F * RND.nextFloat();
            float u = (phase + t * 0.0016F * speed * 20F) % 1F;
            float rad = r * (1.1F + 3.4F * (1 - u));                        // falls inwards
            float ang = (float) (i * 2.399963) + t * 0.004F * (1 + 2 * u);
            float size = (0.035F + 0.05F * RND.nextFloat()) * open;
            int a = (int) (185 * u * open);
            int col = i % 3 == 0 ? rgb(255, 235, 225, a) : rgb(255, 120, 90, a);
            vtx(b, m, c, right, up, ang, rad, col, 1);
            vtx(b, m, c, right, up, ang + 0.06F, rad, col, 1);
            vtx(b, m, c, right, up, ang + 0.06F, rad + size, col, 1);
            vtx(b, m, c, right, up, ang, rad + size, col, 1);
        }
        Tesselator.getInstance().end();
    }

    // ------------------------------------------------------------------------------------------------ darkness / flash
    private static void overlay(RenderLevelStageEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float dark = darkness(e.getCamera().getPosition());
        boolean white = flash > 0;
        if (dark <= 0.01F && !white) return;
        Matrix4f m = e.getPoseStack().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        if (dark > 0.01F) {
            RenderSystem.disableDepthTest();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.SRC_COLOR);
            float k = 1F - dark;
            int c = ((int) (k * 255) << 16) | ((int) (k * 255) << 8) | (int) (k * 255);
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            b.vertex(m, -8, -8, -3).color(c).endVertex();
            b.vertex(m, 8, -8, -3).color(c).endVertex();
            b.vertex(m, 8, 8, -3).color(c).endVertex();
            b.vertex(m, -8, 8, -3).color(c).endVertex();
            Tesselator.getInstance().end();
        }
        if (white) {
            RenderSystem.disableDepthTest();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            int a = (int) (215 * (flash / 8F));
            int c = (a << 24) | 0xFFFFFF;
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            b.vertex(m, -8, -8, -3).color(c).endVertex();
            b.vertex(m, 8, -8, -3).color(c).endVertex();
            b.vertex(m, 8, 8, -3).color(c).endVertex();
            b.vertex(m, -8, 8, -3).color(c).endVertex();
            Tesselator.getInstance().end();
        }
        restore();
        RenderSystem.enableDepthTest();
    }

    // ------------------------------------------------------------------------------------------------ helpers
    private static void additive() {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
    }

    private static void restore() {
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static int rgb(int a, int r, int g, int b) {
        return (Mth.clamp(a, 0, 255) << 24) | (Mth.clamp(r, 0, 255) << 16) | (Mth.clamp(g, 0, 255) << 8) | Mth.clamp(b, 0, 255);
    }

    private static void col(BufferBuilder b, Matrix4f m, Vector3f c, int r, int g, int bl, int a) {
        b.vertex(m, c.x, c.y, c.z).color(r, g, bl, a).endVertex();
    }

    /** One billboarded vertex at angle {@code th} and radius {@code rad} in the plane (right, up). */
    private static void vtx(BufferBuilder b, Matrix4f m, Vector3f c, Vector3f right, Vector3f up, float th, float rad, int argb, int unused) {
        float cs = Mth.cos(th) * rad, sn = Mth.sin(th) * rad;
        b.vertex(m, c.x + right.x * cs + up.x * sn, c.y + right.y * cs + up.y * sn, c.z + right.z * cs + up.z * sn)
                .color((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >>> 24) & 255).endVertex();
    }
}
