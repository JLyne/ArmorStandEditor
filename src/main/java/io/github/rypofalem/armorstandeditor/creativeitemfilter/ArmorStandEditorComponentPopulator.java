package io.github.rypofalem.armorstandeditor.creativeitemfilter;

import io.github.rypofalem.armorstandeditor.ArmorStandEditorPlugin;
import org.bukkit.inventory.ItemStack;
import org.hurricanegames.creativeitemfilter.CreativeItemFilterConfiguration;
import org.hurricanegames.creativeitemfilter.handler.component.ItemComponentPopulator;
import org.jetbrains.annotations.NotNull;

public class ArmorStandEditorComponentPopulator implements ItemComponentPopulator {
	private final ArmorStandEditorPlugin plugin;

	public ArmorStandEditorComponentPopulator(ArmorStandEditorPlugin plugin) {
		this.plugin = plugin;
	}

	@SuppressWarnings("UnstableApiUsage")
	public void populateComponents(@NotNull ItemStack oldItem, @NotNull ItemStack newItem,
								   CreativeItemFilterConfiguration creativeItemFilterConfiguration) {
		if(!plugin.isEditTool(oldItem)) {
			plugin.getLogger().info("Not edit tool?");
			return;
		}

		plugin.getLogger().info("Doing stuff");
		newItem.copyDataFrom(plugin.getEditTool(), c -> true);
	}
}
