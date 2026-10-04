package ru.aiforge.riftworld.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.block.CrateBlockEntity;
import ru.aiforge.riftworld.block.ModelKind;
import ru.aiforge.riftworld.block.RiftBlockEntity;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RiftWorld.MODID);

    private ModBlockEntities() {
    }

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<RiftBlockEntity>> MODEL = TYPES.register("model", () -> BlockEntityType.Builder.of(RiftBlockEntity::new,
            ModBlocks.MODELS.entrySet().stream().filter(e -> !e.getKey().isContainer()).map(e -> (Block) e.getValue().get()).toArray(Block[]::new)).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<ru.aiforge.riftworld.block.LaunchCoreBlockEntity>> LAUNCH_CORE = TYPES.register("launch_core",
            () -> BlockEntityType.Builder.of(ru.aiforge.riftworld.block.LaunchCoreBlockEntity::new, ModBlocks.LAUNCH_CORE.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<CrateBlockEntity>> CRATE = TYPES.register("crate", () -> BlockEntityType.Builder.of(CrateBlockEntity::new,
            ModBlocks.MODELS.get(ModelKind.CRATE_WOOD).get(), ModBlocks.MODELS.get(ModelKind.CRATE_METAL).get()).build(null));
}
