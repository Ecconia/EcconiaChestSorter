package de.ecconia.fabric.chestsorter.mixin.client;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShulkerBoxMenu.class)
public interface ShulkerInventoryMixin {
	@Accessor("container")
	Container getContainer();
}