package io.github.rypofalem.armorstandeditor;

import org.bukkit.Bukkit;

import java.util.logging.Level;

public class Debug {

	private final ArmorStandEditorPlugin plugin;

    public Debug(ArmorStandEditorPlugin plugin) {
        this.plugin = plugin;
    }

    public void log(String msg) {
		boolean debugTurnedOn = plugin.isDebug();
        if (!debugTurnedOn) return;
        Bukkit.getServer().getLogger().log(Level.INFO, "[ArmorStandEditor-Debug] " + msg);
    }
}