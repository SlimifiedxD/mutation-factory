package org.slimecraft.mutationfactory;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class CreatureItemStack {
    private static final Tag<@NotNull String> ENTITY_TYPE_TAG = Tag.String("type");
    private static final Tag<@NotNull String> SPECIES_NAME_TAG = Tag.String("species");
    private static final Tag<@NotNull Integer> LEVEL_TAG = Tag.Integer("level");
    private static final Tag<@NotNull Boolean> MALE_TAG = Tag.Boolean("male");
    private static final Tag<@NotNull Stat> HEALTH_TAG = Tag.Float("health").map(Stat::new, Stat::getCurrentValue);
    private static final Tag<@NotNull Stat> STAMINA_TAG = Tag.Float("stamina").map(Stat::new, Stat::getCurrentValue);
    private static final Tag<@NotNull Stat> OXYGEN_TAG = Tag.Float("oxygen").map(Stat::new, Stat::getCurrentValue);
    private static final Tag<@NotNull Stat> FOOD_TAG = Tag.Float("food").map(Stat::new, Stat::getCurrentValue);
    private static final Tag<@NotNull Stat> WEIGHT_TAG = Tag.Float("weight").map(Stat::new, Stat::getCurrentValue);
    private static final Tag<@NotNull Stat> MELEE_TAG = Tag.Float("melee").map(Stat::new, Stat::getCurrentValue);
    private static final Tag<@NotNull Stat> SPEED_TAG = Tag.Float("speed").map(Stat::new, Stat::getCurrentValue);

    private CreatureItemStack() {
    }

    public static ItemStack toItem(Creature creature) {
        Key.key(creature.getEntityType().key().asString() + "_spawn_egg");
        return ItemStack
                .builder(Material.fromKey(creature.getEntityType().key().asString() + "_spawn_egg"))
                .build()
                .with(DataComponents.MAX_STACK_SIZE, 1)
                .withTag(ENTITY_TYPE_TAG, creature.getEntityType().key().asString())
                .withTag(SPECIES_NAME_TAG, creature.getSpecies().name())
                .withTag(LEVEL_TAG, creature.getLevel())
                .withTag(MALE_TAG, creature.isMale())
                .withTag(Creature.BREEDING_TIME, creature.getBreedTime())
                .withTag(HEALTH_TAG, creature.getHealthStat())
                .withTag(STAMINA_TAG, creature.getStamina())
                .withTag(OXYGEN_TAG, creature.getOxygen())
                .withTag(FOOD_TAG, creature.getFood())
                .withTag(WEIGHT_TAG, creature.getWeight())
                .withTag(MELEE_TAG, creature.getMelee())
                .withTag(SPEED_TAG, creature.getSpeed())
                .withCustomName(Component.text(creature.getSpecies().name())
                        .color(NamedTextColor.AQUA)
                        .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
                .withLore(
                        Component.text("Level: ").append(Component.text(creature.getLevel()).color(NamedTextColor.YELLOW))
                                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                                .colorIfAbsent(NamedTextColor.WHITE),
                        Component.text("Gender: ")
                                .append(
                                        creature.isMale()
                                                ? Component.text("Male").color(TextColor.fromHexString("#31ddf7"))
                                                : Component.text("Female").color(TextColor.fromHexString("#ff2bf8")))
                                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                                .colorIfAbsent(NamedTextColor.WHITE));
    }

    public static Creature toCreature(ItemStack item) {
        return Creature
                .tamed(
                        new Species(EntityType.fromKey(item.getTag(ENTITY_TYPE_TAG)), item.getTag(SPECIES_NAME_TAG)),
                        item.getTag(Creature.BREEDING_TIME),
                        item.getTag(LEVEL_TAG),
                        item.getTag(MALE_TAG),
                        item.getTag(HEALTH_TAG),
                        item.getTag(STAMINA_TAG),
                        item.getTag(OXYGEN_TAG),
                        item.getTag(FOOD_TAG),
                        item.getTag(WEIGHT_TAG),
                        item.getTag(MELEE_TAG),
                        item.getTag(SPEED_TAG),
                        Collections.emptyList()
                );
    }

    public static boolean isCreatureItem(ItemStack item) {
        return item.hasTag(ENTITY_TYPE_TAG);
    }
}
