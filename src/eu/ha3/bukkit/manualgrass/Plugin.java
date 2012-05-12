package eu.ha3.bukkit.manualgrass;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Plugin extends JavaPlugin implements Listener
{
	private int itemID;
	private int radius;
	private int radius_squared;
	private int height_span;
	private int diameter;
	
	private boolean[][] stencil;
	private boolean[][][] grass;
	private boolean[][][] dirt;
	private boolean[][][] apply;
	
	private boolean canBeUsed;
	
	@Override
	public void onEnable()
	{
		canBeUsed = false;
		updateFromConfig();
		
		getServer().getPluginManager().registerEvents(this, this);
		
	}
	
	private void updateFromConfig()
	{
		canBeUsed = false;
		
		itemID = this.getConfig().getInt("grass_spread.item");
		radius = this.getConfig().getInt("grass_spread.radius");
		height_span = this.getConfig().getInt("grass_spread.height_of_split");
		
		canBeUsed = ((radius >= 0) && (height_span >= 0));
		
		if (canBeUsed)
		{
			diameter = radius * 2;
			radius_squared = radius * radius;
			
			/**
			 * The stencil is [x][y].<br>
			 * The tables are is [x][y][z]. Not like Minecraft. Remeber this.
			 * 
			 */
			
			/**
			 * The diameter and height splice will be +1 longer to ensure odd
			 * values in calculations.
			 * 
			 */
			stencil = new boolean[diameter + 1][diameter + 1];
			grass = new boolean[diameter + 1][diameter + 1][1 + height_span * 2];
			dirt = new boolean[diameter + 1][diameter + 1][1 + height_span * 2];
			apply = new boolean[diameter + 1][diameter + 1][1 + height_span * 2];
			
			/**
			 * Create a 2D stencil, which will be used as a cylinder with an
			 * axis on the Minecraft-Y axis, which is the axis leading to the
			 * sky.
			 * 
			 */
			for (int i = -radius; i <= radius; i++)
			{
				for (int j = -radius; j <= radius; j++)
				{
					if ((i * i + j * j) <= radius_squared)
					{
						stencil[i + radius][j + radius] = true;
						
					}
					
				}
				
			}
			
		}
		else
		{
			this.getLogger()
					.severe("Plugin config file has invalid values (This may be due to negative values).");
			
		}
		
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void playerInteractEvent(PlayerInteractEvent event)
	{
		// Check if the config values ware valid.
		if (!canBeUsed)
			return;
		
		// Looking for the action trigger.
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;
		
		Player ply = event.getPlayer();
		
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
		
		// Start computing now.
		World world = ply.getWorld();
		int xs = event.getClickedBlock().getX();
		int ys = event.getClickedBlock().getY();
		int zs = event.getClickedBlock().getZ();
		
		boolean hasGrass = false;
		boolean hasDirt = false;
		
		/**
		 * Find grass and dirt in the vicinity.
		 * 
		 * We create two stencils: The dirt matrix made of the dirt which have a
		 * top surface exposed to skylight. The grass matrix made of the grass
		 * which have a top surface exposed to skylight.
		 * 
		 */
		for (int i = 0; i <= diameter; i++)
		{
			for (int j = 0; j <= diameter; j++)
			{
				/**
				 * Only take in account blocks within the stencil. We don't care
				 * if there are grass blocks outside the stencil that could
				 * spread towards the dirt in the stencil, even if we should. It
				 * simplifies the calculation process.
				 * 
				 */
				if (stencil[i][j])
				{
					for (int hhh = -height_span; hhh <= height_span; hhh++)
					{
						int x = xs + i - radius;
						int y = ys + hhh;
						int z = zs + j - radius;
						
						/**
						 * In order to only paint the top layer of dirt with
						 * grass, and only paing the dirt with valid grass, we
						 * filter the top layer.<br>
						 * We only take in account blocks (dirt and grass) that
						 * receive light from the sky. The reason why we do this
						 * is to perform a very very simple check on whether the
						 * block has something translucent above it. Grass can
						 * spread in the shadow of other objects as long as
						 * there is sky light.<br>
						 * <br>
						 * This also includes blocks that are underwater. The
						 * code below has been commented out because it's easier
						 * to paint water springs this way. Could eventually
						 * make a console command for that.
						 */
						Block block = world.getBlockAt(x, y, z);
						//Block blockAbove = block.getRelative(BlockFace.UP);
						if ((block.getRelative(BlockFace.UP).getLightFromSky() > 0) /*&& !block.getRelative(BlockFace.UP).isLiquid()*/)
						{
							if (block.getType() == Material.GRASS)
							{
								if (!hasGrass)
									hasGrass = true;
								
								grass[i][j][hhh + height_span] = true;
								
							}
							else
							{
								grass[i][j][hhh + height_span] = false;
								
							}
							
							if (block.getType() == Material.DIRT)
							{
								if (!hasDirt)
									hasDirt = true;
								
								dirt[i][j][hhh + height_span] = true;
								
							}
							else
							{
								dirt[i][j][hhh + height_span] = false;
								
							}
							
						}
						else
						{
							/**
							 * Remember to false everything else: We are using
							 * the table data to create a stencil, and we don't
							 * want previous values to stay from older
							 * computation.
							 * 
							 */
							grass[i][j][hhh + height_span] = false;
							dirt[i][j][hhh + height_span] = false;
							
						}
						
					}
					
				}
				
			}
			
		}
		
		// If there is grass and dirt, begin spread.
		if (hasGrass && hasDirt)
		{
			/**
			 * In this algorithm, we dilate the grass matrix into the apply
			 * matrix.<br>
			 * <br>
			 * Explanation: A dirt block transforms into grass if there is at
			 * least one grass block in proximity of its 26 neighbors, so we
			 * just have to blindly dilate the grass matrix, and then use the
			 * dirt matrix as a stencil.
			 */
			for (int i = 0; i <= diameter; i++)
			{
				for (int j = 0; j <= diameter; j++)
				{
					/**
					 * Only take in account blocks within the stencil.
					 */
					if (stencil[i][j])
					{
						for (int hhh = -height_span; hhh <= height_span; hhh++)
						{
							int hhh_matrix = hhh + height_span;
							
							/**
							 * Check the proximity (dilate).
							 */
							boolean shouldSpread = false;
							for (int ip = -1; !shouldSpread && (ip <= 1); ip++)
							{
								int it = i + ip;
								if (0 <= it && it <= diameter)
									for (int jp = -1; !shouldSpread
											&& (jp <= 1); jp++)
									{
										int jt = j + jp;
										if (0 <= jt && jt <= diameter)
											for (int hp = -1; !shouldSpread
													&& (hp <= 1); hp++)
											{
												int ht = hhh_matrix + hp;
												if (0 <= ht
														&& ht <= (height_span + height_span))
													shouldSpread = grass[it][jt][ht];
												
											}
									}
							}
							
							/**
							 * Push result to apply matrix.
							 */
							apply[i][j][hhh_matrix] = shouldSpread;
							
						}
						
					}
					
				}
				
			}
			
			// Actually spread it in the world
			for (int i = 0; i <= diameter; i++)
			{
				for (int j = 0; j <= diameter; j++)
				{
					/**
					 * Only take in account blocks within the stencil. Note that
					 * this is a but redundant since the code below will never
					 * be true if we're out of the stencil. It just saves some
					 * very very little time.
					 */
					if (stencil[i][j])
					{
						for (int hhh = -height_span; hhh <= height_span; hhh++)
						{
							int x = xs + i - radius;
							int y = ys + hhh;
							int z = zs + j - radius;
							
							/**
							 * We check if there is dirt in that position, and
							 * check if we have to apply the change using the
							 * apply matrix. The apply matrix is an dilate
							 * version of the grass matrix, so we use the dirt
							 * matrix as a stencil.
							 * 
							 */
							if (dirt[i][j][hhh + height_span]
									&& apply[i][j][hhh + height_span])
							{
								boolean couldChange = changeToGrass(ply, world,
										x, y, z);
								
								if (!couldChange)
									return; // ABORT EVERYTHING
									
							}
							
						}
						
					}
					
				}
				
			}
			
		}
		
	}
	
	private boolean changeToGrass(Player ply, World world, int x, int y, int z)
	{
		/*
		 *
		00078         CraftWorld craftWorld = ((WorldServer) world).getWorld();
		00079         CraftServer craftServer = ((WorldServer) world).getServer();
		00080 
		00081         Player player = (who == null) ? null : (Player) who.getBukkitEntity();
		00082         CraftItemStack itemInHand = new CraftItemStack(itemstack);
		00083 
		00084         Block blockClicked = craftWorld.getBlockAt(clickedX, clickedY, clickedZ);
		00085         Block placedBlock = replacedBlockState.getBlock();
		00086 
		00087         boolean canBuild = canBuild(craftWorld, player, placedBlock.getX(), placedBlock.getZ());
		00088 
		00089         BlockPlaceEvent event = new BlockPlaceEvent(placedBlock, replacedBlockState, blockClicked, itemInHand, player, canBuild);
		00090         craftServer.getPluginManager().callEvent(event);
		00091 
		00092         return event;
		*/
		
		//http://www.pastie.org/2715934/wrap
		
		/*
		g`"index 5662274..8f64307 100644
		--- "a/C:\\Users\\HellFire\\AppData\\Local\\Temp\\Spo207E.java"
		+++ "b/C:\\Users\\HellFire\\Workspace\\Git\\Spout\\src\\org\\getspout\\spout\\SpoutPlayerListener.java"
		@@ -22,8 +22,10 @@ import org.bukkit.Bukkit;
		import org.bukkit.Location;
		import org.bukkit.Material;
		import org.bukkit.block.Block;
		+import org.bukkit.block.BlockState;
		import org.bukkit.entity.Player;
		import org.bukkit.event.block.Action;
		+import org.bukkit.event.block.BlockPlaceEvent;
		import org.bukkit.event.player.PlayerEvent;
		import org.bukkit.event.player.PlayerInteractEvent;
		import org.bukkit.event.player.PlayerJoinEvent;
		@@ -144,13 +146,32 @@ public class SpoutPlayerListener extends PlayerListener{
					if (newBlockId != 0 ) {
						Block block = event.getClickedBlock().getRelative(event.getBlockFace());
						CustomBlock cb = MaterialData.getCustomBlock(damage);
		+						BlockState oldState = block.getState();
						block.setTypeIdAndData(cb.getBlockId(), (byte)(newMetaData & 0xF), true);
		-						mm.overrideBlock(block, cb);
		+						// TODO: canBuild should be set properly, CraftEventFactory.canBuild() would do this... 
		+						//       but it's private so... here it is >.>
		+						int spawnRadius = Bukkit.getServer().getSpawnRadius();
		+						boolean canBuild = false;
		+						if (spawnRadius <= 0 || player.isOp()) { // Fast checks
		+							canBuild = true;
		+						} else if (Math.max(block.getX(), block.getZ()) > spawnRadius) { // Slower check
		+							canBuild = true;
		+						}
		+						
		+						BlockPlaceEvent placeEvent = new BlockPlaceEvent(block, oldState, event.getClickedBlock(), item, player, canBuild);
						
		-						if(item.getAmount() == 1) {
		-							event.getPlayer().setItemInHand(null);
		+						if (!placeEvent.isCancelled() && placeEvent.canBuild()) {
		+							// Yay, do the override work
		+							mm.overrideBlock(block, cb);
		+							
		+							if(item.getAmount() == 1) {
		+								event.getPlayer().setItemInHand(null);
		+							} else {
		+								item.setAmount(item.getAmount() - 1);
		+							}
						} else {
		-							item.setAmount(item.getAmount() - 1);
		+							// Event cancelled or can't build
		+							block.setTypeIdAndData(oldState.getTypeId(), oldState.getRawData(), true);
						}
					}
				}
		 */
		
		Block block = world.getBlockAt(x, y, z);
		
		// Record the old state, then set the new type on the block.
		// TODO: Is oldState persistent when setType is called on block?
		BlockState oldState = block.getState();
		block.setType(Material.GRASS);
		
		// Send a place event.
		BlockPlaceEvent placeEvent = new BlockPlaceEvent(block, oldState, block
				.getRelative(BlockFace.UP), ply.getItemInHand(), ply, true);
		this.getServer().getPluginManager().callEvent(placeEvent);
		
		// If the event is cancelled, revert the change.
		if (placeEvent.isCancelled() || !placeEvent.canBuild())
		{
			block.setTypeIdAndData(oldState.getTypeId(), oldState.getRawData(),
					true);
			return false;
			
		}
		
		return true;
		
	}
	
}
