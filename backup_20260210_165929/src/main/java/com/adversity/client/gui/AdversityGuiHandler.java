package com.adversity.client.gui;

import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.talisman.ITalismanCapability;
import com.adversity.sanctuary.TileEntitySanctuary;
import com.adversity.sanctuary.inventory.ContainerSanctuary;
import com.adversity.talisman.ContainerTalisman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

public class AdversityGuiHandler implements IGuiHandler {

    public static final int GUI_SANCTUARY = 1;
    public static final int GUI_TALISMAN = 2;
    public static final int GUI_SANCTUARY_TELEPORT = 3;
    public static final int GUI_SANCTUARY_CONTROL = 4; // 圣所控制面板

    @Nullable
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == GUI_SANCTUARY) {
            TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
            if (te instanceof TileEntitySanctuary) {
                return new ContainerSanctuary(player.inventory, (TileEntitySanctuary) te);
            }
        } else if (ID == GUI_TALISMAN) {
            ITalismanCapability cap = CapabilityHandler.getTalismanCapability(player);
            if (cap != null) {
                return new ContainerTalisman(player.inventory, cap);
            }
        } else if (ID == GUI_SANCTUARY_CONTROL) {
            TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
            if (te instanceof TileEntitySanctuary) {
                return new com.adversity.sanctuary.inventory.ContainerSanctuaryControl(
                        player.inventory, (TileEntitySanctuary) te);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == GUI_SANCTUARY) {
            TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
            if (te instanceof TileEntitySanctuary) {
                return new GuiSanctuaryAltar(player.inventory, (TileEntitySanctuary) te);
            }
        } else if (ID == GUI_TALISMAN) {
            ITalismanCapability cap = CapabilityHandler.getTalismanCapability(player);
            if (cap != null) {
                return new GuiTalisman(new ContainerTalisman(player.inventory, cap));
            }
        } else if (ID == GUI_SANCTUARY_TELEPORT) {
            // 传送选择GUI - 需要获取圣所列表
            BlockPos pos = new BlockPos(x, y, z);
            java.util.List<com.adversity.sanctuary.SanctuaryZone> sanctuaries = com.adversity.sanctuary.SanctuaryManager
                    .getAllActivatedSanctuaries(world);
            return new GuiSanctuaryTeleport(pos, sanctuaries, player);
        } else if (ID == GUI_SANCTUARY_CONTROL) {
            TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
            if (te instanceof TileEntitySanctuary) {
                return new GuiSanctuaryControl(player.inventory, (TileEntitySanctuary) te);
            }
        }
        return null;
    }
}
