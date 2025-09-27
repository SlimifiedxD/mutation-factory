package org.slimecraft.mutationfactory;

import net.minestom.server.entity.EntityType;

public record Species(EntityType entityType, String name) {
    public static final Species BULL = new Species(EntityType.COW, "Bull");
    public static final Species JUMBUCK = new Species(EntityType.SHEEP, "Jumbuck");
    public static final Species SCAVENGER = new Species(EntityType.PHANTOM, "Scavenger");
}
