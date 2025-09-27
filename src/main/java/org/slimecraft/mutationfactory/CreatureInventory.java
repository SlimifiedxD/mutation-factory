package org.slimecraft.mutationfactory;

import net.kyori.adventure.text.Component;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;

public class CreatureInventory extends Inventory {
    public CreatureInventory(Creature creature) {
        super(InventoryType.CHEST_6_ROW, Component.text(creature.getSpecies().name()));
    }
}
