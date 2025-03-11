package me.sialim.messagespigot;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageSpigot extends JavaPlugin implements TabExecutor {
    private List<List<String>> messages;
    private int interval;
    private int lastIndex = -1;
    private Random random;
    private BukkitTask messageTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        random = new Random();
        startMessageTask();

        getCommand("messagespigot").setExecutor(this);
        getCommand("messagespigot").setTabCompleter(this);
    }
    private void loadConfig() {
        FileConfiguration config = getConfig();
        interval = config.getInt("interval", 20);

        messages = new ArrayList<>();
        List<String> rawMessages = config.getStringList("messages");

        if (rawMessages == null || rawMessages.isEmpty()) {
            getLogger().warning("No messages found in the config!");
        } else {
            for (String msg : rawMessages) {
                messages.add(Collections.singletonList(msg));
            }
            getLogger().info("Loaded " + messages.size() + " messages.");
        }
    }

    private void startMessageTask() {
        if (messageTask != null) {
            messageTask.cancel();
        }

        messageTask = new BukkitRunnable() {
            @Override public void run() {
                broadcastRandomMessage();
            }
        }.runTaskTimer(this, 0, interval * 20L);
    }

    private void broadcastRandomMessage() {
        int randomIndex;
        if (messages == null || messages.isEmpty()) {
            getLogger().warning("No messages found in the config!");
            return;
        }
        do {
            randomIndex = random.nextInt(messages.size());
        } while (randomIndex == lastIndex);

        lastIndex = randomIndex;
        List<String> message = messages.get(randomIndex);

        for (String line : message) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(PlaceholderAPI.setPlaceholders(p, hex(line)));
            }
        }
    }

    public static String hex(String message) {
        Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String hexCode = message.substring(matcher.start(), matcher.end());
            String replaceSharp = hexCode.replace('#', 'x');

            char[] ch = replaceSharp.toCharArray();
            StringBuilder builder = new StringBuilder("");
            for (char c : ch) {
                builder.append("&" + c);
            }

            message = message.replace(hexCode, builder.toString());
            matcher = pattern.matcher(message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender.isOp()) {
            if (command.getName().equalsIgnoreCase("messagespigot") && args.length > 0) {
                if (args[0].equalsIgnoreCase("reload")) {
                    reloadConfig();
                    loadConfig();
                    startMessageTask();
                    sender.sendMessage(ChatColor.GREEN + "MessageSpigot config reloaded successfully.");
                    return true;
                }
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Operator required to run this command.");
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("messagespigot")) {
            if (args.length == 1) {
                return Arrays.asList("reload");
            }
        }
        return null;
    }
}
