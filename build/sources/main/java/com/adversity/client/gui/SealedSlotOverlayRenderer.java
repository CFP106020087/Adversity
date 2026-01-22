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
            // 检查是否封印：先封主背包(9-35)，再封快捷栏(0-8)
            if (isSlotSealedByCount(slotIndex, sealedCount)) {
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
        // 先封主背包(9-35)，再封快捷栏(0-8)
        Container container = gui.inventorySlots;
        for (Slot slot : container.inventorySlots) {
            // Only check player's main inventory slots
            if (slot.inventory != player.inventory) continue;

            int slotIndex = slot.getSlotIndex();
            // 检查是否封印
            if (isSlotSealedByCount(slotIndex, sealedCount)) {
                renderSealedSlot(guiLeft + slot.xPos, guiTop + slot.yPos);
            }
        }
    }

    /**
     * Render a sealed slot using texture
     */
    private static void renderSealedSlot(int x, int y) {
        Minecraft mc = Minecraft.getMinecraft();

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        // Bind and render the sealed slot texture
        mc.getTextureManager().bindTexture(SEAL_OVERLAY);

        // Draw the texture (16x16 to match slot size)
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, 16, 16, 16, 16);

        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * Check if a slot index is sealed
     * 先封主背包(9-35)，再封快捷栏(0-8)
     */
    public static boolean isSlotSealed(EntityPlayer player, int slotIndex) {
        int sealedCount = getSealedSlotCount(player);
        return isSlotSealedByCount(slotIndex, sealedCount);
    }

    /**
     * Check if a slot index is sealed given the sealed count
     * 先封主背包(9-35)，再封快捷栏(0-8)
     */
    private static boolean isSlotSealedByCount(int slotIndex, int sealedCount) {
        if (sealedCount <= 0) return false;

        // 主背包有27个槽位(9-35)，快捷栏有9个槽位(0-8)
        if (slotIndex >= 9 && slotIndex < 36) {
            // 主背包槽位：先封印
            // slotIndex 9 对应第1个封印，slotIndex 35 对应第27个封印
            int sealOrder = slotIndex - 9;  // 0-26
            return sealOrder < sealedCount;
        } else if (slotIndex >= 0 && slotIndex < 9) {
            // 快捷栏槽位：后封印（在主背包全部封印之后）
            // 需要超过27个封印才开始封快捷栏
            if (sealedCount <= 27) return false;
            int hotbarSealed = sealedCount - 27;  // 快捷栏已封印数
            return slotIndex < hotbarSealed;
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
