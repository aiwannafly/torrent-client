package com.aiwannafly.gui_torrent.torrent.client.tracker_communicator;

import com.aiwannafly.gui_torrent.torrent.client.tracker.TrackerServer;
import com.aiwannafly.gui_torrent.torrent.client.exceptions.ServerNotCorrespondsException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TrackerCommunicatorImpl implements TrackerCommunicator {
    private PrintWriter out;
    private BufferedReader in;

    public TrackerCommunicatorImpl() throws ServerNotCorrespondsException {
        try {
            Socket trackerSocket = new Socket("localhost", TrackerServer.PORT);
            out = new PrintWriter(trackerSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(trackerSocket.getInputStream()));
        } catch (IOException e) {
            throw new ServerNotCorrespondsException("Server not corresponds: " + e.getMessage());
        }
    }

    @Override
    public synchronized void sendToTracker(String msg) {
        out.println(msg);
        out.flush();
    }

    @Override
    public synchronized String receiveFromTracker() {
        try {
            return in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void close() {
        try {
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
