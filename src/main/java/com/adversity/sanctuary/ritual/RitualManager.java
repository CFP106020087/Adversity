package com.adversity.sanctuary.ritual;

import com.adversity.Adversity;
import com.adversity.item.ItemRegistry;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.*;

/**
 * 仪式管理器
 * 管理所有圣所仪式的注册、查找和冷却
 */
public class RitualManager {

        private static final Map<ResourceLocation, Rite> RITES = Maps.newLinkedHashMap();
        private static final Map<String, Long> COOLDOWNS = new HashMap<>();

    public static void init() {
        Adversity.LOGGER.info("Initializing Ritual System...");

        // ==================== 阶段解锁仪式 ====================

        registerRite("awakening",
                new ItemStack(Items.GOLDEN_APPLE),
                new ItemStack(ItemRegistry.SANCTUARY_COMPASS != null ? ItemRegistry.SANCTUARY_COMPASS : Items.COMPASS),
                500, null, "awakened");

        registerRite("scholar_ascension",
                new ItemStack(Blocks.DIAMOND_BLOCK),
                new ItemStack(Items.EXPERIENCE_BOTTLE, 16),
                2000, "awakened", "scholar");

        registerRite("warden_ascension",
                new ItemStack(Items.NETHER_STAR),
                new ItemStack(Items.TOTEM_OF_UNDYING),
                5000, "scholar", "warden");

        registerRite("champion_ascension",
                new ItemStack(Items.DRAGON_BREATH, 8),
                new ItemStack(Blocks.BEACON),
                10000, "warden", "champion");

        // ==================== 实用仪式 ====================

        registerRite("cleanse_flesh",
                new ItemStack(Items.ROTTEN_FLESH, 16),
                new ItemStack(Items.LEATHER, 8),
                100, "awakened", null);

        registerRite("refine_iron",
                new ItemStack(Items.IRON_INGOT, 8),
                new ItemStack(Items.GOLD_INGOT, 4),
                300, "scholar", null);

        registerRite("bless_apple",
                new ItemStack(Items.APPLE, 4),
                new ItemStack(Items.GOLDEN_APPLE),
                500, "scholar", null);

        registerRite("purge_curse",
                new ItemStack(Items.MILK_BUCKET),
                new ItemStack(Items.BUCKET),
                1000, "warden", null);

        registerRite("resurrection",
                new ItemStack(Items.GLASS_BOTTLE, 8),
                new ItemStack(Items.TOTEM_OF_UNDYING),
                3000, "champion", null);

        // ==================== 难度调整 (命令仪式 + per-player + 冷却) ====================

        registerCommandRite("prayer_of_peace",
                        new ItemStack(ItemRegistry.CALM_DUST, 4),
                        200, null, "advdiff sub {player} 1", 6000);

        registerCommandRite("peace_offering",
                        new ItemStack(ItemRegistry.ENTROPY, 1, 1),
                        500, "awakened", "advdiff sub {player} 2", 12000);

        registerCommandRite("tranquility_ritual",
                        new ItemStack(ItemRegistry.ENTROPY, 1, 2),
                        1500, "scholar", "advdiff sub {player} 5", 24000);

        registerCommandRite("realm_cleansing",
                        new ItemStack(Items.NETHER_STAR),
                        5000, "warden", "advdiff reset {player}", 72000);

        Adversity.LOGGER.info("Ritual System initialized with {} rituals", RITES.size());
    }

    // ==================== 注册 API ====================

    public static void registerRite(String name, ItemStack input, ItemStack output, int cost,
                    String reqStage, String rewardStage) {
            registerRite(name, input, output, cost, reqStage, rewardStage, null);
    }

    public static void registerRite(String name, ItemStack input, ItemStack output, int cost,
                    String reqStage, String rewardStage, String command) {
        ResourceLocation id = new ResourceLocation(Adversity.MODID, name);
        RITES.put(id, new Rite(id, input, output, cost, reqStage, rewardStage, command));
        Adversity.LOGGER.debug("Registered ritual: {} (cost={}, req={}, reward={}, cmd={})",
                        name, cost, reqStage, rewardStage, command != null ? "yes" : "no");
}

    public static void registerCommandRite(String name, ItemStack input, int cost,
                    String reqStage, String command) {
            registerCommandRite(name, input, cost, reqStage, command, 0);
    }

    public static void registerCommandRite(String name, ItemStack input, int cost,
                    String reqStage, String command, int cooldownTicks) {
            ResourceLocation id = new ResourceLocation(Adversity.MODID, name);
            RITES.put(id, new Rite(id,
                            Collections.singletonList(input.copy()),
                            ItemStack.EMPTY, cost, reqStage, null, command,
                            RitualCategory.DIFFICULTY, cooldownTicks));
            Adversity.LOGGER.debug("Registered command ritual: {} (cost={}, cd={}t)", name, cost, cooldownTicks);
    }

    public static void registerMultiInputRite(String name, List<ItemStack> inputs, ItemStack output,
                    int cost, String reqStage, String rewardStage,
                    String category) {
            ResourceLocation id = new ResourceLocation(Adversity.MODID, name);
            RITES.put(id, new Rite(id, inputs, output, cost, reqStage, rewardStage, null,
                            RitualCategory.fromString(category), 0));
            Adversity.LOGGER.debug("Registered multi-input ritual: {} ({} inputs)", name, inputs.size());
    }

    public static void registerRite(Rite rite) {
        RITES.put(rite.getId(), rite);
    }

    // ==================== 查找 ====================

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

    public static List<Rite> getRitesByCategory(RitualCategory category) {
            List<Rite> result = new ArrayList<>();
            for (Rite rite : RITES.values()) {
                    if (rite.getCategory() == category) {
                            result.add(rite);
                    }
            }
            return result;
    }

    public static Rite getRiteById(ResourceLocation id) {
        return RITES.get(id);
    }

    public static int getRiteCount() {
        return RITES.size();
    }

    // ==================== 冷却系统 ====================

    public static long getRemainingCooldown(String playerName, Rite rite, long worldTick) {
            if (!rite.hasCooldown())
                    return 0;
            String key = playerName + ":" + rite.getId().toString();
            Long expiry = COOLDOWNS.get(key);
            if (expiry == null)
                    return 0;
            return Math.max(0, expiry - worldTick);
    }

    public static void setCooldown(String playerName, Rite rite, long worldTick) {
            if (!rite.hasCooldown())
                    return;
            String key = playerName + ":" + rite.getId().toString();
            COOLDOWNS.put(key, worldTick + rite.getCooldownTicks());
    }

    public static void cleanupCooldowns(long worldTick) {
            COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() <= worldTick);
    }

    // ==================== CRT 移除 API ====================

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

    public static int removeAllRites() {
            int count = RITES.size();
            RITES.clear();
            Adversity.LOGGER.info("Removed all {} rituals", count);
            return count;
    }
}
