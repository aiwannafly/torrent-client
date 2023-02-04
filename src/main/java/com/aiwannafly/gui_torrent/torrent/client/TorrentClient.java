package com.aiwannafly.gui_torrent.torrent.client;

import com.aiwannafly.gui_torrent.torrent.client.util.ObservableList;
import com.aiwannafly.gui_torrent.torrent.client.exceptions.*;

public interface TorrentClient extends AutoCloseable {

    void download(String torrentFilePath) throws BadTorrentFileException,
            NoSeedsException, ServerNotCorrespondsException, BadServerReplyException;

    void distribute(String torrentFilePath) throws BadTorrentFileException,
            ServerNotCorrespondsException;

    void createTorrent(String filePath) throws TorrentCreateFailureException,
            BadTorrentFileException, ServerNotCorrespondsException;

    void stopDownloading(String torrentFileName) throws BadTorrentFileException;

    void resumeDownloading(String torrentFileName) throws BadTorrentFileException;

    ObservableList<Integer> getCollectedPieces(String torrentFileName) throws BadTorrentFileException;

    ObservableList<Integer> getSentPieces(String torrentFileName) throws BadTorrentFileException;
}
