package it.simonesalvo.hangman.client;

import com.sun.istack.internal.Nullable;
import it.simonesalvo.hangman.common.C;
import it.simonesalvo.hangman.common.MessageType;
import it.simonesalvo.hangman.common.pojo.MessagePOJO;
import it.simonesalvo.hangman.common.utilis.JsonUtils;
import lombok.Data;
import lombok.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Simone Salvo on 23/07/15.
 * www.simonesalvo.it
 */

@Data
public class GuesserManager implements Runnable {

    private final static Logger LOGGER = Logger.getLogger(GuesserManager.class.getName());

    @NonNull private MulticastManager multicastManager;

    @Nullable private Boolean ackReceived;

    synchronized void setAckReceived(@NonNull Boolean val){
        this.ackReceived = val;
    }

    synchronized Boolean getAckReceived(){
        return this.ackReceived;
    }

    @Override
    public void run() {
        this.setAckReceived(Boolean.TRUE);


        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String chose = null;
        String jsonMsg;
        int resendCounter;

        try {

            do {

                System.out.println("Please type a character to send or exit to get out of the game");

                if (br != null) {
                    chose = br.readLine();
                }
                if (chose != null) {
                    if (chose.length() == 1 && this.getAckReceived()) {

                        LOGGER.log(Level.INFO, "Ack received. The user will be able to send the character");

                        MessagePOJO sendingMessage = new MessagePOJO((MessageType.PROPOSAL));
                        sendingMessage.setUser((multicastManager.getClient().getUserManager().getUserPOJO()));
                        sendingMessage.setCharacter(chose);


                        // Preparing json message and encrypting
                        jsonMsg = multicastManager.getEncryptors().encrypt(JsonUtils.encodeJSON(sendingMessage));

                        DatagramPacket msgPacket = new DatagramPacket(jsonMsg.getBytes("UTF-8"),
                                jsonMsg.getBytes("UTF-8").length,
                                InetAddress.getByName(multicastManager.getGame().getMulticastAddress()),
                                Integer.parseInt(multicastManager.getGame().getMulticastPort()));

                        LOGGER.log(Level.INFO, "Sending character...");

                        multicastManager.getMulticastSocket().send(msgPacket);
                        this.setAckReceived(Boolean.FALSE);

                        resendCounter = 0;
                        // Retry policy
                        do {

                            Thread.sleep(C.ACK_TIMEOUT);

                            if(!multicastManager.getGameClosure()) {
                                if (!this.getAckReceived()) {
                                    System.out.println("Previous ack not received, resending...");
                                    multicastManager.getMulticastSocket().send(msgPacket);
                                }
                            }

                            resendCounter ++;

                            if (resendCounter == C.MAXIMUM_RESEND){
                                LOGGER.log(Level.INFO, "The master is not answering anymore. Closing.");
                                closeGame();
                            }

                        } while (!this.getAckReceived() && !multicastManager.getGameClosure() && !(resendCounter == 5));
                    }
                    else if (chose.toUpperCase().equals(C.EXIT)) {
                        LOGGER.log(Level.INFO, "Exit typed, game closure...");
                        closeGame();
                    }
                }
            }
            while(!multicastManager.getGameClosure());
        } catch (IOException | InterruptedException e ) {
            LOGGER.log(Level.SEVERE, "Guesser manager forced closure...");
        }
    }

    /**
     * @throws IOException
     */
    public void closeGame() throws IOException {

        multicastManager.setGameClosure(Boolean.TRUE);

        MessagePOJO sndMessage = new MessagePOJO((MessageType.GAME_LEAVING));
        sndMessage.setUser((multicastManager.getClient().getUserManager().getUserPOJO()));
        String jsonMsg;

        // Preparing json message and encrypting
        jsonMsg = multicastManager.getEncryptors().encrypt(JsonUtils.encodeJSON(sndMessage));

        DatagramPacket msgPacket = new DatagramPacket(jsonMsg.getBytes("UTF-8"),
                jsonMsg.getBytes("UTF-8").length,
                InetAddress.getByName(multicastManager.getGame().getMulticastAddress()),
                Integer.parseInt(multicastManager.getGame().getMulticastPort()));
        LOGGER.log(Level.INFO, "Sending exit message...");


        multicastManager.getMulticastSocket().send(msgPacket);
        multicastManager.getMulticastSocket().leaveGroup(InetAddress.getByName(multicastManager.getGame().getMulticastAddress()));
        multicastManager.getClient().getServerObj().logout((multicastManager.getClient().getUserManager().getUserPOJO()));

    }
}
