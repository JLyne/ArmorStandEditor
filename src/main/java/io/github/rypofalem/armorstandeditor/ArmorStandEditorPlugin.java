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

import io.github.rypofalem.armorstandeditor.language.Language;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class ArmorStandEditorPlugin extends JavaPlugin {

    private Debug debug = new Debug(this);

    private NamespacedKey iconKey;
    private static ArmorStandEditorPlugin instance;
    private Language lang;

    //Server Version Detection
    String languageFolderLocation = "lang/";
    public boolean hasFolia = false;

    //Hardcode the ASE Version
    public static final String ASE_VERSION = "1.21.5-48.3";
    public static final String SEPARATOR_FIELD = "================================";

    public PlayerEditorManager editorManager;

    //Edit Tool Information
    Material editTool;
    String toolType;
    int editToolData = Integer.MIN_VALUE;
    boolean requireToolData = false;
    boolean requireToolName = false;
    Component editToolName = null;
    boolean requireToolLore = false;
    List<?> editToolLore = null;
    boolean enablePerWorld = false;
    List<?> allowedWorldList = null;
    boolean allowCustomModelData = false;
    Integer customModelDataInt = Integer.MIN_VALUE;
    double maxScaleValue;
    double minScaleValue;

    //GUI Settings
    boolean requireSneaking = false;
    boolean sendToActionBar = true;

    //Armor Stand Specific Settings
    double coarseRot;
    double fineRot;

    //Misc Options
    boolean adminOnlyNotifications = false;

    //Glow Entity Colors
    public Scoreboard scoreboard;
    public Team team;
    List<String> asTeams = new ArrayList<>();
    String lockedTeam = "ASLocked";
    String inUseTeam = "AS-InUse";

    //Debugging Options.... Not Exposed
    boolean debugFlag;

    private static ArmorStandEditorPlugin plugin;

    public ArmorStandEditorPlugin() {
        instance = this;
    }

    @Override
    public void onEnable() {

        if (!Util.isFolia())
            scoreboard = Objects.requireNonNull(this.getServer().getScoreboardManager()).getMainScoreboard();

        //Load Messages in Console
        getLogger().info("======= ArmorStandEditor =======");
        getLogger().info("Plugin Version: v" + ASE_VERSION);

        hasFolia = Util.isFolia();

        asTeams.add(lockedTeam);
        asTeams.add(inUseTeam);

        if (!hasFolia) {
            scoreboard = Objects.requireNonNull(this.getServer().getScoreboardManager()).getMainScoreboard();
            registerScoreboards(scoreboard);
        } else {
            getServer().getLogger().warning("Scoreboards currently do not work on Folia. Scoreboard Coloring will not work");
        }


        getLogger().info(SEPARATOR_FIELD);

        //saveResource doesn't accept File.separator on Windows, need to hardcode unix separator "/" instead
        updateConfig("", "config.yml");
        updateConfig(languageFolderLocation, "de_DE.yml");
        updateConfig(languageFolderLocation, "es_ES.yml");
        updateConfig(languageFolderLocation, "fr_FR.yml");
        updateConfig(languageFolderLocation, "ja_JP.yml");
        updateConfig(languageFolderLocation, "nl_NL.yml");
        updateConfig(languageFolderLocation, "pl_PL.yml");
        updateConfig(languageFolderLocation, "pt_BR.yml");
        updateConfig(languageFolderLocation, "ro_RO.yml");
        updateConfig(languageFolderLocation, "ru_RU.yml");
        updateConfig(languageFolderLocation, "test_NA.yml");
        updateConfig(languageFolderLocation, "uk_UA.yml");
        updateConfig(languageFolderLocation, "zh_CN.yml");

        //English is the default language and needs to be unaltered to so that there is always a backup message string
        saveResource("lang/en_US.yml", true);
        lang = new Language(getConfig().getString("lang"), this);

        //Rotation
        coarseRot = getConfig().getDouble("coarse");
        fineRot = getConfig().getDouble("fine");

        // Scale Values for Size
        maxScaleValue = getConfig().getDouble("maxScaleValue");
        minScaleValue = getConfig().getDouble("minScaleValue");

        //Set Tool to be used in game
        toolType = getConfig().getString("tool");
        if (toolType != null) {
            editTool = Material.getMaterial(toolType); //Ignore Warning
        } else {
            getLogger().severe("Unable to get Tool for Use with Plugin. Unable to continue!");
            getLogger().info(SEPARATOR_FIELD);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        //Do we require a custom tool name?
        requireToolName = getConfig().getBoolean("requireToolName", false);
        if (requireToolName) {
            editToolName = getConfig().getRichMessage("toolName", null);
        }

        //Custom Model Data
        allowCustomModelData = getConfig().getBoolean("allowCustomModelData", false);

        if (allowCustomModelData) {
            customModelDataInt = getConfig().getInt("customModelDataInt", Integer.MIN_VALUE);
        }

        //Is there NBT Required for the tool
        requireToolData = getConfig().getBoolean("requireToolData", false);

        if (requireToolData) {
            editToolData = getConfig().getInt("toolData", Integer.MIN_VALUE);
        }

        requireToolLore = getConfig().getBoolean("requireToolLore", false);

        if (requireToolLore) {
            editToolLore = getConfig().getList("toolLore", null);
        }

        enablePerWorld = getConfig().getBoolean("enablePerWorldSupport", false);
        if (enablePerWorld) {
            allowedWorldList = getConfig().getList("allowed-worlds", null);
            if (allowedWorldList != null && allowedWorldList.get(0).equals("*")) {
                allowedWorldList = getServer().getWorlds().stream().map(World::getName).toList();
            }
        }

        //Require Sneaking - Wolfst0rm/ArmorStandEditor#17
        requireSneaking = getConfig().getBoolean("requireSneaking", false);

        //Send Messages to Action Bar
        sendToActionBar = getConfig().getBoolean("sendMessagesToActionBar", true);

        adminOnlyNotifications = getConfig().getBoolean("adminOnlyNotifications", true);

        debugFlag = getConfig().getBoolean("debugFlag", false);
        if (debugFlag) {
            getServer().getLogger().log(Level.INFO, "[ArmorStandEditor-Debug] ArmorStandEditor Debug Mode is now ENABLED! Use this ONLY for testing Purposes. If you can see this and you have debug disabled, please report it as a bug!");
        }

        editorManager = new PlayerEditorManager(this);
        CommandEx execute = new CommandEx(this);

        //CommandExecution and TabCompletion
        Objects.requireNonNull(getCommand("ase")).setExecutor(execute);
        Objects.requireNonNull(getCommand("ase")).setTabCompleter(execute);

        getServer().getPluginManager().registerEvents(editorManager, this);

    }

    //Implement Glow Effects for Wolfstorm/ArmorStandEditor-Issues#5 - Add Disable Slots with Different Glow than Default
    private void registerScoreboards(Scoreboard scoreboard) {
        getServer().getLogger().info("Registering Scoreboards required for Glowing Effects");

        //Register the In Use Team First - It doesnt require a Glow Effect;'/
        scoreboard.registerNewTeam(inUseTeam);

        //Fix for Scoreboard Issue reported by Starnos - Wolfst0rm/ArmorStandEditor-Issues/issues/18
        if (scoreboard.getTeam(lockedTeam) == null) {
            scoreboard.registerNewTeam(lockedTeam);
            scoreboard.getTeam(lockedTeam).color(NamedTextColor.RED);
        } else {
            getServer().getLogger().info("Scoreboard for ASLocked Already exists. Continuing to load");
        }

    }

    private void unregisterScoreboards(Scoreboard scoreboard) {
        getLogger().info("Removing Scoreboards required for Glowing Effects when Disabling Slots...");

        // Locked Team Removal
        team = scoreboard.getTeam(lockedTeam);
        if (team != null) { //Basic Sanity Check to ensure that the team is there
            team.unregister();
        } else {
            getLogger().severe("Team Already Appears to be removed. Please do not do this manually!");
        }

        //ASE-InUse Team Removal
        team = scoreboard.getTeam(inUseTeam);
        if (team != null) { //Basic Sanity Check to ensure that the team is there
            team.unregister();
        } else {
            getLogger().severe("Team Already Appears to be removed. Please do not do this manually!");
        }
    }

    private void updateConfig(String folder, String config) {
        if (!new File(getDataFolder() + File.separator + folder + config).exists()) {
            saveResource(folder + config, false);
        }
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() == editorManager.getMenuHolder()) player.closeInventory();
        }

        if (!hasFolia) {
            scoreboard = Objects.requireNonNull(this.getServer().getScoreboardManager()).getMainScoreboard();
            unregisterScoreboards(scoreboard);
        }
    }

    public Language getLang() {
        return lang;
    }

    public boolean getAllowCustomModelData() {
        return this.getConfig().getBoolean("allowCustomModelData");
    }

    public Material getEditTool() {
        return this.editTool;
    }

    public Integer getCustomModelDataInt() {
        return this.getConfig().getInt("customModelDataInt");
    }

    public boolean getAdminOnlyNotifications() {
        return this.getConfig().getBoolean("adminOnlyNotifications");
    }

    public boolean isEditTool(ItemStack itemStk) {
        if (itemStk == null) {
            return false;
        }
        if (editTool != itemStk.getType()) {
            return false;
        }

        ItemMeta itemMeta = itemStk.getItemMeta();
        if (itemMeta == null) return false;

        //FIX: Depreciated Stack for getDurability
        if (requireToolData) {
            Damageable d1 = (Damageable) itemMeta; //Get the Damageable Options for itemStk
            if (d1 != null) { //We do this to prevent NullPointers
                if (d1.getDamage() != (short) editToolData) {
                    return false;
                }
            }
        }

        if (requireToolName && editToolName != null) {
            if (!itemStk.hasItemMeta()) {
                return false;
            }

            //Get the name of the Edit Tool - If Null, return false
            Component itemName = itemMeta.displayName();

            //If the name of the Edit Tool is not the Name specified in Config then Return false
            if (!itemName.equals(editToolName)) {
                return false;
            }

        }

        if (requireToolLore && editToolLore != null) {

            //If the ItemStack does not have Metadata then we return false
            if (!itemStk.hasItemMeta()) {
                return false;
            }

            //Get the lore of the Item and if it is null - Return False
            List<String> itemLore = itemMeta.getLore();

            //If the Item does not have Lore - Return False
            boolean hasTheItemLore = itemMeta.hasLore();
            if (!hasTheItemLore) {
                return false;
            }

            //Get the localised ListString of editToolLore
            List<String> listStringOfEditToolLore = (List<String>) editToolLore;

            //Return False if itemLore on the item does not match what we expect in the config.
            if (!itemLore.equals(listStringOfEditToolLore)) {
                return false;
            }

        }

        if (allowCustomModelData && customModelDataInt != null) {
            //If the ItemStack does not have Metadata then we return false
            if (!itemStk.hasItemMeta()) {
                return false;
            }
            Integer itemCustomModel = itemMeta.getCustomModelData();
            return itemCustomModel.equals(customModelDataInt);
        }
        return true;
    }

    public void performReload() {

        //Unregister Scoreboard before before performing the reload
        if (!hasFolia) {
            scoreboard = Objects.requireNonNull(this.getServer().getScoreboardManager()).getMainScoreboard();
            unregisterScoreboards(scoreboard);
        }

        //Perform Reload
        reloadConfig();

        //Re-Register Scoreboards
        if (!hasFolia) registerScoreboards(scoreboard);

        //Reload Config File
        reloadConfig();

        //Set Language
        lang = new Language(getConfig().getString("lang"), this);


        //Rotation
        coarseRot = getConfig().getDouble("coarse");
        fineRot = getConfig().getDouble("fine");

        // Scale Values for Size
        maxScaleValue = getConfig().getDouble("maxScaleValue");
        minScaleValue = getConfig().getDouble("minScaleValue");

        //Set Tool to be used in game
        toolType = getConfig().getString("tool");
        if (toolType != null) {
            editTool = Material.getMaterial(toolType); //Ignore Warning
        }

        //Do we require a custom tool name?
        requireToolName = getConfig().getBoolean("requireToolName", false);
        if (requireToolName) {
            editToolName = getConfig().getRichMessage("toolName", null);
        }

        //Custom Model Data
        allowCustomModelData = getConfig().getBoolean("allowCustomModelData", false);

        if (allowCustomModelData) {
            customModelDataInt = getConfig().getInt("customModelDataInt", Integer.MIN_VALUE);
        }

        //Is there NBT Required for the tool
        requireToolData = getConfig().getBoolean("requireToolData", false);

        if (requireToolData) {
            editToolData = getConfig().getInt("toolData", Integer.MIN_VALUE);
        }

        requireToolLore = getConfig().getBoolean("requireToolLore", false);

        if (requireToolLore) {
            editToolLore = getConfig().getList("toolLore", null);
        }


        enablePerWorld = getConfig().getBoolean("enablePerWorldSupport", false);
        if (enablePerWorld) {
            allowedWorldList = getConfig().getList("allowed-worlds", null);
            if (allowedWorldList != null && allowedWorldList.get(0).equals("*")) {
                allowedWorldList = getServer().getWorlds().stream().map(World::getName).toList();
            }
        }

        //Require Sneaking - Wolfst0rm/ArmorStandEditor#17
        requireSneaking = getConfig().getBoolean("requireSneaking", false);

        //Send Messages to Action Bar
        sendToActionBar = getConfig().getBoolean("sendMessagesToActionBar", true);

        adminOnlyNotifications = getConfig().getBoolean("adminOnlyNotifications", true);


        // Add Debug Reload
        debugFlag = getConfig().getBoolean("debugFlag", false);
        if (debugFlag) {
            getServer().getLogger().log(Level.INFO, "[ArmorStandEditor-Debug] ArmorStandEditor Debug Mode is now ENABLED! Use this ONLY for testing Purposes. If you can see this and you have debug disabled, please report it as a bug!");
        }
    }

    public static ArmorStandEditorPlugin instance() {
        return instance;
    }

    public NamespacedKey getIconKey() {
        if (iconKey == null) iconKey = new NamespacedKey(this, "command_icon");
        return iconKey;
    }

    public double getMinScaleValue() {
        return minScaleValue;
    }

    public double getMaxScaleValue() {
        return maxScaleValue;
    }

    /**
     * For debugging ASE - Do not use this outside of Development or stuff
     */
    public boolean isDebug() {
        return debugFlag;
    }
}
