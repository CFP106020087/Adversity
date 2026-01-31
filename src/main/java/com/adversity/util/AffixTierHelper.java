package com.adversity.util;

import com.adversity.config.AdversityConfig;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * 词条等级限制帮助类
 * 解析配置并提供查询API
 */
public class AffixTierHelper {

    // 缓存: affix_id -> int[]{minTier, maxTier}
    private static Map<String, int[]> tierLimits = null;

    public static void reload() {
        tierLimits = new HashMap<>();
        for (String entry : AdversityConfig.affixSettings.affixTierLimits) {
            try {
                // Format: affix_id:min-max (e.g., adversity:splitting:1-5)
                int lastColon = entry.lastIndexOf(':');
                if (lastColon == -1) continue;
                
                String affixId = entry.substring(0, lastColon);
                String range = entry.substring(lastColon + 1);
                
                String[] parts = range.split("-");
                if (parts.length != 2) continue;
                
                int min = Integer.parseInt(parts[0].trim());
                int max = Integer.parseInt(parts[1].trim());
                
                tierLimits.put(affixId, new int[]{min, max});
            } catch (Exception e) {
                // 忽略格式错误
            }
        }
    }

    /**
     * 检查词条是否可以应用于指定等级
     * @param affixId 词条ID (e.g., "adversity:splitting")
     * @param tier 精英等级 (1-10)
     * @return true if allowed, false if blocked by tier limit
     */
    public static boolean isAllowedForTier(String affixId, int tier) {
        if (tierLimits == null) {
            reload();
        }
        
        int[] limits = tierLimits.get(affixId);
        if (limits == null) {
            return true; // 无限制
        }
        
        return tier >= limits[0] && tier <= limits[1];
    }
    
    public static boolean isAllowedForTier(ResourceLocation affixId, int tier) {
        return isAllowedForTier(affixId.toString(), tier);
    }
}
