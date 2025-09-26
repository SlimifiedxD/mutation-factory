package org.slimecraft.mutationfactory;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Creature extends EntityCreature {
    private final String speciesName;
    private final int level;
    private int timesHit;
    private boolean tamed;
    private EventListener<? extends @NotNull InstanceEvent> tameListener;
    private EventListener<? extends @NotNull InstanceEvent> creatureInteractListener;

    public Creature(EntityType entityType, String speciesName, Random random) {
        super(entityType);
        this.speciesName = speciesName;
        this.level = random.nextInt(Config.MIN_LEVEL, Config.MAX_LEVEL);
    }

    public Creature(EntityType entityType, String speciesName, int level) {
        super(entityType);
        this.speciesName = speciesName;
        this.level = level;
        this.tamed = true;
    }

    private void listeners() {
        this.tameListener = EventListener.builder(EntityAttackEvent.class)
                        .handler(event -> {
                            if (!(event.getEntity() instanceof final Player player)) {
                                return;
                            }
                            this.damage(Damage.fromPlayer(player, 0));
                            this.timesHit++;
                            if (this.timesHit == level) {
                                this.tamed = true;
                                player.sendMessage(Component.text("IT WAS TAMED!"));
                                player.getInventory().addItemStack(CreatureItemStack.toItem(this));
                                this.remove();
                            }
                        })
                        .filter(event ->
                                event.getTarget() == this && event.getEntity() instanceof Player)
                        .expireWhen(event ->
                                event.getTarget() == this && this.tamed)
                        .build();

        this.creatureInteractListener = EventListener.builder(PlayerEntityInteractEvent.class)
                .handler(event -> {
                    final Player player = event.getPlayer();
                    if (player.isSneaking()) {
                        player.getInventory().addItemStack(CreatureItemStack.toItem(this));
                        this.remove();
                    } else {
                        player.openInventory(new CreatureInventory(this));
                    }
                })
                .filter(event ->
                        event.getTarget() == this && this.tamed)
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
    public void remove() {
        this.removeListeners();
        this.getPassengers().forEach(entity -> {
            if (entity.getEntityType() != EntityType.TEXT_DISPLAY) {
                return;
            }
            entity.remove();
        });
        super.remove();
    }

    public String getSpeciesName() {
        return this.speciesName;
    }

    public int getLevel() {
        return this.level;
    }
}
