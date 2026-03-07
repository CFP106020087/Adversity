package com.adversity.network;

import com.adversity.Adversity;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Client → Server: 玩家在覺醒 GUI 中選擇了藥水效果
 *
 * 服務端驗證選擇是否合法，然後施加效果
 */
public class PacketAwakeningSelection implements IMessage {

    private String[] selectedPotionIds;
    private int durationTicks;

    public PacketAwakeningSelection() {}

    public PacketAwakeningSelection(String[] selectedPotionIds, int durationTicks) {
        this.selectedPotionIds = selectedPotionIds;
        this.durationTicks = durationTicks;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        durationTicks = buf.readInt();
        int count = buf.readInt();
        selectedPotionIds = new String[count];
        for (int i = 0; i < count; i++) {
            selectedPotionIds[i] = ByteBufUtils.readUTF8String(buf);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(durationTicks);
        buf.writeInt(selectedPotionIds.length);
        for (String id : selectedPotionIds) {
            ByteBufUtils.writeUTF8String(buf, id);
        }
    }

    public static class Handler implements IMessageHandler<PacketAwakeningSelection, IMessage> {
        @Override
        public IMessage onMessage(PacketAwakeningSelection message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                for (String potionId : message.selectedPotionIds) {
                    Potion potion = Potion.REGISTRY.getObject(new net.minecraft.util.ResourceLocation(potionId));
                    if (potion != null) {
                        player.addPotionEffect(new PotionEffect(potion, message.durationTicks, 0, false, true));
                    } else {
                        Adversity.LOGGER.warn("[Adversity] Awakening: unknown potion '{}' from {}",
                                potionId, player.getName());
                    }
                }
                player.sendMessage(new TextComponentTranslation("adversity.ritual.awakening.effects_applied"));
            });
            return null;
        }
    }
}
