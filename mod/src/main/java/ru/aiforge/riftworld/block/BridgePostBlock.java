package ru.aiforge.riftworld.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import ru.aiforge.riftworld.bridge.BridgeDeckGeom;
import ru.aiforge.riftworld.registry.BridgeBlocks;
import ru.aiforge.riftworld.registry.ModItems;

/**
 * Rev 4 item 13: one of FOUR posts of the self-assembling rope bridge.
 * Fixed: more forgiving mate search, rope in either hand, deck always builds even over solid ground,
 * better messages.
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
        boolean hasRopeMain = player.getItemInHand(hand).is(ModItems.BRIDGE_ROPE.get());
        InteractionHand ropeHand = hand;
        if (!hasRopeMain) {
            InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            if (player.getItemInHand(other).is(ModItems.BRIDGE_ROPE.get())) {
                hasRopeMain = true;
                ropeHand = other;
            }
        }
        if (!hasRopeMain) return InteractionResult.PASS;

        if (be.deck() != null) {
            msg(player, "Мост уже собран. Сломай опору — разберёшь его.");
            return InteractionResult.CONSUME;
        }
        if (be.partner() == null) {
            BlockPos mate = findMate(level, pos, player);
            if (mate == null) {
                msg(player, "Нет второй опоры в поле зрения (до " + BridgeDeckGeom.MAX_LEN + " блоков, по высоте ±2). Смотри вдоль моста.");
                return InteractionResult.CONSUME;
            }
            be.pair(mate);
            if (level.getBlockEntity(mate) instanceof BridgePostBE other) other.pair(pos);
            takeRope(player, ropeHand, 1);
            level.playSound(null, pos, SoundEvents.LEASH_KNOT_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            msg(player, "Кабель протянут в длину. Теперь вторая пара опор рядом — и ещё раз верёвкой по опоре (в ширину).");
            return InteractionResult.CONSUME;
        }
        // close rectangle (в ширину)
        BlockPos a = pos, b = be.partner();
        BridgeDeckGeom.Deck deck = findRect(level, a, b);
        if (deck == null) {
            msg(player, "Рядом нет второй связанной пары опор: протяни кабель и для второй пары (ширина до " + BridgeDeckGeom.MAX_WID + ", длина до " + BridgeDeckGeom.MAX_LEN + ").");
            return InteractionResult.CONSUME;
        }
        int placed = 0;
        for (BlockPos cell : deck.plankCells()) {
            BlockState cur = level.getBlockState(cell);
            if (cur.isAir() || cur.canBeReplaced() || cur.is(BridgeBlocks.WALK.get())) {
                level.setBlock(cell, BridgeBlocks.WALK.get().defaultBlockState(), 3);
                placed++;
            } else {
                // solid ground already — don't replace, but still count as built
                placed++;
            }
        }
        if (placed == 0) {
            msg(player, "Не удалось поставить настил — место занято.");
            return InteractionResult.CONSUME;
        }
        level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.2F, 0.8F);
        level.playSound(null, pos, SoundEvents.LEASH_KNOT_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
        for (BlockPos cp : deck.corners())
            if (level.getBlockEntity(cp) instanceof BridgePostBE c) c.stand(deck);
        takeRope(player, ropeHand, 2);
        msg(player, "Мост собран! Длина " + String.format("%.1f", deck.len()) + ", ширина " + String.format("%.1f", deck.width()) + ". Качается!");
        return InteractionResult.CONSUME;
    }

    private static BlockPos findMate(Level level, BlockPos self, Player player) {
        double vx = player.getLookAngle().x, vz = player.getLookAngle().z;
        double len = Math.sqrt(vx * vx + vz * vz);
        if (len < 1e-6) { vx = 1; vz = 0; len = 1; }
        vx /= len; vz /= len;
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(self.offset(-BridgeDeckGeom.MAX_LEN, -2, -BridgeDeckGeom.MAX_LEN),
                self.offset(BridgeDeckGeom.MAX_LEN, 2, BridgeDeckGeom.MAX_LEN))) {
            if (p.equals(self)) continue;
            if (!(level.getBlockState(p).getBlock() instanceof BridgePostBlock)) continue;
            if (level.getBlockEntity(p) instanceof BridgePostBE c) {
                if (c.partner() != null) continue;
                if (c.deck() != null) continue;
            }
            double dx = p.getX() - self.getX(), dy = p.getY() - self.getY(), dz = p.getZ() - self.getZ();
            double horiz = Math.hypot(dx, dz);
            if (horiz < 2 || horiz > BridgeDeckGeom.MAX_LEN) continue;
            if (Math.abs(dy) > 2) continue;
            double align = (dx * vx + dz * vz) / (horiz == 0 ? 1 : horiz);
            if (align < 0.25) continue;
            double score = horiz * (2.2 - align) + Math.abs(dy) * 0.5;
            if (score < bestScore) { bestScore = score; best = p.immutable(); }
        }
        if (best == null) {
            for (BlockPos p : BlockPos.betweenClosed(self.offset(-BridgeDeckGeom.MAX_LEN, -2, -BridgeDeckGeom.MAX_LEN),
                    self.offset(BridgeDeckGeom.MAX_LEN, 2, BridgeDeckGeom.MAX_LEN))) {
                if (p.equals(self)) continue;
                if (!(level.getBlockState(p).getBlock() instanceof BridgePostBlock)) continue;
                if (level.getBlockEntity(p) instanceof BridgePostBE c && (c.partner() != null || c.deck() != null)) continue;
                double dx = p.getX() - self.getX(), dz = p.getZ() - self.getZ();
                double horiz = Math.hypot(dx, dz);
                if (horiz < 2 || horiz > BridgeDeckGeom.MAX_LEN) continue;
                double dy = p.getY() - self.getY();
                if (Math.abs(dy) > 2) continue;
                double score = horiz + Math.abs(dy);
                if (score < bestScore) { bestScore = score; best = p.immutable(); }
            }
        }
        return best;
    }

    private static BridgeDeckGeom.Deck findRect(Level level, BlockPos a, BlockPos b) {
        for (BlockPos origin : new BlockPos[]{a, b}) {
            for (BlockPos wa : BlockPos.betweenClosed(origin.offset(-BridgeDeckGeom.MAX_WID, -2, -BridgeDeckGeom.MAX_WID),
                    origin.offset(BridgeDeckGeom.MAX_WID, 2, BridgeDeckGeom.MAX_WID))) {
                if (wa.equals(a) || wa.equals(b)) continue;
                if (!(level.getBlockState(wa).getBlock() instanceof BridgePostBlock)) continue;
                if (!(level.getBlockEntity(wa) instanceof BridgePostBE wbe)) continue;
                if (wbe.partner() == null || wbe.deck() != null) continue;
                BlockPos wb = wbe.partner();
                if (wb.equals(a) || wb.equals(b) || wb.equals(wa)) continue;
                if (!(level.getBlockState(wb).getBlock() instanceof BridgePostBlock)) continue;
                if (level.getBlockEntity(wb) instanceof BridgePostBE wbe2 && wbe2.deck() != null) continue;
                BlockPos want1 = b.offset(wa.getX() - a.getX(), wa.getY() - a.getY(), wa.getZ() - a.getZ());
                BlockPos want2 = a.offset(wa.getX() - b.getX(), wa.getY() - b.getY(), wa.getZ() - b.getZ());
                boolean ok = wb.distManhattan(want1) <= 4 || wb.distManhattan(want2) <= 4;
                BlockPos want3 = a.offset(wb.getX() - b.getX(), wb.getY() - b.getY(), wb.getZ() - b.getZ());
                BlockPos want4 = b.offset(wb.getX() - a.getX(), wb.getY() - a.getY(), wb.getZ() - a.getZ());
                ok = ok || wa.distManhattan(want3) <= 4 || wa.distManhattan(want4) <= 4;
                if (!ok) {
                    double lenAB = Math.sqrt(a.distSqr(b));
                    double lenWaWb = Math.sqrt(wa.distSqr(wb));
                    if (Math.abs(lenAB - lenWaWb) > 5) continue;
                    double wid1 = Math.sqrt(wa.distSqr(a));
                    double wid2 = Math.sqrt(wb.distSqr(b));
                    double wid3 = Math.sqrt(wa.distSqr(b));
                    double wid4 = Math.sqrt(wb.distSqr(a));
                    double minWid = Math.min(Math.min(wid1, wid2), Math.min(wid3, wid4));
                    if (minWid > BridgeDeckGeom.MAX_WID) continue;
                    if (minWid < 0.5) continue;
                }
                return new BridgeDeckGeom.Deck(a.immutable(), b, wa.immutable(), wb);
            }
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
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof BridgePostBE be) {
            BlockPos partner = be.partner();
            if (partner != null && level.getBlockEntity(partner) instanceof BridgePostBE other) other.unpair();
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
