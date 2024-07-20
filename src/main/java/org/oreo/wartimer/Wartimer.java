package org.oreo.wartimer;

import org.bukkit.plugin.java.JavaPlugin;
import org.oreo.wartimer.commands.WarTimerCommand;

public final class Wartimer extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("WarTimer is on");
        getCommand("warTimer").setExecutor(new WarTimerCommand(this)); // Register a command
    }
}
