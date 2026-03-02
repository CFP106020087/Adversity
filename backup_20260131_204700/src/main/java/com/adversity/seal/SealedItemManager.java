package com.adversity.seal;

import com.adversity.Adversity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 封印物品管理器
 * 管理临时封印的物品，处理装备槽封印和恢复
 */
public class SealedItemManager extends WorldSavedData {

    private static final String DATA_NAME = Adversity.MODID + "_sealed_items";

    /** 封印物品NBT标签 */
    public static final String SEALED_TAG = "AdversitySealed";
    public static final String SEAL_END_TIME = "SealEndTime";
    public static final String ORIGINAL_ITEM = "OriginalItem";

    /** 玩家装备槽封印状态 - 玩家UUID -> 槽位索引 -> 封印结束时间 */
    private final Map<UUID, Map<Integer, Long>> sealedSlots = new HashMap<>();

    /** 虚空存储 - 临时存储"湮灭"效果的物品 */
    private final Map<UUID, List<VoidStoredItem>> voidStorage = new HashMap<>();

    public SealedItemManager() {
        super(DATA_NAME);
    }

    public SealedItemManager(String name) {
        super(name);
    }

    /**
     * 获取世界的封印管理器实例
     */
    public static SealedItemManager get(World world) {
        SealedItemManager data = (SealedItemManager) world.getMapStorage()
            .getOrLoadData(SealedItemManager.class, DATA_NAME);

        if (data == null) {
            data = new SealedItemManager();
            world.getMapStorage().setData(DATA_NAME, data);
        }

        return data;
    }

    // ==================== 装备槽封印 ====================

    /**
     * 封印物品（替换为封印版本）
     */
    public static ItemStack sealItem(ItemStack original, long endTime) {
        if (original.isEmpty()) {
            return original;
        }

        ItemStack sealed = original.copy();
        NBTTagCompound nbt = sealed.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
        }

        // 标记为封印状态
        nbt.setBoolean(SEALED_TAG, true);
        nbt.setLong(SEAL_END_TIME, endTime);

        sealed.setTagCompound(nbt);
        return sealed;
    }

    /**
     * 检查物品是否被封印
     */
    public static boolean isSealed(ItemStack stack) {
        if (stack.isEmpty()) return false;
        NBTTagCompound nbt = stack.getTagCompound();
        return nbt != null && nbt.getBoolean(SEALED_TAG);
    }

    /**
     * 获取封印结束时间
     */
    public static long getSealEndTime(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        NBTTagCompound nbt = stack.getTagCompound();
        return nbt != null ? nbt.getLong(SEAL_END_TIME) : 0;
    }

    /**
     * 解除物品封印
     */
    public static ItemStack unsealItem(ItemStack sealed) {
        if (!isSealed(sealed)) {
            return sealed;
        }

        NBTTagCompound nbt = sealed.getTagCompound();
        if (nbt != null) {
            nbt.removeTag(SEALED_TAG);
            nbt.removeTag(SEAL_END_TIME);

            // 如果NBT为空，则移除
            if (nbt.isEmpty()) {
                sealed.setTagCompound(null);
            }
        }

        return sealed;
    }

    /**
     * 检查并恢复过期的封印
     */
    public static void checkAndUnseal(ItemStack stack, long currentTime) {
        if (isSealed(stack) && currentTime >= getSealEndTime(stack)) {
            unsealItem(stack);
        }
    }

    // ==================== 虚空存储 ====================

    /**
     * 将物品存入虚空（湮灭效果）
     */
    public void storeInVoid(EntityPlayer player, ItemStack stack, long returnTime, int slotIndex) {
        UUID playerId = player.getUniqueID();

        List<VoidStoredItem> playerVoid = voidStorage.computeIfAbsent(playerId, k -> new ArrayList<>());
        playerVoid.add(new VoidStoredItem(stack.copy(), returnTime, slotIndex));

        markDirty();
    }

    /**
     * 检查并返还虚空物品
     */
    public List<VoidStoredItem> getReturnableItems(EntityPlayer player, long currentTime) {
        UUID playerId = player.getUniqueID();
        List<VoidStoredItem> playerVoid = voidStorage.get(playerId);

        if (playerVoid == null || playerVoid.isEmpty()) {
            return Collections.emptyList();
        }

        List<VoidStoredItem> returnable = new ArrayList<>();
        Iterator<VoidStoredItem> it = playerVoid.iterator();

        while (it.hasNext()) {
            VoidStoredItem item = it.next();
            if (currentTime >= item.returnTime) {
                returnable.add(item);
                it.remove();
            }
        }

        if (!returnable.isEmpty()) {
            markDirty();
        }

        return returnable;
    }

    /**
     * 获取玩家的虚空存储数量
     */
    public int getVoidStorageCount(EntityPlayer player) {
        List<VoidStoredItem> playerVoid = voidStorage.get(player.getUniqueID());
        return playerVoid != null ? playerVoid.size() : 0;
    }

    // ==================== NBT 序列化 ====================

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        // 读取虚空存储
        voidStorage.clear();
        NBTTagList voidList = nbt.getTagList("VoidStorage", 10);
        for (int i = 0; i < voidList.tagCount(); i++) {
            NBTTagCompound entry = voidList.getCompoundTagAt(i);
            UUID playerId = UUID.fromString(entry.getString("PlayerId"));

            List<VoidStoredItem> items = new ArrayList<>();
            NBTTagList itemsList = entry.getTagList("Items", 10);
            for (int j = 0; j < itemsList.tagCount(); j++) {
                NBTTagCompound itemNbt = itemsList.getCompoundTagAt(j);
                ItemStack stack = new ItemStack(itemNbt.getCompoundTag("Stack"));
                long returnTime = itemNbt.getLong("ReturnTime");
                int slotIndex = itemNbt.getInteger("SlotIndex");
                items.add(new VoidStoredItem(stack, returnTime, slotIndex));
            }

            voidStorage.put(playerId, items);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        // 保存虚空存储
        NBTTagList voidList = new NBTTagList();
        for (Map.Entry<UUID, List<VoidStoredItem>> entry : voidStorage.entrySet()) {
            NBTTagCompound playerNbt = new NBTTagCompound();
            playerNbt.setString("PlayerId", entry.getKey().toString());

            NBTTagList itemsList = new NBTTagList();
            for (VoidStoredItem item : entry.getValue()) {
                NBTTagCompound itemNbt = new NBTTagCompound();
                itemNbt.setTag("Stack", item.stack.serializeNBT());
                itemNbt.setLong("ReturnTime", item.returnTime);
                itemNbt.setInteger("SlotIndex", item.slotIndex);
                itemsList.appendTag(itemNbt);
            }
            playerNbt.setTag("Items", itemsList);

            voidList.appendTag(playerNbt);
        }
        nbt.setTag("VoidStorage", voidList);

        return nbt;
    }

    /**
     * 虚空存储物品结构
     */
    public static class VoidStoredItem {
        public final ItemStack stack;
        public final long returnTime;
        public final int slotIndex;

        public VoidStoredItem(ItemStack stack, long returnTime, int slotIndex) {
            this.stack = stack;
            this.returnTime = returnTime;
            this.slotIndex = slotIndex;
        }
    }
}
