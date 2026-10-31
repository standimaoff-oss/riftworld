package ru.aiforge.riftworld.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.flight.Flight;
import ru.aiforge.riftworld.item.CrystalBarrageItem;
import ru.aiforge.riftworld.registry.ModEntities;
import ru.aiforge.riftworld.world.CrystalRifts;

/**
 * Серверный обработчик боевых механик «Хрустальных разломов»:
 * <ol>
 *   <li>Поочерёдный пуск построенных кристаллов «Веера Осколков» ({@code crystal_barrage}) строго туда,
 *       куда в момент пуска каждого кристалла смотрит прицел игрока.</li>
 *   <li>Фронтальное поглощение урона защитным 6-секундным щитом «Бастион Граней» ({@code crystal_aegis}).</li>
 *   <li>Пульсирующий маяк резонанса от трофея босса «Призма Архонта Разлома» ({@code astral_prism}).</li>
 *   <li>Однократный спавн Босса Колоннады и его Прислужников на алтаре каждой Колоннады в {@link CrystalRifts}.</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID)
public final class CrystalCombatHandler {
    private static final class QueuedBarrage {
        final UUID playerId;
        final int totalBuilt;
        int nextIndex;
        int delayTicks;

        QueuedBarrage(UUID playerId, int totalBuilt) {
            this.playerId = playerId;
            this.totalBuilt = totalBuilt;
            this.nextIndex = 0;
            this.delayTicks = 0;
        }
    }

    private static final class PrismBeacon {
        final ServerLevel level;
        final UUID ownerId;
        final Vec3 pos;
        int ticksLeft = 100; // 5 секунд

        PrismBeacon(ServerLevel level, UUID ownerId, Vec3 pos) {
            this.level = level;
            this.ownerId = ownerId;
            this.pos = pos;
        }
    }

    /** SavedData, чтобы каждая Колоннада в мире спавнила своего Босса ровно один раз. */
    private static final class ColonnadeSpawnData extends SavedData {
        final Set<Long> spawnedColonnades = new HashSet<>();

        static ColonnadeSpawnData load(CompoundTag tag) {
            ColonnadeSpawnData d = new ColonnadeSpawnData();
            ListTag list = tag.getList("Spawned", Tag.TAG_LONG);
            for (int i = 0; i < list.size(); ++i) {
                d.spawnedColonnades.add(((LongTag) list.get(i)).getAsLong());
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag list = new ListTag();
            for (Long k : spawnedColonnades) {
                list.add(LongTag.valueOf(k));
            }
            tag.put("Spawned", list);
            return tag;
        }
    }

    private static final List<QueuedBarrage> BARRAGES = new ArrayList<>();
    private static final List<PrismBeacon> BEACONS = new ArrayList<>();

    private CrystalCombatHandler() {
    }

    public static void enqueueBarrage(ServerPlayer player, int count) {
        BARRAGES.add(new QueuedBarrage(player.getUUID(), count));
    }

    public static void spawnPrismBeacon(ServerLevel level, UUID ownerId, Vec3 pos) {
        BEACONS.add(new PrismBeacon(level, ownerId, pos));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.player.level().isClientSide) return;
        if (!(e.player instanceof ServerPlayer sp)) return;

        // 1) Поочерёдный пуск построенных кристаллов (1..5) один за другим по живому прицелу игрока!
        Iterator<QueuedBarrage> it = BARRAGES.iterator();
        while (it.hasNext()) {
            QueuedBarrage q = it.next();
            if (!q.playerId.equals(sp.getUUID())) continue;
            if (!sp.isAlive()) {
                it.remove();
                continue;
            }
            if (q.delayTicks > 0) {
                --q.delayTicks;
                continue;
            }

            ServerLevel sl = sp.serverLevel();
            Vec3 slotPos = CrystalBarrageItem.slotWorldPos(sp, q.nextIndex);
            // Точка, куда СЕЙЧАС смотрит перекрестие прицела на дистанции 32 блоков
            Vec3 aimPoint = sp.getEyePosition().add(sp.getLookAngle().normalize().scale(32.0));
            Vec3 dir = aimPoint.subtract(slotPos).normalize();

            CrystalShardEntity shard = new CrystalShardEntity(ModEntities.CRYSTAL_SHARD.get(), sl, sp, 7.0F);
            shard.setPos(slotPos.x, slotPos.y, slotPos.z);
            shard.setDeltaMovement(dir.scale(1.75));
            sl.addFreshEntity(shard);

            sl.playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS,
                    0.95F, 1.15F + q.nextIndex * 0.12F);
            sl.sendParticles(ParticleTypes.END_ROD, slotPos.x, slotPos.y, slotPos.z, 4, 0.04, 0.04, 0.04, 0.02);

            ++q.nextIndex;
            q.delayTicks = 4; // каждые 0.2 с вылетает следующий кристалл очереди!
            if (q.nextIndex >= q.totalBuilt) {
                it.remove();
            }
        }

        // 2) Автоматический спавн Босса и 2 Прислужников на алтаре Колоннады, когда игрок подходит к ней в Элисаре
        if (sp.tickCount % 40 == 0 && sp.level().dimension().equals(Flight.ELISAR_DIM)) {
            checkColonnadeBossSpawn(sp.serverLevel(), sp);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.level.isClientSide || !(e.level instanceof ServerLevel sl)) return;
        Iterator<PrismBeacon> it = BEACONS.iterator();
        while (it.hasNext()) {
            PrismBeacon b = it.next();
            if (b.level != sl) continue;
            --b.ticksLeft;
            if ((b.ticksLeft % 5) == 0) {
                double a = b.ticksLeft * 0.35;
                sl.sendParticles(ParticleTypes.GLOW,
                        b.pos.x + Math.cos(a) * 1.4, b.pos.y + 0.4, b.pos.z + Math.sin(a) * 1.4,
                        2, 0.05, 0.1, 0.05, 0.01);
            }
            if (b.ticksLeft % 20 == 0) {
                sl.playSound(null, BlockPos.containing(b.pos), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.85F, 1.3F);
                sl.sendParticles(ParticleTypes.END_ROD, b.pos.x, b.pos.y + 0.3, b.pos.z, 12, 1.2, 0.3, 1.2, 0.03);
                Player owner = sl.getPlayerByUUID(b.ownerId);
                AABB area = new AABB(b.pos.x - 4.5, b.pos.y - 2.0, b.pos.z - 4.5, b.pos.x + 4.5, b.pos.y + 3.0, b.pos.z + 4.5);
                for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, area, e2 -> e2.isAlive())) {
                    if (le.getUUID().equals(b.ownerId) || le instanceof Player) continue;
                    le.hurt(owner != null ? sl.damageSources().indirectMagic(owner, owner) : sl.damageSources().magic(), 3.0F);
                    le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 35, 0));
                }
            }
            if (b.ticksLeft <= 0) {
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent e) {
        if (!(e.getEntity() instanceof Player player) || player.level().isClientSide) return;
        List<CrystalShieldEntity> shields = player.level().getEntitiesOfClass(CrystalShieldEntity.class,
                player.getBoundingBox().inflate(4.0),
                s -> s.mode() == CrystalShieldEntity.MODE_GUARD && player.getUUID().equals(s.ownerUuid()));
        if (shields.isEmpty()) return;

        CrystalShieldEntity shield = shields.get(0);
        Vec3 srcPos = e.getSource().getSourcePosition();
        if (srcPos != null) {
            Vec3 toSrc = srcPos.subtract(player.position());
            Vec3 horizSrc = new Vec3(toSrc.x, 0, toSrc.z);
            Vec3 shieldDir = Vec3.directionFromRotation(0.0F, shield.shieldYaw());
            if (horizSrc.lengthSqr() > 1.0E-4 && horizSrc.normalize().dot(shieldDir) < -0.15) {
                return; // удар в спину позади щита не блокируется
            }
        }
        shield.absorbHit(e.getAmount());
        e.setCanceled(true);
    }

    private static void checkColonnadeBossSpawn(ServerLevel sl, ServerPlayer sp) {
        CrystalRifts.Region cr = CrystalRifts.regionAt(sp.getX(), sp.getZ());
        if (cr == null) return;
        double dist = Math.hypot(sp.getX() - cr.astroX, sp.getZ() - cr.astroZ);
        if (dist > 82.0) return;
        BlockPos altar = BlockPos.containing(cr.astroX, cr.astroY + 2.0, cr.astroZ);
        if (!sl.isLoaded(altar)) return;

        ColonnadeSpawnData data = sl.getDataStorage().computeIfAbsent(
                ColonnadeSpawnData::load, ColonnadeSpawnData::new, "riftworld_colonnade_bosses");
        long key = (((long) Math.floor(cr.astroX)) << 32) ^ (((long) Math.floor(cr.astroZ)) & 0xFFFFFFFFL);
        if (!data.spawnedColonnades.add(key)) return;
        data.setDirty();

        RiftObeliskBoss boss = ModEntities.RIFT_OBELISK_BOSS.get().create(sl);
        if (boss != null) {
            double bx = cr.astroX, by = cr.astroY + 1.85, bz = cr.astroZ;
            boss.moveTo(bx, by, bz, 0.0F, 0.0F);
            boss.setAnchor(bx, by, bz);
            boss.finalizeSpawn(sl, sl.getCurrentDifficultyAt(altar), MobSpawnType.STRUCTURE, null, null);
            sl.addFreshEntity(boss);
        }
        for (int i = 0; i < 2; ++i) {
            double ang = cr.phi + i * Math.PI;
            double mx = cr.astroX + Math.cos(ang) * 4.8;
            double my = cr.astroY + 1.5;
            double mz = cr.astroZ + Math.sin(ang) * 4.8;
            CrystalMinion m = ModEntities.CRYSTAL_MINION.get().create(sl);
            if (m != null) {
                m.moveTo(mx, my, mz, (float) (i * 180.0F), 0.0F);
                m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(altar), MobSpawnType.STRUCTURE, null, null);
                sl.addFreshEntity(m);
            }
        }
    }
}
