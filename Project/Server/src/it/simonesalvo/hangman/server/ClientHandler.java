package it.simonesalvo.hangman.server;

import it.simonesalvo.hangman.common.MessageType;
import it.simonesalvo.hangman.common.pojo.GamePOJO;
import it.simonesalvo.hangman.common.pojo.MessagePOJO;
import it.simonesalvo.hangman.common.pojo.UserPOJO;
import it.simonesalvo.hangman.common.utilis.JsonUtils;
import lombok.Data;
import lombok.NonNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Simone Salvo on 26/06/15.
 * www.simonesalvo.it
 */

@Data
public class ClientHandler implements Runnable {

    private final static Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());
    @NonNull private Socket clientSocket;
    @NonNull private Server server;

    @Override
    public void run() {

        LOGGER.log(Level.INFO, "Client handler thread running...");


        DataInputStream in = null;
        try {
            in = new DataInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        DataOutputStream out = null;
        try {
            out = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        String clientMessage = null;
        try {
            if (in != null) {
                clientMessage = in.readUTF();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (clientMessage == null){
            try {
                throw  new IOException();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error in client message reading", e);
            }
        }
        MessagePOJO receivedMsg = JsonUtils.decodeJSON(clientMessage, MessagePOJO.class);

        UserPOJO user = receivedMsg.getUser();

        server.getUserIdsSocketsMap().put(user.getID(), clientSocket);

        MessagePOJO sendingMsg = null;

        if (receivedMsg.getMsgType() == MessageType.OPEN_GAME) {

            if (server.getGameHandler().getGameSize() < Integer.parseInt(server.getGameHandler().getMaxGameNumber())) {

                // Check if the doens't exist a game with the same multicast address
                GamePOJO[] games = new GamePOJO[server.getGameHandler().getGames().size()];
                server.getGameHandler().getGames().values().toArray(games);
                boolean found = false;

                for (int i = 0; i < games.length && !found; ++i) {
                    if(games[i].getMulticastAddress().equals(receivedMsg.getGame().getMulticastAddress())){
                        found = true;
                    }
                }
                if (!found) {
                    server.getGameHandler().addGame(receivedMsg.getGame());
                    server.getAdviser().sendGameList();
                }
                else{
                    sendingMsg = new MessagePOJO(MessageType.MULTICAST_EXISTING);
                }
            }
            else
            {
                sendingMsg = new MessagePOJO(MessageType.EXCEDED_GAME);
            }

        } else if (receivedMsg.getMsgType() == MessageType.JOIN_GAME) {

            //Check if the requested game is existing?
            GamePOJO[] games = new GamePOJO[server.getGameHandler().getGames().size()];
            server.getGameHandler().getGames().values().toArray(games);
            boolean found = false;
            for (int i = 0; i < games.length && !found; ++i){
                if (games[i].getMaster().getUser().equals(receivedMsg.getMasterName())){
                    found = true;
                    server.getGameHandler().addUserGame(receivedMsg.getUser(), receivedMsg.getMasterName());
                    server.getAdviser().sendGameList();
                }
            }
            if (!found){
                //The game ID doesn't exist
                sendingMsg = new MessagePOJO(MessageType.GAME_NOT_EXIST);
            }
        }

        if (sendingMsg == null)
            sendingMsg = new MessagePOJO(MessageType.INITILIAZZATION_ACK);


        try {
            if (out != null) {
                out.writeUTF(JsonUtils.encodeJSON(sendingMsg));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //  check some other messages
        // that can be retrieved till the game is started
        // i.e exit
        try {

            if (in != null) {
                clientMessage = in.readUTF();
            }
            receivedMsg = JsonUtils.decodeJSON(clientMessage, MessagePOJO.class);
            if (receivedMsg.getMsgType().equals(MessageType.CLOSE_GAME)) {
                server.getGameHandler().userClosure(receivedMsg.getUser());
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, " Client \"Exit\" not managed anymore");
        }

    }
}
