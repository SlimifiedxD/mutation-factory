package org.slimecraft.mutationfactory;

import net.minestom.server.entity.EntityType;
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
                    new Species(EntityType.COW, "Bull"),
                    30,
                    new Stat(100, 100F),
                    new Stat(100, 300F),
                    Stat.EMPTY,
                    Stat.EMPTY,
                    Stat.EMPTY,
                    new Stat(20, 20F),
                    new Stat(0.4F, 1),
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
                    new Species(EntityType.SHEEP, "Jumbuck"),
                    10,
                    new Stat(20, 50F),
                    new Stat(20, 150F),
                    Stat.EMPTY,
                    Stat.EMPTY,
                    Stat.EMPTY,
                    new Stat(20, 13F),
                    new Stat(0.6F, 1)
            );

    private static final Supplier<Creature> SCAVENGER = () -> Creature
            .wild(
                    new Species(EntityType.PHANTOM, "Scavenger"),
                    60,
                    new Stat(20, 50F),
                    new Stat(20, 150F),
                    Stat.EMPTY,
                    Stat.EMPTY,
                    Stat.EMPTY,
                    new Stat(20, 13F),
                    new Stat(0.7F, 1),
                    creature -> {
                        final EntityAIGroup aiGroup = new EntityAIGroup();
                        aiGroup.getGoalSelectors().add(new MeleeAttackGoal(creature, 0.5, 10, TimeUnit.SERVER_TICK));
                        aiGroup.getTargetSelectors().add(new LastEntityDamagerTarget(creature, 15));
                        aiGroup.getTargetSelectors().add(new ClosestEntityTarget(creature, 15, entity ->
                                entity instanceof Player || entity instanceof Creature target && !target.getSpecies().equals(creature.getSpecies())));
                        creature.addAIGroup(aiGroup);
                    }
            );

    static {
        CREATURES.put("bull", BULL);
        CREATURES.put("sheep", JUMBUCK);
        CREATURES.put("scavenger", SCAVENGER);
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
