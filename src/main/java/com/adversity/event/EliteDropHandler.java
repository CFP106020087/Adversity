package com.adversity.event;

import com.adversity.Adversity;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.item.ItemRegistry;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

/**
 * 精英怪物掉落处理器
 *
 * 掉落设计理念：
 * - 掉落率和数量与等级正相关
 * - 高等级材料只从高等级精英获取
 * - 玩家击杀才有掉落（防止自动化农场）
 * - 掉落位置在怪物身上，增强反馈感
 *
 * 掉落表：
 * T1-T4: 词条残渣 (Affix Essence) - 1~3个，100%概率
 * T5-T7: 词条水晶 (Affix Crystal) - 1~2个，50%-80%概率 + 残渣
 * T8-T10: 虚空碎片 (Void Shard) - 1个，30%-60%概率 + 水晶 + 残渣
 */
@Mod.EventBusSubscriber(modid = Adversity.MODID)
public class EliteDropHandler {

    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onEliteDeath(LivingDeathEvent event) {
        // 只在服务端处理
        if (event.getEntity().world.isRemote) return;

        // 只处理 EntityLiving
        if (!(event.getEntity() instanceof EntityLiving)) return;
        EntityLiving entity = (EntityLiving) event.getEntity();

        // 获取能力数据
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        if (cap == null || cap.getTier() <= 0) return;

        // 检查是否被玩家击杀
        DamageSource source = event.getSource();
        if (!isPlayerKill(source)) return;

        int tier = cap.getTier();
        World world = entity.world;
        BlockPos pos = entity.getPosition();

        // 根据等级决定掉落
        processDrops(world, pos, tier, cap.getDifficultyLevel());

        Adversity.LOGGER.debug("Elite T{} dropped loot at {}", tier, pos);
    }

    /**
     * 检查是否为玩家击杀
     */
    private static boolean isPlayerKill(DamageSource source) {
        if (source == null) return false;

        // 直接伤害来源是玩家
        if (source.getTrueSource() instanceof EntityPlayer) {
            return true;
        }

        // 间接伤害（如箭矢）的真正来源是玩家
        if (source.getImmediateSource() != null &&
            source.getTrueSource() instanceof EntityPlayer) {
            return true;
        }

        return false;
    }

    /**
     * 处理掉落物
     */
    private static void processDrops(World world, BlockPos pos, int tier, float difficulty) {
        // T1-T10 都掉落基础材料（词条残渣）
        dropEssence(world, pos, tier);

        // T5+ 掉落中级材料（词条水晶）
        if (tier >= 5) {
            dropCrystal(world, pos, tier);
        }

        // T8+ 掉落高级材料（虚空碎片）
        if (tier >= 8) {
            dropVoidShard(world, pos, tier);
        }

        // 额外掉落（基于难度的经验加成暂不实现，留作扩展）
    }

    /**
     * 掉落词条残渣
     * T1: 1个 (100%)
     * T2: 1-2个 (100%)
     * T3: 2个 (100%)
     * T4: 2-3个 (100%)
     * T5+: 3个 (100%)
     */
    private static void dropEssence(World world, BlockPos pos, int tier) {
        Item item = ItemRegistry.AFFIX_ESSENCE;
        if (item == null) return;

        int count;
        switch (tier) {
            case 1:
                count = 1;
                break;
            case 2:
                count = 1 + RANDOM.nextInt(2);
                break;
            case 3:
                count = 2;
                break;
            case 4:
                count = 2 + RANDOM.nextInt(2);
                break;
            default:
                count = 3;
                break;
        }

        spawnItemDrop(world, pos, item, count);
    }

    /**
     * 掉落词条水晶
     * T5: 1个 (50%)
     * T6: 1个 (65%)
     * T7: 1-2个 (80%)
     * T8+: 2个 (100%)
     */
    private static void dropCrystal(World world, BlockPos pos, int tier) {
        Item item = ItemRegistry.AFFIX_CRYSTAL;
        if (item == null) return;

        double chance;
        int count;

        switch (tier) {
            case 5:
                chance = 0.50;
                count = 1;
                break;
            case 6:
                chance = 0.65;
                count = 1;
                break;
            case 7:
                chance = 0.80;
                count = 1 + RANDOM.nextInt(2);
                break;
            default: // T8+
                chance = 1.0;
                count = 2;
                break;
        }

        if (RANDOM.nextDouble() < chance) {
            spawnItemDrop(world, pos, item, count);
        }
    }

    /**
     * 掉落虚空碎片
     * T8: 1个 (30%)
     * T9: 1个 (45%)
     * T10: 1-2个 (60%)
     */
    private static void dropVoidShard(World world, BlockPos pos, int tier) {
        Item item = ItemRegistry.VOID_SHARD;
        if (item == null) return;

        double chance;
        int count;

        switch (tier) {
            case 8:
                chance = 0.30;
                count = 1;
                break;
            case 9:
                chance = 0.45;
                count = 1;
                break;
            case 10:
                chance = 0.60;
                count = 1 + RANDOM.nextInt(2);
                break;
            default:
                return;
        }

        if (RANDOM.nextDouble() < chance) {
            spawnItemDrop(world, pos, item, count);
        }
    }

    /**
     * 在世界中生成掉落物
     */
    private static void spawnItemDrop(World world, BlockPos pos, Item item, int count) {
        if (count <= 0) return;

        ItemStack stack = new ItemStack(item, count);

        // 添加随机偏移，让掉落物更自然
        double x = pos.getX() + 0.5 + (RANDOM.nextDouble() - 0.5) * 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5 + (RANDOM.nextDouble() - 0.5) * 0.5;

        EntityItem entityItem = new EntityItem(world, x, y, z, stack);

        // 添加少量上抛速度，视觉效果更好
        entityItem.motionY = 0.2 + RANDOM.nextDouble() * 0.1;
        entityItem.motionX = (RANDOM.nextDouble() - 0.5) * 0.1;
        entityItem.motionZ = (RANDOM.nextDouble() - 0.5) * 0.1;

        // 设置无法拾取延迟（短暂，防止立即吸取）
        entityItem.setPickupDelay(10);

        world.spawnEntity(entityItem);
    }
}
