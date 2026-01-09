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
        // 从背包末尾开始封印（槽位35, 34, 33...）
        // 玩家主背包有36个槽位（0-35），热键栏是0-8，主背包是9-35
        int inventorySize = player.inventory.mainInventory.size();  // 通常是36
        int startSealedSlot = inventorySize - sealedCount;

        boolean ejectedAny = false;
        for (int i = startSealedSlot; i < inventorySize; i++) {
            ItemStack stack = player.inventory.mainInventory.get(i);
            if (!stack.isEmpty()) {
                // 尝试放入背包其他位置
                boolean moved = false;
                for (int j = 0; j < startSealedSlot; j++) {
                    if (player.inventory.mainInventory.get(j).isEmpty()) {
                        player.inventory.mainInventory.set(j, stack.copy());
                        player.inventory.mainInventory.set(i, ItemStack.EMPTY);
                        moved = true;
                        ejectedAny = true;
                        break;
                    }
                }

                // 如果背包已满，掉落物品
                if (!moved) {
                    EntityItem entityItem = player.dropItem(stack.copy(), false);
                    if (entityItem != null) {
                        entityItem.setNoPickupDelay();
                        entityItem.setOwner(player.getName());
                    }
                    player.inventory.mainInventory.set(i, ItemStack.EMPTY);
                    ejectedAny = true;
                }
            }
        }

        // 同步背包
        if (ejectedAny) {
            player.inventory.markDirty();
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
     * @param player 玩家
     * @param slotIndex 背包槽位索引（0-35）
     */
    public static boolean isSlotSealed(EntityPlayer player, int slotIndex) {
        if (player.world.isRemote) return false;

        PermanentCurseManager manager = PermanentCurseManager.get(player.world);
        int sealedCount = manager.getBlackCoffinSealed(player);
        if (sealedCount <= 0) return false;

        int inventorySize = player.inventory.mainInventory.size();
        int startSealedSlot = inventorySize - sealedCount;

        // 只封印主背包（槽位9-35），不封印热键栏
        return slotIndex >= 9 && slotIndex >= startSealedSlot && slotIndex < inventorySize;
    }

    /**
     * 计算可用槽位数（排除封印槽位）
     */
    private static int countAvailableSlots(EntityPlayer player, int sealedCount) {
        int available = 0;
        int inventorySize = player.inventory.mainInventory.size();
        int maxUsableSlot = inventorySize - sealedCount;

        for (int i = 0; i < maxUsableSlot; i++) {
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
