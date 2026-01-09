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
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 湮灭词条 - 临时将物品删除到虚空
 *
 * 机制：
 * 1. 攻击玩家时有概率将背包中的随机物品"湮灭"
 * 2. 物品暂时消失，存储到虚空中
 * 3. 一段时间后物品会自动返还到玩家背包
 * 4. 如果背包已满，物品会掉落在地上
 * 5. 设计理念：心理压力大于实际损失
 */
public class AnnihilateAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "annihilate");

    /** 基础触发概率 */
    private static final float BASE_CHANCE = 0.15f;  // 15%

    /** 物品返还时间（tick） */
    private static final int BASE_RETURN_TIME = 1200;  // 60秒

    private static final Random RANDOM = new Random();

    public AnnihilateAffix() {
        super(
            ID,
            AffixType.SPECIAL,
            30,     // 较低权重
            7.0f    // 难度7以上
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
        float chance = BASE_CHANCE + (tier * 0.02f);

        if (RANDOM.nextFloat() < chance) {
            annihilateRandomItem(player, attacker, tier);
        }

        return damage;
    }

    /**
     * 湮灭随机一件物品
     */
    private void annihilateRandomItem(EntityPlayer player, EntityLiving attacker, int tier) {
        // 收集背包中可湮灭的物品槽位
        List<Integer> availableSlots = new ArrayList<>();

        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            ItemStack stack = player.inventory.mainInventory.get(i);
            if (!stack.isEmpty()) {
                availableSlots.add(i);
            }
        }

        if (availableSlots.isEmpty()) {
            return;
        }

        // 随机选择一个槽位
        int targetSlot = availableSlots.get(RANDOM.nextInt(availableSlots.size()));
        ItemStack targetItem = player.inventory.mainInventory.get(targetSlot);

        // 计算返还时间
        long returnTime = player.world.getTotalWorldTime() + BASE_RETURN_TIME + (tier * 200);

        // 存入虚空
        SealedItemManager manager = SealedItemManager.get(player.world);
        manager.storeInVoid(player, targetItem, returnTime, targetSlot);

        // 从背包移除
        player.inventory.mainInventory.set(targetSlot, ItemStack.EMPTY);

        // 播放效果
        playAnnihilateEffects(player, attacker);

        // 发送视觉效果
        VisualEffectHelper.sendToPlayer(player, VisualEffectType.VOID_GAZE, 60, 0.8f, attacker.getEntityId());

        Adversity.LOGGER.debug("Annihilated item from slot {} for player {}", targetSlot, player.getName());
    }

    @Override
    public void onTick(EntityLiving entity, IAffixData data) {
        // 每2秒检查并返还虚空物品
        if (data.getTickCount() % 40 != 0) {
            return;
        }

        if (entity.world.isRemote) {
            return;
        }

        // 获取附近的玩家并检查是否有物品需要返还
        List<EntityPlayer> nearbyPlayers = entity.world.getEntitiesWithinAABB(
            EntityPlayer.class,
            entity.getEntityBoundingBox().grow(64),
            p -> p != null && p.isEntityAlive()
        );

        SealedItemManager manager = SealedItemManager.get(entity.world);
        long currentTime = entity.world.getTotalWorldTime();

        for (EntityPlayer player : nearbyPlayers) {
            List<SealedItemManager.VoidStoredItem> returnItems = manager.getReturnableItems(player, currentTime);

            for (SealedItemManager.VoidStoredItem item : returnItems) {
                returnItemToPlayer(player, item);
            }
        }
    }

    /**
     * 将物品返还给玩家
     */
    private void returnItemToPlayer(EntityPlayer player, SealedItemManager.VoidStoredItem item) {
        // 尝试放回原槽位
        if (item.slotIndex >= 0 && item.slotIndex < player.inventory.mainInventory.size()) {
            if (player.inventory.mainInventory.get(item.slotIndex).isEmpty()) {
                player.inventory.mainInventory.set(item.slotIndex, item.stack);
                playReturnEffects(player);
                return;
            }
        }

        // 尝试放入背包其他位置
        if (player.inventory.addItemStackToInventory(item.stack)) {
            playReturnEffects(player);
            return;
        }

        // 背包已满，掉落在地上
        player.dropItem(item.stack, false);
        playReturnEffects(player);
    }

    /**
     * 播放湮灭效果
     */
    private void playAnnihilateEffects(EntityPlayer player, EntityLiving attacker) {
        if (player.world.isRemote) return;

        // 音效
        player.world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            SoundEvents.ENTITY_ENDERMEN_TELEPORT,
            SoundCategory.HOSTILE,
            1.0f,
            0.5f
        );

        player.world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            SoundEvents.ENTITY_ELDER_GUARDIAN_AMBIENT,
            SoundCategory.HOSTILE,
            0.5f,
            0.8f
        );

        // 粒子效果 - 物品消失
        if (player.world instanceof WorldServer) {
            ((WorldServer) player.world).spawnParticle(
                EnumParticleTypes.PORTAL,
                player.posX, player.posY + 1, player.posZ,
                50,
                0.5, 0.5, 0.5,
                0.5
            );
            ((WorldServer) player.world).spawnParticle(
                EnumParticleTypes.END_ROD,
                player.posX, player.posY + 1, player.posZ,
                20,
                0.3, 0.3, 0.3,
                0.1
            );
        }
    }

    /**
     * 播放返还效果
     */
    private void playReturnEffects(EntityPlayer player) {
        if (player.world.isRemote) return;

        // 音效
        player.world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            SoundEvents.ENTITY_ITEM_PICKUP,
            SoundCategory.PLAYERS,
            0.5f,
            1.0f
        );

        player.world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            SoundEvents.ENTITY_ENDERMEN_TELEPORT,
            SoundCategory.PLAYERS,
            0.3f,
            1.5f
        );

        // 粒子效果 - 物品出现
        if (player.world instanceof WorldServer) {
            ((WorldServer) player.world).spawnParticle(
                EnumParticleTypes.PORTAL,
                player.posX, player.posY + 1, player.posZ,
                20,
                0.3, 0.3, 0.3,
                0.1
            );
        }
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
