package com.untamedears.citadel.command.commands;

import static com.untamedears.citadel.Utility.sendMessage;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.untamedears.citadel.Citadel;
import com.untamedears.citadel.GroupManager;
import com.untamedears.citadel.command.PlayerCommand;
import com.untamedears.citadel.entity.Faction;
import com.untamedears.citadel.entity.FactionMember;

/**
 * Created by IntelliJ IDEA.
 * User: chrisrico
 * Date: 3/21/12
 * Time: 11:35 AM
 * 
 * Last modified by JonnyD
 * 7/18/12
 */
public class MembersCommand extends PlayerCommand {

	public MembersCommand() {
		super("List Members");
		setDescription("List the members of a group");
		setUsage("/ctmembers §8<group-name>");
		setArgumentRange(1,1);
		setIdentifiers(new String[] {"ctmembers", "ctm"});
	}

	public boolean execute(CommandSender sender, String[] args) {
		GroupManager groupManager = Citadel.getGroupManager();
		String groupName = args[0];
		Faction group = groupManager.getGroup(groupName);
		if(group == null){
			sendMessage(sender, ChatColor.RED, "Group doesn't exist");
			return true;
		}
		String senderName = sender.getName();
		if(!group.isFounder(senderName) && !group.isModerator(senderName)){
			sendMessage(sender, ChatColor.RED, "Invalid permission to access this group");
			return true;
		}
		List<FactionMember> members = new ArrayList<FactionMember>(groupManager.getMembersOfGroup(groupName));
		if(members.isEmpty()){
			sendMessage(sender, ChatColor.RED, "There are no members in this group");
			return true;
		}
		int page = 0;
		if(args.length != 0){
			try {
				page = Integer.parseInt(args[0]) - 1;
			} catch (NumberFormatException ignored){
				
			}
		}
		int numPages = members.size() / 8;
		if(members.size() % 8 != 0){
			numPages++;
		}
		if(numPages == 0){
			numPages = 1;
		}
		if((page >= numPages) || (page < 0)){
			page = 0;
		}
		sendMessage(sender, ChatColor.AQUA, "-----[ Members of %s <" + (page + 1) + "/" + numPages + "> ]-----", groupName);
		int start = page * 8;
		int end = start + 8;
		if(end > members.size()){
			end = members.size();
		}
		for(int m = start; m < end; m++){
			FactionMember member = members.get(m);
			String line = member.getMemberName();
			sendMessage(sender, ChatColor.WHITE, line);
		}
		if(page + 1 < numPages){
			sendMessage(sender, ChatColor.GRAY, "For more type \"/ctmoderators <group-name> [n]\"");
		}
        return true;
	}
   
}
