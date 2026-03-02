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
            default: return null;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        // 确保初始化
        ensureInitialized();
        
        // 检查配置是否启用
        if (!AdversityConfig.sanctuarySettings.enableCraftingRestrictions) {
            return;
        }

        ItemStack result = event.crafting;
        if (result.isEmpty()) return;
        
        Item item = result.getItem();
        
        // 检查是否是需要限制的物品
        if (!SANCTUARY_ITEMS.contains(item)) {
            return;
        }

        EntityPlayer player = event.player;
        if (player == null || player.world.isRemote) return;
        
        // 检查玩家是否在圣所附近
        SanctuaryZone zone = SanctuaryManager.getPlayerSanctuary(player);
        
        if (zone == null || !zone.isActive()) {
            // 不在圣所范围内
            cancelCrafting(event, player, "adversity.crafting.need_sanctuary");
            return;
        }
        
        // 检查距离祭坛的距离
        double dist = Math.sqrt(player.getDistanceSq(zone.center));
        int maxDist = AdversityConfig.sanctuarySettings.craftingProximity;
        
        if (dist > maxDist) {
            // 距离太远
            cancelCrafting(event, player, "adversity.crafting.too_far", maxDist);
            return;
        }
        
        // 检查 Tier 要求
        Integer requiredTier = TIER_REQUIREMENTS.get(item);
        if (requiredTier != null && requiredTier > 0) {
            int zoneTier = zone.tier;
            if (zoneTier < requiredTier) {
                // Tier 不足
                cancelCrafting(event, player, "adversity.crafting.tier_too_low", requiredTier, zoneTier);
                return;
            }
        }
        
        // 合成成功！
    }
    
    /**
     * 取消合成并通知玩家
     */
    private static void cancelCrafting(PlayerEvent.ItemCraftedEvent event, EntityPlayer player, String msgKey, Object... args) {
        // 清空合成结果
        event.crafting.setCount(0);
        
        // 发送消息
        player.sendStatusMessage(new TextComponentTranslation(msgKey, args), true);
    }
}
