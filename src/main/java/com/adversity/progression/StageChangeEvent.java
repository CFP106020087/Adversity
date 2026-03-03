package com.adversity.progression;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * 阶段变更事件
 * 允许其他系统监听/响应阶段变化
 */
public class StageChangeEvent extends Event {

    private final EntityPlayer player;
    private final String stage;
    private final boolean added;

    public StageChangeEvent(EntityPlayer player, String stage, boolean added) {
        this.player = player;
        this.stage = stage;
        this.added = added;
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    public String getStage() {
        return stage;
    }

    public boolean isAdded() {
        return added;
    }

    public boolean isRemoved() {
        return !added;
    }

    /**
     * 阶段变更前事件 - 可取消
     */
    @Cancelable
    public static class Pre extends StageChangeEvent {
        public Pre(EntityPlayer player, String stage, boolean added) {
            super(player, stage, added);
        }
    }

    /**
     * 阶段变更后事件 - 不可取消
     */
    public static class Post extends StageChangeEvent {
        public Post(EntityPlayer player, String stage, boolean added) {
            super(player, stage, added);
        }
    }
}
