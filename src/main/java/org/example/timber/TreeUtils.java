package org.example.timber;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;

import java.util.*;

public class TreeUtils {

    private static final int[][] OFFSETS6 = new int[][]{
            {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
            {0, 1, 0}, {0, -1, 0}
    };

    private static final int[][] OFFSETS26;
    static {
        List<int[]> list = new ArrayList<>();
        for (int dx=-1; dx<=1; dx++) for (int dy=-1; dy<=1; dy++) for (int dz=-1; dz<=1; dz++) {
            if (dx==0 && dy==0 && dz==0) continue;
            list.add(new int[]{dx,dy,dz});
        }
        OFFSETS26 = list.toArray(new int[0][]);
    }

    public enum WoodFamily { OAK, SPRUCE, BIRCH, JUNGLE, ACACIA, DARK_OAK, MANGROVE, CHERRY }

    public static WoodFamily familyOf(Material m) {
        switch (m) {
            case OAK_LOG, STRIPPED_OAK_LOG:             return WoodFamily.OAK;
            case SPRUCE_LOG, STRIPPED_SPRUCE_LOG:       return WoodFamily.SPRUCE;
            case BIRCH_LOG, STRIPPED_BIRCH_LOG:         return WoodFamily.BIRCH;
            case JUNGLE_LOG, STRIPPED_JUNGLE_LOG:       return WoodFamily.JUNGLE;
            case ACACIA_LOG, STRIPPED_ACACIA_LOG:       return WoodFamily.ACACIA;
            case DARK_OAK_LOG, STRIPPED_DARK_OAK_LOG:   return WoodFamily.DARK_OAK;
            case MANGROVE_LOG, STRIPPED_MANGROVE_LOG:   return WoodFamily.MANGROVE;
            case CHERRY_LOG, STRIPPED_CHERRY_LOG:       return WoodFamily.CHERRY;
            default: {
                String n = m.name();
                if (n.contains("PALE_OAK")) return WoodFamily.OAK; // treat as OAK family
                return null;
            }
        }
    }

    public static Set<Block> collectConnectedLogs(Block start, int max, boolean includeDiagonals, int maxHoriz) {
        Set<Block> out = new LinkedHashSet<>();
        if (!Tag.LOGS.isTagged(start.getType())) return out;

        final int sx = start.getX();
        final int sz = start.getZ();

        WoodFamily family = familyOf(start.getType());
        if (family == null) return out;

        ArrayDeque<Block> dq = new ArrayDeque<>();
        dq.add(start);
        out.add(start);

        final int[][] OFFSETS = includeDiagonals ? OFFSETS26 : OFFSETS6;

        while (!dq.isEmpty() && out.size() < max) {
            Block b = dq.pollFirst();
            for (int[] d : OFFSETS) {
                Block n = b.getRelative(d[0], d[1], d[2]);
                if (maxHoriz > 0 && (Math.abs(n.getX()-sx) > maxHoriz || Math.abs(n.getZ()-sz) > maxHoriz)) continue;
                if (!out.contains(n) && isSameFamilyLog(n, family)) {
                    out.add(n);
                    if (out.size() >= max) break;
                    dq.add(n);
                    continue;
                }

                // Leaf-bridge (only when diagonals are allowed and leaves match same family): log -> leaves -> log
                if (includeDiagonals && Tag.LEAVES.isTagged(n.getType()) && leafMatchesFamily(n.getType(), family)) {
                    Block n2 = b.getRelative(d[0]*2, d[1]*2, d[2]*2);
                    if (maxHoriz > 0 && (Math.abs(n2.getX()-sx) > maxHoriz || Math.abs(n2.getZ()-sz) > maxHoriz)) continue;
                    if (!out.contains(n2) && isSameFamilyLog(n2, family)) {
                        out.add(n2);
                        if (out.size() >= max) break;
                        dq.add(n2);
                    }
                }
            }
        }
        return out;
    }

    public static Set<Block> collectConnectedLogs(Block start, int max, boolean includeDiagonals) {
        return collectConnectedLogs(start, max, includeDiagonals, 0);
    }

    public static Set<Block> collectConnectedLogs(Block start, int max) {
        return collectConnectedLogs(start, max, false, 0);
    }

    private static boolean isSameFamilyLog(Block b, WoodFamily family) {
        Material t = b.getType();
        return Tag.LOGS.isTagged(t) && familyOf(t) == family;
    }

    private static boolean leafMatchesFamily(Material leaves, WoodFamily family) {
        return switch (family) {
            case OAK -> leaves == Material.OAK_LEAVES || leaves.name().contains("PALE_OAK_LEAVES");
            case DARK_OAK -> leaves == Material.DARK_OAK_LEAVES;
            case BIRCH -> leaves == Material.BIRCH_LEAVES;
            case SPRUCE -> leaves == Material.SPRUCE_LEAVES;
            case JUNGLE -> leaves == Material.JUNGLE_LEAVES;
            case ACACIA -> leaves == Material.ACACIA_LEAVES;
            case MANGROVE -> leaves == Material.MANGROVE_LEAVES;
            case CHERRY -> leaves == Material.CHERRY_LEAVES;

        };
    }
}