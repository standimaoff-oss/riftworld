package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import ru.aiforge.riftworld.block.WindowLightBlock;
import ru.aiforge.riftworld.block.WindowLightBlockEntity;

/**
 * Косые солнечные лучи из окон замков/храмов Элисара, переведённые с {@code RenderType.debugQuads()}
 * на шейдер-совместимый {@link RenderType#entityTranslucent} + {@link ShaderCompat#WHITE_TEX},
 * чтобы в IterationT и других PBR/рейтрейс шейдерпаках не возникало поломок G-буфера и теней.
 */
public final class WindowLights implements BlockEntityRenderer<WindowLightBlockEntity> {
    public WindowLights(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public boolean shouldRenderOffScreen(WindowLightBlockEntity be) {
        return true;
    }

    @Override
    public void render(WindowLightBlockEntity be, float partial, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (ShaderCompat.isRenderingShadowPass()) return;

        Direction dir = be.getBlockState().getValue(WindowLightBlock.FACING);
        float dx = dir.getStepX(), dz = dir.getStepZ();
        float reach = 5.5F, drop = 4.5F;
        float w0 = 0.42F, h0 = 0.42F, w1 = 1.05F, h1 = 0.85F;

        float x0 = 0.5F - dx * 0.45F, y0 = 0.5F, z0 = 0.5F - dz * 0.45F;
        float x1 = x0 + dx * reach, y1 = y0 - drop, z1 = z0 + dz * reach;
        float px = -dz, pz = dx;

        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucent(ShaderCompat.WHITE_TEX));
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();

        int r = 255, g = 236, b = 185, aNear = 58, aFar = 0;

        quad(vc, m, n,
                x0 - px * w0, y0 + h0, z0 - pz * w0, aNear,
                x0 + px * w0, y0 + h0, z0 + pz * w0, aNear,
                x1 + px * w1, y1 + h1, z1 + pz * w1, aFar,
                x1 - px * w1, y1 + h1, z1 - pz * w1, aFar, r, g, b);
        quad(vc, m, n,
                x0 - px * w0, y0 - h0, z0 - pz * w0, aNear,
                x0 + px * w0, y0 - h0, z0 + pz * w0, aNear,
                x1 + px * w1, y1 - h1, z1 + pz * w1, aFar,
                x1 - px * w1, y1 - h1, z1 - pz * w1, aFar, r, g, b);
        quad(vc, m, n,
                x0 - px * w0, y0 - h0, z0 - pz * w0, aNear,
                x0 - px * w0, y0 + h0, z0 - pz * w0, aNear,
                x1 - px * w1, y1 + h1, z1 - pz * w1, aFar,
                x1 - px * w1, y1 - h1, z1 - pz * w1, aFar, r, g, b);
        quad(vc, m, n,
                x0 + px * w0, y0 - h0, z0 + pz * w0, aNear,
                x0 + px * w0, y0 + h0, z0 + pz * w0, aNear,
                x1 + px * w1, y1 + h1, z1 + pz * w1, aFar,
                x1 + px * w1, y1 - h1, z1 + pz * w1, aFar, r, g, b);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f n,
                             float ax, float ay, float az, int aa,
                             float bx, float by, float bz, int ba,
                             float cx, float cy, float cz, int ca,
                             float dx, float dy, float dz, int da,
                             int r, int g, int b) {
        emit(vc, m, n, ax, ay, az, r, g, b, aa);
        emit(vc, m, n, bx, by, bz, r, g, b, ba);
        emit(vc, m, n, cx, cy, cz, r, g, b, ca);
        emit(vc, m, n, dx, dy, dz, r, g, b, da);

        emit(vc, m, n, dx, dy, dz, r, g, b, da);
        emit(vc, m, n, cx, cy, cz, r, g, b, ca);
        emit(vc, m, n, bx, by, bz, r, g, b, ba);
        emit(vc, m, n, ax, ay, az, r, g, b, aa);
    }

    private static void emit(VertexConsumer vc, Matrix4f m, Matrix3f n,
                             float x, float y, float z, int r, int g, int b, int a) {
        vc.vertex(m, x, y, z)
                .color(r, g, b, a)
                .uv(0.5F, 0.5F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(n, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
