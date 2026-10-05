package ru.aiforge.riftworld.item;

import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** «Крюк-серп стража» - the guards' hooked claw-sickle (dropped by them sometimes). */
public class GuardSickleItem extends SwordItem implements GlbItem {
    public GuardSickleItem(Properties p) {
        super(RiftTiers.EMBER_IRON, 5, -2.2F, p);
    }

    @Override
    public String glb() {
        return "guard_sickle";
    }

    @Override
    public float length() {
        return 1.25F;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return ru.aiforge.riftworld.client.ClientHooks.itemRenderer();
            }
        });
    }
}
