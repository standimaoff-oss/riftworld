package ru.aiforge.riftworld.arena;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import ru.aiforge.riftworld.entity.EmberGuard;
import ru.aiforge.riftworld.entity.MorgrathBoss;
import ru.aiforge.riftworld.registry.ModEntities;

/** Seam between the arena sequence and Morgrath: spawn lying at the entry, rise when the candles are lit, fight, clean-up. */
public final class BossHooks {
    private BossHooks() {
    }

    @Nullable
    static MorgrathBoss boss(ServerLevel sl, @Nullable UUID id) {
        if (id == null) return null;
        Entity e = sl.getEntity(id);
        return e instanceof MorgrathBoss b && b.isAlive() ? b : null;
    }

    /** Entry begins (hall goes dark): the boss lies as if dead in front of the chair. Returns its UUID. */
    @Nullable
    public static UUID prepare(ServerLevel sl, ArenaCoreBlockEntity core) {
        clear(sl, core);
        MorgrathBoss b = ModEntities.BOSS.get().create(sl);
        if (b == null) return null;
        BlockPos c = core.getBlockPos();
        b.moveTo(c.getX() + 0.5, c.getY() + 2.0, c.getZ() - 2.0, 90.0F, 0.0F);
        b.setYHeadRot(90.0F);
        b.setYBodyRot(90.0F);
        b.finalizeSpawn(sl, sl.getCurrentDifficultyAt(c), MobSpawnType.EVENT, null, null);
        b.makeDormant(c);
        sl.addFreshEntity(b);
        return b.getUUID();
    }

    /** All candles lit: the body jerks and rises, then waits by the chair. */
    public static void stand(ServerLevel sl, ArenaCoreBlockEntity core, @Nullable UUID id) {
        MorgrathBoss b = boss(sl, id);
        if (b != null) b.beginRise();
    }

    /** A player came to the chair or struck the boss: roar, guards, fight. Returns the boss UUID (healed by the columns). */
    @Nullable
    public static UUID awaken(ServerLevel sl, ArenaCoreBlockEntity core, @Nullable UUID id) {
        MorgrathBoss b = boss(sl, id);
        if (b == null) {
            id = prepare(sl, core);          // e.g. /riftarena start skipped the entry
            b = boss(sl, id);
            if (b == null) return null;
        }
        b.beginFight();
        return b.getUUID();
    }

    /** Arena reset: remove the boss, the guards and anything left lying around. */
    public static void reset(ServerLevel sl, ArenaCoreBlockEntity core) {
        clear(sl, core);
    }

    static void clear(ServerLevel sl, ArenaCoreBlockEntity core) {
        AABB box = core.hallBox();
        for (MorgrathBoss b : sl.getEntitiesOfClass(MorgrathBoss.class, box)) {
            b.discardGuards(sl);
            b.discard();
        }
        for (EmberGuard g : sl.getEntitiesOfClass(EmberGuard.class, box)) g.discard();
    }
}
