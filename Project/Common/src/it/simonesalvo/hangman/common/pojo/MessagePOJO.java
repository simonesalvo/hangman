package it.simonesalvo.hangman.common.pojo;

import it.simonesalvo.hangman.common.MessageType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.io.Serializable;

/**
 * Created by Simone Salvo on 22/06/15.
 * www.simonesalvo.it
 */

@Data
@NoArgsConstructor
public class MessagePOJO implements Serializable{

    // Type of message
    @NonNull private MessageType msgType;

    // User sending the message
    private UserPOJO user;

    // Attempting character
    private String character;

    // Relative game
    private GamePOJO game;

    // Game mater's name, used when the gamePOJO is not set
    private  String masterName;

    // User ID that sent the last character
    private String ackingID;

    public MessagePOJO(MessageType msgType){
        this.msgType = msgType;
    }
}
