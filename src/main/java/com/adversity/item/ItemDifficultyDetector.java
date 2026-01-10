package com.adversity.item;

import com.adversity.Adversity;
import com.adversity.config.AdversityConfig;
import com.adversity.difficulty.DifficultyManager;
import com.adversity.effect.SuppressionManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 难度探测器 - 信息工具
 * 右键使用显示当前位置的难度信息
 * 包括：难度值、精英概率、预期等级、压制状态等
 *
 * 设计理念：
 * - 透明化难度系统，让玩家理解规则
 * - 帮助玩家规划探索路线
 * - 提供压制状态信息
 */
public class ItemDifficultyDetector extends Item {

    private static final String[] TIER_NAMES = {
        "Normal", "Elite", "Rare", "Veteran", "Epic", "Legendary",
        "Mythic", "Ancient", "Void", "Abyssal", "Terminus"
    };

    private static final TextFormatting[] TIER_COLORS = {
        TextFormatting.GRAY,       // T0 - Normal
        TextFormatting.GREEN,      // T1 - Elite
        TextFormatting.BLUE,       // T2 - Rare
        TextFormatting.DARK_AQUA,  // T3 - Veteran
        TextFormatting.DARK_PURPLE,// T4 - Epic
        TextFormatting.GOLD,       // T5 - Legendary
        TextFormatting.LIGHT_PURPLE,// T6 - Mythic
        TextFormatting.YELLOW,     // T7 - Ancient
        TextFormatting.DARK_GRAY,  // T8 - Void
        TextFormatting.DARK_RED,   // T9 - Abyssal
        TextFormatting.RED         // T10 - Terminus
    };

    public ItemDifficultyDetector() {
        setRegistryName(Adversity.MODID, "difficulty_detector");
        setUnlocalizedName(Adversity.MODID + ".difficulty_detector");
        setCreativeTab(AdversityTab.INSTANCE);
        setMaxStackSize(1);
        setMaxDamage(0); // 无耐久消耗
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack stack = playerIn.getHeldItem(handIn);

        if (!worldIn.isRemote) {
            // 服务端计算并发送信息
            displayDifficultyInfo(worldIn, playerIn);
        }

        // 冷却防止刷屏
        playerIn.getCooldownTracker().setCooldown(this, 20);

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    private void displayDifficultyInfo(World world, EntityPlayer player) {
        BlockPos pos = player.getPosition();
        int dimension = world.provider.getDimension();

        // 计算难度
        float difficulty = DifficultyManager.calculateDifficultyAt(world, pos, player);
        int tier = DifficultyManager.calculateTier(difficulty);

        // 计算精英概率
        double minDiff = AdversityConfig.eliteSettings.minDifficultyForElite;
        double eliteChance = Math.min(
            AdversityConfig.eliteSettings.eliteChance +
                difficulty * AdversityConfig.eliteSettings.eliteChancePerDifficulty,
            AdversityConfig.eliteSettings.maxEliteChance
        );

        // 检查压制状态
        boolean suppressed = SuppressionManager.isSuppressed(dimension, pos);
        SuppressionManager.SuppressionZone nearestZone = SuppressionManager.getNearestZone(dimension, pos);

        // 构建消息
        player.sendMessage(new TextComponentString(
            TextFormatting.GOLD + "=== " +
            TextFormatting.WHITE + "Adversity Difficulty Report" +
            TextFormatting.GOLD + " ==="
        ));

        // 难度值
        player.sendMessage(new TextComponentString(
            TextFormatting.YELLOW + "Difficulty: " +
            getDifficultyColor(difficulty) + String.format("%.2f", difficulty)
        ));

        // 预期等级
        TextFormatting tierColor = tier < TIER_COLORS.length ? TIER_COLORS[tier] : TextFormatting.RED;
        String tierName = tier < TIER_NAMES.length ? TIER_NAMES[tier] : "???";
        player.sendMessage(new TextComponentString(
            TextFormatting.YELLOW + "Expected Tier: " +
            tierColor + tierName + " (T" + tier + ")"
        ));

        // 精英概率
        if (difficulty >= minDiff) {
            player.sendMessage(new TextComponentString(
                TextFormatting.YELLOW + "Elite Chance: " +
                TextFormatting.WHITE + String.format("%.1f%%", eliteChance * 100)
            ));
        } else {
            player.sendMessage(new TextComponentString(
                TextFormatting.YELLOW + "Elite Chance: " +
                TextFormatting.GRAY + String.format("None (difficulty < %.1f)", minDiff)
            ));
        }

        // 怪物属性预览 - 使用新的缩放系统
        if (tier > 0) {
            double healthMult = DifficultyManager.calculateHealthMultiplier(difficulty);
            double damageMult = DifficultyManager.calculateDamageMultiplier(difficulty);
            double armorBonus = DifficultyManager.calculateArmorBonus(difficulty);
            double damageReduction = DifficultyManager.calculateDamageReduction(difficulty);

            player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "Elite Stats: " +
                TextFormatting.RED + String.format("%.1fx HP", healthMult) + TextFormatting.GRAY + ", " +
                TextFormatting.RED + String.format("%.1fx DMG", damageMult) + TextFormatting.GRAY + ", " +
                TextFormatting.AQUA + String.format("+%.1f Armor", armorBonus) + TextFormatting.GRAY + ", " +
                TextFormatting.BLUE + String.format("%.0f%% DR", damageReduction * 100)
            ));

            // 显示缩放模式
            player.sendMessage(new TextComponentString(
                TextFormatting.DARK_GRAY + "Scaling: " +
                AdversityConfig.statScaling.healthScalingMode + "/" +
                AdversityConfig.statScaling.damageScalingMode
            ));
        }

        // 压制状态
        if (suppressed) {
            player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "Status: " +
                TextFormatting.AQUA + "SUPPRESSED" +
                TextFormatting.GRAY + " (No elite spawns)"
            ));
            if (nearestZone != null) {
                int remainingSec = nearestZone.remainingTicks / 20;
                player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "  Remaining: " +
                    TextFormatting.WHITE + String.format("%d:%02d", remainingSec / 60, remainingSec % 60)
                ));
            }
        } else {
            player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "Status: " +
                TextFormatting.WHITE + "Normal"
            ));
        }

        // 距离出生点
        BlockPos spawn = world.getSpawnPoint();
        double distance = Math.sqrt(pos.distanceSq(spawn));
        player.sendMessage(new TextComponentString(
            TextFormatting.GRAY + "Distance from spawn: " +
            TextFormatting.WHITE + String.format("%.0f blocks", distance)
        ));
    }

    private TextFormatting getDifficultyColor(float difficulty) {
        if (difficulty < 2.0f) return TextFormatting.GRAY;
        if (difficulty < 5.0f) return TextFormatting.GREEN;
        if (difficulty < 10.0f) return TextFormatting.YELLOW;
        if (difficulty < 15.0f) return TextFormatting.GOLD;
        if (difficulty < 20.0f) return TextFormatting.RED;
        return TextFormatting.DARK_RED;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("\u00a7e" + I18n.format("item.adversity.difficulty_detector.tooltip"));
        tooltip.add("");
        tooltip.add("\u00a77" + I18n.format("item.adversity.difficulty_detector.usage"));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return false;
    }
}
