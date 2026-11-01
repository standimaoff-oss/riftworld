package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.registry.CastleBlocks;

/**
 * Цветные лучи сквозь витражи Королевского Замка с универсальной совместимостью со ВСЕМИ шейдерпаками
 * (Oculus / Iris / OptiFine: IterationT, Complementary, Bliss, Photon, BSL и др.):
 * <ul>
 *   <li>Оба прохода (объёмный луч {@code shaft} и напольное пятно {@code patch}) используют
 *       строго {@code POSITION_TEX_COLOR} ({@code gbuffers_textured}) вместо нетекстурированного
 *       {@code POSITION_COLOR}, а теневой проход шейдера отсекается через {@link ShaderCompat#isRenderingShadowPass()};</li>
 *   <li>Корректный сброс BlendFunc и PolygonOffset в конце кадра исключает засветку пост-эффектов шейдера.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = RiftWorld.MODID, value = Dist.CLIENT)
public final class WindowLights {
    private WindowLights() {}

    private static final ResourceLocation GLASS_TEX = new ResourceLocation(RiftWorld.MODID, "block/stained_glass");
    private static final int[] PALETTE = {0xE0A52A, 0xC52D1C, 0x3864BC, 0xF0C450, 0x9B2541, 0xE0A52A, 0x3D8D48, 0xC52D1C};
    private static final double TILT = Math.toRadians(28);
    private static final double MAX_LEN = 30, RANGE = 72;

    static final class Col {
        final BlockPos base;
        final int h, axis;
        final int rgb;
        boolean leftOpen, rightOpen;
        int side;
        long geomKey = Long.MIN_VALUE;
        Vec3 dir;
        double lenB, lenT;
        float[][] grid;
        float power;

        Col(BlockPos base, int h, int axis) {
            this.base = base;
            this.h = h;
            this.axis = axis;
            int hsh = (base.getX() * 73856093) ^ (base.getZ() * 19349663) ^ (base.getY() * 83492791);
            this.rgb = PALETTE[Math.floorMod(hsh, PALETTE.length)];
        }
    }

    private static final Map<Long, List<Col>> CHUNKS = new HashMap<>();
    private static int motesClock;

    @SubscribeEvent
    public static void chunkLoad(ChunkEvent.Load e) {
        if (!(e.getLevel() instanceof ClientLevel lvl)) return;
        ChunkAccess c = e.getChunk();
        List<Col> found = scan(lvl, c);
        long key = c.getPos().toLong();
        if (found.isEmpty()) CHUNKS.remove(key);
        else CHUNKS.put(key, found);
    }

    @SubscribeEvent
    public static void chunkUnload(ChunkEvent.Unload e) {
        if (e.getLevel() instanceof ClientLevel) CHUNKS.remove(e.getChunk().getPos().toLong());
    }

    @SubscribeEvent
    public static void levelUnload(LevelEvent.Unload e) {
        if (e.getLevel() instanceof ClientLevel) CHUNKS.clear();
    }

    private static boolean glass(BlockState s) {
        return s.is(CastleBlocks.STAINED_GLASS.get());
    }

    private static List<Col> scan(ClientLevel lvl, ChunkAccess c) {
        List<Col> out = new ArrayList<>();
        LevelChunkSection[] secs = c.getSections();
        ChunkPos cp = c.getPos();
        for (int si = 0; si < secs.length; ++si) {
            LevelChunkSection sec = secs[si];
            if (sec == null || sec.hasOnlyAir() || !sec.getStates().maybeHas(WindowLights::glass)) continue;
            int by = c.getSectionYFromSectionIndex(si) << 4;
            for (int y = 0; y < 16; ++y) {
                for (int z = 0; z < 16; ++z) {
                    for (int x = 0; x < 16; ++x) {
                        BlockState s = sec.getBlockState(x, y, z);
                        if (!glass(s)) continue;
                        BlockPos p = new BlockPos(cp.getMinBlockX() + x, by + y, cp.getMinBlockZ() + z);
                        if (glass(c.getBlockState(p.below()))) continue;
                        int h = 1;
                        while (h < 24 && glass(c.getBlockState(p.above(h)))) ++h;
                        int axis = s.getValue(CrossCollisionBlock.EAST) || s.getValue(CrossCollisionBlock.WEST) ? 0
                                : s.getValue(CrossCollisionBlock.NORTH) || s.getValue(CrossCollisionBlock.SOUTH) ? 1 : -1;
                        if (axis < 0) continue;
                        out.add(new Col(p, h, axis));
                    }
                }
            }
        }
        return out;
    }

    private static Vec3 sunDir(float celestial) {
        double a = celestial * Math.PI * 2;
        double x = -Math.sin(a), up = Math.cos(a);
        return new Vec3(x, up * Math.cos(TILT), up * Math.sin(TILT)).normalize();
    }

    private static boolean solidAt(ClientLevel lvl, Vec3 p) {
        BlockPos bp = BlockPos.containing(p);
        BlockState s = lvl.getBlockState(bp);
        if (s.isAir() || glass(s)) return false;
        VoxelShape sh = s.getCollisionShape(lvl, bp);
        if (sh.isEmpty()) return false;
        double lx = p.x - bp.getX(), ly = p.y - bp.getY(), lz = p.z - bp.getZ();
        for (var bb : sh.toAabbs()) if (bb.contains(lx, ly, lz)) return true;
        return false;
    }

    private static double march(ClientLevel lvl, Vec3 from, Vec3 d, double skip) {
        for (double t = skip; t < MAX_LEN; t += 0.25) {
            if (solidAt(lvl, from.add(d.scale(t)))) return t;
        }
        return -1;
    }

    private static Vec3 normal(Col c) {
        return c.axis == 0 ? new Vec3(0, 0, 1) : new Vec3(1, 0, 0);
    }

    private static Vec3 along(Col c) {
        return c.axis == 0 ? new Vec3(1, 0, 0) : new Vec3(0, 0, 1);
    }

    private static void update(ClientLevel lvl, Col c, Vec3 light, float strength, long key) {
        c.geomKey = key;
        c.power = 0;
        BlockPos b = c.base;
        Vec3 n = normal(c);
        if (c.side == 0 || c.side == 2) {
            BlockPos mid = b.above(c.h / 2);
            int plus = 0, minus = 0;
            for (int k = 2; k <= 5; ++k) {
                plus += lvl.getBrightness(LightLayer.SKY, BlockPos.containing(Vec3.atCenterOf(mid).add(n.scale(k))));
                minus += lvl.getBrightness(LightLayer.SKY, BlockPos.containing(Vec3.atCenterOf(mid).subtract(n.scale(k))));
            }
            c.side = Math.abs(plus - minus) < 6 ? 2 : plus > minus ? -1 : 1;
            Vec3 a = along(c);
            c.leftOpen = !glass(lvl.getBlockState(BlockPos.containing(Vec3.atCenterOf(mid).subtract(a))));
            c.rightOpen = !glass(lvl.getBlockState(BlockPos.containing(Vec3.atCenterOf(mid).add(a))));
        }
        if (c.side == 2 || strength <= 0.01F) return;
        Vec3 inward = n.scale(c.side);
        double enter = light.dot(inward);
        if (enter < 0.12) return;
        Vec3 hz = new Vec3(light.x, 0, light.z);
        double hl = Math.max(hz.length(), 1e-4);
        double el = Math.atan2(-light.y, hl);
        el = Mth.clamp(el, Math.toRadians(22), Math.toRadians(68));
        Vec3 d = hz.scale(1 / hl).scale(Math.cos(el)).add(0, -Math.sin(el), 0).normalize();
        Vec3 o = Vec3.atBottomCenterOf(b);
        double skip = 1.6 / Math.max(Math.abs(d.dot(inward)), 0.2);
        double lb = march(lvl, o.add(0, 0.05, 0), d, skip), lt = march(lvl, o.add(0, c.h - 0.05, 0), d, skip);
        if (lb < 0 || lt < 0) return;
        Vec3 a = along(c);
        float[][] g = new float[3][2 * c.h + 1];
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j <= 2 * c.h; ++j) {
                double vy = Mth.clamp(j * 0.5, 0.05, c.h - 0.05);
                g[i][j] = (float) march(lvl, o.add(a.scale((i - 1) * 0.49)).add(0, vy, 0), d, skip);
            }
        }
        c.grid = g;
        c.dir = d;
        c.lenB = lb;
        c.lenT = lt;
        c.power = (float) (strength * Mth.clamp((enter - 0.12) * 2.5, 0.0, 1.0));
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || CHUNKS.isEmpty()) return;
        if (ShaderCompat.isRenderingShadowPass()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel lvl = mc.level;
        if (lvl == null) return;
        float pt = e.getPartialTick();
        Vec3 cam = e.getCamera().getPosition();
        float cel = lvl.getTimeOfDay(pt);
        Vec3 sun = sunDir(cel);
        boolean day = sun.y > 0.02;
        Vec3 light = day ? sun.scale(-1) : sun;
        float strength = day ? (float) Mth.clamp(sun.y * 4.0, 0.0, 1.0) * (1.0F - lvl.getRainLevel(pt) * 0.7F)
                : (float) Mth.clamp(-sun.y * 4.0, 0.0, 1.0) * 0.32F;
        long key = (long) Math.floor(lvl.getDayTime() / 100.0) * 2 + (day ? 1 : 0);
        List<Col> vis = new ArrayList<>();
        int budget = 12;
        for (List<Col> l : CHUNKS.values()) {
            for (Col c : l) {
                if (c.base.distToCenterSqr(cam) > RANGE * RANGE) continue;
                if (c.geomKey != key && budget-- > 0) update(lvl, c, light, strength, key);
                if (c.power > 0.01F && c.dir != null) vis.add(c);
            }
        }
        if (vis.isEmpty()) return;
        float shaderScale = ShaderCompat.isShaderPackActive() ? 0.65F : 1.0F;
        PoseStack ps = e.getPoseStack();
        Matrix4f m = ps.last().pose();
        float moonTint = day ? 0.0F : 1.0F;

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // ---- 1. Объёмные лучи (POSITION_TEX_COLOR + WHITE_TEX = gbuffers_textured во всех шейдерпаках!)
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, ShaderCompat.WHITE_TEX);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (Col c : vis) shaft(bb, m, c, cam, moonTint, shaderScale);
        Tesselator.getInstance().end();

        // ---- 2. Цветные проекции свинцового переплёта витража на полу/стене (тоже POSITION_TEX_COLOR!)
        TextureAtlasSprite sp = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(GLASS_TEX);
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        RenderSystem.enablePolygonOffset();
        RenderSystem.polygonOffset(-3.0F, -3.0F);
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (Col c : vis) patch(bb, m, c, cam, sp, moonTint, shaderScale);
        Tesselator.getInstance().end();

        RenderSystem.polygonOffset(0.0F, 0.0F);
        RenderSystem.disablePolygonOffset();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static Vector3f v(Vec3 p, Vec3 cam) {
        return new Vector3f((float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z));
    }

    private static int[] tint(Col c, float moon) {
        int r = c.rgb >> 16 & 255, g = c.rgb >> 8 & 255, b = c.rgb & 255;
        float wr = moon > 0 ? 150 : 255, wg = moon > 0 ? 170 : 236, wb = moon > 0 ? 255 : 200;
        return new int[] {(int) Mth.lerp(0.35F, r, wr), (int) Mth.lerp(0.35F, g, wg), (int) Mth.lerp(0.35F, b, wb)};
    }

    private static void shaft(BufferBuilder bb, Matrix4f m, Col c, Vec3 cam, float moon, float shaderScale) {
        Vec3 a = along(c), d = c.dir;
        Vec3 o = Vec3.atBottomCenterOf(c.base);
        Vec3 b0 = o.subtract(a.scale(0.5)), b1 = o.add(a.scale(0.5));
        Vec3 t0 = b0.add(0, c.h, 0), t1 = b1.add(0, c.h, 0);
        Vec3 fb0 = b0.add(d.scale(c.lenB)), fb1 = b1.add(d.scale(c.lenB)), ft0 = t0.add(d.scale(c.lenT)), ft1 = t1.add(d.scale(c.lenT));
        int[] col = tint(c, moon);
        int an = (int) (255 * 0.20F * c.power * shaderScale);
        int af = (int) (255 * 0.035F * c.power * shaderScale);
        Vec3 mb0 = b0.add(t0).scale(0.5), mb1 = b1.add(t1).scale(0.5);
        Vec3 fm0 = fb0.add(ft0).scale(0.5), fm1 = fb1.add(ft1).scale(0.5);
        quad(bb, m, cam, t0, t1, ft1, ft0, col, an, af);
        quad(bb, m, cam, b0, b1, fb1, fb0, col, an, af);
        quad(bb, m, cam, mb0, mb1, fm1, fm0, col, an, af);
        if (c.leftOpen) quad(bb, m, cam, b0, t0, ft0, fb0, col, an, af);
        if (c.rightOpen) quad(bb, m, cam, b1, t1, ft1, fb1, col, an, af);
    }

    private static void quad(BufferBuilder bb, Matrix4f m, Vec3 cam, Vec3 n0, Vec3 n1, Vec3 f1, Vec3 f0, int[] c, int an, int af) {
        Vector3f p;
        p = v(n0, cam); bb.vertex(m, p.x, p.y, p.z).uv(0.5F, 0.5F).color(c[0], c[1], c[2], an).endVertex();
        p = v(n1, cam); bb.vertex(m, p.x, p.y, p.z).uv(0.5F, 0.5F).color(c[0], c[1], c[2], an).endVertex();
        p = v(f1, cam); bb.vertex(m, p.x, p.y, p.z).uv(0.5F, 0.5F).color(c[0], c[1], c[2], af).endVertex();
        p = v(f0, cam); bb.vertex(m, p.x, p.y, p.z).uv(0.5F, 0.5F).color(c[0], c[1], c[2], af).endVertex();
    }

    private static void patch(BufferBuilder bb, Matrix4f m, Col c, Vec3 cam, TextureAtlasSprite sp, float moon, float shaderScale) {
        if (c.grid == null) return;
        Vec3 a = along(c), d = c.dir;
        Vec3 o = Vec3.atBottomCenterOf(c.base);
        Vec3 lift = d.scale(-0.03);
        int alpha = (int) (255 * 0.8F * c.power * shaderScale);
        int r = moon > 0 ? 150 : 255, g = moon > 0 ? 175 : 245, b = moon > 0 ? 255 : 225;
        float u0 = sp.getU0(), u1 = sp.getU1(), v0 = sp.getV0(), v1 = sp.getV1();
        float[][] gr = c.grid;
        for (int i = 0; i < 2; ++i) {
            for (int j = 0; j < 2 * c.h; ++j) {
                float l00 = gr[i][j], l10 = gr[i + 1][j], l11 = gr[i + 1][j + 1], l01 = gr[i][j + 1];
                if (l00 < 0 || l10 < 0 || l11 < 0 || l01 < 0) continue;
                float mx = Math.max(Math.max(l00, l10), Math.max(l11, l01)), mn = Math.min(Math.min(l00, l10), Math.min(l11, l01));
                if (mx - mn > 3.5F) continue;
                Vec3 s00 = o.add(a.scale(i * 0.5 - 0.5)).add(0, j * 0.5, 0), s10 = o.add(a.scale(i * 0.5)).add(0, j * 0.5, 0);
                Vec3 s11 = s10.add(0, 0.5, 0), s01 = s00.add(0, 0.5, 0);
                float uA = Mth.lerp(i * 0.5F, u0, u1), uB = Mth.lerp(i * 0.5F + 0.5F, u0, u1);
                float vB = (j & 1) == 0 ? v1 : Mth.lerp(0.5F, v0, v1), vT = (j & 1) == 0 ? Mth.lerp(0.5F, v0, v1) : v0;
                Vector3f q;
                q = v(s00.add(d.scale(l00)).add(lift), cam); bb.vertex(m, q.x, q.y, q.z).uv(uA, vB).color(r, g, b, alpha).endVertex();
                q = v(s10.add(d.scale(l10)).add(lift), cam); bb.vertex(m, q.x, q.y, q.z).uv(uB, vB).color(r, g, b, alpha).endVertex();
                q = v(s11.add(d.scale(l11)).add(lift), cam); bb.vertex(m, q.x, q.y, q.z).uv(uB, vT).color(r, g, b, alpha).endVertex();
                q = v(s01.add(d.scale(l01)).add(lift), cam); bb.vertex(m, q.x, q.y, q.z).uv(uA, vT).color(r, g, b, alpha).endVertex();
            }
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || CHUNKS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused() || ++motesClock % 2 != 0) return;
        Vec3 eye = mc.player.getEyePosition();
        var rnd = mc.level.random;
        for (List<Col> l : CHUNKS.values()) {
            for (Col c : l) {
                if (c.power < 0.15F || c.dir == null || c.base.distToCenterSqr(eye) > 28 * 28 || rnd.nextInt(14) != 0) continue;
                double t = 0.15 + rnd.nextDouble() * 0.8, k = rnd.nextDouble();
                Vec3 o = Vec3.atBottomCenterOf(c.base).add(along(c).scale(rnd.nextDouble() - 0.5)).add(0, k * c.h, 0);
                Vec3 p = o.add(c.dir.scale((c.lenB + (c.lenT - c.lenB) * k) * t));
                int[] col = tint(c, 0);
                mc.level.addParticle(new DustParticleOptions(new Vector3f(col[0] / 255F, col[1] / 255F, col[2] / 255F), 0.55F),
                        p.x, p.y, p.z, 0, 0.002, 0);
            }
        }
    }
}
