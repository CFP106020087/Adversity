package com.adversity.progression;

import com.adversity.Adversity;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.network.PacketHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Set;

/**
 * 进度事件处理器
 * 负责进度同步（不负责注册，已由 CapabilityHandler 处理）
 */
@Mod.EventBusSubscriber(modid = Adversity.MODID)
public class ProgressionEventHandler {

    // 阶段常量
    public static final String STAGE_AWAKENED = "awakened";
    public static final String STAGE_SCHOLAR = "scholar";
    public static final String STAGE_WARDEN = "warden";
    public static final String STAGE_CHAMPION = "champion";

    /**
     * 注册（现在不需要，因为 CapabilityHandler 已经处理了）
     */
    public static void register() {
        // Capability 由 CapabilityHandler.register() 注册
        // AttachCapabilities 由 CapabilityHandler.onAttachCapabilities() 处理
        Adversity.LOGGER.info("Progression Event Handler initialized (using existing capability)");
    }

    /**
     * 玩家登录时同步进度
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            syncToClient((EntityPlayerMP) event.player);
        }
    }

    /**
     * 定期检查是否需要同步
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        if (!(event.player instanceof EntityPlayerMP))
            return;
        if (event.player.world.isRemote)
            return;

        // 每2秒同步一次（比较轻量的检查）
        if (event.player.ticksExisted % 40 == 0) {
            // 仅当有变化时才同步，这里简化处理
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

    /**
     * 工具方法：获取玩家进度
     */
    public static IAdversityCapability.IProgression getProgression(EntityPlayer player) {
        if (player == null || CapabilityHandler.PROGRESSION_CAPABILITY == null) {
            return null;
        }
        return player.getCapability(CapabilityHandler.PROGRESSION_CAPABILITY, null);
    }

    /**
     * 工具方法：检查玩家是否有阶段
     */
    public static boolean hasStage(EntityPlayer player, String stage) {
        IAdversityCapability.IProgression cap = getProgression(player);
        return cap != null && cap.hasStage(stage);
    }

    /**
     * 工具方法：添加阶段并同步
     */
    public static boolean addStage(EntityPlayer player, String stage) {
        IAdversityCapability.IProgression cap = getProgression(player);
        if (cap != null) {
            if (!cap.hasStage(stage)) {
                cap.addStage(stage);
                if (player instanceof EntityPlayerMP) {
                    syncToClient((EntityPlayerMP) player);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 获取最高等级
     */
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
