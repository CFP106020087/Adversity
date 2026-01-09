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
import net.minecraftforge.fml.common.Loader;
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

    /** Baubles是否可用 */
    private static Boolean baublesLoaded = null;

    /**
     * 检查Baubles是否已加载
     */
    private static boolean isBaublesLoaded() {
        if (baublesLoaded == null) {
            baublesLoaded = Loader.isModLoaded("baubles");
        }
        return baublesLoaded;
    }

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
                    // 重新设置到槽位以触发同步
                    player.setItemStackToSlot(slot, stack);
                    anyUnsealed = true;
                    Adversity.LOGGER.debug("Unsealed equipment in slot {}", slot);
                }
            }
        }

        // 检查主背包
        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            ItemStack stack = player.inventory.mainInventory.get(i);
            if (SealedItemManager.isSealed(stack)) {
                if (currentTime >= SealedItemManager.getSealEndTime(stack)) {
                    SealedItemManager.unsealItem(stack);
                    // 重新设置到槽位以触发同步
                    player.inventory.mainInventory.set(i, stack);
                    anyUnsealed = true;
                    Adversity.LOGGER.debug("Unsealed item in inventory slot {}", i);
                }
            }
        }

        // 强制同步物品栏到客户端
        if (anyUnsealed) {
            player.inventory.markDirty();
            player.inventoryContainer.detectAndSendChanges();
        }

        // 检查Baubles饰品栏
        if (isBaublesLoaded()) {
            anyUnsealed |= checkBaublesInventory(player, currentTime);
        }

        // 检查并返还虚空存储的物品（湮灭词条）
        boolean anyReturned = checkVoidStorage(player, currentTime);

        // 通知玩家
        if (anyUnsealed) {
            player.sendMessage(new TextComponentTranslation("adversity.seal.expired"));
        }
        if (anyReturned) {
            player.sendMessage(new TextComponentTranslation("adversity.void.returned"));
        }
    }

    /**
     * 检查并返还虚空存储中的物品
     */
    private static boolean checkVoidStorage(EntityPlayer player, long currentTime) {
        SealedItemManager manager = SealedItemManager.get(player.world);
        java.util.List<SealedItemManager.VoidStoredItem> returnItems = manager.getReturnableItems(player, currentTime);

        if (returnItems.isEmpty()) {
            return false;
        }

        for (SealedItemManager.VoidStoredItem item : returnItems) {
            returnItemToPlayer(player, item);
            Adversity.LOGGER.debug("Returned void item {} to player {}",
                item.stack.getDisplayName(), player.getName());
        }

        // 强制同步物品栏到客户端
        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();

        return true;
    }

    /**
     * 将物品返还给玩家
     */
    private static void returnItemToPlayer(EntityPlayer player, SealedItemManager.VoidStoredItem item) {
        // 尝试放回原槽位
        if (item.slotIndex >= 0 && item.slotIndex < player.inventory.mainInventory.size()) {
            if (player.inventory.mainInventory.get(item.slotIndex).isEmpty()) {
                player.inventory.mainInventory.set(item.slotIndex, item.stack);
                playReturnEffects(player);
                return;
            }
        }

        // 尝试放入背包其他位置
        if (player.inventory.addItemStackToInventory(item.stack)) {
            playReturnEffects(player);
            return;
        }

        // 背包已满，掉落在地上
        player.dropItem(item.stack, false);
        playReturnEffects(player);
    }

    /**
     * 播放物品返还效果
     */
    private static void playReturnEffects(EntityPlayer player) {
        if (player.world.isRemote) return;

        player.world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            net.minecraft.init.SoundEvents.ENTITY_ITEM_PICKUP,
            net.minecraft.util.SoundCategory.PLAYERS,
            0.5f,
            1.0f
        );
    }

    /**
     * 检查Baubles饰品栏中的封印物品
     */
    private static boolean checkBaublesInventory(EntityPlayer player, long currentTime) {
        try {
            // 使用反射访问Baubles API
            Class<?> baublesApiClass = Class.forName("baubles.api.BaublesApi");
            Object handler = baublesApiClass.getMethod("getBaublesHandler", EntityPlayer.class)
                .invoke(null, player);

            if (handler == null) {
                return false;
            }

            int slots = (int) handler.getClass().getMethod("getSlots").invoke(handler);
            boolean anyUnsealed = false;

            for (int i = 0; i < slots; i++) {
                ItemStack stack = (ItemStack) handler.getClass()
                    .getMethod("getStackInSlot", int.class).invoke(handler, i);

                if (SealedItemManager.isSealed(stack)) {
                    if (currentTime >= SealedItemManager.getSealEndTime(stack)) {
                        // 解除封印
                        SealedItemManager.unsealItem(stack);
                        // 需要将物品放回槽位以更新
                        handler.getClass().getMethod("setStackInSlot", int.class, ItemStack.class)
                            .invoke(handler, i, stack);
                        anyUnsealed = true;
                        Adversity.LOGGER.debug("Unsealed bauble in slot {}", i);
                    }
                }
            }

            return anyUnsealed;
        } catch (Exception e) {
            Adversity.LOGGER.debug("Failed to check Baubles inventory: {}", e.getMessage());
            return false;
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
