package com.aiwannafly.gui_torrent.view;

import com.aiwannafly.gui_torrent.torrent.client.util.torrent.Torrent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.io.IOException;
import java.util.ArrayList;

public interface GUITorrentRenderer {

    enum Status {
        DISTRIBUTED, DOWNLOADING
    }

    enum ButtonStatus {
        STOP, RESUME
    }

    enum ClearType {
        ONLY_D, ONLY_U, ALL
    }

    class FileSection {
        public String torrentFileName;
        public Status status;
        public Button stopResumeButton;
        public ButtonStatus buttonStatus;
        public Label dSpeedLabel;
        public Label uSpeedLabel;
        public ArrayList<Label> labels;
        public double x;
        public double y;
        public Torrent torrent;
        public int sectionsCount = 0;
    }

    Scene getScene() throws IOException;

    FileSection createFileSection(String torrentFilePath, Status status);

    void renderFileSection(FileSection fileSection);

    void clearFileSection(FileSection fileSection, ClearType clearType);

    void renderNewSegmentBar(FileSection fileSection);
}
