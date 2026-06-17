package com.example.rantielytratarget;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class RAntiElytraTarget extends JavaPlugin implements Listener {

    private final Map<UUID, PlayerSwitchData> playerData = new HashMap<>();
    private int maxViolations;
    private long timeWindowMs;
    private int minSwitchInterval;
    private int consecutiveSwitchesRequired;
    private String alertMessage;
    private String banCommand;
    private String banReason;
    private boolean debugMode;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();

        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("rAntiElytraTarget enabled successfully.");
        getLogger().info("Authors: rdbull. & rest.d");
    }

    @Override
    public void onDisable() {
        playerData.clear();
        getLogger().info("rAntiElytraTarget disabled.");
        getLogger().info("Authors: rdbull. & rest.d");
    }

    private void loadConfigValues() {
        FileConfiguration config = getConfig();
        this.maxViolations = config.getInt("max-violations", 10);
        this.timeWindowMs = config.getInt("time-window-seconds", 3) * 1000L;
        this.minSwitchInterval = config.getInt("min-switch-interval-ms", 50);
        this.consecutiveSwitchesRequired = config.getInt("consecutive-switches-required", 5);
        this.alertMessage = config.getString("alert-message",
                "&c&l[Anti-Cheat] &e%player% &6suspicious elytra targeting detected! &7[%count%/%max%]");
        this.banCommand = config.getString("ban-command", "ban %player% %reason%");
        this.banReason = config.getString("ban-reason", "Elytra target hacks detected.");
        this.debugMode = config.getBoolean("debug-mode", false);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerData.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onHotbarSwitch(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        
        // Elytra kontrolü
        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate == null || chestplate.getType() != Material.ELYTRA) {
            return;
        }
        
        // Süzülme kontrolü
        if (!player.isGliding()) {
            return;
        }

        // Slot değişikliği kontrolü
        int previousSlot = event.getPreviousSlot();
        int newSlot = event.getNewSlot();
        
        if (previousSlot == newSlot) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        
        PlayerSwitchData data = playerData.computeIfAbsent(playerId, 
            k -> new PlayerSwitchData());

        // Zaman penceresi kontrolü -eski verileri temizle
        if (currentTime - data.firstSwitchTime > timeWindowMs) {
            data.reset(currentTime);
        }

        // Switch interval kontrolü
        long timeSinceLastSwitch = currentTime - data.lastSwitchTime;
        
        if (timeSinceLastSwitch < minSwitchInterval) {
            data.rapidSwitchCount++;
            
            if (debugMode) {
                getLogger().info(String.format("[Debug] %s - Rapid switch: %dms (Count: %d)", 
                    player.getName(), timeSinceLastSwitch, data.rapidSwitchCount));
            }
            
            // Yeterli hızlı switch var mı?
            if (data.rapidSwitchCount >= consecutiveSwitchesRequired) {
                data.violationCount++;
                
                // Alert gönder
                sendAlert(player, data.violationCount);
                
                // Ban kontrolü
                if (data.violationCount >= maxViolations) {
                    executeBan(player);
                    playerData.remove(playerId);
                    return;
                }
            }
        } else {
            // Yavaş switch - sayaçları sıfırla
            if (data.rapidSwitchCount < consecutiveSwitchesRequired) {
                data.rapidSwitchCount = 0;
            }
        }

        data.lastSwitchTime = currentTime;
        data.totalSwitches++;
        data.previousSlot = previousSlot;
        data.currentSlot = newSlot;
    }

    @EventHandler
    public void onPlayerHitWithFirework(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof Player)) return;
        
        Player attacker = (Player) event.getDamager();
        ItemStack mainHand = attacker.getInventory().getItemInMainHand();
        
        if (mainHand != null && mainHand.getType() == Material.FIREWORK_ROCKET) {
            event.setDamage(0.0D);
        }
    }

    private void sendAlert(Player player, int violations) {
        String message = ChatColor.translateAlternateColorCodes('&',
                alertMessage
                    .replace("%player%", player.getName())
                    .replace("%count%", String.valueOf(violations))
                    .replace("%max%", String.valueOf(maxViolations)));
        
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("rantielytratarget.alert")) {
                staff.sendMessage(message);
            }
        }
        
        getLogger().warning(String.format("%s triggered elytra target detection [%d/%d]", 
            player.getName(), violations, maxViolations));
    }

    private void executeBan(Player player) {
        String command = banCommand
            .replace("%player%", player.getName())
            .replace("%reason%", banReason);
        
        Bukkit.getScheduler().runTask(this, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        });
        
        getLogger().severe(String.format("%s banned for elytra target hacks.", player.getName()));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("rantielytratarget")) {
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "rAntiElytraTarget Commands:");
            sender.sendMessage(ChatColor.GRAY + "/rantielytratarget reload - Reload configuration");
            sender.sendMessage(ChatColor.GRAY + "/rantielytratarget clear <player> - Clear player violations");
            sender.sendMessage(ChatColor.GRAY + "/rantielytratarget info - Show plugin info");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("rantielytratarget.reload")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                return true;
            }

            reloadConfig();
            loadConfigValues();
            sender.sendMessage(ChatColor.GREEN + "Configuration reloaded successfully!");
            getLogger().info("Configuration reloaded by " + sender.getName());
            return true;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            if (!sender.hasPermission("rantielytratarget.clear")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /rantielytratarget clear <player>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found!");
                return true;
            }

            playerData.remove(target.getUniqueId());
            sender.sendMessage(ChatColor.GREEN + "Cleared violations for " + target.getName());
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            sender.sendMessage(ChatColor.GOLD + "=== rAntiElytraTarget Info ===");
            sender.sendMessage(ChatColor.YELLOW + "Max Violations: " + ChatColor.WHITE + maxViolations);
            sender.sendMessage(ChatColor.YELLOW + "Time Window: " + ChatColor.WHITE + (timeWindowMs / 1000) + "s");
            sender.sendMessage(ChatColor.YELLOW + "Min Switch Interval: " + ChatColor.WHITE + minSwitchInterval + "ms");
            sender.sendMessage(ChatColor.YELLOW + "Consecutive Required: " + ChatColor.WHITE + consecutiveSwitchesRequired);
            sender.sendMessage(ChatColor.YELLOW + "Tracked Players: " + ChatColor.WHITE + playerData.size());
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Unknown command. Use /rantielytratarget for help.");
        return true;
    }

    private static class PlayerSwitchData {
        long firstSwitchTime;
        long lastSwitchTime;
        int rapidSwitchCount;
        int violationCount;
        int totalSwitches;
        int previousSlot;
        int currentSlot;

        PlayerSwitchData() {
            this.firstSwitchTime = System.currentTimeMillis();
            this.lastSwitchTime = this.firstSwitchTime;
            this.rapidSwitchCount = 0;
            this.violationCount = 0;
            this.totalSwitches = 0;
            this.previousSlot = -1;
            this.currentSlot = -1;
        }

        void reset(long currentTime) {
            this.firstSwitchTime = currentTime;
            this.lastSwitchTime = currentTime;
            this.rapidSwitchCount = 0;
            this.totalSwitches = 0;
        }
    }
}
