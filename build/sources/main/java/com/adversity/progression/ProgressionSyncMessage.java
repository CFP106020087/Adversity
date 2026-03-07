package com.adversity.progression;

import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashSet;
import java.util.Set;

/**
 * 进度同步网络消息
 * 服务器 -> 客户端
 */
public class ProgressionSyncMessage implements IMessage {

    private Set<String> stages;
    private String newStage;

    public ProgressionSyncMessage() {
        this.stages = new HashSet<>();
        this.newStage = null;
    }

    public ProgressionSyncMessage(Set<String> stages, String newStage) {
        this.stages = new HashSet<>(stages);
        this.newStage = newStage;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        stages.clear();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            stages.add(ByteBufUtils.readUTF8String(buf));
        }
        if (buf.readBoolean()) {
            newStage = ByteBufUtils.readUTF8String(buf);
        } else {
            newStage = null;
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(stages.size());
        for (String stage : stages) {
            ByteBufUtils.writeUTF8String(buf, stage);
        }
        if (newStage != null) {
            buf.writeBoolean(true);
            ByteBufUtils.writeUTF8String(buf, newStage);
        } else {
            buf.writeBoolean(false);
        }
    }

    public static class Handler implements IMessageHandler<ProgressionSyncMessage, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(ProgressionSyncMessage message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().player;
                if (player != null) {
                    IAdversityCapability.IProgression cap = player.getCapability(
                            CapabilityHandler.PROGRESSION_CAPABILITY, null);
                    if (cap != null) {
                        cap.clear();
                        for (String stage : message.stages) {
                            cap.addStage(stage);
                        }
                    }

                    // 如果有新阶段，触发客户端提示和动画
                    if (message.newStage != null && !message.newStage.isEmpty()) {
                        com.adversity.client.gui.StageUnlockOverlay.triggerUnlock(message.newStage);
                    }
                }
            });
            return null;
        }
    }
}
