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

    // TODO: FIX STATS BEING EMPTY
    public static Creature toCreature(ItemStack item) {
        return Creature
                .builder(
                        new Species(EntityType.fromKey(item.getTag(ENTITY_TYPE_TAG)), item.getTag(SPECIES_NAME_TAG)),
                        item.getTag(Creature.BREEDING_TIME),
                        Stat.EMPTY,
                        Stat.EMPTY,
                        Stat.EMPTY,
                        Stat.EMPTY,
                        Stat.EMPTY,
                        Stat.EMPTY,
                        Stat.EMPTY,
                        Collections.emptyList()
                )
                .level(item.getTag(LEVEL_TAG))
                .male(item.getTag(MALE_TAG))
                .tamed(true)
                .build();
    }

    public static boolean isCreatureItem(ItemStack item) {
        return item.hasTag(ENTITY_TYPE_TAG);
    }
}
