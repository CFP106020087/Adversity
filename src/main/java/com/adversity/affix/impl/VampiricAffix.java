package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.client.visual.VisualEffectHelper;
import com.adversity.client.visual.VisualEffectType;
import com.adversity.debuff.DebuffType;
import com.adversity.debuff.PlayerDebuffManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.WorldServer;

import java.util.List;
import java.util.Random;

/**
 * 吸血词条 (重构版) - 血债链接系统
 *
 * 机制：
 * 1. 攻击时给予目标"血债"层数
 * 2. 每层血债增加25%的额外吸血效果
 * 3. 满4层时建立"血链"，持续吸取生命
 * 4. 血债目标受到的吸血效果大幅增强
 * 5. 低血量时会进入狂暴模式，大幅增加吸血
 *
 * 视觉效果：
 * - 血红色边缘闪烁
 * - 血滴效果
 * - 满层时显示血链连接
 */
public class VampiricAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "vampiric");

    /** 基础吸血比例 */
    private static final float BASE_LIFESTEAL = 0.20f;  // 20%

    /** 每等级增加的吸血比例 */
    private static final float LIFESTEAL_PER_TIER = 0.03f;  // +3%

    /** 血债增加的额外吸血 */
    private static final float BLOOD_DEBT_BONUS = 0.25f;  // 每层+25%

    /** 血债持续时间（tick）*/
    private static final int BLOOD_DEBT_DURATION = 200;  // 10秒

    /** 狂暴阈值（生命百分比）*/
    private static final float FRENZY_THRESHOLD = 0.3f;  // 30%血量以下

    /** 狂暴吸血加成 */
    private static final float FRENZY_BONUS = 0.5f;  // +50%

    /** 血链吸血间隔（tick）*/
    private static final int BLOOD_CHAIN_INTERVAL = 40;  // 2秒

    /** 血链吸血伤害 */
    private static final float BLOOD_CHAIN_DAMAGE = 2.0f;

    private static final Random RANDOM = new Random();

    public VampiricAffix() {
        super(
            ID,
            AffixType.DEFENSIVE,
            80,     // 中等权重
            3.0f    // 难度3以上才出现
        );
    }

    @Override
    public float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data) {
        int tier = getTier(attacker);

        // 计算基础吸血比例
        float lifestealRatio = BASE_LIFESTEAL + (tier * LIFESTEAL_PER_TIER);

        // 检查狂暴模式
        boolean isFrenzy = attacker.getHealth() / attacker.getMaxHealth() < FRENZY_THRESHOLD;
        if (isFrenzy) {
            lifestealRatio += FRENZY_BONUS;
        }

        // 如果目标是玩家，处理血债系统
        if (target instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) target;

            // 添加血债层数
            PlayerDebuffManager.addStacks(player, DebuffType.BLOOD_DEBT, 1, BLOOD_DEBT_DURATION, attacker.getEntityId());

            // 重置衰减计时器
            PlayerDebuffManager.DebuffData debuffData = PlayerDebuffManager.getDebuff(player, DebuffType.BLOOD_DEBT);
            if (debuffData != null) {
                debuffData.resetHitTimer();
            }

            // 根据血债层数增加吸血
            int bloodDebtStacks = PlayerDebuffManager.getStacks(player, DebuffType.BLOOD_DEBT);
            lifestealRatio += bloodDebtStacks * BLOOD_DEBT_BONUS;

            // 发送视觉效果
            float intensity = PlayerDebuffManager.getIntensity(player, DebuffType.BLOOD_DEBT);
            VisualEffectHelper.sendToPlayer(player, VisualEffectType.BLOOD_MARK, 40, intensity, attacker.getEntityId());

            // 满层时建立血链
            if (bloodDebtStacks >= DebuffType.BLOOD_DEBT.getMaxStacks()) {
                establishBloodChain(player, attacker);
            }
        }

        // 限制最大吸血
        lifestealRatio = Math.min(lifestealRatio, 1.0f);

        // 计算回复量
        float healAmount = damage * lifestealRatio;

        // 回复生命
        if (healAmount > 0) {
            attacker.heal(healAmount);

            // 生成吸血粒子效果
            if (!attacker.world.isRemote && attacker.world instanceof WorldServer) {
                // 从目标到怪物的粒子流
                double dx = attacker.posX - target.posX;
                double dy = attacker.posY - target.posY;
                double dz = attacker.posZ - target.posZ;

                for (int i = 0; i < 5; i++) {
                    double progress = i / 5.0;
                    double x = target.posX + dx * progress;
                    double y = target.posY + target.height / 2 + dy * progress;
                    double z = target.posZ + dz * progress;

                    ((WorldServer) attacker.world).spawnParticle(
                        EnumParticleTypes.REDSTONE,
                        x, y, z,
                        1,
                        0.1, 0.1, 0.1,
                        0.0
                    );
                }

                // 怪物身上的回复指示
                ((WorldServer) attacker.world).spawnParticle(
                    EnumParticleTypes.HEART,
                    attacker.posX, attacker.posY + attacker.height, attacker.posZ,
                    isFrenzy ? 3 : 1,
                    0.3, 0.2, 0.3,
                    0.0
                );
            }

            // 播放吸血音效
            if (!attacker.world.isRemote && RANDOM.nextFloat() < 0.4f) {
                attacker.world.playSound(
                    null,
                    attacker.posX, attacker.posY, attacker.posZ,
                    SoundEvents.ENTITY_GENERIC_DRINK,
                    SoundCategory.HOSTILE,
                    0.4f,
                    0.6f + RANDOM.nextFloat() * 0.2f
                );
            }

            // 狂暴模式音效
            if (isFrenzy && RANDOM.nextFloat() < 0.3f) {
                attacker.world.playSound(
                    null,
                    attacker.posX, attacker.posY, attacker.posZ,
                    SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED,
                    SoundCategory.HOSTILE,
                    0.3f,
                    1.5f
                );
            }
        }

        return damage;
    }

    @Override
    public void onTick(EntityLiving entity, IAffixData data) {
        if (entity.world.isRemote) return;

        int tickCount = data.getTickCount();

        // 血链吸血
        if (tickCount % BLOOD_CHAIN_INTERVAL == 0) {
            drainBloodChainTargets(entity);
        }

        // 低血量时发出红光
        boolean isFrenzy = entity.getHealth() / entity.getMaxHealth() < FRENZY_THRESHOLD;

        // 生成吸血粒子
        if (tickCount % 20 == 0 && entity.world instanceof WorldServer) {
            EnumParticleTypes particleType = isFrenzy ? EnumParticleTypes.REDSTONE : EnumParticleTypes.SPELL_WITCH;
            ((WorldServer) entity.world).spawnParticle(
                particleType,
                entity.posX, entity.posY + entity.height / 2, entity.posZ,
                isFrenzy ? 5 : 2,
                0.3, 0.3, 0.3,
                0.0
            );
        }

        // 狂暴模式特效
        if (isFrenzy && tickCount % 10 == 0 && entity.world instanceof WorldServer) {
            ((WorldServer) entity.world).spawnParticle(
                EnumParticleTypes.SMOKE_NORMAL,
                entity.posX, entity.posY + entity.height, entity.posZ,
                3,
                0.2, 0.1, 0.2,
                0.02
            );
        }
    }

    /**
     * 建立血链 - 满层血债时
     */
    private void establishBloodChain(EntityPlayer player, EntityLiving source) {
        // 播放建立连接的音效
        if (!player.world.isRemote) {
            player.world.playSound(
                null,
                player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_ENDERMEN_TELEPORT,
                SoundCategory.HOSTILE,
                0.6f,
                0.5f
            );

            // 强烈的血链粒子效果
            if (player.world instanceof WorldServer) {
                double dx = source.posX - player.posX;
                double dy = source.posY - player.posY;
                double dz = source.posZ - player.posZ;

                for (int i = 0; i < 20; i++) {
                    double progress = i / 20.0;
                    double x = player.posX + dx * progress;
                    double y = player.posY + player.height / 2 + dy * progress;
                    double z = player.posZ + dz * progress;

                    ((WorldServer) player.world).spawnParticle(
                        EnumParticleTypes.REDSTONE,
                        x, y, z,
                        2,
                        0.05, 0.05, 0.05,
                        0.0
                    );
                }
            }
        }

        // 强烈的视觉效果
        VisualEffectHelper.sendToPlayer(player, VisualEffectType.BLOOD_MARK, 60, 1.0f, source.getEntityId());

        // 不清除血债，保持连接状态
    }

    /**
     * 吸取血链目标的生命
     */
    private void drainBloodChainTargets(EntityLiving entity) {
        int tier = getTier(entity);
        double range = 10.0 + tier;

        AxisAlignedBB area = entity.getEntityBoundingBox().grow(range);
        List<EntityPlayer> nearbyPlayers = entity.world.getEntitiesWithinAABB(
            EntityPlayer.class, area,
            p -> p != null && p.isEntityAlive() && !p.isSpectator() && !p.isCreative()
        );

        for (EntityPlayer player : nearbyPlayers) {
            int bloodDebtStacks = PlayerDebuffManager.getStacks(player, DebuffType.BLOOD_DEBT);

            // 只吸取满层血债的目标
            if (bloodDebtStacks >= DebuffType.BLOOD_DEBT.getMaxStacks()) {
                // 造成伤害
                float damage = BLOOD_CHAIN_DAMAGE + tier * 0.3f;
                player.attackEntityFrom(
                    new net.minecraft.util.DamageSource("adversity.blood_chain").setDamageBypassesArmor(),
                    damage
                );

                // 回复生命
                entity.heal(damage);

                // 发送视觉效果
                VisualEffectHelper.sendToPlayer(player, VisualEffectType.BLOOD_MARK, 20, 0.7f, entity.getEntityId());

                // 生成连接粒子
                if (entity.world instanceof WorldServer) {
                    double dx = entity.posX - player.posX;
                    double dy = entity.posY - player.posY;
                    double dz = entity.posZ - player.posZ;

                    for (int i = 0; i < 10; i++) {
                        double progress = i / 10.0;
                        double x = player.posX + dx * progress;
                        double y = player.posY + player.height / 2 + dy * progress;
                        double z = player.posZ + dz * progress;

                        ((WorldServer) entity.world).spawnParticle(
                            EnumParticleTypes.REDSTONE,
                            x, y, z,
                            1,
                            0.02, 0.02, 0.02,
                            0.0
                        );
                    }
                }

                // 播放吸血音效
                entity.world.playSound(
                    null,
                    player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_GENERIC_DRINK,
                    SoundCategory.HOSTILE,
                    0.5f,
                    0.5f
                );
            }
        }
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
