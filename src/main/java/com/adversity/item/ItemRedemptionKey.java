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
 * 赎罪之钥 - 用于减少黑棺诅咒（背包槽位封印）
 *
 * 使用方法：右键使用
 * 效果：解封1个背包槽位
 * 限制：无法在已被ban后使用
 */
public class ItemRedemptionKey extends Item {

    /** 每次使用减少的封印槽位数 */
    private static final int REDEMPTION_AMOUNT = 1;

    public ItemRedemptionKey() {
        setRegistryName(Adversity.MODID, "redemption_key");
        setTranslationKey(Adversity.MODID + ".redemption_key");
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

        // 检查玩家是否有黑棺诅咒
        int currentSealed = manager.getBlackCoffinSealed(player);
        if (currentSealed <= 0) {
            player.sendMessage(new TextComponentTranslation("adversity.redemption.no_curse"));
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }

        // 尝试赎罪
        boolean success = manager.redeemCurse(player, PermanentCurseManager.CURSE_BLACK_COFFIN, REDEMPTION_AMOUNT);

        if (success) {
            // 消耗物品
            stack.shrink(1);

            // 播放效果
            world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.PLAYERS, 1.0f, 1.5f);
            world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.PLAYERS, 0.8f, 1.2f);

            // 发送消息
            int newSealed = manager.getBlackCoffinSealed(player);
            int available = 36 - newSealed;
            TextComponentTranslation msg = new TextComponentTranslation(
                "adversity.redemption.success.black_coffin",
                available
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
        tooltip.add(TextFormatting.GRAY + I18n.format("adversity.redemption_key.desc"));
        tooltip.add(TextFormatting.DARK_PURPLE + I18n.format("adversity.redemption_key.effect", REDEMPTION_AMOUNT));
        tooltip.add(TextFormatting.RED + I18n.format("adversity.redemption.warning"));
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;  // 发光效果
    }
}
