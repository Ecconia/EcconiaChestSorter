package de.ecconia.fabric.chestsorter.mixin.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ecconia.fabric.chestsorter.ChestSorter;
import de.ecconia.fabric.chestsorter.SlotItemStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen
{
	@Shadow
	protected abstract void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType);
	
	protected HandledScreenMixin(Text title)
	{
		super(title);
	}
	
	@Inject(at = @At("HEAD"), method = "keyPressed", cancellable = true)
	private void chestSorter$keyPressed(KeyInput input, CallbackInfoReturnable<Boolean> callbackInfoReturnable)
	{
		if(client == null || client.player == null || client.interactionManager == null)
		{
			return; //Precaution, just stop here.
		}
		if(!ChestSorter.keyBinding.matchesKey(input))
		{
			return; //Keybinding not pressed, nothing to do.
		}
		ClientPlayerEntity player = client.player;
		ScreenHandler screenHandler = player.currentScreenHandler; //The screen handler of the current open window.
		if(screenHandler == null || !screenHandler.canUse(player))
		{
			return; //Not a relevant screen handler, skip it.
		}
		if(!screenHandler.getCursorStack().isEmpty())
		{
			return; //Player is holding an item in the hand. This would break the sorting algorithm. It relies on the hand being free.
		}
		
		//Grab the right inventory, depending on the screen type:
		Inventory inventory;
		//Set start and end slot. By default, all slots of an inventory will be sorted.
		int start = 0; //First slot (lower)
		int end = -1; //Last slot (higher) Exclusive!
		if(screenHandler == player.playerScreenHandler)
		{
			inventory = player.getInventory(); //Not used, but let's set it in case.
			//The player inventory has multiple inventory sections, only sort the 3 main rows:
			start = PlayerScreenHandler.INVENTORY_START;
			end = PlayerScreenHandler.INVENTORY_END;
		}
		else if(screenHandler instanceof GenericContainerScreenHandler)
		{
			inventory = ((GenericContainerScreenHandler) screenHandler).getInventory();
		}
		else if(screenHandler instanceof ShulkerBoxScreenHandler)
		{
			inventory = ((ShulkerInventoryMixin) screenHandler).getInventory();
		}
		else
		{
			return; //No inventory here, or unsupported inventory.
		}
		//The end index was not overwritten, hence the whole inventory has to be sorted:
		if(end < 0)
		{
			end = inventory.size();
		}
		
		//The sorting code uses API code, that might not be fully safe. Let's be safe and try to catch anything:
		try
		{
			sort(screenHandler, screenHandler.slots, start, end);
		}
		catch(Throwable t)
		{
			t.printStackTrace();
			client.player.sendMessage(Text.literal("$aError while sorting chest."), false);
		}
		
		//Prevent further processing of this input event, it is consumed by this mod:
		callbackInfoReturnable.setReturnValue(true);
	}
	
	@Unique
	private void sort(ScreenHandler handler, List<Slot> slots, int startIndex, int endIndex)
	{
		//Collect all items that are currently in the relevant inventory slots:
		List<SlotItemStack> items = new ArrayList<>();
		for(int i = startIndex; i < endIndex; i++)
		{
			Slot slot = slots.get(i);
			if(slot.hasStack())
			{
				items.add(new SlotItemStack(slot));
			}
		}
		
		//Sort the list of present items:
		Collections.sort(items);
		
		Slot slotLastInsertedTo = null; //Stores the last filled slot, to add more items onto it, if possible.
		int nextSlotIndex = startIndex; //The inventory is filled from the beginning to the last slot. This is the index of the next slot to fill.
		//Iterate over the sorted items, so that they will be in the correct order:
		for(int i = 0; i < items.size(); i++)
		{
			SlotItemStack toBeSorted = items.get(i);
			boolean didNotYetTakeFromSlotToTakeFrom = true; //The item might be picked up at multiple points in code, this stores if it still has to be picked up.
			Slot slotToTakeFrom = toBeSorted.currentSlot;
			
			//Try to add more items onto the last slot:
			if(slotLastInsertedTo != null)
			{
				ItemStack lastItemStack = slotLastInsertedTo.getStack();
				ItemStack newItemStack = slotToTakeFrom.getStack();
				//Check if the items can be inserted onto the last slot:
				if(ItemStack.areItemsAndComponentsEqual(lastItemStack, newItemStack)
					&& lastItemStack.getCount() <= lastItemStack.getMaxCount())
				{
					click(slotToTakeFrom);
					click(slotLastInsertedTo);
					if(handler.getCursorStack().isEmpty())
					{
						continue; //All items dumped onto the last slot, continue without using a new slot.
					}
					didNotYetTakeFromSlotToTakeFrom = false;
				}
			}
			
			//Sort into a new target slot:
			Slot slotToInsertInto = slots.get(nextSlotIndex++);
			slotLastInsertedTo = slotToInsertInto; //Remember the last slot, for trying to add the next item stack onto it.
			//Previous stack merging code might have already picked up the item, if not pick it up now:
			if(didNotYetTakeFromSlotToTakeFrom)
			{
				click(slotToTakeFrom);
			}
			click(slotToInsertInto);
			
			/*
				At this point, there might still be items in the hand. This is because either:
				 - The slot to insert into had a different item.
					Which means, that the whole target slot is now in the hand and must be moved somewhere else.
				 - The slot to insert into had the same item, but not enough space.
					Which means, we might still hold the same stack with fewer items,
					 but we can pretend we are now holding the stack that was in the target slot.
					 As the item count does not matter for this algorithm (when not merging stacks).
				
				In either case the items we took will be inserted back into the slot they got taken from.
				As per sort algorithm design, we never insert into or take from a slot that is already done being sorted.
				 The only exception is when the last slot is reused, but at this point the hand contains items that are from an unsorted slot.
				That is why it is possible to put back the item, as that is also an unsorted slot.
				However, in that case, we are changing the slot of a to-be-sorted item stack -> it must be updated.
			 */
			if(!handler.getCursorStack().isEmpty())
			{
				//Put leftover items into the slot of the original to-be-sorted item, as that slot is reliably empty right now:
				click(slotToTakeFrom);
				//Find the to-be-sorted item stack, that was just relocated in the list of to-be-sorted item stacks and update its slot index to the new slot:
				for(int j = i + 1; j < items.size(); j++)
				{
					//Skip all items that are already sorted (start at 'i'), including the one for this run ('+1').
					// Skipped items must be skipped, as they might cause the following condition to become true,
					//  then the optimization of early breaking does not work anymore.
					var itemToStillBeSorted = items.get(j);
					if(itemToStillBeSorted.currentSlot == slotToInsertInto)
					{
						itemToStillBeSorted.currentSlot = slotToTakeFrom;
						break; //Found the correct slot.
					}
				}
			}
		}
	}
	
	@Unique
	private void click(Slot slot)
	{
		onMouseClick(slot, 0, 0, SlotActionType.PICKUP);
	}
}
