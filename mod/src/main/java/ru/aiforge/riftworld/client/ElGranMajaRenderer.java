package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import ru.aiforge.riftworld.client.geo.GeoModel;
import ru.aiforge.riftworld.entity.ElGranMajaEntity;

/**
 * 13-bone hierarchy; the head, tail and jaw are animated around their actual pivots.
 *
 * <p>Surface of the geo-file's own sines there is a physical pose: the body bends into the
 * turn (the tail lags behind the head), the whole animal banks like an aircraft, an arch
 * runs down the spine while it breaches, and a travelling wave runs from the neck to the
 * tail with an amplitude that follows the swim speed.
 */
public final class ElGranMajaRenderer extends EntityRenderer<ElGranMajaEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("riftworld", "textures/entity/el_gran_maja.png");
    private static final double CENTER_Z = 104.6875; // midpoint of the 322-block model, in blocks

    /** Суставы позвоночника от шеи к кончику хвоста (голова — корень, её крутим отдельно). */
    private static final String[] SPINE = {
            "neck_1", "neck_2", "trunk_1", "trunk_2", "trunk_3",
            "tail_1", "tail_2", "tail_3", "tail_4", "tail_5", "tail_6"
    };
    /** Положение сустава вдоль тела: 0 — шея, 1 — кончик хвоста. */
    private static final float[] SPINE_U = {
            0.0000F, 0.0999F, 0.1998F, 0.3000F, 0.3999F, 0.4999F, 0.6001F, 0.7000F, 0.7999F, 0.8998F, 1.0000F
    };
    /**
     * Доля общего изгиба, приходящаяся на сустав. Кривизна растёт к хвосту (степень 1.55),
     * поэтому тело складывается как хлыст: шея почти не гнётся, хвост уходит сильнее всего.
     * Сумма = 1.
     */
    private static final float[] BEND_W = {
            0.0000F, 0.0281F, 0.0543F, 0.0723F, 0.0869F, 0.0998F, 0.1117F, 0.1222F, 0.1322F, 0.1416F, 0.1510F
    };
    /** Постоянная кривизна для арки при выпрыгивании (сумма = 1). */
    private static final float[] ARCH_W = {
            0.0000F, 0.0999F, 0.0999F, 0.1002F, 0.0999F, 0.0999F, 0.1002F, 0.0999F, 0.0999F, 0.0999F, 0.1002F
    };
    /** Огибающая бегущей волны: у шеи почти неподвижно, у кончика хвоста — максимум. */
    private static final float[] WAVE_W = {
            0.1800F, 0.2380F, 0.3279F, 0.4359F, 0.5573F, 0.6898F, 0.8324F, 0.9832F, 1.1417F, 1.3073F, 1.4800F
    };

    private static final double TAU = Math.PI * 2.0;
    /** Сколько градусов изгиба набирает тело на каждую единицу угловой скорости (град/тик). */
    private static final float TURN_BEND = 34.0F;
    /** Предельный изгиб тела в повороте, градусов. */
    private static final float MAX_BEND = 8.0F;
    /** Крен в поворот, градусов на единицу угловой скорости. */
    private static final float TURN_BANK = 46.0F;
    private static final float MAX_BANK = 13.0F;
    /** Голова заглядывает внутрь поворота — доля от общего изгиба. */
    private static final float HEAD_LEAD = 0.34F;
    /** Подъём носа и уход хвоста вглубь при выпрыгивании, градусов. */
    private static final float BREACH_LIFT = 2.0F;
    private static final float BREACH_ARCH = 4.0F;
    /** Сколько длин волн укладывается вдоль тела. */
    private static final float WAVES = 1.35F;

    public ElGranMajaRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(ElGranMajaEntity e) { return TEXTURE; }

    @Override
    public boolean shouldRender(ElGranMajaEntity e, Frustum frustum, double x, double y, double z) {
        if (!e.isReadyToRender()) return false;
        int rd = Minecraft.getInstance().options.renderDistance().get();
        double maxDistance = Math.max(192.0, rd * 16.0 + 180.0);
        return e.distanceToSqr(x, y, z) <= maxDistance * maxDistance
                && frustum.isVisible(e.getBoundingBoxForCulling().inflate(180.0, 50.0, 180.0));
    }

    @Override
    public void render(ElGranMajaEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!e.isReadyToRender()) return;
        GeoModel model = GeoModel.of("el_gran_maja");
        if (model == null) return;
        // Use the same entity yaw that the server's collision envelopes test.
        // Mob body-yaw smoothing can otherwise start at zero after spawn or lag behind.
        float bodyYaw = Mth.rotLerp(partial, e.yRotO, e.getYRot());
        float time = e.tickCount + partial;

        float turn = e.turnRate(partial);                    // град/тик, плюс — вправо
        float swim = Mth.clamp(e.swimSpeed(partial) / 0.16F, 0.0F, 1.35F);
        float breach = Mth.clamp(e.breachAmount(partial), 0.0F, 1.0F);

        // Изгиб в поворот: хвост отстаёт от головы, тело складывается в дугу.
        // В открытой воде изгиб полный; упёршись в берег зверь гасит и скорость, и изгиб,
        // чтобы хвост не уходил за консервативный контур столкновений.
        float bend = Mth.clamp(turn * TURN_BEND * (0.30F + 0.70F * swim), -MAX_BEND, MAX_BEND);
        // Крен в поворот плюс медленная «бортовая качка», чтобы зверь никогда не выглядел статичным.
        float bank = Mth.clamp(turn * TURN_BANK, -MAX_BANK, MAX_BANK)
                + 1.6F * Mth.sin(time * 0.019F) + 0.9F * Mth.sin(time * 0.0071F + 1.7F);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        // Крен — вокруг продольной оси тела (в локальных координатах это Z).
        pose.mulPose(Axis.ZP.rotationDegrees(-bank));
        // Riftworld's GeoModel uses Minecraft's Y-down model coordinates.
        // Reflect once here so the reference's dorsal side points upward.
        pose.scale(model.renderScale, -model.renderScale, model.renderScale);
        pose.translate(0.0, 0.0, -CENTER_Z);

        model.animate(time);

        // ---------- физичная поза поверх синусов из geo-файла ----------
        // Голова чуть заглядывает внутрь поворота: так разворот читается как осознанный.
        ModelPart head = model.bone("head");
        if (head != null) {
            head.yRot += (float) Math.toRadians(-bend * HEAD_LEAD);
            // При выпрыгивании нос уходит вверх, а корень — это череп, за ним тянется всё тело.
            head.xRot += (float) Math.toRadians(-BREACH_LIFT * breach
                    + 0.8F * Mth.sin(time * 0.023F));
        }

        // Бегущая волна по телу: чем быстрее плывёт, тем чаще и шире взмах.
        // Частота в радианах за тик: 0.020 — ленивая качка на месте, 0.050 — взмах
        // примерно раз в шесть секунд, как и подобает двухсотметровой змее.
        float amp = 0.30F + 1.30F * swim;
        double phase = time * (0.020 + 0.030 * swim);
        for (int i = 0; i < SPINE.length; ++i) {
            ModelPart part = model.bone(SPINE[i]);
            if (part == null) continue;
            float u = SPINE_U[i];
            float wave = amp * WAVE_W[i] * Mth.sin((float) (phase - TAU * WAVES * u));
            // Горизонтальный взмах + отставание хвоста в повороте.
            part.yRot += (float) Math.toRadians(bend * BEND_W[i] * -1.0F + wave);
            // Вертикальный взмах + арка при выпрыгивании.
            part.xRot += (float) Math.toRadians(-BREACH_ARCH * breach * ARCH_W[i] + wave * 0.45F);
            // Небольшое скручивание вдоль тела, чтобы крен не выглядел плоским.
            part.zRot += (float) Math.toRadians(bank * BEND_W[i] * 0.55F);
        }

        // In Y-up source coordinates: +8 is closed, -13 is fully open.
        float jawDegrees = Mth.lerp(e.jaw(partial), 8.0F, -13.0F);
        model.root.getChild("head").getChild("jaw").xRot = (float) Math.toRadians(-jawDegrees);
        // ModelPart.render recursively draws children; drawing getAllParts()
        // separately duplicates bones and loses their parent transformations.
        model.root.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
