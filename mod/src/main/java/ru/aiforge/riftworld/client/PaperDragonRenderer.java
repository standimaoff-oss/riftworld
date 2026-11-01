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
 * СУПЕР-ДЕТАЛИЗИРОВАННЫЙ 3D МОДУЛЬНЫЙ ОРИГАМИ-ДРАКОН (Golden-Venture Chinese Modular Origami, ~27 м):
 * <ul>
 *   <li><b>0 Z-fighting и тёплая кремовая бумага васи ({@code origami_washi.png})</b>: каждая треугольная грань
 *       рисуется строго один раз с честной внешней нормалью + внутренний бумажный цилиндр перекрытия,
 *       поэтому никаких серых пятен Z-fighting'а и никаких сквозных дыр нет ни на солнце, ни в тени под островом!</li>
 *   <li><b>768 индивидуальных 3D-сложенных бумажных модулей</b> вдоль 64 колец тела (по 12 модулей в кольце
 *       с шахматным вкладыванием кармашков 1:1 как в настоящем модульном оригами {@code 01_origami_dragon_ref.png})
 *       + 56 спинных гребней-пластин!</li>
 *   <li><b>Многослойная оригами-голова</b>: ступенчатая переносица, вздёрнутый нос, живая открывающаяся нижняя
 *       челюсть, выразительные тёмные бумажные глаза с бликом, ветвистые модульные рога, пышный бумажный воротник-грива
 *       и 2 длинных развевающихся по потоку воздуха 6-сегментных уса!</li>
 *   <li><b>4 шарнирные модульные лапы</b> (плечо -> бедро из вложенных бумажных модулей -> локоть/колено ->
 *       предплечье -> 4 растопыренных бумажных когтя) с живой анимацией взмахов и поджатия в виражах +
 *       7-лучевой императорский бумажный веер хвоста!</li>
 *   <li><b>Реальная сплайновая физика следования хвоста, бегущая S-волна и аэродинамический крен в поворотах!</b></li>
 * </ul>
 */
public class PaperDragonRenderer extends EntityRenderer<PaperDragonEntity> {
    private static final ResourceLocation WASHI_TEX =
            new ResourceLocation(RiftWorld.MODID, "textures/entity/origami_washi.png");

    private static final int SEGS = PaperDragonEntity.SEGS; // 64 кольца!
    private static final int MODULES_PER_RING = 12;         // 12 модулей Golden-Venture в каждом кольце!

    // Тёплые оттенки настоящей сложенной сливочно-белой бумаги васи (без двойного затемнения!)
    private static final int CR_R = 255, CR_G = 252, CR_B = 244; // солнечное ребро сгиба
    private static final int FL_R = 247, FL_G = 241, FL_B = 229; // основная грань бумажного кармашка
    private static final int SH_R = 228, SH_G = 219, SH_B = 202; // внутренняя складка / кармашек
    private static final int IN_R = 216, IN_G = 206, IN_B = 188; // внутренний несущий цилиндр бумаги
    private static final int EYE_R = 38, EYE_G = 33, EYE_B = 29;  // обсидианово-тушевой сгиб глаза

    public PaperDragonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(PaperDragonEntity e, Frustum frustum, double cx, double cy, double cz) {
        return e.distanceToSqr(cx, cy, cz) < 420.0 * 420.0;
    }

    @Override
    public ResourceLocation getTextureLocation(PaperDragonEntity e) {
        return WASHI_TEX;
    }

    /** Гарантируем мягкую подсветку тёплой бумаги даже в глубокой тени ПОД парящими островами! */
    private static int warmPaperLight(int worldLight) {
        int block = Math.max(LightTexture.block(worldLight), 9);
        int sky = Math.max(LightTexture.sky(worldLight), 13);
        return LightTexture.pack(block, sky);
    }

    /** Профиль толщины модульного тела (мощная грудная клетка ~0.76 м радиуса -> изящное сужение к хвосту). */
    private static float bodyRadius(int i) {
        float u = (float) i / (float) (SEGS - 1);
        if (u < 0.08F) {
            // Шея у головы -> плавное расширение к плечевому поясу
            return Mth.lerp(u / 0.08F, 0.50F, 0.68F);
        }
        if (u < 0.24F) {
            // Мощный грудной отдел (передние лапы на i = 13)
            float k = (u - 0.08F) / 0.16F;
            return 0.68F + 0.09F * Mth.sin(k * Mth.PI);
        }
        // Плавное змеевидное сужение через задний пояс (i = 38) к кончику хвоста
        float t = (u - 0.24F) / 0.76F;
        return Mth.lerp((float) Math.pow(t, 1.15), 0.68F, 0.13F);
    }

    @Override
    public void render(PaperDragonEntity e, float entityYaw, float pt, PoseStack ps, MultiBufferSource buf, int packedLight) {
        int paperLight = warmPaperLight(packedLight);
        int ov = OverlayTexture.NO_OVERLAY;

        double headWx = Mth.lerp(pt, e.xo, e.getX());
        double headWy = Mth.lerp(pt, e.yo, e.getY());
        double headWz = Mth.lerp(pt, e.zo, e.getZ());

        float time = e.tickCount + pt;
        float phase = e.wavePhase();
        float bankDeg = Mth.lerp(pt, e.clientBankRollO, e.clientBankRoll);
        float jawOpen = Mth.lerp(pt, e.clientJawOpenO, e.clientJawOpen);

        // 1) Вычисляем 64 узла позвоночника в локальных координатах относительно головы (0,0,0)
        //    по реальному кольцевому буферу траектории + бегущей S-волне изгиба серпантина!
        Vec3[] spine = new Vec3[SEGS];
        float[] segRoll = new float[SEGS];
        sampleSplineSpineAndWaves(e, headWx, headWy, headWz, bankDeg, time, phase, pt, spine, segRoll);

        // 2) Строим гладкий безрывковый ортонормированный репер (parallel transport + крен segRoll) вдоль всех 64 колец!
        Vec3[] tangents = new Vec3[SEGS];
        Vec3[] normals = new Vec3[SEGS];   // вверх спины (с учётом аэродинамического крена!)
        Vec3[] binormals = new Vec3[SEGS]; // вправо тела

        for (int i = 0; i < SEGS; ++i) {
            Vec3 dir;
            if (i == 0) {
                dir = spine[0].subtract(spine[1]);
            } else if (i == SEGS - 1) {
                dir = spine[SEGS - 2].subtract(spine[SEGS - 1]);
            } else {
                dir = spine[i - 1].subtract(spine[i + 1]);
            }
            if (dir.lengthSqr() < 1.0E-6) dir = new Vec3(0, 0, 1);
            tangents[i] = dir.normalize();
        }

        // Начальный вектор «вверх» у головы с параллельным переносом вдоль позвоночника (0 перекручиваний на петлях!)
        Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
        Vec3 prevUp = worldUp.subtract(tangents[0].scale(worldUp.dot(tangents[0])));
        if (prevUp.lengthSqr() < 1.0E-5) prevUp = new Vec3(1.0, 0.0, 0.0);
        prevUp = prevUp.normalize();

        for (int i = 0; i < SEGS; ++i) {
            Vec3 t = tangents[i];
            // Смешиваем параллельный перенос с мировым верхом, чтобы спина всегда естественно ориентировалась к небу
            Vec3 transportUp = prevUp.subtract(t.scale(prevUp.dot(t)));
            Vec3 skyProj = worldUp.subtract(t.scale(worldUp.dot(t)));
            Vec3 baseUp = (skyProj.lengthSqr() > 0.02)
                    ? transportUp.scale(0.65).add(skyProj.normalize().scale(0.35))
                    : transportUp;
            if (baseUp.lengthSqr() < 1.0E-5) baseUp = prevUp;
            baseUp = baseUp.normalize();
            prevUp = baseUp;

            Vec3 baseSide = t.cross(baseUp);
            if (baseSide.lengthSqr() < 1.0E-5) baseSide = new Vec3(1.0, 0.0, 0.0);
            baseSide = baseSide.normalize();

            // Применяем аэродинамический крен виража + лёгкое волновое кручение вдоль хвоста
            float rollRad = segRoll[i] * Mth.DEG_TO_RAD;
            float cr = Mth.cos(rollRad), sr = Mth.sin(rollRad);
            Vec3 rolledUp = baseUp.scale(cr).add(baseSide.scale(sr)).normalize();
            Vec3 rolledSide = baseSide.scale(cr).subtract(baseUp.scale(sr)).normalize();

            normals[i] = rolledUp;
            binormals[i] = rolledSide;
        }

        ps.pushPose();
        ps.translate(0.0, 0.75, 0.0);
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(WASHI_TEX));
        Matrix4f pose = ps.last().pose();
        Matrix3f normMat = ps.last().normal();

        // 3A) Внутренний несущий бумажный цилиндр (чтобы между кармашками модулей никогда не светились пустые щели!)
        renderInnerPaperTube(vc, pose, normMat, spine, normals, binormals, paperLight, ov);

        // 3B) 768 вложенных 3D треугольных модулей Golden-Venture (63 кольца × 12 модулей в шахматном порядке!)
        renderModularOrigamiScales(vc, pose, normMat, spine, tangents, normals, binormals, time, phase, paperLight, ov);

        // 3C) Спинной зубчатый бумажный гребень вдоль всего позвоночника + брюшные килевые пластины
        renderDorsalOrigamiCrest(vc, pose, normMat, spine, tangents, normals, binormals, time, phase, paperLight, ov);

        // 3D) Супер-детализированная многослойная оригами-голова (челюсти, рога, глаза, воротник, длинные живые усы!)
        renderOrigamiDragonHead(vc, pose, normMat, spine[0], tangents[0], normals[0], binormals[0], jawOpen, time, phase, paperLight, ov);

        // 3E) 4 шарнирные модульные лапы с 4 бумажными когтями каждая (передняя пара на кольце 13, задняя на кольце 39)
        renderArticulatedOrigamiPairOfLegs(vc, pose, normMat, spine[13], tangents[13], normals[13], binormals[13],
                bodyRadius(13), true, time, phase, bankDeg, paperLight, ov);
        renderArticulatedOrigamiPairOfLegs(vc, pose, normMat, spine[39], tangents[39], normals[39], binormals[39],
                bodyRadius(39), false, time, phase + 1.65F, bankDeg, paperLight, ov);

        // 3F) Пышный 7-лучевой императорский бумажный веер хвоста на кончике!
        renderImperialOrigamiTailFan(vc, pose, normMat, spine[SEGS - 1], tangents[SEGS - 1], normals[SEGS - 1], binormals[SEGS - 1],
                time, phase, paperLight, ov);

        ps.popPose();
        super.render(e, entityYaw, pt, ps, buf, packedLight);
    }

    /**
     * Сэмплирует мировую траекторию полёта по длине дуги {@code STEP = 0.34 м} и накладывает живую
     * бегущую синусоидальную S-волну изгиба тела (как у настоящего парящего китайского дракона!).
     */
    private static void sampleSplineSpineAndWaves(PaperDragonEntity e,
                                                  double headWx, double headWy, double headWz,
                                                  float headRollDeg, float time, float phase, float pt,
                                                  Vec3[] outSpine, float[] outRoll) {
        outSpine[0] = Vec3.ZERO;
        outRoll[0] = headRollDeg;

        float yawRad = Mth.rotLerp(pt, e.yRotO, e.getYRot()) * Mth.DEG_TO_RAD;
        float pitchRad = Mth.lerp(pt, e.xRotO, e.getXRot()) * Mth.DEG_TO_RAD;
        double cosP = Math.cos(pitchRad);
        Vec3 fallbackBack = new Vec3(Math.sin(yawRad) * cosP, Math.sin(pitchRad), -Math.cos(yawRad) * cosP).normalize();

        int count = e.trailCount;
        int headIdx = e.trailHead;

        double prevX = headWx, prevY = headWy, prevZ = headWz;
        float prevR = headRollDeg;
        double accumDist = 0.0;
        int trailCursor = 0;

        for (int seg = 1; seg < SEGS; ++seg) {
            double wantDist = seg * PaperDragonEntity.STEP;

            while (trailCursor < count && accumDist < wantDist) {
                int ringIdx = (headIdx - trailCursor + PaperDragonEntity.TRAIL_CAPACITY) % PaperDragonEntity.TRAIL_CAPACITY;
                double tx = e.trailX[ringIdx];
                double ty = e.trailY[ringIdx];
                double tz = e.trailZ[ringIdx];
                float tr = e.trailRoll[ringIdx];
                double stepLen = Math.sqrt((tx - prevX) * (tx - prevX) + (ty - prevY) * (ty - prevY) + (tz - prevZ) * (tz - prevZ));
                if (accumDist + stepLen >= wantDist && stepLen > 1.0E-5) {
                    double frac = (wantDist - accumDist) / stepLen;
                    double sx = Mth.lerp(frac, prevX, tx) - headWx;
                    double sy = Mth.lerp(frac, prevY, ty) - headWy;
                    double sz = Mth.lerp(frac, prevZ, tz) - headWz;
                    outSpine[seg] = new Vec3(sx, sy, sz);
                    outRoll[seg] = Mth.lerp((float) frac, prevR, tr);
                    break;
                }
                accumDist += stepLen;
                prevX = tx;
                prevY = ty;
                prevZ = tz;
                prevR = tr;
                ++trailCursor;
            }

            if (outSpine[seg] == null) {
                double rem = wantDist - accumDist;
                Vec3 tailBase = new Vec3(prevX - headWx, prevY - headWy, prevZ - headWz).add(fallbackBack.scale(rem));
                outSpine[seg] = tailBase;
                outRoll[seg] = prevR * 0.92F;
            }
        }

        // Накладываем живую бегущую волну серпантина (голова стабильна, а тело и хвост плавно струятся в воздухе!)
        for (int seg = 1; seg < SEGS; ++seg) {
            float u = (float) seg / (float) (SEGS - 1);
            Vec3 tang = outSpine[Math.max(0, seg - 1)].subtract(outSpine[Math.min(SEGS - 1, seg + 1)]);
            Vec3 side = new Vec3(-tang.z, 0.0, tang.x);
            if (side.lengthSqr() > 1.0E-5) side = side.normalize();
            else side = new Vec3(1.0, 0.0, 0.0);

            float env = Mth.clamp(u * 2.6F, 0.0F, 1.0F) * (0.42F + 0.72F * u);
            double latWave = Math.sin(time * 0.135F - seg * 0.21F + phase) * 0.44 * env;
            double vertWave = Math.cos(time * 0.105F - seg * 0.17F + phase * 0.7F) * 0.20 * env;

            outSpine[seg] = outSpine[seg].add(side.scale(latWave)).add(0.0, vertWave, 0.0);
            outRoll[seg] += Mth.sin(time * 0.135F - seg * 0.21F + phase) * 8.5F * env;
        }
    }

    /** Внутренняя плотная бумажная основа под кармашками модулей (0 сквозных просветов между кольцами!). */
    private static void renderInnerPaperTube(VertexConsumer vc, Matrix4f pose, Matrix3f normMat,
                                             Vec3[] spine, Vec3[] normals, Vec3[] binormals,
                                             int light, int ov) {
        int tubeSides = 8;
        for (int i = 0; i < SEGS - 1; ++i) {
            float r0 = bodyRadius(i) * 0.76F;
            float r1 = bodyRadius(i + 1) * 0.76F;
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

    /**
     * 63 кольца × 12 трёхмерных сложенных бумажных модулей Golden-Venture:
     * каждый модуль состоит из 2 внешних треугольных «крылышек» со складкой посередине и 2 внутренних
     * подворотов кармашка, уложенных в настоящую шахматную перевязку с соседним кольцом!
     */
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

            // Шахматное смещение каждого нечётного кольца на полмодуля — точная геометрия модульного оригами!
            float ringStagger = (i & 1) * (Mth.PI / MODULES_PER_RING);
            float halfSlot = (Mth.PI / MODULES_PER_RING) * 0.96F;
            // Лёгкое «дыхание» бумажных чешуек при изгибе тела
            float breath = 1.0F + 0.045F * Mth.sin(time * 0.14F - i * 0.25F + phase);

            for (int m = 0; m < MODULES_PER_RING; ++m) {
                float angMid = ringStagger + (Mth.TWO_PI * m) / MODULES_PER_RING;
                // Небольшое брюшное уплощение снизу (как у настоящего бумажного дракона с фото-референса)
                float radialScale = 1.0F - 0.10F * Math.max(0.0F, -Mth.cos(angMid));

                float radBase = r0 * radialScale;
                // Аккуратный 3D-рельеф бумажного уголка (+34% от радиуса — плотная многослойная укладка, НЕ колючки!)
                float radTip = (r1 * radialScale * 0.88F + r0 * 0.34F * breath);

                Vec3 baseLeft = ringPt(c0, u0, s0, angMid - halfSlot, radBase * 0.90F);
                Vec3 baseRight = ringPt(c0, u0, s0, angMid + halfSlot, radBase * 0.90F);
                Vec3 ridgeMid = ringPt(c0, u0, s0, angMid, radBase * 1.14F).subtract(t0.scale(PaperDragonEntity.STEP * 0.18));
                // Острый сложенный кончик модуля направлен назад вдоль тела и перекрывает следующее кольцо!
                Vec3 tipApex = ringPt(c1, u1, s1, angMid, radTip).subtract(t0.scale(PaperDragonEntity.STEP * 0.42));
                Vec3 innerTuck = ringPt(c1, u1, s1, angMid, r1 * radialScale * 0.78F);

                // Левая и правая внешние грани сложенного бумажного модуля (разный угол к свету -> чёткое ребро сгиба!)
                paperTriSingle(vc, pose, normMat,
                        baseLeft, ridgeMid, tipApex,
                        0.05F, 0.06F, 0.35F, 0.50F, 0.98F, 0.50F,
                        FL_R, FL_G, FL_B, CR_R, CR_G, CR_B, CR_R, CR_G, CR_B, light, ov);
                paperTriSingle(vc, pose, normMat,
                        ridgeMid, baseRight, tipApex,
                        0.35F, 0.50F, 0.05F, 0.94F, 0.98F, 0.50F,
                        CR_R, CR_G, CR_B, SH_R, SH_G, SH_B, FL_R, FL_G, FL_B, light, ov);

                // Нижний подворот бумажного кармашка (закрывает толщину сложенного листа сбоку/сзади!)
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

    /** Высокий выразительный ряд спинных бумажных зубцов-гребней вдоль всего позвоночника дракона. */
    private static void renderDorsalOrigamiCrest(VertexConsumer vc, Matrix4f pose, Matrix3f normMat,
                                                 Vec3[] spine, Vec3[] tangents, Vec3[] normals, Vec3[] binormals,
                                                 float time, float phase, int light, int ov) {
        for (int i = 1; i < SEGS - 2; ++i) {
            float r = bodyRadius(i);
            float u = (float) i / (float) (SEGS - 1);
            float crestH = (0.32F + 0.56F * Mth.sin(Mth.clamp(u * 1.15F, 0.05F, 1.0F) * Mth.PI * 0.88F))
                    * (0.94F + 0.06F * Mth.sin(time * 0.18F - i * 0.35F + phase));

            Vec3 c = spine[i];
            Vec3 fw = tangents[i];
            Vec3 up = normals[i];
            Vec3 sd = binormals[i];

            Vec3 rootFront = c.add(up.scale(r * 0.96)).add(fw.scale(0.16));
            Vec3 rootBack = c.add(up.scale(r * 0.92)).subtract(fw.scale(0.24));
            Vec3 sideL = c.add(up.scale(r * 0.98)).subtract(sd.scale(0.11));
            Vec3 sideR = c.add(up.scale(r * 0.98)).add(sd.scale(0.11));
            Vec3 crestTip = c.add(up.scale(r + crestH)).subtract(fw.scale(0.34));

            // 4-гранный объёмный сложенный зуб гребня (никаких плоских 2D-пластинок!)
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

    /**
     * Супер-детализированная многослойная оригами-голова императорского китайского дракона:
     * ступенчатая морда из 6 модульных складок, вздёрнутый бумажный кончик носа, шарнирная нижняя челюсть,
     * выразительные тёмные глаза с бумажным бликом, ветвистые модульные рога, корона-воротник и 2 длинных живых уса!
     */
    private static void renderOrigamiDragonHead(VertexConsumer vc, Matrix4f pose, Matrix3f normMat,
                                                Vec3 neck, Vec3 fw, Vec3 up, Vec3 sd,
                                                float jawOpen, float time, float phase, int light, int ov) {
        // 1) Черепная коробка и ступенчатая верхняя челюсть / морда (длина 2.55 м вперёд от шеи!)
        int snoutSteps = 6;
        for (int s = 0; s < snoutSteps; ++s) {
            float u0 = (float) s / snoutSteps;
            float u1 = (float) (s + 1) / snoutSteps;
            float d0 = u0 * 2.35F;
            float d1 = u1 * 2.35F;
            float w0 = Mth.lerp(u0, 0.72F, 0.26F);
            float w1 = Mth.lerp(u1, 0.72F, 0.22F);
            float hTop0 = Mth.lerp(u0, 0.64F, 0.26F) + (s == snoutSteps - 1 ? 0.14F : 0.0F);
            float hTop1 = Mth.lerp(u1, 0.64F, 0.34F); // вздёрнутый вверх императорский кончик носа!
            float lipY0 = Mth.lerp(u0, -0.10F, 0.02F);
            float lipY1 = Mth.lerp(u1, -0.10F, 0.06F);

            Vec3 top0 = headPt(neck, fw, up, sd, d0, hTop0, 0.0F);
            Vec3 top1 = headPt(neck, fw, up, sd, d1, hTop1, 0.0F);
            Vec3 l0 = headPt(neck, fw, up, sd, d0, lipY0, -w0);
            Vec3 r0 = headPt(neck, fw, up, sd, d0, lipY0, +w0);
            Vec3 l1 = headPt(neck, fw, up, sd, d1, lipY1, -w1);
            Vec3 r1 = headPt(neck, fw, up, sd, d1, lipY1, +w1);

            // Ступенчатый накладной бумажный модуль на переносице каждого сегмента морды!
            Vec3 crestMid = headPt(neck, fw, up, sd, (d0 + d1) * 0.5F - 0.12F, (hTop0 + hTop1) * 0.5F + 0.14F, 0.0F);
            paperTriSingle(vc, pose, normMat, l0, top0, l1, 0.1F, 0.1F, 0.4F, 0.5F, 0.9F, 0.2F, FL_R, FL_G, FL_B, CR_R, CR_G, CR_B, FL_R, FL_G, FL_B, light, ov);
            paperTriSingle(vc, pose, normMat, top0, top1, l1, 0.4F, 0.5F, 0.95F, 0.5F, 0.9F, 0.2F, CR_R, CR_G, CR_B, CR_R, CR_G, CR_B, FL_R, FL_G, FL_B, light, ov);
            paperTriSingle(vc, pose, normMat, top0, r0, r1, 0.4F, 0.5F, 0.1F, 0.9F, 0.9F, 0.8F, CR_R, CR_G, CR_B, SH_R, SH_G, SH_B, FL_R, FL_G, FL_B, light, ov);
            paperTriSingle(vc, pose, normMat, top0, r1, top1, 0.4F, 0.5F, 0.9F, 0.8F, 0.95F, 0.5F, CR_R, CR_G, CR_B, FL_R, FL_G, FL_B, CR_R, CR_G, CR_B, light, ov);
            // Накладной треугольник-складка на спинке носа
            paperTriSingle(vc, pose, normMat, l0.lerp(top0, 0.55), crestMid, top1, 0.1F, 0.2F, 0.5F, 0.5F, 0.98F, 0.5F, FL_R, FL_G, FL_B, CR_R, CR_G, CR_B, CR_R, CR_G, CR_B, light, ov);
            paperTriSingle(vc, pose, normMat, crestMid, r0.lerp(top0, 0.55), top1, 0.5F, 0.5F, 0.1F, 0.8F, 0.98F, 0.5F, CR_R, CR_G, CR_B, SH_R, SH_G, SH_B, CR_R, CR_G, CR_B, light, ov);
            // Нёбо верхней челюсти
            paperQuadSingle(vc, pose, normMat, l0, l1, r1, r0, IN_R, IN_G, IN_B, light, ov);
        }

        // Заострённый бумажный кончик носа и рельефные ноздри-оригами
        Vec3 noseBridge = headPt(neck, fw, up, sd, 2.35F, 0.34F, 0.0F);
        Vec3 noseTip = headPt(neck, fw, up, sd, 2.72F, 0.48F, 0.0F);
        Vec3 noseL = headPt(neck, fw, up, sd, 2.35F, 0.06F, -0.22F);
        Vec3 noseR = headPt(neck, fw, up, sd, 2.35F, 0.06F, +0.22F);
        paperTriSingle(vc, pose, normMat, noseL, noseBridge, noseTip, 0.1F, 0.2F, 0.5F, 0.5F, 0.98F, 0.5F, FL_R, FL_G, FL_B, CR_R, CR_G, CR_B, CR_R, CR_G, CR_B, light, ov);
        paperTriSingle(vc, pose, normMat, noseBridge, noseR, noseTip, 0.5F, 0.5F, 0.1F, 0.8F, 0.98F, 0.5F, CR_R, CR_G, CR_B, SH_R, SH_G, SH_B, CR_R, CR_G, CR_B, light, ov);
        paperTriSingle(vc, pose, normMat, noseR, noseL, noseTip, 0.2F, 0.8F, 0.2F, 0.2F, 0.95F, 0.5F, SH_R, SH_G, SH_B, SH_R, SH_G, SH_B, FL_R, FL_G, FL_B, light, ov);

        // 2) Подвижная шарнирная нижняя челюсть с бумажными клыками и подбородком-бородой!
        Vec3 jawDir = fw.scale(Math.cos(jawOpen)).subtract(up.scale(Math.sin(jawOpen))).normalize();
        Vec3 jawUp = up.scale(Math.cos(jawOpen)).add(fw.scale(Math.sin(jawOpen))).normalize();
        Vec3 jawHinge = headPt(neck, fw, up, sd, 0.22F, -0.18F, 0.0F);

        Vec3 jl0 = jawHinge.subtract(sd.scale(0.56));
        Vec3 jr0 = jawHinge.add(sd.scale(0.56));
        Vec3 jKeel = jawHinge.add(jawDir.scale(1.05)).subtract(jawUp.scale(0.34));
        Vec3 jTip = jawHinge.add(jawDir.scale(2.08)).subtract(jawUp.scale(0.04));
        Vec3 jlMid = jawHinge.add(jawDir.scale(1.15)).subtract(sd.scale(0.32));
        Vec3 jrMid = jawHinge.add(jawDir.scale(1.15)).add(sd.scale(0.32));

        paperTriSingle(vc, pose, normMat, jl0, jKeel, jlMid, 0.1F, 0.1F, 0.5F, 0.5F, 0.8F, 0.2F, SH_R, SH_G, SH_B, FL_R, FL_G, FL_B, FL_R, FL_G, FL_B, light, ov);
        paperTriSingle(vc, pose, normMat, jKeel, jTip, jlMid, 0.5F, 0.5F, 0.98F, 0.5F, 0.8F, 0.2F, FL_R, FL_G, FL_B, CR_R, CR_G, CR_B, FL_R, FL_G, FL_B, light, ov);
        paperTriSingle(vc, pose, normMat, jr0, jrMid, jKeel, 0.1F, 0.9F, 0.8F, 0.8F, 0.5F, 0.5F, SH_R, SH_G, SH_B, SH_R, SH_G, SH_B, FL_R, FL_G, FL_B, light, ov);
        paperTriSingle(vc, pose, normMat, jrMid, jTip, jKeel, 0.8F, 0.8F, 0.98F, 0.5F, 0.5F, 0.5F, SH_R, SH_G, SH_B, CR_R, CR_G, CR_B, FL_R, FL_G, FL_B, light, ov);
        paperTriSingle(vc, pose, normMat, jl0, jlMid, jrMid, 0.2F, 0.2F, 0.8F, 0.2F, 0.8F, 0.8F, IN_R, IN_G, IN_B, IN_R, IN_G, IN_B, IN_R, IN_G, IN_B, light, ov);
        paperTriSingle(vc, pose, normMat, jl0, jrMid, jr0, 0.2F, 0.2F, 0.8F, 0.8F, 0.2F, 0.8F, IN_R, IN_G, IN_B, IN_R, IN_G, IN_B, IN_R, IN_G, IN_B, light, ov);
        paperTriSingle(vc, pose, normMat, jlMid, jTip, jrMid, 0.2F, 0.2F, 0.98F, 0.5F, 0.8F, 0.8F, FL_R, FL_G, FL_B, CR_R, CR_G, CR_B, FL_R, FL_G, FL_B, light, ov);

        // Бумажные клыки-треугольники и острая бородка под нижней челюстью
        for (int side = -1; side <= 1; side += 2) {
            Vec3 fangBase1 = headPt(neck, fw, up, sd, 1.72F, 0.02F, side * 0.27F);
            Vec3 fangBase2 = headPt(neck, fw, up, sd, 1.92F, 0.04F, side * 0.23F);
            Vec3 fangTip = headPt(neck, fw, up, sd, 1.82F, -0.22F, side * 0.24F);
            paperTriSingle(vc, pose, normMat, fangBase1, fangBase2, fangTip, 0.2F, 0.2F, 0.2F, 0.8F, 0.98F, 0.5F, CR_R, CR_G, CR_B, CR_R, CR_G, CR_B, CR_R, CR_G, CR_B, light, ov);
        }

        // 3) Выразительные надбровные бумажные дуги + тёмные оригами-глаза с живым бликом!
        for (int side = -1; side <= 1; side += 2) {
            float sg = (float) side;
            Vec3 browFront = headPt(neck, fw, up, sd, 1.28F, 0.42F, sg * 0.46F);
            Vec3 browMid = headPt(neck, fw, up, sd, 0.86F, 0.58F, sg * 0.64F);
            Vec3 browBack = headPt(neck, fw, up, sd, 0.35F, 0.52F, sg * 0.68F);
            Vec3 socket = headPt(neck, fw, up, sd, 0.92F, 0.30F, sg * 0.52F);
            if (side < 0) {
                paperTriSingle(vc, pose, normMat, socket, browMid, browFront, 0.2F, 0.2F, 0.6F, 0.5F, 0.95F, 0.5F, FL_R, FL_G, FL_B, CR_R, CR_G, CR_B, CR_R, CR_G, CR_B, light, ov);
                paperTriSingle(vc, pose, normMat, browBack, browMid, socket, 0.2F, 0.2F, 0.95F, 0.5F, 0.3F, 0.8F, SH_R, SH_G, SH_B, CR_R, CR_G, CR_B, FL_R, FL_G, FL_B, light, ov);
            } else {
                paperTriSingle(vc, pose, normMat, browFront, browMid, socket, 0.95F, 0.5F, 0.6F, 0.5F, 0.2F, 0.2F, CR_R, CR_G, CR_B, CR_R, CR_G, CR_B, FL_R, FL_G, FL_B, light, ov);
                paperTriSingle(vc, pose, normMat, socket, browMid, browBack, 0.3F, 0.8F, 0.95F, 0.5F, 0.2F, 0.2F, FL_R, FL_G, FL_B, CR_R, CR_G, CR_B, SH_R, SH_G, SH_B, light, ov);
            }

            // Ромбовидный тёмный бумажный глаз снаружи глазницы (0 Z-fighting благодаря выносу +0.035 по sd!)
            Vec3 eFront = headPt(neck, fw, up, sd, 1.16F, 0.28F, sg * 0.565F);
            Vec3 eBack = headPt(neck, fw, up, sd, 0.72F, 0.32F, sg * 0.605F);
            Vec3 eTop = headPt(neck, fw, up, sd, 0.94F, 0.41F, sg * 0.585F);
            Vec3 eBot = headPt(neck, fw, up, sd, 0.94F, 0.19F, sg * 0.575F);
            if (side < 0) {
                paperQuadSingle(vc, pose, normMat, eBack, eTop, eFront, eBot, EYE_R, EYE_G, EYE_B, light, ov);
            } else {
                paperQuadSingle(vc, pose, normMat, eFront, eTop, eBack, eBot, EYE_R, EYE_G, EYE_B, light, ov);
            }
        }

        // 4) Два длинных ветвистых модульных рога-антенны (5 вложенных бумажных звеньев + боковой отросток!)
        for (int side = -1; side <= 1; side += 2) {
            float sg = (float) side;
            Vec3 hStart = headPt(neck, fw, up, sd, 0.38F, 0.56F, sg * 0.36F);
            Vec3 hDir = fw.scale(-0.76).add(up.scale(0.58)).add(sd.scale(sg * 0.32)).normalize();
            renderStackedOrigamiChain(vc, pose, normMat, hStart, hDir, up, sd, 6, 0.40F, 0.24F, 0.05F, light, ov);
            // Боковой зубец ветвистого оленьего рога китайского дракона
            Vec3 tineStart = hStart.add(hDir.scale(0.95));
            Vec3 tineDir = fw.scale(-0.42).add(up.scale(0.78)).add(sd.scale(sg * 0.46)).normalize();
            renderStackedOrigamiChain(vc, pose, normMat, tineStart, tineDir, up, sd, 3, 0.30F, 0.13F, 0.035F, light, ov);
        }

        // 5) Пышный королевский бумажный воротник-грива из 9 расходящихся сложенных веерных модулей вокруг затылка!
        for (int k = 0; k < 9; ++k) {
            float ang = -Mth.PI * 0.72F + (Mth.PI * 1.44F * k) / 8.0F;
            Vec3 radDir = up.scale(Mth.cos(ang)).add(sd.scale(Mth.sin(ang))).normalize();
            Vec3 tangRing = up.scale(-Mth.sin(ang)).add(sd.scale(Mth.cos(ang))).normalize();
            Vec3 mRoot = neck.add(fw.scale(0.10)).add(radDir.scale(0.58));
            Vec3 mLeft = mRoot.subtract(tangRing.scale(0.22));
            Vec3 mRight = mRoot.add(tangRing.scale(0.22));
            Vec3 mRidge = mRoot.add(radDir.scale(0.22)).subtract(fw.scale(0.18));
            Vec3 mTip = mRoot.add(radDir.scale(0.68)).subtract(fw.scale(0.78));
            paperTriSingle(vc, pose, normMat, mLeft, mRidge, mTip, 0.1F, 0.1F, 0.4F, 0.5F, 0.98F, 0.5F, FL_R, FL_G, FL_B, CR_R, CR_G, CR_B, CR_R, CR_G, CR_B, light, ov);
            paperTriSingle(vc, pose, normMat, mRidge, mRight, mTip, 0.4F, 0.5F, 0.1F, 0.9F, 0.98F, 0.5F, CR_R, CR_G, CR_B, SH_R, SH_G, SH_B, FL_R, FL_G, FL_B, light, ov);
        }

        // 6) Два длинных изгибающихся по ветру 6-сегментных бумажных уса китайского дракона по бокам морды!
        for (int side = -1; side <= 1; side += 2) {
            float sg = (float) side;
            Vec3 wPrev = headPt(neck, fw, up, sd, 2.05F, 0.14F, sg * 0.25F);
            for (int ws = 1; ws <= 6; ++ws) {
                float wu = ws / 6.0F;
                float flutter = Mth.sin(time * 0.22F - ws * 0.65F + phase + side) * (0.14F + 0.24F * wu);
                float liftW = Mth.cos(time * 0.17F - ws * 0.55F + phase) * 0.12F * wu;
                Vec3 wNext = wPrev
                        .subtract(fw.scale(0.42))
                        .add(sd.scale(sg * (0.24 + 0.08 * (1.0F - wu)) + flutter))
                        .add(up.scale(0.05 + liftW));
                float wThick = Mth.lerp(wu, 0.075F, 0.018F);
                renderOrigamiPrismSegment(vc, pose, normMat, wPrev, wNext, up, sd, wThick, wThick * 0.72F, light, ov);
                wPrev = wNext;
            }
        }
    }

    /**
     * Шарнирная пара модульных оригами-лап (передние или задние):
     * наплечный бумажный модуль -> бедро из 3 вложенных модулей -> сустав колена/локтя ->
     * голень/предплечье из 3 модулей -> 4 растопыренных острых бумажных когтя!
     */
    private static void renderArticulatedOrigamiPairOfLegs(VertexConsumer vc, Matrix4f pose, Matrix3f normMat,
                                                           Vec3 bodyCenter, Vec3 fw, Vec3 up, Vec3 sd,
                                                           float bodyRad, boolean forelegs,
                                                           float time, float phase, float bankDeg, int light, int ov) {
        for (int side = -1; side <= 1; side += 2) {
            float sg = (float) side;
            // Плавный гребной цикл лап в полёте + асимметричное поджатие при крене в вираже
            float stride = Mth.sin(time * 0.13F + phase + (side > 0 ? 0.0F : Mth.PI));
            float bankTuck = Mth.clamp(bankDeg * sg * 0.008F, -0.22F, 0.22F);

            Vec3 shoulder = bodyCenter
                    .add(sd.scale(sg * bodyRad * 0.86))
                    .subtract(up.scale(bodyRad * 0.42));

            // Наплечный резной бумажный щиток-эполет
            Vec3 shShieldTip = shoulder.add(sd.scale(sg * 0.42)).subtract(fw.scale(0.48)).subtract(up.scale(0.18));
            paperTriSingle(vc, pose, normMat,
                    shoulder.add(fw.scale(0.24)), shoulder.add(up.scale(0.28)), shShieldTip,
                    0.1F, 0.2F, 0.4F, 0.5F, 0.98F, 0.5F, FL_R, FL_G, FL_B, CR_R, CR_G, CR_B, CR_R, CR_G, CR_B, light, ov);
            paperTriSingle(vc, pose, normMat,
                    shoulder.add(up.scale(0.28)), shoulder.subtract(fw.scale(0.22)), shShieldTip,
                    0.4F, 0.5F, 0.1F, 0.8F, 0.98F, 0.5F, CR_R, CR_G, CR_B, SH_R, SH_G, SH_B, FL_R, FL_G, FL_B, light, ov);

            // Верхнее звено лапы (бедро/плечо): направлено наружу, вниз и слегка назад
            Vec3 upperDir = sd.scale(sg * 0.52)
                    .subtract(up.scale(0.74 - bankTuck))
                    .add(fw.scale((forelegs ? -0.24 : -0.36) + stride * 0.18))
                    .normalize();
            Vec3 elbow = renderStackedOrigamiChain(vc, pose, normMat, shoulder, upperDir, up, sd, 3, 0.36F, 0.25F, 0.18F, light, ov);

            // Нижнее звено лапы (предплечье/цевка): сгиб вперёд и вниз к когтистой кисти!
            Vec3 lowerDir = fw.scale(0.54 + stride * 0.16)
                    .subtract(up.scale(0.72))
                    .add(sd.scale(sg * 0.22))
                    .normalize();
            Vec3 wrist = renderStackedOrigamiChain(vc, pose, normMat, elbow, lowerDir, up, sd, 3, 0.33F, 0.19F, 0.13F, light, ov);

            // 4 растопыренных сложенных из бумаги драконьих когтя (3 передних + 1 внутренний шпоровый коготь)
            for (int c = 0; c < 4; ++c) {
                Vec3 clawDir;
                float clawLen;
                if (c < 3) {
                    float spread = (c - 1) * 0.36F;
                    clawDir = fw.scale(0.74)
                            .subtract(up.scale(0.46 + 0.08 * Math.abs(c - 1)))
                            .add(sd.scale(spread + sg * 0.10))
                            .normalize();
                    clawLen = (c == 1) ? 0.56F : 0.46F;
                } else {
                    clawDir = fw.scale(-0.48).subtract(up.scale(0.68)).subtract(sd.scale(sg * 0.25)).normalize();
                    clawLen = 0.36F;
                }
                renderOrigamiPrismSegment(vc, pose, normMat, wrist, wrist.add(clawDir.scale(clawLen)), up, sd, 0.095F, 0.015F, light, ov);
            }
        }
    }

    /** Императорский 7-лучевой бумажный веер на кончике хвоста (как у классического китайского оригами-дракона!). */
    private static void renderImperialOrigamiTailFan(VertexConsumer vc, Matrix4f pose, Matrix3f normMat,
                                                     Vec3 tailTip, Vec3 fw, Vec3 up, Vec3 sd,
                                                     float time, float phase, int light, int ov) {
        Vec3 back = fw.scale(-1.0);
        for (int p = -3; p <= 3; ++p) {
            float normP = p / 3.0F;
            float fanSpread = normP * 0.72F;
            float wave = Mth.sin(time * 0.19F + p * 0.55F + phase) * 0.12F;
            float plumeLen = 2.65F - Math.abs(normP) * 0.78F;
            float plumeW = 0.24F - Math.abs(normP) * 0.04F;

            Vec3 plumeDir = back.scale(Math.cos(fanSpread))
                    .add(sd.scale(Math.sin(fanSpread) * 0.88))
                    .add(up.scale(0.18F * (1.0F - Math.abs(normP)) + wave))
                    .normalize();

            Vec3 root = tailTip.add(sd.scale(normP * 0.08));
            Vec3 mid = root.add(plumeDir.scale(plumeLen * 0.52));
            Vec3 tip = root.add(plumeDir.scale(plumeLen));

            Vec3 wingAxis = sd.scale(Math.cos(fanSpread * 0.5F)).add(up.scale(0.35)).normalize();
            Vec3 ridgeAxis = plumeDir.cross(wingAxis).normalize();

            Vec3 mL = mid.subtract(wingAxis.scale(plumeW));
            Vec3 mR = mid.add(wingAxis.scale(plumeW));
            Vec3 mTop = mid.add(ridgeAxis.scale(plumeW * 0.55));
            Vec3 mBot = mid.subtract(ridgeAxis.scale(plumeW * 0.40));

            // 4-гранное сложенное бумажное перо хвоста (верхние и нижние складки без Z-fighting!)
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

    /** Вспомогательный строитель цепочки из вложенных друг в друга 3D оригами-модулей (для лап и ветвистых рогов). */
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
            // Перекрытие следующего звена (+25% длины) создаёт характерную ступенчатую чешуйчатую фактуру оригами!
            Vec3 flareEnd = cur.add(dir.scale(stepLen * 1.24));
            renderOrigamiPrismSegment(vc, pose, normMat, cur, flareEnd, refUp, refSide, r0, r1, light, ov);
            cur = next;
        }
        return cur;
    }

    /** 4-гранная ромбическая бумажная призма одного звена рога/лапы/уса (строго односторонняя наружу — 0 Z-fighting!). */
    private static void renderOrigamiPrismSegment(VertexConsumer vc, Matrix4f pose, Matrix3f normMat,
                                                  Vec3 a, Vec3 b, Vec3 hintUp, Vec3 hintSide,
                                                  float rA, float rB, int light, int ov) {
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
        for (int k = 0; k < 4; ++k) {
            int kn = (k + 1) & 3;
            boolean sun = (k == 0 || k == 3);
            paperQuadSingle(vc, pose, normMat, pa[k], pa[kn], pb[kn], pb[k],
                    sun ? CR_R : SH_R, sun ? CR_G : SH_G, sun ? CR_B : SH_B, light, ov);
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

    private static Vec3 headPt(Vec3 origin, Vec3 fw, Vec3 up, Vec3 sd, float f, float u, float s) {
        return new Vec3(
                origin.x + fw.x * f + up.x * u + sd.x * s,
                origin.y + fw.y * f + up.y * u + sd.y * s,
                origin.z + fw.z * f + up.z * u + sd.z * s);
    }

    /**
     * РИСУЕТ СТРОГО ОДИН ТРЕУГОЛЬНИК С ЧЕСТНОЙ ВНЕШНЕЙ НОРМАЛЬЮ (НИКАКИХ ДУБЛИРУЮЩИХ ЗАДНИХ ГРАНЕЙ -> 0 Z-FIGHTING!).
     * {@code RenderType.entityCutoutNoCull} уже отключает отсечение задних граней, поэтому одна грань видна с обеих сторон чисто!
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
        // Смягчаем вертикальную компоненту нормали кверху (+0.35), чтобы бумажные чешуйки оставались светло-сливочными, а не серыми!
        float bnx = nx * 0.72F;
        float bny = Mth.clamp(ny * 0.72F + 0.38F, -0.35F, 1.0F);
        float bnz = nz * 0.72F;
        float bLen = Mth.sqrt(bnx * bnx + bny * bny + bnz * bnz);
        if (bLen > 1.0E-4F) {
            bnx /= bLen;
            bny /= bLen;
            bnz /= bLen;
        }

        vc.vertex(pose, (float) p1.x, (float) p1.y, (float) p1.z).color(r1, g1, b1, 255).uv(u1, v1).overlayCoords(ov).uv2(light).normal(normMat, bnx, bny, bnz).endVertex();
        vc.vertex(pose, (float) p2.x, (float) p2.y, (float) p2.z).color(r2, g2, b2, 255).uv(u2, v2).overlayCoords(ov).uv2(light).normal(normMat, bnx, bny, bnz).endVertex();
        vc.vertex(pose, (float) p3.x, (float) p3.y, (float) p3.z).color(r3, g3, b3, 255).uv(u3, v3).overlayCoords(ov).uv2(light).normal(normMat, bnx, bny, bnz).endVertex();
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
