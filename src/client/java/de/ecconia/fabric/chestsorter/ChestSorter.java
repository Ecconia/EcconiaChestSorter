package de.ecconia.fabric.chestsorter;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class ChestSorter implements ClientModInitializer {
	public static KeyMapping keyBinding;

	@Override
	public void onInitializeClient() {
		keyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"ecconia-chest-sorter.translation.hot-key",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_R,
				KeyMapping.Category.register(Identifier.fromNamespaceAndPath("ecconia-chest-sorter", "main"))));
	}
}