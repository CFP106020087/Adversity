package com.adversity.handler;

import com.adversity.item.bauble.AbstractWardBauble;
import com.adversity.item.bauble.BaubleHelper;
import com.adversity.item.ItemRegistry;
import com.adversity.sanctuary.SanctuaryManager;
import com.adversity.sanctuary.SanctuaryZone;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 饰品效果处理器 - 处理饰品的通用被动效果
 */
@Mod.EventBusSubscriber
public class BaubleEffectHandler {

    // 属性修改器UUID
    private static final UUID GUARDIAN_HEART_HEALTH_UUID = UUID.fromString("b1c2d3e4-1111-2222-3333-444455556666");
    private static final UUID SPATIAL_ANCHOR_ATTACK_UUID = UUID.fromString("b1c2d3e4-2222-3333-4444-555566667777");
    private static final UUID SOUL_CHAIN_ARMOR_UUID = UUID.fromString("b1c2d3e4-3333-4444-5555-666677778888");
    private static final UUID SOUL_CHAIN_TOUGHNESS_UUID = UUID.fromString("b1c2d3e4-4444-5555-6666-777788889999");
    private static final UUID FROST_WARD_SPEED_UUID = UUID.fromString("b1c2d3e4-5555-6666-7777-888899990000");
    private static final UUID COURAGE_CHARM_ATTACK_SPEED_UUID = UUID.fromString("b1c2d3e4-6666-7777-8888-999900001111");

    // 保命冷却追踪
    private static final Map<UUID, Long> lastSavedTime = new HashMap<>();
    private static final long SAVE_COOLDOWN_TICKS = 3600; // 3分钟

    /**
     * 每tick处理玩家属性加成
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START)
            return;
        if (event.player.world.isRemote)
            return;
        if (event.player.ticksExisted % 20 != 0)
            return; // 每秒检查一次

        EntityPlayer player = event.player;
        float tierMultiplier = getTierMultiplier(player);

        // 守护之心: +4生命值
        updateHealthBonus(player, tierMultiplier);

        // 空间锚点: +15%攻击力
        updateAttackBonus(player, tierMultiplier);

        // 灵魂锁链: +4护甲, +2韧性
        updateArmorBonus(player, tierMultiplier);

        // 霜心坠: +15%移速
        updateSpeedBonus(player, tierMultiplier);

        // 勇气护符: +25%攻速
        updateAttackSpeedBonus(player, tierMultiplier);

        // 澄明之眼: 永久夜视
        updateNightVision(player);

        // 净焰之环: 火焰免疫 (通过药水效果)
        updateFireResistance(player);
    }

    /**
     * 守护之心: 低血保命 + 减伤
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerDamage(LivingDamageEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer))
            return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        float tierMultiplier = getTierMultiplier(player);

        // 守护之心: 30%血以下减伤20%
        if (hasBauble(player, ItemRegistry.GUARDIAN_HEART)) {
            if (player.getHealth() / player.getMaxHealth() < 0.3f) {
                float reduction = 0.20f * tierMultiplier;
                event.setAmount(event.getAmount() * (1.0f - reduction));
            }

            // 全局伤害减免 (按设计是15%)
            // 这里按圣所Tier调整
        }

        // 灵魂锁链: 物理减伤10%
        if (hasBauble(player, ItemRegistry.SOUL_CHAIN)) {
            DamageSource source = event.getSource();
            if (!source.isMagicDamage() && !source.isFireDamage()) {
                float reduction = 0.10f * tierMultiplier;
                event.setAmount(event.getAmount() * (1.0f - reduction));
            }
        }
    }

    /**
     * 守护之心: 致命保护 (50%保命)
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onFatalDamage(LivingDamageEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer))
            return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        if (!hasBauble(player, ItemRegistry.GUARDIAN_HEART))
            return;

        // 检查是否会致死
        if (player.getHealth() - event.getAmount() <= 0) {
            UUID uuid = player.getUniqueID();
            long currentTime = player.world.getTotalWorldTime();

            // 检查冷却
            if (lastSavedTime.containsKey(uuid)) {
                if (currentTime - lastSavedTime.get(uuid) < SAVE_COOLDOWN_TICKS) {
                    return; // 还在冷却中
                }
            }

            // 50%概率保命
            if (player.world.rand.nextFloat() < 0.5f) {
                event.setAmount(player.getHealth() - 1.0f); // 保留1血
                player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 100, 1));
                player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 60, 2));
                lastSavedTime.put(uuid, currentTime);
            }
        }
    }

    /**
     * 空间锚点: 击杀回血
     */
    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        DamageSource source = event.getSource();
        if (source.getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) source.getTrueSource();

            // 空间锚点: 击杀恢复1心
            if (hasBauble(player, ItemRegistry.SPATIAL_ANCHOR)) {
                player.heal(2.0f);
            }
        }
    }

    /**
     * 勇气护符: 低血狂暴 (+50%攻击)
     */
    @SubscribeEvent
    public static void onPlayerAttack(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getTrueSource() instanceof EntityPlayer))
            return;
        EntityPlayer player = (EntityPlayer) source.getTrueSource();

        float tierMultiplier = getTierMultiplier(player);

        // 勇气护符: 30%血以下攻击+50%
        if (hasBauble(player, ItemRegistry.COURAGE_CHARM)) {
            if (player.getHealth() / player.getMaxHealth() < 0.3f) {
                float bonus = 0.50f * tierMultiplier;
                event.setAmount(event.getAmount() * (1.0f + bonus));
            }
        }

        // 空间锚点: 暴击伤害+25% (简化：随机触发)
        if (hasBauble(player, ItemRegistry.SPATIAL_ANCHOR)) {
            if (player.world.rand.nextFloat() < 0.25f) { // 简化的暴击判定
                float bonus = 0.25f * tierMultiplier;
                event.setAmount(event.getAmount() * (1.0f + bonus));
            }
        }
    }

    // ========== 属性更新方法 ==========

    private static void updateHealthBonus(EntityPlayer player, float tierMultiplier) {
        IAttributeInstance attr = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (attr == null)
            return;

        // 移除旧修改器
        AttributeModifier old = attr.getModifier(GUARDIAN_HEART_HEALTH_UUID);
        if (old != null)
            attr.removeModifier(old);

        if (hasBauble(player, ItemRegistry.GUARDIAN_HEART)) {
            double bonus = 4.0 * tierMultiplier; // +4生命 (2心)
            attr.applyModifier(new AttributeModifier(GUARDIAN_HEART_HEALTH_UUID,
                    "adversity.guardian_heart_health", bonus, 0));
        }
    }

    private static void updateAttackBonus(EntityPlayer player, float tierMultiplier) {
        IAttributeInstance attr = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        if (attr == null)
            return;

        AttributeModifier old = attr.getModifier(SPATIAL_ANCHOR_ATTACK_UUID);
        if (old != null)
            attr.removeModifier(old);

        if (hasBauble(player, ItemRegistry.SPATIAL_ANCHOR)) {
            double bonus = 0.15 * tierMultiplier; // +15%攻击
            attr.applyModifier(new AttributeModifier(SPATIAL_ANCHOR_ATTACK_UUID,
                    "adversity.spatial_anchor_attack", bonus, 2));
        }
    }

    private static void updateArmorBonus(EntityPlayer player, float tierMultiplier) {
        IAttributeInstance armorAttr = player.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        IAttributeInstance toughnessAttr = player.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS);

        if (armorAttr != null) {
            AttributeModifier old = armorAttr.getModifier(SOUL_CHAIN_ARMOR_UUID);
            if (old != null)
                armorAttr.removeModifier(old);

            if (hasBauble(player, ItemRegistry.SOUL_CHAIN)) {
                double bonus = 4.0 * tierMultiplier; // +4护甲
                armorAttr.applyModifier(new AttributeModifier(SOUL_CHAIN_ARMOR_UUID,
                        "adversity.soul_chain_armor", bonus, 0));
            }
        }

        if (toughnessAttr != null) {
            AttributeModifier old = toughnessAttr.getModifier(SOUL_CHAIN_TOUGHNESS_UUID);
            if (old != null)
                toughnessAttr.removeModifier(old);

            if (hasBauble(player, ItemRegistry.SOUL_CHAIN)) {
                double bonus = 2.0 * tierMultiplier; // +2韧性
                toughnessAttr.applyModifier(new AttributeModifier(SOUL_CHAIN_TOUGHNESS_UUID,
                        "adversity.soul_chain_toughness", bonus, 0));
            }
        }
    }

    private static void updateSpeedBonus(EntityPlayer player, float tierMultiplier) {
        IAttributeInstance attr = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (attr == null)
            return;

        AttributeModifier old = attr.getModifier(FROST_WARD_SPEED_UUID);
        if (old != null)
            attr.removeModifier(old);

        if (hasBauble(player, ItemRegistry.FROST_WARD)) {
            double bonus = 0.15 * tierMultiplier; // +15%移速
            attr.applyModifier(new AttributeModifier(FROST_WARD_SPEED_UUID,
                    "adversity.frost_ward_speed", bonus, 2));
        }
    }

    private static void updateAttackSpeedBonus(EntityPlayer player, float tierMultiplier) {
        IAttributeInstance attr = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED);
        if (attr == null)
            return;

        AttributeModifier old = attr.getModifier(COURAGE_CHARM_ATTACK_SPEED_UUID);
        if (old != null)
            attr.removeModifier(old);

        if (hasBauble(player, ItemRegistry.COURAGE_CHARM)) {
            double bonus = 0.25 * tierMultiplier; // +25%攻速
            attr.applyModifier(new AttributeModifier(COURAGE_CHARM_ATTACK_SPEED_UUID,
                    "adversity.courage_charm_attack_speed", bonus, 2));
        }
    }

    private static void updateNightVision(EntityPlayer player) {
        if (hasBauble(player, ItemRegistry.CLARITY_LENS)) {
            // 永久夜视
            if (!player.isPotionActive(MobEffects.NIGHT_VISION) ||
                    player.getActivePotionEffect(MobEffects.NIGHT_VISION).getDuration() < 400) {
                player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 600, 0, true, false));
            }
        }
    }

    private static void updateFireResistance(EntityPlayer player) {
        if (hasBauble(player, ItemRegistry.FLAME_WARD)) {
            // 火焰抗性
            if (!player.isPotionActive(MobEffects.FIRE_RESISTANCE) ||
                    player.getActivePotionEffect(MobEffects.FIRE_RESISTANCE).getDuration() < 400) {
                player.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE, 600, 0, true, false));
            }
        }
    }

    // ========== 辅助方法 ==========

    private static boolean hasBauble(EntityPlayer player, Item item) {
        return BaubleHelper.hasBauble(player, item);
    }

    /**
     * 获取Tier加成倍率
     * T1=0.7, T2=0.85, T3=1.0, T4=1.2
     */
    private static float getTierMultiplier(EntityPlayer player) {
        SanctuaryZone zone = SanctuaryManager.getPlayerSanctuary(player);
        if (zone == null || !zone.isActive()) {
            return 0.7f; // 圣所外按T1计算
        }

        int tier = zone.tier;
        switch (tier) {
            case 1:
                return 0.7f;
            case 2:
                return 0.85f;
            case 3:
                return 1.0f;
            case 4:
                return 1.2f;
            default:
                return 0.7f;
        }
    }
}
