---
description: How to add a new enchantment to the Adversity mod
---

# 新增附魔流程

## 1. 創建附魔類

在 `src/main/java/com/adversity/enchantment/` 新建 `EnchantmentYourName.java`:

```java
public class EnchantmentYourName extends Enchantment {
    public EnchantmentYourName() {
        super(Rarity.RARE, EnumEnchantmentType.WEAPON, new EntityEquipmentSlot[]{...});
        this.setRegistryName(Adversity.MODID, "your_name");
        this.setName("adversity.your_name");
    }
    // ...
}
```

## 2. 註冊附魔

在 `EnchantmentRegistry.java` 的 `registerEnchantments()` 方法中添加:

```java
event.getRegistry().registerAll(
    // ... existing enchantments ...
    new EnchantmentYourName()
);
```

並添加 ObjectHolder 字段:

```java
@GameRegistry.ObjectHolder(Adversity.MODID + ":your_name")
public static final Enchantment YOUR_NAME = null;
```

## 3. 自動生效的系統

以下系統**不需要手動註冊**，新附魔會自動被發現:

- **EnchantmentGatingHelper**: 動態掃描 `ForgeRegistries.ENCHANTMENTS` 中 `adversity:*` 命名空間
- **附魔台聖所注入**: 自動包含新附魔
- **CRT 門控**: `Sanctuary.setEnchantmentStage()` 可以門控任何已註冊附魔

## 4. 語言文件

在 `zh_cn.lang` 和 `en_us.lang` 添加翻譯:

```
enchantment.adversity.your_name=你的附魔名
```

## 5. (可選) 階段門控

如需限制附魔需要某個 GS 階段:

```zenscript
Sanctuary.setEnchantmentStage("adversity:your_name", "scholar");
```

注意: 如果 `skipAdversityEnchantmentGating = true` (默認), CRT 門控不會影響 Adversity 附魔。此時附魔由聖所系統管理 (需在聖所附近才出現)。
