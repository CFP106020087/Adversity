package com.adversity.enchantment;

import com.adversity.Adversity;
import com.adversity.affix.AffixRegistry;
import com.adversity.affix.IAffix;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 附魔事件处理器
 * 处理主动反制逻辑
 */
@Mod.EventBusSubscriber(modid = Adversity.MODID)
public class EnchantmentHandler {

    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer))
            return;
        if (!(event.getEntityLiving() instanceof EntityLiving))
            return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        EntityLiving target = (EntityLiving) event.getEntityLiving();

        // 获取目标能力
        IAdversityCapability cap = CapabilityHandler.getCapability(target);
        if (cap == null || cap.getTier() <= 0)
            return;

        // 1. 灵魂绑定 (Soulbound) -> 阻止装备被封印
        // 攻击时有几率移除目标的枷锁词条
        int soulboundLevel = EnchantmentHelper.getMaxEnchantmentLevel(EnchantmentRegistry.SOULBOUND, player);
        if (soulboundLevel > 0) {
            ResourceLocation shackleId = new ResourceLocation("adversity:shackle");
            IAffix shackleAffix = AffixRegistry.getAffix(shackleId);
            if (shackleAffix != null && cap.hasAffix(shackleAffix) && RANDOM.nextFloat() < 0.25f * soulboundLevel) {
                removeAffix(target, cap, shackleId);
            }
        }

        // 2. 破咒者 (Breaker) -> 移除精英词条buff
        // 攻击时有几率移除一个随机词条
        int breakerLevel = EnchantmentHelper.getEnchantmentLevel(EnchantmentRegistry.BREAKER,
                player.getHeldItemMainhand());
        if (breakerLevel > 0) {
            // Lv1: 10%, Lv2: 20%, Lv3: 30%
            float chance = 0.10f * breakerLevel;
            if (RANDOM.nextFloat() < chance) {
                // 尝试移除一个随机词条
                List<IAffix> affixes = new ArrayList<>();
                for (com.adversity.affix.AffixData data : cap.getAllAffixData()) {
                    affixes.add(data.getAffix());
                }

                if (!affixes.isEmpty()) {
                    IAffix toRemove = affixes.get(RANDOM.nextInt(affixes.size()));
                    removeAffix(target, cap, toRemove.getId());
                    Adversity.LOGGER.debug("Breaker removed affix: {} from {}",
                            toRemove.getId(), target.getName());
                }
            }
        }

        // 注意：净化之触(PurifyingTouch)已改为缩短封印时间，不再移除词条
        // 其效果在受到封印时的Handler中处理
    }

    private static void removeAffix(EntityLiving mob, IAdversityCapability cap, ResourceLocation id) {
        IAffix affix = AffixRegistry.getAffix(id);
        if (affix != null) {
            if (cap.removeAffix(affix)) {
                // 视觉反馈
                if (mob.world instanceof WorldServer) {
                    ((WorldServer) mob.world).spawnParticle(
                            EnumParticleTypes.CRIT_MAGIC,
                            mob.posX, mob.posY + mob.height / 2, mob.posZ,
                            10, 0.5, 0.5, 0.5, 0.1);
                }
            }
        }
    }
}
