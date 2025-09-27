package org.slimecraft.mutationfactory;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.*;
import net.minestom.server.entity.ai.EntityAIGroup;
import net.minestom.server.entity.ai.goal.RandomStrollGoal;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class Creature extends EntityCreature {
    public static final Tag<@NotNull Integer> BREEDING_TIME = Tag.Integer("breeding_time");

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
    private EventListener<? extends @NotNull InstanceEvent> tameListener;
    private EventListener<? extends @NotNull InstanceEvent> creatureInteractListener;

    /**
     * Construct a creature from the given {@link Builder}.
     * Private because instances are only created through the builder due to the
     * complexity of the class.
     */
    private Creature(Builder builder) {
        super(builder.species.entityType());
        final Random random = new Random();
        this.species = builder.species;
        this.level = Objects.requireNonNullElseGet(builder.level, () ->
                Config.MIN_LEVEL + (int) (Math.pow(random.nextDouble(), 5) * (Config.MAX_LEVEL - Config.MIN_LEVEL + 1)));
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
        this.initializeDefaults();
    }

    private void initializeDefaults() {
        final EntityAIGroup aiGroup = new EntityAIGroup();
        aiGroup.getGoalSelectors().add(new RandomStrollGoal(this, 20));
        this.addAIGroup(aiGroup);
        this.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(this.speed.getBaseValue());
        this.getAttribute(Attribute.MAX_HEALTH).setBaseValue(this.health.getBaseValue());
        this.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(this.melee.getBaseValue());
        this.setTag(BREEDING_TIME, this.breedTime);
    }

    public static Creature wild(
            @NotNull Species species,
            @NotNull Stat health,
            @NotNull Stat melee,
            @NotNull Stat speed,
            @NotNull Consumer<Creature> configurator
    ) {
        return builder(species, 0, health, Stat.EMPTY, Stat.EMPTY, Stat.EMPTY, Stat.EMPTY, melee, speed, Collections.emptyList())
                .configurator(configurator)
                .build();
    }

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

    private void listeners() {
        this.tameListener = EventListener.builder(EntityAttackEvent.class)
                .handler(event -> {
                    if (!(event.getEntity() instanceof final Player player)) {
                        return;
                    }
                    if (!this.tamed) {
                        this.damage(Damage.fromPlayer(player, 0));
                        this.timesHit++;
                        if (this.timesHit == level) {
                            this.tamed = true;
                            player.sendMessage(Component.text("IT WAS TAMED!"));
                            player.getInventory().addItemStack(CreatureItemStack.toItem(this));
                            this.remove();
                        }
                    } else {
                        player.openInventory(new CreatureInventory(this));
                    }
                })
                .filter(event ->
                        event.getTarget() == this && event.getEntity() instanceof Player)
                .build();

        this.creatureInteractListener = EventListener.builder(PlayerEntityInteractEvent.class)
                .handler(event -> {
                    final Player player = event.getPlayer();
                    if (player.isSneaking()) {
                        player.getInventory().addItemStack(CreatureItemStack.toItem(this));
                        this.remove();
                    } else {
                        if (this.getLeashHolder() != null) {
                            this.setLeashHolder(null);
                            return;
                        }
                        this.setLeashHolder(player);
                        final Set<Entity> leashed = player.getLeashedEntities();
                        if (leashed.size() == 2) {
                            leashed.forEach(entity -> {
                                if (!(entity instanceof final Creature creature)) {
                                    return;
                                }
                                if (creature == this) {
                                    return;
                                }
                                if (this.male && !creature.isMale() || creature.isMale() && !this.male) {
                                    this.setLeashHolder(creature);
                                    this.scheduler().submitTask(() -> {
                                        if (creature.getTag(BREEDING_TIME) == 0) {
                                            /*final Creature newCreature = new Creature(
                                                    this.entityType,
                                                    this.speciesName,
                                                    this.level + 100,
                                                    true,
                                                    this.male,
                                                    this.breedTime,
                                                    this.baseDamage,
                                                    null
                                            );
                                            newCreature.getAttribute(Attribute.SCALE).setBaseValue(0.1);
                                            newCreature.setInstance(this.instance, this.position.withZ(z -> z - 2));*/
                                            // TODO: CREATE STATIC tamed() function
                                            creature.setLeashHolder(null);
                                            this.setLeashHolder(null);
                                            return TaskSchedule.stop();
                                        }
                                        creature.updateTag(BREEDING_TIME, remaining -> remaining - 1);
                                        return TaskSchedule.seconds(1);
                                    });
                                }
                            });
                            player.getLeashedEntities().forEach(entity -> {
                                if (!(entity instanceof final Creature creature)) {
                                    return;
                                }
                                creature.setLeashHolder(null);
                            });
                        }
                    }
                })
                .filter(event ->
                        event.getTarget() == this && this.tamed && event.getHand() == PlayerHand.MAIN)
                .build();

        this.instance.eventNode().addListener(this.tameListener);
        this.instance.eventNode().addListener(this.creatureInteractListener);
    }

    private void removeListeners() {
        this.instance.eventNode().removeListener(this.tameListener);
        this.instance.eventNode().removeListener(this.creatureInteractListener);
    }

    private void attachHologram() {
        final Entity hologram = new Entity(EntityType.TEXT_DISPLAY);
        hologram.editEntityMeta(TextDisplayMeta.class, meta -> {
            meta.setAlignment(TextDisplayMeta.Alignment.CENTER);
            meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
            meta.setText(Component.text(this.species.name()).append(Component.text(" | ")).append(Component.text(this.level)));
            meta.setUseDefaultBackground(true);
        });
        this.addPassenger(hologram);
    }

    @Override
    public @NotNull CompletableFuture<Void> setInstance(@NotNull Instance instance, @NotNull Pos spawnPosition) {
        return super.setInstance(instance, spawnPosition).thenRun(() -> {
            this.attachHologram();
            this.listeners();
        });
    }

    @Override
    public void kill() {
        this.whenNoLongerExisting();
        super.kill();
    }

    @Override
    public void remove() {
        this.whenNoLongerExisting();
        super.remove();
    }

    private void whenNoLongerExisting() {
        this.removeListeners();
        this.getPassengers().forEach(entity -> {
            if (entity.getEntityType() != EntityType.TEXT_DISPLAY) {
                return;
            }
            entity.remove();
        });
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
}
