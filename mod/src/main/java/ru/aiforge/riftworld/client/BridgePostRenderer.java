package ru.aiforge.riftworld.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ru.aiforge.riftworld.block.BridgePostBE;
import ru.aiforge.riftworld.bridge.BridgeDeckGeom;
import ru.aiforge.riftworld.registry.BridgeBlocks;

/**
 * Draws the self-assembling bridge (Rev 4 item 13). «Качающийся» — strictly client-side: the deck sways whenever a
 * player stands or jumps on it, with zero packets to the server (a spring on a sway amplitude, stirred by entities
 * over the deck prism, decayed otherwise; wood creaks at turns). The deck follows a cable catenary between the two
 * post tips, so the bridge automatically gets a sag (внизу) and rising handrails.
 */
public class BridgePostRenderer implements BlockEntityRenderer<BridgePostBE> {
    /** sway amplitude per master corner; client-only */
    private static final Map<BlockPos, float[]> SWAY = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Long> LAST_CREAK = new ConcurrentHashMap<>();

    private static final int ROPE = 0xFFBF9D6E;      // tan rope
    private static final int KNOT = 0xFF4C3A24;      // dark rope
    private static final int WOOD_A = 0xFF6E5233;    // plank
    private static final int WOOD_B = 0xFF63452C;    // alternate plank

    public BridgePostRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(BridgePostBE be, float partialTick, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;
        BlockPos self = be.getBlockPos();

        //----- lengthwise cable («в длину»), drawn once by the lexicographically smaller end
        BlockPos partner = be.partner();
        if (partner != null && self.compareTo(partner) < 0) {
            VertexConsumer lines = buf.getBuffer(RenderType.debugLineStrip(3.0));
            Vec3 from = new Vec3(0.5, 1.02, 0.5);
            Vec3 to = new Vec3(partner.getX() - self.getX() + 0.5, partner.getY() - self.getY() + 1.02,
                    partner.getZ() - self.getZ() + 0.5);
            catenary(lines, pose, from, to, 0.06 * from.distanceTo(to), 14, ROPE);
        }

        //----- the deck («в ширину»): only the master corner renders it
        BridgeDeckGeom.Deck deck = be.deck();
        if (deck == null || !deck.master().equals(self)) return;

        float amp = tickSway(self, deck, level);
        double t = level.getGameTime() + partialTick;
        double f1 = Math.sin(t * 0.42), f2 = Math.sin(t * 0.31 + 1.2);

        Vec3 wdir = deck.widthDir();
        double half = deck.width() / 2 + 0.10;

        pose.pushPose();
        pose.translate(-self.getX(), -self.getY(), -self.getZ());   // into world space
        VertexConsumer quads = buf.getBuffer(RenderType.debugQuads());
        VertexConsumer ropes = buf.getBuffer(RenderType.debugLineStrip(2.5));

        // deck planks: follow the sag + the live sway (WITH wooden gaps between the boards)
        int n = Math.max(2, (int) Math.ceil(deck.len() / BridgeDeckGeom.PLANK_STEP));
        for (int i = 0; i < n; ++i) {
            double u0 = (i + 0.08) / (double) n, u1 = (i + 0.92) / (double) n;
            if (!hasCell(level, deck, (u0 + u1) / 2)) continue;      // punched holes stay open
            Vec3[] mid = new Vec3[2];
            double[] roll = new double[2];
            for (int k = 0; k < 2; ++k) {
                double u = k == 0 ? u0 : u1;
                double lat = Math.sin(Math.PI * u) * amp * f1;
                double bob = -Math.sin(Math.PI * u) * amp * 0.35 * f2;
                mid[k] = deck.pt(u).add(wdir.scale(lat)).add(0, bob, 0);
                roll[k] = lat * 0.4;
            }
            Vec3 c00 = mid[0].add(wdir.scale(-half)).add(0, -roll[0] - 0.06, 0);
            Vec3 c01 = mid[0].add(wdir.scale(half)).add(0, roll[0] - 0.06, 0);
            Vec3 c10 = mid[1].add(wdir.scale(-half)).add(0, -roll[1] - 0.06, 0);
            Vec3 c11 = mid[1].add(wdir.scale(half)).add(0, roll[1] - 0.06, 0);
            int col = (i & 1) == 0 ? WOOD_A : WOOD_B;
            quad(quads, pose, c00, c01, c11, c10, col);
            quad(quads, pose, c10, c11, c01, c00, col);              // double-sided
        }
        // the seam knot-cord under the boards, one per side
        for (int w = 0; w <= 1; ++w) {
            Vec3 prev = null;
            for (int i = 0; i <= 16; ++i) {
                double u = i / 16.0;
                double lat = Math.sin(Math.PI * u) * amp * f1;
                Vec3 p = deck.pt(u, w == 0 ? -0.05 : 1.05).add(wdir.scale(lat)).add(0, -0.05, 0);
                if (prev != null) line(ropes, pose, prev, p, KNOT);
                prev = p;
            }
        }
        // handrails: they rise at the post tips and droop toward mid-span (как у настоящего верёвочного моста)
        for (int w = 0; w <= 1; ++w) {
            Vec3 prev = null, prevHang = null;
            for (int i = 0; i <= 16; ++i) {
                double u = i / 16.0;
                double lat = Math.sin(Math.PI * u) * amp * f1;
                Vec3 p = deck.pt(u, w).add(wdir.scale(lat)).add(0, 0.42, 0)
                        .add(0, 1.15 * Math.pow(Math.abs(u - 0.5) * 2, 2.0), 0);
                if (prev != null) line(ropes, pose, prev, p, ROPE);
                if (i % 2 == 0 && i > 0 && i < 16 && hasCell(level, deck, u)) {
                    Vec3 d0 = deck.pt(u, w).add(wdir.scale(lat)).add(0, -0.02, 0);
                    line(ropes, pose, p, d0, KNOT);                 // the hanger cords
                }
                prev = p;
            }
        }
        pose.popPose();
    }

    /** True while the deck's collision cell stands at u (the plank exists) — fixed to also allow solid ground. */
    private static boolean hasCell(Level level, BridgeDeckGeom.Deck deck, double u) {
        Vec3 p = deck.pt(u, 0.5);
        BlockPos bp = BlockPos.containing(p.x, p.y - 0.85, p.z);
        var st = level.getBlockState(bp);
        if (st.is(BridgeBlocks.WALK.get())) return true;
        // punched hole: air with air below -> false (hole stays open)
        // solid ground or walkable terrain -> treat as walkable so plank still renders over terrain
        if (st.isSolidRender(level, bp)) return true;
        BlockPos below = bp.below();
        var belowSt = level.getBlockState(below);
        if (belowSt.isSolidRender(level, below) && !belowSt.isAir()) {
            // bridge over solid ground, WALK may not have been placed to preserve ground
            return true;
        }
        // if current is air but below is WALK or solid, still render? No, for gap we need WALK
        // For gap, we only render if WALK exists, so if we reach here and st is air and below is air -> hole
        return false;
    }

    /** Client-side spring on the sway amplitude. */
    private static float tickSway(BlockPos key, BridgeDeckGeom.Deck deck, Level level) {
        float[] st = SWAY.computeIfAbsent(key, k -> new float[] {0.0F, -1.0F});
        long now = level.getGameTime();
        if ((long) st[1] == now) return st[0];                       // once per tick per bridge
        st[1] = now;
        boolean stir = false;
        if (level instanceof ClientLevel cl) {
            var box = deck.stirBox();
            for (Player p : cl.players()) {
                if (p.getBoundingBox().inflate(0.5, 1.2, 0.5).intersects(box)) {
                    double sp = p.getDeltaMovement().lengthSqr();
                    if (sp > 0.0004 || !p.onGround()) { stir = true; break; }
                }
            }
        }
        float target = stir ? 0.75F : 0.0F;
        float a = st[0] + (target - st[0]) * (stir ? 0.14F : 0.05F);
        st[0] = a;
        if (stir && a > 0.25F) {                                     // the creak
            Long last = LAST_CREAK.get(key);
            if (last == null || now - last > 22) {
                LAST_CREAK.put(key, now);
                Vec3 mid = deck.pt(0.5);
                level.playLocalSound(mid.x, mid.y, mid.z, SoundEvents.SCAFFOLDING_PLACE, SoundSource.BLOCKS,
                        0.35F + level.random.nextFloat() * 0.2F, 0.6F + level.random.nextFloat() * 0.5F, false);
            }
        }
        return st[0];
    }

    //----- drawing helpers ---------------------------------------------------------------------

    private static void catenary(VertexConsumer vc, PoseStack pose, Vec3 from, Vec3 to, double sag, int segs, int color) {
        Vec3 prev = null;
        for (int i = 0; i <= segs; ++i) {
            double u = i / (double) segs;
            Vec3 p = from.lerp(to, u).add(0, -sag * 4 * u * (1 - u), 0);
            if (prev != null) line(vc, pose, prev, p, color);
            prev = p;
        }
    }

    private static void line(VertexConsumer vc, PoseStack pose, Vec3 a, Vec3 b, int color) {
        float r = ((color >> 16) & 255) / 255f, g = ((color >> 8) & 255) / 255f, bl = (color & 255) / 255f, al = ((color >> 24) & 255) / 255f;
        var m = pose.last().pose();
        Vec3 n = b.subtract(a);
        n = n.lengthSqr() < 1e-8 ? Vec3.ZERO : n.normalize();
        vc.vertex(m, (float) a.x, (float) a.y, (float) a.z).color(r, g, bl, al).normal(pose.last().normal(), (float) n.x, (float) n.y, (float) n.z).endVertex();
        vc.vertex(m, (float) b.x, (float) b.y, (float) b.z).color(r, g, bl, al).normal(pose.last().normal(), (float) n.x, (float) n.y, (float) n.z).endVertex();
    }

    private static void quad(VertexConsumer vc, PoseStack pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
        float r = ((color >> 16) & 255) / 255f, g = ((color >> 8) & 255) / 255f, bl = (color & 255) / 255f, al = ((color >> 24) & 255) / 255f;
        var m = pose.last().pose();
        vc.vertex(m, (float) a.x, (float) a.y, (float) a.z).color(r, g, bl, al).normal(pose.last().normal(), 0, 1, 0).endVertex();
        vc.vertex(m, (float) b.x, (float) b.y, (float) b.z).color(r, g, bl, al).normal(pose.last().normal(), 0, 1, 0).endVertex();
        vc.vertex(m, (float) c.x, (float) c.y, (float) c.z).color(r, g, bl, al).normal(pose.last().normal(), 0, 1, 0).endVertex();
        vc.vertex(m, (float) d.x, (float) d.y, (float) d.z).color(r, g, bl, al).normal(pose.last().normal(), 0, 1, 0).endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(BridgePostBE be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
