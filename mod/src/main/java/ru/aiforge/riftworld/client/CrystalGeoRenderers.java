package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.entity.CrystalMinion;
import ru.aiforge.riftworld.entity.CrystalShardEntity;
import ru.aiforge.riftworld.entity.CrystalShieldEntity;
import ru.aiforge.riftworld.entity.IceHailBoulderEntity;
import ru.aiforge.riftworld.entity.IcePillarEntity;
import ru.aiforge.riftworld.entity.RiftObeliskBoss;
import ru.aiforge.riftworld.glb.Pose;

/**
 * v0.19.1: Высокодетализированные .glb-рендереры с объёмными энергетическими потоками:
 *  - Босс Колоннады («Расколотый Обелиск Разлома» — строго БЕЗ тени человека, 1:1 по 01_colonnade_boss_ref.png),
 *  - Хрустальный Страж Колоннады (каменный лик голема с короной кристаллов, вихрем и парящими катар-когтями, 1:1 по 02_crystal_minions_ref.png),
 *  - Осколок Разлома и Бастион Граней.
 */
public final class CrystalGeoRenderers {
    public static final ResourceLocation BOSS_GLB   = new ResourceLocation(RiftWorld.MODID, "models/glb/rift_obelisk_boss.glb");
    public static final ResourceLocation MINION_GLB = new ResourceLocation(RiftWorld.MODID, "models/glb/crystal_minion.glb");
    public static final ResourceLocation SHARD_GLB  = new ResourceLocation(RiftWorld.MODID, "models/glb/crystal_shard.glb");
    public static final ResourceLocation SHIELD_GLB = new ResourceLocation(RiftWorld.MODID, "models/glb/crystal_shield.glb");
    public static final ResourceLocation KATAR_GLB  = new ResourceLocation(RiftWorld.MODID, "models/glb/crystal_katar.glb");
    public static final ResourceLocation PRISM_GLB  = new ResourceLocation(RiftWorld.MODID, "models/glb/astral_prism.glb");

    private static final ResourceLocation FALLBACK_TEX = new ResourceLocation("minecraft", "textures/misc/white.png");

    private CrystalGeoRenderers() {
    }

    /** Двусторонний аддитивный энергетический квад для RenderType.lightning(). */
    static void energyQuad(VertexConsumer vc, Matrix4f m,
                           float x0, float y0, float z0,
                           float x1, float y1, float z1,
                           float x2, float y2, float z2,
                           float x3, float y3, float z3,
                           int r0, int g0, int b0, int a0,
                           int r1, int g1, int b1, int a1) {
        vc.vertex(m, x0, y0, z0).color(r0, g0, b0, a0).endVertex();
        vc.vertex(m, x1, y1, z1).color(r0, g0, b0, a0).endVertex();
        vc.vertex(m, x2, y2, z2).color(r1, g1, b1, a1).endVertex();
        vc.vertex(m, x3, y3, z3).color(r1, g1, b1, a1).endVertex();

        vc.vertex(m, x3, y3, z3).color(r1, g1, b1, a1).endVertex();
        vc.vertex(m, x2, y2, z2).color(r1, g1, b1, a1).endVertex();
        vc.vertex(m, x1, y1, z1).color(r0, g0, b0, a0).endVertex();
        vc.vertex(m, x0, y0, z0).color(r0, g0, b0, a0).endVertex();
    }

    /** Объёмная 3D-лента вдоль кривой (скрещённые перпендикулярные квады для видимости под любым углом). */
    static void energySegment(VertexConsumer vc, Matrix4f m,
                              float ax, float ay, float az, float bx, float by, float bz,
                              float w0, float w1,
                              int r0, int g0, int b0, int a0,
                              int r1, int g1, int b1, int a1) {
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0e-4F) return;
        dx /= len; dy /= len; dz /= len;
        // Два ортогональных поперечных вектора u и v
        float ux = -dz, uy = 0.0F, uz = dx;
        float ul = Mth.sqrt(ux * ux + uz * uz);
        if (ul < 1.0e-3F) {
            ux = 1.0F; uy = 0.0F; uz = 0.0F;
        } else {
            ux /= ul; uz /= ul;
        }
        float vx = dy * uz - dz * uy;
        float vy = dz * ux - dx * uz;
        float vz = dx * uy - dy * ux;

        energyQuad(vc, m,
                ax - ux * w0, ay - uy * w0, az - uz * w0,
                ax + ux * w0, ay + uy * w0, az + uz * w0,
                bx + ux * w1, by + uy * w1, bz + uz * w1,
                bx - ux * w1, by - uy * w1, bz - uz * w1,
                r0, g0, b0, a0, r1, g1, b1, a1);
        energyQuad(vc, m,
                ax - vx * w0, ay - vy * w0, az - vz * w0,
                ax + vx * w0, ay + vy * w0, az + vz * w0,
                bx + vx * w1, by + vy * w1, bz + vz * w1,
                bx - vx * w1, by - vy * w1, bz - vz * w1,
                r0, g0, b0, a0, r1, g1, b1, a1);
    }

    // ==================== 1. БОСС КОЛОННАДЫ (БЕЗ ТЕНИ ЧЕЛОВЕКА!) ====================
    public static final class Boss extends EntityRenderer<RiftObeliskBoss> {
        public static final float BOSS_SCALE = 1.75F;
        private Pose pose;

        public Boss(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.0F;
        }

        @Override
        public boolean shouldRender(RiftObeliskBoss e, Frustum frustum, double cx, double cy, double cz) {
            AABB box = e.getBoundingBox().inflate(6.5, 5.5, 6.5);
            return frustum.isVisible(box);
        }

        @Override
        public void render(RiftObeliskBoss e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
            GlbAssets.Loaded lm = GlbAssets.get(BOSS_GLB);
            if (lm == null) return;
            if (this.pose == null || this.pose.model() != lm.model) {
                this.pose = new Pose(lm.model);
            }

            float age = e.tickCount + pt;
            boolean enraged = e.bossState() != RiftObeliskBoss.STATE_IDLE || e.getHealth() < e.getMaxHealth() * 0.5F;
            float speedMul = enraged ? 1.85F : 1.0F;
            float bob = Mth.sin(age * 0.065F) * 0.10F;

            this.pose.reset();
            // Величественное вращение верхнего каменного рунического кольца + лёгкий наклон прецессии
            this.pose.rotate("ring_top", 0.085F * Mth.sin(age * 0.04F), 1.0F, 0.0F, 0.0F);
            this.pose.rotate("ring_top", 0.065F * Mth.cos(age * 0.035F), 0.0F, 0.0F, 1.0F);
            this.pose.rotate("ring_top", age * 0.028F * speedMul, 0.0F, 1.0F, 0.0F);

            // Вращение двух наклонных спицевых колёс астролябии в собственных плоскостях (ось Z локально)
            this.pose.rotate("wheel_left",  age * 0.048F * speedMul, 0.0F, 0.0F, 1.0F);
            this.pose.rotate("wheel_right", -age * 0.048F * speedMul, 0.0F, 0.0F, 1.0F);

            // Дыхание расколотых половин мраморного обелиска и кристаллического ядра
            float breathe = 0.035F * Mth.sin(age * 0.08F);
            this.pose.translate("obelisk_upper", 0.0F, 0.055F * Mth.sin(age * 0.07F), 0.0F);
            this.pose.translate("obelisk_left",  +breathe, 0.0F, 0.0F);
            this.pose.translate("obelisk_right", -breathe, 0.0F, 0.0F);
            this.pose.rotate("prism_core", -age * 0.042F * speedMul, 0.0F, 1.0F, 0.0F);
            float corePulse = 1.0F + 0.055F * Mth.sin(age * 0.14F);
            this.pose.scale("prism_core", corePulse, corePulse, corePulse);

            // Орбита внешних тёмно-бирюзовых осколков
            this.pose.rotate("orbit_shards", age * 0.032F * speedMul, 0.0F, 1.0F, 0.0F);
            this.pose.compute();

            ps.pushPose();
            float bodyYaw = Mth.rotLerp(pt, e.yBodyRotO, e.yBodyRot);
            ps.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            ps.translate(0.0, bob, 0.0);
            ps.scale(BOSS_SCALE, BOSS_SCALE, BOSS_SCALE);

            int brightLight = Math.max(light, LightTexture.pack(12, 15));
            int overlay = e.hurtTime > 0 ? OverlayTexture.pack(OverlayTexture.u(1.0F), OverlayTexture.v(true)) : OverlayTexture.NO_OVERLAY;
            GlbRenderer.render(lm, this.pose, ps, buf, brightLight, overlay);

            // Объёмные бирюзово-белые потоки энергии Разлома (1:1 по 01_colonnade_boss_ref.png, строго без гуманоидного силуэта!)
            renderBossEnergyStreams(ps, buf, age, bob / BOSS_SCALE, enraged);

            ps.popPose();

            // Вращающиеся 4 морозно-призматических луча (Кастомная атака Босса #4) в мировых координатах!
            int fbt = e.frostBeamTicks();
            if (fbt > 0) {
                renderSweepingFrostBeams(ps, buf, e, fbt, pt);
            }
            super.render(e, yaw, pt, ps, buf, light);
        }

        private static void renderSweepingFrostBeams(PoseStack ps, MultiBufferSource buf, RiftObeliskBoss e, int fbt, float pt) {
            VertexConsumer vc = buf.getBuffer(RenderType.lightning());
            ps.pushPose();
            Matrix4f m = ps.last().pose();
            boolean telegraph = fbt > 48;
            float prog = telegraph ? 0.0F : ((48.0F - (fbt - pt)) / 48.0F);
            float baseDeg = e.frostBeamStartYaw() + prog * 115.0F;
            float len = 17.5F;
            float width = telegraph ? 0.10F : 0.34F;
            int alpha = telegraph ? 95 : 215;

            for (int arm = 0; arm < 4; ++arm) {
                float rad = (baseDeg + arm * 90.0F) * Mth.DEG_TO_RAD;
                float ex = Mth.cos(rad) * len;
                float ez = Mth.sin(rad) * len;
                energySegment(vc, m, 0.0F, 0.85F, 0.0F, ex, 0.85F, ez,
                        width, width * 0.55F,
                        205, 248, 255, alpha,
                        95, 215, 255, alpha / 2);
            }
            ps.popPose();
        }

        private static void renderBossEnergyStreams(PoseStack ps, MultiBufferSource buf, float age, float bob, boolean enraged) {
            VertexConsumer vc = buf.getBuffer(RenderType.lightning());
            Matrix4f m = ps.last().pose();
            float pulse = 0.82F + 0.18F * Mth.sin(age * 0.13F);
            int boost = enraged ? 22 : 0;

            // 1. Вертикальный столб призыва от печати Колоннады (y = -0.84 - bob) до нижнего кристалла обелиска (y = +0.25)
            float sealY = -0.82F - bob;
            energySegment(vc, m, 0.0F, sealY, 0.0F, 0.0F, 0.28F, 0.0F,
                    0.34F * pulse, 0.16F * pulse,
                    40, 235, 255, Math.min(255, 135 + boost),
                    195, 255, 255, Math.min(255, 175 + boost));
            energySegment(vc, m, 0.0F, sealY, 0.0F, 0.0F, 0.22F, 0.0F,
                    0.12F * pulse, 0.06F * pulse,
                    220, 255, 255, 210,
                    255, 255, 255, 225);

            // Горизонтальное сияющее кольцо над самой печатью на полу
            int segs = 16;
            float rIn = 0.42F, rOut = 1.05F + 0.08F * Mth.sin(age * 0.09F);
            for (int i = 0; i < segs; ++i) {
                float a0 = (Mth.TWO_PI * i) / segs + age * 0.02F;
                float a1 = (Mth.TWO_PI * (i + 1)) / segs + age * 0.02F;
                energyQuad(vc, m,
                        Mth.cos(a0) * rIn, sealY + 0.02F, Mth.sin(a0) * rIn,
                        Mth.cos(a1) * rIn, sealY + 0.02F, Mth.sin(a1) * rIn,
                        Mth.cos(a1) * rOut, sealY + 0.02F, Mth.sin(a1) * rOut,
                        Mth.cos(a0) * rOut, sealY + 0.02F, Mth.sin(a0) * rOut,
                        160, 255, 255, 130,
                        20, 210, 245, 0);
            }

            // 2. Геометрические лучи от центрального кристаллического октаэдра (0, 1.62, 0) к ступицам обоих колёс
            energySegment(vc, m, 0.0F, 1.62F, 0.0F, +1.58F, 1.56F, 0.16F,
                    0.10F * pulse, 0.045F,
                    180, 255, 255, 145, 35, 225, 250, 55);
            energySegment(vc, m, 0.0F, 1.62F, 0.0F, -1.58F, 1.56F, 0.16F,
                    0.10F * pulse, 0.045F,
                    180, 255, 255, 145, 35, 225, 250, 55);

            // 3. Две плавные восьмёркообразные ленты бирюзовой энергии, обвивающие оба наклонных колеса и верхнее кольцо!
            int steps = 28;
            for (int ribbon = 0; ribbon < 2; ++ribbon) {
                float phase = age * 0.055F + ribbon * Mth.PI;
                float prevX = 0, prevY = 0, prevZ = 0;
                for (int s = 0; s <= steps; ++s) {
                    float u = (float) s / steps;
                    float t = u * Mth.TWO_PI;
                    // Лемниската / пространственная восьмёрка вокруг левого и правого колёс + подъём к верхнему кольцу
                    float px = 2.08F * Mth.sin(t);
                    float py = 1.62F + 0.78F * Mth.sin(2.0F * t + 0.35F * (ribbon == 0 ? 1 : -1)) + 0.18F * Mth.cos(t + phase);
                    float pz = 0.58F * Mth.cos(t) * (ribbon == 0 ? 1.0F : -1.0F) + 0.22F * Mth.sin(2.0F * t + phase);
                    if (s > 0) {
                        float wave = 0.5F + 0.5F * Mth.sin(t * 2.0F - age * 0.14F + ribbon * 1.7F);
                        float w = (0.032F + 0.046F * wave) * pulse;
                        int alpha = (int) ((52 + 88 * wave + boost) * Mth.sin(u * Mth.PI));
                        if (alpha > 6) {
                            energySegment(vc, m, prevX, prevY, prevZ, px, py, pz, w, w,
                                    55, 240, 255, alpha,
                                    170, 255, 255, alpha);
                        }
                    }
                    prevX = px; prevY = py; prevZ = pz;
                }
            }
        }

        @Override
        public ResourceLocation getTextureLocation(RiftObeliskBoss e) {
            return FALLBACK_TEX;
        }
    }

    // ==================== 2. ХРУСТАЛЬНЫЙ СТРАЖ КОЛОННАДЫ ====================
    public static final class Minion extends EntityRenderer<CrystalMinion> {
        private Pose pose;

        public Minion(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.32F;
        }

        @Override
        public boolean shouldRender(CrystalMinion e, Frustum frustum, double cx, double cy, double cz) {
            return frustum.isVisible(e.getBoundingBox().inflate(1.5, 1.2, 1.5));
        }

        @Override
        public void render(CrystalMinion e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
            GlbAssets.Loaded lm = GlbAssets.get(MINION_GLB);
            if (lm == null) return;
            if (this.pose == null || this.pose.model() != lm.model) {
                this.pose = new Pose(lm.model);
            }

            float age = e.tickCount + pt;
            float bob = Mth.sin(age * 0.11F) * 0.075F;
            float headYawRad = Mth.clamp(Mth.wrapDegrees(Mth.rotLerp(pt, e.yHeadRotO, e.yHeadRot) - Mth.rotLerp(pt, e.yBodyRotO, e.yBodyRot)), -35.0F, 35.0F) * Mth.DEG_TO_RAD;
            float headPitchRad = Mth.lerp(pt, e.xRotO, e.getXRot()) * 0.45F * Mth.DEG_TO_RAD;

            this.pose.reset();
            this.pose.rotate("visage", headYawRad * 0.45F, 0.0F, 1.0F, 0.0F);
            this.pose.rotate("visage", headPitchRad * 0.45F, 1.0F, 0.0F, 0.0F);
            this.pose.translate("jaw", 0.0F, -0.018F * (0.5F + 0.5F * Mth.sin(age * 0.14F)), 0.0F);

            // Встречное вращение внешнего и внутреннего слоёв призрачного бирюзового вихря
            this.pose.rotate("wisp_outer", age * 0.095F, 0.0F, 1.0F, 0.0F);
            this.pose.rotate("wisp_inner", -age * 0.145F, 0.0F, 1.0F, 0.0F);

            // Плавная левитация мраморных наплечников
            float shBob = 0.032F * Mth.sin(age * 0.09F + 0.8F);
            this.pose.translate("shoulder_left",  0.0F, shBob, 0.0F);
            this.pose.translate("shoulder_right", 0.0F, shBob, 0.0F);

            // Независимая левитация и боевой взмах оторванных от тела катар-когтей!
            float swing = e.getAttackAnim(pt);
            float clawBobL = 0.055F * Mth.sin(age * 0.12F + 1.1F);
            float clawBobR = 0.055F * Mth.sin(age * 0.12F + 2.5F);
            float slash = Mth.sin(swing * Mth.PI);
            this.pose.translate("arm_left",  0.0F, clawBobL, slash * 0.28F);
            this.pose.translate("arm_right", 0.0F, clawBobR, slash * 0.28F);
            this.pose.rotate("arm_left",  -slash * 0.55F + 0.06F * Mth.sin(age * 0.10F), 1.0F, 0.0F, 0.0F);
            this.pose.rotate("arm_right", -slash * 0.55F + 0.06F * Mth.cos(age * 0.10F), 1.0F, 0.0F, 0.0F);
            this.pose.compute();

            ps.pushPose();
            float bodyYaw = Mth.rotLerp(pt, e.yBodyRotO, e.yBodyRot);
            ps.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            ps.translate(0.0, bob, 0.0);

            int brightLight = Math.max(light, LightTexture.pack(12, 15));
            int overlay = e.hurtTime > 0 ? OverlayTexture.pack(OverlayTexture.u(1.0F), OverlayTexture.v(true)) : OverlayTexture.NO_OVERLAY;
            GlbRenderer.render(lm, this.pose, ps, buf, brightLight, overlay);

            // Тонкие спиральные энергетические нити в вихревом хвосте и у парящих катар-наручей
            VertexConsumer vc = buf.getBuffer(RenderType.lightning());
            Matrix4f m = ps.last().pose();
            for (int w = 0; w < 3; ++w) {
                float baseA = age * 0.11F + w * (Mth.TWO_PI / 3.0F);
                int steps = 10;
                float px = 0, py = 0, pz = 0;
                for (int s = 0; s <= steps; ++s) {
                    float u = (float) s / steps;
                    float r = 0.24F * (1.0F - u * 0.88F);
                    float a = baseA + u * 3.4F;
                    float x = Mth.cos(a) * r;
                    float y = 0.98F - u * 0.90F;
                    float z = Mth.sin(a) * r;
                    if (s > 0) {
                        int al = (int) (105 * Mth.sin(u * Mth.PI));
                        energySegment(vc, m, px, py, pz, x, y, z, 0.028F * (1.0F - u * 0.5F), 0.022F * (1.0F - u * 0.5F),
                                65, 245, 255, al, 185, 255, 255, al);
                    }
                    px = x; py = y; pz = z;
                }
            }

            ps.popPose();
            super.render(e, yaw, pt, ps, buf, light);
        }

        @Override
        public ResourceLocation getTextureLocation(CrystalMinion e) {
            return FALLBACK_TEX;
        }
    }

    // ==================== 3. СТРЕМИТЕЛЬНЫЙ КРИСТАЛЬНЫЙ ОСКОЛОК ====================
    public static final class Shard extends EntityRenderer<CrystalShardEntity> {
        public Shard(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.0F;
        }

        @Override
        public void render(CrystalShardEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
            GlbAssets.Loaded lm = GlbAssets.get(SHARD_GLB);
            if (lm == null) return;

            ps.pushPose();
            ps.translate(0.0, 0.16, 0.0);
            Vec3 v = e.getDeltaMovement();
            double hDist = Math.hypot(v.x, v.z);
            if (v.lengthSqr() > 1.0E-4) {
                float vyaw = (float) (Math.atan2(v.x, v.z) * (180.0 / Math.PI));
                float vpitch = (float) (-Math.atan2(v.y, hDist) * (180.0 / Math.PI));
                ps.mulPose(Axis.YP.rotationDegrees(vyaw));
                ps.mulPose(Axis.XP.rotationDegrees(vpitch));
            } else {
                ps.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
            }
            ps.mulPose(Axis.ZP.rotationDegrees((e.tickCount + pt) * 20.0F));

            GlbRenderer.render(lm, lm.staticPose(), ps, buf, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }

        @Override
        public ResourceLocation getTextureLocation(CrystalShardEntity e) {
            return FALLBACK_TEX;
        }
    }

    // ==================== 4. 3D КРИСТАЛЬНЫЙ ЩИТ («БАСТИОН ГРАНЕЙ») ====================
    public static final class Shield extends EntityRenderer<CrystalShieldEntity> {
        public Shield(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.0F;
        }

        @Override
        public boolean shouldRender(CrystalShieldEntity e, Frustum frustum, double cx, double cy, double cz) {
            return frustum.isVisible(e.getBoundingBox().inflate(1.2));
        }

        @Override
        public void render(CrystalShieldEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
            GlbAssets.Loaded lm = GlbAssets.get(SHIELD_GLB);
            if (lm == null) return;

            ps.pushPose();
            float renderYaw = e.shieldYaw();
            ps.translate(0.0, 1.05, 0.0);
            ps.mulPose(Axis.YP.rotationDegrees(-renderYaw));
            ps.mulPose(Axis.XP.rotationDegrees(e.getXRot() * 0.45F));
            float pulse = 1.0F + 0.025F * Mth.sin((e.tickCount + pt) * 0.18F);
            ps.scale(1.08F * pulse, 1.08F * pulse, 1.08F * pulse);

            GlbRenderer.render(lm, lm.staticPose(), ps, buf, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }

        @Override
        public ResourceLocation getTextureLocation(CrystalShieldEntity e) {
            return FALLBACK_TEX;
        }
    }

    private static final ResourceLocation BLUE_ICE_TEX = new ResourceLocation("minecraft", "textures/block/blue_ice.png");
    private static final ResourceLocation PACKED_ICE_TEX = new ResourceLocation("minecraft", "textures/block/packed_ice.png");

    // ==================== 5. ЛЕДЯНОЙ СТОЛБ ИЗ-ПОД ЗЕМЛИ (КАСТОМНЫЕ АТАКИ БОССА #1 И #3) ====================
    public static final class IcePillar extends EntityRenderer<IcePillarEntity> {
        public IcePillar(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.0F;
        }

        @Override
        public boolean shouldRender(IcePillarEntity e, Frustum frustum, double cx, double cy, double cz) {
            return e.distanceToSqr(cx, cy, cz) < 160.0 * 160.0;
        }

        @Override
        public void render(IcePillarEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
            float t = e.pillarAge() + pt;
            float h = e.computeHeight(t);
            boolean wave = e.isWaveSpike();
            float baseW = wave ? 0.56F : 0.84F;
            int bright = Math.max(light, LightTexture.pack(13, 15));

            ps.pushPose();
            // 1) Морозное кольцо разлома на земле вокруг основания столба
            float ringFade = Mth.clamp(1.0F - Math.max(0.0F, t - (e.totalLifetime() - 14.0F)) / 14.0F, 0.0F, 1.0F);
            if (ringFade > 0.03F) {
                VertexConsumer lvc = buf.getBuffer(RenderType.lightning());
                Matrix4f lm = ps.last().pose();
                int segs = 12;
                float rIn = baseW * 0.72F, rOut = baseW * 1.48F;
                int ra = (int) (145 * ringFade);
                for (int i = 0; i < segs; ++i) {
                    float a0 = (Mth.TWO_PI * i) / segs;
                    float a1 = (Mth.TWO_PI * (i + 1)) / segs;
                    lvc.vertex(lm, Mth.cos(a0) * rIn, 0.035F, Mth.sin(a0) * rIn).color(210, 248, 255, ra).endVertex();
                    lvc.vertex(lm, Mth.cos(a1) * rIn, 0.035F, Mth.sin(a1) * rIn).color(210, 248, 255, ra).endVertex();
                    lvc.vertex(lm, Mth.cos(a1) * rOut, 0.035F, Mth.sin(a1) * rOut).color(110, 210, 250, 0).endVertex();
                    lvc.vertex(lm, Mth.cos(a0) * rOut, 0.035F, Mth.sin(a0) * rOut).color(110, 210, 250, 0).endVertex();
                }
            }

            if (h > 0.03F) {
                ps.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
                // 2) Ступенчатый гранёно-блочный ледяной столб (плавно выходит снизу вверх, поэтому верхняя площадка всегда ровно на y = h!)
                VertexConsumer blueVc = buf.getBuffer(RenderType.entitySolid(BLUE_ICE_TEX));
                VertexConsumer packVc = buf.getBuffer(RenderType.entitySolid(PACKED_ICE_TEX));

                // Центральный монолитный ствол (ровная площадка на вершине y = h, чтобы игрок устойчиво стоял сверху!)
                drawIceBox(ps, blueVc, -baseW, 0.0F, -baseW, baseW, h, baseW, 242, 250, 255, bright);

                // Острые боковые квадратные ледяные контрфорсы и зубцы вокруг ствола (чуть ниже вершины h, чтобы не мешать стоять сверху)
                float buttressH1 = Math.max(0.0F, h - (wave ? 0.25F : 0.65F));
                float buttressH2 = Math.max(0.0F, h - (wave ? 0.55F : 1.35F));
                if (buttressH1 > 0.08F) {
                    ps.pushPose();
                    ps.mulPose(Axis.YP.rotationDegrees(45.0F));
                    float bw = baseW * 0.86F;
                    drawIceBox(ps, packVc, -bw, 0.0F, -bw, bw, buttressH1, bw, 226, 242, 255, bright);
                    ps.popPose();
                }
                if (buttressH2 > 0.08F) {
                    float sw = baseW * 0.44F;
                    drawIceBox(ps, packVc, -baseW * 1.24F, 0.0F, -sw, -baseW * 0.55F, buttressH2, sw, 232, 246, 255, bright);
                    drawIceBox(ps, packVc, baseW * 0.55F, 0.0F, -sw, baseW * 1.24F, buttressH2 * 0.88F, sw, 232, 246, 255, bright);
                    drawIceBox(ps, packVc, -sw, 0.0F, -baseW * 1.24F, sw, buttressH2 * 0.92F, -baseW * 0.55F, 220, 238, 255, bright);
                    drawIceBox(ps, packVc, -sw, 0.0F, baseW * 0.55F, sw, buttressH2 * 0.82F, baseW * 1.24F, 220, 238, 255, bright);
                }
            }
            ps.popPose();
        }

        @Override
        public ResourceLocation getTextureLocation(IcePillarEntity e) {
            return BLUE_ICE_TEX;
        }
    }

    // ==================== 6. ОГРОМНАЯ ОСТРАЯ КВАДРАТНАЯ ЛЕДЯНАЯ ГЛЫБА + МЕТКА НА ЗЕМЛЕ (АТАКА #2) ====================
    public static final class IceHailBoulder extends EntityRenderer<IceHailBoulderEntity> {
        public IceHailBoulder(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.0F;
        }

        @Override
        public boolean shouldRender(IceHailBoulderEntity e, Frustum frustum, double cx, double cy, double cz) {
            return e.distanceToSqr(cx, cy, cz) < 190.0 * 190.0;
        }

        @Override
        public void render(IceHailBoulderEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
            float age = e.boulderAge() + pt;
            Vec3 curPos = new Vec3(
                    Mth.lerp(pt, e.xo, e.getX()),
                    Mth.lerp(pt, e.yo, e.getY()),
                    Mth.lerp(pt, e.zo, e.getZ()));
            Vec3 target = e.targetGround();
            boolean impacted = e.hasImpacted();
            int bright = LightTexture.FULL_BRIGHT;

            // 1) СНАЧАЛА И ДО ПРИЛЁТА РИСУЕМ НА ЗЕМЛЕ ЯРКУЮ ЛЕДЯНУЮ МЕТКУ ТОЧКИ УДАРА (target - curPos)!
            float markFade = impacted
                    ? Mth.clamp(1.0F - (age - e.impactAge()) / 10.0F, 0.0F, 1.0F)
                    : Mth.clamp(age / 8.0F, 0.15F, 1.0F);
            if (markFade > 0.02F) {
                ps.pushPose();
                ps.translate(target.x - curPos.x, (target.y + 0.045) - curPos.y, target.z - curPos.z);
                VertexConsumer lvc = buf.getBuffer(RenderType.lightning());
                Matrix4f m = ps.last().pose();

                float pulse = 0.90F + 0.10F * Mth.sin(age * 0.32F);
                float outerR = 2.45F * pulse;
                float innerR = 1.95F * pulse;
                int aRing = (int) (215 * markFade);

                // Квадратно-ромбовидный ледяной прицел + внутреннее сходящееся кольцо до момента броска
                int segs = 16;
                for (int i = 0; i < segs; ++i) {
                    float a0 = (Mth.TWO_PI * i) / segs + age * 0.025F;
                    float a1 = (Mth.TWO_PI * (i + 1)) / segs + age * 0.025F;
                    lvc.vertex(m, Mth.cos(a0) * innerR, 0.0F, Mth.sin(a0) * innerR).color(220, 250, 255, aRing).endVertex();
                    lvc.vertex(m, Mth.cos(a1) * innerR, 0.0F, Mth.sin(a1) * innerR).color(220, 250, 255, aRing).endVertex();
                    lvc.vertex(m, Mth.cos(a1) * outerR, 0.0F, Mth.sin(a1) * outerR).color(85, 205, 255, 40).endVertex();
                    lvc.vertex(m, Mth.cos(a0) * outerR, 0.0F, Mth.sin(a0) * outerR).color(85, 205, 255, 40).endVertex();
                }
                // Острое перекрестие и квадратная рамка прицела на земле
                float cr = 2.2F;
                float cw = 0.14F;
                lvc.vertex(m, -cr, 0.01F, -cw).color(245, 253, 255, aRing).endVertex();
                lvc.vertex(m, -cr, 0.01F, cw).color(245, 253, 255, aRing).endVertex();
                lvc.vertex(m, cr, 0.01F, cw).color(245, 253, 255, aRing).endVertex();
                lvc.vertex(m, cr, 0.01F, -cw).color(245, 253, 255, aRing).endVertex();

                lvc.vertex(m, -cw, 0.01F, -cr).color(245, 253, 255, aRing).endVertex();
                lvc.vertex(m, cw, 0.01F, -cr).color(245, 253, 255, aRing).endVertex();
                lvc.vertex(m, cw, 0.01F, cr).color(245, 253, 255, aRing).endVertex();
                lvc.vertex(m, -cw, 0.01F, cr).color(245, 253, 255, aRing).endVertex();

                // Тонкий вертикальный луч-маячок над точкой падения, чтобы игрок мгновенно видел все 5 точек!
                float beaconH = impacted ? 0.6F : 3.2F;
                lvc.vertex(m, -0.09F, 0.0F, 0.0F).color(230, 252, 255, aRing).endVertex();
                lvc.vertex(m, 0.09F, 0.0F, 0.0F).color(230, 252, 255, aRing).endVertex();
                lvc.vertex(m, 0.04F, beaconH, 0.0F).color(120, 215, 255, 0).endVertex();
                lvc.vertex(m, -0.04F, beaconH, 0.0F).color(120, 215, 255, 0).endVertex();
                ps.popPose();
            }

            // 2) САМА ОГРОМНАЯ ОСТРАЯ КВАДРАТНАЯ ЛЕДЯНАЯ ГЛЫБА В НЕБЕ / В ПОЛЁТЕ!
            float spawnScale = Mth.clamp(age / 10.0F, 0.05F, 1.0F);
            if (impacted) {
                spawnScale *= Mth.clamp(1.0F - (age - e.impactAge()) / 8.0F, 0.0F, 1.0F);
            }
            if (spawnScale <= 0.03F) return;

            ps.pushPose();
            ps.scale(spawnScale, spawnScale, spawnScale);
            float spinSpeed = (age >= e.launchTick()) ? 14.0F : 3.2F;
            ps.mulPose(Axis.YP.rotationDegrees(e.getYRot() + age * spinSpeed));
            ps.mulPose(Axis.XP.rotationDegrees(28.0F + age * spinSpeed * 0.65F));
            ps.mulPose(Axis.ZP.rotationDegrees(19.0F));

            VertexConsumer blueVc = buf.getBuffer(RenderType.entitySolid(BLUE_ICE_TEX));
            VertexConsumer packVc = buf.getBuffer(RenderType.entitySolid(PACKED_ICE_TEX));

            // Крупный квадратный центральный блок льда (1.7 x 1.7 x 1.7 м)
            drawIceBox(ps, blueVc, -0.85F, -0.85F, -0.85F, 0.85F, 0.85F, 0.85F, 240, 250, 255, bright);

            // Второй врезанный под 45° угловатый квадратный блок для острых рёбер!
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(45.0F));
            ps.mulPose(Axis.XP.rotationDegrees(45.0F));
            drawIceBox(ps, packVc, -0.76F, -0.76F, -0.76F, 0.76F, 0.76F, 0.76F, 224, 242, 255, bright);
            ps.popPose();

            // 3 длинных пересекающихся квадратных ледяных бруска-шипа по осям X, Y, Z ("острых и квадратненьких"!)
            drawIceBox(ps, blueVc, -1.48F, -0.34F, -0.34F, 1.48F, 0.34F, 0.34F, 248, 253, 255, bright);
            drawIceBox(ps, packVc, -0.34F, -1.56F, -0.34F, 0.34F, 1.56F, 0.34F, 248, 253, 255, bright);
            drawIceBox(ps, blueVc, -0.34F, -0.34F, -1.48F, 0.34F, 0.34F, 1.48F, 232, 246, 255, bright);
            // Ещё более тонкие острые квадратные кончики шипов!
            drawIceBox(ps, packVc, -1.86F, -0.16F, -0.16F, 1.86F, 0.16F, 0.16F, 255, 255, 255, bright);
            drawIceBox(ps, blueVc, -0.16F, -1.95F, -0.16F, 0.16F, 1.95F, 0.16F, 255, 255, 255, bright);
            drawIceBox(ps, packVc, -0.16F, -0.16F, -1.86F, 0.16F, 0.16F, 1.86F, 255, 255, 255, bright);

            ps.popPose();
        }

        @Override
        public ResourceLocation getTextureLocation(IceHailBoulderEntity e) {
            return BLUE_ICE_TEX;
        }
    }

    private static void drawIceBox(PoseStack ps, VertexConsumer vc,
                                   float x0, float y0, float z0, float x1, float y1, float z1,
                                   int r, int g, int b, int light) {
        Matrix4f m = ps.last().pose();
        org.joml.Matrix3f n = ps.last().normal();
        int ov = OverlayTexture.NO_OVERLAY;
        float uSpan = Math.min(1.0F, Math.max(0.25F, (x1 - x0) * 0.5F));
        float vSpan = Math.min(1.0F, Math.max(0.25F, (y1 - y0) * 0.5F));

        // UP (+Y)
        iceQuad(vc, m, n, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, uSpan, uSpan, r, g, b, light, ov);
        // DOWN (-Y)
        iceQuad(vc, m, n, 0, -1, 0, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, uSpan, uSpan, (int) (r * 0.84F), (int) (g * 0.86F), b, light, ov);
        // SOUTH (+Z)
        iceQuad(vc, m, n, 0, 0, 1, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1, uSpan, vSpan, (int) (r * 0.94F), (int) (g * 0.96F), b, light, ov);
        // NORTH (-Z)
        iceQuad(vc, m, n, 0, 0, -1, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0, uSpan, vSpan, (int) (r * 0.94F), (int) (g * 0.96F), b, light, ov);
        // EAST (+X)
        iceQuad(vc, m, n, 1, 0, 0, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0, uSpan, vSpan, (int) (r * 0.89F), (int) (g * 0.92F), b, light, ov);
        // WEST (-X)
        iceQuad(vc, m, n, -1, 0, 0, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1, uSpan, vSpan, (int) (r * 0.89F), (int) (g * 0.92F), b, light, ov);
    }

    private static void iceQuad(VertexConsumer vc, Matrix4f m, org.joml.Matrix3f n,
                                float nx, float ny, float nz,
                                float x0, float y0, float z0,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float u1, float v1, int r, int g, int b, int light, int ov) {
        vc.vertex(m, x0, y0, z0).color(r, g, b, 255).uv(0.0F, 0.0F).overlayCoords(ov).uv2(light).normal(n, nx, ny, nz).endVertex();
        vc.vertex(m, x1, y1, z1).color(r, g, b, 255).uv(0.0F, v1).overlayCoords(ov).uv2(light).normal(n, nx, ny, nz).endVertex();
        vc.vertex(m, x2, y2, z2).color(r, g, b, 255).uv(u1, v1).overlayCoords(ov).uv2(light).normal(n, nx, ny, nz).endVertex();
        vc.vertex(m, x3, y3, z3).color(r, g, b, 255).uv(u1, 0.0F).overlayCoords(ov).uv2(light).normal(n, nx, ny, nz).endVertex();
    }
}

