import java.io.Serializable;

public class Request implements Serializable, Comparable<Request> {
    private String requestType;
    private int sourceAccountUID;
    private int targetAccountUID;
    private int amount; 
    private int timestamp;
    private int sendingServerID;

    public Request(String requestType, int sourceAccountUID, int targetAccountUID, int amount, int timestamp, int sendingServerID) {
        this.requestType = requestType;
        this.sourceAccountUID = sourceAccountUID;
        this.targetAccountUID = targetAccountUID;
        this.amount = amount;
        this.timestamp = timestamp;
        this.sendingServerID = sendingServerID;
    }

    public int compareTo(Request request) {
        if (this == null || request == null) {
            return 0;
        }
        if (this.timestamp != request.getTimestamp()) {
            return this.timestamp - request.getTimestamp();
        } else {
            return this.sendingServerID - request.getSendingServerID();
        }
    }

    public String getRequestType() {
        return requestType;
    }

    public int getSourceAccountUID() {
        return sourceAccountUID;
    }

    public int getTargetAccountUID() {
        return targetAccountUID;
    }

    public int getAmount() {
        return amount;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public int getSendingServerID() {
        return sendingServerID;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public void setSourceAccountUID(int sourceAccountUID) {
        this.sourceAccountUID = sourceAccountUID;
    }

    public void setTargetAccountUID(int targetAccountUID) {
        this.targetAccountUID = targetAccountUID;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public void SetSendingServerID(int sendingServerID) {
        this.sendingServerID = sendingServerID;
    }
    
}