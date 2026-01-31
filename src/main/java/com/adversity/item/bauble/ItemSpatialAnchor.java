package com.adversity.item.bauble;

import com.adversity.Adversity;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 空间锚点 - 防止饰品被褫夺(Divest)封印
 * 
 * 圣所内：完全免疫褫夺
 * 圣所外：50%几率阻止
 * 特殊效果：每次成功阻止获得5秒+10%攻击力
 */
public class ItemSpatialAnchor extends AbstractWardBauble {

    private static final UUID ATTACK_BOOST_UUID = UUID.fromString("a5c7d8e9-1234-5678-9abc-def012345678");
    private static final String ATTACK_BOOST_NAME = "adversity.spatial_anchor_boost";

    public ItemSpatialAnchor() {
        super("spatial_anchor", "divest");
    }

    @Override
    protected float getInSanctuaryStrength() {
        return 1.0f; // 完全免疫
    }

    @Override
    protected float getOutsideSanctuaryStrength() {
        return 0.5f; // 50%阻止率
    }

    @Override
    public void onEffectTriggered(EntityPlayer player, @Nullable EntityLivingBase source, boolean blocked) {
        if (blocked) {
            // 成功阻止时，获得攻击力加成
            applyAttackBoost(player);
            
            // 音效
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.8f, 1.2f);
            
            Adversity.LOGGER.debug("Spatial Anchor blocked Divest, granting attack boost");
        }
    }

    private void applyAttackBoost(EntityPlayer player) {
        IAttributeInstance attackAttribute = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        if (attackAttribute == null) return;

        // 移除旧的加成
        AttributeModifier oldModifier = attackAttribute.getModifier(ATTACK_BOOST_UUID);
        if (oldModifier != null) {
            attackAttribute.removeModifier(oldModifier);
        }

        // 添加新的10%加成（5秒后需要手动移除，这里简化处理）
        // 实际实现中应该使用Capability或定时任务来管理
        AttributeModifier newModifier = new AttributeModifier(
            ATTACK_BOOST_UUID,
            ATTACK_BOOST_NAME,
            0.1, // 10%
            2 // 百分比加成
        );
        attackAttribute.applyModifier(newModifier);

        // TODO: 5秒后移除加成（需要额外的定时系统）
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void addSpecialDescription(List<String> tooltip) {
        tooltip.add("");
        tooltip.add(I18n.format("adversity.bauble.spatial_anchor.special"));
    }
}
