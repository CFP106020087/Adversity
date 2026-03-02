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
 * 致盲词条 (重构版) - 黑暗叠层系统
 *
 * 机制：
 * 1. 攻击时给予目标"黑暗"层数
 * 2. 每层黑暗增加10%的视野遮蔽
 * 3. 满10层时视野严重受限，并迷失方向
 * 4. 黑暗层数不被攻击时会缓慢自然衰减
 * 5. 周围存在黑暗光环，靠近也会积累少量黑暗层数
 *
 * 视觉效果：
 * - 自定义屏幕暗角渲染
 * - 动态收缩的视野
 * - 边缘黑暗噪声纹理
 */
public class BlindingAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "blinding");

    /** 攻击时添加的黑暗层数 */
    private static final int BASE_STACKS_ON_HIT = 2;

    /** 每等级额外黑暗层数 */
    private static final int STACKS_PER_TIER = 1;

    /** 黑暗持续时间（tick）*/
    private static final int DARKNESS_DURATION = 200;  // 10秒

    /** 光环范围 */
    private static final double AURA_RANGE = 5.0;

    /** 光环叠加间隔（tick）*/
    private static final int AURA_INTERVAL = 40;  // 2秒

    private static final Random RANDOM = new Random();

    public BlindingAffix() {
        super(
            ID,
            AffixType.UTILITY,
            80,     // 中等权重
            2.0f    // 难度2以上出现
        );
    }

    @Override
    public float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data) {
        if (!(target instanceof EntityPlayer)) {
            return damage;
        }

        EntityPlayer player = (EntityPlayer) target;
        int tier = getTier(attacker);

        // 检查饰品反制（结界护符 - 光环系统；净化徽章 - 叠层系统；澄明之眼 - 致盲专攻）
        float auraReduction = com.adversity.item.bauble.BaubleHelper.getEffectStrength(player, "aura");
        float stackReduction = com.adversity.item.bauble.BaubleHelper.getEffectStrength(player, "stack_system");
        float blindReduction = com.adversity.item.bauble.BaubleHelper.getEffectStrength(player, "blinding");
        float totalReduction = Math.max(auraReduction, Math.max(stackReduction, blindReduction));
        if (totalReduction >= 1.0f) {
            return damage; // 完全免疫
        }

        // 计算添加的黑暗层数（根据反制减少）
        int stacksToAdd = BASE_STACKS_ON_HIT + (tier / 3 * STACKS_PER_TIER);
        if (totalReduction > 0) {
            stacksToAdd = Math.max(1, (int) (stacksToAdd * (1.0f - totalReduction)));
        }

        // 添加黑暗debuff
        PlayerDebuffManager.addStacks(player, DebuffType.DARKNESS, stacksToAdd, DARKNESS_DURATION, attacker.getEntityId());


        // 重置衰减计时器
        PlayerDebuffManager.DebuffData debuffData = PlayerDebuffManager.getDebuff(player, DebuffType.DARKNESS);
        if (debuffData != null) {
            debuffData.resetHitTimer();
        }

        // 立即发送视觉效果
        float intensity = PlayerDebuffManager.getIntensity(player, DebuffType.DARKNESS);
        VisualEffectHelper.sendToTarget(target, attacker, VisualEffectType.BLINDING, 40, intensity);

        // 播放音效
        if (!attacker.world.isRemote) {
            attacker.world.playSound(
                null,
                target.posX, target.posY, target.posZ,
                SoundEvents.ENTITY_SQUID_AMBIENT,
                SoundCategory.HOSTILE,
                0.6f,
                0.5f + RANDOM.nextFloat() * 0.3f
            );

            // 生成墨汁粒子
            if (attacker.world instanceof WorldServer) {
                ((WorldServer) attacker.world).spawnParticle(
                    EnumParticleTypes.SMOKE_LARGE,
                    target.posX, target.posY + target.height / 2, target.posZ,
                    15,
                    0.3, 0.3, 0.3,
                    0.02
                );
            }
        }

        // 满层时额外效果：短暂迷失方向
        int currentStacks = PlayerDebuffManager.getStacks(player, DebuffType.DARKNESS);
        if (currentStacks >= DebuffType.DARKNESS.getMaxStacks()) {
            applyFullBlindnessEffect(player, attacker);
        }

        return damage;
    }

    @Override
    public void onTick(EntityLiving entity, IAffixData data) {
        if (entity.world.isRemote) return;

        int tickCount = data.getTickCount();

        // 黑暗光环效果
        if (tickCount % AURA_INTERVAL == 0) {
            applyDarknessAura(entity);
        }

        // 每秒生成黑暗粒子
        if (tickCount % 20 == 0 && entity.world instanceof WorldServer) {
            ((WorldServer) entity.world).spawnParticle(
                EnumParticleTypes.SMOKE_NORMAL,
                entity.posX, entity.posY + entity.height / 2, entity.posZ,
                5,
                0.5, 0.5, 0.5,
                0.01
            );
        }
    }

    /**
     * 黑暗光环 - 靠近的玩家也会积累黑暗
     */
    private void applyDarknessAura(EntityLiving entity) {
        int tier = getTier(entity);
        double range = AURA_RANGE + tier * 0.5;

        AxisAlignedBB area = entity.getEntityBoundingBox().grow(range);
        List<EntityPlayer> nearbyPlayers = entity.world.getEntitiesWithinAABB(
            EntityPlayer.class, area,
            p -> p != null && p.isEntityAlive() && !p.isSpectator() && !p.isCreative()
        );

        for (EntityPlayer player : nearbyPlayers) {
            double distance = entity.getDistance(player);
            if (distance > range) continue;

            // 距离越近，叠加越多
            float distanceFactor = 1.0f - (float) (distance / range);
            int auraStacks = Math.max(1, (int) (distanceFactor * 2));

            // 添加光环黑暗
            PlayerDebuffManager.addStacks(player, DebuffType.DARKNESS, auraStacks, 60, entity.getEntityId());

            // 发送轻微视觉效果
            float intensity = PlayerDebuffManager.getIntensity(player, DebuffType.DARKNESS);
            VisualEffectHelper.sendToPlayer(player, VisualEffectType.BLINDING, 20, intensity * 0.5f, entity.getEntityId());
        }
    }

    /**
     * 满层黑暗效果 - 迷失方向
     */
    private void applyFullBlindnessEffect(EntityPlayer player, EntityLiving source) {
        // 随机改变玩家视角（迷失方向）
        float yawChange = (RANDOM.nextFloat() - 0.5f) * 60;
        float pitchChange = (RANDOM.nextFloat() - 0.5f) * 30;
        player.rotationYaw += yawChange;
        player.rotationPitch += pitchChange;
        player.rotationPitch = Math.max(-90, Math.min(90, player.rotationPitch));

        // 强烈的视觉效果
        VisualEffectHelper.sendToPlayer(player, VisualEffectType.BLINDING, 60, 1.0f, source.getEntityId());

        // 播放恐怖音效
        if (!player.world.isRemote) {
            player.world.playSound(
                null,
                player.posX, player.posY, player.posZ,
                SoundEvents.AMBIENT_CAVE,
                SoundCategory.HOSTILE,
                1.0f,
                0.5f
            );
        }
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
