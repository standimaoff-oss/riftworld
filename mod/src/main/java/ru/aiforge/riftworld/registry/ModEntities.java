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
    /** Biome 01: the paper-dragons / kites drifting over the Paper-Dragon Sea (ambient, harmless). */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.PaperDragonEntity>> PAPER_DRAGON = TYPES.register("paper_dragon",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.PaperDragonEntity>of(ru.aiforge.riftworld.entity.PaperDragonEntity::new, MobCategory.AMBIENT)
                    .sized(1.6F, 0.9F).updateInterval(3).clientTrackingRange(16).build("riftworld:paper_dragon"));

    /** Рев 8 «Живое море»: вся ихтиофауна Элисара одной сущностью (вариант — в данных). */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.SeaFishEntity>> SEA_FISH = TYPES.register("sea_fish",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.SeaFishEntity>of(ru.aiforge.riftworld.entity.SeaFishEntity::new, MobCategory.WATER_CREATURE)
                    .sized(0.8F, 0.6F).updateInterval(2).clientTrackingRange(12).build("riftworld:sea_fish"));

    /** v0.19.0: Босс Колоннады «Расколотый Обелиск Разлома» (стационарный левитирующий обелиск без тени человека). */
    public static final RegistryObject<EntityType<ru.aiforge.riftworld.entity.RiftObeliskBoss>> RIFT_OBELISK_BOSS = TYPES.register("rift_obelisk_boss",
            () -> EntityType.Builder.<ru.aiforge.riftworld.entity.RiftObeliskBoss>of(ru.aiforge.riftworld.entity.RiftObeliskBoss::new, MobCategory.MONSTER)
                    .sized(1.8F, 3.8F).fireImmune().clientTrackingRange(12).build("riftworld:rift_obelisk_boss"));

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

    private ModEntities() {
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent e) {
        e.put(BOSS.get(), MorgrathBoss.attributes().build());
        e.put(GUARD.get(), EmberGuard.attributes().build());
        e.put(PAPER_DRAGON.get(), net.minecraft.world.entity.monster.Monster.createMonsterAttributes().build());
        e.put(SEA_FISH.get(), ru.aiforge.riftworld.entity.SeaFishEntity.attributes().build());
        e.put(RIFT_OBELISK_BOSS.get(), ru.aiforge.riftworld.entity.RiftObeliskBoss.attributes().build());
        e.put(CRYSTAL_MINION.get(), ru.aiforge.riftworld.entity.CrystalMinion.attributes().build());
    }
}
