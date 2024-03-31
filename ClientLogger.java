import java.io.*;
import java.time.LocalDateTime;

public class ClientLogger {
    private static final String FILE_NAME = "clientLogfile.log";

    // write to the clientLogfile.log an event record when it sends a request to a server process
    public static synchronized void sendLog(String clientID, String serverID, String operation, String operationName, String parameters) { // 
        try  {
            FileWriter fileWriter = new FileWriter(FILE_NAME, true);
            String logEntry;

            if (!operationName.equals("halt")) { 
                logEntry = String.format("CLNT-%s SRV-%s \"%s\" %s %s %s%n", clientID, serverID, operation.equals("REQ") ? "REQ" : "RSP", LocalDateTime.now(), operationName, parameters);           
            }
            else { // special case when the HALT command is issued by the client -- we print the computed average value of the response times observed by the clients
                logEntry = String.format("CLNT-%s SRV-%s \"%s\" %s %s | Average Response Time = %s%n", clientID, serverID, operation.equals("REQ") ? "REQ" : "RSP", LocalDateTime.now(), operationName, parameters);
            }

            
            fileWriter.write(logEntry);
            fileWriter.flush();
            fileWriter.close();
        }

        catch (IOException e) {
            System.err.println("ClientLogger failed to log!");
        }
    }

    // write to the clientLogfile.log when a response is received
    public static synchronized void recieveLog(String clientID, String serverID, String operation, long response) {
        try  { // NOTE that the response status is represented as 'response' which is going to be the Server Processing Time for the replica process that calls the logger in RMIBankServerImp.java
            FileWriter fileWriter = new FileWriter(FILE_NAME, true); 
            String logEntry = String.format("CLNT-%s SRV-%s \"%s\" %s | Server Processing Time = %s%n", clientID, serverID, operation.equals("REQ") ? "REQ" : "RSP", LocalDateTime.now(), response);
            fileWriter.write(logEntry);
            fileWriter.flush();
            fileWriter.close();
        }

        catch (IOException e) {
            System.err.println("ClientLogger failed to log!");
        }
    }
}


