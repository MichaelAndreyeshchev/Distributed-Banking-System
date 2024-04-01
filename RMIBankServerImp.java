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
import java.util.concurrent.PriorityBlockingQueue;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.nio.file.*;

public class RMIBankServerImp implements RMIBankServer {
    private int serverID; // server ID of this server process
    private ConcurrentHashMap<Integer, Account> accounts = new ConcurrentHashMap<>(); // ConcurrentHashMap that maps unique account IDs to Account objects, representing the bank accounts managed by the server
    private AtomicInteger accountUIDCounter = new AtomicInteger(1); // generate unique IDs for new accounts, starting from 1
    private LamportClock clock = new LamportClock(); // Lamport clock to help with executing the same sequence of operations, using the State Machine Model
    private PriorityBlockingQueue<Request> requests = new PriorityBlockingQueue<>(); // queue that stores the requests for this server process
    private ConcurrentHashMap<Integer, LamportClock> ackRecieved = new ConcurrentHashMap<>(); // concurrent map tracks acknowledgments received from other server replicas.
    private Map<Integer, String> serverIDToAddress = new HashMap<>(); // maps server IDs to their address strings
    private static int port; // post of this server process
    private static String hostname; // hostname of this server process

    public RMIBankServerImp() throws RemoteException {
        super();
    }

    public RMIBankServerImp(String configFilePath, int serverID) throws IOException {
        super();
        this.serverID = serverID;
        loadConfigFile(configFilePath);

        for(int i = 1; i <= 20; i++) { // Create 20 accounts. These will be sequentially given  integer account ID values, starting with 1. It will also initialize the balance of each account to 1000.
            Account account = new Account(i, 1000);
            accounts.put(i, account);
        }
        System.out.println("Initialization is complete! Ready to get requests...");
    }

    public void loadConfigFile(String configFilePath) throws RemoteException, IOException { // reads a configuration file line by line, splitting each line by space. It maps server IDs to their RMI addresses and initializes a new LamportClock for each server ID not matching the current server's ID
        BufferedReader reader = new BufferedReader(new FileReader(configFilePath));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] config = line.split(" ");
            int serverID = Integer.parseInt(config[1]);

            if (serverID != this.serverID) {
                String serverAddress = "//" + config[0] + ":" + config[2] + "/Server_" + config[1];
                serverIDToAddress.put(serverID, serverAddress);
                ackRecieved.put(serverID, new LamportClock()); // initializes a new LamportClock for each server ID not matching the current server's ID
            }
        }
        reader.close();
    }

    public void shutdown() throws RemoteException  { // unbinds the server from the RMI registry and unexports the RMI object, effectively shutting down the server
        System.out.println("Server is terminating...");
        Registry localRegistry = LocateRegistry.getRegistry(hostname, port);
        try{
            localRegistry.unbind("Server_" + serverID);
        }
        catch (Exception e) {
            System.err.println("Server is not bound to the registry, finishing shutdown...");
        }

        UnicastRemoteObject.unexportObject(this, true);
        System.out.println("RMI Server Port Shutdown Completed!");
        System.exit(0);
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

    public void halt(Request r) throws RemoteException, IOException { // HALT command execution
        requests.remove(r);
        int balanceAllAccounts = 0;
        StringBuilder builder = new StringBuilder();

        for (Map.Entry<Integer, Account> entry : accounts.entrySet()) { // calculate the sum of the balance for all 20 accounts and generate a string representing the balance for each account.
            int balance = entry.getValue().getBalance();
            balanceAllAccounts += balance;
            builder.append( "Server-" + serverID + " | Account " + entry.getValue().getUID() + " | Balance = " + entry.getValue().getBalance() + "\n");
        }

        String currentBalanceOf20Accounts = builder.toString();
        
        long totalProcessingTime = 0;
        int serverIDCounter = 0;

        List<String> clientLogEntries = Files.readAllLines(Paths.get("clientLogfile.log"));
        for (String line : clientLogEntries) { // calculate the total server processing time
            if (line.contains("\"RSP\"") && line.contains("SRV-" + serverID)) {
                long serverProcessingTime = Long.parseLong(line.split(" = ")[1]);
                totalProcessingTime += serverProcessingTime;
                serverIDCounter++;
            }
        }

        ServerLogger.haltResultLog(String.valueOf(serverID), "[" + r.getTimestamp() + ", " + r.getSendingServerID() + "]", balanceAllAccounts, requests.size() + "", currentBalanceOf20Accounts, totalProcessingTime / serverIDCounter);
        System.out.println(String.valueOf(serverID) +  " [" + r.getTimestamp() + ", " + r.getSendingServerID() + "] " + " " + balanceAllAccounts + " " +  requests.size());
        shutdown();
    }

    public int getServerID() throws RemoteException {
        return this.serverID;
    }
    public int syncClock(int timestamp){
        clock.update(timestamp);
        return clock.getTime();
    }

    public long clientRequest(Request request) throws RemoteException, MalformedURLException, NotBoundException, IOException {  // handles receiving client request      
        long t0 = System.nanoTime();
        int logicalTime = clock.increment(); // increment Lamport Clock for server
        request.setTimestamp(logicalTime); // set Lamport Clock value of the request to be the local Lamport Clock value of the server
        requests.add(request); 
        ServerLogger.recieveClientLog(String.valueOf(serverID), "[" + logicalTime + ", " + this.serverID + "]", "" + request.getSendingServerID(), request.getRequestType(), " " + request.getSourceAccountUID() + " to " + request.getTargetAccountUID());
        request.SetSendingServerID(this.serverID); // setg the sending server ID to be the server ID of this server process

        cast(request); // multicast request to all other replicas server processes

        processRequest(request);
        long t1 = System.nanoTime();

        return t1 - t0;
    }

    public synchronized void cast(Request request) throws RemoteException, MalformedURLException, NotBoundException {
        for (Map.Entry<Integer, String> entry : serverIDToAddress.entrySet()) { // multicast request to all other replicas server processes
            int replicaID = entry.getKey();
            String replicaAddress = entry.getValue();
            RMIBankServer replica = (RMIBankServer) Naming.lookup(replicaAddress);
            int timestampOther = replica.multicast(request, this.serverID);
            clock.update(timestampOther);
            ackRecieved.get(replicaID).updateNoIncrement(timestampOther);
        }
    }

    public int multicast(Request request, int senderID) throws RemoteException, MalformedURLException, NotBoundException { // this is after I recieve from main server 
        requests.add(request);
        clock.update(request.getTimestamp());
        ServerLogger.recieveMulticastLog(String.valueOf(serverID), "[" + request.getTimestamp() + ", " + request.getSendingServerID() + "]", request.getRequestType(), "");

        return clock.getTime();
    }

    public boolean executeRequestCheck(Request request) throws RemoteException { // check to make sure the request at the head of the queue is one which it had originally received from a client, , and if it is certain that there will be no future request in the system with any smaller timestamp value
        for (Integer replicaID : ackRecieved.keySet()) {
            int timestamp = ackRecieved.get(replicaID).getTime();
            if (timestamp < request.getTimestamp()) { // if request has a greater Lamport Clock 
                return false;
            }
            else if (timestamp == request.getTimestamp() && replicaID < request.getSendingServerID()){ // if request has same Lamport clock value and greater sender server ID
                return false;
            }
        }
        int timestamp = clock.getTime();
        if (timestamp < request.getTimestamp()){ // if request has a greater Lamport Clock 
            return false;
        }
        else if (timestamp == request.getTimestamp() && this.serverID < request.getSendingServerID()){ // if request has same Lamport clock value and greater sender server ID
            return false;
        }
        else {
            return true;
        }
    }

    public void processRequest(Request request) throws MalformedURLException, RemoteException, NotBoundException, IOException { 
        while (!requests.peek().equals(request) || executeRequestCheck(request) == false) {} // wait until request at the head of the queue is one which it had originally received from a client and multicast to the other replicas, and if it is certain that there will be no future request in the system with any smaller timestamp  value
        System.out.println(clock.getTime());
        castExecute(request); // execute the request 
    }

    public synchronized void castExecute(Request request) throws RemoteException, MalformedURLException, NotBoundException, IOException {
        if (request.getRequestType().equals("halt")) { // special request execution case when the request is halt
            for (String replicaAddress : serverIDToAddress.values()) { // loop to make the replicas execute the request 
                RMIBankServer replica = (RMIBankServer) Naming.lookup(replicaAddress);
                // Causes error always when process exits
                try{
                    replica.executeRequest(request);
                }            
                catch (RemoteException e){}
    
            }
            executeRequest(request); // this server process will execute the request
        }
        else { // loop to make the replicas execute the request 
            for (String replicaAddress : serverIDToAddress.values()) {
                RMIBankServer replica = (RMIBankServer) Naming.lookup(replicaAddress);
                replica.executeRequest(request);
            }
            executeRequest(request); // this server process will execute the request
        }
        

    }

    public void executeRequest(Request request) throws RemoteException, IOException {
        while (!requests.peek().equals(request)){} // wait to make sure that  request at the head of the queue is one which it had originally received from a client --> NOTE WE DON'T HAVE TI CHECK IF FUTURE REQUESTS IN THE SYSTEM WILL HAVE SMALLER TIMESTAMPS

        switch (request.getRequestType()) { // execute one of the following requests
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
                halt(request);
                break;
            default:
                System.err.println("ERROR: Unknown Request Type: " + request.getRequestType());
                break;
        }
        ServerLogger.removeLog(String.valueOf(serverID), "[" + request.getTimestamp() + ", " + request.getSendingServerID() + "]");
        requests.remove(request); // remove the request from the queue
    }
    public static void main (String args[]) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: java RMIBankServerImp <serverID> <configFilePath>");
            System.exit(1);
        }

        try {
            hostname = "";
            port = -1;
            int serverID = Integer.parseInt(args[0]);
            String configFilePath = args[1];
            BufferedReader reader = new BufferedReader(new FileReader(configFilePath));
            String line;
            while ((line = reader.readLine()) != null) { // extract the hostname and port of this server process
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


            try {
                registry.bind("Server_" + serverID, bankServerStub);
            }

            catch (Exception e) {
                registry.rebind("Server_" + serverID, bankServerStub);
            }
        
            System.out.println("Server " + serverID + " is ready.");
            System.out.println("//" + hostname + ":" + port + "/Server_" + serverID);
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

