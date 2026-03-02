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
 * 黑天鹅词条 - 永久削减玩家攻击力
 *
 * 机制：
 * 1. 被攻击时有概率永久削减5%攻击力
 * 2. 有可配置上限，超过后依然有效
 * 3. 若攻击力归零，永久ban玩家进入该存档
 * 4. 设计理念：末日系永久诅咒
 * 5. 可通过赎罪物品恢复
 */
public class BlackSwanAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "black_swan");

    /** 基础触发概率 */
    private static final float BASE_CHANCE = 0.1f;  // 10%

    private static final Random RANDOM = new Random();

    public BlackSwanAffix() {
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
        float chance = BASE_CHANCE + (tier * 0.02f);

        if (RANDOM.nextFloat() < chance) {
            applyCurse(player, attacker);
        }

        return damage;
    }

    /**
     * 应用黑天鹅诅咒
     */
    private void applyCurse(EntityPlayer player, EntityLiving attacker) {
        PermanentCurseManager manager = PermanentCurseManager.get(player.world);

        // 增加诅咒
        boolean shouldBan = manager.addBlackSwanCurse(player);

        // 播放效果
        playCurseEffects(player, attacker);

        // 发送视觉效果
        VisualEffectHelper.sendToPlayer(player, VisualEffectType.HORROR, 60, 0.8f, attacker.getEntityId());

        // 如果应该ban玩家
        if (shouldBan && player instanceof EntityPlayerMP) {
            // 延迟踢出，让玩家看到效果
            player.world.getMinecraftServer().addScheduledTask(() -> {
                manager.kickBannedPlayer((EntityPlayerMP) player);
            });
        }

        Adversity.LOGGER.info("Black Swan curse applied to player {}. Current reduction: {}%",
            player.getName(), (int)(manager.getBlackSwanReduction(player) * 100));
    }

    /**
     * 播放诅咒效果
     */
    private void playCurseEffects(EntityPlayer player, EntityLiving attacker) {
        if (player.world.isRemote) return;

        // 不祥的音效
        player.world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            SoundEvents.ENTITY_WITHER_AMBIENT,
            SoundCategory.HOSTILE,
            0.8f,
            0.5f
        );

        player.world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE,
            SoundCategory.HOSTILE,
            1.0f,
            0.8f
        );

        // 黑暗粒子效果
        if (player.world instanceof WorldServer) {
            ((WorldServer) player.world).spawnParticle(
                EnumParticleTypes.SMOKE_LARGE,
                player.posX, player.posY + 1, player.posZ,
                50,
                0.5, 0.5, 0.5,
                0.05
            );
            ((WorldServer) player.world).spawnParticle(
                EnumParticleTypes.SPELL_WITCH,
                player.posX, player.posY + 1, player.posZ,
                30,
                0.3, 0.3, 0.3,
                0.0
            );
        }
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
