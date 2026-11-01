package ru.aiforge.riftworld.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import ru.aiforge.riftworld.block.ConnectedCarpetData;

/**
 * v0.24.0: Zero-Z-fighting 8-neighbor CTM baked model for connected carpets & rugs.
 * Splits the 16x16x1 carpet into a non-overlapping 3x3 sub-grid ([0..3], [3..13], [13..16])
 * so 1x1, 1xN runners, 2x2, NxM rectangles, L-shapes, T-junctions, +-crosses, and inner/outer
 * corners merge into one continuous ornamental carpet with 2x2-block seamless brocade interior.
 */
public final class ConnectedCarpetBakedModel extends BakedModelWrapper<BakedModel> {
    private static final float H = 1.0F / 16.0F;
    private static final float[] CUTS = {0.0F, 3.0F, 13.0F, 16.0F};
    private static final ChunkRenderTypeSet SOLID_SET = ChunkRenderTypeSet.of(RenderType.solid());

    private static final int NORM_UP    = packNormal(0,  127, 0);
    private static final int NORM_DOWN  = packNormal(0, -127, 0);
    private static final int NORM_NORTH = packNormal(0, 0, -127);
    private static final int NORM_SOUTH = packNormal(0, 0,  127);
    private static final int NORM_WEST  = packNormal(-127, 0, 0);
    private static final int NORM_EAST  = packNormal( 127, 0, 0);

    private final ResourceLocation centerLoc;
    private final ResourceLocation trimLoc;

    private volatile TextureAtlasSprite cachedCenter;
    private volatile TextureAtlasSprite cachedTrim;
    private final AtomicReferenceArray<List<BakedQuad>> unculledCache = new AtomicReferenceArray<>(1024);
    private final AtomicReferenceArray<List<BakedQuad>> downCache = new AtomicReferenceArray<>(4);

    public ConnectedCarpetBakedModel(BakedModel originalModel, ResourceLocation centerLoc, ResourceLocation trimLoc) {
        super(originalModel);
        this.centerLoc = centerLoc;
        this.trimLoc = trimLoc;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return SOLID_SET;
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        return ConnectedCarpetData.compute(state.getBlock(), level, pos);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                    ModelData extraData, @Nullable RenderType renderType) {
        if (renderType != null && renderType != RenderType.solid()) {
            return Collections.emptyList();
        }
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite center = atlas.apply(centerLoc);
        TextureAtlasSprite trim = atlas.apply(trimLoc);
        if (center != cachedCenter || trim != cachedTrim) {
            for (int i = 0; i < unculledCache.length(); ++i) unculledCache.set(i, null);
            for (int i = 0; i < downCache.length(); ++i) downCache.set(i, null);
            cachedCenter = center;
            cachedTrim = trim;
        }

        Integer boxed = extraData.get(ConnectedCarpetData.PACKED);
        int packed = boxed != null ? (boxed & 0x3FF) : 0;

        if (side == Direction.DOWN) {
            int parityIdx = (packed >> 8) & 3;
            List<BakedQuad> dq = downCache.get(parityIdx);
            if (dq == null) {
                int px = parityIdx & 1;
                int pz = (parityIdx >> 1) & 1;
                float u0 = px * 8.0F, u1 = u0 + 8.0F;
                float v0 = pz * 8.0F, v1 = v0 + 8.0F;
                dq = List.of(makeDownQuad(center, u0, v0, u1, v1));
                downCache.set(parityIdx, dq);
            }
            return dq;
        }
        if (side != null) {
            return Collections.emptyList();
        }

        List<BakedQuad> res = unculledCache.get(packed);
        if (res == null) {
            res = buildUnculledQuads(packed, center, trim);
            unculledCache.set(packed, res);
        }
        return res;
    }

    private static List<BakedQuad> buildUnculledQuads(int packed, TextureAtlasSprite center, TextureAtlasSprite trim) {
        boolean n  = (packed & ConnectedCarpetData.BIT_N)  != 0;
        boolean ne = (packed & ConnectedCarpetData.BIT_NE) != 0;
        boolean e  = (packed & ConnectedCarpetData.BIT_E)  != 0;
        boolean se = (packed & ConnectedCarpetData.BIT_SE) != 0;
        boolean s  = (packed & ConnectedCarpetData.BIT_S)  != 0;
        boolean sw = (packed & ConnectedCarpetData.BIT_SW) != 0;
        boolean w  = (packed & ConnectedCarpetData.BIT_W)  != 0;
        boolean nw = (packed & ConnectedCarpetData.BIT_NW) != 0;
        int px = (packed >> 8) & 1;
        int pz = (packed >> 9) & 1;

        List<BakedQuad> out = new ArrayList<>(13);

        // 3x3 non-overlapping top sub-tiles
        for (int gz = 0; gz < 3; ++gz) {
            float z0 = CUTS[gz], z1 = CUTS[gz + 1];
            for (int gx = 0; gx < 3; ++gx) {
                float x0 = CUTS[gx], x1 = CUTS[gx + 1];
                int mode = selectSubTileMode(gx, gz, n, ne, e, se, s, sw, w, nw);
                if (mode < 0) {
                    // Seamless 2x2-block center weave
                    float u0 = (px * 16.0F + x0) * 0.5F;
                    float u1 = (px * 16.0F + x1) * 0.5F;
                    float v0 = (pz * 16.0F + z0) * 0.5F;
                    float v1 = (pz * 16.0F + z1) * 0.5F;
                    out.add(makeUpQuad(x0 / 16.0F, z0 / 16.0F, x1 / 16.0F, z1 / 16.0F, center, u0, v0, u1, v1));
                } else {
                    // Trim quadrant: 0=TL (outer/cardinal), 1=TR (horiz), 2=BL (vert), 3=BR (inner concave corner)
                    int qx = mode & 1;
                    int qy = (mode >> 1) & 1;
                    float u0 = qx * 8.0F + x0 * 0.5F;
                    float u1 = qx * 8.0F + x1 * 0.5F;
                    float v0 = qy * 8.0F + z0 * 0.5F;
                    float v1 = qy * 8.0F + z1 * 0.5F;
                    out.add(makeUpQuad(x0 / 16.0F, z0 / 16.0F, x1 / 16.0F, z1 / 16.0F, trim, u0, v0, u1, v1));
                }
            }
        }

        // Outer 1px vertical carpet hems ONLY on unconnected edges (0 internal seam geometry)
        if (!n) out.add(makeSideQuad(Direction.NORTH, trim, 0.0F, 0.0F, 8.0F, 0.5F));
        if (!s) out.add(makeSideQuad(Direction.SOUTH, trim, 0.0F, 7.5F, 8.0F, 8.0F));
        if (!w) out.add(makeSideQuad(Direction.WEST,  trim, 0.0F, 0.0F, 8.0F, 0.5F));
        if (!e) out.add(makeSideQuad(Direction.EAST,  trim, 0.0F, 7.5F, 8.0F, 8.0F));

        return List.copyOf(out);
    }

    /**
     * Returns -1 for seamless center weave, or trim quadrant 0..3:
     *   0 = TL (outer corner / cardinal border)
     *   1 = TR (continuous horizontal N/S border)
     *   2 = BL (continuous vertical W/E border)
     *   3 = BR (inner concave corner miter for L, T, + shapes)
     */
    private static int selectSubTileMode(int gx, int gz,
                                         boolean n, boolean ne, boolean e, boolean se,
                                         boolean s, boolean sw, boolean w, boolean nw) {
        if (gx == 1 && gz == 1) return -1;
        if (gx == 1 && gz == 0) return n ? -1 : 0;
        if (gx == 1 && gz == 2) return s ? -1 : 0;
        if (gx == 0 && gz == 1) return w ? -1 : 0;
        if (gx == 2 && gz == 1) return e ? -1 : 0;

        // 4 corner sub-tiles
        boolean cardV = (gz == 0) ? n : s;
        boolean cardH = (gx == 0) ? w : e;
        boolean diag  = (gz == 0) ? ((gx == 0) ? nw : ne) : ((gx == 0) ? sw : se);

        if (!cardV && !cardH) return 0; // Outer corner
        if (!cardV)           return 1; // Horizontal border continues through corner
        if (!cardH)           return 2; // Vertical border continues through corner
        if (!diag)            return 3; // Inner concave corner miter!
        return -1;                      // Fully surrounded interior
    }

    private static BakedQuad makeUpQuad(float x0, float z0, float x1, float z1,
                                        TextureAtlasSprite sp, float u0, float v0, float u1, float v1) {
        int[] v = new int[32];
        putVertex(v, 0, x0, H, z0, sp.getU(u0), sp.getV(v0), NORM_UP);
        putVertex(v, 1, x0, H, z1, sp.getU(u0), sp.getV(v1), NORM_UP);
        putVertex(v, 2, x1, H, z1, sp.getU(u1), sp.getV(v1), NORM_UP);
        putVertex(v, 3, x1, H, z0, sp.getU(u1), sp.getV(v0), NORM_UP);
        return new BakedQuad(v, -1, Direction.UP, sp, true);
    }

    private static BakedQuad makeDownQuad(TextureAtlasSprite sp, float u0, float v0, float u1, float v1) {
        int[] v = new int[32];
        putVertex(v, 0, 0.0F, 0.0F, 1.0F, sp.getU(u0), sp.getV(v1), NORM_DOWN);
        putVertex(v, 1, 0.0F, 0.0F, 0.0F, sp.getU(u0), sp.getV(v0), NORM_DOWN);
        putVertex(v, 2, 1.0F, 0.0F, 0.0F, sp.getU(u1), sp.getV(v0), NORM_DOWN);
        putVertex(v, 3, 1.0F, 0.0F, 1.0F, sp.getU(u1), sp.getV(v1), NORM_DOWN);
        return new BakedQuad(v, -1, Direction.DOWN, sp, true);
    }

    private static BakedQuad makeSideQuad(Direction dir, TextureAtlasSprite sp, float u0, float v0, float u1, float v1) {
        int[] v = new int[32];
        float su0 = sp.getU(u0), su1 = sp.getU(u1);
        float sv0 = sp.getV(v0), sv1 = sp.getV(v1);
        switch (dir) {
            case NORTH -> {
                putVertex(v, 0, 1.0F, H,    0.0F, su0, sv0, NORM_NORTH);
                putVertex(v, 1, 1.0F, 0.0F, 0.0F, su0, sv1, NORM_NORTH);
                putVertex(v, 2, 0.0F, 0.0F, 0.0F, su1, sv1, NORM_NORTH);
                putVertex(v, 3, 0.0F, H,    0.0F, su1, sv0, NORM_NORTH);
            }
            case SOUTH -> {
                putVertex(v, 0, 0.0F, H,    1.0F, su0, sv0, NORM_SOUTH);
                putVertex(v, 1, 0.0F, 0.0F, 1.0F, su0, sv1, NORM_SOUTH);
                putVertex(v, 2, 1.0F, 0.0F, 1.0F, su1, sv1, NORM_SOUTH);
                putVertex(v, 3, 1.0F, H,    1.0F, su1, sv0, NORM_SOUTH);
            }
            case WEST -> {
                putVertex(v, 0, 0.0F, H,    0.0F, su0, sv0, NORM_WEST);
                putVertex(v, 1, 0.0F, 0.0F, 0.0F, su0, sv1, NORM_WEST);
                putVertex(v, 2, 0.0F, 0.0F, 1.0F, su1, sv1, NORM_WEST);
                putVertex(v, 3, 0.0F, H,    1.0F, su1, sv0, NORM_WEST);
            }
            default -> { // EAST
                putVertex(v, 0, 1.0F, H,    1.0F, su0, sv0, NORM_EAST);
                putVertex(v, 1, 1.0F, 0.0F, 1.0F, su0, sv1, NORM_EAST);
                putVertex(v, 2, 1.0F, 0.0F, 0.0F, su1, sv1, NORM_EAST);
                putVertex(v, 3, 1.0F, H,    0.0F, su1, sv0, NORM_EAST);
            }
        }
        return new BakedQuad(v, -1, dir, sp, true);
    }

    private static void putVertex(int[] buf, int idx, float x, float y, float z, float u, float v, int normal) {
        int o = idx * 8;
        buf[o]     = Float.floatToRawIntBits(x);
        buf[o + 1] = Float.floatToRawIntBits(y);
        buf[o + 2] = Float.floatToRawIntBits(z);
        buf[o + 3] = 0xFFFFFFFF;
        buf[o + 4] = Float.floatToRawIntBits(u);
        buf[o + 5] = Float.floatToRawIntBits(v);
        buf[o + 6] = 0;
        buf[o + 7] = normal;
    }

    private static int packNormal(int nx, int ny, int nz) {
        return (nx & 0xFF) | ((ny & 0xFF) << 8) | ((nz & 0xFF) << 16);
    }
}
