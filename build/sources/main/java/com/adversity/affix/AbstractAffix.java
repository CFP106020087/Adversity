package com.adversity.affix;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collections;
import java.util.Set;

/**
 * 词条抽象基类 - 提供默认实现，方便创建具体词条
 */
public abstract class AbstractAffix implements IAffix {

    protected final ResourceLocation id;
    protected final AffixType type;
    protected final int weight;
    protected final float minDifficulty;
    protected final int minTier;
    protected final int maxTier;

    public AbstractAffix(ResourceLocation id, AffixType type, int weight, float minDifficulty) {
        this(id, type, weight, minDifficulty, 0, 0);
    }

    public AbstractAffix(ResourceLocation id, AffixType type, int weight, float minDifficulty, int minTier, int maxTier) {
        this.id = id;
        this.type = type;
        this.weight = weight;
        this.minDifficulty = minDifficulty;
        this.minTier = minTier;
        this.maxTier = maxTier;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public String getTranslationKey() {
        return "adversity.affix." + id.getPath();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public String getDisplayName() {
        String key = getTranslationKey();
        String translated = I18n.format(key);
        // 如果没有翻译，返回词条ID的路径部分
        return translated.equals(key) ? id.getPath() : translated;
    }

    @Override
    public AffixType getType() {
        return type;
    }

    @Override
    public int getWeight() {
        return weight;
    }

    @Override
    public float getMinDifficulty() {
        return minDifficulty;
    }

    @Override
    public int getMinTier() {
        return minTier;
    }

    @Override
    public int getMaxTier() {
        return maxTier;
    }

    @Override
    public boolean canApplyTo(EntityLiving entity) {
        // 默认可以应用到所有 EntityLiving
        return true;
    }

    @Override
    public boolean isCompatibleWith(IAffix other) {
        // 默认与所有词条兼容
        return true;
    }

    @Override
    public Set<ResourceLocation> getRequiredAffixes() {
        // 默认没有前置要求
        return Collections.emptySet();
    }

    // ==================== 默认空实现 ====================

    @Override
    public void onApply(EntityLiving entity, IAffixData data) {
        // 默认无操作
    }

    @Override
    public void onRemove(EntityLiving entity, IAffixData data) {
        // 默认无操作
    }

    @Override
    public void onTick(EntityLiving entity, IAffixData data) {
        // 默认无操作
    }

    @Override
    public float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data) {
        return damage; // 默认不修改伤害
    }

    @Override
    public float onHurt(EntityLiving entity, DamageSource source, float damage, IAffixData data) {
        return damage; // 默认不修改伤害
    }

    @Override
    public void onDeath(EntityLiving entity, DamageSource source, IAffixData data) {
        // 默认无操作
    }

    @Override
    public NBTTagCompound writeToNBT(IAffixData data) {
        return new NBTTagCompound();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, IAffixData data) {
        // 默认无操作
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || !(obj instanceof IAffix)) return false;
        return id.equals(((IAffix) obj).getId());
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Affix[" + id + "]";
    }
}
