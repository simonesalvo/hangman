package it.simonesalvo.hangman.client;

import com.sun.istack.internal.Nullable;
import it.simonesalvo.hangman.common.C;
import it.simonesalvo.hangman.common.MessageType;
import it.simonesalvo.hangman.common.UserType;
import it.simonesalvo.hangman.common.pojo.GamePOJO;
import it.simonesalvo.hangman.common.pojo.MessagePOJO;
import it.simonesalvo.hangman.common.utilis.JsonUtils;
import lombok.Data;
import lombok.NonNull;
import lombok.Synchronized;
import org.jasypt.util.text.BasicTextEncryptor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Simone Salvo on 28/06/15.
 * www.simonesalvo.it
 */

@Data
public class MulticastManager {

    private final static Logger LOGGER = Logger.getLogger(MulticastManager.class.getName());

    private static final String RETRY_MANAGER = "RETRY_MANAGER";
    private static final String MASTER_EXIT_SENDING_MANAGER = "MASTER_EXIT_SENDING_MANAGER";
    private static final Integer MAXIMUM_ATTEMPT = 5;
    private static final String GAME_TIME_CLOSURE = "GAME_TIME_CLOSURE";

    @NonNull private MulticastSocket multicastSocket;
    @NonNull private GamePOJO game;
    //    @NonNull private UserPOJO user;
    @NonNull private Client client;

    @Nullable private BasicTextEncryptor encryptors;
    @Nullable private Boolean gameClosure;
    @Nullable private Thread gameTimeClosureT;

    @Synchronized Boolean getGameClosure(){
        return this.gameClosure;
    }

    @Synchronized void setGameClosure(@NonNull Boolean val){
        this.gameClosure = val;
    }

//    synchronized Boolean getGameClosureSync(){
//        return this.gameClosure;
//    }

//    synchronized void setGameClosureSync(@NonNull Boolean val){
//        this.gameClosure = val;
//    }

    public MulticastManager(@NonNull GamePOJO game,
                            @NonNull Client client){

        this.setGameClosure(Boolean.FALSE);
        this.encryptors =  new BasicTextEncryptor();
        this.encryptors.setPassword(game.getID());

        this.client = client;
//        this.serverObject = serverObject;

        LOGGER.log(Level.INFO, "Client game multicast manager running...");

        this.game = game;
//        this.user = user;

        try {

            this.multicastSocket = new MulticastSocket(Integer.parseInt(this.game.getMulticastPort()));
            this.multicastSocket.joinGroup(InetAddress.getByName(this.game.getMulticastAddress()));

            LOGGER.log(Level.INFO, "The user just join in the multicast group");

            if (client.getUserManager().getUserPOJO().getType() == UserType.MASTER){
                masterMulticastHandler();
            }else if (client.getUserManager().getUserPOJO().getType() == UserType.GUESSER){
                guesserMulticastReceiver();
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Multicast manager forced closure...");
        }

    }

    private void guesserMulticastReceiver() throws IOException {

        LOGGER.log(Level.INFO, "Guesser multicast handler running...");

        Integer remainingAttempt = MAXIMUM_ATTEMPT;

        GuesserManager guesserManager = new GuesserManager(this);
        // Running a new thread that manage the sending and the retry
        Thread retryHandler = new Thread(guesserManager);

        retryHandler.setName(RETRY_MANAGER);
        retryHandler.start();
        client.getClientThreads().add(retryHandler);
        do{

            // Create a buffer of bytes, which will be used to store
            // the incoming bytes containing the information from the server.
            // 2048 bytes should be enough.

            byte[] buf = new byte[2048];
            DatagramPacket msgPacket = new DatagramPacket(buf, buf.length);
            String msg;

            multicastSocket.receive(msgPacket);
            // Preparing json message and encrypting
            msg =  encryptors.decrypt(new String(buf, 0, buf.length, "UTF-8"));

            MessagePOJO receivedMsg = JsonUtils.decodeJSON(msg, MessagePOJO.class);

            if (receivedMsg.getUser().getType().equals(UserType.MASTER)) {
                LOGGER.log(Level.INFO, "A message has been received");

                if (receivedMsg.getMsgType().equals(MessageType.NO_MORE_GAME_ATTEMPS)) {
                    System.out.println("No more attempts, the keyword was:" + receivedMsg.getGame().getKeywords());
                    retryHandler.interrupt();
                    guesserManager.closeGame();
                } else if (receivedMsg.getMsgType().equals(MessageType.GAME_COMPLETED)) {
                    System.out.println("Game completed, the keyword was:" + receivedMsg.getGame().getKeywords());
                    if (receivedMsg.getAckingID().equals(client.getUserManager().getUserPOJO().getID())) {
                        System.out.println("The winning letter was yours! You have won!");
                    }
                    retryHandler.interrupt();
                    guesserManager.closeGame();
                }
                else if (receivedMsg.getMsgType().equals(MessageType.UPDATE_STATUS_CHAR_KO) ||
                        receivedMsg.getMsgType().equals(MessageType.UPDATE_STATUS_CHAR_OK)){

                    System.out.println("Guesser keywords: "+receivedMsg.getGame().getGuesserKeywords());

                    if (receivedMsg.getAckingID().equals(client.getUserManager().getUserPOJO().getID())) {
                        if (receivedMsg.getMsgType().equals(MessageType.UPDATE_STATUS_CHAR_KO)){
                            remainingAttempt -=1;
                            if (remainingAttempt <= 0){
                                retryHandler.interrupt();
                                guesserManager.closeGame();
                            }
                        }

                        System.out.println("Remaining try: "+remainingAttempt);

                        guesserManager.setAckReceived(Boolean.TRUE);
                        LOGGER.log(Level.INFO, "The received message is an ack for the previous sent character");
                    }
                }
            }
            else if(receivedMsg.getMsgType().equals(MessageType.GAME_LEAVING)) {
                System.out.println("The master is leaving" + receivedMsg.getGame().getKeywords());
                retryHandler.interrupt();
                guesserManager.closeGame();
            }


        }while(!this.getGameClosure());
    }

    /**
     * The function work as following:
     * - Once a guesser-message is retrieved if it comes from a guesser and it is a proposal
     *  calculate the new guessed keyword and send the update in multicast
     */
    private void masterMulticastHandler() throws IOException {
        LOGGER.log(Level.INFO, "Master multicast handler running...");
        Integer globalTry = MAXIMUM_ATTEMPT * (game.getUserList().size() - 1);

        // Running a new thread that manage the exit sending message
        final MasterManager masterManager = new MasterManager(this);
        final Thread masterManagerT = new Thread(masterManager);
        final MulticastManager multicastManager= this;
        // Running a new thread that manage a long game closure timeout
        gameTimeClosureT =(new Thread() {
            public void run() {
                try {
                    Thread.sleep(C.GAME_LONG_TIMEOUT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                LOGGER.log(Level.INFO, "Game time expired. The game is going to be closed...");
                if (!multicastManager.getGameClosure()) {
                    masterManagerT.interrupt();
                    try {
                        masterManager.closeGame();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        gameTimeClosureT.setName(GAME_TIME_CLOSURE);
        gameTimeClosureT.start();
        client.getClientThreads().add(gameTimeClosureT);

        masterManagerT.setName(MASTER_EXIT_SENDING_MANAGER);
        masterManagerT.start();
        client.getClientThreads().add(masterManagerT);

        while (!this.getGameClosure()) {

            // Receive the information and print it.

            byte[] buf = new byte[2048];
            DatagramPacket msgPacket = new DatagramPacket(buf, buf.length);

            multicastSocket.receive(msgPacket);
            // Preparing json message and encrypting
            String msg = encryptors.decrypt(new String(buf, 0, buf.length, "UTF-8"));

            MessagePOJO receivedMsg = JsonUtils.decodeJSON(msg, MessagePOJO.class);

            if (receivedMsg.getUser().getType().equals(UserType.GUESSER)) {
                if (receivedMsg.getMsgType().equals(MessageType.PROPOSAL)) {

                    // Update guesser keywords
                    MessageType res = game.uncoverCharacter(receivedMsg.getCharacter());

                    if (res.equals(MessageType.UPDATE_STATUS_CHAR_KO)) {
                        globalTry--;
                        if (globalTry <= 0) {
                            res = MessageType.NO_MORE_GAME_ATTEMPS;
                        }
                    }

                    MessagePOJO masterAnswer = new MessagePOJO(res);

                    masterAnswer.setCharacter(receivedMsg.getCharacter());
                    masterAnswer.setGame(game);
                    masterAnswer.setUser(client.getUserManager().getUserPOJO());
                    masterAnswer.setAckingID(receivedMsg.getUser().getID());

                    // Preparing json message and encrypting
                    String jsonMsg = encryptors.encrypt(JsonUtils.encodeJSON(masterAnswer));

                    msgPacket = new DatagramPacket(jsonMsg.getBytes("UTF-8"),
                            jsonMsg.getBytes("UTF-8").length,
                            InetAddress.getByName(game.getMulticastAddress()),
                            Integer.parseInt(this.game.getMulticastPort()));

                    multicastSocket.send(msgPacket);

                    if (res.equals(MessageType.NO_MORE_GAME_ATTEMPS) || res.equals(MessageType.GAME_COMPLETED)) {

                        gameTimeClosureT.interrupt();
                        masterManagerT.interrupt();
                        masterManager.closeGame();

                    }
                }
            }
        }
    }
}
