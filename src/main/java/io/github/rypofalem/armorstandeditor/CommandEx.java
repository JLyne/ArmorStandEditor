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

package io.github.rypofalem.armorstandeditor;

import io.github.rypofalem.armorstandeditor.modes.AdjustmentMode;
import io.github.rypofalem.armorstandeditor.modes.Axis;
import io.github.rypofalem.armorstandeditor.modes.EditMode;

import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CommandEx implements CommandExecutor, TabCompleter {
    ArmorStandEditorPlugin plugin;
    final String LISTMODE = "<yellow>/ase mode <" + Util.getEnumList(EditMode.class) + ">";
    final String LISTAXIS = "<yellow>/ase axis <" + Util.getEnumList(Axis.class) + ">";
    final String LISTADJUSTMENT = "<yellow>/ase adj <" + Util.getEnumList(AdjustmentMode.class) + ">";
    final String LISTSLOT = "<yellow>/ase slot <1-9>";
    final String HELP = "<yellow>/ase help or /ase ?";
    final String RELOAD = "<yellow>/ase reload";
    final String GIVECUSTOMMODEL = "<yellow>/ase give";
    final String GETARMORSTATS = "<yellow>/ase stats";
    private Debug debug;

    public CommandEx(ArmorStandEditorPlugin armorStandEditorPlugin) {

        this.plugin = armorStandEditorPlugin;
        this.debug = new Debug(armorStandEditorPlugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender instanceof ConsoleCommandSender) { //Fix to Support #267
            debug.log("Sender is CONSOLE!");
            if (args.length == 0) {
                sender.sendMessage(HELP);
                sender.sendMessage(RELOAD);
                return true;
            } else {
                switch (args[0].toLowerCase()) {
                    case "reload" -> commandReloadConsole(sender);
                    case "help", "?" -> commandHelpConsole(sender);
                    default -> {
                        sender.sendMessage(plugin.getLang().getMessage("noconsolecom", "warn"));
                    }
                }
                return true;
            }

        }

        if (sender instanceof Player player && !getPermissionBasic(player)) {
            debug.log("Sender is Player but asedit.basic is" + getPermissionBasic(player));
            sender.sendMessage(plugin.getLang().getMessage("nopermoption", "warn", "basic"));
            return true;
        } else {
            Player player = (Player) sender;

            debug.log("Sender is Player and asedit.basic is " + getPermissionBasic(player));
            if (args.length == 0) {
                player.sendRichMessage(LISTMODE);
                player.sendRichMessage(LISTAXIS);
                player.sendRichMessage(LISTSLOT);
                player.sendRichMessage(LISTADJUSTMENT);
                player.sendRichMessage(HELP);
                player.sendRichMessage(RELOAD);
                player.sendRichMessage(GIVECUSTOMMODEL);
                player.sendRichMessage(GETARMORSTATS);
                return true;
            }
            switch (args[0].toLowerCase()) {
                case "mode" -> commandMode(player, args);
                case "axis" -> commandAxis(player, args);
                case "adj" -> commandAdj(player, args);
                case "slot" -> commandSlot(player, args);
                case "help", "?" -> commandHelp(player);
                case "give" -> commandGive(player);
                case "reload" -> commandReload(player);
                case "stats" -> commandStats(player);
                default -> {
                    sender.sendRichMessage(LISTMODE);
                    sender.sendRichMessage(LISTAXIS);
                    sender.sendRichMessage(LISTSLOT);
                    sender.sendRichMessage(LISTADJUSTMENT);
                    sender.sendRichMessage(HELP);
                    sender.sendRichMessage(RELOAD);
                    sender.sendRichMessage(GIVECUSTOMMODEL);
                    sender.sendRichMessage(GETARMORSTATS);
                }
            }
            return true;
        }
    }

    // Implemented to fix:
    // https://github.com/Wolfieheart/ArmorStandEditor-Issues/issues/35 &
    // https://github.com/Wolfieheart/ArmorStandEditor-Issues/issues/30 - See Remarks OTHER
    private void commandGive(Player player) {
        if (player.hasPermission("asedit.give")) {
            ItemStack stack = new ItemStack(plugin.getEditTool()); //Only Support EditTool at the MOMENT
            ItemMeta meta = stack.getItemMeta();
            Objects.requireNonNull(meta).setCustomModelData(plugin.getCustomModelDataInt());
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            stack.setItemMeta(meta);
            player.getInventory().addItem(stack);
            player.sendMessage(plugin.getLang().getMessage("give", "info"));
        } else {
            player.sendMessage(plugin.getLang().getMessage("nogive", "warn"));
        }
    }

    private void commandSlot(Player player, String[] args) {

        if (args.length <= 1) {
            player.sendMessage(plugin.getLang().getMessage("noslotnumcom", "warn"));
            player.sendMessage(LISTSLOT);
        }

        if (args.length > 1) {
            try {
                byte slot = (byte) (Byte.parseByte(args[1]) - 0b1);
                if (slot >= 0 && slot < 9) {
                    debug.log("Player has chosen slot: " + slot);
                    plugin.editorManager.getPlayerEditor(player.getUniqueId()).setCopySlot(slot);
                } else {
                    player.sendMessage(LISTSLOT);
                }

            } catch (NumberFormatException nfe) {
                player.sendMessage(LISTSLOT);
            }
        }
    }

    private void commandAdj(Player player, String[] args) {
        if (args.length <= 1) {
            player.sendMessage(plugin.getLang().getMessage("noadjcom", "warn"));
            player.sendMessage(LISTADJUSTMENT);
        }

        if (args.length > 1) {
            for (AdjustmentMode adj : AdjustmentMode.values()) {
                if (adj.toString().toLowerCase().contentEquals(args[1].toLowerCase())) {
                    plugin.editorManager.getPlayerEditor(player.getUniqueId()).setAdjMode(adj);
                    return;
                }
            }
            player.sendMessage(LISTADJUSTMENT);
        }
    }

    private void commandAxis(Player player, String[] args) {
        if (args.length <= 1) {
            player.sendMessage(plugin.getLang().getMessage("noaxiscom", "warn"));
            player.sendMessage(LISTAXIS);
        }

        if (args.length > 1) {
            for (Axis axis : Axis.values()) {
                if (axis.toString().toLowerCase().contentEquals(args[1].toLowerCase())) {
                    debug.log("Player '" + player.getDisplayName() + "' sets the axis to " + axis);
                    plugin.editorManager.getPlayerEditor(player.getUniqueId()).setAxis(axis);
                    return;
                }
            }
            player.sendMessage(LISTAXIS);
        }
    }

    private void commandMode(Player player, String[] args) {
        if (args.length <= 1) {
            player.sendMessage(plugin.getLang().getMessage("nomodecom", "warn"));
            player.sendMessage(LISTMODE);
        }

        try {
            EditMode mode = EditMode.valueOf(args[1].toUpperCase());

            if (!mode.hasPermission(player)) {
                player.sendMessage(plugin.getLang().getMessage("nopermoption", "warn", mode.name().toLowerCase()));
                return;
            }

            plugin.editorManager.getPlayerEditor(player.getUniqueId()).setMode(mode);
            debug.log("Player '" + player.getDisplayName() + "' chose the mode: " + mode);
        } catch(IllegalArgumentException e) {

        }
    }

    private void commandHelp(Player player) {
        player.closeInventory();
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        player.sendMessage(plugin.getLang().getMessage("help", "info", plugin.editTool.name()));
        player.sendMessage("");
        player.sendMessage(plugin.getLang().getMessage("helptips", "info"));
        player.sendMessage("");
        player.sendMessage(plugin.getLang().getMessage("helpurl", ""));
        player.sendMessage(plugin.getLang().getMessage("helpdiscord", ""));
    }

    private void commandHelpConsole(CommandSender sender) {
        sender.sendMessage(plugin.getLang().getMessage("help", "info", plugin.editTool.name()));
        sender.sendMessage("");
        sender.sendMessage(plugin.getLang().getMessage("helptips", "info"));
        sender.sendMessage("");
        sender.sendMessage(plugin.getLang().getMessage("helpurl", "info"));
        sender.sendMessage(plugin.getLang().getMessage("helpdiscord", "info"));
    }

    private void commandReload(Player player) {
        debug.log("Player '" + player.getDisplayName() + "' permission check for asedit.reload: " + getPermissionReload(player));

        if (!(getPermissionReload(player))) return;
        debug.log("Performing reload of config.yml");
        plugin.performReload();
        player.sendMessage(plugin.getLang().getMessage("reloaded", ""));
    }

    private void commandReloadConsole(CommandSender sender) {
        debug.log("Console has decided to reload the plugin....");
        plugin.performReload();
        sender.sendMessage(plugin.getLang().getMessage("reloaded", "info"));
    }

    private void commandStats(Player player) {
        debug.log("Player '" + player.getDisplayName() + "' permission check for asedit.stats: " + getPermissionStats(player));

        if (getPermissionStats(player)) {
            for (Entity e : player.getNearbyEntities(1, 1, 1)) {
                if (e instanceof ArmorStand as) {

                    //Calculation TIME - Might move this out later, but is OK here for now
                    double sizeAttribute;

                    double headX = as.getHeadPose().getX();
                    headX = Math.toDegrees(headX);
                    headX = Math.rint(headX);

                    double headY = as.getHeadPose().getY();
                    headY = Math.toDegrees(headY);
                    headY = Math.rint(headY);

                    double headZ = as.getHeadPose().getZ();
                    headZ = Math.toDegrees(headZ);
                    headZ = Math.rint(headZ);

                    //Body
                    double bodyX = as.getBodyPose().getX();
                    bodyX = Math.toDegrees(bodyX);
                    bodyX = Math.rint(bodyX);

                    double bodyY = as.getBodyPose().getY();
                    bodyY = Math.toDegrees(bodyY);
                    bodyY = Math.rint(bodyY);

                    double bodyZ = as.getBodyPose().getZ();
                    bodyZ = Math.toDegrees(bodyZ);
                    bodyZ = Math.rint(bodyZ);

                    //Arms
                    double rightArmY = as.getRightArmPose().getY();
                    rightArmY = Math.toDegrees(rightArmY);
                    rightArmY = Math.rint(rightArmY);

                    double rightArmZ = as.getRightArmPose().getZ();
                    rightArmZ = Math.toDegrees(rightArmZ);
                    rightArmZ = Math.rint(rightArmZ);

                    double rightArmX = as.getRightArmPose().getX();
                    rightArmX = Math.toDegrees(rightArmX);
                    rightArmX = Math.rint(rightArmX);

                    double leftArmX = as.getLeftArmPose().getX();
                    leftArmX = Math.toDegrees(leftArmX);
                    leftArmX = Math.rint(leftArmX);

                    double leftArmY = as.getLeftArmPose().getY();
                    leftArmY = Math.toDegrees(leftArmY);
                    leftArmY = Math.rint(leftArmY);

                    double leftArmZ = as.getLeftArmPose().getZ();
                    leftArmZ = Math.toDegrees(leftArmZ);
                    leftArmZ = Math.rint(leftArmZ);

                    //Legs
                    double rightLegX = as.getRightLegPose().getX();
                    rightLegX = Math.toDegrees(rightLegX);
                    rightLegX = Math.rint(rightLegX);

                    double rightLegY = as.getRightLegPose().getY();
                    rightLegY = Math.toDegrees(rightLegY);
                    rightLegY = Math.rint(rightLegY);

                    double rightLegZ = as.getRightLegPose().getZ();
                    rightLegZ = Math.toDegrees(rightLegZ);
                    rightArmX = Math.rint(rightLegZ);

                    double leftLegX = as.getLeftLegPose().getX();
                    leftLegX = Math.toDegrees(leftLegX);
                    leftLegX = Math.rint(leftLegX);

                    double leftLegY = as.getLeftLegPose().getY();
                    leftLegY = Math.toDegrees(leftLegY);
                    leftLegY = Math.rint(leftLegY);

                    double leftLegZ = as.getLeftLegPose().getZ();
                    leftLegZ = Math.toDegrees(leftLegZ);
                    leftLegZ = Math.rint(leftLegZ);

                    sizeAttribute = Objects.requireNonNull(as.getAttribute(Attribute.SCALE)).getBaseValue();

                    //Coordinates
                    float locationX = (float) as.getLocation().getX();
                    float locationY = (float) as.getLocation().getY();
                    float locationZ = (float) as.getLocation().getZ();

                    //Toggles
                    boolean isVisible = as.isVisible();
                    boolean armsVisible = as.hasArms();
                    boolean basePlateVisible = as.hasBasePlate();
                    boolean isVulnerable = as.isInvulnerable();
                    boolean hasGravity = as.hasGravity();
                    boolean isGlowing = as.isGlowing();
                    boolean isLocked = plugin.scoreboard.getTeam(plugin.lockedTeam).hasEntry(as.getUniqueId().toString());

                    player.sendRichMessage("<yellow>----------- Armor Stand Statistics -----------");
                    player.sendRichMessage("<yellow>" + plugin.getLang().getMessage("stats"));
                    player.sendRichMessage("<yellow>Head: <aqua>" + headX + " / " + headY + " / " + headZ);
                    player.sendRichMessage("<yellow>Body: <aqua>" + bodyX + " / " + bodyY + " / " + bodyZ);
                    player.sendRichMessage("<yellow>Right Arm: <aqua>" + rightArmX + " / " + rightArmY + " / " + rightArmZ);
                    player.sendRichMessage("<yellow>Left Arm: <aqua>" + leftArmX + " / " + leftArmY + " / " + leftArmZ);
                    player.sendRichMessage("<yellow>Right Leg: <aqua>" + rightLegX + " / " + rightLegY + " / " + rightLegZ);
                    player.sendRichMessage("<yellow>Left Leg: <aqua>" + leftLegX + " / " + leftLegY + " / " + leftLegZ);
                    player.sendRichMessage("<yellow>Coordinates: <aqua>X: " + locationX + " / Y: " + locationY + " / Z: " + locationZ);
                    player.sendRichMessage("<yellow>Is Visible: <aqua>" + isVisible + ". <yellow>Arms Visible: <aqua>" + armsVisible + ". <yellow>Base Plate Visible: <aqua>" + basePlateVisible);
                    player.sendRichMessage("<yellow>Is Vulnerable: <aqua>" + isVulnerable + ". <yellow>Affected by Gravity: <aqua>" + hasGravity);
                    player.sendRichMessage("<yellow>Size: <aqua>" + sizeAttribute + "/" + plugin.getMaxScaleValue() + ". <yellow>Is Glowing: <aqua>" + isGlowing + ". <yellow>Is Locked: <aqua>" + isLocked);
                    player.sendRichMessage("<yellow>----------------------------------------------");

                }
            }
        } else {
            player.sendMessage(plugin.getLang().getMessage("norangeforstats", "warn"));
        }
    }

    private boolean checkPermission(Player player, String permName) {
		return player.hasPermission("asedit." + permName.toLowerCase());
    }

    private boolean getPermissionBasic(Player player) {
        return checkPermission(player, "basic");
    }

    private boolean getPermissionGive(Player player) {
        return checkPermission(player, "give");
    }

    private boolean getPermissionReload(Player player) {
        return checkPermission(player, "reload");
    }

    private boolean getPermissionStats(Player player) {
        return checkPermission(player, "stats");
    }

    //REFACTOR COMPLETION
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> argList = new ArrayList<>();
        Player player = (Player) sender;

        if (isCommandValid(command.getName())) {

            if (args.length == 1) {
                argList.add("mode");
                argList.add("axis");
                argList.add("adj");
                argList.add("slot");
                argList.add("help");
                argList.add("?");

                //Will Only work with permissions
                if (getPermissionGive(player)) {
                    argList.add("give");
                }

                if (getPermissionReload(player)) {
                    argList.add("reload");
                }

                if (getPermissionStats(player)) {
                    argList.add("stats");
                }

                return argList.stream().filter(a -> a.startsWith(args[0].toLowerCase())).toList();
            }

            if (args.length == 2 && args[0].equalsIgnoreCase("mode")) {
                return getModeOptions(player, args[1].toLowerCase());
            }

            if (args.length == 2 && args[0].equalsIgnoreCase("axis")) {
                argList.addAll(getAxisOptions());
            }

            if (args.length == 2 && args[0].equalsIgnoreCase("slot")) {
                argList.addAll(getSlotOptions());
            }

            if (args.length == 2 && args[0].equalsIgnoreCase("adj")) {
                argList.addAll(getAdjOptions());
            }

            return argList.stream().filter(a -> a.toLowerCase().startsWith(args[1].toLowerCase())).toList();
        }

        return Collections.emptyList();
    }

    private boolean isCommandValid(String commandName) {
        return commandName.equalsIgnoreCase("ase") ||
            commandName.equalsIgnoreCase("armorstandeditor") ||
            commandName.equalsIgnoreCase("asedit");
    }

    private List<String> getModeOptions(Player player, String query) {
        return Arrays.stream(EditMode.values())
                .filter(m -> m.hasPermission(player))
                .map(m -> m.name().toLowerCase())
                .filter(m -> m.startsWith(query))
                .toList();
    }

    private List<String> getAxisOptions() {
        return List.of("X", "Y", "Z");
    }

    private List<String> getSlotOptions() {
        return List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
    }

    private List<String> getAdjOptions() {
        return List.of("Coarse", "Fine");
    }

}
