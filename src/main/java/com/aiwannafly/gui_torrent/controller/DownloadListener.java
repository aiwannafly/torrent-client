package com.aiwannafly.gui_torrent.controller;

import com.aiwannafly.gui_torrent.torrent.client.util.ObservableList;
import com.aiwannafly.gui_torrent.view.GUITorrentRenderer;
import com.aiwannafly.gui_torrent.view.Renderer;
import javafx.application.Platform;

import java.util.Set;
import java.util.concurrent.Flow;

public class DownloadListener implements Flow.Subscriber<Boolean> {
    private final GUITorrentRenderer.FileSection fileSection;
    private final ObservableList<Integer> collectedPieces;
    private final Set<String> downloadedTorrents;
    private long lastUpdateTime = System.currentTimeMillis();
    private long currentTime = System.currentTimeMillis();
    private final int piecesPortion;
    private int downloadedCount = 0;

    public DownloadListener(GUITorrentRenderer.FileSection fileSection, ObservableList<Integer>
            collectedPieces, Set<String> downloadedTorrents) {
        this.fileSection = fileSection;
        this.collectedPieces = collectedPieces;
        this.downloadedTorrents = downloadedTorrents;
        int MAX_PORTION = 4;
        this.piecesPortion = Math.min(1 + fileSection.torrent.getPieces().size() / 10,
                MAX_PORTION);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {

    }

    @Override
    public void onNext(Boolean item) {
        Platform.runLater(() -> {
            while (fileSection.sectionsCount < collectedPieces.size()) {
                downloadedCount++;
                Renderer.instance.renderNewSegmentBar(fileSection);
            }
            if (fileSection.torrent.getPieces().size() == collectedPieces.size()) {
                downloadedTorrents.add(fileSection.torrentFileName);
                Renderer.instance.clearFileSection(fileSection, GUITorrentRenderer.ClearType.ONLY_D);
                return;
            }
            if (downloadedCount % piecesPortion != 0) {
                return;
            }
            currentTime = System.currentTimeMillis();
            long speedKB = calcDownloadSpeed();
            lastUpdateTime = currentTime;
            if (fileSection.buttonStatus == GUITorrentRenderer.ButtonStatus.RESUME) {
                return;
            }
            fileSection.dSpeedLabel.setText(String.valueOf(speedKB));
        });
    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onComplete() {

    }

    private long calcDownloadSpeed() {
        double estimatedTime = currentTime - lastUpdateTime;
        long downloadedBytes = piecesPortion *  fileSection.torrent.getPieceLength();
        double estimatedTimeSecs = estimatedTime / 1000;
        double speed = downloadedBytes / estimatedTimeSecs;
        return  (long) speed / 1024;
    }
}
