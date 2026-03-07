package com.adversity.network;

import com.adversity.Adversity;
import com.adversity.progression.ProgressionSyncMessage;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 网络包处理器 - 管理客户端与服务端之间的数据同步
 */
public class PacketHandler {

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Adversity.MODID);

    private static int id = 0;

    /**
     * 注册所有网络包
     */
    public static void init() {
        // 同步怪物难度数据到客户端
        INSTANCE.registerMessage(
            PacketSyncAdversity.Handler.class,
            PacketSyncAdversity.class,
            id++,
            Side.CLIENT
        );

        // 同步视觉效果到客户端
        INSTANCE.registerMessage(
            PacketVisualEffect.Handler.class,
            PacketVisualEffect.class,
            id++,
            Side.CLIENT
        );

        // 同步永久诅咒数据到客户端
        INSTANCE.registerMessage(
            PacketSyncCurse.Handler.class,
            PacketSyncCurse.class,
            id++,
            Side.CLIENT
        );

        // 圣所传送请求 (Client -> Server)
        INSTANCE.registerMessage(
                PacketSanctuaryTeleport.Handler.class,
                PacketSanctuaryTeleport.class,
                id++,
                Side.SERVER);

        // 同步玩家难度设置到客户端 (用于HUD显示)
        INSTANCE.registerMessage(
                PacketSyncPlayerDifficulty.Handler.class,
                PacketSyncPlayerDifficulty.class,
                id++,
                Side.CLIENT);

        // 同步进度阶段到客户端
        INSTANCE.registerMessage(
                ProgressionSyncMessage.Handler.class,
                ProgressionSyncMessage.class,
                id++,
                Side.CLIENT);

        // 圣所操作 (Client -> Server) - 模式切换、升级、激活
        INSTANCE.registerMessage(
                PacketSanctuaryAction.Handler.class,
                PacketSanctuaryAction.class,
                id++,
                Side.SERVER);

        // 打开护符盒 GUI (Client -> Server)
        INSTANCE.registerMessage(
                PacketOpenTalisman.Handler.class,
                PacketOpenTalisman.class,
                id++,
                Side.SERVER);

        // 圣所传送目标选择 (Client -> Server)
        INSTANCE.registerMessage(
                        PacketTeleportToSanctuary.Handler.class,
                        PacketTeleportToSanctuary.class,
                        id++,
                        Side.SERVER);

        // 圣所GUI切换 (Client -> Server)
        INSTANCE.registerMessage(
                        PacketSwitchSanctuaryGui.Handler.class,
                        PacketSwitchSanctuaryGui.class,
                        id++,
                        Side.SERVER);

        // 覺醒選擇 GUI (Server -> Client)
        INSTANCE.registerMessage(
                        PacketOpenAwakeningGUI.Handler.class,
                        PacketOpenAwakeningGUI.class,
                        id++,
                        Side.CLIENT);

        // 覺醒效果選擇 (Client -> Server)
        INSTANCE.registerMessage(
                        PacketAwakeningSelection.Handler.class,
                        PacketAwakeningSelection.class,
                        id++,
                        Side.SERVER);

        // 附魔門控配置保存 (Client -> Server)
        INSTANCE.registerMessage(
                        PacketSaveEnchantGating.Handler.class,
                        PacketSaveEnchantGating.class,
                        id++,
                        Side.SERVER);

        Adversity.LOGGER.info("Network packets registered");
    }
}
