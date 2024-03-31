//package Andrev-Assignment-2.Part B;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.*;
import java.util.*;

public class RMIBankClient {
    public static void main (String args[]) throws Exception {
        try {
            if (args.length != 2) {
                throw new RuntimeException("Usage: java RMIBankClient <threadCount> <configFileName>");
            }
            String configFilePath = args[1]; // config file path specifying the hostname, server ID, and port. The config file is either 'local_host_config_file.txt', 'one_server_config_file.txt', 'three_server_config_file.txt', or 'five_server_config_file.txt'.
            int threads = Integer.parseInt(args[0]); // the number of threads specified
            List<RMIBankServer> bankServerStubs = new ArrayList<>(); // hold the stubs representing each server process
            
            BufferedReader reader = new BufferedReader(new FileReader(configFilePath));
            String line;
            while ((line = reader.readLine()) != null) { // iterate through config file and lookup RMI server stub (representing each server process) and add them into the ArrayList
                String[] config = line.split(" ");
            
                Registry registry = LocateRegistry.getRegistry(config[0], Integer.parseInt(config[2]));
                RMIBankServer bankServerStub = (RMIBankServer) registry.lookup("Server_" + Integer.parseInt(config[1]));
                bankServerStubs.add(bankServerStub);
            }

            Random random = new Random();
            ExecutorService executor = Executors.newFixedThreadPool(threads); // fixed thread pool size
            
            for (int i = 1; i <= threads; i++) { 
                final int thread = i;
                executor.submit(() -> {
                    for (int k = 0; k < 200; k++) {
                        RMIBankServer bankServerStub = bankServerStubs.get(random.nextInt(bankServerStubs.size())); // randomly pick one of the server replicas

                        int sourceAcountUID = random.nextInt(20) + 1; // source random account
                        int targetAccountUID = random.nextInt(20) + 1; // target random account
                        
                        try { // Send the transfer request to the chosen server replica
                            Request request = new Request("transfer", sourceAcountUID, targetAccountUID, 10, -1, thread);
                            ClientLogger.sendLog(thread + "", bankServerStub.getServerID() + "", "REQ", "transfer", " " + sourceAcountUID + " to " + targetAccountUID);
                            long response = bankServerStub.clientRequest(request); // the response is the server processing time
                            ClientLogger.recieveLog(thread + "", bankServerStub.getServerID() + "", "transfer", response);
                        }
        
                        catch (RemoteException e) {
                            System.out.print("Remote Exception error encountered!");
                        } catch (MalformedURLException e) {
                            System.out.print("Maleformed URL error encountered!");
                        } catch (IOException e) {
                                System.out.print("IO Exception error encountered!");
                        } catch (NotBoundException e) {
                            System.out.println(e);
                        }
                    }
                });
            }

            executor.shutdown();

            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            }

            catch (InterruptedException e){
                Thread.currentThread().interrupt();
                System.out.println("Thread was interrupted!");
            }

            RMIBankServer bankServerStub = bankServerStubs.get(0);

            Request request = new Request("halt", -1, -1, -1, -1, 0); // set up request for the HALT command
            long totaResponseTime = 0;
            int fileLineCounter = 0;

            List<String> clientLogEntries = Files.readAllLines(Paths.get("clientLogfile.log"));
            for (String fileLine : clientLogEntries) { // calculate the average value of the response times observed by the client
                if (fileLine.contains("\"RSP\"")) {
                    long serverProcessingTime = Long.parseLong(fileLine.split(" = ")[1]);
                    totaResponseTime += serverProcessingTime;
                    fileLineCounter++;
                }
            }
            // sending a HALT command to the server process with server ID equal to zero 
            ClientLogger.sendLog("0", bankServerStub.getServerID() + "", "REQ", "halt", totaResponseTime / fileLineCounter + "");
            long response = bankServerStub.clientRequest(request);
            ClientLogger.recieveLog("0", bankServerStub.getServerID() + "", "halt", response);
               
        }

    catch (UnmarshalException e) {
        System.out.println("The servers have been sucessfully terminated!");
    }

    catch (Exception e) {
        Thread.currentThread().interrupt();
        System.out.println("Client Exception!");
        e.printStackTrace();
        throw e;
    } 
}
}
