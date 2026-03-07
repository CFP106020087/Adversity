package com.adversity.item;

import com.adversity.Adversity;
import com.adversity.item.bauble.*;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 物品注册管理器
 * 使用 Forge 的 ObjectHolder 和 RegistryEvent 进行注册
 */
@Mod.EventBusSubscriber(modid = Adversity.MODID)
public class ItemRegistry {

    // ==================== 核心物品 ====================

    @GameRegistry.ObjectHolder(Adversity.MODID + ":calm_dust")
    public static final Item CALM_DUST = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":difficulty_detector")
    public static final Item DIFFICULTY_DETECTOR = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":sealed_token")
    public static final Item SEALED_TOKEN = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":affix_guide")
    public static final Item AFFIX_GUIDE = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":affix_lens")
    public static final Item AFFIX_LENS = null;

    // ==================== 赎罪道具 ====================

    @GameRegistry.ObjectHolder(Adversity.MODID + ":redemption_tear")
    public static final Item REDEMPTION_TEAR = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":redemption_heart")
    public static final Item REDEMPTION_HEART = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":redemption_key")
    public static final Item REDEMPTION_KEY = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":entropy")
    public static final Item ENTROPY = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":sanctuary_compass")
    public static final Item SANCTUARY_COMPASS = null;

    // ==================== 守护饰品 ====================

    @GameRegistry.ObjectHolder(Adversity.MODID + ":soul_chain")
    public static final Item SOUL_CHAIN = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":spatial_anchor")
    public static final Item SPATIAL_ANCHOR = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":flame_ward")
    public static final Item FLAME_WARD = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":frost_ward")
    public static final Item FROST_WARD = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":corrosion_bane")
    public static final Item CORROSION_BANE = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":guardian_heart")
    public static final Item GUARDIAN_HEART = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":anchor_stone")
    public static final Item ANCHOR_STONE = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":clarity_lens")
    public static final Item CLARITY_LENS = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":courage_charm")
    public static final Item COURAGE_CHARM = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":enchant_guardian")
    public static final Item ENCHANT_GUARDIAN = null;

    // ==================== 新增守护饰品 ====================

    @GameRegistry.ObjectHolder(Adversity.MODID + ":void_heart")
    public static final Item VOID_HEART = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":bloodrage_emblem")
    public static final Item BLOODRAGE_EMBLEM = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":temporal_watch")
    public static final Item TEMPORAL_WATCH = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":phantom_cloak")
    public static final Item PHANTOM_CLOAK = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":thunder_ring")
    public static final Item THUNDER_RING = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":aegis_medal")
    public static final Item AEGIS_MEDAL = null;

    // ==================== 多维反制饰品 ====================

    @GameRegistry.ObjectHolder(Adversity.MODID + ":purify_badge")
    public static final Item PURIFY_BADGE = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":armor_pierce")
    public static final Item ARMOR_PIERCE = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":anchor_heart")
    public static final Item ANCHOR_HEART = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":barrier_ward")
    public static final Item BARRIER_WARD = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":wither_mark")
    public static final Item WITHER_MARK = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":true_crystal")
    public static final Item TRUE_CRYSTAL = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":guardian_soul")
    public static final Item GUARDIAN_SOUL = null;

    // ==================== 新增反制饰品 ====================

    @GameRegistry.ObjectHolder(Adversity.MODID + ":balance_charm")
    public static final Item BALANCE_CHARM = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":immunity_badge")
    public static final Item IMMUNITY_BADGE = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":memory_crystal")
    public static final Item MEMORY_CRYSTAL = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":soul_seal_dust")
    public static final Item SOUL_SEAL_DUST = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":light_amulet")
    public static final Item LIGHT_AMULET = null;

    // ==================== 材料物品 ====================

    @GameRegistry.ObjectHolder(Adversity.MODID + ":affix_essence")
    public static final Item AFFIX_ESSENCE = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":affix_crystal")
    public static final Item AFFIX_CRYSTAL = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":void_shard")
    public static final Item VOID_SHARD = null;

    // ==================== 工具物品 ====================

    @GameRegistry.ObjectHolder(Adversity.MODID + ":enchant_gating_tool")
    public static final Item ENCHANT_GATING_TOOL = null;

    /**
     * 物品注册事件 - 在游戏启动时自动调用
     */
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        Adversity.LOGGER.info("Registering Adversity items...");

        event.getRegistry().registerAll(
                // 核心物品
                new ItemCalmDust(),
                new ItemDifficultyDetector(),
                new ItemSealedToken(),
                new ItemAffixGuide(),
                new ItemAffixLens(),

                // 赎罪道具 - 用于减少永久诅咒
                new ItemRedemptionTear(),
                new ItemRedemptionHeart(),
                new ItemRedemptionKey(),

                // 熵能燃料
                new ItemEntropy(),

                // 守护饰品 - 装备保护
                new ItemSoulChain(), // 防枷锁
                new ItemSpatialAnchor(), // 防褫夺
                new ItemEnchantGuardian(), // 防消魔

                // 守护饰品 - 元素保护
                new ItemFlameWard(), // 防火焰
                new ItemFrostWard(), // 防冰霜
                new ItemCorrosionBane(), // 防腐蚀

                // 守护饰品 - 诅咒保护
                new ItemGuardianHeart(), // 防永久诅咒

                // 守护饰品 - 战术
                new ItemAnchorStone(), // 防重力
                new ItemClarityLens(), // 防致盲
                new ItemCourageCharm(), // 防恐惧

                // 新增守护饰品 - 高级特化
                new ItemVoidHeart(), // 末地专精
                new ItemBloodrageEmblem(), // 吸血专精
                new ItemTemporalWatch(), // 时间专精
                new ItemPhantomCloak(), // 隐匿专精
                new ItemThunderRing(), // 雷电专精
                new ItemAegisMedal(), // 团队专精

                // 多维反制饰品 - 针对词条机制
                new ItemPurifyBadge(), // 叠层系统
                new ItemArmorPierce(), // 护甲/减伤
                new ItemAnchorHeart(), // 闪避/复活
                new ItemBarrierWard(), // 范围光环
                new ItemWitherMark(), // 吸血/回复
                new ItemTrueCrystal(), // 伤害转换
                new ItemGuardianSoul(), // 装备封印

                // 圣所指南针
                new ItemSanctuaryCompass(),

                // 新增反制饰品
                new ItemBalanceCharm(), // 逆转反制
                new ItemImmunityBadge(), // 破伤风反制
                new ItemMemoryCrystal(), // 遗忘反制
                new ItemSoulSealDust(), // 飞升反制
                new ItemLightAmulet(), // 强欲/贪婪反制

                // 材料物品
                new ItemAffixEssence(),
                new ItemAffixCrystal(),
                new ItemVoidShard(),

                // 工具物品
                new ItemEnchantGatingTool()
        );

        Adversity.LOGGER.info("Adversity items registered");
    }

    /**
     * 模型注册事件 - 客户端专用
     */
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        // 核心物品
        registerItemModel(CALM_DUST);
        registerItemModel(DIFFICULTY_DETECTOR);
        registerItemModel(SEALED_TOKEN);
        registerItemModel(AFFIX_GUIDE);

        // 赎罪道具
        registerItemModel(REDEMPTION_TEAR);
        registerItemModel(REDEMPTION_HEART);
        registerItemModel(REDEMPTION_KEY);

        // 熵能燃料 (多meta)
        registerItemModelWithMeta(ENTROPY, 0, "entropy_shard");
        registerItemModelWithMeta(ENTROPY, 1, "entropy_crystal");
        registerItemModelWithMeta(ENTROPY, 2, "entropy_core");

        // 守护饰品
        registerItemModel(SOUL_CHAIN);
        registerItemModel(SPATIAL_ANCHOR);
        registerItemModel(ENCHANT_GUARDIAN);
        registerItemModel(FLAME_WARD);
        registerItemModel(FROST_WARD);
        registerItemModel(CORROSION_BANE);
        registerItemModel(GUARDIAN_HEART);
        registerItemModel(ANCHOR_STONE);
        registerItemModel(CLARITY_LENS);
        registerItemModel(COURAGE_CHARM);
        registerItemModel(SANCTUARY_COMPASS);

        // 新增守护饰品
        registerItemModel(VOID_HEART);
        registerItemModel(BLOODRAGE_EMBLEM);
        registerItemModel(TEMPORAL_WATCH);
        registerItemModel(PHANTOM_CLOAK);
        registerItemModel(THUNDER_RING);
        registerItemModel(AEGIS_MEDAL);

        // 多维反制饰品
        registerItemModel(PURIFY_BADGE);
        registerItemModel(ARMOR_PIERCE);
        registerItemModel(ANCHOR_HEART);
        registerItemModel(BARRIER_WARD);
        registerItemModel(WITHER_MARK);
        registerItemModel(TRUE_CRYSTAL);
        registerItemModel(GUARDIAN_SOUL);

        // 材料物品
        registerItemModel(AFFIX_ESSENCE);
        registerItemModel(AFFIX_CRYSTAL);
        registerItemModel(VOID_SHARD);

        // 工具物品
        registerItemModel(ENCHANT_GATING_TOOL);

        // 新增反制饰品
        registerItemModel(AFFIX_LENS);
        registerItemModel(BALANCE_CHARM);
        registerItemModel(IMMUNITY_BADGE);
        registerItemModel(MEMORY_CRYSTAL);
        registerItemModel(SOUL_SEAL_DUST);
        registerItemModel(LIGHT_AMULET);
    }

    @SideOnly(Side.CLIENT)
    private static void registerItemModel(Item item) {
        if (item != null && item.getRegistryName() != null) {
            ModelLoader.setCustomModelResourceLocation(
                item,
                0,
                new ModelResourceLocation(item.getRegistryName(), "inventory")
            );
        }
    }

    @SideOnly(Side.CLIENT)
    private static void registerItemModelWithMeta(Item item, int meta, String variant) {
        if (item != null && item.getRegistryName() != null) {
            ModelLoader.setCustomModelResourceLocation(
                    item,
                    meta,
                    new ModelResourceLocation(
                            item.getRegistryName().getNamespace() + ":" + variant,
                            "inventory"));
        }
    }
}

