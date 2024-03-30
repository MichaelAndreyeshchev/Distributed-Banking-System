public class LamportClock {
    private int logicalTime;

    public LamportClock() {
        this.logicalTime = 0;
    }

    public synchronized int getTime() {
        return logicalTime;
    }

    public synchronized int increment() {
        this.logicalTime++;
        return this.logicalTime;
    } 

    public synchronized void update(int receivedLogicalTime) {
        this.logicalTime = Math.max(receivedLogicalTime, logicalTime);
        this.logicalTime++;
    }

    public synchronized void updateNoIncrement(int receivedLogicalTime) {
        this.logicalTime = Math.max(receivedLogicalTime, logicalTime);
    }

}
