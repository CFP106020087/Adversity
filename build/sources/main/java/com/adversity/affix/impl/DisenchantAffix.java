package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.client.visual.VisualEffectHelper;
import com.adversity.client.visual.VisualEffectType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.WorldServer;

import java.util.*;

/**
 * 解咒词条 - 临时沉默玩家装备的附魔
 *
 * 机制：
 * 1. 攻击玩家时有概率临时"封印"一件装备的所有附魔
 * 2. 封印持续1分钟
 * 3. 封印期间附魔效果无效
 * 4. 设计理念：针对重附魔装备玩家
 */
public class DisenchantAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "disenchant");

    /** 基础触发概率 */
    private static final float BASE_CHANCE = 0.2f;  // 20%

    /** 封印持续时间（tick） */
    private static final int SILENCE_DURATION = 1200;  // 60秒

    /** 封印标记NBT Key */
    private static final String SILENCED_TAG = "AdversitySilenced";
    private static final String SILENCED_ENCHANTS_TAG = "SilencedEnchantments";
    private static final String SILENCE_END_TIME_TAG = "SilenceEndTime";

    /** 存储被封印物品的原始附魔 - Key: ItemStack hashCode, Value: 原始附魔NBT */
    private static final Map<Integer, NBTTagList> ORIGINAL_ENCHANTMENTS = new HashMap<>();

    private static final Random RANDOM = new Random();

    public DisenchantAffix() {
        super(
            ID,
            AffixType.UTILITY,
            40,     // 较低权重
            5.0f    // 难度5以上
        );
    }

    @Override
    public float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data) {
        if (!(target instanceof EntityPlayer)) {
            return damage;
        }

        EntityPlayer player = (EntityPlayer) target;
        int tier = getTier(attacker);

        // 检查饰品反制（附魔守护者 - disenchant / 守护之魂 - equipment_seal）
        if (com.adversity.item.bauble.BaubleHelper.tryBlockEffect(player, "disenchant", attacker)) {
            return damage;
        }
        if (com.adversity.item.bauble.BaubleHelper.tryBlockEffect(player, "equipment_seal", attacker)) {
            return damage;
        }

        // 计算触发概率
        float chance = BASE_CHANCE + (tier * 0.03f);

        if (RANDOM.nextFloat() < chance) {
            silenceRandomEquipment(player, attacker, tier);
        }

        return damage;
    }


    /**
     * 随机封印一件装备的附魔
     */
    private void silenceRandomEquipment(EntityPlayer player, EntityLiving attacker, int tier) {
        // 收集所有有附魔的装备
        List<ItemStack> enchantedItems = new ArrayList<>();

        // 主手武器
        ItemStack mainHand = player.getHeldItemMainhand();
        if (!mainHand.isEmpty() && mainHand.isItemEnchanted()) {
            enchantedItems.add(mainHand);
        }

        // 副手
        ItemStack offHand = player.getHeldItemOffhand();
        if (!offHand.isEmpty() && offHand.isItemEnchanted()) {
            enchantedItems.add(offHand);
        }

        // 盔甲
        for (ItemStack armor : player.getArmorInventoryList()) {
            if (!armor.isEmpty() && armor.isItemEnchanted()) {
                enchantedItems.add(armor);
            }
        }

        if (enchantedItems.isEmpty()) {
            return;  // 没有附魔装备
        }

        // 随机选择一件装备
        ItemStack targetItem = enchantedItems.get(RANDOM.nextInt(enchantedItems.size()));

        // 检查是否已被封印
        if (isItemSilenced(targetItem)) {
            return;
        }

        // 执行封印
        silenceItem(targetItem, player, tier);

        // 播放效果
        playDisenchantEffects(player, attacker);

        // 发送视觉效果
        VisualEffectHelper.sendToPlayer(player, VisualEffectType.VOID_GAZE, 40, 0.6f, attacker.getEntityId());
    }

    /**
     * 封印物品的附魔
     */
    private void silenceItem(ItemStack stack, EntityPlayer player, int tier) {
        if (stack.isEmpty()) return;

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }

        // 保存原始附魔
        NBTTagList originalEnchants = nbt.getTagList("ench", 10).copy();
        if (originalEnchants.isEmpty()) {
            return;
        }

        // 存储原始附魔到物品NBT
        nbt.setTag(SILENCED_ENCHANTS_TAG, originalEnchants);

        // 计算封印结束时间
        long endTime = player.world.getTotalWorldTime() + SILENCE_DURATION + (tier * 200);
        nbt.setLong(SILENCE_END_TIME_TAG, endTime);

        // 标记为已封印
        nbt.setBoolean(SILENCED_TAG, true);

        // 移除附魔效果（清空ench标签）
        nbt.removeTag("ench");

        Adversity.LOGGER.debug("Silenced enchantments on item: {}", stack.getDisplayName());
    }

    /**
     * 检查物品是否被封印
     */
    public static boolean isItemSilenced(ItemStack stack) {
        if (stack.isEmpty()) return false;
        NBTTagCompound nbt = stack.getTagCompound();
        return nbt != null && nbt.getBoolean(SILENCED_TAG);
    }

    /**
     * 检查并恢复封印（应该每tick调用）
     */
    public static void checkAndRestoreSilence(ItemStack stack, long currentTime) {
        if (stack.isEmpty()) return;

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null || !nbt.getBoolean(SILENCED_TAG)) {
            return;
        }

        long endTime = nbt.getLong(SILENCE_END_TIME_TAG);
        if (currentTime >= endTime) {
            // 恢复附魔
            restoreEnchantments(stack);
        }
    }

    /**
     * 恢复物品的附魔
     */
    private static void restoreEnchantments(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) return;

        // 恢复原始附魔
        if (nbt.hasKey(SILENCED_ENCHANTS_TAG)) {
            NBTTagList originalEnchants = nbt.getTagList(SILENCED_ENCHANTS_TAG, 10);
            nbt.setTag("ench", originalEnchants);
            nbt.removeTag(SILENCED_ENCHANTS_TAG);
        }

        // 清除封印标记
        nbt.removeTag(SILENCED_TAG);
        nbt.removeTag(SILENCE_END_TIME_TAG);

        Adversity.LOGGER.debug("Restored enchantments on item: {}", stack.getDisplayName());
    }

    /**
     * 播放解咒效果
     */
    private void playDisenchantEffects(EntityPlayer player, EntityLiving attacker) {
        if (player.world.isRemote) return;

        // 音效
        player.world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
            SoundCategory.HOSTILE,
            1.0f,
            0.5f  // 低音调表示解除
        );

        player.world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE,
            SoundCategory.HOSTILE,
            0.5f,
            1.2f
        );

        // 粒子效果
        if (player.world instanceof WorldServer) {
            ((WorldServer) player.world).spawnParticle(
                EnumParticleTypes.ENCHANTMENT_TABLE,
                player.posX, player.posY + 1, player.posZ,
                30,
                0.5, 0.5, 0.5,
                0.5
            );
            ((WorldServer) player.world).spawnParticle(
                EnumParticleTypes.SPELL_WITCH,
                player.posX, player.posY + 1, player.posZ,
                20,
                0.3, 0.3, 0.3,
                0.0
            );
        }
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
