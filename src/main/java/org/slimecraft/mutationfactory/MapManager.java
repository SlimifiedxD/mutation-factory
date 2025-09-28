package org.slimecraft.mutationfactory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.entity.ai.EntityAIGroup;
import net.minestom.server.entity.ai.goal.MeleeAttackGoal;
import net.minestom.server.entity.ai.target.ClosestEntityTarget;
import net.minestom.server.entity.ai.target.LastEntityDamagerTarget;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.scoreboard.TeamManager;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.time.TimeUnit;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public class MapManager {
    private final EventNode<Event> node;
    private final InstanceContainer rootInstance;
    private final Random random;

    public MapManager(EventNode<Event> node) {
        this.node = node;
        this.rootInstance = MinecraftServer.getInstanceManager().createInstanceContainer();
        this.random = new Random();
        this.configureRootInstance();
        this.setupPlayer();
        this.setupEvents();
    }

    private void setupEvents() {
        this.node.addListener(PlayerBlockInteractEvent.class, event -> {
            final MutationFactoryPlayer player = (MutationFactoryPlayer) event.getPlayer();
            final ItemStack item = player.getItemInMainHand();
            if (!CreatureItemStack.isCreatureItem(item)) {
                return;
            }
            final Creature creature = CreatureItemStack.toCreature(item);

            creature.setInstance(this.rootInstance, event.getBlockPosition().withY(y -> y + 1));

            player.addCreatureInSameInstance(creature);
            player.setItemInMainHand(ItemStack.AIR);
        });
        this.node.addListener(EntityAttackEvent.class, event -> {
            if (!(event.getEntity() instanceof final Creature creature)) {
                return;
            }
            if (!(event.getTarget() instanceof final LivingEntity livingEntity)) {
                return;
            }
            livingEntity.damage(Damage.fromEntity(creature, (float) creature.getAttribute(Attribute.ATTACK_DAMAGE).getBaseValue()));
        });
        this.node.addListener(PlayerDeathEvent.class, event -> {
            final Player player = event.getPlayer();
            final Damage source = player.getLastDamageSource();

            if (source == null) {
                event.setChatMessage(player.getName().append(Component.text(" was killed due to unforeseen events")));
                return;
            }
            if (source.getAttacker() instanceof final Creature creature) {
                event.setChatMessage(player.getName().append(Component.text(" was killed by a ").append(Component.text(creature.getSpecies().name()))));
            }
        });
    }

    private void configureRootInstance() {
        this.rootInstance.setGenerator(unit -> {
            unit.modifier().fillHeight(0, 50, Block.GRASS_BLOCK);
        });
        this.rootInstance.setChunkSupplier(LightingChunk::new);
        this.rootInstance.eventNode().addListener(PlayerSpawnEvent.class, event -> {
            final MutationFactoryPlayer player = (MutationFactoryPlayer) event.getPlayer();
            double interactionRange = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE).getBaseValue();

            this.rootInstance.scheduler().buildTask(() -> {
                        final Creature creature = CreatureRegistry.random();
                        final Pos pos = new Pos(this.random.nextInt(0, 50), 50, this.random.nextInt(0, 50));
                        this.rootInstance.loadChunk(pos).thenRun(() -> {
                            creature.setInstance(this.rootInstance, pos);
                        });
                    })
                    .repeat(TaskSchedule.seconds(1))
                    .schedule();

            final Team enemy = MinecraftServer.getTeamManager().createTeam("enemy");
            enemy.setTeamColor(NamedTextColor.RED);
            enemy.updateTeamColor(NamedTextColor.RED);
            this.rootInstance.scheduler().buildTask(() -> {
                        if (!player.isSneaking()) {
                            player.setTarget(null);
                            player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE).setBaseValue(interactionRange);
                            return;
                        }
                        final LivingEntity lookingAt = (LivingEntity) player.getLineOfSightEntity(75, entity -> {
                            if (entity instanceof final Creature creature) {
                                return !player.getCreaturesInSameInstance().contains(creature);
                            }
                            return entity instanceof LivingEntity;
                        });

                        if (lookingAt == null) {
                            player.setTarget(null);
                            player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE).setBaseValue(interactionRange);
                            return;
                        }

                        final Entity lastLookingAt = player.getTarget().orElse(null);

                        if (lastLookingAt != null && lastLookingAt != lookingAt && lastLookingAt.isGlowing()) {
                            lastLookingAt.setGlowing(false);
                            player.setTarget(null);
                            player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE).setBaseValue(interactionRange);
                            return;
                        }
                        player.setTarget(lookingAt);

                        lookingAt.setTeam(enemy);
                        lookingAt.setGlowing(true);
                        player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE).setBaseValue(10000);
                        System.out.println(player.getTarget());
                    })
                    .repeat(TaskSchedule.seconds(1))
                    .schedule();
        });
        this.rootInstance.eventNode().addListener(PlayerEntityInteractEvent.class, event -> {
            if (event.getHand() == PlayerHand.OFF) return;
            final MutationFactoryPlayer player = (MutationFactoryPlayer) event.getPlayer();

            player.getCreaturesInSameInstance().forEach(creature -> {
                creature.initializeAi();
                final EntityAIGroup aiGroup = new EntityAIGroup();
                aiGroup.getGoalSelectors().add(new MeleeAttackGoal(creature, 0.5, 10, TimeUnit.SERVER_TICK));
                aiGroup.getTargetSelectors().add(new LastEntityDamagerTarget(creature, 15));
                aiGroup.getTargetSelectors().add(new ClosestEntityTarget(creature, 15, entity -> {
                    return player.getTarget().isPresent() && player.getTarget().get() == entity;
                }));
                creature.addAIGroup(aiGroup);
            });
        });
    }

    private void setupPlayer() {
        this.node.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(this.rootInstance);
            event.getPlayer().setRespawnPoint(new Pos(0, 50, 0));
        });
    }
}
