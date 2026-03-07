package com.adversity.potion;

import com.adversity.Adversity;
import net.minecraft.potion.Potion;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 藥水效果註冊
 */
@Mod.EventBusSubscriber(modid = Adversity.MODID)
public class PotionRegistry {

    @SubscribeEvent
    public static void registerPotions(RegistryEvent.Register<Potion> event) {
        event.getRegistry().register(PotionNegativeImmunity.INSTANCE);
        Adversity.LOGGER.info("[Adversity] Registered potions");
    }
}
