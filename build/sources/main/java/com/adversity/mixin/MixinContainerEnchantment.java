package com.adversity.mixin;

import com.adversity.Adversity;
import com.adversity.config.AdversityConfig;
import com.adversity.enchantment.EnchantmentRegistry;
import com.adversity.sanctuary.SanctuaryManager;
import com.adversity.sanctuary.SanctuaryZone;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.Random;

/**
 * Mixin 注入 ContainerEnchantment
 * 圣所附近附魔时注入 Adversity 特殊附魔
 * 不在圣所附近时过滤 Adversity 附魔
 * 
 * 完全使用反射，不使用 @Shadow 避免字段映射问题
 */
@Mixin(value = ContainerEnchantment.class, remap = false)
public abstract class MixinContainerEnchantment {

    private static final Random RANDOM = new Random();

    /**
     * 在附魔选项更新后处理 Adversity 附魔
     */
    @Inject(method = "onCraftMatrixChanged", at = @At("RETURN"))
    private void adversity$handleEnchantments(net.minecraft.inventory.IInventory inv, CallbackInfo ci) {
        try {
            // 通过反射获取 playerInventory (在 ContainerEnchantment 中)
            InventoryPlayer playerInv = getPlayerInventory();
            if (playerInv == null || playerInv.player == null)
                return;

            EntityPlayer player = playerInv.player;

            // Debug: Mixin已执行
            Adversity.LOGGER.info("[Enchant Mixin] onCraftMatrixChanged triggered for player: {}", player.getName());

            // 检查玩家是否在圣所附近
            boolean nearSanctuary = isNearSanctuary(player);
            Adversity.LOGGER.info("[Enchant Mixin] Near sanctuary: {}", nearSanctuary);

            if (nearSanctuary) {
                // 在圣所附近时，尝试注入 Adversity 附魔
                Adversity.LOGGER.info("[Enchant Mixin] Attempting to inject Adversity enchantments...");
                injectAdversityEnchantments(player);
            } else if (AdversityConfig.sanctuarySettings.removeEnchantsFromTable) {
                // 不在圣所附近且启用过滤时，移除 Adversity 附魔
                filterAdversityEnchantments();
            }
        } catch (Exception e) {
            Adversity.LOGGER.debug("[Enchant Mixin] Error: {}", e.getMessage());
        }
    }

    /**
     * 获取 playerInventory 字段 (在 ContainerEnchantment 中)
     */
    private InventoryPlayer getPlayerInventory() {
        try {
            Field field = ContainerEnchantment.class.getDeclaredField("playerInventory");
            field.setAccessible(true);
            return (InventoryPlayer) field.get(this);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查玩家是否在圣所附近
     */
    private boolean isNearSanctuary(EntityPlayer player) {
        SanctuaryZone zone = SanctuaryManager.getPlayerSanctuary(player);
        if (zone == null || !zone.isActive()) {
            return false;
        }

        double dist = player.getDistanceSq(zone.center);
        double maxDist = AdversityConfig.sanctuarySettings.craftingProximity *
                AdversityConfig.sanctuarySettings.craftingProximity;
        return dist <= maxDist;
    }

    /**
     * 注入 Adversity 附魔到附魔选项中 (带阶段检查)
     */
    private void injectAdversityEnchantments(EntityPlayer player) {
        try {
            int[] enchantClue = getIntArrayField("enchantClue");
            int[] worldClue = getIntArrayField("worldClue");

            if (enchantClue == null || worldClue == null)
                return;

            // 获取玩家阶段能力
            com.adversity.capability.IAdversityCapability.IProgression progression = player
                    .getCapability(com.adversity.capability.CapabilityHandler.PROGRESSION_CAPABILITY, null);

            Enchantment[] adversityEnchants = {
                    EnchantmentRegistry.SOULBOUND,
                    EnchantmentRegistry.BREAKER,
                    EnchantmentRegistry.ENTROPY_AFFINITY,
                    EnchantmentRegistry.PURIFYING_TOUCH,
                    EnchantmentRegistry.RESOLUTE_WILL
            };

            for (int i = 0; i < enchantClue.length; i++) {
                if (RANDOM.nextFloat() < 0.25f) {
                    Enchantment selectedEnchant = adversityEnchants[RANDOM.nextInt(adversityEnchants.length)];
                    if (selectedEnchant != null) {
                        // 检查阶段要求
                        String requiredStage = com.adversity.sanctuary.StageGatingRegistry
                                .getEnchantmentStageRequirement(selectedEnchant);
                        if (requiredStage != null && (progression == null || !progression.hasStage(requiredStage))) {
                            // 玩家不满足阶段要求，跳过此附魔
                            continue;
                        }

                        int enchId = Enchantment.getEnchantmentID(selectedEnchant);
                        int level = 1 + RANDOM.nextInt(selectedEnchant.getMaxLevel());

                        enchantClue[i] = enchId;
                        worldClue[i] = level;

                        Adversity.LOGGER.info("[Enchant Mixin] Injected {} level {} into slot {}",
                                selectedEnchant.getRegistryName(), level, i);
                    }
                }
            }
        } catch (Exception e) {
            Adversity.LOGGER.debug("Failed to inject Adversity enchantments: {}", e.getMessage());
        }
    }

    /**
     * 过滤 Adversity 附魔 (不在圣所附近时)
     */
    private void filterAdversityEnchantments() {
        try {
            int[] enchantClue = getIntArrayField("enchantClue");
            int[] worldClue = getIntArrayField("worldClue");

            if (enchantClue == null)
                return;

            for (int i = 0; i < enchantClue.length; i++) {
                if (enchantClue[i] >= 0) {
                    Enchantment ench = Enchantment.getEnchantmentByID(enchantClue[i]);
                    if (isAdversityEnchantment(ench)) {
                        enchantClue[i] = -1;
                        if (worldClue != null && i < worldClue.length) {
                            worldClue[i] = 0;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 静默失败
        }
    }

    /**
     * 获取 int[] 字段值
     */
    private int[] getIntArrayField(String fieldName) {
        try {
            Field field = ContainerEnchantment.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (int[]) field.get(this);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查是否是 Adversity 附魔
     */
    private boolean isAdversityEnchantment(Enchantment ench) {
        if (ench == null)
            return false;
        return ench == EnchantmentRegistry.SOULBOUND ||
                ench == EnchantmentRegistry.BREAKER ||
                ench == EnchantmentRegistry.ENTROPY_AFFINITY ||
                ench == EnchantmentRegistry.PURIFYING_TOUCH ||
                ench == EnchantmentRegistry.RESOLUTE_WILL;
    }
}
