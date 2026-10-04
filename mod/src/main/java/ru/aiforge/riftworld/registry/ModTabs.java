package ru.aiforge.riftworld.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.aiforge.riftworld.RiftWorld;

public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RiftWorld.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.riftworld"))
            .icon(() -> new ItemStack(ModItems.RESONANCE_STONE.get()))
            .displayItems((params, out) -> {
                out.accept(ModItems.RESONANCE_STONE.get());
                for (var b : ModBlocks.WITH_ITEM) {
                    var item = ForgeRegistries.ITEMS.getValue(b.getId());
                    if (item != null) out.accept(item);
                }
            }).build());

    private ModTabs() {
    }
}
