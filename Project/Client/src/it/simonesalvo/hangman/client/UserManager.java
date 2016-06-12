package it.simonesalvo.hangman.client;

import it.simonesalvo.hangman.common.CUSTOM_RETURN;
import it.simonesalvo.hangman.common.UserType;
import it.simonesalvo.hangman.common.pojo.ConfigPOJO;
import it.simonesalvo.hangman.common.pojo.UserPOJO;
import it.simonesalvo.hangman.common.utilis.Utils;
import it.simonesalvo.hangman.rmiserver.RmiServerInterface;
import lombok.Data;
import lombok.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Simone Salvo on 30/07/15.
 * www.simonesalvo.it
 */

@Data
public class UserManager {
    @NonNull private RmiServerInterface serverObj;
    @NonNull private ConfigPOJO configObj;
    @NonNull private Client client;
    private UserPOJO userPOJO;

    private final static Logger LOGGER = Logger.getLogger(UserManager.class.getName());
    public CUSTOM_RETURN createAccount() throws IOException {

        LOGGER.log(Level.INFO, "Sending create account request to the server...");

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));

        System.out.println("Please type username");
        String user;
        user = br.readLine();

        if (user == null){
            System.out.println("User error");
            return CUSTOM_RETURN.ERROR;
        }

        System.out.println("Please type password");
        String pwd;
        pwd = br.readLine();

        if (pwd == null){
            System.out.println("Password error");
            return CUSTOM_RETURN.ERROR;
        }

        this.userPOJO = new UserPOJO(
                Utils.md5(user),
                user,
                Utils.md5(pwd),
                configObj.getClientAddress(),
                configObj.getClientSocketPort(),
                configObj.getRMIClientPort(),
                UserType.UNDEFINED);
        client.startCallback();
        return serverObj.createAccount(userPOJO);
    }

    public CUSTOM_RETURN login() throws IOException {

        LOGGER.log(Level.INFO, "Sending login request to the server...");
        String user;
        String pwd;

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));

        try {

            System.out.println("Please type username");
            user = br.readLine();

            if (user == null){
                System.out.println("User error");
                return CUSTOM_RETURN.ERROR;
            }

            System.out.println("Please type password");
            pwd = br.readLine();

            if (pwd == null){
                System.out.println("Password error");
                return CUSTOM_RETURN.ERROR;
            }
        }
        finally {
//            br.close();
        }

        this.userPOJO = new UserPOJO(
                Utils.md5(user),
                user,
                Utils.md5(pwd),
                configObj.getClientAddress(),
                configObj.getClientSocketPort(),
                configObj.getRMIClientPort(),
                UserType.UNDEFINED);

        client.startCallback();

        return serverObj.login(userPOJO);

    }

}