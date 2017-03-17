//package raymondmuex;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RaymondMuEx extends UnicastRemoteObject implements RaymondsIF, Serializable, Runnable {

    //information about current node
    static String NodeName = "";
    static String NodeIP = "";
    static int port = 7394;
    static Registry r;
    static RaymondMuEx ray = null;

    //tracker of token, true if process has the token, false otherwise
    static boolean hasToken = false;
    static boolean tokenInUse = false;

    //queue to hold CS access requesting process ID
    static ArrayList<Integer> queue;

    //stores IPAddress of this process's handler process in the form of a String
    static String Holder = "";

    //stores IPAddress of the process that has the shared Critical Section
    static String CSIPAddress = "129.21.22.196"; // glados

    //shared resource that needs a process to enter critical state to update
    static int updateMyCount = 0;

    //Map of all the running processes
    static HashMap<Integer, String> process = new HashMap<>();
    static HashMap<String, String> processName = new HashMap<>();

    /**
     * Default constructor.
     *
     * @throws RemoteException
     */
    RaymondMuEx() throws RemoteException {
        super();
    }

    /**
     * receives request at current process and decides if token has to be
     * forwarded to the requesting process or if the request is to be forwarded
     * to the holder.
     *
     * @param fromNode
     * @throws RemoteException
     */
    public void getRequest(String fromNode) throws RemoteException {
        System.out.println("\n\n*************");
        System.out.println("In getRequest");
        System.out.println("*************");
        System.out.println("new CS access request from " + processName.get(fromNode));

        //***********************
        //BLOCK 1.1
        //Adding process to queue
        //***********************
//        System.out.println("Enter BLOCK 1.1");
        System.out.println("checking if queue is to be updated");
        int PID = 0;
        //add ID of pequesting process to own queue
        for (int k : process.keySet()) {
            if (fromNode.equals(process.get(k))) {
                PID = k;
                break;
            }
        }
        //add to queue
        if (!queue.contains(PID)) {
            System.out.println("queue has been updated. process was not yet present in the queue.");
            queue.add(PID);
            System.out.println("Current contents of the Queue: " + queue);
        } else {
            System.out.println("queue not updated. process was already present in the queue.");
            System.out.println("Current contents of the Queue: " + queue);
        }

        while (tokenInUse) {
            System.out.print("");
        }
        tokenInUse = true;

        //*******************************
        //BLOCK 1.2
        //Forwarding requests if no token
        //*******************************
//        System.out.println("Enter BLOCK 1.2");
        if (!hasToken) {
            System.out.println(NodeName + " does not have the token.");
            System.out.println("forwarding request to " + processName.get(Holder));
            try {
                r = LocateRegistry.getRegistry(Holder, port);
                RaymondsIF handler = (RaymondsIF) r.lookup("process");
                handler.getRequest(NodeIP);
            } catch (NotBoundException | AccessException ex) {
                System.out.println("***********************************************************************");
                System.out.println("Exception while sending request to holder at getRequest method BLOCK 1.2.");
                System.out.println("***********************************************************************");
            }
        } //****************************
        //BLOCK 1.3
        //Forwarding token if hasToken
        //****************************
        //        System.out.println("Enter BLOCK 1.3");
        else if (hasToken) {
            System.out.println(NodeName + " has the token.");
            System.out.println("calling sendToken for " + processName.get(process.get(queue.get(0))));
            sendToken(process.get(queue.get(0)));
        }
    }

    public static void sendToken(String receivingToken) {

        System.out.println("\n\n************");
        System.out.println("In sendToken");
        System.out.println("************");

        //********************************
        //BLOCK 2.1
        //if next node to enter CS is self
        //********************************
//        System.out.println("Enter BLOCK 2.1");
        if (NodeIP.equals(receivingToken)) {
            try {
                ray.getToken(NodeName);
            } catch (RemoteException ex) {
                Logger.getLogger(RaymondMuEx.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            //************************************
            //BLOCK 2.2
            //if next node to enter CS is external
            //************************************
//        System.out.println("Enter BLOCK 2.2");
            try {
                System.out.println("tokento be sent from " + NodeName + " to " + processName.get(receivingToken));
                r = LocateRegistry.getRegistry(receivingToken, port);
                RaymondsIF receiver = (RaymondsIF) r.lookup("process");

                hasToken = false;

                System.out.print("Holder updated from " + processName.get(Holder));
                Holder = receivingToken;
                System.out.println(" to " + processName.get(Holder));

                //send token to receivingToken node
                System.out.println("token sent to " + processName.get(receivingToken));
                receiver.getToken(NodeName);
                System.out.println("removing " + processName.get(Holder) + " from the queue...");
                queue.remove(0);
                System.out.println("Current contents of the Queue: " + queue);

                if (!queue.isEmpty()) {
                    System.out.println("as queue is not empty, forwarding a token request at holder");
                    r = LocateRegistry.getRegistry(Holder, port);
                    RaymondsIF handler = (RaymondsIF) r.lookup("process");
                    handler.getRequest(NodeName);
                }
                tokenInUse = false;
                System.out.println(NodeName + " currently has the Token: " + hasToken);
            } catch (RemoteException | NotBoundException ex) {
                System.out.println("***********************************************************************");
                System.out.println("Exception at sendToken method BLOCK 2.2.");
                System.out.println("***********************************************************************");
            }
        }
        
    }

    @Override
    public void getToken(String fromNode) throws RemoteException {
        tokenInUse = true;
        System.out.println("\n\n************");
        System.out.println("In getToken");
        System.out.println("************");

        if (NodeName.equals(fromNode)) {
            //******************************************
            //BLOCK 3.1
            //if CS accessing node already had the token
            //******************************************
//        System.out.println("Enter BLOCK 3.1");
            System.out.println("Current contents of the Queue: " + queue);
            System.out.println(NodeName + " is next in line to access CS");

            System.out.println("removing " + processName.get(Holder) + " from the queue...");
            queue.remove(0);
            System.out.println("Current contents of the Queue: " + queue);

            System.out.println("Accessing CS and updating Shared Resource");
            //if has shared resource
            if (NodeIP.equals(CSIPAddress)) {
                updateSharedResource(NodeName);
                //if does not have shared resource
            } else {
                try {
                    r = LocateRegistry.getRegistry(CSIPAddress, port);
                    RaymondsIF CSMachine = (RaymondsIF) r.lookup("process");
                    CSMachine.updateSharedResource(NodeName);
                } catch (NotBoundException | AccessException ex) {
                    System.out.println("**********************************************************************");
                    System.out.println("Exception while updating Shared Resource at getToken method BLOCK 3.1.");
                    System.out.println("**********************************************************************");
                }
            }
            tokenInUse = false;
        } else {
            //********************************************
            //BLOCK 3.2
            //if CS accessing node just received the token
            //********************************************
//        System.out.println("Enter BLOCK 3.2");

            hasToken = true;
            System.out.println("token has been received at " + NodeName);

            System.out.print("Holder updated from " + processName.get(Holder));
            Holder = NodeIP;
            System.out.println(" to " + processName.get(Holder));

            String nextNode = process.get(queue.get(0));

            if (!nextNode.equals(NodeIP)) {
                System.out.println(NodeName + " is not next in line to access CS");
                System.out.println("forwarding token to " + processName.get(nextNode));

                sendToken(nextNode);
            } else {
                System.out.println("Current contents of the Queue: " + queue);
                System.out.println(NodeName + " is next in line to access CS");

                System.out.println("removing " + processName.get(Holder) + " from the queue...");
                queue.remove(0);
                System.out.println("Current contents of the Queue: " + queue);

                System.out.println("Accessing CS and updating Shared Resource");
                //if has shared resource
                if (NodeIP.equals(CSIPAddress)) {
                    updateSharedResource(NodeName);
                    //if does not have shared resource
                } else {
                    try {
                        r = LocateRegistry.getRegistry(CSIPAddress, port);
                        RaymondsIF CSMachine = (RaymondsIF) r.lookup("process");
                        CSMachine.updateSharedResource(NodeName);
                    } catch (NotBoundException | AccessException ex) {
                        System.out.println("**********************************************************************");
                        System.out.println("Exception while updating Shared Resource at getToken method BLOCK 3.2.");
                        System.out.println("**********************************************************************");
                    }
                }
                tokenInUse = false;
            }
        }
        System.out.println(NodeName + "currently has the Token: " + hasToken);
    }

    @Override
    public void updateSharedResource(String enteringNode) throws RemoteException {
        //********************************************
        //BLOCK 4.1
        //updating Shared Resource
        //********************************************
//        System.out.println("Enter BLOCK 4.1");
        System.out.println("\n\n**************************************************");
        System.out.println("process " + enteringNode + " entered CS\nupdating Shared Resource");
        updateMyCount++;
        System.out.println("Shared Resource updated count: " + updateMyCount);
        System.out.println("process " + enteringNode + " exited CS");
        System.out.println("**************************************************\n");
    }

    @Override
    public void run() {

        //schedule new requests
        while (true) {

            //random time to wait before requesting for CS Access (10000 <= wait <= 15000)
            int randomWaitTime = 10000 + (int) (Math.random() * 5000);
            try {
                Thread.sleep(randomWaitTime);
            } catch (InterruptedException ex) {
                System.out.println("*********************************************");
                System.out.println("Exception while Random wait at Scheduler Run.");
                System.out.println("*********************************************");
            }
            try {
                System.out.println("\n\n********************\n"
                        + "New request generated for accessing CS\n"
                        + "********************\n");
                getRequest(NodeIP);
            } catch (RemoteException ex) {
                System.out.println("***********************************************************************");
                System.out.println("Exception while sending a new request for CS access from Run.");
                System.out.println("***********************************************************************");
            }
        } // End of while loop
    } // End of run

    /**
     * assign first token to appropriate process.
     */
    public static void giveFirstToken() {
        if (NodeIP.equals(process.get(1))) {
            hasToken = true;
            System.out.println("*** Token Initially Placed at " + NodeName + " ***");
        }
    }

    /**
     * this method populates the map on this process storing information of all
     * the process upon being called.
     */
    public static void updateMap() {
        System.out.println("*** maps have been updated ***");
        process.put(1, "129.21.22.196"); // glados
        process.put(2, "129.21.37.49"); // rhea
        process.put(3, "129.21.37.16"); // newyork
        process.put(4, "129.21.37.23"); // california
        process.put(5, "129.21.37.2"); // iowa
        process.put(6, "129.21.37.20"); // georgia
        process.put(7, "129.21.37.1"); // maine

        processName.put("129.21.22.196", "glados");
        processName.put("129.21.37.49", "rhea");
        processName.put("129.21.37.16", "newyork");
        processName.put("129.21.37.23", "california");
        processName.put("129.21.37.2", "iowa");
        processName.put("129.21.37.20", "eorgia");
        processName.put("129.21.37.1", "maine");
    }

    /**
     * assign appropriate holders.
     */
    public static void updateHolder() {
        System.out.print("*** Holder has been initialized. Current holder is ");
        if (NodeIP.equals(process.get(1))) {
            Holder = NodeIP;
        } else if (NodeIP.equals(process.get(2))) {
            Holder = process.get(1);
        } else if (NodeIP.equals(process.get(3))) {
            Holder = process.get(1);
        } else if (NodeIP.equals(process.get(4))) {
            Holder = process.get(2);
        } else if (NodeIP.equals(process.get(5))) {
            Holder = process.get(2);
        } else if (NodeIP.equals(process.get(6))) {
            Holder = process.get(3);
        } else if (NodeIP.equals(process.get(7))) {
            Holder = process.get(3);
        } else {
            System.out.println("no handler assigned");
        }
        System.out.println(processName.get(Holder) + " ***");
    }

    public static void main(String[] args) {

        //Instantiating the class object
        try {
            ray = new RaymondMuEx();
        } catch (RemoteException ex) {
            System.out.println("*************************************************************");
            System.out.println("Exception while instantiating new RaymondMuEx object at Main.");
            System.out.println("*************************************************************");
        }

        //ID the process
        try {
            InetAddress IP = InetAddress.getLocalHost();
            NodeName = IP.getHostName();
            NodeIP = IP.getHostAddress();
            System.out.println("*** Process Identification Completed ***");
            System.out.println("Process Name: " + NodeName + "\nProcess IP: " + NodeIP);
        } catch (UnknownHostException ex) {
            System.out.println("**********************************************");
            System.out.println("Exception while Process Identification in Main");
            System.out.println("**********************************************");
        }

        //initialize map of this process
        updateMap();

        //give initial token to process 1
        giveFirstToken();

        //initialize handler of this process
        updateHolder();

        //initialize queue for process
        queue = new ArrayList<>();

        // Start registry
        try {
            r = LocateRegistry.createRegistry(port);
            r.rebind("process", new RaymondMuEx());

        } catch (RemoteException ex) {
            System.out.println("*****************************************");
            System.out.println("Exception while Creating Registry in Main");
            System.out.println("*****************************************");
        }

        Thread scheduler;
        try {
            scheduler = new Thread(new RaymondMuEx());
            scheduler.start();
        } catch (RemoteException ex) {
            System.out.println("*************************************************");
            System.out.println("Exception while Creating Scheduler Thread in Main");
            System.out.println("*************************************************");
        }

    }
}
