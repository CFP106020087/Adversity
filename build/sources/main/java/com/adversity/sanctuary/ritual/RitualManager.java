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

        // ==================== 命令执行仪式 ====================

        // 10. 安宁祈祷 (Prayer of Peace): 降低全局难度1点
        // 输入: 平静之尘 -> 无输出（执行命令）
        registerCommandRite("prayer_of_peace",
                        new ItemStack(ItemRegistry.CALM_DUST, 4),
                        200, null, "advdiff sub 1");

        // 11. 和平献祭 (Peace Offering): 降低全局难度2点
        // 输入: 熵能结晶 -> 无输出（执行命令）
        registerCommandRite("peace_offering",
                        new ItemStack(ItemRegistry.ENTROPY, 1, 1), // CRYSTAL
                        500, "awakened", "advdiff sub 2");

        // 12. 宁静仪式 (Tranquility Ritual): 降低全局难度5点
        // 输入: 熵能核心 -> 无输出（执行命令）
        registerCommandRite("tranquility_ritual",
                        new ItemStack(ItemRegistry.ENTROPY, 1, 2), // CORE
                        1500, "scholar", "advdiff sub 5");

        // 13. 净界仪式 (Realm Cleansing): 重置全局难度设置
        // 输入: 下界之星 -> 无输出（执行命令）
        registerCommandRite("realm_cleansing",
                        new ItemStack(Items.NETHER_STAR),
                        5000, "warden", "advdiff reset");

        Adversity.LOGGER.info("Ritual System initialized with {} rituals", RITES.size());
    }

    public static void registerRite(String name, ItemStack input, ItemStack output, int cost, String reqStage,
            String rewardStage) {
            registerRite(name, input, output, cost, reqStage, rewardStage, null);
    }

    public static void registerRite(String name, ItemStack input, ItemStack output, int cost, String reqStage,
                    String rewardStage, String command) {
        ResourceLocation id = new ResourceLocation(Adversity.MODID, name);
        RITES.put(id, new Rite(id, input, output, cost, reqStage, rewardStage, command));
        Adversity.LOGGER.debug("Registered ritual: {} (cost={}, req={}, reward={}, cmd={})",
                        name, cost, reqStage, rewardStage, command != null ? "yes" : "no");
}

/**
 * 注册纯命令仪式（无物品输出，仅执行命令）
 */
public static void registerCommandRite(String name, ItemStack input, int cost, String reqStage, String command) {
        registerRite(name, input, ItemStack.EMPTY, cost, reqStage, null, command);
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

    /**
     * 移除指定仪式 (用于CRT集成)
     * 
     * @param ritualId 完整仪式ID，如 "adversity:awakening"
     * @return 是否成功移除
     */
    public static boolean removeRite(String ritualId) {
            ResourceLocation id;
            if (ritualId.contains(":")) {
                    id = new ResourceLocation(ritualId);
            } else {
                    id = new ResourceLocation(Adversity.MODID, ritualId);
            }
            Rite removed = RITES.remove(id);
            if (removed != null) {
                    Adversity.LOGGER.info("Removed ritual: {}", ritualId);
                    return true;
            }
            return false;
    }

    /**
     * 移除所有仪式 (用于CRT集成)
     * 
     * @return 移除的仪式数量
     */
    public static int removeAllRites() {
            int count = RITES.size();
            RITES.clear();
            Adversity.LOGGER.info("Removed all {} rituals", count);
            return count;
    }
}
