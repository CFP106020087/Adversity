package com.adversity.item.bauble;

import com.adversity.Adversity;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.talisman.ITalismanCapability;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * 饰品工具类
 * 提供饰品保护效果的检查和触发接口
 */
public class BaubleHelper {

    public static final Random RANDOM = new Random();

    /**
     * 检查玩家是否携带了特定物品
     * 检查: 主背包 -> 副手 -> 护符盒
     */
    public static boolean hasBauble(EntityPlayer player, net.minecraft.item.Item item) {
        if (item == null)
            return false;

        // 检查主背包
        for (ItemStack stack : player.inventory.mainInventory) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                return true;
            }
        }

        // 检查副手
        for (ItemStack stack : player.inventory.offHandInventory) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                return true;
            }
        }

        // 检查护符盒
        ITalismanCapability talisman = CapabilityHandler.getTalismanCapability(player);
        if (talisman != null) {
            for (int i = 0; i < talisman.getSlotCount(); i++) {
                ItemStack stack = talisman.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() == item) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 检查玩家是否有特定类型的守护饰品保护
     * 并根据效果强度决定是否阻止效果
     *
     * @param player   目标玩家
     * @param wardType 守护类型 (shackle, divest, fiery等)
     * @param source   效果来源（攻击者）
     * @return true 如果效果被阻止
     */
    public static boolean tryBlockEffect(EntityPlayer player, String wardType, @Nullable EntityLivingBase source) {
        AbstractWardBauble bauble = findWardBauble(player, wardType);
        if (bauble == null) {
            return false;
        }

        float strength = bauble.getEffectStrength(player);

        // 完全免疫
        if (strength >= 1.0f) {
            bauble.onEffectTriggered(player, source, true);
            Adversity.LOGGER.debug("Bauble {} fully blocked {} effect", bauble.wardType, wardType);
            return true;
        }

        // 概率阻止
        if (strength > 0 && RANDOM.nextFloat() < strength) {
            bauble.onEffectTriggered(player, source, true);
            Adversity.LOGGER.debug("Bauble {} ({}%) blocked {} effect",
                    bauble.wardType, (int) (strength * 100), wardType);
            return true;
        }

        // 未阻止
        return false;
    }

    /**
     * 获取玩家对特定效果的减免比例
     * 
     * @return 0.0-1.0, 0表示无减免，1表示完全免疫
     */
    public static float getReduction(EntityPlayer player, String wardType) {
        AbstractWardBauble bauble = findWardBauble(player, wardType);
        if (bauble == null) {
            return 0.0f;
        }
        return bauble.getEffectStrength(player);
    }

    /**
     * 获取玩家对特定效果的防护强度 (getReduction的别名)
     */
    public static float getEffectStrength(EntityPlayer player, String wardType) {
        return getReduction(player, wardType);
    }

    /**
     * 查找玩家装备的特定类型守护饰品
     * 检查: 主背包 -> 副手 -> 护符盒
     */
    @Nullable
    private static AbstractWardBauble findWardBauble(EntityPlayer player, String wardType) {
        // 检查主背包
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack.isEmpty())
                continue;
            if (stack.getItem() instanceof AbstractWardBauble) {
                AbstractWardBauble bauble = (AbstractWardBauble) stack.getItem();
                if (bauble.wardType.equals(wardType)) {
                    return bauble;
                }
            }
        }

        // 检查副手
        for (ItemStack stack : player.inventory.offHandInventory) {
            if (stack.isEmpty())
                continue;
            if (stack.getItem() instanceof AbstractWardBauble) {
                AbstractWardBauble bauble = (AbstractWardBauble) stack.getItem();
                if (bauble.wardType.equals(wardType)) {
                    return bauble;
                }
            }
        }

        // 检查护符盒
        ITalismanCapability talisman = CapabilityHandler.getTalismanCapability(player);
        if (talisman != null) {
            for (int i = 0; i < talisman.getSlotCount(); i++) {
                ItemStack stack = talisman.getStackInSlot(i);
                if (stack.isEmpty())
                    continue;
                if (stack.getItem() instanceof AbstractWardBauble) {
                    AbstractWardBauble bauble = (AbstractWardBauble) stack.getItem();
                    if (bauble.wardType.equals(wardType)) {
                        return bauble;
                    }
                }
            }
        }

        return null;
    }

    /**
     * 饰品类型常量
     */
    public static final class WardTypes {
        public static final String SHACKLE = "shackle";
        public static final String DIVEST = "divest";
        public static final String FIERY = "fiery";
        public static final String FROSTY = "frosty";
        public static final String WITHERING = "withering";
        public static final String CURSE = "curse";
        public static final String GRAVITY = "gravity";
        public static final String BLINDING = "blinding";
        public static final String HORROR = "horror";
        public static final String DISENCHANT = "disenchant";
        public static final String SPLIT = "split";
        public static final String HEALING = "healing";
        // 新增类型
        public static final String EVASION = "evasion"; // 反制传送/蜕皮
        public static final String DAMAGE_CONVERSION = "damage_conversion"; // 反制反射
        public static final String AURA = "aura"; // 反制光环效果
        public static final String STACK_SYSTEM = "stack_system"; // 反制叠层系统
    }

}
