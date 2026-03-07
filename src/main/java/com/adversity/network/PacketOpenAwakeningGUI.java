package com.adversity.network;

import com.adversity.Adversity;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Server → Client: 開啟覺醒選擇 GUI
 *
 * 攜帶可選藥水列表、選擇數量、持續時間
 */
public class PacketOpenAwakeningGUI implements IMessage {

    private String[] potionIds;
    private int selectCount;
    private int durationTicks;

    public PacketOpenAwakeningGUI() {}

    public PacketOpenAwakeningGUI(String[] potionIds, int selectCount, int durationTicks) {
        this.potionIds = potionIds;
        this.selectCount = selectCount;
        this.durationTicks = durationTicks;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        selectCount = buf.readInt();
        durationTicks = buf.readInt();
        int count = buf.readInt();
        potionIds = new String[count];
        for (int i = 0; i < count; i++) {
            potionIds[i] = ByteBufUtils.readUTF8String(buf);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(selectCount);
        buf.writeInt(durationTicks);
        buf.writeInt(potionIds.length);
        for (String id : potionIds) {
            ByteBufUtils.writeUTF8String(buf, id);
        }
    }

    public String[] getPotionIds() { return potionIds; }
    public int getSelectCount() { return selectCount; }
    public int getDurationTicks() { return durationTicks; }

    public static class Handler implements IMessageHandler<PacketOpenAwakeningGUI, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketOpenAwakeningGUI message, MessageContext ctx) {
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
                net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(
                    new com.adversity.client.gui.GuiAwakeningSelection(
                        message.getPotionIds(),
                        message.getSelectCount(),
                        message.getDurationTicks()
                    )
                );
            });
            return null;
        }
    }
}
