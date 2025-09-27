package org.slimecraft.mutationfactory;

import net.minestom.server.entity.EntityType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

public class CreatureRegistry {
    private static final Map<String, Supplier<Creature>> CREATURES = new HashMap<>();
    private static final Random RANDOM = new Random();

    static {
        CREATURES.put("cow", () -> new Creature(EntityType.COW, "Cow", RANDOM, 10));
        CREATURES.put("sheep", () -> new Creature(EntityType.SHEEP, "Sheep", RANDOM, 30));
    }

    public static Creature of(String identifier) {
        return CREATURES.get(identifier).get();
    }

    public static Creature random() {
        final String[] keys = CREATURES.keySet().toArray(new String[]{});
        final int randomIndex = RANDOM.nextInt(keys.length);
        return CREATURES.get(keys[randomIndex]).get();
    }
}
