package com.adversity.item.bauble;

import com.adversity.Adversity;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 守护之心 - 防止永久诅咒(BlackSwan/BlackFriday/BlackCoffin)
 * 
 * 圣所内：完全免疫永久诅咒
 * 圣所外：75%减少诅咒触发率
 * 特殊效果：每阻止1次诅咒积累1层"守护"，10层后可触发群体护盾
 */
public class ItemGuardianHeart extends AbstractWardBauble {

    // 记录玩家的守护层数
    private static final Map<UUID, Integer> guardianStacks = new HashMap<>();
    private static final int MAX_STACKS = 10;
    private static final int SHIELD_RADIUS = 8;
    private static final int SHIELD_DURATION = 100; // 5秒

    public ItemGuardianHeart() {
        super("guardian_heart", "curse");
    }

    @Override
    protected float getInSanctuaryStrength() {
        return 1.0f;
    }

    @Override
    protected float getOutsideSanctuaryStrength() {
        return 0.75f; // 75%减少
    }

    @Override
    public void onEffectTriggered(EntityPlayer player, @Nullable EntityLivingBase source, boolean blocked) {
        if (blocked) {
            addGuardianStack(player);
        }
    }

    /**
     * 添加守护层数
     */
    private void addGuardianStack(EntityPlayer player) {
        UUID uuid = player.getUniqueID();
        int current = guardianStacks.getOrDefault(uuid, 0);
        current++;

        if (current >= MAX_STACKS) {
            // 触发群体护盾
            triggerGroupShield(player);
            guardianStacks.put(uuid, 0);
        } else {
            guardianStacks.put(uuid, current);

            // 提示音
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS,
                    0.5f, 0.8f + current * 0.1f);
        }
    }

    /**
     * 触发群体护盾效果
     */
    private void triggerGroupShield(EntityPlayer player) {
        // 音效
        player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.BLOCK_ENDERCHEST_OPEN, SoundCategory.PLAYERS, 1.5f, 1.0f);

        // TODO: 对周围玩家施加保护效果
        // 这里简化为给自己添加吸收效果
        player.setAbsorptionAmount(player.getAbsorptionAmount() + 8.0f);

        Adversity.LOGGER.info("Guardian Heart triggered group shield for player {}", player.getName());
    }

    /**
     * 获取当前守护层数
     */
    public static int getGuardianStacks(EntityPlayer player) {
        return guardianStacks.getOrDefault(player.getUniqueID(), 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void addSpecialDescription(List<String> tooltip) {
        tooltip.add("");
        tooltip.add(I18n.format("adversity.bauble.guardian_heart.special", MAX_STACKS));
    }
}
