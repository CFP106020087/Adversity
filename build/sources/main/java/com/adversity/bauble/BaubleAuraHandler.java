package com.adversity.bauble;

import com.adversity.Adversity;
import com.adversity.affix.AffixRegistry;
import com.adversity.affix.IAffix;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.config.AdversityConfig;
import com.adversity.item.ItemRegistry;
import com.adversity.item.bauble.BaubleHelper;
import com.adversity.sanctuary.SanctuaryManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

/**
 * 饰品反制光环处理器
 * 定期检查周围精英，如果玩家佩戴了对应的反制饰品，则压制怪物的词条
 */
@Mod.EventBusSubscriber(modid = Adversity.MODID)
public class BaubleAuraHandler {

    // 检查间隔 (10 tick = 0.5秒)
    private static final int CHECK_INTERVAL = 10;

    // 光环半径
    private static final double AURA_RADIUS = 16.0;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }

        // 配置检查
        if (!AdversityConfig.auraSettings.enableAura)
            return;

        EntityPlayer player = event.player;
        if (player.ticksExisted % AdversityConfig.auraSettings.auraInterval != 0) {
            return;
        }

        // 检查玩家是否携带了反制饰品
        boolean hasSoulChain = BaubleHelper.hasBauble(player, ItemRegistry.SOUL_CHAIN);
        boolean hasSpatialAnchor = BaubleHelper.hasBauble(player, ItemRegistry.SPATIAL_ANCHOR);
        boolean hasGuardianHeart = BaubleHelper.hasBauble(player, ItemRegistry.GUARDIAN_HEART);
        boolean hasFlameWard = BaubleHelper.hasBauble(player, ItemRegistry.FLAME_WARD);
        boolean hasFrostWard = BaubleHelper.hasBauble(player, ItemRegistry.FROST_WARD);

        // 如果没有任何反制饰品，跳过检查
        if (!hasSoulChain && !hasSpatialAnchor && !hasGuardianHeart && !hasFlameWard && !hasFrostWard) {
            return;
        }

        // 检查是否在圣所内 (效果增强)
        boolean inSanctuary = SanctuaryManager.isPlayerInSanctuary(player);
        double range = inSanctuary ? AdversityConfig.auraSettings.auraRadius * 1.5
                : AdversityConfig.auraSettings.auraRadius;

        AxisAlignedBB aabb = player.getEntityBoundingBox().grow(range, range, range);
        List<EntityLiving> entities = player.world.getEntitiesWithinAABB(EntityLiving.class, aabb);

        for (EntityLiving mob : entities) {
            IAdversityCapability cap = CapabilityHandler.getCapability(mob);
            if (cap == null || cap.getTier() <= 0)
                continue;

            // 1. 灵魂锁链 vs 枷锁 (Anti-Shackle)
            if (hasSoulChain) {
                suppressAffix(mob, cap, "adversity:shackle", inSanctuary);
            }

            // 2. 空间锚点 vs 褫夺 (Anti-Divest)
            if (hasSpatialAnchor) {
                suppressAffix(mob, cap, "adversity:divest", inSanctuary);
            }

            // 3. 守护之心 vs 诅咒 (Anti-Curse)
            // 守护之心压制所有永久诅咒和部分负面词条
            if (hasGuardianHeart) {
                suppressAffix(mob, cap, "adversity:black_swan", inSanctuary);
                suppressAffix(mob, cap, "adversity:black_friday", inSanctuary);
                suppressAffix(mob, cap, "adversity:black_coffin", inSanctuary);
                suppressAffix(mob, cap, "adversity:annihilate", inSanctuary);
            }

            // 4. 净焰之环 vs 烈焰 (Extinguish)
            IAffix fieryAffix = AffixRegistry.getAffix(new ResourceLocation("adversity:fiery"));
            if (hasFlameWard && fieryAffix != null && cap.hasAffix(fieryAffix)) {
                mob.extinguish();
                // 移除火焰抗性或保护，这里简单地产生粒子效果表示压制
                spawnSuppressionParticles(mob);
            }

            // 5. 霜心坠 vs 冰霜 (Thaw)
            IAffix frostyAffix = AffixRegistry.getAffix(new ResourceLocation("adversity:frosty"));
            if (hasFrostWard && frostyAffix != null && cap.hasAffix(frostyAffix)) {
                // 如果怪物有缓慢效果则清除? 或者防止它施加
                // 这里主要通过粒子效果反馈
                spawnSuppressionParticles(mob);
            }
        }
    }

    /**
     * 压制特定词条
     */
    private static void suppressAffix(EntityLiving mob, IAdversityCapability cap, String affixId, boolean forceRemove) {
        ResourceLocation id = new ResourceLocation(affixId);
        IAffix affix = AffixRegistry.getAffix(id);
        if (affix == null)
            return;

        if (cap.hasAffix(affix)) {
            // 在圣所内，或者概率判定成功，则完全移除词条
            // 为了"简单粗暴"，只要有反制饰品，就直接禁用该词条的效果
            // 由于目前capability没有"disabledAffixes"列表，我们通过临时移除或标记来处理
            // 这里我们选择直接移除词条 (最彻底的压制)

            if (forceRemove || mob.ticksExisted % 20 == 0) {
                cap.removeAffix(affix);
                spawnSuppressionParticles(mob);

                // 额外反馈：如果是窃取类词条，给一点伤害
                if (affixId.contains("shackle") || affixId.contains("divest")) {
                    mob.attackEntityFrom(net.minecraft.util.DamageSource.MAGIC, 1.0f);
                }
            }
        }
    }

    private static void spawnSuppressionParticles(EntityLiving mob) {
        if (mob.world instanceof WorldServer) {
            ((WorldServer) mob.world).spawnParticle(
                    EnumParticleTypes.SMOKE_NORMAL,
                    mob.posX, mob.posY + mob.height / 2, mob.posZ,
                    3, 0.3, 0.5, 0.3, 0.0);
        }
    }
}
