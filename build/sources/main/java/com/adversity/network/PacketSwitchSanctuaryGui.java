package com.adversity.network;

import com.adversity.Adversity;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 用于切换圣所GUI的网络包
 */
public class PacketSwitchSanctuaryGui implements IMessage {

    private BlockPos pos;
    private int targetGuiId;

    public PacketSwitchSanctuaryGui() {}

    public PacketSwitchSanctuaryGui(BlockPos pos, int targetGuiId) {
        this.pos = pos;
        this.targetGuiId = targetGuiId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        targetGuiId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeInt(targetGuiId);
    }

    public static class Handler implements IMessageHandler<PacketSwitchSanctuaryGui, IMessage> {
        @Override
        public IMessage onMessage(PacketSwitchSanctuaryGui message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                // 关闭当前GUI并打开目标GUI
                player.openGui(Adversity.instance, message.targetGuiId, player.world, 
                        message.pos.getX(), message.pos.getY(), message.pos.getZ());
            });
            return null;
        }
    }
}
