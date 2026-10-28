package dev.nova;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class NovaClient implements ClientModInitializer {
    public static boolean guiEnabled = false;
    private static KeyBinding openGuiKey;
    private static KeyBinding toggleFreecamKey;

    @Override
    public void onInitializeClient() {
        StorageFinderRenderer.register();
        FreecamController.register();

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nova.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyBinding.Category.MISC
        ));

        toggleFreecamKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nova.toggle_freecam",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F6,
                KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.player == null) {
                    continue;
                }

                if (client.currentScreen instanceof NovaScreen) {
                    client.setScreen(null);
                    guiEnabled = false;
                } else {
                    guiEnabled = true;
                    client.setScreen(new NovaScreen());
                }
            }

            while (toggleFreecamKey.wasPressed()) {
                FreecamController.toggle(client);
            }
        });
    }
}
