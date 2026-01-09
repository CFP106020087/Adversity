package com.adversity.debug;

import com.adversity.Adversity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 调试工具：追踪所有盔甲槽变化
 *
 * 会打印完整的调用栈，帮助定位是谁在修改盔甲
 */
@Mod.EventBusSubscriber(modid = Adversity.MODID)
public class ArmorChangeTracker {

    // 设为true启用追踪
    private static final boolean ENABLED = true;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!ENABLED) return;
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        EntityEquipmentSlot slot = event.getSlot();

        // 只追踪盔甲槽
        if (slot.getSlotType() != EntityEquipmentSlot.Type.ARMOR) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        ItemStack from = event.getFrom();
        ItemStack to = event.getTo();

        // 检查黑棺诅咒状态
        int blackCoffinSealed = 0;
        try {
            com.adversity.curse.PermanentCurseManager manager =
                com.adversity.curse.PermanentCurseManager.get(player.world);
            blackCoffinSealed = manager.getBlackCoffinSealed(player);
        } catch (Exception e) {
            // ignore
        }

        // 打印变化信息
        Adversity.LOGGER.warn("========== [ArmorTracker] 盔甲变化检测 ==========");
        Adversity.LOGGER.warn("[ArmorTracker] 玩家: {}", player.getName());
        Adversity.LOGGER.warn("[ArmorTracker] 槽位: {} (index={})", slot.getName(), slot.getIndex());
        Adversity.LOGGER.warn("[ArmorTracker] 变化: '{}' -> '{}'",
            from.isEmpty() ? "空" : from.getDisplayName(),
            to.isEmpty() ? "空" : to.getDisplayName());
        Adversity.LOGGER.warn("[ArmorTracker] 世界时间: {}", player.world.getTotalWorldTime());
        Adversity.LOGGER.warn("[ArmorTracker] 黑棺封印槽位数: {}", blackCoffinSealed);

        // 打印当前所有盔甲状态
        Adversity.LOGGER.warn("[ArmorTracker] 当前盔甲状态:");
        for (int i = 0; i < 4; i++) {
            ItemStack armor = player.inventory.armorInventory.get(i);
            Adversity.LOGGER.warn("[ArmorTracker]   slot[{}] = '{}'", i,
                armor.isEmpty() ? "空" : armor.getDisplayName());
        }

        // 打印调用栈
        Adversity.LOGGER.warn("[ArmorTracker] 调用栈:");
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 2; i < Math.min(stackTrace.length, 25); i++) {
            StackTraceElement element = stackTrace[i];
            String className = element.getClassName();

            // 过滤掉一些无关的类，但保留关键信息
            if (className.contains("adversity") ||
                className.contains("minecraft") ||
                className.contains("forge") ||
                className.contains("fml")) {
                Adversity.LOGGER.warn("[ArmorTracker]   at {}.{}({}:{})",
                    element.getClassName(),
                    element.getMethodName(),
                    element.getFileName(),
                    element.getLineNumber());
            }
        }
        Adversity.LOGGER.warn("========== [ArmorTracker] 结束 ==========");
    }
}
