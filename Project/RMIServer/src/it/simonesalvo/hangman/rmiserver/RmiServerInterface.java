package it.simonesalvo.hangman.rmiserver;

import it.simonesalvo.hangman.common.pojo.UserPOJO;
import it.simonesalvo.hangman.common.CUSTOM_RETURN;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by Simone Salvo on 17/06/15.
 * www.simonesalvo.it
 */
public interface RmiServerInterface extends Remote {
        CUSTOM_RETURN createAccount(UserPOJO usr) throws RemoteException;
        CUSTOM_RETURN login(UserPOJO usr) throws RemoteException;
        CUSTOM_RETURN logout(UserPOJO usr) throws RemoteException;
}
