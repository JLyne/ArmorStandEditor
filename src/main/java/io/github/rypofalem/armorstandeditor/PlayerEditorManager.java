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

import com.google.common.collect.ImmutableList;

import io.github.rypofalem.armorstandeditor.api.ItemFrameGlowEvent;
import io.github.rypofalem.armorstandeditor.menu.ASEHolder;
import io.github.rypofalem.armorstandeditor.protections.*;

import io.papermc.paper.event.player.PlayerNameEntityEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

//Manages PlayerEditors and Player Events related to editing armorstands
public class PlayerEditorManager implements Listener {
    private Debug debug;
    private ArmorStandEditorPlugin plugin;
    private HashMap<UUID, PlayerEditor> players;
    private ASEHolder menuHolder = new ASEHolder(); //Inventory holder that owns the main ase menu inventories for the plugin
    private ASEHolder equipmentHolder = new ASEHolder(); //Inventory holder that owns the equipment menu
    private ASEHolder presetHolder = new ASEHolder(); //Inventory Holder that owns the PresetArmorStand Post Menu
    double coarseAdj;
    double fineAdj;
    double coarseMov;
    double fineMov;
    private boolean ignoreNextInteract = false;
    private ArrayList<ArmorStand> as = null;
    private ArrayList<ItemFrame> itemF = null;
    private Integer noSize = 0;
    Team team;

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
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onArmorStandDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        if (!plugin.isEditTool(player.getInventory().getItemInMainHand())) return;
        if (!((event.getEntity() instanceof ArmorStand) || event.getEntity() instanceof ItemFrame)) {
            event.setCancelled(true);
            debug.log("Open Menu Called for Player: " + player.getDisplayName());
            getPlayerEditor(player.getUniqueId()).openMenu();
            return;
        }
        if (event.getEntity() instanceof ArmorStand) {
            debug.log("Player '" + player.getDisplayName() + "' has left clicked the ArmorStand");
            ArmorStand as = (ArmorStand) event.getEntity();
            event.setCancelled(true);
            if (canEdit(player, as))
                applyLeftTool(player, as);
        } else if (event.getEntity() instanceof ItemFrame) {
            debug.log(" Player '" + player.getDisplayName() + "' has right clicked on an ItemFrame");
            ItemFrame itemf = (ItemFrame) event.getEntity();
            event.setCancelled(true);
            if (canEdit(player, itemf)) applyLeftTool(player, itemf);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onArmorStandInteract(PlayerInteractAtEntityEvent event) {
        if (ignoreNextInteract) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!((event.getRightClicked() instanceof ArmorStand) || event.getRightClicked() instanceof ItemFrame)) return;

        if (event.getRightClicked() instanceof ArmorStand) {
            debug.log("Player '" + player.getDisplayName() + "' has right clicked on an ArmorStand");
            ArmorStand as = (ArmorStand) event.getRightClicked();

            if (!canEdit(player, as)) return;
            if (plugin.isEditTool(player.getInventory().getItemInMainHand())) {
                event.setCancelled(true);
                applyRightTool(player, as);
                return;
            }
        } else if (event.getRightClicked() instanceof ItemFrame) {
            ItemFrame itemFrame = (ItemFrame) event.getRightClicked();

            if (!canEdit(player, itemFrame)) return;
            if (plugin.isEditTool(player.getInventory().getItemInMainHand())) {
                if (!itemFrame.getItem().getType().equals(Material.AIR)) {
                    event.setCancelled(true);
                }
                applyRightTool(player, itemFrame);
                return;
            }

            if (player.getInventory().getItemInMainHand().getType().equals(Material.GLOW_INK_SAC) //attempt glowing
                && player.hasPermission("asedit.basic")
                && plugin.glowItemFrames && player.isSneaking()) {

                ItemFrameGlowEvent e = new ItemFrameGlowEvent(itemFrame, player);
                Bukkit.getPluginManager().callEvent(e);
                if (e.isCancelled()) return;

                ItemStack glowSacs = player.getInventory().getItemInMainHand();
                ItemStack contents = null;
                Rotation rotation = null;
                if (itemFrame.getItem().getType() != Material.AIR) {
                    contents = itemFrame.getItem(); //save item
                    rotation = itemFrame.getRotation(); // save item rotation
                }
                Location itemFrameLocation = itemFrame.getLocation();
                BlockFace facing = itemFrame.getFacing();

                if (player.getGameMode() != GameMode.CREATIVE) {
                    if (glowSacs.getAmount() > 1) {
                        glowSacs.setAmount(glowSacs.getAmount() - 1);
                    } else glowSacs = new ItemStack(Material.AIR);
                }

                itemFrame.remove();
                GlowItemFrame glowFrame = (GlowItemFrame) player.getWorld().spawnEntity(itemFrameLocation, EntityType.GLOW_ITEM_FRAME);
                glowFrame.setFacingDirection(facing);
                if (contents != null) {
                    glowFrame.setItem(contents);
                    glowFrame.setRotation(rotation);
                }

            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
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

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    void onArmorStandBreak(EntityDamageByEntityEvent event) { // Fixes issue #309
        if (!(event.getDamager() instanceof Player)) return; // If the damager is not a player, ignore.
        if (!(event.getEntity()  instanceof ArmorStand)) return; // If the damaged entity is not an ArmorStand, ignore.

        if (event.getEntity() instanceof ArmorStand entityAS) {
            // Check if the ArmorStand is invulnerable and if the damager is a player.
            if (entityAS.isInvulnerable() && event.getDamager() instanceof Player p) {
                // Check if the player is in Creative mode.
                if (p.getGameMode() == GameMode.CREATIVE) {
                    // If the player is in Creative mode and the ArmorStand is invulnerable,
                    // cancel the event to prevent breaking the ArmorStand.
                    p.sendMessage(plugin.getLang().getMessage("unabledestroycreative"));
                    event.setCancelled(true); // Cancel the event to prevent ArmorStand destruction.
                }
            }
        }

        if (event.getEntity() instanceof ArmorStand entityAS && entityAS.isDead()) {
            //TODO: Find a more permanent fix for "Once you destroy that armor stand, the armor stand will keep it's name and colour given by the name tag." THIS IS A TEMP SOLUTION FOR NOW.
            event.getEntity().setCustomName(null);
            event.getEntity().setCustomNameVisible(false);
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSwitchHands(PlayerSwapHandItemsEvent event) {
        debug.log("PlayerSwapHandItemsEvent trigger for Player: " + event.getPlayer().getDisplayName());
        if (!plugin.isEditTool(event.getOffHandItem())) return; //event assumes they are already switched
        event.setCancelled(true);
        Player player = event.getPlayer();

        as = getTargets(player); //Get All ArmorStand closest to player
        itemF = getFrameTargets(player); //Get ItemFrame Closest to Player


        // Check for null and empty lists
        if (as != null && itemF != null && !as.isEmpty() && !itemF.isEmpty()) {
            getPlayerEditor(player.getUniqueId()).sendMessage("doubletarget", "warn");
        } else if (as != null && !as.isEmpty()) {
            getPlayerEditor(player.getUniqueId()).setTarget(as);
        } else if (itemF != null && !itemF.isEmpty()) {
            getPlayerEditor(player.getUniqueId()).setFrameTarget(itemF);
        } else {
            getPlayerEditor(player.getUniqueId()).sendMessage("nodoubletarget", "warn");
        }
    }

    private ArrayList<ArmorStand> getTargets(Player player) {
        Location eyeLaser = player.getEyeLocation();
        Vector direction = player.getLocation().getDirection();
        ArrayList<ArmorStand> armorStands = new ArrayList<>();

        double STEPSIZE = .5;
        Vector STEP = direction.multiply(STEPSIZE);
        double RANGE = 10;
        double LASERRADIUS = .3;
        List<Entity> nearbyEntities = player.getNearbyEntities(RANGE, RANGE, RANGE);
        if (nearbyEntities.isEmpty()) return null;

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

    private ArrayList<ItemFrame> getFrameTargets(Player player) {
        Location eyeLaser = player.getEyeLocation();
        Vector direction = player.getLocation().getDirection();
        ArrayList<ItemFrame> itemFrames = new ArrayList<>();

        double STEPSIZE = .5;
        Vector STEP = direction.multiply(STEPSIZE);
        double RANGE = 10;
        double LASERRADIUS = .3;
        List<Entity> nearbyEntities = player.getNearbyEntities(RANGE, RANGE, RANGE);
        if (nearbyEntities.isEmpty()) return null;

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
        debug.log("Applying Left Tool on ArmorStand for Player: " + player.getDisplayName());
        getPlayerEditor(player.getUniqueId()).editArmorStand(as);
    }

    void applyLeftTool(Player player, ItemFrame itemf) {
        debug.log("Applying Left Tool on ItemFrame for Player: " + player.getDisplayName());
        getPlayerEditor(player.getUniqueId()).editItemFrame(itemf);
    }

    void applyRightTool(Player player, ItemFrame itemf) {
        debug.log("Applying Right Tool on ItemFrame for Player: " + player.getDisplayName());
        getPlayerEditor(player.getUniqueId()).editItemFrame(itemf);
    }

    void applyRightTool(Player player, ArmorStand as) {
        debug.log("Applying Right Tool on ArmorStand for Player: " + player.getDisplayName());
        getPlayerEditor(player.getUniqueId()).reverseEditArmorStand(as);
    }

    //Unused?
    @EventHandler(priority = EventPriority.LOWEST)
    void onRightClickTool(PlayerInteractEvent e) {
        if (!(e.getAction() == Action.LEFT_CLICK_AIR
            || e.getAction() == Action.RIGHT_CLICK_AIR
            || e.getAction() == Action.LEFT_CLICK_BLOCK
            || e.getAction() == Action.RIGHT_CLICK_BLOCK)) return;
        debug.log("Ran on Right Click Tool Event.");
        Player player = e.getPlayer();
        if (!plugin.isEditTool(player.getInventory().getItemInMainHand())) return;
        if (plugin.requireSneaking && !player.isSneaking()) return;
        if (!player.hasPermission("asedit.basic")) return;
        if (plugin.enablePerWorld && (!plugin.allowedWorldList.contains(player.getWorld().getName()))) {
            //Implementation for Per World ASE
            getPlayerEditor(player.getUniqueId()).sendMessage("notincorrectworld", "warn");
            e.setCancelled(true);
            return;
        }
        e.setCancelled(true);
        debug.log("Open Menu Called for Player: " + player.getDisplayName());
        getPlayerEditor(player.getUniqueId()).openMenu();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    void onScrollNCrouch(PlayerItemHeldEvent e) {
        Player player = e.getPlayer();
        if (!player.isSneaking()) return;
        if (!plugin.isEditTool(player.getInventory().getItem(e.getPreviousSlot()))) return;

        e.setCancelled(true);
        if (e.getNewSlot() == e.getPreviousSlot() + 1 || (e.getNewSlot() == 0 && e.getPreviousSlot() == 8)) {
            getPlayerEditor(player.getUniqueId()).cycleAxis(1);
        } else if (e.getNewSlot() == e.getPreviousSlot() - 1 || (e.getNewSlot() == 8 && e.getPreviousSlot() == 0)) {
            getPlayerEditor(player.getUniqueId()).cycleAxis(-1);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onPlayerMenuSelect(InventoryClickEvent e) {
        final InventoryHolder holder = e.getInventory().getHolder();

        if (holder == null) return;
        if (!(holder instanceof ASEHolder)) return;
        if (holder == menuHolder) {
            e.setCancelled(true);
            ItemStack item = e.getCurrentItem();
            if (item != null && item.hasItemMeta()) {
                Player player = (Player) e.getWhoClicked();
                String command = item.getItemMeta().getPersistentDataContainer().get(plugin.getIconKey(), PersistentDataType.STRING);
                if (command != null) {
                    player.performCommand(command);
                    return;
                }
            }
        }
        if (holder == equipmentHolder) {
            ItemStack item = e.getCurrentItem();
            if (item == null) return;
            if (item.getItemMeta() == null) return;
            if (item.getItemMeta().getPersistentDataContainer().has(plugin.getIconKey(), PersistentDataType.STRING)) {
                e.setCancelled(true);
            }
        }

        if (holder == presetHolder) {
            e.setCancelled(true);
            ItemStack item = e.getCurrentItem();
            if (item != null && item.hasItemMeta()) {
                Player player = (Player) e.getWhoClicked();
                String itemName = item.getItemMeta().getPersistentDataContainer().get(ArmorStandEditorPlugin.instance().getIconKey(), PersistentDataType.STRING);
                PlayerEditor pe = players.get(player.getUniqueId());
                pe.presetPoseMenu.handlePresetPose(itemName, player);
            }
        }
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void onPlayerMenuClose(InventoryCloseEvent e) {
        final InventoryHolder holder = e.getInventory().getHolder();

        if (holder == null) return;
        if (!(holder instanceof ASEHolder)) return;
        if (holder == equipmentHolder) {
            PlayerEditor pe = players.get(e.getPlayer().getUniqueId());
            pe.equipMenu.equipArmorstand();

            // Remove the In Use Lock
            if(!Util.isFolia()){
                team = plugin.scoreboard.getTeam(plugin.inUseTeam);
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
