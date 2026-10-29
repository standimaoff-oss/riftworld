package ru.aiforge.riftworld.world;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Рев 14.50 — посадка деревьев Dynamic Trees СВОИМИ РУКАМИ, через API аддона.
 *
 * <p>Почему так. Своя фича DT (dynamictrees:dynamic_tree) в биоме есть (её видно по
 * {@code /riftdtwhy … has_dt=true}), вид для биома база тоже отдаёт ({@code dt_species=handled}),
 * грунт находится, почва принимается ({@code soil_ok=true}) — а деревьев в мире ноль. Мы полтора
 * десятка итераций чинили отдельные звенья этой цепочки по логам и всё равно получали {@code dt=0}:
 * у DT слишком много внутреннего состояния (кэш пуассоновских дисков, порядок фич, SafeChunkBounds
 * в нестандартном генераторе), чтобы его можно было надёжно запустить «как есть».
 *
 * <p>Поэтому делаем то, что DT умеет лучше всего — строит дерево по своим правилам (ствол, ветви,
 * корни, листва, посадочная ямка rooty-блок) — но ВЫЗЫВАЕМ его сами: в своём проходе декорации,
 * в детерминированных точках, на проверенном грунте. Результат в мире — настоящие блоки
 * {@code riftworlddt:*}, то есть аддон действительно меняет деревья, а не «лежит в jar».
 *
 * <p>Всё через рефлексию: Dynamic Trees подключён только как {@code runtimeOnly}, и мод должен
 * собираться без него.
 */
public final class DtTrees {
    private static final Logger LOG = LoggerFactory.getLogger("riftworld");
    private static final AtomicBoolean INIT = new AtomicBoolean();
    private static final AtomicBoolean LOG_ONCE = new AtomicBoolean();
    private static boolean broken;

    private static Class<?> LC_CLS, SB_CLS, GC_CLS, SP_CLS, MUT_CLS;
    private static Method LC_CREATE, SP_GET, GENERATE;
    private static java.lang.reflect.Constructor<?> GC_CTOR;
    private static Object ANY_WG;

    private DtTrees() {
    }

    private static boolean ready() {
        if (broken) return false;
        if (INIT.get()) return true;
        synchronized (INIT) {
            if (INIT.get()) return true;
            try {
                if (!ModList.get().isLoaded("dynamictrees")) {
                    broken = true;
                    return false;
                }
                LC_CLS = Class.forName("com.ferreusveritas.dynamictrees.util.LevelContext");
                SB_CLS = Class.forName("com.ferreusveritas.dynamictrees.util.SafeChunkBounds");
                GC_CLS = Class.forName("com.ferreusveritas.dynamictrees.worldgen.GenerationContext");
                SP_CLS = Class.forName("com.ferreusveritas.dynamictrees.tree.species.Species");
                MUT_CLS = Class.forName("net.minecraft.core.BlockPos$MutableBlockPos");

                LC_CREATE = LC_CLS.getMethod("create", LevelAccessor.class);
                Field anyWg = SB_CLS.getField("ANY_WG");
                ANY_WG = anyWg.get(null);
                SP_GET = SP_CLS.getField("REGISTRY").getType().getMethod("get", ResourceLocation.class);
                GENERATE = SP_CLS.getMethod("generate", GC_CLS);
                GC_CTOR = GC_CLS.getConstructor(LC_CLS, SP_CLS, BlockPos.class, MUT_CLS,
                        net.minecraft.core.Holder.class, net.minecraft.core.Direction.class, int.class, SB_CLS);
                INIT.set(true);
                LOG.info("[riftworld] посадка DT-деревьев через API аддона доступна");
                return true;
            } catch (Throwable t) {
                broken = true;
                LOG.warn("[riftworld] API Dynamic Trees недоступен ({}): деревья DT сажать не будем", t.toString());
                return false;
            }
        }
    }

    /**
     * Пробует вырастить дерево вида {@code speciesId} так, чтобы его корень был в {@code ground}.
     *
     * @return true, если DT что-то поставил в мире.
     */
    public static boolean plant(LevelAccessor level, String speciesId, BlockPos ground, int radius, int facing) {
        if (!ready()) return false;
        if (LOG_ONCE.compareAndSet(false, true)) LOG.info("[riftworld] DT Trees: первая посадка через API ({})", speciesId);
        try {
            Object reg = SP_CLS.getField("REGISTRY").get(null);
            Object species = SP_GET.invoke(reg, new ResourceLocation(speciesId));
            if (species == null) return false;
            try {
                Object valid = SP_CLS.getMethod("isValid").invoke(species);
                if (Boolean.FALSE.equals(valid)) return false;
            } catch (NoSuchMethodException ignored) {
                // у части версий DT проверки нет — просто идём дальше
            }
            BlockState dirt = level.getBlockState(ground);
            if (!(Boolean) SP_CLS.getMethod("isAcceptableSoilForWorldgen", BlockState.class).invoke(species, dirt)) {
                return false;                                  // почва не из списка вида — DT всё равно откажется
            }
            Object lc = LC_CREATE.invoke(null, level);
            net.minecraft.core.Direction[] dirs = {net.minecraft.core.Direction.NORTH,
                    net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.WEST};
            net.minecraft.core.Direction dir = dirs[facing & 3];
            Object ctx = GC_CTOR.newInstance(lc, species, ground, ground.mutable(), level.getBiome(ground),
                    dir, radius, ANY_WG);
            return (Boolean) GENERATE.invoke(species, ctx);
        } catch (Throwable t) {
            if (broken) return false;
            broken = true;
            LOG.warn("[riftworld] посадка DT-дерева не удалась ({}): больше не пробуем", t.toString());
            return false;
        }
    }
}
