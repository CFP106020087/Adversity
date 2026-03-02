package com.adversity.item.bauble;

import com.adversity.Adversity;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 时光怀表 - 时间专精
 * 
 * 通用效果:
 * - 8格内敌人攻速-20%
 * - 受致死伤害时触发2秒时间停止(3分钟CD)
 * 
 * 词条抵抗: 迅捷(Swift)、狂暴(Berserk)、遗忘(Oblivion)
 */
public class ItemTemporalWatch extends AbstractWardBauble {

    private static final UUID SLOW_ATTACK_UUID = UUID.fromString("c7d8e9f0-1234-5678-9abc-def012345678");
    private static final String SLOW_ATTACK_NAME = "adversity.temporal_watch_slow";

    // 时间停止冷却追踪
    private static final Map<UUID, Long> timeStopCooldown = new HashMap<>();
    private static final int COOLDOWN_TICKS = 3600; // 3分钟
    private static final int TIME_STOP_DURATION = 40; // 2秒

    // 减速效果范围和强度
    private static final double EFFECT_RADIUS = 8.0;
    private static final double ATTACK_SPEED_REDUCTION = -0.20; // -20%攻速

    public ItemTemporalWatch() {
        super("temporal_watch", "swift");
    }

    @Override
    protected float getInSanctuaryStrength() {
        return 1.0f;
    }

    @Override
    protected float getOutsideSanctuaryStrength() {
        return 0.6f;
    }

    @Override
    public void onEffectTriggered(EntityPlayer player, @Nullable EntityLivingBase source, boolean blocked) {
        // 减缓周围敌人攻速（由tick处理）
    }

    /**
     * 每tick检查并应用减速光环
     */
    public void applySlowAura(EntityPlayer player) {
        if (player.world.isRemote)
            return;

        AxisAlignedBB aabb = player.getEntityBoundingBox().grow(EFFECT_RADIUS);
        List<EntityLiving> entities = player.world.getEntitiesWithinAABB(EntityLiving.class, aabb);

        for (EntityLiving mob : entities) {
            IAttributeInstance attackSpeed = mob.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED);
            if (attackSpeed == null)
                continue;

            // 移除旧modifier并添加新的
            AttributeModifier existing = attackSpeed.getModifier(SLOW_ATTACK_UUID);
            if (existing == null) {
                AttributeModifier slowMod = new AttributeModifier(
                        SLOW_ATTACK_UUID,
                        SLOW_ATTACK_NAME,
                        ATTACK_SPEED_REDUCTION,
                        2 // 百分比
                );
                attackSpeed.applyModifier(slowMod);
            }
        }
    }

    /**
     * 检查是否可以触发时间停止
     */
    public boolean canTriggerTimeStop(EntityPlayer player) {
        Long lastUse = timeStopCooldown.get(player.getUniqueID());
        if (lastUse == null)
            return true;

        return (player.world.getTotalWorldTime() - lastUse) >= COOLDOWN_TICKS;
    }

    /**
     * 触发时间停止效果（致死伤害时调用）
     * 
     * @return true 如果成功触发
     */
    public boolean triggerTimeStop(EntityPlayer player) {
        if (!canTriggerTimeStop(player))
            return false;

        // 记录冷却
        timeStopCooldown.put(player.getUniqueID(), player.world.getTotalWorldTime());

        // 冻结周围敌人2秒
        AxisAlignedBB aabb = player.getEntityBoundingBox().grow(EFFECT_RADIUS);
        List<EntityLiving> entities = player.world.getEntitiesWithinAABB(EntityLiving.class, aabb);

        for (EntityLiving mob : entities) {
            // 添加极度减速效果模拟时停
            mob.addPotionEffect(new net.minecraft.potion.PotionEffect(
                    net.minecraft.init.MobEffects.SLOWNESS, TIME_STOP_DURATION, 127));
            mob.addPotionEffect(new net.minecraft.potion.PotionEffect(
                    net.minecraft.init.MobEffects.MINING_FATIGUE, TIME_STOP_DURATION, 127));
        }

        // 给玩家恢复一点生命
        player.setHealth(Math.max(player.getHealth(), 2.0f));

        // 音效
        player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 1.0f, 2.0f);

        Adversity.LOGGER.debug("Temporal Watch triggered time stop for player {}", player.getName());
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void addSpecialDescription(List<String> tooltip) {
        tooltip.add("");
        tooltip.add(I18n.format("adversity.bauble.temporal_watch.effect1")); // 减缓敌人攻速
        tooltip.add(I18n.format("adversity.bauble.temporal_watch.effect2")); // 致死时停
    }
}
