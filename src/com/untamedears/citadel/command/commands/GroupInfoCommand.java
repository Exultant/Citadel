package com.untamedears.citadel.command.commands;

import static com.untamedears.citadel.Utility.sendMessage;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.untamedears.citadel.Citadel;
import com.untamedears.citadel.GroupManager;
import com.untamedears.citadel.command.PlayerCommand;
import com.untamedears.citadel.entity.Faction;

public class GroupInfoCommand extends PlayerCommand {

	public GroupInfoCommand() {
		super("Group Information");
		setDescription("Displays information about a group");
		setUsage("/ctgroupinfo <group-name>");
		setArgumentRange(1,1);
		setIdentifiers(new String[] {"ctgroupinfo", "ctgi"});
	}

	public boolean execute(CommandSender sender, String[] args) {
		String groupName = args[0];
		GroupManager groupManager = Citadel.getGroupManager();
		Faction group = groupManager.getGroup(groupName);
		if(group == null){
			sendMessage(sender, ChatColor.RED, "Group doesn't exist");
			return true;
		}
		String senderName = sender.getName();
		if(!group.isFounder(senderName) && !group.isModerator(senderName) && !group.isMember(senderName)){
			sendMessage(sender, ChatColor.RED, "Invalid permission to access this group");
			return true;
		}
		sender.sendMessage("Group Name: " + groupName);
		sender.sendMessage("Owner: " + group.getFounder());
		sender.sendMessage("Moderators: " + groupManager.getModeratorsOfGroup(groupName).size());
		sender.sendMessage("Members: " + groupManager.getMembersOfGroup(groupName).size());
		if(group.isFounder(senderName) || group.isModerator(senderName)){
			String password = group.getPassword();
			sender.sendMessage("Password: " + password);
			String joinable;
			if(password != null && !password.equalsIgnoreCase("null")){
				joinable = "Yes";
			} else {
				joinable = "No";
			}
			sender.sendMessage("Joinable: " + joinable);
		}
		return true;
	}

}
