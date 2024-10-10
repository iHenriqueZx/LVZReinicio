package br.com.henriplugins;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public final class LIZReinicio extends JavaPlugin implements Listener {
    private FileConfiguration config;
    private boolean restartScheduled = false;
    private int countdown = 0;
    private BukkitTask countdownTask;
    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        scheduleRestarts();
        Bukkit.getPluginManager().registerEvents(this, this);
    }
    @Override
    public void onDisable() {
    }
    private void scheduleRestarts() {
        restartScheduled = false;
        List<String> restartTimes = config.getStringList("restart-times");

        for (String time : restartTimes) {
            String[] parts = time.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            new BukkitRunnable() {
                @Override
                public void run() {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                    sdf.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));
                    String currentTime = sdf.format(new Date());

                    if (currentTime.equals(time) && !restartScheduled) {
                        scheduleCountdown();
                    }
                }
            }.runTaskTimer(this, 0L, 20L * 60);
        }
    }
    private void scheduleCountdown() {
        restartScheduled = true;
        countdown = 60;

        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (countdown == 60) {
                    announce("§eO servidor será reiniciado em 1 minuto!");
                } else if (countdown == 30) {
                    announce("§eO servidor será reiniciado em 30 segundos!");
                } else if (countdown <= 5 && countdown > 0) {
                    announce("§eReiniciando em " + countdown + " segundos...");
                } else if (countdown == 0) {
                    announce("§eReiniciando agora!");
                    restartServer();
                    cancel();
                }
                countdown--;
            }
        }.runTaskTimer(this, 0L, 20L);
    }
    private void announce(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
            player.sendTitle("§cReinício", message);
        }
    }
    private void restartServer() {
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "restart");
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("reiniciar")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("agora")) {
                    if (!restartScheduled) {
                        scheduleCountdown();
                    } else {
                        sender.sendMessage("§cUm reinício já está agendado!");
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("cancelar")) {
                    if (restartScheduled) {
                        cancelRestart();
                        sender.sendMessage("§aO reinício foi cancelado com sucesso!");
                    } else {
                        sender.sendMessage("§cNão há reinício agendado para ser cancelado.");
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("reload")) {
                    reloadConfig();
                    config = getConfig();
                    cancelRestart();
                    scheduleRestarts();
                    sender.sendMessage("§aConfigurações recarregadas com sucesso!");
                    return true;
                }
            }
        }
        return false;
    }
    private void cancelRestart() {
        restartScheduled = false;
        countdown = 0;
        if (countdownTask != null) {
            countdownTask.cancel();
        }
    }
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (countdown <= 5 && countdown > 0) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cVocê não pode colocar blocos nos últimos 5 segundos antes do reinício!");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (countdown <= 5 && countdown > 0) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cVocê não pode quebrar blocos nos últimos 5 segundos antes do reinício!");
        }
    }
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (countdown <= 5 && countdown > 0) {
            event.setCancelled(true);
            if (event.getPlayer() instanceof Player) {
                ((Player) event.getPlayer()).sendMessage("§cVocê não pode abrir containers nos últimos 5 segundos antes do reinício!");
            }
        }
    }
}
