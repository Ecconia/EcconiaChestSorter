package de.ecconia.fabric.chestsorter;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ChestSorter implements ClientModInitializer
{
	public static KeyBinding keyBinding;
	
	@Override
	public void onInitializeClient()
	{
		keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"ecconia-chest-sorter.translation.hot-key",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_R,
				"ecconia-chest-sorter.category"
		));
	}
}
