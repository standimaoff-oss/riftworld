package ru.aiforge.riftworld.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import ru.aiforge.riftworld.bridge.BridgeDeckGeom;
import ru.aiforge.riftworld.bridge.BridgeStyle;
import ru.aiforge.riftworld.registry.BridgeBlocks;
import ru.aiforge.riftworld.registry.ModItems;

/**
 * Rev 7: one of FOUR posts of the suspension footbridge (Rev 4 item 13 rebuilt). A post is a real 2.5-block tower with
 * a stone plinth, a shaft, a cap with brackets and a lantern; its STYLE follows the biome it is placed in (ordinary,
 * gardens, grove, burnt = Ashen Lands, mist).
 *
 * <p>Assembly is unchanged in spirit: place the four posts, tie a rope to the far post of the first pair («в длину»),
 * do the same for the second pair, then right-click a post again («в ширину») - the deck, its cables, hangers and net
 * appear between the four posts at any angle and any height difference.</p>
 */
public class BridgePostBlock extends Block implements EntityBlock {
    /** 0 plains, 1 garden, 2 grove, 3 burnt, 4 mist. */
    public static final IntegerProperty STYLE = IntegerProperty.create("style", 0, 4);
    private static final VoxelShape POST_SHAPE = Block.box(3, 0, 3, 13, 16, 13);

    public BridgePostBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(STYLE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(STYLE);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return POST_SHAPE;
    }

    public static BridgeStyle styleOf(BlockState state) {
        return BridgeStyle.of(state.getValue(STYLE));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BridgePostBE(pos, state);
    }

    /** Pick the look of the dimension/biome the post is standing in (muted palettes, auto). */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;
        String path = level.getBiome(pos).unwrapKey().map(k -> k.location().getPath()).orElse("");
        int style = BridgeDeckGeom.styleFor(path);
        if (style != state.getValue(STYLE)) level.setBlock(pos, state.setValue(STYLE, style), 2);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof BridgePostBE be)) return InteractionResult.PASS;
        if (!player.getItemInHand(hand).is(ModItems.BRIDGE_ROPE.get())) return InteractionResult.PASS;

        if (be.deck() != null) {
            msg(player, "Мост уже собран. Сломай опору - он разберётся.");
            return InteractionResult.CONSUME;
        }
        if (be.partner() == null) {
            BlockPos mate = findMate(level, pos, player);
            if (mate == null) {
                msg(player, "Нет второй опоры в поле зрения (до " + BridgeDeckGeom.MAX_LEN + " блоков, по высоте ±3).");
                return InteractionResult.CONSUME;
            }
            be.pair(mate);
            if (level.getBlockEntity(mate) instanceof BridgePostBE other) other.pair(pos);
            takeRope(player, hand, 1);
            level.playSound(null, pos, SoundEvents.LEASH_KNOT_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            msg(player, "Трос протянут («в длину»). Теперь вторая пара опор - и ещё раз по опоре («в ширину»).");
            return InteractionResult.CONSUME;
        }
        //----- close the rectangle («в ширину»)
        if (!(level.getBlockEntity(be.partner()) instanceof BridgePostBE other)) return InteractionResult.CONSUME;
        BlockPos a = pos, b = be.partner();
        BlockPos[] rect = findRect(level, a, b);
        if (rect == null) {
            msg(player, "Рядом нет второй связанной пары опор: протяни трос и для них (ширина до " + BridgeDeckGeom.MAX_WID + ").");
            return InteractionResult.CONSUME;
        }
        int style = state.getValue(STYLE);
        BridgeDeckGeom.Deck deck = new BridgeDeckGeom.Deck(rect[0], rect[1], rect[2], rect[3], style, 0F, 0F);
        placeCells(level, deck);
        level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.2F, 0.8F);
        level.playSound(null, pos, SoundEvents.LEASH_KNOT_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
        level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.7F, 1.4F);
        for (BlockPos cp : deck.corners())
            if (level.getBlockEntity(cp) instanceof BridgePostBE c) c.stand(deck);
        takeRope(player, hand, 2);
        msg(player, "Мост собран («" + styleName(style) + "»). Он качается под ногами и держит настил тросами.");
        return InteractionResult.CONSUME;
    }

    private static String styleName(int id) {
        return switch (BridgeStyle.of(id)) {
            case GARDEN -> "садовый";
            case GROVE -> "роща";
            case BURNT -> "сгоревший";
            case MIST -> "туманный";
            default -> "обычный";
        };
    }

    /** Writes the invisible collision plates of a deck (also used by the world generator). */
    public static void placeCells(Level level, BridgeDeckGeom.Deck deck) {
        for (var e : deck.collisionCells().entrySet()) {
            BlockPos cell = e.getKey();
            BlockState cur = level.getBlockState(cell);
            BlockState want = BridgeBlocks.WALK.get().defaultBlockState().setValue(BridgeWalkBlock.TOP, e.getValue());
            if (cur.isAir() || cur.canBeReplaced() || cur.is(BridgeBlocks.WALK.get())) {
                level.setBlock(cell, want, 2);
            }
        }
    }

    /** The far post in the direction the player looks (prefers alignment, same height +/-3, within reach). */
    private static BlockPos findMate(Level level, BlockPos self, Player player) {
        double vx = player.getLookAngle().x, vz = player.getLookAngle().z;
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(self.offset(-BridgeDeckGeom.MAX_LEN, -3, -BridgeDeckGeom.MAX_LEN),
                self.offset(BridgeDeckGeom.MAX_LEN, 3, BridgeDeckGeom.MAX_LEN))) {
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
    private static BlockPos[] findRect(Level level, BlockPos a, BlockPos b) {
        for (BlockPos wa : BlockPos.betweenClosed(a.offset(-BridgeDeckGeom.MAX_WID, -3, -BridgeDeckGeom.MAX_WID),
                a.offset(BridgeDeckGeom.MAX_WID, 3, BridgeDeckGeom.MAX_WID))) {
            if (wa.equals(a) || wa.equals(b) || !(level.getBlockState(wa).getBlock() instanceof BridgePostBlock)) continue;
            if (!(level.getBlockEntity(wa) instanceof BridgePostBE wbe) || wbe.partner() == null) continue;
            BlockPos wb = wbe.partner();
            BlockPos want = b.offset(wa.getX() - a.getX(), wa.getY() - a.getY(), wa.getZ() - a.getZ());
            if (wb.distManhattan(want) > 3) continue;
            double wid = Math.sqrt(wa.distSqr(a));
            if (wid > BridgeDeckGeom.MAX_WID) continue;
            return new BlockPos[]{a.immutable(), b, wa.immutable(), wb};   // стиль/обрыв дорисовывает вызывающий
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
            BlockPos partner = be.partner();
            if (partner != null && level.getBlockEntity(partner) instanceof BridgePostBE other) other.unpair();
            BridgeDeckGeom.Deck deck = be.deck();
            if (deck != null) {
                for (BlockPos cell : deck.collisionCells().keySet())
                    if (level.getBlockState(cell).is(BridgeBlocks.WALK.get())) level.setBlock(cell, Blocks.AIR.defaultBlockState(), 3);
                for (BlockPos cp : deck.corners())
                    if (!cp.equals(pos) && level.getBlockEntity(cp) instanceof BridgePostBE c) c.clearDeck();
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
