package com.adversity.curse;

import com.adversity.Adversity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
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

    /** 检查间隔（ticks） - 优化性能 */
    private static final int CHECK_INTERVAL = 20;  // 每秒检查一次

    /**
     * 定期检查并强制清空封印槽位
     * 这是最可靠的方法来确保封印槽位不能使用
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;
        if (event.player.ticksExisted % CHECK_INTERVAL != 0) return;

        EntityPlayer player = event.player;
        PermanentCurseManager manager = PermanentCurseManager.get(player.world);

        int sealedCount = manager.getBlackCoffinSealed(player);
        if (sealedCount <= 0) return;

        // 检查封印槽位是否有物品
        // 从背包末尾开始封印（槽位35, 34, 33...）
        // 玩家主背包有36个槽位（0-35），热键栏是0-8
        int inventorySize = player.inventory.mainInventory.size();  // 通常是36
        int startSealedSlot = inventorySize - sealedCount;

        for (int i = startSealedSlot; i < inventorySize; i++) {
            ItemStack stack = player.inventory.mainInventory.get(i);
            if (!stack.isEmpty()) {
                // 将物品弹出到世界
                EntityItem entityItem = player.dropItem(stack.copy(), false);
                if (entityItem != null) {
                    entityItem.setNoPickupDelay();
                    entityItem.setOwner(player.getName());
                }
                player.inventory.mainInventory.set(i, ItemStack.EMPTY);

                // 发送提示（限制频率避免刷屏）
                if (player.ticksExisted % 100 == 0) {
                    TextComponentTranslation msg = new TextComponentTranslation("adversity.curse.slot_sealed");
                    msg.getStyle().setColor(TextFormatting.DARK_PURPLE);
                    player.sendMessage(msg);
                }
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
