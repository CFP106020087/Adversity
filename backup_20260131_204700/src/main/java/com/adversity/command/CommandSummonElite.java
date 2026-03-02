package com.adversity.command;

import com.adversity.affix.AffixRegistry;
import com.adversity.affix.IAffix;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 召唤精英怪指令
 * 
 * 用法:
 * /summonelite <实体ID> <词条1> [词条2] [词条3] ... [等级]
 * 
 * 示例:
 * /summonelite minecraft:zombie frosty - 召唤冰霜僵尸
 * /summonelite minecraft:skeleton fiery vampiric - 召唤烈焰+吸血骷髅
 * /summonelite minecraft:zombie frosty 5 - 召唤5级冰霜僵尸
 * 
 * 无参数使用 /summonelite 查看所有可用词条
 */
public class CommandSummonElite extends CommandBase {

    @Override
    public String getName() {
        return "summonelite";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/summonelite <entity> <affix1> [affix2...] [tier]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // 需要OP权限
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        // 无参数 - 显示所有可用词条
        if (args.length == 0) {
            showAvailableAffixes(sender);
            return;
        }

        if (args.length < 2) {
            throw new CommandException("Usage: /summonelite <entity> <affix1> [affix2...] [tier]");
        }

        // 解析实体类型
        String entityId = args[0];
        ResourceLocation entityLoc;
        if (entityId.contains(":")) {
            entityLoc = new ResourceLocation(entityId);
        } else {
            entityLoc = new ResourceLocation("minecraft", entityId);
        }

        // 验证实体类型存在
        if (!EntityList.isRegistered(entityLoc)) {
            throw new CommandException("Unknown entity type: " + entityId);
        }

        // 解析词条和等级
        List<IAffix> affixes = new ArrayList<>();
        int tier = 1;

        for (int i = 1; i < args.length; i++) {
            String arg = args[i].toLowerCase();

            // 检查是否是数字（等级）
            try {
                tier = Integer.parseInt(arg);
                tier = Math.max(1, Math.min(10, tier));
                continue;
            } catch (NumberFormatException ignored) {
            }

            // 尝试解析为词条
            IAffix affix = findAffix(arg);
            if (affix != null) {
                affixes.add(affix);
            } else {
                throw new CommandException("Unknown affix: " + arg + ". Use /summonelite to see available affixes.");
            }
        }

        if (affixes.isEmpty()) {
            throw new CommandException("At least one affix is required!");
        }

        // 获取召唤位置
        BlockPos pos;
        if (sender instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) sender;
            pos = player.getPosition();
        } else {
            pos = sender.getPosition();
        }

        // 创建实体
        EntityLivingBase entity = (EntityLivingBase) EntityList.createEntityByIDFromName(entityLoc,
                sender.getEntityWorld());
        if (entity == null) {
            throw new CommandException("Failed to create entity: " + entityId);
        }

        // 设置位置
        entity.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

        // 应用 Adversity 能力
        if (!(entity instanceof EntityLiving)) {
            throw new CommandException("Entity is not a living entity: " + entityId);
        }

        EntityLiving livingEntity = (EntityLiving) entity;
        IAdversityCapability cap = CapabilityHandler.getCapability(livingEntity);
        if (cap != null) {
            // 设置等级
            cap.setTier(tier);
            cap.setProcessed(true);

            // 添加所有词条
            for (IAffix affix : affixes) {
                cap.addAffix(affix);
            }

            // 计算并应用属性修改
            float healthMult = 1.0f + (tier * 0.5f); // 每级+50%血量
            float damageMult = 1.0f + (tier * 0.3f); // 每级+30%伤害

            cap.setHealthMultiplier(healthMult);
            cap.setDamageMultiplier(damageMult);

            // 应用生命值
            net.minecraft.entity.ai.attributes.IAttributeInstance healthAttr = livingEntity
                    .getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.MAX_HEALTH);
            if (healthAttr != null) {
                healthAttr.setBaseValue(healthAttr.getBaseValue() * healthMult);
                livingEntity.setHealth(livingEntity.getMaxHealth());
            }

            // 应用攻击力
            net.minecraft.entity.ai.attributes.IAttributeInstance damageAttr = livingEntity
                    .getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.ATTACK_DAMAGE);
            if (damageAttr != null) {
                damageAttr.setBaseValue(damageAttr.getBaseValue() * damageMult);
            }

            // 调用每个词条的 onApply
            for (IAffix affix : affixes) {
                affix.onApply(livingEntity, cap.getAffixData(affix));
            }
        } else {
            throw new CommandException("Entity does not support Adversity capability: " + entityId);
        }

        // 生成实体
        sender.getEntityWorld().spawnEntity(entity);

        // 反馈
        StringBuilder affixNames = new StringBuilder();
        for (int i = 0; i < affixes.size(); i++) {
            if (i > 0)
                affixNames.append(", ");
            affixNames.append(TextFormatting.GOLD).append(affixes.get(i).getId().getPath());
        }

        sender.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "[Adversity] " + TextFormatting.WHITE +
                        "召唤了 T" + tier + " " + affixNames + " " + entityId));
    }

    private void showAvailableAffixes(ICommandSender sender) {
        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== 可用词条列表 ==="));

        Collection<IAffix> allAffixes = AffixRegistry.getAllAffixes();
        if (allAffixes.isEmpty()) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "没有注册任何词条"));
            return;
        }

        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (IAffix affix : allAffixes) {
            if (count > 0)
                sb.append(TextFormatting.WHITE + ", ");
            sb.append(TextFormatting.YELLOW).append(affix.getId().getPath());
            count++;
        }

        sender.sendMessage(new TextComponentString(sb.toString()));
        sender.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "用法: /summonelite <实体> <词条1> [词条2...] [等级1-10]"));
    }

    private IAffix findAffix(String name) {
        // 尝试直接匹配
        for (IAffix affix : AffixRegistry.getAllAffixes()) {
            if (affix.getId().getPath().equalsIgnoreCase(name)) {
                return affix;
            }
        }

        // 尝试部分匹配
        for (IAffix affix : AffixRegistry.getAllAffixes()) {
            if (affix.getId().getPath().toLowerCase().contains(name.toLowerCase())) {
                return affix;
            }
        }

        return null;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
            @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            // 实体类型补全
            Set<ResourceLocation> entities = EntityList.getEntityNameList();
            List<String> entityNames = new ArrayList<>();
            for (ResourceLocation loc : entities) {
                entityNames.add(loc.toString());
            }
            return getListOfStringsMatchingLastWord(args, entityNames);
        }

        if (args.length >= 2) {
            // 词条名补全
            List<String> affixNames = new ArrayList<>();
            for (IAffix affix : AffixRegistry.getAllAffixes()) {
                affixNames.add(affix.getId().getPath());
            }
            // 添加常用等级
            affixNames.addAll(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"));
            return getListOfStringsMatchingLastWord(args, affixNames);
        }

        return Collections.emptyList();
    }
}
