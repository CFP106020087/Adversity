package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.List;

/**
 * 共生词条 - 伤害分摊给附近怪物
 *
 * 机制：
 * 1. 受到伤害时，30%转移给8格范围内的其他怪物
 * 2. 分摊的伤害平均分配
 * 3. 无附近怪物时无效果
 * 4. 设计理念：鼓励集火，惩罚分散输出
 */
public class SymbioticAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "symbiotic");

    /** 伤害转移比例 */
    private static final float TRANSFER_RATIO = 0.3f; // 30%

    /** 共生范围 */
    private static final double SYMBIOTIC_RANGE = 8.0;

    /** 最大分摊目标数 */
    private static final int MAX_TARGETS = 5;

    public SymbioticAffix() {
        super(
                ID,
                AffixType.DEFENSIVE,
                45, // 较低权重
                6.0f // 难度6以上出现
        );
    }

    @Override
    public float onHurt(EntityLiving entity, DamageSource source, float damage, IAffixData data) {
        // 防止共生伤害触发递归
        if ("adversity.symbiotic".equals(source.getDamageType())) {
            return damage;
        }

        int tier = getTier(entity);

        // 查找附近的怪物
        List<EntityLiving> nearbyMobs = findNearbyMobs(entity);
        if (nearbyMobs.isEmpty()) {
            return damage; // 无附近怪物，无效果
        }

        // 计算转移比例
        float transferRatio = TRANSFER_RATIO + (tier * 0.02f);
        transferRatio = Math.min(transferRatio, 0.5f); // 最多50%

        // 检查饰品反制（结界护符 - 抑制光环/共生效果）
        if (source.getTrueSource() instanceof net.minecraft.entity.player.EntityPlayer) {
            net.minecraft.entity.player.EntityPlayer player = (net.minecraft.entity.player.EntityPlayer) source
                    .getTrueSource();
            float auraReduction = com.adversity.item.bauble.BaubleHelper.getEffectStrength(player, "aura");
            if (auraReduction >= 1.0f) {
                return damage; // 完全禁止伤害分摊
            }
            // 根据反制降低转移比例
            transferRatio *= (1.0f - auraReduction);
        }

        // 计算转移伤害
        float transferDamage = damage * transferRatio;
        float damagePerMob = transferDamage / nearbyMobs.size();

        // 分摊伤害给附近怪物
        DamageSource symbioticDamage = new DamageSource("adversity.symbiotic")
                .setDamageBypassesArmor()
                .setDamageIsAbsolute();

        for (EntityLiving mob : nearbyMobs) {
            mob.attackEntityFrom(symbioticDamage, damagePerMob);
        }

        // 返回减少后的伤害
        return damage * (1.0f - transferRatio);
    }

    /**
     * 查找附近的怪物（不包括自己）
     */
    private List<EntityLiving> findNearbyMobs(EntityLiving entity) {
        AxisAlignedBB searchBox = entity.getEntityBoundingBox().grow(SYMBIOTIC_RANGE);

        List<EntityLiving> mobs = entity.world.getEntitiesWithinAABB(
                EntityLiving.class,
                searchBox,
                mob -> mob != entity &&
                        mob.isEntityAlive() &&
                        !mob.isNonBoss() == !entity.isNonBoss() // 相同boss状态
        );

        // 限制数量
        if (mobs.size() > MAX_TARGETS) {
            // 按距离排序，选择最近的
            mobs.sort((a, b) -> Double.compare(
                    a.getDistanceSq(entity),
                    b.getDistanceSq(entity)));
            return mobs.subList(0, MAX_TARGETS);
        }

        return mobs;
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
