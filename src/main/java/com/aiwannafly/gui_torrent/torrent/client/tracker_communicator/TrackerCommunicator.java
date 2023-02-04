package com.aiwannafly.gui_torrent.torrent.client.tracker_communicator;

public interface TrackerCommunicator {

    void sendToTracker(String message);

    String receiveFromTracker();

    void close();
}
