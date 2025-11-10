package org.example.timber;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TimberPlugin extends JavaPlugin {

    private final Set<UUID> toggledOff = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration cfg = getConfig();
        cfg.addDefault("maxBlocks", 512);
        cfg.addDefault("Animations", true);
        cfg.options().copyDefaults(true);
        saveConfig();

        Bukkit.getPluginManager().registerEvents(new TimberListener(this), this);

        PluginCommand cmd = getCommand("timber");
        if (cmd == null) {
            getLogger().severe("Command 'timber' missing from plugin.yml! Did the jar include plugin.yml?");
        } else {
            cmd.setExecutor((sender, command, label, args) -> {
                if (!(sender instanceof org.bukkit.entity.Player player)) {
                    sender.sendMessage("This command is player-only.");
                    return true;
                }
                UUID id = player.getUniqueId();
                if (toggledOff.remove(id)) {
                    player.sendMessage("§aTimber: §fON");
                } else {
                    toggledOff.add(id);
                    player.sendMessage("§cTimber: §fOFF");
                }
                return true;
            });
        }

        getLogger().info("Timber enabled v" + getDescription().getVersion());
    }

    public boolean isToggledOff(UUID id) {
        return toggledOff.contains(id);
    }
}