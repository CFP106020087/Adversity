package com.adversity.seal;

import com.adversity.Adversity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;
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

        // DEBUG: 每60秒输出一次检查状态
        if (player.ticksExisted % 1200 == 0) {
            Adversity.LOGGER.info("[SealDebug] Checking seals for player {}, worldTime={}", player.getName(), currentTime);
            // 打印所有盔甲栏内容
            for (int i = 0; i < player.inventory.armorInventory.size(); i++) {
                ItemStack stack = player.inventory.armorInventory.get(i);
                if (!stack.isEmpty()) {
                    NBTTagCompound nbt = stack.getTagCompound();
                    Adversity.LOGGER.info("[SealDebug] ArmorSlot[{}]: '{}', hasNBT={}, isSealed={}, nbt={}",
                        i, stack.getDisplayName(), nbt != null, SealedItemManager.isSealed(stack),
                        nbt != null ? nbt.toString() : "null");
                }
            }
        }

        // 检查盔甲栏（armorInventory: 0=boots, 1=legs, 2=chest, 3=head）
        for (int i = 0; i < player.inventory.armorInventory.size(); i++) {
            ItemStack stack = player.inventory.armorInventory.get(i);
            if (!stack.isEmpty() && SealedItemManager.isSealed(stack)) {
                long endTime = SealedItemManager.getSealEndTime(stack);
                long remaining = endTime - currentTime;
                Adversity.LOGGER.info("[SealDebug] Found sealed armor '{}' in slot {}, endTime={}, currentTime={}, remaining={} ticks ({} sec)",
                    stack.getDisplayName(), i, endTime, currentTime, remaining, remaining / 20);
                if (currentTime >= endTime) {
                    // 解除封印 - 创建新的ItemStack确保同步
                    ItemStack unsealed = SealedItemManager.unsealItem(stack.copy());
                    player.inventory.armorInventory.set(i, unsealed);
                    anyUnsealed = true;
                    Adversity.LOGGER.info("[SealDebug] >>> UNSEALED armor '{}' in slot {}, isStillSealed={}",
                        unsealed.getDisplayName(), i, SealedItemManager.isSealed(unsealed));
                }
            }
        }

        // 检查副手
        ItemStack offhand = player.inventory.offHandInventory.get(0);
        if (!offhand.isEmpty() && SealedItemManager.isSealed(offhand)) {
            long endTime = SealedItemManager.getSealEndTime(offhand);
            Adversity.LOGGER.info("[SealDebug] Found sealed offhand '{}', endTime={}, currentTime={}",
                offhand.getDisplayName(), endTime, currentTime);
            if (currentTime >= endTime) {
                ItemStack unsealed = SealedItemManager.unsealItem(offhand.copy());
                player.inventory.offHandInventory.set(0, unsealed);
                anyUnsealed = true;
                Adversity.LOGGER.info("[SealDebug] >>> UNSEALED offhand '{}'", unsealed.getDisplayName());
            }
        }

        // 检查主手（热键栏当前选中的槽位）
        int currentSlot = player.inventory.currentItem;
        ItemStack mainhand = player.inventory.mainInventory.get(currentSlot);
        if (!mainhand.isEmpty() && SealedItemManager.isSealed(mainhand)) {
            long endTime = SealedItemManager.getSealEndTime(mainhand);
            Adversity.LOGGER.info("[SealDebug] Found sealed mainhand '{}' in slot {}, endTime={}, currentTime={}",
                mainhand.getDisplayName(), currentSlot, endTime, currentTime);
            if (currentTime >= endTime) {
                ItemStack unsealed = SealedItemManager.unsealItem(mainhand.copy());
                player.inventory.mainInventory.set(currentSlot, unsealed);
                anyUnsealed = true;
                Adversity.LOGGER.info("[SealDebug] >>> UNSEALED mainhand '{}' in slot {}", unsealed.getDisplayName(), currentSlot);
            }
        }

        // 检查主背包其他槽位
        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            if (i == currentSlot) continue;  // 已经检查过主手
            ItemStack stack = player.inventory.mainInventory.get(i);
            if (!stack.isEmpty() && SealedItemManager.isSealed(stack)) {
                long endTime = SealedItemManager.getSealEndTime(stack);
                Adversity.LOGGER.info("[SealDebug] Found sealed item '{}' in inventory slot {}, endTime={}, currentTime={}",
                    stack.getDisplayName(), i, endTime, currentTime);
                if (currentTime >= endTime) {
                    ItemStack unsealed = SealedItemManager.unsealItem(stack.copy());
                    player.inventory.mainInventory.set(i, unsealed);
                    anyUnsealed = true;
                    Adversity.LOGGER.info("[SealDebug] >>> UNSEALED item '{}' in inventory slot {}", unsealed.getDisplayName(), i);
                }
            }
        }

        // 检查Baubles饰品栏
        if (isBaublesLoaded()) {
            anyUnsealed |= checkBaublesInventory(player, currentTime);
        }

        // 检查并返还虚空存储的物品（湮灭词条）
        boolean anyReturned = checkVoidStorage(player, currentTime);

        Adversity.LOGGER.info("[SealDebug] Check complete: anyUnsealed={}, anyReturned={}", anyUnsealed, anyReturned);

        // 强制同步物品栏到客户端
        if (anyUnsealed || anyReturned) {
            player.inventory.markDirty();
            if (player instanceof EntityPlayerMP) {
                // 使用sendContainerToPlayer强制完整同步
                ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
            }
            player.inventoryContainer.detectAndSendChanges();
        }

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
        // 使用主世界的存储以避免跨维度问题
        if (player.getServer() == null) {
            Adversity.LOGGER.warn("[VoidDebug] Server is null for player {}", player.getName());
            return false;
        }

        SealedItemManager manager = SealedItemManager.get(player.getServer().getWorld(0));
        int totalVoidItems = manager.getVoidStorageCount(player);

        // 每60秒输出一次虚空存储状态
        if (player.ticksExisted % 1200 == 0 && totalVoidItems > 0) {
            Adversity.LOGGER.info("[VoidDebug] Player {} has {} items in void storage, currentTime={}",
                player.getName(), totalVoidItems, currentTime);
        }

        java.util.List<SealedItemManager.VoidStoredItem> returnItems = manager.getReturnableItems(player, currentTime);

        if (returnItems.isEmpty()) {
            return false;
        }

        Adversity.LOGGER.info("[VoidDebug] Returning {} items from void to player {}", returnItems.size(), player.getName());

        for (SealedItemManager.VoidStoredItem item : returnItems) {
            Adversity.LOGGER.info("[VoidDebug] >>> Returning '{}' (originalSlot={}, returnTime={})",
                item.stack.getDisplayName(), item.slotIndex, item.returnTime);
            returnItemToPlayer(player, item);
        }

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
                Adversity.LOGGER.info("[VoidDebug] Placed '{}' back to original slot {}",
                    item.stack.getDisplayName(), item.slotIndex);
                playReturnEffects(player);
                return;
            } else {
                Adversity.LOGGER.info("[VoidDebug] Original slot {} is occupied, trying other slots",
                    item.slotIndex);
            }
        }

        // 尝试放入背包其他位置
        if (player.inventory.addItemStackToInventory(item.stack)) {
            Adversity.LOGGER.info("[VoidDebug] Added '{}' to inventory via addItemStackToInventory",
                item.stack.getDisplayName());
            playReturnEffects(player);
            return;
        }

        // 背包已满，掉落在地上
        Adversity.LOGGER.info("[VoidDebug] Inventory full, dropping '{}' on ground",
            item.stack.getDisplayName());
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
