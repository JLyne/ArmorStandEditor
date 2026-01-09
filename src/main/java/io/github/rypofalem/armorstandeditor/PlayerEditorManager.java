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

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.google.common.collect.ImmutableList;

import io.github.rypofalem.armorstandeditor.menu.ASEHolder;
import io.github.rypofalem.armorstandeditor.protections.*;

import io.papermc.paper.event.player.PlayerNameEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

//Manages PlayerEditors and Player Events related to editing armorstands
public class PlayerEditorManager implements Listener {
    private final NamespacedKey highlightKey;

    private final Debug debug;
    private final ArmorStandEditorPlugin plugin;
    private final HashMap<UUID, PlayerEditor> players;
    private final ASEHolder menuHolder = new ASEHolder(); //Inventory holder that owns the main ase menu inventories for the plugin
    private final ASEHolder equipmentHolder = new ASEHolder(); //Inventory holder that owns the equipment menu
    private final ASEHolder presetHolder = new ASEHolder(); //Inventory Holder that owns the PresetArmorStand Post Menu
    private final Set<ArmorStand> highlights = new HashSet<>();
    final double coarseAdj;
    final double fineAdj;
    final double coarseMov;
    final double fineMov;

	// Instantiate protections used to determine whether a player may edit an armor stand or item frame
    private final List<Protection> protections = ImmutableList.of(
        new GriefPreventionProtection(),
        new PlotSquaredProtection(),
        new WorldGuardProtection());

    PlayerEditorManager(ArmorStandEditorPlugin plugin) {
        this.plugin = plugin;
        this.debug = new Debug(plugin);
        players = new HashMap<>();
        coarseAdj = Util.FULL_CIRCLE / plugin.coarseRot;
        fineAdj = Util.FULL_CIRCLE / plugin.fineRot;
        coarseMov = 1;
        fineMov = .03125; // 1/32

        highlightKey = new NamespacedKey(plugin, "highlight_end_time");

        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            Iterator<ArmorStand> iterator = highlights.iterator();

            while (iterator.hasNext()) {
                ArmorStand entry = iterator.next();

                if (!entry.isValid()) {
                    iterator.remove();
                    continue;
                }

                PersistentDataContainer pdc = entry.getPersistentDataContainer();

                if (!pdc.has(highlightKey)) {
                    iterator.remove();
                    continue;
                }

                if (pdc.getOrDefault(highlightKey, PersistentDataType.LONG, 0L) <= entry.getWorld().getGameTime()) {
                    iterator.remove();
                    removeHighlight(entry);
                }
            }
        }, 0, 1L);
    }

    @EventHandler(ignoreCancelled = true)
    void onArmorStandDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

		if (player.isSneaking() || !plugin.isEditTool(player.getInventory().getItemInMainHand())) {
            return;
        }

        if (!((event.getEntity() instanceof ArmorStand) || event.getEntity() instanceof ItemFrame)) {
            event.setCancelled(true);
            debug.log("Open Menu Called for Player: " + player.getName());
            getPlayerEditor(player.getUniqueId()).openMenu();
            return;
        }

        if (event.getEntity() instanceof ArmorStand armorStand) {
            debug.log("Player '" + player.getName() + "' has left clicked the ArmorStand");
            event.setCancelled(true);
            if (canEdit(player, armorStand)) {
                applyLeftTool(player, armorStand);
            }
        } else if (event.getEntity() instanceof ItemFrame itemFrame) {
            debug.log(" Player '" + player.getName() + "' has right clicked on an ItemFrame");
            event.setCancelled(true);
            if (canEdit(player, itemFrame)) applyLeftTool(player, itemFrame);
        }
    }

    @EventHandler(ignoreCancelled = true)
    void onArmorStandInteract(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();

        if (player.isSneaking()) {
            return;
        }

        if (event.getRightClicked() instanceof ArmorStand armorStand) {
            debug.log("Player '" + player.getName() + "' has right clicked on an ArmorStand");

            if (!canEdit(player, armorStand)) {
                return;
            }

            if (plugin.isEditTool(player.getInventory().getItemInMainHand())) {
                event.setCancelled(true);
                applyRightTool(player, armorStand);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();

        if (player.isSneaking()) {
            return;
        }

        if (event.getRightClicked() instanceof ItemFrame itemFrame) {
			if (!canEdit(player, itemFrame)) {
                return;
            }

            if (plugin.isEditTool(player.getInventory().getItemInMainHand())) {
                event.setCancelled(true);
                applyRightTool(player, itemFrame);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityRename(PlayerNameEntityEvent event) {
        if (!(event.getEntity() instanceof ArmorStand)) {
            return;
        }

        if (!event.getPlayer().hasPermission("asedit.rename")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityRenamed(PlayerNameEntityEvent event) {
        if (!(event.getEntity() instanceof ArmorStand armorStand)) {
            return;
        }

        if (event.getName() == null) {
            return;
        }

        // Make new name visible
        armorStand.setCustomNameVisible(true);
    }

    // Prevent breaking of invulnerable armorstands in creative mode. Fixes issue #309
    @EventHandler(ignoreCancelled = true)
    void onArmorStandBreak(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof ArmorStand armorStand)) {
            return; // If the damaged entity is not an ArmorStand, ignore.
        }

        if (armorStand.isInvulnerable()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwitchHands(PlayerSwapHandItemsEvent event) {
        debug.log("PlayerSwapHandItemsEvent trigger for Player: " + event.getPlayer().getName());
        if (!plugin.isEditTool(event.getOffHandItem())) {
            return; //event assumes they are already switched
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

		List<ArmorStand> as = getTargets(player); //Get All ArmorStand closest to player
		List<ItemFrame> itemF = getFrameTargets(player); //Get ItemFrame Closest to Player

        PlayerEditor editor = getPlayerEditor(player.getUniqueId());

		// Check for null and empty lists
        if (!as.isEmpty() && !itemF.isEmpty()) {
            editor.sendMessage("doubletarget", "warn");
        } else if (!as.isEmpty()) {
            editor.setTarget(as);
        } else if (!itemF.isEmpty()) {
            editor.setFrameTarget(itemF);
        } else {
            if (editor.isTargeting() || editor.isTargetingFrame()) {
                editor.setTarget(as);
                editor.setFrameTarget(itemF);
            } else {
                editor.sendMessage("nodoubletarget", "warn");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityAdded(EntityAddToWorldEvent event) {
        if (!(event.getEntity() instanceof ArmorStand armorStand)) {
            return;
        }

        if (armorStand.getPersistentDataContainer().has(highlightKey)) {
            highlights.add(armorStand);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityRemoved(EntityRemoveFromWorldEvent event) {
        if (!(event.getEntity() instanceof ArmorStand armorStand)) {
            return;
        }

        highlights.remove(armorStand);
    }

    private List<ArmorStand> getTargets(Player player) {
        Location eyeLaser = player.getEyeLocation();
        Vector direction = player.getLocation().getDirection();
        List<ArmorStand> armorStands = new ArrayList<>();

        double STEPSIZE = .5;
        Vector STEP = direction.multiply(STEPSIZE);
        double RANGE = 10;
        double LASERRADIUS = .3;
        List<Entity> nearbyEntities = player.getNearbyEntities(RANGE, RANGE, RANGE);
        if (nearbyEntities.isEmpty()) return armorStands;

        for (double i = 0; i < RANGE; i += STEPSIZE) {
            List<Entity> nearby = (List<Entity>) player.getWorld().getNearbyEntities(eyeLaser, LASERRADIUS, LASERRADIUS, LASERRADIUS);
            if (!nearby.isEmpty()) {
                boolean endLaser = false;
                for (Entity e : nearby) {
                    if (e instanceof ArmorStand stand) {
                        armorStands.add(stand);
                        endLaser = true;
                    }
                }

                if (endLaser) break;
            }
            if (eyeLaser.getBlock().getType().isSolid()) break;
            eyeLaser.add(STEP);
        }
        return armorStands;
    }

    private List<ItemFrame> getFrameTargets(Player player) {
        Location eyeLaser = player.getEyeLocation();
        Vector direction = player.getLocation().getDirection();
        List<ItemFrame> itemFrames = new ArrayList<>();

        double STEPSIZE = .5;
        Vector STEP = direction.multiply(STEPSIZE);
        double RANGE = 10;
        double LASERRADIUS = .3;
        List<Entity> nearbyEntities = player.getNearbyEntities(RANGE, RANGE, RANGE);
        if (nearbyEntities.isEmpty()) return itemFrames;

        for (double i = 0; i < RANGE; i += STEPSIZE) {
            List<Entity> nearby = (List<Entity>) player.getWorld().getNearbyEntities(eyeLaser, LASERRADIUS, LASERRADIUS, LASERRADIUS);
            if (!nearby.isEmpty()) {
                boolean endLaser = false;
                for (Entity e : nearby) {
                    if (e instanceof ItemFrame frame) {
                        itemFrames.add(frame);
                        endLaser = true;
                    }
                }

                if (endLaser) break;
            }
            if (eyeLaser.getBlock().getType().isSolid()) break;
            eyeLaser.add(STEP);
        }

        return itemFrames;
    }

    boolean canEdit(Player player, Entity entity) {
        return canMoveTo(player, entity.getLocation());
    }

    boolean canMoveTo(Player player, Location location) {
        Block block = location.getBlock();

        // Check if all protections allow this edit, if one fails, don't allow edit
        return protections.stream().allMatch(protection -> protection.checkPermission(block, player));
    }

    void applyLeftTool(Player player, ArmorStand as) {
        debug.log("Applying Left Tool on ArmorStand for Player: " + player.getName());
        getPlayerEditor(player.getUniqueId()).editArmorStand(as);
    }

    void applyLeftTool(Player player, ItemFrame itemf) {
        debug.log("Applying Left Tool on ItemFrame for Player: " + player.getName());
        getPlayerEditor(player.getUniqueId()).editItemFrame(itemf);
    }

    void applyRightTool(Player player, ItemFrame itemf) {
        debug.log("Applying Right Tool on ItemFrame for Player: " + player.getName());
        getPlayerEditor(player.getUniqueId()).editItemFrame(itemf);
    }

    void applyRightTool(Player player, ArmorStand as) {
        debug.log("Applying Right Tool on ArmorStand for Player: " + player.getName());
        getPlayerEditor(player.getUniqueId()).reverseEditArmorStand(as);
    }

    @EventHandler
    void onRightClickTool(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }

        debug.log("Ran on Right Click Tool Event.");
        Player player = e.getPlayer();

        if (!plugin.isEditTool(player.getInventory().getItemInMainHand())) {
            return;
        }

        if (player.isSneaking() || !player.hasPermission("asedit.basic")) {
            return;
        }

        if (plugin.enablePerWorld && (!plugin.allowedWorldList.contains(player.getWorld().getName()))) {
            //Implementation for Per World ASE
            getPlayerEditor(player.getUniqueId()).sendMessage("notincorrectworld", "warn");
            e.setCancelled(true);
            return;
        }

        e.setCancelled(true);
        debug.log("Open Menu Called for Player: " + player.getName());
        getPlayerEditor(player.getUniqueId()).openMenu();
    }

    @EventHandler(ignoreCancelled = true)
    void onScrollNCrouch(PlayerItemHeldEvent e) {
        Player player = e.getPlayer();
        if (!player.isSneaking()) {
            return;
        }

        if (!plugin.isEditTool(player.getInventory().getItem(e.getPreviousSlot()))) {
            return;
        }

        e.setCancelled(true);
        if (e.getNewSlot() == e.getPreviousSlot() + 1 || (e.getNewSlot() == 0 && e.getPreviousSlot() == 8)) {
            getPlayerEditor(player.getUniqueId()).cycleAxis(1);
        } else if (e.getNewSlot() == e.getPreviousSlot() - 1 || (e.getNewSlot() == 8 && e.getPreviousSlot() == 0)) {
            getPlayerEditor(player.getUniqueId()).cycleAxis(-1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    void onPlayerMenuSelect(InventoryClickEvent e) {
        final InventoryHolder holder = e.getInventory().getHolder();

        if (holder == null) {
            return;
        }

        if (!(holder instanceof ASEHolder)) {
            return;
        }

        if (holder == menuHolder) {
            e.setCancelled(true);
            ItemStack item = e.getCurrentItem();
            if (item != null) {
                Player player = (Player) e.getWhoClicked();
                String command = item.getPersistentDataContainer().get(plugin.getIconKey(), PersistentDataType.STRING);
                if (command != null) {
                    player.performCommand(command);
                    Bukkit.getScheduler().runTask(plugin, () -> player.closeInventory());
                    return;
                }
            }
        }

        if (holder == equipmentHolder) {
            ItemStack item = e.getCurrentItem();
            if (item == null) return;
            if (item.getPersistentDataContainer().has(plugin.getIconKey(), PersistentDataType.STRING)) {
                e.setCancelled(true);
            }
        }

        if (holder == presetHolder) {
            e.setCancelled(true);
            ItemStack item = e.getCurrentItem();
            if (item != null) {
                Player player = (Player) e.getWhoClicked();
                String itemName = item.getPersistentDataContainer().get(ArmorStandEditorPlugin.instance().getIconKey(), PersistentDataType.STRING);
                PlayerEditor pe = players.get(player.getUniqueId());
                pe.presetPoseMenu.handlePresetPose(itemName, player);
                Bukkit.getScheduler().runTask(plugin, () -> player.closeInventory());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void onPlayerMenuClose(InventoryCloseEvent e) {
        final InventoryHolder holder = e.getInventory().getHolder();

        if (holder == null) {
            return;
        }

        if (!(holder instanceof ASEHolder)) {
            return;
        }

        if (holder == equipmentHolder) {
            PlayerEditor pe = players.get(e.getPlayer().getUniqueId());
            pe.equipMenu.equipArmorstand();

            // Remove the In Use Lock
            if (!Util.isFolia()) {
				Team team = plugin.scoreboard.getTeam(plugin.inUseTeam);
                if (team != null) {
                    team.removeEntry(pe.armorStandInUseId.toString());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onPlayerLogOut(PlayerQuitEvent e) {
        removePlayerEditor(e.getPlayer().getUniqueId());
    }

    void highlight(ArmorStand armorStand) {
        armorStand.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 1, false, false));

        // Handle removing glow if armor stands ticking is disabled
        long highlightEndTime = armorStand.getWorld().getGameTime() + 100L;
        armorStand.getPersistentDataContainer().set(highlightKey, PersistentDataType.LONG, highlightEndTime);
        highlights.add(armorStand);
    }

    void removeHighlight(ArmorStand armorStand) {
        armorStand.removePotionEffect(PotionEffectType.GLOWING);
        armorStand.getPersistentDataContainer().remove(highlightKey);
        highlights.remove(armorStand);
    }

    public PlayerEditor getPlayerEditor(UUID uuid) {
        return players.containsKey(uuid) ? players.get(uuid) : addPlayerEditor(uuid);
    }

    PlayerEditor addPlayerEditor(UUID uuid) {
        PlayerEditor pe = new PlayerEditor(uuid, plugin);
        players.put(uuid, pe);
        return pe;
    }

    private void removePlayerEditor(UUID uuid) {
        players.remove(uuid);
    }

    public ASEHolder getMenuHolder() {
        return menuHolder;
    }

    public ASEHolder getEquipmentHolder() {
        return equipmentHolder;
    }

    public ASEHolder getPresetHolder() {
        return presetHolder;
    }
}
