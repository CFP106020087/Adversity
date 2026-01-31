package com.adversity.client;

import com.adversity.network.PacketHandler;
import com.adversity.network.PacketOpenTalisman;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/**
 * Adversity 客户端快捷键绑定
 */
@SideOnly(Side.CLIENT)
public class AdversityKeyBindings {

    public static KeyBinding keyOpenTalisman;

    /**
     * 注册快捷键
     */
    public static void register() {
        keyOpenTalisman = new KeyBinding(
                "key.adversity.open_talisman",
                Keyboard.KEY_V,
                "key.categories.adversity");
        ClientRegistry.registerKeyBinding(keyOpenTalisman);
    }

    /**
     * 处理按键事件 - 使用 ClientTickEvent 兼容 1.12.2
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.currentScreen != null)
            return;

        if (keyOpenTalisman.isPressed()) {
            PacketHandler.INSTANCE.sendToServer(new PacketOpenTalisman());
        }
    }
}
