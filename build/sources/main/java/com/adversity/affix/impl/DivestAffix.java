package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.client.visual.VisualEffectHelper;
import com.adversity.client.visual.VisualEffectType;
import com.adversity.seal.SealedItemManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Loader;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 褫夺词条 - 封印玩家的饰品槽（Baubles）
 *
 * 机制：
 * 1. 攻击玩家时有概率封印随机一件饰品
 * 2. 封印持续一段时间后自动解除
 * 3. 不会封印不可卸下的饰品（canUnequip = false）
 * 4. 设计理念：针对Baubles饰品流派玩家
 * 5. 如果Baubles未安装，则退化为封印主手/副手
 */
public class DivestAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "divest");

    /** 基础触发概率 */
    private static final float BASE_CHANCE = 0.25f;  // 25%

    /** 封印持续时间（tick） */
    private static final int BASE_SEAL_DURATION = 400;  // 20秒

    /** Baubles是否可用 */
    private static Boolean baublesLoaded = null;

    private static final Random RANDOM = new Random();

    public DivestAffix() {
        super(
            ID,
            AffixType.UTILITY,
            40,     // 较低权重
            6.0f    // 难度6以上
        );
    }

    /**
     * 检查Baubles是否已加载
     */
    private static boolean isBaublesLoaded() {
        if (baublesLoaded == null) {
            baublesLoaded = Loader.isModLoaded("baubles");
        }
        return baublesLoaded;
    }

    @Override
    public float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data) {
        if (!(target instanceof EntityPlayer)) {
            return damage;
        }

        EntityPlayer player = (EntityPlayer) target;
        int tier = getTier(attacker);

        // 计算触发概率
        float chance = BASE_CHANCE + (tier * 0.03f);

        if (RANDOM.nextFloat() < chance) {
            if (isBaublesLoaded()) {
                sealRandomBauble(player, attacker, tier);
            } else {
                // Baubles未加载，退化为封印手持物品
                sealHeldItem(player, attacker, tier);
            }
        }

        return damage;
    }

    /**
     * 封印随机一件Baubles饰品
     */
    private void sealRandomBauble(EntityPlayer player, EntityLiving attacker, int tier) {
        try {
            // 使用反射访问Baubles API，以避免硬依赖
            Class<?> baublesApiClass = Class.forName("baubles.api.BaublesApi");
            Object handler = baublesApiClass.getMethod("getBaublesHandler", EntityPlayer.class)
                .invoke(null, player);

            if (handler == null) {
                sealHeldItem(player, attacker, tier);
                return;
            }

            // 获取槽位数量
            int slots = (int) handler.getClass().getMethod("getSlots").invoke(handler);

            // 收集可封印的槽位
            List<Integer> availableSlots = new ArrayList<>();
            for (int i = 0; i < slots; i++) {
                ItemStack stack = (ItemStack) handler.getClass()
                    .getMethod("getStackInSlot", int.class).invoke(handler, i);

                if (!stack.isEmpty() && !SealedItemManager.isSealed(stack)) {
                    // 检查是否可以卸下
                    if (canUnequipBauble(stack, player)) {
                        availableSlots.add(i);
                    }
                }
            }

            if (availableSlots.isEmpty()) {
                return;
            }

            // 随机选择一个槽位
            int targetSlot = availableSlots.get(RANDOM.nextInt(availableSlots.size()));

            // 获取物品
            ItemStack targetItem = (ItemStack) handler.getClass()
                .getMethod("getStackInSlot", int.class).invoke(handler, targetSlot);

            // 计算封印时间
            long endTime = player.world.getTotalWorldTime() + BASE_SEAL_DURATION + (tier * 80);

            // 执行封印
            ItemStack sealedItem = SealedItemManager.sealItem(targetItem, endTime);
            handler.getClass().getMethod("setStackInSlot", int.class, ItemStack.class)
                .invoke(handler, targetSlot, sealedItem);

            // 播放效果
            playSealEffects(player, attacker);
            VisualEffectHelper.sendToPlayer(player, VisualEffectType.VOID_GAZE, 30, 0.5f, attacker.getEntityId());

            Adversity.LOGGER.debug("Sealed bauble in slot {} for player {}", targetSlot, player.getName());

        } catch (Exception e) {
            Adversity.LOGGER.debug("Failed to access Baubles API: {}", e.getMessage());
            sealHeldItem(player, attacker, tier);
        }
    }

    /**
     * 检查Bauble是否可以卸下
     */
    private boolean canUnequipBauble(ItemStack stack, EntityPlayer player) {
        try {
            Class<?> iBaubleClass = Class.forName("baubles.api.IBauble");
            if (!iBaubleClass.isInstance(stack.getItem())) {
                return true;  // 不是IBauble，可以卸下
            }

            // 调用canUnequip方法
            Object result = iBaubleClass.getMethod("canUnequip", ItemStack.class, EntityLivingBase.class)
                .invoke(stack.getItem(), stack, player);

            return (boolean) result;
        } catch (Exception e) {
            return true;  // 默认可以卸下
        }
    }

    /**
     * 封印手持物品（Baubles未加载时的退化行为）
     */
    private void sealHeldItem(EntityPlayer player, EntityLiving attacker, int tier) {
        // 尝试封印主手或副手
        ItemStack mainHand = player.getHeldItemMainhand();
        ItemStack offHand = player.getHeldItemOffhand();

        ItemStack targetItem = null;
        boolean isMainHand = false;

        if (!mainHand.isEmpty() && !SealedItemManager.isSealed(mainHand)) {
            if (!offHand.isEmpty() && !SealedItemManager.isSealed(offHand) && RANDOM.nextBoolean()) {
                targetItem = offHand;
            } else {
                targetItem = mainHand;
                isMainHand = true;
            }
        } else if (!offHand.isEmpty() && !SealedItemManager.isSealed(offHand)) {
            targetItem = offHand;
        }

        if (targetItem == null) {
            return;
        }

        // 计算封印时间
        long endTime = player.world.getTotalWorldTime() + BASE_SEAL_DURATION + (tier * 80);

        // 执行封印
        ItemStack sealedItem = SealedItemManager.sealItem(targetItem, endTime);
        if (isMainHand) {
            player.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, sealedItem);
        } else {
            player.setHeldItem(net.minecraft.util.EnumHand.OFF_HAND, sealedItem);
        }

        // 播放效果
        playSealEffects(player, attacker);
        VisualEffectHelper.sendToPlayer(player, VisualEffectType.VOID_GAZE, 30, 0.5f, attacker.getEntityId());
    }

    /**
     * 播放封印效果
     */
    private void playSealEffects(EntityPlayer player, EntityLiving attacker) {
        if (player.world.isRemote) return;

        // 音效
        player.world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            SoundEvents.ITEM_ARMOR_EQUIP_CHAIN,
            SoundCategory.HOSTILE,
            0.8f,
            0.6f
        );

        player.world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            SoundEvents.ENTITY_ENDERMEN_TELEPORT,
            SoundCategory.HOSTILE,
            0.3f,
            0.5f
        );

        // 粒子效果
        if (player.world instanceof WorldServer) {
            ((WorldServer) player.world).spawnParticle(
                EnumParticleTypes.PORTAL,
                player.posX, player.posY + 1, player.posZ,
                30,
                0.5, 0.5, 0.5,
                0.3
            );
        }
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
