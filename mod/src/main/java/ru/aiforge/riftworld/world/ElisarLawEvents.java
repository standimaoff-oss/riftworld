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
     * Игрок умер и попал на respawn. Если он в розыске — его ждёт камера.
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer original)) return;
        if (event.getEntity() instanceof ServerPlayer clone) {
            // Персонаж-клон — это новый игрок после смерти: переносим розыск.
            ElisarLaw law = ElisarLaw.get((ServerLevel) clone.level());
            ElisarLaw.Wanted w = law.wanted(original);
            if (w != null) {
                law.reportCrime((ServerLevel) clone.level(), clone, Math.max(1, w.witnesses), w.jail);
                law.warnings(original);
            }
        }
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

        // 4) сам игрок оказывается в камере
        player.teleportTo(site.cell().getX() + 0.5D, site.cell().getY() + 1.0D, site.cell().getZ() + 0.5D);
        player.setDeltaMovement(0, 0, 0);
        player.setHealth(player.getMaxHealth());
        player.clearFire();
        law.clear(player);
        player.displayClientMessage(Component.literal(
                "\u00a7cВас посадили в городскую тюрьму за убийство, witnessed горожданами."), true);
        player.displayClientMessage(Component.literal(
                "\u00a7eВаши вещи сложены в сундук на складе рядом с тюрьмой."), true);
    }

    /** Кладёт конфискованные вещи в сундук склада, создавая его при нужде. */
    private static void storeInChest(ServerLevel level, BlockPos chestPos, List<ElisarLaw.ItemRecord> records) {
        BlockState state = level.getBlockState(chestPos);
        if (!state.is(ChestBlock.TYPE)) {
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
            ItemStack left = chest.addItem(stack);
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
        List<ElisarLaw.ItemRecord> records = law.release(player.getUUID());
        if (records.isEmpty()) return;
        // Вещи остаются в сундуке на складе: игрок забирает их сам, открыв сундук.
        // Но если сундука уже нет — возвращаем в инвентарь, чтобы не потерять.
        ElisarJail.Site site = ElisarJail.findNear(level, player.getX(), player.getZ(), 240.0);
        boolean chestThere = site != null && level.getBlockState(site.chest()).is(ChestBlock.TYPE);
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
