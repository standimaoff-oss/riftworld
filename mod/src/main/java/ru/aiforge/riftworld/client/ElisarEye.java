package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import ru.aiforge.riftworld.RiftWorld;

/**
 * 2D-глаз в северо-восточном небе Элисара ({@code DIR = (0.70, +0.40, -0.62)}):
 * <ul>
 *   <li>Рендерится в {@code AFTER_TRANSLUCENT_BLOCKS} на динамической небесной дистанции
 *       {@link ShaderCompat#celestialDistance(Minecraft)} со включённым {@code DepthTest (GL_LEQUAL)}:
 *       <b>любой твёрдый блок, стена, потолок, дерево или далёкий остров на 100% перекрывает глаз —
 *       никакого просвечивания сквозь блоки!</b></li>
 *   <li>Пепельная вуаль {@code ashVeil} рисуется только в ванили в {@code AFTER_SKY} гладким сферическим
 *       куполом (в шейдерпаках она отключена, чтобы не ломать небесный G-буфер IterationT и др.).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID, value = Dist.CLIENT)
public final class ElisarEye {
    public static final ResourceLocation OPEN = tex("eye_open");
    public static final ResourceLocation HALF = tex("eye_half");
    public static final ResourceLocation CLOSED = tex("eye_closed");
    public static final ResourceLocation IRIS = tex("eye_iris");

    public static final Vector3f DIR = new Vector3f(0.70F, 0.40F, -0.62F).normalize();
    private static final float BASE_DIST = 100.0F;
    private static final float BASE_W = 78.0F;
    private static final float BASE_H = 39.0F;
    private static final float ALPHA = 0.52F;

    private ElisarEye() {
    }

    private static ResourceLocation tex(String n) {
        return new ResourceLocation(RiftWorld.MODID, "textures/environment/" + n + ".png");
    }

    /** 0 = open, 1 = half, 2 = closed. */
    static int lid(long gameTime, long dayTime, float pt) {
        long d = Math.floorMod(dayTime, 24000L);
        if (d >= 13000 && d < 23000) return 2;
        if (d >= 12600 && d < 13000 || d >= 23000 && d < 23400) return 1;
        double p = (gameTime + pt) % 180.0;
        if (p < 2.5 || (p >= 5.5 && p < 8.0)) return 1;
        if (p < 5.5) return 2;
        return 0;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent e) {
        if (ShaderCompat.isRenderingShadowPass()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().location().equals(ElisarSky.ID)) return;
        if (e.getCamera().getFluidInCamera() != FogType.NONE) return;

        var cam = e.getCamera().getPosition();
        float gloom = ElisarSky.Fog.gloom(cam);
        Matrix4f m = e.getPoseStack().last().pose();

        // 1. В ванили (без шейдерпака) пепельная вуаль над Адскими угодьями рисуется строго в AFTER_SKY сферой
        if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            if (!ShaderCompat.isShaderPackActive() && gloom > 0.002F) {
                int aTop = Mth.clamp(Math.round(0.80F * gloom * 255.0F), 0, 255);
                int aHor = Mth.clamp(Math.round(0.62F * gloom * 255.0F), 0, 255);
                float radius = Math.min(mc.gameRenderer.getDepthFar() * 0.50F, 170.0F);
                ShaderCompat.drawSphericalSkyDome(m, radius,
                        69, 60, 56, aTop,
                        69, 60, 56, aHor,
                        69, 60, 56, aHor);
            }
            return;
        }

        // 2. Сам небесный 2D-глаз рисуется ПОСЛЕ всех блоков на небесной дистанции skyDist с тестом глубины GL_LEQUAL!
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (gloom > 0.95F) return;

        float pt = e.getPartialTick();
        int lidState = lid(mc.level.getGameTime(), mc.level.getDayTime(), pt);

        float skyDist = ShaderCompat.celestialDistance(mc);
        float scale = skyDist / BASE_DIST;
        float w = BASE_W * scale;
        float h = BASE_H * scale;

        Vector3f right = new Vector3f(DIR).cross(0, 1, 0).normalize();
        Vector3f up = new Vector3f(right).cross(DIR).normalize();
        Vector3f c = new Vector3f(DIR).mul(skyDist);

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        float alpha = ALPHA * (1.0F - gloom);
        if (lidState == 0) {
            double t = (mc.level.getGameTime() + pt) * 0.01;
            float ox = (float) Mth.clamp(-(cam.x * right.x + cam.z * right.z) / 350.0 + Math.sin(t) * 0.15, -1.0, 1.0) * w * 0.045F;
            float oy = (float) Mth.clamp((cam.y - 110.0) / 120.0 + Math.cos(t * 0.7) * 0.15, -1.0, 1.0) * h * 0.07F;
            Vector3f ci = new Vector3f(c).add(new Vector3f(right).mul(ox)).add(new Vector3f(up).mul(oy));
            drawWorldSkyQuad(m, IRIS, ci, right, up, w * 0.5F, h * 0.5F, alpha);
        }
        drawWorldSkyQuad(m, lidState == 0 ? OPEN : (lidState == 1 ? HALF : CLOSED), c, right, up, w * 0.5F, h * 0.5F, alpha);

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /**
     * Рисует 2D-квад небесного глаза Элисара в мировом направлении {@code center} (с векторами {@code right}, {@code up})
     * при активном {@code POSITION_TEX_COLOR} и {@code depthMask(false)}.
     */
    public static void drawWorldSkyQuad(Matrix4f m, ResourceLocation tex,
                                        Vector3f center, Vector3f right, Vector3f up,
                                        float hw, float hh, float alpha) {
        int a = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
        if (a <= 1) return;
        RenderSystem.setShaderTexture(0, tex);
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        v(b, m, center, right, up, -hw,  hh, 0.0F, 0.0F, a);
        v(b, m, center, right, up, -hw, -hh, 0.0F, 1.0F, a);
        v(b, m, center, right, up,  hw, -hh, 1.0F, 1.0F, a);
        v(b, m, center, right, up,  hw,  hh, 1.0F, 0.0F, a);
        Tesselator.getInstance().end();
    }

    private static void v(BufferBuilder b, Matrix4f m, Vector3f c, Vector3f r, Vector3f u,
                          float x, float y, float tu, float tv, int alphaByte) {
        b.vertex(m, c.x + r.x * x + u.x * y, c.y + r.y * x + u.y * y, c.z + r.z * x + u.z * y)
                .uv(tu, tv)
                .color(255, 255, 255, alphaByte)
                .endVertex();
    }
}
