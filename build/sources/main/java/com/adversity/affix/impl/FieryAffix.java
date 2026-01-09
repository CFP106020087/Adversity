package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.affix.IAffix;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.client.visual.VisualEffectHelper;
import com.adversity.client.visual.VisualEffectType;
import com.adversity.debuff.DebuffType;
import com.adversity.debuff.PlayerDebuffManager;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.util.List;
import java.util.Random;

/**
 * 烈焰词条 (重构版) - 灼烧叠层系统
 *
 * 机制：
 * 1. 攻击时给予目标"灼烧"层数
 * 2. 每层灼烧增加20%的灼烧强度
 * 3. 满5层时触发"火焰爆发"，造成大量伤害并清除层数
 * 4. 周围存在灼热光环，靠近会积累灼烧
 * 5. 怪物经过的地方会留下火焰痕迹
 *
 * 视觉效果：
 * - 屏幕边缘火焰红光
 * - 热浪扭曲效果
 * - 满层时屏幕剧烈燃烧
 *
 * 与冰霜词条互斥
 */
public class FieryAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "fiery");

    /** 攻击时添加的灼烧层数 */
    private static final int BASE_STACKS_ON_HIT = 1;

    /** 灼烧持续时间（tick）*/
    private static final int BURN_DURATION = 160;  // 8秒

    /** 灼热光环范围 */
    private static final double AURA_RANGE = 4.0;

    /** 光环叠加间隔（tick）*/
    private static final int AURA_INTERVAL = 30;

    /** 火焰痕迹间隔（tick）*/
    private static final int FIRE_TRAIL_INTERVAL = 20;

    /** 火焰爆发伤害 */
    private static final float BURST_DAMAGE = 8.0f;

    private static final Random RANDOM = new Random();

    public FieryAffix() {
        super(
            ID,
            AffixType.OFFENSIVE,
            100,    // 标准权重
            0.0f    // 无难度限制
        );
    }

    @Override
    public float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data) {
        if (!(target instanceof EntityPlayer)) {
            // 对非玩家目标：只点燃
            target.setFire(3 + getTier(attacker));
            return damage;
        }

        EntityPlayer player = (EntityPlayer) target;
        int tier = getTier(attacker);

        // 计算添加的灼烧层数
        int stacksToAdd = BASE_STACKS_ON_HIT + (tier / 3);

        // 添加灼烧debuff
        PlayerDebuffManager.addStacks(player, DebuffType.BURNING, stacksToAdd, BURN_DURATION, attacker.getEntityId());

        // 重置衰减计时器
        PlayerDebuffManager.DebuffData debuffData = PlayerDebuffManager.getDebuff(player, DebuffType.BURNING);
        if (debuffData != null) {
            debuffData.resetHitTimer();
        }

        // 点燃目标
        int burnSeconds = 3 + (tier / 2);
        target.setFire(burnSeconds);

        // 发送视觉效果
        float intensity = PlayerDebuffManager.getIntensity(player, DebuffType.BURNING);
        VisualEffectHelper.sendToTarget(target, attacker, VisualEffectType.BURNING, 40, intensity);

        // 播放火焰音效和粒子
        if (!attacker.world.isRemote) {
            attacker.world.playSound(
                null,
                target.posX, target.posY, target.posZ,
                SoundEvents.ENTITY_BLAZE_SHOOT,
                SoundCategory.HOSTILE,
                0.6f,
                1.0f + RANDOM.nextFloat() * 0.3f
            );

            if (attacker.world instanceof WorldServer) {
                ((WorldServer) attacker.world).spawnParticle(
                    EnumParticleTypes.FLAME,
                    target.posX, target.posY + target.height / 2, target.posZ,
                    15,
                    0.3, 0.3, 0.3,
                    0.05
                );
            }
        }

        // 满层时触发火焰爆发
        int currentStacks = PlayerDebuffManager.getStacks(player, DebuffType.BURNING);
        if (currentStacks >= DebuffType.BURNING.getMaxStacks()) {
            triggerFireBurst(player, attacker);
        }

        return damage;
    }

    @Override
    public void onTick(EntityLiving entity, IAffixData data) {
        if (entity.world.isRemote) return;

        // 烈焰怪物自身免疫火焰
        if (entity.isBurning()) {
            entity.extinguish();
        }

        int tickCount = data.getTickCount();

        // 灼热光环效果
        if (tickCount % AURA_INTERVAL == 0) {
            applyHeatAura(entity);
        }

        // 火焰痕迹
        if (tickCount % FIRE_TRAIL_INTERVAL == 0) {
            leaveFireTrail(entity);
        }

        // 每秒生成火焰粒子
        if (tickCount % 10 == 0 && entity.world instanceof WorldServer) {
            // 火焰粒子
            ((WorldServer) entity.world).spawnParticle(
                EnumParticleTypes.FLAME,
                entity.posX, entity.posY + entity.height / 2, entity.posZ,
                3,
                0.3, 0.3, 0.3,
                0.02
            );
            // 烟雾粒子
            ((WorldServer) entity.world).spawnParticle(
                EnumParticleTypes.SMOKE_NORMAL,
                entity.posX, entity.posY + entity.height, entity.posZ,
                2,
                0.2, 0.1, 0.2,
                0.01
            );
        }

        // 偶尔播放火焰环境音效
        if (tickCount % 60 == 0 && RANDOM.nextFloat() < 0.3f) {
            entity.world.playSound(
                null,
                entity.posX, entity.posY, entity.posZ,
                SoundEvents.BLOCK_FIRE_AMBIENT,
                SoundCategory.HOSTILE,
                0.5f,
                0.8f + RANDOM.nextFloat() * 0.4f
            );
        }
    }

    /**
     * 灼热光环 - 靠近的玩家会积累灼烧
     */
    private void applyHeatAura(EntityLiving entity) {
        int tier = getTier(entity);
        double range = AURA_RANGE + tier * 0.3;

        AxisAlignedBB area = entity.getEntityBoundingBox().grow(range);
        List<EntityPlayer> nearbyPlayers = entity.world.getEntitiesWithinAABB(
            EntityPlayer.class, area,
            p -> p != null && p.isEntityAlive() && !p.isSpectator() && !p.isCreative()
        );

        for (EntityPlayer player : nearbyPlayers) {
            double distance = entity.getDistance(player);
            if (distance > range) continue;

            // 距离越近，灼烧越强
            float distanceFactor = 1.0f - (float) (distance / range);

            if (distanceFactor > 0.3f) {
                int auraStacks = distanceFactor > 0.7f ? 2 : 1;

                // 添加光环灼烧
                PlayerDebuffManager.addStacks(player, DebuffType.BURNING, auraStacks, 60, entity.getEntityId());

                // 发送视觉效果
                float intensity = PlayerDebuffManager.getIntensity(player, DebuffType.BURNING);
                VisualEffectHelper.sendToPlayer(player, VisualEffectType.BURNING, 20, intensity * 0.4f, entity.getEntityId());

                // 轻微点燃
                if (!player.isBurning()) {
                    player.setFire(1);
                }
            }
        }
    }

    /**
     * 留下火焰痕迹
     */
    private void leaveFireTrail(EntityLiving entity) {
        int tier = getTier(entity);
        if (tier < 3 || RANDOM.nextFloat() > 0.4f) return;  // 低等级不留火焰

        BlockPos pos = entity.getPosition();
        IBlockState state = entity.world.getBlockState(pos);
        IBlockState belowState = entity.world.getBlockState(pos.down());

        // 在空气处放置火焰
        if (state.getBlock() == Blocks.AIR && belowState.isFullCube()) {
            entity.world.setBlockState(pos, Blocks.FIRE.getDefaultState(), 2);
        }
    }

    /**
     * 火焰爆发 - 满层时触发
     */
    private void triggerFireBurst(EntityPlayer player, EntityLiving source) {
        int tier = getTier(source);

        // 造成爆发伤害
        float damage = BURST_DAMAGE + tier * 0.5f;
        player.attackEntityFrom(
            new net.minecraft.util.DamageSource("adversity.fire_burst").setFireDamage(),
            damage
        );

        // 强烈点燃
        player.setFire(5 + tier);

        // 强烈的视觉效果
        VisualEffectHelper.sendToPlayer(player, VisualEffectType.BURNING, 40, 1.0f, source.getEntityId());

        // 播放爆发音效和粒子
        if (!player.world.isRemote) {
            player.world.playSound(
                null,
                player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.HOSTILE,
                0.8f,
                1.2f
            );
            player.world.playSound(
                null,
                player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_BLAZE_SHOOT,
                SoundCategory.HOSTILE,
                1.0f,
                0.8f
            );

            if (player.world instanceof WorldServer) {
                // 大量火焰粒子
                ((WorldServer) player.world).spawnParticle(
                    EnumParticleTypes.FLAME,
                    player.posX, player.posY + player.height / 2, player.posZ,
                    40,
                    0.8, 0.8, 0.8,
                    0.15
                );
                ((WorldServer) player.world).spawnParticle(
                    EnumParticleTypes.LAVA,
                    player.posX, player.posY + 1, player.posZ,
                    10,
                    0.5, 0.5, 0.5,
                    0.0
                );
            }

            // 在玩家周围生成火焰
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (RANDOM.nextFloat() > 0.5f) continue;
                    BlockPos pos = player.getPosition().add(dx, 0, dz);
                    if (player.world.isAirBlock(pos) &&
                        player.world.getBlockState(pos.down()).isFullCube()) {
                        player.world.setBlockState(pos, Blocks.FIRE.getDefaultState(), 2);
                    }
                }
            }
        }

        // 清除灼烧层数
        PlayerDebuffManager.removeDebuff(player, DebuffType.BURNING);
    }

    @Override
    public boolean isCompatibleWith(IAffix other) {
        // 与冰霜词条不兼容
        if (other.getId().equals(FrostyAffix.ID)) {
            return false;
        }
        return super.isCompatibleWith(other);
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
