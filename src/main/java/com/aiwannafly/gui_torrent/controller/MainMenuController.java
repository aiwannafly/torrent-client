package com.aiwannafly.gui_torrent.controller;

import com.aiwannafly.gui_torrent.ApplicationStarter;
import com.aiwannafly.gui_torrent.torrent.client.Constants;
import com.aiwannafly.gui_torrent.torrent.client.util.ObservableList;
import com.aiwannafly.gui_torrent.torrent.client.TorrentClient;
import com.aiwannafly.gui_torrent.torrent.client.exceptions.*;
import com.aiwannafly.gui_torrent.view.Renderer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.aiwannafly.gui_torrent.view.GUITorrentRenderer.*;

public class MainMenuController {
    @FXML
    public Button distributeButton;

    @FXML
    public Button createButton;
    public VBox menu;

    @FXML
    private Button downloadButton;
    private final static Map<String, FileSection> fileSections = new HashMap<>();
    private final static Set<String> downloadedTorrents = new HashSet<>();

    @FXML
    protected void onDownloadButtonClick() {
        TorrentClient torrentClient = ApplicationStarter.getTorrentClient();
        assert torrentClient != null;
        File file = chooseFile("Choose a file to download", "torrents", "*.torrent");
        if (file == null) {
            return;
        }
        String torrentFilePath = file.getAbsolutePath();
        String postfix = Constants.POSTFIX;
        String torrentFileName = torrentFilePath.substring(torrentFilePath.lastIndexOf(Constants.PATH_DIVIDER) + 1);
        if (fileSections.containsKey(torrentFileName)) {
            showErrorAlert("This file is used already");
            return;
        }
        int originalFileLength = torrentFileName.length() - postfix.length();
        String originalFileName;
        if (originalFileLength <= 0) {
            originalFileName = Constants.PREFIX;
        } else {
            originalFileName = Constants.PREFIX + torrentFilePath.substring(0, originalFileLength);
        }
        if (torrentFileName.length() <= postfix.length()) {
            showErrorAlert("Bad file name, it should end with " + postfix);
            return;
        }
        try {
            torrentClient.download(torrentFilePath);
        } catch (BadTorrentFileException | BadServerReplyException |
                NoSeedsException | ServerNotCorrespondsException e) {
            showErrorAlert("Could not download " + originalFileName
                    + ": " + e.getMessage());
            return;
        }
        FileSection fileSection = Renderer.instance.createFileSection(torrentFilePath, Status.DOWNLOADING);
        Renderer.instance.renderFileSection(fileSection);
        fileSections.put(torrentFileName, fileSection);
        fileSection.uSpeedLabel.setText("0");
        ObservableList<Integer> collectedPieces;
        try {
            collectedPieces = torrentClient.getCollectedPieces(torrentFileName);
        } catch (BadTorrentFileException e) {
            e.printStackTrace();
            return;
        }
        collectedPieces.subscribe(new DownloadListener(fileSection, collectedPieces, downloadedTorrents));
        ObservableList<Integer> sentPieces;
        try {
            sentPieces = torrentClient.getSentPieces(torrentFileName);
        } catch (BadTorrentFileException e) {
            e.printStackTrace();
            return;
        }
        sentPieces.subscribe(new UploadListener(fileSection, sentPieces));
    }

    @FXML
    private void onDistributeButtonClick() {
        TorrentClient torrentClient = ApplicationStarter.getTorrentClient();
        assert torrentClient != null;
        File file = chooseFile("Choose a .torrent file to distribute", "torrents", "*.torrent");
        if (file == null) {
            return;
        }
        String torrentFilePath = file.getAbsolutePath();
        String torrentFileName = torrentFilePath.substring(torrentFilePath.lastIndexOf(Constants.PATH_DIVIDER) + 1);
        if (fileSections.containsKey(torrentFileName)) {
            showErrorAlert("This file is distributed already");
            return;
        }
        try {
            torrentClient.distribute(torrentFilePath);
        } catch (BadTorrentFileException | ServerNotCorrespondsException e) {
            showErrorAlert("Could not upload " + torrentFileName + ": " + e.getMessage());
            return;
        }
        renderFileUploading(torrentClient, torrentFileName, torrentFilePath);
    }

    @FXML
    private void onCreateButtonClick() {
        TorrentClient torrentClient = ApplicationStarter.getTorrentClient();
        assert torrentClient != null;
        File file = chooseFile("Choose a file to make its torrent", "uploads", null);
        if (file == null) {
            return;
        }
        String filePath = file.getAbsolutePath();
        String fileName = filePath.substring(filePath.lastIndexOf(Constants.PATH_DIVIDER) + 1);
        try {
            torrentClient.createTorrent(filePath);
        } catch (BadTorrentFileException | TorrentCreateFailureException | ServerNotCorrespondsException e) {
            showErrorAlert("Could not create a torrent for " + fileName +
                    ": " + e.getMessage());
            return;
        }
        String torrentFileName = fileName + Constants.POSTFIX;
        if (fileSections.containsKey(torrentFileName)) {
            showErrorAlert("This file is distributed already");
            return;
        }
        String torrentFilePath = Constants.TORRENT_PATH + torrentFileName;
        renderFileUploading(torrentClient, torrentFileName, torrentFilePath);
    }

    private void renderFileUploading(TorrentClient torrentClient, String torrentFileName, String torrentFilePath) {
        FileSection fileSection = Renderer.instance.createFileSection(torrentFilePath, Status.DISTRIBUTED);
        Renderer.instance.renderFileSection(fileSection);
        fileSections.put(torrentFileName, fileSection);
        ObservableList<Integer> sentPieces;
        try {
            sentPieces = torrentClient.getSentPieces(torrentFileName);
        } catch (BadTorrentFileException e) {
            e.printStackTrace();
            return;
        }
        sentPieces.subscribe(new UploadListener(fileSection, sentPieces));
    }

    @FXML
    protected void onExitButtonClick() {
        exit();
    }

    public static void exit() {
        TorrentClient torrentClient = ApplicationStarter.getTorrentClient();
        assert torrentClient != null;
        try {
            torrentClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Platform.exit();
    }

    private File chooseFile(String message, String dirName, String fileExtension) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(message);
        File dir = new File(dirName);
        fileChooser.setInitialDirectory(dir);
        if (fileExtension != null) {
            FileChooser.ExtensionFilter fileExtensions = new FileChooser.ExtensionFilter("Files", fileExtension);
            fileChooser.getExtensionFilters().add(fileExtensions);
        }
        Stage stage = (Stage) downloadButton.getScene().getWindow();
        return fileChooser.showOpenDialog(stage);
    }

    private static void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void stopResumeHandler(String torrentFileName) {
        if (downloadedTorrents.contains(torrentFileName)) {
            return;
        }
        TorrentClient torrentClient = ApplicationStarter.getTorrentClient();
        ButtonStatus status = fileSections.get(torrentFileName).buttonStatus;
        if (status == ButtonStatus.STOP) {
            try {
                torrentClient.stopDownloading(torrentFileName);
            } catch (BadTorrentFileException e) {
                showErrorAlert(e.getMessage());
                return;
            }
            fileSections.get(torrentFileName).buttonStatus = ButtonStatus.RESUME;
            fileSections.get(torrentFileName).dSpeedLabel.setText("0");
            return;
        }
        try {
            torrentClient.resumeDownloading(torrentFileName);
        } catch (BadTorrentFileException e) {
            showErrorAlert(e.getMessage());
            return;
        }
        fileSections.get(torrentFileName).buttonStatus = ButtonStatus.STOP;
    }
}