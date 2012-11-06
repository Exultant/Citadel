package com.untamedears.citadel.entity;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.ContainerBlock;
import org.bukkit.entity.Player;
import org.bukkit.material.Openable;

import com.untamedears.citadel.DbUpdateAction;
import com.untamedears.citadel.SecurityLevel;

/**
 * User: chrisrico
 */
@Table(name="reinforcement")
@Entity
public class PlayerReinforcement implements
        IReinforcement, Comparable<IReinforcement> {

    public static final List<Integer> SECURABLE = new ArrayList<Integer>();
    public static final List<Integer> NON_REINFORCEABLE = new ArrayList<Integer>();

    @Id private ReinforcementKey id;
    private int materialId;
    private int durability;
    private SecurityLevel securityLevel;
    // @Transient == not persisted in the DB
    @Transient private DbUpdateAction dbAction;

    @Version
    @Column(name="version")
    private int dbRowVersion;  // Do not touch

    @ManyToOne
    @JoinColumn(name = "name")
    private Faction owner;


    public PlayerReinforcement() {
        this.dbAction = DbUpdateAction.NONE;
    }

    public PlayerReinforcement(
	        Block block,
	        ReinforcementMaterial material,
	        Faction owner,
            SecurityLevel securityLevel) {
        this.id = new ReinforcementKey(block);
        this.materialId = material.getMaterial().getId();
        this.durability = material.getStrength();
        this.owner = owner;
        this.securityLevel = securityLevel;
        this.dbAction = DbUpdateAction.INSERT;
    }

    private void flagForDbUpdate() {
        if (getDbAction() == DbUpdateAction.NONE) {
            setDbAction(DbUpdateAction.SAVE);
        }
    }

    public void updateFrom(PlayerReinforcement that) {
        setMaterialId(that.getMaterialId());
        setDurability(that.getDurability());
        setOwner(that.getOwner());
        setSecurityLevel(that.getSecurityLevel());
        if (getDbAction() == DbUpdateAction.DELETE) {
            setDbAction(DbUpdateAction.SAVE);
        }
    }

    public ReinforcementKey getId() { return id; }
    public void setId(ReinforcementKey id) { this.id = id; }

    // Do not touch
    public int getDbRowVersion() { return this.dbRowVersion; }
    public void setDbRowVersion(int value) { this.dbRowVersion = value; }
    // Do not touch

    public Block getBlock() {
        try {
        	return Bukkit.getServer().getWorld(id.getWorld()).getBlockAt(
                    id.getX(),
                    id.getY(),
                    id.getZ());
        } catch (NullPointerException e) {
        	return null;
        }
    }

    public ReinforcementMaterial getMaterial() {
        return ReinforcementMaterial.get(Material.getMaterial(materialId));
    }

    public int getMaterialId() {
        return materialId;
    }

    public void setMaterialId(int materialId) {
        flagForDbUpdate();
        this.materialId = materialId;
    }

    public int getDurability() {
        return durability;
    }

    public void setDurability(int durability) {
        flagForDbUpdate();
        this.durability = durability;
    }

    public SecurityLevel getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(SecurityLevel securityLevel) {
        flagForDbUpdate();
        this.securityLevel = securityLevel;
    }

    public Faction getOwner() {
        return owner;
    }

    public void setOwner(Faction group) {
        flagForDbUpdate();
        this.owner = group;
    }

    public double getHealth() {
        return (double) durability / (double) getMaterial().getStrength();
    }

    public String getHealthText() {
        double health = getHealth();
        if (health > 0.75) {
            return "excellently";
        } else if (health > 0.50) {
            return "well";
        } else if (health > 0.25) {
            return "decently";
        } else {
            return "poorly";
        }
    }

    public String getStatus() {
        String verb;
        if (isSecurable()) {
            verb = "Locked";
        } else {
            verb = "Reinforced";
        }
        return String.format("%s %s with %s",
                verb,
                getHealthText(),
                getMaterial().getMaterial().name());
    }

    public boolean isAccessible(Player player) {
        String name = player.getDisplayName();
        return isAccessible(name);
    }

    public boolean isAccessible(String name) {
        switch (securityLevel) {
            case PRIVATE:
                return name.equals(owner.getFounder());
            case GROUP:
                return name.equals(owner.getFounder()) || owner.isMember(name) || owner.isModerator(name);
            case PUBLIC:
            	return true;
        }
        return false;
    }
    
    public boolean isBypassable(Player player) {
        String name = player.getDisplayName();
        switch (securityLevel) {
            case PRIVATE:
                return name.equals(owner.getFounder());
            default:
                return name.equals(owner.getFounder()) || owner.isModerator(name);
        }
    }

    public boolean isSecurable() {
        Block block = getBlock();
        return block.getState() instanceof ContainerBlock
                || block.getState().getData() instanceof Openable
                || SECURABLE.contains(block.getTypeId());
    }

    @Override
    public String toString() {
        return String.format("%s, material: %s, durability: %d", id, getMaterial().getMaterial().name(), durability);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IReinforcement)) return false;

        IReinforcement r = (IReinforcement)o;
        return this.id.equals(r.getId());
    }

    public int compareTo(IReinforcement r) {
    	return this.id.compareTo(r.getId());
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    public DbUpdateAction getDbAction() { return this.dbAction; }
    public void setDbAction(DbUpdateAction value) { this.dbAction = value; }
}
