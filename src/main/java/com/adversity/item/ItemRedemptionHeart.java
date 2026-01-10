package com.adversity.item;

import com.adversity.Adversity;
import com.adversity.curse.PermanentCurseManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 赎罪之心 - 用于减少黑色星期五诅咒（生命值削减）
 *
 * 使用方法：右键使用
 * 效果：恢复1点最大生命值
 * 限制：无法在已被ban后使用
 */
public class ItemRedemptionHeart extends Item {

    /** 每次使用减少的诅咒量（生命值） */
    private static final float REDEMPTION_AMOUNT = 1.0f;

    public ItemRedemptionHeart() {
        setRegistryName(Adversity.MODID, "redemption_heart");
        setUnlocalizedName(Adversity.MODID + ".redemption_heart");
        setCreativeTab(AdversityTab.INSTANCE);
        setMaxStackSize(16);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        PermanentCurseManager manager = PermanentCurseManager.get(world);

        // 检查玩家是否有黑色星期五诅咒
        float currentReduction = manager.getBlackFridayReduction(player);
        if (currentReduction <= 0) {
            player.sendMessage(new TextComponentTranslation("adversity.redemption.no_curse"));
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }

        // 尝试赎罪
        boolean success = manager.redeemCurse(player, PermanentCurseManager.CURSE_BLACK_FRIDAY, REDEMPTION_AMOUNT);

        if (success) {
            // 消耗物品
            stack.shrink(1);

            // 播放效果
            world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 0.8f);

            // 发送消息
            float newReduction = manager.getBlackFridayReduction(player);
            float remainingHealth = 20.0f - newReduction;
            TextComponentTranslation msg = new TextComponentTranslation(
                "adversity.redemption.success.black_friday",
                (int)remainingHealth
            );
            msg.getStyle().setColor(TextFormatting.GREEN);
            player.sendMessage(msg);

            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        } else {
            // 已被ban，无法赎罪
            TextComponentTranslation msg = new TextComponentTranslation("adversity.redemption.banned");
            msg.getStyle().setColor(TextFormatting.DARK_RED);
            player.sendMessage(msg);
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.GRAY + I18n.format("adversity.redemption_heart.desc"));
        tooltip.add(TextFormatting.RED + I18n.format("adversity.redemption_heart.effect", (int)REDEMPTION_AMOUNT));
        tooltip.add(TextFormatting.DARK_RED + I18n.format("adversity.redemption.warning"));
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;  // 发光效果
    }
}
