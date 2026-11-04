package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import ru.aiforge.riftworld.RiftWorld;

/**
 * Универсальный слой совместимости рендереров RiftWorld со ВСЕМИ шейдерпаками
 * (Oculus, Iris, OptiFine: IterationT, Complementary, Bliss, Photon, BSL, SEUS, Sildur's, Kappa и др.)
 * без жёстких зависимостей времени компиляции.
 *
 * <ul>
 *   <li>Определяет активность любого шейдерпака ({@link #isShaderPackActive()}) и теневого прохода
 *       ({@link #isRenderingShadowPass()}) через кэшированные {@link MethodHandle} за наносекунды;</li>
 *   <li>Предоставляет чистую белую текстуру {@link #WHITE_TEX} для рендера через {@code POSITION_TEX_COLOR}
 *       ({@code gbuffers_textured}), чтобы шейдерпаки не ломали альфа-канал и G-буфер на {@code gbuffers_basic}
 *       или {@code RenderType.debugQuads()};</li>
 *   <li>Вычисляет точную небесную дистанцию {@link #celestialDistance(Minecraft)} дальше всех загруженных чанков,
 *       но строго внутри {@code depthFar}, чтобы небесные 2D-глаза корректно перекрывались любыми твёрдыми блоками
 *       и островами на любой дистанции прорисовки.</li>
 * </ul>
 */
public final class ShaderCompat {
    public static final ResourceLocation WHITE_TEX = new ResourceLocation(RiftWorld.MODID, "textures/misc/white.png");

    private static final Object IRIS_API_INSTANCE;
    private static final MethodHandle IRIS_API_PACK_IN_USE;
    private static final MethodHandle IRIS_API_SHADOW_PASS;

    private static final MethodHandle LEGACY_IRIS_CURRENT_PACK;
    private static final MethodHandle LEGACY_IRIS_SHADOW_STATE;

    private static final Field OPTIFINE_PACK_LOADED;
    private static final Field OPTIFINE_SHADOW_PASS;

    static {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();

        Object apiInst = null;
        MethodHandle apiPack = null;
        MethodHandle apiShadow = null;
        try {
            Class<?> apiCls = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Method getInst = apiCls.getMethod("getInstance");
            apiInst = getInst.invoke(null);
            apiPack = lookup.unreflect(apiCls.getMethod("isShaderPackInUse"));
            apiShadow = lookup.unreflect(apiCls.getMethod("isRenderingShadowPass"));
        } catch (Throwable ignored) {
        }
        IRIS_API_INSTANCE = apiInst;
        IRIS_API_PACK_IN_USE = apiPack;
        IRIS_API_SHADOW_PASS = apiShadow;

        MethodHandle legPack = null;
        MethodHandle legShadow = null;
        try {
            for (String irisClsName : new String[]{"net.irisshaders.iris.Iris", "net.coderbot.iris.Iris"}) {
                try {
                    Class<?> irisCls = Class.forName(irisClsName);
                    legPack = lookup.unreflect(irisCls.getMethod("getCurrentPack"));
                    break;
                } catch (Throwable ignored) {
                }
            }
            for (String shClsName : new String[]{
                    "net.irisshaders.iris.shadows.ShadowRenderingState",
                    "net.coderbot.iris.shadows.ShadowRenderingState"}) {
                try {
                    Class<?> shCls = Class.forName(shClsName);
                    legShadow = lookup.unreflect(shCls.getMethod("areShadowsCurrentlyBeingRendered"));
                    break;
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        LEGACY_IRIS_CURRENT_PACK = legPack;
        LEGACY_IRIS_SHADOW_STATE = legShadow;

        Field ofLoaded = null;
        Field ofShadow = null;
        try {
            Class<?> ofCls = Class.forName("net.optifine.shaders.Shaders");
            ofLoaded = ofCls.getDeclaredField("shaderPackLoaded");
            ofLoaded.setAccessible(true);
            ofShadow = ofCls.getDeclaredField("isShadowPass");
            ofShadow.setAccessible(true);
        } catch (Throwable ignored) {
        }
        OPTIFINE_PACK_LOADED = ofLoaded;
        OPTIFINE_SHADOW_PASS = ofShadow;
    }

    private ShaderCompat() {
    }

    /** Возвращает {@code true}, если активен любой шейдерпак в Oculus, Iris или OptiFine. */
    public static boolean isShaderPackActive() {
        if (IRIS_API_INSTANCE != null && IRIS_API_PACK_IN_USE != null) {
            try {
                if ((boolean) IRIS_API_PACK_IN_USE.invoke(IRIS_API_INSTANCE)) return true;
            } catch (Throwable ignored) {
            }
        }
        if (LEGACY_IRIS_CURRENT_PACK != null) {
            try {
                Object opt = LEGACY_IRIS_CURRENT_PACK.invoke();
                if (opt instanceof Optional<?> o && o.isPresent()) return true;
            } catch (Throwable ignored) {
            }
        }
        if (OPTIFINE_PACK_LOADED != null) {
            try {
                if (OPTIFINE_PACK_LOADED.getBoolean(null)) return true;
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    /** Возвращает {@code true} во время рендера карты теней шейдерпака (Oculus / Iris / OptiFine). */
    public static boolean isRenderingShadowPass() {
        if (IRIS_API_INSTANCE != null && IRIS_API_SHADOW_PASS != null) {
            try {
                if ((boolean) IRIS_API_SHADOW_PASS.invoke(IRIS_API_INSTANCE)) return true;
            } catch (Throwable ignored) {
            }
        }
        if (LEGACY_IRIS_SHADOW_STATE != null) {
            try {
                if ((boolean) LEGACY_IRIS_SHADOW_STATE.invoke()) return true;
            } catch (Throwable ignored) {
            }
        }
        if (OPTIFINE_SHADOW_PASS != null) {
            try {
                if (OPTIFINE_SHADOW_PASS.getBoolean(null)) return true;
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    /**
     * Дистанция небесной сферы (для 2D-глаза Элисара и небесных глаз ритуала):
     * гарантированно дальше всех загруженных чанков и островов ({@code ~2.35 * renderDistance}),
     * но строго внутри дальней плоскости отсечения {@code depthFar}, чтобы тест глубины
     * перекрывал глаз любыми твёрдыми блоками без просвечивания и без клиппинга неба.
     */
    public static float celestialDistance(Minecraft mc) {
        if (mc == null || mc.gameRenderer == null) return 280.0F;
        float renderDist = mc.gameRenderer.getRenderDistance();
        float depthFar = mc.gameRenderer.getDepthFar();
        float desired = Math.max(220.0F, renderDist * 2.35F);
        float maxSafe = Math.max(140.0F, depthFar * 0.78F);
        return Math.min(desired, maxSafe);
    }

    /**
     * Рисует гладкий сферический купол неба без углов куба (строго внутри {@code depthFar})
     * через {@code POSITION_TEX_COLOR} + {@link #WHITE_TEX}.
     */
    public static void drawSphericalSkyDome(Matrix4f mat, float radius,
                                            int rZen, int gZen, int bZen, int aZen,
                                            int rHor, int gHor, int bHor, int aHor,
                                            int rNad, int gNad, int bNad, int aNad) {
        int rings = 12;
        int segs = 24;

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, WHITE_TEX);

        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        for (int ri = 0; ri < rings; ++ri) {
            float v0 = -0.5F * Mth.PI + (Mth.PI * ri) / rings;
            float v1 = -0.5F * Mth.PI + (Mth.PI * (ri + 1)) / rings;
            float y0 = Mth.sin(v0), cos0 = Mth.cos(v0);
            float y1 = Mth.sin(v1), cos1 = Mth.cos(v1);

            int c0r = domeChannel(y0, rZen, rHor, rNad);
            int c0g = domeChannel(y0, gZen, gHor, gNad);
            int c0b = domeChannel(y0, bZen, bHor, bNad);
            int c0a = domeChannel(y0, aZen, aHor, aNad);

            int c1r = domeChannel(y1, rZen, rHor, rNad);
            int c1g = domeChannel(y1, gZen, gHor, gNad);
            int c1b = domeChannel(y1, bZen, bHor, bNad);
            int c1a = domeChannel(y1, aZen, aHor, aNad);

            if (c0a <= 0 && c1a <= 0) continue;

            for (int si = 0; si < segs; ++si) {
                float a0 = (Mth.TWO_PI * si) / segs;
                float a1 = (Mth.TWO_PI * (si + 1)) / segs;
                float x00 = Mth.cos(a0) * cos0 * radius, z00 = Mth.sin(a0) * cos0 * radius;
                float x10 = Mth.cos(a1) * cos0 * radius, z10 = Mth.sin(a1) * cos0 * radius;
                float x11 = Mth.cos(a1) * cos1 * radius, z11 = Mth.sin(a1) * cos1 * radius;
                float x01 = Mth.cos(a0) * cos1 * radius, z01 = Mth.sin(a0) * cos1 * radius;

                bb.vertex(mat, x00, y0 * radius, z00).uv(0.5F, 0.5F).color(c0r, c0g, c0b, c0a).endVertex();
                bb.vertex(mat, x10, y0 * radius, z10).uv(0.5F, 0.5F).color(c0r, c0g, c0b, c0a).endVertex();
                bb.vertex(mat, x11, y1 * radius, z11).uv(0.5F, 0.5F).color(c1r, c1g, c1b, c1a).endVertex();
                bb.vertex(mat, x01, y1 * radius, z01).uv(0.5F, 0.5F).color(c1r, c1g, c1b, c1a).endVertex();
            }
        }

        Tesselator.getInstance().end();

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static int domeChannel(float sinLat, int zen, int hor, int nad) {
        if (sinLat >= 0.0F) {
            float t = (float) Math.pow(sinLat, 0.65);
            return Mth.clamp(Math.round(Mth.lerp(t, hor, zen)), 0, 255);
        }
        float t = (float) Math.pow(-sinLat, 0.75);
        return Mth.clamp(Math.round(Mth.lerp(t, hor, nad)), 0, 255);
    }
}
