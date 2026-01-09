package com.adversity.affix.impl;

import com.adversity.Adversity;
import com.adversity.affix.AbstractAffix;
import com.adversity.affix.AffixType;
import com.adversity.affix.IAffixData;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.client.visual.VisualEffectHelper;
import com.adversity.client.visual.VisualEffectType;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.WorldServer;

import java.util.Random;

/**
 * Nightmare Affix - Spawns near sleep-deprived players
 *
 * Mechanics:
 * 1. This affix is applied to mobs that spawn via NightmareSpawnHandler
 * 2. Mobs with this affix inflict blindness and slowness
 * 3. Has chance to inflict phantom-like effects
 * 4. Design concept: Punish players who avoid sleeping
 */
public class NightmareAffix extends AbstractAffix {

    public static final ResourceLocation ID = new ResourceLocation(Adversity.MODID, "nightmare");

    /** Base trigger chance */
    private static final float BASE_EFFECT_CHANCE = 0.4f;  // 40%

    private static final Random RANDOM = new Random();

    public NightmareAffix() {
        super(
            ID,
            AffixType.SPECIAL,
            0,      // Weight 0 - only spawned by NightmareSpawnHandler, never randomly
            0.0f    // No difficulty requirement - controlled by spawn handler
        );
    }

    @Override
    public float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data) {
        if (!(target instanceof EntityPlayer)) {
            return damage;
        }

        EntityPlayer player = (EntityPlayer) target;
        int tier = getTier(attacker);

        // Calculate effect chance
        float chance = BASE_EFFECT_CHANCE + (tier * 0.05f);

        if (RANDOM.nextFloat() < chance) {
            applyNightmareEffects(player, attacker, tier);
        }

        return damage;
    }

    /**
     * Apply nightmare effects to player
     */
    private void applyNightmareEffects(EntityPlayer player, EntityLiving attacker, int tier) {
        // Blindness - can't see the nightmare coming
        player.addPotionEffect(new PotionEffect(
            MobEffects.BLINDNESS,
            100 + (tier * 20),  // 5-7 seconds
            0
        ));

        // Slowness - fear makes you sluggish
        player.addPotionEffect(new PotionEffect(
            MobEffects.SLOWNESS,
            80 + (tier * 20),   // 4-6 seconds
            tier / 3            // Amplifier based on tier
        ));

        // Higher tiers add more effects
        if (tier >= 3) {
            // Weakness
            player.addPotionEffect(new PotionEffect(
                MobEffects.WEAKNESS,
                100 + (tier * 20),
                0
            ));
        }

        if (tier >= 5) {
            // Mining fatigue
            player.addPotionEffect(new PotionEffect(
                MobEffects.MINING_FATIGUE,
                100 + (tier * 20),
                1
            ));
        }

        if (tier >= 7) {
            // Nausea at high tiers
            player.addPotionEffect(new PotionEffect(
                MobEffects.NAUSEA,
                60 + (tier * 10),
                0
            ));
        }

        // Play nightmare effects
        playNightmareEffects(player, attacker);

        // Send visual effect
        VisualEffectHelper.sendToPlayer(player, VisualEffectType.HORROR, 60, 0.8f, attacker.getEntityId());
    }

    /**
     * Play nightmare visual and sound effects
     */
    private void playNightmareEffects(EntityPlayer player, EntityLiving attacker) {
        if (player.world.isRemote) return;

        // Eerie sound
        player.world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            SoundEvents.ENTITY_PHANTOM_AMBIENT,
            SoundCategory.HOSTILE,
            1.0f,
            0.5f
        );

        player.world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            SoundEvents.AMBIENT_CAVE,
            SoundCategory.HOSTILE,
            0.5f,
            0.3f
        );

        // Dark smoke particles
        if (player.world instanceof WorldServer) {
            ((WorldServer) player.world).spawnParticle(
                EnumParticleTypes.SMOKE_LARGE,
                player.posX, player.posY + 1, player.posZ,
                30,
                0.5, 0.5, 0.5,
                0.02
            );
            ((WorldServer) player.world).spawnParticle(
                EnumParticleTypes.SPELL_MOB,
                player.posX, player.posY + 1, player.posZ,
                20,
                0.3, 0.3, 0.3,
                0.0
            );
        }
    }

    private int getTier(EntityLiving entity) {
        IAdversityCapability cap = CapabilityHandler.getCapability(entity);
        return cap != null ? cap.getTier() : 1;
    }
}
