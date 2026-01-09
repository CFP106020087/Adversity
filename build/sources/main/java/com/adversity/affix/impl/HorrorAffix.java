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
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.WorldServer;

import java.util.List;
import java.util.Random;

/**
 * 恐惧词条 - 压迫性恐惧系统
 *
 * 机制：
 * 1. 周围存在恐惧光环，靠近会积累"恐惧"层数
 * 2. 每层恐惧增加20%恐惧强度
 * 3. 恐惧层数越高，强制后退力越强
 * 4. 满5层时触发"恐慌"，玩家短暂失去控制并被强制后退
 * 5. 恐惧光环范围随怪物等级增加
 * 6. 被攻击时快速叠加恐惧
 *
 * 视觉效果：
 * - 黑色呼吸脉冲覆盖层
 * - 心跳般的节奏闪烁
 * - 满层时屏幕剧烈压迫
 */
public class HorrorAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "horror");

    /** 攻击时添加的恐惧层数 */
    private static final int STACKS_ON_HIT = 2;

    /** 恐惧持续时间（tick）*/
    private static final int FEAR_DURATION = 200;  // 10秒

    /** 恐惧光环范围 */
    private static final double BASE_AURA_RANGE = 6.0;

    /** 每等级增加的光环范围 */
    private static final double RANGE_PER_TIER = 0.5;

    /** 光环叠加间隔（tick）*/
    private static final int AURA_INTERVAL = 30;

    /** 恐惧呼吸间隔（tick）*/
    private static final int BREATH_INTERVAL = 40;

    /** 强制后退力 */
    private static final double BASE_PUSH_FORCE = 0.2;

    /** 满层恐慌后退力 */
    private static final double PANIC_PUSH_FORCE = 0.6;

    private static final Random RANDOM = new Random();

    public HorrorAffix() {
        super(
            ID,
            AffixType.SPECIAL,
            50,     // 较低权重（特殊词条）
            4.0f    // 难度4以上出现
        );
    }

    @Override
    public float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data) {
        if (!(target instanceof EntityPlayer)) {
            return damage;
        }

        EntityPlayer player = (EntityPlayer) target;

        // 被攻击时快速叠加恐惧
        PlayerDebuffManager.addStacks(player, DebuffType.FEAR, STACKS_ON_HIT, FEAR_DURATION, attacker.getEntityId());

        // 重置衰减计时器
        PlayerDebuffManager.DebuffData debuffData = PlayerDebuffManager.getDebuff(player, DebuffType.FEAR);
        if (debuffData != null) {
            debuffData.resetHitTimer();
        }

        // 发送恐惧视觉效果
        float intensity = PlayerDebuffManager.getIntensity(player, DebuffType.FEAR);
        VisualEffectHelper.sendToPlayer(player, VisualEffectType.HORROR, 60, intensity, attacker.getEntityId());

        // 应用后退力
        applyPushBack(player, attacker, intensity);

        // 播放恐怖音效
        if (!attacker.world.isRemote) {
            attacker.world.playSound(
                null,
                target.posX, target.posY, target.posZ,
                SoundEvents.ENTITY_GHAST_SCREAM,
                SoundCategory.HOSTILE,
                0.5f,
                0.5f + RANDOM.nextFloat() * 0.2f
            );
        }

        // 满层时触发恐慌
        int currentStacks = PlayerDebuffManager.getStacks(player, DebuffType.FEAR);
        if (currentStacks >= DebuffType.FEAR.getMaxStacks()) {
            triggerPanic(player, attacker);
        }

        return damage;
    }

    @Override
    public void onTick(EntityLiving entity, IAffixData data) {
        if (entity.world.isRemote) return;

        int tickCount = data.getTickCount();

        // 恐惧光环效果
        if (tickCount % AURA_INTERVAL == 0) {
            applyFearAura(entity);
        }

        // 恐惧呼吸效果（心跳般的脉冲）
        if (tickCount % BREATH_INTERVAL == 0) {
            emitFearPulse(entity);
        }

        // 生成恐怖粒子
        if (tickCount % 15 == 0 && entity.world instanceof WorldServer) {
            // 暗色烟雾
            ((WorldServer) entity.world).spawnParticle(
                EnumParticleTypes.SMOKE_LARGE,
                entity.posX, entity.posY + entity.height / 2, entity.posZ,
                2,
                0.3, 0.3, 0.3,
                0.005
            );
            // 末影粒子
            ((WorldServer) entity.world).spawnParticle(
                EnumParticleTypes.SPELL_MOB,
                entity.posX + (RANDOM.nextDouble() - 0.5) * 2,
                entity.posY + RANDOM.nextDouble() * entity.height,
                entity.posZ + (RANDOM.nextDouble() - 0.5) * 2,
                0, 0.1, 0.1, 0.1, 1.0
            );
        }

        // 偶尔播放恐怖环境音效
        if (tickCount % 100 == 0 && RANDOM.nextFloat() < 0.3f) {
            entity.world.playSound(
                null,
                entity.posX, entity.posY, entity.posZ,
                SoundEvents.AMBIENT_CAVE,
                SoundCategory.HOSTILE,
                0.6f,
                0.3f + RANDOM.nextFloat() * 0.3f
            );
        }
    }

    /**
     * 恐惧光环 - 靠近的玩家会积累恐惧
     */
    private void applyFearAura(EntityLiving entity) {
        int tier = getTier(entity);
        double range = BASE_AURA_RANGE + tier * RANGE_PER_TIER;

        AxisAlignedBB area = entity.getEntityBoundingBox().grow(range);
        List<EntityPlayer> nearbyPlayers = entity.world.getEntitiesWithinAABB(
            EntityPlayer.class, area,
            p -> p != null && p.isEntityAlive() && !p.isSpectator() && !p.isCreative()
        );

        for (EntityPlayer player : nearbyPlayers) {
            double distance = entity.getDistance(player);
            if (distance > range) continue;

            // 距离越近，恐惧越强
            float distanceFactor = 1.0f - (float) (distance / range);
            int auraStacks = Math.max(1, (int) (distanceFactor * 2));

            // 添加光环恐惧
            PlayerDebuffManager.addStacks(player, DebuffType.FEAR, auraStacks, 80, entity.getEntityId());

            // 发送恐惧视觉效果
            float intensity = PlayerDebuffManager.getIntensity(player, DebuffType.FEAR);
            VisualEffectHelper.sendToPlayer(player, VisualEffectType.HORROR, AURA_INTERVAL + 10, intensity * 0.6f, entity.getEntityId());

            // 应用持续的后退力
            if (intensity > 0.2f) {
                applyPushBack(player, entity, intensity * 0.5f);
            }
        }
    }

    /**
     * 发出恐惧脉冲 - 心跳般的效果
     */
    private void emitFearPulse(EntityLiving entity) {
        int tier = getTier(entity);
        double range = BASE_AURA_RANGE + tier * RANGE_PER_TIER;

        AxisAlignedBB area = entity.getEntityBoundingBox().grow(range);
        List<EntityPlayer> nearbyPlayers = entity.world.getEntitiesWithinAABB(
            EntityPlayer.class, area,
            p -> p != null && p.isEntityAlive() && !p.isSpectator()
        );

        // 播放心跳音效
        if (!entity.world.isRemote) {
            entity.world.playSound(
                null,
                entity.posX, entity.posY, entity.posZ,
                SoundEvents.BLOCK_NOTE_BASEDRUM,
                SoundCategory.HOSTILE,
                1.0f,
                0.5f
            );
        }

        // 对所有附近玩家发送脉冲视觉
        for (EntityPlayer player : nearbyPlayers) {
            if (player.isCreative()) continue;

            float intensity = PlayerDebuffManager.getIntensity(player, DebuffType.FEAR);
            if (intensity > 0) {
                // 短促的强烈脉冲
                VisualEffectHelper.sendToPlayer(player, VisualEffectType.HORROR, 10, Math.min(1.0f, intensity + 0.3f), entity.getEntityId());
            }
        }

        // 生成脉冲粒子
        if (entity.world instanceof WorldServer) {
            ((WorldServer) entity.world).spawnParticle(
                EnumParticleTypes.SMOKE_NORMAL,
                entity.posX, entity.posY + entity.height / 2, entity.posZ,
                15,
                range * 0.3, 0.5, range * 0.3,
                0.01
            );
        }
    }

    /**
     * 应用后退力
     */
    private void applyPushBack(EntityPlayer player, EntityLiving source, float intensity) {
        double dx = player.posX - source.posX;
        double dz = player.posZ - source.posZ;
        double length = Math.sqrt(dx * dx + dz * dz);

        if (length > 0) {
            dx /= length;
            dz /= length;

            double force = BASE_PUSH_FORCE * intensity;
            player.motionX += dx * force;
            player.motionZ += dz * force;
            player.velocityChanged = true;
        }
    }

    /**
     * 触发恐慌 - 满层时
     */
    private void triggerPanic(EntityPlayer player, EntityLiving source) {
        // 强制后退
        double dx = player.posX - source.posX;
        double dz = player.posZ - source.posZ;
        double length = Math.sqrt(dx * dx + dz * dz);

        if (length > 0) {
            dx /= length;
            dz /= length;

            player.motionX = dx * PANIC_PUSH_FORCE;
            player.motionY = 0.3;  // 轻微弹起
            player.motionZ = dz * PANIC_PUSH_FORCE;
            player.velocityChanged = true;
        }

        // 短暂失明和反胃效果
        player.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 30, 0));  // 1.5秒失明
        player.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 60, 0));    // 3秒反胃

        // 强烈的恐惧视觉效果
        VisualEffectHelper.sendToPlayer(player, VisualEffectType.HORROR, 40, 1.0f, source.getEntityId());

        // 播放恐慌音效
        if (!player.world.isRemote) {
            player.world.playSound(
                null,
                player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_GHAST_SCREAM,
                SoundCategory.HOSTILE,
                1.0f,
                0.3f
            );
            player.world.playSound(
                null,
                player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_WITHER_SPAWN,
                SoundCategory.HOSTILE,
                0.5f,
                1.5f
            );

            // 恐慌粒子
            if (player.world instanceof WorldServer) {
                ((WorldServer) player.world).spawnParticle(
                    EnumParticleTypes.SMOKE_LARGE,
                    player.posX, player.posY + player.height / 2, player.posZ,
                    30,
                    0.5, 0.5, 0.5,
                    0.1
                );
            }
        }

        // 重置恐惧层数到一半
        PlayerDebuffManager.setStacks(player, DebuffType.FEAR,
            DebuffType.FEAR.getMaxStacks() / 2, FEAR_DURATION / 2, source.getEntityId());
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
