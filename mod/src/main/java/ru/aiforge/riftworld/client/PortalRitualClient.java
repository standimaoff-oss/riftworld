package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
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
import ru.aiforge.riftworld.entity.SpatialRiftEntity;
import ru.aiforge.riftworld.flight.PortalRitual;
import ru.aiforge.riftworld.net.PortalRitualMsg;

/**
 * Клиентская часть ритуала открытия белого Пролома и кинематографической катсцены Белого Города:
 * <ol>
 *   <li>{@link PortalRitual#AWAKEN} (4 с = 80 тиков): окружение плавно бледнеет в тёплый оттенок Элисара
 *       ({@code mutated = 0}), на земле разворачивается резной геометрический узор печати, в небе
 *       открываются 2D-глаза Элисара, затем яркая вспышка открывает белый 3D-Пролом.</li>
 *   <li>{@link PortalRitual#TRANSIT_VEIL} + {@link PortalRitual#CITY_CUTSCENE}:
 *       при входе в пролом экран мягко белеет (не ослепляюще, матово-фарфоровый тон {@code #EAE3D5}),
 *       полностью скрывается инвентарь и весь GUI, блокируется переключение лиц ({@code F5}),
 *       без загрузочного экрана показывается 1 панорамный дальний ракурс Белого Города с названием
 *       измерения и снизу названием города -> текст исчезает -> камера плавно летит к глазам игрока ->
 *       возвращаются интерфейс и управление камерой!</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class PortalRitualClient {
    private static final ResourceLocation FLOOR_TILE_TEX =
            new ResourceLocation(RiftWorld.MODID, "textures/environment/portal_floor_tile.png");
    private static final ResourceLocation FLOOR_MANDALA_TEX =
            new ResourceLocation(RiftWorld.MODID, "textures/environment/portal_floor_mandala.png");
    private static final ResourceLocation OPEN_SEAL_TEX =
            new ResourceLocation(RiftWorld.MODID, "textures/environment/portal_open_seal.png");

    /** Мягкий не-ослепляющий тёпло-фарфоровый цвет экрана перехода и яркий тон вспышки входа в пролом. */
    private static final int SOFT_VEIL_RGB = 0xEAE3D5;
    private static final int ENTRY_FLASH_RGB = 0xFCFAF2;
    private static final int CUTSCENE_CAM_ENTITY_ID = -77701;

    public static int phase = PortalRitual.NONE;
    private static BlockPos center = BlockPos.ZERO;
    private static int tick = 0;

    // Постоянная белая аура вокруг ОТКРЫТОГО пространственного разлома (~50 блоков, к краю всё скуднее!)
    private static BlockPos openRiftCenter = null;
    private static int openRiftAgeTicks = 0;
    private static int openRiftScanCooldown = 0;

    // Кэш граней блоков вокруг открывающегося/открытого портала на 50+ блоков (READ-ONLY: 0 изменённых блоков мира!)
    private static final float MAX_BLEACH_RADIUS = 50.0F;
    private static final int BLEACH_SCAN_R = 52;
    private static final int MAX_FACES = 150000;
    private static BlockPos capturedFor = null;
    private static int faceCount = 0;
    private static final float[] fMinX = new float[MAX_FACES];
    private static final float[] fMinY = new float[MAX_FACES];
    private static final float[] fMinZ = new float[MAX_FACES];
    private static final float[] fMaxX = new float[MAX_FACES];
    private static final float[] fMaxY = new float[MAX_FACES];
    private static final float[] fMaxZ = new float[MAX_FACES];
    private static final byte[] fDir = new byte[MAX_FACES];
    private static final float[] fDist = new float[MAX_FACES];

    // Купол 2D-глаз Элисара высоко в небе при открытии портала
    private static final int SKY_EYE_COUNT = 22;
    private static final Vector3f[] skyEyeDir = new Vector3f[SKY_EYE_COUNT];
    private static final float[] skyEyeWake = new float[SKY_EYE_COUNT];
    private static final float[] skyEyeScale = new float[SKY_EYE_COUNT];
    private static final float[] skyEyeSeed = new float[SKY_EYE_COUNT];

    // Клиентская сущность-камера для дальнего ракурса Белого Города и плавного подлёта к игроку
    private static Marker cutsceneCam = null;
    private static float camYaw = 180.0F;
    private static float camPitch = 10.0F;
    private static boolean hideGuiSaved = false;
    private static boolean prevHideGuiValue = false;
    private static int renderFrameSeq = 0;
    private static int drawnFrameSeq = -1;

    static {
        for (int i = 0; i < SKY_EYE_COUNT; ++i) {
            float golden = i * 2.3999632F;
            float ring = (i % 5) / 4.0F;
            float elDeg = 22.0F + ring * 44.0F + (i == 0 ? 12.0F : 0.0F);
            float el = elDeg * Mth.DEG_TO_RAD;
            float cosEl = Mth.cos(el);
            skyEyeDir[i] = new Vector3f(
                    cosEl * Mth.sin(golden),
                    Mth.sin(el),
                    cosEl * Mth.cos(golden)).normalize();
            skyEyeWake[i] = 10.0F + i * 2.1F;
            skyEyeScale[i] = (i == 0) ? 0.56F : (0.28F + (i % 4) * 0.075F);
            skyEyeSeed[i] = i * 1.713F;
        }
    }

    private PortalRitualClient() {
    }

    public static boolean isHudAndPerspectiveLocked() {
        return phase == PortalRitual.TRANSIT_VEIL
                || phase == PortalRitual.CITY_CUTSCENE
                || phase == PortalRitual.ARRIVE_FLASH;
    }

    public static void onMsg(PortalRitualMsg m) {
        Minecraft mc = Minecraft.getInstance();
        int prevPhase = phase;
        phase = m.phase();
        center = m.center();
        tick = m.elapsed();

        if (phase == PortalRitual.AWAKEN && (prevPhase != PortalRitual.AWAKEN || !center.equals(capturedFor))) {
            capturedFor = null;
            faceCount = 0;
        } else if (phase == PortalRitual.TRANSIT_VEIL || phase == PortalRitual.CITY_CUTSCENE) {
            enforceLockedFirstPersonAndNoScreen(mc);
            if (phase == PortalRitual.CITY_CUTSCENE) {
                updateCutsceneCamera(mc, 0.0F, true);
            }
        } else if (phase == PortalRitual.NONE) {
            releaseCutsceneCamera(mc);
            capturedFor = null;
            faceCount = 0;
        }
    }

    public static float currentTime(float partialTick) {
        return tick + partialTick;
    }

    public static BlockPos activeBleachCenter() {
        if (phase == PortalRitual.AWAKEN) return center;
        return openRiftCenter;
    }

    public static float activeBleachTime(float partialTick) {
        if (phase == PortalRitual.AWAKEN) return tick + partialTick;
        if (openRiftCenter != null) return openRiftAgeTicks + partialTick;
        return 0.0F;
    }

    public static float bleachRadius(float partialTick) {
        // Когда разлом уже открыт — белая аура НЕ пропадает и держится на полных 50 блоках вокруг пролома!
        if (openRiftCenter != null) {
            return MAX_BLEACH_RADIUS;
        }
        if (phase != PortalRitual.AWAKEN) return 0.0F;
        float u = Mth.clamp(currentTime(partialTick) / (PortalRitual.AWAKEN_TICKS - 6.0F), 0.0F, 1.0F);
        return (0.22F * u + 0.78F * u * u) * MAX_BLEACH_RADIUS;
    }

    /**
     * Яркость экранной вспышки и вуали:
     * <ul>
     *   <li>{@code AWAKEN}: 0.0F — вспышка происходит ИМЕННО в момент захода игрока внутрь просвета!</li>
     *   <li>{@code TRANSIT_VEIL}: мгновенная яркая белая вспышка входа в пролом с переходом в мягкую фарфоровую вуаль;</li>
     *   <li>{@code CITY_CUTSCENE}: плавное растворение вуали за первые 18 тиков дальнего ракурса Белого Города;</li>
     *   <li>{@code ARRIVE_FLASH}: мягкое растворение вуали при обратном выходе.</li>
     * </ul>
     */
    public static float veilOpacity(float partialTick) {
        float t = currentTime(partialTick);
        if (phase == PortalRitual.TRANSIT_VEIL) {
            if (t <= 2.5F) {
                float u = Mth.clamp((t + 0.65F) / 2.5F, 0.35F, 1.0F);
                return Mth.sin(u * (Mth.PI * 0.5F));
            }
            return Mth.lerp(Mth.clamp((t - 2.5F) / 6.5F, 0.0F, 1.0F), 1.0F, 0.95F);
        }
        if (phase == PortalRitual.CITY_CUTSCENE) {
            if (t <= 3.0F) return 0.95F;
            float fade = 1.0F - Mth.clamp((t - 3.0F) / 16.0F, 0.0F, 1.0F);
            return fade * fade * 0.95F;
        }
        if (phase == PortalRitual.ARRIVE_FLASH) {
            if (t <= 3.0F) return 0.94F;
            float fade = 1.0F - Mth.clamp((t - 3.0F) / (PortalRitual.ARRIVE_TICKS - 3.0F), 0.0F, 1.0F);
            return fade * fade * 0.94F;
        }
        return 0.0F;
    }

    private static void enforceLockedFirstPersonAndNoScreen(Minecraft mc) {
        if (!hideGuiSaved) {
            prevHideGuiValue = mc.options.hideGui;
            hideGuiSaved = true;
        }
        // Гарантируем, что ForgeGui.render вызывается в каждом кадре (чтобы рисовались мягкая вуаль и титры города,
        // при этом все ванильные оверлеи хотбара/инвентаря/сердец отменяются в onOverlay!)
        mc.options.hideGui = false;

        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        while (mc.options.keyTogglePerspective.consumeClick()) {
            // съедаем любые нажатия F5 во время перехода и катсцены
        }
        while (mc.options.keyInventory.consumeClick()) {
            // блокируем открытие инвентаря во время перехода и катсцены
        }
        if (mc.screen != null && !(mc.screen instanceof WhiteReceivingScreen)) {
            mc.setScreen(null);
        }
    }

    private static void releaseCutsceneCamera(Minecraft mc) {
        if (cutsceneCam != null) {
            if (mc.level != null) {
                mc.level.removeEntity(CUTSCENE_CAM_ENTITY_ID, Entity.RemovalReason.DISCARDED);
            }
            cutsceneCam = null;
        }
        if (mc.player != null && mc.getCameraEntity() != mc.player) {
            mc.setCameraEntity(mc.player);
        }
        if (hideGuiSaved) {
            mc.options.hideGui = prevHideGuiValue;
            hideGuiSaved = false;
        }
    }

    /**
     * Обновляет позицию и углы панорамной камеры Белого Города:
     * <ul>
     *   <li>Тики {@code 0..76}: 1 величественный дальний ракурс Белого Города с южного подступа
     *       {@code (24, arrivalY + 33, 88)} с лёгким кинематографическим дрейфом вперёд (вид на весь остров,
     *       дома и шпили Замка Белого Города);</li>
     *   <li>Тики {@code 76..121}: текст уже исчез, и камера плавно по кривой Эрмита подлетает прямо
     *       к глазам игрока {@code (player.x, player.eyeY, player.z)}, выравнивая взгляд с игроком;</li>
     *   <li>Тики {@code 122..124}: бесшовная передача камеры самому игроку.</li>
     * </ul>
     */
    private static void updateCutsceneCamera(Minecraft mc, float partialTick, boolean snapPrev) {
        if (mc.level == null || mc.player == null) return;
        float t = tick + partialTick;

        if (t >= PortalRitual.CUTSCENE_TICKS - 2.0F) {
            releaseCutsceneCamera(mc);
            return;
        }

        if (cutsceneCam == null || cutsceneCam.level() != mc.level) {
            cutsceneCam = new Marker(EntityType.MARKER, mc.level);
            mc.level.putNonPlayerEntity(CUTSCENE_CAM_ENTITY_ID, cutsceneCam);
        }

        double px = Mth.lerp(partialTick, mc.player.xo, mc.player.getX());
        double py = Mth.lerp(partialTick, mc.player.yo, mc.player.getY()) + mc.player.getEyeHeight();
        double pz = Mth.lerp(partialTick, mc.player.zo, mc.player.getZ());

        double baseY = center.getY();
        // Единый дальний панорамный ракурс Белого Города (смотрит на северо-запад на весь город и Замок!)
        Vec3 shotStart = new Vec3(26.0, baseY + 34.0, 92.0);
        Vec3 shotHoldEnd = new Vec3(21.5, baseY + 30.5, 82.0);
        // Точка взгляда на Белый Город и его центральный Замок (севернее точки прибытия)
        Vec3 cityTarget = new Vec3(-2.0, baseY + 18.0, -18.0);

        Vec3 camPos;
        float targetYaw;
        float targetPitch;

        if (t <= 126.0F) {
            float u = Mth.clamp(t / 126.0F, 0.0F, 1.0F);
            camPos = shotStart.lerp(shotHoldEnd, u);
            Vec3 look = cityTarget.subtract(camPos);
            double horiz = Math.hypot(look.x, look.z);
            targetYaw = (float) (Mth.atan2(-look.x, look.z) * Mth.RAD_TO_DEG);
            targetPitch = (float) (-Mth.atan2(look.y, horiz) * Mth.RAD_TO_DEG);
        } else {
            // Плавный полёт камеры от дальнего ракурса прямо к глазам игрока!
            float u = Mth.clamp((t - 126.0F) / 54.0F, 0.0F, 1.0F);
            float s = u * u * u * (u * (u * 6.0F - 15.0F) + 10.0F); // quintic smoothstep (0 рывков в начале и в конце!)
            Vec3 playerEye = new Vec3(px, py, pz);
            // Лёгкая дуга сверху вниз при подлёте к игроку
            double arcLift = Math.sin(u * Math.PI) * 3.5;
            camPos = shotHoldEnd.lerp(playerEye, s).add(0.0, arcLift, 0.0);

            Vec3 lookStart = cityTarget.subtract(shotHoldEnd);
            float startYaw = (float) (Mth.atan2(-lookStart.x, lookStart.z) * Mth.RAD_TO_DEG);
            float startPitch = (float) (-Mth.atan2(lookStart.y, Math.hypot(lookStart.x, lookStart.z)) * Mth.RAD_TO_DEG);

            targetYaw = rotLerp(s, startYaw, mc.player.getViewYRot(partialTick));
            targetPitch = Mth.lerp(s, startPitch, mc.player.getViewXRot(partialTick));
        }

        camYaw = targetYaw;
        camPitch = targetPitch;

        if (snapPrev) {
            cutsceneCam.xo = camPos.x;
            cutsceneCam.yo = camPos.y;
            cutsceneCam.zo = camPos.z;
        } else {
            cutsceneCam.xo = cutsceneCam.getX();
            cutsceneCam.yo = cutsceneCam.getY();
            cutsceneCam.zo = cutsceneCam.getZ();
        }
        cutsceneCam.setPos(camPos.x, camPos.y, camPos.z);
        cutsceneCam.setYRot(camYaw);
        cutsceneCam.setXRot(camPitch);
        cutsceneCam.yRotO = camYaw;
        cutsceneCam.xRotO = camPitch;

        // На последних 3 тиках (когда камера уже в 0.6 м от глаз игрока) передаём управление без пересечения головы модели
        if (t < PortalRitual.CUTSCENE_TICKS - 3.5F) {
            if (mc.getCameraEntity() != cutsceneCam) {
                mc.setCameraEntity(cutsceneCam);
            }
        } else if (mc.getCameraEntity() != mc.player) {
            mc.setCameraEntity(mc.player);
        }
    }

    private static float rotLerp(float k, float a, float b) {
        return a + k * Mth.wrapDegrees(b - a);
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;

        // Постоянно отслеживаем ближайший открытый SpatialRiftEntity, чтобы белая аура на 50 блоков держалась пока открыт разлом!
        if (mc.level != null && mc.player != null) {
            if (++openRiftScanCooldown >= 4 || openRiftCenter == null) {
                openRiftScanCooldown = 0;
                var rifts = mc.level.getEntitiesOfClass(SpatialRiftEntity.class,
                        mc.player.getBoundingBox().inflate(88.0), Entity::isAlive);
                SpatialRiftEntity nearest = null;
                double bestD2 = Double.MAX_VALUE;
                for (SpatialRiftEntity sr : rifts) {
                    double d2 = sr.distanceToSqr(mc.player);
                    if (d2 < bestD2) {
                        bestD2 = d2;
                        nearest = sr;
                    }
                }
                if (nearest != null) {
                    BlockPos rp = nearest.blockPosition();
                    openRiftAgeTicks = nearest.riftAge();
                    if (openRiftCenter == null || openRiftCenter.distSqr(rp) > 4.0) {
                        openRiftCenter = rp.immutable();
                    }
                } else {
                    openRiftCenter = null;
                    openRiftAgeTicks = 0;
                }
            } else {
                ++openRiftAgeTicks;
            }

            BlockPos bleachTarget = activeBleachCenter();
            boolean inElisar = mc.level.dimension().equals(ru.aiforge.riftworld.flight.Flight.ELISAR_DIM);
            if (!inElisar && bleachTarget != null && (capturedFor == null || capturedFor.distSqr(bleachTarget) > 4.0)) {
                captureBleachFaces(mc.level, bleachTarget);
            } else if ((inElisar || (bleachTarget == null && phase == PortalRitual.NONE)) && capturedFor != null) {
                capturedFor = null;
                faceCount = 0;
            }
        } else {
            openRiftCenter = null;
            capturedFor = null;
            faceCount = 0;
        }

        if (phase == PortalRitual.NONE) return;

        if (isHudAndPerspectiveLocked()) {
            enforceLockedFirstPersonAndNoScreen(mc);
        }

        int maxLen = switch (phase) {
            case PortalRitual.AWAKEN -> PortalRitual.AWAKEN_SETTLE_TICKS + 4;
            case PortalRitual.TRANSIT_VEIL -> PortalRitual.TRANSIT_TICKS + 24;
            case PortalRitual.CITY_CUTSCENE -> PortalRitual.CUTSCENE_TICKS + 2;
            default -> PortalRitual.ARRIVE_TICKS + 4;
        };

        if (tick < maxLen) {
            ++tick;
        } else if (phase != PortalRitual.TRANSIT_VEIL) {
            phase = PortalRitual.NONE;
            releaseCutsceneCamera(mc);
            if (mc.screen instanceof WhiteReceivingScreen) {
                mc.setScreen(null);
            }
        }

        if (phase == PortalRitual.CITY_CUTSCENE) {
            updateCutsceneCamera(mc, 0.0F, tick <= 1);
        }
    }

    /** Блокируем F5 и клавишу инвентаря на уровне клавиатурного ввода во время входа в портал и катсцены. */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key e) {
        if (!isHudAndPerspectiveLocked()) return;
        Minecraft mc = Minecraft.getInstance();
        enforceLockedFirstPersonAndNoScreen(mc);
    }

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening e) {
        if (phase == PortalRitual.NONE) return;
        var s = e.getNewScreen();
        if ((s instanceof ReceivingLevelScreen || s instanceof ProgressScreen || s instanceof GenericDirtMessageScreen)
                && !(s instanceof WhiteReceivingScreen)) {
            e.setNewScreen(new WhiteReceivingScreen());
            return;
        }
        // Во время входа в пролом и катсцены Белого Города не открываем инвентарь и контейнеры!
        if (isHudAndPerspectiveLocked() && s instanceof AbstractContainerScreen<?>) {
            e.setCanceled(true);
        }
    }

    /** Полностью скрываем руку/предмет от 1-го лица во время входа в пролом и панорамной катсцены города! */
    @SubscribeEvent
    public static void onRenderHand(net.minecraftforge.client.event.RenderHandEvent e) {
        if (isHudAndPerspectiveLocked()) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderTickStart(TickEvent.RenderTickEvent e) {
        if (e.phase == TickEvent.Phase.START) {
            ++renderFrameSeq;
        }
    }

    /**
     * Полностью скрываем весь игровой HUD (хотбар, инвентарь, сердца, прицел) во время входа в портал и катсцены,
     * а ровно 1 раз за кадр внутри буфера ForgeGui (ДО blitToScreen!) рисуем мягкую фарфоровую вуаль и титры города!
     */
    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Pre e) {
        if (phase == PortalRitual.NONE) return;
        if (isHudAndPerspectiveLocked()) {
            if (drawnFrameSeq != renderFrameSeq) {
                drawnFrameSeq = renderFrameSeq;
                renderCutsceneVeilAndTitles(e.getGuiGraphics(), e.getPartialTick());
            }
            e.setCanceled(true);
            return;
        }
        float a = veilOpacity(e.getPartialTick());
        if (a <= 0.004F) return;
        GuiGraphics g = e.getGuiGraphics();
        int alpha = Mth.clamp(Math.round(a * 255.0F), 0, 255);
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), (alpha << 24) | 0xFAF4E8);
    }

    @SubscribeEvent
    public static void cameraAngles(ViewportEvent.ComputeCameraAngles e) {
        if (phase != PortalRitual.CITY_CUTSCENE) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        float pt = (float) e.getPartialTick();
        float t = tick + pt;
        if (t >= PortalRitual.CUTSCENE_TICKS - 3.0F) return;

        // Плавно вычисляем точные пофреймовые углы камеры для 60+ FPS без ступенек тика!
        double baseY = center.getY();
        Vec3 shotStart = new Vec3(26.0, baseY + 34.0, 92.0);
        Vec3 shotHoldEnd = new Vec3(21.5, baseY + 30.5, 82.0);
        Vec3 cityTarget = new Vec3(-2.0, baseY + 18.0, -18.0);
        if (t <= 126.0F) {
            float u = Mth.clamp(t / 126.0F, 0.0F, 1.0F);
            Vec3 cp = shotStart.lerp(shotHoldEnd, u);
            Vec3 look = cityTarget.subtract(cp);
            e.setYaw((float) (Mth.atan2(-look.x, look.z) * Mth.RAD_TO_DEG));
            e.setPitch((float) (-Mth.atan2(look.y, Math.hypot(look.x, look.z)) * Mth.RAD_TO_DEG));
        } else {
            float u = Mth.clamp((t - 126.0F) / 54.0F, 0.0F, 1.0F);
            float s = u * u * u * (u * (u * 6.0F - 15.0F) + 10.0F);
            Vec3 lookStart = cityTarget.subtract(shotHoldEnd);
            float startYaw = (float) (Mth.atan2(-lookStart.x, lookStart.z) * Mth.RAD_TO_DEG);
            float startPitch = (float) (-Mth.atan2(lookStart.y, Math.hypot(lookStart.x, lookStart.z)) * Mth.RAD_TO_DEG);
            e.setYaw(rotLerp(s, startYaw, mc.player.getViewYRot(pt)));
            e.setPitch(Mth.lerp(s, startPitch, mc.player.getViewXRot(pt)));
        }
        e.setRoll(0.0F);
    }

    private static void renderCutsceneVeilAndTitles(GuiGraphics g, float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        int sw = g.guiWidth();
        int sh = g.guiHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        // 1) Яркая вспышка ИМЕННО при входе в просвет (TRANSIT_VEIL t <= 4) с переходом в мягкую фарфоровую вуаль
        float veil = veilOpacity(pt);
        if (veil > 0.004F) {
            int alpha = Mth.clamp(Math.round(veil * 255.0F), 0, 255);
            int rgb = SOFT_VEIL_RGB;
            if (phase == PortalRitual.TRANSIT_VEIL) {
                float flashMix = 1.0F - Mth.clamp(currentTime(pt) / 4.5F, 0.0F, 1.0F);
                int vr = Math.round(Mth.lerp(flashMix, (SOFT_VEIL_RGB >> 16) & 0xFF, (ENTRY_FLASH_RGB >> 16) & 0xFF));
                int vg = Math.round(Mth.lerp(flashMix, (SOFT_VEIL_RGB >> 8) & 0xFF, (ENTRY_FLASH_RGB >> 8) & 0xFF));
                int vb = Math.round(Mth.lerp(flashMix, SOFT_VEIL_RGB & 0xFF, ENTRY_FLASH_RGB & 0xFF));
                rgb = (vr << 16) | (vg << 8) | vb;
            }
            g.fill(0, 0, sw, sh, (alpha << 24) | rgb);
        }

        // 2) В фазе CITY_CUTSCENE на дальнем ракурсе города показываем Название Измерения и снизу Название Города,
        //    после чего к тику 124 текст полностью исчезает ПЕРЕД тем, как камера плавно полетит к игроку!
        if (phase == PortalRitual.CITY_CUTSCENE) {
            float t = currentTime(pt);
            float textIn = Mth.clamp((t - 10.0F) / 14.0F, 0.0F, 1.0F);
            float textOut = 1.0F - Mth.clamp((t - 106.0F) / 18.0F, 0.0F, 1.0F);
            float textAlpha = textIn * textIn * (3.0F - 2.0F * textIn) * (textOut * textOut);

            if (textAlpha > 0.02F) {
                Font font = mc.font;
                String dimTitle = Component.translatable("cutscene.riftworld.elisar.dim").getString();
                String citySub = Component.translatable("cutscene.riftworld.elisar.city").getString();

                int aMain = Mth.clamp(Math.round(textAlpha * 252.0F), 4, 255);
                int aPlate = Mth.clamp(Math.round(textAlpha * 138.0F), 0, 255);
                int aGold = Mth.clamp(Math.round(textAlpha * 235.0F), 4, 255);

                int centerX = sw / 2;
                int bannerY = (int) (sh * 0.72F);

                // Тонкая кинематографическая полупрозрачная подложка, чтобы светлые буквы идеально читались на фоне Белого Города
                int plateHalfW = 156;
                g.fill(centerX - plateHalfW, bannerY - 24, centerX + plateHalfW, bannerY + 32, (aPlate << 24) | 0x1A1712);

                // Крупное название измерения: «Э Л И С А Р»
                g.pose().pushPose();
                g.pose().translate(centerX, bannerY - 15, 0.0F);
                g.pose().scale(2.40F, 2.40F, 1.0F);
                int dimW = font.width(dimTitle);
                g.drawString(font, dimTitle, -dimW / 2, 0, (aMain << 24) | 0xFCF7EB, true);
                g.pose().popPose();

                // Изящная золотая разделительная линия с центральным ромбом
                int lineHalfW = 92;
                g.fill(centerX - lineHalfW, bannerY + 9, centerX + lineHalfW, bannerY + 10, (aGold << 24) | 0xE2C478);
                g.fill(centerX - 3, bannerY + 7, centerX + 3, bannerY + 12, (aGold << 24) | 0xF5E1A4);

                // Снизу — название города: «Белый Город Святилища»
                g.pose().pushPose();
                g.pose().translate(centerX, bannerY + 16, 0.0F);
                g.pose().scale(1.25F, 1.25F, 1.0F);
                int subW = font.width(citySub);
                g.drawString(font, citySub, -subW / 2, 0, (aMain << 24) | 0xEDE3CA, true);
                g.pose().popPose();
            }
        }

        g.flush();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static float openSkyAuraFactor(Vec3 camPos, float pt) {
        if (phase == PortalRitual.AWAKEN) {
            float u = Mth.clamp((currentTime(pt) - 8.0F) / 58.0F, 0.0F, 1.0F);
            return u * u * (3.0F - 2.0F * u);
        }
        if (openRiftCenter != null) {
            double dx = camPos.x - (openRiftCenter.getX() + 0.5);
            double dz = camPos.z - (openRiftCenter.getZ() + 0.5);
            float dist = (float) Math.hypot(dx, dz);
            float k = Mth.clamp((54.0F - dist) / 42.0F, 0.0F, 1.0F);
            return k * k * 0.72F;
        }
        return 0.0F;
    }

    @SubscribeEvent
    public static void fogColor(ViewportEvent.ComputeFogColor e) {
        float k = openSkyAuraFactor(e.getCamera().getPosition(), (float) e.getPartialTick());
        if (k <= 0.002F) return;
        e.setRed(Mth.lerp(k, e.getRed(), 0.945F));
        e.setGreen(Mth.lerp(k, e.getGreen(), 0.928F));
        e.setBlue(Mth.lerp(k, e.getBlue(), 0.888F));
    }

    @SubscribeEvent
    public static void renderFog(ViewportEvent.RenderFog e) {
        if (e.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;
        if (e.getCamera().getFluidInCamera() != FogType.NONE) return;
        if (ShaderCompat.isShaderPackActive()) return;
        float k = openSkyAuraFactor(e.getCamera().getPosition(), (float) e.getPartialTick());
        if (k <= 0.002F) return;
        float far = e.getFarPlaneDistance();
        e.setNearPlaneDistance(Mth.lerp(k, far * 0.65F, 26.0F));
        e.setFarPlaneDistance(Mth.lerp(k, far, 86.0F));
        e.setCanceled(true);
    }

    @SubscribeEvent
    public static void renderStage(RenderLevelStageEvent e) {
        boolean hasBleach = (phase == PortalRitual.AWAKEN || openRiftCenter != null);
        if (phase == PortalRitual.NONE && !hasBleach) return;
        if (ShaderCompat.isRenderingShadowPass()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float pt = e.getPartialTick();
        Vec3 cam = e.getCamera().getPosition();
        PoseStack ps = e.getPoseStack();

        if (hasBleach && e.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY
                && !ShaderCompat.isShaderPackActive()) {
            float k = openSkyAuraFactor(cam, pt);
            int skyA = Mth.clamp(Math.round(k * 232.0F), 0, 232);
            if (skyA > 2) {
                float r = Math.min(mc.gameRenderer.getDepthFar() * 0.52F, 180.0F);
                ShaderCompat.drawSphericalSkyDome(ps.last().pose(), r,
                        242, 238, 228, skyA,
                        231, 226, 214, skyA,
                        216, 210, 196, skyA);
            }
            return;
        }

        if (hasBleach && e.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            renderElisarSurfaceAndFloorPatterns(ps, cam, pt);
        } else if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            if (hasBleach) {
                renderOpeningGroundSeal(ps, cam, pt);
            }
            if (phase == PortalRitual.AWAKEN) {
                renderCelestial2DEyesInSky(mc, ps, e.getCamera(), pt);
            }
            if (mc.options.hideGui && isHudAndPerspectiveLocked()) {
                renderAwakenFlashQuad(ps, e.getCamera(), pt);
            }
        }
    }

    /** Красивый разворачивающийся на земле многокольцевой геометрический узор при открытии и вокруг открытого портала! */
    private static void renderOpeningGroundSeal(PoseStack ps, Vec3 cam, float pt) {
        BlockPos bc = activeBleachCenter();
        if (bc == null) return;
        float t = activeBleachTime(pt);
        float ease = (openRiftCenter != null) ? 1.0F : Mth.clamp(t / 52.0F, 0.0F, 1.0F);
        ease = 1.0F - (1.0F - ease) * (1.0F - ease) * (1.0F - ease);
        if (ease <= 0.02F) return;

        float cx = bc.getX() + 0.5F - (float) cam.x;
        float cy = bc.getY() + 0.045F - (float) cam.y;
        float cz = bc.getZ() + 0.5F - (float) cam.z;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, OPEN_SEAL_TEX);

        drawWorldGroundDisc(ps, cx, cy, cz, 5.4F * ease, t * 0.024F, 252, 247, 236, Mth.clamp(Math.round(ease * 242.0F), 0, 255));
        drawWorldGroundDisc(ps, cx, cy + 0.015F, cz, 3.0F * ease, -t * 0.040F, 255, 253, 246, Mth.clamp(Math.round(ease * 252.0F), 0, 255));

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void drawWorldGroundDisc(PoseStack ps, float cx, float cy, float cz, float rad, float rot,
                                            int r, int g, int b, int a) {
        if (a <= 2) return;
        ps.pushPose();
        ps.translate(cx, cy, cz);
        ps.mulPose(com.mojang.math.Axis.YP.rotation(rot));
        Matrix4f m = ps.last().pose();
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bb.vertex(m, -rad, 0.0F, -rad).uv(0.0F, 0.0F).color(r, g, b, a).endVertex();
        bb.vertex(m, -rad, 0.0F, rad).uv(0.0F, 1.0F).color(r, g, b, a).endVertex();
        bb.vertex(m, rad, 0.0F, rad).uv(1.0F, 1.0F).color(r, g, b, a).endVertex();
        bb.vertex(m, rad, 0.0F, -rad).uv(1.0F, 0.0F).color(r, g, b, a).endVertex();
        Tesselator.getInstance().end();
        ps.popPose();
    }

    private static void renderAwakenFlashQuad(PoseStack ps, Camera camera, float pt) {
        float fl = veilOpacity(pt);
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
        bb.vertex(mat, (float) p0.x, (float) p0.y, (float) p0.z).uv(0.5F, 0.5F).color(250, 245, 234, alpha).endVertex();
        bb.vertex(mat, (float) p1.x, (float) p1.y, (float) p1.z).uv(0.5F, 0.5F).color(250, 245, 234, alpha).endVertex();
        bb.vertex(mat, (float) p2.x, (float) p2.y, (float) p2.z).uv(0.5F, 0.5F).color(250, 245, 234, alpha).endVertex();
        bb.vertex(mat, (float) p3.x, (float) p3.y, (float) p3.z).uv(0.5F, 0.5F).color(250, 245, 234, alpha).endVertex();
        Tesselator.getInstance().end();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void renderElisarSurfaceAndFloorPatterns(PoseStack ps, Vec3 cam, float pt) {
        if (faceCount <= 0) return;
        BlockPos bc = activeBleachCenter();
        if (bc == null) return;
        float rad = bleachRadius(pt);
        if (rad <= 0.2F) return;

        Matrix4f mat = ps.last().pose();
        float camX = (float) cam.x, camY = (float) cam.y, camZ = (float) cam.z;
        float rcx = bc.getX() + 0.5F;
        float rcz = bc.getZ() + 0.5F;
        float t = activeBleachTime(pt);

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

        RenderSystem.setShaderTexture(0, ShaderCompat.WHITE_TEX);
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < faceCount; ++i) {
            if (fDir[i] == 1) continue;
            float d = fDist[i];
            if (d > rad) continue;
            // Волна раскрытия + постоянный градиент на 50 блоков: у разлома густо-белый, к краю 50 м всё скуднее!
            float wave = Mth.clamp((rad - d) / 6.5F, 0.0F, 1.0F);
            float radial = Mth.clamp((MAX_BLEACH_RADIUS - d) / (MAX_BLEACH_RADIUS - 7.0F), 0.0F, 1.0F);
            float w = wave * wave * (3.0F - 2.0F * wave) * (float) Math.pow(radial, 1.50);
            int a = Mth.clamp(Math.round(w * 224.0F), 0, 255);
            if (a <= 2) continue;

            int r, g, b;
            switch (fDir[i]) {
                case 0 -> { r = 196; g = 190; b = 178; }
                case 2, 3 -> { r = 236; g = 231; b = 220; }
                default -> { r = 224; g = 219; b = 207; }
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
                    bb.vertex(mat, xp, y0, z1).uv(0.5F, 0.5F).color(r, g, b, a).endVertex();
                    bb.vertex(mat, xp, y1, z1).uv(0.5F, 0.5F).color(r, g, b, a).endVertex();
                    bb.vertex(mat, xp, y1, z0).uv(0.5F, 0.5F).color(r, g, b, a).endVertex();
                    bb.vertex(mat, xp, y0, z0).uv(0.5F, 0.5F).color(r, g, b, a).endVertex();
                }
            }
        }
        Tesselator.getInstance().end();

        RenderSystem.setShaderTexture(0, FLOOR_TILE_TEX);
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < faceCount; ++i) {
            if (fDir[i] != 1) continue;
            float d = fDist[i];
            if (d > rad) continue;
            float wave = Mth.clamp((rad - d) / 6.0F, 0.0F, 1.0F);
            float radial = Mth.clamp((MAX_BLEACH_RADIUS - d) / (MAX_BLEACH_RADIUS - 7.5F), 0.0F, 1.0F);
            float w = wave * wave * (3.0F - 2.0F * wave) * (float) Math.pow(radial, 1.42);
            float crest = Mth.clamp(1.0F - Math.abs((rad - d) - 1.2F) / 1.6F, 0.0F, 1.0F);
            int tr = 255;
            int tg = Mth.clamp(Math.round(Mth.lerp(crest * 0.25F, 254.0F, 240.0F)), 0, 255);
            int tb = Mth.clamp(Math.round(Mth.lerp(crest * 0.35F, 250.0F, 198.0F)), 0, 255);
            int a = Mth.clamp(Math.round(w * 246.0F), 0, 255);
            if (a <= 2) continue;

            float wx0 = fMinX[i], wz0 = fMinZ[i];
            float wx1 = fMaxX[i], wz1 = fMaxZ[i];
            float yp = (fMaxY[i] - camY) + 0.004F;

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

        RenderSystem.polygonOffset(-2.5F, -4.0F);
        RenderSystem.setShaderTexture(0, FLOOR_MANDALA_TEX);
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float rot = t * 0.0055F;
        float cosR = Mth.cos(rot), sinR = Mth.sin(rot);

        for (int i = 0; i < faceCount; ++i) {
            if (fDir[i] != 1) continue;
            float d = fDist[i];
            if (d > rad || d > 17.5F) continue;
            if (fMaxY[i] > bc.getY() + 4.5F) continue;

            float uWave = Mth.clamp((rad - d) / 4.2F, 0.0F, 1.0F);
            float innerFade = Mth.clamp((17.5F - d) / 8.5F, 0.0F, 1.0F);
            float pulse = 0.90F + 0.10F * Mth.sin(t * 0.14F - d * 0.35F);
            int a = Mth.clamp(Math.round(uWave * uWave * (3.0F - 2.0F * uWave) * innerFade * 246.0F * pulse), 0, 255);
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

    private static void renderCelestial2DEyesInSky(Minecraft mc, PoseStack ps, Camera camera, float pt) {
        float t = currentTime(pt);
        float settleFade = t <= PortalRitual.AWAKEN_TICKS
                ? 1.0F
                : 1.0F - Mth.clamp((t - PortalRitual.AWAKEN_TICKS) / 12.0F, 0.0F, 1.0F);
        if (settleFade <= 0.01F) return;

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

            float fade = Mth.clamp(dt / 9.0F, 0.0F, 1.0F);
            float alpha = fade * 0.74F * settleFade;
            if (alpha <= 0.01F) continue;

            ResourceLocation lid;
            if (dt < 3.5F) {
                lid = ElisarEye.CLOSED;
            } else if (dt < 7.0F) {
                lid = ElisarEye.HALF;
            } else {
                float blinkCycle = (t * 0.065F + skyEyeSeed[i] * 2.1F) % 6.283F;
                if (t < PortalRitual.AWAKEN_TICKS - 10.0F && blinkCycle > 5.88F) {
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

    private static void captureBleachFaces(ClientLevel level, BlockPos c) {
        capturedFor = c.immutable();
        faceCount = 0;
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos np = new BlockPos.MutableBlockPos();
        Direction[] dirs = Direction.values();

        double cx = c.getX() + 0.5, cy = c.getY() + 0.5, cz = c.getZ() + 0.5;
        int r2Max = BLEACH_SCAN_R * BLEACH_SCAN_R;

        // Обходим концентрическими кольцами от центра разлома наружу до 52 блоков, чтобы поле всегда было 100% симметричным!
        for (int ring = 0; ring <= BLEACH_SCAN_R && faceCount < MAX_FACES; ++ring) {
            for (int dx = -ring; dx <= ring && faceCount < MAX_FACES; ++dx) {
                for (int dz = -ring; dz <= ring && faceCount < MAX_FACES; ++dz) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    int h2 = dx * dx + dz * dz;
                    if (h2 > r2Max) continue;

                    for (int dy = -12; dy <= 24 && faceCount < MAX_FACES; ++dy) {
                        if (h2 + dy * dy > r2Max) continue;

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
                                        + (by + 0.5 - cy) * (by + 0.5 - cy) * 0.45
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
    }

    /** Мягкая не-ослепляющая фарфоровая маска вместо ванильного земляного экрана загрузки при смене измерения. */
    public static final class WhiteReceivingScreen extends ReceivingLevelScreen {
        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {
            g.fill(0, 0, this.width, this.height, 0xFF000000 | SOFT_VEIL_RGB);
        }

        @Override
        public void renderBackground(GuiGraphics g) {
        }
    }
}
