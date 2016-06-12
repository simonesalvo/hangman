package it.simonesalvo.hangman.server;

import com.google.gson.reflect.TypeToken;
import it.simonesalvo.hangman.common.C;
import it.simonesalvo.hangman.common.CUSTOM_RETURN;
import it.simonesalvo.hangman.common.RMI_NAMES;
import it.simonesalvo.hangman.common.pojo.UserPOJO;
import it.simonesalvo.hangman.common.utilis.JsonUtils;
import it.simonesalvo.hangman.rmicallbck.RmiClientInterface;
import lombok.NonNull;
import lombok.Synchronized;
import org.json.JSONException;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Simone Salvo on 20/06/15.
 * www.simonesalvo.it
 */

public class UserManager implements Serializable{
    private final static Logger LOGGER = Logger.getLogger(UserManager.class.getName());
    private static final String USR_JSON = "/usr.json";
    private ArrayList<UserPOJO> connectedUsers;
    private Map<String, RmiClientInterface> clientObjMaps;
    private Adviser adviser;

    public UserManager(@NonNull Adviser adviser){
        this.adviser = adviser;
        connectedUsers = new ArrayList<>();
        clientObjMaps = new HashMap<>();
    }

    @Synchronized public Map<String, RmiClientInterface> getClientObjMaps() {
        return clientObjMaps;
    }


    /**
     * Validate username with regular expression
     * @param usr username for validation
     * @return true valid username, false invalid username
     */
    private boolean validateUsr(@NonNull String usr){
        //TODO future constraints
        return usr.length() > C.USER_MIN_SIZE;
    }

    /**
     * Validate password with regular expression
     * @param pwd password for validation
     * @return true valid password, false invalid password
     */
    private boolean validatePwd(@NonNull String pwd){
        //TODO future constraints
        return pwd.length() > C.PWD_MIN_SIZE;
    }

    private CUSTOM_RETURN createUsr(@NonNull UserPOJO user) throws JSONException, IOException, ParseException {

        LOGGER.log(Level.INFO, "Creating user...");

        String filePath = new File("").getAbsolutePath();
        String filenamePath = filePath.concat(USR_JSON);

        java.util.ArrayList<UserPOJO> usrList = new ArrayList<>();
        java.lang.reflect.Type type = new TypeToken<ArrayList<UserPOJO>>() {}.getType();

        if (Files.exists(Paths.get(filenamePath))) {

            try {
                String jsonString = readFile(filenamePath);

                if (!jsonString.isEmpty()) {
                    usrList.addAll((Collection<? extends UserPOJO>) JsonUtils.decodeJSON(jsonString, type));

                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            LOGGER.log(Level.INFO, "Deleting old json file...");
            Files.delete(Paths.get(filenamePath));

        } else {
            if (!(new File(filenamePath).createNewFile())){
                return CUSTOM_RETURN.INVALID_USER;
            }
        }

        usrList.add(user);
        getClientObjMaps().put(user.getID(), connectWithClientRMI(user));

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(filenamePath), "utf-8"))) {
            writer.write(JsonUtils.encodeJSON(usrList));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "User manager error in user creation", e);
        }

        getConnectedUsers().add(user);
        adviser.sendGameList();
        return  CUSTOM_RETURN.ACCOUNT_CREATED;
    }

    private boolean usrExist(@NonNull String usr){

        LOGGER.log(Level.INFO, "Checking if user exists");

        String filePath = new File("").getAbsolutePath();
        String filenamePath = filePath.concat(USR_JSON);
        java.lang.reflect.Type type = new TypeToken<ArrayList<UserPOJO>>() {}.getType();
        if (!Files.exists(Paths.get(filenamePath)))
            return false;
        ArrayList<UserPOJO> usrList = new ArrayList<>();

        try {
            String jsonString = readFile(filenamePath);

            if (!jsonString.isEmpty()) {
                usrList.addAll((Collection<? extends UserPOJO>) JsonUtils.decodeJSON(jsonString, type));

                for (UserPOJO user : usrList) {
                    if (user.getUser().equals(usr))
                        return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    String readFile(@NonNull String fileName) throws IOException {
        BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(fileName), "UTF8"));
        try {
            StringBuilder sb = new StringBuilder();

            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(" ");
                line = br.readLine();
            }

            return sb.toString();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "User manager error");
            return null;
        }
        finally {
            br.close();
        }
    }

    public CUSTOM_RETURN createAccount(@NonNull UserPOJO usr) throws RemoteException
    {

        LOGGER.log(Level.INFO, "Creating account...");
        // Check username
        if (!validateUsr(usr.getUser()))
            return CUSTOM_RETURN.INVALID_USER;

        // Check password
        if (!validatePwd(usr.getPassword()))
            return CUSTOM_RETURN.INVALID_PWD;

        // Check if already exist
        if(usrExist(usr.getUser()))
            return CUSTOM_RETURN.USR_EXISTING;

        // If the user isn't already registered then register it
        try {
            return createUsr(usr);
        } catch (IOException | ParseException | JSONException e) {
            e.printStackTrace();
        }

        return CUSTOM_RETURN.INVALID_USER;
    }

    public CUSTOM_RETURN login(@NonNull UserPOJO usr) throws RemoteException {
        LOGGER.log(Level.INFO, "Login...");
        String filePath = new File("").getAbsolutePath();
        String filenamePath = filePath.concat(USR_JSON);
        java.lang.reflect.Type type = new TypeToken<ArrayList<UserPOJO>>() {}.getType();
        if (!Files.exists(Paths.get(filenamePath)))
            return CUSTOM_RETURN.ERROR;

        ArrayList<UserPOJO> usrList = new ArrayList<>();

        try {
            String jsonString = readFile(filenamePath);

            if (!jsonString.isEmpty()) {
                usrList.addAll((Collection<? extends UserPOJO>) JsonUtils.decodeJSON(jsonString, type));
                for (UserPOJO user : usrList) {
                    if (user.getUser().equals(usr.getUser()) && user.getPassword().equals(usr.getPassword())) {

                        // User successfully connected
                        getConnectedUsers().remove(user);
                        getConnectedUsers().add(user);
                        getClientObjMaps().remove(user.getID());
                        getClientObjMaps().put(user.getID(), connectWithClientRMI(usr));
                        adviser.sendGameList();
                        return CUSTOM_RETURN.AUTHENTICATED;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return CUSTOM_RETURN.ACCESS_DENIED;
    }

    private RmiClientInterface connectWithClientRMI(@NonNull UserPOJO user) {

        try {

            Registry myRegistry = LocateRegistry.getRegistry(user.getClientAddress(),
                    Integer.parseInt(user.getRmiClientPort()));

            // search for myMessage service
            if (myRegistry != null) {
                    return (RmiClientInterface) myRegistry.lookup(RMI_NAMES.CLIENT_RMI);
            }

        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Connection with client rmi error", e);
        }

        return null;
    }

    public CUSTOM_RETURN logout(@NonNull UserPOJO usr) throws RemoteException {
        LOGGER.log(Level.INFO, "Logout...");
        getConnectedUsers().remove(usr);
        getClientObjMaps().remove(usr.getID());
        return CUSTOM_RETURN.LOGOUT_SUCCESSED;
    }

    @Synchronized public ArrayList<UserPOJO> getConnectedUsers() {
        return connectedUsers;
    }
}
