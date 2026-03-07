import mods.adversity.Sanctuary;

// ============================================================
// 附魔門控測試腳本 — 黑白名單模式
// 使用白名單模式：只有列表內的附魔免 gate，其餘全部需要 champion
// 測試方式:
//   1. 放入 scripts/ 資料夾
//   2. runClient → 開附魔台 → 只有鋒利出現
//   3. /progression add champion → 全部恢復
//   4. 開鐵砧 → 非鋒利附魔書無法使用
// ============================================================

// 白名單模式 (useBlacklist=false): 列表內放行，其餘全部 gate
Sanctuary.setEnchantmentGating("champion", true, [
    "minecraft:sharpness",
    "minecraft:unbreaking",
    "minecraft:efficiency"
]);

print("[Adversity Test] Enchant gating: whitelist mode, only sharpness exempt, all others gated to champion");
