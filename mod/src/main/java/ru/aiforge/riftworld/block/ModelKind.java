package ru.aiforge.riftworld.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Footprint and GLB of every 3D-model block. Offsets are given in the model frame (front = south):
 * x = to the viewer's right (east), back = rows behind the front row (north), h = height in blocks.
 */
public enum ModelKind {
    PORTAL_HELM("portal_helm", -1, 1, 1, 3, Block.box(0, 0, 0, 16, 16, 16)),
    PORTAL_LEVER("portal_lever", 0, 0, 0, 2, Block.box(3, 0, 3, 13, 16, 13)),
    CONTROL_CONSOLE("control_console", 0, 1, 0, 2, Block.box(0, 0, 0, 16, 16, 16)),
    LATHE("lathe", 0, 1, 0, 1, Block.box(0, 0, 2, 16, 16, 14)),
    MILLING_MACHINE("milling_machine", 0, 0, 0, 2, Block.box(1, 0, 1, 15, 16, 15)),
    SPECIMEN_TANK("specimen_tank", 0, 0, 0, 3, Block.box(1, 0, 1, 15, 16, 15)),
    ELECTRICAL_CABINET("electrical_cabinet", 0, 0, 0, 2, Block.box(1, 0, 1, 15, 16, 15)),
    GENERATOR("generator", -1, 1, 1, 2, Block.box(0, 0, 0, 16, 16, 16)),
    TRANSFORMER("transformer", 0, 1, 0, 2, Block.box(0, 0, 0, 16, 16, 16)),
    SERVER_RACK("server_rack", 0, 0, 0, 2, Block.box(1, 0, 1, 15, 16, 15)),
    SECURITY_DESK("security_desk", -1, 1, 0, 2, Block.box(0, 0, 0, 16, 16, 16)),
    CRATE_WOOD("crate_wood", 0, 0, 0, 1, Block.box(1, 0, 1, 15, 15, 15)),
    CRATE_METAL("crate_metal", 0, 0, 0, 1, Block.box(1, 0, 1, 15, 14, 15)),
    LAB_DOOR("lab_door", 0, 1, 0, 3, Block.box(0, 0, 6, 16, 16, 10)),
    LAB_TABLE("lab_table", 0, 1, 0, 1, Block.box(0, 0, 0, 16, 15, 16)),
    SHELF("shelf", 0, 1, 0, 3, Block.box(0, 0, 1, 16, 16, 15)),
    OFFICE_CHAIR("office_chair", 0, 0, 0, 1, Block.box(3, 0, 3, 13, 15, 13)),
    LOCKER("locker", 0, 0, 0, 2, Block.box(1, 0, 1, 15, 16, 15)),
    TRASH_BIN_TIPPED("trash_bin_tipped", 0, 0, 0, 1, Block.box(1, 0, 1, 15, 8, 15)),
    CHANDELIER("chandelier", 0, 0, 0, 1, Block.box(4, 5, 4, 12, 16, 12)),
    SIREN("siren", 0, 0, 0, 1, Block.box(3, 4, 0, 13, 14, 10)),
    SHELF_FALLEN("shelf_fallen", -1, 1, 0, 1, Block.box(0, 0, 1, 16, 10, 15)),
    WALL_NOTES("wall_notes", 0, 0, 0, 1, Block.box(1, 2, 0, 15, 14, 2)),
    DESK_COMPUTER("desk_computer", 0, 1, 0, 2, Block.box(0, 0, 0, 16, 13, 16)),
    TABLE_FLIPPED("table_flipped", 0, 1, 0, 1, Block.box(0, 0, 0, 16, 16, 16)),
    BARRELS("barrels", 0, 0, 0, 1, Block.box(1, 0, 1, 15, 14, 15)),
    FIRE_EXTINGUISHER("fire_extinguisher", 0, 0, 0, 1, Block.box(4, 2, 0, 12, 13, 9)),
    BOX_PILE("box_pile", 0, 0, 0, 1, Block.box(1, 0, 1, 15, 13, 15)),
    PAPER_PILE("paper_pile", 0, 0, 0, 1, Block.box(0, 0, 0, 16, 1, 16)),
    CHAIR_FALLEN("chair_fallen", 0, 0, 0, 1, Block.box(1, 0, 1, 15, 9, 15)),
    // ---- Elisar city «Странники» (Part 3)
    STREET_LAMP("street_lamp", 0, 0, 0, 3, Block.box(5, 0, 5, 11, 16, 11)),
    SIGN_SHOP("sign_shop", 0, 0, 0, 1, Block.box(6, 6, 0, 10, 16, 16)),
    SIGN_TAVERN("sign_tavern", 0, 0, 0, 1, Block.box(6, 6, 0, 10, 16, 16)),
    SIGN_FORGE("sign_forge", 0, 0, 0, 1, Block.box(6, 6, 0, 10, 16, 16)),
    SIGN_FISH("sign_fish", 0, 0, 0, 1, Block.box(6, 6, 0, 10, 16, 16)),
    SIGN_POLICE("sign_police", 0, 0, 0, 1, Block.box(6, 6, 0, 10, 16, 16)),
    SIGN_HUNTER("sign_hunter", 0, 0, 0, 1, Block.box(6, 6, 0, 10, 16, 16)),
    SIGN_MARKET("sign_market", 0, 0, 0, 1, Block.box(6, 6, 0, 10, 16, 16)),
    SIGN_TEMPLE("sign_temple", 0, 0, 0, 1, Block.box(6, 6, 0, 10, 16, 16)),
    BOUNTY_BOARD("bounty_board", 0, 1, 0, 3, Block.box(0, 0, 6, 16, 16, 10)),
    MARKET_STALL("market_stall", -1, 1, 1, 3, Block.box(0, 0, 0, 16, 15, 16)),
    MARKET_STALL_FISH("market_stall_fish", -1, 1, 1, 3, Block.box(0, 0, 0, 16, 15, 16)),
    WAGON_OVERGROWN("wagon_overgrown", -1, 1, 1, 2, Block.box(0, 0, 0, 16, 14, 16)),
    BENCH("bench", 0, 1, 0, 1, Block.box(0, 0, 2, 16, 8, 14)),
    WELL("well", 0, 1, 1, 3, Block.box(0, 0, 0, 16, 12, 16)),
    FISH_BARREL("fish_barrel", 0, 0, 0, 1, Block.box(2, 0, 2, 14, 13, 14)),
    FISH_RACK("fish_rack", 0, 1, 0, 2, Block.box(0, 0, 6, 16, 16, 10)),
    FISHING_NET("fishing_net", 0, 1, 0, 2, Block.box(0, 0, 13, 16, 16, 16)),
    ROWBOAT("rowboat", -1, 1, 0, 1, Block.box(0, 0, 0, 16, 16, 16)),
    TROPHY_ANTLERS("trophy_antlers", 0, 0, 0, 2, Block.box(3, 0, 12, 13, 16, 16)),
    HIDE_RACK("hide_rack", 0, 0, 0, 2, Block.box(1, 0, 6, 15, 16, 10)),
    WEAPON_RACK("weapon_rack", 0, 0, 0, 2, Block.box(1, 0, 3, 15, 16, 16)),
    FORGE_HEARTH("forge_hearth", -1, 1, 1, 4, Block.box(0, 0, 0, 16, 13, 16)),
    ANVIL_STUMP("anvil_stump", 0, 0, 0, 1, Block.box(2, 0, 2, 14, 14, 14)),
    QUENCH_BARREL("quench_barrel", 0, 0, 0, 1, Block.box(3, 0, 3, 13, 12, 13)),
    TOOL_WALL("tool_wall", 0, 1, 0, 2, Block.box(0, 0, 13, 16, 16, 16)),
    IDOL("idol", 0, 0, 0, 4, Block.box(3, 0, 3, 13, 16, 13)),
    ALTAR_STONE("altar_stone", 0, 1, 0, 1, Block.box(0, 0, 1, 16, 13, 15)),
    FIRE_BOWL("fire_bowl", 0, 0, 0, 2, Block.box(2, 0, 2, 14, 16, 14)),
    SACRED_POLES("sacred_poles", 0, 0, 0, 3, Block.box(4, 0, 4, 12, 16, 12)),
    TABLE_WOOD("table_wood", 0, 1, 0, 1, Block.box(0, 0, 0, 16, 14, 16)),
    CHAIR_WOOD("chair_wood", 0, 0, 0, 1, Block.box(3, 0, 3, 13, 8, 13)),
    STOOL("stool", 0, 0, 0, 1, Block.box(4, 0, 4, 12, 10, 12)),
    BED_WOOD("bed_wood", 0, 0, 1, 1, Block.box(1, 0, 0, 15, 9, 16)),
    WARDROBE("wardrobe", 0, 0, 0, 2, Block.box(1, 0, 1, 15, 16, 15)),
    BAR_COUNTER("bar_counter", -1, 1, 0, 2, Block.box(0, 0, 2, 16, 16, 16)),
    KEG_RACK("keg_rack", 0, 1, 0, 2, Block.box(0, 0, 1, 16, 16, 15)),
    FIREPLACE("fireplace", -1, 1, 0, 3, Block.box(0, 0, 0, 16, 16, 16)),
    SHOP_COUNTER("shop_counter", 0, 1, 0, 2, Block.box(0, 0, 2, 16, 16, 14)),
    SHOP_SHELF("shop_shelf", 0, 1, 0, 2, Block.box(0, 0, 8, 16, 16, 16)),
    RUG("rug", 0, 1, 1, 1, Block.box(0, 0, 0, 16, 1, 16)),
    COOKING_POT("cooking_pot", 0, 0, 0, 2, Block.box(2, 0, 2, 14, 16, 14)),
    WANTED_WALL("wanted_wall", 0, 0, 0, 2, Block.box(1, 0, 14, 15, 16, 16)),
    CRATE_STACK("crate_stack", 0, 0, 0, 1, Block.box(1, 0, 1, 15, 16, 15)),
    LANTERN_HANGING("lantern_hanging", 0, 0, 0, 1, Block.box(4, 4, 4, 12, 16, 12)),
    // ---- Elisar v2 (baked OBJ block models)
    LANTERN_HANG("lantern_hang", 0, 0, 0, 1, Block.box(5, 5, 5, 11, 16, 11)),
    LANTERN_POST("lantern_post", 0, 0, 0, 2, Block.box(5, 0, 5, 11, 16, 11)),
    LANTERN_WALL("lantern_wall", 0, 0, 0, 1, Block.box(5, 5, 4, 11, 16, 16)),
    PAINTING_ISLANDS("painting_islands", 0, 0, 0, 2, Block.box(1, 0, 14, 15, 2, 16)),
    PAINTING_PORTRAIT("painting_portrait", 0, 0, 0, 2, Block.box(1, 0, 14, 15, 2, 16)),
    PAINTING_EYE("painting_eye", 0, 1, 0, 2, Block.box(0, 0, 14, 16, 2, 16)),
    KITCHEN_STOVE("kitchen_stove", 0, 0, 0, 2, Block.box(0, 0, 1, 16, 14, 16)),
    KITCHEN_COUNTER("kitchen_counter", 0, 0, 0, 1, Block.box(0, 0, 2, 16, 15, 16)),
    DISH_CUPBOARD("dish_cupboard", 0, 0, 0, 2, Block.box(0, 0, 8, 16, 16, 16)),
    BOOKSHELF_E("bookshelf_e", 0, 0, 0, 2, Block.box(0, 0, 8, 16, 16, 16)),
    SACKS("sacks", 0, 0, 0, 1, Block.box(1, 0, 1, 15, 14, 15)),
    FIREWOOD_STACK("firewood_stack", 0, 0, 0, 1, Block.box(0, 0, 0, 16, 16, 16)),
    FIRE_PIT("fire_pit", 0, 0, 0, 1, Block.box(1, 0, 1, 15, 5, 15)),
    FLOWER_POT_E("flower_pot_e", 0, 0, 0, 1, Block.box(5, 0, 5, 11, 10, 11)),
    HERBS_HANGING("herbs_hanging", 0, 0, 0, 1, Block.box(1, 9, 6, 15, 16, 10)),
    // ---- Rev 3: castle «Чертог Тлеющего Рога» (baked OBJ)
    CANDLE_STAND("candle_stand", 0, 0, 0, 2, Block.box(4, 0, 4, 12, 16, 12)),
    BOSS_THRONE("boss_throne", -1, 1, 1, 4, Block.box(0, 0, 0, 16, 16, 16)),
    // ---- Rev 3 C-2: castle rooms (baked OBJ)
    CASTLE_CHANDELIER("castle_chandelier", 0, 0, 0, 1, Block.box(2, 4, 2, 14, 16, 14)),
    WALL_SCONCE("wall_sconce", 0, 0, 0, 1, Block.box(5, 3, 10, 11, 14, 16)),
    FEAST_TABLE("feast_table", -1, 1, 0, 1, Block.box(0, 0, 2, 16, 13, 14)),
    LONG_TABLE("long_table", -1, 1, 0, 1, Block.box(0, 0, 2, 16, 13, 14)),
    CASTLE_BENCH("castle_bench", -1, 1, 0, 1, Block.box(0, 0, 4, 16, 8, 12)),
    HIGH_CHAIR("high_chair", 0, 0, 0, 2, Block.box(2, 0, 2, 14, 16, 14)),
    GUARD_WEAPON_RACK("guard_weapon_rack", -1, 1, 0, 3, Block.box(0, 0, 4, 16, 16, 12)),
    GUARD_ARMOR("guard_armor", 0, 0, 0, 2, Block.box(3, 0, 3, 13, 16, 13)),
    CANOPY_BED("canopy_bed", 0, 1, 2, 3, Block.box(0, 0, 0, 16, 10, 16)),
    CASTLE_CHEST("castle_chest", 0, 0, 0, 1, Block.box(1, 0, 2, 15, 14, 14)),
    CHAINED_LECTERN("chained_lectern", 0, 0, 0, 2, Block.box(3, 0, 3, 13, 16, 13)),
    CHAPEL_ALTAR("chapel_altar", -1, 1, 0, 2, Block.box(0, 0, 1, 16, 16, 15)),
    HEARTH_CAULDRON("hearth_cauldron", 0, 0, 0, 2, Block.box(1, 0, 1, 15, 16, 15)),
    ALCHEMY_TABLE("alchemy_table", 0, 1, 0, 2, Block.box(0, 0, 2, 16, 16, 16)),
    WALL_SHACKLES("wall_shackles", 0, 0, 0, 2, Block.box(1, 0, 12, 15, 16, 16)),
    GOLD_HOARD("gold_hoard", 0, 0, 0, 1, Block.box(0, 0, 0, 16, 9, 16)),
    TAPESTRY_HORN("tapestry_horn", 0, 0, 0, 3, Block.box(1, 0, 14, 15, 16, 16)),
    BONE_PILE("bone_pile", 0, 0, 0, 1, Block.box(1, 0, 1, 15, 4, 15));

    public final String glb;
    public final int x0, x1, back, h;
    public final VoxelShape masterShape;

    ModelKind(String glb, int x0, int x1, int back, int h, VoxelShape shape) {
        this.glb = glb;
        this.x0 = x0;
        this.x1 = x1;
        this.back = back;
        this.h = h;
        this.masterShape = shape;
    }

    public boolean passable() {
        return switch (this) {
            case WALL_NOTES, FIRE_EXTINGUISHER, PAPER_PILE, CHANDELIER, SIGN_SHOP, SIGN_TAVERN, SIGN_FORGE, SIGN_FISH,
                 SIGN_POLICE, SIGN_HUNTER, SIGN_MARKET, SIGN_TEMPLE, FISHING_NET, TROPHY_ANTLERS, TOOL_WALL, RUG,
                 WANTED_WALL, SACRED_POLES, LANTERN_HANGING, LANTERN_HANG, LANTERN_WALL, PAINTING_ISLANDS,
                 PAINTING_PORTRAIT, PAINTING_EYE, HERBS_HANGING, CASTLE_CHANDELIER, WALL_SCONCE, WALL_SHACKLES,
                 TAPESTRY_HORN, BONE_PILE -> true;
            default -> false;
        };
    }

    /** Light emitted by Elisar props while POWERED (lit by the city generator); forge coals glow always. */
    public int light(boolean powered) {
        if (this == FORGE_HEARTH) return 12;
        if (!powered) return 0;
        return switch (this) {
            case STREET_LAMP, FIRE_BOWL -> 15;
            case LANTERN_HANGING -> 14;
            case LANTERN_HANG, LANTERN_POST, LANTERN_WALL -> 15;
            case FIRE_PIT -> 14;
            case CANDLE_STAND -> 12;
            case CASTLE_CHANDELIER -> 15;
            case WALL_SCONCE -> 12;
            case HEARTH_CAULDRON -> 13;
            case CHAPEL_ALTAR -> 10;
            case FEAST_TABLE -> 8;
            case ALCHEMY_TABLE -> 7;
            case LONG_TABLE -> 6;
            case KITCHEN_STOVE -> 10;
            case FIREPLACE -> 13;
            case COOKING_POT -> 11;
            case ALTAR_STONE -> 9;
            case TABLE_WOOD -> 6;
            default -> 0;
        };
    }

    public boolean isElisar() {
        return this.ordinal() >= STREET_LAMP.ordinal();
    }

    public boolean isContainer() {
        return this == CRATE_WOOD || this == CRATE_METAL || this == CASTLE_CHEST;
    }

    public int width() {
        return x1 - x0 + 1;
    }

    public int depth() {
        return back + 1;
    }

    /** Model-frame offset (x right, z = -back row) rotated to world for the given facing. */
    public static BlockPos rotate(int x, int y, int z, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(-z, y, x);
            case EAST -> new BlockPos(z, y, -x);
            default -> new BlockPos(x, y, z);
        };
    }

    /** World offsets of all cells except the master (0,0,0). */
    public List<BlockPos> partOffsets(Direction facing) {
        List<BlockPos> out = new ArrayList<>();
        for (int x = x0; x <= x1; ++x)
            for (int b = 0; b <= back; ++b)
                for (int y = 0; y < h; ++y) {
                    if (x == 0 && b == 0 && y == 0) continue;
                    out.add(rotate(x, y, -b, facing));
                }
        return out;
    }
}
