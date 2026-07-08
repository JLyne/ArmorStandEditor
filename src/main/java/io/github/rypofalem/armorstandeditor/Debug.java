package io.github.rypofalem.armorstandeditor;

public class Debug {

	private final ArmorStandEditorPlugin plugin;

    public Debug(ArmorStandEditorPlugin plugin) {
        this.plugin = plugin;
    }

    public void log(String msg) {
		boolean debugTurnedOn = plugin.isDebug();
        if (!debugTurnedOn) return;
        plugin.getLogger().info(msg);
    }
}