import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.registry.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;

public class RMIBankServerImp implements RMIBankServer {
    private int serverID;
    private ConcurrentHashMap<Integer, Account> accounts = new ConcurrentHashMap<>(); // ConcurrentHashMap that maps unique account IDs to Account objects, representing the bank accounts managed by the server
    private AtomicInteger accountUIDCounter = new AtomicInteger(1); // generate unique IDs for new accounts, starting from 1
    private LamportClock clock = new LamportClock(); // Lamport clock to help with executing the same sequence of operations, using the State Machine Model
    private PriorityQueue<Request> requests = new PriorityQueue<>(Comparator.comparingInt(Request::getTimestamp));
    private ConcurrentHashMap<Integer, Integer> ackRecieved = new ConcurrentHashMap<>();
    private Map<Integer, String> serverIDToAddress = new HashMap<>();
    //private Map<Integer, RMIBankServer> replicaServers = new HashMap();

    public RMIBankServerImp() throws RemoteException {
        super();
    }

    public RMIBankServerImp(String configFilePath, int serverID) throws IOException {
        super();
        this.serverID = serverID;
        loadConfigFile(configFilePath);

        for(int i = 1; i <= 20; i++) {
            Account account = new Account(i, 1000);
            accounts.put(i, account);
        }
        System.out.println("Initialization is complete! Ready to get requests...");
    }

    public void loadConfigFile(String configFilePath) throws RemoteException, IOException {
        BufferedReader reader = new BufferedReader(new FileReader(configFilePath));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] config = line.split(" ");
            int serverID = Integer.parseInt(config[1]);

            if (serverID != this.serverID) {
                String serverAddress = "//" + config[0] + ":" + config[2] + "/Server_" + config[1];
                serverIDToAddress.put(serverID, serverAddress);
            }
        }
        reader.close();
    }

    public void shutdown() throws RemoteException { // unbinds the server from the RMI registry and unexports the RMI object, effectively shutting down the server
        try {
            System.out.println("Server is terminating...");

            for (String replicaAddress : serverIDToAddress.values()) {
                replicaAddress = replicaAddress.replaceFirst("//", "");
                String host = replicaAddress.split(":", 2)[0];
                int port = Integer.parseInt(replicaAddress.split(":", 2)[1].split("/", 2)[0]);

                //RMIBankServer replica = (RMIBankServer) Naming.lookup(replicaAddress);
                Registry localRegistry = LocateRegistry.getRegistry(host, port);

                String[] boundNames = localRegistry.list();
                System.out.println("Names bound in RMI registry:");
                for (String name : boundNames) {
                    System.out.println(name);
                    //RMIBankServer bankServerStub = (RMIBankServer) registry.lookup(name);
                }
                localRegistry.unbind("Server_" + serverID);
                
            }
            UnicastRemoteObject.unexportObject(this, true);
            System.out.println("RMI Server Port Shutdown Completed!");
            //System.exit(0);
        
        }

        catch (Exception e) {
            System.out.println(e);
        }
    }

    public int createAccount() throws RemoteException { // generates a unique ID for a new account, creates an account, logs the operation, and stores it in the accounts map
        int sourceAcountUID = accountUIDCounter.getAndIncrement();
        Account account = new Account(sourceAcountUID);
        accounts.put(sourceAcountUID, account);

        return sourceAcountUID;
    }

    public String deposit(int sourceAcountUID, int amount) throws RemoteException { // adds a specified amount to an existing account and logs the operation
        Account account = accounts.get(sourceAcountUID);

        if (account != null) {
            account.deposit(amount);
            return "OK";
        }

        else {
            return "FAILED";
        }
    }

    public int getBalance(int sourceAcountUID) throws RemoteException { // retrieves the balance of a specified account and logs the operation
        Account account = accounts.get(sourceAcountUID);

        if (account != null) {
            int balance = account.getBalance();
            return balance;
        }

        else {
            return -1;
        }
    }

    public String transfer(int sourceAcountUID, int targetAccountUID, int amount) throws RemoteException { // transfers funds from one account to another if sufficient funds are available and logs the operation.
        Account account = accounts.get(sourceAcountUID);
        Account targetAccount = accounts.get(targetAccountUID);

        if (account != null && targetAccount != null && account.transfer(amount)) {
            targetAccount.deposit(amount);
            return "OK";
        }

        else {
            return "FAILED";
        }
    }

    public String halt() throws RemoteException {
        int balanceAllAccounts = 0;

        for (Map.Entry<Integer, Account> entry : accounts.entrySet()) {
            int balance = entry.getValue().getBalance();
            balanceAllAccounts += balance;
        }
        
        StringBuilder sb = new StringBuilder();
        for (Request request : requests) {
            sb.append(request.toString());
            sb.append(System.lineSeparator()); // For newline
        }

        String requestQueueString = sb.toString();

        ServerLogger.haltResultLog(String.valueOf(serverID), "[" + clock.getTime() + ", " + serverID + "]", balanceAllAccounts, requestQueueString);
        System.out.println(String.valueOf(serverID) +  " [" + clock.getTime() + ", " + serverID + "] " + " " + balanceAllAccounts + " " + requestQueueString);

        return "OK";

    }

    public int getServerID() throws RemoteException {
        return this.serverID;
    }

    public String clientRequest(Request request) throws RemoteException, MalformedURLException, NotBoundException {        
        clock.increment();
        int logicalTime = clock.getTime();
        request.setTimestamp(logicalTime);
        ServerLogger.recieveClientLog(String.valueOf(serverID), "[" + logicalTime + ", " + this.serverID + "]", "" + request.getSendingServerID(), request.getRequestType(), "");
        request.SetSendingServerID(this.serverID);
        requests.add(request);
        
        for (String replicaAddress : serverIDToAddress.values()) {
            RMIBankServer replica = (RMIBankServer) Naming.lookup(replicaAddress);
            replica.multicast(request, this.serverID);
        }

        return "OK";
    }

    public void multicast(Request request, int senderID) throws RemoteException, MalformedURLException, NotBoundException {
        clock.update(request.getTimestamp());
        ServerLogger.recieveMulticastLog(String.valueOf(serverID), "[" + clock.getTime() + ", " + serverID + "]", request.getRequestType(), "");

        requests.add(request);
        String senderAddress = serverIDToAddress.get(senderID);
        RMIBankServer sender = (RMIBankServer) Naming.lookup(senderAddress);
        sender.acknowledge(request.getTimestamp());
    }

    public void acknowledge(int timestamp) throws RemoteException, MalformedURLException, NotBoundException {
        clock.update(timestamp);
        ackRecieved.put(timestamp, ackRecieved.getOrDefault(timestamp, 0) + 1);
        //for (String replicaAddress : replicaServers) {
        //    ackRecieved.put(timestamp, ackRecieved.getOrDefault(timestamp, 0) + 1);
        //}
        processRequest();
    }

    public boolean executeRequestCheck(Request request) throws RemoteException {
        return ackRecieved.getOrDefault(request.getTimestamp(), 0) >= (serverIDToAddress.size() + 1);
    }

    public void processRequest() throws MalformedURLException, RemoteException, NotBoundException {
        while (!requests.isEmpty() && executeRequestCheck(requests.peek())) {
            System.out.println(clock.getTime());
            Request request = requests.poll();
            executeRequest(request);

            for (String replicaAddress : serverIDToAddress.values()) {
                RMIBankServer replica = (RMIBankServer) Naming.lookup(replicaAddress);
                replica.executeRequest(request);
            }
        }
    }

    public void executeRequest(Request request) throws RemoteException {
        requests.remove(request.getTimestamp());
        ServerLogger.removeLog(String.valueOf(serverID), "[" + clock.getTime() + ", " + serverID + "]");

        
        switch (request.getRequestType()) {
            case "createAccount":
                createAccount();
                break;
            case "deposit":
                deposit(request.getSourceAccountUID(), request.getAmount());
                break;
            case "transfer":
                transfer(request.getSourceAccountUID(), request.getTargetAccountUID(), request.getAmount());
                break;

            case "halt":
                halt();
                break;
            default:
                System.err.println("ERROR: Unknown Request Type: " + request.getRequestType());
                break;
        }
    }
    public static void main (String args[]) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: java RMIBankServerImp <serverID> <configFilePath>");
            System.exit(1);
        }

        try {
            String hostname = "";
            int port = -1;
            int serverID = Integer.parseInt(args[0]);
            String configFilePath = args[1];
            BufferedReader reader = new BufferedReader(new FileReader(configFilePath));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] config = line.split(" ");

                if (serverID == Integer.parseInt(config[1])) {
                    hostname = config[0];
                    port = Integer.parseInt(config[2]);
                }
            }
            reader.close();

            if (port == -1) {
                throw new Exception("Entered a non existant serverID!");
            }

            Registry registry;

            try {
                registry = LocateRegistry.createRegistry(port);
            }

            catch (Exception e) {
                registry = LocateRegistry.getRegistry(port);
            }

            

            RMIBankServerImp bankServer = new RMIBankServerImp(configFilePath, serverID);
            
            System.setProperty("java.rmi.server.hostname", hostname);
            RMIBankServer bankServerStub = (RMIBankServer) UnicastRemoteObject.exportObject(bankServer, 0) ;


            //Registry registry = LocateRegistry.createRegistry(5000 + serverID); // Example port assignment logic
            registry.bind("Server_" + serverID, bankServerStub);
            System.out.println("Server " + serverID + " is ready.");
            System.out.println("//" + hostname + ":" + port + "/Server_" + serverID);
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
