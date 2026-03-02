package com.adversity.sanctuary.block;

import com.adversity.Adversity;
import com.adversity.sanctuary.SanctuaryType;
import com.adversity.sanctuary.TileEntitySanctuary;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * 天然圣所祭坛方块
 * 世界生成时放置，玩家右键激活
 */
public class BlockSanctuaryAltar extends Block implements ITileEntityProvider {

    private static final Random RANDOM = new Random();
    private int tier = 1;

    public BlockSanctuaryAltar() {
        super(Material.ROCK);
        setRegistryName(Adversity.MODID, "sanctuary_altar");
        setTranslationKey(Adversity.MODID + ".sanctuary_altar");
        setHardness(50.0f); // 非常坚硬
        setResistance(2000.0f); // 防爆
        setLightLevel(0.5f);
        setCreativeTab(null); // 不在创造模式标签页显示（世界生成专用）
    }

    public BlockSanctuaryAltar setTier(int tier) {
        this.tier = tier;
        return this;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        TileEntitySanctuary te = new TileEntitySanctuary();
        te.setType(SanctuaryType.NATURAL);
        te.setTier(tier);
        return te;
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
            EntityPlayer playerIn, EnumHand hand,
            EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) {
            return true;
        }

        TileEntity te = worldIn.getTileEntity(pos);
        if (!(te instanceof TileEntitySanctuary)) {
            return false;
        }

        TileEntitySanctuary sanctuary = (TileEntitySanctuary) te;

        // Shift+右键 (空手): 打开传送选择GUI
        if (playerIn.isSneaking() && playerIn.getHeldItem(hand).isEmpty()) {
            if (!sanctuary.isActive()) {
                playerIn.sendMessage(new TextComponentTranslation("adversity.sanctuary.not_active"));
                return true;
            }

            // 通过 IGuiHandler 打开传送选择GUI (因为需要获取圣所列表)
            playerIn.openGui(com.adversity.Adversity.instance,
                    com.adversity.client.gui.AdversityGuiHandler.GUI_SANCTUARY_TELEPORT, worldIn, pos.getX(),
                    pos.getY(), pos.getZ());
            return true;
        }

        // Shift+右键 (持物): 尝试进行仪式
        if (playerIn.isSneaking() && !playerIn.getHeldItem(hand).isEmpty()) {
            net.minecraft.item.ItemStack held = playerIn.getHeldItem(hand);
            net.minecraft.item.ItemStack result = sanctuary.performRitual(playerIn, held);
            playerIn.setHeldItem(hand, result);
            return true;
        }

        // 如果未激活，尝试激活 (需要正确的激活物品)
        if (!sanctuary.isActivated()) {
            net.minecraft.item.ItemStack heldItem = playerIn.getHeldItem(hand);

            // 检查手持物品是否是激活物品
            if (!isActivationItem(heldItem)) {
                // 显示需要什么物品
                String[] items = com.adversity.config.AdversityConfig.sanctuarySettings.activationItems;
                if (items.length > 0) {
                    playerIn.sendMessage(
                            new TextComponentTranslation("adversity.sanctuary.need_activation_item", items[0]));
                }
                return true;
            }

            if (sanctuary.activate()) {
                // 如果配置了消耗物品
                if (com.adversity.config.AdversityConfig.sanctuarySettings.consumeActivationItem) {
                    heldItem.shrink(1);
                }
                playerIn.sendMessage(
                        new TextComponentTranslation("adversity.sanctuary.activated", sanctuary.getTier()));
                playActivationEffects(worldIn, pos);
                return true;
            } else {
                playerIn.sendMessage(new TextComponentTranslation("adversity.sanctuary.activation_failed"));
                return true;
            }
        }

        // 已激活，打开GUI
        playerIn.openGui(Adversity.instance, com.adversity.client.gui.AdversityGuiHandler.GUI_SANCTUARY, worldIn,
                pos.getX(), pos.getY(), pos.getZ());
        return true;
    }

    private void teleportPlayerToSanctuary(EntityPlayer player, int dim, BlockPos pos) {
        if (player.world.isRemote)
            return;

        // 播放传送音效
        player.world.playSound(null, player.getPosition(), SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.PLAYERS,
                1.0f, 1.0f);

        if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            net.minecraft.entity.player.EntityPlayerMP mp = (net.minecraft.entity.player.EntityPlayerMP) player;
            if (mp.dimension != dim) {
                WorldServer targetWorld = mp.server.getWorld(dim);
                mp.changeDimension(dim, new SanctuaryTeleporter(targetWorld, pos.getX(), pos.getY(), pos.getZ()));
            } else {
                // 同维度传送 (如果需要)
                mp.connection.setPlayerLocation(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, mp.rotationYaw,
                        mp.rotationPitch);
            }
        }
    }

    // 简单的传送器，定点传送不创建传送门
    public static class SanctuaryTeleporter extends net.minecraft.world.Teleporter {
        private final double x, y, z;

        public SanctuaryTeleporter(WorldServer world, double x, double y, double z) {
            super(world);
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public void placeInPortal(net.minecraft.entity.Entity entity, float rotationYaw) {
            entity.setLocationAndAngles(x + 0.5, y + 1.5, z + 0.5, entity.rotationYaw, 0.0F);
            entity.motionX = 0;
            entity.motionY = 0;
            entity.motionZ = 0;
        }

        @Override
        public boolean placeInExistingPortal(net.minecraft.entity.Entity entity, float rotationYaw) {
            return false;
        }

        @Override
        public boolean makePortal(net.minecraft.entity.Entity entity) {
            return false;
        }

        @Override
        public void removeStalePortalLocations(long worldTime) {
            // Do nothing
        }
    }

    private void showSanctuaryInfo(EntityPlayer player, TileEntitySanctuary sanctuary) {
        int fuel = sanctuary.getFuel();
        int maxFuel = sanctuary.getMaxFuel();
        int percent = maxFuel > 0 ? (fuel * 100 / maxFuel) : 0;

        player.sendMessage(new TextComponentTranslation(
                "adversity.sanctuary.info",
                sanctuary.getTier(),
                fuel,
                maxFuel,
                percent));
    }

    private void playActivationEffects(World world, BlockPos pos) {
        // 音效
        world.playSound(null, pos, SoundEvents.BLOCK_END_PORTAL_SPAWN,
                SoundCategory.BLOCKS, 1.0f, 1.0f);
        world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                SoundCategory.BLOCKS, 1.0f, 0.5f);

        // 粒子效果
        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            // 向上的光柱效果
            for (int i = 0; i < 50; i++) {
                ws.spawnParticle(EnumParticleTypes.END_ROD,
                        pos.getX() + 0.5, pos.getY() + 1 + i * 0.5, pos.getZ() + 0.5,
                        3, 0.2, 0.1, 0.2, 0.02);
            }
            // 环形粒子
            ws.spawnParticle(EnumParticleTypes.PORTAL,
                    pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                    100, 1.0, 0.5, 1.0, 0.5);
        }
    }

    @Override
    public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntitySanctuary) {
            TileEntitySanctuary sanctuary = (TileEntitySanctuary) te;
            if (sanctuary.isActive()) {
                // 激活状态的粒子效果
                float fuelPercent = sanctuary.getFuelPercentage();

                // 火焰大小反映燃料量
                double flameHeight = 0.5 + fuelPercent * 1.5;
                worldIn.spawnParticle(EnumParticleTypes.FLAME,
                        pos.getX() + 0.5, pos.getY() + 1 + flameHeight, pos.getZ() + 0.5,
                        0, 0.05, 0);

                // 偶尔的光点
                if (rand.nextFloat() < 0.3f) {
                    worldIn.spawnParticle(EnumParticleTypes.END_ROD,
                            pos.getX() + 0.5 + (rand.nextDouble() - 0.5) * 2,
                            pos.getY() + 1.5,
                            pos.getZ() + 0.5 + (rand.nextDouble() - 0.5) * 2,
                            0, 0.02, 0);
                }
            }
        }
    }

    /**
     * 检查物品是否是配置的激活物品
     * 支持格式: modid:item_name 或 modid:item_name:metadata
     */
    private boolean isActivationItem(net.minecraft.item.ItemStack stack) {
        if (stack.isEmpty())
            return false;

        String[] configItems = com.adversity.config.AdversityConfig.sanctuarySettings.activationItems;
        if (configItems == null || configItems.length == 0) {
            // 没有配置物品时，允许任意物品激活
            return true;
        }

        net.minecraft.util.ResourceLocation stackId = stack.getItem().getRegistryName();
        if (stackId == null)
            return false;

        String stackRegistryName = stackId.toString();
        int stackMeta = stack.getMetadata();

        for (String configItem : configItems) {
            if (configItem == null || configItem.isEmpty())
                continue;

            String[] parts = configItem.split(":");
            if (parts.length < 2)
                continue;

            // 构建注册名 (modid:item_name)
            String configRegName = parts[0] + ":" + parts[1];

            // 检查注册名是否匹配
            if (stackRegistryName.equals(configRegName)) {
                // 如果配置了 metadata，也要检查
                if (parts.length >= 3) {
                    try {
                        int configMeta = Integer.parseInt(parts[2]);
                        if (stackMeta == configMeta) {
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        // 无效的 metadata，忽略 metadata 检查
                        return true;
                    }
                } else {
                    // 没有配置 metadata，只检查物品类型
                    return true;
                }
            }
        }

        return false;
    }
}
