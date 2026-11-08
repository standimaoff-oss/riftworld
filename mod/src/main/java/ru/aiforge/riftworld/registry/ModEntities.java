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
            .<MorgrathBoss>of(MorgrathBoss::new, MobCategory.MONSTER).sized(1.4F, 3.0F).fireImmune().clientTrackingRange(10)
            .build("riftworld:morgrath"));
    public static final RegistryObject<EntityType<EmberGuard>> GUARD = TYPES.register("ember_guard", () -> EntityType.Builder
            .<EmberGuard>of(EmberGuard::new, MobCategory.MONSTER).sized(0.8F, 2.0F).fireImmune().clientTrackingRange(8)
            .build("riftworld:ember_guard"));
    public static final RegistryObject<EntityType<ScytheItemEntity>> SCYTHE_ITEM = TYPES.register("scythe_item", () -> EntityType.Builder
            .<ScytheItemEntity>of(ScytheItemEntity::new, MobCategory.MISC).sized(0.5F, 0.3F).fireImmune().clientTrackingRange(8)
            .updateInterval(20).build("riftworld:scythe_item"));
    /** v0.24.0: Огромный плавный модульный 3D оригами-дракон (~47.7 м), парящий вдоль диких островов, под сводами и над морем (вне Города!). */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.PaperDragonEntity>> PAPER_DRAGON = TYPES.register("paper_dragon",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.PaperDragonEntity>of(ru.aiforge.riftworld.entity.PaperDragonEntity::new, MobCategory.AMBIENT)
                    .sized(3.8F, 2.8F).updateInterval(3).clientTrackingRange(32).build("riftworld:paper_dragon"));

    /** v0.22.0: Безблочный белый 3D-геометрический Пролом в Пространстве (от узкого острия у земли до широкого разлома в небе). */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.SpatialRiftEntity>> SPATIAL_RIFT = TYPES.register("spatial_rift",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.SpatialRiftEntity>of(ru.aiforge.riftworld.entity.SpatialRiftEntity::new, MobCategory.MISC)
                    .sized(2.2F, 4.2F).fireImmune().updateInterval(2).clientTrackingRange(24).build("riftworld:spatial_rift"));

    /** v0.23.0: Кастомная атака Босса — вытянутый светлый кристальный столб из-под земли (поднимает стоящего игрока!). */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.IcePillarEntity>> ICE_PILLAR = TYPES.register("ice_pillar",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.IcePillarEntity>of(ru.aiforge.riftworld.entity.IcePillarEntity::new, MobCategory.MISC)
                    .sized(1.36F, 7.6F).fireImmune().updateInterval(1).clientTrackingRange(16).build("riftworld:ice_pillar"));

    /** v0.23.0: Кастомная атака Босса — вытянутая светлая кристальная глыба-копьё в небе + метка приземления. */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.IceHailBoulderEntity>> ICE_HAIL_BOULDER = TYPES.register("ice_hail_boulder",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.IceHailBoulderEntity>of(ru.aiforge.riftworld.entity.IceHailBoulderEntity::new, MobCategory.MISC)
                    .sized(1.4F, 4.6F).fireImmune().updateInterval(1).clientTrackingRange(18).build("riftworld:ice_hail_boulder"));

    /** Рев 8 «Живое море»: вся ихтиофауна Элисара одной сущностью (вариант — в данных). */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.SeaFishEntity>> SEA_FISH = TYPES.register("sea_fish",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.SeaFishEntity>of(ru.aiforge.riftworld.entity.SeaFishEntity::new, MobCategory.WATER_CREATURE)
                    .sized(0.8F, 0.6F).updateInterval(2).clientTrackingRange(12).build("riftworld:sea_fish"));

    /** v0.23.0: Ещё более крупный Босс Колоннады «Расколотый Обелиск Разлома» (2.45x, стационарный левитирующий обелиск без тени человека). */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.RiftObeliskBoss>> RIFT_OBELISK_BOSS = TYPES.register("rift_obelisk_boss",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.RiftObeliskBoss>of(ru.aiforge.riftworld.entity.RiftObeliskBoss::new, MobCategory.MONSTER)
                    .sized(4.3F, 9.4F).fireImmune().clientTrackingRange(20).build("riftworld:rift_obelisk_boss"));

    /** v0.19.0: Призываемый парящий «Гранёный Прислужник» Колоннады. */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.CrystalMinion>> CRYSTAL_MINION = TYPES.register("crystal_minion",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.CrystalMinion>of(ru.aiforge.riftworld.entity.CrystalMinion::new, MobCategory.MONSTER)
                    .sized(0.85F, 2.1F).fireImmune().clientTrackingRange(10).build("riftworld:crystal_minion"));

    /** v0.19.0: Стремительный кристальный осколок (снаряд Веера Осколков и Босса Колоннады). */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.CrystalShardEntity>> CRYSTAL_SHARD = TYPES.register("crystal_shard",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.CrystalShardEntity>of(ru.aiforge.riftworld.entity.CrystalShardEntity::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F).updateInterval(2).clientTrackingRange(8).build("riftworld:crystal_shard"));

    /** v0.19.0: 3D кристальный щит «Бастиона Граней» (режим тарана ЛКМ / защиты 6с ПКМ). */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.CrystalShieldEntity>> CRYSTAL_SHIELD = TYPES.register("crystal_shield",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.CrystalShieldEntity>of(ru.aiforge.riftworld.entity.CrystalShieldEntity::new, MobCategory.MISC)
                    .sized(2.1F, 2.1F).updateInterval(1).clientTrackingRange(10).build("riftworld:crystal_shield"));

    /** v0.27.0: Дружелюбная летающая рыбка-стрекоза (фарфоровое тело + селадоновые плавники + 4 крылышка стрекозы) для обычных биомов. */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.SkyDartFishEntity>> SKY_DART_FISH = TYPES.register("sky_dart_fish",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.SkyDartFishEntity>of(ru.aiforge.riftworld.entity.SkyDartFishEntity::new, MobCategory.CREATURE)
                    .sized(0.55F, 0.45F).updateInterval(2).clientTrackingRange(12).build("riftworld:sky_dart_fish"));

    /** v0.27.0: Белоснежный грациозный олень с живыми цветами на ветвистых рогах для обычных мирных биомов. */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.BlossomStagEntity>> BLOSSOM_STAG = TYPES.register("blossom_stag",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.BlossomStagEntity>of(ru.aiforge.riftworld.entity.BlossomStagEntity::new, MobCategory.CREATURE)
                    .sized(0.95F, 1.95F).clientTrackingRange(14).build("riftworld:blossom_stag"));

    /** v0.32.0: Лиоплевродон — 12-блочный засадник глубин Элисара (концепт `reports/mob_refs/03_ref_liopleurodon.png`). */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.LiopleurodonEntity>> LIOPLEURODON =
            TYPES.register("liopleurodon", () -> EntityType.Builder
                    .<ru.aiforge.riftworld.entity.LiopleurodonEntity>of(ru.aiforge.riftworld.entity.LiopleurodonEntity::new,
                            MobCategory.WATER_CREATURE)
                    .sized(4.4F, 2.6F).updateInterval(2).clientTrackingRange(24)
                    .build("riftworld:liopleurodon"));

    /** El Gran Maja: small anchor; full animated terrain collision is checked separately. */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.ElGranMajaEntity>> EL_GRAN_MAJA =
            TYPES.register("el_gran_maja", () -> EntityType.Builder
                    .<ru.aiforge.riftworld.entity.ElGranMajaEntity>of(ru.aiforge.riftworld.entity.ElGranMajaEntity::new,
                            MobCategory.WATER_CREATURE)
                    .sized(1.0F, 1.0F).updateInterval(2).clientTrackingRange(32)
                    .build("riftworld:el_gran_maja"));

    /** v0.34.0: Пегас — крылатый скакун «земля/воздух» (концепт `reports/mob_refs/04_ref_pegasus.png`). */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.PegasusEntity>> PEGASUS =
            TYPES.register("pegasus", () -> EntityType.Builder
                    .<ru.aiforge.riftworld.entity.PegasusEntity>of(ru.aiforge.riftworld.entity.PegasusEntity::new,
                            MobCategory.CREATURE)
                    .sized(1.3F, 2.0F).clientTrackingRange(12)
                    .build("riftworld:pegasus"));

    /** v0.35.0: Вендиго — хозяин «Мороковой Пади» (концепт `02_night_horror_wendigo.jpg`). */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.WendigoEntity>> WENDIGO =
            TYPES.register("wendigo", () -> EntityType.Builder
                    .<ru.aiforge.riftworld.entity.WendigoEntity>of(ru.aiforge.riftworld.entity.WendigoEntity::new,
                            MobCategory.MONSTER)
                    .sized(0.9F, 2.7F).clientTrackingRange(10)
                    .build("riftworld:wendigo"));

    /** v0.39.0: Морок — собственный хоррор-моб «Мороковой Пади» (быстрый, кружит, моргает). */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.MorokEntity>> MOROK =
            TYPES.register("morok", () -> EntityType.Builder
                    .<ru.aiforge.riftworld.entity.MorokEntity>of(ru.aiforge.riftworld.entity.MorokEntity::new,
                            MobCategory.MONSTER)
                    .sized(0.85F, 2.45F).clientTrackingRange(10)
                    .build("riftworld:morok"));

    /** v0.41.0: обычный горожанин-торговец, стражник и лучник — три отдельных типа сущностей. */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.ElisarVillagerEntity>> ELISAR_VILLAGER =
            TYPES.register("elisar_villager", () -> EntityType.Builder
                    .<ru.aiforge.riftworld.entity.ElisarVillagerEntity>of(ru.aiforge.riftworld.entity.ElisarVillagerEntity::new, MobCategory.CREATURE)
                    .sized(0.65F, 1.95F).clientTrackingRange(12).build("riftworld:elisar_villager"));
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.ElisarGuardEntity>> ELISAR_GUARD =
            TYPES.register("elisar_guard", () -> EntityType.Builder
                    .<ru.aiforge.riftworld.entity.ElisarGuardEntity>of(ru.aiforge.riftworld.entity.ElisarGuardEntity::new, MobCategory.CREATURE)
                    .sized(0.70F, 2.05F).clientTrackingRange(12).build("riftworld:elisar_guard"));
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.ElisarArcherEntity>> ELISAR_ARCHER =
            TYPES.register("elisar_archer", () -> EntityType.Builder
                    .<ru.aiforge.riftworld.entity.ElisarArcherEntity>of(ru.aiforge.riftworld.entity.ElisarArcherEntity::new, MobCategory.CREATURE)
                    .sized(0.65F, 1.95F).clientTrackingRange(12).build("riftworld:elisar_archer"));

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
        e.put(PEGASUS.get(), ru.aiforge.riftworld.entity.PegasusEntity.attributes().build());
        e.put(WENDIGO.get(), ru.aiforge.riftworld.entity.WendigoEntity.attributes().build());
        e.put(MOROK.get(), ru.aiforge.riftworld.entity.MorokEntity.attributes().build());
        e.put(ELISAR_VILLAGER.get(), ru.aiforge.riftworld.entity.ElisarVillagerEntity.attributes().build());
        e.put(ELISAR_GUARD.get(), ru.aiforge.riftworld.entity.ElisarGuardEntity.attributes().build());
        e.put(ELISAR_ARCHER.get(), ru.aiforge.riftworld.entity.ElisarArcherEntity.attributes().build());
    }
}
