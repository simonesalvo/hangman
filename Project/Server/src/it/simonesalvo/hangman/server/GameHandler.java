package it.simonesalvo.hangman.server;

import it.simonesalvo.hangman.common.GameStatus;
import it.simonesalvo.hangman.common.UserType;
import it.simonesalvo.hangman.common.pojo.GamePOJO;
import it.simonesalvo.hangman.common.pojo.UserPOJO;
import lombok.Data;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Simone Salvo on 26/06/15.
 * www.simonesalvo.it
 */

@Data
public class GameHandler {

    private final static Logger LOGGER = Logger.getLogger(GameHandler.class.getName());

    @NonNull private String maxGameNumber;
    private HashMap<String, GamePOJO> games;


    public GameHandler(@NonNull String maxGame){
        games = new HashMap<>();
        maxGameNumber = maxGame;
    }

    public synchronized int getGameSize() {
        return games.size();
    }

    public synchronized void addGame(@NonNull GamePOJO game) {
        LOGGER.log(Level.INFO, "Adding a new game");
        games.put(game.getID(), game);

    }
    public synchronized void removeGame(@NonNull String gameID) {
        LOGGER.log(Level.INFO, "Removing a game...");
        games.remove(gameID);
        LOGGER.log(Level.INFO, "Game removed");
    }

    public void addUserGame(@NonNull UserPOJO user, @NonNull String masterName) {
        String gameID = null;

        GamePOJO[] gameList = new GamePOJO[games.size()];
        games.values().toArray(gameList);

        boolean found = false;
        for (int i = 0; i < gameList.length && !found; ++ i){
            if (gameList[i].getMaster().getUser().equals(masterName)){
                gameID = gameList[i].getID();
                found = true;
            }
        }

        if (gameID != null) {
            games.get(gameID).addUser(user);
        }
    }

    public void userClosure(@NonNull UserPOJO user) {
        for (GamePOJO game : games.values()){
            boolean found = false;
            ArrayList<UserPOJO> users = game.getUserList();
            for (int i = 0; i < users.size() && !found; ++i){
                if (users.get(i).getID().equals(user.getID()))
                {
                    if (user.getType().equals(UserType.MASTER)){
                        games.get(game.getID()).setStatus(GameStatus.ANNULLED);
                    }
                    else if(user.getType().equals(UserType.GUESSER)){
                        games.get(game.getID()).getUserList().remove(user);
                    }
                }
            }
        }
    }
}
