package org.example.timber;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TimberListener implements Listener {

    private final TimberPlugin plugin;

    public TimberListener(TimberPlugin plugin) { this.plugin = plugin; }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block origin = event.getBlock();
        Player player = event.getPlayer();
        FileConfiguration cfg = plugin.getConfig();

        if (!Tag.LOGS.isTagged(origin.getType())) return;
        Material originalLogType = origin.getType();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (!player.hasPermission("timber.use")) return;

        List<String> worlds = cfg.getStringList("enabledWorlds");
        if (!worlds.isEmpty() && !worlds.contains(origin.getWorld().getName())) return;
        if (cfg.getBoolean("sneakToDisable", true) && player.isSneaking() && !player.hasPermission("timber.force")) return;

        ItemStack tool = player.getInventory().getItem(EquipmentSlot.HAND);
        if (cfg.getBoolean("requireAxe", true)) {
            if (tool == null) return;
            Set<String> allowed = new HashSet<>(cfg.getStringList("allowedAxes"));
            if (!allowed.contains(tool.getType().name())) return;
        }

        int max = Math.max(1, cfg.getInt("maxBlocks", 512));
        boolean diag = switch (TreeUtils.familyOf(originalLogType)) {
            case OAK, JUNGLE, CHERRY -> true;
            default -> {
                if (originalLogType.name().contains("PALE_OAK")) yield true; // treat Pale Oak like OAK
                yield false;
            }
        };
        Set<Block> cluster = TreeUtils.collectConnectedLogs(origin, max, diag);

        boolean do2x2 = cfg.getBoolean("replant2x2", true);
        List<Location> plannedSpots = SaplingHelper.planReplantSpotsFromCluster(origin.getLocation(), cluster, originalLogType, do2x2);

        event.setCancelled(true);

        boolean consumeDurability = cfg.getBoolean("consumeDurabilityPerBlock", true);
        int broken = 0;
        for (Block b : cluster) {
            if (consumeDurability && (tool == null || tool.getType() == Material.AIR || tool.getAmount() <= 0)) break;
            b.breakNaturally(tool);
            broken++;
            if (consumeDurability && !damageTool(tool)) tool.setAmount(0);
        }

        if (broken > 0 && cfg.getBoolean("replantSapling", true)) {
            Material sapling = SaplingHelper.saplingFor(originalLogType);
            if (sapling != null && !plannedSpots.isEmpty()) {
                int delay = cfg.getInt("saplingReplantDelayTicks", 20);
                new BukkitRunnable() {
                    @Override public void run() {
                        for (Location base : plannedSpots) {
                            SaplingHelper.plantSapling(base, sapling);
                        }
                    }
                }.runTaskLater(plugin, delay);
            }
        }

        if (broken > 0) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§aTimber: §f" + broken + " logs"));
        }
    }

    private boolean damageTool(ItemStack tool) {
        if (tool == null) return false;
        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable dmg)) return true;
        int lvl = tool.getEnchantmentLevel(Enchantment.UNBREAKING);
        boolean shouldDamage = lvl <= 0 || (ThreadLocalRandom.current().nextInt(lvl + 1) == 0);
        if (shouldDamage) {
            int newDamage = dmg.getDamage() + 1;
            int max = tool.getType().getMaxDurability();
            if (newDamage >= max) return false;
            dmg.setDamage(newDamage);
            tool.setItemMeta(dmg);
        }
        return true;
    }
}