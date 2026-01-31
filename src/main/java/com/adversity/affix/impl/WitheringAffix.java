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
 * 凋零词条 (重构版) - 腐蚀叠层系统
 *
 * 机制：
 * 1. 攻击时给予目标"腐蚀"层数
 * 2. 每层腐蚀降低约16.7%护甲（通过Debuff系统实现）
 * 3. 腐蚀层数越高，持续伤害越强
 * 4. 满6层时触发"腐烂爆发"，造成大量伤害并扩散给附近玩家
 * 5. 周围存在腐蚀光环，靠近会逐渐腐蚀并破坏地面方块
 *
 * 视觉效果：
 * - 屏幕边缘紫黑色腐蚀纹理
 * - 绿色凋零条纹
 * - 满层时屏幕剧烈腐烂效果
 */
public class WitheringAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "withering");

    /** 攻击时添加的腐蚀层数 */
    private static final int BASE_STACKS_ON_HIT = 1;

    /** 每等级额外腐蚀层数 */
    private static final int STACKS_PER_TIER = 1;

    /** 腐蚀持续时间（tick）*/
    private static final int CORROSION_DURATION = 240;  // 12秒

    /** 腐蚀光环范围 */
    private static final double AURA_RANGE = 4.0;

    /** 光环叠加间隔（tick）*/
    private static final int AURA_INTERVAL = 60;

    /** 地面腐蚀范围 */
    private static final int DECAY_GROUND_RADIUS = 2;

    /** 腐烂爆发伤害 */
    private static final float DECAY_BURST_DAMAGE = 8.0f;

    /** 腐烂爆发扩散范围 */
    private static final double BURST_SPREAD_RANGE = 5.0;

    private static final Random RANDOM = new Random();

    public WitheringAffix() {
        super(
            ID,
            AffixType.OFFENSIVE,
            70,     // 中等权重
            5.0f    // 难度5以上出现（较强力）
        );
    }

    @Override
    public float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data) {
        if (!(target instanceof EntityPlayer)) {
            return damage;
        }

        EntityPlayer player = (EntityPlayer) target;
        int tier = getTier(attacker);

        // 检查饰品反制（腐蚀克星）
        float reduction = com.adversity.item.bauble.BaubleHelper.getEffectStrength(player, "withering");
        if (reduction >= 1.0f) {
            return damage; // 完全免疫
        }

        // 计算添加的腐蚀层数（根据反制减少）
        int stacksToAdd = BASE_STACKS_ON_HIT + (tier / 4 * STACKS_PER_TIER);
        if (reduction > 0) {
            stacksToAdd = Math.max(1, (int) (stacksToAdd * (1.0f - reduction)));
        }

        // 添加腐蚀debuff
        PlayerDebuffManager.addStacks(player, DebuffType.CORROSION, stacksToAdd, CORROSION_DURATION, attacker.getEntityId());


        // 重置衰减计时器
        PlayerDebuffManager.DebuffData debuffData = PlayerDebuffManager.getDebuff(player, DebuffType.CORROSION);
        if (debuffData != null) {
            debuffData.resetHitTimer();
        }

        // 发送视觉效果
        float intensity = PlayerDebuffManager.getIntensity(player, DebuffType.CORROSION);
        VisualEffectHelper.sendToTarget(target, attacker, VisualEffectType.DECAY, 60, intensity);

        // 播放凋零音效和粒子
        if (!attacker.world.isRemote) {
            attacker.world.playSound(
                null,
                target.posX, target.posY, target.posZ,
                SoundEvents.ENTITY_WITHER_AMBIENT,
                SoundCategory.HOSTILE,
                0.4f,
                1.2f + RANDOM.nextFloat() * 0.3f
            );

            // 凋零粒子
            if (attacker.world instanceof WorldServer) {
                ((WorldServer) attacker.world).spawnParticle(
                    EnumParticleTypes.SMOKE_NORMAL,
                    target.posX, target.posY + target.height / 2, target.posZ,
                    15,
                    0.3, 0.3, 0.3,
                    0.03
                );
                // 紫色粒子
                ((WorldServer) attacker.world).spawnParticle(
                    EnumParticleTypes.SPELL_WITCH,
                    target.posX, target.posY + target.height / 2, target.posZ,
                    8,
                    0.3, 0.3, 0.3,
                    0.02
                );
            }
        }

        // 满层时触发腐烂爆发
        int currentStacks = PlayerDebuffManager.getStacks(player, DebuffType.CORROSION);
        if (currentStacks >= DebuffType.CORROSION.getMaxStacks()) {
            triggerDecayBurst(player, attacker);
        }

        return damage;
    }

    @Override
    public void onTick(EntityLiving entity, IAffixData data) {
        if (entity.world.isRemote) return;

        int tickCount = data.getTickCount();

        // 腐蚀光环效果
        if (tickCount % AURA_INTERVAL == 0) {
            applyDecayAura(entity);
        }

        // 腐蚀地面
        if (tickCount % 80 == 0) {
            decayGround(entity);
        }

        // 每秒生成凋零粒子
        if (tickCount % 20 == 0 && entity.world instanceof WorldServer) {
            ((WorldServer) entity.world).spawnParticle(
                EnumParticleTypes.SPELL_WITCH,
                entity.posX, entity.posY + entity.height / 2, entity.posZ,
                4,
                0.4, 0.4, 0.4,
                0.01
            );
            ((WorldServer) entity.world).spawnParticle(
                EnumParticleTypes.SMOKE_NORMAL,
                entity.posX, entity.posY + 0.2, entity.posZ,
                3,
                0.3, 0.1, 0.3,
                0.005
            );
        }
    }

    /**
     * 腐蚀光环 - 靠近的玩家会积累腐蚀
     */
    private void applyDecayAura(EntityLiving entity) {
        int tier = getTier(entity);
        double range = AURA_RANGE + tier * 0.2;

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
            int auraStacks = distanceFactor > 0.5f ? 1 : 0;

            if (auraStacks > 0) {
                // 添加光环腐蚀
                PlayerDebuffManager.addStacks(player, DebuffType.CORROSION, auraStacks, 100, entity.getEntityId());

                // 发送轻微视觉效果
                float intensity = PlayerDebuffManager.getIntensity(player, DebuffType.CORROSION);
                VisualEffectHelper.sendToPlayer(player, VisualEffectType.DECAY, 30, intensity * 0.3f, entity.getEntityId());
            }
        }

        // 播放凋零音效
        if (!entity.world.isRemote && RANDOM.nextFloat() < 0.2f) {
            entity.world.playSound(
                null,
                entity.posX, entity.posY, entity.posZ,
                SoundEvents.ENTITY_WITHER_HURT,
                SoundCategory.HOSTILE,
                0.2f,
                0.3f
            );
        }
    }

    /**
     * 腐蚀地面 - 破坏周围的方块
     */
    private void decayGround(EntityLiving entity) {
        int tier = getTier(entity);
        int radius = DECAY_GROUND_RADIUS + tier / 5;

        BlockPos center = entity.getPosition();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius) continue;
                if (RANDOM.nextFloat() > 0.08f) continue;  // 8%几率

                BlockPos pos = center.add(x, -1, z);
                IBlockState state = entity.world.getBlockState(pos);

                // 将草变成泥土
                if (state.getBlock() == Blocks.GRASS) {
                    entity.world.setBlockState(pos, Blocks.DIRT.getDefaultState(), 2);
                }
                // 将泥土变成沙砾
                else if (state.getBlock() == Blocks.DIRT) {
                    entity.world.setBlockState(pos, Blocks.GRAVEL.getDefaultState(), 2);
                }
                // 破坏植物
                else if (state.getBlock() == Blocks.TALLGRASS ||
                         state.getBlock() == Blocks.YELLOW_FLOWER ||
                         state.getBlock() == Blocks.RED_FLOWER) {
                    entity.world.setBlockToAir(pos);
                }
                // 将树叶变成空气
                else if (state.getBlock() == Blocks.LEAVES || state.getBlock() == Blocks.LEAVES2) {
                    entity.world.setBlockToAir(pos);
                }
            }
        }
    }

    /**
     * 腐烂爆发 - 满层时触发
     */
    private void triggerDecayBurst(EntityPlayer player, EntityLiving source) {
        // 造成爆发伤害
        player.attackEntityFrom(
            new net.minecraft.util.DamageSource("adversity.decay_burst").setDamageBypassesArmor(),
            DECAY_BURST_DAMAGE
        );

        // 扩散腐蚀给附近玩家
        AxisAlignedBB area = player.getEntityBoundingBox().grow(BURST_SPREAD_RANGE);
        List<EntityPlayer> nearbyPlayers = player.world.getEntitiesWithinAABB(
            EntityPlayer.class, area,
            p -> p != null && p != player && p.isEntityAlive() && !p.isSpectator() && !p.isCreative()
        );

        for (EntityPlayer nearby : nearbyPlayers) {
            // 扩散一半的层数
            int spreadStacks = DebuffType.CORROSION.getMaxStacks() / 2;
            PlayerDebuffManager.addStacks(nearby, DebuffType.CORROSION, spreadStacks, CORROSION_DURATION / 2, source.getEntityId());
            VisualEffectHelper.sendToPlayer(nearby, VisualEffectType.DECAY, 40, 0.5f, source.getEntityId());
        }

        // 强烈的视觉效果
        VisualEffectHelper.sendToPlayer(player, VisualEffectType.DECAY, 60, 1.0f, source.getEntityId());

        // 播放爆发音效和粒子
        if (!player.world.isRemote) {
            player.world.playSound(
                null,
                player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_WITHER_SHOOT,
                SoundCategory.HOSTILE,
                1.0f,
                0.5f
            );

            if (player.world instanceof WorldServer) {
                // 大量凋零粒子
                ((WorldServer) player.world).spawnParticle(
                    EnumParticleTypes.SMOKE_LARGE,
                    player.posX, player.posY + player.height / 2, player.posZ,
                    40,
                    1.0, 0.8, 1.0,
                    0.1
                );
                ((WorldServer) player.world).spawnParticle(
                    EnumParticleTypes.SPELL_WITCH,
                    player.posX, player.posY + player.height / 2, player.posZ,
                    25,
                    1.0, 0.8, 1.0,
                    0.05
                );
            }
        }

        // 重置层数
        PlayerDebuffManager.setStacks(player, DebuffType.CORROSION, 2, CORROSION_DURATION / 2, source.getEntityId());
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
