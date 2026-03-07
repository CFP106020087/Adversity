package com.adversity.handler;

import com.adversity.Adversity;
import com.adversity.config.AdversityConfig;
import com.adversity.item.ItemRegistry;
import com.adversity.sanctuary.SanctuaryManager;
import com.adversity.sanctuary.SanctuaryZone;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 圣所合成限制处理器
 * 
 * 限制特殊饰品只能在圣所祭坛附近合成
 * 支持基于 Tier 的合成限制
 */
@Mod.EventBusSubscriber(modid = Adversity.MODID)
public class SanctuaryCraftingHandler {

    // 需要在圣所附近合成的物品
    private static final Set<Item> SANCTUARY_ITEMS = new HashSet<>();
    
    // 物品 -> 所需 Tier 的映射
    private static final Map<Item, Integer> TIER_REQUIREMENTS = new HashMap<>();
    
    // 缓存是否已初始化
    private static boolean initialized = false;

    /**
     * 初始化物品列表 (延迟到 FML 完成后)
     */
    private static void ensureInitialized() {
        if (initialized) return;
        initialized = true;
        
        // 添加所有饰品到限制列表
        addIfNotNull(ItemRegistry.SOUL_CHAIN);
        addIfNotNull(ItemRegistry.SPATIAL_ANCHOR);
        addIfNotNull(ItemRegistry.GUARDIAN_HEART);
        addIfNotNull(ItemRegistry.FLAME_WARD);
        addIfNotNull(ItemRegistry.FROST_WARD);
        addIfNotNull(ItemRegistry.CORROSION_BANE);
        addIfNotNull(ItemRegistry.CLARITY_LENS);
        addIfNotNull(ItemRegistry.COURAGE_CHARM);
        addIfNotNull(ItemRegistry.ENCHANT_GUARDIAN);
        addIfNotNull(ItemRegistry.ANCHOR_STONE);
        addIfNotNull(ItemRegistry.SANCTUARY_COMPASS);
        // Phase 2: 补全全部饰品
        addIfNotNull(ItemRegistry.VOID_HEART);
        addIfNotNull(ItemRegistry.BLOODRAGE_EMBLEM);
        addIfNotNull(ItemRegistry.TEMPORAL_WATCH);
        addIfNotNull(ItemRegistry.PHANTOM_CLOAK);
        addIfNotNull(ItemRegistry.THUNDER_RING);
        addIfNotNull(ItemRegistry.AEGIS_MEDAL);
        addIfNotNull(ItemRegistry.PURIFY_BADGE);
        addIfNotNull(ItemRegistry.ARMOR_PIERCE);
        addIfNotNull(ItemRegistry.ANCHOR_HEART);
        addIfNotNull(ItemRegistry.BARRIER_WARD);
        addIfNotNull(ItemRegistry.WITHER_MARK);
        addIfNotNull(ItemRegistry.TRUE_CRYSTAL);
        addIfNotNull(ItemRegistry.GUARDIAN_SOUL);
        addIfNotNull(ItemRegistry.BALANCE_CHARM);
        addIfNotNull(ItemRegistry.IMMUNITY_BADGE);
        addIfNotNull(ItemRegistry.MEMORY_CRYSTAL);
        addIfNotNull(ItemRegistry.SOUL_SEAL_DUST);
        addIfNotNull(ItemRegistry.LIGHT_AMULET);
        
        // 解析 Tier 配置
        parseTierRequirements();
        
        Adversity.LOGGER.info("[Adversity] SanctuaryCraftingHandler initialized with {} items", SANCTUARY_ITEMS.size());
    }
    
    private static void addIfNotNull(Item item) {
        if (item != null) {
            SANCTUARY_ITEMS.add(item);
        }
    }
    
    /**
     * 解析配置中的 Tier 要求
     */
    private static void parseTierRequirements() {
        for (String entry : AdversityConfig.sanctuarySettings.baublesTierRequirements) {
            if (entry == null || !entry.contains(":")) continue;
            
            String[] parts = entry.split(":", 2);
            if (parts.length != 2) continue;
            
            String itemName = parts[0].trim();
            int requiredTier;
            try {
                requiredTier = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                continue;
            }
            
            // 根据名称匹配物品
            Item item = findItemByName(itemName);
            if (item != null) {
                TIER_REQUIREMENTS.put(item, requiredTier);
            }
        }
    }
    
    /**
     * 根据名称查找物品
     */
    private static Item findItemByName(String name) {
        // 简单的名称匹配
        switch (name.toLowerCase()) {
            case "soul_chain": return ItemRegistry.SOUL_CHAIN;
            case "spatial_anchor": return ItemRegistry.SPATIAL_ANCHOR;
            case "guardian_heart": return ItemRegistry.GUARDIAN_HEART;
            case "flame_ward": return ItemRegistry.FLAME_WARD;
            case "frost_ward": return ItemRegistry.FROST_WARD;
            case "corrosion_bane": return ItemRegistry.CORROSION_BANE;
            case "clarity_lens": return ItemRegistry.CLARITY_LENS;
            case "courage_charm": return ItemRegistry.COURAGE_CHARM;
            case "enchant_guardian": return ItemRegistry.ENCHANT_GUARDIAN;
            case "anchor_stone": return ItemRegistry.ANCHOR_STONE;
            case "sanctuary_compass": return ItemRegistry.SANCTUARY_COMPASS;
            // Phase 2: 补全全部饰品
            case "void_heart":
                return ItemRegistry.VOID_HEART;
            case "bloodrage_emblem":
                return ItemRegistry.BLOODRAGE_EMBLEM;
            case "temporal_watch":
                return ItemRegistry.TEMPORAL_WATCH;
            case "phantom_cloak":
                return ItemRegistry.PHANTOM_CLOAK;
            case "thunder_ring":
                return ItemRegistry.THUNDER_RING;
            case "aegis_medal":
                return ItemRegistry.AEGIS_MEDAL;
            case "purify_badge":
                return ItemRegistry.PURIFY_BADGE;
            case "armor_pierce":
                return ItemRegistry.ARMOR_PIERCE;
            case "anchor_heart":
                return ItemRegistry.ANCHOR_HEART;
            case "barrier_ward":
                return ItemRegistry.BARRIER_WARD;
            case "wither_mark":
                return ItemRegistry.WITHER_MARK;
            case "true_crystal":
                return ItemRegistry.TRUE_CRYSTAL;
            case "guardian_soul":
                return ItemRegistry.GUARDIAN_SOUL;
            case "balance_charm":
                return ItemRegistry.BALANCE_CHARM;
            case "immunity_badge":
                return ItemRegistry.IMMUNITY_BADGE;
            case "memory_crystal":
                return ItemRegistry.MEMORY_CRYSTAL;
            case "soul_seal_dust":
                return ItemRegistry.SOUL_SEAL_DUST;
            case "light_amulet":
                return ItemRegistry.LIGHT_AMULET;
            default: return null;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ensureInitialized();

        EntityPlayer player = event.player;
        if (player == null || player.world.isRemote)
            return;

        ItemStack result = event.crafting;
        if (result.isEmpty()) return;

        Item item = result.getItem();

        // Stage 門控已由 MixinCraftingManager 在 findMatchingRecipe 層攔截
        // （合成結果直接為空，不需要事後退還材料）

        // ===== 2. 聖所限制檢查（僅適用飾品 SANCTUARY_ITEMS） =====
        if (!AdversityConfig.sanctuarySettings.enableCraftingRestrictions) {
            return;
        }

        if (!SANCTUARY_ITEMS.contains(item)) {
            return;
        }

        // 検查是否在聖所附近
        SanctuaryZone zone = SanctuaryManager.getPlayerSanctuary(player);

        if (zone == null || !zone.isActive()) {
            cancelCrafting(event, player, "adversity.crafting.need_sanctuary");
            return;
        }

        double dist = Math.sqrt(player.getDistanceSq(zone.center));
        int maxDist = AdversityConfig.sanctuarySettings.craftingProximity;

        if (dist > maxDist) {
            cancelCrafting(event, player, "adversity.crafting.too_far", maxDist);
            return;
        }

        // Tier 要求
        Integer requiredTier = TIER_REQUIREMENTS.get(item);
        if (requiredTier != null && requiredTier > 0) {
            int zoneTier = zone.tier;
            if (zoneTier < requiredTier) {
                cancelCrafting(event, player, "adversity.crafting.tier_too_low", requiredTier, zoneTier);
                return;
            }
        }
    }
    
    /**
     * 取消合成并通知玩家
     * 
     * 由于 PlayerEvent.ItemCraftedEvent 不可取消（材料已被消耗），
     * 需要将合成矩阵中的材料归还给玩家。
     */
    private static void cancelCrafting(PlayerEvent.ItemCraftedEvent event, EntityPlayer player, String msgKey, Object... args) {
        // 清空合成结果
        event.crafting.setCount(0);
        
        // 归还被消耗的材料
        // ItemCraftedEvent 触发时，craftMatrix 每个原本有材料的槽位已被 decrStackSize(1)
        // 对于仍非空的槽位：可以确定消耗了1个同类物品，补回1个
        // 对于已变空的槽位：无法确定原物品类型，无法补回（极端边缘情况）
        if (event.craftMatrix != null) {
            for (int i = 0; i < event.craftMatrix.getSizeInventory(); i++) {
                ItemStack remaining = event.craftMatrix.getStackInSlot(i);
                if (!remaining.isEmpty()) {
                    // 这个槽位原来有 N+1 个物品，消耗1个后剩余 N 个
                    // 补回被消耗的1个
                    ItemStack refund = remaining.copy();
                    refund.setCount(1);
                    if (!player.inventory.addItemStackToInventory(refund)) {
                        player.dropItem(refund, false);
                    }
                }
            }
        }

        // 发送消息
        player.sendStatusMessage(new TextComponentTranslation(msgKey, args), true);
    }
}
