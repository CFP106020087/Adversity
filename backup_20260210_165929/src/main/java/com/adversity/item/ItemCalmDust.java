package com.adversity.item;

import com.adversity.Adversity;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IPlayerDifficulty;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 宁静之尘 - 稀有消耗品
 * 右键使用：降低个人难度倍率30%，持续10分钟
 * 
 * 设计理念：
 * - 精英怪稀有掉落 (0.5%)，用于关键时刻
 * - 降低个人难度而非区域压制，更加灵活
 * - 效果叠加有上限，防止完全无敌
 */
public class ItemCalmDust extends Item {

    // 效果参数
    /** 难度降低量 (0.05 = 永久降低5%) */
    public static final float DIFFICULTY_REDUCTION = 0.05f;
    /** 最低难度倍率 (防止叠加到0) */
    public static final float MIN_MULTIPLIER = 0.3f;
    /** 效果持续时间 (tick) - 10分钟 = 12000 ticks */
    public static final int EFFECT_DURATION = 12000;

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
            // 服务端：获取玩家难度能力
            IPlayerDifficulty cap = CapabilityHandler.getPlayerDifficulty(playerIn);
            if (cap != null) {
                float currentMultiplier = cap.getDifficultyMultiplier();
                float newMultiplier = Math.max(MIN_MULTIPLIER, currentMultiplier - DIFFICULTY_REDUCTION);

                // 如果已经在最低值，提示无效
                if (currentMultiplier <= MIN_MULTIPLIER) {
                    playerIn.sendStatusMessage(
                            new net.minecraft.util.text.TextComponentTranslation(
                                    "item.adversity.calm_dust.already_min"),
                            true);
                    return new ActionResult<>(EnumActionResult.FAIL, stack);
                }

                cap.setDifficultyMultiplier(newMultiplier);

                // 消耗物品
                if (!playerIn.isCreative()) {
                    stack.shrink(1);
                }

                // 发送成功消息
                int reductionPercent = (int) ((1.0f - newMultiplier) * 100);
                playerIn.sendStatusMessage(
                        new net.minecraft.util.text.TextComponentTranslation(
                                "item.adversity.calm_dust.activated_new",
                                reductionPercent,
                                EFFECT_DURATION / 20 / 60),
                        true);

                // 标记恢复时间 (通过NBT或药水效果实现持续时间)
                // TODO: 实现定时恢复机制 - 目前效果是永久的，需要通过其他方式恢复
            }
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
                1.5F
        );

        playerIn.getCooldownTracker().setCooldown(this, 20); // 1秒冷却防止误触

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @SideOnly(Side.CLIENT)
    private void spawnActivationParticles(World world, EntityPlayer player) {
        // 生成向上飘散的治愈粒子
        for (int i = 0; i < 30; i++) {
            double angle = Math.random() * Math.PI * 2;
            double distance = Math.random() * 1.5;
            double px = player.posX + Math.cos(angle) * distance;
            double py = player.posY + 0.5 + Math.random() * 1.5;
            double pz = player.posZ + Math.sin(angle) * distance;

            world.spawnParticle(
                    EnumParticleTypes.VILLAGER_HAPPY,
                px, py, pz,
                    0, 0.1, 0
            );
        }
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC; // 升级为史诗稀有度
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("\u00a7d" + I18n.format("item.adversity.calm_dust.tooltip_new"));
        tooltip.add("");
        tooltip.add("\u00a77" + I18n.format("item.adversity.calm_dust.effect_new",
                (int) (DIFFICULTY_REDUCTION * 100)));
        tooltip.add("\u00a78" + I18n.format("item.adversity.calm_dust.source"));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return true; // 发光效果表示稀有
    }
}
