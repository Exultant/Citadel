package com.untamedears.citadel.command.commands;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import com.untamedears.citadel.Citadel;
import com.untamedears.citadel.GroupManager;
import com.untamedears.citadel.command.CommandUtils;
import com.untamedears.citadel.command.PlayerCommand;
import com.untamedears.citadel.entity.Faction;
import com.untamedears.citadel.entity.Member;

public class PlayerStatsCommand extends PlayerCommand {

    public class SendResultsTask implements Runnable {
        public QueryDbTask previousTask;
        public SendResultsTask(QueryDbTask pt) {
            previousTask = pt;
        }
        @Override
        public void run() {
            for (String line : previousTask.results) {
                previousTask.sender.sendMessage(line);
            }
        }
    }

    public class QueryDbTask implements Runnable {
        public CommandSender sender;
        public String playerName;
        public List<String> results = new LinkedList<String>();
        public QueryDbTask(CommandSender s, String pn) {
            sender = s;
            playerName = pn;
        }
        @Override
        public void run() {
            GroupManager groupManager = Citadel.getGroupManager();
            Set<Faction> memberGroups = groupManager.getGroupsByMember(playerName);
            Set<Faction> moderatorGroups = groupManager.getGroupsByModerator(playerName);
            Set<Faction> founderGroups = groupManager.getGroupsByFounder(playerName);
            results.add("Player name: "+playerName);
            if (founderGroups.size() > 0)
                results.add("Admin of groups: "+CommandUtils.joinFactionSet(founderGroups));
            if (moderatorGroups.size() > 0)
                results.add("Moderator of groups: "+CommandUtils.joinFactionSet(moderatorGroups));
            if (memberGroups.size() > 0)
                results.add("Member of groups: "+CommandUtils.joinFactionSet(memberGroups));

            Faction group = null;
            Member member = Citadel.getMemberManager().getMember(playerName);
            if (member != null) {
                group = member.getPersonalGroup();
            }
            if (group != null) {
                String personalGroupName = group.getName();
                results.add("Personal group reinforcements: ");
                CommandUtils.formatReinforcements(results, playerName, CommandUtils.countReinforcements(personalGroupName));
            } else {
                results.add("Player has no personal group.");
            }
            Bukkit.getScheduler().runTask(
                Citadel.getPlugin(), new SendResultsTask(this));
        }
    }

    public PlayerStatsCommand() {
        super("View Player Stats");
        setDescription("View citadel player stats");
        setUsage("/ctpstats <player-name>");
        setArgumentRange(1, 1);
        setIdentifiers(new String[] {"ctpstats", "ctpst"});
    }

    public boolean execute(CommandSender sender, String[] args) {
        Bukkit.getScheduler().runTaskAsynchronously(
            Citadel.getPlugin(), new QueryDbTask(sender, args[0]));
        return true;
    }

}
