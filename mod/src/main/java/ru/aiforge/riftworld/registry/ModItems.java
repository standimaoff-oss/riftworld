package ru.aiforge.riftworld.registry;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
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

    // v0.27.0: Яйца призыва двух мирных существ обычного биома (летающая рыбка-стрекоза и белоснежный цветущий олень)
    public static final RegistryObject<Item> SKY_DART_EGG = ITEMS.register("sky_dart_fish_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.SKY_DART_FISH, 0xF5F1E8, 0x7EC5BB, new Item.Properties()) {
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
                            net.minecraft.world.phys.Vec3 spawnPos = player.getEyePosition().add(player.getLookAngle().scale(3.5));
                            var ent = ModEntities.SKY_DART_FISH.get().spawn(
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
    // v0.32.0: Лиоплевродон (море) — яйцо призыва и дроп с хищника
    public static final RegistryObject<Item> LIOPLEURODON_EGG = ITEMS.register("liopleurodon_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.LIOPLEURODON, 0x26323C, 0xC4C5BE,
                    new Item.Properties()));
    public static final RegistryObject<Item> EL_GRAN_MAJA_EGG = ITEMS.register("el_gran_maja_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.EL_GRAN_MAJA, 0x263632, 0xA59D8B,
                    new Item.Properties()));
    public static final RegistryObject<Item> LIOPLEURODON_TOOTH = ITEMS.register("liopleurodon_tooth",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> SEA_SCALE = ITEMS.register("sea_scale",
            () -> new Item(new Item.Properties()));

    /** v0.39.0: Морок — своё яйцо призыва (редкость UNCOMMON, как у хозяев чащи). */
    public static final RegistryObject<Item> MOROK_EGG = ITEMS.register("morok_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.MOROK, 0x15181C, 0xFF7A18,
                    new Item.Properties()));
    /** Трофей: раскалённый уголёк, который Морок носит вместо глаза. */
    public static final RegistryObject<Item> MOROK_EYE = ITEMS.register("morok_ember",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE).fireResistant()));

    public static final RegistryObject<Item> BLOSSOM_STAG_EGG = ITEMS.register("blossom_stag_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.BLOSSOM_STAG, 0xFAF8F5, 0xF4B8C4, new Item.Properties()));

    private static RegistryObject<Item> egg(String id, int variant, int back, int spots) {
        return ITEMS.register(id, () -> new ru.aiforge.riftworld.item.SeaEggItem(variant, new Item.Properties()));
    }

    // ==========================================================================================
    // v0.40.0 «Снаряжение и люди Элисара»: два металла, два комплекта брони и два комплекта
    // инструментов/оружия. Аурит — руда, из которой уже делают монеты; рифт-кристалл — редкая
    // руда всего из 2–3 биомов. Все крафты — ванильные раскладки с подменой материала.
    // ==========================================================================================

    /** Слиток аурита: выплавляется из ауритовой руды, идёт и в монеты, и в «среднюю» броню. */
    public static final RegistryObject<Item> AURITE_INGOT = ITEMS.register("aurite_ingot",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** Рифт-кристалл: редкий самоцвет из Хрустальных разломов, глубинного камня и Фиолетовых холмов. */
    public static final RegistryObject<Item> RIFTCRYSTAL = ITEMS.register("riftcrystal",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));

    // --- Броня: аурит (средняя) и рифт-кристалл (сильная) ---
    public static final RegistryObject<Item> AURITE_HELMET = armour("aurite_helmet",
            ru.aiforge.riftworld.item.RiftArmorMaterials.AURITE, ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> AURITE_CHESTPLATE = armour("aurite_chestplate",
            ru.aiforge.riftworld.item.RiftArmorMaterials.AURITE, ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> AURITE_LEGGINGS = armour("aurite_leggings",
            ru.aiforge.riftworld.item.RiftArmorMaterials.AURITE, ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> AURITE_BOOTS = armour("aurite_boots",
            ru.aiforge.riftworld.item.RiftArmorMaterials.AURITE, ArmorItem.Type.BOOTS);

    public static final RegistryObject<Item> RIFTCRYSTAL_HELMET = armour("riftcrystal_helmet",
            ru.aiforge.riftworld.item.RiftArmorMaterials.RIFTCRYSTAL, ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> RIFTCRYSTAL_CHESTPLATE = armour("riftcrystal_chestplate",
            ru.aiforge.riftworld.item.RiftArmorMaterials.RIFTCRYSTAL, ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> RIFTCRYSTAL_LEGGINGS = armour("riftcrystal_leggings",
            ru.aiforge.riftworld.item.RiftArmorMaterials.RIFTCRYSTAL, ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> RIFTCRYSTAL_BOOTS = armour("riftcrystal_boots",
            ru.aiforge.riftworld.item.RiftArmorMaterials.RIFTCRYSTAL, ArmorItem.Type.BOOTS);

    // --- Инструменты: кирка, топор, лопата, мотыга (2 полных набора: аурит и рифт-кристалл) ---
    public static final RegistryObject<Item> AURITE_PICKAXE = ITEMS.register("aurite_pickaxe",
            () -> new PickaxeItem(ru.aiforge.riftworld.item.RiftGearTiers.AURITE, 1, -2.8F, gearAurite()));
    public static final RegistryObject<Item> AURITE_AXE = ITEMS.register("aurite_axe",
            () -> new AxeItem(ru.aiforge.riftworld.item.RiftGearTiers.AURITE, 6.0F, -3.0F, gearAurite()));
    public static final RegistryObject<Item> AURITE_SHOVEL = ITEMS.register("aurite_shovel",
            () -> new ShovelItem(ru.aiforge.riftworld.item.RiftGearTiers.AURITE, 2.0F, -3.0F, gearAurite()));
    public static final RegistryObject<Item> AURITE_HOE = ITEMS.register("aurite_hoe",
            () -> new HoeItem(ru.aiforge.riftworld.item.RiftGearTiers.AURITE, -2, -0.5F, gearAurite()));

    public static final RegistryObject<Item> RIFTCRYSTAL_PICKAXE = ITEMS.register("riftcrystal_pickaxe",
            () -> new PickaxeItem(ru.aiforge.riftworld.item.RiftGearTiers.RIFTCRYSTAL, 2, -2.7F, gearRiftcrystal()));
    public static final RegistryObject<Item> RIFTCRYSTAL_AXE = ITEMS.register("riftcrystal_axe",
            () -> new AxeItem(ru.aiforge.riftworld.item.RiftGearTiers.RIFTCRYSTAL, 7.5F, -2.9F, gearRiftcrystal()));
    public static final RegistryObject<Item> RIFTCRYSTAL_SHOVEL = ITEMS.register("riftcrystal_shovel",
            () -> new ShovelItem(ru.aiforge.riftworld.item.RiftGearTiers.RIFTCRYSTAL, 3.0F, -2.9F, gearRiftcrystal()));
    public static final RegistryObject<Item> RIFTCRYSTAL_HOE = ITEMS.register("riftcrystal_hoe",
            () -> new HoeItem(ru.aiforge.riftworld.item.RiftGearTiers.RIFTCRYSTAL, -1, -0.5F, gearRiftcrystal()));

    // --- Оружие и вооружение Элисара (референс 03: мечи, изогнутый рифт-лук, щит + вооружение Стража 06) ---
    public static final RegistryObject<Item> AURITE_SWORD = ITEMS.register("aurite_sword",
            () -> new SwordItem(ru.aiforge.riftworld.item.RiftGearTiers.AURITE, 4, -2.3F, gearAurite()));
    public static final RegistryObject<Item> RIFTCRYSTAL_SWORD = ITEMS.register("riftcrystal_sword",
            () -> new SwordItem(ru.aiforge.riftworld.item.RiftGearTiers.RIFTCRYSTAL, 5, -2.2F, gearRiftcrystal()));
    public static final RegistryObject<Item> ELISAR_BOW = ITEMS.register("elisar_bow",
            () -> new ru.aiforge.riftworld.item.ElisarBowItem(new Item.Properties().durability(960).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> ELISAR_SHIELD = ITEMS.register("elisar_shield",
            () -> new ru.aiforge.riftworld.item.ElisarShieldItem(new Item.Properties().durability(840).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> ELISAR_GUARD_SWORD = ITEMS.register("elisar_guard_sword",
            () -> new SwordItem(net.minecraft.world.item.Tiers.IRON, 4, -2.4F, new Item.Properties()));
    public static final RegistryObject<Item> ELISAR_GUARD_SHIELD = ITEMS.register("elisar_guard_shield",
            () -> new ru.aiforge.riftworld.item.ElisarShieldItem(new Item.Properties().durability(560)));

    // --- Яйца призыва жителей Элисара (референсы 05–07) ---
    public static final RegistryObject<Item> ELISAR_VILLAGER_EGG = ITEMS.register("elisar_villager_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.ELISAR_VILLAGER, 0x8F7B84, 0xAC8A9D, new Item.Properties()));
    public static final RegistryObject<Item> ELISAR_GUARD_EGG = ITEMS.register("elisar_guard_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.ELISAR_GUARD, 0xC27E3F, 0xEDECF3, new Item.Properties()));
    public static final RegistryObject<Item> ELISAR_ARCHER_EGG = ITEMS.register("elisar_archer_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.ELISAR_ARCHER, 0x6A493A, 0xD09873, new Item.Properties()));

    // ==========================================================================================
    // v0.47.0 «Фауна биомов Элисара» — 15 яиц призыва + ведро с морским коньком + 6 трофеев/даров
    // ==========================================================================================

    public static final RegistryObject<Item> PORCELAIN_CRANE_EGG = ITEMS.register("porcelain_crane_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.PORCELAIN_CRANE, 0xF7F4EC, 0xD4AF5A, new Item.Properties()));
    public static final RegistryObject<Item> BASALT_BEETLE_EGG = ITEMS.register("basalt_beetle_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.BASALT_BEETLE, 0x25272C, 0xFF5A1F, new Item.Properties()));
    public static final RegistryObject<Item> ASH_MOTH_EGG = airOrGroundEgg("ash_moth_spawn_egg", ModEntities.ASH_MOTH, 0x3E3B39, 0xFF7B29);
    public static final RegistryObject<Item> AMETHYST_FOX_EGG = ITEMS.register("amethyst_fox_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.AMETHYST_FOX, 0x6E4CA8, 0xD5A8FF, new Item.Properties()));
    public static final RegistryObject<Item> SIZO_LEAFWING_EGG = ITEMS.register("sizo_leafwing_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.SIZO_LEAFWING, 0x5D8E7F, 0xD9EAE2, new Item.Properties()));
    public static final RegistryObject<Item> AZURE_FROG_EGG = ITEMS.register("azure_frog_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.AZURE_FROG, 0x1D8FD8, 0x78F2D0, new Item.Properties()));
    public static final RegistryObject<Item> BLOSSOM_SNAIL_EGG = ITEMS.register("blossom_snail_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.BLOSSOM_SNAIL, 0xE8B4C8, 0xFAF4EA, new Item.Properties()));
    public static final RegistryObject<Item> PHANTOM_WOLF_EGG = ITEMS.register("phantom_wolf_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.PHANTOM_WOLF, 0x26323F, 0x8FE8F5, new Item.Properties()));
    public static final RegistryObject<Item> CRYSTAL_GECKO_EGG = ITEMS.register("crystal_gecko_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.CRYSTAL_GECKO, 0x7BE3F2, 0xE8FBFF, new Item.Properties()));
    public static final RegistryObject<Item> FACETED_OWL_EGG = airOrGroundEgg("faceted_owl_spawn_egg", ModEntities.FACETED_OWL, 0xDCE6F2, 0x52D8F6);
    public static final RegistryObject<Item> BONE_RAVEN_EGG = airOrGroundEgg("bone_raven_spawn_egg", ModEntities.BONE_RAVEN, 0x1E2024, 0xDCD4C4);
    public static final RegistryObject<Item> CORAL_HERMIT_CRAB_EGG = ITEMS.register("coral_hermit_crab_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.CORAL_HERMIT_CRAB, 0xD95B43, 0xF2D3AC, new Item.Properties()));
    public static final RegistryObject<Item> REEF_SEAHORSE_EGG = airOrGroundEgg("reef_seahorse_spawn_egg", ModEntities.REEF_SEAHORSE, 0xF28C38, 0xFCE28B);
    public static final RegistryObject<Item> GHOST_SQUID_EGG = airOrGroundEgg("ghost_squid_spawn_egg", ModEntities.GHOST_SQUID, 0x1B2845, 0x6CF6FF);
    public static final RegistryObject<Item> SILVER_SEA_SERPENT_EGG = airOrGroundEgg("silver_sea_serpent_spawn_egg", ModEntities.SILVER_SEA_SERPENT, 0xB8CDD9, 0x36D6E8);

    public static final RegistryObject<Item> REEF_SEAHORSE_BUCKET = ITEMS.register("reef_seahorse_bucket",
            () -> new net.minecraft.world.item.MobBucketItem(
                    ModEntities.REEF_SEAHORSE,
                    ModFluids.SOURCE,
                    () -> net.minecraft.sounds.SoundEvents.BUCKET_EMPTY_FISH,
                    new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> PORCELAIN_FEATHER = ITEMS.register("porcelain_feather",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> BASALT_SCUTE = ITEMS.register("basalt_scute",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON).fireResistant()));
    public static final RegistryObject<Item> ASH_WING_DUST = ITEMS.register("ash_wing_dust",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PHANTOM_FANG = ITEMS.register("phantom_fang",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> BONE_PINION = ITEMS.register("bone_pinion",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ABYSSAL_INK_SAC = ITEMS.register("abyssal_ink_sac",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    private static <E extends net.minecraft.world.entity.Mob> RegistryObject<Item> airOrGroundEgg(
            String id, java.util.function.Supplier<? extends net.minecraft.world.entity.EntityType<E>> typeSup,
            int bg, int fg) {
        return ITEMS.register(id, () -> new net.minecraftforge.common.ForgeSpawnEggItem(typeSup, bg, fg, new Item.Properties()) {
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
                        net.minecraft.world.phys.Vec3 spawnPos = player.getEyePosition().add(player.getLookAngle().scale(4.0));
                        var ent = typeSup.get().spawn(
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
    }

    private static Item.Properties gearAurite() {
        return new Item.Properties().rarity(Rarity.UNCOMMON);
    }

    private static Item.Properties gearRiftcrystal() {
        return new Item.Properties().rarity(Rarity.RARE).fireResistant();
    }

    private static RegistryObject<Item> armour(String id, ArmorMaterial material, ArmorItem.Type slot) {
        boolean rc = material == ru.aiforge.riftworld.item.RiftArmorMaterials.RIFTCRYSTAL;
        return ITEMS.register(id, () -> new ru.aiforge.riftworld.item.ElisarArmorItem(
                material, slot, rc ? gearRiftcrystal() : gearAurite()));
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
