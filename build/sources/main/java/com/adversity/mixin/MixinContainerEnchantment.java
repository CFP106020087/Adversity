package com.adversity.mixin;

import com.adversity.Adversity;
import com.adversity.config.AdversityConfig;
import com.adversity.enchantment.EnchantmentGatingHelper;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 附魔台門控 Mixin
 *
 * 職責：
 * 1. 過濾 enchantClue[] 預覽 + 停用門控槽位
 * 2. 附魔完成後移除被門控的附魔
 * 3. 聖所附近注入 Adversity 附魔
 *
 * 所有業務邏輯委託給 EnchantmentGatingHelper。
 */
@Mixin(value = ContainerEnchantment.class, remap = false)
public abstract class MixinContainerEnchantment {

    // vanilla fields — 使用 SRG 名（因 remap=false）
    @Shadow
    public int[] field_185001_h; // enchantClue

    @Shadow
    public int[] field_185002_i; // worldClue

    @Shadow
    public int[] field_75167_g; // enchantLevels

    @Shadow
    public IInventory field_75168_e; // tableInventory

    /**
     * 注入點 1: onCraftMatrixChanged RETURN
     * 過濾 clue 預覽 + 聖所注入
     */
    @Inject(method = { "onCraftMatrixChanged", "func_75130_a" }, at = @At("RETURN"))
    private void adversity$filterClues(IInventory inv, CallbackInfo ci) {
        try {
            EntityPlayer player = EnchantmentGatingHelper.getPlayerFromContainer((Container) (Object) this);
            if (player == null)
                return;

            // 過濾被門控的附魔 (隱藏 clue + 停用槽位)
            EnchantmentGatingHelper.filterEnchantClues(field_185001_h, field_185002_i, field_75167_g, player);

            // 聖所附近注入 Adversity 附魔（必須傳入附魔台物品以檢查兼容性）
            if (AdversityConfig.sanctuarySettings.removeEnchantsFromTable) {
                if (EnchantmentGatingHelper.isNearSanctuary(player)) {
                    ItemStack itemInTable = field_75168_e.getStackInSlot(0);
                    EnchantmentGatingHelper.injectSanctuaryEnchantments(field_185001_h, field_185002_i, field_75167_g, player,
                            itemInTable);
                }
            }
        } catch (Exception e) {
            Adversity.LOGGER.warn("[Enchant Mixin] Error in filterClues: {}", e.getMessage());
        }
    }

    /**
     * 注入點 2: enchantItem RETURN
     * 附魔完成後，從結果物品中移除被門控的附魔
     */
    @Inject(method = { "enchantItem", "func_75140_a" }, at = @At("RETURN"))
    private void adversity$stripGatedEnchants(EntityPlayer playerIn, int id, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (playerIn == null)
                return;

            // 取得剛被附魔的物品 (slot 0)
            ItemStack enchantedItem = field_75168_e.getStackInSlot(0);
            if (enchantedItem.isEmpty() || !enchantedItem.isItemEnchanted())
                return;

            NBTTagList enchants = enchantedItem.getEnchantmentTagList();
            NBTTagList filtered = new NBTTagList();
            boolean changed = false;

            for (int i = 0; i < enchants.tagCount(); i++) {
                NBTTagCompound tag = enchants.getCompoundTagAt(i);
                int enchId = tag.getShort("id");
                Enchantment ench = Enchantment.getEnchantmentByID(enchId);
                if (ench != null && !EnchantmentGatingHelper.isEnchantmentAllowed(ench, playerIn)) {
                    changed = true; // 跳過被門控的附魔
                } else {
                    filtered.appendTag(tag.copy());
                }
            }

            if (changed && enchantedItem.getTagCompound() != null) {
                enchantedItem.getTagCompound().setTag("ench", filtered);
                Adversity.LOGGER.debug("[Enchant Mixin] Stripped gated enchantments from result");
            }
        } catch (Exception e) {
            Adversity.LOGGER.warn("[Enchant Mixin] Error in stripGatedEnchants: {}", e.getMessage());
        }
    }
}

