package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.client.visual.VisualEffectHelper;
import com.adversity.client.visual.VisualEffectType;
import com.adversity.compat.BaublesCompat;
import com.adversity.config.AdversityConfig;
import com.adversity.item.ItemRegistry;
import com.adversity.item.ItemSealedToken;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.EnumHand;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Loader;

import java.util.*;

/**
 * 褫夺词条 - 封印玩家的饰品槽（Baubles）
 *
 * 机制：
 * 1. 攻击玩家时有概率封印随机一件饰品
 * 2. 饰品转化为令牌，N秒后自动恢复
 * 3. 不会封印不可卸下的饰品（canUnequip = false）
 * 4. 如果Baubles未安装，则退化为封印主手/副手
 */
public class DivestAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "divest");

    /** 基础触发概率 */
    private static final float BASE_CHANCE = 0.25f;

    /** 基础封印持续时间（tick） - 可通过配置覆盖 */
    private static final int BASE_SEAL_DURATION = 400;  // 20秒

    /** 冷却时间（tick） */
    private static final long SEAL_COOLDOWN = 60;  // 3秒

    /** Baubles是否可用 */
    private static Boolean baublesLoaded = null;

    /** 玩家冷却记录 */
    private static final Map<UUID, Long> playerCooldowns = new HashMap<>();

    private static final Random RANDOM = new Random();

    public DivestAffix() {
        super(
            ID,
            AffixType.UTILITY,
            40,     // 较低权重
            6.0f    // 难度6以上
        );
    }

    /**
     * 检查Baubles是否已加载
     */
    private static boolean isBaublesLoaded() {
        if (baublesLoaded == null) {
            baublesLoaded = Loader.isModLoaded("baubles");
        }
        return baublesLoaded;
    }

    @Override
    public float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data) {
        if (!(target instanceof EntityPlayer)) {
            return damage;
        }

        EntityPlayer player = (EntityPlayer) target;

        // 检查冷却
        long currentTime = player.world.getTotalWorldTime();
        UUID playerId = player.getUniqueID();
        Long lastSealTime = playerCooldowns.get(playerId);

        if (lastSealTime != null && (currentTime - lastSealTime) < SEAL_COOLDOWN) {
            return damage;
        }

        int tier = getTier(attacker);

        // 计算触发概率
        float chance = BASE_CHANCE + (tier * 0.03f);

        if (RANDOM.nextFloat() < chance) {
            // 设置冷却
            playerCooldowns.put(playerId, currentTime);

            boolean success;
            if (isBaublesLoaded()) {
                success = sealRandomBauble(player, attacker, tier, currentTime);
            } else {
                // Baubles未加载，退化为封印手持物品
                success = sealHeldItem(player, attacker, tier, currentTime);
            }

            if (!success) {
                playerCooldowns.remove(playerId);
            }
        }

        return damage;
    }

    /**
     * 封印随机一件Baubles饰品（使用令牌系统）
     */
    private boolean sealRandomBauble(EntityPlayer player, EntityLiving attacker, int tier, long currentTime) {
        if (ItemRegistry.SEALED_TOKEN == null) {
            Adversity.LOGGER.error("[Divest] SEALED_TOKEN未注册!");
            return false;
        }

        // 获取可封印的槽位（通过BaublesCompat）
        List<Integer> availableSlots = getBaubleAvailableSlots(player);

        if (availableSlots.isEmpty()) {
            Adversity.LOGGER.debug("[Divest] 没有可封印的饰品");
            return false;
        }

        // 随机选择一个槽位
        int targetSlot = availableSlots.get(RANDOM.nextInt(availableSlots.size()));

        // 获取物品（通过BaublesCompat）
        ItemStack targetItem = getBaubleStackInSlot(player, targetSlot);
        if (targetItem.isEmpty()) {
            return false;
        }

        // 创建快照
        NBTTagCompound itemNbt = targetItem.serializeNBT();
        String displayName = targetItem.getDisplayName();

        // 计算封印时间（从配置读取）
        long duration = AdversityConfig.affixSettings.divestSealDuration +
                       (tier * AdversityConfig.affixSettings.sealDurationPerTier);
        long endTime = currentTime + duration;

        // 获取槽位类型名称
        String slotType = getBaubleSlotTypeName(targetSlot);

        // 创建令牌
        ItemStack itemFromSnapshot = new ItemStack(itemNbt);
        ItemStack token = ItemSealedToken.createToken(itemFromSnapshot, slotType, targetSlot, endTime, duration);

        if (token.isEmpty() || !token.hasTagCompound()) {
            Adversity.LOGGER.error("[Divest] 令牌创建失败!");
            return false;
        }

        // 验证目标槽位状态未变
        ItemStack currentItem = getBaubleStackInSlot(player, targetSlot);
        if (currentItem.isEmpty() || !currentItem.getDisplayName().equals(displayName)) {
            Adversity.LOGGER.warn("[Divest] 目标槽位物品已变化，中止操作");
            return false;
        }

        // 清空Baubles槽位
        setBaubleStackInSlot(player, targetSlot, ItemStack.EMPTY);

        // 放置令牌到背包
        int tokenSlot = player.inventory.getFirstEmptyStack();
        if (tokenSlot != -1) {
            player.inventory.mainInventory.set(tokenSlot, token);
            Adversity.LOGGER.info("[Divest] 令牌放入背包槽 {}", tokenSlot);
        } else {
            player.dropItem(token, false);
            Adversity.LOGGER.info("[Divest] 背包满，令牌掉落到地上");
        }

        // 播放效果
        playSealEffects(player, attacker);
        VisualEffectHelper.sendToPlayer(player, VisualEffectType.VOID_GAZE, 30, 0.5f, attacker.getEntityId());

        Adversity.LOGGER.info("[Divest] 封印饰品成功: slot={}, item='{}'", targetSlot, displayName);
        return true;
    }

    /**
     * 封印手持物品（Baubles未加载时的退化行为）
     */
    private boolean sealHeldItem(EntityPlayer player, EntityLiving attacker, int tier, long currentTime) {
        if (ItemRegistry.SEALED_TOKEN == null) {
            return false;
        }

        // 尝试封印主手或副手
        ItemStack mainHand = player.getHeldItemMainhand();
        ItemStack offHand = player.getHeldItemOffhand();

        ItemStack targetItem = null;
        String slotType = null;
        int slotIndex = -1;
        boolean isMainHand = false;

        if (!mainHand.isEmpty() && !(mainHand.getItem() instanceof ItemSealedToken)) {
            if (!offHand.isEmpty() && !(offHand.getItem() instanceof ItemSealedToken) && RANDOM.nextBoolean()) {
                targetItem = offHand.copy();
                slotType = "OFFHAND";
            } else {
                targetItem = mainHand.copy();
                slotType = "MAINHAND";
                isMainHand = true;
            }
        } else if (!offHand.isEmpty() && !(offHand.getItem() instanceof ItemSealedToken)) {
            targetItem = offHand.copy();
            slotType = "OFFHAND";
        }

        if (targetItem == null) {
            return false;
        }

        // 计算封印时间（从配置读取）
        long duration = AdversityConfig.affixSettings.divestSealDuration +
                       (tier * AdversityConfig.affixSettings.sealDurationPerTier);
        long endTime = currentTime + duration;

        // 创建令牌
        ItemStack token = ItemSealedToken.createToken(targetItem, slotType, slotIndex, endTime, duration);

        if (token.isEmpty()) {
            return false;
        }

        // 清空原槽位
        if (isMainHand) {
            player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        } else {
            player.setHeldItem(EnumHand.OFF_HAND, ItemStack.EMPTY);
        }

        // 放置令牌
        int tokenSlot = player.inventory.getFirstEmptyStack();
        if (tokenSlot != -1) {
            player.inventory.mainInventory.set(tokenSlot, token);
        } else {
            player.dropItem(token, false);
        }

        // 播放效果
        playSealEffects(player, attacker);
        VisualEffectHelper.sendToPlayer(player, VisualEffectType.VOID_GAZE, 30, 0.5f, attacker.getEntityId());

        return true;
    }

    // ========== Baubles延迟加载方法 ==========

    private List<Integer> getBaubleAvailableSlots(EntityPlayer player) {
        return BaublesCompat.getAvailableSlotsForSeal(player);
    }

    private ItemStack getBaubleStackInSlot(EntityPlayer player, int slot) {
        return BaublesCompat.getStackInSlot(player, slot);
    }

    private void setBaubleStackInSlot(EntityPlayer player, int slot, ItemStack stack) {
        BaublesCompat.setStackInSlot(player, slot, stack);
    }

    private String getBaubleSlotTypeName(int slot) {
        return BaublesCompat.getSlotTypeName(slot);
    }

    // ========== 效果和工具方法 ==========

    private void playSealEffects(EntityPlayer player, EntityLiving attacker) {
        if (player.world.isRemote) return;

        // 音效
        player.world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            SoundEvents.ITEM_ARMOR_EQUIP_CHAIN,
            SoundCategory.HOSTILE,
            0.8f,
            0.6f
        );

        player.world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            SoundEvents.ENTITY_ENDERMEN_TELEPORT,
            SoundCategory.HOSTILE,
            0.3f,
            0.5f
        );

        // 粒子效果
        if (player.world instanceof WorldServer) {
            ((WorldServer) player.world).spawnParticle(
                EnumParticleTypes.PORTAL,
                player.posX, player.posY + 1, player.posZ,
                30,
                0.5, 0.5, 0.5,
                0.3
            );
        }
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }

    /**
     * 清理过期的冷却记录
     */
    public static void cleanupCooldowns(long currentTime) {
        playerCooldowns.entrySet().removeIf(entry ->
            (currentTime - entry.getValue()) > SEAL_COOLDOWN * 10);
    }
}
