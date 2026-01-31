package com.adversity.handler;

import com.adversity.Adversity;
import com.adversity.config.AdversityConfig;
import com.adversity.enchantment.EnchantmentRegistry;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.IMerchant;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Iterator;
import java.util.Map;

/**
 * 村民交易处理器
 * 
 * 从村民交易中移除包含 Adversity 附魔的物品
 */
@Mod.EventBusSubscriber(modid = Adversity.MODID)
public class VillagerTradeHandler {

    /**
     * 当玩家与村民交互时，过滤交易列表
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onVillagerInteract(PlayerInteractEvent.EntityInteract event) {
        // 检查配置是否启用
        if (!AdversityConfig.sanctuarySettings.removeEnchantsFromVillagers) {
            return;
        }

        if (event.getWorld().isRemote) return;
        
        // 检查是否是村民
        if (!(event.getTarget() instanceof EntityVillager)) {
            return;
        }

        EntityVillager villager = (EntityVillager) event.getTarget();
        
        // 获取交易列表
        MerchantRecipeList recipes = villager.getRecipes(event.getEntityPlayer());
        if (recipes == null || recipes.isEmpty()) {
            return;
        }

        // 过滤包含 Adversity 附魔的交易
        Iterator<MerchantRecipe> iterator = recipes.iterator();
        while (iterator.hasNext()) {
            MerchantRecipe recipe = iterator.next();
            
            // 检查输出物品
            ItemStack output = recipe.getItemToSell();
            if (containsAdversityEnchantment(output)) {
                iterator.remove();
                continue;
            }
            
            // 检查第一个输入物品
            ItemStack input1 = recipe.getItemToBuy();
            if (containsAdversityEnchantment(input1)) {
                iterator.remove();
                continue;
            }
            
            // 检查第二个输入物品
            ItemStack input2 = recipe.getSecondItemToBuy();
            if (containsAdversityEnchantment(input2)) {
                iterator.remove();
            }
        }
    }

    /**
     * 检查物品是否包含 Adversity 附魔
     */
    private static boolean containsAdversityEnchantment(ItemStack stack) {
        if (stack.isEmpty()) return false;

        // 获取物品的附魔
        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
        
        for (Enchantment ench : enchants.keySet()) {
            if (isAdversityEnchantment(ench)) {
                return true;
            }
        }

        // 对于附魔书，还需要检查存储的附魔
        if (stack.getItem() instanceof ItemEnchantedBook) {
            NBTTagList storedEnchants = ItemEnchantedBook.getEnchantments(stack);
            for (int i = 0; i < storedEnchants.tagCount(); i++) {
                NBTTagCompound tag = storedEnchants.getCompoundTagAt(i);
                int id = tag.getShort("id");
                Enchantment ench = Enchantment.getEnchantmentByID(id);
                if (isAdversityEnchantment(ench)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 检查是否是 Adversity 附魔
     */
    private static boolean isAdversityEnchantment(Enchantment ench) {
        if (ench == null) return false;
        return ench == EnchantmentRegistry.SOULBOUND ||
               ench == EnchantmentRegistry.BREAKER ||
               ench == EnchantmentRegistry.ENTROPY_AFFINITY ||
               ench == EnchantmentRegistry.PURIFYING_TOUCH ||
               ench == EnchantmentRegistry.RESOLUTE_WILL;
    }
}
