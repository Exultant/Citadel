package com.untamedears.citadel.command.commands;

import static com.untamedears.citadel.Utility.sendMessage;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.untamedears.citadel.Citadel;
import com.untamedears.citadel.GroupManager;
import com.untamedears.citadel.PersonalGroupManager;
import com.untamedears.citadel.ReinforcementManager;
import com.untamedears.citadel.command.PlayerCommand;
import com.untamedears.citadel.entity.Faction;

/**
 * User: JonnyD
 * Date: 7/18/12
 * Time: 11:57 PM
 */
public class DeleteCommand extends PlayerCommand {

	public DeleteCommand() {
		super("Delete group");
		setDescription("Deletes a group");
		setUsage("/ctdelete �8<group-name>");
		setArgumentRange(1,1);
		setIdentifiers(new String[] {"ctdelete", "ctdel"});
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
		if(!group.isFounder(senderName)){
			sendMessage(sender, ChatColor.RED, "Invalid permission to delete this group");
			return true;
		}
		if(group.isPersonalGroup()){
			sendMessage(sender, ChatColor.RED, "You cannot delete your default group");
			return true;
		}
		
		PersonalGroupManager personalGroupManager = Citadel.getPersonalGroupManager();
		String personalGroup = personalGroupManager.getPersonalGroup(senderName).getGroupName();
		
		ReinforcementManager reinforcementManager = Citadel.getReinforcementManager();
		reinforcementManager.moveReinforcements(groupName, personalGroup);
		
		groupManager.removeAllMembersFromGroup(groupName);
		groupManager.removeAllModeratorsFromGroup(groupName);
		groupManager.removeGroup(group);
		
		sendMessage(sender, ChatColor.GREEN, "Deleted group: %s", groupName);
		return true;
	}

}
