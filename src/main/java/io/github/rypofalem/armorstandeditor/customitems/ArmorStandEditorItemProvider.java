package io.github.rypofalem.armorstandeditor.customitems;

import io.github.rypofalem.armorstandeditor.ArmorStandEditorPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import uk.co.notnull.CustomItems.api.items.CustomItem;
import uk.co.notnull.CustomItems.api.items.provider.CustomItemProvider;

import java.util.Collections;
import java.util.List;

public final class ArmorStandEditorItemProvider implements CustomItemProvider {
	private final ArmorStandEditorPlugin plugin;
	private final CustomItem editToolItem;

	public ArmorStandEditorItemProvider(ArmorStandEditorPlugin plugin) {
		this.plugin = plugin;

		editToolItem = CustomItem.builder().id(new NamespacedKey(plugin, "edit_tool"))
			.displayName(Component.text("Edit Tool"))
			.generator((player, quantity) -> plugin.getEditTool())
			.build();
	}

	public List<CustomItem> provideItems() {
		return Collections.singletonList(editToolItem);
	}

	public CustomItem identifyItem(ItemStack itemStack) {
		return plugin.isEditTool(itemStack) ? editToolItem : null;
	}
}
