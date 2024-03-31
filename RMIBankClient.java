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
import java.io.*;
import java.util.*;

public class RMIBankClient {
    public static void main (String args[]) throws Exception {
        try {
            if (args.length != 2) {
                throw new RuntimeException("Usage: java RMIBankClient <threadCount> <configFileName>");
                //System.err.println("Usage: java BankClient <serverHostname> <serverPortnumber> <threadCount> <iterationCount>");
                //System.exit(1);
            }
            String configFilePath = args[1];
            int threads = Integer.parseInt(args[0]);
            List<RMIBankServer> bankServerStubs = new ArrayList<>();
            
            BufferedReader reader = new BufferedReader(new FileReader(configFilePath));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] config = line.split(" ");
            
                Registry registry = LocateRegistry.getRegistry(config[0], Integer.parseInt(config[2]));
                // String[] boundNames = registry.list();
                // System.out.println("Names bound in RMI registry:");
                // for (String name : boundNames) {
                //     System.out.println(name);
                //     //RMIBankServer bankServerStub = (RMIBankServer) registry.lookup(name);

                // }
                // RMIBankServer bankServerStub = (RMIBankServer) registry.lookup("//" + config[0] + ":" + Integer.parseInt(config[2]) + "/Server_" + Integer.parseInt(config[1]));
                RMIBankServer bankServerStub = (RMIBankServer) registry.lookup("Server_" + Integer.parseInt(config[1]));
                bankServerStubs.add(bankServerStub);
            }

            Random random = new Random();
            ExecutorService executor = Executors.newFixedThreadPool(threads); // fixed thread pool size
            
            for (int i = 1; i <= threads; i++) {
                final int thread = i;
                executor.submit(() -> {
                    for (int k = 0; k < 200; k++) {
                        RMIBankServer bankServerStub = bankServerStubs.get(random.nextInt(bankServerStubs.size()));

                        int sourceAcountUID = random.nextInt(20) + 1;
                        int targetAccountUID = random.nextInt(20) + 1;
                        
                        try {
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

            Request request = new Request("halt", -1, -1, -1, -1, 0);
            ClientLogger.sendLog("0", bankServerStub.getServerID() + "", "REQ", "halt", "");
            long response = bankServerStub.clientRequest(request);
            ClientLogger.recieveLog("0", bankServerStub.getServerID() + "", "halt", response);

            //RMIBankServer serverStub = bankServerStubs.get(0);
            //serverStub.shutdown();

            //for (RMIBankServer serverStub : bankServerStubs) {
            //    serverStub.shutdown();
            //}
            // int totalAccountsBalance = 0;
            // RMIBankServer bankServerStub = bankServerStubs.get(random.nextInt(bankServerStubs.size()));

            // for (int i = 1; i <= 20; i++) { // after all threads have completed their tasks, it calculates and prints the total balance across all accounts again
            //     int balance = bankServerStub.getBalance(i);
            //     Request request = Request("transfer", sourceAcountUID, targetAccountUID, 10, -1, -1);
            //     bankServerStub.clientRequest(request);
                
            //     totalAccountsBalance += balance;

            // }
            // System.out.println("The Total Account Balance is: " + totalAccountsBalance);
               
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
