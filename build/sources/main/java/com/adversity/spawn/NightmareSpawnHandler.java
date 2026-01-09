package com.adversity.spawn;

import com.adversity.Adversity;
import com.adversity.affix.AffixRegistry;
import com.adversity.affix.IAffix;
import com.adversity.affix.impl.NightmareAffix;
import com.adversity.capability.CapabilityHandler;
import com.adversity.capability.IAdversityCapability;
import com.adversity.client.visual.VisualEffectHelper;
import com.adversity.client.visual.VisualEffectType;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.stats.StatList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;

/**
 * Nightmare Spawn Handler
 * Spawns nightmare-afflicted mobs near players who haven't slept for a long time
 */
@Mod.EventBusSubscriber(modid = Adversity.MODID)
public class NightmareSpawnHandler {

    /** Ticks per Minecraft day */
    private static final int TICKS_PER_DAY = 24000;

    /** Days without sleep before nightmares start appearing */
    private static final int DAYS_UNTIL_NIGHTMARE = 3;

    /** Base spawn chance per check (every 5 seconds) */
    private static final float BASE_SPAWN_CHANCE = 0.05f;

    /** Maximum spawn chance */
    private static final float MAX_SPAWN_CHANCE = 0.25f;

    /** Check interval in ticks (5 seconds) */
    private static final int CHECK_INTERVAL = 100;

    /** Maximum spawn distance from player */
    private static final int MAX_SPAWN_DISTANCE = 24;

    /** Minimum spawn distance from player */
    private static final int MIN_SPAWN_DISTANCE = 8;

    private static final Random RANDOM = new Random();

    /**
     * Check for nightmare spawns on player tick
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;
        if (event.player.ticksExisted % CHECK_INTERVAL != 0) return;

        EntityPlayer player = event.player;

        // Only spawn at night
        if (player.world.isDaytime()) return;

        // Calculate days without sleep
        int daysWithoutSleep = getDaysWithoutSleep(player);

        if (daysWithoutSleep < DAYS_UNTIL_NIGHTMARE) return;

        // Calculate spawn chance based on days without sleep
        float spawnChance = calculateSpawnChance(daysWithoutSleep);

        if (RANDOM.nextFloat() < spawnChance) {
            spawnNightmare(player, daysWithoutSleep);
        }
    }

    /**
     * Get the number of days the player hasn't slept
     */
    private static int getDaysWithoutSleep(EntityPlayer player) {
        // Get time since last rest
        // In Minecraft, StatList.TIME_SINCE_REST tracks ticks since last sleep
        int timeSinceRest = player.getStatFile().readStat(StatList.TIME_SINCE_REST);
        return timeSinceRest / TICKS_PER_DAY;
    }

    /**
     * Calculate spawn chance based on days without sleep
     */
    private static float calculateSpawnChance(int daysWithoutSleep) {
        // Chance increases with days without sleep
        float extraDays = daysWithoutSleep - DAYS_UNTIL_NIGHTMARE;
        float chance = BASE_SPAWN_CHANCE + (extraDays * 0.03f);
        return Math.min(chance, MAX_SPAWN_CHANCE);
    }

    /**
     * Spawn a nightmare mob near the player
     */
    private static void spawnNightmare(EntityPlayer player, int daysWithoutSleep) {
        World world = player.world;

        // Find a valid spawn position
        BlockPos spawnPos = findSpawnPosition(player);
        if (spawnPos == null) return;

        // Choose a mob type
        EntityLiving nightmare = createNightmareMob(world, daysWithoutSleep);
        if (nightmare == null) return;

        // Set position
        nightmare.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);

        // Check if spawn location is valid
        if (!nightmare.isNotColliding()) {
            nightmare.setDead();
            return;
        }

        // Apply nightmare affix
        applyNightmareAffix(nightmare, daysWithoutSleep);

        // Spawn the mob
        world.spawnEntity(nightmare);

        // Play spawn effects
        playSpawnEffects(world, player, spawnPos);

        // Send visual effect to player
        if (player instanceof EntityPlayerMP) {
            VisualEffectHelper.sendToPlayer(player, VisualEffectType.HORROR, 40, 0.6f, nightmare.getEntityId());
        }

        Adversity.LOGGER.debug("Spawned nightmare for player {} (days without sleep: {})",
            player.getName(), daysWithoutSleep);
    }

    /**
     * Find a valid spawn position near the player
     */
    private static BlockPos findSpawnPosition(EntityPlayer player) {
        World world = player.world;

        for (int attempt = 0; attempt < 10; attempt++) {
            // Random angle
            double angle = RANDOM.nextDouble() * Math.PI * 2;

            // Random distance
            int distance = MIN_SPAWN_DISTANCE + RANDOM.nextInt(MAX_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE);

            int x = MathHelper.floor(player.posX + Math.cos(angle) * distance);
            int z = MathHelper.floor(player.posZ + Math.sin(angle) * distance);

            // Find ground level
            BlockPos checkPos = new BlockPos(x, (int) player.posY, z);

            // Search vertically for valid ground
            for (int y = -5; y <= 5; y++) {
                BlockPos testPos = checkPos.up(y);
                if (isValidSpawnLocation(world, testPos)) {
                    return testPos;
                }
            }
        }

        return null;
    }

    /**
     * Check if location is valid for spawning
     */
    private static boolean isValidSpawnLocation(World world, BlockPos pos) {
        // Check ground is solid
        if (!world.getBlockState(pos.down()).isSideSolid(world, pos.down(), net.minecraft.util.EnumFacing.UP)) {
            return false;
        }

        // Check space is clear
        if (!world.isAirBlock(pos) || !world.isAirBlock(pos.up())) {
            return false;
        }

        // Check light level is low enough
        if (world.getLightFromNeighbors(pos) > 7) {
            return false;
        }

        return true;
    }

    /**
     * Create a nightmare mob based on days without sleep
     */
    private static EntityLiving createNightmareMob(World world, int daysWithoutSleep) {
        // Higher days = stronger mob types
        int mobType = RANDOM.nextInt(100);

        if (daysWithoutSleep >= 7) {
            // After a week, spawn tougher mobs more often
            if (mobType < 30) {
                return new EntityCreeper(world);
            } else if (mobType < 60) {
                return new EntitySkeleton(world);
            } else {
                return new EntityZombie(world);
            }
        } else if (daysWithoutSleep >= 5) {
            if (mobType < 20) {
                return new EntityCreeper(world);
            } else if (mobType < 50) {
                return new EntitySkeleton(world);
            } else if (mobType < 80) {
                return new EntityZombie(world);
            } else {
                return new EntitySpider(world);
            }
        } else {
            // Early nightmares are mostly zombies and spiders
            if (mobType < 40) {
                return new EntityZombie(world);
            } else if (mobType < 70) {
                return new EntitySpider(world);
            } else {
                return new EntitySkeleton(world);
            }
        }
    }

    /**
     * Apply nightmare affix and stats to mob
     */
    private static void applyNightmareAffix(EntityLiving mob, int daysWithoutSleep) {
        IAdversityCapability cap = CapabilityHandler.getCapability(mob);
        if (cap == null) return;

        // Calculate tier based on days without sleep
        int tier = Math.min(1 + (daysWithoutSleep - DAYS_UNTIL_NIGHTMARE), 10);
        cap.setTier(tier);

        // Apply nightmare affix
        IAffix nightmareAffix = AffixRegistry.getAffix(NightmareAffix.ID);
        if (nightmareAffix != null) {
            cap.addAffix(nightmareAffix);
        }

        // Also give them one random additional affix for variety
        if (RANDOM.nextFloat() < 0.3f) {
            // TODO: Add random affix from registry
        }

        // Make nightmare mobs silent approach
        mob.setSilent(true);

        // Set persistence so they don't despawn immediately
        mob.enablePersistence();
    }

    /**
     * Play spawn effects
     */
    private static void playSpawnEffects(World world, EntityPlayer player, BlockPos pos) {
        // Eerie sound only player can hear
        world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            SoundEvents.ENTITY_PHANTOM_FLAP,
            SoundCategory.HOSTILE,
            0.5f,
            0.3f
        );

        // Particles at spawn location
        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(
                net.minecraft.util.EnumParticleTypes.SMOKE_LARGE,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                20,
                0.5, 0.5, 0.5,
                0.02
            );
        }
    }
}
