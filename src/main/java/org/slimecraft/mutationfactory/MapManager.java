package org.slimecraft.mutationfactory;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.timer.TaskSchedule;

import java.util.Random;

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
                    final Player player = event.getPlayer();
                    final ItemStack item = player.getItemInMainHand();
                    if (!CreatureItemStack.isCreatureItem(item)) {
                        return;
                    }
                    final Creature creature = CreatureItemStack.toCreature(item);

                    creature.setInstance(this.rootInstance, event.getBlockPosition().withY(y -> y + 1));
                    player.setItemInMainHand(ItemStack.AIR);
                });
    }

    private void configureRootInstance() {
        this.rootInstance.setGenerator(unit -> {
            unit.modifier().fillHeight(0, 50, Block.GRASS_BLOCK);
        });
        this.rootInstance.setChunkSupplier(LightingChunk::new);
        MinecraftServer.getSchedulerManager().buildTask(() -> {
            final Creature creature = CreatureRegistry.random();
            creature.setInstance(this.rootInstance, new Pos(this.random.nextInt(0, 50), 50, this.random.nextInt(0, 50)));
        })
                .repeat(TaskSchedule.seconds(1))
                .schedule();
    }

    private void setupPlayer() {
        this.node.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(this.rootInstance);
            event.getPlayer().setRespawnPoint(new Pos(0, 50, 0));
        });
    }
}
