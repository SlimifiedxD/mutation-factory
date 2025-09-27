package org.slimecraft.mutationfactory;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class CreatureService {
    private final Creature creature;
    private EventListener<? extends @NotNull InstanceEvent> tameListener;
    private EventListener<? extends @NotNull InstanceEvent> creatureInteractListener;
    private int timesHit;

    public CreatureService(Creature creature) {
        this.creature = creature;
    }

    public void whenSpawned() {
        this.initializeListeners();
        this.attachListeners();
        this.attachHologram();
    }

    public void whenNoLongerExisting() {
        this.detachListeners();
        this.creature.getPassengers().forEach(entity -> {
            if (entity.getEntityType() != EntityType.TEXT_DISPLAY) {
                return;
            }
            entity.remove();
        });
    }

    private void attachHologram() {
        final Entity hologram = new Entity(EntityType.TEXT_DISPLAY);
        hologram.editEntityMeta(TextDisplayMeta.class, meta -> {
            meta.setAlignment(TextDisplayMeta.Alignment.CENTER);
            meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
            meta.setText(Component.text(this.creature.getSpecies().name()).append(Component.text(" | ")).append(Component.text(this.creature.getLevel())));
            meta.setUseDefaultBackground(true);
        });
        this.creature.addPassenger(hologram);
    }

    private void initializeListeners() {
        this.tameListener = EventListener.builder(EntityAttackEvent.class)
                .handler(event -> {
                    if (!(event.getEntity() instanceof final Player player)) {
                        return;
                    }
                    if (!this.creature.isTamed()) {
                        this.creature.damage(Damage.fromPlayer(player, 0));
                        this.timesHit++;
                        if (this.timesHit == creature.getLevel()) {
                            this.creature.setTamed(true);
                            player.sendMessage(Component.text("IT WAS TAMED!"));
                            player.getInventory().addItemStack(CreatureItemStack.toItem(this.creature));
                            this.creature.remove();
                        }
                    } else {
                        player.openInventory(new CreatureInventory(this.creature));
                    }
                })
                .filter(event ->
                        event.getTarget() == this.creature && event.getEntity() instanceof Player)
                .build();

        this.creatureInteractListener = EventListener.builder(PlayerEntityInteractEvent.class)
                .handler(event -> {
                    final Player player = event.getPlayer();
                    if (player.isSneaking()) {
                        player.getInventory().addItemStack(CreatureItemStack.toItem(this.creature));
                        this.creature.remove();
                    } else {
                        if (this.creature.getLeashHolder() != null) {
                            this.creature.setLeashHolder(null);
                            return;
                        }
                        this.creature.setLeashHolder(player);
                        final Set<Entity> leashed = player.getLeashedEntities();
                        if (leashed.size() == 2) {
                            leashed.forEach(entity -> {
                                if (!(entity instanceof final Creature creature)) {
                                    return;
                                }
                                if (creature == this.creature) {
                                    return;
                                }
                                if (this.creature.getSpecies().name().equals(creature.getSpecies().name()) && (this.creature.isMale() != creature.isMale())) {
                                    this.creature.setLeashHolder(creature);
                                    this.creature.scheduler().submitTask(() -> {
                                        if (creature.getTag(Creature.BREEDING_TIME_REMAINING) == 0) {
                                            final Creature baby = Creature.tamed(
                                                    this.creature.getSpecies(),
                                                    this.creature.getBreedTime(),
                                                    this.creature.getLevel() + 100,
                                                    this.creature.isMale(),
                                                    this.creature.getHealthStat(),
                                                    this.creature.getStamina(),
                                                    this.creature.getOxygen(),
                                                    this.creature.getFood(),
                                                    this.creature.getWeight(),
                                                    this.creature.getMelee(),
                                                    this.creature.getSpeed(),
                                                    this.creature.getAdditionalStats()
                                            );
                                            baby.getAttribute(Attribute.SCALE).setBaseValue(0.1);
                                            baby.setInstance(this.creature.getInstance(), this.creature.getPosition().withZ(z -> z - 2));
                                            creature.setLeashHolder(null);
                                            this.creature.setLeashHolder(null);
                                            return TaskSchedule.stop();
                                        }
                                        creature.updateTag(Creature.BREEDING_TIME_REMAINING, remaining -> remaining - 1);
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
                        event.getTarget() == this.creature && this.creature.isTamed() && event.getHand() == PlayerHand.MAIN)
                .build();
    }

    private void attachListeners() {
        this.creature.getInstance().eventNode().addListener(this.tameListener);
        this.creature.getInstance().eventNode().addListener(this.creatureInteractListener);
    }

    private void detachListeners() {
        this.creature.getInstance().eventNode().removeListener(this.tameListener);
        this.creature.getInstance().eventNode().removeListener(this.creatureInteractListener);
    }
}
