package com.aiwannafly.gui_torrent.torrent.client.downloader;

import com.aiwannafly.gui_torrent.torrent.client.exceptions.BadServerReplyException;
import com.aiwannafly.gui_torrent.torrent.client.exceptions.ServerNotCorrespondsException;
import com.aiwannafly.gui_torrent.torrent.client.tracker_communicator.TrackerCommunicator;
import com.aiwannafly.gui_torrent.torrent.client.util.ObservableList;
import com.aiwannafly.gui_torrent.torrent.client.util.torrent.Torrent;
import com.aiwannafly.gui_torrent.torrent.client.Constants;
import com.aiwannafly.gui_torrent.torrent.client.file_manager.FileManager;
import com.aiwannafly.gui_torrent.torrent.client.exceptions.BadTorrentFileException;
import com.aiwannafly.gui_torrent.torrent.client.exceptions.NoSeedsException;

import java.util.*;
import java.util.concurrent.*;


public class MultyDownloadManager implements Downloader {
    private final ExecutorService leechPool = Executors.newFixedThreadPool(
            Constants.DOWNLOAD_MAX_THREADS_COUNT);
    private final FileManager fileManager;
    private final String peerId;
    private final ArrayList<String> removalList = new ArrayList<>();
    private final Map<String, DownloadManager> downloadManagers = new HashMap<>();
    private final ExecutorService downloader = Executors.newSingleThreadExecutor();
    private final CompletionService<Status> downloadService =
            new ExecutorCompletionService<>(downloader);
    private final Queue<String> stoppedTorrents = new ArrayDeque<>();
    private final Queue<DownloadManager> newTorrents = new ArrayBlockingQueue<>(100);
    private final TrackerCommunicator trackerComm;
    private boolean downloading = false;

    enum Status {
        FINISHED, NOT_FINISHED
    }

    public MultyDownloadManager(FileManager fileManager, String peerId, TrackerCommunicator trackerComm) {
        this.fileManager = fileManager;
        this.peerId = peerId;
        this.trackerComm = trackerComm;
    }

    @Override
    public void addTorrentForDownloading(Torrent torrent, ObservableList<Integer> myPieces)
            throws NoSeedsException, ServerNotCorrespondsException, BadServerReplyException {
        String torrentFileName = torrent.getName() + Constants.POSTFIX;
        DownloadManager downloadManager = new DownloadManager(torrent,
                fileManager, peerId, leechPool, myPieces, trackerComm);
        if (!downloading) {
            downloadManagers.put(torrentFileName, downloadManager);
        } else {
            newTorrents.add(downloadManager);
        }
    }

    @Override
    public void launchDownloading() {
        if (downloading) {
            return;
        }
        downloading = true;
        downloadService.submit(() -> {
            while (!downloadManagers.isEmpty()) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                while (!newTorrents.isEmpty()) {
                    DownloadManager downloadManager = newTorrents.remove();
                    String torrentFileName = downloadManager.getTorrentFile().getName() +
                            Constants.POSTFIX;
                    downloadManagers.put(torrentFileName, downloadManager);
                }
                int waitingCount = 0;
                for (String torrentFileName: downloadManagers.keySet()) {
                    if (stoppedTorrents.contains(torrentFileName)) {
                        continue;
                    }
                    DownloadManager downloadManager = downloadManagers.get(torrentFileName);
                    DownloadManager.Result result = downloadManager.downloadNextPiece();
                    if (result.downloadStatus == DownloadManager.DownloadStatus.WAITING_FOR_PEERS) {
                        waitingCount++;
                    }
                    if (result.downloadStatus == DownloadManager.DownloadStatus.FINISHED) {
                        removalList.add(torrentFileName);
                    }
                }
                if (waitingCount == downloadManagers.size()) {
                    /* all download managers are waiting for peers, we need to wait */
                    Thread.sleep(DownloadManager.CONNECTIONS_UPDATE_TIME);
                }
                for (String torrent: removalList) {
                    downloadManagers.remove(torrent).shutdown();
                }
                removalList.clear();
            }
            downloading = false;
            return Status.FINISHED;
        });
    }

    @Override
    public void stopDownloading(String torrentFileName) throws BadTorrentFileException {
        if (downloadManagers.containsKey(torrentFileName)) {
            stoppedTorrents.add(torrentFileName);
            return;
        }
        throw new BadTorrentFileException("File " + torrentFileName + " is not downloaded");
    }

    @Override
    public void resumeDownloading(String torrentFileName) throws BadTorrentFileException {
        if (downloadManagers.containsKey(torrentFileName)) {
            stoppedTorrents.remove(torrentFileName);
            return;
        }
        throw new BadTorrentFileException("File " + torrentFileName + " is not downloaded");
    }

    @Override
    public void shutdown() {
        if (!downloading) {
            leechPool.shutdown();
            long waitTimeSecs = 1;
            try {
                boolean completed = leechPool.awaitTermination(waitTimeSecs, TimeUnit.SECONDS);
                if (!completed) {
                    System.err.println("=== Execution was not completed");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            leechPool.shutdownNow();
        }
        for (String torrentFileName: downloadManagers.keySet()) {
            downloadManagers.get(torrentFileName).shutdown();
        }
        downloader.shutdownNow();
    }
}
