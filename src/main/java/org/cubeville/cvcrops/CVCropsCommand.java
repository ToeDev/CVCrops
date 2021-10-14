package org.cubeville.cvcrops;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

public class CVCropsCommand implements CommandExecutor {
	
	private CVCrops plugin;
	
	public CVCropsCommand(CVCrops plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(args.length < 1 || args.length > 3) {
			sender.sendMessage(ChatColor.GOLD + "Command syntax is: " + ChatColor.AQUA + "/cvcrops " + ChatColor.GOLD + "<" + ChatColor.AQUA + "list" + ChatColor.GOLD + "|" + ChatColor.AQUA + "add" + ChatColor.GOLD + "|" + ChatColor.AQUA + "remove" + ChatColor.GOLD + "> " + ChatColor.GOLD + "[" + ChatColor.AQUA + "world" + ChatColor.GOLD + "] " + ChatColor.GOLD + "<" + ChatColor.AQUA + "region" + ChatColor.GOLD + ">");
			return true;
		}
		if(args[0].equalsIgnoreCase("list")) {
			if(args.length > 2) {
				sender.sendMessage(ChatColor.GOLD + "Command syntax is: " + ChatColor.AQUA + "/cvcrops list" + ((sender instanceof Player) ? "" : " <world>"));
				return true;
			}
			if(!(sender instanceof Player) && args.length == 1) {
				sender.sendMessage("Specify a worldname with /cvcrops list <world>");
				return true;
			}
			World world;
			if(args.length == 2) {
				world = Bukkit.getWorld(args[1]);
				if(world == null) {
					sender.sendMessage(ChatColor.GOLD + "Command syntax is: " + ChatColor.AQUA + "/cvcrops add" + ((sender instanceof Player) ? "" : " <world>") + ChatColor.GOLD + " <" + ChatColor.AQUA + "region" + ChatColor.GOLD + ">");
					return true;
				}
			} else {
				world = ((Player) sender).getWorld();
			}
			List<String> regions = plugin.getRegions(world.getName().toLowerCase());
			if(regions == null) {
				sender.sendMessage(ChatColor.DARK_RED + "The world name " + ChatColor.RED + world.getName() + ChatColor.DARK_RED + " doesn't exist in the cvcrops config!");
				return true;
			}
			sender.sendMessage(ChatColor.GOLD + "List of CVCrops Regions:");
			for(String region : regions) {
				sender.sendMessage(ChatColor.AQUA + "- " + region);
			}
			return true;
		}
		if(args[0].equalsIgnoreCase("add")) {
			if(args.length > 3 || args.length < 2) {
				sender.sendMessage(ChatColor.GOLD + "Command syntax is: " + ChatColor.AQUA + "/cvcrops add" + ((sender instanceof Player) ? "" : " <world>") + ChatColor.GOLD + " <" + ChatColor.AQUA + "region" + ChatColor.GOLD + ">");
				return true;
			}
			if(!(sender instanceof Player) && args.length == 2) {
				sender.sendMessage("Specify a world and region name with /cvcrops add <world> <region> when entering from console!");
				return true;
			}
			if(args.length == 3) {
				World world = Bukkit.getWorld(args[1]);
				if(world == null) {
					sender.sendMessage(ChatColor.DARK_RED + "The world name " + ChatColor.RED + args[1] + ChatColor.DARK_RED + " doesn't exist at all, anywhere, ever.");
					return true;
				}
				plugin.addRegion(sender, world.getName().toLowerCase(), args[2].toLowerCase());
			} else {
				plugin.addRegion(sender, ((Player) sender).getWorld().getName().toLowerCase(), args[1].toLowerCase());
			}
		}
		if(args[0].equalsIgnoreCase("remove")) {
			if(args.length > 3 || args.length < 2) {
				sender.sendMessage(ChatColor.GOLD + "Command syntax is: " + ChatColor.AQUA + "/cvcrops remove" + ((sender instanceof Player) ? "" : " <world>") + ChatColor.GOLD + " <" + ChatColor.AQUA + "region" + ChatColor.GOLD + ">");
				return true;
			}
			if(!(sender instanceof Player) && args.length == 2) {
				sender.sendMessage("Specify a world and region name with /cvcrops remove <world> <region> when entering from console!");
				return true;
			}
			if(args.length == 3) {
				World world = Bukkit.getWorld(args[1]);
				if(world == null) {
					sender.sendMessage(ChatColor.DARK_RED + "The world name " + ChatColor.RED + args[1] + ChatColor.DARK_RED + " doesn't exist at all, anywhere, ever.");
					return true;
				}
				plugin.removeRegion(sender, world.getName().toLowerCase(), args[2].toLowerCase());
			} else {
				plugin.removeRegion(sender, ((Player) sender).getWorld().getName().toLowerCase(), args[1].toLowerCase());
			}
		}	
		return true;
	}
}
