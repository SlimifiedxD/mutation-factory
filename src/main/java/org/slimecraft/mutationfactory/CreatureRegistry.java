package org.slimecraft.mutationfactory;

import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.ai.EntityAIGroup;
import net.minestom.server.entity.ai.goal.MeleeAttackGoal;
import net.minestom.server.entity.ai.target.ClosestEntityTarget;
import net.minestom.server.entity.ai.target.LastEntityDamagerTarget;
import net.minestom.server.utils.time.TimeUnit;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

public class CreatureRegistry {
    private static final Map<String, Supplier<Creature>> CREATURES = new HashMap<>();
    private static final Random RANDOM = new Random();

    static {
        CREATURES.put("bull", () -> new Creature(EntityType.COW, "Bull", RANDOM, 10, 10.5F, creature -> {
            final EntityAIGroup aiGroup = new EntityAIGroup();
            aiGroup.getGoalSelectors().add(new MeleeAttackGoal(creature, 1.6, 2, TimeUnit.SECOND));
            aiGroup.getTargetSelectors().add(new LastEntityDamagerTarget(creature, 32));
            aiGroup.getTargetSelectors().add(new ClosestEntityTarget(creature, 32, entity -> entity instanceof Player));

            return aiGroup;
        }));
        CREATURES.put("sheep", () -> new Creature(EntityType.SHEEP, "Sheep", RANDOM, 30, 0F, creature -> {
            return new EntityAIGroup();
        }));
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
