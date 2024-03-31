import java.io.*;
import java.time.LocalDateTime;

public class ServerLogger {
    private static final String FILE_NAME = "serverLogfile";

    public static synchronized void recieveClientLog(String serverID, String requestTimestamp, String clientID, String operationName, String parameters) { // logging when the server receives a request from a client
        try  {
            FileWriter fileWriter = new FileWriter(FILE_NAME + "_" + serverID + ".log", true);
            String logEntry = String.format("Server-%s CLIENT-REQ %s %s Thread-%s %s %s%n", serverID, LocalDateTime.now(), requestTimestamp, clientID, operationName, parameters);            
            fileWriter.write(logEntry);
            fileWriter.flush();
            fileWriter.close();
        }

        catch (IOException e) {
            System.err.println("ServerLogger failed to log!");
        }
    }

    public static synchronized void recieveMulticastLog(String serverID, String requestTimestamp, String operationName, String parameters) { // logging when a server replica receives a request multicast by another Server Process Replica
        try  {
            FileWriter fileWriter = new FileWriter(FILE_NAME + "_" + serverID + ".log", true);
            String logEntry = String.format("Server-%s SRV-REQ %s %s %s %s%n", serverID, LocalDateTime.now(), requestTimestamp, operationName, parameters);            
            fileWriter.write(logEntry);
            fileWriter.flush();
            fileWriter.close();
        }

        catch (IOException e) {
            System.err.println("ServerLogger failed to log!");
        }
    }

    public static synchronized void removeLog(String serverID, String requestTimestamp) { // logging when a server removes a request from the request queue for processing
        try  {
            FileWriter fileWriter = new FileWriter(FILE_NAME + "_" + serverID + ".log", true);
            String logEntry = String.format("Server-%s REQ_PROCESSING %s %s%n", serverID, LocalDateTime.now(), requestTimestamp);            
            fileWriter.write(logEntry);
            fileWriter.flush();
            fileWriter.close();
        }

        catch (IOException e) {
            System.err.println("ServerLogger failed to log!");
        }
    }

    public static synchronized void haltResultLog(String serverID, String requestTimestamp, int sumAllAccountBalance, String requestQueueSize, String currentBalanceOf20Accounts, long averageProcessingTime) { // SPECIAL EXTRA logging when the server executes the HALT command 
        try  {
            FileWriter fileWriter = new FileWriter(FILE_NAME + "_" + serverID + ".log", true);
            fileWriter.write(currentBalanceOf20Accounts); // write the balance for each account in the server
            fileWriter.flush();

            String logEntry = String.format("Server-%s REQ_HALT %s %s | Sum of the balance in all 20 accounts = %s | Request Queue Size = %s | Average Processing Time = %s%n", serverID, LocalDateTime.now(), requestTimestamp, "" + sumAllAccountBalance, requestQueueSize, averageProcessingTime + "");            
            fileWriter.write(logEntry);
            fileWriter.flush();
            fileWriter.close();
        }

        catch (IOException e) {
            System.err.println("ServerLogger failed to log!");
        }
    }
}


