package com.aiwannafly.gui_torrent.torrent.client.tracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class TrackerCommandHandler implements Runnable {
    private final Socket clientSocket;
    private final TrackerServer server;
    private final static int PEER_ID_LENGTH = 20;
    public final static String GET_COMMAND = "get";
    public final static String EXIT_COMMAND = "exit";
    public final static String SET_LISTENING_SOCKET = "listen-port";
    public final static String SHOW_COMMAND = "show";
    private final static String PEERS_LIST = "peers";
    private final static String PEER_ID = "peer_id";
    private final static String WRONG_COMMAND_MSG = "Wrong command";
    private final static String INCOMPLETE_COMMAND_MSG = "Incomplete command";;
    private PrintWriter out = null;
    private BufferedReader in = null;

    public TrackerCommandHandler(Socket socket, TrackerServer server) {
        this.clientSocket = socket;
        this.server = server;
    }

    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            while (true) {
                String command = in.readLine();
                if (null == command) {
                    break;
                }
                System.out.printf("Sent from the client: %s\n", command);
                if (command.equals(EXIT_COMMAND)) {
                    synchronized (server) {
                        server.getClients().remove(clientSocket);
                        for (String torrentFileName: server.getPeerPorts().keySet()) {
                            server.getPeerPorts().get(torrentFileName).remove(clientSocket);
                        }
                    }
                    break;
                }
                String message = getResponse(command);
                if (message != null) {
                    out.println(message);
                    out.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getResponse(String command) {
        String[] words = command.split(" ");
        switch (words[0]) {
            case GET_COMMAND -> {
                if (words.length < 2) {
                    return INCOMPLETE_COMMAND_MSG;
                }
                if (PEER_ID.equals(words[1])) {
                    return generatePeerId();
                }
                return WRONG_COMMAND_MSG;
            }
            case SHOW_COMMAND -> {
                if (words.length < 2) {
                    return INCOMPLETE_COMMAND_MSG;
                }
                if (PEERS_LIST.equals(words[1])) {
                    if (words.length < 3) {
                        return INCOMPLETE_COMMAND_MSG;
                    }
                    String torrentFileName = words[2];
                    StringBuilder message = new StringBuilder();
                    message.append("Peers: ");
                    synchronized (server) {
                        Map<Socket, Integer> fileSeeds = server.getPeerPorts().get(torrentFileName);
                        for (Socket client : fileSeeds.keySet()) {
                            if (client == clientSocket) {
                                continue;
                            }
                            int port = fileSeeds.get(client);
                            message.append(port).append(" ");
                        }
                    }
                    return message.toString();
                }
                return WRONG_COMMAND_MSG;
            }
            case SET_LISTENING_SOCKET -> {
                if (words.length < 3) {
                    return INCOMPLETE_COMMAND_MSG;
                }
                int port = Integer.parseInt(words[1]);
                String torrentFileName = words[2];
                synchronized (server) {
                    server.getPeerPorts().computeIfAbsent(torrentFileName, k -> new HashMap<>());
                    if (server.getPeerPorts().get(torrentFileName).containsKey(clientSocket)) {
                        server.getPeerPorts().get(torrentFileName).replace(clientSocket, port);
                    } else {
                        server.getPeerPorts().get(torrentFileName).put(clientSocket, port);
                    }
                }
            }
        }
        return WRONG_COMMAND_MSG;
    }

    private String generatePeerId() {
        StringBuilder peerId = new StringBuilder();
        peerId.append("PEER_AIW_");
        peerId.append(clientSocket.getRemoteSocketAddress());
        peerId.append(peerId.toString().hashCode());
        return peerId.substring(0, PEER_ID_LENGTH);
    }
}
