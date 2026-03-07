import mods.adversity.Sanctuary;
import mods.adversity.EliteLoot;
import crafttweaker.data.IData;

// ============================================================
// 儀式系統綜合測試腳本
//
// 測試項目:
//   1. 物品門控 — 鑽石劍、鑽石甲需要 scholar
//   2. 5 輸入融合儀式 — 測試動態 GUI (5 個輸入槽 + 同心圓排版)
//   3. 2 輸入 / 3 輸入 / 4 輸入儀式 — 測試不同槽位數
//   4. 1 輸入基礎儀式 — 向後兼容
//   5. T5 精英怪必掉鑽石磚
//
// 測試方式:
//   1. 放入 run/scripts/
//   2. gradlew runClient
//   3. 找到聖所核心 → 打開 GUI
//   4. 確認有 5 個輸入槽位（同心圓）+ 1 個中心輸出
//   5. 分別測試各儀式的物品放入與執行
//   6. /advdiff add <player> 80 → 殺怪 → 確認鑽石磚掉落
//   7. /progression add scholar → 確認門控解除
// ============================================================

// ==================== 1. 物品門控 ====================

Sanctuary.setItemStage(<minecraft:diamond_sword>, "scholar");
Sanctuary.setItemStages([
    <minecraft:diamond_chestplate>,
    <minecraft:diamond_leggings>,
    <minecraft:diamond_boots>,
    <minecraft:diamond_helmet>
], "scholar");

print("[Test] Item gating: diamond sword + armor → scholar");

// ==================== 2. 多輸入融合儀式（5 inputs） ====================
// 這是最大的，決定動態 GUI 顯示 5 個槽位

Sanctuary.addMultiInputRitual("five_gem_fusion",
    [
        <minecraft:diamond> * 4,
        <minecraft:emerald> * 4,
        <minecraft:redstone> * 16,
        <minecraft:dye:4> * 16,
        <minecraft:gold_ingot> * 8
    ],
    <minecraft:nether_star>,
    3000,
    null,       // 無前置階段（方便測試）
    null,       // 無獎勵階段
    "crafting"
);

print("[Test] 5-input ritual registered: five_gem_fusion");

// ==================== 3. 4 輸入融合儀式 ====================

Sanctuary.addMultiInputRitual("four_element_merge",
    [
        <minecraft:blaze_powder> * 4,
        <minecraft:ghast_tear>,
        <minecraft:magma_cream> * 4,
        <minecraft:ender_pearl> * 4
    ],
    <minecraft:dragon_breath> * 8,
    2000,
    null, null, "crafting"
);

print("[Test] 4-input ritual registered: four_element_merge");

// ==================== 4. 3 輸入融合儀式 ====================

Sanctuary.addMultiInputRitual("triple_ore_refine",
    [
        <minecraft:iron_ingot> * 16,
        <minecraft:gold_ingot> * 8,
        <minecraft:diamond> * 2
    ],
    <minecraft:emerald> * 8,
    1000,
    null, null, "crafting"
);

print("[Test] 3-input ritual registered: triple_ore_refine");

// ==================== 5. 2 輸入融合儀式 ====================

Sanctuary.addMultiInputRitual("dual_crystal_forge",
    [
        <minecraft:prismarine_shard> * 8,
        <minecraft:quartz> * 16
    ],
    <minecraft:sea_lantern> * 4,
    500,
    null, null, "crafting"
);

print("[Test] 2-input ritual registered: dual_crystal_forge");

// ==================== 6. 基礎 1 輸入儀式（兼容測試） ====================

Sanctuary.addRitual("simple_convert",
    <minecraft:coal> * 64,
    <minecraft:diamond>,
    100,
    null,
    null
);

print("[Test] 1-input ritual registered: simple_convert");

// ==================== 7. 儀式效果綁定測試 ====================
// setRitualEffect(儀式ID, 效果ID, 參數)
// ⚠️ 「儀式ID」= 你用 addRitual 等注冊的名稱（如 "simple_convert"）
// ⚠️ 「效果ID」= IRitualEffect 實作的 registry key（如 "adversity:awakening"）
//     兩者是不同的東西！效果 ID 要在 RitualEffectRegistry 中存在

// 把 "simple_convert" 儀式綁定覺醒效果：完成煤→鑽石的同時打開覺醒 GUI
Sanctuary.setRitualEffect("simple_convert", "adversity:awakening", {
    select_count: 2,
    duration_ticks: 600,
    use_blacklist: true
} as IData);

print("[Test] setRitualEffect: simple_convert → awakening (select 2 effects for 30s)");

// ==================== 8. T5 精英掉落 ====================

// T5 = 最高 tier，必掉 (chance=1.0) 鑽石磚
EliteLoot.addDrop(5, <minecraft:diamond_block>, 1.0);

// 順便加一些低 tier 的測試
EliteLoot.addDrop(3, <minecraft:iron_ingot> * 4, 0.5);
EliteLoot.addDrop(4, <minecraft:gold_ingot> * 2, 0.3);

print("[Test] Elite loot: T5 → guaranteed diamond block");

// ==================== 測試總結 ====================

print("============================================================");
print("[Adversity Test] Ritual system test script loaded!");
print("  -> Item gating: diamond gear → scholar");
print("  -> Rituals: 1/2/3/4/5 input variants registered");
print("  -> Max input count = 5 (GUI should show 5 slots)");
print("  -> setRitualEffect: awakening effect configured");
print("  -> Elite loot: T5 → diamond_block (100%)");
print("============================================================");

