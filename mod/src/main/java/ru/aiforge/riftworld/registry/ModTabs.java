package ru.aiforge.riftworld.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.aiforge.riftworld.RiftWorld;

@Mod.EventBusSubscriber(modid = RiftWorld.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RiftWorld.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.riftworld"))
            .icon(() -> new ItemStack(ModItems.RESONANCE_STONE.get()))
            .displayItems((params, out) -> {
                out.accept(ModItems.RESONANCE_STONE.get());
                out.accept(ModFluids.BUCKET.get());
                out.accept(ModItems.MORGRATH_SCYTHE.get());
                out.accept(ModItems.GUARD_SICKLE.get());
                out.accept(ModItems.CRYSTAL_KATAR.get());
                out.accept(ModItems.CRYSTAL_BARRAGE.get());
                out.accept(ModItems.CRYSTAL_AEGIS.get());
                out.accept(ModItems.ASTRAL_PRISM.get());
                out.accept(ModItems.ELISAR_DUCAT.get());
                out.accept(ModItems.AURITE_INGOT.get());
                out.accept(ModItems.RIFTCRYSTAL.get());
                out.accept(ModItems.AURITE_SWORD.get());
                out.accept(ModItems.AURITE_PICKAXE.get());
                out.accept(ModItems.AURITE_AXE.get());
                out.accept(ModItems.AURITE_SHOVEL.get());
                out.accept(ModItems.AURITE_HELMET.get());
                out.accept(ModItems.AURITE_CHESTPLATE.get());
                out.accept(ModItems.AURITE_LEGGINGS.get());
                out.accept(ModItems.AURITE_BOOTS.get());
                out.accept(ModItems.RIFTCRYSTAL_SWORD.get());
                out.accept(ModItems.RIFTCRYSTAL_PICKAXE.get());
                out.accept(ModItems.RIFTCRYSTAL_AXE.get());
                out.accept(ModItems.RIFTCRYSTAL_SHOVEL.get());
                out.accept(ModItems.RIFTCRYSTAL_HELMET.get());
                out.accept(ModItems.RIFTCRYSTAL_CHESTPLATE.get());
                out.accept(ModItems.RIFTCRYSTAL_LEGGINGS.get());
                out.accept(ModItems.RIFTCRYSTAL_BOOTS.get());
                out.accept(ModItems.BRIDGE_ROPE.get());
                out.accept(ModItems.GILDED_PAPER.get());
                out.accept(ModItems.EMERALD_PAPER.get());
                out.accept(ModItems.DIAMOND_PAPER.get());
                out.accept(ModItems.AMETHYST_PAPER.get());
                out.accept(ModItems.CRIMSON_PAPER.get());
                out.accept(ModItems.LAPIS_PAPER.get());
                out.accept(ModItems.DRAGON_SADDLE.get());
                out.accept(ModItems.DRAGON_WHISTLE.get());
                out.accept(ModItems.LIOPLEURODON_TOOTH.get());
                out.accept(ModItems.SEA_SCALE.get());

                // Все 16 яиц призыва мобов мода (включая Бумажного дракона и все 11 морских видов!)
                acceptAllSpawnEggs(out);

                for (var b : ModBlocks.WITH_ITEM) {
                    var item = ForgeRegistries.ITEMS.getValue(b.getId());
                    if (item != null) out.accept(item);
                }
            }).build());

    private static void acceptAllSpawnEggs(CreativeModeTab.Output out) {
        out.accept(ModItems.LIOPLEURODON_EGG.get());
        out.accept(ModItems.EL_GRAN_MAJA_EGG.get());
        out.accept(ModItems.BLOSSOM_STAG_EGG.get());
        out.accept(ModItems.PEGASUS_EGG.get());
        out.accept(ModItems.WENDIGO_EGG.get());
        out.accept(ModItems.PEGASUS_FEATHER.get());
        out.accept(ModItems.WENDIGO_ANTLER.get());
        out.accept(ModItems.MOROK_EGG.get());
        out.accept(ModItems.MOROK_EYE.get());
        out.accept(ModItems.SKY_DART_EGG.get());
        out.accept(ModItems.PAPER_DRAGON_EGG.get());
        out.accept(ModItems.MORGRATH_EGG.get());
        out.accept(ModItems.GUARD_EGG.get());
        out.accept(ModItems.RIFT_OBELISK_BOSS_EGG.get());
        out.accept(ModItems.CRYSTAL_MINION_EGG.get());
        out.accept(ModItems.EGG_SILVER.get());
        out.accept(ModItems.EGG_GOLD.get());
        out.accept(ModItems.EGG_STRIPED.get());
        out.accept(ModItems.EGG_JELLY.get());
        out.accept(ModItems.EGG_RAY.get());
        out.accept(ModItems.EGG_ANGLER.get());
        out.accept(ModItems.EGG_EEL.get());
        out.accept(ModItems.EGG_SURGEON.get());
        out.accept(ModItems.EGG_TURTLE.get());
        out.accept(ModItems.EGG_GLOOM.get());
        out.accept(ModItems.EGG_STING.get());
    }

    @SubscribeEvent
    public static void onBuildVanillaTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            acceptAllSpawnEggs(event);
        }
    }

    private ModTabs() {
    }
}
