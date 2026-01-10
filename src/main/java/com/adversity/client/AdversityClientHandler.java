package com.adversity.client;

import com.adversity.client.render.EntityInfoRenderer;
import com.adversity.config.AdversityConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * 客户端事件处理器 - 处理渲染等客户端逻辑
 * 使用从服务端同步过来的缓存数据进行渲染
 */
@SideOnly(Side.CLIENT)
public class AdversityClientHandler {

    // 渲染距离 - 玩家看向怪物时显示UI的最大距离
    private static final double LOOK_AT_DISTANCE = 48.0;
    private static final double PARTICLE_DISTANCE = 24.0;

    // 缓存当前玩家看向的实体（每tick更新一次）
    private static Entity lookedAtEntity = null;

    private static final Random RANDOM = new Random();
    private int tickCounter = 0;

    /**
     * 玩家切换维度时清除缓存，并处理粒子效果
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            // 退出世界时清除缓存
            ClientAdversityCache.clearAll();
            lookedAtEntity = null;
            return;
        }

        // 每 tick 更新玩家看向的实体（用于UI显示）
        updateLookedAtEntity(mc);

        // 每 5 tick 生成粒子（减少性能开销）
        tickCounter++;
        if (tickCounter % 5 == 0) {
            spawnTierParticles(mc);
        }
    }

    /**
     * 更新玩家当前看向的实体
     * 使用自定义射线检测，范围为 LOOK_AT_DISTANCE
     */
    private void updateLookedAtEntity(Minecraft mc) {
        if (mc.player == null || mc.world == null) {
            lookedAtEntity = null;
            return;
        }

        // 获取玩家视线方向
        Vec3d eyePos = mc.player.getPositionEyes(1.0f);
        Vec3d lookVec = mc.player.getLook(1.0f);
        Vec3d endPos = eyePos.add(lookVec.scale(LOOK_AT_DISTANCE));

        // 射线检测实体
        lookedAtEntity = rayTraceEntities(mc, eyePos, endPos);
    }

    /**
     * 射线检测实体
     * @return 射线命中的最近实体，如果没有则返回 null
     */
    @Nullable
    private Entity rayTraceEntities(Minecraft mc, Vec3d start, Vec3d end) {
        Entity result = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : mc.world.loadedEntityList) {
            if (!(entity instanceof EntityLiving)) continue;
            if (!entity.isEntityAlive()) continue;

            // 根据实体大小调整碰撞检测范围
            float expandSize = Math.max(0.3f, entity.width * 0.15f);
            AxisAlignedBB box = entity.getEntityBoundingBox().grow(expandSize);

            // 检查射线是否与碰撞箱相交
            RayTraceResult rayResult = box.calculateIntercept(start, end);
            if (rayResult != null) {
                double distance = start.distanceTo(rayResult.hitVec);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    result = entity;
                }
            }
        }

        return result;
    }

    /**
     * 为有等级的怪物生成粒子效果
     */
    private void spawnTierParticles(Minecraft mc) {
        if (mc.player == null || mc.world == null) return;

        // 清理已死亡实体的缓存
        cleanupDeadEntities(mc);

        for (Entity entity : mc.world.loadedEntityList) {
            if (!(entity instanceof EntityLiving)) continue;

            double distSq = mc.player.getDistanceSq(entity);
            if (distSq > PARTICLE_DISTANCE * PARTICLE_DISTANCE) continue;

            ClientAdversityCache.CachedEntityData data = ClientAdversityCache.getEntityData(entity.getEntityId());
            if (data == null || data.tier <= 0) continue;

            // 根据等级选择粒子类型和数量
            spawnParticlesForTier(mc, entity, data.tier);
        }
    }

    /**
     * 清理已死亡或不存在的实体缓存
     */
    private void cleanupDeadEntities(Minecraft mc) {
        // 每 100 tick 清理一次（约 5 秒）
        if (tickCounter % 100 != 0) return;

        java.util.Set<Integer> validIds = new java.util.HashSet<>();
        for (Entity entity : mc.world.loadedEntityList) {
            if (entity instanceof EntityLiving && entity.isEntityAlive()) {
                validIds.add(entity.getEntityId());
            }
        }

        // 清理已死亡/移除的实体缓存
        ClientAdversityCache.retainOnly(validIds);
    }

    /**
     * 根据等级生成不同的粒子 (10级系统)
     * 高等级粒子更密集，大型怪物粒子更多
     */
    private void spawnParticlesForTier(Minecraft mc, Entity entity, int tier) {
        // 根据实体大小调整粒子数量
        float sizeMultiplier = Math.max(1.0f, (entity.width + entity.height) / 2.0f);
        int particleCount = (int) Math.ceil(sizeMultiplier);

        for (int i = 0; i < particleCount; i++) {
            double x = entity.posX + (RANDOM.nextDouble() - 0.5) * entity.width * 1.2;
            double y = entity.posY + RANDOM.nextDouble() * entity.height;
            double z = entity.posZ + (RANDOM.nextDouble() - 0.5) * entity.width * 1.2;

            // 粒子速度（向上飘动）
            double vx = (RANDOM.nextDouble() - 0.5) * 0.05;
            double vy = 0.02 + RANDOM.nextDouble() * 0.03;
            double vz = (RANDOM.nextDouble() - 0.5) * 0.05;

            spawnParticleByTier(mc, tier, x, y, z, vx, vy, vz);
        }
    }

    /**
     * 根据等级生成对应粒子
     */
    private void spawnParticleByTier(Minecraft mc, int tier, double x, double y, double z,
                                      double vx, double vy, double vz) {
        switch (tier) {
            case 1: // 精英 - 绿色粒子（稀疏）
                if (RANDOM.nextInt(4) == 0) {
                    mc.world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, x, y, z, vx, vy, vz);
                }
                break;
            case 2: // 稀有 - 蓝色水花
                if (RANDOM.nextInt(3) == 0) {
                    mc.world.spawnParticle(EnumParticleTypes.WATER_SPLASH, x, y, z, vx, vy, vz);
                }
                break;
            case 3: // 精锐 - 青色气泡
                if (RANDOM.nextInt(3) == 0) {
                    mc.world.spawnParticle(EnumParticleTypes.WATER_BUBBLE, x, y, z, vx, vy, vz);
                }
                break;
            case 4: // 史诗 - 紫色传送门
                if (RANDOM.nextInt(2) == 0) {
                    mc.world.spawnParticle(EnumParticleTypes.PORTAL, x, y, z, vx, vy * 2, vz);
                }
                break;
            case 5: // 传说 - 金色火焰
                mc.world.spawnParticle(EnumParticleTypes.FLAME, x, y, z, vx, vy, vz);
                break;
            case 6: // 神话 - 红色火焰 + 烟雾
                mc.world.spawnParticle(EnumParticleTypes.FLAME, x, y, z, vx, vy, vz);
                if (RANDOM.nextInt(2) == 0) {
                    mc.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, y, z, 0, 0.02, 0);
                }
                break;
            case 7: // 远古 - 紫色 + 附魔
                mc.world.spawnParticle(EnumParticleTypes.PORTAL, x, y, z, vx, vy * 2, vz);
                mc.world.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE, x, y + 0.5, z, 0, 0.1, 0);
                break;
            case 8: // 虚空 - 末影粒子
                mc.world.spawnParticle(EnumParticleTypes.PORTAL, x, y, z, vx, vy * 3, vz);
                mc.world.spawnParticle(EnumParticleTypes.SUSPENDED_DEPTH, x, y, z, 0, 0, 0);
                break;
            case 9: // 深渊 - 岩浆 + 火焰
                mc.world.spawnParticle(EnumParticleTypes.FLAME, x, y, z, vx, vy, vz);
                mc.world.spawnParticle(EnumParticleTypes.LAVA, x, y, z, 0, 0, 0);
                break;
            case 10: // 终焉 - 所有效果 + 爆炸
                mc.world.spawnParticle(EnumParticleTypes.FLAME, x, y, z, vx, vy, vz);
                mc.world.spawnParticle(EnumParticleTypes.PORTAL, x, y, z, vx, vy * 2, vz);
                mc.world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, x, y, z, 0, 0.05, 0);
                if (RANDOM.nextInt(3) == 0) {
                    mc.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, x, y, z, 0, 0, 0);
                }
                break;
        }
    }

    /**
     * 渲染实体名称后显示词条信息和血条
     * 只在玩家目光对准实体时显示
     */
    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Post<EntityLiving> event) {
        if (!(event.getEntity() instanceof EntityLiving)) return;

        EntityLiving entity = (EntityLiving) event.getEntity();

        // 从客户端缓存获取数据
        ClientAdversityCache.CachedEntityData data = ClientAdversityCache.getEntityData(entity.getEntityId());
        if (data == null || data.tier <= 0) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;

        // 只在玩家目光对准该实体时显示
        if (lookedAtEntity != entity) return;

        // 使用新的渲染器
        EntityInfoRenderer.render(entity, data, event.getX(), event.getY(), event.getZ());
    }
}
