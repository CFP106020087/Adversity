package com.adversity.capability;

import com.adversity.Adversity;
import com.adversity.capability.talisman.ITalismanCapability;
import com.adversity.capability.talisman.TalismanCapability;
import com.adversity.capability.talisman.TalismanCapabilityProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;

import javax.annotation.Nullable;

/**
 * Capability 注册和管理
 */
public class CapabilityHandler {

    public static final ResourceLocation CAPABILITY_ID = new ResourceLocation(Adversity.MODID, "adversity_data");
    public static final ResourceLocation PLAYER_DIFFICULTY_ID = new ResourceLocation(Adversity.MODID, "player_difficulty");
    public static final ResourceLocation TALISMAN_ID = new ResourceLocation(Adversity.MODID, "talisman");

    @CapabilityInject(IAdversityCapability.class)
    public static Capability<IAdversityCapability> ADVERSITY_CAPABILITY = null;

    @CapabilityInject(IPlayerDifficulty.class)
    public static Capability<IPlayerDifficulty> PLAYER_DIFFICULTY_CAPABILITY = null;

    @CapabilityInject(IAdversityCapability.IProgression.class)
    public static Capability<IAdversityCapability.IProgression> PROGRESSION_CAPABILITY = null;

    @CapabilityInject(ITalismanCapability.class)
    public static Capability<ITalismanCapability> TALISMAN_CAPABILITY = null;

    /**
     * 注册 Capability
     */
    public static void register() {
        // 怪物词条 Capability
        CapabilityManager.INSTANCE.register(
            IAdversityCapability.class,
            new AdversityCapabilityStorage(),
            AdversityCapability::new
        );

        // 玩家难度 Capability
        CapabilityManager.INSTANCE.register(
            IPlayerDifficulty.class,
            new Capability.IStorage<IPlayerDifficulty>() {
                @Nullable
                @Override
                public NBTBase writeNBT(Capability<IPlayerDifficulty> capability, IPlayerDifficulty instance, EnumFacing side) {
                    return instance.serializeNBT();
                }

                @Override
                public void readNBT(Capability<IPlayerDifficulty> capability, IPlayerDifficulty instance, EnumFacing side, NBTBase nbt) {
                    if (nbt instanceof NBTTagCompound) {
                        instance.deserializeNBT((NBTTagCompound) nbt);
                    }
                }
            },
            PlayerDifficulty::new
        );

        // 玩家进度 Capability
        CapabilityManager.INSTANCE.register(
                IAdversityCapability.IProgression.class,
                new com.adversity.capability.ProgressionStorage(),
                com.adversity.capability.ProgressionCapability::new);

        // 护符盒 Capability
        CapabilityManager.INSTANCE.register(
                ITalismanCapability.class,
                new Capability.IStorage<ITalismanCapability>() {
                    @Nullable
                    @Override
                    public NBTBase writeNBT(Capability<ITalismanCapability> capability, ITalismanCapability instance,
                            EnumFacing side) {
                        return instance.serializeNBT();
                    }

                    @Override
                    public void readNBT(Capability<ITalismanCapability> capability, ITalismanCapability instance,
                            EnumFacing side, NBTBase nbt) {
                        if (nbt instanceof NBTTagCompound) {
                            instance.deserializeNBT((NBTTagCompound) nbt);
                        }
                    }
                },
                TalismanCapability::new);

        Adversity.LOGGER.info("Adversity Capabilities registered");
    }

    /**
     * 附加 Capability 到实体
     */
    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();

        // 玩家 - 附加难度设置 & 进度数据 & 护符盒
        if (entity instanceof EntityPlayer) {
            event.addCapability(PLAYER_DIFFICULTY_ID, new PlayerDifficultyProvider());
            event.addCapability(new ResourceLocation(Adversity.MODID, "progression"),
                    new com.adversity.capability.ProgressionCapabilityProvider());
            event.addCapability(TALISMAN_ID, new TalismanCapabilityProvider());
        }

        // EntityLiving（怪物、动物等）- 附加词条数据
        if (entity instanceof EntityLiving) {
            event.addCapability(CAPABILITY_ID, new AdversityCapabilityProvider());
        }
    }

    /**
     * 玩家死亡后复活时保留数据
     */
    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            // 难度设置
            IPlayerDifficulty oldCap = getPlayerDifficulty(event.getOriginal());
            IPlayerDifficulty newCap = getPlayerDifficulty(event.getEntityPlayer());
            if (oldCap != null && newCap != null) {
                newCap.deserializeNBT(oldCap.serializeNBT());
            }

            // 护符盒 - 死亡后保留
            ITalismanCapability oldTalisman = getTalismanCapability(event.getOriginal());
            ITalismanCapability newTalisman = getTalismanCapability(event.getEntityPlayer());
            if (oldTalisman != null && newTalisman != null) {
                newTalisman.deserializeNBT(oldTalisman.serializeNBT());
            }

            // 进度阶段 - 死亡后保留
            IAdversityCapability.IProgression oldProg = event.getOriginal().getCapability(PROGRESSION_CAPABILITY, null);
            IAdversityCapability.IProgression newProg = event.getEntityPlayer().getCapability(PROGRESSION_CAPABILITY,
                    null);
            if (oldProg != null && newProg != null) {
                newProg.deserializeNBT(oldProg.serializeNBT());
            }
        }
    }

    /**
     * 获取实体的 Adversity Capability
     */
    @Nullable
    public static IAdversityCapability getCapability(Entity entity) {
        if (entity == null || ADVERSITY_CAPABILITY == null) {
            return null;
        }

        if (entity.hasCapability(ADVERSITY_CAPABILITY, null)) {
            return entity.getCapability(ADVERSITY_CAPABILITY, null);
        }

        return null;
    }

    /**
     * 获取玩家的难度设置
     */
    @Nullable
    public static IPlayerDifficulty getPlayerDifficulty(EntityPlayer player) {
        if (player == null || PLAYER_DIFFICULTY_CAPABILITY == null) {
            return null;
        }

        if (player.hasCapability(PLAYER_DIFFICULTY_CAPABILITY, null)) {
            return player.getCapability(PLAYER_DIFFICULTY_CAPABILITY, null);
        }

        return null;
    }

    /**
     * 检查实体是否有 Adversity Capability
     */
    public static boolean hasCapability(Entity entity) {
        return entity != null && ADVERSITY_CAPABILITY != null
            && entity.hasCapability(ADVERSITY_CAPABILITY, null);
    }

    /**
     * 获取玩家的护符盒能力
     */
    @Nullable
    public static ITalismanCapability getTalismanCapability(EntityPlayer player) {
        if (player == null || TALISMAN_CAPABILITY == null) {
            return null;
        }
        if (player.hasCapability(TALISMAN_CAPABILITY, null)) {
            return player.getCapability(TALISMAN_CAPABILITY, null);
        }
        return null;
    }

    /**
     * 追踪玩家游玩时间（用于个人难度计算）
     */
    @SubscribeEvent
    public void onPlayerTick(net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent event) {
        // 只在服务端、每个tick结束时处理
        if (event.phase != net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END)
            return;
        if (event.player.world.isRemote)
            return;

        // 每tick增加1游玩时间
        IPlayerDifficulty playerDiff = getPlayerDifficulty(event.player);
        if (playerDiff != null) {
            playerDiff.addPlayTime(1);
        }
    }

    /**
     * 玩家登录时重置数据（修复跨存档数据泄漏问题）
     * 在单人游戏中切换世界时，玩家实体可能被复用，
     * 导致旧世界的capability数据残留在内存中。
     * 通过在登录时检查并重新初始化，确保数据来自当前世界的NBT。
     */
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (event.player.world.isRemote)
            return;

        // 获取玩家的capability
        IPlayerDifficulty cap = getPlayerDifficulty(event.player);
        if (cap == null)
            return;

        // 检查玩家NBT中是否有保存的数据
        // 如果是新玩家或新世界，应该使用默认值
        NBTTagCompound playerData = event.player.getEntityData();
        NBTTagCompound forgeData = playerData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);

        // Forge capability数据存储在 ForgeCaps 中
        // 如果没有保存的capability数据，说明是新玩家/新世界，重置为默认值
        if (!forgeData.hasKey("adversity_player_difficulty_initialized")) {
            // 标记已初始化
            forgeData.setBoolean("adversity_player_difficulty_initialized", true);
            playerData.setTag(EntityPlayer.PERSISTED_NBT_TAG, forgeData);

            // 重置所有值为默认
            cap.setDifficultyMultiplier(1.0f);
            cap.setDifficultyDisabled(false);
            cap.resetKillCount();
            cap.setPlayTime(0);
            cap.setHealthScalingMode(IPlayerDifficulty.ScalingMode.DEFAULT);
            cap.setDamageScalingMode(IPlayerDifficulty.ScalingMode.DEFAULT);

            Adversity.LOGGER.debug("Initialized player difficulty for {}", event.player.getName());
        }

        // 同步玩家难度设置到客户端 (用于HUD正确显示)
        if (event.player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            com.adversity.network.PacketHandler.INSTANCE.sendTo(
                    new com.adversity.network.PacketSyncPlayerDifficulty(cap),
                    (net.minecraft.entity.player.EntityPlayerMP) event.player);
        }
    }
}
