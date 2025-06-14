package io.github.rypofalem.armorstandeditor.customitems;

import io.github.rypofalem.armorstandeditor.ArmorStandEditorPlugin;
import org.bukkit.Bukkit;
import uk.co.notnull.CustomItems.api.CustomItems;

public final class CustomItemsHandler {;
	private final ArmorStandEditorItemProvider provider;
	private final CustomItems customItems = (CustomItems) Bukkit.getPluginManager().getPlugin("CustomItems");

	public CustomItemsHandler(ArmorStandEditorPlugin plugin) {
		provider = new ArmorStandEditorItemProvider(plugin);
		assert customItems != null;
		customItems.getItemManager().registerProvider(provider);
	}

	public void unregisterProvider() {
		assert customItems != null;
		customItems.getItemManager().unregisterProvider(provider);
	}
}
