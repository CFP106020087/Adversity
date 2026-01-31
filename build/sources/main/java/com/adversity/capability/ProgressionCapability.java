package com.adversity.capability;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;

import java.util.HashSet;
import java.util.Set;

/**
 * 玩家进度能力接口实现
 * 管理玩家已解锁的阶段 (GameStages)
 */
public class ProgressionCapability implements IAdversityCapability.IProgression {

    private final Set<String> unlockedStages = new HashSet<>();

    @Override
    public boolean hasStage(String stage) {
        return unlockedStages.contains(stage);
    }

    @Override
    public void addStage(String stage) {
        unlockedStages.add(stage);
    }

    @Override
    public void removeStage(String stage) {
        unlockedStages.remove(stage);
    }

    @Override
    public Set<String> getStages() {
        return new HashSet<>(unlockedStages);
    }

    @Override
    public void clear() {
        unlockedStages.clear();
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (String stage : unlockedStages) {
            list.appendTag(new NBTTagString(stage));
        }
        nbt.setTag("Stages", list);
        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        unlockedStages.clear();
        if (nbt.hasKey("Stages", Constants.NBT.TAG_LIST)) {
            NBTTagList list = nbt.getTagList("Stages", Constants.NBT.TAG_STRING);
            for (int i = 0; i < list.tagCount(); i++) {
                unlockedStages.add(list.getStringTagAt(i));
            }
        }
    }
}
