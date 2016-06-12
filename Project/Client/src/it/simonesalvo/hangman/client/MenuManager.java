package it.simonesalvo.hangman.client;

import com.sun.istack.internal.Nullable;
import it.simonesalvo.hangman.common.C;
import it.simonesalvo.hangman.common.CUSTOM_RETURN;
import it.simonesalvo.hangman.common.MessageType;
import it.simonesalvo.hangman.common.UserType;
import it.simonesalvo.hangman.common.pojo.MessagePOJO;
import it.simonesalvo.hangman.common.utilis.JsonUtils;
import lombok.Data;
import lombok.NonNull;

import java.io.*;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Simone Salvo on 30/07/15.
 * www.simonesalvo.it
 */

@Data
public class MenuManager {

    private final static Logger LOGGER = Logger.getLogger(MenuManager.class.getName());

    private final static String CHOICE_MENU  =  "Welcome player! \n"+
            "-------Press 1 create a new account \n"+
            "-------Press 2 login \n" +
            "-------Type 3 to exit \n" +
            "-------Enter your choice: ";

    private final static String MAIN_MENU = "-------Press 1 to create a game\n" +
            "-------Press 2 to join a game \n" +
            "-------Type 3 to exit \n";

    private final static String GET_KEYWORD = "Please type the for-guesser keywords";
    private static final String SOCKET_TIMEOUT_MANAGER = "SOCKET_TIMEOUT_MANAGER";
    private static final String EXIT_MANAGER_THREAD = "EXIT_MANAGER_THREAD";

    @NonNull Client client;

    @Nullable private Thread socketManagerT;
    @Nullable private Thread exitManagerT;

    public void start() throws IOException, NotBoundException {
        LOGGER.log(Level.INFO, "Showing menus...");
        CUSTOM_RETURN res;

        do {
            do {
                res = userMenu();
            }
            while (!((res.equals(CUSTOM_RETURN.ACCOUNT_CREATED)) ||
                    ((res.equals(CUSTOM_RETURN.AUTHENTICATED))) ||
                    res.equals(CUSTOM_RETURN.EXIT)));

            if (!res.equals(CUSTOM_RETURN.EXIT)) {
                do {
                    res = gameMenu();
                }
                while (!res.equals(CUSTOM_RETURN.ERROR) && !res.equals(CUSTOM_RETURN.TO_LOG_IN));
            }

        }while(!res.equals(CUSTOM_RETURN.EXIT));

        if (res.equals(CUSTOM_RETURN.EXIT)){
            LOGGER.log(Level.INFO, "Client closure...");
        }
    }

    private CUSTOM_RETURN userMenu() throws IOException, NotBoundException {

        LOGGER.log(Level.INFO, "Showing starter menu, user will be able to login/create account...");

        client.clean();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));

        System.out.println(CHOICE_MENU);
        String choice;

        choice = br.readLine();
        if (choice == null){
            return CUSTOM_RETURN.ERROR;
        }
//        client.startCallback();
        client.setUserManager(new UserManager(client.getServerObj(), client.getConfigObj(), client));

        switch (choice)
        {
            case "1": {
                try {
                    LOGGER.log(Level.INFO, "Create account selected");
                    return client.getUserManager().createAccount();
                } catch (IOException e) {
                    return CUSTOM_RETURN.ERROR;
                }
            }
            case "2": {
                try {
                    LOGGER.log(Level.INFO, "Login selected");
                    return client.getUserManager().login();
                } catch (IOException e) {
                    return CUSTOM_RETURN.ERROR;
                }
            }
            case "3":{
                LOGGER.log(Level.INFO, "Exit selected");
                return CUSTOM_RETURN.EXIT;
            }
            default: {
                return CUSTOM_RETURN.ERROR;
            }
        }
    }

    private CUSTOM_RETURN gameMenu() throws IOException {
        LOGGER.log(Level.INFO, "Showing main menu, user will be able to create a game or to log in a opened game");

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
        String choice;

        System.out.println(MAIN_MENU);
        choice = br.readLine();

        if (choice == null){
            return CUSTOM_RETURN.ERROR;
        }

        client.setSocket(new Socket(client.getConfigObj().getServerSocketAddress(),
                Integer.parseInt(client.getConfigObj().getServerSocketPort())));
        client.setGameManager(new GameManager(client.getConfigObj(), client.getSocket()));
        socketManagerT = (new Thread() {
            public void run() {
                LOGGER.log(Level.INFO,"Socket time out thread manager running...");

                try {
                    Thread.sleep(C.LONG_TIMEOUT);
                    if (client.getSocket().isBound() || client.getSocket().isConnected()) {
                        client.getSocket().close();
                    }
                } catch (InterruptedException | IOException e) {
                    LOGGER.log(Level.SEVERE, "Socket time-out manager thread closed!");
                }
            }
        });

        socketManagerT.setName(SOCKET_TIMEOUT_MANAGER);
        socketManagerT.start();
        client.getClientThreads().add(socketManagerT);

        switch (choice){
            case "1": {

                LOGGER.log(Level.INFO, "Creating game option selected");

                System.out.println(GET_KEYWORD);

                String keywords = br.readLine();

                if (keywords == null){
                    return CUSTOM_RETURN.ERROR;
                }
                System.out.println("Please type the number of needed guesser");
                Integer minMaxUser = Integer.parseInt(br.readLine());

                client.getUserManager().getUserPOJO().setType(UserType.MASTER);
                client.getGameManager().createGame(keywords, client.getUserManager().getUserPOJO(), minMaxUser);
                break;
            }
            case "2": {

                LOGGER.log(Level.INFO, "Joining game option selected");

                System.out.println("Please type the master's name game you want join");
                String masterName = br.readLine();

                if (masterName == null){
                    return CUSTOM_RETURN.ERROR;
                }
                client.getUserManager().getUserPOJO().setType(UserType.GUESSER);
                client.getGameManager().joinGame(client.getUserManager().getUserPOJO(), masterName);
                break;
            }
            case "3":{
                LOGGER.log(Level.INFO, "Returning to the login menu");
                socketManagerT.interrupt();
                client.getSocket().close();
                return CUSTOM_RETURN.TO_LOG_IN;
            }
            default: return CUSTOM_RETURN.ERROR;
        }

        LOGGER.log(Level.INFO, "Getting answer from a server after the creation game/join game option selected");

        exitManagerT = (new Thread() {
            public static final String EXIT = "EXIT";

            public void run() {

                String msg = null;
                byte[] inputData = new byte[1024];


                System.out.println("Type exit if you don't want wait anymore...");
                do {
                    try {
                        Thread.sleep(1000);
                        System.in.read(inputData, 0, System.in.available());
                        msg = new String(inputData, "UTF-8");
                    } catch (InterruptedException | IOException e) {
                        LOGGER.log(Level.SEVERE, "Have a look on 214 menumanager");
                    }
                }while(msg != null || !msg.toUpperCase().substring(0,4).equals(EXIT));

                try {
                    client.getGameManager().logout(client.getUserManager().getUserPOJO());
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Have a look on 221 menumanager");
                }

                if (client.getSocket().isBound() || client.getSocket().isConnected()) {
                    try {
                        client.getSocket().close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    socketManagerT.interrupt();
                }

            }
        });

        exitManagerT.setName(EXIT_MANAGER_THREAD);
        exitManagerT.start();
        client.getClientThreads().add(exitManagerT);
        InputStream inFromServer = client.getSocket().getInputStream();
        LOGGER.log(Level.INFO, "Waiting for a server message");
        DataInputStream in = new DataInputStream(inFromServer);

        MessagePOJO serverAnswer = JsonUtils.decodeJSON(in.readUTF(), MessagePOJO.class);

        return manageServerAnswer(serverAnswer, client.getSocket());

    }

    private CUSTOM_RETURN manageServerAnswer(@NonNull MessagePOJO serverAnswer,
                                             @NonNull Socket socket) throws IOException {


        // Once the user is logged inside a game (by game creation or game login)
        // it will get an ack by the server
        // then the user will wait again for a start game message if everything went
        // correctly or a error message
        if (serverAnswer.getMsgType().equals(MessageType.INITILIAZZATION_ACK)) {
            LOGGER.log(Level.INFO, "Initialization ack received...");

            InputStream inFromServer = socket.getInputStream();
            DataInputStream in = new DataInputStream(inFromServer);
            serverAnswer = JsonUtils.decodeJSON(in.readUTF(), MessagePOJO.class);

            return this.manageServerAnswer(serverAnswer, socket);
        }
        else if (serverAnswer.getMsgType().equals(MessageType.GAME_NOT_EXIST)) {
            System.out.println("The game you are trying to log in doesn't exist.");
            exitManagerT.interrupt();
            socketManagerT.interrupt();
            socket.close();
            return CUSTOM_RETURN.TO_LOG_IN;
        }
        else if (serverAnswer.getMsgType().equals(MessageType.EXCEDED_GAME)){
            System.out.println("You are not able to create another game." +
                    " Game limit number reached.");
            exitManagerT.interrupt();
            socketManagerT.interrupt();
            socket.close();
            return CUSTOM_RETURN.TO_LOG_IN;
        }
        else if (serverAnswer.getMsgType().equals(MessageType.MULTICAST_EXISTING)){
            System.out.println("The multicast address you would like user" +
                    " is used in another game. Please logout, change it in your" +
                    " configuration file and login again.");
            exitManagerT.interrupt();
            socketManagerT.interrupt();
            socket.close();
            return CUSTOM_RETURN.TO_LOG_IN;
        }
        else if (serverAnswer.getMsgType().equals(MessageType.START_GAME)){
            LOGGER.log(Level.INFO, "Game starting message received...");
            exitManagerT.interrupt();
            socketManagerT.interrupt();
            new MulticastManager(serverAnswer.getGame(), client);
            socket.close();
        }
        else if (serverAnswer.getMsgType().equals(MessageType.GAME_ANNULLED)){
            LOGGER.log(Level.INFO, "Game annulled message received...");
            exitManagerT.interrupt();
            socketManagerT.interrupt();
            socket.close();
            return CUSTOM_RETURN.TO_LOG_IN;
        }
        return CUSTOM_RETURN.TO_LOG_IN;
    }
}
