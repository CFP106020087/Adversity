package com.adversity.sanctuary.ritual.effect;

import com.adversity.Adversity;
import com.adversity.curse.PermanentCurseManager;
import com.adversity.potion.PotionNegativeImmunity;
import com.adversity.sanctuary.TileEntitySanctuary;
import com.adversity.sanctuary.ritual.IRitualEffect;
import com.adversity.sanctuary.ritual.Rite;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 淨化詛咒效果
 *
 * 功能：
 * 1. 贖回指定類型的模組詛咒（PermanentCurseManager.redeemCurse）
 * 2. 清除當前所有負面藥水效果
 * 3. 授予 NEGATIVE_IMMUNITY potion 持續 immunity_duration tick
 *
 * JSON effect_params:
 *   curse_type: "black_swan" | "black_friday" | "black_coffin" | "all"
 *   purge_amount: float/int — 贖回量
 *   immunity_duration: int — 免疫持續 tick（可選，默認 0 = 不免疫）
 */
public class PurgeCurseEffect implements IRitualEffect {

    @Override
    public void apply(EntityPlayer player, Rite rite, TileEntitySanctuary sanctuary, Map<String, Object> params) {
        if (params == null) {
            Adversity.LOGGER.warn("[Adversity] PurgeCurseEffect called without params for rite {}", rite.getId());
            return;
        }

        // 1. 贖回詛咒
        String curseType = params.containsKey("curse_type") ? String.valueOf(params.get("curse_type")) : "all";
        float purgeAmount = 0.1f;
        if (params.containsKey("purge_amount")) {
            purgeAmount = ((Number) params.get("purge_amount")).floatValue();
        }

        PermanentCurseManager manager = PermanentCurseManager.get(player.world);

        if ("all".equals(curseType)) {
            manager.redeemCurse(player, PermanentCurseManager.CURSE_BLACK_SWAN, purgeAmount);
            manager.redeemCurse(player, PermanentCurseManager.CURSE_BLACK_FRIDAY, purgeAmount);
            manager.redeemCurse(player, PermanentCurseManager.CURSE_BLACK_COFFIN, purgeAmount);
        } else {
            manager.redeemCurse(player, curseType, purgeAmount);
        }

        // 同步詛咒數據到客戶端
        manager.syncToClient(player);

        // 2. 清除當前所有負面藥水效果
        List<Potion> toRemove = new ArrayList<>();
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getPotion().isBadEffect()) {
                toRemove.add(effect.getPotion());
            }
        }
        for (Potion potion : toRemove) {
            player.removePotionEffect(potion);
        }

        // 3. 授予免疫 buff（如果配置了 immunity_duration）
        int immunityDuration = 0;
        if (params.containsKey("immunity_duration")) {
            immunityDuration = ((Number) params.get("immunity_duration")).intValue();
        }
        if (immunityDuration > 0) {
            player.addPotionEffect(new PotionEffect(PotionNegativeImmunity.INSTANCE, immunityDuration, 0, false, true));
        }

        // 提示 + 粒子
        player.sendMessage(new TextComponentTranslation("adversity.ritual.purge_curse.success"));
        if (player.world instanceof WorldServer) {
            ((WorldServer) player.world).spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                    player.posX, player.posY + 1.0, player.posZ,
                    30, 0.5, 0.5, 0.5, 0.1);
        }
    }

    @Override
    public String getDescription() {
        return "Purge permanent curses and grant negative effect immunity";
    }
}
