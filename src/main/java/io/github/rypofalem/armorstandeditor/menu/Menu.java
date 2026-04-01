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
import io.papermc.paper.datacomponent.item.PotionContents;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

public class Menu {
    private final Inventory menuInv;
    private final PlayerEditor pe;
	private final Debug debug;

    public Menu(PlayerEditor pe) {
        this.pe = pe;
        this.debug = new Debug(pe.plugin);
		Component name = pe.plugin.getLang().getMessage("mainmenutitle", "menutitle");
        menuInv = Bukkit.createInventory(pe.getManager().getMenuHolder(), 54, name);
        fillInventory();
    }

    @SuppressWarnings("UnstableApiUsage")
	private void fillInventory() {

        menuInv.clear();

        ItemStack xAxis;
        ItemStack yAxis;
        ItemStack zAxis;
        ItemStack coarseAdj;
        ItemStack fineAdj;
        ItemStack rotate;
        ItemStack headPos;
        ItemStack rightArmPos;
        ItemStack bodyPos;
        ItemStack leftArmPos;
        ItemStack reset;
        ItemStack showArms;
        ItemStack visibility;
        ItemStack size;
        ItemStack rightLegPos;
        ItemStack glowing;
        ItemStack leftLegPos;
        ItemStack plate;
        ItemStack copy = null;
        ItemStack paste = null;
        ItemStack slot1 = null;
        ItemStack slot2 = null;
        ItemStack slot3 = null;
        ItemStack slot4 = null;
        ItemStack help;
        ItemStack itemFrameVisible;
        ItemStack itemFrameGlow;
        ItemStack blankSlot;
        ItemStack presetItem;

        //Variables that need to be Initialized
        ItemStack place;
        ItemStack equipment;
        ItemStack disableSlots;
        ItemStack gravity;
        ItemStack toggleVulnerabilty;

        //Slots with No Value
        blankSlot = createIcon(ItemType.BLACK_STAINED_GLASS_PANE.createItemStack(),
            "blankslot", "");

        //Axis - X, Y, Z for Movement
        xAxis = createIcon(ItemType.RED_CONCRETE.createItemStack(),
            "xaxis", "axis x");

        yAxis = createIcon(ItemType.GREEN_CONCRETE.createItemStack(),
            "yaxis", "axis y");

        zAxis = createIcon(ItemType.BLUE_CONCRETE.createItemStack(),
            "zaxis", "axis z");

        //Movement Speed
        coarseAdj = createIcon(ItemType.COARSE_DIRT.createItemStack(),
            "coarseadj", "adjustment coarse");

        fineAdj = createIcon(ItemType.SMOOTH_SANDSTONE.createItemStack(),
            "fineadj", "adjustment fine");

        //Reset Changes
        reset = createIcon(ItemType.WATER_BUCKET.createItemStack(),
            "reset", "mode reset");

        //Which Part to Move
        headPos = createIcon(ItemType.IRON_HELMET.createItemStack(),
            "head", "mode head");

        bodyPos = createIcon(ItemType.IRON_CHESTPLATE.createItemStack(),
            "body", "mode body");

        leftLegPos = createIcon(ItemType.IRON_LEGGINGS.createItemStack(),
            "leftleg", "mode leftleg");

        rightLegPos = createIcon(ItemType.IRON_LEGGINGS.createItemStack(),
            "rightleg", "mode rightleg");

        leftArmPos = createIcon(ItemType.STICK.createItemStack(),
            "leftarm", "mode leftarm");

        rightArmPos = createIcon(ItemType.STICK.createItemStack(),
            "rightarm", "mode rightarm");

        showArms = createIcon(ItemType.STICK.createItemStack(),
            "showarms", "mode showarms");

        presetItem = createIcon(ItemType.BOOKSHELF.createItemStack(), "presetmenu", "mode preset");

        if (EditMode.VISIBILITY.hasPermission(pe.getPlayer())) {
            visibility = ItemType.POTION.createItemStack();
            visibility.setData(DataComponentTypes.POTION_CONTENTS,
                               PotionContents.potionContents().potion(PotionType.INVISIBILITY).build());
            createIcon(visibility, "invisible", "mode visibility");
        } else {
            visibility = blankSlot;
        }

        if (EditMode.ITEMFRAMEVISIBILITY.hasPermission(pe.getPlayer())) {
            itemFrameVisible = ItemType.ITEM_FRAME.createItemStack();
            createIcon(itemFrameVisible, "itemframevisible", "mode itemframevisibility");
        } else {
            itemFrameVisible = blankSlot;
        }

        if (EditMode.ITEMFRAMEGLOW.hasPermission(pe.getPlayer())) {
            itemFrameGlow = ItemType.GLOW_ITEM_FRAME.createItemStack();
            createIcon(itemFrameGlow, "itemframeglow", "mode itemframeglow");
        } else {
            itemFrameGlow = blankSlot;
        }

        if (EditMode.VULNERABILITY.hasPermission(pe.getPlayer())) {
            toggleVulnerabilty = createIcon(ItemType.TOTEM_OF_UNDYING.createItemStack(),
                "vulnerability", "mode vulnerability");
        } else {
            toggleVulnerabilty = blankSlot;
        }

        if (EditMode.SIZE.hasPermission(pe.getPlayer())) {
            size = createIcon(ItemType.PUFFERFISH.createItemStack(),
                "size", "mode size");
        } else {
            size = blankSlot;
        }

        if (EditMode.DISABLESLOTS.hasPermission(pe.getPlayer())) {
            disableSlots = createIcon(ItemType.BARRIER.createItemStack(), "disableslots", "mode disableslots");
        } else {
            disableSlots = blankSlot;
        }

        if (EditMode.GRAVITY.hasPermission(pe.getPlayer())) {
            gravity = createIcon(ItemType.SAND.createItemStack(), "gravity", "mode gravity");
        } else {
            gravity = blankSlot;
        }

        if (EditMode.BASEPLATE.hasPermission(pe.getPlayer())) {
            plate = createIcon(ItemType.SMOOTH_STONE_SLAB.createItemStack(),
                "baseplate", "mode baseplate");
        } else {
            plate = blankSlot;
        }

        if (EditMode.PLACEMENT.hasPermission(pe.getPlayer())) {
            place = createIcon(ItemType.RAIL.createItemStack(),
                "placement", "mode placement");
        } else {
            place = blankSlot;
        }

        if (EditMode.ROTATE.hasPermission(pe.getPlayer())) {
            rotate = createIcon(ItemType.COMPASS.createItemStack(),
                "rotate", "mode rotate");
        } else {
            rotate = blankSlot;
        }

        if (EditMode.EQUIPMENT.hasPermission(pe.getPlayer())) {
            equipment = createIcon(ItemType.CHEST.createItemStack(),
                "equipment", "mode equipment");
        } else {
            equipment = blankSlot;
        }

        if (EditMode.COPY.hasPermission(pe.getPlayer())) {
            copy = createIcon(ItemType.FLOWER_BANNER_PATTERN.createItemStack(),
                "copy", "mode copy");

            slot1 = createIcon(ItemType.BOOK.createItemStack(),
                "copyslot", "slot 1", "1");

            slot2 = createIcon(ItemType.BOOK.createItemStack(2),
                "copyslot", "slot 2", "2");

            slot3 = createIcon(ItemType.BOOK.createItemStack(3),
                "copyslot", "slot 3", "3");

            slot4 = createIcon(ItemType.BOOK.createItemStack(4),
                "copyslot", "slot 4", "4");
        }

        if (EditMode.PASTE.hasPermission(pe.getPlayer())) {
            paste = createIcon(ItemType.FEATHER.createItemStack(),
                "paste", "mode paste");
        }

        if (EditMode.GLOW.hasPermission(pe.getPlayer())) {
            glowing = createIcon(ItemType.GLOW_INK_SAC.createItemStack(),
                "armorstandglow",
                "mode glow");
        } else {
            glowing = blankSlot;
        }

        help = createIcon(ItemType.NETHER_STAR.createItemStack(), "helpgui", "help");

        ItemStack[] items = {
            help, blankSlot, blankSlot, xAxis, yAxis, zAxis, blankSlot, itemFrameVisible, itemFrameGlow,
            copy, paste, blankSlot, blankSlot, headPos, reset, blankSlot, blankSlot, blankSlot,
            slot1, slot2, blankSlot, rightArmPos, bodyPos, leftArmPos, blankSlot, rotate, place,
            slot3, slot4, blankSlot, rightLegPos, equipment, leftLegPos, blankSlot, coarseAdj, fineAdj,
            presetItem, blankSlot, blankSlot, blankSlot, blankSlot, blankSlot, blankSlot, blankSlot, disableSlots,
            blankSlot, showArms, visibility, glowing, size, plate, toggleVulnerabilty, gravity, blankSlot
        };

        menuInv.setContents(items);
    }

    private ItemStack createIcon(ItemStack icon, String path, String command) {
        return createIcon(icon, path, command, null);
    }

    @SuppressWarnings("UnstableApiUsage")
	private ItemStack createIcon(ItemStack icon, String path, String command, String option) {
        if (!command.isEmpty()) {
            icon.editPersistentDataContainer(
                    pdc -> pdc.set(ArmorStandEditorPlugin.instance().getIconKey(),
                                   PersistentDataType.STRING, "ase " + command));
            icon.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay()
                    .addHiddenComponents(DataComponentTypes.POTION_CONTENTS)
                    .addHiddenComponents(DataComponentTypes.ATTRIBUTE_MODIFIERS).build());
        } else {
            icon.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
        }

        icon.setData(DataComponentTypes.CUSTOM_NAME, getIconName(path, option));
        icon.setData(DataComponentTypes.LORE, ItemLore.lore().addLine(getIconDescription(path, option)).build());

        return icon;
    }


    private Component getIconName(String path, String option) {
        return pe.plugin.getLang().getMessage(path, "iconname", option);
    }


    private Component getIconDescription(String path, String option) {
        return pe.plugin.getLang().getMessage(path + ".description", "icondescription", option);
    }

    public void openMenu() {
        if (pe.getPlayer().hasPermission("asedit.basic")) {
            fillInventory();
            debug.log("Player '" + pe.getPlayer().getName() + "' has opened the Main ASE Menu");
            pe.getPlayer().openInventory(menuInv);
        }
    }
}