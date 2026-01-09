package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.client.visual.VisualEffectHelper;
import com.adversity.client.visual.VisualEffectType;
import com.adversity.item.ItemRegistry;
import com.adversity.item.ItemSealedToken;
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
        float roll = RANDOM.nextFloat();

        Adversity.LOGGER.info("[SealToken] onAttack triggered: player={}, tier={}, chance={}, roll={}, willTrigger={}",
            player.getName(), tier, chance, roll, roll < chance);

        if (roll < chance) {
            sealRandomEquipment(player, attacker, tier);
        }

        return damage;
    }

    /**
     * 随机封印一件盔甲
     */
    private void sealRandomEquipment(EntityPlayer player, EntityLiving attacker, int tier) {
        // 检查物品是否已注册
        if (ItemRegistry.SEALED_TOKEN == null) {
            return;
        }

        // 直接检查四个盔甲槽，不使用循环
        // armorInventory: 0=boots, 1=legs, 2=chest, 3=head
        int[] armorIndices = {0, 1, 2, 3};
        List<Integer> availableIndices = new ArrayList<>();

        for (int i : armorIndices) {
            ItemStack armor = player.inventory.armorInventory.get(i);
            if (!armor.isEmpty() && !(armor.getItem() instanceof ItemSealedToken)) {
                availableIndices.add(i);
            }
        }

        if (availableIndices.isEmpty()) {
            return;
        }

        // 随机选择一个盔甲槽
        int targetIndex = availableIndices.get(RANDOM.nextInt(availableIndices.size()));

        // 直接从armorInventory获取并复制
        ItemStack originalArmor = player.inventory.armorInventory.get(targetIndex);
        ItemStack armorCopy = originalArmor.copy();

        if (armorCopy.isEmpty()) {
            return;
        }

        // 计算封印时间
        long currentTime = player.world.getTotalWorldTime();
        long duration = BASE_SEAL_DURATION + (tier * 100);
        long endTime = currentTime + duration;

        // 获取槽位类型
        String slotType;
        switch (targetIndex) {
            case 0: slotType = "ARMOR_FEET"; break;
            case 1: slotType = "ARMOR_LEGS"; break;
            case 2: slotType = "ARMOR_CHEST"; break;
            case 3: slotType = "ARMOR_HEAD"; break;
            default: slotType = "ARMOR"; break;
        }

        // 创建令牌
        ItemStack token = ItemSealedToken.createToken(armorCopy, slotType, targetIndex, endTime, duration);
        if (token.isEmpty() || !token.hasTagCompound()) {
            Adversity.LOGGER.error("[SealToken] Failed to create token!");
            return;
        }

        Adversity.LOGGER.info("[SealToken] Sealing '{}' from armor slot {}", armorCopy.getDisplayName(), targetIndex);

        // 直接设置armorInventory槽位为空
        player.inventory.armorInventory.set(targetIndex, ItemStack.EMPTY);

        // 添加令牌到背包
        if (!player.inventory.addItemStackToInventory(token)) {
            player.dropItem(token, false);
        }

        // 播放效果
        playSealEffects(player, attacker);
        VisualEffectHelper.sendToPlayer(player, VisualEffectType.GRAVITY_DISTORT, 40, 0.5f, attacker.getEntityId());
    }

    /**
     * 获取槽位类型字符串
     */
    private String getSlotTypeString(EntityEquipmentSlot slot) {
        switch (slot) {
            case HEAD: return "ARMOR_HEAD";
            case CHEST: return "ARMOR_CHEST";
            case LEGS: return "ARMOR_LEGS";
            case FEET: return "ARMOR_FEET";
            case MAINHAND: return "MAINHAND";
            case OFFHAND: return "OFFHAND";
            default: return "INVENTORY";
        }
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
