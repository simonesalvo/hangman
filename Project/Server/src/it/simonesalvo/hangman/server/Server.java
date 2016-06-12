package it.simonesalvo.hangman.server;

import it.simonesalvo.hangman.common.RMI_NAMES;
import it.simonesalvo.hangman.common.pojo.ConfigPOJO;
import it.simonesalvo.hangman.common.utilis.JsonUtils;
import lombok.Data;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Data
public class Server {
    private final static Logger LOGGER = Logger.getLogger(Server.class.getName());

    private static final String SERVER_CONFIG_FILE = "server_conf.json";
    private static final String CONNECTION_HANDLER_THREAD = "CONNECTION HANDLER THREAD";
    private static final String ADVISER_THREAD = "ADVISER THREAD";
    private static final String EXIT = "EXIT";
    private Boolean isServerActive;

    private ConfigPOJO config;

    private ArrayList<Thread> serverThreads;
    private ArrayList<Socket> serverSockets;
    private ServerSocket socket;

    private Map<String, Socket> userIdsSocketsMap;
    private GameHandler gameHandler;
    private RMIServerImpl remoteObj;
    private Registry registry;
    private Adviser adviser;
    private ConnectionHandler connectionHandler;

    private UserManager userManager;

    public Server() throws IOException, NotBoundException {
        isServerActive = Boolean.TRUE;
        LOGGER.log(Level.INFO, "Instantiate server ");
        serverThreads = new ArrayList<>();
        serverSockets = new ArrayList<>();
        userIdsSocketsMap = new HashMap<>();

        JSONParser parser = new JSONParser();

        Object obj = null;
        try {
            obj = parser.parse(new InputStreamReader(
                    new FileInputStream(SERVER_CONFIG_FILE), "UTF8")); //);new FileReader(SERVER_CONFIG_FILE));
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        JSONObject jsonObject =  (JSONObject) obj;

        if (jsonObject != null) {
            config = JsonUtils.decodeJSON(jsonObject.toString(), ConfigPOJO.class);
        }
        gameHandler = new GameHandler(config.getMaxGameNumber());

        System.setProperty("java.rmi.server.hostname", config.getServerSocketAddress());
        registry = LocateRegistry.createRegistry(Integer.parseInt(config.getRMIServerPort()));
        adviser = new Adviser(this);
//        adviser.setServer(this);
        userManager = new UserManager(adviser);

        // Create a new service named SERVER_RMI
        remoteObj = new RMIServerImpl(userManager);
        registry.rebind(RMI_NAMES.SERVER_RMI, remoteObj);

        socket = new ServerSocket(Integer.parseInt(config.getServerSocketPort()));

        LOGGER.log(Level.INFO, "RMI server binding completed");

        Thread adviserT = (new Thread(adviser));
        adviserT.setName(ADVISER_THREAD);
        adviserT.start();

        // Creation of a thread that manage the connections
        connectionHandler = new ConnectionHandler(this);
        Thread connectionHandlerT = new Thread(connectionHandler);
        connectionHandlerT.setName(CONNECTION_HANDLER_THREAD);
        connectionHandlerT.start();

        serverThreads.add(adviserT);
        serverThreads.add(connectionHandlerT);

        System.out.println("Server ready...");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
        do{
            System.out.println("Type exit to switch off it");
        }
        while(!br.readLine().toUpperCase().equals(EXIT));
        closeServer();

    }

    private void closeServer() throws IOException, NotBoundException {
        isServerActive = Boolean.FALSE;
        registry.unbind(RMI_NAMES.SERVER_RMI);
        // this will also remove us from the RMI runtime
        UnicastRemoteObject.unexportObject(remoteObj, true);

        socket.close();

        //each thread has to be interrupted
        if (serverThreads!= null) {
            for (Thread t : serverThreads) {
                if (!t.isInterrupted()) {
                    t.interrupt();
                }
            }
        }

        //each socket has to be closed
        if (serverSockets != null){
            for (Socket s : serverSockets){
                if (s.isConnected() || s.isBound()){
                    s.close();
                }
            }
        }
    }

    public static void main(String args[]) {

        LOGGER.log(Level.SEVERE, "Server running...");
        Server s = null;
        try {
            s = new Server();
        } catch (IOException | NotBoundException e) {
            LOGGER.log(Level.SEVERE, "Have a look on 147 Server0", e);
        }finally {
            try {
                if (s != null) {
                    s.closeServer();
                }
            } catch (IOException | NotBoundException e) {
                LOGGER.log(Level.SEVERE, "Have a look on 153 Server");
            }
        }

    }
}
