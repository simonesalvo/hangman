package it.simonesalvo.hangman.common.pojo;

import it.simonesalvo.hangman.common.UserType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.io.Serializable;

/**
 * Created by Simone Salvo on 22/06/15.
 * www.simonesalvo.it
 */

@Data
@AllArgsConstructor
public class UserPOJO implements Serializable{

    @NonNull private String ID;
    @NonNull private String user;
    @NonNull private String password;
    @NonNull private String clientAddress;
    @NonNull private String clientSocketPort;
    @NonNull private String rmiClientPort;
    @NonNull private UserType type;

    public UserPOJO() {}
}
