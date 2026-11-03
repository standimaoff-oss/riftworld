package ru.aiforge.riftworld;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * v0.38.0 — атмосфера «Мороковой Пади» вынесена в конфиг.
 *
 * <p>Раньше плотность и дальность мглы были зашиты числом в {@code ElisarSky}: игрок не мог
 * ни сделать биом «проходимым» (уменьшить мглу), ни усилить хоррор. Теперь они лежат в
 * {@code config/riftworld-atmosphere.toml}:
 *
 * <pre>
 * [morok]
 *   fogNearBlocks = 6.5    # ближняя граница мглы (в блоках)
 *   fogFarBlocks  = 54.0   # дальняя граница: сколько вообще видно в чаще
 *   color         = "#C9D6D8"
 *   colorStrength = 0.94   # насколько густо мгла красит небо
 * </pre>
 *
 * <p>Файл клиентский: дальность прорисовки тумана — чисто клиентская величина, серверу она
 * не нужна и не должна рассинхронизировать игроков.
 */
public final class RiftConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.DoubleValue MOROK_FOG_NEAR;
    public static final ForgeConfigSpec.DoubleValue MOROK_FOG_FAR;
    public static final ForgeConfigSpec.DoubleValue MOROK_FOG_STRENGTH;
    public static final ForgeConfigSpec.ConfigValue<String> MOROK_FOG_COLOR;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("«Мороковая Падь»: насколько густая мгла стоит в чаще").push("morok");
        MOROK_FOG_NEAR = b
                .comment("Ближняя граница тумана в блоках. Меньше — мгла начинается раньше.")
                .defineInRange("fogNearBlocks", 6.5D, 1.0D, 64.0D);
        MOROK_FOG_FAR = b
                .comment("Дальняя граница тумана в блоках: сколько видно в чаще.")
                .defineInRange("fogFarBlocks", 54.0D, 8.0D, 256.0D);
        MOROK_FOG_COLOR = b
                .comment("Цвет мглы в HEX (#RRGGBB). Костяной серо-зелёный из концепта — #C9D6D8.")
                .define("color", "#C9D6D8");
        MOROK_FOG_STRENGTH = b
                .comment("Насколько сильно мгла перекрашивает небо и горизонт (0..1).")
                .defineInRange("colorStrength", 0.94D, 0.0D, 1.0D);
        b.pop();
        CLIENT_SPEC = b.build();
    }

    private RiftConfig() {
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, "riftworld-atmosphere.toml");
    }

    /** Ширина мглы в блоках; безопасно возвращает значение из концепта, если конфиг ещё не загружен. */
    public static float morokFogNear() {
        try {
            return MOROK_FOG_NEAR.get().floatValue();
        } catch (Throwable ignored) {
            return 6.5F;
        }
    }

    public static float morokFogFar() {
        try {
            return MOROK_FOG_FAR.get().floatValue();
        } catch (Throwable ignored) {
            return 54.0F;
        }
    }

    public static float morokFogStrength() {
        try {
            return MOROK_FOG_STRENGTH.get().floatValue();
        } catch (Throwable ignored) {
            return 0.94F;
        }
    }

    /** Цвет мглы: {@code {r, g, b}} в 0..1. */
    public static float[] morokFogColor() {
        String hex = "#C9D6D8";
        try {
            hex = MOROK_FOG_COLOR.get();
        } catch (Throwable ignored) {
            // конфиг ещё не загружен — берём цвет концепта
        }
        try {
            String h = hex.trim();
            if (h.startsWith("#")) h = h.substring(1);
            int v = Integer.parseInt(h, 16);
            return new float[]{((v >> 16) & 0xFF) / 255.0F, ((v >> 8) & 0xFF) / 255.0F, (v & 0xFF) / 255.0F};
        } catch (Throwable ignored) {
            return new float[]{0.788F, 0.839F, 0.847F};
        }
    }
}
