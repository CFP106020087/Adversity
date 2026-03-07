package com.adversity.proxy;

import com.adversity.Adversity;
import com.adversity.affix.AffixRegistry;
import com.adversity.capability.CapabilityHandler;
import com.adversity.affix.impl.AffixEffectHandler;
import com.adversity.debuff.DebuffEventHandler;
import com.adversity.difficulty.DifficultyManager;
import com.adversity.event.MobEventHandler;
import com.adversity.network.PacketHandler;
import com.adversity.progression.ProgressionEventHandler;
import com.adversity.sanctuary.ritual.RitualManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        // 注册 Capability
        CapabilityHandler.register();

        // 注册进度系统 Capability
        ProgressionEventHandler.register();

        // 初始化词条注册表
        AffixRegistry.init();

        // 初始化仪式系统
        RitualManager.init();

        // 初始化网络包处理
        PacketHandler.init();

        Adversity.LOGGER.info("Capability, Affix Registry, Progression and Network initialized");
    }

    public void init(FMLInitializationEvent event) {
        // 注册事件处理器
        MinecraftForge.EVENT_BUS.register(new MobEventHandler());
        MinecraftForge.EVENT_BUS.register(new CapabilityHandler());

        // 注册Debuff处理器
        MinecraftForge.EVENT_BUS.register(new DebuffEventHandler());

        // 注册词条效果处理器（用于解咒恢复等）
        MinecraftForge.EVENT_BUS.register(new AffixEffectHandler());

        // 注册饰品反制光环 (Sanctuary 2.0)
        MinecraftForge.EVENT_BUS.register(new com.adversity.bauble.BaubleAuraHandler());

        // 注册附魔反制逻辑 (Sanctuary 2.0)
        MinecraftForge.EVENT_BUS.register(new com.adversity.enchantment.EnchantmentHandler());

        // 初始化难度管理器
        DifficultyManager.init();

        // 注册圣所世界生成器
        net.minecraftforge.fml.common.registry.GameRegistry.registerWorldGenerator(
                new com.adversity.sanctuary.SanctuaryGenerator(), 100);

        // 注册圣所保护处理器 (使祭坛和周围方块不可破坏)
        MinecraftForge.EVENT_BUS.register(new com.adversity.sanctuary.SanctuaryProtectionHandler());

        // 注册藥水免疫攔截器
        MinecraftForge.EVENT_BUS.register(new com.adversity.potion.PotionImmunityHandler());

        // 注册內建特殊儀式效果（獨立於 enableBuiltinRituals）
        if (com.adversity.config.AdversityConfig.sanctuarySettings.enableBuiltinSpecialRituals) {
            com.adversity.sanctuary.ritual.RitualEffectRegistry.register("purge_curse",
                    new com.adversity.sanctuary.ritual.effect.PurgeCurseEffect());
            com.adversity.sanctuary.ritual.RitualEffectRegistry.register("awakening",
                    new com.adversity.sanctuary.ritual.effect.AwakeningEffect());
            Adversity.LOGGER.info("Built-in special ritual effects registered (purge_curse, awakening)");
        } else {
            Adversity.LOGGER.info("Built-in special ritual effects disabled by config");
        }

        Adversity.LOGGER.info("Event handlers registered");
    }

    public void postInit(FMLPostInitializationEvent event) {
        Adversity.LOGGER.info("Adversity loaded successfully");
    }
}
