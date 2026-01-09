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
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

/**
 * Sealed Slot Overlay Renderer
 * Renders X overlay on inventory slots sealed by Black Coffin curse
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = Adversity.MODID, value = Side.CLIENT)
public class SealedSlotOverlayRenderer {

    private static final ResourceLocation SEAL_OVERLAY = new ResourceLocation(Adversity.MODID, "textures/gui/sealed_slot.png");

    /**
     * Prevent mouse clicks on sealed slots
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onMouseClick(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!(event.getGui() instanceof GuiContainer)) return;
        if (!Mouse.getEventButtonState()) return;  // Only handle button press, not release

        GuiContainer gui = (GuiContainer) event.getGui();
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;

        if (player == null) return;

        int sealedCount = ClientCurseCache.getSealedSlots();
        if (sealedCount <= 0) return;

        // Get GUI position
        int guiLeft = getGuiLeft(gui);
        int guiTop = getGuiTop(gui);

        // Get mouse position relative to GUI
        int mouseX = Mouse.getEventX() * gui.width / mc.displayWidth;
        int mouseY = gui.height - Mouse.getEventY() * gui.height / mc.displayHeight - 1;

        // Check if mouse is over a sealed slot
        Container container = gui.inventorySlots;
        for (Slot slot : container.inventorySlots) {
            if (slot.inventory != player.inventory) continue;

            int slotIndex = slot.getSlotIndex();
            if (slotIndex >= 9 && slotIndex < 36) {
                if (slotIndex >= (36 - sealedCount)) {
                    // Check if mouse is over this slot
                    int slotX = guiLeft + slot.xPos;
                    int slotY = guiTop + slot.yPos;

                    if (mouseX >= slotX && mouseX < slotX + 16 &&
                        mouseY >= slotY && mouseY < slotY + 16) {
                        // Cancel the click
                        event.setCanceled(true);
                        // Play deny sound
                        player.playSound(net.minecraft.init.SoundEvents.BLOCK_NOTE_BASS, 0.5f, 0.5f);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Render sealed slot overlays on top of GUI (foreground)
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onGuiDrawForeground(GuiScreenEvent.DrawScreenEvent.Post event) {
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
            // Only check player's main inventory slots
            if (slot.inventory != player.inventory) continue;

            int slotIndex = slot.getSlotIndex();
            // Main inventory is slots 9-35 (after hotbar 0-8)
            // We seal from the end: slot 35, 34, 33...
            if (slotIndex >= 9 && slotIndex < 36) {
                // Check if this slot is sealed (from the end)
                // Sealed from slot (36 - sealedCount) to slot 35
                if (slotIndex >= (36 - sealedCount)) {
                    renderSealedSlot(guiLeft + slot.xPos, guiTop + slot.yPos);
                }
            }
        }
    }

    /**
     * Render a sealed slot with gray background and red X
     */
    private static void renderSealedSlot(int x, int y) {
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();

        // Draw dark gray background (makes slot look disabled)
        Gui.drawRect(x, y, x + 16, y + 16, 0xCC333333);

        // Draw red X with thicker lines
        int redColor = 0xFFDD2222;

        // Draw X using thick diagonal lines
        for (int i = 0; i < 14; i++) {
            int thickness = 2;
            // Top-left to bottom-right diagonal
            Gui.drawRect(x + 1 + i, y + 1 + i, x + 1 + i + thickness, y + 1 + i + thickness, redColor);
            // Top-right to bottom-left diagonal
            Gui.drawRect(x + 15 - i - thickness, y + 1 + i, x + 15 - i, y + 1 + i + thickness, redColor);
        }

        // Draw border to make it more visible
        int borderColor = 0xFF880000;
        // Top border
        Gui.drawRect(x, y, x + 16, y + 1, borderColor);
        // Bottom border
        Gui.drawRect(x, y + 15, x + 16, y + 16, borderColor);
        // Left border
        Gui.drawRect(x, y, x + 1, y + 16, borderColor);
        // Right border
        Gui.drawRect(x + 15, y, x + 16, y + 16, borderColor);

        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    /**
     * Check if a slot index is sealed
     */
    public static boolean isSlotSealed(EntityPlayer player, int slotIndex) {
        int sealedCount = getSealedSlotCount(player);
        if (sealedCount <= 0) return false;

        // Main inventory slots 9-35, sealed from end
        if (slotIndex >= 9 && slotIndex < 36) {
            return slotIndex >= (36 - sealedCount);
        }
        return false;
    }

    /**
     * Get sealed slot count for player
     * Uses cached value on client side
     */
    private static int getSealedSlotCount(EntityPlayer player) {
        // On client, we need to get this from synced capability
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
