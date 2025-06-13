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
package io.github.rypofalem.armorstandeditor.api;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

public class ArmorStandRenameEvent extends ArmorStandEvent implements Cancellable {
	/* Generated for Bukkit */
	private static final HandlerList handlers = new HandlerList();
	protected final Player player;
	protected String name;
	private boolean cancelled = false;

	public ArmorStandRenameEvent(ArmorStand armorStand, Player player, String name) {
		super(armorStand);
		this.player = player;
		this.name = name;
	}

	public static HandlerList getHandlerList() {
		return (handlers);
	}

	@Override
	public HandlerList getHandlers() {
		return (handlers);
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		cancelled = cancel;
	}

	public String getName() {
		return name;
	}

	public Player getPlayer() {
		return player;
	}
}
