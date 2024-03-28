import java.io.*;
import java.time.LocalDateTime;

public class ClientLogger {
    private static final String FILE_NAME = "clientLogfile.log";

    public static synchronized void sendLog(String clientID, String serverID, String operation, String operationName, String parameters) {
        try  {
            FileWriter fileWriter = new FileWriter(FILE_NAME, true);
            String logEntry = String.format("CLNT-%s SRV-%s \"%s\" %s %s %s%n", clientID, serverID, operation.equals("REQ") ? "REQ" : "RSP", LocalDateTime.now(), operationName, parameters);
            fileWriter.write(logEntry);
            fileWriter.flush();
            fileWriter.close();
        }

        catch (IOException e) {
            System.err.println("ClientLogger failed to log!");
        }
    }

    public static synchronized void recieveLog(String clientID, String serverID, String operation, String status) {
        try  {
            FileWriter fileWriter = new FileWriter(FILE_NAME, true);
            String logEntry = String.format("CLNT-%s SRV-%s \"%s\" %s %s%n", clientID, serverID, operation.equals("REQ") ? "REQ" : "RSP", LocalDateTime.now(), status);
            fileWriter.write(logEntry);
            fileWriter.flush();
            fileWriter.close();
        }

        catch (IOException e) {
            System.err.println("ClientLogger failed to log!");
        }
    }
}


