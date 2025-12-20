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

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.rypofalem.armorstandeditor.modes.AdjustmentMode;
import io.github.rypofalem.armorstandeditor.modes.Axis;
import io.github.rypofalem.armorstandeditor.modes.EditMode;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;

public class Commands {
    private final ArmorStandEditorPlugin plugin;
    private final Debug debug;

    public Commands(ArmorStandEditorPlugin armorStandEditorPlugin, io.papermc.paper.command.brigadier.Commands commands) {
        this.plugin = armorStandEditorPlugin;
        this.debug = new Debug(armorStandEditorPlugin);

        // ase reload
        LiteralCommandNode<CommandSourceStack> reloadCommand = literal("reload")
                .requires(source -> source.getSender().hasPermission("asedit.reload"))
                .executes(ctx -> commandReload(ctx.getSource().getSender()))
                .build();

        // ase mode <mode>
        LiteralArgumentBuilder<CommandSourceStack> modeCommand = literal("mode")
                .requires(source -> source.getSender() instanceof Player);

        for (EditMode mode : EditMode.values()) {
            modeCommand.then(
                    literal(mode.name().toLowerCase())
                            .requires(source -> mode.hasPermission(source.getSender()))
                            .executes(ctx ->
                                              commandMode(ctx.getSource().getSender(), mode)));
        }

        // ase axis <axis>
        LiteralArgumentBuilder<CommandSourceStack> axisCommand = literal("axis")
                .requires(source -> source.getSender() instanceof Player);

        for (Axis axis : Axis.values()) {
            axisCommand.then(
                    literal(axis.name().toLowerCase())
                            .executes(ctx ->
                                              commandAxis(ctx.getSource().getSender(), axis)));
        }

        // ase adjustment <adjustmentMode>
        LiteralArgumentBuilder<CommandSourceStack> adjustmentCommand = literal("adjustment")
                .requires(source -> source.getSender() instanceof Player);

        for (AdjustmentMode adjustmentMode : AdjustmentMode.values()) {
            adjustmentCommand.then(
                    literal(adjustmentMode.name().toLowerCase())
                            .executes(ctx ->
                                              commandAdj(ctx.getSource().getSender(), adjustmentMode)));
        }

        // ase slot <slot>
        LiteralArgumentBuilder<CommandSourceStack> slotCommand = literal("slot")
                .requires(source -> source.getSender() instanceof Player)
                .then(argument("slot", integer(1, 10))
                              .executes(ctx ->
                                                commandSlot(ctx.getSource().getSender(),
                                                            ctx.getArgument("slot", Integer.class))));

        for (AdjustmentMode adjustmentMode : AdjustmentMode.values()) {
            adjustmentCommand.then(
                    literal(adjustmentMode.name().toLowerCase())
                            .executes(ctx ->
                                              commandAdj(ctx.getSource().getSender(), adjustmentMode)));
        }

        // ase give
        LiteralArgumentBuilder<CommandSourceStack> giveCommand = literal("give")
                .requires(source -> source.getSender() instanceof Player player
                        && player.hasPermission("asedit.give"))
                .executes(ctx -> commandGive(ctx.getSource().getSender()));

        // ase help
        LiteralArgumentBuilder<CommandSourceStack> helpCommand = literal("help")
                .requires(source -> source.getSender() instanceof Player)
                .executes(ctx -> commandHelp(ctx.getSource().getSender()));

        // ase
        LiteralCommandNode<CommandSourceStack> topCommand = literal("ase")
                .requires(source -> source.getSender().hasPermission("asedit.basic"))
                .then(reloadCommand)
                .then(modeCommand)
                .then(axisCommand)
                .then(adjustmentCommand)
                .then(slotCommand)
                .then(giveCommand)
                .then(helpCommand)
                .build();

        commands.register(topCommand, "Changes the function of the armorstand edit tool.",
                          List.of("asedit", "armorstandeditor"));
    }

    private int commandGive(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLang().getMessage("noconsolecom", "warn"));
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        }

        ItemStack stack = plugin.getEditTool();
        player.getInventory().addItem(stack);
        player.sendMessage(plugin.getLang().getMessage("give", "info"));

        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private int commandSlot(CommandSender sender, int slot) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLang().getMessage("noconsolecom", "warn"));
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        }

        debug.log("Player has chosen slot: " + slot);
        plugin.editorManager.getPlayerEditor(player.getUniqueId()).setCopySlot(slot);
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private int commandAdj(CommandSender sender, AdjustmentMode adjustmentMode) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLang().getMessage("noconsolecom", "warn"));
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        }

        debug.log("Player '" + player.getName() + "' sets the adjustment mode to " + adjustmentMode);
        plugin.editorManager.getPlayerEditor(player.getUniqueId()).setAdjMode(adjustmentMode);
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private int commandAxis(CommandSender sender, Axis axis) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLang().getMessage("noconsolecom", "warn"));
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        }

        debug.log("Player '" + player.getName() + "' sets the axis to " + axis);
        plugin.editorManager.getPlayerEditor(player.getUniqueId()).setAxis(axis);
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private int commandMode(CommandSender sender, EditMode mode) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLang().getMessage("noconsolecom", "warn"));
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        }

        plugin.editorManager.getPlayerEditor(player.getUniqueId()).setMode(mode);
        debug.log("Player '" + player.getName() + "' chose the mode: " + mode);
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private int commandHelp(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLang().getMessage("noconsolecom", "warn"));
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        }

        player.closeInventory();
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        player.sendMessage(plugin.getLang().getMessage("help", "info"));
        player.sendMessage(Component.empty());
        player.sendMessage(plugin.getLang().getMessage("helptips", "info"));
        player.sendMessage(Component.empty());
        player.sendMessage(plugin.getLang().getMessage("helpurl"));
        player.sendMessage(plugin.getLang().getMessage("helpdiscord"));
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private int commandReload(CommandSender sender) {
        debug.log("Performing reload of config.yml");
        plugin.performReload();
        sender.sendMessage(plugin.getLang().getMessage("reloaded"));

        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }
}
