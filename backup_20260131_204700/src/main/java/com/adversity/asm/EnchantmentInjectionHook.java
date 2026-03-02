package com.adversity.asm;

import com.adversity.Adversity;
import com.adversity.config.AdversityConfig;
import com.adversity.enchantment.EnchantmentRegistry;
import com.adversity.sanctuary.SanctuaryManager;
import com.adversity.sanctuary.SanctuaryZone;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ContainerEnchantment;

import java.lang.reflect.Field;
import java.util.Random;

/**
 * Hook for ContainerEnchantment enchantment injection
 * Called via ASM from onCraftMatrixChanged
 */
public class EnchantmentInjectionHook {

    private static final Random RANDOM = new Random();

    /**
     * Called at the end of ContainerEnchantment.onCraftMatrixChanged
     * Injects or filters Adversity enchantments based on sanctuary proximity
     */
    public static void onEnchantmentUpdate(ContainerEnchantment container) {
        try {
            // 获取 world 和 position
            net.minecraft.world.World world = getField(container, "world", net.minecraft.world.World.class);
            net.minecraft.util.math.BlockPos pos = getField(container, "position",
                    net.minecraft.util.math.BlockPos.class);

            if (world == null || pos == null) {
                return;
            }

            // 找到在附魔台附近的玩家 (16格范围)
            EntityPlayer player = world.getClosestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 16.0,
                    false);
            if (player == null) {
                return;
            }

            System.out.println(
                    "[Adversity] EnchantmentInjectionHook: Player " + player.getName() + " at enchanting table");

            // 检查玩家是否在圣所附近
            boolean nearSanctuary = isNearSanctuary(player);
            System.out.println("[Adversity] Near sanctuary: " + nearSanctuary);

            if (nearSanctuary) {
                injectAdversityEnchantments(container);
            } else if (AdversityConfig.sanctuarySettings.removeEnchantsFromTable) {
                filterAdversityEnchantments(container);
            }
        } catch (Exception e) {
            System.err.println("[Adversity] EnchantmentInjectionHook error: " + e.getMessage());
        }
    }

    private static boolean isNearSanctuary(EntityPlayer player) {
        SanctuaryZone zone = SanctuaryManager.getPlayerSanctuary(player);
        if (zone == null || !zone.isActive()) {
            return false;
        }

        double dist = player.getDistanceSq(zone.center);
        double maxDist = AdversityConfig.sanctuarySettings.craftingProximity *
                AdversityConfig.sanctuarySettings.craftingProximity;
        return dist <= maxDist;
    }

    private static void injectAdversityEnchantments(ContainerEnchantment container) {
        try {
            int[] enchantClue = getField(container, "enchantClue", int[].class);
            int[] worldClue = getField(container, "worldClue", int[].class);

            if (enchantClue == null || worldClue == null) {
                System.out.println("[Adversity] enchantClue or worldClue is null");
                return;
            }

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
                        int enchId = Enchantment.getEnchantmentID(selectedEnchant);
                        int level = 1 + RANDOM.nextInt(selectedEnchant.getMaxLevel());

                        enchantClue[i] = enchId;
                        worldClue[i] = level;

                        System.out.println("[Adversity] Injected " + selectedEnchant.getRegistryName() +
                                " level " + level + " into slot " + i);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Adversity] Failed to inject enchantments: " + e.getMessage());
        }
    }

    private static void filterAdversityEnchantments(ContainerEnchantment container) {
        try {
            int[] enchantClue = getField(container, "enchantClue", int[].class);
            int[] worldClue = getField(container, "worldClue", int[].class);

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
            // Silent fail
        }
    }

    private static boolean isAdversityEnchantment(Enchantment ench) {
        if (ench == null)
            return false;
        return ench == EnchantmentRegistry.SOULBOUND ||
                ench == EnchantmentRegistry.BREAKER ||
                ench == EnchantmentRegistry.ENTROPY_AFFINITY ||
                ench == EnchantmentRegistry.PURIFYING_TOUCH ||
                ench == EnchantmentRegistry.RESOLUTE_WILL;
    }

    // 用于在 Pre-hook 和 Post-hook 之间传递选中的附魔
    private static final ThreadLocal<AdversityEnchantData> PENDING_ENCHANT = new ThreadLocal<>();

    private static class AdversityEnchantData {
        Enchantment enchantment;
        int level;

        AdversityEnchantData(Enchantment enchantment, int level) {
            this.enchantment = enchantment;
            this.level = level;
        }
    }

    /**
     * Called at the BEGINNING of ContainerEnchantment.enchantItem
     * Captures the intended Adversity enchantment from the clue BEFORE seed reset
     */
    public static void onEnchantItemPre(ContainerEnchantment container, EntityPlayer player, int slotId) {
        PENDING_ENCHANT.remove(); // Clear previous

        try {
            if (!isNearSanctuary(player))
                return;

            int[] enchantClue = getField(container, "enchantClue", int[].class);
            int[] worldClue = getField(container, "worldClue", int[].class);

            if (enchantClue == null || worldClue == null || slotId < 0 || slotId >= enchantClue.length) {
                return;
            }

            int enchId = enchantClue[slotId];
            int level = worldClue[slotId];

            Enchantment ench = Enchantment.getEnchantmentByID(enchId);
            if (isAdversityEnchantment(ench)) {
                PENDING_ENCHANT.set(new AdversityEnchantData(ench, level));
            }
        } catch (Exception e) {
            // Check failed silently
        }
    }

    /**
     * Called at the END of ContainerEnchantment.enchantItem
     * Applies the captured enchantment
     */
    public static void onEnchantItemAfter(boolean successes, ContainerEnchantment container, EntityPlayer player,
            int slotId) {
        AdversityEnchantData data = PENDING_ENCHANT.get();
        PENDING_ENCHANT.remove(); // Always cleanup

        if (!successes || data == null)
            return;

        try {
            // 获取附魔槽中的物品 (此时可能是附魔书了)
            net.minecraft.inventory.IInventory tableInv = getField(container, "tableInventory",
                    net.minecraft.inventory.IInventory.class);
            if (tableInv == null)
                return;

            net.minecraft.item.ItemStack stack = tableInv.getStackInSlot(0);
            if (stack.isEmpty())
                return;

            // 强制添加附魔（即使是书也直接添加 NBT）
            if (stack.getItem() == net.minecraft.init.Items.ENCHANTED_BOOK) {
                net.minecraft.item.ItemEnchantedBook.addEnchantment(stack,
                        new net.minecraft.enchantment.EnchantmentData(data.enchantment, data.level));
            } else {
                stack.addEnchantment(data.enchantment, data.level);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(Object obj, String fieldName, Class<T> type) {
        // 先尝试 MCP 名称
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(obj);
        } catch (Exception e) {
            // MCP 名失败，尝试 SRG 名
        }

        // 尝试 SRG name
        String srgName = getSrgName(fieldName);
        if (srgName != null) {
            try {
                Field field = obj.getClass().getDeclaredField(srgName);
                field.setAccessible(true);
                return (T) field.get(obj);
            } catch (Exception e) {
                // SRG 名也失败
            }
        }

        // 打印所有可用字段用于调试
        System.out.println("[Adversity] Available fields in " + obj.getClass().getSimpleName() + ":");
        for (Field f : obj.getClass().getDeclaredFields()) {
            System.out.println("  - " + f.getName() + " : " + f.getType().getSimpleName());
        }

        return null;
    }

    private static String getSrgName(String mcpName) {
        switch (mcpName) {
            case "playerInventory":
                return "field_75168_e";
            case "enchantClue":
                return "field_185001_h";
            case "worldClue":
                return "field_185002_i";
            default:
                return null;
        }
    }
}
