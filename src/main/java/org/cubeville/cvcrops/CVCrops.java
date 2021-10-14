package org.cubeville.cvcrops;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CVCrops extends JavaPlugin implements Listener {
	
	private Logger logger;
	
	private Map<Location, Integer> replantTasks;
	private BukkitScheduler scheduler;
	private Map<String, List<String>> cropRegions;
	private File regionsDir;
	private int growInterval;
	private int growChance;
	
	@Override
	public void onEnable() {
		
		this.logger = getLogger();
		this.scheduler = this.getServer().getScheduler();
		
		final File dataDir = getDataFolder();
		if(!dataDir.exists()) {
			dataDir.mkdirs();
		}
		regionsDir = new File(dataDir, "regions");
		if(!regionsDir.exists()) {
			regionsDir.mkdirs();
		}
		File configFile = new File(dataDir, "config.yml");
		if(!configFile.exists()) {
			try {
				configFile.createNewFile();
				final InputStream defaultConfig = this.getResource(configFile.getName());
				final FileOutputStream outputStream = new FileOutputStream(configFile);
				final byte[] buffer = new byte[4096];
				int bytesRead;			
				while ((bytesRead = defaultConfig.read(buffer)) != -1) {
					outputStream.write(buffer, 0, bytesRead);
				}			
				outputStream.flush();
				outputStream.close();
			}
			catch(IOException e) {
				logger.log(Level.WARNING, "Unable to generate config file", e);
				throw new RuntimeException("Unable to generate config file", e);
			}	
		}
		
		logger.info("Reading CVCrops regions from each world file");
		this.replantTasks = new HashMap<>();
		this.cropRegions = new HashMap<>();
		for(File regionFile : regionsDir.listFiles()) {
			YamlConfiguration regionConfig = new YamlConfiguration();
			try {
				regionConfig.load(regionFile);
			}
			catch(IOException | InvalidConfigurationException e) {
				logger.log(Level.WARNING, "Unable to load region file " + regionFile.getName(), e);
				continue;
			}
			cropRegions.put(regionFile.getName().substring(0, regionFile.getName().length() - 4).toLowerCase(), regionConfig.getStringList("regions"));
		}
		
		logger.info("Reading CVCrops Config File");
		YamlConfiguration mainConfig = new YamlConfiguration();
		try {
			mainConfig.load(configFile);
			growInterval = mainConfig.getInt("cropgrowinterval", 40);
			growChance = mainConfig.getInt("cropgrowchance", 100);
		}
		catch(IOException | InvalidConfigurationException e) {
			logger.log(Level.WARNING, "Unable to load config file", e);
			growInterval = 40;
			growChance = 100;
		}
		
		this.getCommand("cvcrops").setExecutor(new CVCropsCommand(this));
		Bukkit.getPluginManager().registerEvents(this, this);		
	}
	
	@EventHandler
	public void onCropBreak(final BlockBreakEvent event) {
		if(!(event.getBlock().getBlockData() instanceof Ageable)) {
			return;
		}
		final Block block = event.getBlock();
		final Material material = block.getType();
		final org.bukkit.Location loc = block.getLocation();
		final String world = loc.getWorld().getName();
		if(material.equals(Material.WHEAT) || material.equals(Material.CARROTS) || material.equals(Material.POTATOES) || material.equals(Material.BEETROOTS)) {
			RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
			RegionQuery query = container.createQuery();
			ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(loc));
			for(ProtectedRegion region : set) {
				if(cropRegions.get(world.toLowerCase()).contains(region.getId().toLowerCase())) {
					if(replantTasks.containsKey(block.getLocation())) {
						scheduler.cancelTask(replantTasks.get(block.getLocation()));
					} 
					scheduler.runTaskLater(this, () -> replantCrop(block, material), growInterval);
				}
			}
		}
	}
	
	public void replantCrop(Block block, Material material) {		
		block.setType(material);		
		replantTasks.put(block.getLocation(), scheduler.runTaskTimer(this, () -> {		
			growCrop(block, material);	
		}, growInterval, growInterval).getTaskId());
	}
		
	public void growCrop(Block block, Material material) {		
		Ageable ageable = (Ageable) block.getBlockData();
		if(ageable.getAge() >= ageable.getMaximumAge()) {
			scheduler.cancelTask(replantTasks.get(block.getLocation()));
			return;
		}
		if((new Random()).nextInt(100) < growChance) {
			ageable.setAge(ageable.getAge() + 1);
			block.setBlockData(ageable);
		}
	}

	public List<String> getRegions(String world) {
		return cropRegions.get(world.toLowerCase());
	}
	
	public void addRegion(CommandSender sender, String world, String region) {
		List<String> regions = this.getRegions(world);
		File regionFile = new File(regionsDir, world.toLowerCase() + ".yml");
		if (regions == null) {
			regions = new ArrayList<String>();
		} else if (regions.contains(region.toLowerCase())) {
			sender.sendMessage(ChatColor.GOLD + "The region " + ChatColor.AQUA + region + ChatColor.GOLD + " already exists in the CVCrops region list for this world.");
			return;
		}
		if (!regionFile.exists()) {
			try {
				regionFile.createNewFile();
			}
			catch (IOException e) {
				logger.log(Level.WARNING, "Unable to create region file " + regionFile.getName(), e);
				sender.sendMessage("Error adding region");
				return;
			}
		}
		regions.add(region.toLowerCase());
		cropRegions.put(world.toLowerCase(), regions);
		YamlConfiguration regionConfig = new YamlConfiguration();
		regionConfig.set("regions", regions);
		try {
			regionConfig.save(regionFile);
		}
		catch (IOException e) {
			logger.log(Level.WARNING, "Unable to save region file " + regionFile.getName(), e);
			sender.sendMessage("Error saving region");
			return;
		}
		sender.sendMessage(ChatColor.GREEN + "The region " + ChatColor.AQUA + region + ChatColor.GREEN + " has been added to the CVCrops region list.");
	}
	
	public void removeRegion(CommandSender sender, String world, String region) {
		List<String> regions = this.getRegions(world);
		File regionFile = new File(regionsDir, world.toLowerCase() + ".yml");
		if (regions == null || !regions.contains(region.toLowerCase())) {
			sender.sendMessage(ChatColor.GOLD + "The region " + ChatColor.AQUA + region + ChatColor.GOLD + " doesn't exist in the CVCrops region list for this world.");
			return;
		}
		regions.remove(region.toLowerCase());
		cropRegions.put(world.toLowerCase(), regions);
		if (!regionFile.exists()) {
			try {
				regionFile.createNewFile();
			}
			catch (IOException e) {
				logger.log(Level.WARNING, "Unable to remove region file " + regionFile.getName(), e);
				sender.sendMessage("Error removing region");
				return;
			}
		}
		YamlConfiguration regionConfig = new YamlConfiguration();
		regionConfig.set("regions", regions);
		try {
			regionConfig.save(regionFile);
		}
		catch (IOException e) {
			logger.log(Level.WARNING, "Unable to save region file " + regionFile.getName(), e);
			sender.sendMessage("Error saving region");
			return;
		}
		sender.sendMessage(ChatColor.RED + "The region " + ChatColor.AQUA + region + ChatColor.RED + " has been removed from the CVCrops region list.");
	}
	
	@Override
	public void onDisable() {
		logger.info("CVCrops Disabled Successfully");
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
