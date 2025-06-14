/*
 * ArmorStandEditor: Bukkit plugin to allow editing armor stand attributes
 * Copyright (C) 2016-2023  RypoFalem
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package io.github.rypofalem.armorstandeditor.menu;

import io.github.rypofalem.armorstandeditor.Debug;
import io.github.rypofalem.armorstandeditor.PlayerEditor;

import io.github.rypofalem.armorstandeditor.modes.EditMode;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

@SuppressWarnings("UnstableApiUsage")
public class EquipmentMenu {
    private final Inventory menuInv;
    private final Debug debug;
    private final PlayerEditor pe;
    private final ArmorStand armorstand;

	public EquipmentMenu(PlayerEditor pe, ArmorStand as) {
        this.pe = pe;
        this.armorstand = as;
        this.debug = new Debug(pe.plugin);
		Component name = pe.plugin.getLang().getMessage("equiptitle", "menutitle");
        menuInv = Bukkit.createInventory(pe.getManager().getEquipmentHolder(), 18, name);
    }

    private void fillInventory() {
        menuInv.clear();
        EntityEquipment equipment = armorstand.getEquipment();
        ItemStack helmet = equipment.getHelmet();
        ItemStack chest = equipment.getChestplate();
        ItemStack pants = equipment.getLeggings();
        ItemStack feetsies = equipment.getBoots();
        ItemStack rightHand = equipment.getItemInMainHand();
        ItemStack leftHand = equipment.getItemInOffHand();
        equipment.clear();
        
        ItemStack disabledIcon = new ItemStack(Material.BARRIER);
        disabledIcon.setData(DataComponentTypes.CUSTOM_NAME,
                             pe.plugin.getLang().getMessage("disabled", "warn")); //equipslot.msg <option>
        disabledIcon.editPersistentDataContainer(
                pdc -> pdc.set(pe.plugin.getIconKey(), PersistentDataType.STRING, "ase icon")); // mark as icon)

        ItemStack helmetIcon = createIcon(Material.LEATHER_HELMET, "helm");
        ItemStack chestIcon = createIcon(Material.LEATHER_CHESTPLATE, "chest");
        ItemStack pantsIcon = createIcon(Material.LEATHER_LEGGINGS, "pants");
        ItemStack feetsiesIcon = createIcon(Material.LEATHER_BOOTS, "boots");
        ItemStack rightHandIcon = createIcon(Material.WOODEN_SWORD, "rhand");
        ItemStack leftHandIcon = createIcon(Material.SHIELD, "lhand");
        ItemStack[] items =
            {helmetIcon, chestIcon, pantsIcon, feetsiesIcon, rightHandIcon, leftHandIcon, disabledIcon, disabledIcon, disabledIcon,
                helmet, chest, pants, feetsies, rightHand, leftHand, disabledIcon, disabledIcon, disabledIcon
            };
        menuInv.setContents(items);
    }

    private ItemStack createIcon(Material mat, String slot) {
        ItemStack icon = new ItemStack(mat);

        icon.editPersistentDataContainer(
                pdc -> pdc.set(pe.plugin.getIconKey(), PersistentDataType.STRING, "ase icon"));
        icon.setData(DataComponentTypes.CUSTOM_NAME,
                     pe.plugin.getLang().getMessage("equipslot", "iconname", slot)); //equipslot.msg <option>

        icon.setData(DataComponentTypes.LORE, ItemLore.lore()
                .addLine(pe.plugin.getLang().getMessage("equipslot.description", "icondescription", slot))); //equioslot.description.msg <option>));
        icon.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay()
                .addHiddenComponents(DataComponentTypes.ATTRIBUTE_MODIFIERS).build());

        return icon;
    }

    public void openMenu() {
        pe.getPlayer().closeInventory();
        if (EditMode.EQUIPMENT.hasPermission(pe.getPlayer())) {
            fillInventory();
            debug.log("Player '" + pe.getPlayer().getName() + "' has opened the Equipment Menu.");
            pe.getPlayer().openInventory(menuInv);
        }
    }

    public void equipArmorstand() {
		ItemStack helmet = menuInv.getItem(9);
		ItemStack chest = menuInv.getItem(10);
		ItemStack pants = menuInv.getItem(11);
		ItemStack feetsies = menuInv.getItem(12);
		ItemStack rightHand = menuInv.getItem(13);
		ItemStack leftHand = menuInv.getItem(14);

        debug.log("Equipping the ArmorStand with the following items: ");
        debug.log("Helmet: " + helmet);
        debug.log("Chest: " + chest);
        debug.log("Chest: " + chest);
        debug.log("Pants: " + pants);
        debug.log("Boots: " + feetsies);
        debug.log("R-Hand: " + rightHand);
        debug.log("L-Hand: " + leftHand);

        armorstand.getEquipment().setHelmet(helmet);
        armorstand.getEquipment().setChestplate(chest);
        armorstand.getEquipment().setLeggings(pants);
        armorstand.getEquipment().setBoots(feetsies);
        armorstand.getEquipment().setItemInMainHand(rightHand);
        armorstand.getEquipment().setItemInOffHand(leftHand);
    }
}
