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

    public static final RegistryObject<Item> RESONANCE_STONE = ITEMS.register("resonance_stone",
            () -> new ResonanceStoneItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

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
