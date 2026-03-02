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
 * 
 * 阶段层级 (从高到低):
 * champion > warden > scholar > awakened
 * 高阶段自动包含低阶段的所有权限
 */
public class ProgressionCapability implements IAdversityCapability.IProgression {

    private final Set<String> unlockedStages = new HashSet<>();

    // 阶段层级定义 (索引越大等级越高)
    private static final String[] STAGE_HIERARCHY = { "awakened", "scholar", "warden", "champion" };

    @Override
    public boolean hasStage(String stage) {
        if (stage == null || stage.isEmpty())
            return true;

        // 规范化为小写进行比较
        String normalizedStage = stage.toLowerCase();

        // 直接检查是否拥有该阶段 (也需要小写比较)
        for (String playerStage : unlockedStages) {
            if (playerStage.equalsIgnoreCase(normalizedStage)) {
                return true;
            }
        }

        // 检查是否拥有更高层级的阶段
        int requiredLevel = getStageLevel(normalizedStage);
        if (requiredLevel < 0) {
            // 非层级阶段，只检查精确匹配
            return false;
        }

        // 检查玩家是否拥有任何更高层级的阶段
        for (String playerStage : unlockedStages) {
            int playerLevel = getStageLevel(playerStage.toLowerCase());
            if (playerLevel >= requiredLevel) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取阶段层级 (-1 表示非层级阶段)
     */
    private static int getStageLevel(String stage) {
        for (int i = 0; i < STAGE_HIERARCHY.length; i++) {
            if (STAGE_HIERARCHY[i].equals(stage)) {
                return i;
            }
        }
        return -1; // 非层级阶段
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
