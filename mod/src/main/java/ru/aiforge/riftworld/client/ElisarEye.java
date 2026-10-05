package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import ru.aiforge.riftworld.RiftWorld;

/**
 * The eye in the Elisar sky (reference loc12): a huge, faint pixel-line eye fixed in the north-east sky.
 * Drawn right after the sky, so every island and the fog are in front of it - it can never be reached.
 * It blinks every ~9 s, sleeps (closed lid with hanging lashes) at night and its iris follows the player.
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID, value = Dist.CLIENT)
public final class ElisarEye {
    private static final ResourceLocation OPEN = tex("eye_open"), HALF = tex("eye_half"), CLOSED = tex("eye_closed"), IRIS = tex("eye_iris");
    private static final Vector3f DIR = new Vector3f(0.70F, 0.40F, -0.62F).normalize();
    private static final float DIST = 100F, W = 78F, H = 39F, ALPHA = 0.42F;

    private ElisarEye() {}

    private static ResourceLocation tex(String n) {
        return new ResourceLocation(RiftWorld.MODID, "textures/environment/" + n + ".png");
    }

    /** 0 = open, 1 = half, 2 = closed. */
    static int lid(long gameTime, long dayTime, float pt) {
        long d = Math.floorMod(dayTime, 24000L);
        if (d >= 13000 && d < 23000) return 2;
        if (d >= 12600 && d < 13000 || d >= 23000 && d < 23400) return 1;
        double p = (gameTime + pt) % 180.0;
        if (p < 2.5 || (p >= 5.5 && p < 8)) return 1;
        if (p < 5.5) return 2;
        return 0;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().location().equals(ElisarSky.ID)) return;
        float pt = e.getPartialTick();
        int lid = lid(mc.level.getGameTime(), mc.level.getDayTime(), pt);
        var cam = e.getCamera().getPosition();

        Vector3f right = new Vector3f(DIR).cross(0, 1, 0).normalize();
        Vector3f up = new Vector3f(right).cross(DIR).normalize();
        Vector3f c = new Vector3f(DIR).mul(DIST);
        Matrix4f m = e.getPoseStack().last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        if (lid == 0) {
            // the iris looks at the player: offset by where the camera stands relative to the city, plus a slow drift
            double t = (mc.level.getGameTime() + pt) * 0.01;
            float ox = (float) Mth.clamp(-(cam.x * right.x + cam.z * right.z) / 350.0 + Math.sin(t) * 0.15, -1, 1) * W * 0.045F;
            float oy = (float) Mth.clamp((cam.y - 110) / 120.0 + Math.cos(t * 0.7) * 0.15, -1, 1) * H * 0.07F;
            Vector3f ci = new Vector3f(c).add(new Vector3f(right).mul(ox)).add(new Vector3f(up).mul(oy)).add(new Vector3f(DIR).mul(-0.2F));
            quad(m, IRIS, ci, right, up, ALPHA);
        }
        quad(m, lid == 0 ? OPEN : lid == 1 ? HALF : CLOSED, c, right, up, ALPHA);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    private static void quad(Matrix4f m, ResourceLocation tex, Vector3f c, Vector3f r, Vector3f u, float a) {
        RenderSystem.setShaderTexture(0, tex);
        float hw = W / 2, hh = H / 2;
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        v(b, m, c, r, u, -hw, hh, 0, 0, a);
        v(b, m, c, r, u, -hw, -hh, 0, 1, a);
        v(b, m, c, r, u, hw, -hh, 1, 1, a);
        v(b, m, c, r, u, hw, hh, 1, 0, a);
        Tesselator.getInstance().end();
    }

    private static void v(BufferBuilder b, Matrix4f m, Vector3f c, Vector3f r, Vector3f u, float x, float y, float tu, float tv, float a) {
        b.vertex(m, c.x + r.x * x + u.x * y, c.y + r.y * x + u.y * y, c.z + r.z * x + u.z * y).uv(tu, tv).color(1F, 1F, 1F, a).endVertex();
    }
}
