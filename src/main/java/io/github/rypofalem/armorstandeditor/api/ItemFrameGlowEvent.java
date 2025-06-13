package io.github.rypofalem.armorstandeditor.api;

import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

public class ItemFrameGlowEvent extends ItemFrameEvent implements Cancellable {
	/* Generated for Bukkit */
	private static final HandlerList handlers = new HandlerList();
	protected final Player player;
	private boolean cancelled = false;

	public ItemFrameGlowEvent(ItemFrame itemFrame, Player player) {
		super(itemFrame);
		this.player = player;
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

	public Player getPlayer() {
		return player;
	}
}