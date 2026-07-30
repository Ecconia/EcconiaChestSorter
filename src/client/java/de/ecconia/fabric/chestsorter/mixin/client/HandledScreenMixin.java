package de.ecconia.fabric.chestsorter.mixin.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ecconia.fabric.chestsorter.ChestSorter;
import de.ecconia.fabric.chestsorter.SlotItemStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin extends Screen {

	@Shadow
	protected abstract void slotClicked(Slot slot, int slotId, int mouseButton, ContainerInput clickType);

	protected HandledScreenMixin(Component title) {
		super(title);
	}

	@Inject(at = @At("HEAD"), method = "keyPressed", cancellable = true)
	private void chestSorter$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
		if (this.minecraft == null || this.minecraft.player == null || this.minecraft.gameMode == null) {
			return; // Precaution, just stop here.
		}
		if (!ChestSorter.keyBinding.matches(event)) {
			return; // Keybinding not pressed, nothing to do.
		}

		LocalPlayer player = this.minecraft.player;
		AbstractContainerMenu containerMenu = player.containerMenu; // Current open window menu
		if (containerMenu == null || !containerMenu.stillValid(player)) {
			return; // Not a relevant menu, skip it.
		}
		if (!containerMenu.getCarried().isEmpty()) {
			return; // Player is holding an item in the hand.
		}

		// Grab the right container, depending on the screen type:
		Container container;
		int start = 0; // First slot
		int end = -1; // Last slot (Exclusive)

		if (containerMenu == player.inventoryMenu) {
			container = player.getInventory();
			start = InventoryMenu.INV_SLOT_START;
			end = InventoryMenu.INV_SLOT_END;
		} else if (containerMenu instanceof ChestMenu chestMenu) {
			container = chestMenu.getContainer();
		} else if (containerMenu instanceof ShulkerBoxMenu shulkerBoxMenu) {
			container = ((ShulkerInventoryMixin) shulkerBoxMenu).getContainer();
		} else {
			return; // No container here, or unsupported screen.
		}

		if (end < 0) {
			end = container.getContainerSize();
		}

		try {
			sort(containerMenu, containerMenu.slots, start, end);
		} catch (Throwable t) {
			t.printStackTrace();
			player.sendSystemMessage(Component.literal("§aError while sorting chest."));
		}

		callbackInfoReturnable.setReturnValue(true);
	}

	@Unique
	private void sort(AbstractContainerMenu menu, List<Slot> slots, int startIndex, int endIndex) {
		List<SlotItemStack> items = new ArrayList<>();
		for (int i = startIndex; i < endIndex; i++) {
			Slot slot = slots.get(i);
			if (slot.hasItem()) {
				items.add(new SlotItemStack(slot));
			}
		}

		Collections.sort(items);

		Slot slotLastInsertedTo = null;
		int nextSlotIndex = startIndex;

		for (int i = 0; i < items.size(); i++) {
			SlotItemStack toBeSorted = items.get(i);
			boolean didNotYetTakeFromSlotToTakeFrom = true;
			Slot slotToTakeFrom = toBeSorted.currentSlot;

			if (slotLastInsertedTo != null) {
				ItemStack lastItemStack = slotLastInsertedTo.getItem();
				ItemStack newItemStack = slotToTakeFrom.getItem();

				if (ItemStack.isSameItemSameComponents(lastItemStack, newItemStack)
						&& lastItemStack.getCount() <= lastItemStack.getMaxStackSize()) {
					click(slotToTakeFrom);
					click(slotLastInsertedTo);
					if (menu.getCarried().isEmpty()) {
						continue;
					}
					didNotYetTakeFromSlotToTakeFrom = false;
				}
			}

			Slot slotToInsertInto = slots.get(nextSlotIndex++);
			slotLastInsertedTo = slotToInsertInto;

			if (didNotYetTakeFromSlotToTakeFrom) {
				click(slotToTakeFrom);
			}
			click(slotToInsertInto);

			if (!menu.getCarried().isEmpty()) {
				click(slotToTakeFrom);
				for (int j = i + 1; j < items.size(); j++) {
					var itemToStillBeSorted = items.get(j);
					if (itemToStillBeSorted.currentSlot == slotToInsertInto) {
						itemToStillBeSorted.currentSlot = slotToTakeFrom;
						break;
					}
				}
			}
		}
	}

	@Unique
	private void click(Slot slot) {
		slotClicked(slot, 0, 0, ContainerInput.PICKUP);
	}
}