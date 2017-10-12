package api;

public class Transaction {
	private final long timestamp;
    private final double amount;
    
    public Transaction(){
    	timestamp = 0;
    	amount = 0;
    }

    public Transaction(long timestamp, double amount) {
        this.timestamp = timestamp;
        this.amount = amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getAmount() {
        return amount;
    }
}
