package riftcheck;

import net.minecraftforge.fml.ModList;
import ru.aiforge.riftworld.compat.DynamicTreesCompat;
import ru.aiforge.riftworld.tree.TreeGen;
import ru.aiforge.riftworld.tree.TreeSpecies;

/**
 * Checks the Dynamic Trees soft dependency — the one rule that decides who owns the trees of Elisar.
 *
 * <p>The mod must work in three situations and this test walks through all of them without Minecraft: DT absent
 * (our engine grows the trees), DT installed (DT grows them, our engine must produce exactly nothing), and no Forge
 * mod list at all (data generation, unit tests) where the safe answer is "DT absent".</p>
 *
 * <p>It also pins the caching contract: the mod-list lookup happens once per state, because it is called from the
 * chunk generator on every chunk.</p>
 */
public final class DtCompatCheck {

    private static int checks;

    public static void main(String[] args) {
        // 1. no Forge at all: must not throw, must fall back to our own engine
        DynamicTreesCompat.force(null);
        ModList.explode = true;
        check("без Forge: DT считается отсутствующей", !DynamicTreesCompat.loaded());
        check("без Forge: наш генератор работает (фолбэк)", DynamicTreesCompat.ownTreesEnabled());
        ModList.explode = false;

        // 2. DT absent
        DynamicTreesCompat.force(null);
        ModList.LOADED.clear();
        check("DT не установлена: loaded()==false", !DynamicTreesCompat.loaded());
        check("DT не установлена: деревья растут нашим движком", DynamicTreesCompat.ownTreesEnabled()
                && nodesUnderOwnership() > 0);

        // 3. DT installed: our engine must go silent
        ModList.LOADED.put(DynamicTreesCompat.MOD_ID, true);
        DynamicTreesCompat.force(null);
        check("DT установлена: loaded()==true", DynamicTreesCompat.loaded());
        check("DT установлена: наш движок не ставит ни одного блока", !DynamicTreesCompat.ownTreesEnabled()
                && nodesUnderOwnership() == 0);

        // 4. the answer is cached: a mod list changed after the fact must not flip it mid-world
        ModList.LOADED.put(DynamicTreesCompat.MOD_ID, false);
        check("ответ кэшируется (двойной посадки в одном мире быть не может)", DynamicTreesCompat.loaded());

        // 5. test hook / forced fallback: the built-in engine can be run even with DT installed
        DynamicTreesCompat.force(false);
        check("force(false) включает наш движок при установленной DT", DynamicTreesCompat.ownTreesEnabled());
        DynamicTreesCompat.force(true);
        check("force(true) глушит наш движок без DT", !DynamicTreesCompat.ownTreesEnabled());

        // 6. reset returns to the real mod list
        ModList.LOADED.put(DynamicTreesCompat.MOD_ID, true);
        DynamicTreesCompat.force(null);
        check("force(null) возвращает реальную мод-листу", DynamicTreesCompat.loaded()
                && !DynamicTreesCompat.ownTreesEnabled());
        ModList.LOADED.put(DynamicTreesCompat.MOD_ID, false);
        DynamicTreesCompat.force(null);
        check("force(null) сбрасывает и кэш", !DynamicTreesCompat.loaded());

        System.out.println("DtCompatCheck: OK — " + checks + " проверок: DT есть/нет/Forge нет, кэш, фолбэк");
    }

    /** Exactly what the chunk generator does when it reaches the last block of a chunk. */
    private static int nodesUnderOwnership() {
        if (!DynamicTreesCompat.ownTreesEnabled()) {
            return 0;      // the generator skips the whole call: not a single tree block reaches the chunk
        }
        int[] count = {0};
        TreeGen.build(TreeSpecies.SIZO, 10, 424242, (dx, dy, dz, kind, axis) -> count[0]++);
        return count[0];
    }

    private static void check(String what, boolean ok) {
        checks++;
        if (!ok) {
            System.out.println("ПРОВАЛ: " + what);
            System.exit(1);
        }
    }

    private DtCompatCheck() {
    }
}
