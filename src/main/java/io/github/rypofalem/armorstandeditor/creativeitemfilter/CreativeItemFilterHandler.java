package io.github.rypofalem.armorstandeditor.creativeitemfilter;

import io.github.rypofalem.armorstandeditor.ArmorStandEditorPlugin;
import org.bukkit.Bukkit;
import org.hurricanegames.creativeitemfilter.CreativeItemFilter;
import org.hurricanegames.creativeitemfilter.handler.component.ItemComponentPopulatorFactory;

public class CreativeItemFilterHandler {

	public CreativeItemFilterHandler(ArmorStandEditorPlugin plugin) {
		boolean cifEnabled = Bukkit.getPluginManager().isPluginEnabled("CreativeItemFilter");

		if(!cifEnabled) {
			return;
		}

		ItemComponentPopulatorFactory factory = ((CreativeItemFilter) Bukkit.getPluginManager().getPlugin("CreativeItemFilter"))
				.getComponentPopulatorFactory();

		factory.addPopulator(new ArmorStandEditorComponentPopulator(plugin));
	}
}

