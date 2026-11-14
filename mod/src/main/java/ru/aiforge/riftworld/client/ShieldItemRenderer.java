package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.client.model.ShieldItemModel;
import ru.aiforge.riftworld.item.ElisarShieldItem;

/**
 * v0.53.0 — рендер щитов Элисара в руке.
 *
 * <p>В Forge 1.20.1 ванильный рендерер щита рисует <b>только</b> предмет
 * {@code minecraft:shield}: кастомный {@code ShieldItem} без собственного
 * рендерера остаётся в руке пустым (ванильная модель {@code builtin/entity}
 * для кастомного щита не работает). Поэтому щит рисуется здесь — той же
 * ванильной геометрией (12×22×1 «plate» + 2×6×6 «handle»), но с текстурой
 * мода, чтобы щит был в палитре аурита/рифт-кристалла, а не деревянный.
 *
 * <p>Текстура: {@code textures/entity/<имя>_base_nopattern.png} в пространстве
 * имён мода — ровно как у ванильного щита ({@code entity/shield_base_nopattern}).
 */
public class ShieldItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static ShieldItemRenderer instance;

    private final ShieldItemModel model;

    public ShieldItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
        this.model = new ShieldItemModel(models.bakeLayer(ShieldItemModel.LAYER_LOCATION));
    }

    /** Единственный экземпляр — его отдаёт {@link #getCustomRenderer()}. */
    public static ShieldItemRenderer instance() {
        if (instance == null) {
            Minecraft mc = Minecraft.getInstance();
            instance = new ShieldItemRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
        }
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps,
                             MultiBufferSource buffers, int light, int overlay) {
        ResourceLocation texture = textureFor(stack);
        if (texture == null) {
            return;
        }
        RenderType type = RenderType.entityCutoutNoCull(texture);
        VertexConsumer vc = buffers.getBuffer(type);

        // Та же ориентация, что у ванильного щита в руке.
        ps.pushPose();
        ps.scale(1.0F, -1.0F, -1.0F);
        this.model.plate().render(ps, vc, light, OverlayTexture.NO_OVERLAY);
        this.model.handle().render(ps, vc, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }

    /**
     * Текстура щита по предмету: щит Элисара — палитра аурита,
     * щит Стража — палитра рифт-кристалла.
     */
    private static ResourceLocation textureFor(ItemStack stack) {
        if (!(stack.getItem() instanceof ElisarShieldItem)) {
            return null;
        }
        String name = stack.getItem() == ru.aiforge.riftworld.registry.ModItems.ELISAR_GUARD_SHIELD.get()
                ? "elisar_guard_shield"
                : "elisar_shield";
        return new ResourceLocation(RiftWorld.MODID, "textures/entity/" + name + "_base_nopattern.png");
    }
}
