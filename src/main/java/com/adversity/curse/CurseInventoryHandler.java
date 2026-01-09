package com.adversity.curse;

import com.adversity.Adversity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 诅咒背包处理器
 * 处理黑棺封印槽位的交互阻止
 */
@Mod.EventBusSubscriber(modid = Adversity.MODID)
public class CurseInventoryHandler {

    /** 检查间隔（ticks） - 每tick检查确保立即响应 */
    private static final int CHECK_INTERVAL = 1;

    /**
     * 每tick检查并强制清空封印槽位
     * 确保物品无法停留在封印槽位
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;
        PermanentCurseManager manager = PermanentCurseManager.get(player.world);

        int sealedCount = manager.getBlackCoffinSealed(player);
        if (sealedCount <= 0) return;

        // 检查封印槽位是否有物品
        // 先封主背包(9-35)，再封快捷栏(0-8)
        boolean ejectedAny = false;

        // 检查主背包封印槽位（9-35，最多27个）
        int mainInvSealed = Math.min(sealedCount, 27);
        for (int i = 9; i < 9 + mainInvSealed; i++) {
            ItemStack stack = player.inventory.mainInventory.get(i);
            if (!stack.isEmpty()) {
                // 直接清空槽位并尝试移动物品
                ItemStack copy = stack.copy();
                player.inventory.mainInventory.set(i, ItemStack.EMPTY);

                // 找到可用槽位或掉落
                int targetSlot = findAvailableSlot(player, sealedCount);
                if (targetSlot >= 0) {
                    player.inventory.mainInventory.set(targetSlot, copy);
                } else {
                    player.dropItem(copy, false);
                }
                ejectedAny = true;
            }
        }

        // 检查快捷栏封印槽位（0-8，在主背包全封后）
        if (sealedCount > 27) {
            int hotbarSealed = sealedCount - 27;
            for (int i = 0; i < hotbarSealed && i < 9; i++) {
                ItemStack stack = player.inventory.mainInventory.get(i);
                if (!stack.isEmpty()) {
                    ItemStack copy = stack.copy();
                    player.inventory.mainInventory.set(i, ItemStack.EMPTY);

                    int targetSlot = findAvailableSlot(player, sealedCount);
                    if (targetSlot >= 0) {
                        player.inventory.mainInventory.set(targetSlot, copy);
                    } else {
                        player.dropItem(copy, false);
                    }
                    ejectedAny = true;
                }
            }
        }

        // 强制同步背包
        if (ejectedAny) {
            player.inventory.markDirty();
            if (player instanceof EntityPlayerMP) {
                ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
            }
            player.inventoryContainer.detectAndSendChanges();

            // 发送提示（限制频率避免刷屏，每5秒最多一次）
            if (player.ticksExisted % 100 == 0) {
                TextComponentTranslation msg = new TextComponentTranslation("adversity.curse.slot_sealed");
                msg.getStyle().setColor(TextFormatting.DARK_PURPLE);
                player.sendMessage(msg);
            }
        }
    }

    /**
     * 阻止拾取物品到已满的背包（考虑封印槽位）
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (event.getEntityPlayer().world.isRemote) return;

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
     * 检查玩家背包槽位是否被封印
     * 先封主背包(9-35)，再封快捷栏(0-8)
     * @param player 玩家
     * @param slotIndex 背包槽位索引（0-35）
     */
    public static boolean isSlotSealed(EntityPlayer player, int slotIndex) {
        if (player.world.isRemote) return false;

        PermanentCurseManager manager = PermanentCurseManager.get(player.world);
        int sealedCount = manager.getBlackCoffinSealed(player);
        if (sealedCount <= 0) return false;

        // 主背包有27个槽位(9-35)，快捷栏有9个槽位(0-8)
        if (slotIndex >= 9 && slotIndex < 36) {
            // 主背包槽位：先封印
            // slotIndex 9 对应第1个封印，slotIndex 35 对应第27个封印
            int sealOrder = slotIndex - 9;  // 0-26
            return sealOrder < sealedCount;
        } else if (slotIndex >= 0 && slotIndex < 9) {
            // 快捷栏槽位：后封印（在主背包全部封印之后）
            // 需要超过27个封印才开始封快捷栏
            if (sealedCount <= 27) return false;
            int hotbarSealed = sealedCount - 27;  // 快捷栏已封印数
            return slotIndex < hotbarSealed;
        }
        return false;
    }

    /**
     * 找到第一个可用槽位（未封印且为空）
     */
    private static int findAvailableSlot(EntityPlayer player, int sealedCount) {
        // 主背包封印数（最多27）
        int mainInvSealed = Math.min(sealedCount, 27);
        // 快捷栏封印数（超过27后）
        int hotbarSealed = sealedCount > 27 ? sealedCount - 27 : 0;

        // 先检查未封印的主背包槽位 (9+mainInvSealed 到 35)
        for (int i = 9 + mainInvSealed; i < 36; i++) {
            if (player.inventory.mainInventory.get(i).isEmpty()) {
                return i;
            }
        }

        // 再检查未封印的快捷栏槽位 (hotbarSealed 到 8)
        for (int i = hotbarSealed; i < 9; i++) {
            if (player.inventory.mainInventory.get(i).isEmpty()) {
                return i;
            }
        }

        return -1;  // 没有可用槽位
    }

    /**
     * 计算可用槽位数（排除封印槽位）
     */
    private static int countAvailableSlots(EntityPlayer player, int sealedCount) {
        int available = 0;

        // 主背包封印数（最多27）
        int mainInvSealed = Math.min(sealedCount, 27);
        // 快捷栏封印数（超过27后）
        int hotbarSealed = sealedCount > 27 ? sealedCount - 27 : 0;

        // 检查未封印的主背包槽位 (9+mainInvSealed 到 35)
        for (int i = 9 + mainInvSealed; i < 36; i++) {
            if (player.inventory.mainInventory.get(i).isEmpty()) {
                available++;
            }
        }

        // 检查未封印的快捷栏槽位 (hotbarSealed 到 8)
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
