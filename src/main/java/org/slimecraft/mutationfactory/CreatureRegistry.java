package org.slimecraft.mutationfactory;

import net.minestom.server.entity.Player;
import net.minestom.server.entity.ai.EntityAIGroup;
import net.minestom.server.entity.ai.goal.MeleeAttackGoal;
import net.minestom.server.entity.ai.target.ClosestEntityTarget;
import net.minestom.server.entity.ai.target.LastEntityDamagerTarget;
import net.minestom.server.utils.time.TimeUnit;

import java.util.*;
import java.util.function.Supplier;

public class CreatureRegistry {
    private static final Map<String, Supplier<Creature>> CREATURES = new HashMap<>();
    private static final Random RANDOM = new Random();

    private static final Supplier<Creature> BULL = () -> Creature
            .wild(
                    Species.BULL,
                    new Stat(100),
                    new Stat(100),
                    new Stat(0.8F),
                    creature -> {
                        final EntityAIGroup aiGroup = new EntityAIGroup();
                        aiGroup.getGoalSelectors().add(new MeleeAttackGoal(creature, 0.5, 2, TimeUnit.SECOND));
                        aiGroup.getTargetSelectors().add(new LastEntityDamagerTarget(creature, 5));
                        aiGroup.getTargetSelectors().add(new ClosestEntityTarget(creature, 5, entity ->
                                entity instanceof Player || entity instanceof Creature target && !target.getSpecies().name().equals("Bull")));
                        creature.addAIGroup(aiGroup);
                    });

    private static final Supplier<Creature> JUMBUCK = () -> Creature
            .wild(
                    Species.JUMBUCK,
                    new Stat(100),
                    new Stat(100),
                    new Stat(1.2F),
                    creature -> {}
            );

    static {
        CREATURES.put("bull", BULL);
        CREATURES.put("sheep", JUMBUCK);
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
