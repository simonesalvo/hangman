package it.simonesalvo.hangman.client;

import it.simonesalvo.hangman.common.C;
import it.simonesalvo.hangman.common.MessageType;
import it.simonesalvo.hangman.common.pojo.MessagePOJO;
import it.simonesalvo.hangman.common.utilis.JsonUtils;
import lombok.Data;
import lombok.NonNull;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Simone Salvo on 24/07/15.
 * www.simonesalvo.it
 */

@Data
/**
 * The meaning of the master manager is to have a thread that can allow
 * the client-master to close the game by typing the exit command
 *
 */
public class MasterManager implements Runnable {

    @NonNull private final static Logger LOGGER = Logger.getLogger(MasterManager.class.getName());
    @NonNull private MulticastManager multicastManager;

    @Override
    public void run() {

        String msg = null;
        System.out.println("Type exit to get out of the game");
        byte[] inputData = new byte[1024];

        do {
            try {
                Thread.sleep(1000);
                System.in.read(inputData, 0, System.in.available());
                msg = new String(inputData, "UTF-8");
            } catch (InterruptedException | IOException e) {
                LOGGER.log(Level.SEVERE, "Have a look on 45 masterManager");
            }
        }while(!msg.toUpperCase().substring(0, 4).equals(C.EXIT));


        if (!multicastManager.getGameClosure()) {

            multicastManager.getGameTimeClosureT().interrupt();
            Thread.currentThread().interrupt();
            try {
                this.closeGame();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    public void closeGame() throws IOException {
        if (!multicastManager.getGameClosure()) {
            multicastManager.setGameClosure(Boolean.TRUE);

            MessagePOJO sndMessage = new MessagePOJO((MessageType.GAME_LEAVING));
            sndMessage.setUser(multicastManager.getClient().getUserManager().getUserPOJO());

            // Preparing json message and encrypting
            String jsonMsg = multicastManager.getEncryptors().encrypt(JsonUtils.encodeJSON(sndMessage));

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

}
