package com.adversity.item;

import com.adversity.Adversity;
import com.adversity.effect.SuppressionManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 宁静之尘 - 消耗品
 * 右键使用，在以玩家为中心的区域内临时压制精英生成
 *
 * 设计理念：
 * - 提供临时喘息空间，但不是永久解决方案
 * - 成本合理，鼓励玩家在危险区域谨慎使用
 * - 视觉效果明显，让玩家知道效果何时结束
 */
public class ItemCalmDust extends Item {

    // 效果参数 (可通过配置调整)
    public static final int SUPPRESSION_RADIUS = 32; // 32格半径
    public static final int SUPPRESSION_DURATION = 6000; // 5分钟 (6000 ticks)

    public ItemCalmDust() {
        setRegistryName(Adversity.MODID, "calm_dust");
        setTranslationKey(Adversity.MODID + ".calm_dust");
        setCreativeTab(AdversityTab.INSTANCE);
        setMaxStackSize(16);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack stack = playerIn.getHeldItem(handIn);

        if (!worldIn.isRemote) {
            // 服务端：创建压制区域
            BlockPos center = playerIn.getPosition();
            SuppressionManager.addSuppression(
                worldIn.provider.getDimension(),
                center,
                SUPPRESSION_RADIUS,
                SUPPRESSION_DURATION
            );

            // 消耗物品
            if (!playerIn.isCreative()) {
                stack.shrink(1);
            }

            // 发送消息
            playerIn.sendStatusMessage(
                new net.minecraft.util.text.TextComponentTranslation(
                    "item.adversity.calm_dust.activated",
                    SUPPRESSION_RADIUS,
                    SUPPRESSION_DURATION / 20 / 60
                ),
                true
            );
        } else {
            // 客户端：播放粒子效果
            spawnActivationParticles(worldIn, playerIn);
        }

        // 播放音效
        worldIn.playSound(
            playerIn,
            playerIn.posX, playerIn.posY, playerIn.posZ,
            SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
            SoundCategory.PLAYERS,
            1.0F,
            1.2F
        );

        playerIn.getCooldownTracker().setCooldown(this, 20); // 1秒冷却防止误触

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @SideOnly(Side.CLIENT)
    private void spawnActivationParticles(World world, EntityPlayer player) {
        // 生成扩散粒子效果
        for (int i = 0; i < 50; i++) {
            double angle = Math.random() * Math.PI * 2;
            double distance = Math.random() * 3;
            double px = player.posX + Math.cos(angle) * distance;
            double py = player.posY + 1 + Math.random();
            double pz = player.posZ + Math.sin(angle) * distance;

            world.spawnParticle(
                EnumParticleTypes.SPELL_WITCH,
                px, py, pz,
                (Math.random() - 0.5) * 0.5,
                Math.random() * 0.5,
                (Math.random() - 0.5) * 0.5
            );
        }
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.UNCOMMON;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("\u00a7a" + I18n.format("item.adversity.calm_dust.tooltip"));
        tooltip.add("");
        tooltip.add("\u00a77" + I18n.format("item.adversity.calm_dust.effect",
            SUPPRESSION_RADIUS, SUPPRESSION_DURATION / 20 / 60));
        tooltip.add("\u00a78" + I18n.format("item.adversity.calm_dust.hint"));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return false;
    }
}
