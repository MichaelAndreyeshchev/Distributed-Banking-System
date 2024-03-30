import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIBankServer extends Remote {
    void loadConfigFile(String configFilePath) throws RemoteException, IOException;
    int createAccount() throws RemoteException;
    String deposit(int sourceAcountUID, int amount) throws RemoteException;
    int getBalance(int sourceAcountUID) throws RemoteException;
    String transfer(int sourceAcountUID, int targetAccountUID, int amount) throws RemoteException;
    void shutdown() throws RemoteException;
    void halt(Request r) throws RemoteException;
    int getServerID() throws RemoteException;
    int syncClock(int timestamp) throws RemoteException;
    String clientRequest(Request request) throws RemoteException, MalformedURLException, NotBoundException;
    int multicast(Request request, int senderID) throws RemoteException, MalformedURLException, NotBoundException;
    boolean executeRequestCheck(Request request) throws RemoteException;
    void executeRequest(Request request) throws RemoteException;
}
