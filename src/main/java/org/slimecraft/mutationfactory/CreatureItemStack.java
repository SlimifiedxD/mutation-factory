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

import java.util.Random;

public class CreatureItemStack {
    private static final Tag<@NotNull String> TYPE_TAG = Tag.String("type");
    private static final Tag<@NotNull String> SPECIES_TAG = Tag.String("species");
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
                .withTag(TYPE_TAG, creature.getEntityType().key().asString())
                .withTag(SPECIES_TAG, creature.getSpeciesName())
                .withTag(LEVEL_TAG, creature.getLevel())
                .withTag(MALE_TAG, creature.isMale())
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
        return new Creature(EntityType.fromKey(item.getTag(TYPE_TAG)), item.getTag(SPECIES_TAG), item.getTag(LEVEL_TAG), item.getTag(MALE_TAG));
    }

    public static boolean isCreatureItem(ItemStack item) {
        return item.hasTag(TYPE_TAG);
    }
}
