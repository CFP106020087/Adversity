package com.adversity.curse;

import com.adversity.Adversity;
import com.adversity.network.PacketHandler;
import com.adversity.network.PacketSyncCurse;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.UUID;

/**
 * Curse Effect Handler
 * Applies curse effects to player attributes and handles login checks
 */
@Mod.EventBusSubscriber(modid = Adversity.MODID)
public class CurseEffectHandler {

    /** UUID for Black Swan attack modifier */
    private static final UUID BLACK_SWAN_MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-4789-abcd-ef0123456789");

    /** UUID for Black Friday health modifier */
    private static final UUID BLACK_FRIDAY_MODIFIER_UUID = UUID.fromString("b2c3d4e5-f6a7-4890-bcde-f01234567890");

    /** Modifier name for Black Swan */
    private static final String BLACK_SWAN_MODIFIER_NAME = "Adversity Black Swan Curse";

    /** Modifier name for Black Friday */
    private static final String BLACK_FRIDAY_MODIFIER_NAME = "Adversity Black Friday Curse";

    /**
     * Check if player is banned on login
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;

        EntityPlayerMP player = (EntityPlayerMP) event.player;
        PermanentCurseManager manager = PermanentCurseManager.get(player.world);

        // Check if player is banned
        if (manager.isPlayerBanned(player.getUniqueID())) {
            // Kick after a short delay so they see the message
            player.getServer().addScheduledTask(() -> {
                manager.kickBannedPlayer(player);
            });
            return;
        }

        // Apply curse effects on login
        applyCurseEffects(player, manager);
    }

    /**
     * Periodically update curse effects
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;
        if (event.player.ticksExisted % 100 != 0) return; // Every 5 seconds

        EntityPlayer player = event.player;
        PermanentCurseManager manager = PermanentCurseManager.get(player.world);

        applyCurseEffects(player, manager);
    }

    /**
     * Apply all curse effects to player
     */
    private static void applyCurseEffects(EntityPlayer player, PermanentCurseManager manager) {
        applyBlackSwanEffect(player, manager);
        applyBlackFridayEffect(player, manager);

        // Sync to client
        if (player instanceof EntityPlayerMP) {
            syncCurseDataToClient((EntityPlayerMP) player, manager);
        }
    }

    /**
     * Sync curse data to client for UI rendering
     */
    private static void syncCurseDataToClient(EntityPlayerMP player, PermanentCurseManager manager) {
        int sealedSlots = manager.getBlackCoffinSealed(player);
        float attackReduction = manager.getBlackSwanReduction(player);
        float healthReduction = manager.getBlackFridayReduction(player);

        PacketHandler.INSTANCE.sendTo(
            new PacketSyncCurse(sealedSlots, attackReduction, healthReduction),
            player
        );
    }

    /**
     * Apply Black Swan attack reduction
     */
    private static void applyBlackSwanEffect(EntityPlayer player, PermanentCurseManager manager) {
        IAttributeInstance attribute = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        if (attribute == null) return;

        // Remove existing modifier
        AttributeModifier existing = attribute.getModifier(BLACK_SWAN_MODIFIER_UUID);
        if (existing != null) {
            attribute.removeModifier(existing);
        }

        // Apply new modifier if cursed
        float reduction = manager.getBlackSwanReduction(player);
        if (reduction > 0) {
            AttributeModifier modifier = new AttributeModifier(
                BLACK_SWAN_MODIFIER_UUID,
                BLACK_SWAN_MODIFIER_NAME,
                -reduction,
                2  // Operation 2 = multiply percentage
            );
            attribute.applyModifier(modifier);
        }
    }

    /**
     * Apply Black Friday health reduction
     */
    private static void applyBlackFridayEffect(EntityPlayer player, PermanentCurseManager manager) {
        IAttributeInstance attribute = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (attribute == null) return;

        // Remove existing modifier
        AttributeModifier existing = attribute.getModifier(BLACK_FRIDAY_MODIFIER_UUID);
        if (existing != null) {
            attribute.removeModifier(existing);
        }

        // Apply new modifier if cursed
        float reduction = manager.getBlackFridayReduction(player);
        if (reduction > 0) {
            AttributeModifier modifier = new AttributeModifier(
                BLACK_FRIDAY_MODIFIER_UUID,
                BLACK_FRIDAY_MODIFIER_NAME,
                -reduction,
                0  // Operation 0 = add/subtract
            );
            attribute.applyModifier(modifier);

            // Cap current health to max health
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    /**
     * Handle player cloning (respawn, dimension change)
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // Curse data is stored in WorldSavedData, not on player
        // So we just need to reapply the effects
        if (!event.isWasDeath()) return;

        EntityPlayer newPlayer = event.getEntityPlayer();
        if (newPlayer.world.isRemote) return;

        // Delay application to ensure world is loaded
        newPlayer.getServer().addScheduledTask(() -> {
            PermanentCurseManager manager = PermanentCurseManager.get(newPlayer.world);
            applyCurseEffects(newPlayer, manager);
        });
    }
}
