package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.item.ScytheItem;
import ru.aiforge.riftworld.item.WeaponCombo;
import ru.aiforge.riftworld.net.ComboMsg;
import ru.aiforge.riftworld.net.Net;

/**
 * Rev 4: client side of the weapon combos - picks the step on a charged left click, tells the server, and animates the
 * weapon: own first-person keyframes per step (instead of the vanilla jab) and own third-person arm poses.
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID, value = Dist.CLIENT)
public final class ComboClient {
    static int step = 2;
    static long start = -1000;
    static boolean scythe;

    private ComboClient() {
    }

    static float dur() {
        return scythe ? 17.0F : 10.0F;
    }

    /** 0..1 while a combo animation runs, -1 otherwise. */
    static float progress(float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return -1;
        float t = (mc.level.getGameTime() - start + partial) / dur();
        return t >= 0 && t < 1 ? t : -1;
    }

    @SubscribeEvent
    public static void onClick(InputEvent.InteractionKeyMappingTriggered e) {
        if (!e.isAttack() || e.getHand() != InteractionHand.MAIN_HAND) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || p.isSpectator()) return;
        ItemStack st = p.getMainHandItem();
        if (!WeaponCombo.isComboWeapon(st) || p.getAttackStrengthScale(0.5F) < 0.85F) return;
        long now = mc.level.getGameTime();
        boolean sc = st.getItem() instanceof ScytheItem;
        if (now - start < (sc ? 10 : 6)) return;
        step = sc == scythe && now - start <= 50 ? (step + 1) % 3 : 0;
        scythe = sc;
        start = now;
        Net.CH.sendToServer(new ComboMsg(step));
    }

    // ------------------------------------------------------------------------------------------------ keyframes
    // {t, tx, ty, tz, rx, ry, rz}: rest -> wind-up -> strike -> recover (rest)
    private static final float[][][] FP_SCYTHE = {
            {{0, 0, 0, 0, 0, 0, 0}, {0.3F, 0.30F, 0.06F, 0.10F, 5, 70, -22}, {0.52F, -0.38F, -0.06F, -0.12F, -5, -88, 18}, {1, 0, 0, 0, 0, 0, 0}},
            {{0, 0, 0, 0, 0, 0, 0}, {0.32F, 0.0F, 0.36F, 0.14F, 55, 8, 0}, {0.5F, 0.0F, -0.20F, -0.12F, -80, 0, 0}, {1, 0, 0, 0, 0, 0, 0}},
            {{0, 0, 0, 0, 0, 0, 0}, {0.25F, 0.12F, -0.10F, 0.0F, 12, 40, -12}, {0.62F, 0.0F, -0.22F, -0.05F, -8, -320, 10}, {1, 0, 0, 0, 0, -360, 0}}};
    private static final float[][][] FP_SICKLE = {
            {{0, 0, 0, 0, 0, 0, 0}, {0.28F, 0.16F, 0.20F, 0.05F, 30, 20, -50}, {0.5F, -0.26F, -0.16F, -0.16F, -55, -30, 42}, {1, 0, 0, 0, 0, 0, 0}},
            {{0, 0, 0, 0, 0, 0, 0}, {0.28F, -0.26F, 0.10F, 0.0F, 10, -62, 40}, {0.5F, 0.30F, -0.05F, -0.16F, -20, 70, -30}, {1, 0, 0, 0, 0, 0, 0}},
            {{0, 0, 0, 0, 0, 0, 0}, {0.3F, 0.0F, 0.06F, 0.26F, 22, 0, 45}, {0.5F, 0.0F, 0.0F, -0.48F, -12, 0, -45}, {1, 0, 0, 0, 0, 0, 0}}};
    // third person: {t, rArmX, rArmY, rArmZ, lArmX, lArmY, lArmZ} (radians)
    private static final float[][][] TP_SCYTHE = {
            {{0, -0.9F, -0.3F, 0, -0.9F, 0.6F, 0}, {0.3F, -1.25F, 0.95F, 0, -1.1F, 1.3F, 0}, {0.52F, -1.2F, -1.15F, 0, -1.3F, -0.5F, 0}, {1, -0.9F, -0.3F, 0, -0.9F, 0.6F, 0}},
            {{0, -0.9F, -0.3F, 0, -0.9F, 0.6F, 0}, {0.32F, -2.95F, -0.1F, 0, -2.9F, 0.2F, 0}, {0.5F, -0.45F, -0.1F, 0, -0.55F, 0.3F, 0}, {1, -0.9F, -0.3F, 0, -0.9F, 0.6F, 0}},
            {{0, -0.9F, -0.3F, 0, -0.9F, 0.6F, 0}, {0.25F, -1.0F, 1.25F, 0, -0.8F, 1.5F, 0}, {0.62F, -1.35F, -1.45F, 0.3F, -1.4F, -0.7F, 0}, {1, -0.9F, -0.3F, 0, -0.9F, 0.6F, 0}}};
    private static final float[][][] TP_SICKLE = {
            {{0, -0.3F, 0, 0, 0, 0, 0}, {0.28F, -2.5F, 0.4F, -0.5F, 0, 0, 0}, {0.5F, -0.5F, -0.6F, 0.3F, 0, 0, 0}, {1, -0.3F, 0, 0, 0, 0, 0}},
            {{0, -0.3F, 0, 0, 0, 0, 0}, {0.28F, -1.4F, -1.0F, 0, 0, 0, 0}, {0.5F, -1.3F, 0.95F, 0, 0, 0, 0}, {1, -0.3F, 0, 0, 0, 0, 0}},
            {{0, -0.3F, 0, 0, 0, 0, 0}, {0.3F, -0.55F, 0.1F, 0, -0.6F, 0, 0}, {0.5F, -1.65F, 0, 0, -0.2F, 0, 0}, {1, -0.3F, 0, 0, 0, 0, 0}}};

    private static float ease(float u, boolean strike) {
        return strike ? 1 - (1 - u) * (1 - u) * (1 - u) : u * u * (3 - 2 * u);
    }

    static float[] sample(float[][] k, float t) {
        int i = 0;
        while (i < k.length - 2 && t > k[i + 1][0]) ++i;
        float[] a = k[i], b = k[i + 1];
        float u = Mth.clamp((t - a[0]) / (b[0] - a[0]), 0, 1);
        u = ease(u, i == 1);                                    // the strike segment snaps fast
        float[] r = new float[a.length - 1];
        for (int j = 1; j < a.length; ++j) r[j - 1] = Mth.lerp(u, a[j], b[j]);
        return r;
    }

    /** First person: replaces the vanilla arm / swing transforms for the combo weapons. */
    static boolean handTransform(PoseStack ps, LocalPlayer p, HumanoidArm arm, ItemStack st, float partial, float equip, float swing) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        boolean sc = st.getItem() instanceof ScytheItem;
        float t = arm == p.getMainArm() && sc == scythe ? progress(partial) : -1;
        if (t >= 0) {
            float[] v = sample((sc ? FP_SCYTHE : FP_SICKLE)[step], t);
            ps.translate(side * 0.56F, -0.52F + equip * -0.6F, -0.72F);
            ps.translate(side * v[0], v[1], v[2]);
            ps.mulPose(Axis.YP.rotationDegrees(side * v[4]));
            ps.mulPose(Axis.ZP.rotationDegrees(side * v[5]));
            ps.mulPose(Axis.XP.rotationDegrees(v[3]));
            return true;
        }
        // weak (uncharged) swing: the vanilla jab
        float f = -0.4F * Mth.sin(Mth.sqrt(swing) * (float) Math.PI);
        float f1 = 0.2F * Mth.sin(Mth.sqrt(swing) * (float) Math.PI * 2F);
        float f2 = -0.2F * Mth.sin(swing * (float) Math.PI);
        ps.translate(side * f, f1, f2);
        ps.translate(side * 0.56F, -0.52F + equip * -0.6F, -0.72F);
        float g = Mth.sin(swing * swing * (float) Math.PI);
        ps.mulPose(Axis.YP.rotationDegrees(side * (45.0F + g * -20.0F)));
        float g1 = Mth.sin(Mth.sqrt(swing) * (float) Math.PI);
        ps.mulPose(Axis.ZP.rotationDegrees(side * g1 * -20.0F));
        ps.mulPose(Axis.XP.rotationDegrees(g1 * -80.0F));
        ps.mulPose(Axis.YP.rotationDegrees(side * -45.0F));
        return true;
    }

    /** Third person arm pose (a Forge custom ArmPose). The vanilla attack-swing offsets added afterwards are pre-cancelled. */
    static void armPose(HumanoidModel<?> m, LivingEntity e, HumanoidArm arm, boolean sc) {
        Minecraft mc = Minecraft.getInstance();
        float t = e == mc.player && sc == scythe ? progress(mc.getFrameTime()) : -1;
        boolean right = arm == HumanoidArm.RIGHT;
        float s = right ? 1 : -1;
        var main = right ? m.rightArm : m.leftArm;
        var off = right ? m.leftArm : m.rightArm;
        float[] v = sample((sc ? TP_SCYTHE : TP_SICKLE)[t >= 0 ? step : 0], Math.max(t, 0));
        if (t < 0 && !sc) {                                      // idle one-handed: the vanilla "item" pose
            main.xRot = main.xRot * 0.5F - (float) Math.PI / 10F;
            main.yRot = 0;
            return;
        }
        main.xRot = v[0] + (t < 0 ? main.xRot * 0.15F : 0);
        main.yRot = s * v[1];
        main.zRot = s * v[2];
        if (sc) {
            off.xRot = v[3];
            off.yRot = s * v[4];
            off.zRot = s * v[5];
        }
        float at = m.attackTime;
        if (t >= 0 && at > 0) {                                  // cancel HumanoidModel#setupAttackAnimation for the main arm
            float by = Mth.sin(Mth.sqrt(at) * (float) Math.PI * 2F) * 0.2F * s;
            float f = 1.0F - at;
            f *= f;
            f *= f;
            f = 1.0F - f;
            float f1 = Mth.sin(f * (float) Math.PI);
            float f2 = Mth.sin(at * (float) Math.PI) * -(m.head.xRot - 0.7F) * 0.75F;
            main.xRot += f1 * 1.2F + f2;
            main.yRot -= by * 3.0F;
            main.zRot -= Mth.sin(at * (float) Math.PI) * -0.4F;
            off.yRot -= by;
            if (right) off.xRot -= by;
        }
    }

    private static HumanoidModel.ArmPose poseScythe, poseSickle;

    static HumanoidModel.ArmPose pose(boolean sc) {
        if (poseScythe == null) {
            poseScythe = HumanoidModel.ArmPose.create("RIFTWORLD_SCYTHE", true, (m, e, a) -> armPose(m, e, a, true));
            poseSickle = HumanoidModel.ArmPose.create("RIFTWORLD_SICKLE", false, (m, e, a) -> armPose(m, e, a, false));
        }
        return sc ? poseScythe : poseSickle;
    }

    /** Client extensions shared by both combo weapons: GLB renderer + the animations above. */
    public static IClientItemExtensions ext(boolean sc) {
        return new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return ClientHooks.itemRenderer();
            }

            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity e, InteractionHand hand, ItemStack st) {
                return hand == InteractionHand.MAIN_HAND ? pose(sc) : null;
            }

            @Override
            public boolean applyForgeHandTransform(PoseStack ps, LocalPlayer p, HumanoidArm arm, ItemStack st, float partial, float equip, float swing) {
                return handTransform(ps, p, arm, st, partial, equip, swing);
            }
        };
    }
}
