package api;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ControllerManager {
	private static ControllerManager instance;
	
    public static final int INTERVAL_SECS = 60; 
		
	private final ReadWriteLock _readWriteLock;
	
	// key: timestamp of start interval's transaction
	private Map<Long, Statistics> _statisticCacheMap; 
	
	private ControllerManager(){
		_readWriteLock = new ReentrantReadWriteLock();
		_statisticCacheMap = new HashMap<Long, Statistics>(); 
	}
	
	public static ControllerManager getInstance(){
        if(instance == null){
            synchronized(ControllerManager.class){
                if(instance == null){
                    instance = new ControllerManager();
                }    
            }
        } 
        return instance;
    }
	
	public Map<Long, Statistics> getCache() {
		return this._statisticCacheMap;
	}
	
	public Lock getReadLock() {
		return this._readWriteLock.readLock();
	}
	
	public Lock getWriteLock() {
		return this._readWriteLock.writeLock();
	}
}
