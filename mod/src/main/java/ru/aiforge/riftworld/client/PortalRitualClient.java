package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
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
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.flight.PortalRitual;
import ru.aiforge.riftworld.net.PortalRitualMsg;

/**
 * v0.20.0: Клиентская визуализация кастомного бесшовного телепорта в Элисар:
 * <ul>
 *   <li>Плавное выбеливание блоков вокруг портала от его центра в тёплый бело-слоновый тон Элисара
 *       (<b>чисто визуальная 3D-оболочка поверх геометрии — в коде не меняется ни один блок!</b>);</li>
 *   <li>Появление в воздухе 34 парящих мистических глаз Разлома, неотрывно смотрящих на игрока;</li>
 *   <li>Резкая белая вспышка и подавление ванильного экрана загрузки земли ({@link ReceivingLevelScreen}).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID, value = Dist.CLIENT)
public final class PortalRitualClient {
    private static final int MAX_FACES = 36000;
    private static final int BLEACH_SCAN_R = 34;
    private static final int EYE_COUNT = 34;

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

    // Параметры 34 парящих в воздухе глаз вокруг портала и игрока
    private static final double[] eyeOffX = new double[EYE_COUNT];
    private static final double[] eyeOffY = new double[EYE_COUNT];
    private static final double[] eyeOffZ = new double[EYE_COUNT];
    private static final float[] eyeScale = new float[EYE_COUNT];
    private static final float[] eyeWake = new float[EYE_COUNT];
    private static final float[] eyeSeed = new float[EYE_COUNT];

    static {
        // Золотая спираль Фибоначчи вокруг арки портала на разных высотах и дистанциях (3.2 .. 13.5 блоков)
        for (int i = 0; i < EYE_COUNT; ++i) {
            double u = (i + 0.5) / EYE_COUNT;
            double goldenAngle = i * 2.39996323;
            double r = 3.2 + 10.2 * Math.sqrt(u) + Math.sin(i * 1.9) * 0.8;
            double y = 0.6 + (i % 7) * 1.08 + Math.cos(i * 2.7) * 0.45;
            eyeOffX[i] = Math.cos(goldenAngle) * r;
            eyeOffY[i] = y;
            eyeOffZ[i] = Math.sin(goldenAngle) * r;
            eyeScale[i] = 0.46F + (float) ((i * 37 % 11) / 10.0) * 0.52F;
            eyeWake[i] = 44.0F + (38.0F * i) / EYE_COUNT;
            eyeSeed[i] = i * 1.713F;
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
        return phase == PortalRitual.AWAKEN && center.distSqr(pos) <= 25.0;
    }

    public static float currentTime(float pt) {
        return tick + pt;
    }

    /** Текущий радиус волны белого цвета слоновой кости от центра портала (0 -> 37 блоков). */
    public static float bleachRadius(float pt) {
        if (phase != PortalRitual.AWAKEN) return 0.0F;
        float u = Mth.clamp(currentTime(pt) / 92.0F, 0.0F, 1.0F);
        float s = u * u * (3.0F - 2.0F * u);
        return 0.8F + 36.5F * s;
    }

    /** Яркость резкой белой вспышки (0..1) на границе переноса без экрана загрузки. */
    public static float flashIntensity(float pt) {
        float t = currentTime(pt);
        if (phase == PortalRitual.AWAKEN) {
            float f = Mth.clamp((t - (PortalRitual.AWAKEN_TICKS - 8.0F)) / 6.5F, 0.0F, 1.0F);
            return f * f; // резкая вспышка!
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

    /** Полное подавление ванильного земляного экрана загрузки при смене измерения! */
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
        // Тёпло-белая ослепительная вспышка в тон света Элисара (#FFFDF8)
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), (alpha << 24) | 0xFFFDF8);
    }

    @SubscribeEvent
    public static void fogColor(ViewportEvent.ComputeFogColor e) {
        if (phase != PortalRitual.AWAKEN) return;
        float u = Mth.clamp((currentTime((float) e.getPartialTick()) - 14.0F) / 68.0F, 0.0F, 1.0F);
        float k = u * u * (3.0F - 2.0F * u);
        e.setRed(Mth.lerp(k, e.getRed(), 0.962F));
        e.setGreen(Mth.lerp(k, e.getGreen(), 0.948F));
        e.setBlue(Mth.lerp(k, e.getBlue(), 0.912F));
    }

    @SubscribeEvent
    public static void renderFog(ViewportEvent.RenderFog e) {
        if (phase != PortalRitual.AWAKEN || e.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;
        if (e.getCamera().getFluidInCamera() != FogType.NONE) return;
        float u = Mth.clamp((currentTime((float) e.getPartialTick()) - 18.0F) / 70.0F, 0.0F, 1.0F);
        if (u <= 0.001F) return;
        float k = u * u * (3.0F - 2.0F * u);
        float far = e.getFarPlaneDistance();
        e.setNearPlaneDistance(Mth.lerp(k, far * 0.65F, 16.0F));
        e.setFarPlaneDistance(Mth.lerp(k, far, 44.0F));
        e.setCanceled(true);
    }

    @SubscribeEvent
    public static void renderStage(RenderLevelStageEvent e) {
        if (phase != PortalRitual.AWAKEN) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float pt = e.getPartialTick();
        Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = e.getPoseStack();

        if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            renderWorldIvoryBleach(ps, cam, pt);
        } else if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            renderPortalSingularityAndWatchingEyes(ps, e.getCamera(), cam, pt);
        }
    }

    /**
     * Визуально превращает блоки вокруг портала (траву, деревья, руины) в благородный белый известняк Элисара,
     * расходящийся волной от центра портала, БЕЗ единого изменения блоков в мире!
     */
    private static void renderWorldIvoryBleach(PoseStack ps, Vec3 cam, float pt) {
        if (faceCount <= 0) return;
        float rad = bleachRadius(pt);
        if (rad <= 0.2F) return;

        Matrix4f mat = ps.last().pose();
        float cx = (float) cam.x, cy = (float) cam.y, cz = (float) cam.z;

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.polygonOffset(-1.5F, -2.5F);
        RenderSystem.enablePolygonOffset();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < faceCount; ++i) {
            float d = fDist[i];
            if (d > rad) continue;
            float u = Mth.clamp((rad - d) / 5.6F, 0.0F, 1.0F);
            float w = u * u * (3.0F - 2.0F * u);
            int a = Mth.clamp(Math.round(w * 246.0F), 0, 255);
            if (a <= 2) continue;

            int r, g, b;
            switch (fDir[i]) {
                case 1 -> { r = 248; g = 244; b = 235; } // UP — солнечный бело-слоновый тон Элисара
                case 0 -> { r = 208; g = 201; b = 188; } // DOWN
                case 2, 3 -> { r = 236; g = 230; b = 219; } // NORTH / SOUTH
                default -> { r = 224; g = 218; b = 205; }   // WEST / EAST
            }

            float x0 = fMinX[i] - cx, y0 = fMinY[i] - cy, z0 = fMinZ[i] - cz;
            float x1 = fMaxX[i] - cx, y1 = fMaxY[i] - cy, z1 = fMaxZ[i] - cz;

            switch (fDir[i]) {
                case 0 -> { // DOWN (-Y)
                    bb.vertex(mat, x0, y0, z1).color(r, g, b, a).endVertex();
                    bb.vertex(mat, x0, y0, z0).color(r, g, b, a).endVertex();
                    bb.vertex(mat, x1, y0, z0).color(r, g, b, a).endVertex();
                    bb.vertex(mat, x1, y0, z1).color(r, g, b, a).endVertex();
                }
                case 1 -> { // UP (+Y)
                    bb.vertex(mat, x0, y1, z0).color(r, g, b, a).endVertex();
                    bb.vertex(mat, x0, y1, z1).color(r, g, b, a).endVertex();
                    bb.vertex(mat, x1, y1, z1).color(r, g, b, a).endVertex();
                    bb.vertex(mat, x1, y1, z0).color(r, g, b, a).endVertex();
                }
                case 2 -> { // NORTH (-Z)
                    bb.vertex(mat, x1, y1, z0).color(r, g, b, a).endVertex();
                    bb.vertex(mat, x1, y0, z0).color(r, g, b, a).endVertex();
                    bb.vertex(mat, x0, y0, z0).color(r, g, b, a).endVertex();
                    bb.vertex(mat, x0, y1, z0).color(r, g, b, a).endVertex();
                }
                case 3 -> { // SOUTH (+Z)
                    bb.vertex(mat, x0, y1, z1).color(r, g, b, a).endVertex();
                    bb.vertex(mat, x0, y0, z1).color(r, g, b, a).endVertex();
                    bb.vertex(mat, x1, y0, z1).color(r, g, b, a).endVertex();
                    bb.vertex(mat, x1, y1, z1).color(r, g, b, a).endVertex();
                }
                case 4 -> { // WEST (-X)
                    bb.vertex(mat, x0, y1, z0).color(r, g, b, a).endVertex();
                    bb.vertex(mat, x0, y0, z0).color(r, g, b, a).endVertex();
                    bb.vertex(mat, x0, y0, z1).color(r, g, b, a).endVertex();
                    bb.vertex(mat, x0, y1, z1).color(r, g, b, a).endVertex();
                }
                case 5 -> { // EAST (+X)
                    bb.vertex(mat, x1, y1, z1).color(r, g, b, a).endVertex();
                    bb.vertex(mat, x1, y0, z1).color(r, g, b, a).endVertex();
                    bb.vertex(mat, x1, y0, z0).color(r, g, b, a).endVertex();
                    bb.vertex(mat, x1, y1, z0).color(r, g, b, a).endVertex();
                }
            }
        }

        Tesselator.getInstance().end();

        RenderSystem.disablePolygonOffset();
        RenderSystem.polygonOffset(0.0F, 0.0F);
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /**
     * Рисует световое ядро в центре арки портала и множество (34) парящих в воздухе глаз Разлома,
     * каждый из которых плавно открывается и неотрывно следит зрачком за игроком!
     */
    private static void renderPortalSingularityAndWatchingEyes(PoseStack ps, Camera camera, Vec3 cam, float pt) {
        float t = currentTime(pt);
        Matrix4f mat = ps.last().pose();

        Vector3f leftV = camera.getLeftVector();
        Vector3f upV = camera.getUpVector();
        Vec3 right = new Vec3(-leftV.x(), -leftV.y(), -leftV.z()).normalize();
        Vec3 up = new Vec3(upV.x(), upV.y(), upV.z()).normalize();

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // 1. Сияющее бело-золотое сердце Разлома в центре портала
        Vec3 pCenter = Vec3.atCenterOf(center).subtract(cam);
        float coreR = 0.85F + 0.55F * Mth.clamp(t / 60.0F, 0.0F, 1.0F) + 0.12F * Mth.sin(t * 0.25F);
        drawBillboardDisc(bb, mat, pCenter, right, up, coreR * 2.1F, 248, 240, 214, 115, 248, 240, 214, 0, 20);
        drawBillboardDisc(bb, mat, pCenter, right, up, coreR * 0.95F, 255, 253, 246, 235, 248, 236, 198, 0, 16);

        // 2. Множество парящих в воздухе глаз, смотрящих прямо на игрока
        double baseCx = center.getX() + 0.5;
        double baseCy = center.getY() + 0.5;
        double baseCz = center.getZ() + 0.5;

        for (int i = 0; i < EYE_COUNT; ++i) {
            float dt = t - eyeWake[i];
            if (dt <= 0.0F) continue;

            float fadeIn = Mth.clamp(dt / 7.5F, 0.0F, 1.0F);
            // Редкое живое моргание отдельных глаз; перед самой вспышкой все глаза распахиваются шире!
            float blinkCycle = (t * 0.075F + eyeSeed[i] * 1.9F) % 6.283F;
            float blink = (t < PortalRitual.AWAKEN_TICKS - 14 && blinkCycle > 5.85F)
                    ? Mth.sin(((blinkCycle - 5.85F) / 0.433F) * Mth.PI)
                    : 0.0F;
            float wideSurge = Mth.clamp((t - (PortalRitual.AWAKEN_TICKS - 14.0F)) / 10.0F, 0.0F, 1.0F) * 0.22F;
            float open = Mth.clamp(Mth.clamp(dt / 10.0F, 0.08F, 1.0F) * (1.0F - 0.88F * blink) + wideSurge, 0.06F, 1.22F);

            double bob = Math.sin(t * 0.09 + eyeSeed[i]) * 0.18;
            Vec3 worldEye = new Vec3(baseCx + eyeOffX[i], baseCy + eyeOffY[i] + bob, baseCz + eyeOffZ[i]);
            // Не рисуем глаз слишком близко к лицу игрока (< 1.6 блока), чтобы не перекрывать обзор целиком
            if (worldEye.distanceToSqr(cam) < 2.6) continue;

            Vec3 rel = worldEye.subtract(cam);
            Vec3 toCam = rel.scale(-1.0).normalize();
            float s = eyeScale[i];

            drawWatchingEye(bb, mat, rel, right, up, toCam, s, open, fadeIn, t, eyeSeed[i]);
        }

        Tesselator.getInstance().end();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /**
     * Высококонтрастный мистический парящий глаз Разлома в 3D-пространстве:
     * тёмно-обсидиановые веки с золотистым контуром, фарфорово-белая склера и живой бирюзово-золотой
     * зрачок со световым бликом, неотрывно направленный на игрока!
     */
    private static void drawWatchingEye(BufferBuilder bb, Matrix4f mat,
                                        Vec3 centerRel, Vec3 right, Vec3 up, Vec3 forwardToCam,
                                        float scale, float openness, float alphaMul, float t, float seed) {
        int segs = 12;
        float halfW = 0.82F * scale;
        float halfH = 0.36F * scale * openness;
        float lidThick = (0.085F + 0.03F * (1.0F - Mth.clamp(openness, 0.0F, 1.0F))) * scale;

        // 0) Тёмно-золотистый эфирный ореол позади глаза, чтобы глаз чётко читался на фоне белеющего мира!
        Vec3 back = centerRel.subtract(forwardToCam.scale(0.02));
        drawBillboardEllipse(bb, mat, back, right, up, halfW * 1.42F, Math.max(0.24F * scale, halfH * 1.75F),
                36, 30, 48, Math.round(115 * alphaMul), 212, 182, 108, 0, 14);

        // 1) Миндалевидная фарфоровая склера глаза
        int scA = Math.round(248 * alphaMul);
        int lidA = Math.round(252 * alphaMul);
        int goldA = Math.round(215 * alphaMul);
        for (int i = 0; i < segs; ++i) {
            float u0 = -1.0F + (2.0F * i) / segs;
            float u1 = -1.0F + (2.0F * (i + 1)) / segs;
            float arch0 = (1.0F - u0 * u0);
            float arch1 = (1.0F - u1 * u1);

            float x0 = u0 * halfW, x1 = u1 * halfW;
            float yTop0 = arch0 * halfH, yTop1 = arch1 * halfH;
            float yBot0 = -arch0 * halfH, yBot1 = -arch1 * halfH;

            // Склера (чуть притенённая к уголкам для объёма глазного яблока)
            int shade0 = 232 + Math.round(23 * arch0);
            int shade1 = 232 + Math.round(23 * arch1);
            Vec3 st0 = pt2d(centerRel, right, up, x0, yTop0);
            Vec3 sb0 = pt2d(centerRel, right, up, x0, yBot0);
            Vec3 sb1 = pt2d(centerRel, right, up, x1, yBot1);
            Vec3 st1 = pt2d(centerRel, right, up, x1, yTop1);
            quad(bb, mat, st0, sb0, sb1, st1, shade0, shade0, shade1, scA);

            // Верхнее обсидиановое веко + золотая ресничная дуга
            Vec3 lidFront = centerRel.add(forwardToCam.scale(0.016));
            Vec3 ut0 = pt2d(lidFront, right, up, x0, yTop0 + lidThick);
            Vec3 ub0 = pt2d(lidFront, right, up, x0, yTop0 - lidThick * 0.18F);
            Vec3 ub1 = pt2d(lidFront, right, up, x1, yTop1 - lidThick * 0.18F);
            Vec3 ut1 = pt2d(lidFront, right, up, x1, yTop1 + lidThick);
            quad(bb, mat, ut0, ub0, ub1, ut1, 24, 20, 34, lidA);

            Vec3 gt0 = pt2d(lidFront, right, up, x0, yTop0 + lidThick * 1.32F);
            Vec3 gt1 = pt2d(lidFront, right, up, x1, yTop1 + lidThick * 1.32F);
            quad(bb, mat, gt0, ut0, ut1, gt1, 216, 180, 96, goldA);

            // Нижнее обсидиановое веко
            Vec3 lt0 = pt2d(lidFront, right, up, x0, yBot0 + lidThick * 0.16F);
            Vec3 lb0 = pt2d(lidFront, right, up, x0, yBot0 - lidThick * 0.82F);
            Vec3 lb1 = pt2d(lidFront, right, up, x1, yBot1 - lidThick * 0.82F);
            Vec3 lt1 = pt2d(lidFront, right, up, x1, yBot1 + lidThick * 0.16F);
            quad(bb, mat, lt0, lb0, lb1, lt1, 28, 24, 38, lidA);
        }

        // 2) Радужка и зрачок, неотрывно смотрящие прямо на игрока (с лёгким живым микротремором взгляда)
        if (openness > 0.18F) {
            float gazeX = Mth.sin(t * 0.19F + seed * 3.1F) * 0.035F * scale;
            float gazeY = Mth.cos(t * 0.15F + seed * 2.3F) * 0.020F * scale;
            Vec3 irisCenter = centerRel.add(forwardToCam.scale(0.008))
                    .add(right.scale(gazeX)).add(up.scale(gazeY));
            float irisR = Math.min(0.24F * scale, halfH * 0.88F);
            // Бирюзово-золотая радужка Разлома
            drawBillboardDisc(bb, mat, irisCenter, right, up, irisR,
                    48, 204, 228, Math.round(248 * alphaMul),
                    24, 96, 132, Math.round(245 * alphaMul), 14);
            drawBillboardDisc(bb, mat, irisCenter.add(forwardToCam.scale(0.003)), right, up, irisR * 0.72F,
                    232, 188, 86, Math.round(235 * alphaMul),
                    52, 198, 224, Math.round(180 * alphaMul), 12);
            // Глубокий угольно-чёрный зрачок
            Vec3 pupilCenter = irisCenter.add(forwardToCam.scale(0.006));
            drawBillboardDisc(bb, mat, pupilCenter, right, up, irisR * 0.44F,
                    10, 9, 16, Math.round(255 * alphaMul),
                    14, 12, 22, Math.round(250 * alphaMul), 12);
            // Живой белый блик на роговице
            Vec3 spark = pupilCenter.add(forwardToCam.scale(0.004))
                    .add(right.scale(-irisR * 0.26F)).add(up.scale(irisR * 0.26F));
            drawBillboardDisc(bb, mat, spark, right, up, irisR * 0.16F,
                    255, 255, 255, Math.round(250 * alphaMul),
                    255, 255, 255, 0, 8);
        }
    }

    private static Vec3 pt2d(Vec3 origin, Vec3 right, Vec3 up, float x, float y) {
        return origin.add(right.scale(x)).add(up.scale(y));
    }

    private static void quad(BufferBuilder bb, Matrix4f mat, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                             int r, int g, int bl, int alpha) {
        bb.vertex(mat, (float) a.x, (float) a.y, (float) a.z).color(r, g, bl, alpha).endVertex();
        bb.vertex(mat, (float) b.x, (float) b.y, (float) b.z).color(r, g, bl, alpha).endVertex();
        bb.vertex(mat, (float) c.x, (float) c.y, (float) c.z).color(r, g, bl, alpha).endVertex();
        bb.vertex(mat, (float) d.x, (float) d.y, (float) d.z).color(r, g, bl, alpha).endVertex();
    }

    private static void drawBillboardDisc(BufferBuilder bb, Matrix4f mat, Vec3 c, Vec3 right, Vec3 up, float radius,
                                          int rIn, int gIn, int bIn, int aIn,
                                          int rOut, int gOut, int bOut, int aOut, int segs) {
        drawBillboardEllipse(bb, mat, c, right, up, radius, radius, rIn, gIn, bIn, aIn, rOut, gOut, bOut, aOut, segs);
    }

    private static void drawBillboardEllipse(BufferBuilder bb, Matrix4f mat, Vec3 c, Vec3 right, Vec3 up,
                                             float rx, float ry,
                                             int rIn, int gIn, int bIn, int aIn,
                                             int rOut, int gOut, int bOut, int aOut, int segs) {
        float cx = (float) c.x, cy = (float) c.y, cz = (float) c.z;
        for (int i = 0; i < segs; ++i) {
            double a0 = (Math.PI * 2.0 * i) / segs;
            double a1 = (Math.PI * 2.0 * (i + 1)) / segs;
            Vec3 p0 = c.add(right.scale(Math.cos(a0) * rx)).add(up.scale(Math.sin(a0) * ry));
            Vec3 p1 = c.add(right.scale(Math.cos(a1) * rx)).add(up.scale(Math.sin(a1) * ry));
            bb.vertex(mat, cx, cy, cz).color(rIn, gIn, bIn, aIn).endVertex();
            bb.vertex(mat, (float) p0.x, (float) p0.y, (float) p0.z).color(rOut, gOut, bOut, aOut).endVertex();
            bb.vertex(mat, (float) p1.x, (float) p1.y, (float) p1.z).color(rOut, gOut, bOut, aOut).endVertex();
            bb.vertex(mat, cx, cy, cz).color(rIn, gIn, bIn, aIn).endVertex();
        }
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
                    if (!isLeaves && colShape.isEmpty()) continue; // пропускаем мелкую траву/цветы

                    AABB box = isLeaves ? new AABB(0, 0, 0, 1, 1, 1) : colShape.bounds();
                    // Лёгкий органический шум фронта волны, чтобы белизна ползла живым узором, а не циркулем
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

    /** Чисто-белая маска вместо ванильного земляного экрана загрузки при смене измерения в пике вспышки. */
    public static final class WhiteReceivingScreen extends ReceivingLevelScreen {
        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {
            g.fill(0, 0, this.width, this.height, 0xFFFFFDF8);
        }

        @Override
        public void renderBackground(GuiGraphics g) {
        }
    }
}
