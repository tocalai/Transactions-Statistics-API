package api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApiController {
	
	
	private ControllerManager _manager = ControllerManager.getInstance();
	
    @RequestMapping(value = "/transactions", method = RequestMethod.POST, produces = "application/json; charset=UTF-8")
    public ResponseEntity<?> transactions(@RequestBody Transaction transaction) {
         		
    	 try {
    		 // TODO
    		 // create the new task to insert the transaction to database or Redis(for example) as well
    		 // not implement yet
    		 
    		 System.out.println("Enter post transaction, " + "Thread Id: " + Thread.currentThread().getId());
    		 _manager.getWriteLock().lock();

    		Long cacheTimestamp = _manager.getCache().size() == 0 ? null : (Long)_manager.getCache().keySet().toArray()[0];
    		double amount = transaction.getAmount();
    	
    		if(cacheTimestamp == null) {
    			
    			// no data in cache
    			insertCache(amount, transaction.getTimestamp());
    			return new ResponseEntity<>(new EmptyJsonResponse(), HttpStatus.CREATED);
    		}
    		else {
    			if(isInInterval(transaction.getTimestamp(), cacheTimestamp, ControllerManager.INTERVAL_SECS)){
    				// update the cache
    				updateCache(cacheTimestamp, transaction);
    				return new ResponseEntity<>(new EmptyJsonResponse(), HttpStatus.CREATED);
    			}
    			else {
    				// remove old interval data
    				_manager.getCache().remove(cacheTimestamp);
    				// insert cache
    				insertCache(amount, transaction.getTimestamp());
    				return new ResponseEntity<>(new EmptyJsonResponse(), HttpStatus.NO_CONTENT);
    			}
    		}
    	 }
    	 finally {
    		 _manager.getWriteLock().unlock();
    		 System.out.println("Exit post transaction, " + "Thread Id: " + Thread.currentThread().getId());
    	 }
    	
    }
    
    @RequestMapping(value = "/statistics/", method = RequestMethod.GET, produces = "application/json; charset=UTF-8")
    public ResponseEntity<?> statistics() {
    	try {
    		_manager.getReadLock().lock();
    		long curTimestamp = System.currentTimeMillis();
    		Long cacheTimestamp = _manager.getCache().size() == 0 ? null : (Long)_manager.getCache().keySet().toArray()[0];
    		if(cacheTimestamp != null) {
    			if(isInInterval(curTimestamp, cacheTimestamp, ControllerManager.INTERVAL_SECS)) {
    				 return new ResponseEntity<>(_manager.getCache().get(cacheTimestamp), HttpStatus.OK);
        		}
    			
    		}
    		
    		return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
    		
    	}
    	finally {
    		_manager.getReadLock().unlock();
    	}
    }
    
    private boolean isInInterval(long timestampNew, long timestampOld, int interval) {
    	
    	interval = interval == 0 ? ControllerManager.INTERVAL_SECS : interval;
    	if((timestampNew - timestampOld) / 1000.0 <= interval) {
    		return true;
    	}
    	
    	return false;
    }
    
    private void insertCache(double amount, long timestamp) {
    	Statistics st = new Statistics(amount, amount, amount, amount, 1);
		_manager.getCache().put(timestamp, st);
    }
    
    private void updateCache(long key, Transaction transaction) {
    	Statistics st = _manager.getCache().get(key);
    	double amount = transaction.getAmount();
    	long count = st.getCount() + 1;
    	double sum = st.getSum() + amount;
    	double max = st.getMax() >= amount ? st.getMax() : amount;
    	double min = st.getMin() <= amount ? st.getMin() : amount;
    	st.setCount(count);
    	st.setSum(sum);
    	st.setAvg(sum / count);
    	st.setMax(max);
    	st.setMin(min);
    	
    	_manager.getCache().put(key, st);
    }
    
}
