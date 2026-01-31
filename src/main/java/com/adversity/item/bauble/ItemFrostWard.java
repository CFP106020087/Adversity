package com.adversity.item.bauble;

import com.adversity.Adversity;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 霜心坠 - 防止冰霜(Frosty)词条伤害
 * 
 * 圣所内：完全免疫冰霜词条
 * 圣所外：50%减少冰霜层叠速度
 * 特殊效果：被冻结后2秒内再次受击获得3秒无敌
 */
public class ItemFrostWard extends AbstractWardBauble {

    // 记录玩家最近被冻结的时间
    private static final Map<UUID, Long> lastFreezeTime = new HashMap<>();
    
    // 冻结后的保护窗口（tick）
    private static final int PROTECTION_WINDOW = 40; // 2秒
    // 无敌持续时间（tick）
    private static final int INVULNERABILITY_DURATION = 60; // 3秒
    
    // 记录玩家的无敌结束时间
    private static final Map<UUID, Long> invulnerabilityEndTime = new HashMap<>();

    public ItemFrostWard() {
        super("frost_ward", "frosty");
    }

    @Override
    protected float getInSanctuaryStrength() {
        return 1.0f;
    }

    @Override
    protected float getOutsideSanctuaryStrength() {
        return 0.5f;
    }

    /**
     * 当玩家被完全冻结时调用
     */
    public void onFreeze(EntityPlayer player) {
        lastFreezeTime.put(player.getUniqueID(), player.world.getTotalWorldTime());
        Adversity.LOGGER.debug("Frost Ward recorded freeze for player {}", player.getName());
    }

    /**
     * 检查是否在保护窗口内
     */
    public boolean isInProtectionWindow(EntityPlayer player) {
        Long freezeTime = lastFreezeTime.get(player.getUniqueID());
        if (freezeTime == null) return false;
        
        long currentTime = player.world.getTotalWorldTime();
        return (currentTime - freezeTime) <= PROTECTION_WINDOW;
    }

    /**
     * 触发无敌效果（在保护窗口内受击时）
     */
    public void triggerInvulnerability(EntityPlayer player) {
        long endTime = player.world.getTotalWorldTime() + INVULNERABILITY_DURATION;
        invulnerabilityEndTime.put(player.getUniqueID(), endTime);
        
        // 清除冻结记录
        lastFreezeTime.remove(player.getUniqueID());
        
        // 音效
        player.world.playSound(null, player.posX, player.posY, player.posZ,
            SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.0f, 1.5f);
        
        Adversity.LOGGER.debug("Frost Ward triggered invulnerability for player {}", player.getName());
    }

    /**
     * 检查玩家是否处于无敌状态
     */
    public static boolean isInvulnerable(EntityPlayer player) {
        Long endTime = invulnerabilityEndTime.get(player.getUniqueID());
        if (endTime == null) return false;
        
        if (player.world.getTotalWorldTime() < endTime) {
            return true;
        } else {
            invulnerabilityEndTime.remove(player.getUniqueID());
            return false;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void addSpecialDescription(List<String> tooltip) {
        tooltip.add("");
        tooltip.add(I18n.format("adversity.bauble.frost_ward.special"));
    }
}
