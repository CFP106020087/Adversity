package com.adversity.client.handler;

import com.adversity.sanctuary.SanctuaryData;
import com.adversity.sanctuary.SanctuaryZone;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 圣所边界效果处理器
 * 当玩家靠近圣所边界时显示粒子墙和警告
 */
@Mod.EventBusSubscriber(modid = "adversity", value = Side.CLIENT)
@SideOnly(Side.CLIENT)
public class SanctuaryBoundaryHandler {

    // 检测间隔 (ticks) - 不需要每tick都检测
    private static final int CHECK_INTERVAL = 10;
    
    // 边界警告距离阈值
    private static final double PARTICLE_DISTANCE = 20.0; // 20格内显示粒子
    private static final double WARNING_DISTANCE = 10.0;  // 10格内显示警告
    
    // 粒子生成参数
    private static final int PARTICLES_PER_RENDER = 12;   // 每次生成的粒子数量
    private static final double PARTICLE_HEIGHT = 4.0;    // 粒子墙高度
    
    // 状态追踪
    private static int tickCounter = 0;
    private static long lastWarningTime = 0;
    private static final long WARNING_COOLDOWN = 3000; // 警告消息冷却3秒

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null || mc.isGamePaused()) return;
        
        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) return;
        tickCounter = 0;
        
        EntityPlayer player = mc.player;
        World world = mc.world;
        
        // 获取玩家所在维度的圣所
        SanctuaryData data = SanctuaryData.get(world);
        if (data == null) return;
        
        int playerDim = player.dimension;
        BlockPos playerPos = player.getPosition();
        double playerX = player.posX;
        double playerZ = player.posZ;
        
        // 检查所有圣所的边界距离
        for (SanctuaryZone zone : data.getAllSanctuaries()) {
            if (zone.dimension != playerDim || !zone.isActive()) continue;
            
            double dx = playerX - zone.center.getX();
            double dz = playerZ - zone.center.getZ();
            double distanceToCenter = Math.sqrt(dx * dx + dz * dz);
            double distanceToBoundary = Math.abs(distanceToCenter - zone.radius);
            
            // 玩家在圣所边界附近
            if (distanceToBoundary < PARTICLE_DISTANCE) {
                // 渲染粒子墙
                renderBoundaryParticles(world, player, zone, distanceToCenter);
                
                // 显示警告（进入或离开圣所时）
                if (distanceToBoundary < WARNING_DISTANCE) {
                    showBoundaryWarning(player, distanceToCenter < zone.radius);
                }
                
                // 只处理最近的一个圣所边界
                break;
            }
        }
    }
    
    /**
     * 在玩家附近的边界位置渲染粒子
     */
    private static void renderBoundaryParticles(World world, EntityPlayer player, 
            SanctuaryZone zone, double distanceToCenter) {
        
        double centerX = zone.center.getX() + 0.5;
        double centerZ = zone.center.getZ() + 0.5;
        double radius = zone.radius;
        
        // 计算玩家相对圣所中心的角度
        double dx = player.posX - centerX;
        double dz = player.posZ - centerZ;
        double playerAngle = Math.atan2(dz, dx);
        
        // 在玩家视野方向的边界位置生成粒子 (±45度范围)
        double angleSpread = Math.PI / 4; // 45度
        
        for (int i = 0; i < PARTICLES_PER_RENDER; i++) {
            // 随机角度偏移
            double angle = playerAngle + (world.rand.nextDouble() - 0.5) * angleSpread * 2;
            
            // 边界点坐标
            double bx = centerX + radius * Math.cos(angle);
            double bz = centerZ + radius * Math.sin(angle);
            double by = player.posY + (world.rand.nextDouble() - 0.5) * PARTICLE_HEIGHT;
            
            // 确保粒子在玩家可视范围内
            double distToParticle = Math.sqrt(
                    Math.pow(bx - player.posX, 2) + Math.pow(bz - player.posZ, 2));
            if (distToParticle > 30) continue; // 太远不渲染
            
            // 选择粒子类型：圣所内用蓝色，外用红色
            EnumParticleTypes particleType;
            if (distanceToCenter < radius) {
                // 在圣所内，显示蓝色屏障粒子
                particleType = EnumParticleTypes.WATER_BUBBLE;
            } else {
                // 在圣所外，显示红色警告粒子
                particleType = EnumParticleTypes.FLAME;
            }
            
            world.spawnParticle(particleType, bx, by, bz, 0, 0.02, 0);
        }
    }
    
    /**
     * 显示边界警告消息
     */
    private static void showBoundaryWarning(EntityPlayer player, boolean entering) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastWarningTime < WARNING_COOLDOWN) return;
        lastWarningTime = currentTime;
        
        String messageKey = entering ? 
                "adversity.sanctuary.boundary.leaving" : 
                "adversity.sanctuary.boundary.entering";
        
        player.sendStatusMessage(new TextComponentTranslation(messageKey), true);
    }
}
