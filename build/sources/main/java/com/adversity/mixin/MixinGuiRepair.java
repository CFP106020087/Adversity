package com.adversity.mixin;

import com.adversity.client.gui.GatingHudRenderer;
import com.adversity.enchantment.EnchantmentGatingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.GuiRepair;
import net.minecraft.client.resources.I18n;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 鐵砧 GUI — GS badge + 附魔書門控提示
 */
@Mixin(value = GuiRepair.class, remap = false)
public class MixinGuiRepair {

    @Inject(method = { "drawGuiContainerForegroundLayer", "func_146979_b" }, at = @At("TAIL"))
    private void adversity$renderGatingHud(int mouseX, int mouseY, CallbackInfo ci) {
        GuiContainer gui = (GuiContainer) (Object) this;
        List<String> blocked = adversity$getBlockedAnvilEnchantments(gui);
        GatingHudRenderer.setBlockedTitle("adversity.gui.blocked_enchants");
        GatingHudRenderer.setBlockedItems(blocked);
        GatingHudRenderer.renderGsHud(gui.mc.fontRenderer, gui.getXSize());

        // 客戶端強制清空被 gate 的鐵砧輸出（防止幽靈物品渲染）
        if (!blocked.isEmpty() && gui.inventorySlots.inventorySlots.size() >= 3) {
            Slot outputSlot = gui.inventorySlots.getSlot(2);
            if (outputSlot.getHasStack()) {
                outputSlot.putStack(ItemStack.EMPTY);
            }
        }
    }

    @Inject(method = { "mouseClicked", "func_73864_a" }, at = @At("HEAD"), cancellable = true)
    private void adversity$handleHudClick(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        GuiContainer gui = (GuiContainer) (Object) this;
        int relX = mouseX - gui.getGuiLeft();
        int relY = mouseY - gui.getGuiTop();
        if (GatingHudRenderer.handleClick(relX, relY)) {
            ci.cancel();
        }
    }

    /**
     * 檢查鐵砧附魔書上是否有被 gate 的附魔
     */
    private List<String> adversity$getBlockedAnvilEnchantments(GuiContainer gui) {
        List<String> blocked = new ArrayList<>();
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null)
            return blocked;

        if (gui.inventorySlots.inventorySlots.size() < 2)
            return blocked;

        Slot bookSlot = gui.inventorySlots.getSlot(1);
        if (!bookSlot.getHasStack())
            return blocked;

        ItemStack bookStack = bookSlot.getStack();
        if (!(bookStack.getItem() instanceof ItemEnchantedBook))
            return blocked;

        NBTTagList enchants = ItemEnchantedBook.getEnchantments(bookStack);
        for (int i = 0; i < enchants.tagCount(); i++) {
            NBTTagCompound tag = enchants.getCompoundTagAt(i);
            int id = tag.getShort("id");
            Enchantment ench = Enchantment.getEnchantmentByID(id);
            if (ench == null)
                continue;

            if (!EnchantmentGatingHelper.isEnchantmentAllowed(ench, player)) {
                blocked.add(I18n.format(ench.getName()));
            }
        }
        return blocked;
    }
}
