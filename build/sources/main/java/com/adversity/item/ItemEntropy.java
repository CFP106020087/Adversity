package com.adversity.item;

import com.adversity.Adversity;
import com.adversity.item.AdversityTab;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.adversity.config.AdversityConfig;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 熵能物品 - 圣所的燃料
 * 从精英怪物掉落，用于维护圣所运作
 * 
 * 有3个等级（通过metadata区分）：
 * - 0: 熵能碎片 (默认10燃料，可通过CFG调整)
 * - 1: 熵能结晶 (默认50燃料，可通过CFG调整)
 * - 2: 熵能核心 (默认200燃料，可通过CFG调整)
 */
public class ItemEntropy extends Item {

    public static final int SHARD = 0;
    public static final int CRYSTAL = 1;
    public static final int CORE = 2;

    // 保留常量作为代码内引用的默认值
    public static final int SHARD_FUEL = 10;
    public static final int CRYSTAL_FUEL = 50;
    public static final int CORE_FUEL = 200;

    public ItemEntropy() {
        setRegistryName(Adversity.MODID, "entropy");
        setTranslationKey(Adversity.MODID + ".entropy");
        setMaxStackSize(64);
        setHasSubtypes(true);
        setMaxDamage(0);
        setCreativeTab(AdversityTab.INSTANCE);
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        switch (stack.getMetadata()) {
            case SHARD:
                return getTranslationKey() + ".shard";
            case CRYSTAL:
                return getTranslationKey() + ".crystal";
            case CORE:
                return getTranslationKey() + ".core";
            default:
                return getTranslationKey();
        }
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (this.isInCreativeTab(tab)) {
            items.add(new ItemStack(this, 1, SHARD));
            items.add(new ItemStack(this, 1, CRYSTAL));
            items.add(new ItemStack(this, 1, CORE));
        }
    }

    /**
     * 获取物品的燃料值
     */
    public static int getFuelValue(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemEntropy)) {
            return 0;
        }
        switch (stack.getMetadata()) {
            case SHARD:
                return AdversityConfig.sanctuarySettings.entropyShardFuel;
            case CRYSTAL:
                return AdversityConfig.sanctuarySettings.entropyCrystalFuel;
            case CORE:
                return AdversityConfig.sanctuarySettings.entropyCoreFuel;
            default:
                return AdversityConfig.sanctuarySettings.entropyShardFuel;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn,
            List<String> tooltip, ITooltipFlag flagIn) {
        int fuelValue = getFuelValue(stack);
        tooltip.add(I18n.format("adversity.entropy.fuel_value", fuelValue));
        tooltip.add(I18n.format("adversity.entropy.usage_hint"));
    }

    /**
     * 创建熵能碎片
     */
    public static ItemStack createShard(int count) {
        return new ItemStack(ItemRegistry.ENTROPY, count, SHARD);
    }

    /**
     * 创建熵能结晶
     */
    public static ItemStack createCrystal(int count) {
        return new ItemStack(ItemRegistry.ENTROPY, count, CRYSTAL);
    }

    /**
     * 创建熵能核心
     */
    public static ItemStack createCore(int count) {
        return new ItemStack(ItemRegistry.ENTROPY, count, CORE);
    }
}
