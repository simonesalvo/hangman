package it.simonesalvo.hangman.client;

import it.simonesalvo.hangman.common.UserType;
import it.simonesalvo.hangman.common.pojo.GamePOJO;
import it.simonesalvo.hangman.rmicallbck.RmiClientInterface;
import lombok.NonNull;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Simone Salvo on 20/06/15.
 * www.simonesalvo.it
 */

public class RMIClientImpl extends UnicastRemoteObject implements RmiClientInterface, Serializable{

    private final static Logger LOGGER = Logger.getLogger(MulticastManager.class.getName());
    private static final int NOT_FOUND = -1;

    public RMIClientImpl() throws RemoteException {
    }

    @Override
    public void sendAvailableGames(@NonNull GamePOJO[] availableGames) throws RemoteException {

        LOGGER.log(Level.INFO, "Game available RMI called");

        if (availableGames.length == 0) {
            System.out.println("Sorry, No games available! Wait or create a game");
        } else {
            System.out.println("The follows is the list of the available games id \n" +
                    "you can join on of them or create on new game.");

            int masterFound = NOT_FOUND;
            for (GamePOJO game : availableGames) {
                for (int i = 0; i < game.getUserList().size() && masterFound == NOT_FOUND; ++i){
                    if ( game.getUserList().get(i).getType().equals(UserType.MASTER)){
                        masterFound = i;
                    }
                }
                if (masterFound >= 0) {
                    System.out.println(" ---> Available game, the master is: " + game.getUserList().get(masterFound).getUser());
                    masterFound = NOT_FOUND;
                }
            }
        }
    }

}
