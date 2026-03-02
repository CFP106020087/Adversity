package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixData;
import com.adversity.affix.AffixRegistry;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.affix.IAffix;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 飞升词条 - 死亡时有概率升阶为更高级的防御词条
 *
 * 机制：
 * 1. 该生物死亡时有概率原地生成同类型怪物
 * 2. 新怪物继承所有词条，但防御词条会升阶：
 *    - 英雄 → 神明
 *    - 神明 → 外神
 * 3. 如果已经是外神，则不再升阶
 * 4. 升阶概率随tier增加
 */
public class AscensionAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "ascension");

    /** 基础升阶概率 */
    private static final float BASE_ASCENSION_CHANCE = 0.3f;  // 30%

    /** 每tier增加的概率 */
    private static final float CHANCE_PER_TIER = 0.05f;

    private static final Random RANDOM = new Random();

    public AscensionAffix() {
        super(
            ID,
            AffixType.SPECIAL,
            25,     // 低权重（强力词条）
            6.0f    // 难度6以上
        );
    }

    @Override
    public void onDeath(EntityLiving entity, DamageSource source, IAffixData data) {
        if (entity.world.isRemote) return;

        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        if (cap == null) return;

        int tier = cap.getTier();

        // 计算升阶概率
        float chance = BASE_ASCENSION_CHANCE + (tier * CHANCE_PER_TIER);

        // 检查是否有可升阶的词条
        ResourceLocation upgradeFrom = null;
        ResourceLocation upgradeTo = null;

        for (AffixData affixData : cap.getAllAffixData()) {
            IAffix affix = affixData.getAffix();
            ResourceLocation affixId = affix.getId();
            if (affixId.equals(HeroAffix.ID)) {
                upgradeFrom = HeroAffix.ID;
                upgradeTo = DivineAffix.ID;
                break;
            } else if (affixId.equals(DivineAffix.ID)) {
                upgradeFrom = DivineAffix.ID;
                upgradeTo = OuterGodAffix.ID;
                break;
            }
        }

        // 如果没有可升阶的词条，或已经是最高级，则不触发
        if (upgradeFrom == null) {
            return;
        }

        // 检查封魂之尘反制（检查击杀者）
        if (source.getTrueSource() instanceof net.minecraft.entity.player.EntityPlayer) {
            net.minecraft.entity.player.EntityPlayer player = (net.minecraft.entity.player.EntityPlayer) source
                    .getTrueSource();
            float sealReduction = com.adversity.item.bauble.BaubleHelper.getReduction(player, "ascension");
            if (sealReduction >= 1.0f) {
                // 完全阻止复活
                return;
            } else if (sealReduction > 0) {
                // 降低复活几率
                chance = chance * (1.0f - sealReduction);
            }
        }

        // 概率检查
        if (RANDOM.nextFloat() > chance) {
            return;
        }

        // 执行飞升
        performAscension(entity, cap, upgradeFrom, upgradeTo);
    }

    /**
     * 执行飞升 - 生成升阶后的怪物
     */
    private void performAscension(EntityLiving original, IAdversityCapability originalCap,
                                   ResourceLocation upgradeFrom, ResourceLocation upgradeTo) {
        World world = original.world;
        BlockPos pos = original.getPosition();

        // 播放飞升效果
        playAscensionEffects(world, pos);

        // 创建新实体（同类型）
        ResourceLocation entityId = EntityList.getKey(original);
        if (entityId == null) return;

        EntityLiving newEntity = (EntityLiving) EntityList.createEntityByIDFromName(entityId, world);
        if (newEntity == null) return;

        // 设置位置
        newEntity.setPosition(original.posX, original.posY, original.posZ);
        newEntity.rotationYaw = original.rotationYaw;
        newEntity.rotationPitch = original.rotationPitch;

        // 复制能力数据
        IAdversityCapability newCap = CapabilityHandler.getCapability(newEntity);
        if (newCap == null) {
            return;
        }

        // 设置tier（可以考虑升tier）
        newCap.setTier(originalCap.getTier());

        // 复制词条，替换升阶词条
        List<IAffix> newAffixes = new ArrayList<>();
        for (AffixData affixData : originalCap.getAllAffixData()) {
            IAffix affix = affixData.getAffix();
            if (affix.getId().equals(upgradeFrom)) {
                // 替换为升阶后的词条
                IAffix upgradedAffix = AffixRegistry.getAffix(upgradeTo);
                if (upgradedAffix != null) {
                    newAffixes.add(upgradedAffix);
                }
            } else {
                newAffixes.add(affix);
            }
        }

        // 应用词条
        for (IAffix affix : newAffixes) {
            newCap.addAffix(affix);
        }

        // 生成实体
        world.spawnEntity(newEntity);

        // 播放升阶完成效果
        playAscensionCompleteEffects(world, newEntity);
    }

    /**
     * 播放飞升开始效果
     */
    private void playAscensionEffects(World world, BlockPos pos) {
        // 音效
        world.playSound(
            null,
            pos.getX(), pos.getY(), pos.getZ(),
            SoundEvents.ENTITY_WITHER_SPAWN,
            SoundCategory.HOSTILE,
            1.0f,
            1.5f
        );

        // 粒子效果
        if (world instanceof WorldServer) {
            // 灵魂粒子向上升起
            ((WorldServer) world).spawnParticle(
                EnumParticleTypes.PORTAL,
                pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                100,
                0.5, 1.0, 0.5,
                0.5
            );
            ((WorldServer) world).spawnParticle(
                EnumParticleTypes.END_ROD,
                pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                50,
                0.5, 1.0, 0.5,
                0.1
            );
        }
    }

    /**
     * 播放飞升完成效果
     */
    private void playAscensionCompleteEffects(World world, EntityLiving newEntity) {
        // 音效
        world.playSound(
            null,
            newEntity.posX, newEntity.posY, newEntity.posZ,
            SoundEvents.ENTITY_ELDER_GUARDIAN_AMBIENT,
            SoundCategory.HOSTILE,
            1.0f,
            0.5f
        );

        // 粒子效果
        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(
                EnumParticleTypes.EXPLOSION_HUGE,
                newEntity.posX, newEntity.posY + 1, newEntity.posZ,
                1,
                0, 0, 0,
                0,
                new int[0]
            );
            ((WorldServer) world).spawnParticle(
                EnumParticleTypes.DRAGON_BREATH,
                newEntity.posX, newEntity.posY + 1, newEntity.posZ,
                50,
                1.0, 1.0, 1.0,
                0.1,
                new int[0]
            );
        }
    }

    @Override
    public boolean isCompatibleWith(IAffix other) {
        // 与分裂词条不兼容（避免无限分裂+升阶）
        if (other.getId().equals(SplittingAffix.ID)) {
            return false;
        }
        return super.isCompatibleWith(other);
    }

    @Override
    public Set<ResourceLocation> getRequiredAffixes() {
        // 飞升词条需要英雄或神明词条才能生效
        // 外神是最高级，不需要飞升
        Set<ResourceLocation> required = new HashSet<>();
        required.add(HeroAffix.ID);
        required.add(DivineAffix.ID);
        return required;
    }
}
