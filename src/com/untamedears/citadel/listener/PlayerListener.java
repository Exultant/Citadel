package com.untamedears.citadel.listener;

import com.untamedears.citadel.Citadel;
import com.untamedears.citadel.PlacementMode;
import com.untamedears.citadel.entity.PlayerState;
import com.untamedears.citadel.entity.ReinforcementMaterial;
import com.untamedears.citadel.access.AccessDelegate;
import com.untamedears.citadel.entity.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.material.Openable;

import static com.untamedears.citadel.Utility.createReinforcement;
import static com.untamedears.citadel.Utility.sendMessage;

/**
 * Created by IntelliJ IDEA.
 * User: chrisrico
 * Date: 3/21/12
 * Time: 9:57 PM
 */
public class PlayerListener implements Listener {
    
    private Citadel plugin;

    public PlayerListener(Citadel plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void login(PlayerLoginEvent ple) {
        String name = ple.getPlayer().getDisplayName();
        Faction faction = plugin.dao.findGroupByName(name);
        if (faction == null) {
            plugin.logVerbose("Created personal faction for player %s", name);

            faction = new Faction(name, name);
            plugin.dao.save(faction);
        }
        for (FactionMember member : plugin.dao.findGroupMembers(name)) {
            Player player = plugin.getServer().getPlayer(member.getMemberName());
            if (player != null)
                sendMessage(player, ChatColor.WHITE, "%s has logged in", name);
        }
    }

    @EventHandler
    public void quit(PlayerQuitEvent pqe) {
        String name = pqe.getPlayer().getDisplayName();
        for (FactionMember member : plugin.dao.findGroupMembers(name)) {
            Player player = plugin.getServer().getPlayer(member.getMemberName());
            if (player != null)
                sendMessage(player, ChatColor.GRAY, "%s has logged out", name);
        }
        PlayerState.remove(pqe.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void interact(PlayerInteractEvent pie) {
        if (pie.isCancelled() || !pie.hasBlock()) return;

        Player player = pie.getPlayer();
        Block block = pie.getClickedBlock();

        AccessDelegate accessDelegate = AccessDelegate.getDelegate(block);
        block = accessDelegate.getBlock();
        Reinforcement reinforcement = accessDelegate.getReinforcement();

        checkAccessiblity(pie, reinforcement);
        if (pie.isCancelled()) return;

        PlayerState state = PlayerState.get(player);
        maybeOpenIronDoor(state, block);

        switch (state.getMode()) {
            case NORMAL:
            case FORTIFICATION:
                return;
            case INFO:
                // did player click on a reinforced block?
                if (reinforcement != null) {
                    ChatColor color = reinforcement.isAccessible(player) ? ChatColor.GREEN : ChatColor.RED;
                    sendMessage(player, color, "%s, security: %s", reinforcement.getStatus(), reinforcement.getSecurityLevel().name());
                }
                break;
            default:
                // player is in reinforcement mode
                if (reinforcement == null) {
                    createReinforcement(player, block);
                } else if (reinforcement.isAccessible(player)) {
                    ReinforcementMaterial material = ReinforcementMaterial.get(player.getItemInHand().getType());
                    boolean accessChange = reinforcement.getSecurityLevel() != state.getSecurityLevel();
                    boolean materialChange = material != null && reinforcement.getMaterialId() != material.getMaterialId();
                    boolean repair = !materialChange && material != null && reinforcement.getHealth() < 1;
                    if (accessChange) {
                        reinforcement.setSecurityLevel(state.getSecurityLevel());
                    }
                    if (materialChange) {
                        player.getInventory().remove(material.getRequiredMaterials());
                        reinforcement.setMaterialId(material.getMaterialId());
                    } else if (repair) {
                        player.getInventory().remove(material.getRequiredMaterials());
                        reinforcement.setDurability(reinforcement.getMaterial().getStrength());
                    }
                    if (accessChange || materialChange || repair) {
                        plugin.dao.save(reinforcement);
                        String message = null;
                        if (materialChange) {
                            message = "Upgraded reinforcement to "+ material.getMaterial().name();
                        } else if (repair) {
                            message = "Repaired reinforcement";
                        }
                        if (accessChange) {
                            if (message == null) message = "Changed";
                            else message += ",";

                            message += " security level " + reinforcement.getSecurityLevel().name();
                        }
                        sendMessage(player, ChatColor.GREEN, message);
                    }
                } else {
                    sendMessage(player, ChatColor.RED, "You are not permitted to modify this reinforcement");
                }
            pie.setCancelled(true);
            if (state.getMode() == PlacementMode.REINFORCEMENT_SINGLE_BLOCK) {
                state.reset();
            }
        }
    }
    
    private void checkAccessiblity(PlayerInteractEvent pie, Reinforcement reinforcement) {
        if (reinforcement != null
                && reinforcement.isSecurable()
                && !reinforcement.isAccessible(pie.getPlayer())) {
            if (pie.getAction() == Action.LEFT_CLICK_BLOCK && reinforcement.getBlock().getState().getData() instanceof Openable) {
                // because openable objects can be opened with a left or right click
                // and we want to deny use but allow destruction, we need to limit
                // opening secured doors to right clicking
                pie.setUseInteractedBlock(Event.Result.DENY);
            } else if (pie.getAction() == Action.RIGHT_CLICK_BLOCK) {
                plugin.logVerbose("%s failed to access locked reinforcement %s", pie.getPlayer().getDisplayName(), reinforcement);
                // this block is secured and is inaccesible to the player
                sendMessage(pie.getPlayer(), ChatColor.RED, reinforcement.getStatus());
                pie.setCancelled(true);
            }
        }
    }
    
    private void maybeOpenIronDoor(PlayerState state, Block block) {
        if (block.getType() == Material.IRON_DOOR_BLOCK && (state.getMode() == PlacementMode.NORMAL || state.getMode() == PlacementMode.INFO)) {
            // TODO: this doesn't work
//            Block topBlock = block.getRelative(BlockFace.UP);
//            for (Block doorBlock : new Block[] { block, topBlock }) {
//                Door door = (Door) doorBlock.getState().getData();
//                plugin.logVerbose("Toggling %s", door);
//                door.setOpen(!door.isOpen());
//                doorBlock.getState().setData(door);
//                doorBlock.getState().update();
//            }
        }
    }
}