package ru.aiforge.riftworld.client.model.fauna;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Mob;

/**
 * Shared Mojmap 1.20.1 base for Riftworld v0.47.0 Blockbench MCP fauna models.
 *
 * <p>Captures exact initial bind poses (positions, rotations, scales) of all animated bones
 * and provides exact Blockbench {@code flip_y: true} delta helpers ({@code -rx, -ry, +rz} and
 * {@code -dx, -dy, +dz}) plus dynamic render alpha for spectral/crystalline creatures.
 */
public abstract class FaunaBaseModel<T extends Mob> extends EntityModel<T> {
    protected static final float DEG_TO_RAD = (float) (Math.PI / 180.0D);

    protected record RestPose(
            ModelPart part,
            float px, float py, float pz,
            float rx, float ry, float rz,
            float sx, float sy, float sz) {
        public static RestPose of(ModelPart p) {
            return new RestPose(p, p.x, p.y, p.z, p.xRot, p.yRot, p.zRot, p.xScale, p.yScale, p.zScale);
        }
    }

    private final List<RestPose> restPoses = new ArrayList<>();
    protected float renderAlpha = 1.0F;

    protected void captureRestPoses(ModelPart... parts) {
        for (ModelPart p : parts) {
            if (p != null) {
                this.restPoses.add(RestPose.of(p));
            }
        }
    }

    protected void resetToBindPose() {
        this.renderAlpha = 1.0F;
        for (RestPose rp : this.restPoses) {
            rp.part.x = rp.px;
            rp.part.y = rp.py;
            rp.part.z = rp.pz;
            rp.part.xRot = rp.rx;
            rp.part.yRot = rp.ry;
            rp.part.zRot = rp.rz;
            rp.part.xScale = rp.sx;
            rp.part.yScale = rp.sy;
            rp.part.zScale = rp.sz;
        }
    }

    protected static float rad(float deg) {
        return deg * DEG_TO_RAD;
    }

    /** Exact Blockbench {@code flip_y: true} Euler rotation delta in degrees. */
    protected static void addBbDeg(ModelPart p, float dxDeg, float dyDeg, float dzDeg) {
        p.xRot -= dxDeg * DEG_TO_RAD;
        p.yRot -= dyDeg * DEG_TO_RAD;
        p.zRot += dzDeg * DEG_TO_RAD;
    }

    /** Exact Blockbench {@code flip_y: true} translation delta in model units (1/16 block). */
    protected static void addBbPos(ModelPart p, float dx, float dy, float dz) {
        p.x -= dx;
        p.y -= dy;
        p.z += dz;
    }

    protected abstract ModelPart rootPart();

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        this.rootPart().render(poseStack, vertexConsumer, packedLight, packedOverlay,
                red, green, blue, alpha * this.renderAlpha);
    }
}
