package com.adversity.sanctuary;

import net.minecraft.util.IStringSerializable;

/**
 * 圣所模式
 */
public enum SanctuaryMode implements IStringSerializable {
    SAFE("safe"),   // 庇护模式：阻止精英生成 (默认)
    FARM("farm"),   // 狩猎模式：允许精英生成但限制等级 (T1-T4)
    EASE("ease");   // 压制模式：降低区域内难度数值

    private final String name;

    SanctuaryMode(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public static SanctuaryMode fromString(String name) {
        for (SanctuaryMode mode : values()) {
            if (mode.name.equals(name)) {
                return mode;
            }
        }
        return SAFE; // 默认为庇护模式
    }

    public SanctuaryMode next() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}
