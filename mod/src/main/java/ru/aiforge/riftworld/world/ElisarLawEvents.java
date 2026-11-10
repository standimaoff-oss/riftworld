package ru.aiforge.riftworld.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import ru.aiforge.riftworld.RiftWorld;

import java.util.ArrayList;
import java.util.List;

/**
 * v0.49.0 — «городской закон» Элисара: арест, тюрьма и склад при тюрьме.
 *
 * <p>Полная цепочка, как её просил игрок:
 * <ol>
 *   <li>Убил стража (или жителя) <b>на глазах у города</b> — преступление
 *       зафиксировано ({@link ElisarWitness}), игрок попадает в розыск.</li>
 *   <li>Стражи переходят в погоню за разыскиваемым ({@link ElisarGuardEntity}).</li>
 *   <li>Игрок умер (от стража, от падения, от чего угодно) — если розыск был
 *       активен, игрок <b>просыпается в камере</b> городской тюрьмы.</li>
 *   <li>Вещи арестованного <b>складываются в сундук на складе рядом с
 *       тюрьмой</b> и возвращаются при выходе из камеры.</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID)
public final class ElisarLawEvents {

    private ElisarLawEvents() {
    }

    /**
     * Игрок умер и попал на respawn.
     *
     * <p>Розыск живёт в данных мира ({@link ElisarLaw} — SavedData), поэтому
     * переносить его на клона НЕ нужно: он и так остаётся в силе. Клон того
     * же игрока имеет тот же UUID, а состояние закона и так хранится по
     * UUID. Копировать розыск заново значило бы обнулять его срок.</p>
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // Розыск и клетка переживают смерть сами: см. ElisarLaw (SavedData).
    }

    /**
     * Смерть игрока: если он в розыске — тюрьма и конфискация.
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = (ServerLevel) player.level();
        ElisarLaw law = ElisarLaw.get(level);
        if (!law.isWanted(level, player)) return;

        // 1) конфискация: вещи уходят на склад при тюрьме
        List<ElisarLaw.ItemRecord> records = new ArrayList<>();
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); ++i) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            records.add(new ElisarLaw.ItemRecord(
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                    stack.getCount()));
            inv.setItem(i, ItemStack.EMPTY);
        }
        law.confiscate(player.getUUID(), records);

        // 2) тюрьма: находим (или строим) камеру и склад
        ElisarJail.Site site = ElisarJail.findNear(level, player.getX(), player.getZ(), 240.0);
        if (site == null) {
            // Нет тюрьмы рядом — строим новую у ближайшей дороги.
            var road = nearestRoad(level, player.getX(), player.getZ());
            if (road != null) {
                site = ElisarJail.build(level, road.x, road.z, road.face);
            }
        }
        if (site == null) {
            player.displayClientMessage(Component.literal(
                    "\u00a7cГород не нашёл тюрьму для вас. Розыск остаётся в силе."), true);
            return;
        }

        // 3) сундук на складе: конфискованные вещи кладём в СУНДУК, а не в AIR
        if (!records.isEmpty()) {
            storeInChest(level, site.chest(), records);
        }

        // 4) запоминаем клетку: игрок окажется в ней при ВОЗРОЖДЕНИИ.
        // Телепортировать мёртвого игрока бессмысленно — возрождение всё
        // равно поставит его на точку спавна, поэтому перенос делаем в
        // PlayerRespawnEvent (см. onRespawn).
        law.setJail(player.getUUID(), site.cell());
        law.clear(player);
    }

    /**
     * Кладёт стопку в сундук. В 1.20.1 у {@link ChestBlockEntity} нет своего
     * {@code addItem} и нет {@code ContainerHelper.addItem}, поэтому раскладываем
     * по слотам вручную.
     */
    private static ItemStack addToChest(ChestBlockEntity chest, ItemStack stack) {
        int size = chest.getContainerSize();
        // сначала доливаем уже существующие стопки
        for (int i = 0; i < size; ++i) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            ItemStack inSlot = chest.getItem(i);
            if (!inSlot.isEmpty() && ItemStack.isSameItemSameTags(inSlot, stack)) {
                int room = inSlot.getMaxStackSize() - inSlot.getCount();
                if (room > 0) {
                    int moved = Math.min(room, stack.getCount());
                    inSlot.grow(moved);
                    stack.shrink(moved);
                    chest.setItem(i, inSlot);
                }
            }
        }
        // затем занимаем пустые слоты
        for (int i = 0; i < size && !stack.isEmpty(); ++i) {
            if (!chest.getItem(i).isEmpty()) continue;
            int moved = Math.min(stack.getMaxStackSize(), stack.getCount());
            chest.setItem(i, stack.copyWithCount(moved));
            stack.shrink(moved);
        }
        return stack;
    }

    /** Кладёт конфискованные вещи в сундук склада, создавая его при нужде. */
    private static void storeInChest(ServerLevel level, BlockPos chestPos, List<ElisarLaw.ItemRecord> records) {
        BlockState state = level.getBlockState(chestPos);
        if (!state.is(net.minecraft.world.level.block.Blocks.CHEST)) {
            // Сундука нет (например, его сломали) — ставим новый.
            level.setBlock(chestPos, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(), 3);
            state = level.getBlockState(chestPos);
        }
        if (!(level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest)) return;
        for (ElisarLaw.ItemRecord rec : records) {
            var id = net.minecraft.resources.ResourceLocation.tryParse(rec.itemId());
            if (id == null) continue;
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
            if (item == null || item == net.minecraft.world.item.Items.AIR) continue;
            ItemStack stack = new ItemStack(item, Math.max(1, rec.count()));
            // Если сундук полон — остаток падает рядом, чтобы вещи не пропали.
            ItemStack left = addToChest(chest, stack);
            if (!left.isEmpty()) {
                net.minecraft.world.entity.item.ItemEntity dropped = new net.minecraft.world.entity.item.ItemEntity(
                        level, chestPos.getX() + 0.5D, chestPos.getY() + 1.0D, chestPos.getZ() + 0.5D, left);
                level.addFreshEntity(dropped);
            }
        }
        chest.setChanged();
    }

    /** Ближайшая дорога города — чтобы поставить тюрьму «у дороги». */
    private record RoadPoint(double x, double z, Direction face) {
    }

    private static RoadPoint nearestRoad(ServerLevel level, double x, double z) {
        CityPlan plan = CityPlan.get();
        PatrolRoute route = PatrolRoute.nearestTo(plan, level, x, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z), z, 300.0);
        if (route == null) return null;
        PatrolRoute.Waypoint wp = route.nearest(x, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z), z);
        if (wp == null) return null;
        // Тюрьма встаёт перпендикулярно направлению дороги.
        Direction face = Direction.fromYRot(wp.yaw()).getOpposite();
        return new RoadPoint(wp.x(), wp.z(), face);
    }

    /** После возрождения в тюрьме — вернуть конфискованные вещи из сундука. */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = (ServerLevel) player.level();
        ElisarLaw law = ElisarLaw.get(level);

        // Разыскиваемого после смерти сажают в клетку городской тюрьмы.
        BlockPos cell = law.takeJail(player.getUUID());
        if (cell != null) {
            player.teleportTo(cell.getX() + 0.5D, cell.getY() + 1.0D, cell.getZ() + 0.5D);
            player.setDeltaMovement(0, 0, 0);
            player.setHealth(player.getMaxHealth());
            player.clearFire();
            player.displayClientMessage(Component.literal(
                    "\u00a7cВас посадили в городскую тюрьму: убийство видели горожане."), true);
            player.displayClientMessage(Component.literal(
                    "\u00a7eВаши вещи сложены в сундук на складе рядом с тюрьмой."), true);
        }

        List<ElisarLaw.ItemRecord> records = law.release(player.getUUID());
        if (records.isEmpty()) return;
        // Вещи остаются в сундуке на складе: игрок забирает их сам, открыв сундук.
        // Но если сундука уже нет — возвращаем в инвентарь, чтобы не потерять.
        ElisarJail.Site site = ElisarJail.findNear(level, player.getX(), player.getZ(), 240.0);
        boolean chestThere = site != null && level.getBlockState(site.chest()).is(net.minecraft.world.level.block.Blocks.CHEST);
        if (chestThere) {
            player.displayClientMessage(Component.literal(
                    "\u00a7eВаши вещи ждут в сундуке на складе рядом с тюрьмой."), true);
            return;
        }
        for (ElisarLaw.ItemRecord rec : records) {
            var id = net.minecraft.resources.ResourceLocation.tryParse(rec.itemId());
            if (id == null) continue;
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
            if (item == null || item == net.minecraft.world.item.Items.AIR) continue;
            player.getInventory().add(new ItemStack(item, Math.max(1, rec.count())));
        }
    }

    /** Сохраняемое состояние розыска при перезагрузке мира. */
    @Mod.EventBusSubscriber(modid = RiftWorld.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        private ModEvents() {
        }

        @SubscribeEvent
        public static void onSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(() -> {
                // Регистрация хранилища закона происходит в ElisarLaw.get().
            });
        }
    }
}
