package com.adversity.mixin;

import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.sanctuary.StageGatingRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * JEI 配方動態 per-player 過濾
 *
 * Mixin 進 JEI 的 RecipeRegistry.getRecipeWrappers()。
 * JEI 每次顯示配方列表都會調用此方法 → 我們在 RETURN 攔截，
 * 根據當前客戶端玩家的 stage 過濾掉被 gate 的配方。
 *
 * 注意：JEI 是第三方 mod，方法名不會 obfuscate，無需雙名稱。
 * remap = false 因為 JEI 不在 vanilla mapping 中。
 */
@Mixin(value = mezz.jei.recipes.RecipeRegistry.class, remap = false)
public class MixinJeiRecipeRegistry {

    /**
     * 過濾帶 IFocus 的查詢（搜索、點擊物品時觸發）
     */
    @Inject(method = "getRecipeWrappers(Lmezz/jei/api/recipe/IRecipeCategory;Lmezz/jei/api/recipe/IFocus;)Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private <T extends mezz.jei.api.recipe.IRecipeWrapper, V> void adversity$filterGatedWithFocus(
            mezz.jei.api.recipe.IRecipeCategory<T> category,
            mezz.jei.api.recipe.IFocus<V> focus,
            CallbackInfoReturnable<List<T>> cir) {
        adversity$filterGatedRecipes(cir);
    }

    /**
     * 過濾無 IFocus 的查詢（瀏覽整個分類時觸發）
     */
    @Inject(method = "getRecipeWrappers(Lmezz/jei/api/recipe/IRecipeCategory;)Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private <T extends mezz.jei.api.recipe.IRecipeWrapper> void adversity$filterGatedNoFocus(
            mezz.jei.api.recipe.IRecipeCategory<T> category,
            CallbackInfoReturnable<List<T>> cir) {
        adversity$filterGatedRecipes(cir);
    }

    /**
     * 共用過濾邏輯：遍歷配方的 output，檢查是否被當前玩家的 stage gate。
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void adversity$filterGatedRecipes(CallbackInfoReturnable cir) {
        List<?> wrappers = (List<?>) cir.getReturnValue();
        if (wrappers == null || wrappers.isEmpty())
            return;

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null)
            return;

        IAdversityCapability.IProgression progression = player.getCapability(
                CapabilityHandler.PROGRESSION_CAPABILITY, null);

        // 如果沒有 capability → 不過濾（安全降級）
        if (progression == null)
            return;

        // 如果沒有任何 item stage 設定 → 跳過（性能優化）
        if (StageGatingRegistry.getItemStageCount() == 0)
            return;

        List<Object> filtered = null;

        for (Object wrapper : wrappers) {
            if (adversity$isWrapperGated(wrapper, progression)) {
                if (filtered == null) {
                    // 延遲建立 — 只有真的需要過濾時才複製
                    filtered = new ArrayList<>(wrappers);
                }
                filtered.remove(wrapper);
            }
        }

        if (filtered != null) {
            cir.setReturnValue(filtered);
        }
    }

    /**
     * 檢查一個 IRecipeWrapper 的 output 是否被 gate。
     *
     * 透過反射調用 getIngredients() 取得 outputs，
     * 如果任何 output 被 StageGatingRegistry gate 且玩家不滿足 → 返回 true。
     */
    private boolean adversity$isWrapperGated(Object wrapper,
            IAdversityCapability.IProgression progression) {
        try {
            // 取得 outputs: 調用 wrapper.getIngredients(ingredients) 然後讀取 outputs
            // 更簡單的方式：如果 wrapper 包裝了 IRecipe，直接拿 recipeOutput
            if (wrapper instanceof mezz.jei.api.recipe.IRecipeWrapper) {
                // 嘗試直接從 vanilla wrapper 取 recipe output
                List<ItemStack> outputs = adversity$extractOutputs(
                        (mezz.jei.api.recipe.IRecipeWrapper) wrapper);
                for (ItemStack output : outputs) {
                    if (!output.isEmpty()) {
                        Set<String> required = StageGatingRegistry.getItemStageRequirements(output);
                        if (required != null) {
                            if (!StageGatingRegistry.playerMeetsItemRequirements(progression, output)) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 安全降級 — 不過濾
        }
        return false;
    }

    /**
     * 從 IRecipeWrapper 提取 output ItemStacks。
     * 使用 JEI 的 Ingredients API。
     */
    @SuppressWarnings("unchecked")
    private List<ItemStack> adversity$extractOutputs(mezz.jei.api.recipe.IRecipeWrapper wrapper) {
        List<ItemStack> result = new ArrayList<>();
        try {
            // 使用 JEI 內部的 Ingredients 實現
            mezz.jei.api.ingredients.IIngredients ingredients = adversity$createIngredients();
            if (ingredients == null)
                return result;

            wrapper.getIngredients(ingredients);

            List<List<ItemStack>> outputLists = ingredients.getOutputs(ItemStack.class);
            if (outputLists != null) {
                for (List<ItemStack> list : outputLists) {
                    if (list != null) {
                        result.addAll(list);
                    }
                }
            }
        } catch (Exception e) {
            // 降級
        }
        return result;
    }

    /**
     * 建立 JEI Ingredients 實例（透過反射）。
     */
    private mezz.jei.api.ingredients.IIngredients adversity$createIngredients() {
        try {
            // mezz.jei.ingredients.Ingredients 實作 IIngredients
            Class<?> clazz = Class.forName("mezz.jei.ingredients.Ingredients");
            return (mezz.jei.api.ingredients.IIngredients) clazz.newInstance();
        } catch (Exception e) {
            return null;
        }
    }
}
