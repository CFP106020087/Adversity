package com.adversity.event;

import com.adversity.affix.AffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.config.AdversityConfig;
import com.adversity.difficulty.DifficultyManager;
import com.adversity.network.PacketHandler;
import com.adversity.network.PacketSyncAdversity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import com.adversity.curse.PermanentCurseManager;
import com.adversity.spawn.NightmareSpawnHandler;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 怪物事件处理器 - 处理生成、攻击、受伤、死亡等事件
 */
public class MobEventHandler {

    /**
     * 实体加入世界时处理
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote) return;
        if (!(event.getEntity() instanceof EntityLiving)) return;

        EntityLiving entity = (EntityLiving) event.getEntity();

        // 检查配置 - 是否应该处理此实体
        if (!AdversityConfig.shouldProcess(entity)) return;

        // 找最近的玩家
        EntityPlayer nearestPlayer = event.getWorld().getClosestPlayerToEntity(entity, 128);

        // 处理生成
        DifficultyManager.processSpawnedEntity(entity, nearestPlayer);
    }

    /**
     * 实体更新时处理（tick 词条效果）
     * 优化：只处理已标记为有词条的实体
     */
    @SubscribeEvent
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (event.getEntityLiving().world.isRemote) return;
        if (!(event.getEntityLiving() instanceof EntityLiving)) return;

        EntityLiving entity = (EntityLiving) event.getEntityLiving();

        // 快速检查：利用实体的持久数据标记，避免每 tick 都获取 capability
        if (!entity.getEntityData().getBoolean("adversity.hasAffixes")) return;

        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        if (cap == null || cap.getAffixCount() == 0) {
            // 标记已失效，清除标记
            entity.getEntityData().removeTag("adversity.hasAffixes");
            return;
        }

        // 处理每个词条的 tick
        for (AffixData data : cap.getAllAffixData()) {
            if (data.isActive()) {
                // 处理冷却
                data.decrementCooldown();
                data.incrementTick();

                // 调用词条的 tick 方法
                data.getAffix().onTick(entity, data);
            }
        }
    }

    /**
     * 实体攻击时处理
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntityLiving().world.isRemote) return;

        DamageSource source = event.getSource();
        float damage = event.getAmount();

        // 检查攻击者是否有词条
        if (source.getTrueSource() instanceof EntityLiving) {
            EntityLiving attacker = (EntityLiving) source.getTrueSource();
            IAdversityCapability attackerCap = CapabilityHandler.getCapability(attacker);

            if (attackerCap != null && attackerCap.getAffixCount() > 0) {
                // 应用伤害倍率
                damage *= attackerCap.getDamageMultiplier();

                // 记录词条伤害前的基础伤害
                float damageBeforeAffixes = damage;

                // 处理攻击型词条
                for (AffixData data : attackerCap.getAllAffixData()) {
                    if (data.isActive() && data.getCooldown() <= 0) {
                        damage = data.getAffix().onAttack(attacker, event.getEntityLiving(), damage, data);
                    }
                }

                // 如果目标是玩家，应用圣所词条伤害压制
                if (event.getEntityLiving() instanceof EntityPlayer) {
                    EntityPlayer targetPlayer = (EntityPlayer) event.getEntityLiving();
                    float sanctuaryReduction = com.adversity.sanctuary.SanctuaryManager
                            .getAffixDamageReduction(targetPlayer);
                    if (sanctuaryReduction > 0) {
                        // 只压制词条造成的额外伤害
                        float affixDamage = damage - damageBeforeAffixes;
                        if (affixDamage > 0) {
                            float reducedAffixDamage = affixDamage * (1.0f - sanctuaryReduction);
                            damage = damageBeforeAffixes + reducedAffixDamage;
                        }
                    }
                }
            }
        }

        // 检查被攻击者是否有 Adversity 数据（减伤和词条）
        if (event.getEntityLiving() instanceof EntityLiving) {
            EntityLiving target = (EntityLiving) event.getEntityLiving();
            IAdversityCapability targetCap = CapabilityHandler.getCapability(target);

            if (targetCap != null) {
                // 应用减伤（所有受 Adversity 处理的怪物都有减伤）
                float damageReduction = targetCap.getDamageReduction();
                if (damageReduction > 0) {
                    damage *= (1.0f - damageReduction);
                }

                // 处理防御型词条（只有有词条的怪物）
                if (targetCap.getAffixCount() > 0) {
                    for (AffixData data : targetCap.getAllAffixData()) {
                        if (data.isActive() && data.getCooldown() <= 0) {
                            damage = data.getAffix().onHurt(target, source, damage, data);
                        }
                    }
                }
            }
        }

        // 设置最终伤害
        event.setAmount(damage);
    }

    /**
     * 实体死亡时处理
     */
    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntityLiving().world.isRemote) return;
        if (!(event.getEntityLiving() instanceof EntityLiving)) return;

        EntityLiving entity = (EntityLiving) event.getEntityLiving();
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);

        if (cap == null || cap.getAffixCount() == 0) return;

        // 调用每个词条的死亡处理
        for (AffixData data : cap.getAllAffixData()) {
            data.getAffix().onDeath(entity, event.getSource(), data);
        }
    }

    // 延迟同步队列 - 处理 StartTracking 在实体处理前触发的竞争条件
    private static final CopyOnWriteArrayList<PendingSync> PENDING_SYNCS = new CopyOnWriteArrayList<>();

    private static class PendingSync {
        final EntityLiving entity;
        final EntityPlayerMP player;
        int ticksRemaining;

        PendingSync(EntityLiving entity, EntityPlayerMP player, int ticksRemaining) {
            this.entity = entity;
            this.player = player;
            this.ticksRemaining = ticksRemaining;
        }
    }

    /**
     * 玩家开始跟踪实体时同步数据
     * 这确保了当玩家加入服务器或移动到已处理实体附近时能够收到数据
     *
     * 注意：StartTracking 可能在 EntityJoinWorldEvent 之前触发，
     * 此时实体尚未被 processSpawnedEntity() 处理。
     * 如果尚未处理，加入延迟队列等待处理完成后再同步。
     */
    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof EntityLiving)) return;
        if (!(event.getEntityPlayer() instanceof EntityPlayerMP)) return;

        EntityLiving entity = (EntityLiving) event.getTarget();
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();

        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        if (cap == null)
            return;

        if (!cap.isProcessed()) {
            // 实体尚未被处理（竞争条件），加入延迟队列，10 tick 后重试
            PENDING_SYNCS.add(new PendingSync(entity, player, 10));
            return;
        }

        sendSyncPacket(entity, player);
    }

    /**
     * 服务端 tick - 处理延迟同步队列
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        if (PENDING_SYNCS.isEmpty())
            return;

        Iterator<PendingSync> it = PENDING_SYNCS.iterator();
        while (it.hasNext()) {
            PendingSync pending = it.next();
            pending.ticksRemaining--;

            if (pending.ticksRemaining <= 0) {
                PENDING_SYNCS.remove(pending);
                sendSyncPacket(pending.entity, pending.player);
            }
        }
    }

    /**
     * 向指定玩家发送实体词条同步包
     */
    private void sendSyncPacket(EntityLiving entity, EntityPlayerMP player) {
        if (!entity.isEntityAlive())
            return;
        if (player.connection == null || !player.connection.getNetworkManager().isChannelOpen())
            return;

        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        if (cap == null || !cap.isProcessed() || cap.getTier() <= 0) return;

        // 收集词条ID
        List<ResourceLocation> affixIds = new ArrayList<>();
        for (AffixData data : cap.getAllAffixData()) {
            affixIds.add(data.getAffix().getId());
        }

        // 向该玩家发送同步包
        PacketSyncAdversity packet = new PacketSyncAdversity(
            entity.getEntityId(),
            cap.getTier(),
            cap.getDifficultyLevel(),
            cap.getHealthMultiplier(),
            cap.getDamageMultiplier(),
            affixIds
        );

        PacketHandler.INSTANCE.sendTo(packet, player);
    }

    /**
     * 玩家睡眠起床时更新睡眠时间（用于梦魇系统）
     */
    @SubscribeEvent
    public void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (event.getEntityPlayer().world.isRemote) return;

        // 只在正常起床时更新（不是被打断）
        if (!event.wakeImmediately() && event.updateWorld()) {
            NightmareSpawnHandler.onPlayerSleep(event.getEntityPlayer());
        }
    }

    /**
     * 玩家登录时同步诅咒数据
     */
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (event.player.world.isRemote) return;

        // 同步诅咒数据到客户端
        PermanentCurseManager manager = PermanentCurseManager.get(event.player.world);
        manager.syncToClient(event.player);
    }
}
