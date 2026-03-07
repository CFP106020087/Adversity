package com.adversity.potion;

import com.adversity.Adversity;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;

/**
 * 負面效果免疫藥水
 *
 * 持有此藥水效果的玩家免疫所有負面藥水的施加。
 * 攔截邏輯在 PotionImmunityHandler 通過 PotionEvent.PotionApplicableEvent 實現。
 */
public class PotionNegativeImmunity extends Potion {

    public static final PotionNegativeImmunity INSTANCE = new PotionNegativeImmunity();

    protected PotionNegativeImmunity() {
        super(false, 0x00FFDD); // 正面效果，青綠色
        setPotionName("effect.adversity.negative_immunity");
        setRegistryName(new ResourceLocation(Adversity.MODID, "negative_immunity"));
    }

    @Override
    public boolean isReady(int duration, int amplifier) {
        return false; // 不需要 tick 邏輯，純標記用
    }
}
