package ru.aiforge.riftworld.registry;

import com.google.common.collect.ImmutableSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.aiforge.riftworld.RiftWorld;

/** The three civic occupations shown in the Elisar concept art. Each owns a vanilla POI-backed job site. */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ElisarProfessions {
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(ForgeRegistries.POI_TYPES, RiftWorld.MODID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS, RiftWorld.MODID);

    public static final RegistryObject<PoiType> EXCHANGE_POI = point("aurite_exchange", ModBlocks.AURITE_EXCHANGE);
    public static final RegistryObject<PoiType> GUARD_POI = point("city_guard_post", ModBlocks.CITY_GUARD_POST);
    public static final RegistryObject<PoiType> RANGER_POI = point("ranger_fletching_table", ModBlocks.RANGER_FLETCHING_TABLE);

    public static final RegistryObject<VillagerProfession> DUCATIER = profession("ducatier", EXCHANGE_POI, SoundEvents.VILLAGER_WORK_WEAPONSMITH);
    public static final RegistryObject<VillagerProfession> CITY_GUARD = profession("city_guard", GUARD_POI, SoundEvents.VILLAGER_WORK_ARMORER);
    public static final RegistryObject<VillagerProfession> RANGER = profession("ranger", RANGER_POI, SoundEvents.VILLAGER_WORK_FLETCHER);

    private static RegistryObject<PoiType> point(String id, RegistryObject<? extends net.minecraft.world.level.block.Block> block) {
        return POI_TYPES.register(id, () -> new PoiType(ImmutableSet.copyOf(block.get().getStateDefinition().getPossibleStates()), 1, 1));
    }

    private static RegistryObject<VillagerProfession> profession(String id, RegistryObject<PoiType> poi, net.minecraft.sounds.SoundEvent sound) {
        return PROFESSIONS.register(id, () -> new VillagerProfession(id,
                holder -> holder.value() == poi.get(), holder -> holder.value() == poi.get(),
                ImmutableSet.of(), ImmutableSet.of(), sound));
    }

    public static void register(IEventBus bus) {
        POI_TYPES.register(bus);
        PROFESSIONS.register(bus);
    }

    @SubscribeEvent
    public static void addTrades(VillagerTradesEvent event) {
        if (event.getType() == DUCATIER.get()) {
            event.getTrades().get(1).add((villager, random) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 2), new ItemStack(ModItems.ELISAR_DUCAT.get(), 6), 16, 3, 0.05F));
            event.getTrades().get(2).add((villager, random) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 5), new ItemStack(ModItems.AURITE_INGOT.get()), 8, 8, 0.05F));
        } else if (event.getType() == CITY_GUARD.get()) {
            event.getTrades().get(1).add((villager, random) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 7), new ItemStack(ModItems.AURITE_SWORD.get()), 4, 5, 0.08F));
            event.getTrades().get(3).add((villager, random) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 24), new ItemStack(ModItems.AURITE_CHESTPLATE.get()), 2, 15, 0.08F));
        } else if (event.getType() == RANGER.get()) {
            event.getTrades().get(1).add((villager, random) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD), new ItemStack(Items.ARROW, 12), 16, 2, 0.05F));
            event.getTrades().get(3).add((villager, random) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 18), new ItemStack(ModItems.AURITE_PICKAXE.get()), 3, 12, 0.08F));
        }
    }

    private ElisarProfessions() {}
}
