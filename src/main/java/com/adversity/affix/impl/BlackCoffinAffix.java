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
 * 黑棺词条 - 永久封印玩家背包槽位
 *
 * 机制：
 * 1. 被攻击时有概率永久封印一个背包槽位
 * 2. 封印槽位显示X覆盖层，无法使用
 * 3. 有可配置上限，超过后依然有效
 * 4. 若所有槽位被封印，永久ban玩家进入该存档
 * 5. 设计理念：末日系永久诅咒
 * 6. 可通过赎罪物品恢复
 */
public class BlackCoffinAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "black_coffin");

    /** 基础触发概率 */
    private static final float BASE_CHANCE = 0.06f;  // 6%

    private static final Random RANDOM = new Random();

    public BlackCoffinAffix() {
        super(
            ID,
            AffixType.SPECIAL,
            10,     // 极低权重（非常危险）
            9.0f    // 难度9以上
        );
    }

    @Override
    public float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data) {
        if (!(target instanceof EntityPlayer)) {
            return damage;
        }

        EntityPlayer player = (EntityPlayer) target;
        int tier = getTier(attacker);

        // 计算触发概率
        float chance = BASE_CHANCE + (tier * 0.01f);

        if (RANDOM.nextFloat() < chance) {
            applyCurse(player, attacker);
        }

        return damage;
    }

    /**
     * 应用黑棺诅咒
     */
    private void applyCurse(EntityPlayer player, EntityLiving attacker) {
        PermanentCurseManager manager = PermanentCurseManager.get(player.world);

        // 增加诅咒
        boolean shouldBan = manager.addBlackCoffinCurse(player);

        // 播放效果
        playCurseEffects(player, attacker);

        // 发送视觉效果
        VisualEffectHelper.sendToPlayer(player, VisualEffectType.VOID_GAZE, 60, 0.9f, attacker.getEntityId());

        // 如果应该ban玩家
        if (shouldBan && player instanceof EntityPlayerMP) {
            // 延迟踢出，让玩家看到效果
            player.world.getMinecraftServer().addScheduledTask(() -> {
                manager.kickBannedPlayer((EntityPlayerMP) player);
            });
        }

        Adversity.LOGGER.info("Black Coffin curse applied to player {}. Sealed slots: {}/{}",
            player.getName(),
            manager.getBlackCoffinSealed(player),
            PermanentCurseManager.BLACK_COFFIN_MAX_SLOTS);
    }

    /**
     * 播放诅咒效果
     */
    private void playCurseEffects(EntityPlayer player, EntityLiving attacker) {
        if (player.world.isRemote) return;

        // 棺材关闭音效
        player.world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            SoundEvents.BLOCK_IRON_DOOR_CLOSE,
            SoundCategory.HOSTILE,
            1.0f,
            0.3f
        );

        player.world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            SoundEvents.ENTITY_WITHER_SPAWN,
            SoundCategory.HOSTILE,
            0.3f,
            0.5f
        );

        // 黑暗封印粒子效果
        if (player.world instanceof WorldServer) {
            ((WorldServer) player.world).spawnParticle(
                EnumParticleTypes.SMOKE_LARGE,
                player.posX, player.posY + 1, player.posZ,
                40,
                0.5, 0.5, 0.5,
                0.02
            );
            ((WorldServer) player.world).spawnParticle(
                EnumParticleTypes.PORTAL,
                player.posX, player.posY + 1, player.posZ,
                30,
                0.3, 0.3, 0.3,
                0.5
            );
        }
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
