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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import ru.aiforge.riftworld.entity.PaperDragonEntity;

/**
 * v0.20.0: Процедурный 3D-рендерер модульного белого оригами-дракона в китайском стиле
 * (1:1 по {@code reports/origami_water_refs/01_origami_dragon_ref.png}).
 *
 * <p>Тело из 36 телескопических накладывающихся друг на друга бумажных воротников-модулей
 * (Golden-Venture 3D origami) с острыми отогнутыми назад треугольными чешуями, глубокими
 * теневыми карманами в складках бумаги, угловатой оригами-головой китайского дракона с рогами
 * и усами, четырьмя сложенными бумажными лапами с тремя когтями и острым бумажным конусом хвоста.
 */
public class PaperDragonRenderer extends EntityRenderer<PaperDragonEntity> {
    private static final ResourceLocation WHITE_TEX = new ResourceLocation("minecraft", "textures/misc/white.png");

    private static final int SEGS = 36;
    private static final double STEP = 0.235;
    private static final int FLAPS = 8;

    // Чистая скульптурная палитра сложенной тёпло-белой бумаги (1:1 по 01_origami_dragon_ref.png)
    private static final int PAPER_LIT    = 0xFCFBF6;
    private static final int PAPER_WARM   = 0xF2EDE1;
    private static final int PAPER_SIDE   = 0xE2DACB;
    private static final int PAPER_CREASE = 0x7A7263;
    private static final int EYE_FOLD     = 0x3A352E;

    private static final Vec3 SUN_DIR = new Vec3(0.38, 0.84, 0.39).normalize();

    public PaperDragonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(PaperDragonEntity e, Frustum frustum, double cx, double cy, double cz) {
        AABB box = e.getBoundingBox().inflate(10.5, 6.5, 10.5);
        return frustum.isVisible(box);
    }

    @Override
    public void render(PaperDragonEntity e, float yaw, float pt, PoseStack pose, MultiBufferSource buf, int light) {
        e.ensureTrailInitialized();

        double hx = Mth.lerp(pt, e.xo, e.getX());
        double hy = Mth.lerp(pt, e.yo, e.getY()) + 0.52;
        double hz = Mth.lerp(pt, e.zo, e.getZ());
        Vec3 headWorld = new Vec3(hx, hy, hz);

        double age = e.tickCount + pt + (e.getId() & 31) * 4.3;

        // 1. Выборка 37 узлов хребта по реальной 3D-дуге полёта с фиксированным шагом STEP
        Vec3[] raw = sampleTrailArc(e, headWorld, yaw, pt);

        // 2. Накладываем плавную бегущую вертикальную и горизонтальную оригами-волну
        Vec3[] spine = new Vec3[SEGS + 1];
        spine[0] = Vec3.ZERO;
        for (int i = 1; i <= SEGS; ++i) {
            Vec3 prev = raw[i - 1];
            Vec3 next = raw[Math.min(SEGS, i + 1)];
            Vec3 dir = prev.subtract(next);
            if (dir.lengthSqr() < 1.0e-6) dir = new Vec3(0, 0, 1);
            dir = dir.normalize();
            Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
            if (side.lengthSqr() < 1.0e-5) side = new Vec3(1, 0, 0);
            side = side.normalize();
            Vec3 up = side.cross(dir).normalize();
            if (up.y < 0) up = up.scale(-1.0);

            double u = (double) i / SEGS;
            double env = Math.sin(Math.min(1.0, u * 2.8) * Math.PI * 0.5) * (1.0 - 0.35 * u * u);
            double latWave = Math.sin(i * 0.34 - age * 0.145) * 0.30 * env;
            double vertWave = Math.cos(i * 0.30 - age * 0.125) * 0.28 * env;
            spine[i] = raw[i].add(side.scale(latWave)).add(up.scale(vertWave));
        }
        // Перенормируем длину звеньев, чтобы бумажные модули не растягивались на крутых виражах
        for (int i = 1; i <= SEGS; ++i) {
            Vec3 d = spine[i].subtract(spine[i - 1]);
            double len = d.length();
            if (len < 1.0e-5) {
                d = new Vec3(0, 0, -1);
                len = 1.0;
            }
            spine[i] = spine[i - 1].add(d.scale(STEP / len));
        }

        // 3. Ортонормированный репер (T вперёд к голове, U вверх, R вправо) с параллельным переносом без скручивания
        Vec3[] T = new Vec3[SEGS + 1];
        Vec3[] U = new Vec3[SEGS + 1];
        Vec3[] R = new Vec3[SEGS + 1];

        Vec3 t0 = spine[0].subtract(spine[1]);
        if (t0.lengthSqr() < 1.0e-6) {
            double ry = Math.toRadians(Mth.rotLerp(pt, e.yRotO, e.getYRot()));
            t0 = new Vec3(-Math.sin(ry), 0.0, Math.cos(ry));
        }
        T[0] = t0.normalize();
        Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
        Vec3 r0 = T[0].cross(worldUp);
        if (r0.lengthSqr() < 1.0e-5) r0 = new Vec3(1.0, 0.0, 0.0);
        R[0] = r0.normalize();
        U[0] = R[0].cross(T[0]).normalize();

        for (int i = 1; i <= SEGS; ++i) {
            Vec3 ti = i < SEGS ? spine[i - 1].subtract(spine[i + 1]) : spine[i - 1].subtract(spine[i]);
            if (ti.lengthSqr() < 1.0e-6) ti = T[i - 1];
            T[i] = ti.normalize();
            // Параллельный перенос вектора U[i-1] с мягким возвратом к зениту, чтобы спинной гребень всегда смотрел вверх
            Vec3 uProj = U[i - 1].subtract(T[i].scale(U[i - 1].dot(T[i])));
            Vec3 skyProj = worldUp.subtract(T[i].scale(worldUp.dot(T[i])));
            if (skyProj.lengthSqr() > 1.0e-4) {
                uProj = uProj.scale(0.84).add(skyProj.normalize().scale(0.16));
            }
            if (uProj.lengthSqr() < 1.0e-5) uProj = worldUp;
            U[i] = uProj.normalize();
            R[i] = T[i].cross(U[i]).normalize();
        }

        // Радиус тела по длине (средних размеров: плотный торс ~0.36..0.40, плавное сужение к шее и хвосту)
        double[] rad = new double[SEGS + 1];
        for (int i = 0; i <= SEGS; ++i) {
            double u = (double) i / SEGS;
            double neck = 0.78 + 0.22 * Mth.clamp(i / 4.0, 0.0, 1.0);
            double taper = Math.pow(Math.max(0.06, 1.0 - u * 0.88), 0.72);
            rad[i] = 0.38 * neck * taper;
        }

        int brightLight = Math.max(light, LightTexture.pack(12, 15));
        pose.pushPose();
        pose.translate(0.0, 0.52, 0.0);
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(WHITE_TEX));

        // 4. 36 модульных оригами-воротников тела со ступенчатыми треугольными складками и спинным гребнем
        for (int i = 0; i < SEGS; ++i) {
            drawOrigamiCollar(vc, pose, spine[i], spine[i + 1], T[i], U[i], R[i], U[i + 1], R[i + 1],
                    rad[i], rad[i + 1], i, brightLight);
        }

        // 5. Четыре сложенные бумажные лапы с тремя когтями (передняя и задняя пары, как на референсе)
        drawOrigamiLegs(vc, pose, spine, T, U, R, rad, age, brightLight);

        // 6. Угловатая многослойная оригами-голова китайского дракона
        drawOrigamiHead(vc, pose, spine[0], T[0], U[0], R[0], age, brightLight);

        // 7. Заострённый бумажный конус на кончике хвоста
        drawOrigamiTailCone(vc, pose, spine[SEGS], T[SEGS], U[SEGS], R[SEGS], rad[SEGS], brightLight);

        pose.popPose();
        super.render(e, yaw, pt, pose, buf, light);
    }

    /** Реконструкция точек хребта в мировых смещениях относительно головы. */
    private static Vec3[] sampleTrailArc(PaperDragonEntity e, Vec3 headWorld, float yaw, float pt) {
        Vec3[] out = new Vec3[SEGS + 1];
        out[0] = Vec3.ZERO;

        int n = e.trailCount;
        if (n < 2) {
            double ry = Math.toRadians(yaw);
            Vec3 back = new Vec3(Math.sin(ry), 0.0, -Math.cos(ry));
            for (int i = 1; i <= SEGS; ++i) {
                out[i] = back.scale(i * STEP);
            }
            return out;
        }

        Vec3 prevSample = Vec3.ZERO;
        double accDist = 0.0;
        int outIdx = 1;
        double wantDist = STEP;

        for (int k = 0; k < n && outIdx <= SEGS; ++k) {
            int ringIdx = (e.trailHead - 1 - k + PaperDragonEntity.TRAIL_CAPACITY) % PaperDragonEntity.TRAIL_CAPACITY;
            Vec3 curSample = new Vec3(
                    e.trailX[ringIdx] - headWorld.x,
                    (e.trailY[ringIdx] + 0.52) - headWorld.y,
                    e.trailZ[ringIdx] - headWorld.z);
            double segLen = curSample.distanceTo(prevSample);
            if (segLen < 1.0e-4) continue;
            while (accDist + segLen >= wantDist && outIdx <= SEGS) {
                double frac = (wantDist - accDist) / segLen;
                out[outIdx] = prevSample.lerp(curSample, frac);
                ++outIdx;
                wantDist += STEP;
            }
            accDist += segLen;
            prevSample = curSample;
        }

        if (outIdx <= SEGS) {
            Vec3 tailDir = outIdx >= 2 ? out[outIdx - 1].subtract(out[outIdx - 2]) : prevSample;
            if (tailDir.lengthSqr() < 1.0e-5) {
                double ry = Math.toRadians(Mth.rotLerp(pt, e.yRotO, e.getYRot()));
                tailDir = new Vec3(Math.sin(ry), 0.0, -Math.cos(ry));
            }
            tailDir = tailDir.normalize();
            while (outIdx <= SEGS) {
                out[outIdx] = out[outIdx - 1].add(tailDir.scale(STEP));
                ++outIdx;
            }
        }
        return out;
    }

    /**
     * Один модульный воротник 3D-оригами: 8 вложенных треугольных бумажных карманов со смещением вполшага
     * (кирпичная перевязка модулей как на фото 01_origami_dragon_ref.png) + острый спинной гребень!
     */
    private static void drawOrigamiCollar(VertexConsumer vc, PoseStack pose,
                                          Vec3 pFront, Vec3 pBack,
                                          Vec3 T, Vec3 U0, Vec3 R0, Vec3 U1, Vec3 R1,
                                          double r0, double r1, int segIdx, int light) {
        double stagger = (segIdx & 1) * (Math.PI / FLAPS);
        double frontIn = r0 * 0.77;
        double flareOut = r0 * 1.25;
        double nextIn = r1 * 0.76;

        // Отогнутый назад нахлёст бумажного лепестка (заходит поверх следующего сегмента, создавая глубокую тень!)
        Vec3 pLip = pBack.subtract(T.scale(STEP * 0.34));

        for (int f = 0; f < FLAPS; ++f) {
            double aL = stagger + (Math.PI * 2.0 * f) / FLAPS;
            double aM = stagger + (Math.PI * 2.0 * (f + 0.5)) / FLAPS;
            double aR = stagger + (Math.PI * 2.0 * (f + 1.0)) / FLAPS;

            Vec3 dirL0 = radial(U0, R0, aL);
            Vec3 dirM0 = radial(U0, R0, aM);
            Vec3 dirR0 = radial(U0, R0, aR);

            Vec3 dirL1 = radial(U1, R1, aL);
            Vec3 dirM1 = radial(U1, R1, aM);
            Vec3 dirR1 = radial(U1, R1, aR);

            // Усиление верхних и боковых гребней, как у сложенного бумажного дракона на фото
            double crestBoost = dirM0.dot(U0) > 0.55 ? 1.24 : (Math.abs(dirM0.dot(R0)) > 0.72 ? 1.12 : 1.0);

            Vec3 fL = pFront.add(dirL0.scale(frontIn));
            Vec3 fM = pFront.add(dirM0.scale(frontIn * 1.05));
            Vec3 fR = pFront.add(dirR0.scale(frontIn));

            Vec3 tip = pLip.add(dirM1.scale(flareOut * crestBoost));
            Vec3 valleyL = pBack.add(dirL1.scale(nextIn * 0.96));
            Vec3 valleyR = pBack.add(dirR1.scale(nextIn * 0.96));
            Vec3 pocket = pBack.add(dirM1.scale(nextIn * 0.84));

            int baseCol = (f & 1) == 0 ? PAPER_LIT : PAPER_WARM;
            // Две внешние грани сложенного бумажного модуля (образуют острую грань посередине)
            paperTri(vc, pose, fL, fM, tip, baseCol, light);
            paperTri(vc, pose, fM, fR, tip, PAPER_SIDE, light);
            // Боковые бумажные перепонки в ложбинах модуля
            paperTri(vc, pose, fL, tip, valleyL, PAPER_WARM, light);
            paperTri(vc, pose, tip, fR, valleyR, PAPER_SIDE, light);
            // Тёмный внутренний карман-складка под нависающим кончиком бумаги (ключевая деталь 3D-оригами!)
            paperTri(vc, pose, valleyL, tip, pocket, PAPER_CREASE, light);
            paperTri(vc, pose, pocket, tip, valleyR, PAPER_CREASE, light);
        }

        // Выразительный центральный спинной гребень из сложенных треугольников бумаги
        if (segIdx < SEGS - 2) {
            double hSpine = r0 * 1.62;
            Vec3 sFront = pFront.add(U0.scale(r0 * 0.92));
            Vec3 sLeft = pFront.add(U0.scale(r0 * 0.80)).subtract(R0.scale(r0 * 0.24)).subtract(T.scale(STEP * 0.45));
            Vec3 sRight = pFront.add(U0.scale(r0 * 0.80)).add(R0.scale(r0 * 0.24)).subtract(T.scale(STEP * 0.45));
            Vec3 sApex = pBack.add(U1.scale(hSpine)).subtract(T.scale(STEP * 0.48));
            Vec3 sBase = pBack.add(U1.scale(r1 * 0.86));

            paperTri(vc, pose, sFront, sLeft, sApex, PAPER_LIT, light);
            paperTri(vc, pose, sFront, sApex, sRight, PAPER_WARM, light);
            paperTri(vc, pose, sLeft, sBase, sApex, PAPER_CREASE, light);
            paperTri(vc, pose, sApex, sBase, sRight, PAPER_CREASE, light);
        }
    }

    /** Угловатая многослойная белая оригами-голова китайского дракона (1:1 с правой частью фото-референса). */
    private static void drawOrigamiHead(VertexConsumer vc, PoseStack pose,
                                        Vec3 h, Vec3 T, Vec3 U, Vec3 R, double age, int light) {
        double s = 0.50;
        double jawOpen = 0.08 + 0.03 * Math.sin(age * 0.09);

        // Верхняя гранёная морда + вздёрнутый бумажный рожок на кончике носа (точь-в-точь как на фото!)
        Vec3 snoutTip = h.add(T.scale(s * 2.15)).add(U.scale(s * 0.08));
        Vec3 noseHorn = snoutTip.subtract(T.scale(s * 0.22)).add(U.scale(s * 0.62));
        Vec3 bridge = h.add(T.scale(s * 1.05)).add(U.scale(s * 0.56));
        Vec3 crown = h.subtract(T.scale(s * 0.15)).add(U.scale(s * 0.78));

        for (int side = -1; side <= 1; side += 2) {
            Vec3 rS = R.scale(side);
            Vec3 lipMid = h.add(T.scale(s * 1.18)).add(rS.scale(s * 0.52)).subtract(U.scale(s * 0.08));
            Vec3 cheek = h.add(T.scale(s * 0.15)).add(rS.scale(s * 0.76)).add(U.scale(s * 0.10));
            Vec3 brow = h.add(T.scale(s * 0.55)).add(rS.scale(s * 0.58)).add(U.scale(s * 0.54));

            // Носовая пирамида и вздёрнутый носовой шип-складка
            paperTri(vc, pose, snoutTip, noseHorn, lipMid, side > 0 ? PAPER_LIT : PAPER_WARM, light);
            paperTri(vc, pose, noseHorn, bridge, lipMid, PAPER_LIT, light);
            paperTri(vc, pose, bridge, brow, lipMid, PAPER_WARM, light);
            paperTri(vc, pose, brow, cheek, lipMid, PAPER_SIDE, light);
            paperTri(vc, pose, bridge, crown, brow, PAPER_LIT, light);

            // Тёмная бумажная глазница под надбровной складкой
            Vec3 eyeA = h.add(T.scale(s * 0.82)).add(rS.scale(s * 0.55)).add(U.scale(s * 0.24));
            Vec3 eyeB = h.add(T.scale(s * 0.44)).add(rS.scale(s * 0.63)).add(U.scale(s * 0.34));
            Vec3 eyeC = h.add(T.scale(s * 0.62)).add(rS.scale(s * 0.58)).add(U.scale(s * 0.16));
            paperTri(vc, pose, eyeA, eyeB, eyeC, EYE_FOLD, light);

            // Каскад из 4 отогнутых назад щёчных и челюстных бумажных складок (характерный веер головы на фото)
            for (int k = 0; k < 4; ++k) {
                double zOff = 0.85 - k * 0.36;
                double flare = 0.62 + k * 0.13;
                Vec3 cFront = h.add(T.scale(s * (zOff + 0.28))).add(rS.scale(s * (flare - 0.18))).subtract(U.scale(s * 0.04));
                Vec3 cTop = h.add(T.scale(s * (zOff + 0.12))).add(rS.scale(s * (flare - 0.12))).add(U.scale(s * 0.30));
                Vec3 cTip = h.add(T.scale(s * (zOff - 0.34))).add(rS.scale(s * (flare + 0.22))).add(U.scale(s * (0.08 + k * 0.06)));
                Vec3 cIn = h.add(T.scale(s * (zOff - 0.18))).add(rS.scale(s * (flare - 0.22))).subtract(U.scale(s * 0.10));
                paperTri(vc, pose, cFront, cTop, cTip, PAPER_LIT, light);
                paperTri(vc, pose, cFront, cTip, cIn, PAPER_SIDE, light);
                paperTri(vc, pose, cTop, cIn, cTip, PAPER_CREASE, light);
            }

            // Двухсекционные сложенные бумажные рога дракона, уходящие назад и вверх
            Vec3 hBase = crown.add(rS.scale(s * 0.34));
            Vec3 hMid = hBase.subtract(T.scale(s * 0.95)).add(U.scale(s * 0.68)).add(rS.scale(s * 0.42));
            Vec3 hTip = hMid.subtract(T.scale(s * 0.92)).add(U.scale(s * 0.46)).add(rS.scale(s * 0.24));
            Vec3 hRidge = hBase.add(U.scale(s * 0.20)).add(rS.scale(s * 0.16));
            paperTri(vc, pose, hBase, hRidge, hMid, PAPER_LIT, light);
            paperTri(vc, pose, hMid, hRidge.subtract(T.scale(s * 0.72)).add(U.scale(s * 0.48)), hTip, PAPER_WARM, light);

            // Изящные белые бумажные усы дракона, развевающиеся по потоку воздуха
            Vec3 w0 = h.add(T.scale(s * 1.68)).add(rS.scale(s * 0.36)).subtract(U.scale(s * 0.04));
            double sway = Math.sin(age * 0.14 + side) * 0.14;
            Vec3 w1 = w0.subtract(T.scale(s * 0.75)).add(rS.scale(s * (0.68 + sway))).subtract(U.scale(s * 0.22));
            Vec3 w2 = w1.subtract(T.scale(s * 1.05)).add(rS.scale(s * (0.45 - sway))).add(U.scale(s * 0.12));
            paperRibbon(vc, pose, w0, w1, U.scale(0.032), PAPER_LIT, light);
            paperRibbon(vc, pose, w1, w2, U.scale(0.022), PAPER_WARM, light);
        }

        // Нижняя сложенная бумажная челюсть с зубчатым отгибом
        Vec3 chinTip = h.add(T.scale(s * 1.82)).subtract(U.scale(s * (0.34 + jawOpen)));
        Vec3 jawL = h.add(T.scale(s * 0.55)).subtract(R.scale(s * 0.48)).subtract(U.scale(s * 0.30));
        Vec3 jawR = h.add(T.scale(s * 0.55)).add(R.scale(s * 0.48)).subtract(U.scale(s * 0.30));
        Vec3 throat = h.subtract(T.scale(s * 0.15)).subtract(U.scale(s * 0.46));
        paperTri(vc, pose, chinTip, jawL, throat, PAPER_SIDE, light);
        paperTri(vc, pose, chinTip, throat, jawR, PAPER_WARM, light);
        paperTri(vc, pose, chinTip, jawR, jawL, PAPER_CREASE, light);
    }

    /** Четыре сложенные бумажные лапы с угловатым бедром, голенью и веером из трёх бумажных когтей. */
    private static void drawOrigamiLegs(VertexConsumer vc, PoseStack pose,
                                        Vec3[] spine, Vec3[] T, Vec3[] U, Vec3[] R, double[] rad,
                                        double age, int light) {
        int[] legSegs = {9, 22};
        for (int li = 0; li < legSegs.length; ++li) {
            int idx = legSegs[li];
            Vec3 c = spine[idx];
            Vec3 t = T[idx], u = U[idx], r = R[idx];
            double w = rad[idx];
            double swing = Math.sin(age * 0.11 + li * 2.1) * 0.16;

            for (int side = -1; side <= 1; side += 2) {
                Vec3 rS = r.scale(side);
                Vec3 hip = c.add(rS.scale(w * 0.85)).subtract(u.scale(w * 0.35));
                Vec3 haunchPeak = hip.add(rS.scale(w * 0.72)).add(u.scale(w * 0.42)).subtract(t.scale(w * 0.35));
                Vec3 knee = hip.add(rS.scale(w * 0.88)).subtract(u.scale(w * 1.45)).add(t.scale(w * (0.45 + swing)));
                Vec3 ankle = knee.add(rS.scale(w * 0.20)).subtract(u.scale(w * 1.15)).subtract(t.scale(w * 0.35));

                // Сложенное бумажное бедро (2 рельефных треугольных модуля)
                paperTri(vc, pose, hip, haunchPeak, knee, PAPER_LIT, light);
                paperTri(vc, pose, hip.subtract(t.scale(w * 0.55)), knee, haunchPeak, PAPER_WARM, light);

                // Голень-складка
                Vec3 shinBack = knee.subtract(t.scale(w * 0.52)).add(rS.scale(w * 0.18));
                paperTri(vc, pose, knee, shinBack, ankle, PAPER_LIT, light);
                paperTri(vc, pose, knee.add(rS.scale(w * 0.22)), ankle, shinBack, PAPER_SIDE, light);

                // Широкая трёпалая оригами-стопа (как на нижнем крае фото 01_origami_dragon_ref.png)
                for (int claw = -1; claw <= 1; ++claw) {
                    Vec3 clawDir = t.scale(0.36 - Math.abs(claw) * 0.06)
                            .add(rS.scale(0.14 + claw * side * 0.15))
                            .subtract(u.scale(0.08));
                    Vec3 clawTip = ankle.add(clawDir);
                    Vec3 clawRidge = ankle.add(u.scale(0.09)).add(clawDir.scale(0.42));
                    Vec3 clawSide = ankle.add(r.scale(claw * 0.08));
                    paperTri(vc, pose, ankle, clawRidge, clawTip, PAPER_LIT, light);
                    paperTri(vc, pose, clawSide, clawTip, clawRidge, PAPER_WARM, light);
                }
            }
        }
    }

    /** Ступенчато сужающийся острый бумажный конус хвоста (левая часть фото 01_origami_dragon_ref.png). */
    private static void drawOrigamiTailCone(VertexConsumer vc, PoseStack pose,
                                            Vec3 tailBase, Vec3 T, Vec3 U, Vec3 R, double rEnd, int light) {
        Vec3 coneTip = tailBase.subtract(T.scale(STEP * 2.5)).add(U.scale(0.04));
        int sides = 6;
        for (int k = 0; k < sides; ++k) {
            double a0 = (Math.PI * 2.0 * k) / sides;
            double a1 = (Math.PI * 2.0 * (k + 1)) / sides;
            Vec3 p0 = tailBase.add(radial(U, R, a0).scale(rEnd * 0.95));
            Vec3 p1 = tailBase.add(radial(U, R, a1).scale(rEnd * 0.95));
            paperTri(vc, pose, p0, p1, coneTip, (k & 1) == 0 ? PAPER_LIT : PAPER_SIDE, light);
        }
    }

    private static Vec3 radial(Vec3 U, Vec3 R, double angle) {
        return R.scale(Math.cos(angle)).add(U.scale(Math.sin(angle)));
    }

    private static void paperRibbon(VertexConsumer vc, PoseStack pose, Vec3 a, Vec3 b, Vec3 halfW, int color, int light) {
        Vec3 a0 = a.subtract(halfW), a1 = a.add(halfW);
        Vec3 b0 = b.subtract(halfW), b1 = b.add(halfW);
        paperTri(vc, pose, a0, a1, b1, color, light);
        paperTri(vc, pose, a0, b1, b0, color, light);
    }

    /** Двусторонний бумажный треугольник с чётким гранёным освещением по нормали складки. */
    private static void paperTri(VertexConsumer vc, PoseStack pose, Vec3 p1, Vec3 p2, Vec3 p3, int baseColor, int light) {
        Vec3 n = p2.subtract(p1).cross(p3.subtract(p1));
        double lenSq = n.lengthSqr();
        if (lenSq < 1.0e-10) return;
        n = n.scale(1.0 / Math.sqrt(lenSq));

        double sun = n.dot(SUN_DIR);
        double shadeFront = 0.68 + 0.32 * Math.max(0.0, sun);
        double shadeBack = 0.62 + 0.26 * Math.max(0.0, -sun);

        emitQuad(vc, pose, p1, p2, p3, p3, n, mulRgb(baseColor, shadeFront), light);
        emitQuad(vc, pose, p3, p2, p1, p1, n.scale(-1.0), mulRgb(baseColor, shadeBack), light);
    }

    private static void emitQuad(VertexConsumer vc, PoseStack pose,
                                 Vec3 a, Vec3 b, Vec3 c, Vec3 d, Vec3 n, int rgb, int light) {
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int bl = rgb & 255;
        Matrix4f pm = pose.last().pose();
        Matrix3f nm = pose.last().normal();
        float nx = (float) n.x, ny = (float) n.y, nz = (float) n.z;

        vc.vertex(pm, (float) a.x, (float) a.y, (float) a.z).color(r, g, bl, 255).uv(0.5F, 0.5F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nm, nx, ny, nz).endVertex();
        vc.vertex(pm, (float) b.x, (float) b.y, (float) b.z).color(r, g, bl, 255).uv(0.5F, 0.5F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nm, nx, ny, nz).endVertex();
        vc.vertex(pm, (float) c.x, (float) c.y, (float) c.z).color(r, g, bl, 255).uv(0.5F, 0.5F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nm, nx, ny, nz).endVertex();
        vc.vertex(pm, (float) d.x, (float) d.y, (float) d.z).color(r, g, bl, 255).uv(0.5F, 0.5F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(nm, nx, ny, nz).endVertex();
    }

    private static int mulRgb(int rgb, double k) {
        int r = Mth.clamp((int) Math.round(((rgb >> 16) & 255) * k), 0, 255);
        int g = Mth.clamp((int) Math.round(((rgb >> 8) & 255) * k), 0, 255);
        int b = Mth.clamp((int) Math.round((rgb & 255) * k), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    @Override
    public ResourceLocation getTextureLocation(PaperDragonEntity e) {
        return WHITE_TEX;
    }
}
