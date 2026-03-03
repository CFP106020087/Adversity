package com.adversity.progression;

import com.adversity.Adversity;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自动阶段触发注册表
 * 支持基于击杀、难度达标等条件自动解锁阶段
 */
@Mod.EventBusSubscriber(modid = Adversity.MODID)
public class StageTriggerRegistry {

    private static final List<KillTrigger> KILL_TRIGGERS = new ArrayList<>();
    private static final List<DifficultyTrigger> DIFFICULTY_TRIGGERS = new ArrayList<>();
    private static final Map<String, Integer> playerKillCounts = new HashMap<>();

    public static class KillTrigger {
        public final String id;
        public final ResourceLocation entityId;
        public final int requiredKills;
        public final String rewardStage;

        public KillTrigger(String id, ResourceLocation entityId, int requiredKills, String rewardStage) {
            this.id = id;
            this.entityId = entityId;
            this.requiredKills = requiredKills;
            this.rewardStage = rewardStage;
        }
    }

    public static class DifficultyTrigger {
        public final String id;
        public final double minDifficulty;
        public final String rewardStage;

        public DifficultyTrigger(String id, double minDifficulty, String rewardStage) {
            this.id = id;
            this.minDifficulty = minDifficulty;
            this.rewardStage = rewardStage;
        }
    }

    // ==================== 注册 API ====================

    public static void registerKillTrigger(String id, String entityId, int killCount, String stage) {
        KILL_TRIGGERS.add(new KillTrigger(id, new ResourceLocation(entityId), killCount, stage));
        Adversity.LOGGER.info("Registered kill trigger: {} (entity={}, kills={}, stage={})", id, entityId, killCount,
                stage);
    }

    public static void registerDifficultyTrigger(String id, double minDiff, String stage) {
        DIFFICULTY_TRIGGERS.add(new DifficultyTrigger(id, minDiff, stage));
        Adversity.LOGGER.info("Registered difficulty trigger: {} (diff>={}, stage={})", id, minDiff, stage);
    }

    public static void clearAll() {
        KILL_TRIGGERS.clear();
        DIFFICULTY_TRIGGERS.clear();
        playerKillCounts.clear();
        Adversity.LOGGER.info("Cleared all stage triggers");
    }

    public static int getKillTriggerCount() {
        return KILL_TRIGGERS.size();
    }

    public static int getDifficultyTriggerCount() {
        return DIFFICULTY_TRIGGERS.size();
    }

    // ==================== 事件处理 ====================

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (KILL_TRIGGERS.isEmpty())
            return;
        if (event.getSource().getTrueSource() == null)
            return;
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer))
            return;
        if (event.getEntityLiving().world.isRemote)
            return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        EntityLivingBase killed = event.getEntityLiving();
        ResourceLocation killedId = net.minecraft.entity.EntityList.getKey(killed);
        if (killedId == null)
            return;

        IAdversityCapability.IProgression cap = player.getCapability(CapabilityHandler.PROGRESSION_CAPABILITY, null);
        if (cap == null)
            return;

        for (KillTrigger trigger : KILL_TRIGGERS) {
            if (!trigger.entityId.equals(killedId))
                continue;
            if (cap.hasStage(trigger.rewardStage))
                continue;

            String key = player.getName() + ":" + trigger.id;
            int count = playerKillCounts.getOrDefault(key, 0) + 1;
            playerKillCounts.put(key, count);

            if (count >= trigger.requiredKills) {
                ProgressionEventHandler.addStage(player, trigger.rewardStage);
                playerKillCounts.remove(key);
                Adversity.LOGGER.info("Kill trigger '{}' activated for {}: stage '{}' unlocked",
                        trigger.id, player.getName(), trigger.rewardStage);
            }
        }
    }

    /**
     * 检查难度触发器
     */
    public static void checkDifficultyTriggers(EntityPlayer player, double currentDifficulty) {
        if (DIFFICULTY_TRIGGERS.isEmpty())
            return;

        IAdversityCapability.IProgression cap = player.getCapability(CapabilityHandler.PROGRESSION_CAPABILITY, null);
        if (cap == null)
            return;

        for (DifficultyTrigger trigger : DIFFICULTY_TRIGGERS) {
            if (currentDifficulty >= trigger.minDifficulty && !cap.hasStage(trigger.rewardStage)) {
                ProgressionEventHandler.addStage(player, trigger.rewardStage);
                Adversity.LOGGER.info("Difficulty trigger '{}' activated for {}: stage '{}' unlocked",
                        trigger.id, player.getName(), trigger.rewardStage);
            }
        }
    }
}
