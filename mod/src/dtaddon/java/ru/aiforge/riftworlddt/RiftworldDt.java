package ru.aiforge.riftworlddt;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Рев 13 — АДДОН К DYNAMIC TREES для мода «Разлом».
 *
 * <p>Почти весь аддон — данные: в jar лежит папка {@code trees/riftworlddt/}, которую Dynamic Trees
 * сканирует у всех модов. Оттуда берутся семейства, виды, свойства листьев/грунта, формы крон
 * и правила, где деревья растут.
 *
 * <p><b>Что было сломано и что теперь сделано.</b>
 * <ol>
 *   <li><b>Краш NPE в {@code SoilProperties#getBlock()}.</b> DT читает JSON в момент регистрации
 *       блоков и требует, чтобы «примитивные» блоки мода (<code>riftworld:sizo_log</code>,
 *       <code>riftworld:elisar_grass</code>) уже были в реестре. Мы регистрируем их на приоритете
 *       HIGHEST (см. {@code ru.aiforge.riftworld.registry.EarlyRegistrations}).</li>
 *   <li><b>«Не найдены LeavesProperties для riftworlddt:sizo / riftworlddt:ambra».</b> Причина та же:
 *       {@code primitive_leaves} указывали на ещё не зарегистрированные блоки, DT подставлял
 *       {@code minecraft:air}, свойства листьев считались недействительными, и виды оставались без листвы.</li>
 *   <li><b>Блоки DT регистрировались в namespace {@code dynamictrees}.</b> Без вызова
 *       {@code RegistryHandler.setup(modid)} DT принудительно переименовывает namespace любого
 *       своего блока в {@code dynamictrees} ({@code correctRegistryName}). Из-за этого не находились
 *       наши blockstate-файлы (они лежат в {@code assets/riftworlddt}) и не работал
 *       {@code give riftworlddt:sizo_seed}. Теперь setup вызывается рефлексией (чтобы аддон
 *       собирался и без DT в classpath) и <b>проверяется</b>: если хендлер не зарегистрировался,
 *       аддон падает с понятным сообщением, а не заливает лог 255 «missing model».</li>
 * </ol>
 */
@Mod(RiftworldDt.MOD_ID)
public class RiftworldDt {
    public static final String MOD_ID = "riftworlddt";
    private static final Logger LOG = LoggerFactory.getLogger("riftworlddt");

    public RiftworldDt() {
        if (!ModList.get().isLoaded("dynamictrees")) {
            LOG.warn("[riftworlddt] Dynamic Trees не найден — аддон простаивает (данные в trees/ никому не нужны)");
            return;                     // аддон без DT просто ничего не делает
        }
        try {
            // Точка входа DT для аддонов: свой RegistryHandler → наши блоки/предметы
            // регистрируются под namespace riftworlddt (а не dynamictrees).
            Class<?> handlerCls = Class.forName("com.ferreusveritas.dynamictrees.api.registry.RegistryHandler");
            handlerCls.getMethod("setup", String.class).invoke(null, MOD_ID);
            // Проверяем, что хендлер реально встал в реестр DT: иначе correctRegistryName()
            // переименует ВСЕ наши блоки в dynamictrees:* и ассеты не найдутся.
            Object handler = handlerCls.getMethod("get", String.class).invoke(null, MOD_ID);
            boolean valid = handler != null && (Boolean) handler.getClass().getMethod("isValid").invoke(handler);
            if (!valid) {
                throw new IllegalStateException("RegistryHandler для '" + MOD_ID + "' не зарегистрировался");
            }
            LOG.info("[riftworlddt] RegistryHandler setup OK: блоки/предметы аддона идут в namespace '{}'", MOD_ID);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Dynamic Trees найден, но API RegistryHandler недоступен "
                    + "(версия DT несовместима с аддоном " + MOD_ID + ")", e);
        }
        installGroundFinder();
    }

    /**
     * Рев 14.40 — ПОЧЕМУ ДЕРЕВЬЯ DT НЕ ПОЯВЛЯЛИСЬ В МИРЕ, хотя вид регистрировался.
     *
     * <p>DT ищет место под дерево через {@code GroundFinder}: по умолчанию берётся
     * {@code WorldSurface}-heightmap мира. У нас <b>свой</b> генератор: рельеф пишется в чанк
     * напрямую, WG-heightmap на момент фич остаётся пустым, поэтому DT «находил грунт» в воздухе
     * и молча возвращал {@code FAIL_SOIL}/{@code NO_GROUND}. Отсюда и «аддон ничего не меняет»:
     * ни деревьев, ни кораллов в мире, только через ручную команду settree.
     *
     * <p>Лечим честно: регистрируем для измерения {@code riftworld:elisar} свой {@code GroundFinder},
     * который не верит heightmap, а сканирует колонку сверху вниз и отдаёт первый твёрдый блок
     * с двумя блоками воздуха над ним. Интерфейс берём прокси, чтобы аддон по-прежнему собирался
     * без Dynamic Trees в classpath (см. шапку класса).
     */
    private static void installGroundFinder() {
        try {
            Class<?> gf = Class.forName("com.ferreusveritas.dynamictrees.api.worldgen.GroundFinder");
            Object finder = java.lang.reflect.Proxy.newProxyInstance(gf.getClassLoader(), new Class<?>[]{gf},
                    (proxy, method, args) -> {
                        String name = method.getName();
                        if ("findGround".equals(name)) {
                            net.minecraft.world.level.LevelAccessor level = (net.minecraft.world.level.LevelAccessor) args[0];
                            net.minecraft.core.BlockPos start = (net.minecraft.core.BlockPos) args[1];
                            net.minecraft.core.BlockPos ground = surfaceUnder(level, start);
                            return ground == null ? java.util.List.of() : java.util.List.of(ground);
                        }
                        if ("toString".equals(name)) return "riftworld:surface-scan";
                        if ("hashCode".equals(name)) return System.identityHashCode(proxy);
                        if ("equals".equals(name)) return proxy == args[0];
                        return null;
                    });
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim =
                    net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                            new net.minecraft.resources.ResourceLocation("riftworld", "elisar"));
            gf.getMethod("registerGroundFinder", net.minecraft.resources.ResourceKey.class, gf).invoke(null, dim, finder);
            LOG.info("[riftworlddt] GroundFinder для riftworld:elisar установлен: грунт ищется сканом колонки");
        } catch (Throwable t) {
            LOG.warn("[riftworlddt] GroundFinder не установлен ({}); DT будет искать грунт по heightmap", t.toString());
        }
    }

    /**
     * Верхний твёрдый блок колонки, над которым два блока воздуха. Листва, цветы и лаванда
     * грунтом не считаются: дерево должно стоять на земле, а не на кусте.
     */
    private static net.minecraft.core.BlockPos surfaceUnder(net.minecraft.world.level.LevelAccessor level,
                                                            net.minecraft.core.BlockPos start) {
        if (start == null) return null;
        net.minecraft.core.BlockPos.MutableBlockPos p =
                new net.minecraft.core.BlockPos.MutableBlockPos(start.getX(), 0, start.getZ());
        for (int y = level.getMaxBuildHeight() - 1; y > level.getMinBuildHeight(); --y) {
            p.setY(y);
            net.minecraft.world.level.block.state.BlockState s = level.getBlockState(p);
            if (s.isAir() || !s.getFluidState().isEmpty()) continue;
            if (!s.isCollisionShapeFullBlock(level, p)) continue;
            net.minecraft.resources.ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(s.getBlock());
            if (id != null && (id.getPath().contains("leaves") || id.getPath().contains("bloom")
                    || id.getPath().contains("lavender") || id.getPath().endsWith("_bed"))) continue;
            p.move(0, 1, 0);
            boolean open = level.getBlockState(p).isAir();
            p.move(0, 1, 0);
            open = open && level.getBlockState(p).isAir();
            p.move(0, -2, 0);
            if (open) return p.immutable();
        }
        return null;
    }
}
