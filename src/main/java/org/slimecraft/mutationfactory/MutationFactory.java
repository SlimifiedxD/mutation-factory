package org.slimecraft.mutationfactory;

import net.minestom.server.MinecraftServer;
import net.minestom.server.event.GlobalEventHandler;

public class MutationFactory {
    public static void main(String[] args) {
        final MinecraftServer minecraftServer = MinecraftServer.init();
        final GlobalEventHandler eventHandler = MinecraftServer.getGlobalEventHandler();
        new MapManager(eventHandler);

        minecraftServer.start("0.0.0.0", 25565);
    }
}
