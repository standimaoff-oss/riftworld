package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import ru.aiforge.riftworld.block.BridgePostBE;
import ru.aiforge.riftworld.bridge.BridgeDeckGeom;
import ru.aiforge.riftworld.bridge.BridgeStyle;

/**
 * Rev 7: the REAL suspension footbridge renderer (reference: the user's photo). Everything the player sees here is a
 * textured, lit mesh - no debug render types, no block staircase:
 *
 * <ul>
 *   <li>transverse deck planks with a visible thickness, alternating UV offset and a wooden lip at the edges;</li>
 *   <li>two longitudinal stringers under the deck edge;</li>
 *   <li>a main cable per side that rises to the post caps, with vertical hangers every ~1.2 blocks;</li>
 *   <li>a diamond safety net between the deck edge and the hand cable (chains for the burnt/mist styles);</li>
 *   <li>worn styles lose planks and get torn netting + dangling ends;</li>
 *   <li>the whole thing sways (client-side spring, no packets) and is lit by the level light at every sample.</li>
 * </ul>
 */
public class BridgePostRenderer implements BlockEntityRenderer<BridgePostBE> {
    private static final Map<BlockPos, float[]> SWAY = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Long> LAST_CREAK = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> TEX = new ConcurrentHashMap<>();

    public BridgePostRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    private static ResourceLocation entityTex(String name) {
        return TEX.computeIfAbsent(name, n -> new ResourceLocation("riftworld", "textures/entity/bridge/" + n + ".png"));
    }

    private static RenderType cutout(ResourceLocation tex) {
        return RenderType.entityCutoutNoCull(tex);
    }

    @Override
    public void render(BridgePostBE be, float partialTick, PoseStack pose, MultiBufferSource buf, int packedLight, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;
        BlockPos self = be.getBlockPos();

        //----- a tied cable between two posts that are not closed into a deck yet («в длину»)
        BlockPos partner = be.partner();
        if (partner != null && self.compareTo(partner) < 0) {
            BridgeStyle style = BridgePostRenderer.styleOf(level, self);
            Vec3 from = new Vec3(0.5, BridgeDeckGeom.CABLE_ANCHOR, 0.5);
            Vec3 to = new Vec3(partner.getX() - self.getX() + 0.5, partner.getY() - self.getY() + BridgeDeckGeom.CABLE_ANCHOR,
                    partner.getZ() - self.getZ() + 0.5);
            int light = LevelRenderer.getLightColor(level, self.above(2));
            ResourceLocation tex = entityTex(style.cableTex);
            int segs = 14;
            Vec3 prev = from;
            for (int i = 1; i <= segs; ++i) {
                double u = i / (double) segs;
                Vec3 p = from.lerp(to, u).add(0, -0.10 * from.distanceTo(to) * 0.25 * 4 * u * (1 - u), 0);
                tube(pose, buf.getBuffer(cutout(tex)), prev, p, 0.035, light, u * 4);
                prev = p;
            }
        }

        BridgeDeckGeom.Deck deck = be.deck();
        if (deck == null || !deck.master().equals(self)) return;

        BridgeStyle style = deck.style();
        float amp = tickSway(self, deck, level);
        double t = level.getGameTime() + partialTick;
        double ph1 = Math.sin(t * 0.42), ph2 = Math.sin(t * 0.31 + 1.2);

        ResourceLocation plankTex = entityTex(style.plankTex);
        ResourceLocation cableTex = entityTex(style.cableTex);
        ResourceLocation netTex = entityTex("net");

        pose.pushPose();
        pose.translate(-self.getX(), -self.getY(), -self.getZ());       // into world space

        Vec3 wdir = deck.widthDir();
        int n = deck.plankCount();
        int light = LevelRenderer.getLightColor(level, self.above(1));
        double halfW = deck.width() / 2.0;

        //------------------------------------------------------------------ planks
        VertexConsumer planks = buf.getBuffer(cutout(plankTex));
        for (int i = 0; i < n; ++i) {
            double u = (i + 0.5) / n;
            if (!deck.plankAt(u)) continue;
            double u0 = Math.max(0.0, (i + 0.02) / n), u1 = Math.min(1.0, (i + 0.98) / n);
            Vec3 p00 = sway(deck.pt(u0, 0), u0, 0, deck, amp, ph1, ph2, wdir);
            Vec3 p01 = sway(deck.pt(u0, 1), u0, 1, deck, amp, ph1, ph2, wdir);
            Vec3 p10 = sway(deck.pt(u1, 0), u1, 0, deck, amp, ph1, ph2, wdir);
            Vec3 p11 = sway(deck.pt(u1, 1), u1, 1, deck, amp, ph1, ph2, wdir);
            Vec3 up = up(p00, p01, p10);
            double th = BridgeDeckGeom.PLANK_TH;
            Vec3 d00 = p00.subtract(up.scale(th)), d01 = p01.subtract(up.scale(th));
            Vec3 d10 = p10.subtract(up.scale(th)), d11 = p11.subtract(up.scale(th));
            double uvOff = (i & 1) == 0 ? 0.0 : 0.5;
            // top (grain across the bridge)
            quad(planks, pose, p00, p10, p11, p01, uvOff, 0.0, uvOff + 0.5, 1.0, light, 0xFFFFFFFF);
            // bottom
            quad(planks, pose, d01, d11, d10, d00, 0.0, 0.0, 1.0, 1.0, light, 0xFFB9B0A4);
            // both long sides (the wooden lip you see in the photo)
            quad(planks, pose, d00, d10, p10, p00, 0.0, 0.0, 0.5, 0.34, light, 0xFFD8CFC2);
            quad(planks, pose, p01, p11, d11, d01, 0.0, 0.0, 0.5, 0.34, light, 0xFFCFC6B9);
            // the two short sides
            quad(planks, pose, d00, p00, p01, d01, 0.0, 0.0, 0.5, 0.34, light, 0xFFC7BEB1);
            quad(planks, pose, d10, d11, p11, p10, 0.0, 0.0, 0.5, 0.34, light, 0xFFC7BEB1);
        }

        //------------------------------------------------------------------ stringers under the deck edges
        VertexConsumer beams = buf.getBuffer(cutout(plankTex));
        for (int side = 0; side < 2; ++side) {
            Vec3 prev = null;
            for (int i = 0; i <= 24; ++i) {
                double u = i / 24.0;
                Vec3 p = sway(deck.edge(u, side), u, side, deck, amp, ph1, ph2, wdir).add(0, -0.22, 0);
                if (prev != null) tube(beams, pose, prev, p, 0.075, light, i * 0.6);
                prev = p;
            }
        }

        //------------------------------------------------------------------ main cables + hangers
        VertexConsumer cables = buf.getBuffer(cutout(cableTex));
        boolean chains = style.chainRail;
        for (int side = 0; side < 2; ++side) {
            Vec3 prevTop = null, prevMid = null;
            for (int i = 0; i <= 32; ++i) {
                double u = i / 32.0;
                if (deck.open(u)) { prevTop = null; prevMid = null; continue; }
                Vec3 c = deck.railTop(u, side);
                Vec3 pTop = sway(c, u, side, deck, amp, ph1, ph2, wdir);
                // the cable rises into the post cap at both ends
                if (prevTop != null) tube(cables, pose, prevTop, pTop, chains ? 0.026 : 0.038, light, i * 0.5);
                prevTop = pTop;
                if (!chains) {
                    Vec3 m = deck.railMid(u, side);
                    Vec3 pMid = sway(m, u, side, deck, amp, ph1, ph2, wdir);
                    if (prevMid != null) tube(cables, pose, prevMid, pMid, 0.024, light, i * 0.5);
                    prevMid = pMid;
                } else if (i % 6 == 0) {                       // burnt: a ragged chain hanging to the deck
                    Vec3 base = sway(deck.edge(u, side), u, side, deck, amp, ph1, ph2, wdir);
                    if (Math.abs(pTop.y - base.y) > 0.9) tube(cables, pose, pTop, pTop.lerp(base, 0.55), 0.018, light, 0);
                }
            }
            // vertical hangers + the safety net
            double[] us = deck.hangerUs();
            VertexConsumer net = buf.getBuffer(cutout(netTex));
            for (int i = 0; i < us.length; ++i) {
                double u = us[i];
                if (deck.open(u)) continue;
                Vec3 top = sway(deck.railTop(u, side), u, side, deck, amp, ph1, ph2, wdir);
                Vec3 bot = sway(deck.edge(u, side), u, side, deck, amp, ph1, ph2, wdir);
                tube(cables, pose, top, bot, 0.016, light, 0);
                if (!chains && i + 1 < us.length) {
                    double u2 = us[i + 1];
                    if (deck.open(u2)) continue;
                    // the mesh band between this hanger and the next one (two rows: deck->mid, mid->hand cable)
                    int l1 = LevelRenderer.getLightColor(level, BlockPos.containing(bot));
                    Vec3 t2 = sway(deck.railTop(u2, side), u2, side, deck, amp, ph1, ph2, wdir);
                    Vec3 b2 = sway(deck.edge(u2, side), u2, side, deck, amp, ph1, ph2, wdir);
                    long seed = (long) side * 977 + (long) Math.floor(u * 50) * 31 + self.asLong() * 7;
                    boolean torn = style.holes > 0.2F && (Math.floorMod(seed, 100) < 18);
                    if (!torn) {
                        Vec3 m1 = bot.lerp(top, 0.55), m2 = b2.lerp(t2, 0.55);
                        quad(net, pose, bot, b2, m2, m1, 0.0, 0.0, 1.6, 0.55, l1, 0xFFE8E8E8);
                        quad(net, pose, m1, m2, t2, top, 0.0, 0.0, 1.6, 0.45, l1, 0xFFDCDCDC);
                    } else {
                        // torn piece hanging down from the hand cable
                        tube(cables, pose, top, top.add(0, -0.45, 0), 0.014, l1, 0);
                    }
                }
            }
        }

        //------------------------------------------------------------------ dangling ends at a collapsed span
        if (deck.isBroken()) {
            Vec3 a = deck.pt(Math.max(0, deck.breakFrom())), b = deck.pt(Math.min(1, deck.breakTo()));
            for (int k = 0; k < 3; ++k) {
                double u = deck.breakFrom() + 0.004 * k;
                Vec3 p = deck.edge(Math.min(1, u), k % 2);
                tube(cables, pose, p, p.add(0, -1.0 - k * 0.55, 0), 0.02, level.getLightColor(level, self), 0);
            }
            Vec3 mid = a.lerp(b, 0.5).add(0, -1.1, 0);
            tube(planks, pose, a.add(0, -0.05, 0), mid, 0.03, light, 0);
        }
        pose.popPose();
    }

    /** Per-u horizontal sway + roll + bob (client-only, so the deck feels alive under the feet). */
    private static Vec3 sway(Vec3 p, double u, double w, BridgeDeckGeom.Deck deck, double amp, double ph1, double ph2, Vec3 wdir) {
        double shape = Math.sin(Math.PI * Mth.clamp(u, 0, 1));
        double lat = shape * amp * ph1;
        double roll = lat * 0.30 * (w - 0.5) * 2.0;
        double bob = -shape * amp * 0.30 * ph2;
        return p.add(wdir.x * lat, bob + roll, wdir.z * lat);
    }

    private static Vec3 up(Vec3 a, Vec3 b, Vec3 c) {
        Vec3 n = b.subtract(a).cross(c.subtract(a));
        if (n.lengthSqr() < 1e-8) return new Vec3(0, 1, 0);
        n = n.normalize();
        return n.y < 0 ? n.scale(-1) : n;
    }

    /** Client-side spring on the sway amplitude (stirred by players over the deck). */
    private static float tickSway(BlockPos key, BridgeDeckGeom.Deck deck, Level level) {
        float[] st = SWAY.computeIfAbsent(key, k -> new float[] {0.0F, -1.0F});
        long now = level.getGameTime();
        if ((long) st[1] == now) return st[0];
        st[1] = now;
        boolean stir = false;
        if (level instanceof ClientLevel cl) {
            var box = deck.stirBox();
            for (Player p : cl.players()) {
                if (p.getBoundingBox().inflate(0.6, 1.4, 0.6).intersects(box)) {
                    if (p.getDeltaMovement().lengthSqr() > 0.0004 || !p.onGround()) { stir = true; break; }
                }
            }
        }
        float target = stir ? 0.62F : 0.0F;
        float a = st[0] + (target - st[0]) * (stir ? 0.13F : 0.045F);
        st[0] = a;
        if (stir && a > 0.22F) {
            Long last = LAST_CREAK.get(key);
            if (last == null || now - last > 26) {
                LAST_CREAK.put(key, now);
                Vec3 mid = deck.pt(0.5);
                level.playLocalSound(mid.x, mid.y, mid.z, SoundEvents.SCAFFOLDING_STEP, SoundSource.BLOCKS,
                        0.30F + level.random.nextFloat() * 0.15F, 0.55F + level.random.nextFloat() * 0.4F, false);
                if (now % 78 == 0)
                    level.playLocalSound(mid.x, mid.y, mid.z, SoundEvents.CHAIN_STEP, SoundSource.BLOCKS, 0.22F, 0.8F, false);
            }
        }
        return st[0];
    }

    private static BridgeStyle styleOf(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof ru.aiforge.riftworld.block.BridgePostBlock) {
            return ru.aiforge.riftworld.block.BridgePostBlock.styleOf(state);
        }
        return BridgeStyle.PLAINS;
    }

    // ================================================================== drawing helpers

    /** A textured quad; UVs are given explicitly so textures tile by world size. */
    private static void quad(VertexConsumer vc, PoseStack pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                             double u0, double v0, double u1, double v1, int light, int color) {
        Matrix4f m = pose.last().pose();
        Matrix3f nrm = pose.last().normal();
        Vec3 n = b.subtract(a).cross(d.subtract(a));
        n = n.lengthSqr() < 1e-8 ? new Vec3(0, 1, 0) : n.normalize();
        float r = ((color >> 16) & 255) / 255f, g = ((color >> 8) & 255) / 255f, bl = (color & 255) / 255f, al = ((color >>> 24) & 255) / 255f;
        vert(vc, m, nrm, a, u0, v1, light, r, g, bl, al, n);
        vert(vc, m, nrm, b, u1, v1, light, r, g, bl, al, n);
        vert(vc, m, nrm, c, u1, v0, light, r, g, bl, al, n);
        vert(vc, m, nrm, d, u0, v0, light, r, g, bl, al, n);
    }

    /** A cable/hanger/chunk of a chain: a square tube between two points. */
    private static void tube(PoseStack pose, VertexConsumer vc, Vec3 from, Vec3 to, double r, int light, double uOff) {
        Vec3 axis = to.subtract(from);
        double len = axis.length();
        if (len < 1e-4) return;
        axis = axis.scale(1 / len);
        Vec3 ref = Math.abs(axis.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 s1 = axis.cross(ref).normalize().scale(r);
        Vec3 s2 = axis.cross(s1).normalize().scale(r);
        Matrix4f m = pose.last().pose();
        Matrix3f nrm = pose.last().normal();
        int vRep = Math.max(1, (int) Math.ceil(len * 3));
        Vec3[] a = {from.add(s1), from.subtract(s1), from.add(s2), from.subtract(s2)};
        Vec3[] b = {to.add(s1), to.subtract(s1), to.add(s2), to.subtract(s2)};
        Vec3[] nn = {s1, s1.scale(-1), s2, s2.scale(-1)};
        for (int i = 0; i < 4; ++i) {
            int j = (i + 1) % 4;
            face(vc, m, nrm, a[i], a[j], b[j], b[i], uOff, uOff + vRep, nn[i].normalize(), light);
        }
    }

    private static void face(VertexConsumer vc, Matrix4f m, Matrix3f nrm, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                             double u0, double u1, Vec3 n, int light) {
        vert(vc, m, nrm, a, u0, 1, light, 1, 1, 1, 1, n);
        vert(vc, m, nrm, b, u1, 1, light, 1, 1, 1, 1, n);
        vert(vc, m, nrm, c, u1, 0, light, 1, 1, 1, 1, n);
        vert(vc, m, nrm, d, u0, 0, light, 1, 1, 1, 1, n);
    }

    private static void vert(VertexConsumer vc, Matrix4f m, Matrix3f nrm, Vec3 p, double u, double v, int light,
                             float r, float g, float b, float a, Vec3 n) {
        vc.vertex(m, (float) p.x, (float) p.y, (float) p.z)
                .color(r, g, b, a)
                .uv((float) u, (float) v)
                .overlay(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(nrm, (float) n.x, (float) n.y, (float) n.z)
                .endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(BridgePostBE be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 160;
    }
}
