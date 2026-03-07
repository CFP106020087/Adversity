package com.adversity.sanctuary.ritual;

import com.adversity.sanctuary.TileEntitySanctuary;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Map;

/**
 * 儀式特殊效果接口
 *
 * 每種效果獨立實作，通過 RitualEffectRegistry 註冊。
 * Rite 的 effect_params（來自 JSON/CRT）作為參數傳入。
 */
public interface IRitualEffect {

    /**
     * 執行儀式效果
     *
     * @param player    觸發儀式的玩家
     * @param rite      儀式定義
     * @param sanctuary 聖所方塊實體
     * @param params    效果參數（來自 JSON effect_params 或 CRT）
     */
    void apply(EntityPlayer player, Rite rite, TileEntitySanctuary sanctuary, Map<String, Object> params);

    /**
     * 效果描述（用於 JEI / GUI tooltip）
     */
    String getDescription();
}
