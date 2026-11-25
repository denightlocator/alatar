package dev.atlas.client;

import dev.atlas.client.ui.AtlasScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Client entrypoint. The menu key is handled on a press edge so it cannot repeat while held. */
public final class AtlasClient implements ClientModInitializer {
    private boolean rightShiftWasDown;
    private long lastMenuToggleMillis;
    private static final long TOGGLE_DEBOUNCE_MILLIS = 350;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(Minecraft client) {
        long window = client.getWindow().handle();
        boolean rightShiftDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        boolean pressedThisTick = rightShiftDown && !rightShiftWasDown;
        rightShiftWasDown = rightShiftDown;
        long now = System.currentTimeMillis();
        if (pressedThisTick && now - lastMenuToggleMillis >= TOGGLE_DEBOUNCE_MILLIS) {
            lastMenuToggleMillis = now;
            if (client.screen instanceof AtlasScreen) {
                client.setScreen(null);
            } else if (client.screen == null) {
                client.setScreen(new AtlasScreen());
            }
        }
    }
}
