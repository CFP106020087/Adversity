package com.adversity.config;

import com.adversity.Adversity;
import com.adversity.sanctuary.ritual.Rite;
import com.adversity.sanctuary.ritual.RitualCategory;
import com.adversity.sanctuary.ritual.RitualManager;
import com.google.gson.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 儀式 JSON 配置載入器
 *
 * 從 config/adversity/rituals.json 讀取儀式配置。
 * CRT ZenScript 在之後載入，同 ID 自然覆蓋（Map.put 語義）。
 */
public class RitualConfigLoader {

    private static final String FILE_NAME = "rituals.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * 在 FMLInitializationEvent 時調用。
     * 如果文件不存在，生成帶教學的默認 JSON。
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
            return;
        }

        try {
            JsonObject root = parseJson(file);
            if (root == null)
                return;

            JsonArray rituals = root.has("rituals") ? root.getAsJsonArray("rituals") : null;
            if (rituals == null || rituals.size() == 0) {
                Adversity.LOGGER.info("[Adversity] No rituals defined in {}", FILE_NAME);
                return;
            }

            int count = 0;
            for (JsonElement elem : rituals) {
                if (!elem.isJsonObject())
                    continue;
                JsonObject obj = elem.getAsJsonObject();

                // 跳過純註釋物件（只有 _comment 字段）
                if (!obj.has("id"))
                    continue;

                try {
                    Rite rite = parseRite(obj);
                    if (rite != null) {
                        RitualManager.registerRite(rite);
                        count++;
                    }
                } catch (Exception e) {
                    String id = obj.has("id") ? obj.get("id").getAsString() : "unknown";
                    Adversity.LOGGER.warn("[Adversity] Failed to parse ritual '{}' in {}: {}",
                            id, FILE_NAME, e.getMessage());
                }
            }

            Adversity.LOGGER.info("[Adversity] Loaded {} rituals from JSON", count);

        } catch (Exception e) {
            Adversity.LOGGER.error("[Adversity] Failed to load {}", FILE_NAME, e);
        }
    }

    /**
     * 解析單個儀式 JSON 物件
     */
    private static Rite parseRite(JsonObject obj) {
        String idStr = obj.get("id").getAsString();
        ResourceLocation id = idStr.contains(":")
                ? new ResourceLocation(idStr)
                : new ResourceLocation(Adversity.MODID, idStr);

        // 解析輸入
        List<ItemStack> inputs = new ArrayList<>();
        if (obj.has("inputs") && obj.get("inputs").isJsonArray()) {
            for (JsonElement inputElem : obj.getAsJsonArray("inputs")) {
                ItemStack stack = parseItemStack(inputElem.getAsJsonObject());
                if (!stack.isEmpty()) {
                    inputs.add(stack);
                }
            }
        }

        if (inputs.isEmpty()) {
            Adversity.LOGGER.warn("[Adversity] Ritual '{}' has no valid inputs, skipping", idStr);
            return null;
        }

        // 解析輸出（可選）
        ItemStack output = ItemStack.EMPTY;
        if (obj.has("output") && !obj.get("output").isJsonNull() && obj.get("output").isJsonObject()) {
            output = parseItemStack(obj.getAsJsonObject("output"));
        }

        int entropyCost = obj.has("entropy_cost") ? obj.get("entropy_cost").getAsInt() : 0;
        String requiredStage = optString(obj, "required_stage");
        String rewardStage = optString(obj, "reward_stage");
        String command = optString(obj, "command");
        int cooldownTicks = obj.has("cooldown_ticks") ? obj.get("cooldown_ticks").getAsInt() : 0;

        // 分類
        RitualCategory category;
        String catStr = optString(obj, "category");
        if (catStr != null) {
            category = RitualCategory.fromString(catStr);
        } else {
            category = guessCategory(rewardStage, command);
        }

        // 效果 ID（可選）
        ResourceLocation effectId = null;
        String effectStr = optString(obj, "effect");
        if (effectStr != null) {
            effectId = effectStr.contains(":")
                    ? new ResourceLocation(effectStr)
                    : new ResourceLocation(Adversity.MODID, effectStr);
        }

        // 效果參數（可選）
        Map<String, Object> effectParams = new HashMap<>();
        if (obj.has("effect_params") && obj.get("effect_params").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : obj.getAsJsonObject("effect_params").entrySet()) {
                JsonElement val = entry.getValue();
                if (val.isJsonPrimitive()) {
                    JsonPrimitive prim = val.getAsJsonPrimitive();
                    if (prim.isNumber()) {
                        effectParams.put(entry.getKey(), prim.getAsNumber());
                    } else if (prim.isBoolean()) {
                        effectParams.put(entry.getKey(), prim.getAsBoolean());
                    } else {
                        effectParams.put(entry.getKey(), prim.getAsString());
                    }
                } else if (val.isJsonArray()) {
                    // 陣列轉 List<String>
                    List<String> list = new ArrayList<>();
                    for (JsonElement arrElem : val.getAsJsonArray()) {
                        if (arrElem.isJsonPrimitive()) {
                            list.add(arrElem.getAsString());
                        }
                    }
                    effectParams.put(entry.getKey(), list);
                }
            }
        }

        return new Rite(id, inputs, output, entropyCost,
                requiredStage, rewardStage, command, category, cooldownTicks,
                effectId, effectParams.isEmpty() ? null : effectParams);
    }

    /**
     * 解析物品 JSON：{"item": "minecraft:golden_apple", "count": 1, "meta": 0}
     */
    private static ItemStack parseItemStack(JsonObject obj) {
        if (!obj.has("item"))
            return ItemStack.EMPTY;

        String itemId = obj.get("item").getAsString();
        Item item = Item.getByNameOrId(itemId);
        if (item == null) {
            Adversity.LOGGER.warn("[Adversity] Unknown item: {}", itemId);
            return ItemStack.EMPTY;
        }

        int count = obj.has("count") ? obj.get("count").getAsInt() : 1;
        int meta = obj.has("meta") ? obj.get("meta").getAsInt() : 0;

        return new ItemStack(item, count, meta);
    }

    private static String optString(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull())
            return null;
        String val = obj.get(key).getAsString();
        return val.isEmpty() ? null : val;
    }

    private static RitualCategory guessCategory(String rewardStage, String command) {
        if (rewardStage != null && !rewardStage.isEmpty())
            return RitualCategory.STAGE_UNLOCK;
        if (command != null && !command.isEmpty()) {
            if (command.contains("advdiff"))
                return RitualCategory.DIFFICULTY;
            return RitualCategory.UTILITY;
        }
        return RitualCategory.CRAFTING;
    }

    private static JsonObject parseJson(File file) {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, JsonObject.class);
        } catch (Exception e) {
            Adversity.LOGGER.error("[Adversity] Failed to parse {}", file.getName(), e);
            return null;
        }
    }

    private static void generateDefault(File file) {
        String defaultContent = "{\n"
                + "  \"_comment\": \"=== Adversity Ritual Configuration ===\",\n"
                + "  \"_doc_priority\": \"CraftTweaker scripts override these settings. CRT takes priority.\",\n"
                + "  \"_doc_categories\": \"stage_unlock | crafting | utility | difficulty | custom\",\n"
                + "  \"_doc_fields\": {\n"
                + "    \"id\": \"Unique ritual ID. Auto-prepends 'adversity:' if no colon present.\",\n"
                + "    \"inputs\": \"Array of input items: [{item, count, meta(optional)}]. Single or multiple.\",\n"
                + "    \"output\": \"Output item: {item, count, meta(optional)}. Omit or null for command-only.\",\n"
                + "    \"entropy_cost\": \"Sanctuary entropy cost (integer).\",\n"
                + "    \"required_stage\": \"Stage required to perform. null = no requirement.\",\n"
                + "    \"reward_stage\": \"Stage granted upon completion. null = no reward.\",\n"
                + "    \"command\": \"Server command. Placeholders: {player}, {x}, {y}, {z}, {dim}.\",\n"
                + "    \"cooldown_ticks\": \"Cooldown in ticks (20=1sec). 0 = no cooldown.\",\n"
                + "    \"category\": \"Category for display. Auto-guessed if omitted.\",\n"
                + "    \"effect\": \"Ritual effect ID for special effects (e.g. adversity:purge_curse).\",\n"
                + "    \"effect_params\": \"Parameters for the effect (depends on effect type).\"\n"
                + "  },\n"
                + "\n"
                + "  \"_example_stage_unlock\": {\n"
                + "    \"id\": \"awakening\",\n"
                + "    \"inputs\": [{\"item\": \"minecraft:golden_apple\", \"count\": 1}],\n"
                + "    \"output\": {\"item\": \"adversity:sanctuary_compass\", \"count\": 1},\n"
                + "    \"entropy_cost\": 500,\n"
                + "    \"reward_stage\": \"awakened\",\n"
                + "    \"category\": \"stage_unlock\"\n"
                + "  },\n"
                + "\n"
                + "  \"_example_command_ritual\": {\n"
                + "    \"id\": \"prayer_of_peace\",\n"
                + "    \"inputs\": [{\"item\": \"adversity:calm_dust\", \"count\": 4}],\n"
                + "    \"entropy_cost\": 200,\n"
                + "    \"command\": \"advdiff sub {player} 1\",\n"
                + "    \"cooldown_ticks\": 6000,\n"
                + "    \"category\": \"difficulty\"\n"
                + "  },\n"
                + "\n"
                + "  \"_example_purge\": {\n"
                + "    \"id\": \"purge_curse\",\n"
                + "    \"inputs\": [{\"item\": \"minecraft:milk_bucket\", \"count\": 1}],\n"
                + "    \"entropy_cost\": 1000,\n"
                + "    \"required_stage\": \"warden\",\n"
                + "    \"effect\": \"adversity:purge_curse\",\n"
                + "    \"effect_params\": {\n"
                + "      \"curse_type\": \"black_swan\",\n"
                + "      \"purge_amount\": 0.1,\n"
                + "      \"immunity_duration\": 6000\n"
                + "    }\n"
                + "  },\n"
                + "\n"
                + "  \"rituals\": []\n"
                + "}\n";

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(defaultContent);
        } catch (IOException e) {
            Adversity.LOGGER.error("[Adversity] Failed to generate default {}", FILE_NAME, e);
        }
    }
}
