package com.adversity.sanctuary.ritual.effect;

import com.adversity.Adversity;
import com.adversity.network.PacketHandler;
import com.adversity.network.PacketOpenAwakeningGUI;
import com.adversity.sanctuary.TileEntitySanctuary;
import com.adversity.sanctuary.ritual.IRitualEffect;
import com.adversity.sanctuary.ritual.Rite;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.Potion;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 覺醒效果
 *
 * 功能：開啟自選效果 GUI，玩家選擇 N 個正面效果持續 M tick。
 * 支持黑名單/白名單模式。
 *
 * JSON effect_params:
 * select_count: int — 可選效果數量
 * duration_ticks: int — 持續時間
 * use_blacklist: boolean — true=黑名單（排除 filter_list），false=白名單
 * filter_list: string[] — 藥水 registry name 列表
 */
public class AwakeningEffect implements IRitualEffect {

    @Override
    public void apply(EntityPlayer player, Rite rite, TileEntitySanctuary sanctuary, Map<String, Object> params) {
        int durationTicks = 600;
        int selectCount = 3;
        boolean useBlacklist = true;
        List<String> filterList = new ArrayList<>();

        if (params != null) {
            if (params.containsKey("duration_ticks")) {
                durationTicks = ((Number) params.get("duration_ticks")).intValue();
            }
            if (params.containsKey("select_count")) {
                selectCount = ((Number) params.get("select_count")).intValue();
            }
            if (params.containsKey("use_blacklist")) {
                useBlacklist = (Boolean) params.get("use_blacklist");
            }
            if (params.containsKey("filter_list")) {
                Object fl = params.get("filter_list");
                if (fl instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> list = (List<String>) fl;
                    filterList.addAll(list);
                }
            }
        }

        if (!(player instanceof EntityPlayerMP))
            return;

        // 收集可選藥水列表
        List<String> availablePotions = new ArrayList<>();
        for (Potion potion : Potion.REGISTRY) {
            if (potion.isBadEffect())
                continue; // 只正面效果
            String regName = Potion.REGISTRY.getNameForObject(potion).toString();

            if (useBlacklist) {
                // 黑名單：排除 filter_list 中的
                if (!filterList.contains(regName)) {
                    availablePotions.add(regName);
                }
            } else {
                // 白名單：只包含 filter_list 中的
                if (filterList.contains(regName)) {
                    availablePotions.add(regName);
                }
            }
        }

        if (availablePotions.isEmpty()) {
            player.sendMessage(new TextComponentTranslation("adversity.ritual.awakening.no_effects"));
            Adversity.LOGGER.warn("[Adversity] Awakening: no available effects after filtering");
            return;
        }

        // 確保 selectCount 不超過可用數量
        selectCount = Math.min(selectCount, availablePotions.size());

        // 發送 GUI 開啟封包
        PacketHandler.INSTANCE.sendTo(
                new PacketOpenAwakeningGUI(
                        availablePotions.toArray(new String[0]),
                        selectCount,
                        durationTicks),
                (EntityPlayerMP) player);

        Adversity.LOGGER.info("[Adversity] Awakening GUI sent to {} ({} potions, select {})",
                player.getName(), availablePotions.size(), selectCount);
    }

    @Override
    public String getDescription() {
        return "Awakening ritual: choose positive effects";
    }
}
