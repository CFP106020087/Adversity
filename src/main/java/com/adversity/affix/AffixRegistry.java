package com.adversity.affix;

import com.adversity.Adversity;
import com.adversity.affix.impl.*;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 词条注册表 - 管理所有已注册的词条
 */
public class AffixRegistry {

    private static final Map<ResourceLocation, IAffix> REGISTRY = new LinkedHashMap<>();
    private static final List<IAffix> AFFIXES_BY_TYPE_OFFENSIVE = new ArrayList<>();
    private static final List<IAffix> AFFIXES_BY_TYPE_DEFENSIVE = new ArrayList<>();
    private static final List<IAffix> AFFIXES_BY_TYPE_UTILITY = new ArrayList<>();
    private static final List<IAffix> AFFIXES_BY_TYPE_SPECIAL = new ArrayList<>();

    private static boolean initialized = false;

    /**
     * 初始化词条注册表
     */
    public static void init() {
        if (initialized) return;
        initialized = true;

        Adversity.LOGGER.info("Initializing Affix Registry");

        // 注册内置词条
        registerBuiltinAffixes();

        Adversity.LOGGER.info("Affix Registry initialized with {} affixes", REGISTRY.size());
    }

    /**
     * 注册词条
     */
    public static void register(IAffix affix) {
        if (affix == null) {
            throw new IllegalArgumentException("Cannot register null affix");
        }

        ResourceLocation id = affix.getId();
        if (REGISTRY.containsKey(id)) {
            Adversity.LOGGER.warn("Affix with id {} is already registered, skipping", id);
            return;
        }

        REGISTRY.put(id, affix);

        // 按类型分类
        switch (affix.getType()) {
            case OFFENSIVE:
                AFFIXES_BY_TYPE_OFFENSIVE.add(affix);
                break;
            case DEFENSIVE:
                AFFIXES_BY_TYPE_DEFENSIVE.add(affix);
                break;
            case UTILITY:
                AFFIXES_BY_TYPE_UTILITY.add(affix);
                break;
            case SPECIAL:
                AFFIXES_BY_TYPE_SPECIAL.add(affix);
                break;
        }

        Adversity.LOGGER.debug("Registered affix: {}", id);
    }

    /**
     * 根据 ID 获取词条
     */
    @Nullable
    public static IAffix getAffix(ResourceLocation id) {
        return REGISTRY.get(id);
    }

    /**
     * 根据字符串 ID 获取词条
     */
    @Nullable
    public static IAffix getAffix(String id) {
        return getAffix(new ResourceLocation(id));
    }

    /**
     * 获取所有已注册的词条
     */
    public static Collection<IAffix> getAllAffixes() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    /**
     * 获取指定类型的所有词条
     */
    public static List<IAffix> getAffixesByType(AffixType type) {
        switch (type) {
            case OFFENSIVE:
                return Collections.unmodifiableList(AFFIXES_BY_TYPE_OFFENSIVE);
            case DEFENSIVE:
                return Collections.unmodifiableList(AFFIXES_BY_TYPE_DEFENSIVE);
            case UTILITY:
                return Collections.unmodifiableList(AFFIXES_BY_TYPE_UTILITY);
            case SPECIAL:
                return Collections.unmodifiableList(AFFIXES_BY_TYPE_SPECIAL);
            default:
                return Collections.emptyList();
        }
    }

    /**
     * 获取已注册词条数量
     */
    public static int getAffixCount() {
        return REGISTRY.size();
    }

    /**
     * 检查词条是否已注册
     */
    public static boolean isRegistered(ResourceLocation id) {
        return REGISTRY.containsKey(id);
    }

    /**
     * 获取所有词条 ID
     */
    public static Set<ResourceLocation> getAffixIds() {
        return Collections.unmodifiableSet(REGISTRY.keySet());
    }

    /**
     * 注册内置词条
     */
    private static void registerBuiltinAffixes() {
        // ========== 攻击型词条 (OFFENSIVE) ==========
        register(new FieryAffix());           // 烈焰 - 攻击时点燃目标 (难度0+)
        register(new FrostyAffix());          // 冰霜 - 攻击时减速目标 (难度0+)
        register(new WitheringAffix());       // 凋零 - 攻击时施加凋零 (难度5+)
        register(new ReaperAffix());          // 死神 - 玩家死亡越少怪物越强 (难度3+)
        register(new BluntAffix());           // 钝击 - 护甲越高伤害越高 (难度2+)
        register(new GiantSlayerAffix());     // 巨人杀手 - 最大生命越高伤害越高 (难度3+)
        register(new ShieldBreakerAffix());   // 破盾 - 吸收值越高伤害越高 (难度4+)
        register(new BloodthirstAffix());     // 渴血 - 玩家血越低伤害越高 (难度2+)
        register(new AvariceAffix());         // 强欲 - 背包物品越多伤害越高 (难度2+)
        register(new TetanusAffix());         // 破伤风 - 5次命中扣半血 (难度3+)
        register(new UncleanAffix());         // 不洁 - 随机负面效果 (难度2+)

        // ========== 防御型词条 (DEFENSIVE) ==========
        register(new VampiricAffix());        // 吸血 - 攻击回血 (难度3+)
        register(new ReflectiveAffix());      // 反射 - 反弹伤害 (难度4+)
        register(new RegeneratingAffix());    // 再生 - 持续回血 (难度3+)
        register(new HeroAffix());            // 英雄 - 伤害开平方根 (难度6+)
        register(new DivineAffix());          // 神明 - 伤害开立方根 (难度8+)
        register(new OuterGodAffix());        // 外神 - 伤害开四次根 (难度10+)
        register(new ReversalAffix());        // 反转 - 伤害反转机制 (难度5+)

        // ========== 功能型词条 (UTILITY) ==========
        register(new HasteAffix());           // 迅捷 - 移动速度加成 (难度1+)
        register(new BlindingAffix());        // 致盲 - 攻击时致盲 (难度2+)
        register(new GravityAffix());         // 引力 - 拉动附近玩家 (难度4+)
        register(new OblivionAffix());        // 遗忘 - 扣除经验值 (难度2+)
        register(new PurifierAffix());        // 净化者 - 夺取正面药水 (难度4+)
        register(new DisenchantAffix());      // 解咒 - 封印装备附魔 (难度5+)
        register(new ShackleAffix());         // 枷锁 - 封印装备槽 (难度5+)
        register(new DivestAffix());          // 褫夺 - 封印饰品槽 (难度6+)

        // ========== 特殊型词条 (SPECIAL) ==========
        register(new TeleportingAffix());     // 传送 - 受伤时传送 (难度5+)
        register(new SplittingAffix());       // 分裂 - 死亡时分裂 (难度6+)
        register(new HorrorAffix());          // 恐惧 - 强制后退+恐惧光环 (难度4+)
        register(new AscensionAffix());       // 飞升 - 死亡时升阶词条 (难度6+)
        register(new AnnihilateAffix());      // 湮灭 - 虚空临时删除物品 (难度7+)
    }
}
