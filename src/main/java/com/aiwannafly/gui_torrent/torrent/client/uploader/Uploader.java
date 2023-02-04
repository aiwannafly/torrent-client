package com.aiwannafly.gui_torrent.torrent.client.uploader;

public interface Uploader {
    void launchDistribution();

    int getListeningPort();

    void shutdown();
}
