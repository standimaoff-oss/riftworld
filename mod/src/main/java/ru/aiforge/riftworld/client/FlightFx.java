package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.block.LaunchCoreBlockEntity;
import ru.aiforge.riftworld.flight.Flight;
import ru.aiforge.riftworld.glb.Pose;

/**
 * Everything the flight draws:
 * <ul>
 * <li>the multicolour laser: a shimmering band wrapping the bottom 20 % of the capsule sphere and a rainbow beam down
 * (and up) the shaft;</li>
 * <li>in the transit dimension: the capsule itself and the space around it - stars, Earth with its atmosphere falling
 * away below, the beam still tying the capsule to Earth, the jump (stars stretch into streaks) and Elisar growing ahead.</li>
 * </ul>
 * Space is drawn right after the (empty) sky without depth, at most 260 blocks out, so the capsule always sits in front.
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID, value = Dist.CLIENT)
public final class FlightFx {
    public static final ResourceLocation TRANSIT_FX = new ResourceLocation(RiftWorld.MODID, "transit");
    private static final ResourceLocation CAPSULE = new ResourceLocation(RiftWorld.MODID, "models/glb/capsule.glb");
    private static final ResourceLocation EARTH = env("earth"), ELISAR = env("elisar_planet"), MOON = env("moon"), GLOW = env("glow");
    /** Sphere of the capsule (relative to the core block bottom-centre). */
    private static final float SPHERE_Y = 4.6F, SPHERE_R = 4.5F;
    private static final Vector3f SUN = new Vector3f(0.55F, 0.5F, 0.65F).normalize();
    private static final int STARS = 900;
    private static final float[] STAR = new float[STARS * 5];

    static {
        Random r = new Random(0x5EED);
        for (int i = 0; i < STARS; ++i) {
            Vector3f v = new Vector3f((float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian()).normalize();
            STAR[i * 5] = v.x;
            STAR[i * 5 + 1] = v.y;
            STAR[i * 5 + 2] = v.z;
            STAR[i * 5 + 3] = 0.35F + r.nextFloat() * r.nextFloat() * 1.1F;   // size
            STAR[i * 5 + 4] = r.nextFloat();                                  // hue / twinkle seed
        }
    }

    private FlightFx() {}

    private static ResourceLocation env(String n) {
        return new ResourceLocation(RiftWorld.MODID, "textures/environment/" + n + ".png");
    }

    private static boolean inTransit() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.dimension().equals(Flight.TRANSIT_DIM);
    }

    // ------------------------------------------------------------------------------------------------ transit sky / fog
    public static final class TransitEffects extends DimensionSpecialEffects {
        public TransitEffects() {
            super(Float.NaN, false, SkyType.NONE, false, false);
        }

        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 fog, float brightness) {
            return Vec3.ZERO;
        }

        @Override
        public boolean isFoggyAt(int x, int z) {
            return false;
        }
    }

    @SubscribeEvent
    public static void fogColor(ViewportEvent.ComputeFogColor e) {
        if (!inTransit()) return;
        e.setRed(0.004F);
        e.setGreen(0.006F);
        e.setBlue(0.016F);
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog e) {
        if (!inTransit()) return;
        e.setNearPlaneDistance(400F);
        e.setFarPlaneDistance(800F);
        e.setCanceled(true);
    }

    // ------------------------------------------------------------------------------------------------ render
    @SubscribeEvent
    public static void render(RenderLevelStageEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float pt = e.getPartialTick();
        Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = e.getPoseStack();
        boolean transit = inTransit();
        if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY && transit) {
            space(ps.last().pose(), FlightClient.phase == Flight.TRANSIT ? FlightClient.time(pt) / Flight.TRANSIT_T : 0, pt);
            return;
        }
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (transit) capsule(ps, cam, mc);
        if (FlightClient.phase == Flight.NONE) return;
        Vec3 base = Vec3.atBottomCenterOf(transit ? Flight.TRANSIT_CORE : FlightClient.core);
        float off = transit ? 0 : FlightClient.offset(pt);
        float t = (mc.level.getGameTime() % 100000L) + pt;
        Matrix4f m = ps.last().pose();
        Vector3f c = new Vector3f((float) (base.x - cam.x), (float) (base.y + off - cam.y), (float) (base.z - cam.z));
        beginAdditive();
        laserCap(m, c, t, 1F);
        if (!transit) {
            // the beam the capsule rides on: from the bottom of the shaft through the sphere and on into the sky
            float fade = FlightClient.phase == Flight.ARRIVE ? 1 - Mth.clamp((FlightClient.time(pt) - 52F) / 8F, 0, 1) : 1;
            Vector3f bottom = new Vector3f(c.x, (float) (base.y - LaunchCoreBlockEntity.DEPTH - 1 - cam.y), c.z);
            Vector3f under = new Vector3f(c.x, c.y + 0.15F, c.z);
            rainbowBeam(m, bottom, under, 0.42F, t, 0.85F * fade);
            Vector3f top = new Vector3f(c.x, c.y + SPHERE_Y + SPHERE_R, c.z), sky = new Vector3f(c.x, c.y + 160F, c.z);
            rainbowBeam(m, top, sky, 0.3F, t, 0.6F * fade);
        }
        endAdditive();
    }

    private static void capsule(PoseStack ps, Vec3 cam, Minecraft mc) {
        GlbAssets.Loaded c = GlbAssets.get(CAPSULE);
        if (c == null) return;
        Pose p = c.scratch();
        p.hide("lights_on", false);
        p.hide("screens_live", false);
        p.hide("screens_dead", true);
        p.compute();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        ps.pushPose();
        ps.translate(Flight.TRANSIT_CORE.getX() + 0.5 - cam.x, Flight.TRANSIT_CORE.getY() - cam.y, Flight.TRANSIT_CORE.getZ() + 0.5 - cam.z);
        GlbRenderer.render(c, p, ps, buf, LightTexture.pack(13, 13), OverlayTexture.NO_OVERLAY);
        ps.popPose();
        buf.endBatch();
    }

    // ------------------------------------------------------------------------------------------------ laser
    private static void beginAdditive() {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
    }

    private static void endAdditive() {
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static int hue(float h, float a) {
        h = h - Mth.floor(h);
        int rgb = Mth.hsvToRgb(h, 0.75F, 1F);
        return (Mth.clamp((int) (a * 255), 0, 255) << 24) | rgb;
    }

    private static void col(BufferBuilder b, Matrix4f m, float x, float y, float z, int argb) {
        b.vertex(m, x, y, z).color((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >>> 24)).endVertex();
    }

    /** Shimmering rainbow band over the lower 20 % of the sphere (y 0.1 .. 1.9 above the capsule floor block). */
    private static void laserCap(Matrix4f m, Vector3f c, float t, float a) {
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int seg = 48, rings = 5;
        for (int layer = 0; layer < 2; ++layer) {
            float R = SPHERE_R + 0.08F + layer * 0.07F;
            for (int k = 0; k < rings; ++k) {
                float y0 = 0.1F + 1.8F * k / rings, y1 = 0.1F + 1.8F * (k + 1) / rings;
                float s0 = y0 - SPHERE_Y, s1 = y1 - SPHERE_Y;
                float r0 = Mth.sqrt(Math.max(0, R * R - s0 * s0)), r1 = Mth.sqrt(Math.max(0, R * R - s1 * s1));
                float a0 = a * (0.75F - 0.6F * k / rings) * (layer == 0 ? 1 : 0.5F), a1 = a * (0.75F - 0.6F * (k + 1) / rings) * (layer == 0 ? 1 : 0.5F);
                if (k == rings - 1) a1 = 0;
                for (int i = 0; i < seg; ++i) {
                    float u0 = (float) i / seg, u1 = (float) (i + 1) / seg;
                    float th0 = u0 * Mth.TWO_PI, th1 = u1 * Mth.TWO_PI;
                    float sh = layer == 0 ? t * 0.02F : -t * 0.031F;
                    float w0 = 0.75F + 0.25F * Mth.sin(th0 * 6 + t * 0.4F + k), w1 = 0.75F + 0.25F * Mth.sin(th1 * 6 + t * 0.4F + k);
                    col(b, m, c.x + Mth.cos(th0) * r0, c.y + y0, c.z + Mth.sin(th0) * r0, hue(u0 + sh, a0 * w0));
                    col(b, m, c.x + Mth.cos(th1) * r0, c.y + y0, c.z + Mth.sin(th1) * r0, hue(u1 + sh, a0 * w1));
                    col(b, m, c.x + Mth.cos(th1) * r1, c.y + y1, c.z + Mth.sin(th1) * r1, hue(u1 + sh + 0.05F, a1 * w1));
                    col(b, m, c.x + Mth.cos(th0) * r1, c.y + y1, c.z + Mth.sin(th0) * r1, hue(u0 + sh + 0.05F, a1 * w0));
                }
            }
        }
        Tesselator.getInstance().end();
    }

    /** Six twisting coloured strands around the axis A-B, each a camera-facing strip, plus a white core. */
    private static void rainbowBeam(Matrix4f m, Vector3f A, Vector3f B, float rad, float t, float a) {
        if (a <= 0.01F) return;
        Vector3f ax = new Vector3f(B).sub(A);
        float len = ax.length();
        if (len < 0.01F) return;
        ax.div(len);
        Vector3f p1 = Math.abs(ax.y) < 0.9F ? new Vector3f(ax).cross(0, 1, 0).normalize() : new Vector3f(ax).cross(1, 0, 0).normalize();
        Vector3f p2 = new Vector3f(ax).cross(p1).normalize();
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int n = 6;
        for (int i = 0; i <= n; ++i) {
            boolean coreStrand = i == n;
            float phi = i * Mth.TWO_PI / n + t * 0.15F;
            float rr = coreStrand ? 0 : rad;
            Vector3f o = new Vector3f(p1).mul(Mth.cos(phi) * rr).add(new Vector3f(p2).mul(Mth.sin(phi) * rr));
            Vector3f a0 = new Vector3f(A).add(o), b0 = new Vector3f(B).add(o);
            Vector3f mid = new Vector3f(a0).add(b0).mul(0.5F);
            Vector3f side = new Vector3f(ax).cross(mid).normalize().mul(coreStrand ? rad * 0.55F : rad * 0.32F);
            int ca = coreStrand ? (((int) (a * 200)) << 24) | 0xFFFFFF : hue((float) i / n + t * 0.01F, a * 0.8F);
            col(b, m, a0.x - side.x, a0.y - side.y, a0.z - side.z, ca);
            col(b, m, a0.x + side.x, a0.y + side.y, a0.z + side.z, ca);
            col(b, m, b0.x + side.x, b0.y + side.y, b0.z + side.z, ca);
            col(b, m, b0.x - side.x, b0.y - side.y, b0.z - side.z, ca);
        }
        Tesselator.getInstance().end();
    }

    // ------------------------------------------------------------------------------------------------ space
    private static float smooth(float x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    /** u = 0..1 over the transit phase. */
    private static void space(Matrix4f m, float u, float pt) {
        Minecraft mc = Minecraft.getInstance();
        float t = (mc.level.getGameTime() % 100000L) + pt;
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        float speed = FlightClient.bell(u, 0.34F, 0.66F);          // the jump
        // stars (streaks during the jump: we fly towards -Z, the stars rush past towards +Z)
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Vector3f motion = new Vector3f(0, 0, 1);
        for (int i = 0; i < STARS; ++i) {
            Vector3f d = new Vector3f(STAR[i * 5], STAR[i * 5 + 1], STAR[i * 5 + 2]);
            float sz = STAR[i * 5 + 3], seed = STAR[i * 5 + 4];
            Vector3f p = new Vector3f(d).mul(240F);
            float tw = 0.65F + 0.35F * Mth.sin(t * 0.2F + seed * 40F);
            Vector3f r = new Vector3f(d).cross(0, 1, 0);
            if (r.lengthSquared() < 1e-4F) r.set(1, 0, 0);
            r.normalize().mul(sz * 0.5F);
            Vector3f up = new Vector3f(r).cross(d).normalize().mul(sz * 0.5F);
            float streak = speed * (40F + seed * 70F) * (1.05F - Math.abs(d.z));
            int ca = speed > 0.05F ? hue(seed, Math.min(1, 0.55F + speed)) : (((int) (tw * 235)) << 24) | 0xF4F2FF;
            if (streak > 1F) {
                Vector3f e2 = new Vector3f(p).add(new Vector3f(motion).mul(streak));
                Vector3f side = new Vector3f(motion).cross(p).normalize().mul(sz * 0.45F);
                col(b, m, p.x - side.x, p.y - side.y, p.z - side.z, ca);
                col(b, m, p.x + side.x, p.y + side.y, p.z + side.z, ca);
                col(b, m, e2.x + side.x, e2.y + side.y, e2.z + side.z, ca & 0x00FFFFFF);
                col(b, m, e2.x - side.x, e2.y - side.y, e2.z - side.z, ca & 0x00FFFFFF);
            } else {
                col(b, m, p.x - r.x - up.x, p.y - r.y - up.y, p.z - r.z - up.z, ca);
                col(b, m, p.x + r.x - up.x, p.y + r.y - up.y, p.z + r.z - up.z, ca);
                col(b, m, p.x + r.x + up.x, p.y + r.y + up.y, p.z + r.z + up.z, ca);
                col(b, m, p.x - r.x + up.x, p.y - r.y + up.y, p.z - r.z + up.z, ca);
            }
        }
        Tesselator.getInstance().end();

        // Earth: huge below at lift-off, falling away behind us
        // ahead and below through the glass at first, then sinking away behind and under the capsule
        // fills the lower half of the view ahead at first (above the capsule floor), then shrinks and drifts off to the right
        float aE = 0.74F * (float) Math.exp(-u * 3.4F) + 0.012F;
        Vector3f dirE = new Vector3f(0.05F + u * 1.5F, -0.3F - u * 0.2F, -1F + u * 0.55F).normalize();
        float D = 200F;
        Vector3f cE = new Vector3f(dirE).mul(D);
        float rE = D * Mth.sin(Math.min(aE, 1.1F));
        float fadeE = 1 - smooth((u - 0.62F) / 0.18F);
        if (fadeE > 0) {
            glow(m, cE, rE * 1.32F, 0.45F, 0.7F, 1F, 0.85F * fadeE);
            RenderSystem.defaultBlendFunc();
            sphere(m, EARTH, cE, rE, t * 0.0009F, fadeE, 1F, 1F, 1F);
            // the moon a little to the side
            Vector3f cM = new Vector3f(dirE).add(0.55F, 0.25F, 0.15F).normalize().mul(D);
            RenderSystem.defaultBlendFunc();
            sphere(m, MOON, cM, rE * 0.18F, t * 0.0004F, fadeE, 1F, 1F, 1F);
            // the beam that still ties the capsule to Earth
            float beamA = 1 - smooth((u - 0.12F) / 0.18F);
            if (beamA > 0) {
                beginAdditive();
                RenderSystem.disableDepthTest();
                Vector3f cap = new Vector3f(0, -2.4F, 0);
                rainbowBeam(m, cap, new Vector3f(cE).mul(0.97F), 0.55F, t, 0.9F * beamA);
            }
        }

        // Elisar ahead: a dot first, then filling the view
        float g = smooth((u - 0.3F) / 0.7F);
        float aP = 0.006F + 0.82F * g * g * g;
        Vector3f dirP = new Vector3f(0.12F, 0.2F - 0.3F * g, -1).normalize();
        Vector3f cP = new Vector3f(dirP).mul(D);
        float rP = D * Mth.sin(Math.min(aP, 1.1F));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        glow(m, cP, rP * 1.45F, 1F, 0.97F, 0.9F, Mth.clamp((u - 0.22F) * 5F, 0, 1) * 0.8F);
        RenderSystem.defaultBlendFunc();
        sphere(m, ELISAR, cP, rP, -t * 0.0006F, Mth.clamp((u - 0.22F) * 5F, 0, 1), 1F, 1F, 1F);

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** Textured lat-long sphere lit from {@link #SUN}; back faces are skipped on the CPU (no depth here). */
    private static void sphere(Matrix4f m, ResourceLocation tex, Vector3f c, float R, float spin, float a, float tr, float tg, float tb) {
        if (R < 0.05F || a <= 0.01F) return;
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, tex);
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        int LON = 40, LAT = 20;
        for (int j = 0; j < LAT; ++j) {
            for (int i = 0; i < LON; ++i) {
                float u0 = (float) i / LON, u1 = (float) (i + 1) / LON, v0 = (float) j / LAT, v1 = (float) (j + 1) / LAT;
                float cu = (u0 + u1) / 2, cv = (v0 + v1) / 2;
                Vector3f nc = sp(cu, cv);
                Vector3f pc = new Vector3f(nc).mul(R).add(c);
                if (nc.dot(pc) > 0) continue;                         // faces away from the camera (camera at the origin)
                v(b, m, c, R, u0, v0, spin, a, tr, tg, tb);
                v(b, m, c, R, u0, v1, spin, a, tr, tg, tb);
                v(b, m, c, R, u1, v1, spin, a, tr, tg, tb);
                v(b, m, c, R, u1, v0, spin, a, tr, tg, tb);
            }
        }
        Tesselator.getInstance().end();
    }

    private static Vector3f sp(float u, float v) {
        float th = u * Mth.TWO_PI, ph = v * Mth.PI;
        return new Vector3f(Mth.cos(th) * Mth.sin(ph), Mth.cos(ph), Mth.sin(th) * Mth.sin(ph));
    }

    private static void v(BufferBuilder b, Matrix4f m, Vector3f c, float R, float u, float vv, float spin, float a, float tr, float tg, float tb) {
        Vector3f n = sp(u, vv);
        float lit = 0.18F + 0.95F * Math.max(0, n.dot(SUN));
        lit = Math.min(1F, lit);
        b.vertex(m, c.x + n.x * R, c.y + n.y * R, c.z + n.z * R).uv(u + spin, vv).color(tr * lit, tg * lit, tb * lit, a).endVertex();
    }

    /** Soft additive halo, always facing the camera. */
    private static void glow(Matrix4f m, Vector3f c, float size, float r, float g, float bl, float a) {
        if (a <= 0.01F || size < 0.05F) return;
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, GLOW);
        Vector3f d = new Vector3f(c).normalize();
        Vector3f rt = new Vector3f(d).cross(0, 1, 0);
        if (rt.lengthSquared() < 1e-4F) rt.set(1, 0, 0);
        rt.normalize().mul(size);
        Vector3f up = new Vector3f(rt).cross(d).normalize().mul(size);
        Vector3f p = new Vector3f(c).sub(new Vector3f(d).mul(size * 0.2F));
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        b.vertex(m, p.x - rt.x - up.x, p.y - rt.y - up.y, p.z - rt.z - up.z).uv(0, 1).color(r, g, bl, a).endVertex();
        b.vertex(m, p.x + rt.x - up.x, p.y + rt.y - up.y, p.z + rt.z - up.z).uv(1, 1).color(r, g, bl, a).endVertex();
        b.vertex(m, p.x + rt.x + up.x, p.y + rt.y + up.y, p.z + rt.z + up.z).uv(1, 0).color(r, g, bl, a).endVertex();
        b.vertex(m, p.x - rt.x + up.x, p.y - rt.y + up.y, p.z - rt.z + up.z).uv(0, 0).color(r, g, bl, a).endVertex();
        Tesselator.getInstance().end();
        RenderSystem.defaultBlendFunc();
    }
}
