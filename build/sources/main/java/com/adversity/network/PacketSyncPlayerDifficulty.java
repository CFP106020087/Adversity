package com.adversity.network;

import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IPlayerDifficulty;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 同步玩家难度设置数据包
 * 从服务端发送到客户端，用于HUD显示正确的玩家倍率
 */
public class PacketSyncPlayerDifficulty implements IMessage {

    private float difficultyMultiplier;
    private boolean difficultyDisabled;

    public PacketSyncPlayerDifficulty() {}

    public PacketSyncPlayerDifficulty(IPlayerDifficulty cap) {
        this.difficultyMultiplier = cap.getDifficultyMultiplier();
        this.difficultyDisabled = cap.isDifficultyDisabled();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        difficultyMultiplier = buf.readFloat();
        difficultyDisabled = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeFloat(difficultyMultiplier);
        buf.writeBoolean(difficultyDisabled);
    }

    /**
     * 客户端消息处理器
     */
    public static class Handler implements IMessageHandler<PacketSyncPlayerDifficulty, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketSyncPlayerDifficulty message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> handleMessage(message));
            return null;
        }

        @SideOnly(Side.CLIENT)
        private void handleMessage(PacketSyncPlayerDifficulty message) {
            EntityPlayer player = Minecraft.getMinecraft().player;
            if (player == null) return;
            
            IPlayerDifficulty cap = CapabilityHandler.getPlayerDifficulty(player);
            if (cap == null) return;
            
            cap.setDifficultyMultiplier(message.difficultyMultiplier);
            cap.setDifficultyDisabled(message.difficultyDisabled);
        }
    }
}
