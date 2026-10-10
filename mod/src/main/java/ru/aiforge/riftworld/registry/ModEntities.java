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

    private ModEntities() {
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent e) {
        e.put(BOSS.get(), MorgrathBoss.attributes().build());
        e.put(GUARD.get(), EmberGuard.attributes().build());
        e.put(PAPER_DRAGON.get(), net.minecraft.world.entity.monster.Monster.createMonsterAttributes().build());
    }
}
