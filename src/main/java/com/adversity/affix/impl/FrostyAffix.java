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
import net.minecraft.block.Block;
import net.minecraft.block.BlockSnow;
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
 * 冰霜词条 (重构版) - 冻结叠层系统
 *
 * 机制：
 * 1. 攻击时给予目标"冻结"层数
 * 2. 每层冻结降低12.5%移动速度（通过Debuff系统实现）
 * 3. 满8层时短暂完全冻结（无法移动1秒）
 * 4. 周围存在寒霜光环，靠近会积累冻结并留下霜冻地面
 * 5. 冻结层数会随时间自然衰减
 *
 * 视觉效果：
 * - 屏幕边缘冰晶纹理
 * - 蓝色色调叠加
 * - 满层时屏幕完全冰封
 *
 * 与烈焰词条互斥
 */
public class FrostyAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "frosty");

    /** 攻击时添加的冻结层数 */
    private static final int BASE_STACKS_ON_HIT = 2;

    /** 每3等级额外冻结层数 */
    private static final int STACKS_PER_TIER = 1;

    /** 冻结持续时间（tick）*/
    private static final int FROST_DURATION = 160;  // 8秒

    /** 寒霜光环范围 */
    private static final double AURA_RANGE = 4.0;

    /** 光环叠加间隔（tick）*/
    private static final int AURA_INTERVAL = 30;

    /** 地面冰冻范围 */
    private static final int FREEZE_GROUND_RADIUS = 3;

    private static final Random RANDOM = new Random();

    public FrostyAffix() {
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
            return damage;
        }

        EntityPlayer player = (EntityPlayer) target;
        int tier = getTier(attacker);

        // 计算添加的冻结层数
        int stacksToAdd = BASE_STACKS_ON_HIT + (tier / 3 * STACKS_PER_TIER);

        // 添加冻结debuff
        PlayerDebuffManager.addStacks(player, DebuffType.FROST, stacksToAdd, FROST_DURATION, attacker.getEntityId());

        // 重置衰减计时器
        PlayerDebuffManager.DebuffData debuffData = PlayerDebuffManager.getDebuff(player, DebuffType.FROST);
        if (debuffData != null) {
            debuffData.resetHitTimer();
        }

        // 发送视觉效果
        float intensity = PlayerDebuffManager.getIntensity(player, DebuffType.FROST);
        VisualEffectHelper.sendToTarget(target, attacker, VisualEffectType.FROZEN, 40, intensity);

        // 播放冰冻音效和粒子
        if (!attacker.world.isRemote) {
            attacker.world.playSound(
                null,
                target.posX, target.posY, target.posZ,
                SoundEvents.BLOCK_GLASS_BREAK,
                SoundCategory.HOSTILE,
                0.6f,
                1.2f + RANDOM.nextFloat() * 0.3f
            );

            // 冰霜粒子
            if (attacker.world instanceof WorldServer) {
                ((WorldServer) attacker.world).spawnParticle(
                    EnumParticleTypes.SNOWBALL,
                    target.posX, target.posY + target.height / 2, target.posZ,
                    20,
                    0.4, 0.4, 0.4,
                    0.05
                );
            }
        }

        // 满层时完全冻结
        int currentStacks = PlayerDebuffManager.getStacks(player, DebuffType.FROST);
        if (currentStacks >= DebuffType.FROST.getMaxStacks()) {
            applyFullFreezeEffect(player, attacker);
        }

        return damage;
    }

    @Override
    public void onTick(EntityLiving entity, IAffixData data) {
        if (entity.world.isRemote) return;

        int tickCount = data.getTickCount();

        // 寒霜光环效果
        if (tickCount % AURA_INTERVAL == 0) {
            applyFrostAura(entity);
        }

        // 冻结地面
        if (tickCount % 40 == 0) {
            freezeGround(entity);
        }

        // 每秒生成冰霜粒子
        if (tickCount % 10 == 0 && entity.world instanceof WorldServer) {
            ((WorldServer) entity.world).spawnParticle(
                EnumParticleTypes.SNOW_SHOVEL,
                entity.posX, entity.posY + entity.height / 2, entity.posZ,
                3,
                0.4, 0.4, 0.4,
                0.02
            );
        }
    }

    @Override
    public void onApply(EntityLiving entity, IAffixData data) {
        // 冰霜怪物免疫火焰
        entity.setFire(0);
    }

    /**
     * 寒霜光环 - 靠近的玩家会积累冻结
     */
    private void applyFrostAura(EntityLiving entity) {
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

            // 距离越近，叠加越多
            float distanceFactor = 1.0f - (float) (distance / range);
            int auraStacks = Math.max(1, (int) (distanceFactor * 1.5));

            // 添加光环冻结
            PlayerDebuffManager.addStacks(player, DebuffType.FROST, auraStacks, 60, entity.getEntityId());

            // 发送轻微视觉效果
            float intensity = PlayerDebuffManager.getIntensity(player, DebuffType.FROST);
            VisualEffectHelper.sendToPlayer(player, VisualEffectType.FROZEN, 20, intensity * 0.4f, entity.getEntityId());
        }

        // 播放寒风音效
        if (!entity.world.isRemote && RANDOM.nextFloat() < 0.3f) {
            entity.world.playSound(
                null,
                entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_PLAYER_BREATH,
                SoundCategory.HOSTILE,
                0.4f,
                0.4f
            );
        }
    }

    /**
     * 冻结地面 - 在怪物周围生成冰/雪
     */
    private void freezeGround(EntityLiving entity) {
        int tier = getTier(entity);
        int radius = FREEZE_GROUND_RADIUS + tier / 4;

        BlockPos center = entity.getPosition();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius) continue;
                if (RANDOM.nextFloat() > 0.15f) continue;  // 15%几率

                BlockPos pos = center.add(x, 0, z);
                BlockPos below = pos.down();
                BlockPos groundPos = pos;

                // 找到地面
                for (int y = 0; y >= -2; y--) {
                    BlockPos check = pos.add(0, y, 0);
                    IBlockState state = entity.world.getBlockState(check);
                    if (state.isFullCube()) {
                        groundPos = check.up();
                        break;
                    }
                }

                IBlockState currentState = entity.world.getBlockState(groundPos);
                IBlockState belowState = entity.world.getBlockState(groundPos.down());

                // 将水变成冰
                if (belowState.getBlock() == Blocks.WATER || belowState.getBlock() == Blocks.FLOWING_WATER) {
                    entity.world.setBlockState(groundPos.down(), Blocks.ICE.getDefaultState(), 2);
                }
                // 在空气处放置雪
                else if (currentState.getBlock() == Blocks.AIR && belowState.isFullCube()) {
                    entity.world.setBlockState(groundPos, Blocks.SNOW_LAYER.getDefaultState(), 2);
                }
            }
        }
    }

    /**
     * 满层冻结效果 - 完全冻结
     */
    private void applyFullFreezeEffect(EntityPlayer player, EntityLiving source) {
        // 完全停止移动
        player.motionX = 0;
        player.motionY = Math.min(player.motionY, 0);  // 保留下落
        player.motionZ = 0;
        player.velocityChanged = true;

        // 强烈的视觉效果
        VisualEffectHelper.sendToPlayer(player, VisualEffectType.FROZEN, 30, 1.0f, source.getEntityId());

        // 冰冻音效
        if (!player.world.isRemote) {
            player.world.playSound(
                null,
                player.posX, player.posY, player.posZ,
                SoundEvents.BLOCK_GLASS_PLACE,
                SoundCategory.HOSTILE,
                1.0f,
                0.5f
            );

            // 大量冰霜粒子
            if (player.world instanceof WorldServer) {
                ((WorldServer) player.world).spawnParticle(
                    EnumParticleTypes.SNOWBALL,
                    player.posX, player.posY + player.height / 2, player.posZ,
                    40,
                    0.5, 0.8, 0.5,
                    0.1
                );
            }
        }

        // 重置层数到一半（不完全清除）
        PlayerDebuffManager.setStacks(player, DebuffType.FROST,
            DebuffType.FROST.getMaxStacks() / 2, FROST_DURATION / 2, source.getEntityId());
    }

    @Override
    public boolean isCompatibleWith(IAffix other) {
        // 与烈焰词条不兼容
        if (other.getId().equals(FieryAffix.ID)) {
            return false;
        }
        return super.isCompatibleWith(other);
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
