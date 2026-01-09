package com.adversity.debuff;

import com.adversity.client.visual.VisualEffectHelper;
import com.adversity.client.visual.VisualEffectType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Map;
import java.util.UUID;

/**
 * Debuff事件处理器
 * 处理debuff的tick更新、效果应用和视觉同步
 */
public class DebuffEventHandler {

    // 属性修改器UUID
    private static final UUID FROST_SLOW_UUID = UUID.fromString("a3b4c5d6-e7f8-9012-3456-789abcdef012");
    private static final UUID CORROSION_ARMOR_UUID = UUID.fromString("b4c5d6e7-f890-1234-5678-9abcdef01234");

    // 自定义伤害源
    public static final DamageSource CORROSION_DAMAGE = new DamageSource("adversity.corrosion")
        .setDamageBypassesArmor();
    public static final DamageSource BURNING_DAMAGE = new DamageSource("adversity.burning")
        .setFireDamage();
    public static final DamageSource VOID_DAMAGE = new DamageSource("adversity.void")
        .setDamageBypassesArmor()
        .setDamageIsAbsolute();

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            PlayerDebuffManager.tickAll();
        }
    }

    /** Debuff效果应用间隔 - 优化性能 */
    private static final int DEBUFF_CHECK_INTERVAL = 4;  // 每4tick检查一次 (5次/秒)

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;
        if (event.player.ticksExisted % DEBUFF_CHECK_INTERVAL != 0) return;  // 优化：不是每tick都运行

        EntityPlayer player = event.player;
        Map<DebuffType, PlayerDebuffManager.DebuffData> debuffs = PlayerDebuffManager.getAllDebuffs(player);

        // 如果没有debuff，直接返回
        if (debuffs.isEmpty()) return;

        for (Map.Entry<DebuffType, PlayerDebuffManager.DebuffData> entry : debuffs.entrySet()) {
            DebuffType type = entry.getKey();
            PlayerDebuffManager.DebuffData data = entry.getValue();

            // 应用debuff效果
            applyDebuffEffect(player, type, data);

            // 同步视觉效果
            if (type.hasVisualEffect()) {
                syncVisualEffect(player, type, data);
            }
        }

        // 清理不再有debuff的属性修改器
        cleanupModifiers(player, debuffs);
    }

    /**
     * 应用debuff效果
     */
    private void applyDebuffEffect(EntityPlayer player, DebuffType type, PlayerDebuffManager.DebuffData data) {
        float intensity = type.getEffectStrength(data.stacks);

        switch (type) {
            case FROST:
                applyFrostEffect(player, intensity);
                break;

            case CORROSION:
                applyCorrosionEffect(player, intensity, data);
                break;

            case BURNING:
                applyBurningEffect(player, intensity, data);
                break;

            case FEAR:
                applyFearEffect(player, intensity, data);
                break;

            case VOID_MARK:
                applyVoidMarkEffect(player, intensity, data);
                break;

            case DARKNESS:
            case BLOOD_DEBT:
            case GRAVITY_LOCK:
                // 这些debuff效果由词条直接处理或只有视觉效果
                break;
        }
    }

    /**
     * 冰霜效果 - 降低移动速度
     */
    private void applyFrostEffect(EntityPlayer player, float intensity) {
        IAttributeInstance speedAttr = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speedAttr == null) return;

        // 移除旧修改器
        speedAttr.removeModifier(FROST_SLOW_UUID);

        // 应用新修改器（降低移动速度）
        if (intensity > 0) {
            double slowAmount = -intensity * 0.8;  // 最多降低80%速度
            AttributeModifier modifier = new AttributeModifier(
                FROST_SLOW_UUID, "Adversity Frost Slow", slowAmount, 2
            );
            speedAttr.applyModifier(modifier);
        }

        // 满层时短暂完全冻结
        if (intensity >= 1.0f && player.ticksExisted % 20 == 0) {
            player.motionX = 0;
            player.motionZ = 0;
            player.velocityChanged = true;
        }
    }

    /**
     * 腐蚀效果 - 降低护甲 + 持续伤害
     */
    private void applyCorrosionEffect(EntityPlayer player, float intensity, PlayerDebuffManager.DebuffData data) {
        IAttributeInstance armorAttr = player.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.removeModifier(CORROSION_ARMOR_UUID);

            if (intensity > 0) {
                // 降低护甲（按百分比）
                double armorReduction = -armorAttr.getBaseValue() * intensity;
                AttributeModifier modifier = new AttributeModifier(
                    CORROSION_ARMOR_UUID, "Adversity Corrosion", armorReduction, 0
                );
                armorAttr.applyModifier(modifier);
            }
        }

        // 每40tick造成一次伤害
        if (player.ticksExisted % 40 == 0) {
            float damage = 1.0f + (data.stacks * 0.5f);  // 基础1点 + 每层0.5点
            player.attackEntityFrom(CORROSION_DAMAGE, damage);
        }
    }

    /**
     * 灼烧效果 - 火焰伤害，满层爆发
     */
    private void applyBurningEffect(EntityPlayer player, float intensity, PlayerDebuffManager.DebuffData data) {
        // 保持玩家燃烧
        if (player.ticksExisted % 20 == 0) {
            player.setFire(2);
        }

        // 满层时爆发伤害并清除层数
        if (data.stacks >= DebuffType.BURNING.getMaxStacks()) {
            float burstDamage = 6.0f;  // 爆发伤害
            player.attackEntityFrom(BURNING_DAMAGE, burstDamage);
            PlayerDebuffManager.removeDebuff(player, DebuffType.BURNING);

            // 爆炸效果
            player.world.newExplosion(null, player.posX, player.posY + 1, player.posZ,
                0.5f, true, false);
        }
    }

    /**
     * 恐惧效果 - 强制后退
     */
    private void applyFearEffect(EntityPlayer player, float intensity, PlayerDebuffManager.DebuffData data) {
        // 每10tick施加一次后退力
        if (player.ticksExisted % 10 == 0) {
            // 获取来源实体位置
            net.minecraft.entity.Entity source = player.world.getEntityByID(data.sourceEntityId);
            if (source != null) {
                double dx = player.posX - source.posX;
                double dz = player.posZ - source.posZ;
                double length = Math.sqrt(dx * dx + dz * dz);
                if (length > 0) {
                    dx /= length;
                    dz /= length;

                    // 后退力与强度成正比
                    double force = 0.3 * intensity;
                    player.motionX += dx * force;
                    player.motionZ += dz * force;
                    player.velocityChanged = true;
                }
            }
        }
    }

    /**
     * 虚空标记效果 - 穿透伤害
     */
    private void applyVoidMarkEffect(EntityPlayer player, float intensity, PlayerDebuffManager.DebuffData data) {
        // 每60tick造成穿透伤害
        if (player.ticksExisted % 60 == 0) {
            float damage = 2.0f * intensity;  // 最多2点穿透伤害
            player.attackEntityFrom(VOID_DAMAGE, damage);
        }
    }

    /**
     * 同步视觉效果到客户端
     */
    private void syncVisualEffect(EntityPlayer player, DebuffType type, PlayerDebuffManager.DebuffData data) {
        if (data.remainingTicks % 5 != 0) return;  // 每5tick同步一次

        VisualEffectType visualType = type.getVisualEffect();
        if (visualType == null) return;

        float intensity = type.getEffectStrength(data.stacks);
        int duration = Math.min(data.remainingTicks, 20);  // 最多同步20tick

        VisualEffectHelper.sendToPlayer(player, visualType, duration, intensity, data.sourceEntityId);
    }

    /**
     * 清理不再需要的属性修改器
     */
    private void cleanupModifiers(EntityPlayer player, Map<DebuffType, PlayerDebuffManager.DebuffData> debuffs) {
        // 如果没有冰霜debuff，移除减速
        if (!debuffs.containsKey(DebuffType.FROST)) {
            IAttributeInstance speedAttr = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
            if (speedAttr != null) {
                speedAttr.removeModifier(FROST_SLOW_UUID);
            }
        }

        // 如果没有腐蚀debuff，移除护甲减少
        if (!debuffs.containsKey(DebuffType.CORROSION)) {
            IAttributeInstance armorAttr = player.getEntityAttribute(SharedMonsterAttributes.ARMOR);
            if (armorAttr != null) {
                armorAttr.removeModifier(CORROSION_ARMOR_UUID);
            }
        }
    }

    /**
     * 处理伤害事件 - 应用血债加成
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        // 血债效果：增加吸血词条的吸血量
        PlayerDebuffManager.DebuffData bloodDebt = PlayerDebuffManager.getDebuff(player, DebuffType.BLOOD_DEBT);
        if (bloodDebt != null) {
            // 血债不直接增加伤害，而是记录在此供吸血词条使用
            // 吸血词条会检查这个debuff来增加吸血量
        }
    }
}
