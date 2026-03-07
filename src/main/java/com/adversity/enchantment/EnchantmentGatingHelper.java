package com.adversity.enchantment;

import com.adversity.Adversity;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.config.AdversityConfig;
import com.adversity.sanctuary.SanctuaryManager;
import com.adversity.sanctuary.SanctuaryZone;
import com.adversity.sanctuary.StageGatingRegistry;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * 附魔門控業務邏輯統一工具類
 *
 * 所有 Mixin 注入點委託到此類，Mixin 自身不包含業務邏輯。
 * 新增附魔時不需要修改本類 — 動態查詢 ForgeRegistries。
 */
public class EnchantmentGatingHelper {

    private static final Random RANDOM = new Random();

    // ==================== 附魔判定 ====================

    /**
     * 動態判定是否為 Adversity 模組的附魔
     */
    public static boolean isAdversityEnchantment(Enchantment enchantment) {
        if (enchantment == null)
            return false;
        ResourceLocation regName = enchantment.getRegistryName();
        return regName != null && Adversity.MODID.equals(regName.getNamespace());
    }

    /**
     * 動態取得所有 Adversity 附魔
     */
    public static List<Enchantment> getAllAdversityEnchantments() {
        List<Enchantment> result = new ArrayList<>();
        for (Enchantment ench : ForgeRegistries.ENCHANTMENTS) {
            if (isAdversityEnchantment(ench)) {
                result.add(ench);
            }
        }
        return result;
    }

    /**
     * 檢查附魔是否對指定玩家開放
     *
     * 優先順序：
     * 1. 黑白名單系統（stage_gating.json 的 enchant_gating 區段）
     * 2. 逐條 setEnchantmentStage（StageGatingRegistry）
     *
     * @return true = 允許, false = 被門控
     */
    public static boolean isEnchantmentAllowed(Enchantment enchantment, EntityPlayer player) {
        if (enchantment == null || player == null)
            return true;

        // 如果是 Adversity 附魔且配置跳過門控 → 交由聖所系統管理
        if (isAdversityEnchantment(enchantment) && AdversityConfig.sanctuarySettings.skipAdversityEnchantmentGating) {
            return true;
        }

        // 1. 檢查黑白名單系統（啟用時完全接管）
        if (StageGatingRegistry.isEnchantGatingEnabled()) {
            if (StageGatingRegistry.isEnchantmentGated(enchantment)) {
                // 被 gate：需要驗證階段
                String required = StageGatingRegistry.getEnchantGatingStage();
                IAdversityCapability.IProgression cap = player.getCapability(CapabilityHandler.PROGRESSION_CAPABILITY,
                        null);
                return cap != null && cap.hasStage(required);
            }
            // 不在 gate 範圍內（白名單豁免）→ 直接放行
            return true;
        }

        // 2. 檢查逐條 StageGatingRegistry
        String required = StageGatingRegistry.getEnchantmentStageRequirement(enchantment);
        if (required == null)
            return true; // 無門控要求

        IAdversityCapability.IProgression cap = player.getCapability(CapabilityHandler.PROGRESSION_CAPABILITY, null);
        return cap != null && cap.hasStage(required);
    }

    // ==================== 從 Container 取得玩家 ====================

    /**
     * 從 Container 的 slot 列表中取得玩家
     * ContainerEnchantment 的 slot 2+ 是玩家背包 slot
     */
    public static EntityPlayer getPlayerFromContainer(Container container) {
        if (container == null)
            return null;
        try {
            List<Slot> slots = container.inventorySlots;
            // slot 2 起始是玩家背包 — 遍歷找到 InventoryPlayer
            for (int i = 2; i < slots.size(); i++) {
                Slot slot = slots.get(i);
                if (slot != null && slot.inventory instanceof InventoryPlayer) {
                    return ((InventoryPlayer) slot.inventory).player;
                }
            }
        } catch (Exception e) {
            Adversity.LOGGER.warn("[Enchant Gating] Failed to get player from container: {}", e.getMessage());
        }
        return null;
    }

    // ==================== 附魔台過濾 ====================

    /**
     * 過濾附魔台 clue 預覽 + 停用門控槽位
     *
     * 兩層過濾：
     * 1. 被 stage gate 的附魔 → 隱藏
     * 2. Adversity 附魔在非聖所附近時 → 隱藏（當 removeEnchantsFromTable=true）
     */
    public static void filterEnchantClues(int[] enchantClue, int[] worldClue, int[] enchantLevels,
            EntityPlayer player) {
        if (enchantClue == null || player == null)
            return;

        boolean nearSanctuary = isNearSanctuary(player);
        boolean removeAdvEnchants = AdversityConfig.sanctuarySettings.removeEnchantsFromTable;

        for (int i = 0; i < enchantClue.length; i++) {
            if (enchantClue[i] >= 0) {
                Enchantment ench = Enchantment.getEnchantmentByID(enchantClue[i]);
                if (ench == null)
                    continue;

                boolean shouldBlock = false;

                // 1. Stage gating
                if (!isEnchantmentAllowed(ench, player)) {
                    shouldBlock = true;
                }

                // 2. Adversity 附魔在非聖所附近時移除
                if (removeAdvEnchants && !nearSanctuary && isAdversityEnchantment(ench)) {
                    shouldBlock = true;
                }

                if (shouldBlock) {
                    enchantClue[i] = -1;
                    if (worldClue != null && i < worldClue.length) {
                        worldClue[i] = -1;
                    }
                    if (enchantLevels != null && i < enchantLevels.length) {
                        enchantLevels[i] = 0;
                    }
                }
            }
        }
    }

    /**
     * 過濾 buildEnchantmentList 的結果，移除被門控的附魔
     */
    public static List<EnchantmentData> filterEnchantmentList(List<EnchantmentData> original, EntityPlayer player) {
        if (original == null || player == null)
            return original;

        Iterator<EnchantmentData> it = original.iterator();
        while (it.hasNext()) {
            EnchantmentData data = it.next();
            if (!isEnchantmentAllowed(data.enchantment, player)) {
                it.remove();
            }
        }
        return original;
    }

    /**
     * 在聖所附近時，嘗試將 Adversity 附魔注入到空的 clue 槽位
     *
     * @param itemInTable 附魔台中的物品（用於 canApply 兼容性檢查）
     */
    public static void injectSanctuaryEnchantments(int[] enchantClue, int[] worldClue, int[] enchantLevels,
            EntityPlayer player, ItemStack itemInTable) {
        if (enchantClue == null || player == null)
            return;
        if (itemInTable == null || itemInTable.isEmpty())
            return;

        List<Enchantment> advEnchants = getAllAdversityEnchantments();
        if (advEnchants.isEmpty())
            return;

        // 檢查玩家階段 + 物品兼容性
        List<Enchantment> available = new ArrayList<>();
        for (Enchantment e : advEnchants) {
            // 關鍵：必須檢查附魔是否能應用到當前物品
            if (!e.canApplyAtEnchantingTable(itemInTable))
                continue;

            String required = StageGatingRegistry.getEnchantmentStageRequirement(e);
            if (required == null) {
                available.add(e); // 無門控要求
            } else {
                IAdversityCapability.IProgression cap = player.getCapability(CapabilityHandler.PROGRESSION_CAPABILITY,
                        null);
                if (cap != null && cap.hasStage(required)) {
                    available.add(e);
                }
            }
        }
        if (available.isEmpty())
            return;

        for (int i = 0; i < enchantClue.length; i++) {
            if (enchantLevels != null && enchantLevels[i] > 0 && RANDOM.nextFloat() < 0.25f) {
                Enchantment selected = available.get(RANDOM.nextInt(available.size()));
                int enchId = Enchantment.getEnchantmentID(selected);
                int level = 1 + RANDOM.nextInt(Math.max(1, selected.getMaxLevel()));

                enchantClue[i] = enchId;
                if (worldClue != null && i < worldClue.length) {
                    worldClue[i] = level;
                }
            }
        }
    }

    // ==================== 聖所距離檢查 ====================

    /**
     * 檢查玩家是否在聖所附近
     */
    public static boolean isNearSanctuary(EntityPlayer player) {
        if (player == null)
            return false;
        SanctuaryZone zone = SanctuaryManager.getPlayerSanctuary(player);
        if (zone == null || !zone.isActive())
            return false;

        double dist = player.getDistanceSq(zone.center);
        double maxDist = AdversityConfig.sanctuarySettings.craftingProximity
                * AdversityConfig.sanctuarySettings.craftingProximity;
        return dist <= maxDist;
    }

    // ==================== 鐵砧過濾 ====================

    /**
     * 過濾鐵砧輸出：如果附魔書上有任何被 stage gate 的附魔，直接清空輸出。
     * 不做部分 strip —— 避免渲染「假的」可拿取物品。
     *
     * @param outputSlot 鐵砧輸出 inventory
     * @param inputSlots 鐵砧輸入 inventory (slot0=物品, slot1=附魔書/材料)
     * @param player     玩家
     */
    public static void filterAnvilOutput(IInventory outputSlot, IInventory inputSlots, EntityPlayer player) {
        if (outputSlot == null || inputSlots == null || player == null)
            return;

        ItemStack output = outputSlot.getStackInSlot(0);
        if (output.isEmpty())
            return;

        ItemStack secondInput = inputSlots.getStackInSlot(1);
        if (secondInput.isEmpty())
            return;

        // 只處理附魔書的情況
        if (!(secondInput.getItem() instanceof ItemEnchantedBook))
            return;

        // 檢查附魔書上是否有任何被 gate 的附魔
        NBTTagList bookEnchants = ItemEnchantedBook.getEnchantments(secondInput);
        for (int i = 0; i < bookEnchants.tagCount(); i++) {
            NBTTagCompound tag = bookEnchants.getCompoundTagAt(i);
            int id = tag.getShort("id");
            Enchantment ench = Enchantment.getEnchantmentByID(id);
            if (ench != null && !isEnchantmentAllowed(ench, player)) {
                // 有任何被門控的附魔 → 清空輸出，走 vanilla 無輸出路線
                outputSlot.setInventorySlotContents(0, ItemStack.EMPTY);
                return;
            }
        }
    }

    /**
     * 從物品的附魔 NBT 中移除被 stage gate 的附魔
     */
    private static void stripGatedEnchantments(ItemStack stack, EntityPlayer player) {
        NBTTagList enchants = stack.getEnchantmentTagList();
        NBTTagList filtered = new NBTTagList();

        for (int i = 0; i < enchants.tagCount(); i++) {
            NBTTagCompound tag = enchants.getCompoundTagAt(i);
            int id = tag.getShort("id");
            Enchantment ench = Enchantment.getEnchantmentByID(id);
            if (ench == null || isEnchantmentAllowed(ench, player)) {
                filtered.appendTag(tag.copy());
            }
        }

        if (stack.getTagCompound() != null) {
            stack.getTagCompound().setTag("ench", filtered);
        }
    }
}
