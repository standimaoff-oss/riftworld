package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import ru.aiforge.riftworld.client.geo.GeoModel;
import ru.aiforge.riftworld.entity.CrystalMinion;
import ru.aiforge.riftworld.entity.CrystalShardEntity;
import ru.aiforge.riftworld.entity.CrystalShieldEntity;
import ru.aiforge.riftworld.entity.RiftObeliskBoss;

/**
 * 3D-рендереры сущностей Колоннады Разлома:
 * <ul>
 *   <li>{@link Boss} — «Расколотый Обелиск Разлома» (без тени человека, с 3 вращающимися кольцами астролябии);</li>
 *   <li>{@link Minion} — «Гранёный Прислужник» (парящий конструкт с клинками-катарами на руках);</li>
 *   <li>{@link Shield} — 3D кристальный щит «Бастиона Граней»;</li>
 *   <li>{@link Shard} — 3D летящий кристальный осколок.</li>
 * </ul>
 */
public final class CrystalGeoRenderers {
    private static final ResourceLocation TEX_BOSS = new ResourceLocation("riftworld", "textures/entity/rift_obelisk_boss.png");
    private static final ResourceLocation TEX_MINION = new ResourceLocation("riftworld", "textures/entity/crystal_minion.png");
    private static final ResourceLocation TEX_SHIELD = new ResourceLocation("riftworld", "textures/entity/crystal_shield.png");
    private static final ResourceLocation TEX_SHARD = new ResourceLocation("riftworld", "textures/entity/crystal_shard.png");

    private CrystalGeoRenderers() {
    }

    private static ModelPart childOrNull(ModelPart parent, String name) {
        try {
            return parent.getChild(name);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static final class Boss extends EntityRenderer<RiftObeliskBoss> {
        public Boss(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.0F; // левитирующий обелиск Разлома без тени!
        }

        @Override
        public ResourceLocation getTextureLocation(RiftObeliskBoss e) {
            return TEX_BOSS;
        }

        @Override
        public void render(RiftObeliskBoss e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
            GeoModel m = GeoModel.of("rift_obelisk_boss");
            if (m == null) return;
            float t = e.tickCount + partial;
            float spinBoost = e.bossState() == RiftObeliskBoss.STATE_IDLE ? 1.0F : 2.8F;

            m.animate(0.0F);
            ModelPart body = childOrNull(m.root, "body");
            if (body != null) {
                ModelPart core = childOrNull(body, "prism_core");
                if (core != null) {
                    core.yRot = t * 0.08F * spinBoost;
                    core.xRot = Mth.sin(t * 0.05F) * 0.14F;
                }
                ModelPart r1 = childOrNull(body, "ring_inner");
                if (r1 != null) {
                    r1.yRot += t * 0.11F * spinBoost;
                    r1.xRot += t * 0.05F * spinBoost;
                }
                ModelPart r2 = childOrNull(body, "ring_mid");
                if (r2 != null) {
                    r2.yRot -= t * 0.075F * spinBoost;
                    r2.zRot += t * 0.045F * spinBoost;
                }
                ModelPart r3 = childOrNull(body, "ring_outer");
                if (r3 != null) {
                    r3.yRot += t * 0.048F * spinBoost;
                    r3.xRot -= t * 0.032F * spinBoost;
                }
                ModelPart orb = childOrNull(body, "orbit_shards");
                if (orb != null) {
                    orb.yRot = -t * 0.055F;
                }
                float spread = e.bossState() == RiftObeliskBoss.STATE_PULSE ? 0.12F : 0.03F * Mth.sin(t * 0.07F);
                ModelPart left = childOrNull(body, "obelisk_left");
                ModelPart right = childOrNull(body, "obelisk_right");
                if (left != null) left.zRot += spread;
                if (right != null) right.zRot -= spread;
            }

            pose.pushPose();
            float yRot = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
            pose.mulPose(Axis.YP.rotationDegrees(180.0F - yRot));
            pose.scale(m.renderScale, m.renderScale, m.renderScale);

            VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX_BOSS));
            int glowLight = Math.max(light, LightTexture.pack(13, 15));
            m.root.render(pose, vc, glowLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            pose.popPose();
            super.render(e, yaw, partial, pose, buf, light);
        }
    }

    public static final class Minion extends EntityRenderer<CrystalMinion> {
        public Minion(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.25F;
        }

        @Override
        public ResourceLocation getTextureLocation(CrystalMinion e) {
            return TEX_MINION;
        }

        @Override
        public void render(CrystalMinion e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
            GeoModel m = GeoModel.of("crystal_minion");
            if (m == null) return;
            float t = e.tickCount + partial;
            m.animate(0.0F);

            ModelPart body = childOrNull(m.root, "body");
            if (body != null) {
                ModelPart tail = childOrNull(body, "wisp_tail");
                if (tail != null) {
                    tail.xRot = 0.18F + Mth.sin(t * 0.18F) * 0.16F;
                    tail.zRot = Mth.cos(t * 0.14F) * 0.12F;
                }
                ModelPart armR = childOrNull(body, "arm_right");
                ModelPart armL = childOrNull(body, "arm_left");
                int slash = e.slashTicks();
                float slashWave = slash > 0 ? Mth.sin((12.0F - slash + partial) / 12.0F * (float) Math.PI) : 0.0F;
                if (armR != null) {
                    armR.xRot += -slashWave * 1.15F + Mth.sin(t * 0.12F) * 0.08F;
                    armR.yRot = -slashWave * 0.35F;
                }
                if (armL != null) {
                    armL.xRot += -slashWave * 1.15F + Mth.cos(t * 0.12F) * 0.08F;
                    armL.yRot = slashWave * 0.35F;
                }
            }

            pose.pushPose();
            pose.translate(0.0, Mth.sin(t * 0.12F) * 0.08F, 0.0);
            float yRot = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
            pose.mulPose(Axis.YP.rotationDegrees(180.0F - yRot));
            pose.scale(m.renderScale, m.renderScale, m.renderScale);

            VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX_MINION));
            int glowLight = Math.max(light, LightTexture.pack(11, 15));
            m.root.render(pose, vc, glowLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            pose.popPose();
            super.render(e, yaw, partial, pose, buf, light);
        }
    }

    public static final class Shield extends EntityRenderer<CrystalShieldEntity> {
        public Shield(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.0F;
        }

        @Override
        public ResourceLocation getTextureLocation(CrystalShieldEntity e) {
            return TEX_SHIELD;
        }

        @Override
        public void render(CrystalShieldEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
            GeoModel m = GeoModel.of("crystal_shield");
            if (m == null) return;
            m.animate(0.0F);
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(180.0F - e.shieldYaw()));
            float pulse = 1.0F + 0.025F * Mth.sin((e.tickCount + partial) * 0.25F);
            pose.scale(pulse, pulse, pulse);
            VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucent(TEX_SHIELD));
            m.root.render(pose, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 0.92F);
            pose.popPose();
        }
    }

    public static final class Shard extends EntityRenderer<CrystalShardEntity> {
        public Shard(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.0F;
        }

        @Override
        public ResourceLocation getTextureLocation(CrystalShardEntity e) {
            return TEX_SHARD;
        }

        @Override
        public void render(CrystalShardEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
            GeoModel m = GeoModel.of("crystal_shard");
            if (m == null) return;
            m.animate(0.0F);
            pose.pushPose();
            pose.translate(0.0, 0.15, 0.0);
            pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partial, e.yRotO, e.getYRot()) - 180.0F));
            pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partial, e.xRotO, e.getXRot())));
            pose.mulPose(Axis.ZP.rotationDegrees((e.tickCount + partial) * 24.0F));
            pose.scale(0.85F, 0.85F, 0.85F);
            VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX_SHARD));
            m.root.render(pose, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            pose.popPose();
        }
    }
}
