package com.midnightsmp.midnightantialt;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class MidnightAntiAlt extends JavaPlugin implements Listener, CommandExecutor {

    private int maxAccountsPerIp;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();

        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("antialt") != null) {
            getCommand("antialt").setExecutor(this);
        }

        getLogger().info("MidnightAntiAlt Enabled!");
    }

    private void loadConfigValues() {
        reloadConfig();
        FileConfiguration config = getConfig();
        config.addDefault("max-accounts-per-ip", 1);
        config.addDefault("kick-message", "&c[MidnightAntiAlt] Alt accounts are not allowed! Maximum 1 account per IP.");
        config.options().copyDefaults(true);
        saveConfig();

        maxAccountsPerIp = config.getInt("max-accounts-per-ip", 1);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        String ip = event.getAddress().getHostAddress().replace(".", "_");
        String username = event.getName();

        List<String> registeredUsers = getConfig().getStringList("ip-data." + ip);
        if (registeredUsers == null) {
            registeredUsers = new ArrayList<>();
        }

        if (registeredUsers.contains(username.toLowerCase())) {
            return;
        }

        if (registeredUsers.size() >= maxAccountsPerIp) {
            String kickMsg = ChatColor.translateAlternateColorCodes('&', 
                getConfig().getString("kick-message", "&cAlt accounts are not allowed!"));
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, kickMsg);
            return;
        }

        registeredUsers.add(username.toLowerCase());
        getConfig().set("ip-data." + ip, registeredUsers);
        saveConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("midnightantialt.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            loadConfigValues();
            sender.sendMessage(ChatColor.GREEN + "[MidnightAntiAlt] Config reloaded successfully!");
            return true;
        }

        if (args.length > 1 && args[0].equalsIgnoreCase("check")) {
            String target = args[1].toLowerCase();
            String foundIp = null;
            List<String> alts = new ArrayList<>();

            if (getConfig().getConfigurationSection("ip-data") != null) {
                for (String ipKey : getConfig().getConfigurationSection("ip-data").getKeys(false)) {
                    List<String> users = getConfig().getStringList("ip-data." + ipKey);
                    if (users.contains(target)) {
                        foundIp = ipKey.replace("_", ".");
                        alts = users;
                        break;
                    }
                }
            }

            if (foundIp == null) {
                sender.sendMessage(ChatColor.RED + "No IP record found for player: " + target);
            } else {
                sender.sendMessage(ChatColor.GOLD + "=== AntiAlt Check: " + target + " ===");
                sender.sendMessage(ChatColor.YELLOW + "IP Address: " + foundIp);
                sender.sendMessage(ChatColor.YELLOW + "Linked Accounts (" + alts.size() + "): " + String.join(", ", alts));
            }
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Usage: /antialt [check <player> | reload]");
        return true;
    }
}
