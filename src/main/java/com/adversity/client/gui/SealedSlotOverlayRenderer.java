package com.adversity.client.gui;

import com.adversity.Adversity;
import com.adversity.curse.PermanentCurseManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Sealed Slot Overlay Renderer
 * Renders X overlay on inventory slots sealed by Black Coffin curse
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = Adversity.MODID, value = Side.CLIENT)
public class SealedSlotOverlayRenderer {

    private static final ResourceLocation SEAL_OVERLAY = new ResourceLocation(Adversity.MODID, "textures/gui/sealed_slot.png");

    /**
     * Render sealed slot overlays after GUI background
     */
    @SubscribeEvent
    public static void onGuiDrawBackground(GuiScreenEvent.BackgroundDrawnEvent event) {
        if (!(event.getGui() instanceof GuiContainer)) return;

        GuiContainer gui = (GuiContainer) event.getGui();
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;

        if (player == null) return;

        // Get sealed slot count from capability or cache
        int sealedCount = getSealedSlotCount(player);
        if (sealedCount <= 0) return;

        // Get GUI position
        int guiLeft = getGuiLeft(gui);
        int guiTop = getGuiTop(gui);

        // Render overlays on sealed slots
        Container container = gui.inventorySlots;
        for (Slot slot : container.inventorySlots) {
            // Only check player's main inventory slots (9-35)
            if (slot.inventory != player.inventory) continue;

            int slotIndex = slot.getSlotIndex();
            // Main inventory is slots 9-35 (after hotbar)
            if (slotIndex >= 9 && slotIndex < 36) {
                // Check if this slot is sealed (from the end)
                // Sealed from slot (36 - sealedCount) to slot 35
                if (slotIndex >= (36 - sealedCount)) {
                    renderXMark(guiLeft + slot.xPos, guiTop + slot.yPos);
                }
            }
        }
    }

    /**
     * Render seal overlay at slot position
     */
    private static void renderSealOverlay(int x, int y) {
        Minecraft mc = Minecraft.getMinecraft();

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 0.9f);

        mc.getTextureManager().bindTexture(SEAL_OVERLAY);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, 16, 16, 16, 16);

        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * Render X mark directly without texture
     */
    private static void renderXMark(int x, int y) {
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();

        // Draw semi-transparent dark background
        Gui.drawRect(x, y, x + 16, y + 16, 0xAA000000);

        // Draw red X
        int color = 0xFFCC0000;
        int lineWidth = 2;

        // Draw X using rectangles (diagonal lines approximation)
        for (int i = 0; i < 16; i++) {
            // Top-left to bottom-right diagonal
            Gui.drawRect(x + i, y + i, x + i + lineWidth, y + i + lineWidth, color);
            // Top-right to bottom-left diagonal
            Gui.drawRect(x + 16 - i - lineWidth, y + i, x + 16 - i, y + i + lineWidth, color);
        }

        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    /**
     * Get sealed slot count for player
     * Uses cached value on client side
     */
    private static int getSealedSlotCount(EntityPlayer player) {
        // On client, we need to get this from synced capability
        // For now, use direct world access (will be synced via packets later)
        if (player.world.isRemote) {
            return ClientCurseCache.getSealedSlots();
        }

        PermanentCurseManager manager = PermanentCurseManager.get(player.world);
        return manager.getBlackCoffinSealed(player);
    }

    /**
     * Get GUI left position using reflection
     */
    private static int getGuiLeft(GuiContainer gui) {
        try {
            java.lang.reflect.Field field = GuiContainer.class.getDeclaredField("guiLeft");
            field.setAccessible(true);
            return field.getInt(gui);
        } catch (Exception e) {
            // Fallback: try obfuscated name
            try {
                java.lang.reflect.Field field = GuiContainer.class.getDeclaredField("field_147003_i");
                field.setAccessible(true);
                return field.getInt(gui);
            } catch (Exception e2) {
                return 0;
            }
        }
    }

    /**
     * Get GUI top position using reflection
     */
    private static int getGuiTop(GuiContainer gui) {
        try {
            java.lang.reflect.Field field = GuiContainer.class.getDeclaredField("guiTop");
            field.setAccessible(true);
            return field.getInt(gui);
        } catch (Exception e) {
            // Fallback: try obfuscated name
            try {
                java.lang.reflect.Field field = GuiContainer.class.getDeclaredField("field_147009_r");
                field.setAccessible(true);
                return field.getInt(gui);
            } catch (Exception e2) {
                return 0;
            }
        }
    }

    /**
     * Client-side cache for curse data (synced from server)
     */
    public static class ClientCurseCache {
        private static int sealedSlots = 0;
        private static float attackReduction = 0f;
        private static float healthReduction = 0f;

        public static void update(int sealed, float attack, float health) {
            sealedSlots = sealed;
            attackReduction = attack;
            healthReduction = health;
        }

        public static int getSealedSlots() {
            return sealedSlots;
        }

        public static float getAttackReduction() {
            return attackReduction;
        }

        public static float getHealthReduction() {
            return healthReduction;
        }

        public static void reset() {
            sealedSlots = 0;
            attackReduction = 0f;
            healthReduction = 0f;
        }
    }
}
