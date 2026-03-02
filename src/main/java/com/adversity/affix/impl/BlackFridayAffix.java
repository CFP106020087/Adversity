package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.client.visual.VisualEffectHelper;
import com.adversity.client.visual.VisualEffectType;
import com.adversity.curse.PermanentCurseManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.WorldServer;

import java.util.Random;

/**
 * 黑色星期五词条 - 永久削减玩家最大生命值
 *
 * 机制：
 * 1. 被攻击时有概率永久削减半格血
 * 2. 有可配置下限，最低保留一定生命值
 * 3. 若生命值归零，永久ban玩家进入该存档
 * 4. 设计理念：末日系永久诅咒
 * 5. 可通过赎罪物品恢复
 */
public class BlackFridayAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "black_friday");

    /** 基础触发概率 */
    private static final float BASE_CHANCE = 0.08f;  // 8%

    private static final Random RANDOM = new Random();

    public BlackFridayAffix() {
        super(
            ID,
            AffixType.SPECIAL,
            10,     // 极低权重（非常危险）
            8.0f    // 难度8以上
        );
    }

    @Override
    public float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data) {
        if (!(target instanceof EntityPlayer)) {
            return damage;
        }

        EntityPlayer player = (EntityPlayer) target;
        int tier = getTier(attacker);

        // 检查守护之心饰品反制（curse类型）
        if (com.adversity.item.bauble.BaubleHelper.tryBlockEffect(player, "curse", attacker)) {
            return damage;
        }

        // 计算触发概率
        float chance = BASE_CHANCE + (tier * 0.015f);

        if (RANDOM.nextFloat() < chance) {
            applyCurse(player, attacker);
        }

        return damage;
    }

    /**
     * 应用黑色星期五诅咒
     */
    private void applyCurse(EntityPlayer player, EntityLiving attacker) {
        PermanentCurseManager manager = PermanentCurseManager.get(player.world);

        // 增加诅咒
        boolean shouldBan = manager.addBlackFridayCurse(player);

        // 播放效果
        playCurseEffects(player, attacker);

        // 发送视觉效果
        VisualEffectHelper.sendToPlayer(player, VisualEffectType.BLOOD_MARK, 60, 0.9f, attacker.getEntityId());

        // 如果应该ban玩家
        if (shouldBan && player instanceof EntityPlayerMP) {
            // 延迟踢出，让玩家看到效果
            player.world.getMinecraftServer().addScheduledTask(() -> {
                manager.kickBannedPlayer((EntityPlayerMP) player);
            });
        }

        Adversity.LOGGER.info("Black Friday curse applied to player {}. Current reduction: {} hearts",
            player.getName(), manager.getBlackFridayReduction(player) / 2);
    }

    /**
     * 播放诅咒效果
     */
    private void playCurseEffects(EntityPlayer player, EntityLiving attacker) {
        if (player.world.isRemote) return;

        // 心碎音效
        player.world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            SoundEvents.ENTITY_WITHER_HURT,
            SoundCategory.HOSTILE,
            0.8f,
            0.6f
        );

        player.world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            SoundEvents.BLOCK_GLASS_BREAK,
            SoundCategory.HOSTILE,
            0.5f,
            0.5f
        );

        // 血色粒子效果
        if (player.world instanceof WorldServer) {
            ((WorldServer) player.world).spawnParticle(
                EnumParticleTypes.DAMAGE_INDICATOR,
                player.posX, player.posY + 1, player.posZ,
                30,
                0.5, 0.5, 0.5,
                0.1
            );
            ((WorldServer) player.world).spawnParticle(
                EnumParticleTypes.REDSTONE,
                player.posX, player.posY + 1, player.posZ,
                50,
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
