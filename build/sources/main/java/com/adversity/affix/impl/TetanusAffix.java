package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.client.visual.VisualEffectHelper;
import com.adversity.client.visual.VisualEffectType;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.WorldServer;
import net.minecraft.util.EnumParticleTypes;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 破伤风词条 - 累计命中5次后扣除玩家一半生命
 *
 * 机制：
 * 1. 每次攻击玩家累计命中计数
 * 2. 达到5次后触发"破伤风发作"，扣除玩家当前生命的一半
 * 3. 计数有衰减机制（10秒不被打重置）
 * 4. 设计理念：鼓励hit-and-run战术
 */
public class TetanusAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "tetanus");

    /** 触发所需命中次数 */
    private static final int HITS_REQUIRED = 5;

    /** 命中计数衰减时间（tick） */
    private static final int DECAY_TIME = 200;  // 10秒

    /** 扣除生命比例 */
    private static final float HEALTH_DRAIN_RATIO = 0.5f;  // 50%

    /** 最小扣血量 */
    private static final float MIN_DAMAGE = 4.0f;

    /** 玩家命中计数 - Key: 玩家UUID, Value: [命中次数, 最后命中时间] */
    private static final Map<UUID, long[]> HIT_COUNTERS = new HashMap<>();

    public TetanusAffix() {
        super(
            ID,
            AffixType.OFFENSIVE,
            70,     // 中等权重
            3.0f    // 难度3以上
        );
    }

    @Override
    public float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data) {
        if (!(target instanceof EntityPlayer)) {
            return damage;
        }

        EntityPlayer player = (EntityPlayer) target;
        UUID playerId = player.getUniqueID();
        long currentTime = player.world.getTotalWorldTime();
        int tier = getTier(attacker);

        // 获取或创建计数器
        long[] counter = HIT_COUNTERS.get(playerId);
        if (counter == null) {
            counter = new long[]{0, currentTime};
            HIT_COUNTERS.put(playerId, counter);
        }

        // 检查衰减
        int decayTime = DECAY_TIME - (tier * 10);  // 每tier减少0.5秒衰减时间
        if (currentTime - counter[1] > decayTime) {
            // 超时，重置计数
            counter[0] = 0;
        }

        // 增加命中次数
        counter[0]++;
        counter[1] = currentTime;

        // 计算所需命中次数（tier越高所需次数越少）
        int requiredHits = Math.max(3, HITS_REQUIRED - (tier / 3));

        // 检查免疫徽章反制
        float immunityReduction = com.adversity.item.bauble.BaubleHelper.getReduction(player, "tetanus");
        if (immunityReduction > 0) {
            // 增加所需命中次数
            requiredHits = (int) (requiredHits / (1.0f - immunityReduction * 0.5f));
        }

        // 发送进度提示
        if (counter[0] < requiredHits) {
            // 发送警告视觉效果
            float intensity = (float) counter[0] / requiredHits;
            VisualEffectHelper.sendToPlayer(player, VisualEffectType.DECAY, 20, intensity * 0.5f, attacker.getEntityId());
        }

        // 检查是否触发
        if (counter[0] >= requiredHits) {
            triggerTetanus(player, attacker, tier);
            counter[0] = 0;  // 重置计数
        }

        return damage;
    }

    @Override
    public void onTick(EntityLiving entity, IAffixData data) {
        // 每5秒清理过期的计数器
        if (data.getTickCount() % 100 == 0) {
            cleanupCounters(entity.world.getTotalWorldTime());
        }
    }

    /**
     * 触发破伤风发作
     */
    private void triggerTetanus(EntityPlayer player, EntityLiving source, int tier) {
        // 计算伤害（当前生命的一半）
        float currentHealth = player.getHealth();
        float damage = Math.max(currentHealth * HEALTH_DRAIN_RATIO, MIN_DAMAGE);

        // 造成伤害
        player.attackEntityFrom(
            new net.minecraft.util.DamageSource("adversity.tetanus").setDamageBypassesArmor(),
            damage
        );

        // 强烈视觉效果
        VisualEffectHelper.sendToPlayer(player, VisualEffectType.DECAY, 60, 1.0f, source.getEntityId());

        // 音效和粒子
        if (!player.world.isRemote) {
            player.world.playSound(
                null,
                player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE,
                SoundCategory.HOSTILE,
                1.0f,
                0.5f
            );

            if (player.world instanceof WorldServer) {
                ((WorldServer) player.world).spawnParticle(
                    EnumParticleTypes.DAMAGE_INDICATOR,
                    player.posX, player.posY + player.height / 2, player.posZ,
                    20,
                    0.5, 0.5, 0.5,
                    0.1
                );
                ((WorldServer) player.world).spawnParticle(
                    EnumParticleTypes.SPELL_MOB,
                    player.posX, player.posY + 1, player.posZ,
                    30,
                    0.5, 0.5, 0.5,
                    0.0
                );
            }
        }
    }

    /**
     * 清理过期的计数器
     */
    private void cleanupCounters(long currentTime) {
        Iterator<Map.Entry<UUID, long[]>> it = HIT_COUNTERS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, long[]> entry = it.next();
            if (currentTime - entry.getValue()[1] > DECAY_TIME * 2) {
                it.remove();
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(IAffixData data) {
        NBTTagCompound nbt = new NBTTagCompound();
        // 计数器存储在静态Map中，不需要NBT序列化
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, IAffixData data) {
        // 不需要读取
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
