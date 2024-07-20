package org.oreo.wartimer.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.oreo.wartimer.Wartimer;
import phonon.nodes.Nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;



public class WarTimerCommand implements CommandExecutor, TabCompleter {

    //TODO make these booleans accessible via the command

    private boolean canAnexTerritory = true;
    private boolean canOnlyAttackBorders = false;
    private boolean destructionEnabled = true; //TODO Im not sure what this one does make sure to check it out

    private final Wartimer plugin;

    public WarTimerCommand(Wartimer plugin) {
        this.plugin = plugin;
        initialize(plugin);
    }

    public static void initialize(Wartimer plugin){ //TODO I don't know if this is good either
        warTimer = plugin.getConfig().getInt("shutDownTimer");
        warTimerPaused = plugin.getConfig().getBoolean("wasWarPaused");
    }

    public static int warTimer;
    public static boolean warTimerPaused;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "You don't have permissions to use this command");
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "You must specify a use");
            sender.sendMessage(ChatColor.RED + "Do /warTimer help for a list of commands");
            return true;
        }

        boolean isNodesWarOn = Nodes.INSTANCE.getWar().getEnabled$nodes();

        switch (args[0].toLowerCase()) {
            case "help":
                sender.sendMessage("/warTimer start <number> <unit of time> - Sets a war at the specified amount of time");
                sender.sendMessage("/warTimer end - Ends the war and deletes the timer");
                sender.sendMessage("/warTimer increment <number> <unit of time> - Adds the defined time to the war timer");
                sender.sendMessage("/warTimer decrement <number> <unit of time> - Removes the defined time from the war timer");
                sender.sendMessage("/warTimer pause - Pauses the war but keeps the current time left");
                sender.sendMessage("/warTimer resume - Resumes the war");
                return true;

            case "start":
                return handleStartCommand(sender, args, isNodesWarOn);

            case "end":
                return handleEndCommand(sender, isNodesWarOn);

            case "increment":
                return handleIncrementCommand(sender, args);

            case "decrement":
                return handleDecrementCommand(sender, args);

            case "pause":
                return handlePauseCommand(sender, isNodesWarOn);

            case "resume":
                return handleResumeCommand(sender);

            case "get-timer":
                return handleGetTimerCommand(sender,args);

            default:
                sender.sendMessage(ChatColor.RED + "Unknown command. Do /warTimer help for a list of commands.");
                return true;
        }
    }

    private boolean handleStartCommand(CommandSender sender, String[] args, boolean isNodesWarOn) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /warTimer start <number> <unit of time>");
            return true;
        }

        if (warTimer > 0 || isNodesWarOn) {
            sender.sendMessage(ChatColor.RED + "War is already enabled");
            return true;
        }

        String numberStr = args[1];
        String unitOfTime = args[2].toLowerCase();

        if (!isInteger(numberStr)) {
            sender.sendMessage(ChatColor.RED + "The amount of time needs to be a whole number");
            return true;
        }

        int number = Integer.parseInt(numberStr);
        switch (unitOfTime) {
            case "h":
                warTimer("h", number);
                sender.sendMessage(ChatColor.GREEN + "War started for " + number + " hours.");
                break;
            case "m":
                warTimer("m", number);
                sender.sendMessage(ChatColor.GREEN + "War started for " + number + " minutes.");
                break;
            case "s":
                warTimer("s", number);
                sender.sendMessage(ChatColor.GREEN + "War started for " + number + " seconds.");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Invalid unit of time. Use s, m, or h.");
                return true;
        }
        return true;
    }

    private boolean handleEndCommand(CommandSender sender, boolean isNodesWarOn) {
        if (!isNodesWarOn) {
            sender.sendMessage(ChatColor.RED + "Nodes war is already off");
            return true;
        }

        warTimer = 0;
        Nodes.INSTANCE.disableWar();
        sender.sendMessage(ChatColor.GREEN + "War has ended.");
        return true;
    }

    private boolean handleIncrementCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /warTimer increment <number> <unit of time>");
            return true;
        }

        return modifyTimer(sender, args, true);
    }

    private boolean handleDecrementCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /warTimer decrement <number> <unit of time>");
            return true;
        }

        return modifyTimer(sender, args, false);
    }

    private boolean handlePauseCommand(CommandSender sender, boolean isNodesWarOn) {
        if (!isNodesWarOn) {
            sender.sendMessage(ChatColor.RED + "Nodes war is off");
            return true;
        }

        if (!warTimerPaused) {
            warTimerPaused = true;
            sender.sendMessage(ChatColor.GREEN + "War has been paused.");
        } else {
            sender.sendMessage(ChatColor.GREEN + "War is already paused.");
        }
        return true;
    }

    private boolean handleResumeCommand(CommandSender sender) {
        if (warTimerPaused) {
            warTimerPaused = false;
            sender.sendMessage(ChatColor.GREEN + "War has been resumed.");
            warTimer("s", warTimer);
        } else {
            sender.sendMessage(ChatColor.GREEN + "War is already resumed.");
        }
        return true;
    }

    private boolean handleGetTimerCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /warTimer get-timer <unit of time>");
            return true;
        }

        String unit = args[1].toLowerCase();
        switch (unit) {
            case "s":
                sender.sendMessage(ChatColor.DARK_GREEN + "War has " + warTimer + " seconds left.");
                break;
            case "m":
                sender.sendMessage(ChatColor.DARK_GREEN + "War has " + warTimer / 60 + " minutes left.");
                break;
            case "h":
                sender.sendMessage(ChatColor.DARK_GREEN + "War has " + (warTimer / 60) / 60 + " hours left.");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Invalid unit of time. Use s, m, or h.");
                return true;
        }
        return true;
    }

    private boolean modifyTimer(CommandSender sender, String[] args, boolean increment) {
        String numberStr = args[1];
        String unitOfTime = args[2].toLowerCase();

        if (!isInteger(numberStr)) {
            sender.sendMessage(ChatColor.RED + "The amount of time needs to be a whole number");
            return true;
        }

        int number = Integer.parseInt(numberStr);
        int timeInSeconds = 0;

        switch (unitOfTime) {
            case "h":
                timeInSeconds = number * 60 * 60;
                break;
            case "m":
                timeInSeconds = number * 60;
                break;
            case "s":
                timeInSeconds = number;
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Invalid unit of time. Use s, m, or h.");
                return true;
        }

        if (increment) {
            warTimer += timeInSeconds;
            sender.sendMessage(ChatColor.GREEN + "War incremented by " + number + " " + unitOfTime + ".");
        } else {
            warTimer -= timeInSeconds;
            sender.sendMessage(ChatColor.GREEN + "War decremented by " + number + " " + unitOfTime + ".");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String alias, String[] args) {
        if (!commandSender.isOp()) {
            return Collections.emptyList();
        }

        List<String> availableCommands = new ArrayList<>();

        if (args.length == 1) {
            availableCommands.add("start");
            availableCommands.add("end");
            availableCommands.add("increment");
            availableCommands.add("decrement");
            availableCommands.add("pause");
            availableCommands.add("resume");
            availableCommands.add("get-timer");

            return availableCommands.stream()
                    .filter(cmd -> cmd.startsWith(args[0]))
                    .collect(Collectors.toList());
        }


        if (args.length == 3 && isInteger(args[1]) || args[0].equalsIgnoreCase("get-timer")) {
            availableCommands.add("h");
            availableCommands.add("m");
            availableCommands.add("s");

            return availableCommands;
        }

        return Collections.emptyList();
    }

    private boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void warTimer(String unit, int time) {
        if (unit.equalsIgnoreCase("m")) {
            time *= 60;
        } else if (unit.equalsIgnoreCase("h")) {
            time *= 60 * 60;
        }

        Nodes.INSTANCE.enableWar(canAnexTerritory, canOnlyAttackBorders, destructionEnabled);
        for (Player playerM : Bukkit.getOnlinePlayers()) {
            playerM.sendMessage(ChatColor.DARK_RED +""+ ChatColor.BOLD + "[War] Nodes war enabled");
        }


        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.0f);
        }

        warTimer = time;

        new BukkitRunnable() {
            @Override
            public void run() {
                warTimer--;

                if (warTimerPaused){
                    Nodes.INSTANCE.disableWar();
                    for (Player playerM : Bukkit.getOnlinePlayers()) {
                        playerM.sendMessage(ChatColor.DARK_RED +""+ ChatColor.BOLD + "[War] Nodes war Paused");
                    }
                    this.cancel();
                }

                if (warTimer <= 0) {
                    warTimer = 0; //TODO MAKE THIS MODIFY THE CONFIG FILE
                    Nodes.INSTANCE.disableWar();
                    for (Player playerM : Bukkit.getOnlinePlayers()) {
                        playerM.sendMessage(ChatColor.DARK_RED +""+ ChatColor.BOLD + "[War] Nodes war disabled");
                    }
                    this.cancel();
                }

                if (warTimer % 60 == 0){ //TODO check if this even works
                    // I dont think periodically saving will be a good idea but its the best i can think of rn
                    plugin.getConfig().set("shutDownTimer",warTimer);
                    plugin.getConfig().set("wasWarPaused",warTimerPaused);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
}
