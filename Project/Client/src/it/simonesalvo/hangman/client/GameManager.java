package it.simonesalvo.hangman.client;

import it.simonesalvo.hangman.common.MessageType;
import it.simonesalvo.hangman.common.pojo.ConfigPOJO;
import it.simonesalvo.hangman.common.pojo.GamePOJO;
import it.simonesalvo.hangman.common.pojo.MessagePOJO;
import it.simonesalvo.hangman.common.pojo.UserPOJO;
import it.simonesalvo.hangman.common.utilis.JsonUtils;
import lombok.Data;
import lombok.NonNull;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Simone Salvo on 20/06/15.
 * www.simonesalvo.it
 */


@Data
public class GameManager {

    @NonNull private ConfigPOJO config;
    @NonNull private Socket socket;

    private final static Logger LOGGER = Logger.getLogger(GameManager.class.getName());

    public void createGame(@NonNull String keywords,
                           @NonNull UserPOJO user,
                           @NonNull Integer minMaxUsers) throws IOException {

        LOGGER.log(Level.INFO, "Sending create game request to the server...");

        MessagePOJO socketMessage = new MessagePOJO(MessageType.OPEN_GAME);
        socketMessage.setUser(user);
        socketMessage.setGame(new GamePOJO(keywords, user, minMaxUsers,
                config.getMulticastAddress(),
                config.getMulticastPort()));

        OutputStream outToServer = socket.getOutputStream();
        DataOutputStream out = new DataOutputStream(outToServer);

        LOGGER.log(Level.INFO, "Sending 'create game' message thought socket...");
        out.writeUTF(JsonUtils.encodeJSON(socketMessage));

    }

    public void joinGame(@NonNull UserPOJO user, @NonNull String masterName) throws IOException {

        LOGGER.log(Level.INFO, "Send join game request to the server...");

        MessagePOJO socketMessage = new MessagePOJO(MessageType.JOIN_GAME);
        socketMessage.setUser(user);
        socketMessage.setMasterName(masterName);

        OutputStream outToServer = socket.getOutputStream();
        DataOutputStream out = new DataOutputStream(outToServer);

        LOGGER.log(Level.INFO, "Sending 'join game' message thought socket...");
        out.writeUTF(JsonUtils.encodeJSON(socketMessage));

    }

    public void logout(@NonNull UserPOJO user) throws IOException {
        LOGGER.log(Level.INFO, "Sending create game request to the server...");

        MessagePOJO socketMessage = new MessagePOJO(MessageType.LOGOUT);
        socketMessage.setUser(user);

        OutputStream outToServer = socket.getOutputStream();
        DataOutputStream out = new DataOutputStream(outToServer);

        LOGGER.log(Level.INFO, "Sending 'create game' message thought socket...");
        out.writeUTF(JsonUtils.encodeJSON(socketMessage));
    }
}
