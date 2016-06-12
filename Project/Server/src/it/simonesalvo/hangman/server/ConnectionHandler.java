package it.simonesalvo.hangman.server;

import com.sun.istack.internal.Nullable;
import it.simonesalvo.hangman.common.C;
import lombok.Data;
import lombok.NonNull;

import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Simone Salvo on 25/06/15.
 * www.simonesalvo.it
 */

@Data
public class ConnectionHandler implements Runnable{

    private final static Logger LOGGER = Logger.getLogger(ConnectionHandler.class.getName());
    private final static String CLIENT_HANDLER = "CLIENT_HANDLER";
    private static final String CLIENT_SOCKET_TIMING_THREAD = "CLIENT_SOCKET_TIMING_THREAD";

    @NonNull Server server;
    @Nullable Socket clientSocket;
    @Nullable Thread clientHandler;

    @Override
    public void run() {

        try {

            LOGGER.log(Level.INFO, "Connection handler thread running...");
            while (server.getIsServerActive()) {
                clientSocket = server.getSocket().accept();

                if (server.getIsServerActive() && (clientSocket.isConnected() || clientSocket.isBound())) {
                    server.getServerSockets().add(clientSocket);

                    clientHandler = new Thread(new ClientHandler(clientSocket, server));
                    clientHandler.setName(CLIENT_HANDLER);

                    Thread clientSocketTimingThread = (new Thread() {

                        public void run() {
                            try {
                                LOGGER.log(Level.INFO, "Running server-client socket timing thread...");
                                Thread.sleep(C.LONG_TIMEOUT);
                                clientHandler.interrupt();
                                clientSocket.close();
                            } catch (InterruptedException | IOException e) {
                                LOGGER.log(Level.SEVERE, "Server client socket timing thread forced closure...");
                            }
                        }
                    });

                    clientSocketTimingThread.setName(CLIENT_SOCKET_TIMING_THREAD);
                    clientSocketTimingThread.start();
                    server.getServerThreads().add(clientSocketTimingThread);

                    clientHandler.start();
                    server.getServerThreads().add(clientHandler);
                }
                else if (clientSocket.isBound() || clientSocket.isConnected()){
                    clientSocket.close();
                }
            }
        }
        catch (Exception e) {
            LOGGER.log(Level.INFO, "ConnectionHandler exception ");
        }
    }


}
