package com.adversity.block;

import com.adversity.Adversity;
import com.adversity.sanctuary.TileEntitySanctuary;
import com.adversity.sanctuary.block.BlockSanctuaryAltar;
import com.adversity.sanctuary.block.BlockSanctuaryCore;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 方块注册表
 */
@Mod.EventBusSubscriber(modid = Adversity.MODID)
public class BlockRegistry {

    @GameRegistry.ObjectHolder(Adversity.MODID + ":sanctuary_altar")
    public static final Block SANCTUARY_ALTAR = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":sanctuary_core")
    public static final Block SANCTUARY_CORE = null;

    @GameRegistry.ObjectHolder(Adversity.MODID + ":sanctuary_stone")
    public static final Block SANCTUARY_STONE = null;

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        Adversity.LOGGER.info("Registering Adversity blocks...");

        event.getRegistry().registerAll(
                new BlockSanctuaryAltar(),
                new BlockSanctuaryCore(),
                new com.adversity.sanctuary.block.BlockSanctuaryStone());

        // 注册TileEntity
        GameRegistry.registerTileEntity(TileEntitySanctuary.class,
                Adversity.MODID + ":tile_sanctuary");

        Adversity.LOGGER.info("Adversity blocks registered");
    }

    @SubscribeEvent
    public static void registerItemBlocks(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
                createItemBlock(SANCTUARY_ALTAR),
                createItemBlock(SANCTUARY_CORE),
                createItemBlock(SANCTUARY_STONE));
    }

    private static ItemBlock createItemBlock(Block block) {
        ItemBlock itemBlock = new ItemBlock(block);
        itemBlock.setRegistryName(block.getRegistryName());
        return itemBlock;
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        registerBlockModel(SANCTUARY_ALTAR);
        registerBlockModel(SANCTUARY_CORE);
        registerBlockModel(SANCTUARY_STONE);
    }

    @SideOnly(Side.CLIENT)
    private static void registerBlockModel(Block block) {
        if (block != null && block.getRegistryName() != null) {
            Item item = Item.getItemFromBlock(block);
            if (item != null) {
                ModelLoader.setCustomModelResourceLocation(
                        item,
                        0,
                        new ModelResourceLocation(block.getRegistryName(), "inventory"));
            }
        }
    }
}
