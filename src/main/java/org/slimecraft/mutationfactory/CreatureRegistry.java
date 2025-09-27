package org.slimecraft.mutationfactory;

import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.ai.EntityAIGroup;
import net.minestom.server.entity.ai.goal.MeleeAttackGoal;
import net.minestom.server.entity.ai.target.ClosestEntityTarget;
import net.minestom.server.entity.ai.target.LastEntityDamagerTarget;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.utils.time.TimeUnit;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

public class CreatureRegistry {
    private static final Map<String, Supplier<Creature>> CREATURES = new HashMap<>();
    private static final Random RANDOM = new Random();

    private static final Supplier<Creature> BULL = () -> new Creature(EntityType.COW, "Bull", null, null, null, 10, 10.5F, creature -> {
        final EntityAIGroup aiGroup = new EntityAIGroup();
        aiGroup.getGoalSelectors().add(new MeleeAttackGoal(creature, 0.5, 2, TimeUnit.SECOND));
        aiGroup.getTargetSelectors().add(new LastEntityDamagerTarget(creature, 5));
        aiGroup.getTargetSelectors().add(new ClosestEntityTarget(creature, 5, entity ->
                entity instanceof Player || entity instanceof Creature target && !target.getSpeciesName().equals("Bull")));
        creature.addAIGroup(aiGroup);
    });

    private static final Supplier<Creature> SHEEP = () -> new Creature(EntityType.SHEEP, "Sheep", null, null, null, 30, 0, creature -> {
        creature.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(1.2);
    });

    static {
        CREATURES.put("bull", BULL);
        CREATURES.put("sheep", SHEEP);
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
