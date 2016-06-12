package it.simonesalvo.hangman.common.pojo;

import lombok.Data;

/**
 * Created by Simone Salvo on 30/06/15.
 * www.simonesalvo.it
 */

@Data
public class ConfigPOJO {

    private String serverSocketAddress;
    private String serverSocketPort;
    private String clientAddress;
    private String clientSocketPort;
    private String multicastPort;
    private String multicastAddress;
    private String RMIServerPort;
    private String RMIClientPort;
    private String maxGameNumber;
}
