package com.aiwannafly.gui_torrent.torrent.client.tracker;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TrackerServer {
    public static final int PORT = 1000;
    private final ArrayList<Socket> clients = new ArrayList<>();
    private final static Map<String, Map<Socket, Integer>> peerPorts = new HashMap<>();

    public void run() {
        ServerSocket server = null;
        try {
            server = new ServerSocket(PORT);
            server.setReuseAddress(true);
            System.out.println("Server is running");
            while (true) {
                Socket client = server.accept();
                System.out.println("=== New client connected: " + client.getInetAddress().getHostAddress()+
                        " " + client.getLocalPort());
                synchronized (clients) {
                    clients.add(client);
                }
                Thread currentClientThread = new Thread(new TrackerCommandHandler(client, this));
                currentClientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public ArrayList<Socket> getClients() {
        return clients;
    }

    public Map<String, Map<Socket, Integer>> getPeerPorts() {
        return peerPorts;
    }

    public static void main(String[] args) {
        TrackerServer trackerServer = new TrackerServer();
        trackerServer.run();
    }
}