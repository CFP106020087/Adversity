package com.adversity.item;

import com.adversity.Adversity;
import com.adversity.block.BlockRegistry;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 圣所指南针 - 指向最近天然圣所的位置
 * 
 * 功能:
 * - 自动追踪最近的天然圣所
 * - 显示距离和方向
 * - 右键刷新搜索
 */
public class ItemSanctuaryCompass extends Item {

    /** 搜索圣所的最大范围 */
    private static final int SEARCH_RADIUS = 2000;

    /** 搜索间隔 (ticks) */
    private static final int SEARCH_INTERVAL = 100; // 5秒

    public ItemSanctuaryCompass() {
        setRegistryName(Adversity.MODID, "sanctuary_compass");
        setTranslationKey(Adversity.MODID + ".sanctuary_compass");
        setCreativeTab(AdversityTab.INSTANCE);
        setMaxStackSize(1);
    }

    @Override
    public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        if (worldIn.isRemote || !(entityIn instanceof EntityPlayer)) {
            return;
        }

        // 定期更新目标位置
        NBTTagCompound nbt = getOrCreateNBT(stack);
        long lastSearch = nbt.getLong("LastSearch");
        long currentTime = worldIn.getTotalWorldTime();

        if (currentTime - lastSearch >= SEARCH_INTERVAL) {
            updateTargetPosition(stack, worldIn, entityIn.getPosition());
            nbt.setLong("LastSearch", currentTime);
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack stack = playerIn.getHeldItem(handIn);

        if (!worldIn.isRemote) {
            // 立即刷新搜索
            updateTargetPosition(stack, worldIn, playerIn.getPosition());
            
            NBTTagCompound nbt = getOrCreateNBT(stack);
            if (nbt.hasKey("TargetX")) {
                int x = nbt.getInteger("TargetX");
                int y = nbt.getInteger("TargetY");
                int z = nbt.getInteger("TargetZ");
                int distance = (int) playerIn.getPosition().getDistance(x, y, z);
                
                playerIn.sendMessage(new TextComponentTranslation(
                        "item.adversity.sanctuary_compass.found",
                        x, y, z, distance));
            } else {
                playerIn.sendMessage(new TextComponentTranslation(
                        "item.adversity.sanctuary_compass.not_found"));
            }
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    /**
     * 搜索并更新最近圣所位置
     */
    private void updateTargetPosition(ItemStack stack, World world, BlockPos playerPos) {
        NBTTagCompound nbt = getOrCreateNBT(stack);
        
        BlockPos nearest = findNearestSanctuary(world, playerPos);
        
        if (nearest != null) {
            nbt.setInteger("TargetX", nearest.getX());
            nbt.setInteger("TargetY", nearest.getY());
            nbt.setInteger("TargetZ", nearest.getZ());
            nbt.setBoolean("HasTarget", true);
        } else {
            nbt.setBoolean("HasTarget", false);
            nbt.removeTag("TargetX");
            nbt.removeTag("TargetY");
            nbt.removeTag("TargetZ");
        }
    }

    /**
     * 寻找最近的圣所祭坛
     * 使用网格计算最近的圣所生成点
     */
    @Nullable
    private BlockPos findNearestSanctuary(World world, BlockPos playerPos) {
        // 圣所生成间隔和偏移 (与SanctuaryGenerator一致)
        final int SPACING = 1000;
        final int OFFSET = 500;
        final int Y = 32; // 地底生成高度

        int px = playerPos.getX();
        int pz = playerPos.getZ();

        // 计算周围9个可能的圣所网格点
        int gridX = Math.round((float)(px - OFFSET) / SPACING);
        int gridZ = Math.round((float)(pz - OFFSET) / SPACING);

        BlockPos nearest = null;
        double minDist = Double.MAX_VALUE;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int sanctuaryX = (gridX + dx) * SPACING + OFFSET;
                int sanctuaryZ = (gridZ + dz) * SPACING + OFFSET;
                BlockPos pos = new BlockPos(sanctuaryX, Y, sanctuaryZ);

                double dist = playerPos.distanceSq(pos);
                if (dist < minDist && dist < SEARCH_RADIUS * SEARCH_RADIUS) {
                    // 验证该位置确实有圣所祭坛
                    if (world.isBlockLoaded(pos)) {
                        if (world.getBlockState(pos).getBlock() == BlockRegistry.SANCTUARY_ALTAR) {
                            nearest = pos;
                            minDist = dist;
                        }
                    } else {
                        // 区块未加载，假设存在
                        nearest = pos;
                        minDist = dist;
                    }
                }
            }
        }

        return nearest;
    }

    /**
     * 获取指南针指向角度 (用于渲染)
     */
    @SideOnly(Side.CLIENT)
    public static float getCompassAngle(ItemStack stack, World world, EntityPlayer player) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null || !nbt.getBoolean("HasTarget")) {
            // 无目标时随机晃动
            return (float) (Math.random() * 360);
        }

        int targetX = nbt.getInteger("TargetX");
        int targetZ = nbt.getInteger("TargetZ");

        double dx = targetX - player.posX;
        double dz = targetZ - player.posZ;

        double angle = Math.atan2(dz, dx);
        double playerYaw = Math.toRadians(player.rotationYaw);

        return (float) Math.toDegrees(angle - playerYaw + Math.PI / 2);
    }

    private NBTTagCompound getOrCreateNBT(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.RARE;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("\u00a77" + I18n.format("item.adversity.sanctuary_compass.tooltip"));
        tooltip.add("\u00a78" + I18n.format("item.adversity.sanctuary_compass.hint"));

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null && nbt.getBoolean("HasTarget")) {
            int x = nbt.getInteger("TargetX");
            int y = nbt.getInteger("TargetY");
            int z = nbt.getInteger("TargetZ");
            tooltip.add("\u00a7a" + I18n.format("item.adversity.sanctuary_compass.target", x, y, z));
        } else {
            tooltip.add("\u00a7c" + I18n.format("item.adversity.sanctuary_compass.no_target"));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        return nbt != null && nbt.getBoolean("HasTarget");
    }
}
