package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.flight.PortalRitual;
import ru.aiforge.riftworld.net.PortalRitualMsg;

/**
 * v0.21.0: Обновлённая клиентская визуализация бесшовного телепорта через 3D-Прорыв Пространства в Элисар
 * (<b>100% совместимо с любыми шейдерпаками, {@code mutated = 0} — ни один блок в коде не меняется!</b>):
 * <ul>
 *   <li>Окружающий мир окрашивается НЕ в плоский белый цвет, а в фирменный тёплый известняково-кремовый оттенок
 *       измерения Элисар ({@code ~#D4CEBE}), а <b>прямо поверх пола накладываются резные геометрические и
 *       рунические узоры Элисара</b> ({@code portal_floor_tile.png} + радиальная мандала {@code portal_floor_mandala.png});</li>
 *   <li>Вместо мелких 3D-глаз рядом с игроком <b>высоко в куполе неба</b> (точно как в измерении Элисар!)
 *       открываются величественные 2D-глаза {@code eye_closed/eye_half/eye_open/eye_iris.png} на небесной дистанции
 *       {@link ShaderCompat#celestialDistance} с честным тестом глубины {@code GL_LEQUAL} (не просвечиваются сквозь блоки!);</li>
 *   <li>Финальная резкая вспышка света Элисара и мгновенный перенос без экрана загрузки.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID, value = Dist.CLIENT)
public final class PortalRitualClient {
    private static final ResourceLocation FLOOR_TILE_TEX =
            new ResourceLocation(RiftWorld.MODID, "textures/environment/portal_floor_tile.png");
    private static final ResourceLocation FLOOR_MANDALA_TEX =
            new ResourceLocation(RiftWorld.MODID, "textures/environment/portal_floor_mandala.png");

    private static final int MAX_FACES = 36000;
    private static final int BLEACH_SCAN_R = 34;
    private static final int SKY_EYE_COUNT = 16;

    public static int phase = PortalRitual.NONE;
    public static BlockPos center = BlockPos.ZERO;
    public static int tick = 0;

    // Кэш открытых граней блоков вокруг портала (ТОЛЬКО ЧТЕНИЕ из ClientLevel, 0 изменений блоков!)
    private static int faceCount = 0;
    private static final float[] fMinX = new float[MAX_FACES];
    private static final float[] fMinY = new float[MAX_FACES];
    private static final float[] fMinZ = new float[MAX_FACES];
    private static final float[] fMaxX = new float[MAX_FACES];
    private static final float[] fMaxY = new float[MAX_FACES];
    private static final float[] fMaxZ = new float[MAX_FACES];
    private static final byte[] fDir = new byte[MAX_FACES];
    private static final float[] fDist = new float[MAX_FACES];
    private static BlockPos capturedFor = null;

    // Мировые единичные векторы направления к 16 небесным 2D-глазам Элисара (ВСЕ строго высоко в небе dy > 0!)
    private static final Vector3f[] skyEyeDir = new Vector3f[SKY_EYE_COUNT];
    private static final float[] skyEyeScale = new float[SKY_EYE_COUNT];
    private static final float[] skyEyeWake = new float[SKY_EYE_COUNT];
    private static final float[] skyEyeSeed = new float[SKY_EYE_COUNT];

    static {
        // Первые 4 небесных глаза обрамляют в вышине ствол уходящего в небо Разлома на севере (-Z),
        // а остальные 12 равномерно раскрываются по всему верхнему куполу неба (24° .. 66° над горизонтом)!
        float[][] curated = {
                {-0.42F, 0.44F, -0.79F, 0.88F},
                { 0.44F, 0.52F, -0.73F, 0.80F},
                {-0.18F, 0.66F, -0.73F, 0.64F},
                { 0.22F, 0.34F, -0.91F, 0.58F}
        };
        for (int i = 0; i < SKY_EYE_COUNT; ++i) {
            if (i < curated.length) {
                skyEyeDir[i] = new Vector3f(curated[i][0], curated[i][1], curated[i][2]).normalize();
                skyEyeScale[i] = curated[i][3];
            } else {
                double az = ((i - curated.length) * 2.39996323) + 0.55;
                double el = Math.toRadians(26.0 + ((i * 7) % 8) * 5.2); // строго +26°..+63° вверх в небе!
                float dx = (float) (Math.cos(az) * Math.cos(el));
                float dy = (float) Math.sin(el);
                float dz = (float) (Math.sin(az) * Math.cos(el));
                skyEyeDir[i] = new Vector3f(dx, dy, dz).normalize();
                skyEyeScale[i] = 0.48F + (i % 5) * 0.10F;
            }
            skyEyeWake[i] = 26.0F + (52.0F * i) / SKY_EYE_COUNT;
            skyEyeSeed[i] = i * 1.917F;
        }
    }

    private PortalRitualClient() {
    }

    public static void onMsg(PortalRitualMsg m) {
        int prevPhase = phase;
        phase = m.phase();
        center = m.center();
        tick = m.elapsed();

        Minecraft mc = Minecraft.getInstance();
        if (phase == PortalRitual.AWAKEN && (prevPhase != PortalRitual.AWAKEN || !center.equals(capturedFor))) {
            if (mc.level != null) {
                captureBleachFaces(mc.level, center);
            }
        } else if (phase == PortalRitual.NONE) {
            capturedFor = null;
            faceCount = 0;
            if (mc.screen instanceof WhiteReceivingScreen) {
                mc.setScreen(null);
            }
        }
    }

    public static boolean isNearActivePortal(BlockPos pos) {
        return phase == PortalRitual.AWAKEN && center.distSqr(pos) <= 36.0;
    }

    public static float currentTime(float pt) {
        return tick + pt;
    }

    /** Текущий радиус волны оттенка Элисара и напольных узоров от центра Разлома (0 -> 37 блоков). */
    public static float bleachRadius(float pt) {
        if (phase != PortalRitual.AWAKEN) return 0.0F;
        float u = Mth.clamp(currentTime(pt) / 90.0F, 0.0F, 1.0F);
        float s = u * u * (3.0F - 2.0F * u);
        return 0.9F + 36.5F * s;
    }

    /** Яркость резкой вспышки света Элисара (0..1) на границе переноса без экрана загрузки. */
    public static float flashIntensity(float pt) {
        float t = currentTime(pt);
        if (phase == PortalRitual.AWAKEN) {
            float f = Mth.clamp((t - (PortalRitual.AWAKEN_TICKS - 8.0F)) / 6.5F, 0.0F, 1.0F);
            return f * f;
        }
        if (phase == PortalRitual.ARRIVE_FLASH) {
            if (t <= 4.0F) return 1.0F;
            float fade = 1.0F - Mth.clamp((t - 4.0F) / (PortalRitual.ARRIVE_TICKS - 4.0F), 0.0F, 1.0F);
            return fade * fade;
        }
        return 0.0F;
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || phase == PortalRitual.NONE) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;
        if (phase == PortalRitual.AWAKEN && capturedFor == null && mc.level != null) {
            captureBleachFaces(mc.level, center);
        }
        int maxLen = phase == PortalRitual.AWAKEN ? PortalRitual.AWAKEN_TICKS + 6 : PortalRitual.ARRIVE_TICKS + 4;
        if (tick < maxLen) {
            ++tick;
        } else if (phase == PortalRitual.ARRIVE_FLASH) {
            phase = PortalRitual.NONE;
            if (mc.screen instanceof WhiteReceivingScreen) {
                mc.setScreen(null);
            }
        }
    }

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening e) {
        if (phase == PortalRitual.NONE) return;
        var s = e.getNewScreen();
        if ((s instanceof ReceivingLevelScreen || s instanceof ProgressScreen || s instanceof GenericDirtMessageScreen)
                && !(s instanceof WhiteReceivingScreen)) {
            e.setNewScreen(new WhiteReceivingScreen());
        }
    }

    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Pre e) {
        if (phase == PortalRitual.NONE) return;
        float a = flashIntensity(e.getPartialTick());
        if (a <= 0.004F) return;
        GuiGraphics g = e.getGuiGraphics();
        int alpha = Mth.clamp(Math.round(a * 255.0F), 0, 255);
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), (alpha << 24) | 0xFFF7EFE0);
    }

    @SubscribeEvent
    public static void fogColor(ViewportEvent.ComputeFogColor e) {
        if (phase != PortalRitual.AWAKEN) return;
        float u = Mth.clamp((currentTime((float) e.getPartialTick()) - 12.0F) / 68.0F, 0.0F, 1.0F);
        float k = u * u * (3.0F - 2.0F * u);
        // Точный тёплый известняково-кремовый тон неба Элисара (чтобы 2D-глаза в небе читались контрастно 1:1 как в Элисаре!)
        e.setRed(Mth.lerp(k, e.getRed(), 0.945F));
        e.setGreen(Mth.lerp(k, e.getGreen(), 0.928F));
        e.setBlue(Mth.lerp(k, e.getBlue(), 0.888F));
    }

    @SubscribeEvent
    public static void renderFog(ViewportEvent.RenderFog e) {
        if (phase != PortalRitual.AWAKEN || e.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;
        if (e.getCamera().getFluidInCamera() != FogType.NONE) return;
        if (ShaderCompat.isShaderPackActive()) return;
        float u = Mth.clamp((currentTime((float) e.getPartialTick()) - 18.0F) / 70.0F, 0.0F, 1.0F);
        if (u <= 0.001F) return;
        float k = u * u * (3.0F - 2.0F * u);
        float far = e.getFarPlaneDistance();
        e.setNearPlaneDistance(Mth.lerp(k, far * 0.65F, 24.0F));
        e.setFarPlaneDistance(Mth.lerp(k, far, 76.0F));
        e.setCanceled(true);
    }

    @SubscribeEvent
    public static void renderStage(RenderLevelStageEvent e) {
        if (phase == PortalRitual.NONE) return;
        if (ShaderCompat.isRenderingShadowPass()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float pt = e.getPartialTick();
        Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = e.getPoseStack();

        // Плавная небесная вуаль оттенка Элисара в ванили, чтобы обычное голубое небо мягко превращалось в небо Элисара
        if (phase == PortalRitual.AWAKEN && e.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY
                && !ShaderCompat.isShaderPackActive()) {
            float u = Mth.clamp((currentTime(pt) - 10.0F) / 68.0F, 0.0F, 1.0F);
            int skyA = Mth.clamp(Math.round(u * u * (3.0F - 2.0F * u) * 238.0F), 0, 238);
            if (skyA > 2) {
                float r = Math.min(mc.gameRenderer.getDepthFar() * 0.52F, 180.0F);
                ShaderCompat.drawSphericalSkyDome(ps.last().pose(), r,
                        242, 238, 228, skyA,
                        231, 226, 214, skyA,
                        216, 210, 196, skyA);
            }
            return;
        }

        if (phase == PortalRitual.AWAKEN && e.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            renderElisarSurfaceAndFloorPatterns(ps, cam, pt);
        } else if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            if (phase == PortalRitual.AWAKEN) {
                renderCelestial2DEyesInSky(mc, ps, e.getCamera(), pt);
            }
            if (mc.options.hideGui) {
                renderCameraFlashVeil(ps, e.getCamera(), pt);
            }
        }
    }

    private static void renderCameraFlashVeil(PoseStack ps, Camera camera, float pt) {
        float fl = flashIntensity(pt);
        if (fl <= 0.005F) return;
        int alpha = Mth.clamp(Math.round(fl * 255.0F), 0, 255);
        Matrix4f mat = ps.last().pose();
        Vector3f look = camera.getLookVector();
        Vector3f leftV = camera.getLeftVector();
        Vector3f upV = camera.getUpVector();
        Vec3 fwd = new Vec3(look.x(), look.y(), look.z()).scale(0.18);
        Vec3 right = new Vec3(-leftV.x(), -leftV.y(), -leftV.z()).scale(1.8);
        Vec3 up = new Vec3(upV.x(), upV.y(), upV.z()).scale(1.8);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, ShaderCompat.WHITE_TEX);

        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Vec3 p0 = fwd.subtract(right).add(up);
        Vec3 p1 = fwd.subtract(right).subtract(up);
        Vec3 p2 = fwd.add(right).subtract(up);
        Vec3 p3 = fwd.add(right).add(up);
        bb.vertex(mat, (float) p0.x, (float) p0.y, (float) p0.z).uv(0.5F, 0.5F).color(247, 239, 224, alpha).endVertex();
        bb.vertex(mat, (float) p1.x, (float) p1.y, (float) p1.z).uv(0.5F, 0.5F).color(247, 239, 224, alpha).endVertex();
        bb.vertex(mat, (float) p2.x, (float) p2.y, (float) p2.z).uv(0.5F, 0.5F).color(247, 239, 224, alpha).endVertex();
        bb.vertex(mat, (float) p3.x, (float) p3.y, (float) p3.z).uv(0.5F, 0.5F).color(247, 239, 224, alpha).endVertex();
        Tesselator.getInstance().end();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /**
     * 1) Боковые/нижние грани блоков и деревьев плавно окрашивает в тёплый известняково-песчаниковый
     *    оттенок измерения Элисар (НЕ просто белый!);
     * 2) На все верхние грани пола ({@code UP}) прямо накладывает резные храмовые плиты и золотые рунические
     *    узоры Элисара ({@link #FLOOR_TILE_TEX} + радиальную мандалу {@link #FLOOR_MANDALA_TEX})!
     * Ни один блок в мире при этом не изменяется ({@code mutated = 0}).
     */
    private static void renderElisarSurfaceAndFloorPatterns(PoseStack ps, Vec3 cam, float pt) {
        if (faceCount <= 0) return;
        float rad = bleachRadius(pt);
        if (rad <= 0.2F) return;

        Matrix4f mat = ps.last().pose();
        float camX = (float) cam.x, camY = (float) cam.y, camZ = (float) cam.z;
        float rcx = center.getX() + 0.5F;
        float rcz = center.getZ() + 0.5F;
        float t = currentTime(pt);

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.polygonOffset(-1.5F, -2.5F);
        RenderSystem.enablePolygonOffset();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);

        BufferBuilder bb = Tesselator.getInstance().getBuilder();

        // ПРОХОД A: Боковые и нижние грани (стволы, листва, камни руин) -> тёплый известняково-пергаментный оттенок Элисара
        RenderSystem.setShaderTexture(0, ShaderCompat.WHITE_TEX);
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < faceCount; ++i) {
            if (fDir[i] == 1) continue; // пол (UP) рисуем во втором проходе сразу с узором Элисара!
            float d = fDist[i];
            if (d > rad) continue;
            float u = Mth.clamp((rad - d) / 5.4F, 0.0F, 1.0F);
            float w = u * u * (3.0F - 2.0F * u);
            int a = Mth.clamp(Math.round(w * 218.0F), 0, 255);
            if (a <= 2) continue;

            // Истинный тёплый оттенок известняка/травы Элисара (из elisar_grass_top & temple_gold_rune: ~211, 205, 189)
            int r, g, b;
            switch (fDir[i]) {
                case 0 -> { r = 166; g = 159; b = 144; } // DOWN
                case 2, 3 -> { r = 198; g = 191; b = 175; } // NORTH / SOUTH
                default -> { r = 184; g = 177; b = 161; }   // WEST / EAST
            }

            float x0 = fMinX[i] - camX, y0 = fMinY[i] - camY, z0 = fMinZ[i] - camZ;
            float x1 = fMaxX[i] - camX, y1 = fMaxY[i] - camY, z1 = fMaxZ[i] - camZ;
            float eps = 0.004F;

            switch (fDir[i]) {
                case 0 -> {
                    float yn = y0 - eps;
                    bb.vertex(mat, x0, yn, z1).uv(0.5F, 0.5F).color(r, g, b, a).endVertex();
                    bb.vertex(mat, x0, yn, z0).uv(0.5F, 0.5F).color(r, g, b, a).endVertex();
                    bb.vertex(mat, x1, yn, z0).uv(0.5F, 0.5F).color(r, g, b, a).endVertex();
                    bb.vertex(mat, x1, yn, z1).uv(0.5F, 0.5F).color(r, g, b, a).endVertex();
                }
                case 2 -> {
                    float zn = z0 - eps;
                    bb.vertex(mat, x1, y1, zn).uv(0.5F, 0.5F).color(r, g, b, a).endVertex();
                    bb.vertex(mat, x1, y0, zn).uv(0.5F, 0.5F).color(r, g, b, a).endVertex();
                    bb.vertex(mat, x0, y0, zn).uv(0.5F, 0.5F).color(r, g, b, a).endVertex();
                    bb.vertex(mat, x0, y1, zn).uv(0.5F, 0.5F).color(r, g, b, a).endVertex();
                }
                case 3 -> {
                    float zp = z1 + eps;
                    bb.vertex(mat, x0, y1, zp).uv(0.5F, 0.5F).color(r, g, b, a).endVertex();
                    bb.vertex(mat, x0, y0, zp).uv(0.5F, 0.5F).color(r, g, b, a).endVertex();
                    bb.vertex(mat, x1, y0, zp).uv(0.5F, 0.5F).color(r, g, b, a).endVertex();
                    bb.vertex(mat, x1, y1, zp).uv(0.5F, 0.5F).color(r, g, b, a).endVertex();
                }
                case 4 -> {
                    float xn = x0 - eps;
                    bb.vertex(mat, xn, y1, z0).uv(0.5F, 0.5F).color(r, g, b, a).endVertex();
                    bb.vertex(mat, xn, y0, z0).uv(0.5F, 0.5F).color(r, g, b, a).endVertex();
                    bb.vertex(mat, xn, y0, z1).uv(0.5F, 0.5F).color(r, g, b, a).endVertex();
                    bb.vertex(mat, xn, y1, z1).uv(0.5F, 0.5F).color(r, g, b, a).endVertex();
                }
                case 5 -> {
                    float xp = x1 + eps;
                    bb.vertex(mat, xp, y1, z1).uv(0.5F, 0.5F).color(r, g, b, a).endVertex();
                    bb.vertex(mat, xp, y0, z1).uv(0.5F, 0.5F).color(r, g, b, a).endVertex();
                    bb.vertex(mat, xp, y0, z0).uv(0.5F, 0.5F).color(r, g, b, a).endVertex();
                    bb.vertex(mat, xp, y1, z0).uv(0.5F, 0.5F).color(r, g, b, a).endVertex();
                }
            }
        }
        Tesselator.getInstance().end();

        // ПРОХОД B: Все верхние грани пола (UP) — бесшовный храмовый узор Элисара (4x4 м медальоны + золотая инкрустация)
        RenderSystem.setShaderTexture(0, FLOOR_TILE_TEX);
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < faceCount; ++i) {
            if (fDir[i] != 1) continue;
            float d = fDist[i];
            if (d > rad) continue;
            float u = Mth.clamp((rad - d) / 5.0F, 0.0F, 1.0F);
            float w = u * u * (3.0F - 2.0F * u);
            // Золотистый бегущий гребень прямо на кромке волны!
            float crest = Mth.clamp(1.0F - Math.abs((rad - d) - 1.2F) / 1.4F, 0.0F, 1.0F);
            int tr = Mth.clamp(Math.round(Mth.lerp(crest * 0.35F, 255.0F, 255.0F)), 0, 255);
            int tg = Mth.clamp(Math.round(Mth.lerp(crest * 0.35F, 255.0F, 236.0F)), 0, 255);
            int tb = Mth.clamp(Math.round(Mth.lerp(crest * 0.45F, 255.0F, 178.0F)), 0, 255);
            int a = Mth.clamp(Math.round(w * 244.0F), 0, 255);
            if (a <= 2) continue;

            float wx0 = fMinX[i], wz0 = fMinZ[i];
            float wx1 = fMaxX[i], wz1 = fMaxZ[i];
            float yp = (fMaxY[i] - camY) + 0.004F;

            // Мировые UV с периодом 4x4 блока, центрированные относительно оси Разлома
            float u0 = (wx0 - rcx + 2.0F) * 0.25F;
            float u1 = (wx1 - rcx + 2.0F) * 0.25F;
            float v0 = (wz0 - rcz + 2.0F) * 0.25F;
            float v1 = (wz1 - rcz + 2.0F) * 0.25F;

            bb.vertex(mat, wx0 - camX, yp, wz0 - camZ).uv(u0, v0).color(tr, tg, tb, a).endVertex();
            bb.vertex(mat, wx0 - camX, yp, wz1 - camZ).uv(u0, v1).color(tr, tg, tb, a).endVertex();
            bb.vertex(mat, wx1 - camX, yp, wz1 - camZ).uv(u1, v1).color(tr, tg, tb, a).endVertex();
            bb.vertex(mat, wx1 - camX, yp, wz0 - camZ).uv(u1, v0).color(tr, tg, tb, a).endVertex();
        }
        Tesselator.getInstance().end();

        // ПРОХОД C: Прямо поверх пола вокруг Разлома (радиус 16 блоков) — Большая Руническая Мандала Элисара!
        RenderSystem.polygonOffset(-2.5F, -4.0F);
        RenderSystem.setShaderTexture(0, FLOOR_MANDALA_TEX);
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float rot = t * 0.0045F; // еле заметное торжественное вращение астролябии-мандалы по полу
        float cosR = Mth.cos(rot), sinR = Mth.sin(rot);

        for (int i = 0; i < faceCount; ++i) {
            if (fDir[i] != 1) continue;
            float d = fDist[i];
            if (d > rad || d > 17.5F) continue;
            // Исключаем кроны высоких деревьев (> 5 блоков выше основания разлома), чтобы мандала лежала именно по полу и ступеням руин!
            if (fMaxY[i] > center.getY() + 4.5F) continue;

            float uWave = Mth.clamp((rad - d) / 4.2F, 0.0F, 1.0F);
            float pulse = 0.90F + 0.10F * Mth.sin(t * 0.14F - d * 0.35F);
            int a = Mth.clamp(Math.round(uWave * uWave * (3.0F - 2.0F * uWave) * 252.0F * pulse), 0, 255);
            if (a <= 3) continue;

            float wx0 = fMinX[i], wz0 = fMinZ[i];
            float wx1 = fMaxX[i], wz1 = fMaxZ[i];
            float yp = (fMaxY[i] - camY) + 0.007F;

            emitMandalaVertex(bb, mat, wx0, wz0, yp, camX, camZ, rcx, rcz, cosR, sinR, a);
            emitMandalaVertex(bb, mat, wx0, wz1, yp, camX, camZ, rcx, rcz, cosR, sinR, a);
            emitMandalaVertex(bb, mat, wx1, wz1, yp, camX, camZ, rcx, rcz, cosR, sinR, a);
            emitMandalaVertex(bb, mat, wx1, wz0, yp, camX, camZ, rcx, rcz, cosR, sinR, a);
        }
        Tesselator.getInstance().end();

        RenderSystem.disablePolygonOffset();
        RenderSystem.polygonOffset(0.0F, 0.0F);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static void emitMandalaVertex(BufferBuilder bb, Matrix4f mat,
                                          float wx, float wz, float yp,
                                          float camX, float camZ,
                                          float rcx, float rcz, float cosR, float sinR, int alpha) {
        float dx = wx - rcx;
        float dz = wz - rcz;
        float rx = dx * cosR - dz * sinR;
        float rz = dx * sinR + dz * cosR;
        float u = rx / 32.0F + 0.5F;
        float v = rz / 32.0F + 0.5F;
        bb.vertex(mat, wx - camX, yp, wz - camZ).uv(u, v).color(255, 252, 242, alpha).endVertex();
    }

    /**
     * Рисует высоко в куполе неба (на небесной дистанции {@link ShaderCompat#celestialDistance}, НЕ близко к игроку!)
     * аутентичные 2D-глаза измерения Элисар ({@code eye_closed/eye_half/eye_open/eye_iris.png}).
     * Включён тест глубины {@code GL_LEQUAL}, поэтому деревья, стены и руины честно перекрывают глаза!
     */
    private static void renderCelestial2DEyesInSky(Minecraft mc, PoseStack ps, Camera camera, float pt) {
        float t = currentTime(pt);
        float skyDist = ShaderCompat.celestialDistance(mc);
        float distScale = skyDist / 100.0F;

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

        Matrix4f mat = ps.last().pose();
        Vec3 lookVec = new Vec3(camera.getLookVector().x(), camera.getLookVector().y(), camera.getLookVector().z());

        for (int i = 0; i < SKY_EYE_COUNT; ++i) {
            float dt = t - skyEyeWake[i];
            if (dt <= 0.0F) continue;

            float fade = Mth.clamp(dt / 11.0F, 0.0F, 1.0F);
            // Чуть более выразительная альфа (0.72), чтобы 2D-глаза Элисара в небе отчётливо читались во время ритуала!
            float alpha = fade * 0.72F;
            if (alpha <= 0.01F) continue;

            ResourceLocation lid;
            if (dt < 4.0F) {
                lid = ElisarEye.CLOSED;
            } else if (dt < 8.5F) {
                lid = ElisarEye.HALF;
            } else {
                float blinkCycle = (t * 0.065F + skyEyeSeed[i] * 2.1F) % 6.283F;
                if (t < PortalRitual.AWAKEN_TICKS - 14.0F && blinkCycle > 5.88F) {
                    lid = (blinkCycle > 6.02F && blinkCycle < 6.16F) ? ElisarEye.CLOSED : ElisarEye.HALF;
                } else {
                    lid = ElisarEye.OPEN;
                }
            }

            Vector3f dir = skyEyeDir[i];
            Vector3f right = new Vector3f(dir).cross(0, 1, 0).normalize();
            Vector3f up = new Vector3f(right).cross(dir).normalize();

            float sc = skyEyeScale[i] * distScale;
            float w = 74.0F * sc;
            float h = 37.0F * sc;
            float dEye = skyDist - i * 0.45F;
            Vector3f c = new Vector3f(dir).mul(dEye);

            if (lid != ElisarEye.CLOSED) {
                // Зрачок плавно смещается в сторону взгляда игрока, как у главного небесного глаза Элисара!
                float gx = Mth.clamp((float) (-lookVec.dot(new Vec3(right.x, right.y, right.z)) * 0.65)
                        + Mth.sin(t * 0.03F + skyEyeSeed[i]) * 0.16F, -0.85F, 0.85F) * w * 0.05F;
                float gy = Mth.clamp((float) (-lookVec.dot(new Vec3(up.x, up.y, up.z)) * 0.45)
                        + Mth.cos(t * 0.024F + skyEyeSeed[i]) * 0.12F, -0.55F, 0.55F) * h * 0.07F;
                Vector3f ci = new Vector3f(c).add(new Vector3f(right).mul(gx)).add(new Vector3f(up).mul(gy));
                ElisarEye.drawWorldSkyQuad(mat, ElisarEye.IRIS, ci, right, up, w * 0.5F, h * 0.5F, alpha);
            }
            ElisarEye.drawWorldSkyQuad(mat, lid, c, right, up, w * 0.5F, h * 0.5F, alpha);
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /**
     * Однократное чтение геометрии вокруг портала (READ-ONLY: ни один блок в мире НЕ изменяется!).
     */
    private static void captureBleachFaces(ClientLevel level, BlockPos c) {
        capturedFor = c.immutable();
        faceCount = 0;
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos np = new BlockPos.MutableBlockPos();
        Direction[] dirs = Direction.values();

        double cx = c.getX() + 0.5, cy = c.getY() + 0.5, cz = c.getZ() + 0.5;
        int r2Max = BLEACH_SCAN_R * BLEACH_SCAN_R;

        for (int dy = -12; dy <= 22 && faceCount < MAX_FACES; ++dy) {
            for (int dx = -BLEACH_SCAN_R; dx <= BLEACH_SCAN_R && faceCount < MAX_FACES; ++dx) {
                for (int dz = -BLEACH_SCAN_R; dz <= BLEACH_SCAN_R && faceCount < MAX_FACES; ++dz) {
                    int d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 > r2Max) continue;

                    int bx = c.getX() + dx, by = c.getY() + dy, bz = c.getZ() + dz;
                    mp.set(bx, by, bz);
                    if (!level.isLoaded(mp)) continue;

                    BlockState st = level.getBlockState(mp);
                    if (st.isAir() || !st.getFluidState().isEmpty()) continue;

                    boolean isLeaves = st.getBlock() instanceof LeavesBlock;
                    VoxelShape colShape = st.getCollisionShape(level, mp);
                    if (!isLeaves && colShape.isEmpty()) continue;

                    AABB box = isLeaves ? new AABB(0, 0, 0, 1, 1, 1) : colShape.bounds();
                    float jitter = (float) (Math.sin(bx * 0.43 + bz * 0.37 + by * 0.29) * 1.15);
                    float dist = Math.max(0.0F, (float) Math.sqrt(
                            (bx + 0.5 - cx) * (bx + 0.5 - cx)
                                    + (by + 0.5 - cy) * (by + 0.5 - cy)
                                    + (bz + 0.5 - cz) * (bz + 0.5 - cz)) + jitter);

                    for (int di = 0; di < 6 && faceCount < MAX_FACES; ++di) {
                        Direction d = dirs[di];
                        np.setWithOffset(mp, d);
                        BlockState nb = level.getBlockState(np);
                        if (nb.isSolidRender(level, np)) continue;
                        if (isLeaves && nb.getBlock() instanceof LeavesBlock) continue;

                        int idx = faceCount++;
                        fMinX[idx] = (float) (bx + box.minX);
                        fMinY[idx] = (float) (by + box.minY);
                        fMinZ[idx] = (float) (bz + box.minZ);
                        fMaxX[idx] = (float) (bx + box.maxX);
                        fMaxY[idx] = (float) (by + box.maxY);
                        fMaxZ[idx] = (float) (bz + box.maxZ);
                        fDir[idx] = (byte) di;
                        fDist[idx] = dist;
                    }
                }
            }
        }
    }

    /** Маска в тон света Элисара вместо ванильного земляного экрана загрузки при смене измерения. */
    public static final class WhiteReceivingScreen extends ReceivingLevelScreen {
        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {
            g.fill(0, 0, this.width, this.height, 0xFFF7EFE0);
        }

        @Override
        public void renderBackground(GuiGraphics g) {
        }
    }
}
