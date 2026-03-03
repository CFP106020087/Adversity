package com.adversity.progression;

import com.adversity.Adversity;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.network.PacketHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Set;

/**
 * 进度事件处理器
 * 负责进度同步、事件触发、难度触发器检查
 */
@Mod.EventBusSubscriber(modid = Adversity.MODID)
public class ProgressionEventHandler {

    public static final String STAGE_AWAKENED = "awakened";
    public static final String STAGE_SCHOLAR = "scholar";
    public static final String STAGE_WARDEN = "warden";
    public static final String STAGE_CHAMPION = "champion";

    public static void register() {
        Adversity.LOGGER.info("Progression Event Handler initialized");
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            syncToClient((EntityPlayerMP) event.player);
        }
    }

    /**
     * 定期检查 dirty 标记并同步 + 检查难度触发器
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        if (!(event.player instanceof EntityPlayerMP))
            return;
        if (event.player.world.isRemote)
            return;

        EntityPlayerMP player = (EntityPlayerMP) event.player;

        // 每10秒检查难度触发器
        if (player.ticksExisted % 200 == 100) {
            IAdversityCapability mobCap = player.getCapability(
                    CapabilityHandler.ADVERSITY_CAPABILITY, null);
            if (mobCap != null) {
                double diff = mobCap.getDifficultyLevel();
                StageTriggerRegistry.checkDifficultyTriggers(player, diff);
            }
        }
    }

    /**
     * 同步进度到客户端
     */
    public static void syncToClient(EntityPlayerMP player) {
        IAdversityCapability.IProgression cap = getProgression(player);
        if (cap != null) {
            PacketHandler.INSTANCE.sendTo(new ProgressionSyncMessage(cap.getStages()), player);
        }
    }

    public static IAdversityCapability.IProgression getProgression(EntityPlayer player) {
        if (player == null || CapabilityHandler.PROGRESSION_CAPABILITY == null) {
            return null;
        }
        return player.getCapability(CapabilityHandler.PROGRESSION_CAPABILITY, null);
    }

    public static boolean hasStage(EntityPlayer player, String stage) {
        IAdversityCapability.IProgression cap = getProgression(player);
        return cap != null && cap.hasStage(stage);
    }

    /**
     * 添加阶段并同步 + 触发 StageChangeEvent
     */
    public static boolean addStage(EntityPlayer player, String stage) {
        IAdversityCapability.IProgression cap = getProgression(player);
        if (cap != null && !cap.hasStage(stage)) {
            // Pre event (cancelable)
            StageChangeEvent.Pre preEvent = new StageChangeEvent.Pre(player, stage, true);
            if (MinecraftForge.EVENT_BUS.post(preEvent)) {
                return false;
            }

            cap.addStage(stage);
            if (player instanceof EntityPlayerMP) {
                syncToClient((EntityPlayerMP) player);
            }

            // Post event
            MinecraftForge.EVENT_BUS.post(new StageChangeEvent.Post(player, stage, true));
            return true;
        }
        return false;
    }

    /**
     * 移除阶段并同步 + 触发 StageChangeEvent
     */
    public static boolean removeStage(EntityPlayer player, String stage) {
        IAdversityCapability.IProgression cap = getProgression(player);
        if (cap != null && cap.hasStage(stage)) {
            StageChangeEvent.Pre preEvent = new StageChangeEvent.Pre(player, stage, false);
            if (MinecraftForge.EVENT_BUS.post(preEvent)) {
                return false;
            }

            cap.removeStage(stage);
            if (player instanceof EntityPlayerMP) {
                syncToClient((EntityPlayerMP) player);
            }

            MinecraftForge.EVENT_BUS.post(new StageChangeEvent.Post(player, stage, false));
            return true;
        }
        return false;
    }

    public static int getHighestTier(EntityPlayer player) {
        IAdversityCapability.IProgression cap = getProgression(player);
        if (cap == null)
            return 0;

        Set<String> stages = cap.getStages();
        if (stages.contains(STAGE_CHAMPION))
            return 4;
        if (stages.contains(STAGE_WARDEN))
            return 3;
        if (stages.contains(STAGE_SCHOLAR))
            return 2;
        if (stages.contains(STAGE_AWAKENED))
            return 1;
        return 0;
    }
}
