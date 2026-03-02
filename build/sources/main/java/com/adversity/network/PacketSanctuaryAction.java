package com.adversity.network;

import com.adversity.sanctuary.TileEntitySanctuary;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 圣所操作网络包
 * 用于客户端请求切换模式、升级等操作
 */
public class PacketSanctuaryAction implements IMessage {

    public enum Action {
        TOGGLE_MODE,
        UPGRADE,
        ACTIVATE,
        PERFORM_RITUAL
    }

    private BlockPos pos;
    private int action;

    public PacketSanctuaryAction() {}

    public PacketSanctuaryAction(BlockPos pos, Action action) {
        this.pos = pos;
        this.action = action.ordinal();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int x = buf.readInt();
        int y = buf.readInt();
        int z = buf.readInt();
        pos = new BlockPos(x, y, z);
        action = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        buf.writeInt(action);
    }

    public static class Handler implements IMessageHandler<PacketSanctuaryAction, IMessage> {
        @Override
        public IMessage onMessage(PacketSanctuaryAction message, MessageContext ctx) {
            ctx.getServerHandler().player.getServerWorld().addScheduledTask(() -> {
                World world = ctx.getServerHandler().player.world;
                TileEntity te = world.getTileEntity(message.pos);

                if (!(te instanceof TileEntitySanctuary)) {
                    return;
                }

                TileEntitySanctuary sanctuary = (TileEntitySanctuary) te;
                Action actionType = Action.values()[message.action % Action.values().length];

                switch (actionType) {
                    case TOGGLE_MODE:
                        sanctuary.toggleMode();
                        break;
                    case UPGRADE:
                        sanctuary.upgradeTier();
                        break;
                    case ACTIVATE:
                        sanctuary.activate();
                        break;
                    case PERFORM_RITUAL:
                        sanctuary.performRitualFromSlot(ctx.getServerHandler().player);
                        break;
                }
            });

            return null;
        }
    }
}
