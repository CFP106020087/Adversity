package com.adversity.client;

import com.adversity.affix.AffixRegistry;
import com.adversity.affix.IAffix;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Random;

/**
 * 客户端事件处理器 - 处理渲染等客户端逻辑
 * 使用从服务端同步过来的缓存数据进行渲染
 */
@SideOnly(Side.CLIENT)
public class AdversityClientHandler {

    // 渲染距离
    private static final double RENDER_DISTANCE = 32.0;
    private static final double PARTICLE_DISTANCE = 24.0;

    // 血条尺寸
    private static final float HEALTH_BAR_WIDTH = 40.0f;
    private static final float HEALTH_BAR_HEIGHT = 4.0f;

    // 等级名称（后备，优先使用翻译）- 10级系统
    private static final String[] TIER_NAMES_FALLBACK = {
        "",           // T0
        "Elite",      // T1 - 精英
        "Rare",       // T2 - 稀有
        "Veteran",    // T3 - 精锐
        "Epic",       // T4 - 史诗
        "Legendary",  // T5 - 传说
        "Mythic",     // T6 - 神话
        "Ancient",    // T7 - 远古
        "Void",       // T8 - 虚空
        "Abyssal",    // T9 - 深渊
        "Terminus"    // T10 - 终焉
    };

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
            return;
        }

        // 每 5 tick 生成粒子（减少性能开销）
        tickCounter++;
        if (tickCounter % 5 == 0) {
            spawnTierParticles(mc);
        }
    }

    /**
     * 为有等级的怪物生成粒子效果（类似 Champions）
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
     * 高等级粒子更密集
     */
    private void spawnParticlesForTier(Minecraft mc, Entity entity, int tier) {
        double x = entity.posX + (RANDOM.nextDouble() - 0.5) * entity.width;
        double y = entity.posY + RANDOM.nextDouble() * entity.height;
        double z = entity.posZ + (RANDOM.nextDouble() - 0.5) * entity.width;

        // 粒子速度（向上飘动）
        double vx = (RANDOM.nextDouble() - 0.5) * 0.05;
        double vy = 0.02 + RANDOM.nextDouble() * 0.03;
        double vz = (RANDOM.nextDouble() - 0.5) * 0.05;

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
        if (mc.pointedEntity != entity) return;

        double distance = mc.player.getDistanceSq(entity);
        if (distance > RENDER_DISTANCE * RENDER_DISTANCE) return;

        // 渲染等级、词条信息和血条
        renderAffixInfo(entity, data, event.getX(), event.getY(), event.getZ());
    }

    /**
     * 渲染词条信息和血条
     */
    private void renderAffixInfo(EntityLiving entity, ClientAdversityCache.CachedEntityData data,
                                  double x, double y, double z) {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fontRenderer = mc.fontRenderer;

        // 计算渲染位置（在实体上方）
        float height = entity.height + 0.5f;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + height + 0.3, z);
        GlStateManager.glNormal3f(0.0f, 1.0f, 0.0f);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0f, 1.0f, 0.0f);
        GlStateManager.rotate((mc.gameSettings.thirdPersonView == 2 ? -1 : 1) * mc.getRenderManager().playerViewX, 1.0f, 0.0f, 0.0f);
        GlStateManager.scale(-0.025f, -0.025f, 0.025f);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        );
        GlStateManager.disableTexture2D();

        // 渲染血条
        renderHealthBar(entity, data);

        GlStateManager.enableTexture2D();

        // 构建显示文本 - 等级名称（如 Elite, Rare, Epic, Boss）
        TextFormatting tierColor = getTierColor(data.tier);
        String tierName = getTierName(data.tier);
        String tierStr = tierColor + "★ " + tierName + " ★";

        // 绘制等级文本
        int tierWidth = fontRenderer.getStringWidth(tierStr);
        fontRenderer.drawString(tierStr, -tierWidth / 2, -12, 0xFFFFFF);

        // 绘制词条名称
        List<ResourceLocation> affixIds = data.affixIds;
        if (!affixIds.isEmpty()) {
            StringBuilder affixText = new StringBuilder();
            for (int i = 0; i < affixIds.size(); i++) {
                IAffix affix = AffixRegistry.getAffix(affixIds.get(i));
                if (affix != null) {
                    if (i > 0) affixText.append(" ");
                    affixText.append(getAffixColor(affix)).append(affix.getDisplayName());
                }
            }
            String affixStr = affixText.toString();
            int affixWidth = fontRenderer.getStringWidth(affixStr);
            fontRenderer.drawString(affixStr, -affixWidth / 2, 0, 0xFFFFFF);
        }

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.popMatrix();
    }

    /**
     * 渲染血条
     */
    private void renderHealthBar(EntityLiving entity, ClientAdversityCache.CachedEntityData data) {
        float healthPercent = entity.getHealth() / entity.getMaxHealth();
        healthPercent = Math.max(0, Math.min(1, healthPercent));

        float halfWidth = HEALTH_BAR_WIDTH / 2;
        float yOffset = -20;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // 背景（黑色）
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(-halfWidth - 1, yOffset - 1, 0).color(0, 0, 0, 128).endVertex();
        buffer.pos(-halfWidth - 1, yOffset + HEALTH_BAR_HEIGHT + 1, 0).color(0, 0, 0, 128).endVertex();
        buffer.pos(halfWidth + 1, yOffset + HEALTH_BAR_HEIGHT + 1, 0).color(0, 0, 0, 128).endVertex();
        buffer.pos(halfWidth + 1, yOffset - 1, 0).color(0, 0, 0, 128).endVertex();
        tessellator.draw();

        // 血条背景（深红）
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(-halfWidth, yOffset, 0).color(64, 0, 0, 200).endVertex();
        buffer.pos(-halfWidth, yOffset + HEALTH_BAR_HEIGHT, 0).color(64, 0, 0, 200).endVertex();
        buffer.pos(halfWidth, yOffset + HEALTH_BAR_HEIGHT, 0).color(64, 0, 0, 200).endVertex();
        buffer.pos(halfWidth, yOffset, 0).color(64, 0, 0, 200).endVertex();
        tessellator.draw();

        // 当前血量（根据等级变色）
        int[] color = getTierHealthColor(data.tier);
        float currentWidth = HEALTH_BAR_WIDTH * healthPercent;
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(-halfWidth, yOffset, 0).color(color[0], color[1], color[2], 220).endVertex();
        buffer.pos(-halfWidth, yOffset + HEALTH_BAR_HEIGHT, 0).color(color[0], color[1], color[2], 220).endVertex();
        buffer.pos(-halfWidth + currentWidth, yOffset + HEALTH_BAR_HEIGHT, 0).color(color[0], color[1], color[2], 220).endVertex();
        buffer.pos(-halfWidth + currentWidth, yOffset, 0).color(color[0], color[1], color[2], 220).endVertex();
        tessellator.draw();
    }

    /**
     * 根据等级获取翻译后的名称 (10级系统)
     */
    private String getTierName(int tier) {
        if (tier <= 0 || tier > 10) {
            return "T" + tier;
        }
        String key = "adversity.tier." + tier;
        String translated = I18n.format(key);
        // 如果没有翻译，使用后备名称
        if (translated.equals(key)) {
            return tier < TIER_NAMES_FALLBACK.length ? TIER_NAMES_FALLBACK[tier] : "T" + tier;
        }
        return translated;
    }

    /**
     * 根据等级获取文字颜色 (10级系统)
     */
    private TextFormatting getTierColor(int tier) {
        switch (tier) {
            case 1: return TextFormatting.GREEN;        // 精英 - 绿色
            case 2: return TextFormatting.BLUE;         // 稀有 - 蓝色
            case 3: return TextFormatting.AQUA;         // 精锐 - 青色
            case 4: return TextFormatting.LIGHT_PURPLE; // 史诗 - 淡紫
            case 5: return TextFormatting.GOLD;         // 传说 - 金色
            case 6: return TextFormatting.RED;          // 神话 - 红色
            case 7: return TextFormatting.DARK_PURPLE;  // 远古 - 深紫
            case 8: return TextFormatting.DARK_BLUE;    // 虚空 - 深蓝
            case 9: return TextFormatting.DARK_RED;     // 深渊 - 深红
            case 10: return TextFormatting.BLACK;       // 终焉 - 黑色
            default: return TextFormatting.WHITE;
        }
    }

    /**
     * 根据等级获取血条颜色 (10级系统)
     */
    private int[] getTierHealthColor(int tier) {
        switch (tier) {
            case 1: return new int[]{85, 255, 85};      // 精英 - 绿色
            case 2: return new int[]{85, 85, 255};      // 稀有 - 蓝色
            case 3: return new int[]{85, 255, 255};     // 精锐 - 青色
            case 4: return new int[]{255, 85, 255};     // 史诗 - 紫色
            case 5: return new int[]{255, 170, 0};      // 传说 - 金色
            case 6: return new int[]{255, 85, 85};      // 神话 - 红色
            case 7: return new int[]{170, 0, 170};      // 远古 - 深紫
            case 8: return new int[]{0, 0, 170};        // 虚空 - 深蓝
            case 9: return new int[]{170, 0, 0};        // 深渊 - 深红
            case 10: return new int[]{50, 50, 50};      // 终焉 - 黑灰
            default: return new int[]{255, 255, 255};   // 白色
        }
    }

    /**
     * 根据词条类型获取颜色
     */
    private TextFormatting getAffixColor(IAffix affix) {
        switch (affix.getType()) {
            case OFFENSIVE:
                return TextFormatting.RED;
            case DEFENSIVE:
                return TextFormatting.AQUA;
            case UTILITY:
                return TextFormatting.YELLOW;
            case SPECIAL:
                return TextFormatting.LIGHT_PURPLE;
            default:
                return TextFormatting.GRAY;
        }
    }
}
