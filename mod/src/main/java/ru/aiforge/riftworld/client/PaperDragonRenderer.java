package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.entity.PaperDragonEntity;

/**
 * v0.24.0: Enlarged (~47.7m), butter-smooth Modular 3D Origami Dragon Renderer.
 * <ul>
 *   <li>Interpolates directly across the persistent 72-node arc-length-locked spine chain
 *       ({@code prevChain -> chain}) and smoothed head forward vector — 0 jitter, 0 vanishing head/points!</li>
 *   <li>Uses non-degenerate midpoint-split quads ({@code p1, p2, mid(p2,p3), p3}) so zero triangles
 *       are ever dropped by GPU/Mesa rasterizers or shaderpacks!</li>
 *   <li>1.62x enlarged scale (chest diameter ~2.48m, 71 rings × 14 Golden-Venture folded paper pockets = 994 modules).</li>
 * </ul>
 */
public class PaperDragonRenderer extends EntityRenderer<PaperDragonEntity> {
    private static final ResourceLocation TEX = new ResourceLocation(RiftWorld.MODID, "textures/entity/paper_dragon.png");

    private static final int SEGS = PaperDragonEntity.SEGS;
    private static final int MODULES_PER_RING = 14;
    /** 1.62x enlargement over v0.22–v0.23 -> ~47.7m colossal celestial origami dragon! */
    private static final float SCALE = 1.62F;

    private static final int FORELEG_SEG = 15;
    private static final int HINDLEG_SEG = 44;

    private static final int CR_R = 252, CR_G = 249, CR_B = 241;
    private static final int FL_R = 240, FL_G = 234, FL_B = 221;
    private static final int SH_R = 219, SH_G = 211, SH_B = 195;
    private static final int IN_R = 200, IN_G = 191, IN_B = 175;
    private static final int EYE_R = 46, EYE_G = 40, EYE_B = 38;

    public PaperDragonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(PaperDragonEntity e) {
        return TEX;
    }

    @Override
    public boolean shouldRender(PaperDragonEntity e, Frustum frustum, double camX, double camY, double camZ) {
        return e.distanceToSqr(camX, camY, camZ) < 384.0 * 384.0;
    }

    private static float bodyRadius(int seg) {
        float u = (float) seg / (float) (SEGS - 1);
        float base;
        if (u < 0.08F) {
            base = Mth.lerp(u / 0.08F, 0.56F, 0.68F);
        } else if (u < 0.24F) {
            float k = (u - 0.08F) / 0.16F;
            base = Mth.lerp(k * k * (3.0F - 2.0F * k), 0.68F, 0.77F);
        } else if (u < 0.64F) {
            float k = (u - 0.24F) / 0.40F;
            base = Mth.lerp(k, 0.77F, 0.60F);
        } else {
            float k = (u - 0.64F) / 0.36F;
            base = Mth.lerp(k * k, 0.60F, 0.16F);
        }
        return base * SCALE;
    }

    @Override
    public void render(PaperDragonEntity e, float entityYaw, float pt, PoseStack ps, MultiBufferSource buf, int packedLight) {
        int blockL = Math.max(LightTexture.block(packedLight), 10);
        int skyL = Math.max(LightTexture.sky(packedLight), 14);
        int warmLight = LightTexture.pack(blockL, skyL);
        int ov = OverlayTexture.NO_OVERLAY;

        float time = e.tickCount + pt;
        float phase = e.wavePhase();
        float headRollDeg = Mth.lerp(pt, e.bankRollPrev, e.bankRoll);
        float jawOpen = Mth.lerp(pt, e.jawAnimPrev, e.jawAnim);
        float dive = Mth.lerp(pt, e.diveBlendPrev, e.diveBlend);
        float coil = Mth.lerp(pt, e.coilBlendPrev, e.coilBlend);
        int whiskerRgb = e.whiskerColorRgb();
        int saddleTier = e.saddleTier();

        double headWx = Mth.lerp(pt, e.xo, e.getX());
        double headWy = Mth.lerp(pt, e.yo, e.getY());
        double headWz = Mth.lerp(pt, e.zo, e.getZ());

        Vec3[] spine = new Vec3[SEGS];
        float[] segRoll = new float[SEGS];
        Vec3 headForward = samplePersistentChain(e, pt, headWx, headWy, headWz, headRollDeg, spine, segRoll);

        // Parallel-transport (Bishop) frames along the 72-node spine — zero sudden roll flips at steep pitch!
        Vec3[] tangents = new Vec3[SEGS];
        Vec3[] normals = new Vec3[SEGS];
        Vec3[] binormals = new Vec3[SEGS];

        tangents[0] = headForward;
        for (int i = 1; i < SEGS; ++i) {
            Vec3 dir = (i < SEGS - 1)
                    ? spine[i - 1].subtract(spine[i + 1])
                    : spine[i - 1].subtract(spine[i]);
            if (dir.lengthSqr() < 1.0E-6) {
                tangents[i] = tangents[i - 1];
            } else {
                tangents[i] = dir.normalize();
            }
        }

        // Initial orthonormal frame at neck (i = 0)
        Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
        Vec3 baseSide0 = tangents[0].cross(worldUp);
        if (baseSide0.lengthSqr() < 1.0E-4) {
            baseSide0 = new Vec3(1.0, 0.0, 0.0);
        } else {
            baseSide0 = baseSide0.normalize();
        }
        Vec3 baseUp0 = baseSide0.cross(tangents[0]).normalize();

        Vec3 prevUpRef = baseUp0;
        for (int i = 0; i < SEGS; ++i) {
            Vec3 fw = tangents[i];
            // Parallel-transport prevUpRef onto plane perpendicular to fw, gently relaxed toward horizon up
            Vec3 upProj = prevUpRef.subtract(fw.scale(prevUpRef.dot(fw)));
            Vec3 worldUpProj = worldUp.subtract(fw.scale(worldUp.dot(fw)));
            if (worldUpProj.lengthSqr() > 1.0E-3) {
                upProj = upProj.scale(0.84).add(worldUpProj.normalize().scale(0.16));
            }
            if (upProj.lengthSqr() < 1.0E-5) {
                upProj = prevUpRef;
            } else {
                upProj = upProj.normalize();
            }
            prevUpRef = upProj;

            Vec3 sideRef = fw.cross(upProj);
            if (sideRef.lengthSqr() < 1.0E-5) sideRef = baseSide0;
            else sideRef = sideRef.normalize();

            float rRad = segRoll[i] * Mth.DEG_TO_RAD;
            float cr = Mth.cos(rRad);
            float sr = Mth.sin(rRad);
            normals[i] = upProj.scale(cr).add(sideRef.scale(sr)).normalize();
            binormals[i] = sideRef.scale(cr).subtract(upProj.scale(sr)).normalize();
        }

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX));
        ps.pushPose();
        ps.translate(0.0, e.getBbHeight() * 0.45, 0.0);
        Matrix4f pose = ps.last().pose();
        Matrix3f normMat = ps.last().normal();

        renderInnerPaperTube(vc, pose, normMat, spine, normals, binormals, warmLight, ov);
        renderModularOrigamiScales(vc, pose, normMat, spine, tangents, normals, binormals, time, phase, warmLight, ov);
        renderDorsalOrigamiCrest(vc, pose, normMat, spine, tangents, normals, binormals, time, phase, dive, warmLight, ov);

        if (saddleTier > 0) {
            renderOrigamiDragonSaddle(vc, pose, normMat, spine, tangents, normals, binormals, saddleTier, warmLight, ov);
        }

        renderOrigamiDragonHead(vc, pose, normMat,
                spine[0], tangents[0], normals[0], binormals[0],
                jawOpen, time, phase, dive, coil, whiskerRgb, warmLight, ov);

        renderArticulatedOrigamiPairOfLegs(vc, pose, normMat,
                spine[FORELEG_SEG], tangents[FORELEG_SEG], normals[FORELEG_SEG], binormals[FORELEG_SEG],
                bodyRadius(FORELEG_SEG), true, time, phase, segRoll[FORELEG_SEG], dive, coil, warmLight, ov);
        renderArticulatedOrigamiPairOfLegs(vc, pose, normMat,
                spine[HINDLEG_SEG], tangents[HINDLEG_SEG], normals[HINDLEG_SEG], binormals[HINDLEG_SEG],
                bodyRadius(HINDLEG_SEG), false, time, phase + 1.8F, segRoll[HINDLEG_SEG], dive, coil, warmLight, ov);

        renderImperialOrigamiTailFan(vc, pose, normMat,
                spine[SEGS - 1], tangents[SEGS - 1], normals[SEGS - 1], binormals[SEGS - 1],
                time, phase, dive, warmLight, ov);

        ps.popPose();
        super.render(e, entityYaw, pt, ps, buf, packedLight);
    }

    /**
     * Interpolates the persistent 72-node client chain at partialTick {@code pt} and returns
     * the guaranteed unit-length head forward vector.
     */
    private static Vec3 samplePersistentChain(PaperDragonEntity e, float pt,
                                              double headWx, double headWy, double headWz, float headRollDeg,
                                              Vec3[] outSpine, float[] outRoll) {
        if (!e.chainInit) {
            float yawRad = Mth.rotLerp(pt, e.yRotO, e.getYRot()) * Mth.DEG_TO_RAD;
            float pitchRad = Mth.clamp(Mth.lerp(pt, e.xRotO, e.getXRot()), -30.0F, 30.0F) * Mth.DEG_TO_RAD;
            Vec3 fw = new Vec3(-Mth.sin(yawRad) * Mth.cos(pitchRad), -Mth.sin(pitchRad), Mth.cos(yawRad) * Mth.cos(pitchRad));
            if (fw.lengthSqr() < 1.0E-5) fw = new Vec3(0.0, 0.0, 1.0);
            else fw = fw.normalize();
            for (int i = 0; i < SEGS; ++i) {
                outSpine[i] = fw.scale(-i * PaperDragonEntity.STEP);
                outRoll[i] = headRollDeg * (1.0F - 0.35F * ((float) i / (SEGS - 1)));
            }
            return fw;
        }

        double c0x = Mth.lerp(pt, e.prevChainX[0], e.chainX[0]);
        double c0y = Mth.lerp(pt, e.prevChainY[0], e.chainY[0]);
        double c0z = Mth.lerp(pt, e.prevChainZ[0], e.chainZ[0]);

        outSpine[0] = Vec3.ZERO;
        outRoll[0] = headRollDeg;
        for (int i = 1; i < SEGS; ++i) {
            double sx = Mth.lerp(pt, e.prevChainX[i], e.chainX[i]) - c0x;
            double sy = Mth.lerp(pt, e.prevChainY[i], e.chainY[i]) - c0y;
            double sz = Mth.lerp(pt, e.prevChainZ[i], e.chainZ[i]) - c0z;
            outSpine[i] = new Vec3(sx, sy, sz);
            outRoll[i] = Mth.lerp(pt, e.prevChainRoll[i], e.chainRoll[i]);
        }

        // Smooth unit head forward vector blended seamlessly with neck chord (0->1)
        Vec3 hFw = new Vec3(
                Mth.lerp(pt, e.prevHeadFx, e.headFx),
                Mth.lerp(pt, e.prevHeadFy, e.headFy),
                Mth.lerp(pt, e.prevHeadFz, e.headFz));
        Vec3 neckChord = outSpine[0].subtract(outSpine[1]);
        if (neckChord.lengthSqr() > 1.0E-4) {
            neckChord = neckChord.normalize();
            hFw = hFw.scale(0.65).add(neckChord.scale(0.35));
        }
        if (hFw.lengthSqr() < 1.0E-5) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return hFw.normalize();
    }

    private static void renderInnerPaperTube(VertexConsumer vc, Matrix4f pose, Matrix3f normMat,
                                             Vec3[] spine, Vec3[] normals, Vec3[] binormals,
                                             int light, int ov) {
        int tubeSides = 10;
        for (int i = 0; i < SEGS - 1; ++i) {
            float r0 = bodyRadius(i) * 0.78F;
            float r1 = bodyRadius(i + 1) * 0.78F;
            Vec3 c0 = spine[i], c1 = spine[i + 1];
            Vec3 u0 = normals[i], s0 = binormals[i];
            Vec3 u1 = normals[i + 1], s1 = binormals[i + 1];

            for (int k = 0; k < tubeSides; ++k) {
                float a0 = (Mth.TWO_PI * k) / tubeSides;
                float a1 = (Mth.TWO_PI * (k + 1)) / tubeSides;
                Vec3 p00 = ringPt(c0, u0, s0, a0, r0);
                Vec3 p01 = ringPt(c0, u0, s0, a1, r0);
                Vec3 p11 = ringPt(c1, u1, s1, a1, r1);
                Vec3 p10 = ringPt(c1, u1, s1, a0, r1);
                paperQuadSingle(vc, pose, normMat, p00, p01, p11, p10, IN_R, IN_G, IN_B, light, ov);
            }
        }
    }

    private static void renderModularOrigamiScales(VertexConsumer vc, Matrix4f pose, Matrix3f normMat,
                                                   Vec3[] spine, Vec3[] tangents, Vec3[] normals, Vec3[] binormals,
                                                   float time, float phase, int light, int ov) {
        for (int i = 0; i < SEGS - 1; ++i) {
            float r0 = bodyRadius(i);
            float r1 = bodyRadius(i + 1);
            Vec3 c0 = spine[i];
            Vec3 c1 = spine[i + 1];
            Vec3 t0 = tangents[i];
            Vec3 u0 = normals[i], s0 = binormals[i];
            Vec3 u1 = normals[i + 1], s1 = binormals[i + 1];

            float ringStagger = (i & 1) * (Mth.PI / MODULES_PER_RING);
            float halfSlot = (Mth.PI / MODULES_PER_RING) * 0.96F;
            float breath = 1.0F + 0.035F * Mth.sin(time * 0.085F - i * 0.22F + phase);

            for (int m = 0; m < MODULES_PER_RING; ++m) {
                float angMid = ringStagger + (Mth.TWO_PI * m) / MODULES_PER_RING;
                float radialScale = 1.0F - 0.10F * Math.max(0.0F, -Mth.cos(angMid));

                float radBase = r0 * radialScale;
                float radTip = (r1 * radialScale * 0.88F + r0 * 0.34F * breath);

                Vec3 baseLeft = ringPt(c0, u0, s0, angMid - halfSlot, radBase * 0.90F);
                Vec3 baseRight = ringPt(c0, u0, s0, angMid + halfSlot, radBase * 0.90F);
                Vec3 ridgeMid = ringPt(c0, u0, s0, angMid, radBase * 1.14F).subtract(t0.scale(PaperDragonEntity.STEP * 0.18));
                Vec3 tipApex = ringPt(c1, u1, s1, angMid, radTip).subtract(t0.scale(PaperDragonEntity.STEP * 0.42));
                Vec3 innerTuck = ringPt(c1, u1, s1, angMid, r1 * radialScale * 0.78F);

                paperTriSingle(vc, pose, normMat,
                        baseLeft, ridgeMid, tipApex,
                        0.05F, 0.06F, 0.35F, 0.50F, 0.98F, 0.50F,
                        FL_R, FL_G, FL_B, CR_R, CR_G, CR_B, CR_R, CR_G, CR_B, light, ov);
                paperTriSingle(vc, pose, normMat,
                        ridgeMid, baseRight, tipApex,
                        0.35F, 0.50F, 0.05F, 0.94F, 0.98F, 0.50F,
                        CR_R, CR_G, CR_B, SH_R, SH_G, SH_B, FL_R, FL_G, FL_B, light, ov);

                paperTriSingle(vc, pose, normMat,
                        baseLeft, tipApex, innerTuck,
                        0.10F, 0.08F, 0.96F, 0.50F, 0.25F, 0.50F,
                        SH_R, SH_G, SH_B, FL_R, FL_G, FL_B, IN_R, IN_G, IN_B, light, ov);
                paperTriSingle(vc, pose, normMat,
                        tipApex, baseRight, innerTuck,
                        0.96F, 0.50F, 0.10F, 0.92F, 0.25F, 0.50F,
                        FL_R, FL_G, FL_B, SH_R, SH_G, SH_B, IN_R, IN_G, IN_B, light, ov);
            }
        }
    }

    private static void renderDorsalOrigamiCrest(VertexConsumer vc, Matrix4f pose, Matrix3f normMat,
                                                 Vec3[] spine, Vec3[] tangents, Vec3[] normals, Vec3[] binormals,
                                                 float time, float phase, float dive, int light, int ov) {
        for (int i = 1; i < SEGS - 2; ++i) {
            if (i >= 3 && i <= 6) continue; // место под седло остаётся аккуратным
            float r = bodyRadius(i);
            float u = (float) i / (float) (SEGS - 1);
            float ripple = 0.90F + 0.14F * Mth.sin(time * 0.16F - i * 0.34F + phase) * (1.0F - 0.6F * dive);
            float diveSweep = 1.0F - 0.32F * dive;
            float crestH = SCALE * (0.32F + 0.56F * Mth.sin(Mth.clamp(u * 1.15F, 0.05F, 1.0F) * Mth.PI * 0.88F))
                    * ripple * diveSweep;
            float lateralSway = 0.08F * SCALE * Mth.cos(time * 0.16F - i * 0.34F + phase) * (1.0F - dive);

            Vec3 c = spine[i];
            Vec3 fw = tangents[i];
            Vec3 up = normals[i];
            Vec3 sd = binormals[i];

            Vec3 rootFront = c.add(up.scale(r * 0.96)).add(fw.scale(0.16 * SCALE));
            Vec3 rootBack = c.add(up.scale(r * 0.92)).subtract(fw.scale(0.24 * SCALE));
            Vec3 sideL = c.add(up.scale(r * 0.98)).subtract(sd.scale(0.11 * SCALE));
            Vec3 sideR = c.add(up.scale(r * 0.98)).add(sd.scale(0.11 * SCALE));
            Vec3 crestTip = c.add(up.scale(r + crestH))
                    .subtract(fw.scale((0.34F + 0.28F * dive) * SCALE))
                    .add(sd.scale(lateralSway));

            paperTriSingle(vc, pose, normMat, rootFront, sideL, crestTip,
                    0.1F, 0.5F, 0.2F, 0.1F, 0.98F, 0.5F, CR_R, CR_G, CR_B, FL_R, FL_G, FL_B, CR_R, CR_G, CR_B, light, ov);
            paperTriSingle(vc, pose, normMat, sideR, rootFront, crestTip,
                    0.2F, 0.9F, 0.1F, 0.5F, 0.98F, 0.5F, SH_R, SH_G, SH_B, CR_R, CR_G, CR_B, FL_R, FL_G, FL_B, light, ov);
            paperTriSingle(vc, pose, normMat, sideL, rootBack, crestTip,
                    0.2F, 0.1F, 0.1F, 0.5F, 0.98F, 0.5F, FL_R, FL_G, FL_B, SH_R, SH_G, SH_B, FL_R, FL_G, FL_B, light, ov);
            paperTriSingle(vc, pose, normMat, rootBack, sideR, crestTip,
                    0.1F, 0.5F, 0.2F, 0.9F, 0.98F, 0.5F, SH_R, SH_G, SH_B, SH_R, SH_G, SH_B, FL_R, FL_G, FL_B, light, ov);
        }
    }

    private static void renderOrigamiDragonSaddle(VertexConsumer vc, Matrix4f pose, Matrix3f normMat,
                                                  Vec3[] spine, Vec3[] tangents, Vec3[] normals, Vec3[] binormals,
                                                  int tier, int light, int ov) {
        boolean custom = tier >= 2;
        int mainR = custom ? 186 : 138, mainG = custom ? 38 : 82, mainB = custom ? 44 : 44;
        int darkR = custom ? 128 : 98,  darkG = custom ? 24 : 56, darkB = custom ? 30 : 30;
        int trimR = custom ? 250 : 212, trimG = custom ? 206 : 168, trimB = custom ? 76 : 68;

        for (int i = 3; i <= 6; ++i) {
            float r0 = bodyRadius(i) * 1.04F;
            float r1 = bodyRadius(i + 1) * 1.04F;
            Vec3 c0 = spine[i], c1 = spine[i + 1];
            Vec3 u0 = normals[i], s0 = binormals[i];
            Vec3 u1 = normals[i + 1], s1 = binormals[i + 1];

            for (int side = -1; side <= 1; side += 2) {
                float aTop = 0.0F;
                float aMid = side * 0.58F;
                float aFlap = side * 1.12F;
                Vec3 p00 = ringPt(c0, u0, s0, aTop, r0);
                Vec3 p01 = ringPt(c0, u0, s0, aMid, r0);
                Vec3 p11 = ringPt(c1, u1, s1, aMid, r1);
                Vec3 p10 = ringPt(c1, u1, s1, aTop, r1);
                paperQuadSingle(vc, pose, normMat, p00, p01, p11, p10, mainR, mainG, mainB, light, ov);

                Vec3 f00 = p01;
                Vec3 f01 = ringPt(c0, u0, s0, aFlap, r0 * 1.02F);
                Vec3 f11 = ringPt(c1, u1, s1, aFlap, r1 * 1.02F);
                Vec3 f10 = p11;
                paperQuadSingle(vc, pose, normMat, f00, f01, f11, f10, darkR, darkG, darkB, light, ov);
            }
        }
        // Передняя лука и высокая задняя спинка седла + стремена
        Vec3 pommelBase = spine[3].add(normals[3].scale(bodyRadius(3) * 1.02));
        Vec3 pommelTip = pommelBase.add(normals[3].scale(0.32 * SCALE)).add(tangents[3].scale(0.14 * SCALE));
        renderTintedPrismSegment(vc, pose, normMat, pommelBase, pommelTip, normals[3], binormals[3],
                0.18F * SCALE, 0.08F * SCALE, trimR, trimG, trimB, light, ov);

        Vec3 cantleBase = spine[6].add(normals[6].scale(bodyRadius(6) * 1.02));
        Vec3 cantleTip = cantleBase.add(normals[6].scale((custom ? 0.44 : 0.30) * SCALE)).subtract(tangents[6].scale(0.18 * SCALE));
        renderTintedPrismSegment(vc, pose, normMat, cantleBase, cantleTip, normals[6], binormals[6],
                0.24F * SCALE, 0.11F * SCALE, trimR, trimG, trimB, light, ov);

        for (int side = -1; side <= 1; side += 2) {
            Vec3 strapTop = ringPt(spine[4], normals[4], binormals[4], side * 1.05F, bodyRadius(4) * 1.03F);
            Vec3 stirrup = strapTop.subtract(normals[4].scale(0.52 * SCALE)).add(binormals[4].scale(side * 0.08 * SCALE));
            renderTintedPrismSegment(vc, pose, normMat, strapTop, stirrup, normals[4], binormals[4],
                    0.045F * SCALE, 0.065F * SCALE, trimR, trimG, trimB, light, ov);
        }
    }

    private static void renderOrigamiDragonHead(VertexConsumer vc, Matrix4f pose, Matrix3f normMat,
                                                Vec3 neck, Vec3 fw, Vec3 up, Vec3 sd,
                                                float jawOpen, float time, float phase,
                                                float dive, float coil, int whiskerRgb, int light, int ov) {
        int snoutSteps = 6;
        for (int s = 0; s < snoutSteps; ++s) {
            float u0 = (float) s / snoutSteps;
            float u1 = (float) (s + 1) / snoutSteps;
            float d0 = u0 * 2.35F;
            float d1 = u1 * 2.35F;
            float w0 = Mth.lerp(u0, 0.72F, 0.26F);
            float w1 = Mth.lerp(u1, 0.72F, 0.22F);
            float hTop0 = Mth.lerp(u0, 0.64F, 0.26F) + (s == snoutSteps - 1 ? 0.14F : 0.0F);
            float hTop1 = Mth.lerp(u1, 0.64F, 0.34F);
            float lipY0 = Mth.lerp(u0, -0.10F, 0.02F);
            float lipY1 = Mth.lerp(u1, -0.10F, 0.06F);

            Vec3 top0 = headPtScaled(neck, fw, up, sd, d0, hTop0, 0.0F);
            Vec3 top1 = headPtScaled(neck, fw, up, sd, d1, hTop1, 0.0F);
            Vec3 l0 = headPtScaled(neck, fw, up, sd, d0, lipY0, -w0);
            Vec3 r0 = headPtScaled(neck, fw, up, sd, d0, lipY0, +w0);
            Vec3 l1 = headPtScaled(neck, fw, up, sd, d1, lipY1, -w1);
            Vec3 r1 = headPtScaled(neck, fw, up, sd, d1, lipY1, +w1);

            Vec3 crestMid = headPtScaled(neck, fw, up, sd, (d0 + d1) * 0.5F - 0.12F, (hTop0 + hTop1) * 0.5F + 0.14F, 0.0F);
            paperTriSingle(vc, pose, normMat, l0, top0, l1, 0.1F, 0.1F, 0.4F, 0.5F, 0.9F, 0.2F, FL_R, FL_G, FL_B, CR_R, CR_G, CR_B, FL_R, FL_G, FL_B, light, ov);
            paperTriSingle(vc, pose, normMat, top0, top1, l1, 0.4F, 0.5F, 0.95F, 0.5F, 0.9F, 0.2F, CR_R, CR_G, CR_B, CR_R, CR_G, CR_B, FL_R, FL_G, FL_B, light, ov);
            paperTriSingle(vc, pose, normMat, top0, r0, r1, 0.4F, 0.5F, 0.1F, 0.9F, 0.9F, 0.8F, CR_R, CR_G, CR_B, SH_R, SH_G, SH_B, FL_R, FL_G, FL_B, light, ov);
            paperTriSingle(vc, pose, normMat, top0, r1, top1, 0.4F, 0.5F, 0.9F, 0.8F, 0.95F, 0.5F, CR_R, CR_G, CR_B, FL_R, FL_G, FL_B, CR_R, CR_G, CR_B, light, ov);
            paperTriSingle(vc, pose, normMat, l0.lerp(top0, 0.55), crestMid, top1, 0.1F, 0.2F, 0.5F, 0.5F, 0.98F, 0.5F, FL_R, FL_G, FL_B, CR_R, CR_G, CR_B, CR_R, CR_G, CR_B, light, ov);
            paperTriSingle(vc, pose, normMat, crestMid, r0.lerp(top0, 0.55), top1, 0.5F, 0.5F, 0.1F, 0.8F, 0.98F, 0.5F, CR_R, CR_G, CR_B, SH_R, SH_G, SH_B, CR_R, CR_G, CR_B, light, ov);
            paperQuadSingle(vc, pose, normMat, l0, l1, r1, r0, IN_R, IN_G, IN_B, light, ov);
        }

        Vec3 noseBridge = headPtScaled(neck, fw, up, sd, 2.35F, 0.34F, 0.0F);
        Vec3 noseTip = headPtScaled(neck, fw, up, sd, 2.72F, 0.48F, 0.0F);
        Vec3 noseL = headPtScaled(neck, fw, up, sd, 2.35F, 0.06F, -0.22F);
        Vec3 noseR = headPtScaled(neck, fw, up, sd, 2.35F, 0.06F, +0.22F);
        paperTriSingle(vc, pose, normMat, noseL, noseBridge, noseTip, 0.1F, 0.2F, 0.5F, 0.5F, 0.98F, 0.5F, FL_R, FL_G, FL_B, CR_R, CR_G, CR_B, CR_R, CR_G, CR_B, light, ov);
        paperTriSingle(vc, pose, normMat, noseBridge, noseR, noseTip, 0.5F, 0.5F, 0.1F, 0.8F, 0.98F, 0.5F, CR_R, CR_G, CR_B, SH_R, SH_G, SH_B, CR_R, CR_G, CR_B, light, ov);
        paperTriSingle(vc, pose, normMat, noseR, noseL, noseTip, 0.2F, 0.8F, 0.2F, 0.2F, 0.95F, 0.5F, SH_R, SH_G, SH_B, SH_R, SH_G, SH_B, FL_R, FL_G, FL_B, light, ov);

        Vec3 jawDir = fw.scale(Math.cos(jawOpen)).subtract(up.scale(Math.sin(jawOpen))).normalize();
        Vec3 jawUp = up.scale(Math.cos(jawOpen)).add(fw.scale(Math.sin(jawOpen))).normalize();
        Vec3 jawHinge = headPtScaled(neck, fw, up, sd, 0.22F, -0.18F, 0.0F);

        Vec3 jl0 = jawHinge.subtract(sd.scale(0.56 * SCALE));
        Vec3 jr0 = jawHinge.add(sd.scale(0.56 * SCALE));
        Vec3 jKeel = jawHinge.add(jawDir.scale(1.05 * SCALE)).subtract(jawUp.scale(0.34 * SCALE));
        Vec3 jTip = jawHinge.add(jawDir.scale(2.08 * SCALE)).subtract(jawUp.scale(0.04 * SCALE));
        Vec3 jlMid = jawHinge.add(jawDir.scale(1.15 * SCALE)).subtract(sd.scale(0.32 * SCALE));
        Vec3 jrMid = jawHinge.add(jawDir.scale(1.15 * SCALE)).add(sd.scale(0.32 * SCALE));

        paperTriSingle(vc, pose, normMat, jl0, jKeel, jlMid, 0.1F, 0.1F, 0.5F, 0.5F, 0.8F, 0.2F, SH_R, SH_G, SH_B, FL_R, FL_G, FL_B, FL_R, FL_G, FL_B, light, ov);
        paperTriSingle(vc, pose, normMat, jKeel, jTip, jlMid, 0.5F, 0.5F, 0.98F, 0.5F, 0.8F, 0.2F, FL_R, FL_G, FL_B, CR_R, CR_G, CR_B, FL_R, FL_G, FL_B, light, ov);
        paperTriSingle(vc, pose, normMat, jr0, jrMid, jKeel, 0.1F, 0.9F, 0.8F, 0.8F, 0.5F, 0.5F, SH_R, SH_G, SH_B, SH_R, SH_G, SH_B, FL_R, FL_G, FL_B, light, ov);
        paperTriSingle(vc, pose, normMat, jrMid, jTip, jKeel, 0.8F, 0.8F, 0.98F, 0.5F, 0.5F, 0.5F, SH_R, SH_G, SH_B, CR_R, CR_G, CR_B, FL_R, FL_G, FL_B, light, ov);
        paperTriSingle(vc, pose, normMat, jl0, jlMid, jrMid, 0.2F, 0.2F, 0.8F, 0.2F, 0.8F, 0.8F, IN_R, IN_G, IN_B, IN_R, IN_G, IN_B, IN_R, IN_G, IN_B, light, ov);
        paperTriSingle(vc, pose, normMat, jl0, jrMid, jr0, 0.2F, 0.2F, 0.8F, 0.8F, 0.2F, 0.8F, IN_R, IN_G, IN_B, IN_R, IN_G, IN_B, IN_R, IN_G, IN_B, light, ov);
        paperTriSingle(vc, pose, normMat, jlMid, jTip, jrMid, 0.2F, 0.2F, 0.98F, 0.5F, 0.8F, 0.8F, FL_R, FL_G, FL_B, CR_R, CR_G, CR_B, FL_R, FL_G, FL_B, light, ov);

        for (int side = -1; side <= 1; side += 2) {
            Vec3 fangBase1 = headPtScaled(neck, fw, up, sd, 1.72F, 0.02F, side * 0.27F);
            Vec3 fangBase2 = headPtScaled(neck, fw, up, sd, 1.92F, 0.04F, side * 0.23F);
            Vec3 fangTip = headPtScaled(neck, fw, up, sd, 1.82F, -0.22F, side * 0.24F);
            paperTriSingle(vc, pose, normMat, fangBase1, fangBase2, fangTip, 0.2F, 0.2F, 0.2F, 0.8F, 0.98F, 0.5F, CR_R, CR_G, CR_B, CR_R, CR_G, CR_B, CR_R, CR_G, CR_B, light, ov);
        }

        for (int side = -1; side <= 1; side += 2) {
            float sg = (float) side;
            Vec3 browFront = headPtScaled(neck, fw, up, sd, 1.28F, 0.42F, sg * 0.46F);
            Vec3 browMid = headPtScaled(neck, fw, up, sd, 0.86F, 0.58F, sg * 0.64F);
            Vec3 browBack = headPtScaled(neck, fw, up, sd, 0.35F, 0.52F, sg * 0.68F);
            Vec3 socket = headPtScaled(neck, fw, up, sd, 0.92F, 0.30F, sg * 0.52F);
            if (side < 0) {
                paperTriSingle(vc, pose, normMat, socket, browMid, browFront, 0.2F, 0.2F, 0.6F, 0.5F, 0.95F, 0.5F, FL_R, FL_G, FL_B, CR_R, CR_G, CR_B, CR_R, CR_G, CR_B, light, ov);
                paperTriSingle(vc, pose, normMat, browBack, browMid, socket, 0.2F, 0.2F, 0.95F, 0.5F, 0.3F, 0.8F, SH_R, SH_G, SH_B, CR_R, CR_G, CR_B, FL_R, FL_G, FL_B, light, ov);
            } else {
                paperTriSingle(vc, pose, normMat, browFront, browMid, socket, 0.95F, 0.5F, 0.6F, 0.5F, 0.2F, 0.2F, CR_R, CR_G, CR_B, CR_R, CR_G, CR_B, FL_R, FL_G, FL_B, light, ov);
                paperTriSingle(vc, pose, normMat, socket, browMid, browBack, 0.3F, 0.8F, 0.95F, 0.5F, 0.2F, 0.2F, FL_R, FL_G, FL_B, CR_R, CR_G, CR_B, SH_R, SH_G, SH_B, light, ov);
            }

            Vec3 eFront = headPtScaled(neck, fw, up, sd, 1.16F, 0.28F, sg * 0.57F);
            Vec3 eBack = headPtScaled(neck, fw, up, sd, 0.72F, 0.32F, sg * 0.61F);
            Vec3 eTop = headPtScaled(neck, fw, up, sd, 0.94F, 0.41F, sg * 0.59F);
            Vec3 eBot = headPtScaled(neck, fw, up, sd, 0.94F, 0.19F, sg * 0.58F);
            if (side < 0) {
                paperQuadSingle(vc, pose, normMat, eBack, eTop, eFront, eBot, EYE_R, EYE_G, EYE_B, light, ov);
            } else {
                paperQuadSingle(vc, pose, normMat, eFront, eTop, eBack, eBot, EYE_R, EYE_G, EYE_B, light, ov);
            }
        }

        for (int side = -1; side <= 1; side += 2) {
            float sg = (float) side;
            Vec3 hStart = headPtScaled(neck, fw, up, sd, 0.38F, 0.56F, sg * 0.36F);
            Vec3 hDir = fw.scale(-0.76).add(up.scale(0.58)).add(sd.scale(sg * 0.32)).normalize();
            renderStackedOrigamiChain(vc, pose, normMat, hStart, hDir, up, sd, 6, 0.42F * SCALE, 0.25F * SCALE, 0.055F * SCALE, light, ov);
            Vec3 tineStart = hStart.add(hDir.scale(0.95 * SCALE));
            Vec3 tineDir = fw.scale(-0.42).add(up.scale(0.78)).add(sd.scale(sg * 0.46)).normalize();
            renderStackedOrigamiChain(vc, pose, normMat, tineStart, tineDir, up, sd, 3, 0.32F * SCALE, 0.14F * SCALE, 0.04F * SCALE, light, ov);
        }

        for (int k = 0; k < 11; ++k) {
            float ang = -Mth.PI * 0.74F + (Mth.PI * 1.48F * k) / 10.0F;
            Vec3 radDir = up.scale(Mth.cos(ang)).add(sd.scale(Mth.sin(ang))).normalize();
            Vec3 tangRing = up.scale(-Mth.sin(ang)).add(sd.scale(Mth.cos(ang))).normalize();
            Vec3 mRoot = neck.add(fw.scale(0.10 * SCALE)).add(radDir.scale(0.60 * SCALE));
            Vec3 mLeft = mRoot.subtract(tangRing.scale(0.23 * SCALE));
            Vec3 mRight = mRoot.add(tangRing.scale(0.23 * SCALE));
            Vec3 mRidge = mRoot.add(radDir.scale(0.24 * SCALE)).subtract(fw.scale(0.18 * SCALE));
            Vec3 mTip = mRoot.add(radDir.scale(0.72 * SCALE)).subtract(fw.scale(0.82 * SCALE));
            paperTriSingle(vc, pose, normMat, mLeft, mRidge, mTip, 0.1F, 0.1F, 0.4F, 0.5F, 0.98F, 0.5F, FL_R, FL_G, FL_B, CR_R, CR_G, CR_B, CR_R, CR_G, CR_B, light, ov);
            paperTriSingle(vc, pose, normMat, mRidge, mRight, mTip, 0.4F, 0.5F, 0.1F, 0.9F, 0.98F, 0.5F, CR_R, CR_G, CR_B, SH_R, SH_G, SH_B, FL_R, FL_G, FL_B, light, ov);
        }

        int wR = (whiskerRgb >> 16) & 0xFF;
        int wG = (whiskerRgb >> 8) & 0xFF;
        int wB = whiskerRgb & 0xFF;
        int whiskerLight = (whiskerRgb != 0xF5EFE2) ? LightTexture.pack(15, 15) : light;
        float waveScale = (1.0F - 0.82F * dive) * (1.0F - 0.65F * coil);
        for (int side = -1; side <= 1; side += 2) {
            float sg = (float) side;
            Vec3 wPrev = headPtScaled(neck, fw, up, sd, 2.05F, 0.14F, sg * 0.25F);
            for (int ws = 1; ws <= 7; ++ws) {
                float wu = ws / 7.0F;
                float flutter = Mth.sin(time * 0.16F - ws * 0.62F + phase + side) * (0.16F + 0.28F * wu) * SCALE * waveScale;
                float liftW = Mth.cos(time * 0.13F - ws * 0.50F + phase) * 0.14F * wu * SCALE * waveScale;
                float outward = (0.24F + 0.08F * (1.0F - wu)) * (1.0F - 0.68F * dive);
                Vec3 wNext = wPrev
                        .subtract(fw.scale((0.42F + 0.12F * dive) * SCALE))
                        .add(sd.scale(sg * outward * SCALE + flutter))
                        .add(up.scale((0.05F * (1.0F - dive)) * SCALE + liftW));
                float wThick = Mth.lerp(wu, 0.086F, 0.022F) * SCALE;
                renderTintedPrismSegment(vc, pose, normMat, wPrev, wNext, up, sd, wThick, wThick * 0.72F,
                        wR, wG, wB, whiskerLight, ov);
                wPrev = wNext;
            }
        }
    }

    private static void renderArticulatedOrigamiPairOfLegs(VertexConsumer vc, Matrix4f pose, Matrix3f normMat,
                                                           Vec3 bodyCenter, Vec3 fw, Vec3 up, Vec3 sd,
                                                           float bodyRad, boolean forelegs,
                                                           float time, float phase, float bankDeg,
                                                           float dive, float coil, int light, int ov) {
        for (int side = -1; side <= 1; side += 2) {
            float sg = (float) side;
            float legPhase = time * 0.185F + phase + (side > 0 ? 0.0F : Mth.PI);
            float stride = Mth.sin(legPhase) * (1.0F - dive) * (1.0F - 0.85F * coil);
            float kneeLift = Mth.cos(legPhase) * (1.0F - dive) * (1.0F - 0.85F * coil);
            float clawFlex = (0.5F + 0.5F * Mth.sin(legPhase - 0.7F)) * (1.0F - dive) * (1.0F - coil);
            float bankTuck = Mth.clamp(bankDeg * sg * 0.008F, -0.20F, 0.20F);

            Vec3 shoulder = bodyCenter
                    .add(sd.scale(sg * bodyRad * (0.86 - 0.24 * dive)))
                    .subtract(up.scale(bodyRad * (0.42 + 0.10 * dive)));

            Vec3 shShieldTip = shoulder.add(sd.scale(sg * (0.42 - 0.18 * dive) * SCALE))
                    .subtract(fw.scale((0.48 + 0.22 * dive) * SCALE))
                    .subtract(up.scale(0.18 * SCALE));
            paperTriSingle(vc, pose, normMat,
                    shoulder.add(fw.scale(0.24 * SCALE)), shoulder.add(up.scale(0.28 * SCALE)), shShieldTip,
                    0.1F, 0.2F, 0.4F, 0.5F, 0.98F, 0.5F, FL_R, FL_G, FL_B, CR_R, CR_G, CR_B, CR_R, CR_G, CR_B, light, ov);
            paperTriSingle(vc, pose, normMat,
                    shoulder.add(up.scale(0.28 * SCALE)), shoulder.subtract(fw.scale(0.22 * SCALE)), shShieldTip,
                    0.4F, 0.5F, 0.1F, 0.8F, 0.98F, 0.5F, CR_R, CR_G, CR_B, SH_R, SH_G, SH_B, FL_R, FL_G, FL_B, light, ov);

            // Плечо/бедро: активный мах в полёте, плотное прижатие назад к брюху в пикировании, спокойный сгиб во сне
            Vec3 flightUpper = sd.scale(sg * (0.50 - 0.12 * coil))
                    .subtract(up.scale((0.70 - 0.26 * coil) - bankTuck - kneeLift * 0.24))
                    .add(fw.scale((forelegs ? -0.18 : -0.30) + stride * 0.46))
                    .normalize();
            Vec3 diveUpper = fw.scale(-0.94).subtract(up.scale(0.22)).add(sd.scale(sg * 0.14)).normalize();
            Vec3 upperDir = flightUpper.lerp(diveUpper, dive).normalize();

            Vec3 elbow = renderStackedOrigamiChain(vc, pose, normMat, shoulder, upperDir, up, sd, 3,
                    0.37F * SCALE, 0.26F * SCALE, 0.185F * SCALE, light, ov);

            // Предплечье/голень: выразительное сгибание-разгибание локтя и колена!
            Vec3 flightLower = fw.scale((0.52 + 0.20 * coil) + stride * 0.52)
                    .subtract(up.scale((0.68 - 0.36 * coil) + kneeLift * 0.28))
                    .add(sd.scale(sg * 0.20))
                    .normalize();
            Vec3 diveLower = fw.scale(-0.96).subtract(up.scale(0.14)).add(sd.scale(sg * 0.08)).normalize();
            Vec3 lowerDir = flightLower.lerp(diveLower, dive).normalize();

            Vec3 wrist = renderStackedOrigamiChain(vc, pose, normMat, elbow, lowerDir, up, sd, 3,
                    0.34F * SCALE, 0.195F * SCALE, 0.135F * SCALE, light, ov);

            // Пальцы и когти: живое раскрытие и схватывание в такт маху (а в пикировании — сжаты в обтекаемый пучок!)
            for (int c = 0; c < 4; ++c) {
                Vec3 clawDir;
                float clawLen;
                if (c < 3) {
                    float spread = (c - 1) * 0.38F * (1.0F - 0.78F * dive) * (0.75F + 0.40F * clawFlex);
                    Vec3 flClaw = fw.scale(0.72 - 0.34 * clawFlex)
                            .subtract(up.scale(0.44 + 0.34 * clawFlex + 0.08 * Math.abs(c - 1)))
                            .add(sd.scale(spread + sg * 0.10))
                            .normalize();
                    Vec3 dvClaw = fw.scale(-0.96).subtract(up.scale(0.18)).add(sd.scale(spread * 0.3)).normalize();
                    clawDir = flClaw.lerp(dvClaw, dive).normalize();
                    clawLen = ((c == 1) ? 0.58F : 0.47F) * SCALE;
                } else {
                    Vec3 flDew = fw.scale(-0.48).subtract(up.scale(0.68)).subtract(sd.scale(sg * 0.25)).normalize();
                    Vec3 dvDew = fw.scale(-0.95).subtract(up.scale(0.22)).normalize();
                    clawDir = flDew.lerp(dvDew, dive).normalize();
                    clawLen = 0.37F * SCALE;
                }
                renderOrigamiPrismSegment(vc, pose, normMat, wrist, wrist.add(clawDir.scale(clawLen)),
                        up, sd, 0.098F * SCALE, 0.016F * SCALE, light, ov);
            }
        }
    }

    private static void renderImperialOrigamiTailFan(VertexConsumer vc, Matrix4f pose, Matrix3f normMat,
                                                     Vec3 tailTip, Vec3 fw, Vec3 up, Vec3 sd,
                                                     float time, float phase, float dive, int light, int ov) {
        Vec3 back = fw.scale(-1.0);
        for (int p = -3; p <= 3; ++p) {
            float normP = p / 3.0F;
            float fanSpread = normP * 0.74F * (1.0F - 0.62F * dive);
            float wave = Mth.sin(time * 0.15F + p * 0.58F + phase) * 0.16F * (1.0F - 0.75F * dive);
            float plumeLen = (2.75F - Math.abs(normP) * 0.80F + 0.45F * dive) * SCALE;
            float plumeW = (0.25F - Math.abs(normP) * 0.04F) * SCALE;

            Vec3 plumeDir = back.scale(Math.cos(fanSpread))
                    .add(sd.scale(Math.sin(fanSpread) * 0.88))
                    .add(up.scale(0.18F * (1.0F - Math.abs(normP)) + wave))
                    .normalize();

            Vec3 root = tailTip.add(sd.scale(normP * 0.09 * SCALE));
            Vec3 mid = root.add(plumeDir.scale(plumeLen * 0.52));
            Vec3 tip = root.add(plumeDir.scale(plumeLen));

            Vec3 wingAxis = sd.scale(Math.cos(fanSpread * 0.5F)).add(up.scale(0.35)).normalize();
            Vec3 ridgeAxis = plumeDir.cross(wingAxis).normalize();

            Vec3 mL = mid.subtract(wingAxis.scale(plumeW));
            Vec3 mR = mid.add(wingAxis.scale(plumeW));
            Vec3 mTop = mid.add(ridgeAxis.scale(plumeW * 0.55));
            Vec3 mBot = mid.subtract(ridgeAxis.scale(plumeW * 0.40));

            paperTriSingle(vc, pose, normMat, root, mL, mTop, 0.1F, 0.2F, 0.5F, 0.1F, 0.5F, 0.5F, FL_R, FL_G, FL_B, FL_R, FL_G, FL_B, CR_R, CR_G, CR_B, light, ov);
            paperTriSingle(vc, pose, normMat, root, mTop, mR, 0.1F, 0.8F, 0.5F, 0.5F, 0.5F, 0.9F, FL_R, FL_G, FL_B, CR_R, CR_G, CR_B, SH_R, SH_G, SH_B, light, ov);
            paperTriSingle(vc, pose, normMat, mL, tip, mTop, 0.5F, 0.1F, 0.98F, 0.5F, 0.5F, 0.5F, FL_R, FL_G, FL_B, CR_R, CR_G, CR_B, CR_R, CR_G, CR_B, light, ov);
            paperTriSingle(vc, pose, normMat, mTop, tip, mR, 0.5F, 0.5F, 0.98F, 0.5F, 0.5F, 0.9F, CR_R, CR_G, CR_B, CR_R, CR_G, CR_B, SH_R, SH_G, SH_B, light, ov);

            paperTriSingle(vc, pose, normMat, root, mBot, mL, 0.1F, 0.5F, 0.5F, 0.5F, 0.5F, 0.1F, SH_R, SH_G, SH_B, IN_R, IN_G, IN_B, SH_R, SH_G, SH_B, light, ov);
            paperTriSingle(vc, pose, normMat, root, mR, mBot, 0.1F, 0.5F, 0.5F, 0.9F, 0.5F, 0.5F, SH_R, SH_G, SH_B, SH_R, SH_G, SH_B, IN_R, IN_G, IN_B, light, ov);
            paperTriSingle(vc, pose, normMat, mL, mBot, tip, 0.5F, 0.1F, 0.5F, 0.5F, 0.98F, 0.5F, SH_R, SH_G, SH_B, IN_R, IN_G, IN_B, FL_R, FL_G, FL_B, light, ov);
            paperTriSingle(vc, pose, normMat, mBot, mR, tip, 0.5F, 0.5F, 0.5F, 0.9F, 0.98F, 0.5F, IN_R, IN_G, IN_B, SH_R, SH_G, SH_B, FL_R, FL_G, FL_B, light, ov);
        }
    }

    private static Vec3 renderStackedOrigamiChain(VertexConsumer vc, Matrix4f pose, Matrix3f normMat,
                                                  Vec3 start, Vec3 dir, Vec3 refUp, Vec3 refSide,
                                                  int count, float stepLen, float startRad, float endRad,
                                                  int light, int ov) {
        Vec3 cur = start;
        for (int i = 0; i < count; ++i) {
            float u0 = (float) i / count;
            float u1 = (float) (i + 1) / count;
            float r0 = Mth.lerp(u0, startRad, endRad);
            float r1 = Mth.lerp(u1, startRad, endRad) * 0.72F;
            Vec3 next = cur.add(dir.scale(stepLen));
            Vec3 flareEnd = cur.add(dir.scale(stepLen * 1.24));
            renderOrigamiPrismSegment(vc, pose, normMat, cur, flareEnd, refUp, refSide, r0, r1, light, ov);
            cur = next;
        }
        return cur;
    }

    private static void renderOrigamiPrismSegment(VertexConsumer vc, Matrix4f pose, Matrix3f normMat,
                                                  Vec3 a, Vec3 b, Vec3 hintUp, Vec3 hintSide,
                                                  float rA, float rB, int light, int ov) {
        renderTintedPrismSegment(vc, pose, normMat, a, b, hintUp, hintSide, rA, rB, CR_R, CR_G, CR_B, light, ov);
    }

    private static void renderTintedPrismSegment(VertexConsumer vc, Matrix4f pose, Matrix3f normMat,
                                                 Vec3 a, Vec3 b, Vec3 hintUp, Vec3 hintSide,
                                                 float rA, float rB, int colR, int colG, int colB, int light, int ov) {
        Vec3 axis = b.subtract(a);
        if (axis.lengthSqr() < 1.0E-6) return;
        axis = axis.normalize();
        Vec3 xAx = axis.cross(hintUp);
        if (xAx.lengthSqr() < 1.0E-5) xAx = axis.cross(hintSide);
        if (xAx.lengthSqr() < 1.0E-5) xAx = new Vec3(1, 0, 0);
        xAx = xAx.normalize();
        Vec3 yAx = xAx.cross(axis).normalize();

        Vec3[] pa = {
                a.add(yAx.scale(rA)),
                a.add(xAx.scale(rA)),
                a.subtract(yAx.scale(rA)),
                a.subtract(xAx.scale(rA))
        };
        Vec3[] pb = {
                b.add(yAx.scale(rB)),
                b.add(xAx.scale(rB)),
                b.subtract(yAx.scale(rB)),
                b.subtract(xAx.scale(rB))
        };
        int shR = Math.max(0, (colR * 214) / 255);
        int shG = Math.max(0, (colG * 210) / 255);
        int shB = Math.max(0, (colB * 204) / 255);
        for (int k = 0; k < 4; ++k) {
            int kn = (k + 1) & 3;
            boolean sun = (k == 0 || k == 3);
            paperQuadSingle(vc, pose, normMat, pa[k], pa[kn], pb[kn], pb[k],
                    sun ? colR : shR, sun ? colG : shG, sun ? colB : shB, light, ov);
        }
    }

    private static Vec3 ringPt(Vec3 center, Vec3 up, Vec3 side, float ang, float radius) {
        float ca = Mth.cos(ang) * radius;
        float sa = Mth.sin(ang) * radius;
        return new Vec3(
                center.x + up.x * ca + side.x * sa,
                center.y + up.y * ca + side.y * sa,
                center.z + up.z * ca + side.z * sa);
    }

    private static Vec3 headPtScaled(Vec3 origin, Vec3 fw, Vec3 up, Vec3 sd, float f, float u, float s) {
        float fs = f * SCALE, us = u * SCALE, ss = s * SCALE;
        return new Vec3(
                origin.x + fw.x * fs + up.x * us + sd.x * ss,
                origin.y + fw.y * fs + up.y * us + sd.y * ss,
                origin.z + fw.z * fs + up.z * us + sd.z * ss);
    }

    /**
     * Emits a triangle as TWO positive-area halves of a quad: (p1, p2, mid(p2,p3), p3) —
     * 0 degenerate zero-area triangles, 0 Z-fighting, 0 dropped points on any GPU/Mesa/shader pipeline!
     */
    private static void paperTriSingle(VertexConsumer vc, Matrix4f pose, Matrix3f normMat,
                                       Vec3 p1, Vec3 p2, Vec3 p3,
                                       float u1, float v1, float u2, float v2, float u3, float v3,
                                       int r1, int g1, int b1,
                                       int r2, int g2, int b2,
                                       int r3, int g3, int b3,
                                       int light, int ov) {
        Vec3 e1 = p2.subtract(p1);
        Vec3 e2 = p3.subtract(p1);
        Vec3 n = e1.cross(e2);
        double nl = n.length();
        float nx = nl > 1.0E-6 ? (float) (n.x / nl) : 0.0F;
        float ny = nl > 1.0E-6 ? (float) (n.y / nl) : 1.0F;
        float nz = nl > 1.0E-6 ? (float) (n.z / nl) : 0.0F;
        float bnx = nx * 0.72F;
        float bny = Mth.clamp(ny * 0.72F + 0.38F, -0.35F, 1.0F);
        float bnz = nz * 0.72F;
        float bLen = Mth.sqrt(bnx * bnx + bny * bny + bnz * bnz);
        if (bLen > 1.0E-4F) {
            bnx /= bLen;
            bny /= bLen;
            bnz /= bLen;
        }

        float mx = (float) ((p2.x + p3.x) * 0.5);
        float my = (float) ((p2.y + p3.y) * 0.5);
        float mz = (float) ((p2.z + p3.z) * 0.5);
        float mu = (u2 + u3) * 0.5F;
        float mv = (v2 + v3) * 0.5F;
        int mr = (r2 + r3) >> 1;
        int mg = (g2 + g3) >> 1;
        int mb = (b2 + b3) >> 1;

        vc.vertex(pose, (float) p1.x, (float) p1.y, (float) p1.z).color(r1, g1, b1, 255).uv(u1, v1).overlayCoords(ov).uv2(light).normal(normMat, bnx, bny, bnz).endVertex();
        vc.vertex(pose, (float) p2.x, (float) p2.y, (float) p2.z).color(r2, g2, b2, 255).uv(u2, v2).overlayCoords(ov).uv2(light).normal(normMat, bnx, bny, bnz).endVertex();
        vc.vertex(pose, mx, my, mz).color(mr, mg, mb, 255).uv(mu, mv).overlayCoords(ov).uv2(light).normal(normMat, bnx, bny, bnz).endVertex();
        vc.vertex(pose, (float) p3.x, (float) p3.y, (float) p3.z).color(r3, g3, b3, 255).uv(u3, v3).overlayCoords(ov).uv2(light).normal(normMat, bnx, bny, bnz).endVertex();
    }

    private static void paperQuadSingle(VertexConsumer vc, Matrix4f pose, Matrix3f normMat,
                                        Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4,
                                        int r, int g, int b, int light, int ov) {
        Vec3 n = p2.subtract(p1).cross(p4.subtract(p1));
        double nl = n.length();
        float nx = nl > 1.0E-6 ? (float) (n.x / nl) : 0.0F;
        float ny = nl > 1.0E-6 ? (float) (n.y / nl) : 1.0F;
        float nz = nl > 1.0E-6 ? (float) (n.z / nl) : 0.0F;
        float bnx = nx * 0.70F;
        float bny = Mth.clamp(ny * 0.70F + 0.40F, -0.30F, 1.0F);
        float bnz = nz * 0.70F;
        float bLen = Mth.sqrt(bnx * bnx + bny * bny + bnz * bnz);
        if (bLen > 1.0E-4F) {
            bnx /= bLen;
            bny /= bLen;
            bnz /= bLen;
        }

        vc.vertex(pose, (float) p1.x, (float) p1.y, (float) p1.z).color(r, g, b, 255).uv(0.15F, 0.15F).overlayCoords(ov).uv2(light).normal(normMat, bnx, bny, bnz).endVertex();
        vc.vertex(pose, (float) p2.x, (float) p2.y, (float) p2.z).color(r, g, b, 255).uv(0.15F, 0.85F).overlayCoords(ov).uv2(light).normal(normMat, bnx, bny, bnz).endVertex();
        vc.vertex(pose, (float) p3.x, (float) p3.y, (float) p3.z).color(r, g, b, 255).uv(0.85F, 0.85F).overlayCoords(ov).uv2(light).normal(normMat, bnx, bny, bnz).endVertex();
        vc.vertex(pose, (float) p4.x, (float) p4.y, (float) p4.z).color(r, g, b, 255).uv(0.85F, 0.15F).overlayCoords(ov).uv2(light).normal(normMat, bnx, bny, bnz).endVertex();
    }
}
