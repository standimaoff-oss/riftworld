package ru.aiforge.riftworld.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.block.ModelBlock;
import ru.aiforge.riftworld.item.ModelBlockItem;
import ru.aiforge.riftworld.item.ResonanceStoneItem;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RiftWorld.MODID);

    /** Biome 01: creative spawn egg of the paper dragon. */
    public static final RegistryObject<Item> PAPER_DRAGON_EGG = ITEMS.register("paper_dragon_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.PAPER_DRAGON, 0xF3EFE4, 0xD22E28, new Item.Properties()));

    /** Rev 4 item 13: rope for self-assembling bridges (craft: 3 string in a column). */
    public static final RegistryObject<Item> BRIDGE_ROPE = ITEMS.register("bridge_rope",
            () -> new Item(new Item.Properties().stacksTo(32)));

    public static final RegistryObject<Item> RESONANCE_STONE = ITEMS.register("resonance_stone",
            () -> new ResonanceStoneItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    /** Валюта города: выплавляется из ауритовой руды, что змеится в ярусах Висячих садов (биом 07). */
    public static final RegistryObject<Item> ELISAR_DUCAT = ITEMS.register("elisar_ducat",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> MORGRATH_SCYTHE = ITEMS.register("morgrath_scythe",
            () -> new ru.aiforge.riftworld.item.ScytheItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> GUARD_SICKLE = ITEMS.register("guard_sickle",
            () -> new ru.aiforge.riftworld.item.GuardSickleItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> MORGRATH_EGG = ITEMS.register("morgrath_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.BOSS, 0x2a1d18, 0xd8c8a8, new Item.Properties()));
    public static final RegistryObject<Item> GUARD_EGG = ITEMS.register("ember_guard_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.GUARD, 0x3b2b26, 0xe0602a, new Item.Properties()));

    static {
        for (RegistryObject<? extends net.minecraft.world.level.block.Block> b : ModBlocks.WITH_ITEM) {
            ITEMS.register(b.getId().getPath(), () -> b.get() instanceof ModelBlock mb
                    ? new ModelBlockItem(mb, new Item.Properties())
                    : new BlockItem(b.get(), new Item.Properties()));
        }
    }

    private ModItems() {
    }
}
