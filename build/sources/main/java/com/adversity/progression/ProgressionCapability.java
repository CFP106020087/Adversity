package com.adversity.progression;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * 进度能力实现
 */
public class ProgressionCapability implements IProgression {

    @CapabilityInject(IProgression.class)
    public static Capability<IProgression> CAPABILITY = null;

    // 预定义阶段常量
    public static final String STAGE_UNINITIATED = "uninitiated";
    public static final String STAGE_AWAKENED = "awakened";
    public static final String STAGE_SCHOLAR = "scholar";
    public static final String STAGE_WARDEN = "warden";
    public static final String STAGE_CHAMPION = "champion";

    private final Set<String> stages = new HashSet<>();
    private boolean dirty = false;

    // 阶段层级定义 (索引越大等级越高): champion > warden > scholar > awakened
    private static final String[] STAGE_HIERARCHY = { "awakened", "scholar", "warden", "champion" };

    @Override
    public boolean hasStage(String stage) {
        if (stage == null || stage.isEmpty())
            return true;

        String normalizedStage = stage.toLowerCase();

        // 直接检查是否拥有该阶段
        if (stages.contains(normalizedStage)) {
            return true;
        }

        // 检查是否拥有更高层级的阶段
        int requiredLevel = getStageLevel(normalizedStage);
        if (requiredLevel < 0) {
            // 非层级阶段，只检查精确匹配
            return false;
        }

        // 检查玩家是否拥有任何更高层级的阶段
        for (String playerStage : stages) {
            int playerLevel = getStageLevel(playerStage);
            if (playerLevel >= requiredLevel) {
                return true;
            }
        }
        return false;
    }

    private static int getStageLevel(String stage) {
        for (int i = 0; i < STAGE_HIERARCHY.length; i++) {
            if (STAGE_HIERARCHY[i].equals(stage)) {
                return i;
            }
        }
        return -1; // 非层级阶段
    }

    @Override
    public boolean addStage(String stage) {
        boolean added = stages.add(stage.toLowerCase());
        if (added) {
            markDirty();
        }
        return added;
    }

    @Override
    public boolean removeStage(String stage) {
        boolean removed = stages.remove(stage.toLowerCase());
        if (removed) {
            markDirty();
        }
        return removed;
    }

    @Override
    public Set<String> getStages() {
        return new HashSet<>(stages);
    }

    @Override
    public void clearStages() {
        stages.clear();
        markDirty();
    }

    @Override
    public int getHighestTier() {
        if (hasStage(STAGE_CHAMPION)) return 4;
        if (hasStage(STAGE_WARDEN)) return 3;
        if (hasStage(STAGE_SCHOLAR)) return 2;
        if (hasStage(STAGE_AWAKENED)) return 1;
        return 0;
    }

    @Override
    public void markDirty() {
        this.dirty = true;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void clearDirty() {
        this.dirty = false;
    }

    // ==================== NBT 序列化 ====================

    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (String stage : stages) {
            list.appendTag(new NBTTagString(stage));
        }
        nbt.setTag("stages", list);
        return nbt;
    }

    public void deserializeNBT(NBTTagCompound nbt) {
        stages.clear();
        NBTTagList list = nbt.getTagList("stages", Constants.NBT.TAG_STRING);
        for (int i = 0; i < list.tagCount(); i++) {
            stages.add(list.getStringTagAt(i));
        }
    }

    // ==================== Capability Storage ====================

    public static class Storage implements Capability.IStorage<IProgression> {
        @Nullable
        @Override
        public NBTBase writeNBT(Capability<IProgression> capability, IProgression instance, EnumFacing side) {
            if (instance instanceof ProgressionCapability) {
                return ((ProgressionCapability) instance).serializeNBT();
            }
            return new NBTTagCompound();
        }

        @Override
        public void readNBT(Capability<IProgression> capability, IProgression instance, EnumFacing side, NBTBase nbt) {
            if (instance instanceof ProgressionCapability && nbt instanceof NBTTagCompound) {
                ((ProgressionCapability) instance).deserializeNBT((NBTTagCompound) nbt);
            }
        }
    }

    // ==================== Capability Provider ====================

    public static class Provider implements ICapabilitySerializable<NBTTagCompound> {
        private final ProgressionCapability instance = new ProgressionCapability();

        @Override
        public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
            return capability == CAPABILITY;
        }

        @Nullable
        @Override
        public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
            if (capability == CAPABILITY) {
                return CAPABILITY.cast(instance);
            }
            return null;
        }

        @Override
        public NBTTagCompound serializeNBT() {
            return instance.serializeNBT();
        }

        @Override
        public void deserializeNBT(NBTTagCompound nbt) {
            instance.deserializeNBT(nbt);
        }
    }
}
