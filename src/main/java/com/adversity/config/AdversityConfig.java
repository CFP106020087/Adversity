package com.adversity.config;

import com.adversity.Adversity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.IMob;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@Config(modid = Adversity.MODID, name = "adversity")
public class AdversityConfig {

    @Config.Comment({
        "Entity Filter Settings",
        "实体过滤设置"
    })
    public static final EntityFilter entityFilter = new EntityFilter();

    @Config.Comment({
        "Difficulty Source Settings (Distance, Time, etc.)",
        "难度来源设置（距离、时间等）"
    })
    public static final DifficultySource difficultySource = new DifficultySource();

    @Config.Comment({
        "Mob Stat Scaling Settings",
        "怪物属性缩放设置",
        "This is where you configure how mob stats scale with difficulty.",
        "这里配置怪物属性如何随难度缩放。"
    })
    public static final StatScaling statScaling = new StatScaling();

    @Config.Comment({
        "Elite Spawn Settings",
        "精英生成设置"
    })
    public static final EliteSettings eliteSettings = new EliteSettings();

    @Config.Comment({
        "Affix Settings - Control which affixes are enabled/disabled",
        "词条设置 - 控制哪些词条启用/禁用"
    })
    public static final AffixSettings affixSettings = new AffixSettings();

    @Config.Comment({
        "Loot and Reward Settings",
        "战利品和奖励设置"
    })
    public static final LootSettings lootSettings = new LootSettings();

    @Config.Comment({
        "Permanent Curse Settings (Black Swan, Black Friday, Black Coffin)",
        "永久诅咒设置 (黑天鹅、黑色星期五、黑棺)"
    })
    public static final PermanentCurseSettings curseSettings = new PermanentCurseSettings();

    @Config.Comment({
        "Nightmare Spawn Settings",
        "梦魇生成设置"
    })
    public static final NightmareSettings nightmareSettings = new NightmareSettings();

    @Config.Comment({
        "Client Display Settings",
        "客户端显示设置"
    })
    public static final ClientSettings clientSettings = new ClientSettings();

    @Config.Comment({
            "Sanctuary Settings - Safe zones for players",
            "圣所设置 - 玩家安全区域"
    })
    public static final SanctuarySettings sanctuarySettings = new SanctuarySettings();

    // ==================== 实体过滤 ====================

    public static class EntityFilter {

        @Config.Comment({
            "If true, only entities in the whitelist will be affected.",
            "若为 true，只有白名单中的实体会被影响。"
        })
        public boolean whitelistOnly = false;

        @Config.Comment({
            "Entity whitelist. Format: modid:entity_name",
            "实体白名单。格式: modid:entity_name"
        })
        public String[] whitelist = new String[] {};

        @Config.Comment({
            "Entity blacklist. These entities will NEVER be affected.",
            "实体黑名单。这些实体永远不会受到影响。"
        })
        public String[] blacklist = new String[] {
            "minecraft:ender_dragon",
            "minecraft:wither"
        };
    }

    // ==================== 难度来源 ====================
    //
    // === 难度计算公式 / DIFFICULTY CALCULATION FORMULA ===
    //
    // 最终难度 = (距离难度 × 距离权重) + (时间难度 × 时间权重)
    // Final Difficulty = (Distance Difficulty × distanceWeight) + (Time Difficulty
    // × timeWeight)
    //
    // 距离难度 = max(0, (distance - safeDistance) / blocksPerDifficulty)
    // Distance Difficulty = max(0, (distance_from_origin - safeDistance) /
    // blocksPerDifficulty)
    //
    // 时间难度 = worldTime / (daysPerDifficulty × 24000)
    // Time Difficulty = worldTimeInTicks / (daysPerDifficulty × 24000)
    //
    // 示例 / Example:
    // 距离2000格, 安全距离500, blocksPerDifficulty=500 → 距离难度 = (2000-500)/500 = 3
    // 游戏第4天, daysPerDifficulty=2 → 时间难度 = 4/2 = 2
    // 最终难度 = 3×1.0 + 2×1.0 = 5
    //

    public static class DifficultySource {

        @Config.Comment({
            "Safe distance (blocks) - no difficulty within this range from origin",
            "安全距离（格）- 在此范围内不计算距离难度",
            "",
            "For RLCraft with random spawn (±10000), set this to ~15000",
            "RLCraft随机出生点(±10000)建议设为15000"
        })
        @Config.RangeDouble(min = 0, max = 100000)
        public double safeDistance = 5000;

        @Config.Comment({
            "Distance (in blocks) per 1 difficulty point (after safe distance)",
            "超出安全距离后，每增加多少格距离增加 1 点难度"
        })
        @Config.RangeDouble(min = 50, max = 10000)
        public double blocksPerDifficulty = 1500;

        @Config.Comment({
            "Maximum difficulty from distance",
            "距离难度的最大值 (0 = 无上限)"
        })
        @Config.RangeDouble(min = 0, max = 1000)
        public double maxDistanceDifficulty = 50;

        @Config.Comment({
            "Days per 1 difficulty point",
            "每过多少天增加 1 点难度"
        })
        @Config.RangeDouble(min = 0.5, max = 100)
        public double daysPerDifficulty = 2; // 加速增长 (每2天+1难度，配合词条系统平衡)

        @Config.Comment({
            "Maximum difficulty from time (0 = no limit)",
            "时间难度的最大值 (0 = 无上限)"
        })
        @Config.RangeDouble(min = 0, max = 1000)
        public double maxTimeDifficulty = 50;

        @Config.Comment({
            "Weight for distance difficulty in final calculation",
            "距离难度在最终计算中的权重"
        })
        @Config.RangeDouble(min = 0, max = 10)
        public double distanceWeight = 1.0;

        @Config.Comment({
            "Weight for time difficulty in final calculation",
            "时间难度在最终计算中的权重"
        })
        @Config.RangeDouble(min = 0, max = 10)
        public double timeWeight = 0.8;

        @Config.Comment({
                "Use per-player play time instead of global world time for time difficulty",
                "使用玩家个人游玩时间而非全局世界时间计算时间难度",
                "",
                "true = Each player has their own difficulty progression (recommended for multiplayer)",
                "false = All players share the same time-based difficulty (legacy behavior)",
                "",
                "true = 每个玩家有独立的难度进度（多人游戏推荐）",
                "false = 所有玩家共享时间难度（传统模式）"
        })
        public boolean usePerPlayerTime = true;
    }

    // ==================== 属性缩放 (核心重构) ====================

    public static class StatScaling {

        @Config.Comment({
            "=== HEALTH SCALING ===",
            "=== 生命值缩放 ===",
            "",
            "Scaling mode for health. Options:",
            "  LINEAR     - base + diff × rate (predictable, vanilla-like)",
            "  EXPONENTIAL - base × e^(diff × rate) (mid-game ramp up)",
            "  COMPOUND   - base × (1 + rate)^diff (smooth exponential, recommended)",
            "  POLYNOMIAL - base + diff^power × rate (end-game explosion)",
            "  LOGARITHMIC - base + ln(1+diff) × rate (soft cap)",
            "  SIGMOID    - smooth transition to max value",
            "",
            "生命值缩放模式。可选:",
            "  LINEAR - 线性增长，适合原版体验",
            "  EXPONENTIAL - 指数增长，适合中型模组包",
            "  COMPOUND - 复合增长，推荐通用模组包",
            "  POLYNOMIAL - 多项式增长，适合重型模组包",
            "  LOGARITHMIC - 对数增长，软上限",
            "  SIGMOID - 平滑过渡到最大值"
        })
        public String healthScalingMode = "LINEAR";

        @Config.Comment({
            "Base health multiplier (usually 1.0)",
            "基础生命值倍率（通常为 1.0）"
        })
        @Config.RangeDouble(min = 0.1, max = 10)
        public double healthBase = 1.0;

        @Config.Comment({
            "Health growth rate per difficulty point",
            "每点难度的生命值增长率",
            "",
            "Examples with COMPOUND mode:",
            "  0.10 = diff20 → 6.7x, diff50 → 117x",
            "  0.15 = diff20 → 16x, diff50 → 1084x",
            "  0.20 = diff20 → 38x, diff50 → 9100x",
            "  0.30 = diff20 → 190x, diff50 → 497929x"
        })
        @Config.RangeDouble(min = 0, max = 2)
        public double healthRate = 0.08;

        @Config.Comment({
            "Power exponent for POLYNOMIAL mode",
            "POLYNOMIAL 模式的幂次"
        })
        @Config.RangeDouble(min = 1, max = 5)
        public double healthPower = 2.0;

        @Config.Comment({
            "Maximum health multiplier (0 = no limit)",
            "最大生命值倍率 (0 = 无上限)",
            "",
            "For heavy modpacks, set this to 0 or a very high value",
            "重型模组包建议设为 0 或极高值"
        })
        @Config.RangeDouble(min = 0, max = 1000000000)
        public double healthMax = 0;

        @Config.Comment({
            "=== DAMAGE SCALING ===",
            "=== 攻击力缩放 ===",
            "",
            "Scaling mode for damage"
        })
        public String damageScalingMode = "LINEAR";

        @Config.Comment("Base damage multiplier")
        @Config.RangeDouble(min = 0.1, max = 10)
        public double damageBase = 1.0;

        @Config.Comment({
            "Damage growth rate per difficulty point",
            "每点难度的攻击力增长率",
            "",
            "Typically lower than health to keep fights challenging but fair"
        })
        @Config.RangeDouble(min = 0, max = 2)
        public double damageRate = 0.05;

        @Config.Comment("Power exponent for POLYNOMIAL mode")
        @Config.RangeDouble(min = 1, max = 5)
        public double damagePower = 2.0;

        @Config.Comment("Maximum damage multiplier (0 = no limit)")
        @Config.RangeDouble(min = 0, max = 1000000000)
        public double damageMax = 0;

        @Config.Comment({
            "=== ARMOR SCALING ===",
            "=== 盔甲值缩放 ===",
            "",
            "Scaling mode for armor (added to base armor)"
        })
        public String armorScalingMode = "LINEAR";

        @Config.Comment("Base armor bonus")
        @Config.RangeDouble(min = 0, max = 100)
        public double armorBase = 0;

        @Config.Comment({
            "Armor bonus per difficulty point",
            "每点难度增加的盔甲值"
        })
        @Config.RangeDouble(min = 0, max = 10)
        public double armorRate = 0.2;

        @Config.Comment("Power exponent for POLYNOMIAL mode")
        @Config.RangeDouble(min = 1, max = 5)
        public double armorPower = 1.5;

        @Config.Comment({
            "Maximum armor bonus (recommended: 20-30 for vanilla, higher for modpacks)",
            "最大盔甲加成 (原版建议 20-30，模组包可更高)"
        })
        @Config.RangeDouble(min = 0, max = 10000)
        public double armorMax = 30;

        @Config.Comment({
            "=== DAMAGE REDUCTION SCALING ===",
            "=== 减伤系统缩放 ===",
            "",
            "Damage reduction is a percentage that reduces incoming damage",
            "减伤是一个减少受到伤害的百分比",
            "",
            "Scaling mode for damage reduction"
        })
        public String damageReductionScalingMode = "SIGMOID";

        @Config.Comment("Base damage reduction (0-1, e.g., 0.1 = 10%)")
        @Config.RangeDouble(min = 0, max = 0.99)
        public double damageReductionBase = 0;

        @Config.Comment({
            "Damage reduction growth rate",
            "减伤增长率",
            "",
            "For SIGMOID mode, this controls how fast it approaches max"
        })
        @Config.RangeDouble(min = 0, max = 1)
        public double damageReductionRate = 0.03;

        @Config.Comment("Power exponent for POLYNOMIAL mode")
        @Config.RangeDouble(min = 1, max = 5)
        public double damageReductionPower = 1.5;

        @Config.Comment({
            "Maximum damage reduction (cap)",
            "减伤上限",
            "",
            "0.9 = max 90% reduction (mobs take at least 10% damage)",
            "0.99 = near-immunity (only for extreme modpacks)",
            "0.9 = 最多减少 90% 伤害",
            "0.99 = 接近免疫（仅限极端模组包）"
        })
        @Config.RangeDouble(min = 0, max = 0.99)
        public double damageReductionMax = 0.5;

        @Config.Comment({
            "=== ADVANCED: ARMOR PENETRATION RESISTANCE ===",
            "=== 高级：护甲穿透抗性 ===",
            "",
            "Some modpacks have armor penetration/true damage mechanics.",
            "This setting adds resistance to such effects.",
            "某些模组包有护甲穿透/真实伤害机制。",
            "此设置可添加对这些效果的抗性。",
            "",
            "Enable armor penetration resistance scaling"
        })
        public boolean enableArmorPenResist = false;

        @Config.Comment("Armor penetration resistance per difficulty (percentage)")
        @Config.RangeDouble(min = 0, max = 0.1)
        public double armorPenResistRate = 0.01;

        @Config.Comment("Maximum armor penetration resistance")
        @Config.RangeDouble(min = 0, max = 0.9)
        public double armorPenResistMax = 0.5;
    }

    // ==================== 精英设置 ====================

    public static class EliteSettings {

        @Config.Comment({
            "Base chance for a mob to become elite (0.0 to 1.0)",
            "怪物成为精英的基础概率"
        })
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double eliteChance = 0.15;

        @Config.Comment({
            "Additional elite chance per difficulty point",
            "每点难度增加的精英概率"
        })
        @Config.RangeDouble(min = 0.0, max = 0.5)
        public double eliteChancePerDifficulty = 0.02;

        @Config.Comment({
            "Maximum elite chance",
            "精英概率上限"
        })
        @Config.RangeDouble(min = 0.1, max = 1.0)
        public double maxEliteChance = 0.55;

        @Config.Comment({
            "Minimum difficulty required for elite spawns",
            "生成精英所需的最低难度"
        })
        @Config.RangeDouble(min = 0, max = 50)
        public double minDifficultyForElite = 1.5;

        @Config.Comment({
            "=== TIER THRESHOLDS ===",
            "=== 等级阈值 ===",
            "",
            "Difficulty thresholds for each tier (T1-T10)",
            "Mobs below the first threshold are normal (T0)",
            "各等级的难度阈值（T1-T10）",
            "低于第一个阈值的为普通怪物（T0）"
        })
        public double[] tierThresholds = new double[] {
            2.0,   // T1 - Elite
            3.5,   // T2 - Rare
            5.5,   // T3 - Veteran
            8.0,   // T4 - Epic
            11.0,  // T5 - Legendary
            15.0,  // T6 - Mythic
            20.0,  // T7 - Ancient
            27.0,  // T8 - Void
            35.0,  // T9 - Abyssal
            45.0   // T10 - Terminus
        };

        @Config.Comment({
            "Number of affixes per tier (T1-T10)",
            "各等级的词条数量（T1-T10）",
            "Format: min for each tier, randomness added internally"
        })
        public int[] affixCountPerTier = new int[] {
            1,  // T1
            1,  // T2 (+0-1 random)
            2,  // T3
            2,  // T4 (+0-1 random)
            3,  // T5
            3,  // T6 (+0-1 random)
            4,  // T7
            4,  // T8 (+0-1 random)
            5,  // T9
            5   // T10 (+0-1 random)
        };
    }

    // ==================== 词条设置 ====================

    public static class AffixSettings {

        @Config.Comment({
            "Disabled affixes. These affixes will never be applied to any mob.",
            "禁用的词条列表。这些词条永远不会被应用到任何怪物上。",
            "Format: modid:affix_name (e.g., adversity:splitting, adversity:teleporting)",
            "格式: modid:affix_name（例如 adversity:splitting, adversity:teleporting）"
        })
        public String[] disabledAffixes = new String[] {};

        @Config.Comment({
            "=== FORCED TIER ENTITIES ===",
            "=== 强制等级实体 ===",
            "",
            "Force specific entities to always be a specific tier.",
            "强制特定实体始终为指定等级。",
            "",
            "Format: entity_id|tier (e.g., lycanites:rahovart|10 for Terminus)",
            "格式: entity_id|tier（例如 lycanites:rahovart|10 表示终焉）",
            "",
            "Tier names: T1=Elite, T5=Legendary, T10=Terminus",
            "等级名称: T1=精英, T5=传奇, T10=终焉"
        })
        public String[] forcedTierEntities = new String[] {
            // "lycanites:rahovart|10",  // Example: Rahovart is always Terminus
            // "iceandfire:ice_dragon|8"  // Example: Ice Dragon is always T8
        };

        @Config.Comment({
            "=== ELITE BLACKLIST ===",
            "=== 精英黑名单 ===",
            "",
            "Entities in this list will NEVER become elite (but still get stat scaling).",
            "此列表中的实体永远不会成为精英（但仍会获得属性加成）。",
            "Format: modid:entity_name",
            "格式: modid:entity_name"
        })
        public String[] eliteBlacklistEntities = new String[] {};

        @Config.Comment({
            "=== PER-AFFIX ENTITY BLACKLIST ===",
            "=== 词条实体黑名单 ===",
            "",
            "Block specific affixes from specific entities.",
            "阻止特定词条应用于特定实体。",
            "Format: affix_id|entity_id (e.g., adversity:splitting|minecraft:slime)",
            "格式: affix_id|entity_id（例如 adversity:splitting|minecraft:slime）"
        })
        public String[] affixEntityBlacklist = new String[] {
            "adversity:splitting|minecraft:slime",  // 史莱姆不应该分裂（已经会分裂了）
            "adversity:splitting|minecraft:magma_cube"  // 岩浆怪同理
        };

        @Config.Comment({
            "=== FORCED AFFIXES FOR SPECIFIC ENTITIES ===",
            "=== 特定实体的强制词条 ===",
            "",
            "Force specific affixes to always appear on specific entities (when elite).",
            "强制特定词条始终出现在特定实体上（当成为精英时）。",
            "Format: entity_id|affix_id (e.g., minecraft:wither_skeleton|adversity:withering)",
            "格式: entity_id|affix_id（例如 minecraft:wither_skeleton|adversity:withering）"
        })
        public String[] forcedAffixesForEntities = new String[] {
            "minecraft:wither_skeleton|adversity:withering"
        };

        @Config.Comment({
            "=== SEAL AFFIX SETTINGS ===",
            "=== 封印词条设置 ===",
            "",
            "Base seal duration for Shackle affix (ticks, 20 = 1 second)",
            "枷锁词条的基础封印时间（tick，20 = 1秒）"
        })
        @Config.RangeInt(min = 100, max = 6000)
        public int shackleSealDuration = 600;  // 30秒

        @Config.Comment({
            "Base seal duration for Divest affix (ticks, 20 = 1 second)",
            "褫夺词条的基础封印时间（tick，20 = 1秒）"
        })
        @Config.RangeInt(min = 100, max = 6000)
        public int divestSealDuration = 400;  // 20秒

        @Config.Comment({
            "Additional seal duration per tier (ticks)",
            "每级额外封印时间（tick）"
        })
        @Config.RangeInt(min = 0, max = 200)
        public int sealDurationPerTier = 80;  // 4秒每级

        @Config.Comment({
                "=== AFFIX TIER LIMITS ===",
                "=== 词条等级限制 ===",
                "",
                "Limit specific affixes to only appear on certain tiers.",
                "限制特定词条只能出现在特定等级。",
                "",
                "Format: affix_id:min-max (e.g., adversity:splitting:1-5 means T1-T5 only)",
                "格式: affix_id:min-max（例如 adversity:splitting:1-5 表示只在T1-T5出现）"
        })
        public String[] affixTierLimits = new String[] {
                // "adversity:splitting:1-5", // Example: Splitting only on T1-T5
                // "adversity:outer_god:8-10" // Example: Outer God only on T8-T10
        };
    }

    // ==================== 永久诅咒设置 ====================

    public static class PermanentCurseSettings {

        @Config.Comment({
            "Enable permanent curse system (Black Swan, Black Friday, Black Coffin)",
            "启用永久诅咒系统（黑天鹅、黑色星期五、黑棺）"
        })
        public boolean enablePermanentCurses = true;

        @Config.Comment({
            "=== BLACK SWAN (Attack Reduction) ===",
            "=== 黑天鹅（攻击力削减）===",
            "",
            "Attack power reduction per curse trigger (percentage)",
            "每次诅咒触发时削减的攻击力（百分比）"
        })
        @Config.RangeDouble(min = 0.01, max = 0.5)
        public double blackSwanReductionPerTrigger = 0.05;

        @Config.Comment({
            "Ban threshold for Black Swan (1.0 = 100% reduction = banned)",
            "黑天鹅的封禁阈值（1.0 = 100%削减 = 封禁）"
        })
        @Config.RangeDouble(min = 0.5, max = 1.0)
        public double blackSwanBanThreshold = 1.0;

        @Config.Comment({
            "=== BLACK FRIDAY (Health Reduction) ===",
            "=== 黑色星期五（生命值削减）===",
            "",
            "Health reduction per curse trigger (half hearts)",
            "每次诅咒触发时削减的生命值（半心）"
        })
        @Config.RangeDouble(min = 0.5, max = 4.0)
        public double blackFridayReductionPerTrigger = 1.0;

        @Config.Comment({
            "Ban threshold for Black Friday (when remaining health <= this)",
            "黑色星期五的封禁阈值（当剩余生命值 <= 此值时封禁）"
        })
        @Config.RangeDouble(min = 0, max = 2.0)
        public double blackFridayBanThreshold = 0;

        @Config.Comment({
            "=== BLACK COFFIN (Inventory Sealing) ===",
            "=== 黑棺（背包封印）===",
            "",
            "Slots sealed per curse trigger",
            "每次诅咒触发时封印的槽位数"
        })
        @Config.RangeInt(min = 1, max = 9)
        public int blackCoffinSlotsPerTrigger = 1;

        @Config.Comment({
            "[Deprecated] Black Coffin now bans only when all 36 slots (including hotbar) are sealed",
            "[已弃用] 黑棺现在仅在全部36个槽位（包括快捷栏）都被封印时才会封禁玩家",
            "This config is kept for backward compatibility but has no effect",
            "此配置保留用于向后兼容，但不再生效"
        })
        @Config.RangeInt(min = 9, max = 36)
        public int blackCoffinBanThreshold = 36;
    }

    // ==================== 梦魇设置 ====================

    public static class NightmareSettings {

        @Config.Comment({
            "Enable nightmare spawn system",
            "启用梦魇生成系统"
        })
        public boolean enableNightmares = true;

        @Config.Comment({
            "Days without sleep before nightmares start spawning",
            "多少天不睡觉后开始生成梦魇"
        })
        @Config.RangeInt(min = 1, max = 14)
        public int daysUntilNightmare = 5;

        @Config.Comment({
            "Base spawn chance per check (every 5 seconds at night)",
            "每次检查的基础生成概率（夜间每5秒检查一次）"
        })
        @Config.RangeDouble(min = 0.01, max = 0.5)
        public double baseSpawnChance = 0.03;

        @Config.Comment({
            "Maximum spawn chance",
            "最大生成概率"
        })
        @Config.RangeDouble(min = 0.1, max = 1.0)
        public double maxSpawnChance = 0.25;

        @Config.Comment({
            "Additional spawn chance per day without sleep (after threshold)",
            "每多一天不睡觉增加的生成概率"
        })
        @Config.RangeDouble(min = 0.01, max = 0.2)
        public double spawnChancePerDay = 0.03;

        @Config.Comment({
            "Minimum spawn distance from player (blocks)",
            "距离玩家的最小生成距离（格）"
        })
        @Config.RangeInt(min = 4, max = 24)
        public int minSpawnDistance = 8;

        @Config.Comment({
            "Maximum spawn distance from player (blocks)",
            "距离玩家的最大生成距离（格）"
        })
        @Config.RangeInt(min = 16, max = 64)
        public int maxSpawnDistance = 24;
    }

    // ==================== 客户端显示设置 ====================

    public static class ClientSettings {

        @Config.Comment({
            "Enable difficulty HUD display in screen corner",
            "在屏幕角落启用难度HUD显示"
        })
        public boolean enableDifficultyHUD = true;

        @Config.Comment({
            "Enable mob tier display above elite mobs",
            "在精英怪物头顶显示等级"
        })
        public boolean enableMobTierDisplay = true;

        @Config.Comment({
            "Enable health bar display above elite mobs",
            "在精英怪物头顶显示血条"
        })
        public boolean enableHealthBar = true;

        @Config.Comment({
            "Enable affix names display above elite mobs",
            "在精英怪物头顶显示词条名称"
        })
        public boolean enableAffixDisplay = true;

        @Config.Comment({
            "HUD position: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT",
            "HUD位置: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT"
        })
        public String hudPosition = "TOP_LEFT";

        @Config.Comment({
            "HUD X offset from the edge (positive = towards center)",
            "HUD距离边缘的X偏移量（正值 = 向中心移动）"
        })
        @Config.RangeInt(min = 0, max = 500)
        public int hudOffsetX = 5;

        @Config.Comment({
            "HUD Y offset from the edge (positive = towards center)",
            "HUD距离边缘的Y偏移量（正值 = 向中心移动）"
        })
        @Config.RangeInt(min = 0, max = 500)
        public int hudOffsetY = 5;
    }

    // ==================== 战利品设置 ====================

    public static class LootSettings {

        @Config.Comment({
            "Enable mod item drops from elite mobs (disable if using LootTweaker)",
            "启用精英怪物的模组物品掉落（使用LootTweaker时建议禁用）"
        })
        public boolean enableModItemDrops = true;

        @Config.Comment({
            "Base XP multiplier for elite mobs (multiplied by tier)",
            "精英怪物的基础经验倍率（乘以等级）",
            "Formula: baseXP × (1 + tier × xpMultiplierPerTier)",
            "公式: 基础经验 × (1 + 等级 × xpMultiplierPerTier)"
        })
        @Config.RangeDouble(min = 0, max = 10)
        public double xpMultiplierPerTier = 0.8;

        @Config.Comment({
            "Maximum XP multiplier (cap)",
            "经验倍率上限"
        })
        @Config.RangeDouble(min = 1, max = 100)
        public double maxXpMultiplier = 10.0;

        @Config.Comment({
            "Drop multiplier for vanilla loot (based on tier)",
            "原版战利品的掉落倍率（基于等级）",
            "Formula: baseDrops × (1 + tier × lootMultiplierPerTier)",
            "公式: 基础掉落 × (1 + 等级 × lootMultiplierPerTier)"
        })
        @Config.RangeDouble(min = 0, max = 5)
        public double lootMultiplierPerTier = 0.35;

        @Config.Comment({
            "Maximum loot multiplier (cap)",
            "战利品倍率上限"
        })
        @Config.RangeDouble(min = 1, max = 50)
        public double maxLootMultiplier = 8.0;

        @Config.Comment({
            "Enable bonus XP orbs for elite kills",
            "启用精英击杀的额外经验球"
        })
        public boolean enableBonusXp = true;

        @Config.Comment({
            "Enable extra vanilla loot drops for elite kills",
            "启用精英击杀的额外原版战利品"
        })
        public boolean enableExtraLoot = true;
    }

    // ==================== 圣所设置 ====================

    public static class SanctuarySettings {
        @Config.Comment({
                "Enable built-in rituals (stage unlock, utility, difficulty adjustment)",
                "启用内建仪式（阶段解锁、实用仪式、难度调整）",
                "",
                "Set to false to delegate all ritual registration to CraftTweaker / external mods.",
                "设为 false 则完全由 CraftTweaker / 外部模组控制仪式注册。",
                "Default: false (recommended for modpack authors)",
                "默认: false（模组包作者推荐）"
        })
        public boolean enableBuiltinRituals = false;

        @Config.Comment({
                "Enable built-in special ritual effects (purge_curse, awakening).",
                "启用内建特殊仪式效果（净化诅咒、觉醒）。",
                "",
                "Independent from enableBuiltinRituals.",
                "与 enableBuiltinRituals 独立，可以单独关闭特殊效果。",
                "When disabled, these effects can be registered via CRT using setRitualEffect().",
                "Default: true"
        })
        public boolean enableBuiltinSpecialRituals = true;

        @Config.Comment({
                "Enable sanctuary system",
                "启用圣所系统"
        })
        public boolean enableSanctuaries = true;

        @Config.Comment({
                "Items required to activate a sanctuary (registry name format)",
                "激活圣所所需物品（注册名格式）",
                "Format: modid:item_name or modid:item_name:metadata",
                "格式: modid:item_name 或 modid:item_name:metadata",
                "Examples: minecraft:nether_star, minecraft:diamond_block, thermalfoundation:material:136",
                "示例: minecraft:nether_star, minecraft:diamond_block, thermalfoundation:material:136"
        })
        public String[] activationItems = new String[] { "minecraft:nether_star" };

        @Config.Comment({
                "Consume activation item when activating sanctuary",
                "激活圣所时是否消耗激活物品"
        })
        public boolean consumeActivationItem = true;

        @Config.Comment({
                "Natural sanctuary base radius (blocks)",
                "天然圣所基础半径（格）"
        })
        @Config.RangeInt(min = 100, max = 10000)
        public int naturalBaseRadius = 500;

        @Config.Comment({
                "Natural sanctuary radius per tier (blocks)",
                "天然圣所每级增加的半径（格）"
        })
        @Config.RangeInt(min = 100, max = 5000)
        public int naturalRadiusPerTier = 500;

        @Config.Comment({
                "Artificial sanctuary radius (blocks)",
                "人造圣所半径（格）"
        })
        @Config.RangeInt(min = 4, max = 32)
        public int artificialRadius = 8;

        @Config.Comment({
                "Natural sanctuary fuel capacity",
                "天然圣所燃料容量"
        })
        @Config.RangeInt(min = 1000, max = 1000000)
        public int naturalFuelCapacity = 5000;

        @Config.Comment({
                "Dimensions where natural sanctuaries can generate",
                "允许生成天然圣所的维度ID列表",
                "Default: 0 (Overworld)",
                "默认: 0 (主世界)"
        })
        public int[] allowedDimensions = new int[] { 0 };

        @Config.Comment({
                "Natural sanctuary fuel usage per hour",
                "天然圣所每小时燃料消耗"
        })
        @Config.RangeInt(min = 0, max = 1000)
        public int naturalFuelUsage = 250;

        @Config.Comment({
                "Artificial sanctuary fuel usage per hour",
                "人造圣所每小时燃料消耗"
        })
        @Config.RangeInt(min = 0, max = 1000)
        public int artificialFuelUsage = 300;

        @Config.Comment({
                "Fuel usage multiplier for SAFE mode (highest protection = highest cost)",
                "庇护模式燃料消耗倍率（最高保护 = 最高消耗）"
        })
        @Config.RangeDouble(min = 0.1, max = 10.0)
        public double safeModeFuelMultiplier = 2.5;

        @Config.Comment({
                "Artificial sanctuary fuel capacity",
                "人造圣所燃料容量"
        })
        @Config.RangeInt(min = 1000, max = 1000000)
        public int artificialFuelCapacity = 2000;

        @Config.Comment({
                "Fuel usage multiplier for FARM mode (lowest cost, allows elites)",
                "狩猎模式燃料消耗倍率（最低消耗，允许精英生成）"
        })
        @Config.RangeDouble(min = 0.1, max = 10.0)
        public double farmModeFuelMultiplier = 0.3;

        @Config.Comment({
                "Fuel usage multiplier for EASE mode (medium cost, reduces difficulty)",
                "压制模式燃料消耗倍率（中等消耗，降低难度）"
        })
        @Config.RangeDouble(min = 0.1, max = 10.0)
        public double easeModeFuelMultiplier = 1.2;

        @Config.Comment({
                "Fuel usage multiplier per tier level (higher tier = more fuel)",
                "每等级额外燃料消耗倍率（等级越高消耗越重）",
                "Formula: modeMultiplier × (1 + (tier-1) × tierFuelMultiplier)",
                "公式: 模式倍率 × (1 + (等级-1) × 此值)",
                "Example: tier 5 with 0.25 = modeMultiplier × 2.0"
        })
        @Config.RangeDouble(min = 0.0, max = 2.0)
        public double tierFuelMultiplier = 0.3;

        @Config.Comment({
                "Difficulty reduction multiplier for EASE mode (0.5 = 50% difficulty)",
                "压制模式的难度减少倍率（0.5 = 50%难度）"
        })
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double easeModeDifficultyMultiplier = 0.5;

        // ==================== 熵能燃料值 ====================

        @Config.Comment({
                "Entropy Shard fuel value (dropped by T1-T4 elites)",
                "熵能碎片燃料值（T1-T4精英掉落）"
        })
        @Config.RangeInt(min = 1, max = 1000)
        public int entropyShardFuel = 10;

        @Config.Comment({
                "Entropy Crystal fuel value (dropped by T5-T7 elites)",
                "熵能结晶燃料值（T5-T7精英掉落）"
        })
        @Config.RangeInt(min = 1, max = 5000)
        public int entropyCrystalFuel = 50;

        @Config.Comment({
                "Entropy Core fuel value (dropped by T8-T10 elites)",
                "熵能核心燃料值（T8-T10精英掉落）"
        })
        @Config.RangeInt(min = 1, max = 20000)
        public int entropyCoreFuel = 200;

        // ==================== 维度黑名单 ====================

        @Config.Comment({
                "Dimension blacklist - Sanctuaries will NEVER generate in these dimensions",
                "维度黑名单 - 圣所永远不会在这些维度生成",
                "This takes priority over allowedDimensions",
                "此设置优先于 allowedDimensions"
        })
        public int[] dimensionBlacklist = new int[] { 1 }; // 默认禁止末地

        // ==================== 合成限制 ====================

        @Config.Comment({
                "Enable sanctuary crafting restrictions for baubles",
                "启用圣所饰品合成限制"
        })
        public boolean enableCraftingRestrictions = true;

        @Config.Comment({
                "Distance from sanctuary altar required for crafting special items (blocks)",
                "合成特殊物品所需的圣所祭坛距离（格）"
        })
        @Config.RangeInt(min = 8, max = 64)
        public int craftingProximity = 20;

        // ==================== 附魔限制 ====================

        @Config.Comment({
                "Remove Adversity enchantments from enchanting table when NOT near sanctuary",
                "当玩家不在圣所附近时，从附魔台移除 Adversity 附魔"
        })
        public boolean removeEnchantsFromTable = true;

        @Config.Comment({
                "Remove Adversity enchantments from villager trades",
                "从村民交易移除 Adversity 附魔"
        })
        public boolean removeEnchantsFromVillagers = true;

        @Config.Comment({
                "Skip CRT stage gating for Adversity enchantments (they use sanctuary proximity instead)",
                "跳過 CRT 階段門控對 Adversity 附魔的影響（它們由聖所系統獨立管理）"
        })
        public boolean skipAdversityEnchantmentGating = true;

        // ==================== Tier 限制 ====================

        @Config.Comment({
                "Tier-based crafting restrictions for baubles",
                "饰品按 Tier 等级限制合成",
                "Format: item_name:required_tier (e.g., guardian_heart:3)",
                "格式: item_name:required_tier（例如 guardian_heart:3）"
        })
        public String[] baublesTierRequirements = new String[] {
                "guardian_heart:3",
                "spatial_anchor:2",
                "soul_chain:2",
                "enchant_guardian:2",
                "flame_ward:1",
                "frost_ward:1",
                "corrosion_bane:1",
                "clarity_lens:1",
                "courage_charm:1",
                "anchor_stone:1"
        };
    }

    @Config.Comment({
            "Bauble Aura Settings",
            "饰品光环设置"
    })
    public static final AuraSettings auraSettings = new AuraSettings();

    public static class AuraSettings {
        @Config.Comment({
                "Enable bauble suppression aura",
                "启用饰品压制光环"
        })
        public boolean enableAura = true;

        @Config.Comment({
                "Aura radius in blocks",
                "光环半径（格）"
        })
        @Config.RangeInt(min = 4, max = 64)
        public int auraRadius = 16;

        @Config.Comment({
                "Aura check interval in ticks",
                "光环检查间隔（tick）"
        })
        @Config.RangeInt(min = 1, max = 100)
        public int auraInterval = 10;
    }

    // ==================== 运行时缓存 ====================

    private static Set<ResourceLocation> whitelistCache = new HashSet<>();
    private static Set<ResourceLocation> blacklistCache = new HashSet<>();
    private static Set<ResourceLocation> disabledAffixCache = new HashSet<>();
    private static Map<ResourceLocation, Integer> forcedTierCache = new java.util.HashMap<>();
    private static Set<ResourceLocation> eliteBlacklistCache = new HashSet<>();
    private static Map<ResourceLocation, Set<ResourceLocation>> affixEntityBlacklistCache = new java.util.HashMap<>();
    private static Map<ResourceLocation, Set<ResourceLocation>> forcedAffixesForEntitiesCache = new java.util.HashMap<>();
    private static boolean cacheInitialized = false;
    private static final Map<Class<?>, Boolean> entityClassCache = new WeakHashMap<>();

    public static void refreshCache() {
        whitelistCache.clear();
        blacklistCache.clear();
        disabledAffixCache.clear();
        forcedTierCache.clear();
        eliteBlacklistCache.clear();
        affixEntityBlacklistCache.clear();
        forcedAffixesForEntitiesCache.clear();
        entityClassCache.clear();

        for (String entry : entityFilter.whitelist) {
            if (entry != null && !entry.isEmpty()) {
                whitelistCache.add(new ResourceLocation(entry.trim()));
            }
        }

        for (String entry : entityFilter.blacklist) {
            if (entry != null && !entry.isEmpty()) {
                blacklistCache.add(new ResourceLocation(entry.trim()));
            }
        }

        // 缓存禁用的词条
        for (String entry : affixSettings.disabledAffixes) {
            if (entry != null && !entry.isEmpty()) {
                disabledAffixCache.add(new ResourceLocation(entry.trim()));
            }
        }

        // 缓存强制等级实体
        for (String entry : affixSettings.forcedTierEntities) {
            if (entry != null && !entry.isEmpty() && entry.contains("|")) {
                String[] parts = entry.split("\\|", 2);
                if (parts.length == 2) {
                    try {
                        ResourceLocation entityId = new ResourceLocation(parts[0].trim());
                        int tier = Integer.parseInt(parts[1].trim());
                        tier = Math.max(1, Math.min(10, tier)); // Clamp 1-10
                        forcedTierCache.put(entityId, tier);
                    } catch (NumberFormatException e) {
                        // Skip invalid entries
                    }
                }
            }
        }

        // 缓存精英黑名单
        for (String entry : affixSettings.eliteBlacklistEntities) {
            if (entry != null && !entry.isEmpty()) {
                eliteBlacklistCache.add(new ResourceLocation(entry.trim()));
            }
        }

        // 缓存词条-实体黑名单
        for (String entry : affixSettings.affixEntityBlacklist) {
            if (entry != null && !entry.isEmpty() && entry.contains("|")) {
                String[] parts = entry.split("\\|", 2);
                if (parts.length == 2) {
                    ResourceLocation affixId = new ResourceLocation(parts[0].trim());
                    ResourceLocation entityId = new ResourceLocation(parts[1].trim());
                    affixEntityBlacklistCache.computeIfAbsent(affixId, k -> new HashSet<>()).add(entityId);
                }
            }
        }

        // 缓存强制词条-实体映射
        for (String entry : affixSettings.forcedAffixesForEntities) {
            if (entry != null && !entry.isEmpty() && entry.contains("|")) {
                String[] parts = entry.split("\\|", 2);
                if (parts.length == 2) {
                    ResourceLocation entityId = new ResourceLocation(parts[0].trim());
                    ResourceLocation affixId = new ResourceLocation(parts[1].trim());
                    forcedAffixesForEntitiesCache.computeIfAbsent(entityId, k -> new HashSet<>()).add(affixId);
                }
            }
        }

        cacheInitialized = true;
        Adversity.LOGGER.info("Config cache refreshed: {} whitelist, {} blacklist, {} disabled affixes, {} forced tier, {} elite blacklist",
            whitelistCache.size(), blacklistCache.size(), disabledAffixCache.size(),
            forcedTierCache.size(), eliteBlacklistCache.size());
    }

    public static boolean shouldProcess(EntityLiving entity) {
        if (!cacheInitialized) {
            refreshCache();
        }

        Class<?> entityClass = entity.getClass();
        Boolean cached = entityClassCache.get(entityClass);
        if (cached != null) {
            return cached;
        }

        boolean result = shouldProcessInternal(entity);
        entityClassCache.put(entityClass, result);
        return result;
    }

    private static boolean shouldProcessInternal(EntityLiving entity) {
        ResourceLocation entityId = EntityList.getKey(entity);
        if (entityId == null) {
            return false;
        }

        if (blacklistCache.contains(entityId)) {
            return false;
        }

        if (whitelistCache.contains(entityId)) {
            return true;
        }

        if (entityFilter.whitelistOnly) {
            return false;
        }

        return entity instanceof IMob;
    }

    // ==================== 词条配置检查 ====================

    /**
     * 检查词条是否被禁用
     */
    public static boolean isAffixDisabled(ResourceLocation affixId) {
        if (!cacheInitialized) {
            refreshCache();
        }
        return disabledAffixCache.contains(affixId);
    }

    /**
     * 获取实体的强制等级，返回-1表示无强制等级
     */
    public static int getForcedTier(EntityLiving entity) {
        if (!cacheInitialized) {
            refreshCache();
        }
        ResourceLocation entityId = EntityList.getKey(entity);
        if (entityId == null) {
            return -1;
        }
        return forcedTierCache.getOrDefault(entityId, -1);
    }

    /**
     * 检查实体是否在精英黑名单中（永远不会成为精英）
     */
    public static boolean isEliteBlacklisted(EntityLiving entity) {
        if (!cacheInitialized) {
            refreshCache();
        }
        ResourceLocation entityId = EntityList.getKey(entity);
        return entityId != null && eliteBlacklistCache.contains(entityId);
    }

    /**
     * 检查特定词条是否被特定实体屏蔽
     */
    public static boolean isAffixBlockedForEntity(ResourceLocation affixId, EntityLiving entity) {
        if (!cacheInitialized) {
            refreshCache();
        }
        Set<ResourceLocation> blockedEntities = affixEntityBlacklistCache.get(affixId);
        if (blockedEntities == null || blockedEntities.isEmpty()) {
            return false;
        }
        ResourceLocation entityId = EntityList.getKey(entity);
        return entityId != null && blockedEntities.contains(entityId);
    }

    /**
     * 获取特定实体的强制词条列表
     */
    public static Set<ResourceLocation> getForcedAffixesForEntity(EntityLiving entity) {
        if (!cacheInitialized) {
            refreshCache();
        }
        ResourceLocation entityId = EntityList.getKey(entity);
        if (entityId == null) {
            return java.util.Collections.emptySet();
        }
        Set<ResourceLocation> forced = forcedAffixesForEntitiesCache.get(entityId);
        return forced != null ? forced : java.util.Collections.emptySet();
    }

    // ==================== 配置同步 ====================

    @Mod.EventBusSubscriber(modid = Adversity.MODID)
    public static class ConfigSyncHandler {

        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(Adversity.MODID)) {
                ConfigManager.sync(Adversity.MODID, Config.Type.INSTANCE);
                refreshCache();
            }
        }
    }
}
