package com.adversity.sanctuary.ritual;

import com.adversity.Adversity;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * 儀式效果註冊表
 *
 * 管理 IRitualEffect 實例的註冊與查找。
 * 內建效果在 init 時註冊，CRT/JSON 可以覆蓋。
 */
public class RitualEffectRegistry {

    private static final Map<ResourceLocation, IRitualEffect> EFFECTS = new HashMap<>();

    /**
     * 註冊效果
     */
    public static void register(String id, IRitualEffect effect) {
        ResourceLocation loc = id.contains(":")
                ? new ResourceLocation(id)
                : new ResourceLocation(Adversity.MODID, id);
        EFFECTS.put(loc, effect);
        Adversity.LOGGER.debug("[Adversity] Registered ritual effect: {}", loc);
    }

    /**
     * 按 ID 查找效果
     */
    public static IRitualEffect get(ResourceLocation id) {
        return EFFECTS.get(id);
    }

    /**
     * 按 string ID 查找
     */
    public static IRitualEffect get(String id) {
        ResourceLocation loc = id.contains(":")
                ? new ResourceLocation(id)
                : new ResourceLocation(Adversity.MODID, id);
        return EFFECTS.get(loc);
    }

    /**
     * 檢查效果是否已註冊
     */
    public static boolean has(ResourceLocation id) {
        return EFFECTS.containsKey(id);
    }

    public static int getCount() {
        return EFFECTS.size();
    }

    public static void clear() {
        EFFECTS.clear();
    }
}
