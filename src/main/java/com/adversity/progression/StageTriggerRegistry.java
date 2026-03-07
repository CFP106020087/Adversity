package com.adversity.progression;

import com.adversity.Adversity;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 自动阶段触发注册表
 * 支持基于击杀、难度达标等条件自动解锁阶段
 * 
 * 击杀计数持久化到 PlayerPersisted NBT:
 * PlayerPersisted.AdversityKillCounts.{triggerId} = int
 */
@Mod.EventBusSubscriber(modid = Adversity.MODID)
public class StageTriggerRegistry {

    private static final String NBT_KILL_COUNTS = "AdversityKillCounts";

    private static final List<KillTrigger> KILL_TRIGGERS = new ArrayList<>();
    private static final List<DifficultyTrigger> DIFFICULTY_TRIGGERS = new ArrayList<>();

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
        Adversity.LOGGER.info("Cleared all stage triggers");
    }

    public static int getKillTriggerCount() {
        return KILL_TRIGGERS.size();
    }

    public static int getDifficultyTriggerCount() {
        return DIFFICULTY_TRIGGERS.size();
    }

    // ==================== 持久化 API ====================

    /**
     * 从 PlayerPersisted NBT 读取击杀计数
     */
    private static int getKillCount(EntityPlayer player, String triggerId) {
        NBTTagCompound persisted = player.getEntityData()
                .getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if (!persisted.hasKey(NBT_KILL_COUNTS)) {
            return 0;
        }
        return persisted.getCompoundTag(NBT_KILL_COUNTS).getInteger(triggerId);
    }

    /**
     * 写入击杀计数到 PlayerPersisted NBT
     */
    private static void setKillCount(EntityPlayer player, String triggerId, int count) {
        NBTTagCompound playerData = player.getEntityData();
        NBTTagCompound persisted = playerData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        NBTTagCompound killCounts = persisted.getCompoundTag(NBT_KILL_COUNTS);

        if (count <= 0) {
            killCounts.removeTag(triggerId);
        } else {
            killCounts.setInteger(triggerId, count);
        }

        persisted.setTag(NBT_KILL_COUNTS, killCounts);
        playerData.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
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

            int count = getKillCount(player, trigger.id) + 1;
            setKillCount(player, trigger.id, count);

            if (count >= trigger.requiredKills) {
                ProgressionEventHandler.addStage(player, trigger.rewardStage);
                setKillCount(player, trigger.id, 0); // 清除已完成的计数
                Adversity.LOGGER.info("Kill trigger '{}' activated for {}: stage '{}' unlocked ({}/{})",
                        trigger.id, player.getName(), trigger.rewardStage, count, trigger.requiredKills);
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
