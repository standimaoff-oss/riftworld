package ru.aiforge.riftworld.registry;

import com.mojang.blaze3d.shaders.FogShape;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.joml.Vector3f;
import ru.aiforge.riftworld.RiftWorld;

/** Elisar water: clear sky-blue, behaves like water (swim, flow, waterfalls). */
public final class ModFluids {
    public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, RiftWorld.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, RiftWorld.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RiftWorld.MODID);

    public static final RegistryObject<FluidType> TYPE = TYPES.register("elisar_water", () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.riftworld.elisar_water").canSwim(true).canDrown(true).canExtinguish(true).canHydrate(true)
            .supportsBoating(true).fallDistanceModifier(0F).motionScale(0.014D).canPushEntity(true)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL).sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)) {
        @Override
        public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
            consumer.accept(new IClientFluidTypeExtensions() {
                private final ResourceLocation still = new ResourceLocation(RiftWorld.MODID, "block/elisar_water_still");
                private final ResourceLocation flow = new ResourceLocation(RiftWorld.MODID, "block/elisar_water_flow");

                @Override
                public ResourceLocation getStillTexture() {
                    return still;
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return flow;
                }

                @Override
                public ResourceLocation getOverlayTexture() {
                    return still;
                }

                @Override
                public int getTintColor() {
                    return 0xFFFFFFFF;
                }

                /** Рев 14.21: вода Висячих садов — яркая бирюза, как на референсе (остальное море прежнее). */
                @Override
                public int getTintColor(net.minecraft.world.level.material.FluidState state,
                                        net.minecraft.world.level.BlockAndTintGetter getter,
                                        net.minecraft.core.BlockPos pos) {
                    if (pos == null) return 0xFFFFFFFF;
                    try {
                        if (ru.aiforge.riftworld.world.WaterfallGardens.inside(pos.getX() + 0.5, pos.getZ() + 0.5)) {
                            return 0xFF7FD9F2;      // бирюзовый оттенок поверх текстуры
                        }
                    } catch (Throwable ignored) {
                    }
                    return 0xFFFFFFFF;
                }

                /** 0 floating on the crest, 1 kneeling on the dunes 42+ blocks down. */
                private static float depth(Camera cam) {
                    int sea = ru.aiforge.riftworld.world.CityPlan.get().ocean;
                    return net.minecraft.util.Mth.clamp((sea - (float) cam.getPosition().y) / 42.0F, 0.0F, 1.0F);
                }

                @Override
                public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
                    float d = depth(camera);
                    // Рев 8: глубина больше не чёрная — холодный синий с бирюзовым свечением снизу
                    return new Vector3f(net.minecraft.util.Mth.lerp(d, 0.20F, 0.07F), net.minecraft.util.Mth.lerp(d, 0.56F, 0.26F), net.minecraft.util.Mth.lerp(d, 0.80F, 0.45F));
                }

                @Override
                public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance, float partialTick, float nearDistance, float farDistance, FogShape shape) {
                    float d = depth(camera);
                    com.mojang.blaze3d.systems.RenderSystem.setShaderFogStart(-6F + d * 2F);
                    com.mojang.blaze3d.systems.RenderSystem.setShaderFogEnd(net.minecraft.util.Mth.lerp(d, 78F, 52F));
                }
            });
        }
    });

    public static final RegistryObject<ForgeFlowingFluid.Source> SOURCE = FLUIDS.register("elisar_water", () -> new ru.aiforge.riftworld.block.ElisarWaterFluid.Source(props()));
    public static final RegistryObject<ForgeFlowingFluid.Flowing> FLOWING = FLUIDS.register("flowing_elisar_water", () -> new ru.aiforge.riftworld.block.ElisarWaterFluid.Flowing(props()));
    public static final RegistryObject<Item> BUCKET = ITEMS.register("elisar_water_bucket",
            () -> new BucketItem(SOURCE, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    private static ForgeFlowingFluid.Properties props() {
        return new ForgeFlowingFluid.Properties(TYPE, SOURCE, FLOWING).block(ElisarBlocks.WATER).bucket(BUCKET)
                .slopeFindDistance(4).levelDecreasePerBlock(1).tickRate(5);
    }

    private ModFluids() {
    }
}
