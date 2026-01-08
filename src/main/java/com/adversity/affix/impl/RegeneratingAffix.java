package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;

/**
 * 再生词条 - 持续回复生命值
 *
 * 效果：每秒回复最大生命值的一定百分比
 * 回复率随等级提升
 */
public class RegeneratingAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "regenerating");

    /** 回复间隔（tick） */
    private static final int REGEN_INTERVAL = 20;  // 每秒

    /** 基础回复比例（每秒） */
    private static final float BASE_REGEN = 0.01f;  // 1%

    /** 每等级增加的回复比例 */
    private static final float REGEN_PER_TIER = 0.005f;  // +0.5%

    public RegeneratingAffix() {
        super(
            ID,
            AffixType.DEFENSIVE,
            80,     // 中等权重
            3.0f    // 难度3以上才出现
        );
    }

    @Override
    public void onTick(EntityLiving entity, IAffixData data) {
        // 使用内部计数器控制回复频率
        int tickCount = data.getTickCount();

        if (tickCount % REGEN_INTERVAL == 0) {
            // 检查是否需要回复
            if (entity.getHealth() < entity.getMaxHealth()) {
                int tier = getTier(entity);

                // 计算回复比例
                float regenRatio = BASE_REGEN + (tier * REGEN_PER_TIER);
                regenRatio = Math.min(regenRatio, 0.05f);  // 最多5%每秒

                // 计算回复量
                float healAmount = entity.getMaxHealth() * regenRatio;

                // 回复生命
                entity.heal(healAmount);

                // 生成粒子效果
                if (!entity.world.isRemote && entity.world instanceof WorldServer) {
                    ((WorldServer) entity.world).spawnParticle(
                        EnumParticleTypes.HEART,
                        entity.posX, entity.posY + entity.height + 0.2, entity.posZ,
                        1,  // 数量
                        0.3, 0.1, 0.3,  // 偏移
                        0.0  // 速度
                    );
                }
            }
        }

        // 注意：tick计数由 MobEventHandler 统一调用 incrementTick() 管理
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
