package ru.aiforge.riftworld.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import ru.aiforge.riftworld.registry.ModItems;
import ru.aiforge.riftworld.world.ElisarLaw;
import ru.aiforge.riftworld.world.ElisarWitness;
import ru.aiforge.riftworld.world.PatrolRoute;

/**
 * v0.49.0 — Страж Элисара (референс 06_elisar_guard.png) — стражник города и
 * деревень в золотисто-бирюзовом шлеме и кирасе, со стальным мечом в левой
 * руке и гербовым щитом в правой.
 *
 * <p>Страж ведёт себя как городская стража, а не как обычный монстр:</p>
 * <ul>
 *   <li><b>Патруль по дорогам и мостам.</b> Страж идёт по настоящим
 *       дорожным сегментам города ({@link PatrolRoute}), а не бродит наугад.</li>
 *   <li><b>Предупреждение, а не убийство.</b> Удар по стражу — это
 *       предупреждение: страж кричит, отступает и зовёт подкрепление, но не
 *       убивает сразу. До трёх предупреждений.</li>
 *   <li><b>Розыск.</b> Если смерть жителя или стража видел кто-то из города,
 *       убийца попадает в розыск: стражи переходят в погоню.</li>
 * </ul>
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = ru.aiforge.riftworld.RiftWorld.MODID)
public final class ElisarGuardEntity extends PathfinderMob {

    private static final EntityDataAccessor<Boolean> ON_PATROL =
            SynchedEntityData.defineId(ElisarGuardEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> WARNINGS =
            SynchedEntityData.defineId(ElisarGuardEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ALARM =
            SynchedEntityData.defineId(ElisarGuardEntity.class, EntityDataSerializers.BOOLEAN);

    private int shieldRaiseTicks = 0;

    /** Текущий маршрут патруля (не сохраняется — строится заново). */
    @Nullable
    private PatrolRoute route;
    /** Индекс текущей точки маршрута. */
    private int routeIndex;
    /** Куда идти прямо сейчас. */
    @Nullable
    private BlockPos patrolTarget;

    public ElisarGuardEntity(EntityType<? extends ElisarGuardEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        if (this.getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
        }
        equipDefaultArmament();
    }

    public void equipDefaultArmament() {
        // Референс 06: меч в левой руке (MAINHAND при LeftHanded=true), щит — на правой руке (OFFHAND)!
        this.setLeftHanded(true);
        if (this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.ELISAR_GUARD_SWORD.get()));
        }
        if (this.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty()) {
            this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.ELISAR_GUARD_SHIELD.get()));
        }
        this.setDropChance(EquipmentSlot.MAINHAND, 0.06F);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.06F);
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 44.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.31D)
                .add(Attributes.FOLLOW_RANGE, 26.0D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.30D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        // ВАЖНО: значения синхронизированных данных нужно СНАЧАЛА зарегистрировать
        // через define(), и только потом ставить. Без define() обращение к
        // несуществующему ключу даёт NullPointerException прямо в конструкторе
        // сущности — страж просто не появляется в игре.
        this.entityData.define(ON_PATROL, Boolean.TRUE);
        this.entityData.define(WARNINGS, 0);
        this.entityData.define(ALARM, Boolean.FALSE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.18D, true));
        this.goalSelector.addGoal(2, new PatrolGoal(this));
        this.goalSelector.addGoal(3, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.90D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, ElisarGuardEntity.class, ElisarArcherEntity.class).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false, e -> e instanceof Enemy));
        // Розыск: игрок, которого город видел убийцей, становится целью.
        this.targetSelector.addGoal(3, new WantedTargetGoal(this));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                  MobSpawnType reason, @Nullable SpawnGroupData spawnData,
                                                  @Nullable CompoundTag dataTag) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
        equipDefaultArmament();
        if (reason == MobSpawnType.CHUNK_GENERATION || reason == MobSpawnType.STRUCTURE) {
            this.restrictTo(this.blockPosition(), 34);
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Патруль
    // ------------------------------------------------------------------

    public boolean onPatrol() {
        return this.entityData.get(ON_PATROL);
    }

    public void setOnPatrol(boolean value) {
        this.entityData.set(ON_PATROL, value);
    }

    /** Тревога по городу: стражи рядом переходят в погоню. */
    public boolean alarm() {
        return this.entityData.get(ALARM);
    }

    public void setAlarm(boolean value) {
        this.entityData.set(ALARM, value);
    }

    public int warnings() {
        return this.entityData.get(WARNINGS);
    }

    /** Текущий маршрут патруля (для отладки и тестов). */
    @Nullable
    public PatrolRoute route() {
        return this.route;
    }

    /** Индекс текущей точки маршрута. */
    public int routeIndex() {
        return this.routeIndex;
    }

    /** Куда страж идёт прямо сейчас. */
    @Nullable
    public BlockPos patrolTarget() {
        return this.patrolTarget;
    }

    /**
     * Найти маршрут патруля рядом и начать по нему ходить.
     *
     * @return {@code true}, если маршрут найден
     */
    public boolean assignPatrol(ServerLevel level) {
        var plan = ru.aiforge.riftworld.world.CityPlan.get();
        PatrolRoute found = PatrolRoute.nearestTo(plan, level, this.getX(), this.getY(), this.getZ(), 28.0);
        if (found == null || found.isEmpty()) {
            return false;
        }
        this.route = found;
        this.routeIndex = found.nearestIndex(this.getX(), this.getY(), this.getZ());
        this.patrolTarget = null;
        this.setOnPatrol(true);
        return true;
    }

    /** Перейти к следующей точке маршрута. */
    private void advanceRoute() {
        if (this.route == null || this.route.isEmpty()) {
            this.patrolTarget = null;
            return;
        }
        this.routeIndex = (this.routeIndex + 1) % this.route.size();
        PatrolRoute.Waypoint wp = this.route.points().get(this.routeIndex);
        this.patrolTarget = BlockPos.containing(wp.x(), wp.y(), wp.z());
    }

    // ------------------------------------------------------------------
    // Закон: предупреждение, розыск, тюрьма
    // ------------------------------------------------------------------

    /**
     * Предупредить нарушителя, не убивая.
     *
     * @return {@code true}, если нарушитель получил предупреждение
     */
    public boolean warnOffender(LivingEntity offender) {
        if (offender instanceof ServerPlayer player) {
            ServerLevel level = (ServerLevel) player.level();
            ElisarLaw law = ElisarLaw.get(level);
            int n = law.warn(player);
            this.entityData.set(WARNINGS, n);
            player.displayClientMessage(Component.literal(
                    switch (n) {
                        case 1 -> "\u00a7eСтраж Элисара: «Стой! Прекрати — последний раз прошу!»";
                        case 2 -> "\u00a7eСтраж Элисара: «Я повторяю! Уходи, пока цел!»";
                        default -> "\u00a7cСтраж Элисара: «Ты предупреждён! Следующий удар будет последним!»";
                    }), true);
            // Предупреждение — это тревога, но ещё не розыск.
            raiseAlarm(level, player);
            return true;
        }
        // Неигрок (моб) — страж его отпугивает, но не убивает сразу.
        if (offender instanceof net.minecraft.world.entity.Mob mob) {
            mob.setTarget(null);
            mob.knockback(0.4F, this.getX() - mob.getX(), this.getZ() - mob.getZ());
        }
        return false;
    }

    /** Поднять тревогу: соседние стражи и жители узнают о нарушителе. */
    public void raiseAlarm(ServerLevel level, LivingEntity offender) {
        this.setAlarm(true);
        var area = this.getBoundingBox().inflate(40.0, 16.0, 40.0);
        for (ElisarGuardEntity guard : level.getEntitiesOfClass(ElisarGuardEntity.class, area)) {
            guard.setAlarm(true);
            guard.setOnPatrol(false);
        }
        for (ElisarVillagerEntity villager : level.getEntitiesOfClass(ElisarVillagerEntity.class, area)) {
            villager.alertDefenders(offender);
        }
    }

    /**
     * Зафиксировать убийство: если преступление видели, начинается розыск.
     *
     * @return число очевидцев
     */
    public int reportKill(ServerLevel level, Entity victim, Entity criminal) {
        int witnesses = ElisarWitness.countWitnesses(level, victim, criminal);
        if (witnesses > 0 && criminal instanceof ServerPlayer player) {
            ElisarLaw law = ElisarLaw.get(level);
            law.reportCrime(level, player, witnesses, null);
            player.displayClientMessage(Component.literal(
                    "\u00a7cВас увидели. Вы в розыске — стражи города ищут убийцу."), true);
            raiseAlarm(level, player);
        }
        return witnesses;
    }

    @Override
    /**
     * Страж погиб. Если убийцу видели другие горожане или стражи — начинается
     * розыск: стражи переходят в погоню, а после смерти игрок окажется в
     * городской тюрьме, а его вещи — в сундуке склада рядом с ней.
     *
     * <p>В 1.20.1 Forge нет {@code LivingEntity.DeadEntityReport}: смерть приходит
     * событием {@link net.minecraftforge.event.entity.living.LivingDeathEvent},
     * поэтому проверка «видел ли город» живёт в обработчике события, а этот
     * метод остаётся обычным хуком сущности.
     */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onGuardDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ElisarGuardEntity guard)) return;
        if (!(guard.level() instanceof ServerLevel level)) return;
        Entity killer = event.getSource() == null ? null : event.getSource().getEntity();
        if (killer == null || killer == guard) return;
        int witnesses = ElisarWitness.countWitnesses(level, guard, killer);
        if (witnesses <= 0) return;
        if (killer instanceof ServerPlayer player) {
            ElisarLaw law = ElisarLaw.get(level);
            law.reportCrime(level, player, witnesses, null);
            player.displayClientMessage(Component.literal(
                    "\u00a7cВас увидели. Вы в розыске — стражи города ищут убийцу."), true);
            // Тревога: соседние стражи бросаются в погоню за убийцей.
            var area = guard.getBoundingBox().inflate(48.0, 20.0, 48.0);
            for (ElisarGuardEntity other : level.getEntitiesOfClass(ElisarGuardEntity.class, area)) {
                other.setAlarm(true);
                other.setOnPatrol(false);
                other.setTarget(player);
            }
        }
    }

    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            LivingEntity tgt = this.getTarget();
            if (tgt != null && tgt.isAlive() && !this.getOffhandItem().isEmpty()) {
                double dSq = this.distanceToSqr(tgt);
                if (dSq > 12.0D && dSq < 256.0D && this.tickCount % 35 < 16) {
                    if (!this.isUsingItem()) {
                        this.startUsingItem(InteractionHand.OFF_HAND);
                        this.shieldRaiseTicks = 16;
                    }
                }
            }
            if (this.shieldRaiseTicks > 0) {
                --this.shieldRaiseTicks;
                if (this.shieldRaiseTicks == 0 && this.isUsingItem()) {
                    this.stopUsingItem();
                }
            }
            // Тревога спадает сама, если нарушителя рядом нет.
            if (this.alarm() && this.getTarget() == null && this.tickCount % 200 == 0) {
                this.setAlarm(false);
                this.setOnPatrol(true);
            }
        }
    }

    /**
     * v0.49.0 — удар по стражу вызывает ОТВЕТНЫЙ УДАР и ПРЕДУПРЕЖДЕНИЕ.
     *
     * <p>Раньше страж просто получал урон и забывал об этом. Теперь:
     * <ul>
     *   <li>страж <b>бьёт в ответ</b> того, кто его ударил (не убивая сразу —
     *       это предупреждение, а не казнь);</li>
     *   <li>нарушитель получает предупреждение со счётом 1..3;</li>
     *   <li>поднимается тревога: соседние стражи бросаются на помощь.</li>
     * </ul>
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hit = super.hurt(source, amount);
        if (!hit || this.level().isClientSide) return hit;

        Entity attacker = source.getEntity();
        // Отвечать только живому нападающему: падение с высоты или урон от
        // стихии не должны вызывать погоню.
        if (!(attacker instanceof LivingEntity living) || !living.isAlive()) return hit;
        if (living instanceof net.minecraft.world.entity.Mob mob && !mob.canAttack(this)) return hit;

        // Страж бьёт в ответ один раз за удар: он ЗАЩИЩАЕТСЯ, а не мстит.
        if (!(attacker instanceof ServerPlayer player)) {
            // Моб или другое существо — страж предупреждает и отгоняет.
            if (this.getTarget() == null) {
                this.setTarget(living);
                this.setOnPatrol(false);
            }
            this.warnOffender(living);
            return hit;
        }

        // Игрок: предупреждение со счётом и ответный удар без убийства.
        int n = this.warnOffender(player) ? this.warnings() : 0;
        this.setTarget(player);
        this.setOnPatrol(false);
        this.raiseAlarm(this.level() instanceof ServerLevel sl ? sl : (ServerLevel) this.level(), player);

        // Ответный удар: страж бьёт, но НЕ добивает — жизнь игрока не
        // отнимается этим ударом (урон ограничен и не убивает сразу).
        if (this.getTarget() == player && this.distanceTo(player) <= 3.0D && n > 0) {
            player.hurt(this.level().damageSources().mobAttack(this), 3.0F);
            this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
        return hit;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.isUsingItem()) {
            this.stopUsingItem();
        }
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.knockback(0.6F,
                    this.getX() - living.getX(),
                    this.getZ() - living.getZ());
        }
        return hit;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("OnPatrol", this.onPatrol());
        tag.putBoolean("Alarm", this.alarm());
        tag.putInt("Warnings", this.warnings());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setOnPatrol(!tag.contains("OnPatrol") || tag.getBoolean("OnPatrol"));
        this.setAlarm(tag.getBoolean("Alarm"));
        this.entityData.set(WARNINGS, tag.getInt("Warnings"));
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.isBlocking() ? SoundEvents.SHIELD_BLOCK : SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    /**
     * Преследование игрока, находящегося в розыске.
     */
    public static final class WantedTargetGoal extends net.minecraft.world.entity.ai.goal.target.TargetGoal {
        private final ElisarGuardEntity guard;

        public WantedTargetGoal(ElisarGuardEntity guard) {
            super(guard, false);
            this.guard = guard;
        }

        @Override
        public boolean canUse() {
            if (!(this.guard.level() instanceof ServerLevel level)) {
                return false;
            }
            if (this.guard.getTarget() != null) {
                return false;
            }
            if (this.guard.onPatrol() && !this.guard.alarm()) {
                return false;
            }
            for (ServerPlayer player : level.players()) {
                if (ElisarLaw.get(level).isWanted(level, player)
                        && this.guard.distanceToSqr(player) < 4096.0) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void start() {
            if (!(this.guard.level() instanceof ServerLevel level)) {
                return;
            }
            for (ServerPlayer player : level.players()) {
                if (ElisarLaw.get(level).isWanted(level, player)
                        && this.guard.distanceToSqr(player) < 4096.0) {
                    this.guard.setTarget(player);
                    return;
                }
            }
        }
    }

    /**
     * Патруль по дорогам и мостам города.
     */
    public static final class PatrolGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final ElisarGuardEntity guard;

        public PatrolGoal(ElisarGuardEntity guard) {
            this.guard = guard;
            this.setFlags(java.util.EnumSet.of(net.minecraft.world.entity.ai.goal.Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!this.guard.onPatrol()) {
                return false;
            }
            if (this.guard.getTarget() != null) {
                return false;
            }
            if (this.guard.route() == null) {
                return this.guard.level() instanceof ServerLevel level
                        && this.guard.assignPatrol(level);
            }
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.guard.onPatrol()
                    && this.guard.getTarget() == null
                    && this.guard.route() != null;
        }

        @Override
        public void tick() {
            BlockPos target = this.guard.patrolTarget();
            if (target == null) {
                this.guard.advanceRoute();
                target = this.guard.patrolTarget();
            }
            if (target == null) {
                return;
            }
            double dSq = this.guard.distanceToSqr(
                    target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
            if (dSq < 2.5) {
                this.guard.advanceRoute();
                target = this.guard.patrolTarget();
                if (target == null) {
                    return;
                }
            }
            this.guard.getNavigation().moveTo(
                    target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
        }

        @Override
        public void stop() {
            this.guard.getNavigation().stop();
        }
    }
}
