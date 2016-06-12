package it.simonesalvo.hangman.client;

import it.simonesalvo.hangman.common.RMI_NAMES;
import it.simonesalvo.hangman.common.pojo.ConfigPOJO;
import it.simonesalvo.hangman.common.utilis.JsonUtils;
import it.simonesalvo.hangman.rmiserver.RmiServerInterface;
import lombok.Data;
import lombok.NonNull;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.Socket;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Simone Salvo on 22/06/15.
 * www.simonesalvo.it
 */
@Data
public class Client  {

    private final static String CLIENT_CONFIG_FILE ="/client_conf_";
    private final static String CLIENT_CONFIG_FILE_EXT =".json";
    private final static Logger LOGGER = Logger.getLogger(Client.class.getName());

    private RMIClientImpl rmiClientObj;
    private Registry registry;
    private ConfigPOJO configObj;
    private RmiServerInterface serverObj;
    private UserManager userManager;
    private GameManager gameManager;
    private Socket socket;
    private ArrayList<Thread> clientThreads;
    private Client() {

        LOGGER.log(Level.INFO, "Client object created");
        try {

            clientThreads = new ArrayList<>();
            configObj = createConfigObj();
            if (configObj == null){
                throw new NullPointerException();
            }
            serverObj = this.connectWithServerRMI(configObj);

            // Be careful. Don't remove or move the following code line. Don't create the registry twice.
            registry = LocateRegistry.createRegistry(Integer.parseInt(configObj.getRMIClientPort()));

            if (serverObj != null) {
                new MenuManager(this).start();
            }

            this.clean();

        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Something went wrong...", e);
        } finally {
            try {
                this.clean();
            } catch (IOException | NotBoundException e) {
                e.printStackTrace();
            }
            System.out.println("Closure almost done...");
        }
    }


    public static void main(String[] args) {

        LOGGER.log(Level.INFO, "Client.java main running ...");
        new Client();
    }

    public ConfigPOJO createConfigObj() throws UnsupportedEncodingException {
        JSONParser parser = new JSONParser();

        String filePath = new File("").getAbsolutePath();

        // get config file name
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));

        String configFileNumber = null;

        System.out.println("Please type your client config file number");
        try {
            configFileNumber = br.readLine();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Config file reading error", e);
        }

        Object obj = null;
        try {
            String fileName = filePath.concat(CLIENT_CONFIG_FILE + configFileNumber + CLIENT_CONFIG_FILE_EXT);
            obj = parser.parse(new InputStreamReader(
                    new FileInputStream(fileName), "UTF8"));//new FileReader(fileName));
        } catch (IOException | ParseException e) {
            LOGGER.log(Level.SEVERE, "Client config file in Client.java ", e);
        }

        JSONObject jsonObject =  (JSONObject) obj;

        LOGGER.log(Level.INFO, "Client config file correctly read");
        if (jsonObject != null) {
            return JsonUtils.decodeJSON(jsonObject.toString(), ConfigPOJO.class);
        }else {
            return null;
        }
    }

    private RmiServerInterface connectWithServerRMI(@NonNull ConfigPOJO config) throws RemoteException, NotBoundException {

        Registry myRegistry = null;

        try {

            myRegistry = LocateRegistry.getRegistry(config.getServerSocketAddress(),
                    Integer.parseInt(config.getRMIServerPort()));
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Connection with server rmi ");
        }

        return  (RmiServerInterface) (myRegistry != null ? myRegistry.lookup(RMI_NAMES.SERVER_RMI) : null);
    }

    public void closeCallback(){
        LOGGER.log(Level.INFO, "Client callback closing...");
        try {
            if (registry != null) {
                registry.unbind(RMI_NAMES.CLIENT_RMI);
            }
        } catch (RemoteException | NotBoundException ignored) {}
        try {
            if (rmiClientObj != null) {
                UnicastRemoteObject.unexportObject(rmiClientObj, true);
            }
        } catch (NoSuchObjectException ignored) { }
    }

    public void startCallback() {

        try {

            System.setProperty("java.rmi.server.hostname", configObj.getClientAddress());
            // create a new service named myMessage
            rmiClientObj = new RMIClientImpl();
            registry.rebind(RMI_NAMES.CLIENT_RMI, rmiClientObj);
            LOGGER.log(Level.INFO, " RMI client callback started");

        }
        catch (Exception e)
        {
            LOGGER.log(Level.INFO, "Starting RMI client callback error");
        }

    }

    public void clean() throws IOException, NotBoundException {
        LOGGER.log(Level.INFO, "Client cleaning...");
        this.closeCallback();

        if (socket!=null) {
            socket.close();
        }

        //each thread has to be interrupted
        if (clientThreads!= null) {
            for (Thread t : clientThreads) {
                if (!t.isInterrupted()) {
                    t.interrupt();
                }
            }
        }

    }
}
