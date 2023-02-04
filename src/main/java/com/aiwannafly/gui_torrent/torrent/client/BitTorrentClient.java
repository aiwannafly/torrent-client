package com.aiwannafly.gui_torrent.torrent.client;

import com.aiwannafly.gui_torrent.torrent.client.util.ObservableList;
import com.aiwannafly.gui_torrent.torrent.client.file_manager.FileManager;
import com.aiwannafly.gui_torrent.torrent.client.file_manager.FileManagerImpl;
import com.aiwannafly.gui_torrent.torrent.client.tracker_communicator.TrackerCommunicator;
import com.aiwannafly.gui_torrent.torrent.client.tracker_communicator.TrackerCommunicatorImpl;
import com.aiwannafly.gui_torrent.torrent.client.util.torrent.Torrent;
import com.aiwannafly.gui_torrent.torrent.client.util.torrent.TorrentParser;
import com.aiwannafly.gui_torrent.torrent.client.downloader.Downloader;
import com.aiwannafly.gui_torrent.torrent.client.downloader.MultyDownloadManager;
import com.aiwannafly.gui_torrent.torrent.client.exceptions.*;
import com.aiwannafly.gui_torrent.torrent.client.uploader.UploadLauncher;
import com.aiwannafly.gui_torrent.torrent.client.uploader.Uploader;
import com.aiwannafly.gui_torrent.torrent.client.util.TorrentFileCreator;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class BitTorrentClient implements TorrentClient {
    private String peerId;
    private TrackerCommunicator trackerComm;
    private final FileManager fileManager;
    private Downloader downloader;
    private final Map<String, Uploader> uploaders = new HashMap<>();
    private final Map<String, ObservableList<Integer>> myPieces = new HashMap<>();
    private final Map<String, ObservableList<Integer>> sentPieces = new HashMap<>();

    public BitTorrentClient() {
        fileManager = new FileManagerImpl();
    }

    @Override
    public void download(String torrentFilePath) throws BadTorrentFileException,
            NoSeedsException, ServerNotCorrespondsException, BadServerReplyException {
        String torrentFileName = torrentFilePath.substring(torrentFilePath.lastIndexOf(Constants.PATH_DIVIDER) + 1);
        Torrent torrentFile;
        try {
            torrentFile = TorrentParser.parseTorrent(torrentFilePath);
        } catch (IOException e) {
            throw new BadTorrentFileException("Could not open torrent file " + e.getMessage());
        }
        if (trackerComm == null) {
            initTrackerCommunicator();
        }
        ObservableList<Integer> myPieces = new ObservableList<>();
        this.myPieces.put(torrentFileName, myPieces);
        distributePart(torrentFile, torrentFileName, myPieces);
        if (downloader == null) {
            downloader = new MultyDownloadManager(fileManager, peerId, trackerComm);
        }
        try {
            downloader.addTorrentForDownloading(torrentFile, myPieces);
        } catch (BadServerReplyException | NoSeedsException |
        ServerNotCorrespondsException e) {
            uploaders.remove(torrentFileName).shutdown();
            throw e;
        }
        downloader.launchDownloading();
    }

    @Override
    public void distribute(String torrentFilePath) throws BadTorrentFileException, ServerNotCorrespondsException {
        ObservableList<Integer> allPieces = new ObservableList<>();
        String torrentFileName = torrentFilePath.substring(torrentFilePath.lastIndexOf(Constants.PATH_DIVIDER) + 1);
        myPieces.put(torrentFileName, allPieces);
        String postfix = Constants.POSTFIX;
        if (torrentFileName.length() <= postfix.length()) {
            throw new BadTorrentFileException("Bad name");
        }
        if (!torrentFileName.endsWith(postfix)) {
            throw new BadTorrentFileException("Bad name");
        }
        Torrent torrentFile;
        try {
            torrentFile = TorrentParser.parseTorrent(torrentFilePath);
        } catch (IOException e) {
            throw new BadTorrentFileException("Failed to load torrent: " + e.getMessage());
        }
        for (int i = 0; i < torrentFile.getPieces().size(); i++) {
            allPieces.add(i);
        }
        distributePart(torrentFile, torrentFileName, allPieces);
    }

    private void distributePart(Torrent torrentFile, String fileName, ObservableList<Integer> pieces)
    throws ServerNotCorrespondsException {
        if (uploaders.containsKey(fileName)) {
            return;
        }
        if (trackerComm == null) {
            initTrackerCommunicator();
        }
        sentPieces.put(fileName, new ObservableList<>());
        Uploader uploader = new UploadLauncher(torrentFile, fileManager, peerId, pieces, trackerComm,
                sentPieces.get(fileName));
        uploader.launchDistribution();
        uploaders.put(fileName, uploader);
    }

    @Override
    public void createTorrent(String filePath) throws TorrentCreateFailureException,
            BadTorrentFileException, ServerNotCorrespondsException {
        String torrentFilePath = filePath + Constants.POSTFIX;
        String torrentFileName = torrentFilePath.substring(torrentFilePath.lastIndexOf(Constants.PATH_DIVIDER) + 1);
        File torrentFile = new File(Constants.TORRENT_PATH + torrentFileName);
        File originalFile = new File(filePath);
        try {
            TorrentFileCreator.createTorrent(torrentFile, originalFile, Constants.TRACKER_URL);
        } catch (IOException e) {
            throw new TorrentCreateFailureException("Could not make .torrent file");
        }
        distribute(Constants.TORRENT_PATH + torrentFileName);
    }

    @Override
    public void stopDownloading(String torrentFileName) throws BadTorrentFileException {
        downloader.stopDownloading(torrentFileName);
    }

    @Override
    public void resumeDownloading(String torrentFileName) throws BadTorrentFileException {
        downloader.resumeDownloading(torrentFileName);
    }

    @Override
    public ObservableList<Integer> getCollectedPieces(String torrentFileName) throws BadTorrentFileException {
        if (!myPieces.containsKey(torrentFileName)) {
            throw new BadTorrentFileException("=== File not found");
        }
        return myPieces.get(torrentFileName);
    }

    @Override
    public ObservableList<Integer> getSentPieces(String torrentFileName) throws BadTorrentFileException {
        if (!sentPieces.containsKey(torrentFileName)) {
            throw new BadTorrentFileException("=== File not found");
        }
        return sentPieces.get(torrentFileName);
    }

    @Override
    public void close() {
        if (trackerComm != null) {
            trackerComm.sendToTracker(Constants.STOP_COMMAND);
            trackerComm.close();
        }
        for (Uploader uploader : uploaders.values()) {
            uploader.shutdown();
        }
        if (downloader != null) {
            downloader.shutdown();
        }
        try {
            fileManager.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initTrackerCommunicator() throws ServerNotCorrespondsException {
        trackerComm = new TrackerCommunicatorImpl();
        trackerComm.sendToTracker("get peer_id");
        peerId = trackerComm.receiveFromTracker();
    }
}
