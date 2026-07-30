package de.ecconia.fabric.chestsorter;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SlotItemStack implements Comparable<SlotItemStack> {
	public Slot currentSlot;
	public ItemStack item;
	public int id;

	public SlotItemStack(Slot slot) {
		currentSlot = slot;
		item = slot.getItem();
		id = Item.getId(item.getItem());
	}

	@Override
	public int compareTo(@NotNull SlotItemStack o) {
		return Integer.compare(this.id, o.id);
	}
}