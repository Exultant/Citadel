package com.untamedears.citadel.command.commands;

import static com.untamedears.citadel.Utility.getSecurityLevel;
import static com.untamedears.citadel.Utility.sendMessage;
import static com.untamedears.citadel.Utility.setMultiMode;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.untamedears.citadel.Citadel;
import com.untamedears.citadel.GroupManager;
import com.untamedears.citadel.PlacementMode;
import com.untamedears.citadel.SecurityLevel;
import com.untamedears.citadel.command.PlayerCommand;
import com.untamedears.citadel.entity.Faction;
import com.untamedears.citadel.entity.PlayerState;
import com.untamedears.citadel.entity.ReinforcementMaterial;

/**
 * User: JonnyD
 * Date: 7/18/12
 * Time: 11:57 PM
 */
public class FortifyCommand extends PlayerCommand {

	public FortifyCommand() {
		super("Fority Mode");
		setDescription("Toggle fortification mode");
		setUsage("/ctfortify §8[security-level]");
		setArgumentRange(0, 2);
		setIdentifiers(new String[] {"ctfortify", "ctf"});
	}

	public boolean execute(CommandSender sender, String[] args) {
		Player player = (Player) sender;
		PlayerState state = PlayerState.get(player);
		
		String secLevel = null;
		String groupName = null;
		if(args.length != 0){
			secLevel = args[0];
			if(args.length == 2){
				groupName = args[1];
			}
		}
		if(secLevel != null && secLevel.equalsIgnoreCase("group")){
			if(groupName == null || groupName.isEmpty() || groupName.equals("")){
				sender.sendMessage(new StringBuilder().append("§cYou must specify a group in group fortification mode").toString());
				sender.sendMessage(new StringBuilder().append("§cUsage:§e ").append("/ctfortify §8group <group-name>").toString());
				return true;
			}
			GroupManager groupManager = Citadel.getGroupManager();
			Faction group = groupManager.getGroup(groupName);
			if(group == null){
				sendMessage(sender, ChatColor.RED, "Group doesn't exist");
				return true;
			}
			String senderName = sender.getName();
			if(!group.isFounder(senderName) && !group.isModerator(senderName)){
				sendMessage(sender, ChatColor.RED, "Invalid permission to use this group");
				return true;
			}
			if(group.isPersonalGroup()){
				sendMessage(sender, ChatColor.RED, "You cannot share your default group");
				return true;
			}
			state.setFaction(group);
		} else {
			state.setFaction(Citadel.getMemberManager().getMember(player).getPersonalGroup());
		}
		
		SecurityLevel securityLevel = getSecurityLevel(args, player);
        if (securityLevel == null) return false;

        ReinforcementMaterial material = ReinforcementMaterial.get(player.getItemInHand().getType());
        if (state.getMode() == PlacementMode.FORTIFICATION) {
            // Only change material if a valid reinforcement material in hand and not current reinforcement
            if (material != null && material != state.getReinforcementMaterial()) {
                // Switch reinforcement materials without turning off and on again
                state.reset();
                state.setFortificationMaterial(material);
            }
            setMultiMode(PlacementMode.FORTIFICATION, securityLevel, args, player, state);
        } else {
            if (material == null) {
                sendMessage(sender, ChatColor.YELLOW, "Invalid reinforcement material %s", player.getItemInHand().getType().name());
            } else {
                state.setFortificationMaterial(material);
                setMultiMode(PlacementMode.FORTIFICATION, securityLevel, args, player, state);
            }
        }
        
        return true;
	}

}
