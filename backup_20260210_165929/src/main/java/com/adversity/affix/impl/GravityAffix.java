package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.WorldServer;

import java.util.List;

/**
 * 引力词条 - 周期性将附近玩家拉向自己
 *
 * 效果：每隔一段时间，将附近玩家向怪物方向拉动
 * 范围和拉力随等级提升
 */
public class GravityAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "gravity");

    /** 拉力触发间隔（tick） */
    private static final int PULL_INTERVAL = 60;  // 3秒

    /** 基础拉力范围 */
    private static final double BASE_RANGE = 6.0;

    /** 每等级增加的范围 */
    private static final double RANGE_PER_TIER = 1.0;

    /** 基础拉力强度 */
    private static final double BASE_PULL_STRENGTH = 0.15;

    /** 每等级增加的拉力 */
    private static final double PULL_PER_TIER = 0.03;

    public GravityAffix() {
        super(
            ID,
            AffixType.UTILITY,
            60,     // 中等偏低权重
            4.0f    // 难度4以上出现
        );
    }

    @Override
    public void onTick(EntityLiving entity, IAffixData data) {
        // 只在服务端处理
        if (entity.world.isRemote) return;

        int tickCount = data.getTickCount();
        if (tickCount % PULL_INTERVAL != 0) return;

        int tier = getTier(entity);

        // 计算范围和拉力
        double range = BASE_RANGE + (tier * RANGE_PER_TIER);
        double pullStrength = BASE_PULL_STRENGTH + (tier * PULL_PER_TIER);
        pullStrength = Math.min(pullStrength, 0.4);  // 限制最大拉力

        // 查找范围内的玩家
        AxisAlignedBB area = entity.getEntityBoundingBox().grow(range);
        List<EntityPlayer> nearbyPlayers = entity.world.getEntitiesWithinAABB(
            EntityPlayer.class, area,
            p -> p != null && p.isEntityAlive() && !p.isSpectator() && !p.isCreative()
        );

        if (nearbyPlayers.isEmpty()) return;

        boolean pulled = false;
        for (EntityPlayer player : nearbyPlayers) {
            double distance = entity.getDistance(player);
            if (distance < 1.5) continue;  // 太近不拉

            // 检查饰品反制（锚石项链）
            float reduction = com.adversity.item.bauble.BaubleHelper.getEffectStrength(player, "gravity");
            if (reduction >= 1.0f) {
                continue; // 完全免疫
            }

            // 计算拉力方向
            double dx = entity.posX - player.posX;
            double dy = entity.posY - player.posY;
            double dz = entity.posZ - player.posZ;

            // 归一化
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length > 0) {
                dx /= length;
                dy /= length;
                dz /= length;
            }

            // 应用拉力（根据距离衰减，并考虑反制减少）
            double distanceFactor = 1.0 - (distance / range);
            double actualPull = pullStrength * distanceFactor * (1.0 - reduction);

            player.motionX += dx * actualPull;
            player.motionY += dy * actualPull * 0.5;  // 垂直方向减弱
            player.motionZ += dz * actualPull;
            player.velocityChanged = true;

            pulled = true;
        }


        // 播放音效和粒子
        if (pulled && entity.world instanceof WorldServer) {
            entity.world.playSound(
                null,
                entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_ENDERMEN_TELEPORT,
                SoundCategory.HOSTILE,
                0.5f,
                0.5f
            );

            // 生成粒子效果
            ((WorldServer) entity.world).spawnParticle(
                EnumParticleTypes.PORTAL,
                entity.posX, entity.posY + entity.height / 2, entity.posZ,
                20,  // 数量
                range * 0.3, 1.0, range * 0.3,  // 偏移
                0.05  // 速度
            );
        }
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
