package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Random;

/**
 * 传送词条 - 受伤时有几率传送到附近
 *
 * 效果：受到伤害时有几率短距离传送，可以躲避连续攻击
 * 传送几率和距离随等级提升
 */
public class TeleportingAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "teleporting");

    /** 基础传送几率 */
    private static final float BASE_CHANCE = 0.2f;  // 20%

    /** 每等级增加的传送几率 */
    private static final float CHANCE_PER_TIER = 0.03f;  // +3%

    /** 基础传送距离 */
    private static final int BASE_DISTANCE = 4;

    /** 每等级增加的传送距离 */
    private static final int DISTANCE_PER_TIER = 1;

    /** 传送冷却时间（tick） */
    private static final int TELEPORT_COOLDOWN = 40;  // 2秒

    private static final Random RANDOM = new Random();

    public TeleportingAffix() {
        super(
            ID,
            AffixType.SPECIAL,  // 特殊型
            60,     // 较低权重
            5.0f    // 难度5以上才出现
        );
    }

    @Override
    public float onHurt(EntityLiving entity, DamageSource source, float damage, IAffixData data) {
        // 检查冷却
        if (data.getCooldown() > 0) {
            return damage;
        }

        int tier = getTier(entity);

        // 计算传送几率
        float teleportChance = BASE_CHANCE + (tier * CHANCE_PER_TIER);
        teleportChance = Math.min(teleportChance, 0.5f);  // 最多50%

        // 随机判定
        if (RANDOM.nextFloat() < teleportChance) {
            // 计算传送距离
            int maxDistance = BASE_DISTANCE + (tier * DISTANCE_PER_TIER);

            // 尝试传送
            if (tryTeleport(entity, maxDistance)) {
                // 设置冷却
                data.setCooldown(TELEPORT_COOLDOWN);

                // 播放音效
                if (!entity.world.isRemote) {
                    entity.world.playSound(
                        null,
                        entity.posX, entity.posY, entity.posZ,
                        SoundEvents.ENTITY_ENDERMEN_TELEPORT,
                        SoundCategory.HOSTILE,
                        0.8f,
                        1.0f
                    );
                }
            }
        }

        return damage;
    }

    @Override
    public void onTick(EntityLiving entity, IAffixData data) {
        // 冷却递减
        if (data.getCooldown() > 0) {
            data.decrementCooldown();
        }
    }

    /**
     * 尝试传送到附近安全位置
     */
    private boolean tryTeleport(EntityLiving entity, int maxDistance) {
        World world = entity.world;

        for (int attempts = 0; attempts < 16; attempts++) {
            // 随机目标位置
            double dx = (RANDOM.nextDouble() - 0.5) * 2 * maxDistance;
            double dy = (RANDOM.nextDouble() - 0.5) * maxDistance;
            double dz = (RANDOM.nextDouble() - 0.5) * 2 * maxDistance;

            double newX = entity.posX + dx;
            double newY = entity.posY + dy;
            double newZ = entity.posZ + dz;

            // 检查目标位置是否有效
            BlockPos targetPos = new BlockPos(newX, newY, newZ);

            if (isValidTeleportLocation(world, targetPos, entity)) {
                // 生成传送前粒子
                spawnTeleportParticles(world, entity.posX, entity.posY, entity.posZ);

                // 传送
                entity.setPositionAndUpdate(newX, newY, newZ);

                // 生成传送后粒子
                spawnTeleportParticles(world, newX, newY, newZ);

                return true;
            }
        }

        return false;
    }

    /**
     * 检查传送位置是否有效
     */
    private boolean isValidTeleportLocation(World world, BlockPos pos, EntityLiving entity) {
        // 检查下方是否有实心方块
        BlockPos below = pos.down();
        if (!world.getBlockState(below).isSideSolid(world, below, net.minecraft.util.EnumFacing.UP)) {
            return false;
        }

        // 检查目标位置是否有空间
        BlockPos above = pos.up();
        if (!world.isAirBlock(pos) || !world.isAirBlock(above)) {
            return false;
        }

        return true;
    }

    /**
     * 生成传送粒子
     */
    private void spawnTeleportParticles(World world, double x, double y, double z) {
        if (!world.isRemote && world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(
                EnumParticleTypes.PORTAL,
                x, y + 1, z,
                32,  // 数量
                0.5, 1.0, 0.5,  // 偏移
                0.1  // 速度
            );
        }
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
