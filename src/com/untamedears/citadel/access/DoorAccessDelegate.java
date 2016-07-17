package com.untamedears.citadel.access;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.material.MaterialData;
/**
 * Created by IntelliJ IDEA.
 * User: chrisrico
 * Date: 3/23/12
 * Time: 3:26 PM
 */
public class DoorAccessDelegate extends AccessDelegate<MaterialData> {
    public static boolean canDelegate(Block block, MaterialData data) {
        return isDoorType(block.getType());
    }
    public DoorAccessDelegate(Block block, MaterialData data) {
        super(block, data);
    }
    
    public static boolean isDoorType(Material mat) {
    	switch(mat) {
	    	case WOOD_DOOR:
	    	case WOODEN_DOOR:
	    	case ACACIA_DOOR:
	    	case SPRUCE_DOOR:
	    	case JUNGLE_DOOR:
	    	case DARK_OAK_DOOR:
	    	case BIRCH_DOOR:
	    		return true;
	    	default:
	    		return false;
    	}
    }

    @Override
    protected boolean shouldDelegate() {
        return isDoorType(block.getRelative(BlockFace.DOWN).getType());
    }

    @Override
    protected void delegate() {
        block = block.getRelative(BlockFace.DOWN);
    }
}