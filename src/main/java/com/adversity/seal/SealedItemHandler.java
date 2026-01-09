package com.adversity.seal;

import com.adversity.Adversity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 封印物品事件处理器
 * 处理封印物品的效果无效化和自动解封
 */
@Mod.EventBusSubscriber(modid = Adversity.MODID)
public class SealedItemHandler {

    /** 检查间隔（ticks） */
    private static final int CHECK_INTERVAL = 20;  // 每秒检查一次

    /**
     * 定期检查并解除过期封印
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;
        if (event.player.ticksExisted % CHECK_INTERVAL != 0) return;

        EntityPlayer player = event.player;
        long currentTime = player.world.getTotalWorldTime();
        boolean anyUnsealed = false;

        // 检查所有装备槽
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            ItemStack stack = player.getItemStackFromSlot(slot);
            if (SealedItemManager.isSealed(stack)) {
                if (currentTime >= SealedItemManager.getSealEndTime(stack)) {
                    // 解除封印
                    SealedItemManager.unsealItem(stack);
                    anyUnsealed = true;
                }
            }
        }

        // 检查主背包
        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            ItemStack stack = player.inventory.mainInventory.get(i);
            if (SealedItemManager.isSealed(stack)) {
                if (currentTime >= SealedItemManager.getSealEndTime(stack)) {
                    SealedItemManager.unsealItem(stack);
                    anyUnsealed = true;
                }
            }
        }

        // 通知玩家
        if (anyUnsealed) {
            player.sendMessage(new TextComponentTranslation("adversity.seal.expired"));
        }
    }

    /**
     * 当装备变更时检查封印状态
     * 封印装备不提供属性加成
     */
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        ItemStack newItem = event.getTo();

        // 如果新装备的物品是封印状态，发送警告
        if (SealedItemManager.isSealed(newItem)) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            player.sendMessage(new TextComponentTranslation("adversity.seal.equipped_sealed"));
        }
    }

    /**
     * 获取物品的有效属性修改器（排除封印物品）
     * 用于其他系统检查物品是否应该生效
     */
    public static boolean shouldItemProvideStats(ItemStack stack) {
        return !stack.isEmpty() && !SealedItemManager.isSealed(stack);
    }
}
