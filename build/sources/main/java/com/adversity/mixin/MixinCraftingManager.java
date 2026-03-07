package com.adversity.mixin;

import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.sanctuary.StageGatingRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;

/**
 * 攔截 CraftingManager.findMatchingRecipe
 *
 * 1.12.2 中此方法返回 IRecipe（非 ItemStack）。
 * 當 recipe output 被 Stage Gate 限制時，返回 null（無匹配配方）。
 */
@Mixin(value = net.minecraft.item.crafting.CraftingManager.class, remap = false)
public class MixinCraftingManager {

    @Inject(method = { "findMatchingRecipe", "func_192413_b" }, at = @At("RETURN"), cancellable = true)
    private static void adversity$blockGatedRecipes(InventoryCrafting craftMatrix, World worldIn,
            CallbackInfoReturnable<IRecipe> cir) {
        IRecipe recipe = cir.getReturnValue();
        if (recipe == null)
            return;

        // 取得配方的 output
        ItemStack result = recipe.getRecipeOutput();
        if (result.isEmpty())
            return;

        // 查 stage gating
        Set<String> requiredStages = StageGatingRegistry.getItemStageRequirements(result);
        if (requiredStages == null)
            return;

        // 反查 player
        EntityPlayer player = adversity$findPlayerFromCraftMatrix(craftMatrix);
        if (player == null)
            return; // 取不到 → 放行

        // 查 progression
        IAdversityCapability.IProgression progression = player.getCapability(
                CapabilityHandler.PROGRESSION_CAPABILITY, null);

        if (!StageGatingRegistry.playerMeetsItemRequirements(progression, result)) {
            cir.setReturnValue(null); // null = 無匹配配方 → output slot 為空
        }
    }

    private static EntityPlayer adversity$findPlayerFromCraftMatrix(InventoryCrafting craftMatrix) {
        try {
            Container container = adversity$getEventHandler(craftMatrix);
            if (container == null)
                return null;

            List<IContainerListener> listeners = adversity$getListeners(container);
            if (listeners == null)
                return null;

            for (IContainerListener listener : listeners) {
                if (listener instanceof EntityPlayer) {
                    return (EntityPlayer) listener;
                }
            }
        } catch (Exception e) {
            // 安全降級
        }
        return null;
    }

    private static Container adversity$getEventHandler(InventoryCrafting craftMatrix) {
        try {
            java.lang.reflect.Field f = InventoryCrafting.class.getDeclaredField("field_70465_c");
            f.setAccessible(true);
            return (Container) f.get(craftMatrix);
        } catch (NoSuchFieldException e) {
            try {
                java.lang.reflect.Field f = InventoryCrafting.class.getDeclaredField("eventHandler");
                f.setAccessible(true);
                return (Container) f.get(craftMatrix);
            } catch (Exception e2) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<IContainerListener> adversity$getListeners(Container container) {
        try {
            java.lang.reflect.Field f = Container.class.getDeclaredField("field_177758_a");
            f.setAccessible(true);
            return (List<IContainerListener>) f.get(container);
        } catch (NoSuchFieldException e) {
            try {
                java.lang.reflect.Field f = Container.class.getDeclaredField("listeners");
                f.setAccessible(true);
                return (List<IContainerListener>) f.get(container);
            } catch (Exception e2) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
