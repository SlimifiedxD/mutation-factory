package org.slimecraft.mutationfactory;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MutationFactoryPlayer extends Player {
    private final List<Creature> creaturesInSameInstance;
    private Entity target;

    public MutationFactoryPlayer(@NotNull PlayerConnection playerConnection, @NotNull GameProfile gameProfile) {
        super(playerConnection, gameProfile);
        this.creaturesInSameInstance = new ArrayList<>();
    }

    public void addCreatureInSameInstance(Creature creature) {
        this.creaturesInSameInstance.add(creature);
    }

    public void removeCreatureInSameInstance(Creature creature) {
        this.creaturesInSameInstance.remove(creature);
    }


    public List<Creature> getCreaturesInSameInstance() {
        return this.creaturesInSameInstance;
    }

    public Optional<Entity> getTarget() {
        return Optional.ofNullable(target);
    }

    public void setTarget(Entity target) {
        this.target = target;
    }
}
