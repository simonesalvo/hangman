package it.simonesalvo.hangman.rmicallbck;

import com.sun.javafx.beans.annotations.NonNull;
import it.simonesalvo.hangman.common.pojo.GamePOJO;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by Simone Salvo on 17/06/15.
 * www.simonesalvo.it
 */

public interface RmiClientInterface extends Remote {

        void sendAvailableGames(@NonNull GamePOJO[] availableGames) throws RemoteException;
}
