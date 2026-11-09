package ru.aiforge.riftworld.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.aiforge.riftworld.RiftWorld;
import ru.aiforge.riftworld.entity.EmberGuard;
import ru.aiforge.riftworld.entity.MorgrathBoss;
import ru.aiforge.riftworld.entity.ScytheItemEntity;

@Mod.EventBusSubscriber(modid = RiftWorld.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RiftWorld.MODID);

    public static final RegistryObject<EntityType<MorgrathBoss>> BOSS = TYPES.register("morgrath", () -> EntityType.Builder
            .<MorgrathBoss>of(MorgrathBoss::new, MobCategory.MONSTER).sized(1.4F, 3.0F).fireImmune().clientTrackingRange(64)
            .build("riftworld:morgrath"));
    public static final RegistryObject<EntityType<EmberGuard>> GUARD = TYPES.register("ember_guard", () -> EntityType.Builder
            .<EmberGuard>of(EmberGuard::new, MobCategory.MONSTER).sized(0.8F, 2.0F).fireImmune().clientTrackingRange(64)
            .build("riftworld:ember_guard"));
    public static final RegistryObject<EntityType<ScytheItemEntity>> SCYTHE_ITEM = TYPES.register("scythe_item", () -> EntityType.Builder
            .<ScytheItemEntity>of(ScytheItemEntity::new, MobCategory.MISC).sized(0.5F, 0.3F).fireImmune().clientTrackingRange(64)
            .updateInterval(20).build("riftworld:scythe_item"));

    // v0.40.0: народ Элисара (референсы 05–07) — житель, страж и лучник.
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.ElisarVillagerEntity>> ELISAR_VILLAGER = TYPES.register("elisar_villager",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.ElisarVillagerEntity>of(ru.aiforge.riftworld.entity.ElisarVillagerEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).clientTrackingRange(64).build("riftworld:elisar_villager"));
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.ElisarGuardEntity>> ELISAR_GUARD = TYPES.register("elisar_guard",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.ElisarGuardEntity>of(ru.aiforge.riftworld.entity.ElisarGuardEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).clientTrackingRange(64).build("riftworld:elisar_guard"));
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.ElisarArcherEntity>> ELISAR_ARCHER = TYPES.register("elisar_archer",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.ElisarArcherEntity>of(ru.aiforge.riftworld.entity.ElisarArcherEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).clientTrackingRange(64).build("riftworld:elisar_archer"));
    /** v0.24.0: Огромный плавный модульный 3D оригами-дракон (~47.7 м), парящий вдоль диких островов, под сводами и над морем (вне Города!). */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.PaperDragonEntity>> PAPER_DRAGON = TYPES.register("paper_dragon",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.PaperDragonEntity>of(ru.aiforge.riftworld.entity.PaperDragonEntity::new, MobCategory.AMBIENT)
                    .sized(3.8F, 2.8F).updateInterval(2).clientTrackingRange(128).build("riftworld:paper_dragon"));

    /** v0.22.0: Безблочный белый 3D-геометрический Пролом в Пространстве (от узкого острия у земли до широкого разлома в небе). */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.SpatialRiftEntity>> SPATIAL_RIFT = TYPES.register("spatial_rift",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.SpatialRiftEntity>of(ru.aiforge.riftworld.entity.SpatialRiftEntity::new, MobCategory.MISC)
                    .sized(2.2F, 4.2F).fireImmune().updateInterval(2).clientTrackingRange(64).build("riftworld:spatial_rift"));

    /** v0.23.0: Кастомная атака Босса — вытянутый светлый кристальный столб из-под земли (поднимает стоящего игрока!). */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.IcePillarEntity>> ICE_PILLAR = TYPES.register("ice_pillar",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.IcePillarEntity>of(ru.aiforge.riftworld.entity.IcePillarEntity::new, MobCategory.MISC)
                    .sized(1.36F, 7.6F).fireImmune().updateInterval(1).clientTrackingRange(64).build("riftworld:ice_pillar"));

    /** v0.23.0: Кастомная атака Босса — вытянутая светлая кристальная глыба-копьё в небе + метка приземления. */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.IceHailBoulderEntity>> ICE_HAIL_BOULDER = TYPES.register("ice_hail_boulder",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.IceHailBoulderEntity>of(ru.aiforge.riftworld.entity.IceHailBoulderEntity::new, MobCategory.MISC)
                    .sized(1.4F, 4.6F).fireImmune().updateInterval(1).clientTrackingRange(64).build("riftworld:ice_hail_boulder"));

    /** Рев 8 «Живое море»: вся ихтиофауна Элисара одной сущностью (вариант — в данных). */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.SeaFishEntity>> SEA_FISH = TYPES.register("sea_fish",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.SeaFishEntity>of(ru.aiforge.riftworld.entity.SeaFishEntity::new, MobCategory.WATER_CREATURE)
                    .sized(0.8F, 0.6F).updateInterval(2).clientTrackingRange(64).build("riftworld:sea_fish"));

    /** v0.23.0: Ещё более крупный Босс Колоннады «Расколотый Обелиск Разлома» (2.45x, стационарный левитирующий обелиск без тени человека). */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.RiftObeliskBoss>> RIFT_OBELISK_BOSS = TYPES.register("rift_obelisk_boss",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.RiftObeliskBoss>of(ru.aiforge.riftworld.entity.RiftObeliskBoss::new, MobCategory.MONSTER)
                    .sized(4.3F, 9.4F).fireImmune().clientTrackingRange(64).build("riftworld:rift_obelisk_boss"));

    /** v0.19.0: Призываемый парящий «Гранёный Прислужник» Колоннады. */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.CrystalMinion>> CRYSTAL_MINION = TYPES.register("crystal_minion",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.CrystalMinion>of(ru.aiforge.riftworld.entity.CrystalMinion::new, MobCategory.MONSTER)
                    .sized(0.85F, 2.1F).fireImmune().clientTrackingRange(64).build("riftworld:crystal_minion"));

    /** v0.19.0: Стремительный кристальный осколок (снаряд Веера Осколков и Босса Колоннады). */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.CrystalShardEntity>> CRYSTAL_SHARD = TYPES.register("crystal_shard",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.CrystalShardEntity>of(ru.aiforge.riftworld.entity.CrystalShardEntity::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F).updateInterval(2).clientTrackingRange(64).build("riftworld:crystal_shard"));

    /** v0.19.0: 3D кристальный щит «Бастиона Граней» (режим тарана ЛКМ / защиты 6с ПКМ). */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.CrystalShieldEntity>> CRYSTAL_SHIELD = TYPES.register("crystal_shield",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.CrystalShieldEntity>of(ru.aiforge.riftworld.entity.CrystalShieldEntity::new, MobCategory.MISC)
                    .sized(2.1F, 2.1F).updateInterval(1).clientTrackingRange(64).build("riftworld:crystal_shield"));

    /** v0.27.0: Дружелюбная летающая рыбка-стрекоза (фарфоровое тело + селадоновые плавники + 4 крылышка стрекозы) для обычных биомов. */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.SkyDartFishEntity>> SKY_DART_FISH = TYPES.register("sky_dart_fish",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.SkyDartFishEntity>of(ru.aiforge.riftworld.entity.SkyDartFishEntity::new, MobCategory.CREATURE)
                    .sized(0.55F, 0.45F).updateInterval(2).clientTrackingRange(64).build("riftworld:sky_dart_fish"));

    /** v0.27.0: Белоснежный грациозный олень с живыми цветами на ветвистых рогах для обычных мирных биомов. */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.BlossomStagEntity>> BLOSSOM_STAG = TYPES.register("blossom_stag",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.BlossomStagEntity>of(ru.aiforge.riftworld.entity.BlossomStagEntity::new, MobCategory.CREATURE)
                    .sized(0.95F, 1.95F).clientTrackingRange(64).build("riftworld:blossom_stag"));

    /** v0.32.0: Лиоплевродон — 12-блочный засадник глубин Элисара (концепт `reports/mob_refs/03_ref_liopleurodon.png`). */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.LiopleurodonEntity>> LIOPLEURODON =
            TYPES.register("liopleurodon", () -> EntityType.Builder
                    .<ru.aiforge.riftworld.entity.LiopleurodonEntity>of(ru.aiforge.riftworld.entity.LiopleurodonEntity::new,
                            MobCategory.WATER_CREATURE)
                    .sized(4.4F, 2.6F).updateInterval(2).clientTrackingRange(64)
                    .build("riftworld:liopleurodon"));

    /** El Gran Maja: small anchor; full animated terrain collision is checked separately. */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.ElGranMajaEntity>> EL_GRAN_MAJA =
            TYPES.register("el_gran_maja", () -> EntityType.Builder
                    .<ru.aiforge.riftworld.entity.ElGranMajaEntity>of(ru.aiforge.riftworld.entity.ElGranMajaEntity::new,
                            MobCategory.WATER_CREATURE)
                    .sized(1.0F, 1.0F).updateInterval(2).clientTrackingRange(64)
                    .build("riftworld:el_gran_maja"));





    /** v0.39.0: Морок — собственный хоррор-моб «Мороковой Пади» (быстрый, кружит, моргает). */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.MorokEntity>> MOROK =
            TYPES.register("morok", () -> EntityType.Builder
                    .<ru.aiforge.riftworld.entity.MorokEntity>of(ru.aiforge.riftworld.entity.MorokEntity::new,
                            MobCategory.MONSTER)
                    .sized(0.85F, 2.45F).clientTrackingRange(64)
                    .build("riftworld:morok"));

    // ==========================================================================================
    // v0.47.0 «Фауна биомов Элисара» — 15 существ Hytale/Blockbench с многокостными анимациями:
    // ==========================================================================================

    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.fauna.PorcelainCraneEntity>> PORCELAIN_CRANE =
            TYPES.register("porcelain_crane", () -> EntityType.Builder
                    .<ru.aiforge.riftworld.entity.fauna.PorcelainCraneEntity>of(
                            ru.aiforge.riftworld.entity.fauna.PorcelainCraneEntity::new, MobCategory.CREATURE)
                    .sized(0.65F, 1.80F).clientTrackingRange(64).build("riftworld:porcelain_crane"));

    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.fauna.BasaltBeetleEntity>> BASALT_BEETLE =
            TYPES.register("basalt_beetle", () -> EntityType.Builder
                    .<ru.aiforge.riftworld.entity.fauna.BasaltBeetleEntity>of(
                            ru.aiforge.riftworld.entity.fauna.BasaltBeetleEntity::new, MobCategory.CREATURE)
                    .sized(1.25F, 0.98F).fireImmune().clientTrackingRange(64).build("riftworld:basalt_beetle"));

    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.fauna.AshMothEntity>> ASH_MOTH =
            TYPES.register("ash_moth", () -> EntityType.Builder
                    .<ru.aiforge.riftworld.entity.fauna.AshMothEntity>of(
                            ru.aiforge.riftworld.entity.fauna.AshMothEntity::new, MobCategory.MONSTER)
                    .sized(0.85F, 0.75F).fireImmune().clientTrackingRange(64).build("riftworld:ash_moth"));

    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.fauna.AmethystFoxEntity>> AMETHYST_FOX =
            TYPES.register("amethyst_fox", () -> EntityType.Builder
                    .<ru.aiforge.riftworld.entity.fauna.AmethystFoxEntity>of(
                            ru.aiforge.riftworld.entity.fauna.AmethystFoxEntity::new, MobCategory.CREATURE)
                    .sized(0.65F, 0.80F).clientTrackingRange(64).build("riftworld:amethyst_fox"));

    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.fauna.SizoLeafwingEntity>> SIZO_LEAFWING =
            TYPES.register("sizo_leafwing", () -> EntityType.Builder
                    .<ru.aiforge.riftworld.entity.fauna.SizoLeafwingEntity>of(
                            ru.aiforge.riftworld.entity.fauna.SizoLeafwingEntity::new, MobCategory.CREATURE)
                    .sized(0.85F, 1.25F).clientTrackingRange(64).build("riftworld:sizo_leafwing"));

    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.fauna.AzureFrogEntity>> AZURE_FROG =
            TYPES.register("azure_frog", () -> EntityType.Builder
                    .<ru.aiforge.riftworld.entity.fauna.AzureFrogEntity>of(
                            ru.aiforge.riftworld.entity.fauna.AzureFrogEntity::new, MobCategory.CREATURE)
                    .sized(0.55F, 0.50F).clientTrackingRange(64).build("riftworld:azure_frog"));

    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.fauna.BlossomSnailEntity>> BLOSSOM_SNAIL =
            TYPES.register("blossom_snail", () -> EntityType.Builder
                    .<ru.aiforge.riftworld.entity.fauna.BlossomSnailEntity>of(
                            ru.aiforge.riftworld.entity.fauna.BlossomSnailEntity::new, MobCategory.CREATURE)
                    .sized(0.75F, 0.80F).clientTrackingRange(64).build("riftworld:blossom_snail"));

    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.fauna.PhantomWolfEntity>> PHANTOM_WOLF =
            TYPES.register("phantom_wolf", () -> EntityType.Builder
                    .<ru.aiforge.riftworld.entity.fauna.PhantomWolfEntity>of(
                            ru.aiforge.riftworld.entity.fauna.PhantomWolfEntity::new, MobCategory.MONSTER)
                    .sized(0.85F, 1.10F).clientTrackingRange(64).build("riftworld:phantom_wolf"));

    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.fauna.CrystalGeckoEntity>> CRYSTAL_GECKO =
            TYPES.register("crystal_gecko", () -> EntityType.Builder
                    .<ru.aiforge.riftworld.entity.fauna.CrystalGeckoEntity>of(
                            ru.aiforge.riftworld.entity.fauna.CrystalGeckoEntity::new, MobCategory.CREATURE)
                    .sized(0.65F, 0.50F).clientTrackingRange(64).build("riftworld:crystal_gecko"));

    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.fauna.FacetedOwlEntity>> FACETED_OWL =
            TYPES.register("faceted_owl", () -> EntityType.Builder
                    .<ru.aiforge.riftworld.entity.fauna.FacetedOwlEntity>of(
                            ru.aiforge.riftworld.entity.fauna.FacetedOwlEntity::new, MobCategory.CREATURE)
                    .sized(0.60F, 0.90F).clientTrackingRange(64).build("riftworld:faceted_owl"));


    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.fauna.CoralHermitCrabEntity>> CORAL_HERMIT_CRAB =
            TYPES.register("coral_hermit_crab", () -> EntityType.Builder
                    .<ru.aiforge.riftworld.entity.fauna.CoralHermitCrabEntity>of(
                            ru.aiforge.riftworld.entity.fauna.CoralHermitCrabEntity::new, MobCategory.WATER_CREATURE)
                    .sized(0.80F, 0.80F).clientTrackingRange(64).build("riftworld:coral_hermit_crab"));

    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.fauna.ReefSeahorseEntity>> REEF_SEAHORSE =
            TYPES.register("reef_seahorse", () -> EntityType.Builder
                    .<ru.aiforge.riftworld.entity.fauna.ReefSeahorseEntity>of(
                            ru.aiforge.riftworld.entity.fauna.ReefSeahorseEntity::new, MobCategory.WATER_CREATURE)
                    .sized(0.45F, 0.85F).clientTrackingRange(64).build("riftworld:reef_seahorse"));

    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.fauna.GhostSquidEntity>> GHOST_SQUID =
            TYPES.register("ghost_squid", () -> EntityType.Builder
                    .<ru.aiforge.riftworld.entity.fauna.GhostSquidEntity>of(
                            ru.aiforge.riftworld.entity.fauna.GhostSquidEntity::new, MobCategory.WATER_CREATURE)
                    .sized(1.30F, 1.75F).clientTrackingRange(64).build("riftworld:ghost_squid"));

    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.fauna.SilverSeaSerpentEntity>> SILVER_SEA_SERPENT =
            TYPES.register("silver_sea_serpent", () -> EntityType.Builder
                    .<ru.aiforge.riftworld.entity.fauna.SilverSeaSerpentEntity>of(
                            ru.aiforge.riftworld.entity.fauna.SilverSeaSerpentEntity::new, MobCategory.WATER_CREATURE)
                    .sized(1.25F, 1.85F).updateInterval(2).clientTrackingRange(64).build("riftworld:silver_sea_serpent"));

    private ModEntities() {
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent e) {
        e.put(BOSS.get(), MorgrathBoss.attributes().build());
        e.put(GUARD.get(), EmberGuard.attributes().build());
        e.put(PAPER_DRAGON.get(), ru.aiforge.riftworld.entity.PaperDragonEntity.attributes().build());
        e.put(SEA_FISH.get(), ru.aiforge.riftworld.entity.SeaFishEntity.attributes().build());
        e.put(RIFT_OBELISK_BOSS.get(), ru.aiforge.riftworld.entity.RiftObeliskBoss.attributes().build());
        e.put(CRYSTAL_MINION.get(), ru.aiforge.riftworld.entity.CrystalMinion.attributes().build());
        e.put(SKY_DART_FISH.get(), ru.aiforge.riftworld.entity.SkyDartFishEntity.attributes().build());
        e.put(BLOSSOM_STAG.get(), ru.aiforge.riftworld.entity.BlossomStagEntity.attributes().build());
        e.put(LIOPLEURODON.get(), ru.aiforge.riftworld.entity.LiopleurodonEntity.attributes().build());
        e.put(EL_GRAN_MAJA.get(), ru.aiforge.riftworld.entity.ElGranMajaEntity.attributes().build());
        e.put(ELISAR_VILLAGER.get(), ru.aiforge.riftworld.entity.ElisarVillagerEntity.attributes().build());
        e.put(ELISAR_GUARD.get(), ru.aiforge.riftworld.entity.ElisarGuardEntity.attributes().build());
        e.put(ELISAR_ARCHER.get(), ru.aiforge.riftworld.entity.ElisarArcherEntity.attributes().build());
        e.put(MOROK.get(), ru.aiforge.riftworld.entity.MorokEntity.attributes().build());

        e.put(PORCELAIN_CRANE.get(), ru.aiforge.riftworld.entity.fauna.PorcelainCraneEntity.attributes().build());
        e.put(BASALT_BEETLE.get(), ru.aiforge.riftworld.entity.fauna.BasaltBeetleEntity.attributes().build());
        e.put(ASH_MOTH.get(), ru.aiforge.riftworld.entity.fauna.AshMothEntity.attributes().build());
        e.put(AMETHYST_FOX.get(), ru.aiforge.riftworld.entity.fauna.AmethystFoxEntity.attributes().build());
        e.put(SIZO_LEAFWING.get(), ru.aiforge.riftworld.entity.fauna.SizoLeafwingEntity.attributes().build());
        e.put(AZURE_FROG.get(), ru.aiforge.riftworld.entity.fauna.AzureFrogEntity.attributes().build());
        e.put(BLOSSOM_SNAIL.get(), ru.aiforge.riftworld.entity.fauna.BlossomSnailEntity.attributes().build());
        e.put(PHANTOM_WOLF.get(), ru.aiforge.riftworld.entity.fauna.PhantomWolfEntity.attributes().build());
        e.put(CRYSTAL_GECKO.get(), ru.aiforge.riftworld.entity.fauna.CrystalGeckoEntity.attributes().build());
        e.put(FACETED_OWL.get(), ru.aiforge.riftworld.entity.fauna.FacetedOwlEntity.attributes().build());
        e.put(CORAL_HERMIT_CRAB.get(), ru.aiforge.riftworld.entity.fauna.CoralHermitCrabEntity.attributes().build());
        e.put(REEF_SEAHORSE.get(), ru.aiforge.riftworld.entity.fauna.ReefSeahorseEntity.attributes().build());
        e.put(GHOST_SQUID.get(), ru.aiforge.riftworld.entity.fauna.GhostSquidEntity.attributes().build());
        e.put(SILVER_SEA_SERPENT.get(), ru.aiforge.riftworld.entity.fauna.SilverSeaSerpentEntity.attributes().build());
    }
}
