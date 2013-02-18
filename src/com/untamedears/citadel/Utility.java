package com.untamedears.citadel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Wool;

import com.untamedears.citadel.access.AccessDelegate;
import com.untamedears.citadel.entity.Faction;
import com.untamedears.citadel.entity.PlayerState;
import com.untamedears.citadel.entity.IReinforcement;
import com.untamedears.citadel.entity.NaturalReinforcement;
import com.untamedears.citadel.entity.PlayerReinforcement;
import com.untamedears.citadel.entity.ReinforcementMaterial;

/**
 * Created by IntelliJ IDEA.
 * User: chrisrico
 * Date: 3/23/12
 * Time: 3:37 PM
 */
public class Utility {

    private static Map<SecurityLevel, MaterialData> securityMaterial;
    private static Random rng = new Random();
    
    static {
        securityMaterial = new HashMap<SecurityLevel, MaterialData>();
        securityMaterial.put(SecurityLevel.PUBLIC, new Wool(DyeColor.GREEN));
        securityMaterial.put(SecurityLevel.GROUP, new Wool(DyeColor.YELLOW));
        securityMaterial.put(SecurityLevel.PRIVATE, new Wool(DyeColor.RED));
    }

    public static Block getAttachedChest(Block block) {
        if (block.getType() == Material.CHEST)
            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
                Block b = block.getRelative(face);
                if (b.getType() == Material.CHEST) {
                    return b;
                }
            }
        return null;
    }

    public static IReinforcement createNaturalReinforcement(Block block) {
        Material material = block.getType();
        int breakCount = Citadel
            .getConfigManager()
            .getMaterialBreakCount(material.getId(), block.getY());
        if (breakCount <= 1) {
            return null;
        }
        NaturalReinforcement nr = new NaturalReinforcement(block, breakCount);
        Citadel.getReinforcementManager().addReinforcement(nr);
        return nr;
    }

    public static IReinforcement createPlayerReinforcement(final Player player, Block block) {
        int blockTypeId = block.getTypeId();
        if (PlayerReinforcement.NON_REINFORCEABLE.contains(blockTypeId)) return null;

        PlayerState state = PlayerState.get(player);
        final ReinforcementMaterial material;
        switch (state.getMode()) {
            case REINFORCEMENT:
            case REINFORCEMENT_SINGLE_BLOCK:
                Material inHand = player.getItemInHand().getType();
                material = ReinforcementMaterial.get(inHand);
                if (material == null) {
                    sendMessage(player, ChatColor.RED, "Material in hand %s is not a valid reinforcement material.", inHand.name());
                    state.reset();
                    return null;
                }
                break;
            case FORTIFICATION:
                material = state.getReinforcementMaterial();
                break;
            case NORMAL:
            	material = ReinforcementMaterial.get(Material.AIR);
            	break;
            default:
                return null;
        }

    	// get group
        Faction group = state.getFaction();
    	if(group == null){
    		try {
    		group = Citadel.getMemberManager().getMember(player.getDisplayName()).getPersonalGroup();
    		} catch (NullPointerException e){
    			sendMessage(player, ChatColor.RED, "You don't seem to have a personal group. Try logging out and back in first");
    			return null;
    		}
    	}

    	// decrement material if its not air 
    	if(material.getMaterial() != Material.AIR)
    	{
	        if (player.getInventory().contains(material.getMaterial(), material.getRequirements()))
	        {
	    		Bukkit.getScheduler().scheduleSyncDelayedTask(Citadel.getPlugin(), new Runnable()
	    		{
	    			public void run() {
	    				player.getInventory().removeItem(material.getRequiredMaterials());
	    				//TODO: there will eventually be a better way to flush inventory changes to the client
	    				player.updateInventory();
	    			}
	    		}, 1);
	        }
	        else
	        {
	        	return null;
	        }
    	}

        // create the reinforcement
    	PlayerReinforcement reinforcement = new PlayerReinforcement(block, material, group, state.getSecurityLevel());
        reinforcement = (PlayerReinforcement)Citadel.getReinforcementManager().addReinforcement(reinforcement);

        // inform them if not in normal mode
        if(state.getMode() != PlacementMode.NORMAL)
        {
	        String securityLevelText = state.getSecurityLevel().name();
	        if(securityLevelText.equalsIgnoreCase("group")){
	        	securityLevelText = securityLevelText + "-" + state.getFaction().getName();
	        }
	        sendThrottledMessage(player, ChatColor.GREEN, String.format("Reinforced with %s at security level %s", material.getMaterial().name(), securityLevelText).replace("with AIR", "by hand"));
        }
        
        //Citadel.warning(String.format("PlRein:%s:%d@%s,%d,%d,%d",
        //    player.getName(), material.getMaterialId(), block.getWorld().getName(), block.getX(), block.getY(), block.getZ()));
        // TODO: enable chained flashers, they're pretty cool
        //new BlockFlasher(block, material.getFlasher()).start(getPlugin());
        //new BlockFlasher(block, material.getFlasher()).chain(securityMaterial.get(state.getSecurityLevel())).start();
        return reinforcement;
    }
    
    public static void sendMessage(CommandSender sender, ChatColor color, String messageFormat, Object... params) {
        sender.sendMessage(color + String.format(messageFormat, params));
    }
    
    public static void damagePlayerTool(final Player player) {
    	if(player == null) {
    		return;
    	}
    	
    	if(player.getItemInHand() == null) {
    		return;
    	}
    	
    	switch(player.getItemInHand().getType()) {
    	case WOOD_PICKAXE:
    	case WOOD_AXE:
    	case WOOD_SPADE:

    	case GOLD_PICKAXE:
    	case GOLD_AXE:
    	case GOLD_SPADE:

    	case STONE_PICKAXE:
    	case STONE_AXE:
    	case STONE_SPADE:

    	case IRON_PICKAXE:
    	case IRON_AXE:
    	case IRON_SPADE:

    	case DIAMOND_PICKAXE:
    	case DIAMOND_AXE:
    	case DIAMOND_SPADE:
    		Bukkit.getScheduler().scheduleSyncDelayedTask(Citadel.getPlugin(), new Runnable() {
    			public void run() {
    		    	ItemStack hand = player.getItemInHand();
    		    	
    		    	int unbreak = hand.getEnchantmentLevel(Enchantment.DURABILITY);
    		    	if(rng.nextDouble() < 1/(unbreak+1))
    		    	{
    		    		hand.setDurability((short)(hand.getDurability() + 1)); // not sure why this is backwards, but it is
    		    		player.setItemInHand(hand.getDurability() > hand.getType().getMaxDurability() ? null : hand);
    		    	}
    			}
    		}, 0);
    	}
    }

    public static void sendThrottledMessage(CommandSender sender, ChatColor color, String messageFormat, Object... params) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            PlayerState state = PlayerState.get(player);
            if (System.currentTimeMillis() - state.getLastThrottledMessage() > (1000 * 30)) {
                sendMessage(player, color, messageFormat, params);
            }
            state.setLastThrottledMessage(System.currentTimeMillis());
        }
    }

    public static boolean explodeReinforcement(Block block) {
        AccessDelegate delegate = AccessDelegate.getDelegate(block);
        IReinforcement reinforcement = delegate.getReinforcement();
        if (reinforcement == null) {
            reinforcement = (IReinforcement)createNaturalReinforcement(
                    block);
        }
        if (reinforcement == null) {
            return false;
        }
        return reinforcementDamaged(reinforcement);
    }

    public static boolean maybeReinforcementDamaged(Block block) {
        AccessDelegate delegate = AccessDelegate.getDelegate(block);
        IReinforcement reinforcement = delegate.getReinforcement();
        return reinforcement != null && reinforcementDamaged(reinforcement);
    }

    public static boolean reinforcementDamaged(IReinforcement reinforcement) {
        reinforcement.setDurability(reinforcement.getDurability() - 1);
        boolean cancelled = reinforcement.getDurability() > 0;
        if (reinforcement.getDurability() <= 0) {
            cancelled = reinforcementBroken(reinforcement);
        } else {
            if (reinforcement instanceof PlayerReinforcement) {
                Citadel.info("Reinforcement damaged at " + reinforcement.getBlock().getLocation().toString());
            }
            Citadel.getReinforcementManager().addReinforcement(reinforcement);
        }
        return cancelled;
    }

    public static boolean reinforcementBroken(IReinforcement reinforcement) {
        Citadel.getReinforcementManager().removeReinforcement(reinforcement);
        if (reinforcement instanceof PlayerReinforcement) {
            PlayerReinforcement pr = (PlayerReinforcement)reinforcement;
            Citadel.info("Reinforcement destroyed at " + pr.getBlock().getLocation().toString());

            if (rng.nextDouble() <= pr.getHealth()) {
                Location location = pr.getBlock().getLocation();
    	        ReinforcementMaterial material = pr.getMaterial();
                if(material.getMaterial() != Material.AIR)
                {
                    location.getWorld().dropItem(location, material.getRequiredMaterials());
                }
            }
            return pr.isSecurable();
        }
        return false;  // implicit isSecureable() == false
    }

    public static SecurityLevel getSecurityLevel(String[] args, Player player) {
        if (args.length > 0) {
            try {
                return SecurityLevel.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException e) {
                sendMessage(player, ChatColor.RED, "Invalid access level %s", args[0]);
                return null;
            }
        }
        return SecurityLevel.PRIVATE;
    }
    
    private static List<PlacementMode> MULTI_MODE = Arrays.asList(PlacementMode.FORTIFICATION, PlacementMode.INFO, PlacementMode.REINFORCEMENT);

    public static void setMultiMode(PlacementMode mode, SecurityLevel securityLevel, String[] args, Player player, PlayerState state) {
        if (!MULTI_MODE.contains(mode)) return;

        if (state.getMode() == mode && state.getSecurityLevel() == securityLevel) {
            state.reset();
            sendMessage(player, ChatColor.GREEN, "%s mode off", mode.name());
        } else {
            state.setMode(mode);
            state.setSecurityLevel(securityLevel);
            switch (mode) {
                case REINFORCEMENT:
                    sendMessage(player, ChatColor.GREEN, "%s mode %s", mode.name(), securityLevel.name());
                    break;
                case FORTIFICATION:
                    sendMessage(player, ChatColor.GREEN, "%s mode %s, %s", mode.name(), state.getReinforcementMaterial().getMaterial().name(), securityLevel.name());
                    break;
                case INFO:
                    sendMessage(player, ChatColor.GREEN, "%s mode on", mode.name());
                    break;
            }
            state.checkResetMode();
        }
    }
    
    public static void setSingleMode(SecurityLevel securityLevel, PlayerState state, Player player) {
        if (state.getMode() != PlacementMode.REINFORCEMENT_SINGLE_BLOCK) {
            state.setSecurityLevel(securityLevel);
            state.setMode(PlacementMode.REINFORCEMENT_SINGLE_BLOCK);
            sendMessage(player, ChatColor.GREEN, "Single block reinforcement mode %s", securityLevel.name() + ".");
        }
    }
    
    public static String getTruncatedMaterialMessage(String prefix, List<Integer> materials) {
	    StringBuilder builder = new StringBuilder();
	    for (int materialId : materials) {
	        if (builder.length() > 0) builder.append(", ");
	        builder.append(Material.getMaterial(materialId).name());
	    }
	    builder.insert(0, prefix);
	    return builder.toString();
	}
}
