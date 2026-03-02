package com.adversity.network;

import com.adversity.sanctuary.SanctuaryManager;
import com.adversity.sanctuary.TileEntitySanctuary;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 圣所传送请求包 (Client -> Server)
 */
public class PacketSanctuaryTeleport implements IMessage {

    private int x;
    private int y;
    private int z;
    private int targetX;
    private int targetY;
    private int targetZ;

    public PacketSanctuaryTeleport() {}

    public PacketSanctuaryTeleport(BlockPos currentPos, BlockPos targetPos) {
        this.x = currentPos.getX();
        this.y = currentPos.getY();
        this.z = currentPos.getZ();
        this.targetX = targetPos.getX();
        this.targetY = targetPos.getY();
        this.targetZ = targetPos.getZ();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        targetX = buf.readInt();
        targetY = buf.readInt();
        targetZ = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(targetX);
        buf.writeInt(targetY);
        buf.writeInt(targetZ);
    }

    public static class Handler implements IMessageHandler<PacketSanctuaryTeleport, IMessage> {
        @Override
        public IMessage onMessage(PacketSanctuaryTeleport message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            BlockPos currentPos = new BlockPos(message.x, message.y, message.z);
            BlockPos targetPos = new BlockPos(message.targetX, message.targetY, message.targetZ);

            player.getServerWorld().addScheduledTask(() -> {
                // 验证距离
                if (player.getDistanceSq(currentPos) > 64.0) {
                    return;
                }

                // 获取当前的祭坛实体
                TileEntity te = player.world.getTileEntity(currentPos);
                if (te instanceof TileEntitySanctuary) {
                    TileEntitySanctuary sanctuary = (TileEntitySanctuary) te;
                    
                    // 尝试传送
                    if (sanctuary.teleportTo(player, targetPos)) {
                        player.sendMessage(new TextComponentTranslation("adversity.sanctuary.teleport.success"));
                    } else {
                        player.sendMessage(new TextComponentTranslation("adversity.sanctuary.teleport.failed"));
                    }
                }
            });
            return null;
        }
    }
}
