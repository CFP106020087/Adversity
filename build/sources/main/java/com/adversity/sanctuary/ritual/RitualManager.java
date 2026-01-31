package com.adversity.sanctuary.ritual;

import com.adversity.Adversity;
import com.adversity.item.ItemRegistry;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * 仪式管理器
 * 管理所有圣所仪式的注册和查找
 */
public class RitualManager {

    private static final Map<ResourceLocation, Rite> RITES = Maps.newHashMap();

    public static void init() {
        Adversity.LOGGER.info("Initializing Ritual System...");

        // ==================== 阶段解锁仪式 ====================

        // 1. 初始觉醒 (Awakening): 解锁 "awakened" 阶段
        // 输入: 金苹果 -> 输出: 圣所指南针
        // 消耗: 500 熵能, 无前置要求
        registerRite("awakening",
                new ItemStack(Items.GOLDEN_APPLE),
                new ItemStack(ItemRegistry.SANCTUARY_COMPASS != null ? ItemRegistry.SANCTUARY_COMPASS : Items.COMPASS),
                500, null, "awakened");

        // 2. 学者晋升 (Scholar Ascension): 解锁 "scholar" 阶段
        // 输入: 钻石块 -> 输出: 附魔之瓶
        // 消耗: 2000 熵能, 需要 "awakened"
        registerRite("scholar_ascension",
                new ItemStack(Blocks.DIAMOND_BLOCK),
                new ItemStack(Items.EXPERIENCE_BOTTLE, 16),
                2000, "awakened", "scholar");

        // 3. 守望者晋升 (Warden Ascension): 解锁 "warden" 阶段
        // 输入: 下界之星碎片(用海洋之心代替) -> 输出: 图腾
        // 消耗: 5000 熵能, 需要 "scholar"
        registerRite("warden_ascension",
                new ItemStack(Items.NETHER_STAR),
                new ItemStack(Items.TOTEM_OF_UNDYING),
                5000, "scholar", "warden");

        // 4. 冠军晋升 (Champion Ascension): 解锁 "champion" 阶段
        // 输入: 龙蛋(用龙息代替) -> 输出: 信标
        // 消耗: 10000 熵能, 需要 "warden"
        registerRite("champion_ascension",
                new ItemStack(Items.DRAGON_BREATH, 8),
                new ItemStack(Blocks.BEACON),
                10000, "warden", "champion");

        // ==================== 实用仪式 ====================

        // 5. 净化 (Cleanse): 腐肉 -> 皮革
        registerRite("cleanse_flesh",
                new ItemStack(Items.ROTTEN_FLESH, 16),
                new ItemStack(Items.LEATHER, 8),
                100, "awakened", null);

        // 6. 提炼 (Refine): 铁锭 -> 金锭
        registerRite("refine_iron",
                new ItemStack(Items.IRON_INGOT, 8),
                new ItemStack(Items.GOLD_INGOT, 4),
                300, "scholar", null);

        // 7. 祝福 (Bless): 普通苹果 -> 金苹果
        registerRite("bless_apple",
                new ItemStack(Items.APPLE, 4),
                new ItemStack(Items.GOLDEN_APPLE),
                500, "scholar", null);

        // 8. 净化诅咒 (Purge Curse): 消除诅咒
        // 输入: 牛奶桶 -> 输出: 空桶 (实际效果通过代码触发)
        registerRite("purge_curse",
                new ItemStack(Items.MILK_BUCKET),
                new ItemStack(Items.BUCKET),
                1000, "warden", null);

        // 9. 复活之水 (Resurrection): 空瓶 -> 不死图腾
        registerRite("resurrection",
                new ItemStack(Items.GLASS_BOTTLE, 8),
                new ItemStack(Items.TOTEM_OF_UNDYING),
                3000, "champion", null);

        Adversity.LOGGER.info("Ritual System initialized with {} rituals", RITES.size());
    }

    public static void registerRite(String name, ItemStack input, ItemStack output, int cost, String reqStage,
            String rewardStage) {
        ResourceLocation id = new ResourceLocation(Adversity.MODID, name);
        RITES.put(id, new Rite(id, input, output, cost, reqStage, rewardStage));
        Adversity.LOGGER.debug("Registered ritual: {} (cost={}, req={}, reward={})",
                name, cost, reqStage, rewardStage);
    }

    public static void registerRite(Rite rite) {
        RITES.put(rite.getId(), rite);
    }

    public static Rite getRite(ItemStack input) {
        for (Rite rite : RITES.values()) {
            if (rite.matches(input)) {
                return rite;
            }
        }
        return null;
    }

    public static List<Rite> getAllRites() {
        return Lists.newArrayList(RITES.values());
    }

    public static Rite getRiteById(ResourceLocation id) {
        return RITES.get(id);
    }

    public static int getRiteCount() {
        return RITES.size();
    }
}
