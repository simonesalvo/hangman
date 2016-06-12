package it.simonesalvo.hangman.common.pojo;

import it.simonesalvo.hangman.common.GameStatus;
import it.simonesalvo.hangman.common.MessageType;
import it.simonesalvo.hangman.common.UserType;
import it.simonesalvo.hangman.common.utilis.Utils;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

/**
 * Created by Simone Salvo on 22/06/15.
 * www.simonesalvo.it
 */

@Data
@NoArgsConstructor
public class GamePOJO implements Serializable{

    private static final String COVER_CHAR = "*";

    @NonNull private String keywords;
    @NonNull private Integer minMaxUsers;
    @NonNull private String multicastAddress;
    @NonNull private String multicastPort;

    private String ID;
    private ArrayList<UserPOJO> userList;
    private StringBuilder guesserKeywords;
    private GameStatus status;

    public GamePOJO( @NonNull String keywords,
                     @NonNull UserPOJO user,
                     @NonNull Integer minMaxUsers,
                     @NonNull String multicastAddress,
                     @NonNull String multicastPort){

        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;

        this.ID = Utils.md5(keywords.concat(user.getUser()).concat(String.valueOf(new Date().getTime())));

        this.userList = new ArrayList<>();
        this.keywords = keywords;
        this.userList.add(user);
        this.status = GameStatus.HIDLE;
        this.minMaxUsers = minMaxUsers;
        this.guesserKeywords = new StringBuilder(keywords.length());

        for (int i = 0; i < keywords.length(); ++i){
            this.guesserKeywords.append('*');
        }
    }

    public void addUser(@NonNull UserPOJO user) {
        userList.add(user);
    }

    public MessageType uncoverCharacter(@NonNull String chr) {

        if (chr.isEmpty() || chr.length() > 1){
            return MessageType.INVALID_CHARACTER;
        }

        if (!(keywords.contains(chr)) || (keywords.contains(chr) && guesserKeywords.indexOf(chr) >= 0)){
            return MessageType.UPDATE_STATUS_CHAR_KO;
        }

        Vector<Integer> indices = new Vector<>();
        int tempIndex;
        // letterAttempts++;

        tempIndex = keywords.indexOf(chr);

        while (tempIndex >= 0) {
            indices.add(tempIndex);
            tempIndex = keywords.indexOf(chr, tempIndex + 1);
        }

        if (indices.size() > 0) {
            for (int index : indices) {
                guesserKeywords = guesserKeywords.replace(index, index+1, chr);
            }
        }

        if(guesserKeywords.indexOf(COVER_CHAR)>=0)
            return MessageType.UPDATE_STATUS_CHAR_OK;
        else {
            status = GameStatus.COMPLETED;
            return MessageType.GAME_COMPLETED;
        }
    }


    public UserPOJO getMaster() {
        for (UserPOJO user: userList){
            if (user.getType().equals(UserType.MASTER)){
                return user;
            }
        }
        return null;
    }

}
