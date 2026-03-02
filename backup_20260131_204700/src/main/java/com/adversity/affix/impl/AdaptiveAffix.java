package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;

import java.util.LinkedList;

/**
 * 适应词条 - 重复伤害类型抗性
 *
 * 机制：
 * 1. 记录最近5次受到的伤害类型
 * 2. 重复的伤害类型每次+15%抗性
 * 3. 最高60%抗性
 * 4. 切换伤害类型可重置抗性
 * 5. 设计理念：强迫玩家使用多种伤害类型
 */
public class AdaptiveAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "adaptive");

    /** 每次重复增加的抗性 */
    private static final float RESISTANCE_PER_HIT = 0.15f;  // 15%

    /** 最大抗性 */
    private static final float MAX_RESISTANCE = 0.6f;  // 60%

    /** 记录的伤害类型数量 */
    private static final int HISTORY_SIZE = 5;

    public AdaptiveAffix() {
        super(
            ID,
            AffixType.DEFENSIVE,
            30,     // 较低权重（强力词条）
            8.0f    // 难度8以上出现
        );
    }

    @Override
    public float onHurt(EntityLiving entity, DamageSource source, float damage, IAffixData data) {
        String damageType = source.getDamageType();
        int tier = getTier(entity);

        // 获取伤害历史
        String historyKey = "adaptHistory";
        String history = data.getCustomString(historyKey);
        LinkedList<String> damageHistory = parseHistory(history);

        // 计算当前伤害类型的连续次数
        int repeatCount = countRepeats(damageHistory, damageType);

        // 计算抗性
        float resistance = repeatCount * RESISTANCE_PER_HIT;
        resistance = resistance * (1.0f + tier * 0.1f);  // tier加成
        resistance = Math.min(resistance, MAX_RESISTANCE);

        // 记录这次伤害
        damageHistory.addFirst(damageType);
        if (damageHistory.size() > HISTORY_SIZE) {
            damageHistory.removeLast();
        }
        data.setCustomString(historyKey, serializeHistory(damageHistory));

        // 应用抗性
        return damage * (1.0f - resistance);
    }

    /**
     * 计算伤害类型的连续重复次数
     */
    private int countRepeats(LinkedList<String> history, String damageType) {
        int count = 0;
        for (String type : history) {
            if (damageType.equals(type)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 解析历史字符串
     */
    private LinkedList<String> parseHistory(String history) {
        LinkedList<String> list = new LinkedList<>();
        if (history != null && !history.isEmpty()) {
            String[] parts = history.split(",");
            for (String part : parts) {
                if (!part.isEmpty()) {
                    list.add(part);
                }
            }
        }
        return list;
    }

    /**
     * 序列化历史列表
     */
    private String serializeHistory(LinkedList<String> history) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < history.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(history.get(i));
        }
        return sb.toString();
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
