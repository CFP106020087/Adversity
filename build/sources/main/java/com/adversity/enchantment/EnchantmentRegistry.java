package com.adversity.enchantment;

import com.adversity.Adversity;
import net.minecraft.enchantment.Enchantment;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * 附魔注册表
 */
@Mod.EventBusSubscriber(modid = Adversity.MODID)
public class EnchantmentRegistry {

    @GameRegistry.ObjectHolder(Adversity.MODID + ":soulbound")
    public static final Enchantment SOULBOUND = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":breaker")
    public static final Enchantment BREAKER = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":entropy_affinity")
    public static final Enchantment ENTROPY_AFFINITY = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":purifying_touch")
    public static final Enchantment PURIFYING_TOUCH = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":resolute_will")
    public static final Enchantment RESOLUTE_WILL = null;

    @SubscribeEvent
    public static void registerEnchantments(RegistryEvent.Register<Enchantment> event) {
        Adversity.LOGGER.info("Registering Adversity enchantments...");

        event.getRegistry().registerAll(
            new EnchantmentSoulbound(),
            new EnchantmentBreaker(),
            new EnchantmentEntropyAffinity(),
            new EnchantmentPurifyingTouch(),
            new EnchantmentResoluteWill()
        );

        Adversity.LOGGER.info("Adversity enchantments registered");
    }
}
