package com.adversity.affix.impl;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 词条效果事件处理器
 * 处理净化者和解咒词条的全局效果
 */
public class AffixEffectHandler {

    /**
     * 处理正面药水效果阻止（净化者词条）
     */
    @SubscribeEvent
    public void onPotionApplicable(PotionEvent.PotionApplicableEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        Potion potion = event.getPotionEffect().getPotion();

        // 检查是否应该阻止正面药水
        if (PurifierAffix.shouldBlockPositivePotion(player, potion)) {
            event.setResult(Event.Result.DENY);
        }
    }

    /**
     * 每tick检查并恢复被封印的附魔（解咒词条）
     */
    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        // 每20tick检查一次（1秒）
        if (player.ticksExisted % 20 != 0) {
            return;
        }

        long currentTime = player.world.getTotalWorldTime();

        // 检查主手
        ItemStack mainHand = player.getHeldItemMainhand();
        DisenchantAffix.checkAndRestoreSilence(mainHand, currentTime);

        // 检查副手
        ItemStack offHand = player.getHeldItemOffhand();
        DisenchantAffix.checkAndRestoreSilence(offHand, currentTime);

        // 检查盔甲
        for (ItemStack armor : player.getArmorInventoryList()) {
            DisenchantAffix.checkAndRestoreSilence(armor, currentTime);
        }
    }
}
