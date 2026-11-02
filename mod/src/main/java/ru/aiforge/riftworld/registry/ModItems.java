package ru.aiforge.riftworld.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.block.ModelBlock;
import ru.aiforge.riftworld.item.ModelBlockItem;
import ru.aiforge.riftworld.item.ResonanceStoneItem;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RiftWorld.MODID);

    /** Biome 01: creative spawn egg of the paper dragon (works on ground, water, AND right-clicking mid-air!). */
    public static final RegistryObject<Item> PAPER_DRAGON_EGG = ITEMS.register("paper_dragon_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.PAPER_DRAGON, 0xF3EFE4, 0xD22E28, new Item.Properties()) {
                @Override
                public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(
                        net.minecraft.world.level.Level level,
                        net.minecraft.world.entity.player.Player player,
                        net.minecraft.world.InteractionHand hand) {
                    var res = super.use(level, player, hand);
                    if (res.getResult().consumesAction()) return res;
                    net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
                    var hit = getPlayerPOVHitResult(level, player, net.minecraft.world.level.ClipContext.Fluid.ANY);
                    if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
                        if (!level.isClientSide && level instanceof net.minecraft.server.level.ServerLevel sl) {
                            net.minecraft.world.phys.Vec3 spawnPos = player.getEyePosition().add(player.getLookAngle().scale(14.0));
                            var ent = ModEntities.PAPER_DRAGON.get().spawn(
                                    sl, stack, player, net.minecraft.core.BlockPos.containing(spawnPos),
                                    net.minecraft.world.entity.MobSpawnType.SPAWN_EGG, false, false);
                            if (ent != null) {
                                ent.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), 0.0F);
                                if (!player.getAbilities().instabuild) stack.shrink(1);
                                player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
                            }
                        }
                        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
                    }
                    return res;
                }
            });

    /** Rev 4 item 13: rope for self-assembling bridges (craft: 3 string in a column). */
    public static final RegistryObject<Item> BRIDGE_ROPE = ITEMS.register("bridge_rope",
            () -> new Item(new Item.Properties().stacksTo(32)));

    public static final RegistryObject<Item> RESONANCE_STONE = ITEMS.register("resonance_stone",
            () -> new ResonanceStoneItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    /** Валюта города: выплавляется из ауритовой руды, что змеится в ярусах Висячих садов (биом 07). */
    public static final RegistryObject<Item> ELISAR_DUCAT = ITEMS.register("elisar_ducat",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    // v0.26.0: Крафтовая особая бумага для приручения Бумажного Дракона (окрашивает его 2 уса!),
    // кастомное Драконье седло Элисара и Горн Дракона (призыв с небес даже вне зоны симуляции)
    public static final RegistryObject<Item> GILDED_PAPER = ITEMS.register("gilded_paper",
            () -> new ru.aiforge.riftworld.item.DragonPaperItem(ru.aiforge.riftworld.item.DragonPaperItem.WHISKER_GILDED,
                    new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> EMERALD_PAPER = ITEMS.register("emerald_paper",
            () -> new ru.aiforge.riftworld.item.DragonPaperItem(ru.aiforge.riftworld.item.DragonPaperItem.WHISKER_EMERALD,
                    new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> DIAMOND_PAPER = ITEMS.register("diamond_paper",
            () -> new ru.aiforge.riftworld.item.DragonPaperItem(ru.aiforge.riftworld.item.DragonPaperItem.WHISKER_DIAMOND,
                    new Item.Properties().stacksTo(64).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> AMETHYST_PAPER = ITEMS.register("amethyst_paper",
            () -> new ru.aiforge.riftworld.item.DragonPaperItem(ru.aiforge.riftworld.item.DragonPaperItem.WHISKER_AMETHYST,
                    new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> CRIMSON_PAPER = ITEMS.register("crimson_paper",
            () -> new ru.aiforge.riftworld.item.DragonPaperItem(ru.aiforge.riftworld.item.DragonPaperItem.WHISKER_CRIMSON,
                    new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> LAPIS_PAPER = ITEMS.register("lapis_paper",
            () -> new ru.aiforge.riftworld.item.DragonPaperItem(ru.aiforge.riftworld.item.DragonPaperItem.WHISKER_LAPIS,
                    new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> DRAGON_SADDLE = ITEMS.register("dragon_saddle",
            () -> new ru.aiforge.riftworld.item.DragonSaddleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> DRAGON_WHISTLE = ITEMS.register("dragon_whistle",
            () -> new ru.aiforge.riftworld.item.DragonWhistleItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> MORGRATH_SCYTHE = ITEMS.register("morgrath_scythe",
            () -> new ru.aiforge.riftworld.item.ScytheItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> GUARD_SICKLE = ITEMS.register("guard_sickle",
            () -> new ru.aiforge.riftworld.item.GuardSickleItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> MORGRATH_EGG = ITEMS.register("morgrath_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.BOSS, 0x2a1d18, 0xd8c8a8, new Item.Properties()));
    public static final RegistryObject<Item> GUARD_EGG = ITEMS.register("ember_guard_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.GUARD, 0x3b2b26, 0xe0602a, new Item.Properties()));

    // v0.19.0: Босс Колоннады, его Прислужники и 4 уникальных кристальных оружия/артефакта
    public static final RegistryObject<Item> CRYSTAL_KATAR = ITEMS.register("crystal_katar",
            () -> new ru.aiforge.riftworld.item.CrystalKatarItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> CRYSTAL_BARRAGE = ITEMS.register("crystal_barrage",
            () -> new ru.aiforge.riftworld.item.CrystalBarrageItem(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> CRYSTAL_AEGIS = ITEMS.register("crystal_aegis",
            () -> new ru.aiforge.riftworld.item.CrystalAegisItem(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> ASTRAL_PRISM = ITEMS.register("astral_prism",
            () -> new ru.aiforge.riftworld.item.AstralPrismItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> RIFT_OBELISK_BOSS_EGG = ITEMS.register("rift_obelisk_boss_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.RIFT_OBELISK_BOSS, 0xDDE4F0, 0x56E8FF, new Item.Properties()));
    public static final RegistryObject<Item> CRYSTAL_MINION_EGG = ITEMS.register("crystal_minion_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.CRYSTAL_MINION, 0xCFD8E8, 0x32C4E8, new Item.Properties()));

    // Рев 8 + v0.26.0: яйца призыва ВСЕХ 11 видов морской живности Элисара!
    public static final RegistryObject<Item> EGG_SILVER = egg("silver_fish_spawn_egg", ru.aiforge.riftworld.entity.SeaFishEntity.SILVER, 0xC6D3DC, 0x2B3440);
    public static final RegistryObject<Item> EGG_GOLD = egg("gold_fish_spawn_egg", ru.aiforge.riftworld.entity.SeaFishEntity.GOLD, 0xE9C25A, 0xC29A38);
    public static final RegistryObject<Item> EGG_STRIPED = egg("striped_fish_spawn_egg", ru.aiforge.riftworld.entity.SeaFishEntity.STRIPED, 0x4DB8C4, 0xF6E58D);
    public static final RegistryObject<Item> EGG_JELLY = egg("jelly_fish_spawn_egg", ru.aiforge.riftworld.entity.SeaFishEntity.JELLY, 0x9FE6FF, 0xD8F7FF);
    public static final RegistryObject<Item> EGG_RAY = egg("ray_fish_spawn_egg", ru.aiforge.riftworld.entity.SeaFishEntity.RAY, 0x68798C, 0xDCE6EC);
    public static final RegistryObject<Item> EGG_ANGLER = egg("angler_fish_spawn_egg", ru.aiforge.riftworld.entity.SeaFishEntity.ANGLER, 0x2C3947, 0xDFFFA8);
    public static final RegistryObject<Item> EGG_EEL = egg("eel_fish_spawn_egg", ru.aiforge.riftworld.entity.SeaFishEntity.EEL, 0x394854, 0xA8D8CE);
    // Рев 10 «Живое море 2»: тропические мирные (хирург, черепаха) и редкие пещерные (глот, жгучая медуза)
    public static final RegistryObject<Item> EGG_SURGEON = egg("surgeon_fish_spawn_egg", ru.aiforge.riftworld.entity.SeaFishEntity.SURGEON, 0x2F6FD0, 0xF2D24C);
    public static final RegistryObject<Item> EGG_TURTLE = egg("turtle_spawn_egg", ru.aiforge.riftworld.entity.SeaFishEntity.TURTLE, 0x4E7A4A, 0xE3DCC0);
    public static final RegistryObject<Item> EGG_GLOOM = egg("gloom_fish_spawn_egg", ru.aiforge.riftworld.entity.SeaFishEntity.GLOOM, 0x2A1F36, 0xC46BFF);
    public static final RegistryObject<Item> EGG_STING = egg("sting_fish_spawn_egg", ru.aiforge.riftworld.entity.SeaFishEntity.STING, 0xB05AD6, 0xFFE9FF);

    private static RegistryObject<Item> egg(String id, int variant, int back, int spots) {
        return ITEMS.register(id, () -> new ru.aiforge.riftworld.item.SeaEggItem(variant, new Item.Properties()));
    }

    static {
        for (RegistryObject<? extends net.minecraft.world.level.block.Block> b : ModBlocks.WITH_ITEM) {
            ITEMS.register(b.getId().getPath(), () -> (b.get() instanceof ModelBlock mb && !(mb instanceof ru.aiforge.riftworld.block.MergingRugBlock))
                    ? new ModelBlockItem(mb, new Item.Properties())
                    : new BlockItem(b.get(), new Item.Properties()));
        }
    }

    private ModItems() {
    }
}
