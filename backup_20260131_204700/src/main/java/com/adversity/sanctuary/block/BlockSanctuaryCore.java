package com.adversity.sanctuary.block;

import com.adversity.Adversity;
import com.adversity.item.ItemEntropy;
import com.adversity.sanctuary.SanctuaryType;
import com.adversity.sanctuary.TileEntitySanctuary;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * 人造圣所核心方块
 * 玩家放置并搭建3x3x3多方块结构后激活
 */
public class BlockSanctuaryCore extends Block implements ITileEntityProvider {

    public BlockSanctuaryCore() {
        super(Material.ROCK);
        setRegistryName(Adversity.MODID, "sanctuary_core");
        setTranslationKey(Adversity.MODID + ".sanctuary_core");
        setHardness(10.0f);
        setResistance(50.0f);
        setLightLevel(0.3f);
        // TODO: 设置创造模式标签页
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        TileEntitySanctuary te = new TileEntitySanctuary();
        te.setType(SanctuaryType.ARTIFICIAL);
        te.setTier(0); // 人造圣所无等级
        return te;
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
            EntityPlayer playerIn, EnumHand hand,
            EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) {
            return true;
        }

        TileEntity te = worldIn.getTileEntity(pos);
        if (!(te instanceof TileEntitySanctuary)) {
            return false;
        }

        TileEntitySanctuary sanctuary = (TileEntitySanctuary) te;
        ItemStack heldItem = playerIn.getHeldItem(hand);

        // 检查是否在添加燃料
        if (heldItem.getItem() instanceof ItemEntropy) {
            if (sanctuary.isActivated()) {
                int fuelValue = ItemEntropy.getFuelValue(heldItem);
                int added = sanctuary.addFuel(fuelValue);
                if (added >= 0) {
                    heldItem.shrink(1);
                    playerIn.sendMessage(new TextComponentTranslation(
                            "adversity.sanctuary.fuel_added",
                            fuelValue,
                            sanctuary.getFuel(),
                            sanctuary.getMaxFuel()));
                    playFuelEffects(worldIn, pos);
                    return true;
                }
            }
        }

        if (sanctuary.isActivated()) {
            // 已激活，显示信息
            showSanctuaryInfo(playerIn, sanctuary);
        } else {
            // 检查多方块结构
            if (checkMultiblockStructure(worldIn, pos)) {
                if (sanctuary.activate()) {
                    playerIn.sendMessage(new TextComponentTranslation(
                            "adversity.sanctuary.artificial_activated"));
                    playActivationEffects(worldIn, pos);
                } else {
                    playerIn.sendMessage(new TextComponentTranslation(
                            "adversity.sanctuary.activation_failed"));
                }
            } else {
                playerIn.sendMessage(new TextComponentTranslation(
                        "adversity.sanctuary.structure_incomplete"));
            }
        }

        return true;
    }

    /**
     * 检查多方块结构
     * 3x3x3 结构：
     * - 底层和顶层：9个石砖
     * - 中层边缘：8个錾制石砖
     * - 中层中心：核心方块本身
     */
    private boolean checkMultiblockStructure(World world, BlockPos corePos) {
        // 中层中间是核心位置
        // 检查中层边缘（8个位置）
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0)
                    continue; // 跳过中心（核心本身）

                BlockPos checkPos = corePos.add(dx, 0, dz);
                if (!isChiseledStoneBrick(world, checkPos)) {
                    return false;
                }
            }
        }

        // 检查底层和顶层（各9个位置）
        for (int dy : new int[] { -1, 1 }) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos checkPos = corePos.add(dx, dy, dz);
                    if (!isStoneBrick(world, checkPos)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean isStoneBrick(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        // 石砖或其变种
        return state.getBlock() == Blocks.STONEBRICK;
    }

    private boolean isChiseledStoneBrick(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        // 錾制石砖 (meta = 3)
        return state.getBlock() == Blocks.STONEBRICK &&
                state.getBlock().getMetaFromState(state) == 3;
    }

    private void showSanctuaryInfo(EntityPlayer player, TileEntitySanctuary sanctuary) {
        int fuel = sanctuary.getFuel();
        int maxFuel = sanctuary.getMaxFuel();
        int percent = maxFuel > 0 ? (fuel * 100 / maxFuel) : 0;

        player.sendMessage(new TextComponentTranslation(
                "adversity.sanctuary.info_artificial",
                fuel,
                maxFuel,
                percent));
    }

    private void playActivationEffects(World world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.BLOCK_END_PORTAL_SPAWN,
                SoundCategory.BLOCKS, 0.8f, 1.2f);

        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.PORTAL,
                    pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                    50, 0.5, 0.5, 0.5, 0.2);
        }
    }

    private void playFuelEffects(World world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.BLOCKS, 0.5f, 1.0f);

        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                    pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                    10, 0.3, 0.3, 0.3, 0.05);
        }
    }

    @Override
    public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntitySanctuary) {
            TileEntitySanctuary sanctuary = (TileEntitySanctuary) te;
            if (sanctuary.isActive()) {
                float fuelPercent = sanctuary.getFuelPercentage();

                // 较小的火焰（人造圣所效果弱）
                double flameHeight = 0.3 + fuelPercent * 0.7;
                worldIn.spawnParticle(EnumParticleTypes.FLAME,
                        pos.getX() + 0.5, pos.getY() + 1 + flameHeight, pos.getZ() + 0.5,
                        0, 0.03, 0);
            }
        }
    }
}
