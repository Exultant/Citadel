package com.untamedears.citadel.entity;

import java.util.HashMap;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;

import com.untamedears.citadel.NaturalReinforcementConfig;

/**
 * User: erocs
 */
public class NaturalReinforcement implements IReinforcement {
    // HashMap<materialId, breakCount>
    public static final HashMap<Integer, NaturalReinforcementConfig> CONFIGURATION =
        new HashMap<Integer, NaturalReinforcementConfig>();

    private ReinforcementKey id_;
    private boolean broken_;
    private int max_durability_;
    private Random random;

    public NaturalReinforcement() {
        this.random = new Random();
    }

    public NaturalReinforcement(Block block, int max_durability) {
        this.id_ = new ReinforcementKey(block);
        this.max_durability_ = max_durability;
        this.random = new Random();
    }

    public ReinforcementKey getId() { return id_; }
    public void setId(ReinforcementKey id) { this.id_ = id; }

    public Block getBlock() {
        try {
    	    return Bukkit.getServer().getWorld(id_.getWorld()).getBlockAt(
                    id_.getX(),
                    id_.getY(),
                    id_.getZ());
        } catch (NullPointerException e){
    	    return null;
        }
    }

    public int getMaxDurability() { return max_durability_; }
    public void setMaxDurability(int max_durability) { this.max_durability_ = max_durability; }

    public double getHealth() {
        return 1.0;
    }

    public String getHealthText() {
        return "naturally";
    }

    public String getStatus() { return getHealthText(); }

    @Override
    public String toString() {
        return String.format("%s, hardness %d", id_, max_durability_);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IReinforcement)) return false;

        IReinforcement r = (IReinforcement)o;
        return this.id_.equals(r.getId());
    }

    public int compareTo(IReinforcement r) {
    	return this.id_.compareTo(r.getId());
    }

    @Override
    public int hashCode() {
        return this.id_.hashCode();
    }
    
    @Override
    public boolean isBroken() {
        return broken_;
    }
    
    @Override
    public boolean breakOnce() {
        if (broken_) return true;
        if (random.nextInt(max_durability_) == 0) {
          broken_ = true;
        }
        return broken_;
    }
}
