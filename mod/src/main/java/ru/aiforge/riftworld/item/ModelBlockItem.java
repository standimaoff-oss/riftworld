package ru.aiforge.riftworld.item;

import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import ru.aiforge.riftworld.block.ModelBlock;

/** Block item drawn with the block's GLB (see ModelItemRenderer). */
public class ModelBlockItem extends BlockItem {
    public ModelBlockItem(ModelBlock block, Properties props) {
        super(block, props);
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
