package org.example.timber;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.*;

public class SaplingHelper {

    public static Material saplingFor(Material log) {
        switch (log) {
            case OAK_LOG, STRIPPED_OAK_LOG:             return Material.OAK_SAPLING;
            case SPRUCE_LOG, STRIPPED_SPRUCE_LOG:       return Material.SPRUCE_SAPLING;
            case BIRCH_LOG, STRIPPED_BIRCH_LOG:         return Material.BIRCH_SAPLING;
            case JUNGLE_LOG, STRIPPED_JUNGLE_LOG:       return Material.JUNGLE_SAPLING;
            case ACACIA_LOG, STRIPPED_ACACIA_LOG:       return Material.ACACIA_SAPLING;
            case DARK_OAK_LOG, STRIPPED_DARK_OAK_LOG:   return Material.DARK_OAK_SAPLING;
            case MANGROVE_LOG, STRIPPED_MANGROVE_LOG:   return Material.MANGROVE_PROPAGULE;
            case CHERRY_LOG, STRIPPED_CHERRY_LOG:       return Material.CHERRY_SAPLING;
            default: {
                String n = log.name();
                if (n.contains("PALE_OAK")) {
                    Material sap = Material.matchMaterial("PALE_OAK_SAPLING");
                    if (sap != null) return sap;
                }
                return null;
            }
        }
    }

    public static boolean isTwoByTwoSpecies(Material log) {
        switch (log) {
            case SPRUCE_LOG, STRIPPED_SPRUCE_LOG,
                 JUNGLE_LOG, STRIPPED_JUNGLE_LOG,
                 DARK_OAK_LOG, STRIPPED_DARK_OAK_LOG:
                return true;
            default:
                return false;
        }
    }

    /**
     * True if ANY log in the cluster sits directly on plantable ground (i.e., logs that actually touch the ground).
     * Used to avoid replanting when the player only breaks a floating branch.
     */
    public static boolean clusterTouchesGround(Set<Block> cluster) {
        if (cluster == null || cluster.isEmpty()) return false;
        for (Block b : cluster) {
            Block below = b.getRelative(0, -1, 0);
            if (isPlantable(below.getType())) return true;
        }
        return false;
    }

    public static List<Location> planReplantSpotsFromCluster(Location origin, Set<Block> cluster, Material logType, boolean allow2x2) {
        Location single = findPlantBase(origin);
        if (!allow2x2 || !isTwoByTwoSpecies(logType) || cluster == null || cluster.isEmpty()) {
            return single != null ? List.of(single) : List.of();
        }

        int baseY = Integer.MAX_VALUE;
        for (Block b : cluster) baseY = Math.min(baseY, b.getY());

        Set<Long> base = new HashSet<>();
        for (Block b : cluster) {
            if (b.getY() == baseY && b.getType() == logType) {
                base.add(key(b.getX(), b.getZ()));
            }
        }
        if (base.isEmpty()) return single != null ? List.of(single) : List.of();

        for (Long k : base) {
            int x = (int)(k >> 32);
            int z = (int)(k & 0xffffffffL);
            long k1 = key(x+1, z);
            long k2 = key(x, z+1);
            long k3 = key(x+1, z+1);
            if (base.contains(k1) && base.contains(k2) && base.contains(k3)) {
                World w = origin.getWorld();
                List<Location> out = new ArrayList<>(4);
                Location b0 = findPlantBase(new Location(w, x, baseY, z));
                Location b1 = findPlantBase(new Location(w, x+1, baseY, z));
                Location b2 = findPlantBase(new Location(w, x, baseY, z+1));
                Location b3 = findPlantBase(new Location(w, x+1, baseY, z+1));
                if (b0!=null) out.add(b0);
                if (b1!=null) out.add(b1);
                if (b2!=null) out.add(b2);
                if (b3!=null) out.add(b3);
                return out.isEmpty() && single!=null ? List.of(single) : out;
            }
        }
        return single != null ? List.of(single) : List.of();
    }

    public static Location findPlantBase(Location origin) {
        if (origin == null) return null;
        Location pos = origin.clone();
        World w = pos.getWorld();
        int minY = w.getMinHeight();
        while (pos.getY() > minY && org.bukkit.Tag.LOGS.isTagged(pos.getBlock().getType())) pos.add(0, -1, 0);
        while (pos.getY() > minY && pos.getBlock().getType() == Material.AIR) pos.add(0, -1, 0);
        Material ground = pos.getBlock().getType();
        if (!isPlantable(ground)) return null;
        return pos.add(0, 1, 0);
    }

    public static boolean plantSapling(Location target, Material sapling) {
        if (target == null || sapling == null) return false;
        Block b = target.getBlock();
        Material t = b.getType();
        if (t == Material.AIR || isReplaceableForSapling(t)) {
            if (t != Material.AIR) b.setType(Material.AIR, false);
            b.setType(sapling, false);
            return true;
        }
        return false;
    }

    private static boolean isPlantable(Material m) {
        switch (m) {
            case DIRT, GRASS_BLOCK, PODZOL, COARSE_DIRT, ROOTED_DIRT, MYCELIUM, MOSS_BLOCK:
                return true;
            default:
                return false;
        }
    }

    private static boolean isReplaceableForSapling(Material m) {
        return m == Material.SHORT_GRASS ||
                m == Material.TALL_GRASS ||
                m == Material.FERN ||
                m == Material.LARGE_FERN ||
                m == Material.DEAD_BUSH ||
                m == Material.SWEET_BERRY_BUSH ||
                m == Material.SNOW ||
                m.name().endsWith("_FLOWER");
    }

    private static long key(int x, int z) { return ((long)x << 32) ^ (z & 0xffffffffL); }
}