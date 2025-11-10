package org.example.timber;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Tiny helper for simple visual/audio effects. Optional to use.
 */
public final class Animation {

    private Animation() {}

    /**
     * Plays a short particle/sound sequence along the broken logs (bottom → top).
     */


    /**
     * Small sparkle when saplings get placed.
     */
    public static void playReplantAnimation(Collection<Location> spots, JavaPlugin plugin) {
        if (spots == null || spots.isEmpty()) return;
        for (Location base : spots) {
            if (base == null) continue;
            Location loc = base.clone().add(0.5, 0.1, 0.5);
            new BukkitRunnable() {
                @Override public void run() {
                    World w = loc.getWorld();
                    if (w == null) return;
                    w.spawnParticle(Particle.HAPPY_VILLAGER, loc, 6, 0.2, 0.3, 0.2, 0.01);
                    w.playSound(loc, Sound.BLOCK_GRASS_PLACE, 0.6f, 1.2f);
                }
            }.runTaskLater(plugin, 1);
        }
    }

    private static BlockData safeBlockData(Material m) {
        try { return m.createBlockData(); } catch (Throwable t) { return null; }
    }
}