package com.adversity.network;

import com.adversity.Adversity;
import com.adversity.client.gui.AdversityGuiHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 客户端请求打开护符盒 GUI 的网络包
 */
public class PacketOpenTalisman implements IMessage {

    public PacketOpenTalisman() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        // 无数据
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // 无数据
    }

    public static class Handler implements IMessageHandler<PacketOpenTalisman, IMessage> {
        @Override
        public IMessage onMessage(PacketOpenTalisman message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                player.openGui(Adversity.instance, AdversityGuiHandler.GUI_TALISMAN,
                        player.world, (int) player.posX, (int) player.posY, (int) player.posZ);
            });
            return null;
        }
    }
}
