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
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

@SuppressWarnings({"unused"})
public class RAntiElytraTarget extends JavaPlugin implements Listener {

    private final Map<UUID, S> a = new HashMap<>();
    private int maxLogs;
    private long resetMillis;
    private String alertMessage;
    private String banCommand;
    private String banReason;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();

        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("rAntiElytraTarget aktif edildi.");
        getLogger().info("Authors: rdbull. & rest.d");
    }

    @Override
    public void onDisable() {
        getLogger().info("rAntiElytraTarget deaktif edildi.");
        getLogger().info("Authors: rdbull. & rest.d");
    }

    private void loadConfigValues() {
        FileConfiguration config = getConfig();
        this.maxLogs = config.getInt("max-logs", 7);
        int resetSeconds = config.getInt("log-reset-seconds", 15);
        this.resetMillis = resetSeconds * 1000L;
        this.alertMessage = config.getString("alert-message",
                "&c&l[Uyarı] &e%player% &6Elytra target şüphesi! &7[x%count%]");
        this.banCommand = config.getString("ban-command", "ban %player% %reason%");
        this.banReason = config.getString("ban-reason", "Elytra target tespit edildi.");
    }

    @EventHandler
    public void onHotbarSwitch(PlayerItemHeldEvent evt) {
        Player p = evt.getPlayer();
        ItemStack ch = p.getInventory().getChestplate();
        if (ch == null || ch.getType() != Material.ELYTRA || !p.isGliding()) return;

        Material oldM = e(p, evt.getPreviousSlot());
        Material newM = e(p, evt.getNewSlot());

        long now = System.currentTimeMillis();
        UUID id = p.getUniqueId();
        S s = a.getOrDefault(id, new S(oldM, newM, now, 0));

        if (now - s.t > resetMillis) s.c = 0;

        if (now - s.t <= 1L) {
            s.c++;
            for (Player pl : Bukkit.getOnlinePlayers()) {
                if (pl.hasPermission("rantielytratarget.alert")) {
                    String msg = ChatColor.translateAlternateColorCodes('&',
                            alertMessage.replace("%player%", p.getName()).replace("%count%", String.valueOf(s.c)));
                    pl.sendMessage(msg);
                }
            }
            if (s.c >= maxLogs) {
                String cmd = banCommand.replace("%player%", p.getName()).replace("%reason%", banReason);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                getLogger().info(p.getName() + " Elytra Target tespit edildi, işlem uygulandı.");
                a.remove(id);
                return;
            }
        }

        s.f = oldM;
        s.g = newM;
        s.t = now;
        a.put(id, s);
    }

    @EventHandler
    public void onPlayerHitWithFirework(EntityDamageByEntityEvent ev) {
        if (!(ev.getDamager() instanceof Player)) return;
        if (!(ev.getEntity() instanceof Player)) return;
        Player attacker = (Player) ev.getDamager();
        if (attacker.getInventory().getItemInMainHand().getType() == Material.FIREWORK_ROCKET)
            ev.setDamage(0.0D);
    }

    private Material e(Player p, int s) {
        ItemStack it = p.getInventory().getItem(s);
        return (it != null) ? it.getType() : Material.AIR;
    }

    private static class S {
        Material f, g;
        long t;
        int c;
        S(Material f, Material g, long t, int c) {
            this.f = f;
            this.g = g;
            this.t = t;
            this.c = c;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("rantielytratarget")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("rantielytratarget.reload")) {
                    sender.sendMessage(ChatColor.RED + "Bu komutu kullanma izniniz yok!");
                    return true;
                }

                reloadConfig();
                loadConfigValues();
                sender.sendMessage(ChatColor.GREEN + "rAntiElytraTarget config dosyası yeniden yüklendi!");
                getLogger().info("Config yeniden yüklendi.");
                return true;
            }

            sender.sendMessage(ChatColor.YELLOW + "Kullanım: /rantielytratarget reload");
            return true;
        }
        return false;
    }
}