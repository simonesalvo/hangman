package it.simonesalvo.hangman.server;

import it.simonesalvo.hangman.common.GameStatus;
import it.simonesalvo.hangman.common.MessageType;
import it.simonesalvo.hangman.common.pojo.GamePOJO;
import it.simonesalvo.hangman.common.pojo.MessagePOJO;
import it.simonesalvo.hangman.common.pojo.UserPOJO;
import it.simonesalvo.hangman.common.utilis.JsonUtils;
import lombok.Data;
import lombok.NonNull;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

/**
 * Created by Simone Salvo on 27/06/15.
 * www.simonesalvo.it
 */

@Data
public class Adviser implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(Adviser.class.getName());
    private static final int SLEEP_TIME = 5000;

    @NonNull private Server server;

    public void sendGameList() {
        // for each user check isn't busy and then send the games list.
        boolean userBusy = false;

        GamePOJO[] availableGames = new GamePOJO[server.getGameHandler().getGames().size()];
        server.getGameHandler().getGames().values().toArray(availableGames);

        if ((availableGames.length > 0)) {

            if (!server.getUserManager().getConnectedUsers().isEmpty()) {
                for (UserPOJO connectedUser : server.getUserManager().getConnectedUsers()) {
                    for (GamePOJO game : availableGames) {
                        for (UserPOJO user : game.getUserList()) {
                            if (user.getID().equals(connectedUser.getID())) {
                                userBusy = true;
                            }
                        }
                    }

                    if (!userBusy) {
                        // sending games list
                        try {
                            server.getUserManager().getClientObjMaps().get(connectedUser.getID()).sendAvailableGames(availableGames);
                        } catch (RemoteException e) {
                            server.getUserManager().getClientObjMaps().remove(connectedUser.getID());
                            LOGGER.log(Level.SEVERE, "Something sending available games list went wrong");
                        }
                    }
                    userBusy = false;
                }
            }
        }
    }

    @Override
    public void run() {

        LOGGER.log(Level.INFO, "Adviser thread running...");
        LOGGER.log(Level.INFO, "Getting all games and and start it if the number of needed guesser is reached");

        while(server.getIsServerActive()) {
            try {

                sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                LOGGER.log(Level.INFO, "Adviser thread ");
            }
            if (server.getIsServerActive()) {
                GamePOJO[] games = new GamePOJO[server.getGameHandler().getGames().size()];
                server.getGameHandler().getGames().values().toArray(games);
                // for each  game check if there is a new game ready to start, if any,
                // send to all the linked users a start_game message
                for (GamePOJO game : games) {
                    if (game.getUserList() != null) {
                        if (game.getUserList().size() == game.getMinMaxUsers()) {
                            if (game.getStatus().equals(GameStatus.HIDLE)) {
                                MessagePOJO sendingMessage = new MessagePOJO(MessageType.START_GAME);
                                LOGGER.log(Level.INFO, "A new game is ready to start...");

                                for (UserPOJO user : game.getUserList()) {

                                    Socket userSocket = server.getUserIdsSocketsMap().get(user.getID());
                                    sendingMessage.setGame(game);

                                    try {
                                        new DataOutputStream(userSocket.getOutputStream()).writeUTF((JsonUtils.encodeJSON(sendingMessage)));
                                        server.getGameHandler().getGames().remove(game.getID());
                                        server.getUserIdsSocketsMap().remove(user.getID());

                                        int found = -1;
                                        for (int i=0; found == -1 && i < server.getUserManager().getConnectedUsers().size();++i){
                                            if (server.getUserManager().getConnectedUsers().get(i).getID().equals(user.getID())){
                                                found = i;
                                            }
                                        }
                                        if (found != -1){
                                            server.getUserManager().getConnectedUsers().remove(found);
                                        }
//                                        server.getUserManager().getConnectedUsers().remove(user);
                                        userSocket.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            else if (game.getStatus().equals(GameStatus.ANNULLED)){
                                MessagePOJO sendingMessage = new MessagePOJO(MessageType.GAME_ANNULLED);
                                LOGGER.log(Level.INFO, "A new game is ready to be annulled...");
                                for (UserPOJO user : game.getUserList()) {

                                    Socket userSocket = server.getUserIdsSocketsMap().get(user.getID());

                                    try {
                                        new DataOutputStream(userSocket.getOutputStream()).writeUTF((JsonUtils.encodeJSON(sendingMessage)));
                                        server.getGameHandler().getGames().remove(game.getID());
                                        server.getUserIdsSocketsMap().remove(user.getID());
                                        int found = -1;
                                        for (int i=0; found == -1 && i < server.getUserManager().getConnectedUsers().size();++i){
                                            if (server.getUserManager().getConnectedUsers().get(i).getID().equals(user.getID())){
                                                found = i;
                                            }
                                        }
                                        if (found != -1){
                                            server.getUserManager().getConnectedUsers().remove(found);
                                        }

                                        userSocket.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
//
//    private  GamePOJO[] getAvailableGames() {
////        ArrayList<GamePOJO> games = new ArrayList<>();
////        for (GamePOJO game : server.getGameHandler().getGames().values()) {
////            if (game.getStatus().equals(GameStatus.HIDLE)) {
////                if (game.getUserList().size() < game.getMinMaxUsers()) {
////                    games.add(game);
////                }
////            }
////        }
//
////        return availableGames;
//    }
}
