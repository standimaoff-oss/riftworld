package ru.aiforge.riftworld.world;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import ru.aiforge.riftworld.entity.ElisarArcherEntity;
import ru.aiforge.riftworld.entity.ElisarGuardEntity;
import ru.aiforge.riftworld.entity.ElisarVillagerEntity;

/**
 * v0.49.0 — «Закон Элисара»: наблюдатели преступлений.
 *
 * <p>Кто-то должен <b>увидеть</b> преступление, чтобы оно стало известным
 * городу. Наблюдателями считаются:</p>
 * <ul>
 *   <li>стражи и лучники Элисара в радиусе 32 блоков;</li>
 *   <li>жители Элисара в радиусе 24 блоков;</li>
 *   <li>другие игроки в радиусе 32 блоков.</li>
 * </ul>
 *
 * <p>Если наблюдателей не оказалось — город ничего не знает, розыска не
 * будет. Это делает последствия видимыми и справедливыми: преступление на
 * глазах у жителей — всегда найдёт отклик.</p>
 */
public final class ElisarWitness {

    private ElisarWitness() {
    }

    /** Радиус, в котором страж замечает преступление. */
    public static final double GUARD_RADIUS = 32.0;
    /** Радиус, в котором житель замечает преступление. */
    public static final double VILLAGER_RADIUS = 24.0;
    /** Радиус, в котором игрок замечает преступление. */
    public static final double PLAYER_RADIUS = 32.0;

    /**
     * Посчитать наблюдателей преступления.
     *
     * @return число очевидцев (0 — никто не видел)
     */
    public static int countWitnesses(ServerLevel level, Entity victim, Entity criminal) {
        if (criminal == null) {
            return 0;
        }
        int count = 0;

        AABB guardBox = criminal.getBoundingBox().inflate(GUARD_RADIUS);
        for (ElisarGuardEntity guard : level.getEntitiesOfClass(ElisarGuardEntity.class, guardBox)) {
            if (guard == criminal || !guard.isAlive()) {
                continue;
            }
            if (canSee(guard, criminal)) {
                count++;
            }
        }
        for (ElisarArcherEntity archer : level.getEntitiesOfClass(ElisarArcherEntity.class, guardBox)) {
            if (archer == criminal || !archer.isAlive()) {
                continue;
            }
            if (canSee(archer, criminal)) {
                count++;
            }
        }

        AABB villagerBox = criminal.getBoundingBox().inflate(VILLAGER_RADIUS);
        for (ElisarVillagerEntity villager : level.getEntitiesOfClass(ElisarVillagerEntity.class, villagerBox)) {
            if (villager == criminal || !villager.isAlive()) {
                continue;
            }
            if (canSee(villager, criminal)) {
                count++;
            }
        }

        AABB playerBox = criminal.getBoundingBox().inflate(PLAYER_RADIUS);
        for (Player player : level.players()) {
            if (player == criminal || !(player instanceof ServerPlayer)) {
                continue;
            }
            if (!player.getBoundingBox().intersects(playerBox)) {
                continue;
            }
            if (canSee(player, criminal)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Насколько «видимо» преступление наблюдателю: не за стеной ли он.
     * Используем простую трассировку луча: если между наблюдателем и
     * преступником есть твёрдый блок — преступление не видели.
     */
    public static boolean canSee(LivingEntity observer, Entity criminal) {
        if (observer == criminal) {
            return false;
        }
        Vec3 eye = observer.getEyePosition();
        Vec3 target = criminal.position();
        // Страж в 3 блоках от преступника почти наверняка его видит.
        if (observer.distanceToSqr(criminal) <= 9.0) {
            return true;
        }
        Vec3 dir = target.subtract(eye);
        double len = dir.length();
        if (len < 1.0E-4) {
            return true;
        }
        dir = dir.normalize();

        // Шаг 0.4 блока — грубый, но достаточный луч.
        double step = 0.4;
        for (double d = 0.8; d < len - 0.6; d += step) {
            BlockPos p = BlockPos.containing(eye.x + dir.x * d, eye.y + dir.y * d, eye.z + dir.z * d);
            var state = observer.level().getBlockState(p);
            if (state.isAir() || state.getCollisionShape(observer.level(), p).isEmpty()) {
                continue;
            }
            if (state.is(SIGHT_THROUGH)) {
                continue;
            }
            return false;
        }
        return true;
    }

    /** Блоки, сквозь которые преступление всё равно «видно». */
    private static final TagKey<Block> SIGHT_THROUGH =
            TagKey.create(Registries.BLOCK, new ResourceLocation("minecraft", "impermeable"));

    /**
     * Найти ближайшего живого свидетеля, который заметил преступление.
     */
    @Nullable
    public static LivingEntity findWitness(ServerLevel level, Entity criminal) {
        AABB box = criminal.getBoundingBox().inflate(GUARD_RADIUS);
        for (ElisarGuardEntity guard : level.getEntitiesOfClass(ElisarGuardEntity.class, box)) {
            if (guard.isAlive() && canSee(guard, criminal)) {
                return guard;
            }
        }
        for (ElisarVillagerEntity villager : level.getEntitiesOfClass(ElisarVillagerEntity.class, box)) {
            if (villager.isAlive() && canSee(villager, criminal)) {
                return villager;
            }
        }
        return null;
    }
}
