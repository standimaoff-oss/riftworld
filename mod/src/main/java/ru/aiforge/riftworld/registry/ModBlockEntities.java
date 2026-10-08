package ru.aiforge.riftworld.registry;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.block.CrateBlockEntity;
import ru.aiforge.riftworld.block.ModelKind;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RiftWorld.MODID);

    private ModBlockEntities() {
    }

    /** The two GLB crates (wood + metal): containers with 27 slots drawn by the model renderer. */
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<CrateBlockEntity>> CRATE = TYPES.register("crate", () -> BlockEntityType.Builder.of(CrateBlockEntity::new,
            ModBlocks.MODELS.get(ModelKind.CRATE_WOOD).get(), ModBlocks.MODELS.get(ModelKind.CRATE_METAL).get()).build(null));
}
