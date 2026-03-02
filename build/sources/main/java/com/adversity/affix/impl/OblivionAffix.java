package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.WorldServer;

import java.util.Random;

/**
 * 遗忘词条 - 攻击时扣除玩家经验值
 *
 * 机制：
 * 1. 每次攻击玩家时扣除一定经验值
 * 2. 玩家等级越高，扣除越多
 * 3. 最低保留1级经验
 * 4. 设计理念：惩罚高等级玩家，增加紧张感
 */
public class OblivionAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "oblivion");

    /** 基础经验扣除量 */
    private static final int BASE_XP_DRAIN = 50;

    /** 最低保留等级 */
    private static final int MIN_LEVEL = 1;

    private static final Random RANDOM = new Random();

    public OblivionAffix() {
        super(
            ID,
            AffixType.UTILITY,
            75,     // 中等权重
            2.0f    // 难度2以上
        );
    }

    @Override
    public float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data) {
        if (!(target instanceof EntityPlayer)) {
            return damage;
        }

        EntityPlayer player = (EntityPlayer) target;
        int tier = getTier(attacker);

        // 检查玩家等级
        if (player.experienceLevel <= MIN_LEVEL) {
            return damage;
        }

        // 计算经验扣除量
        int xpDrain = calculateXpDrain(player, tier);

        // 检查记忆水晶反制
        float memoryReduction = com.adversity.item.bauble.BaubleHelper.getReduction(player, "oblivion");
        if (memoryReduction >= 1.0f) {
            // 完全保护，不扣除经验
            return damage;
        } else if (memoryReduction > 0) {
            // 部分保护，减少扣除量
            xpDrain = (int) (xpDrain * (1.0f - memoryReduction));
        }

        // 扣除经验
        drainExperience(player, xpDrain);

        // 播放效果
        playDrainEffects(player, attacker);

        return damage;
    }

    /**
     * 计算经验扣除量
     */
    private int calculateXpDrain(EntityPlayer player, int tier) {
        // 基础扣除 + 玩家等级加成
        int drain = BASE_XP_DRAIN + (player.experienceLevel * 5);

        // tier加成
        drain = (int) (drain * (1.0f + tier * 0.2f));

        // 随机波动 (80%-120%)
        drain = (int) (drain * (0.8f + RANDOM.nextFloat() * 0.4f));

        return drain;
    }

    /**
     * 扣除玩家经验
     */
    private void drainExperience(EntityPlayer player, int amount) {
        // 计算扣除后的经验
        int totalXp = getTotalExperience(player);
        int newTotalXp = Math.max(getExperienceForLevel(MIN_LEVEL), totalXp - amount);

        // 应用新经验值
        player.experienceTotal = newTotalXp;
        player.experienceLevel = getLevelForExperience(newTotalXp);
        player.experience = (float) (newTotalXp - getExperienceForLevel(player.experienceLevel))
            / (float) player.xpBarCap();
    }

    /**
     * 获取玩家总经验值
     */
    private int getTotalExperience(EntityPlayer player) {
        return getExperienceForLevel(player.experienceLevel)
            + (int) (player.experience * player.xpBarCap());
    }

    /**
     * 获取达到指定等级所需的总经验值
     */
    private int getExperienceForLevel(int level) {
        if (level <= 0) return 0;
        if (level <= 16) {
            return level * level + 6 * level;
        } else if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            return (int) (4.5 * level * level - 162.5 * level + 2220);
        }
    }

    /**
     * 根据总经验值计算等级
     */
    private int getLevelForExperience(int xp) {
        int level = 0;
        while (getExperienceForLevel(level + 1) <= xp) {
            level++;
        }
        return level;
    }

    /**
     * 播放扣除效果
     */
    private void playDrainEffects(EntityPlayer player, EntityLiving source) {
        if (player.world.isRemote) return;

        // 音效
        player.world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
            SoundCategory.HOSTILE,
            0.5f,
            0.5f  // 低音调表示失去经验
        );

        // 粒子效果（反向经验球效果）
        if (player.world instanceof WorldServer) {
            ((WorldServer) player.world).spawnParticle(
                EnumParticleTypes.SPELL_WITCH,
                player.posX, player.posY + 1, player.posZ,
                15,
                0.5, 0.5, 0.5,
                0.0
            );
        }
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
