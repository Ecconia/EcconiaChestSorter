package de.ecconia.fabric.chestsorter;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.NotNull;

public class SlotItemStack implements Comparable<SlotItemStack>
{
	public Slot currentSlot;
	public ItemStack item;
	public int id;
	
	public SlotItemStack(Slot slot)
	{
		currentSlot = slot;
		item = slot.getStack();
		id = Item.getRawId(item.getItem());
	}
	
	@Override
	public int compareTo(@NotNull SlotItemStack o)
	{
		return id - o.id;
	}
}
