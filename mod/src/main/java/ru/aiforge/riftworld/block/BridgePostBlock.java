package ru.aiforge.riftworld.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import ru.aiforge.riftworld.bridge.BridgeDeckGeom;
import ru.aiforge.riftworld.registry.BridgeBlocks;
import ru.aiforge.riftworld.registry.ModItems;

/**
 * Rev 4 item 13: one of FOUR posts of the self-assembling rope bridge. Build: place post A, tie a rope to the far
 * post B («в длину») - do the same with the second pair - right-click any post again («в ширину»): the bridge
 * assembles its sagging deck at any angle and sways as you walk and jump.
 */
public class BridgePostBlock extends Block implements EntityBlock {
    public BridgePostBlock(Properties p) {
        super(p);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BridgePostBE(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof BridgePostBE be)) return InteractionResult.PASS;
        if (!player.getItemInHand(hand).is(ModItems.BRIDGE_ROPE.get())) return InteractionResult.PASS;

        if (be.deck() != null) {
            msg(player, "Мост уже собран. Сломат опору - разберёшь его.");
            return InteractionResult.CONSUME;
        }
        if (be.partner() == null) {
            BlockPos mate = findMate(level, pos, player);
            if (mate == null) {
                msg(player, "Нет второй опоры в поле зрения (до " + BridgeDeckGeom.MAX_LEN + " блоков, по высоте ±2).");
                return InteractionResult.CONSUME;
            }
            be.pair(mate);
            if (level.getBlockEntity(mate) instanceof BridgePostBE other) other.pair(pos);
            takeRope(player, hand, 1);
            level.playSound(null, pos, SoundEvents.LEASH_KNOT_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            msg(player, "Кабель протянут. Теперь вторая пара опор - и ещё раз по опоре («в ширину»).");
            return InteractionResult.CONSUME;
        }
        //----- close the rectangle («в ширину»)
        BridgePostBE other = level.getBlockEntity(be.partner()) instanceof BridgePostBE o ? o : null;
        if (other == null) return InteractionResult.CONSUME;
        BlockPos a = pos, b = be.partner();
        BridgeDeckGeom.Deck deck = findRect(level, a, b);
        if (deck == null) {
            msg(player, "Рядом нет второй связанной пары опор: протяни кабель и для них (ширина до " + BridgeDeckGeom.MAX_WID + ").");
            return InteractionResult.CONSUME;
        }
        // reserve the deck: planks as invisible collision cells
        for (BlockPos cell : deck.plankCells()) {
            BlockState cur = level.getBlockState(cell);
            if (cur.isAir() || cur.canBeReplaced()) level.setBlock(cell, BridgeBlocks.WALK.get().defaultBlockState(), 2);
        }
        level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.2F, 0.8F);
        level.playSound(null, pos, SoundEvents.LEASH_KNOT_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
        for (BlockPos cp : deck.corners())
            if (level.getBlockEntity(cp) instanceof BridgePostBE c) c.stand(deck);
        takeRope(player, hand, 2);
        msg(player, "Мост собран! Осторожно: он качается под ногами.");
        return InteractionResult.CONSUME;
    }

    /** The far post in the direction the player looks (prefers alignment, same height +/-2, within reach). */
    private static BlockPos findMate(Level level, BlockPos self, Player player) {
        double vx = player.getLookAngle().x, vz = player.getLookAngle().z;
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(self.offset(-BridgeDeckGeom.MAX_LEN, -2, -BridgeDeckGeom.MAX_LEN),
                self.offset(BridgeDeckGeom.MAX_LEN, 2, BridgeDeckGeom.MAX_LEN))) {
            if (p.equals(self) || !(level.getBlockState(p).getBlock() instanceof BridgePostBlock)) continue;
            if (level.getBlockEntity(p) instanceof BridgePostBE c && c.partner() != null) continue;
            double dx = p.getX() - self.getX(), dz = p.getZ() - self.getZ();
            double dist = Math.hypot(dx, dz);
            if (dist < 2) continue;
            double align = (dx * vx + dz * vz) / dist;
            if (align < 0.45) continue;
            double score = dist * (2.0 - align);
            if (score < bestScore) { bestScore = score; best = p.immutable(); }
        }
        return best;
    }

    /** Looks for a second cabled pair near A that mirrors the A-B line into a rectangle. */
    private static BridgeDeckGeom.Deck findRect(Level level, BlockPos a, BlockPos b) {
        for (BlockPos wa : BlockPos.betweenClosed(a.offset(-BridgeDeckGeom.MAX_WID, -2, -BridgeDeckGeom.MAX_WID),
                a.offset(BridgeDeckGeom.MAX_WID, 2, BridgeDeckGeom.MAX_WID))) {
            if (wa.equals(a) || wa.equals(b) || !(level.getBlockState(wa).getBlock() instanceof BridgePostBlock)) continue;
            if (!(level.getBlockEntity(wa) instanceof BridgePostBE wbe) || wbe.partner() == null) continue;
            BlockPos wb = wbe.partner();
            // rectangle check: wb must sit at (b + (wa - a)) within 3 blocks
            BlockPos want = b.offset(wa.getX() - a.getX(), wa.getY() - a.getY(), wa.getZ() - a.getZ());
            if (wb.distManhattan(want) > 3) continue;
            double wid = Math.sqrt(wa.distSqr(a));
            if (wid > BridgeDeckGeom.MAX_WID) continue;
            return new BridgeDeckGeom.Deck(a.immutable(), b, wa.immutable(), wb);
        }
        return null;
    }

    private static void takeRope(Player player, InteractionHand hand, int n) {
        if (!player.getAbilities().instabuild) player.getItemInHand(hand).shrink(n);
    }

    private static void msg(Player player, String s) {
        player.displayClientMessage(Component.literal(s), true);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof BridgePostBE be) {
            // release the partner's knot
            BlockPos partner = be.partner();
            if (partner != null && level.getBlockEntity(partner) instanceof BridgePostBE other) other.unpair();
            // collapse the deck: clear walk cells and unstand all corners
            BridgeDeckGeom.Deck deck = be.deck();
            if (deck != null) {
                for (BlockPos cell : deck.plankCells())
                    if (level.getBlockState(cell).is(BridgeBlocks.WALK.get())) level.setBlock(cell, Blocks.AIR.defaultBlockState(), 3);
                for (BlockPos cp : deck.corners())
                    if (!cp.equals(pos) && level.getBlockEntity(cp) instanceof BridgePostBE c) c.clearDeck();
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
