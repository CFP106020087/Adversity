package com.adversity.network;

import com.adversity.client.visual.VisualEffectManager;
import com.adversity.client.visual.VisualEffectType;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 视觉效果同步数据包
 * 从服务端发送到客户端，用于触发屏幕视觉效果
 */
public class PacketVisualEffect implements IMessage {

    private int effectTypeId;
    private int duration;
    private float intensity;
    private int sourceEntityId;
    private boolean remove;  // true = 移除效果, false = 添加效果

    public PacketVisualEffect() {
    }

    /**
     * 添加效果
     */
    public PacketVisualEffect(VisualEffectType type, int duration, float intensity, int sourceEntityId) {
        this.effectTypeId = type.getId();
        this.duration = duration;
        this.intensity = intensity;
        this.sourceEntityId = sourceEntityId;
        this.remove = false;
    }

    /**
     * 移除效果
     */
    public PacketVisualEffect(VisualEffectType type, boolean remove) {
        this.effectTypeId = type.getId();
        this.duration = 0;
        this.intensity = 0;
        this.sourceEntityId = -1;
        this.remove = true;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        effectTypeId = buf.readInt();
        duration = buf.readInt();
        intensity = buf.readFloat();
        sourceEntityId = buf.readInt();
        remove = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(effectTypeId);
        buf.writeInt(duration);
        buf.writeFloat(intensity);
        buf.writeInt(sourceEntityId);
        buf.writeBoolean(remove);
    }

    /**
     * 客户端消息处理器
     */
    public static class Handler implements IMessageHandler<PacketVisualEffect, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketVisualEffect message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> handleMessage(message));
            return null;
        }

        @SideOnly(Side.CLIENT)
        private void handleMessage(PacketVisualEffect message) {
            VisualEffectType type = VisualEffectType.fromId(message.effectTypeId);
            if (type == null) {
                return;
            }

            if (message.remove) {
                VisualEffectManager.removeEffect(type);
            } else {
                VisualEffectManager.addEffect(
                    type,
                    message.duration,
                    message.intensity,
                    message.sourceEntityId
                );
            }
        }
    }
}
