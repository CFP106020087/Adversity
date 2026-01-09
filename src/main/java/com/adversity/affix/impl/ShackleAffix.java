package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.client.visual.VisualEffectHelper;
import com.adversity.client.visual.VisualEffectType;
import com.adversity.seal.SealedItemManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 枷锁词条 - 封印玩家的装备槽
 *
 * 机制：
 * 1. 攻击玩家时有概率封印随机一件装备
 * 2. 封印持续一段时间后自动解除
 * 3. 封印期间装备效果无效（通过NBT标记）
 * 4. 设计理念：迫使玩家裸装战斗
 */
public class ShackleAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "shackle");

    /** 基础触发概率 */
    private static final float BASE_CHANCE = 0.2f;  // 20%

    /** 封印持续时间（tick） */
    private static final int BASE_SEAL_DURATION = 600;  // 30秒

    private static final Random RANDOM = new Random();

    public ShackleAffix() {
        super(
            ID,
            AffixType.UTILITY,
            50,     // 中等权重
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

        // 计算触发概率
        float chance = BASE_CHANCE + (tier * 0.03f);

        if (RANDOM.nextFloat() < chance) {
            sealRandomEquipment(player, attacker, tier);
        }

        return damage;
    }

    /**
     * 随机封印一件装备
     */
    private void sealRandomEquipment(EntityPlayer player, EntityLiving attacker, int tier) {
        // 收集可封印的装备槽
        List<EntityEquipmentSlot> availableSlots = new ArrayList<>();

        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            ItemStack stack = player.getItemStackFromSlot(slot);
            if (!stack.isEmpty() && !SealedItemManager.isSealed(stack)) {
                availableSlots.add(slot);
            }
        }

        if (availableSlots.isEmpty()) {
            return;  // 没有可封印的装备
        }

        // 随机选择一个槽位
        EntityEquipmentSlot targetSlot = availableSlots.get(RANDOM.nextInt(availableSlots.size()));
        ItemStack targetItem = player.getItemStackFromSlot(targetSlot);

        // 计算封印时间
        long currentTime = player.world.getTotalWorldTime();
        long endTime = currentTime + BASE_SEAL_DURATION + (tier * 100);
        long durationTicks = endTime - currentTime;

        Adversity.LOGGER.info("[SealDebug] Sealing '{}' in slot {} for player {}, endTime={}, duration={} ticks ({} sec)",
            targetItem.getDisplayName(), targetSlot, player.getName(), endTime, durationTicks, durationTicks / 20);

        // 执行封印
        ItemStack sealedItem = SealedItemManager.sealItem(targetItem, endTime);

        Adversity.LOGGER.info("[SealDebug] Created sealed item, isSealed={}, sealEndTime={}",
            SealedItemManager.isSealed(sealedItem), SealedItemManager.getSealEndTime(sealedItem));

        player.setItemStackToSlot(targetSlot, sealedItem);

        // 播放效果
        playSealEffects(player, attacker);

        // 发送视觉效果
        VisualEffectHelper.sendToPlayer(player, VisualEffectType.GRAVITY_DISTORT, 40, 0.5f, attacker.getEntityId());
    }

    /**
     * 播放封印效果
     */
    private void playSealEffects(EntityPlayer player, EntityLiving attacker) {
        if (player.world.isRemote) return;

        // 音效
        player.world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            SoundEvents.BLOCK_ANVIL_PLACE,
            SoundCategory.HOSTILE,
            0.5f,
            0.5f
        );

        player.world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE,
            SoundCategory.HOSTILE,
            0.5f,
            0.8f
        );

        // 粒子效果
        if (player.world instanceof WorldServer) {
            // 锁链粒子效果
            ((WorldServer) player.world).spawnParticle(
                EnumParticleTypes.CRIT,
                player.posX, player.posY + 1, player.posZ,
                20,
                0.5, 0.5, 0.5,
                0.1
            );
            ((WorldServer) player.world).spawnParticle(
                EnumParticleTypes.SMOKE_NORMAL,
                player.posX, player.posY + 1, player.posZ,
                15,
                0.3, 0.3, 0.3,
                0.05
            );
        }
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
