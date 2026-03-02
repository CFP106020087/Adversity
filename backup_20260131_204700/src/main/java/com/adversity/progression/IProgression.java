package com.adversity.progression;

import java.util.Set;

/**
 * 进度阶段接口
 * 用于追踪玩家的游戏进度阶段
 */
public interface IProgression {

    /**
     * 检查玩家是否拥有特定阶段
     */
    boolean hasStage(String stage);

    /**
     * 添加阶段
     * @return true 如果阶段是新添加的
     */
    boolean addStage(String stage);

    /**
     * 移除阶段
     * @return true 如果阶段被成功移除
     */
    boolean removeStage(String stage);

    /**
     * 获取所有阶段
     */
    Set<String> getStages();

    /**
     * 清除所有阶段
     */
    void clearStages();

    /**
     * 获取当前最高阶段等级 (0-4)
     * 0=Uninitiated, 1=Awakened, 2=Scholar, 3=Warden, 4=Champion
     */
    int getHighestTier();

    /**
     * 标记需要同步到客户端
     */
    void markDirty();

    /**
     * 是否需要同步
     */
    boolean isDirty();

    /**
     * 清除脏标记
     */
    void clearDirty();
}
