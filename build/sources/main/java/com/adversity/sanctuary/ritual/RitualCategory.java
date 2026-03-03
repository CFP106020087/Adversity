package com.adversity.sanctuary.ritual;

/**
 * 仪式分类枚举
 */
public enum RitualCategory {
    STAGE_UNLOCK("stage_unlock"),
    CRAFTING("crafting"),
    DIFFICULTY("difficulty"),
    UTILITY("utility"),
    CUSTOM("custom");

    private final String id;

    RitualCategory(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static RitualCategory fromString(String name) {
        try {
            return RitualCategory.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CUSTOM;
        }
    }
}
