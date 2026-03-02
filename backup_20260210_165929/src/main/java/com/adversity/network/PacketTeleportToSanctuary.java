package com.adversity.network;

import com.adversity.sanctuary.SanctuaryManager;
import com.adversity.sanctuary.SanctuaryZone;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 客户端请求传送到指定圣所
 */
public class PacketTeleportToSanctuary implements IMessage {

    private int targetDim;
    private int targetX;
    private int targetY;
    private int targetZ;

    public PacketTeleportToSanctuary() {}

    public PacketTeleportToSanctuary(int dimension, BlockPos pos) {
        this.targetDim = dimension;
        this.targetX = pos.getX();
        this.targetY = pos.getY();
        this.targetZ = pos.getZ();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        targetDim = buf.readInt();
        targetX = buf.readInt();
        targetY = buf.readInt();
        targetZ = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(targetDim);
        buf.writeInt(targetX);
        buf.writeInt(targetY);
        buf.writeInt(targetZ);
    }

    public static class Handler implements IMessageHandler<PacketTeleportToSanctuary, IMessage> {
        @Override
        public IMessage onMessage(PacketTeleportToSanctuary message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                BlockPos targetPos = new BlockPos(message.targetX, message.targetY, message.targetZ);

                // 验证: 玩家必须在已激活的圣所内
                SanctuaryZone currentZone = SanctuaryManager.getPlayerSanctuary(player);
                if (currentZone == null) {
                    player.sendMessage(new TextComponentTranslation("adversity.sanctuary.teleport.not_in_sanctuary"));
                    return;
                }

                // 验证: 目标必须是已激活的圣所
                if (!SanctuaryManager.isSanctuaryAt(player.world, targetPos)) {
                    // 可能在不同维度，需要加载检查
                    MinecraftServer server = player.getServer();
                    if (server == null) return;
                    
                    WorldServer targetWorld = server.getWorld(message.targetDim);
                    if (targetWorld == null || !SanctuaryManager.isSanctuaryAt(targetWorld, targetPos)) {
                        player.sendMessage(new TextComponentTranslation("adversity.sanctuary.teleport.invalid"));
                        return;
                    }
                }

                // 不能传送到同一个圣所
                if (currentZone.center.equals(targetPos) && currentZone.dimension == message.targetDim) {
                    player.sendMessage(new TextComponentTranslation("adversity.sanctuary.teleport.same"));
                    return;
                }

                // 消耗燃料 (从当前圣所扣除，跨维度消耗更多)
                int fuelCost = player.dimension == message.targetDim ? 200 : 500;
                if (currentZone.fuel < fuelCost) {
                    player.sendMessage(new TextComponentTranslation("adversity.sanctuary.teleport.no_fuel"));
                    return;
                }
                SanctuaryManager.consumeFuel(player.world, currentZone.center, fuelCost);

                // 播放传送音效
                player.world.playSound(null, player.posX, player.posY, player.posZ,
                        SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.PLAYERS, 1.0f, 1.5f);

                // 执行传送
                if (player.dimension != message.targetDim) {
                    // 跨维度传送
                    MinecraftServer server = player.getServer();
                    if (server != null) {
                        WorldServer targetWorld = server.getWorld(message.targetDim);
                        player.changeDimension(message.targetDim, new SimpleTeleporter(targetWorld, targetPos));
                    }
                } else {
                    // 同维度传送
                    player.setPositionAndUpdate(targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5);
                }

                // 成功消息
                player.sendMessage(new TextComponentTranslation("adversity.sanctuary.teleport.success"));
            });

            return null;
        }
    }

    /**
     * 简单传送器
     */
    public static class SimpleTeleporter extends Teleporter {
        private final BlockPos target;

        public SimpleTeleporter(WorldServer world, BlockPos target) {
            super(world);
            this.target = target;
        }

        @Override
        public void placeInPortal(net.minecraft.entity.Entity entity, float rotationYaw) {
            entity.setLocationAndAngles(target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5, entity.rotationYaw, 0.0F);
            entity.motionX = 0;
            entity.motionY = 0;
            entity.motionZ = 0;
        }

        @Override
        public boolean placeInExistingPortal(net.minecraft.entity.Entity entity, float rotationYaw) {
            return false;
        }

        @Override
        public boolean makePortal(net.minecraft.entity.Entity entity) {
            return false;
        }

        @Override
        public void removeStalePortalLocations(long worldTime) {
        }
    }
}
