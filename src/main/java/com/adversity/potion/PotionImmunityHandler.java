package com.adversity.potion;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 負面藥水免疫攔截器
 *
 * 使用 Forge PotionEvent.PotionApplicableEvent 事件驅動攔截，
 * 零 tick 開銷。當玩家持有 NEGATIVE_IMMUNITY potion 時阻止所有負面效果施加。
 */
public class PotionImmunityHandler {

    @SubscribeEvent
    public void onPotionApplicable(PotionEvent.PotionApplicableEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        PotionEffect incoming = event.getPotionEffect();

        // 玩家持有免疫 buff 且即將施加的是負面效果 → 阻止
        if (player.isPotionActive(PotionNegativeImmunity.INSTANCE)
                && incoming.getPotion().isBadEffect()) {
            event.setResult(Event.Result.DENY);
        }
    }
}
