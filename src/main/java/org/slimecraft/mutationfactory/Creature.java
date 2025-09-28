package org.slimecraft.mutationfactory;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.*;
import net.minestom.server.entity.ai.EntityAIGroup;
import net.minestom.server.entity.ai.goal.MeleeAttackGoal;
import net.minestom.server.entity.ai.goal.RandomStrollGoal;
import net.minestom.server.entity.ai.target.ClosestEntityTarget;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Represents a creature that can spawn in-game. Creatures can range from everyday animals to mythological
 * beings. The class favors composition over inheritance, hence the class not being abstract; examples are the
 * {@link Consumer} configurer, and the many {@link Stat}s required in the {@link Builder} constructor.
 * The class is suffering from separation of concerns, hence its heinous length.
 */
public class Creature extends EntityCreature {
    public static final Tag<@NotNull Integer> BREEDING_TIME_REMAINING = Tag.Integer("breeding_time");

    private final Species species;
    private final int level;
    private int timesHit;
    private boolean tamed;
    private final boolean male;
    private final int breedTime;
    private final Stat health;
    private final Stat stamina;
    private final Stat oxygen;
    private final Stat food;
    private final Stat weight;
    private final Stat melee;
    private final Stat speed;
    private final List<Stat> additionalStats;
    private final CreatureService creatureService;
    private final EntityAIGroup defaultAiGroup;

    /**
     * Construct a creature from the given {@link Builder}.
     * The constructor is private because instances are only created through the builder due to the
     * complexity of the class.
     */
    private Creature(Builder builder) {
        super(builder.species.entityType());
        final Random random = new Random();
        this.creatureService = new CreatureService(this);
        this.species = builder.species;
        this.level = Objects.requireNonNullElseGet(builder.level, () ->
                Config.MIN_LEVEL + (int) (Math.pow(random.nextDouble(), 5) * Config.MAX_LEVEL));
        if (builder.tamed != null) {
            this.tamed = builder.tamed;
        }
        this.male = Objects.requireNonNullElseGet(builder.male, random::nextBoolean);
        this.breedTime = builder.breedTime;
        this.health = builder.health;
        this.stamina = builder.stamina;
        this.oxygen = builder.oxygen;
        this.food = builder.food;
        this.weight = builder.weight;
        this.melee = builder.melee;
        this.speed = builder.speed;
        this.additionalStats = builder.additionalStats;
        if (builder.configurator != null) {
            builder.configurator.accept(this);
        }
        this.defaultAiGroup = new EntityAIGroup();
        this.initializeDefaults();
    }

    /**
     * Initialize the defaults of this creature; this includes both its default behaviour and
     * turning all its {@link Stat}s into attributes.
     */
    private void initializeDefaults() {
        this.initializeAi();
        this.initializeAttributes();
        this.setTag(BREEDING_TIME_REMAINING, this.breedTime);
    }

    public void initializeAi() {
        if (!this.tamed) {
            this.defaultAiGroup.getGoalSelectors().add(new RandomStrollGoal(this, 20));
        }
        this.defaultAiGroup.getGoalSelectors().add(new MeleeAttackGoal(this, 0.5, 10, TimeUnit.SERVER_TICK));
        this.addAIGroup(defaultAiGroup);
    }

    public void initializeAttributes() {
        this.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(this.speed.getBaseValue());
        this.getAttribute(Attribute.MAX_HEALTH).setBaseValue(this.health.getBaseValue());
        this.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(this.melee.getBaseValue());
    }

    /**
     * Construct a wild {@link Creature}. Though wild creatures cannot use most stats,
     * all stats are required because wild creatures are the basis for creating tamed creatures;
     * the stats inputted here will be the base stats of the tamed creature with no upgrades and no
     * breeding levels.
     */
    public static Creature wild(
            @NotNull Species species,
            int breedTime,
            @NotNull Stat health,
            @NotNull Stat stamina,
            @NotNull Stat oxygen,
            @NotNull Stat food,
            @NotNull Stat weight,
            @NotNull Stat melee,
            @NotNull Stat speed,
            @Nullable Consumer<Creature> configurator
    ) {
        return builder(species, breedTime, health, stamina, oxygen, food, weight, melee, speed, Collections.emptyList())
                .configurator(configurator)
                .build();
    }

    /**
     * Construct a wild {@link Creature} without a configurator; refer to {@link Creature#wild(Species, int, Stat, Stat, Stat, Stat, Stat, Stat, Stat, Consumer)}
     * for more detail.
     */
    public static Creature wild(
            @NotNull Species species,
            int breedTime,
            @NotNull Stat health,
            @NotNull Stat stamina,
            @NotNull Stat oxygen,
            @NotNull Stat food,
            @NotNull Stat weight,
            @NotNull Stat melee,
            @NotNull Stat speed
    ) {
        return wild(species, breedTime, health, stamina, oxygen, food, weight, melee, speed, null);
    }

    public static Creature tamed(
            @NotNull Species species,
            int breedTime,
            Integer level,
            Boolean male,
            @NotNull Stat health,
            @NotNull Stat stamina,
            @NotNull Stat oxygen,
            @NotNull Stat food,
            @NotNull Stat weight,
            @NotNull Stat melee,
            @NotNull Stat speed,
            List<Stat> additionalStats
    ) {
        return builder(species, breedTime, health, stamina, oxygen, food, weight, melee, speed, additionalStats)
                .tamed(true)
                .level(level)
                .male(male)
                .build();
    }

    /**
     * Construct a {@link Builder}. This exists for style purposes; the behaviour is no different
     * from if one were to directly instantiate {@link Builder}.
     */
    public static Builder builder(
            @NotNull Species species,
            int breedTime,
            @NotNull Stat health,
            @NotNull Stat stamina,
            @NotNull Stat oxygen,
            @NotNull Stat food,
            @NotNull Stat weight,
            @NotNull Stat melee,
            @NotNull Stat speed,
            @NotNull List<Stat> additionalStats
    ) {
        return new Builder(species, breedTime, health, stamina, oxygen, food, weight, melee, speed, additionalStats);
    }

    /**
     * A helper class for constructing {@link Creature}s. The complexity of the class
     * warrants the use of a builder for required and optional values.
     */
    public static final class Builder {
        private final Species species;
        private final int breedTime;
        private Integer level;
        private Boolean tamed;
        private Boolean male;
        private final Stat health;
        private final Stat stamina;
        private final Stat oxygen;
        private final Stat food;
        private final Stat weight;
        private final Stat melee;
        private final Stat speed;
        private final List<Stat> additionalStats;
        private Consumer<Creature> configurator;

        public Builder(
                @NotNull Species species,
                int breedTime,
                @NotNull Stat health,
                @NotNull Stat stamina,
                @NotNull Stat oxygen,
                @NotNull Stat food,
                @NotNull Stat weight,
                @NotNull Stat melee,
                @NotNull Stat speed,
                @NotNull List<Stat> additionalStats) {
            this.species = species;
            this.breedTime = breedTime;
            this.health = health;
            this.stamina = stamina;
            this.oxygen = oxygen;
            this.food = food;
            this.weight = weight;
            this.melee = melee;
            this.speed = speed;
            this.additionalStats = additionalStats;
        }

        public Builder level(Integer level) {
            this.level = level;
            return this;
        }

        public Builder tamed(Boolean tamed) {
            this.tamed = tamed;
            return this;
        }

        public Builder male(Boolean male) {
            this.male = male;
            return this;
        }

        public Builder configurator(Consumer<Creature> configurator) {
            this.configurator = configurator;
            return this;
        }

        public Creature build() {
            return new Creature(this);
        }
    }

    @Override
    public @NotNull CompletableFuture<Void> setInstance(@NotNull Instance instance, @NotNull Pos spawnPosition) {
        return super.setInstance(instance, spawnPosition).thenRun(this.creatureService::whenSpawned);
    }

    @Override
    public void kill() {
        this.creatureService.whenNoLongerExisting();
        super.kill();
    }

    @Override
    public void remove() {
        this.creatureService.whenNoLongerExisting();
        super.remove();
    }

    public Species getSpecies() {
        return this.species;
    }

    public int getLevel() {
        return this.level;
    }

    public boolean isMale() {
        return this.male;
    }

    public int getBreedTime() {
        return this.breedTime;
    }

    /**
     * Get the health stat of this creature. The 'stat' suffix is appended
     * because {@link EntityCreature} has the {@link EntityCreature#getHealth()} method.
     */
    public Stat getHealthStat() {
        return health;
    }

    public Stat getStamina() {
        return stamina;
    }

    public Stat getOxygen() {
        return oxygen;
    }

    public Stat getFood() {
        return food;
    }

    public Stat getWeight() {
        return weight;
    }

    public Stat getMelee() {
        return melee;
    }

    public Stat getSpeed() {
        return speed;
    }

    public List<Stat> getAdditionalStats() {
        return additionalStats;
    }

    public boolean isTamed() {
        return this.tamed;
    }

    public void setTamed(boolean tamed) {
        this.tamed = tamed;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Creature creature = (Creature) o;
        return level == creature.level && timesHit == creature.timesHit && tamed == creature.tamed && male == creature.male && breedTime == creature.breedTime && Objects.equals(species, creature.species) && Objects.equals(health, creature.health) && Objects.equals(stamina, creature.stamina) && Objects.equals(oxygen, creature.oxygen) && Objects.equals(food, creature.food) && Objects.equals(weight, creature.weight) && Objects.equals(melee, creature.melee) && Objects.equals(speed, creature.speed) && Objects.equals(additionalStats, creature.additionalStats) && Objects.equals(creatureService, creature.creatureService);
    }

    @Override
    public int hashCode() {
        return Objects.hash(species, level, timesHit, tamed, male, breedTime, health, stamina, oxygen, food, weight, melee, speed, additionalStats, creatureService);
    }
}
