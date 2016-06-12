package it.simonesalvo.hangman.server;

import it.simonesalvo.hangman.common.CUSTOM_RETURN;
import it.simonesalvo.hangman.common.pojo.UserPOJO;
import it.simonesalvo.hangman.rmiserver.RmiServerInterface;
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

public class RMIServerImpl extends UnicastRemoteObject implements RmiServerInterface, Serializable{

    private final static Logger LOGGER = Logger.getLogger(RMIServerImpl.class.getName());

   private UserManager userManager;

    public RMIServerImpl(@NonNull UserManager userManager)  throws RemoteException  {
        this.userManager = userManager;
    }

    @Override
    public CUSTOM_RETURN createAccount(@NonNull UserPOJO usr) throws RemoteException {
        LOGGER.log(Level.INFO, "Creating account: server-RMI");
        return userManager.createAccount(usr);
    }

    @Override
    public CUSTOM_RETURN login(@NonNull UserPOJO usr) throws RemoteException {
        LOGGER.log(Level.INFO, "Login: server-RMI");
        return userManager.login(usr);
    }

    @Override
    public CUSTOM_RETURN logout(@NonNull UserPOJO usr) throws RemoteException {
        LOGGER.log(Level.INFO, "Logout: server-RMI");
        return userManager.logout(usr);
    }
}
