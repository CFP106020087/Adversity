package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;

/**
 * 分裂词条 - 死亡时分裂成多个较弱的副本
 *
 * 效果：死亡时生成2个较弱版本的自己（血量减半）
 * 分裂出的怪物不会再次分裂（防止无限循环）
 * 分裂出的怪物不掉落战利品
 */
public class SplittingAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "splitting");

    /** 分裂数量 */
    private static final int SPLIT_COUNT = 2;

    /** 分裂后血量倍率 */
    private static final float HEALTH_MULTIPLIER = 0.5f;

    /** 分裂后伤害倍率 */
    private static final float DAMAGE_MULTIPLIER = 0.7f;

    /** NBT标记：已分裂（防止无限分裂） */
    private static final String TAG_HAS_SPLIT = "adversity.hasSplit";

    public SplittingAffix() {
        super(
            ID,
            AffixType.SPECIAL,
            50,     // 较低权重（强力词条）
            6.0f    // 难度6以上才出现
        );
    }

    @Override
    public void onDeath(EntityLiving entity, DamageSource source, IAffixData data) {
        // 检查是否已经是分裂体（防止无限循环）
        if (entity.getEntityData().getBoolean(TAG_HAS_SPLIT)) {
            return;
        }

        World world = entity.world;
        if (world.isRemote) {
            return;
        }

        // 生成分裂体
        for (int i = 0; i < SPLIT_COUNT; i++) {
            try {
                spawnSplitCopy(entity, world, i);
            } catch (Exception e) {
                Adversity.LOGGER.warn("Failed to spawn split copy: {}", e.getMessage());
            }
        }

        // 播放音效
        world.playSound(
            null,
            entity.posX, entity.posY, entity.posZ,
            SoundEvents.ENTITY_SLIME_SQUISH,
            SoundCategory.HOSTILE,
            1.0f,
            0.8f
        );
    }

    /**
     * 生成分裂副本
     * 使用 EntityList 创建实体，比反射更安全
     */
    private void spawnSplitCopy(EntityLiving original, World world, int index) {
        // 获取原实体的注册名
        ResourceLocation entityId = EntityList.getKey(original);
        if (entityId == null) {
            Adversity.LOGGER.warn("Cannot split entity without registry name: {}", original.getClass().getName());
            return;
        }

        // 使用 EntityList 创建同类型实体（更安全的方式）
        Entity newEntity = EntityList.createEntityByIDFromName(entityId, world);
        if (!(newEntity instanceof EntityLiving)) {
            Adversity.LOGGER.warn("Failed to create split copy for: {}", entityId);
            return;
        }

        EntityLiving copy = (EntityLiving) newEntity;

        // 设置位置（轻微偏移）
        double offsetX = (index == 0 ? -1 : 1) * 0.5;
        double offsetZ = (world.rand.nextDouble() - 0.5) * 1.0;
        copy.setLocationAndAngles(
            original.posX + offsetX,
            original.posY,
            original.posZ + offsetZ,
            world.rand.nextFloat() * 360,
            0
        );

        // 标记为分裂体（不会再次分裂，不掉落战利品）
        copy.getEntityData().setBoolean(TAG_HAS_SPLIT, true);

        // 减少血量
        IAttributeInstance healthAttr = copy.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (healthAttr != null) {
            double newHealth = original.getMaxHealth() * HEALTH_MULTIPLIER;
            healthAttr.setBaseValue(Math.max(newHealth, 1.0));
        }
        copy.setHealth(copy.getMaxHealth());

        // 减少伤害
        IAttributeInstance damageAttr = copy.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        if (damageAttr != null) {
            double baseDamage = damageAttr.getBaseValue();
            damageAttr.setBaseValue(baseDamage * DAMAGE_MULTIPLIER);
        }

        // 复制仇恨目标
        EntityLivingBase target = original.getAttackTarget();
        if (target != null) {
            copy.setAttackTarget(target);
        }

        // 生成实体
        world.spawnEntity(copy);

        // 清除分裂体的 Adversity 能力（不应该有词条）
        IAdversityCapability cap = CapabilityHandler.getCapability(copy);
        if (cap != null) {
            cap.setProcessed(true);  // 标记已处理，不会获得词条
            cap.setTier(0);  // 普通等级
        }
    }

    @Override
    public boolean canApplyTo(EntityLiving entity) {
        // 不能应用于已经是分裂体的怪物
        if (entity.getEntityData().getBoolean(TAG_HAS_SPLIT)) {
            return false;
        }

        // 不能应用于 Boss 级生物
        if (!entity.isNonBoss()) {
            return false;
        }

        return super.canApplyTo(entity);
    }
}
