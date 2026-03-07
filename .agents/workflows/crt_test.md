---
description: How to write and test CRT scripts for the Adversity ritual/gating/loot system
---

# CRT 測試工作流程

## 1. 撰寫 ZS 測試腳本

在 `run/scripts/` 目錄下建立 `.zs` 文件。

可用的三個 ZenClass：
```zenscript
import mods.adversity.Progression;   // 進度階段
import mods.adversity.Sanctuary;     // 聖所儀式 + 門控
import mods.adversity.EliteLoot;     // 精英掉落
```

> 完整 API 文檔見：`brain/crt_tutorial.md`（Artifact）

### 儀式 API 速查

```zenscript
// 基礎
Sanctuary.addRitual(name, input, output, cost, reqStage, rewardStage);
// 命令
Sanctuary.addCommandRitual(name, input, cost, reqStage, cmd);
// 冷卻命令
Sanctuary.addCooldownCommandRitual(name, input, cost, reqStage, cmd, cdTick);
// 帶命令+輸出
Sanctuary.addRitualWithCommand(name, in, out, cost, req, reward, cmd);
// 多輸入
Sanctuary.addMultiInputRitual(name, in[], out, cost, req, reward, category);
// 綁定效果 (IData 參數)
Sanctuary.setRitualEffect(riteId, effectId, {params} as crafttweaker.api.data.IData);
// 移除
Sanctuary.removeRitual(id);
Sanctuary.removeAllRituals();
```

### 門控 API 速查

```zenscript
Sanctuary.setItemStage(<item>, "stage");
Sanctuary.setItemStages([<item1>,<item2>], "stage");
Sanctuary.setEnchantmentStage("modid:name", "stage");
Sanctuary.removeItemStage(<item>);
Sanctuary.removeEnchantmentStage("modid:name");
```

### 精英掉落 API 速查

```zenscript
EliteLoot.addDrop(tier, <item>, chance);  // chance: 0.0~1.0, tier: HP 強化 tier
EliteLoot.clear();
```

### 注意事項

- 1.12.2 物品 ID：青金石是 `<minecraft:dye:4>`，不是 `<minecraft:lapis_lazuli>`
- `setRitualEffect` 的第三個參數需要 `as crafttweaker.api.data.IData` 強轉
- 空陣列需要 `[] as string[]` 明確類型

## 2. 編譯模組

// turbo
```
cd c:\mywork\adversity && .\gradlew compileJava
```

## 3. 啟動客戶端

```
cd c:\mywork\adversity && .\gradlew runClient
```

## 4. 測試驗證

進入遊戲後用以下命令測試：

```
/progression add awakened       # 解鎖階段
/progression add scholar
/progression add warden
/progression add champion
/progression remove awakened    # 移除階段
/progression tier               # 顯示當前等級
```

### 測試物品門控
1. 在合成台嘗試使用被門控物品 → 應該被阻止
2. `/progression add <stage>` → 應該可以正常使用

### 測試儀式
1. 找到/放置聖所核心
2. 打開 GUI → 檢查動態槽位數量（應符合最大 input 數）
3. 放入物品 → 按 "執行儀式" → 驗證輸出

### 測試精英掉落
1. 提高區域難度：`/advdiff add <player> 50`
2. 擊殺強化怪物 → 檢查是否掉落對應物品

## 5. 熱重載 (不需重啟)

修改 `.zs` 文件後执行：
```
/ct reload
```
CRT 會重新加載所有腳本。但注意 `removeAllRituals()` 需要在腳本開頭確保清理。
