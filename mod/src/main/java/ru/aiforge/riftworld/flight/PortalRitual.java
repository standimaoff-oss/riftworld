package ru.aiforge.riftworld.flight;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.net.Net;
import ru.aiforge.riftworld.net.PortalRitualMsg;
import ru.aiforge.riftworld.registry.ModItems;
import ru.aiforge.riftworld.registry.TempleBlocks;
import ru.aiforge.riftworld.world.CityPlan;

/**
 * v0.20.0: Кастомный бесшовный переход через портал в Элисар БЕЗ загрузочного экрана!
 *
 * <ol>
 *   <li>Активация действием у портала: поднести кристалл/предмет, нажать ПКМ или расколоть багровую печать ЛКМ;</li>
 *   <li>Игрок остаётся в обычном мире, пока чанки точки прибытия в {@code riftworld:elisar} фоном подгружаются тикетами;</li>
 *   <li>От центра портала мир плавно белеет в тон слоновой кости города Элисара (чисто визуально на клиенте —
 *       <b>в коде не меняется ни один блок!</b>);</li>
 *   <li>В воздухе вокруг портала и игрока открывается множество парящих глаз, неотрывно смотрящих на игрока;</li>
 *   <li>Резкая ослепительно-белая вспышка — и мгновенное появление на беломраморной площади Элисара без экрана земли!</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID)
public final class PortalRitual {
    public static final int NONE = 0;
    public static final int AWAKEN = 1;
    public static final int ARRIVE_FLASH = 2;

    /** Длительность фазы пробуждения в обычном мире (6.4 с): выбеливание + парящие глаза + резкая вспышка. */
    public static final int AWAKEN_TICKS = 128;
    /** Короткое затухание белой вспышки уже в загруженном Элисаре (0.9 с). */
    public static final int ARRIVE_TICKS = 18;

    private static final class State {
        final BlockPos portalCenter;
        final int destApronX, destApronZ;
        final List<ChunkPos> preloadQueue = new ArrayList<>(49);
        int preloadIdx = 0;
        BlockPos resolvedTarget;
        int phase = AWAKEN;
        int tick = 0;

        State(BlockPos portalCenter, int destApronX, int destApronZ) {
            this.portalCenter = portalCenter;
            this.destApronX = destApronX;
            this.destApronZ = destApronZ;
            int ccx = destApronX >> 4, ccz = destApronZ >> 4;
            for (int r = 0; r <= 3; ++r) {
                for (int dx = -r; dx <= r; ++dx) {
                    for (int dz = -r; dz <= r; ++dz) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                        preloadQueue.add(new ChunkPos(ccx + dx, ccz + dz));
                    }
                }
            }
        }
    }

    private static final Map<UUID, State> ACTIVE = new HashMap<>();

    private PortalRitual() {
    }

    public static boolean isActive(UUID id) {
        return ACTIVE.containsKey(id);
    }

    /** Проверяет, является ли предмет подходящим даром для пробуждения портала. */
    public static boolean isOfferingItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(Items.AMETHYST_SHARD)
                || stack.is(Items.QUARTZ)
                || stack.is(Items.ENDER_PEARL)
                || stack.is(Items.DIAMOND)
                || stack.is(Items.EMERALD)
                || stack.is(Items.GOLD_INGOT)
                || stack.is(Items.LAPIS_LAZULI)
                || stack.is(Items.PRISMARINE_CRYSTALS)
                || stack.is(Items.GLOWSTONE_DUST)
                || stack.is(ModItems.ASTRAL_PRISM.get())
                || stack.is(ModItems.CRYSTAL_KATAR.get())
                || stack.is(ModItems.CRYSTAL_BARRAGE.get())
                || stack.is(ModItems.CRYSTAL_AEGIS.get());
    }

    /** Находит геометрический центр полотна портала {@code demon_gate} рядом с указанной точкой. */
    public static BlockPos findGateCenter(Level level, BlockPos near) {
        int sx = 0, sy = 0, sz = 0, cnt = 0;
        for (BlockPos p : BlockPos.betweenClosed(near.offset(-4, -4, -4), near.offset(4, 4, 4))) {
            if (level.getBlockState(p).is(TempleBlocks.DEMON_GATE.get())) {
                sx += p.getX();
                sy += p.getY();
                sz += p.getZ();
                ++cnt;
            }
        }
        return cnt > 0 ? new BlockPos(Math.round((float) sx / cnt), Math.round((float) sy / cnt), Math.round((float) sz / cnt)) : near.immutable();
    }

    /**
     * Запускает кастомный переход в Элисар (вызывается при нажатии/подношении/ударе по порталу или из пробы).
     * Блоки мира НЕ изменяются!
     */
    public static boolean start(ServerPlayer sp, BlockPos rawOrigin, String triggerKind) {
        if (sp == null || !sp.isAlive() || Flight.locked(sp) || ACTIVE.containsKey(sp.getUUID())) {
            return false;
        }
        if (!(sp.level() instanceof ServerLevel sl)) return false;
        ServerLevel elisar = sl.getServer().getLevel(Flight.ELISAR_DIM);
        if (elisar == null || sl.dimension() == Flight.ELISAR_DIM) {
            return false;
        }

        BlockPos center = findGateCenter(sl, rawOrigin);
        int[] apron = computeTempleApronXZ();
        State st = new State(center, apron[0], apron[1]);
        ACTIVE.put(sp.getUUID(), st);

        // Немедленно вешаем билет предзагрузки на чанки Элисара, пока игрок остаётся в Обычном мире!
        ChunkPos destChunk = new ChunkPos(apron[0] >> 4, apron[1] >> 4);
        elisar.getChunkSource().addRegionTicket(TicketType.PORTAL, destChunk, 4, new BlockPos(apron[0], 90, apron[1]));

        // Звуковой и визуальный отклик пробуждения печати (без изменения блоков!)
        if ("offer".equals(triggerKind)) {
            sl.playSound(null, center, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.4F, 1.15F);
            sl.playSound(null, center, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.1F, 1.25F);
            sp.displayClientMessage(Component.literal("\u0414\u0430\u0440 \u043f\u0440\u0438\u043d\u044f\u0442 \u0420\u0430\u0437\u043b\u043e\u043c\u043e\u043c \u2014 \u043c\u0438\u0440 \u0440\u0430\u0441\u0442\u0432\u043e\u0440\u044f\u0435\u0442\u0441\u044f \u0432 \u0431\u0435\u043b\u0438\u0437\u043d\u0435..."), true);
        } else if ("break".equals(triggerKind)) {
            sl.playSound(null, center, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1.4F, 0.85F);
            sl.playSound(null, center, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 0.75F);
            sp.displayClientMessage(Component.literal("\u041f\u0435\u0447\u0430\u0442\u044c \u0432\u0440\u0430\u0442 \u0440\u0430\u0441\u043a\u043e\u043b\u043e\u0442\u0430 \u2014 \u042d\u043b\u0438\u0441\u0430\u0440 \u0441\u043c\u043e\u0442\u0440\u0438\u0442 \u0441\u043a\u0432\u043e\u0437\u044c \u0420\u0430\u0437\u043b\u043e\u043c..."), true);
        } else {
            sl.playSound(null, center, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.2F, 1.25F);
            sl.playSound(null, center, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.3F, 0.95F);
            sp.displayClientMessage(Component.literal("\u0412\u0440\u0430\u0442\u0430 \u043f\u0440\u043e\u0431\u0443\u0436\u0434\u0435\u043d\u044b \u2014 \u0441\u0432\u0435\u0442 \u042d\u043b\u0438\u0441\u0430\u0440\u0430 \u0437\u0430\u0442\u043e\u043f\u043b\u044f\u0435\u0442 \u043c\u0438\u0440..."), true);
        }
        sl.sendParticles(ParticleTypes.END_ROD, center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                36, 0.85, 1.15, 0.85, 0.06);

        Net.send(sp, new PortalRitualMsg(AWAKEN, center, 0));
        return true;
    }

    private static int[] computeTempleApronXZ() {
        int fx = 0, fz = 0;
        for (CityPlan.Plot pl : CityPlan.get().plots) {
            if (!pl.template().equals("temple_great") && !pl.template().equals("city_castle")) continue;
            fx = pl.x0() + pl.w() / 2;
            fz = pl.z0() + pl.d() - 3;
            break;
        }
        return new int[]{fx, fz};
    }

    /** Позволяет активировать Разлом нажатием ПКМ по рунным камням рядом или прямо в ствол 3D-Разлома на уровне глаз! */
    @SubscribeEvent
    public static void onRightClickFrame(PlayerInteractEvent.RightClickBlock e) {
        Level level = e.getLevel();
        BlockPos pos = e.getPos();
        BlockState st = level.getBlockState(pos);
        if (level.dimension() == Flight.ELISAR_DIM) return;
        if (!st.is(TempleBlocks.DEMON_FRAME.get()) && !st.is(TempleBlocks.GOLD_RUNE.get())) {
            // Если игрок кликнул по земле прямо у подножия 3D-Разлома с кристаллом/даром в руке
            if (!isOfferingItem(e.getItemStack())) return;
        }

        // Проверяем, что рядом действительно есть якорь Разлома demon_gate
        BlockPos gateAnchor = null;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-3, -2, -3), pos.offset(3, 3, 3))) {
            if (level.getBlockState(p).is(TempleBlocks.DEMON_GATE.get())) {
                gateAnchor = p.immutable();
                break;
            }
        }
        if (gateAnchor == null) return;

        if (level.isClientSide) {
            e.setCancellationResult(InteractionResult.SUCCESS);
            e.setCanceled(true);
            return;
        }
        if (e.getEntity() instanceof ServerPlayer sp) {
            ItemStack held = sp.getItemInHand(e.getHand());
            boolean offered = isOfferingItem(held);
            if (start(sp, gateAnchor, offered ? "offer" : "press")) {
                if (offered && !sp.getAbilities().instabuild) {
                    held.shrink(1);
                }
            }
            e.setCancellationResult(InteractionResult.CONSUME);
            e.setCanceled(true);
        }
    }

    /** Активация ПКМ с предметом в воздухе при взгляде на узкий наземный ствол 3D-Прорыва Пространства (y .. y+4.5). */
    @SubscribeEvent
    public static void onRightClickItemTowardsRift(PlayerInteractEvent.RightClickItem e) {
        Level level = e.getLevel();
        if (level.dimension() == Flight.ELISAR_DIM) return;
        var player = e.getEntity();
        BlockPos p0 = player.blockPosition();
        BlockPos gateAnchor = null;
        for (BlockPos p : BlockPos.betweenClosed(p0.offset(-5, -3, -5), p0.offset(5, 3, 5))) {
            if (level.getBlockState(p).is(TempleBlocks.DEMON_GATE.get())) {
                // Проверяем, что взгляд игрока направлен в сторону вертикальной оси Разлома
                var eye = player.getEyePosition();
                var look = player.getLookAngle();
                double rx = (p.getX() + 0.5) - eye.x;
                double rz = (p.getZ() + 0.5) - eye.z;
                double horizDist = Math.hypot(rx, rz);
                if (horizDist <= 5.5) {
                    double dot = (rx * look.x + rz * look.z) / Math.max(0.001, Math.hypot(rx, rz) * Math.hypot(look.x, look.z));
                    if (dot > 0.72) {
                        gateAnchor = p.immutable();
                        break;
                    }
                }
            }
        }
        if (gateAnchor == null) return;
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            ItemStack held = sp.getItemInHand(e.getHand());
            boolean offered = isOfferingItem(held);
            if (start(sp, gateAnchor, offered ? "offer" : "press")) {
                if (offered && !sp.getAbilities().instabuild) {
                    held.shrink(1);
                }
            }
        }
        e.setCancellationResult(InteractionResult.SUCCESS);
        e.setCanceled(true);
    }

    /** Защита игрока от случайного урона во время ритуала перехода. */
    @SubscribeEvent
    public static void onHurt(LivingAttackEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp && ACTIVE.containsKey(sp.getUUID())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            ACTIVE.remove(sp.getUUID());
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) return;
        MinecraftServer server = e.getServer();
        ServerLevel elisar = server.getLevel(Flight.ELISAR_DIM);

        Iterator<Map.Entry<UUID, State>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, State> entry = it.next();
            ServerPlayer sp = server.getPlayerList().getPlayer(entry.getKey());
            State s = entry.getValue();
            if (sp == null || elisar == null) {
                if (sp != null) Net.send(sp, new PortalRitualMsg(NONE, BlockPos.ZERO, 0));
                it.remove();
                continue;
            }

            ++s.tick;

            if (s.phase == AWAKEN) {
                // Плавная фоновая предзагрузка по 1 чанку Элисара за тик, пока игрок смотрит на белеющий мир и глаза!
                if (s.preloadIdx < s.preloadQueue.size()) {
                    ChunkPos cp = s.preloadQueue.get(s.preloadIdx++);
                    elisar.getChunk(cp.x, cp.z);
                }
                if (s.resolvedTarget == null && s.preloadIdx >= 9) {
                    BlockPos mid = new BlockPos(s.destApronX, 64, s.destApronZ);
                    s.resolvedTarget = elisar.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, mid).above();
                }

                // Атмосферные звуковые акценты открытия глаз и нарастания вспышки
                if (s.tick == 44 || s.tick == 82) {
                    sp.level().playSound(null, sp.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.9F, 0.85F + (s.tick == 82 ? 0.20F : 0.0F));
                }
                if (s.tick == AWAKEN_TICKS - 7) {
                    // Момент резкой белой вспышки!
                    sp.level().playSound(null, sp.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.85F, 1.55F);
                    sp.level().playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 1.35F);
                }

                if (s.tick % 6 == 0) {
                    Net.send(sp, new PortalRitualMsg(AWAKEN, s.portalCenter, s.tick));
                }

                if (s.tick >= AWAKEN_TICKS) {
                    BlockPos target = s.resolvedTarget != null
                            ? s.resolvedTarget
                            : elisar.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(s.destApronX, 64, s.destApronZ)).above();
                    ChunkPos tcp = new ChunkPos(target);
                    elisar.getChunkSource().addRegionTicket(TicketType.PORTAL, tcp, 4, target);

                    // Перенос строго внутри пика белой вспышки в уже сгенерированные чанки Элисара!
                    s.phase = ARRIVE_FLASH;
                    s.tick = 0;
                    Net.send(sp, new PortalRitualMsg(ARRIVE_FLASH, target, 0));
                    sp.teleportTo(elisar, target.getX() + 0.5, target.getY(), target.getZ() + 0.5, sp.getYRot(), sp.getXRot());
                    sp.fallDistance = 0.0F;
                    Net.send(sp, new PortalRitualMsg(ARRIVE_FLASH, target, 0));
                    elisar.playSound(null, target, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.1F, 1.25F);
                }
            } else if (s.phase == ARRIVE_FLASH) {
                if (s.tick % 4 == 0) {
                    Net.send(sp, new PortalRitualMsg(ARRIVE_FLASH, s.portalCenter, s.tick));
                }
                if (s.tick >= ARRIVE_TICKS) {
                    Net.send(sp, new PortalRitualMsg(NONE, BlockPos.ZERO, 0));
                    it.remove();
                }
            }
        }
    }
}
