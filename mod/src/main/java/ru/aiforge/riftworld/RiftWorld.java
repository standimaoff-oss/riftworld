package ru.aiforge.riftworld;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import ru.aiforge.riftworld.registry.ModBlockEntities;
import ru.aiforge.riftworld.registry.ModBlocks;
import ru.aiforge.riftworld.registry.ModFluids;
import ru.aiforge.riftworld.registry.ModItems;
import ru.aiforge.riftworld.registry.ModWorldgen;
import ru.aiforge.riftworld.registry.ModTabs;

/** «Разлом» — abandoned laboratory and the road to planet Elysar. */
@Mod(RiftWorld.MODID)
public final class RiftWorld {
    public static final String MODID = "riftworld";

    public RiftWorld() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        // Рев 13: блоки — раньше всех. Аддон Dynamic Trees читает trees/*.json в RegisterEvent(BLOCKS)
        // на обычном приоритете и должен видеть riftworld:*-блоки, иначе получает air, «теряет»
        // LeavesProperties и падает NPE в SoilProperties#getBlock (см. EarlyRegistrations).
        ru.aiforge.riftworld.registry.EarlyRegistrations.registerFirst(bus, ModBlocks.BLOCKS);
        ru.aiforge.riftworld.registry.ModEntities.TYPES.register(bus);
        ModItems.ITEMS.register(bus);
        ModFluids.TYPES.register(bus);
        ModFluids.FLUIDS.register(bus);
        ModFluids.ITEMS.register(bus);
        ModWorldgen.GENERATORS.register(bus);
        ModWorldgen.BIOME_SOURCES.register(bus);
        ModWorldgen.STRUCTURE_TYPES.register(bus);
        ModWorldgen.PIECES.register(bus);
        ModBlockEntities.TYPES.register(bus);
        ModTabs.TABS.register(bus);
        ru.aiforge.riftworld.net.Net.init();
    }
}
