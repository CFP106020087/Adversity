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
import net.minecraft.nbt.NBTTagCompound;
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
     *
     * 只操作目标槽位，完全不碰其他槽位
     */
    private void sealRandomEquipment(EntityPlayer player, EntityLiving attacker, int tier) {
        // 检查物品是否已注册
        if (ItemRegistry.SEALED_TOKEN == null) {
            return;
        }

        // 收集可封印的槽位索引
        List<Integer> availableIndices = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            ItemStack armor = player.inventory.armorInventory.get(i);
            if (!armor.isEmpty() && !(armor.getItem() instanceof ItemSealedToken)) {
                availableIndices.add(i);
            }
        }

        Adversity.LOGGER.info("[SealToken] 可封印槽位: {}", availableIndices);

        if (availableIndices.isEmpty()) {
            Adversity.LOGGER.info("[SealToken] 没有可封印的盔甲!");
            return;
        }

        // 随机选择一个盔甲槽
        int targetIndex = availableIndices.get(RANDOM.nextInt(availableIndices.size()));

        // 获取目标物品
        ItemStack armorToSeal = player.inventory.armorInventory.get(targetIndex);

        // 立即序列化为NBT，保存当前状态
        NBTTagCompound armorNBT = armorToSeal.serializeNBT();

        Adversity.LOGGER.info("[SealToken] 选中槽位: {}, 物品: '{}'",
            targetIndex, armorToSeal.getDisplayName());

        // 从NBT创建完全独立的副本用于令牌（先创建令牌，成功后再清空槽位）
        ItemStack armorForToken = new ItemStack(armorNBT);

        if (armorForToken.isEmpty()) {
            Adversity.LOGGER.error("[SealToken] 物品副本为空!");
            return;
        }

        // 计算封印时间
        long currentTime = player.world.getTotalWorldTime();
        long duration = BASE_SEAL_DURATION + (tier * 100);
        long endTime = currentTime + duration;

        // 获取槽位类型字符串
        String slotType;
        switch (targetIndex) {
            case 0: slotType = "ARMOR_FEET"; break;
            case 1: slotType = "ARMOR_LEGS"; break;
            case 2: slotType = "ARMOR_CHEST"; break;
            case 3: slotType = "ARMOR_HEAD"; break;
            default: slotType = "ARMOR"; break;
        }

        // 先创建令牌（在清空槽位之前）
        ItemStack token = ItemSealedToken.createToken(armorForToken, slotType, targetIndex, endTime, duration);
        if (token.isEmpty() || !token.hasTagCompound()) {
            Adversity.LOGGER.error("[SealToken] 令牌创建失败!");
            return;
        }

        Adversity.LOGGER.info("[SealToken] 令牌创建成功，现在清空槽位");

        // 令牌创建成功后，才清空槽位
        player.inventory.armorInventory.set(targetIndex, ItemStack.EMPTY);

        // 验证盔甲状态
        Adversity.LOGGER.info("[SealToken] === 当前盔甲状态 ===");
        for (int i = 0; i < 4; i++) {
            ItemStack a = player.inventory.armorInventory.get(i);
            Adversity.LOGGER.info("[SealToken] armorInventory[{}] = '{}' (empty={})",
                i, a.getDisplayName(), a.isEmpty());
        }

        // 添加令牌到背包
        if (!player.inventory.addItemStackToInventory(token)) {
            player.dropItem(token, false);
            Adversity.LOGGER.info("[SealToken] 背包满，令牌掉落");
        } else {
            Adversity.LOGGER.info("[SealToken] 令牌添加到背包");
        }

        // 只标记脏，让MC自己同步，不强制调用sendContainerToPlayer
        player.inventory.markDirty();

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
