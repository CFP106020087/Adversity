package com.adversity.network;

import com.adversity.client.gui.SealedSlotOverlayRenderer;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Packet for syncing permanent curse data to client
 */
public class PacketSyncCurse implements IMessage {

    private int sealedSlots;
    private float attackReduction;
    private float healthReduction;

    public PacketSyncCurse() {}

    public PacketSyncCurse(int sealedSlots, float attackReduction, float healthReduction) {
        this.sealedSlots = sealedSlots;
        this.attackReduction = attackReduction;
        this.healthReduction = healthReduction;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.sealedSlots = buf.readInt();
        this.attackReduction = buf.readFloat();
        this.healthReduction = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(sealedSlots);
        buf.writeFloat(attackReduction);
        buf.writeFloat(healthReduction);
    }

    public static class Handler implements IMessageHandler<PacketSyncCurse, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketSyncCurse message, MessageContext ctx) {
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
                SealedSlotOverlayRenderer.ClientCurseCache.update(
                    message.sealedSlots,
                    message.attackReduction,
                    message.healthReduction
                );
            });
            return null;
        }
    }
}
