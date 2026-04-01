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

import io.github.rypofalem.armorstandeditor.ArmorStandEditorPlugin;
import io.github.rypofalem.armorstandeditor.Debug;
import io.github.rypofalem.armorstandeditor.PlayerEditor;

import io.github.rypofalem.armorstandeditor.modes.EditMode;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.EulerAngle;

public class PresetArmorPosesMenu {
    private final Inventory menuInv;
    private final Debug debug;
    private final PlayerEditor pe;
    private final ArmorStandEditorPlugin plugin = ArmorStandEditorPlugin.instance();
    private final ArmorStand armorstand;

	public PresetArmorPosesMenu(PlayerEditor pe, ArmorStand as) {
        this.pe = pe;
        this.armorstand = as;
        this.debug = new Debug(pe.plugin);
		Component name = plugin.getLang().getMessage("presettitle", "menutitle");
        menuInv = Bukkit.createInventory(pe.getManager().getPresetHolder(), 36, name);
    }

    @SuppressWarnings("UnstableApiUsage")
	private void fillInventory() {
        menuInv.clear();

        //Blank Slots
        ItemStack blank = createIcon(ItemType.BLACK_STAINED_GLASS_PANE.createItemStack(), "blankslot");

        //Presets -- Here to test things out, will get better names soon TM
        ItemStack sitting = createIcon(ItemType.ARMOR_STAND.createItemStack(), "sitting");
        ItemStack waving = createIcon(ItemType.ARMOR_STAND.createItemStack(2), "waving");
        ItemStack greet1 = createIcon(ItemType.ARMOR_STAND.createItemStack(3), "greeting 1");
        ItemStack greet2 = createIcon(ItemType.ARMOR_STAND.createItemStack(4), "greeting 2");
        ItemStack cheer = createIcon(ItemType.ARMOR_STAND.createItemStack(5), "cheers");
        ItemStack archer = createIcon(ItemType.ARMOR_STAND.createItemStack(6), "archer");
        ItemStack dancing = createIcon(ItemType.ARMOR_STAND.createItemStack(7), "dancing");
        ItemStack hanging = createIcon(ItemType.ARMOR_STAND.createItemStack(8), "hanging");
        ItemStack present = createIcon(ItemType.ARMOR_STAND.createItemStack(9), "present");
        ItemStack fishing = createIcon(ItemType.ARMOR_STAND.createItemStack(10), "fishing");

        //Utilities
        ItemStack backToMenu = createIcon(ItemType.RED_WOOL.createItemStack(), "backtomenu");
        ItemStack howToPreset = createIcon(ItemType.BOOK.createItemStack(), "howtopreset");

        //Build for the Menu ---- DO NOT MODIFY THIS UNLESS YOU KNOW WHAT YOU ARE DOING!
        ItemStack[] items = {
            blank, blank, blank, blank, blank, blank, blank, blank, blank,
            blank, backToMenu, sitting, waving, greet1, greet2, cheer, archer, blank,
            blank, howToPreset, dancing, hanging, present, fishing, blank, blank, blank,
            blank, blank, blank, blank, blank, blank, blank, blank, blank
        };

        menuInv.setContents(items);
    }

    @SuppressWarnings("UnstableApiUsage")
	private ItemStack createIcon(ItemStack icon, String path) {
        icon.setData(DataComponentTypes.CUSTOM_NAME, getIconName(path));
        icon.editPersistentDataContainer(
				pdc -> pdc.set(ArmorStandEditorPlugin.instance().getIconKey(),
							   PersistentDataType.STRING, path));
        icon.setData(DataComponentTypes.LORE, ItemLore.lore().addLine(getIconDescription(path)).build());
        icon.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay()
				.addHiddenComponents(DataComponentTypes.ATTRIBUTE_MODIFIERS).build());
		return icon;
    }

    private Component getIconName(String path) {
        return plugin.getLang().getMessage(path, "iconname");
    }

    private Component getIconDescription(String path) {
        return plugin.getLang().getMessage(path + ".description", "icondescription");
    }

    public void openMenu() {
        if (pe.getPlayer().hasPermission("asedit.basic")) {
            fillInventory();
            debug.log("Player '" + pe.getPlayer().getName() + "' has opened the ArmorStand Preset Menu");
            pe.getPlayer().openInventory(menuInv);
        }
    }

    public void handlePresetPose(String itemName, Player player) {
        if (itemName == null) return;
        if (player == null) return;

        debug.log("Player '" + player.getName() + "' has chosen the Preset AS Pose '" + itemName + "'");
        //Do the Preset
		switch (itemName) {
			case "sitting" -> {
				setPresetPose(player, 345, 0, 10, 350, 0, 350, 280, 20, 0, 280, 340, 0, 0, 0, 0, 0, 0, 0);
				player.playSound(player.getLocation(), Sound.BLOCK_COMPARATOR_CLICK, 1, 1);
				player.closeInventory();
			}
			case "waving" -> {
				setPresetPose(player, 220, 20, 0, 350, 0, 350, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
				player.playSound(player.getLocation(), Sound.BLOCK_COMPARATOR_CLICK, 1, 1);
				player.closeInventory();
			}
			case "greeting 1" -> {
				setPresetPose(player, 260, 20, 0, 260, 340, 0, 340, 0, 0, 20, 0, 0, 0, 0, 0, 0, 0, 0);
				player.playSound(player.getLocation(), Sound.BLOCK_COMPARATOR_CLICK, 1, 1);
				player.closeInventory();
			}
			case "greeting 2" -> {
				setPresetPose(player, 260, 10, 0, 260, 350, 0, 320, 0, 0, 10, 0, 0, 340, 0, 350, 0, 0, 0);
				player.playSound(player.getLocation(), Sound.BLOCK_COMPARATOR_CLICK, 1, 1);
				player.closeInventory();
			}
			case "archer" -> {
				setPresetPose(player, 270, 350, 0, 280, 50, 0, 340, 0, 10, 20, 0, 350, 0, 0, 0, 0, 0, 0);
				player.playSound(player.getLocation(), Sound.BLOCK_COMPARATOR_CLICK, 1, 1);
				player.closeInventory();
			}
			case "dancing" -> {
				setPresetPose(player, 14, 0, 110, 20, 0, 250, 250, 330, 0, 15, 330, 0, 350, 350, 0, 0, 0, 0);
				player.playSound(player.getLocation(), Sound.BLOCK_COMPARATOR_CLICK, 1, 1);
				player.closeInventory();
			}
			case "cheers" -> {
				setPresetPose(player, 250, 60, 0, 20, 10, 0, 10, 0, 0, 350, 0, 0, 340, 0, 0, 0, 0, 0);
				player.playSound(player.getLocation(), Sound.BLOCK_COMPARATOR_CLICK, 1, 1);
				player.closeInventory();
			}
			case "hanging" -> {
				setPresetPose(player, 1, 33, 67, -145, -33, -4, -42, 21, 1, -100, 0, -1, -29, -38, -18, 0, -4, 0);
				player.playSound(player.getLocation(), Sound.BLOCK_COMPARATOR_CLICK, 1, 1);
				player.closeInventory();
			}
			case "present" -> {
				setPresetPose(player, 280, 330, 0, 10, 0, 350, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
				player.playSound(player.getLocation(), Sound.BLOCK_COMPARATOR_CLICK, 1, 1);
				player.closeInventory();
			}
			case "fishing" -> {
				setPresetPose(player, 300, 320, 0, 300, 40, 0, 280, 20, 0, 280, 340, 0, 0, 0, 0, 0, 0, 0);
				player.playSound(player.getLocation(), Sound.BLOCK_COMPARATOR_CLICK, 1, 1);
				player.closeInventory();
			}
			case "backtomenu" -> {
				player.playSound(player.getLocation(), Sound.BLOCK_COMPARATOR_CLICK, 1, 1);
				player.closeInventory();
				pe.openMenu();
			}
			case "howtopreset" -> {
				player.sendMessage(pe.plugin.getLang().getMessage("howtopresetmsg"));
				player.sendMessage(pe.plugin.getLang().getMessage("helpurl"));
				player.sendMessage(pe.plugin.getLang().getMessage("helpdiscord"));
				player.playSound(player.getLocation(), Sound.BLOCK_COMPARATOR_CLICK, 1, 1);
				player.closeInventory();
			}
		}
    }

    public void setPresetPose(Player player, double rightArmRoll, double rightArmYaw, double rightArmPitch,
        double leftArmRoll, double leftArmYaw, double leftArmPitch,
        double rightLegRoll, double rightLegYaw, double rightLegPitch,
        double leftLegRoll, double LeftLegYaw, double llp_yaw,
        double headRoll, double headYaw, double headPitch,
        double bodyRoll, double bodyYaw, double bodyPitch) {

        if (!EditMode.PRESET.hasPermission(player)) {
            return;
        }

        //Do the right positions based on what is given
        rightArmRoll = Math.toRadians(rightArmRoll);
        rightArmYaw = Math.toRadians(rightArmYaw);
        rightArmPitch = Math.toRadians(rightArmPitch);
        EulerAngle rightArmEulerAngle = new EulerAngle(rightArmRoll, rightArmYaw, rightArmPitch);
        armorstand.setRightArmPose(rightArmEulerAngle);

        // Calculate and set left arm settings
        leftArmRoll = Math.toRadians(leftArmRoll);
        leftArmYaw = Math.toRadians(leftArmYaw);
        leftArmPitch = Math.toRadians(leftArmPitch);
        EulerAngle leftArmEulerAngle = new EulerAngle(leftArmRoll, leftArmYaw, leftArmPitch);
        armorstand.setLeftArmPose(leftArmEulerAngle);

        // Calculate and set right leg settings
        rightLegRoll = Math.toRadians(rightLegRoll);
        rightLegYaw = Math.toRadians(rightLegYaw);
        rightLegPitch = Math.toRadians(rightLegPitch);
        EulerAngle rightLegEulerAngle = new EulerAngle(rightLegRoll, rightLegYaw, rightLegPitch);
        armorstand.setRightLegPose(rightLegEulerAngle);

        // Calculate and set left leg settings
        leftLegRoll = Math.toRadians(leftLegRoll);
        LeftLegYaw = Math.toRadians(LeftLegYaw);
        llp_yaw = Math.toRadians(llp_yaw);
        EulerAngle leftLegEulerAngle = new EulerAngle(leftLegRoll, LeftLegYaw, llp_yaw);
        armorstand.setLeftLegPose(leftLegEulerAngle);

        // Calculate and set body settings
        bodyRoll = Math.toRadians(bodyRoll);
        bodyYaw = Math.toRadians(bodyYaw);
        bodyPitch = Math.toRadians(bodyPitch);
        EulerAngle bodyEulerAngle = new EulerAngle(bodyRoll, bodyYaw, bodyPitch);
        armorstand.setBodyPose(bodyEulerAngle);

        // Calculate and set head settings
        headRoll = Math.toRadians(headRoll);
        headYaw = Math.toRadians(headYaw);
        headPitch = Math.toRadians(headPitch);
        EulerAngle headEulerAngle = new EulerAngle(headRoll, headYaw, headPitch);
        armorstand.setHeadPose(headEulerAngle);
    }
}
