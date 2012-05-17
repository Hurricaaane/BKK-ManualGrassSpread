package eu.ha3.bukkit.manualgrass;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GrassTripleExecutor implements CommandExecutor
{
	private Plugin plugin;
	private Set<Player> who;
	
	public GrassTripleExecutor(Plugin plugin)
	{
		this.plugin = plugin;
		this.who = new HashSet<Player>();
		
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String str,
			String[] args)
	{
		Player ply = plugin.getServer().getPlayer(sender.getName());
		
		if (ply == null)
		{
			return false;
			
		}
		
		if (plugin.canPlayerTripleMode(ply))
		{
			if (!plugin.isPlayerTripleModeEnabled(ply))
			{
				plugin.setPlayerTripleModeEnabled(ply, true);
				if (!who.contains(ply))
				{
					who.add(ply);
					ply.sendMessage("The grass spread radius is now tripled. "
							+ "Make sure no legitimate dirt is nearby when using this tool, "
							+ "as the radius can reach places you may not see.");
					
				}
				else
				{
					ply.sendMessage("Triple mode enabled.");
					
				}
				
			}
			
			else
			{
				plugin.setPlayerTripleModeEnabled(ply, false);
				ply.sendMessage("Triple mode disabled.");
				
			}
			
			return true;
			
		}
		else
		{
			return false;
			
		}
		
	}
	
}
