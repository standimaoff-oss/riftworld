package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.client.geo.GeoModel;
import ru.aiforge.riftworld.item.CrystalAegisItem;
import ru.aiforge.riftworld.item.CrystalBarrageItem;
import ru.aiforge.riftworld.net.CrystalActionMsg;
import ru.aiforge.riftworld.net.Net;

/**
 * Клиентский обработчик кристального снаряжения:
 * <ol>
 *   <li>Перехватывает нажатие ЛКМ с «Веером Осколков» или «Бастионом Граней» в главной руке
 *       и отправляет на сервер {@link CrystalActionMsg} (одиночный выстрел / таранный щит).</li>
 *   <li>Во время удержания ПКМ с «Веером Осколков» рисует прямо в поле зрения игрока (FOV)
 *       полудугу из 1..5 строящихся 3D-кристаллов, нацеленных в перекрестие прицела!</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID, value = Dist.CLIENT)
public final class CrystalClientHandler {
    private static final ResourceLocation TEX_SHARD = new ResourceLocation("riftworld", "textures/entity/crystal_shard.png");

    private CrystalClientHandler() {
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered e) {
        if (!e.isAttack() || e.getHand() != InteractionHand.MAIN_HAND) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || p.isSpectator() || p.isUsingItem()) return;

        ItemStack st = p.getMainHandItem();
        if (st.getItem() instanceof CrystalBarrageItem || st.getItem() instanceof CrystalAegisItem) {
            e.setCanceled(true);
            e.setSwingHand(true);
            if (!p.getCooldowns().isOnCooldown(st.getItem())) {
                Net.CH.sendToServer(new CrystalActionMsg(CrystalActionMsg.LEFT_CLICK_MAIN));
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || !p.isUsingItem()) return;
        ItemStack use = p.getUseItem();
        if (!(use.getItem() instanceof CrystalBarrageItem barrage)) return;

        int used = barrage.getUseDuration(use) - p.getUseItemRemainingTicks();
        int count = CrystalBarrageItem.builtCrystals(used);
        if (count <= 0) return;

        GeoModel shardModel = GeoModel.of("crystal_shard");
        if (shardModel == null) return;
        shardModel.animate(0.0F);

        Camera cam = e.getCamera();
        Vec3 camPos = cam.getPosition();
        float partial = e.getPartialTick();
        Vec3 fwd = p.getViewVector(partial).normalize();
        Vec3 right = fwd.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0E-4) {
            right = Vec3.directionFromRotation(0.0F, p.getViewYRot(partial) + 90.0F);
        } else {
            right = right.normalize();
        }
        Vec3 up = right.cross(fwd).normalize();
        Vec3 eye = p.getEyePosition(partial);
        Vec3 aim = eye.add(fwd.scale(28.0));

        PoseStack ps = e.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_SHARD));

        for (int i = 0; i < count; ++i) {
            double[] off = CrystalBarrageItem.FOV_OFFSETS[i];
            float bob = 0.03F * Mth.sin((p.tickCount + partial) * 0.22F + i * 1.1F);
            Vec3 worldPos = eye.add(right.scale(off[0])).add(up.scale(off[1] + bob)).add(fwd.scale(off[2]));
            Vec3 toAim = aim.subtract(worldPos).normalize();
            float yaw = (float) (Mth.atan2(toAim.x, toAim.z) * (180.0 / Math.PI));
            float pitch = (float) (Mth.atan2(toAim.y, toAim.horizontalDistance()) * (180.0 / Math.PI));

            ps.pushPose();
            ps.translate(worldPos.x - camPos.x, worldPos.y - camPos.y, worldPos.z - camPos.z);
            ps.mulPose(Axis.YP.rotationDegrees(yaw - 180.0F));
            ps.mulPose(Axis.XP.rotationDegrees(pitch));
            ps.mulPose(Axis.ZP.rotationDegrees((p.tickCount + partial) * 5.0F + i * 36.0F));
            ps.scale(0.52F, 0.52F, 0.52F);
            shardModel.root.render(ps, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            ps.popPose();
        }
        buffers.endBatch(RenderType.entityCutoutNoCull(TEX_SHARD));
    }
}
