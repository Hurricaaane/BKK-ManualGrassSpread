package eu.ha3.bukkit.manualgrass;

/*
           DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
                   Version 2, December 2004

Copyright (C) 2004 Sam Hocevar <sam@hocevar.net>

Everyone is permitted to copy and distribute verbatim or modified
copies of this license document, and changing it is allowed as long
as the name is changed.

           DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
  TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

 0. You just DO WHAT THE FUCK YOU WANT TO.

 */

import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Plugin extends JavaPlugin implements Listener
{
	private int itemID;
	
	private GrassSpread simpleSpreader;
	private GrassSpread tripleSpreader;
	
	private Set<Player> tripleUsers;
	
	@Override
	public void onEnable()
	{
		tripleUsers = new HashSet<Player>();
		
		updateFromConfig();
		
		getServer().getPluginManager().registerEvents(this, this);
		
		String commandName = getDescription().getCommands().keySet().iterator()
				.next();
		getCommand(commandName).setExecutor(new GrassTripleExecutor(this));
		
	}
	
	private void updateFromConfig()
	{
		itemID = this.getConfig().getInt("grass_spread.item");
		
		int radius = this.getConfig().getInt("grass_spread.radius");
		int height_span = this.getConfig().getInt(
				"grass_spread.height_of_split");
		
		simpleSpreader = new GrassSpread(radius, height_span);
		tripleSpreader = new GrassSpread(radius * 3, height_span * 3);
		
	}
	
	/**
	 * Changes the triple mode of a player if it can be done.
	 * 
	 * @param ply
	 * @param enable
	 */
	public void setPlayerTripleModeEnabled(Player ply, boolean enable)
	{
		if (enable)
		{
			if (canPlayerTripleMode(ply))
			{
				tripleUsers.add(ply);
				
			}
			
		}
		else
		{
			// No need to check if the player has permission before removing it
			tripleUsers.remove(ply);
			
		}
		
	}
	
	/**
	 * If the player can have triple mode.
	 * 
	 * @param ply
	 * @param enable
	 */
	public boolean canPlayerTripleMode(Player ply)
	{
		return ply.hasPermission("manualgrassspread.triple");
		
	}
	
	/**
	 * If the player has triple mode enabled.
	 * 
	 * @param ply
	 * @return
	 */
	public boolean isPlayerTripleModeEnabled(Player ply)
	{
		return canPlayerTripleMode(ply) && tripleUsers.contains(ply);
		
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void playerInteractEvent(PlayerInteractEvent event)
	{
		// Looking for the action trigger.
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;
		
		Player ply = event.getPlayer();
		
		// Check for trigger item
		if (ply.getItemInHand() == null)
			return;
		
		if (ply.getItemInHand().getTypeId() != itemID)
			return;
		
		// Check for permission.
		if (!ply.hasPermission("manualgrassspread.simple"))
			return;
		
		// Check for block.
		if (event.getClickedBlock() == null)
			return;
		
		GrassSpread spreader;
		
		// If the user can use Triple mode and has it enabled
		// Do the double check just in case an user loses permission while it's being used.
		if (isPlayerTripleModeEnabled(ply))
		{
			spreader = tripleSpreader;
			
		}
		// If the user is using the simple mode
		else
		{
			spreader = simpleSpreader;
			
		}
		spreader.spreadFromLocation(event.getClickedBlock().getLocation(), ply);
		
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void playerInteractEvent(PlayerJoinEvent event)
	{
		setPlayerTripleModeEnabled(event.getPlayer(), false);
		
	}
	
}
