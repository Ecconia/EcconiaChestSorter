package de.ecconia.fabric.chestsorter.mixin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ecconia.fabric.chestsorter.ChestSorter;
import de.ecconia.fabric.chestsorter.SlotItemStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
	private void chestSorter$keyPressed(int keycode, int scancode, int modifiers, CallbackInfoReturnable<Boolean> callbackInfoReturnable)
	{
		if(client == null || client.player == null || client.interactionManager == null)
		{
			return;
		}
		if(ChestSorter.keyBinding.matchesKey(keycode, scancode))
		{
			ClientPlayerEntity player = client.player;
			ScreenHandler screenHandler = player.currentScreenHandler;
			if(screenHandler != null && screenHandler != player.playerScreenHandler && screenHandler.canUse(player))
			{
				Inventory inventory;
				if(screenHandler instanceof GenericContainerScreenHandler)
				{
					inventory = ((GenericContainerScreenHandler) screenHandler).getInventory();
				}
				else if(screenHandler instanceof ShulkerBoxScreenHandler)
				{
					inventory = ((ShulkerInventoryMixin) screenHandler).getInventory();
				}
				else
				{
					return;
				}
				
				if(screenHandler.getCursorStack().isEmpty())
				{
					try
					{
						sort(screenHandler, inventory, screenHandler.slots);
					}
					catch(Throwable t)
					{
						t.printStackTrace();
						client.player.sendMessage(Text.literal("$aError while sorting chest."));
					}
					
					callbackInfoReturnable.setReturnValue(true); //Declare as handled.
				}
			}
		}
	}
	
	private void sort(ScreenHandler handler, Inventory inventory, List<Slot> slots)
	{
		List<SlotItemStack> items = new ArrayList<>();
		for(int i = 0; i < inventory.size(); i++)
		{
			Slot slot = slots.get(i);
			if(slot.hasStack())
			{
				items.add(new SlotItemStack(slot));
			}
		}
		
		Collections.sort(items);
		
		Slot slotLastInsertedTo = null;
		int nextSlotIndex = 0;
		for(SlotItemStack toBeSorted : items)
		{
			boolean didNotYetTakeFromSlotToTakeFrom = true;
			Slot slotToTakeFrom = toBeSorted.currentSlot;
			
			//Check if we can insert into the last target slot, else skip:
			if(slotLastInsertedTo != null)
			{
				ItemStack lastItemStack = slotLastInsertedTo.getStack();
				ItemStack newItemStack = slotToTakeFrom.getStack();
				if(
						ItemStack.canCombine(lastItemStack, newItemStack) &&
						lastItemStack.getCount() <= lastItemStack.getMaxCount()
				)
				{
					click(slotToTakeFrom);
					click(slotLastInsertedTo);
					if(handler.getCursorStack().isEmpty())
					{
						continue;
					}
					didNotYetTakeFromSlotToTakeFrom = false;
				}
			}
			
			//Sort into a new target slot:
			Slot slotToInsertInto = slots.get(nextSlotIndex++);
			slotLastInsertedTo = slotToInsertInto;
			if(didNotYetTakeFromSlotToTakeFrom)
			{
				click(slotToTakeFrom);
			}
			click(slotToInsertInto);
			
			//If we have items left over, because either:
			// - The slot to insert into had some different item
			// - The slot to insert from had the same item but it is left over
			//The we want to put it back to where we got it from,
			// but either way, we meddled with a slot that is not yet sorted correctly.
			//Due to moving of that stack its slot must be updated, since it will be processed later.
			if(!handler.getCursorStack().isEmpty())
			{
				//Insert leftovers to the slot that we emptied earlier:
				click(slotToTakeFrom);
				//Find original slot reference and replace it:
				for(SlotItemStack sis : items)
				{
					if(sis.currentSlot == slotToInsertInto)
					{
						sis.currentSlot = slotToTakeFrom;
					}
				}
			}
		}
	}
	
	private void click(Slot slot)
	{
		onMouseClick(slot, 0, 0, SlotActionType.PICKUP);
	}
}
