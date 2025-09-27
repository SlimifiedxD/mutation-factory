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
import java.util.function.Function;

public class Creature extends EntityCreature {
    public static final Tag<@NotNull Integer> BREEDING_TIME = Tag.Integer("breeding_time");

    private final EntityType entityType;
    private final String speciesName;
    private final int level;
    private int timesHit;
    private boolean tamed;
    private final boolean male;
    private final int breedTime;
    private final float damage;
    private EventListener<? extends @NotNull InstanceEvent> tameListener;
    private EventListener<? extends @NotNull InstanceEvent> creatureInteractListener;

    public Creature(EntityType entityType, String speciesName, Random random, int breedTime, float damage, Consumer<Creature> configurator, Function<Creature, EntityAIGroup> aiGroupFunction) {
        super(entityType);
        this.entityType = entityType;
        this.speciesName = speciesName;
        this.level = Config.MIN_LEVEL + (int) (Math.pow(random.nextDouble(), 5) * (Config.MAX_LEVEL - Config.MIN_LEVEL + 1));
        this.male = random.nextBoolean();
        this.breedTime = breedTime;
        this.damage = damage;
        this.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.5);
        this.setTag(BREEDING_TIME, this.breedTime);
        configurator.accept(this);

        final EntityAIGroup aiGroup = aiGroupFunction.apply(this);
        aiGroup.getGoalSelectors().add(new RandomStrollGoal(this, 20));
        this.addAIGroup(aiGroup);
    }

    public Creature(EntityType entityType, String speciesName, int level, boolean male, int breedTime, float damage) {
        super(entityType);
        this.entityType = entityType;
        this.speciesName = speciesName;
        this.level = level;
        this.male = male;
        this.tamed = true;
        this.breedTime = breedTime;
        this.damage = damage;
        this.setTag(BREEDING_TIME, this.breedTime);
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
                                            final Creature newCreature = new Creature(
                                                    this.entityType,
                                                    this.speciesName,
                                                    this.level + 100,
                                                    this.male,
                                                    this.breedTime,
                                                    this.damage
                                                    );
                                            newCreature.getAttribute(Attribute.SCALE).setBaseValue(0.1);
                                            newCreature.setInstance(this.instance, this.position.withZ(z -> z - 2));
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
            meta.setText(Component.text(this.speciesName).append(Component.text(" | ")).append(Component.text(this.level)));
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

    public String getSpeciesName() {
        return this.speciesName;
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

    public float getDamage() {
        return this.damage;
    }
}
