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
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.WorldServer;

import java.util.*;

/**
 * 枷锁词条 - 封印玩家的装备槽
 *
 * 重写版本 - 确保时序正确：
 * 1. 冷却机制防止连续触发
 * 2. 先快照盔甲状态再操作
 * 3. 原子化操作
 */
public class ShackleAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "shackle");

    // 配置
    private static final float BASE_CHANCE = 0.2f;
    private static final int BASE_SEAL_DURATION = 600;  // 30秒
    private static final long SEAL_COOLDOWN = 60;  // 3秒冷却，防止连续触发

    // 玩家冷却记录
    private static final Map<UUID, Long> playerCooldowns = new HashMap<>();

    private static final Random RANDOM = new Random();

    public ShackleAffix() {
        super(ID, AffixType.UTILITY, 50, 5.0f);
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
            Adversity.LOGGER.debug("[Shackle] 冷却中，跳过 (剩余: {} ticks)",
                SEAL_COOLDOWN - (currentTime - lastSealTime));
            return damage;
        }

        int tier = getTier(attacker);
        float chance = BASE_CHANCE + (tier * 0.03f);
        float roll = RANDOM.nextFloat();

        Adversity.LOGGER.info("[Shackle] 触发检测: player={}, chance={}, roll={}, trigger={}",
            player.getName(), chance, roll, roll < chance);

        if (roll < chance) {
            // 设置冷却（在执行前设置，防止重入）
            playerCooldowns.put(playerId, currentTime);

            // 执行封印
            boolean success = executeSeal(player, attacker, tier, currentTime);

            if (!success) {
                // 封印失败，移除冷却
                playerCooldowns.remove(playerId);
            }
        }

        return damage;
    }

    /**
     * 执行封印操作
     *
     * 时序：
     * 1. 快照当前盔甲状态（NBT序列化，完全独立）
     * 2. 验证有可封印的盔甲
     * 3. 随机选择目标
     * 4. 创建令牌
     * 5. 验证目标槽位状态未变
     * 6. 原子执行：清空盔甲槽 + 添加令牌
     */
    private boolean executeSeal(EntityPlayer player, EntityLiving attacker, int tier, long currentTime) {
        if (ItemRegistry.SEALED_TOKEN == null) {
            Adversity.LOGGER.error("[Shackle] SEALED_TOKEN未注册!");
            return false;
        }

        // ===== 第一步：快照盔甲状态 =====
        ArmorSnapshot[] snapshots = new ArmorSnapshot[4];
        List<Integer> availableSlots = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            ItemStack armor = player.inventory.armorInventory.get(i);
            if (!armor.isEmpty()) {
                // 完全序列化为NBT，创建独立快照
                snapshots[i] = new ArmorSnapshot(i, armor.serializeNBT(), armor.getDisplayName());

                if (!(armor.getItem() instanceof ItemSealedToken)) {
                    availableSlots.add(i);
                }
            }
        }

        Adversity.LOGGER.info("[Shackle] 快照完成，可封印槽位: {}", availableSlots);

        if (availableSlots.isEmpty()) {
            Adversity.LOGGER.info("[Shackle] 没有可封印的盔甲");
            return false;
        }

        // ===== 第二步：选择目标 =====
        int targetSlot = availableSlots.get(RANDOM.nextInt(availableSlots.size()));
        ArmorSnapshot targetSnapshot = snapshots[targetSlot];

        Adversity.LOGGER.info("[Shackle] 选中槽位: {}, 物品: '{}'",
            targetSlot, targetSnapshot.displayName);

        // ===== 第三步：从快照创建令牌 =====
        ItemStack armorFromSnapshot = new ItemStack(targetSnapshot.nbt);
        if (armorFromSnapshot.isEmpty()) {
            Adversity.LOGGER.error("[Shackle] 从快照恢复物品失败!");
            return false;
        }

        long duration = BASE_SEAL_DURATION + (tier * 100);
        long endTime = currentTime + duration;
        String slotType = getSlotType(targetSlot);

        ItemStack token = ItemSealedToken.createToken(armorFromSnapshot, slotType, targetSlot, endTime, duration);
        if (token.isEmpty() || !token.hasTagCompound()) {
            Adversity.LOGGER.error("[Shackle] 令牌创建失败!");
            return false;
        }

        // ===== 第四步：验证当前状态与快照一致 =====
        ItemStack currentArmor = player.inventory.armorInventory.get(targetSlot);
        if (currentArmor.isEmpty()) {
            Adversity.LOGGER.warn("[Shackle] 目标槽位已变空，中止操作");
            return false;
        }

        // 比较当前物品与快照是否一致（通过显示名称简单比较）
        if (!currentArmor.getDisplayName().equals(targetSnapshot.displayName)) {
            Adversity.LOGGER.warn("[Shackle] 目标槽位物品已变化，中止操作");
            return false;
        }

        // ===== 第五步：原子执行 =====
        // 先找到令牌的目标位置
        int tokenSlot = player.inventory.getFirstEmptyStack();
        if (tokenSlot == -1) {
            // 背包满，直接掉落令牌
            Adversity.LOGGER.info("[Shackle] 背包满，令牌将掉落");
        }

        // 执行：清空盔甲槽
        player.inventory.armorInventory.set(targetSlot, ItemStack.EMPTY);

        // 执行：放置令牌
        if (tokenSlot != -1) {
            player.inventory.mainInventory.set(tokenSlot, token);
            Adversity.LOGGER.info("[Shackle] 令牌放入背包槽 {}", tokenSlot);
        } else {
            player.dropItem(token, false);
            Adversity.LOGGER.info("[Shackle] 令牌掉落到地上");
        }

        // ===== 第六步：验证结果 =====
        Adversity.LOGGER.info("[Shackle] === 操作后盔甲状态 ===");
        for (int i = 0; i < 4; i++) {
            ItemStack a = player.inventory.armorInventory.get(i);
            Adversity.LOGGER.info("[Shackle] slot[{}] = '{}' (empty={})",
                i, a.getDisplayName(), a.isEmpty());
        }

        // 标记脏
        player.inventory.markDirty();

        // 播放效果
        playSealEffects(player, attacker);
        VisualEffectHelper.sendToPlayer(player, VisualEffectType.GRAVITY_DISTORT, 40, 0.5f, attacker.getEntityId());

        Adversity.LOGGER.info("[Shackle] 封印成功完成");
        return true;
    }

    private String getSlotType(int index) {
        switch (index) {
            case 0: return "ARMOR_FEET";
            case 1: return "ARMOR_LEGS";
            case 2: return "ARMOR_CHEST";
            case 3: return "ARMOR_HEAD";
            default: return "ARMOR";
        }
    }

    private void playSealEffects(EntityPlayer player, EntityLiving attacker) {
        if (player.world.isRemote) return;

        player.world.playSound(null, player.posX, player.posY, player.posZ,
            SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.HOSTILE, 0.5f, 0.5f);
        player.world.playSound(null, player.posX, player.posY, player.posZ,
            SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.HOSTILE, 0.5f, 0.8f);

        if (player.world instanceof WorldServer) {
            ((WorldServer) player.world).spawnParticle(EnumParticleTypes.CRIT,
                player.posX, player.posY + 1, player.posZ, 20, 0.5, 0.5, 0.5, 0.1);
            ((WorldServer) player.world).spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                player.posX, player.posY + 1, player.posZ, 15, 0.3, 0.3, 0.3, 0.05);
        }
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }

    /**
     * 盔甲快照 - 完全独立的物品状态记录
     */
    private static class ArmorSnapshot {
        final int slot;
        final NBTTagCompound nbt;
        final String displayName;

        ArmorSnapshot(int slot, NBTTagCompound nbt, String displayName) {
            this.slot = slot;
            this.nbt = nbt.copy();  // 深拷贝NBT
            this.displayName = displayName;
        }
    }

    /**
     * 清理过期的冷却记录（可选，防止内存泄漏）
     */
    public static void cleanupCooldowns(long currentTime) {
        playerCooldowns.entrySet().removeIf(entry ->
            (currentTime - entry.getValue()) > SEAL_COOLDOWN * 10);
    }
}
