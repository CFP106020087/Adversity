package com.adversity.item;

import com.adversity.Adversity;
import com.adversity.affix.AffixData;
import com.adversity.affix.IAffix;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * 词条透镜 - 扫描怪物词条的道具
 * 
 * 机制：
 * 1. 右键对准怪物使用
 * 2. 显示该怪物的所有词条列表
 * 3. 包含每个词条的简要说明和反制提示
 * 4. 扫描距离16格
 */
public class ItemAffixLens extends Item {

    /** 扫描距离 */
    private static final double SCAN_RANGE = 16.0;

    public ItemAffixLens() {
        setRegistryName(Adversity.MODID, "affix_lens");
        setTranslationKey(Adversity.MODID + ".affix_lens");
        setMaxStackSize(1);
        setCreativeTab(AdversityTab.INSTANCE);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        // 射线检测
        EntityLiving target = findTargetEntity(world, player);

        if (target == null) {
            player.sendStatusMessage(new TextComponentTranslation("item.adversity.affix_lens.no_target"), true);
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }

        // 获取目标的词条能力
        IAdversityCapability cap = CapabilityHandler.getCapability(target);

        if (cap == null || cap.getAllAffixData().isEmpty()) {
            player.sendStatusMessage(new TextComponentTranslation("item.adversity.affix_lens.no_affix",
                    target.getName()), false);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        // 显示词条信息
        displayAffixInfo(player, target, cap);

        // 冷却
        player.getCooldownTracker().setCooldown(this, 20); // 1秒冷却

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    /**
     * 查找视线方向的目标实体
     */
    private EntityLiving findTargetEntity(World world, EntityPlayer player) {
        Vec3d eyePos = player.getPositionEyes(1.0f);
        Vec3d lookVec = player.getLookVec();
        Vec3d endPos = eyePos.add(lookVec.x * SCAN_RANGE, lookVec.y * SCAN_RANGE, lookVec.z * SCAN_RANGE);

        // 扩展的碰撞箱检测
        AxisAlignedBB scanArea = player.getEntityBoundingBox()
                .expand(lookVec.x * SCAN_RANGE, lookVec.y * SCAN_RANGE, lookVec.z * SCAN_RANGE)
                .grow(1.0);

        EntityLiving closestTarget = null;
        double closestDist = SCAN_RANGE;

        for (EntityLiving entity : world.getEntitiesWithinAABB(EntityLiving.class, scanArea)) {
            // 检查是否在视线内
            AxisAlignedBB entityBox = entity.getEntityBoundingBox().grow(0.3);
            RayTraceResult hit = entityBox.calculateIntercept(eyePos, endPos);

            if (hit != null) {
                double dist = eyePos.distanceTo(hit.hitVec);
                if (dist < closestDist) {
                    closestDist = dist;
                    closestTarget = entity;
                }
            }
        }

        return closestTarget;
    }

    /**
     * 显示词条信息给玩家
     */
    private void displayAffixInfo(EntityPlayer player, EntityLiving target, IAdversityCapability cap) {
        // 标题
        player.sendMessage(new TextComponentString(TextFormatting.GOLD + "══════ " +
                TextFormatting.YELLOW + target.getName() +
                TextFormatting.GRAY + " [T" + cap.getTier() + "] " +
                TextFormatting.GOLD + "══════"));

        // 列出所有词条
        Collection<AffixData> affixes = cap.getAllAffixData();
        for (AffixData data : affixes) {
            IAffix affix = data.getAffix();
            String affixKey = "affix.adversity." + affix.getId().getPath() + ".name";

            // 根据词条类型选择颜色
            TextFormatting color;
            switch (affix.getType()) {
                case OFFENSIVE:
                    color = TextFormatting.RED;
                    break;
                case DEFENSIVE:
                    color = TextFormatting.BLUE;
                    break;
                case UTILITY:
                    color = TextFormatting.YELLOW;
                    break;
                case SPECIAL:
                    color = TextFormatting.DARK_PURPLE;
                    break;
                default:
                    color = TextFormatting.WHITE;
            }

            // 使用 TextComponentTranslation 代替 I18n（服务端安全）
            player.sendMessage(new TextComponentString(color + "▪ ")
                    .appendSibling(new TextComponentTranslation(affixKey)));
        }

        player.sendMessage(new TextComponentString(TextFormatting.GOLD + "═════════════════════════"));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.GRAY + I18n.format("item.adversity.affix_lens.tooltip"));
        tooltip.add(TextFormatting.DARK_GRAY + I18n.format("item.adversity.affix_lens.usage"));
    }
}
