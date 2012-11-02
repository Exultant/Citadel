package com.untamedears.citadel.dao;

import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import com.untamedears.citadel.Citadel;
import com.untamedears.citadel.SecurityLevel;
import com.untamedears.citadel.entity.IReinforcement;
import com.untamedears.citadel.entity.NaturalReinforcement;
import com.untamedears.citadel.entity.PlayerReinforcement;
import com.untamedears.citadel.entity.ReinforcementKey;

public class CitadelCachingDao extends CitadelDao {
    public static String MakeChunkId( Chunk chunk ) {
        return String.format("%s.%d.%d", chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    HashMap<String, ChunkCache> cachesByChunkId;
    PriorityQueue<ChunkCache> cachesByTime;
    long maxAge;
    int maxChunks;
    int counterChunkCacheLoads;
    int counterChunkUnloads;
    int counterChunkTimeouts;
    int counterReinforcementsSaved;
    int counterReinforcementsDeleted;
    int counterCacheHits;
    int counterPreventedThrashing;

    public CitadelCachingDao(JavaPlugin plugin){
        super(plugin);
        cachesByTime = new PriorityQueue<ChunkCache>();
        cachesByChunkId = new HashMap<String, ChunkCache>();
        maxAge = Citadel.getConfigManager().getCacheMaxAge();
        maxChunks = Citadel.getConfigManager().getCacheMaxChunks();
        counterChunkCacheLoads = 0;
        counterChunkUnloads = 0;
        counterChunkTimeouts = 0;
        counterReinforcementsSaved = 0;
        counterReinforcementsDeleted = 0;
        counterCacheHits = 0;
        counterPreventedThrashing = 0;
    }

    public ChunkCache getCacheOfBlock( Block block ) throws RefuseToPreventThrashingException {
        String chunkId = MakeChunkId(block.getChunk());
        ChunkCache cache = cachesByChunkId.get(chunkId);

        if( cache != null ){
            cachesByTime.remove(cache);
            cache.Access();
            cachesByTime.add(cache);
            ++counterCacheHits;
        }else{
            int removeCount = Math.max(5, cachesByTime.size() - maxChunks + 1);
            ChunkCache last = cachesByTime.peek();
            while (last != null && removeCount > 0 && last.getLastAccessed() + maxAge < System.currentTimeMillis()) {
                cachesByChunkId.remove(last.getChunkId());
                cachesByTime.poll();
                last.flush();
                last = cachesByTime.peek();
                --removeCount;
                ++counterChunkTimeouts;
            }
            if( cachesByTime.size() > maxChunks ){
                // WARNING: This WILL cause NaturalReinforcements to NOT be saved and thus lost.
                // TODO: Figure out why this needed to be added
                ++counterPreventedThrashing;
                throw new RefuseToPreventThrashingException();
            }
            ++counterChunkCacheLoads;
            cache = new ChunkCache( this, block.getChunk() );
            cachesByChunkId.put( cache.getChunkId(), cache );
            cachesByTime.add( cache );
        }
        return cache;
    }

    @Override
    public IReinforcement findReinforcement( Location location ){
        return findReinforcement(location.getBlock());
    }

    @Override
    public IReinforcement findReinforcement( Block block ){
        try{
            ChunkCache cache = getCacheOfBlock( block );
            return cache.findReinforcement(block);
        }catch( RefuseToPreventThrashingException e ){
            Citadel.warning( "Bypassing RAM cache to prevent database thrashing.  Consider raising caching.max_chunks");
            return super.findReinforcement( block );
        }
    }

    @Override
    public void save(Object o){
        if( o instanceof IReinforcement ){
            IReinforcement r = (IReinforcement)o;
            try{
                getCacheOfBlock( r.getBlock() ).save( r );
            }catch( RefuseToPreventThrashingException e ){
                if ( !(r instanceof NaturalReinforcement) ){
                    Citadel.warning( "Bypassing RAM cache to prevent database thrashing.  Consider raising caching.max_chunks");
                    super.save( r );
                }
            }
        }else{
            super.save( o );
        }
    }
    
    @Override
    public void delete(Object o){
        if( o instanceof IReinforcement ){
            IReinforcement r = (IReinforcement)o;
            try{
                getCacheOfBlock( r.getBlock() ).delete( r );
            }catch( RefuseToPreventThrashingException e ){
                if ( !(r instanceof NaturalReinforcement) ){
                    Citadel.warning( "Bypassing RAM cache to prevent database thrashing.  Consider raising caching.max_chunks");
                    super.delete( r );
                }
            }
        }else{
            super.delete( o );
        }
    }

    //There should be some interface to implement that calls this automatically.
    public void shutDown(){
        while( !cachesByTime.isEmpty() ){
            ChunkCache cache = cachesByTime.poll();
            cachesByChunkId.remove(cache.getChunkId());
            cache.flush();
        }
    }

    public void unloadChunk(Chunk chunk) {
        // Flush any chunk data to the DB when the chunk unloads. Leave the
        //  ChunkCache intact in case the chunk reloads before it is removed
        //  from the ChunkCache cache.
        String chunkId = MakeChunkId(chunk);
        ChunkCache cache = cachesByChunkId.get(chunkId);
        if (cache != null) {
            cache.flush();
        }
        ++counterChunkUnloads;
    }

    public void addCounterReinforcementsSaved(int delta) {
        counterReinforcementsSaved += delta;
    }

    public void addCounterReinforcementsDeleted(int delta) {
        counterReinforcementsDeleted += delta;
    }

    private enum DBUpdateAction {
        SAVE,
        DELETE
    }

    private class ChunkCache implements
            Comparable<ChunkCache> {
        private CitadelCachingDao dao;
        private HashMap<PlayerReinforcement, DBUpdateAction> dbUpdates;
        private TreeSet<IReinforcement> cache;//if RAM isn't a problem replace this with a HashSet.
        private String chunkId;
        private long lastAccessed;

        public ChunkCache( CitadelCachingDao dao, Chunk chunk ){
            this.dao = dao;
            this.chunkId = CitadelCachingDao.MakeChunkId(chunk);
            cache = new TreeSet<IReinforcement>( findReinforcementsInChunk( chunk ));
            dbUpdates = new HashMap<PlayerReinforcement, DBUpdateAction>();
            lastAccessed = System.currentTimeMillis();
        }

        public long getLastAccessed() { return lastAccessed; }
        public void Access() { lastAccessed = System.currentTimeMillis(); }

        public IReinforcement findReinforcement( Location l ){
            return findReinforcement(l.getBlock());
        }

        public IReinforcement findReinforcement( Block block ){
            IReinforcement key = new NaturalReinforcement();
            key.setId(new ReinforcementKey(block));
            IReinforcement r = cache.floor( key );

            if( r != null && r.equals(key) ){
                if ( r instanceof PlayerReinforcement){
                    dbUpdates.put((PlayerReinforcement)r, DBUpdateAction.SAVE);
    	        }
                return r;
            }else{
                return null;
            }
        }

        public void save( IReinforcement r ){
            if (r.getDurability() <= 0)
            {
                delete(r);
                return;
            }

            if( cache.contains(r) ){
                //Yes, this makes sense.
                //If we're editing the cache, then our new "r" will equal the old "r"
                //because reinforements are compared by their ReinforcementKeys, and the
                //new and old Reinforcements have the same key.  So we're removing the reinforcement
                //from the set that matches the new "r" (which the old "r" does) and then adding
                //the new "r".
                cache.remove( r );
                cache.add( r );
            }else{
                cache.add( r );
            }

            if ( r instanceof PlayerReinforcement){
                dbUpdates.put((PlayerReinforcement)r, DBUpdateAction.SAVE);
            }
        }

        public void delete( IReinforcement r ){
            cache.remove( r );
            if ( r instanceof PlayerReinforcement){
                dbUpdates.put((PlayerReinforcement)r, DBUpdateAction.DELETE);
            }
        }

        public void flush(){
            LinkedList<PlayerReinforcement> toSave = new LinkedList<PlayerReinforcement>();
            LinkedList<PlayerReinforcement> toDelete = new LinkedList<PlayerReinforcement>();
            for (Map.Entry<PlayerReinforcement, DBUpdateAction> entry : dbUpdates.entrySet()) {
                if (entry.getValue() == DBUpdateAction.DELETE) {
                    toDelete.add(entry.getKey());
                } else {
                    toSave.add(entry.getKey());
                }
            }
            dbUpdates.clear();
            dao.addCounterReinforcementsSaved(toSave.size());
            dao.addCounterReinforcementsDeleted(toDelete.size());
            int saveSuccess = getDatabase().save(toSave);
            int deleteSuccess = getDatabase().delete(toDelete);
        }

        public String getChunkId(){
            return chunkId;
        }

        public String toString(){
            return String.format("Cache (%s) has %d unsaved reinforcements.", chunkId, dbUpdates.size());
        }

        public int compareTo(ChunkCache that) {
            if (this.lastAccessed < that.lastAccessed) {
                return -1;
            } else if (this.lastAccessed > that.lastAccessed) {
                return 1;
            }
            return 0;
        }
    }

    private class RefuseToPreventThrashingException extends Exception {}
}
