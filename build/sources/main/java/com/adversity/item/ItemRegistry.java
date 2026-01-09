package com.adversity.item;

import com.adversity.Adversity;
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

    // 使用 ObjectHolder 自动注入已注册的物品实例
    @GameRegistry.ObjectHolder(Adversity.MODID + ":affix_essence")
    public static final Item AFFIX_ESSENCE = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":calm_dust")
    public static final Item CALM_DUST = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":difficulty_detector")
    public static final Item DIFFICULTY_DETECTOR = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":affix_crystal")
    public static final Item AFFIX_CRYSTAL = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":void_shard")
    public static final Item VOID_SHARD = null;

    /**
     * 物品注册事件 - 在游戏启动时自动调用
     */
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        Adversity.LOGGER.info("Registering Adversity items...");

        event.getRegistry().registerAll(
            // 基础材料 - 词条残渣 (T1-T4掉落)
            new ItemAffixEssence(),

            // 中级材料 - 词条水晶 (T5-T7掉落)
            new ItemAffixCrystal(),

            // 高级材料 - 虚空碎片 (T8-T10掉落)
            new ItemVoidShard(),

            // 功能物品 - 宁静之尘 (临时压制区域精英生成)
            new ItemCalmDust(),

            // 工具 - 难度探测器 (显示区域难度信息)
            new ItemDifficultyDetector()
        );

        Adversity.LOGGER.info("Adversity items registered");
    }

    /**
     * 模型注册事件 - 客户端专用
     */
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        registerItemModel(AFFIX_ESSENCE);
        registerItemModel(AFFIX_CRYSTAL);
        registerItemModel(VOID_SHARD);
        registerItemModel(CALM_DUST);
        registerItemModel(DIFFICULTY_DETECTOR);
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
}
