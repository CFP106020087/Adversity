package com.adversity.curse;

import com.adversity.Adversity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 黑棺封印背包事件處理器
 *
 * 職責分工：
 * - MixinSlot: 攔截 Slot 方法級操作 (isItemValid/canTakeStack/getSlotStackLimit)
 * - 本類: 攔截 Forge 事件級操作 (拾取物品、Q丟出)
 * - SealedSlotOverlayRenderer: 客戶端視覺 overlay + 滑鼠點擊阻止
 */
@Mod.EventBusSubscriber(modid = Adversity.MODID)
public class CurseInventoryHandler {

    /**
     * 阻止拾取物品到已满的背包（考虑封印槽位）
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (event.getEntityPlayer().world.isRemote)
            return;

        EntityPlayer player = event.getEntityPlayer();
        PermanentCurseManager manager = PermanentCurseManager.get(player.world);

        int sealedCount = manager.getBlackCoffinSealed(player);
        if (sealedCount <= 0) return;

        // 计算可用槽位
        int availableSlots = countAvailableSlots(player, sealedCount);

        // 如果没有可用槽位，取消拾取
        if (availableSlots <= 0) {
            event.setCanceled(true);
        }
    }

    /**
     * 阻止從封印槽位用 Q 鍵丟出物品
     * ItemTossEvent 在物品從背包移除後觸發，
     * 取消後需要將物品放回背包
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemToss(ItemTossEvent event) {
        if (event.getPlayer().world.isRemote)
            return;

        EntityPlayer player = event.getPlayer();
        PermanentCurseManager manager = PermanentCurseManager.get(player.world);
        int sealedCount = manager.getBlackCoffinSealed(player);
        if (sealedCount <= 0) return;

        // 检查当前选中的快捷栏槽位是否被封印
        int selectedSlot = player.inventory.currentItem; // 0-8
        if (isSlotSealed(player, selectedSlot)) {
            // 取消丟出，把物品放回
            event.setCanceled(true);
            ItemStack tossed = event.getEntityItem().getItem();
            player.inventory.mainInventory.set(selectedSlot, tossed);
            if (player instanceof EntityPlayerMP) {
                ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
            }
        }
    }

    /**
     * 检查玩家背包槽位是否被封印
     * 先封主背包(9-35)，再封快捷栏(0-8)
     */
    public static boolean isSlotSealed(EntityPlayer player, int slotIndex) {
        if (player.world.isRemote) return false;

        PermanentCurseManager manager = PermanentCurseManager.get(player.world);
        int sealedCount = manager.getBlackCoffinSealed(player);
        if (sealedCount <= 0) return false;

        if (slotIndex >= 9 && slotIndex < 36) {
            int sealOrder = slotIndex - 9;
            return sealOrder < sealedCount;
        } else if (slotIndex >= 0 && slotIndex < 9) {
            if (sealedCount <= 27) return false;
            int hotbarSealed = sealedCount - 27;
            return slotIndex < hotbarSealed;
        }
        return false;
    }

    /**
     * 计算可用槽位数（排除封印槽位）
     */
    private static int countAvailableSlots(EntityPlayer player, int sealedCount) {
        int available = 0;

        int mainInvSealed = Math.min(sealedCount, 27);
        int hotbarSealed = sealedCount > 27 ? sealedCount - 27 : 0;

        for (int i = 9 + mainInvSealed; i < 36; i++) {
            if (player.inventory.mainInventory.get(i).isEmpty()) {
                available++;
            }
        }

        for (int i = hotbarSealed; i < 9; i++) {
            if (player.inventory.mainInventory.get(i).isEmpty()) {
                available++;
            }
        }

        return available;
    }

    /**
     * 获取玩家的有效背包大小（排除封印槽位）
     */
    public static int getEffectiveInventorySize(EntityPlayer player) {
        PermanentCurseManager manager = PermanentCurseManager.get(player.world);
        int sealedCount = manager.getBlackCoffinSealed(player);
        return player.inventory.mainInventory.size() - sealedCount;
    }
}
