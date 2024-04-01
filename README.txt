Group members: Michael and Wyatt (andr0821 & rasmu984)

# Running One Server Replica Process ON SEPERATE MACHINES (the machines are listed in the one_server_config_file):
```
$ javac *.java

$ java RMIBankClient 24 "one_server_config_file.txt"

$ java RMIBankServerImp 0 "one_server_config_file.txt"
```
# Running Three Server Replica Process ON SEPERATE MACHINES (the machines are listed in the three_server_config_file):
```
$ javac *.java

$ java RMIBankClient 24 "three_server_config_file.txt"

$ java RMIBankServerImp 0 "three_server_config_file.txt"

$ java RMIBankServerImp 1 "three_server_config_file.txt"

$ java RMIBankServerImp 2 "three_server_config_file.txt"
```
# Running Five Server Replica Process ON SEPERATE MACHINES (the machines are listed in the five_server_config_file):
```
$ javac *.java

$ java RMIBankClient 24 "five_server_config_file.txt"

$ java RMIBankServerImp 0 "five_server_config_file.txt"

$ java RMIBankServerImp 1 "five_server_config_file.txt"

$ java RMIBankServerImp 2 "five_server_config_file.txt"

$ java RMIBankServerImp 3 "five_server_config_file.txt"

$ java RMIBankServerImp 4 "five_server_config_file.txt"
```
# Client Logging [within clientLogfile.log]: 
All of the client logging follow the specifications of the Assignment #5 PDF.
The client logging (both request and response logging) happens in the file ClientLogger.java.
These methods in ClientLogger.java are called in the RMIBankClient.java file. 
For all log responses in clientLogfile.log the average value of the service processing 
time is recorded. Then, when the HALT operation is sent by the client the average 
value of the response times observed by the clients is recorded at the very end of clientLogfile.log.

# Server Logging [within serverLogfile_{SERVER_ID}.log]:
All of the server logging follow the specifications of the Assignment #5 PDF.
The server logging (client recieve, multicast recieve, request processing, and halt request logging) happens in the file ServerLogger.java.
These methods in ServerLogger.java are called in the RMIBankServerImp.java file. 
All log responses are recorded in serverLogfile_{SERVER_ID}.log, where SERVER_ID is the server ID of the replica. 

# LamportClock.java
Contains all of the logic for the lamport clock operations such as increment, update, and get lamport time.

# Request.java
Contains the class depicting the format of the request objects that we send betweenn the client and replicas and replicas to each other. 

# Account.java
Contains all o fthe logic representing the bank accounts.

# Additional Notes
Note that the account balancess for all accounts in the servers are printed at the end of serverLogfile_{SERVER_ID}.log, where SERVER_ID is the server ID of the replica. 
Also, the last line contains the sum of the balance in all 20 accounts.
Also, the timestamp in the Request.java class refers to the Lamport Clock value!

# Performance Measurements
    * One Server Replica Process: 
        * Average Processing Time for the Server 0 = 151244472
        * Average Response Time for the Client = 151244472

    * Three Server Replica Process: 
        * Average Processing Time for the Server 0 = 520117530
        * Average Processing Time for the Server 1 = 520117530
        * Average Processing Time for the Server 2 = 520117530
        * Average Response Time for the Client = 524655105

    * Five Server Replica Process: 
        * Average Processing Time for the Server 0 = 642960963
        * Average Processing Time for the Server 1 = 642960963
        * Average Processing Time for the Server 2 = 642960963
        * Average Processing Time for the Server 3 = 642960963
        * Average Processing Time for the Server 4 = 642960963
        * Average Response Time for the Client = 638863212