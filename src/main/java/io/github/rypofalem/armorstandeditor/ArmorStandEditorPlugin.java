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

import io.github.rypofalem.armorstandeditor.creativeitemfilter.CreativeItemFilterHandler;
import io.github.rypofalem.armorstandeditor.customitems.CustomItemsHandler;
import io.github.rypofalem.armorstandeditor.language.Language;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;

@SuppressWarnings("UnstableApiUsage")
public final class ArmorStandEditorPlugin extends JavaPlugin implements Listener {
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    private NamespacedKey iconKey;
    private static ArmorStandEditorPlugin instance;
    private Language lang;

	public boolean hasFolia = false;

    public PlayerEditorManager editorManager;

    //Edit Tool
    private final NamespacedKey editToolKey = new NamespacedKey(this, "edit_tool");
    private final NamespacedKey recipeKey = new NamespacedKey(this, "edit_tool");
    private ItemType editToolItemType;
    private ItemStack editTool;
    private boolean pluginManagedEditTool;

    boolean enablePerWorld = false;
    List<?> allowedWorldList = null;
    private double maxScaleValue;
    private double minScaleValue;

    //GUI Settings
    boolean sendToActionBar = true;

    //Armor Stand Specific Settings
    double coarseRot;
    double fineRot;

    //Glow Entity Colors
    public Scoreboard scoreboard;
	final String lockedTeam = "ASLocked";
    final String inUseTeam = "AS-InUse";

	//Blocked Names
    List<String> blockedNames = new ArrayList<>();

    //Debugging Options.... Not Exposed
    private boolean debugFlag;

	private CustomItemsHandler customItemsHandler;
	private CreativeItemFilterHandler creativeItemFilterHandler;

	public ArmorStandEditorPlugin() {
        instance = this;
    }

    @Override
    public void onEnable() {

        if (!Util.isFolia())
            scoreboard = Objects.requireNonNull(this.getServer().getScoreboardManager()).getMainScoreboard();

        hasFolia = Util.isFolia();

        if (!hasFolia) {
            scoreboard = Objects.requireNonNull(this.getServer().getScoreboardManager()).getMainScoreboard();
            registerScoreboards(scoreboard);
        } else {
            getServer().getLogger().warning("Scoreboards currently do not work on Folia. Scoreboard Coloring will not work");
        }

        //saveResource doesn't accept File.separator on Windows, need to hardcode unix separator "/" instead
        updateConfig("", "config.yml");
		//Server Version Detection
		String languageFolderLocation = "lang/";
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

        enablePerWorld = getConfig().getBoolean("enablePerWorldSupport", false);
        if (enablePerWorld) {
            allowedWorldList = getConfig().getList("allowed-worlds", null);
            if (allowedWorldList != null && allowedWorldList.getFirst().equals("*")) {
                allowedWorldList = getServer().getWorlds().stream().map(World::getName).toList();
            }
        }

        //Send Messages to Action Bar
        sendToActionBar = getConfig().getBoolean("sendMessagesToActionBar", true);

        debugFlag = getConfig().getBoolean("debugFlag", false);
        if (debugFlag) {
            getServer().getLogger().log(Level.INFO, "[ArmorStandEditor-Debug] ArmorStandEditor Debug Mode is now ENABLED! Use this ONLY for testing Purposes. If you can see this and you have debug disabled, please report it as a bug!");
        }

        editorManager = new PlayerEditorManager(this);

        LifecycleEventManager<@NotNull Plugin> manager = getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS,
                                     event -> new Commands(this, event.registrar()));

        getServer().getPluginManager().registerEvents(editorManager, this);
        getServer().getPluginManager().registerEvents(this, this);

        pluginManagedEditTool = getConfig().getBoolean("pluginManagedTool", false);
        initEditTool();
        initRecipe();
    }

	@EventHandler
	public void onPluginEnable(PluginEnableEvent event) {
		switch (event.getPlugin().getName()) {
			case "CustomItems" -> {
				getLogger().info("Registering CustomItems provider");
				customItemsHandler = new CustomItemsHandler(this);
			}
			case "CreativeItemFilter" -> {
				getLogger().info("Initialising CreativeItemFilter handler");
				creativeItemFilterHandler = new CreativeItemFilterHandler(this);
			}
		}
	}

	@EventHandler
	public void onPluginDisable(PluginDisableEvent event) {
		switch (event.getPlugin().getName()) {
			case "CustomItems" -> {
				if (customItemsHandler != null) {
					getLogger().info("Disabling CustomItems provider");
					customItemsHandler = null;
				}
			}
			case "CreativeItemFilter" -> {
				if (creativeItemFilterHandler != null) {
					getLogger().info("Disabling WorldGuard handler");
					creativeItemFilterHandler = null;
				}
			}
		}
	}

    @EventHandler
	public void onServerResourcesReloaded(ServerResourcesReloadedEvent event) {
		initRecipe();
	}

    //Implement Glow Effects for Wolfstorm/ArmorStandEditor-Issues#5 - Add Disable Slots with Different Glow than Default
    private void registerScoreboards(Scoreboard scoreboard) {
        getServer().getLogger().info("Registering Scoreboards required for Glowing Effects");

        //Register the In Use Team First - It doesn't require a Glow Effect
        if(scoreboard.getTeam(inUseTeam) == null) {
            scoreboard.registerNewTeam(inUseTeam);
        }

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
		Team team = scoreboard.getTeam(lockedTeam);
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

    private void initEditTool() {
        String toolItemTypeName = getConfig().getString("toolMaterial", ItemType.FLINT.key().toString());

        Component editToolItemName = getConfig().getRichMessage("toolItemName", null);
        List<Component> editToolLore = getConfig().getStringList("toolLore").stream()
                .map(miniMessage::deserialize).toList();

        NamespacedKey editToolItemModel = NamespacedKey.fromString(getConfig().getString("toolItemModel", ""));
		NamespacedKey key = NamespacedKey.fromString(toolItemTypeName);

		if(key == null) {
			throw new IllegalArgumentException("Invalid type for edit tool: " + toolItemTypeName);
		}

		ItemType editToolItemType = Registry.ITEM.get(key);

		if(editToolItemType == null) {
			throw new IllegalArgumentException("Invalid type for edit tool: " + toolItemTypeName);
		}

		this.editToolItemType = editToolItemType;
        ItemStack editTool = editToolItemType.createItemStack();

        if (!pluginManagedEditTool) {
            return;
        }

		editTool.editPersistentDataContainer(
				pdc -> pdc.set(editToolKey, PersistentDataType.BOOLEAN, true));

        if (editToolItemName != null) {
		    editTool.setData(DataComponentTypes.ITEM_NAME, editToolItemName);
        }

		editTool.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
		editTool.setData(DataComponentTypes.UNBREAKABLE);
		editTool.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay()
				.addHiddenComponents(DataComponentTypes.UNBREAKABLE).build());

		if(!editToolLore.isEmpty()) {
			editTool.setData(DataComponentTypes.LORE, ItemLore.lore(editToolLore));
		}

		if(editToolItemModel != null) {
			editTool.setData(DataComponentTypes.ITEM_MODEL, editToolItemModel);
		}

		this.editTool = editTool;
    }

    private void initRecipe() {
		if(Bukkit.getRecipe(recipeKey) != null) {
			Bukkit.removeRecipe(recipeKey);
		}

        if (!getConfig().getBoolean("toolAllowCrafting") || editTool == null) {
            return;
        }

		List<String> shape = getConfig().getStringList("toolRecipeShape");
		Map<Character, RecipeChoice> ingredients = new HashMap<>();
		boolean recipeNotEmpty = false;

		if(shape.isEmpty() || shape.size() > 3) {
			throw new IllegalArgumentException("Recipe shape must contain 1-3 rows");
		}

		for (String s : shape) {
			if(s.length() > 3) {
				throw new IllegalArgumentException("Recipe rows must contain 1-3 columns");
			}

			if(!s.isEmpty()) {
				recipeNotEmpty = true;
			}

			for(int i = 0; i < s.length(); i++) {
				char c = s.charAt(i);

				if(c == ' ') {
					continue;
				}

				String ingredient = getConfig().getString("toolRecipeIngredients." + c);

				if(ingredient == null) {
					throw new IllegalArgumentException("Missing recipe ingredient: " + c);
				}

				NamespacedKey key = NamespacedKey.fromString(ingredient);

				if(key == null) {
					throw new IllegalArgumentException("Invalid recipe ingredient for " + c + ": " + ingredient);
				}

				ItemType itemType = Registry.ITEM.get(key);

				if(itemType == null) {
					throw new IllegalArgumentException("Invalid recipe ingredient for " + c + ": " + ingredient);
				}

				RecipeChoice choice;

				// Use Purpur's setPredicate when possible to exclude custom items from this and other plugins
				// in crafting recipes
				try {
					choice = new RecipeChoice.ExactChoice(itemType.createItemStack());
					Method setPredicate = choice.getClass().getMethod("setPredicate", Predicate.class);
					Predicate<ItemStack> predicate = (ItemStack item) ->
							item.getType().getKey().equals(itemType.key()) && item.getPersistentDataContainer().isEmpty();
					setPredicate.invoke(choice, predicate);
				} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
					choice = RecipeChoice.itemType(itemType);
				}

				ingredients.put(c, choice);
			}
		}

		if(!recipeNotEmpty) {
			throw new IllegalArgumentException("Recipe must not be empty");
		}

		ShapedRecipe recipe = new ShapedRecipe(recipeKey, editTool);
		recipe.setCategory(CraftingBookCategory.EQUIPMENT);

		recipe.shape(shape.toArray(new String[0]));
		ingredients.forEach(recipe::setIngredient);

		Bukkit.addRecipe(recipe);
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

		Bukkit.removeRecipe(recipeKey);

		if(customItemsHandler != null) {
			customItemsHandler.unregisterProvider();
		}
    }

    public Language getLang() {
        return lang;
    }

    public ItemStack getEditTool() {
        return this.editTool;
    }

    public boolean isEditTool(ItemStack item) {
        if (!pluginManagedEditTool) {
            return item != null && item.getType().asItemType() == editToolItemType;
        }

        return item != null && item.getPersistentDataContainer().has(editToolKey);
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

        enablePerWorld = getConfig().getBoolean("enablePerWorldSupport", false);
        if (enablePerWorld) {
            allowedWorldList = getConfig().getList("allowed-worlds", null);
            if (allowedWorldList != null && allowedWorldList.getFirst().equals("*")) {
                allowedWorldList = getServer().getWorlds().stream().map(World::getName).toList();
            }
        }

        //Send Messages to Action Bar
        sendToActionBar = getConfig().getBoolean("sendMessagesToActionBar", true);

		blockedNames = getConfig().getStringList("blocked-names");

        // Add Debug Reload
        debugFlag = getConfig().getBoolean("debugFlag", false);
        if (debugFlag) {
            getServer().getLogger().log(Level.INFO, "[ArmorStandEditor-Debug] ArmorStandEditor Debug Mode is now ENABLED! Use this ONLY for testing Purposes. If you can see this and you have debug disabled, please report it as a bug!");
        }

		pluginManagedEditTool = getConfig().getBoolean("pluginManagedTool", false);
        initEditTool();
        initRecipe();
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
