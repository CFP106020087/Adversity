package com.adversity.item;

import com.adversity.Adversity;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 封印令牌物品
 * 当装备被封印时，创建此物品保存原物品数据
 * 玩家持有此物品，耐久条显示剩余时间
 * 到期后自动恢复原物品到原槽位
 */
public class ItemSealedToken extends Item {

    // NBT标签
    public static final String TAG_ORIGINAL_ITEM = "OriginalItem";
    public static final String TAG_SLOT_TYPE = "SlotType";  // ARMOR, MAINHAND, OFFHAND, INVENTORY, BAUBLE
    public static final String TAG_SLOT_INDEX = "SlotIndex";
    public static final String TAG_SEAL_END_TIME = "SealEndTime";
    public static final String TAG_SEAL_DURATION = "SealDuration";  // 用于计算耐久条
    public static final String TAG_ORIGINAL_NAME = "OriginalName";

    public ItemSealedToken() {
        setRegistryName(Adversity.MODID, "sealed_token");
        setTranslationKey(Adversity.MODID + ".sealed_token");
        setMaxStackSize(1);
        setMaxDamage(100);  // 用于显示耐久条
        setCreativeTab(CreativeTabs.MISC);
    }

    /**
     * 创建封印令牌
     * @param originalItem 原物品
     * @param slotType 槽位类型 (ARMOR, MAINHAND, OFFHAND, INVENTORY, BAUBLE)
     * @param slotIndex 槽位索引
     * @param sealEndTime 封印结束时间
     * @param sealDuration 封印总时长(ticks)
     */
    public static ItemStack createToken(ItemStack originalItem, String slotType, int slotIndex,
                                         long sealEndTime, long sealDuration) {
        ItemStack token = new ItemStack(ItemRegistry.SEALED_TOKEN);
        NBTTagCompound nbt = new NBTTagCompound();

        // 保存原物品
        nbt.setTag(TAG_ORIGINAL_ITEM, originalItem.serializeNBT());
        nbt.setString(TAG_SLOT_TYPE, slotType);
        nbt.setInteger(TAG_SLOT_INDEX, slotIndex);
        nbt.setLong(TAG_SEAL_END_TIME, sealEndTime);
        nbt.setLong(TAG_SEAL_DURATION, sealDuration);
        nbt.setString(TAG_ORIGINAL_NAME, originalItem.getDisplayName());

        token.setTagCompound(nbt);
        return token;
    }

    /**
     * 获取原物品
     */
    public static ItemStack getOriginalItem(ItemStack token) {
        if (token.isEmpty() || !(token.getItem() instanceof ItemSealedToken)) {
            return ItemStack.EMPTY;
        }
        NBTTagCompound nbt = token.getTagCompound();
        if (nbt == null || !nbt.hasKey(TAG_ORIGINAL_ITEM)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(nbt.getCompoundTag(TAG_ORIGINAL_ITEM));
    }

    /**
     * 获取槽位类型
     */
    public static String getSlotType(ItemStack token) {
        NBTTagCompound nbt = token.getTagCompound();
        return nbt != null ? nbt.getString(TAG_SLOT_TYPE) : "";
    }

    /**
     * 获取槽位索引
     */
    public static int getSlotIndex(ItemStack token) {
        NBTTagCompound nbt = token.getTagCompound();
        return nbt != null ? nbt.getInteger(TAG_SLOT_INDEX) : -1;
    }

    /**
     * 获取封印结束时间
     */
    public static long getSealEndTime(ItemStack token) {
        NBTTagCompound nbt = token.getTagCompound();
        return nbt != null ? nbt.getLong(TAG_SEAL_END_TIME) : 0;
    }

    /**
     * 获取封印总时长
     */
    public static long getSealDuration(ItemStack token) {
        NBTTagCompound nbt = token.getTagCompound();
        return nbt != null ? nbt.getLong(TAG_SEAL_DURATION) : 1;
    }

    /**
     * 检查封印是否到期
     */
    public static boolean isExpired(ItemStack token, long currentTime) {
        return currentTime >= getSealEndTime(token);
    }

    /**
     * 计算剩余时间比例 (0.0 - 1.0)
     */
    public static float getRemainingRatio(ItemStack token, long currentTime) {
        long endTime = getSealEndTime(token);
        long duration = getSealDuration(token);
        if (duration <= 0) return 0;

        long remaining = endTime - currentTime;
        if (remaining <= 0) return 0;
        if (remaining >= duration) return 1;

        return (float) remaining / duration;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null && nbt.hasKey(TAG_ORIGINAL_NAME)) {
            String originalName = nbt.getString(TAG_ORIGINAL_NAME);
            return TextFormatting.DARK_PURPLE + "⛓ " + originalName + " [封印中]";
        }
        return TextFormatting.DARK_PURPLE + "封印令牌";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) return;

        // 显示原物品信息
        String slotType = nbt.getString(TAG_SLOT_TYPE);
        String slotName = getSlotTypeName(slotType);
        tooltip.add(TextFormatting.GRAY + "原槽位: " + TextFormatting.YELLOW + slotName);

        // 显示剩余时间
        if (worldIn != null) {
            long currentTime = worldIn.getTotalWorldTime();
            long endTime = getSealEndTime(stack);
            long remaining = Math.max(0, endTime - currentTime);
            int seconds = (int) (remaining / 20);

            tooltip.add(TextFormatting.GRAY + "剩余时间: " + TextFormatting.RED + seconds + " 秒");
        }

        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "封印到期后物品将自动恢复");
    }

    private String getSlotTypeName(String slotType) {
        switch (slotType) {
            case "ARMOR_HEAD": return "头盔";
            case "ARMOR_CHEST": return "胸甲";
            case "ARMOR_LEGS": return "护腿";
            case "ARMOR_FEET": return "靴子";
            case "MAINHAND": return "主手";
            case "OFFHAND": return "副手";
            case "INVENTORY": return "背包";
            default:
                // 动态处理饰品槽位 (BAUBLE_0, BAUBLE_1, ...)
                if (slotType.startsWith("BAUBLE_")) {
                    try {
                        int slotIndex = Integer.parseInt(slotType.substring(7));
                        return "饰品槽 " + (slotIndex + 1);
                    } catch (NumberFormatException e) {
                        return "饰品";
                    }
                }
                return slotType;
        }
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;  // 总是显示耐久条
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        // 返回已消耗的比例 (0.0 = 满, 1.0 = 空)
        World world = net.minecraft.client.Minecraft.getMinecraft().world;
        if (world == null) return 0;

        float remaining = getRemainingRatio(stack, world.getTotalWorldTime());
        return 1.0 - remaining;  // 反转，使耐久条从满到空
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        // 紫色耐久条
        return 0x8B00FF;
    }

    /**
     * 将封印令牌恢复为原物品
     * @return 是否成功恢复
     */
    public static boolean restoreItem(EntityPlayer player, ItemStack token, int tokenSlot) {
        if (token.isEmpty() || !(token.getItem() instanceof ItemSealedToken)) {
            return false;
        }

        ItemStack originalItem = getOriginalItem(token);
        if (originalItem.isEmpty()) {
            return false;
        }

        String slotType = getSlotType(token);
        int slotIndex = getSlotIndex(token);

        Adversity.LOGGER.info("[SealToken] Restoring '{}' to slot type={}, index={}",
            originalItem.getDisplayName(), slotType, slotIndex);

        boolean restored = false;

        // 根据槽位类型恢复
        if (slotType.startsWith("BAUBLE")) {
            restored = restoreToBauble(player, originalItem, slotIndex);
        } else {
            switch (slotType) {
                case "ARMOR_HEAD":
                    restored = restoreToArmor(player, originalItem, 3);
                    break;
                case "ARMOR_CHEST":
                    restored = restoreToArmor(player, originalItem, 2);
                    break;
                case "ARMOR_LEGS":
                    restored = restoreToArmor(player, originalItem, 1);
                    break;
                case "ARMOR_FEET":
                    restored = restoreToArmor(player, originalItem, 0);
                    break;
                case "MAINHAND":
                case "INVENTORY":
                    restored = restoreToInventory(player, originalItem, slotIndex);
                    break;
                case "OFFHAND":
                    restored = restoreToOffhand(player, originalItem);
                    break;
                default:
                    // 尝试放入背包
                    restored = player.inventory.addItemStackToInventory(originalItem);
                    if (!restored) {
                        player.dropItem(originalItem, false);
                        restored = true;
                    }
            }
        }

        // 移除令牌
        if (restored) {
            player.inventory.mainInventory.set(tokenSlot, ItemStack.EMPTY);
            Adversity.LOGGER.info("[SealToken] Successfully restored item and removed token from slot {}", tokenSlot);
        }

        return restored;
    }

    private static boolean restoreToArmor(EntityPlayer player, ItemStack item, int armorIndex) {
        if (player.inventory.armorInventory.get(armorIndex).isEmpty()) {
            player.inventory.armorInventory.set(armorIndex, item);
            return true;
        }
        // 槽位被占用，放入背包
        if (player.inventory.addItemStackToInventory(item)) {
            return true;
        }
        // 背包满，掉落
        player.dropItem(item, false);
        return true;
    }

    private static boolean restoreToInventory(EntityPlayer player, ItemStack item, int slotIndex) {
        if (slotIndex >= 0 && slotIndex < player.inventory.mainInventory.size()) {
            if (player.inventory.mainInventory.get(slotIndex).isEmpty()) {
                player.inventory.mainInventory.set(slotIndex, item);
                return true;
            }
        }
        // 原槽位被占用，放入背包
        if (player.inventory.addItemStackToInventory(item)) {
            return true;
        }
        // 背包满，掉落
        player.dropItem(item, false);
        return true;
    }

    private static boolean restoreToOffhand(EntityPlayer player, ItemStack item) {
        if (player.inventory.offHandInventory.get(0).isEmpty()) {
            player.inventory.offHandInventory.set(0, item);
            return true;
        }
        // 副手被占用，放入背包
        if (player.inventory.addItemStackToInventory(item)) {
            return true;
        }
        // 背包满，掉落
        player.dropItem(item, false);
        return true;
    }

    private static boolean restoreToBauble(EntityPlayer player, ItemStack item, int slotIndex) {
        // 检查Baubles是否加载
        if (!net.minecraftforge.fml.common.Loader.isModLoaded("baubles")) {
            // Baubles未加载，放入背包
            if (player.inventory.addItemStackToInventory(item)) {
                return true;
            }
            player.dropItem(item, false);
            return true;
        }

        // 使用BaublesCompat恢复
        return restoreToBaubleInternal(player, item, slotIndex);
    }

    /**
     * 内部方法 - 使用BaublesCompat恢复饰品
     * 延迟加载，只在Baubles存在时调用
     */
    private static boolean restoreToBaubleInternal(EntityPlayer player, ItemStack item, int slotIndex) {
        ItemStack current = com.adversity.compat.BaublesCompat.getStackInSlot(player, slotIndex);

        if (current.isEmpty()) {
            com.adversity.compat.BaublesCompat.setStackInSlot(player, slotIndex, item);
            return true;
        }

        // Baubles槽位被占用，放入背包
        if (player.inventory.addItemStackToInventory(item)) {
            return true;
        }
        player.dropItem(item, false);
        return true;
    }
}
