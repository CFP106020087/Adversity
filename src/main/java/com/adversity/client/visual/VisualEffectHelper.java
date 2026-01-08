package com.adversity.client.visual;

import com.adversity.network.PacketHandler;
import com.adversity.network.PacketVisualEffect;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.List;

/**
 * 视觉效果发送帮助类
 * 用于从服务端向客户端发送视觉效果
 */
public class VisualEffectHelper {

    /**
     * 向单个玩家发送视觉效果
     */
    public static void sendToPlayer(EntityPlayer player, VisualEffectType type, int duration, float intensity, int sourceEntityId) {
        if (player instanceof EntityPlayerMP) {
            PacketHandler.INSTANCE.sendTo(
                new PacketVisualEffect(type, duration, intensity, sourceEntityId),
                (EntityPlayerMP) player
            );
        }
    }

    /**
     * 向单个玩家移除视觉效果
     */
    public static void removeFromPlayer(EntityPlayer player, VisualEffectType type) {
        if (player instanceof EntityPlayerMP) {
            PacketHandler.INSTANCE.sendTo(
                new PacketVisualEffect(type, true),
                (EntityPlayerMP) player
            );
        }
    }

    /**
     * 向范围内所有玩家发送视觉效果
     */
    public static void sendToNearbyPlayers(EntityLiving source, double range, VisualEffectType type, int duration, float intensity) {
        if (source.world.isRemote) return;

        AxisAlignedBB area = source.getEntityBoundingBox().grow(range);
        List<EntityPlayer> players = source.world.getEntitiesWithinAABB(
            EntityPlayer.class, area,
            p -> p != null && p.isEntityAlive() && !p.isSpectator()
        );

        for (EntityPlayer player : players) {
            sendToPlayer(player, type, duration, intensity, source.getEntityId());
        }
    }

    /**
     * 向被攻击者发送视觉效果（如果是玩家）
     */
    public static void sendToTarget(net.minecraft.entity.EntityLivingBase target, EntityLiving attacker, VisualEffectType type, int duration, float intensity) {
        if (target instanceof EntityPlayer) {
            sendToPlayer((EntityPlayer) target, type, duration, intensity, attacker.getEntityId());
        }
    }

    /**
     * 计算基于距离的强度衰减
     */
    public static float calculateDistanceIntensity(EntityLiving source, EntityPlayer player, double maxRange, float maxIntensity) {
        double distance = source.getDistance(player);
        if (distance >= maxRange) {
            return 0.0f;
        }
        float distanceFactor = 1.0f - (float) (distance / maxRange);
        return maxIntensity * distanceFactor;
    }

    /**
     * 向范围内玩家发送距离衰减的视觉效果
     */
    public static void sendToNearbyPlayersWithFalloff(EntityLiving source, double range, VisualEffectType type, int duration, float maxIntensity) {
        if (source.world.isRemote) return;

        AxisAlignedBB area = source.getEntityBoundingBox().grow(range);
        List<EntityPlayer> players = source.world.getEntitiesWithinAABB(
            EntityPlayer.class, area,
            p -> p != null && p.isEntityAlive() && !p.isSpectator()
        );

        for (EntityPlayer player : players) {
            float intensity = calculateDistanceIntensity(source, player, range, maxIntensity);
            if (intensity > 0.05f) {
                sendToPlayer(player, type, duration, intensity, source.getEntityId());
            }
        }
    }
}
