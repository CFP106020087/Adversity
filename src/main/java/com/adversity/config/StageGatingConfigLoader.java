package com.adversity.config;

import com.adversity.Adversity;
import com.adversity.sanctuary.StageGatingRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraftforge.fml.common.Loader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Stage Gating JSON 配置載入器
 *
 * 從 config/adversity/stage_gating.json 讀取門控配置。
 * CRT ZenScript 在之後載入，同 key 自然覆蓋（Map.put 語義）。
 */
public class StageGatingConfigLoader {

    private static final String FILE_NAME = "stage_gating.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * 在 FMLInitializationEvent 時調用。
     * 如果文件不存在，生成帶教學的默認 JSON。
     * 解析時跳過 _ 開頭的 key（_comment, _example, _doc_*）。
     */
    public static void loadIfNeeded() {
        File configDir = new File(Loader.instance().getConfigDir(), "adversity");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File file = new File(configDir, FILE_NAME);

        if (!file.exists()) {
            generateDefault(file);
            Adversity.LOGGER.info("[Adversity] Generated default {}", FILE_NAME);
            return; // 空配置，不需載入
        }

        try {
            JsonObject root = parseJson(file);
            if (root == null)
                return;

            int itemCount = loadStringSection(root, "items", (id, stage) -> StageGatingRegistry
                    .setItemStage(new net.minecraft.util.ResourceLocation(id), stage));

            int enchantCount = loadStringSection(root, "enchantments", (id, stage) -> StageGatingRegistry
                    .setEnchantmentStage(new net.minecraft.util.ResourceLocation(id), stage));

            int dimCount = loadIntKeySection(root, "dimensions",
                    StageGatingRegistry::setDimensionStage);

            int entityCount = loadStringSection(root, "entities",
                    StageGatingRegistry::setEntityStage);

            int biomeCount = loadStringSection(root, "biomes",
                    StageGatingRegistry::setBiomeStage);

            // 附魔黑白名單
            loadEnchantGatingSection(root);

            Adversity.LOGGER.info(
                    "[Adversity] Stage gating loaded from JSON: {} items, {} enchantments, {} dimensions, {} entities, {} biomes",
                    itemCount, enchantCount, dimCount, entityCount, biomeCount);

        } catch (Exception e) {
            Adversity.LOGGER.error("[Adversity] Failed to load {}", FILE_NAME, e);
        }
    }

    /**
     * 載入 string key → string value 的 section，跳過 _ 開頭的 key。
     */
    private static int loadStringSection(JsonObject root, String section, StringSetter setter) {
        if (!root.has(section) || !root.get(section).isJsonObject())
            return 0;
        int count = 0;
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject(section).entrySet()) {
            if (entry.getKey().startsWith("_"))
                continue;
            if (!entry.getValue().isJsonPrimitive())
                continue;
            setter.set(entry.getKey(), entry.getValue().getAsString());
            count++;
        }
        return count;
    }

    /**
     * 載入 int key (dimension ID) → string value 的 section。
     */
    private static int loadIntKeySection(JsonObject root, String section, IntSetter setter) {
        if (!root.has(section) || !root.get(section).isJsonObject())
            return 0;
        int count = 0;
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject(section).entrySet()) {
            if (entry.getKey().startsWith("_"))
                continue;
            if (!entry.getValue().isJsonPrimitive())
                continue;
            try {
                int key = Integer.parseInt(entry.getKey());
                setter.set(key, entry.getValue().getAsString());
                count++;
            } catch (NumberFormatException e) {
                Adversity.LOGGER.warn("[Adversity] Invalid dimension ID in {}: {}", FILE_NAME, entry.getKey());
            }
        }
        return count;
    }

    private static JsonObject parseJson(File file) {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, JsonObject.class);
        } catch (Exception e) {
            Adversity.LOGGER.error("[Adversity] Failed to parse {}", file.getName(), e);
            return null;
        }
    }

    /**
     * 載入 enchant_gating 黑白名單區段
     */
    private static void loadEnchantGatingSection(JsonObject root) {
        if (!root.has("enchant_gating") || !root.get("enchant_gating").isJsonObject())
            return;
        JsonObject section = root.getAsJsonObject("enchant_gating");

        boolean enabled = section.has("enabled") && section.get("enabled").getAsBoolean();
        if (!enabled)
            return;

        String stage = section.has("required_stage") ? section.get("required_stage").getAsString() : "scholar";
        boolean useBlacklist = !section.has("use_blacklist") || section.get("use_blacklist").getAsBoolean();

        java.util.Set<net.minecraft.util.ResourceLocation> list = new java.util.HashSet<>();
        if (section.has("enchantment_list") && section.get("enchantment_list").isJsonArray()) {
            for (JsonElement elem : section.getAsJsonArray("enchantment_list")) {
                if (elem.isJsonPrimitive()) {
                    list.add(new net.minecraft.util.ResourceLocation(elem.getAsString()));
                }
            }
        }

        StageGatingRegistry.setEnchantmentGating(enabled, stage, useBlacklist, list);
        Adversity.LOGGER.info("[Adversity] Enchant gating: stage={}, blacklist={}, entries={}",
                stage, useBlacklist, list.size());
    }

    private static void generateDefault(File file) {
        String defaultContent = "{\n"
                + "  \"_comment\": \"=== Adversity Stage Gating Configuration ===\",\n"
                + "  \"_doc_stages\": \"Available stages: newcomer, awakened, explorer, scholar, warden, champion, master\",\n"
                + "  \"_doc_priority\": \"CraftTweaker scripts override these settings. CRT takes priority.\",\n"
                + "  \"_doc_format\": \"Map resource IDs to required stage names. Keys starting with _ are comments and ignored.\",\n"
                + "\n"
                + "  \"items\": {\n"
                + "    \"_comment\": \"Item gating: players below required stage cannot craft/use these items.\",\n"
                + "    \"_example_1\": \"\\\"minecraft:ender_pearl\\\": \\\"explorer\\\"\",\n"
                + "    \"_example_2\": \"\\\"minecraft:elytra\\\": \\\"master\\\"\"\n"
                + "  },\n"
                + "\n"
                + "  \"enchantments\": {\n"
                + "    \"_comment\": \"Enchantment gating: hidden from enchanting table/anvil until required stage.\",\n"
                + "    \"_example\": \"\\\"minecraft:mending\\\": \\\"scholar\\\"\"\n"
                + "  },\n"
                + "\n"
                + "  \"dimensions\": {\n"
                + "    \"_comment\": \"Dimension gating: portal travel blocked until required stage. Keys are dimension IDs.\",\n"
                + "    \"_example_1\": \"\\\"-1\\\": \\\"explorer\\\" (Nether)\",\n"
                + "    \"_example_2\": \"\\\"1\\\": \\\"scholar\\\" (The End)\",\n"
                + "    \"_doc_ids\": \"0=Overworld, -1=Nether, 1=End. Modded dimensions have their own IDs.\"\n"
                + "  },\n"
                + "\n"
                + "  \"entities\": {\n"
                + "    \"_comment\": \"Entity gating: entities do not spawn until required stage.\",\n"
                + "    \"_example\": \"\\\"minecraft:wither\\\": \\\"warden\\\"\"\n"
                + "  },\n"
                + "\n"
                + "  \"biomes\": {\n"
                + "    \"_comment\": \"Biome gating: players warned/blocked from entering biomes until required stage.\",\n"
                + "    \"_example\": \"\\\"minecraft:hell\\\": \\\"explorer\\\"\"\n"
                + "  },\n"
                + "\n"
                + "  \"enchant_gating\": {\n"
                + "    \"_comment\": \"Bulk enchantment gating. use_blacklist=true: listed enchants gated. false: listed exempt, all others gated.\",\n"
                + "    \"enabled\": false,\n"
                + "    \"required_stage\": \"scholar\",\n"
                + "    \"use_blacklist\": true,\n"
                + "    \"enchantment_list\": []\n"
                + "  }\n"
                + "}\n";

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(defaultContent);
        } catch (IOException e) {
            Adversity.LOGGER.error("[Adversity] Failed to generate default {}", FILE_NAME, e);
        }
    }

    @FunctionalInterface
    private interface StringSetter {
        void set(String key, String value);
    }

    @FunctionalInterface
    private interface IntSetter {
        void set(int key, String value);
    }
}
