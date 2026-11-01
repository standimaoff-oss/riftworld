package ru.aiforge.riftworld.flight;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.entity.SpatialRiftEntity;
import ru.aiforge.riftworld.net.Net;
import ru.aiforge.riftworld.net.PortalRitualMsg;
import ru.aiforge.riftworld.registry.ModEntities;
import ru.aiforge.riftworld.registry.ModItems;
import ru.aiforge.riftworld.registry.TempleBlocks;

/**
 * Безблочный ритуал открытия Белого Пространственного Пролома и кинематографического прибытия в Элисар:
 * <ol>
 *   <li>{@link #AWAKEN} (4 секунды = 80 тиков): всё вокруг плавно бледнеет до тёплого оттенка Элисара
 *       ({@code mutated = 0}), на земле разворачивается красивый сакрально-геометрический узор, в небе
 *       открываются 2D-глаза Элисара -> в финале яркая вспышка разрывает пространство и открывает
 *       высокий белый геометрический 3D-Пролом ({@link SpatialRiftEntity}) от земли до неба!</li>
 *   <li>{@link #TRANSIT_VEIL} (12 тиков): когда игрок ЗАХОДИТ в белый пролом, экран мягко белеет
 *       (не слишком ярко), скрывается инвентарь и весь GUI, блокируется переключение лиц (F5),
 *       и без земляного экрана загрузки игрок переносится в Белый Город Элисара.</li>
 *   <li>{@link #CITY_CUTSCENE} (124 тика): 1 дальний панорамный ракурс Белого Города с названием
 *       измерения («ЭЛИСАР») и снизу названием города («Белый Город Святилища») -> текст плавно
 *       исчезает -> камера плавно подлетает к глазам игрока -> возвращаются интерфейс и управление!</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PortalRitual {
    public static final int NONE = 0;
    public static final int AWAKEN = 1;
    public static final int TRANSIT_VEIL = 2;
    public static final int CITY_CUTSCENE = 3;
    public static final int ARRIVE_FLASH = 4;

    /** 4.0 секунды (80 тиков) постепенного побледнения до оттенка Элисара + узора перед яркой вспышкой открытия! */
    public static final int AWAKEN_TICKS = 80;
    public static final int AWAKEN_SETTLE_TICKS = 92;
    /** 0.6 секунды мягкого побеления экрана при заходе в пролом перед бесшовным переносом. */
    public static final int TRANSIT_TICKS = 12;
    /** 6.2 секунды кинематографической катсцены Белого Города (дальний ракурс + титры -> плавный подлёт к игроку). */
    public static final int CUTSCENE_TICKS = 124;
    public static final int ARRIVE_TICKS = 26;

    private static final class Session {
        final UUID playerId;
        ResourceKey<Level> dimKey;
        BlockPos portalCenter;
        int mode; // AWAKEN, TRANSIT_VEIL, CITY_CUTSCENE, ARRIVE_FLASH
        int tick;
        boolean riftSpawned;

        Session(UUID playerId, ResourceKey<Level> dimKey, BlockPos portalCenter, int mode) {
            this.playerId = playerId;
            this.dimKey = dimKey;
            this.portalCenter = portalCenter;
            this.mode = mode;
        }
    }

    private static final Map<UUID, Session> ACTIVE = new HashMap<>();
    private static int itemScanCooldown = 0;

    private PortalRitual() {
    }

    public static boolean isOfferingItem(ItemStack st) {
        if (st.isEmpty()) return false;
        return st.is(ModItems.RESONANCE_STONE.get())
                || st.is(ModItems.ELISAR_DUCAT.get())
                || st.is(Items.DIAMOND)
                || st.is(Items.EMERALD)
                || st.is(Items.AMETHYST_SHARD)
                || st.is(Items.ENDER_PEARL)
                || st.is(Items.GOLD_INGOT)
                || st.is(ItemTags.FLOWERS);
    }

    /** Проверяет, открыт ли уже белый пролом рядом с точкой. */
    public static SpatialRiftEntity findNearbyOpenRift(Level level, BlockPos center, double radius) {
        AABB box = new AABB(center).inflate(radius, 8.0, radius);
        List<SpatialRiftEntity> found = level.getEntitiesOfClass(SpatialRiftEntity.class, box, Entity::isAlive);
        return found.isEmpty() ? null : found.get(0);
    }

    private static final class Entity {
        static boolean isAlive(SpatialRiftEntity e) {
            return e != null && e.isAlive();
        }
    }

    /**
     * Запускает 4-секундный ритуал ОТКРЫТИЯ белого геометрического пролома (в конце спавнит {@link SpatialRiftEntity}).
     */
    public static boolean start(ServerPlayer sp, BlockPos origin, String reason) {
        if (sp == null || sp.level().isClientSide) return false;
        Session existing = ACTIVE.get(sp.getUUID());
        if (existing != null) return false;
        if (findNearbyOpenRift(sp.level(), origin, 5.0) != null) return false;

        ServerLevel level = sp.serverLevel();
        BlockPos center = origin.immutable();
        ACTIVE.put(sp.getUUID(), new Session(sp.getUUID(), level.dimension(), center, AWAKEN));

        level.playSound(null, center, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.15F, 0.72F);
        level.playSound(null, center, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.12F);
        Net.send(sp, new PortalRitualMsg(AWAKEN, center, 0));
        return true;
    }

    /**
     * Срабатывает, когда игрок ЗАХОДИТ ногами в открытый белый 3D-Пролом ({@link SpatialRiftEntity}).
     */
    public static boolean beginWalkInTransit(ServerPlayer sp, SpatialRiftEntity rift) {
        if (sp == null || sp.level().isClientSide) return false;
        Session existing = ACTIVE.get(sp.getUUID());
        if (existing != null && existing.mode != AWAKEN) return false;

        BlockPos center = rift != null ? rift.blockPosition() : sp.blockPosition();
        Session ses = new Session(sp.getUUID(), sp.serverLevel().dimension(), center, TRANSIT_VEIL);
        ACTIVE.put(sp.getUUID(), ses);

        sp.serverLevel().playSound(null, center, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.1F, 0.88F);
        Net.send(sp, new PortalRitualMsg(TRANSIT_VEIL, center, 0));
        return true;
    }

    /** Спавнит открытый белый 3D-геометрический Пролом в точке {@code center}. */
    public static SpatialRiftEntity spawnOpenBreach(ServerLevel level, BlockPos center) {
        SpatialRiftEntity existing = findNearbyOpenRift(level, center, 4.5);
        if (existing != null) return existing;
        SpatialRiftEntity rift = ModEntities.SPATIAL_RIFT.get().create(level);
        if (rift == null) return null;
        rift.moveTo(center.getX() + 0.5, center.getY() + 0.02, center.getZ() + 0.5, 0.0F, 0.0F);
        level.addFreshEntity(rift);
        return rift;
    }

    /** Активация древней печати храма (MOSAIC_CORE / GOLD_RUNE) подношением предмета в руке. */
    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel().isClientSide || e.getHand() != InteractionHand.MAIN_HAND) return;
        ItemStack held = e.getItemStack();
        if (!isOfferingItem(held)) return;
        BlockState st = e.getLevel().getBlockState(e.getPos());
        if (!st.is(TempleBlocks.MOSAIC_CORE.get()) && !st.is(TempleBlocks.GOLD_RUNE.get())) return;
        if (e.getEntity() instanceof ServerPlayer sp) {
            BlockPos riftPos = e.getPos().above();
            if (start(sp, riftPos, "altar_offer")) {
                if (!sp.getAbilities().instabuild) {
                    held.shrink(1);
                }
                e.setCancellationResult(InteractionResult.CONSUME);
                e.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        MinecraftServer srv = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;

        // Проверка брошенного на алтарную печать MOSAIC_CORE предмета-подношения (раз в 10 тиков)
        if (++itemScanCooldown >= 10) {
            itemScanCooldown = 0;
            for (ServerPlayer sp : srv.getPlayerList().getPlayers()) {
                if (ACTIVE.containsKey(sp.getUUID())) continue;
                ServerLevel lvl = sp.serverLevel();
                List<ItemEntity> drops = lvl.getEntitiesOfClass(ItemEntity.class, sp.getBoundingBox().inflate(10.0),
                        it -> it.isAlive() && it.getAge() >= 8 && isOfferingItem(it.getItem()));
                for (ItemEntity drop : drops) {
                    BlockPos bp = drop.blockPosition();
                    BlockPos below = bp.below();
                    if (lvl.getBlockState(below).is(TempleBlocks.MOSAIC_CORE.get())
                            || lvl.getBlockState(below).is(TempleBlocks.GOLD_RUNE.get())) {
                        drop.getItem().shrink(1);
                        if (drop.getItem().isEmpty()) drop.discard();
                        start(sp, bp, "thrown_offer");
                        break;
                    }
                }
            }
        }

        if (ACTIVE.isEmpty()) return;
        Iterator<Map.Entry<UUID, Session>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Session s = it.next().getValue();
            ServerPlayer sp = srv.getPlayerList().getPlayer(s.playerId);
            if (sp == null || !sp.isAlive()) {
                it.remove();
                continue;
            }
            ++s.tick;

            if (s.mode == AWAKEN) {
                if (sp.level().dimension() != s.dimKey) {
                    Net.send(sp, new PortalRitualMsg(NONE, BlockPos.ZERO, 0));
                    it.remove();
                    continue;
                }
                ServerLevel lvl = sp.serverLevel();
                double cx = s.portalCenter.getX() + 0.5;
                double cy = s.portalCenter.getY() + 0.2;
                double cz = s.portalCenter.getZ() + 0.5;

                if (s.tick % 4 == 0 && s.tick <= AWAKEN_TICKS) {
                    double ringR = 0.6 + (s.tick / (double) AWAKEN_TICKS) * 12.0;
                    for (int k = 0; k < 10; ++k) {
                        double a = (k / 10.0) * Math.PI * 2.0 + s.tick * 0.11;
                        lvl.sendParticles(ParticleTypes.END_ROD,
                                cx + Math.cos(a) * ringR, cy + 0.15 + (k % 3) * 0.35, cz + Math.sin(a) * ringR,
                                1, 0.03, 0.08, 0.03, 0.01);
                    }
                }
                if (s.tick == 28 || s.tick == 54) {
                    lvl.playSound(null, s.portalCenter, SoundEvents.AMETHYST_BLOCK_CHIME,
                            SoundSource.BLOCKS, 1.1F, 0.72F + s.tick * 0.005F);
                }
                if (s.tick == 72) {
                    lvl.playSound(null, s.portalCenter, SoundEvents.BEACON_POWER_SELECT,
                            SoundSource.BLOCKS, 1.35F, 1.18F);
                }
                // На пике яркой вспышки (t = 78) пространство разрывается и появляется белый 3D-Пролом!
                if (!s.riftSpawned && s.tick >= AWAKEN_TICKS - 2) {
                    s.riftSpawned = true;
                    spawnOpenBreach(lvl, s.portalCenter);
                    lvl.sendParticles(ParticleTypes.FLASH, cx, cy + 2.0, cz, 2, 0.0, 0.0, 0.0, 0.0);
                    lvl.sendParticles(ParticleTypes.END_ROD, cx, cy + 3.0, cz, 45, 0.8, 3.2, 0.8, 0.08);
                    lvl.playSound(null, s.portalCenter, SoundEvents.END_PORTAL_SPAWN,
                            SoundSource.BLOCKS, 0.85F, 1.32F);
                }
                if (s.tick % 20 == 0 && s.tick < AWAKEN_SETTLE_TICKS) {
                    Net.send(sp, new PortalRitualMsg(AWAKEN, s.portalCenter, s.tick));
                }
                if (s.tick >= AWAKEN_SETTLE_TICKS) {
                    Net.send(sp, new PortalRitualMsg(NONE, BlockPos.ZERO, 0));
                    it.remove();
                }

            } else if (s.mode == TRANSIT_VEIL) {
                if (s.tick >= TRANSIT_TICKS) {
                    boolean intoElisar = sp.level().dimension() != Flight.ELISAR_DIM;
                    BlockPos arrival = performDimensionalTransfer(sp);
                    s.dimKey = sp.level().dimension();
                    s.portalCenter = arrival;
                    s.tick = 0;
                    if (intoElisar) {
                        s.mode = CITY_CUTSCENE;
                        prewarmCityCutsceneChunks(sp.serverLevel(), arrival);
                        Net.send(sp, new PortalRitualMsg(CITY_CUTSCENE, arrival, 0));
                    } else {
                        s.mode = ARRIVE_FLASH;
                        Net.send(sp, new PortalRitualMsg(ARRIVE_FLASH, arrival, 0));
                    }
                }

            } else if (s.mode == CITY_CUTSCENE) {
                // Во время катсцены мягко удерживаем игрока на точке прибытия без урона и смещения
                sp.setDeltaMovement(0.0, 0.0, 0.0);
                sp.fallDistance = 0.0F;
                if (s.tick % 25 == 0 && s.tick < CUTSCENE_TICKS) {
                    Net.send(sp, new PortalRitualMsg(CITY_CUTSCENE, s.portalCenter, s.tick));
                }
                if (s.tick >= CUTSCENE_TICKS) {
                    Net.send(sp, new PortalRitualMsg(NONE, BlockPos.ZERO, 0));
                    it.remove();
                }

            } else if (s.mode == ARRIVE_FLASH) {
                if (s.tick >= ARRIVE_TICKS) {
                    Net.send(sp, new PortalRitualMsg(NONE, BlockPos.ZERO, 0));
                    it.remove();
                }
            }
        }
    }

    private static void prewarmCityCutsceneChunks(ServerLevel dest, BlockPos arrival) {
        int baseCx = arrival.getX() >> 4;
        int baseCz = arrival.getZ() >> 4;
        for (int dx = -3; dx <= 3; ++dx) {
            for (int dz = -5; dz <= 6; ++dz) {
                ChunkPos cp = new ChunkPos(baseCx + dx, baseCz + dz);
                dest.getChunk(cp.x, cp.z);
            }
        }
    }

    private static BlockPos performDimensionalTransfer(ServerPlayer sp) {
        MinecraftServer srv = sp.getServer();
        if (srv == null) return sp.blockPosition();
        boolean inElisar = sp.level().dimension() == Flight.ELISAR_DIM;
        ServerLevel dest = srv.getLevel(inElisar ? Level.OVERWORLD : Flight.ELISAR_DIM);
        if (dest == null) return sp.blockPosition();

        int tx = inElisar ? sp.getBlockX() : 0;
        int tz = inElisar ? sp.getBlockZ() : 22;
        dest.getChunk(tx >> 4, tz >> 4);
        int ty = dest.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, tx, tz);
        if (ty <= dest.getMinBuildHeight() + 2) ty = inElisar ? 72 : 128;

        sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 180, 0, false, false));
        sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 4, false, false));
        // Направляем взгляд игрока строго на север (yaw = 180°), прямо на проспект и Замок Белого Города!
        float arriveYaw = inElisar ? sp.getYRot() : 180.0F;
        float arrivePitch = inElisar ? 0.0F : -5.0F;
        sp.teleportTo(dest, tx + 0.5, ty + 0.2, tz + 0.5, arriveYaw, arrivePitch);
        dest.playSound(null, BlockPos.containing(tx + 0.5, ty + 1.0, tz + 0.5),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.95F, 1.15F);
        return new BlockPos(tx, ty, tz);
    }
}
