package com.aiwannafly.gui_torrent.view;

import com.aiwannafly.gui_torrent.ApplicationStarter;
import com.aiwannafly.gui_torrent.controller.MainMenuController;
import com.aiwannafly.gui_torrent.torrent.client.Constants;
import com.aiwannafly.gui_torrent.torrent.client.util.torrent.Torrent;
import com.aiwannafly.gui_torrent.torrent.client.util.torrent.TorrentParser;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class Renderer implements GUITorrentRenderer {
    public static final Color APP_COLOR = new Color(36.0 / 255, 1, 0, 1);
    public static final String APP_CSS = "green_styles.css";
    private static final double NUM_FIELD_LEN = 30;
    private static final double NAME_FIELD_LEN = 250;
    private static final double SIZE_FIELD_LEN = 70;
    private static final double STATUS_FIELD_LEN = 100;
    private static final double D_SPEED_FIELD_LEN = 80;
    private static final double U_SPEED_FIELD_LEN = 80;
    private static final double BAR_FIELD_LEN = 350;
    private static final double LABEL_HEIGHT = 30;
    private static final double SECTION_LEN = NUM_FIELD_LEN +
            NAME_FIELD_LEN + SIZE_FIELD_LEN + STATUS_FIELD_LEN +
            + D_SPEED_FIELD_LEN + U_SPEED_FIELD_LEN + BAR_FIELD_LEN;
    private static final String STYLESHEET = Objects.requireNonNull(ApplicationStarter.class.getResource(
            APP_CSS)).toExternalForm();
    private static final double WINDOW_WIDTH = 1200;
    private static final double WINDOW_HEIGHT = 250;
    private static final double TOP_OFFSET = 10;
    private static final double LEFT_OFFSET = 20;
    private static final double MENU_LENGTH = 160;
    private Pane rootPane;
    private Pane buttonsPane;
    private int filesCount = 0;
    public static final Renderer instance = new Renderer();

    public Scene getScene() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ApplicationStarter.class.getResource("fxml/main-menu.fxml"));
        rootPane = new Pane();
        Background b = new Background(new BackgroundImage(TexturePack.backgroundImage, BackgroundRepeat.REPEAT,
                BackgroundRepeat.REPEAT, null, null));
        rootPane.setBackground(b);
        buttonsPane = fxmlLoader.load();
        rootPane.getChildren().add(buttonsPane);
        Scene scene = new Scene(rootPane, WINDOW_WIDTH, WINDOW_HEIGHT);
        scene.getStylesheets().add(STYLESHEET);
        scene.setFill(Color.BLACK);
        showHeaders();
        return scene;
    }

    @Override
    public FileSection createFileSection(String torrentFilePath, Status status) {
        FileSection fileSection = new FileSection();
        String torrentFileName = torrentFilePath.substring(torrentFilePath.lastIndexOf(Constants.PATH_DIVIDER) + 1);
        Torrent torrentFile;
        try {
            torrentFile = TorrentParser.parseTorrent(torrentFilePath);
        } catch (IOException e) {
            e.printStackTrace();
            return fileSection;
        }
        fileSection.status = status;
        fileSection.x = MENU_LENGTH + LEFT_OFFSET;
        fileSection.y = TOP_OFFSET + LABEL_HEIGHT * (++filesCount);
        fileSection.labels = new ArrayList<>();
        Label numLabel = new Label(String.valueOf(filesCount));
        numLabel.setPrefWidth(NUM_FIELD_LEN);
        fileSection.torrentFileName = torrentFileName;
        Label nameLabel = new Label(torrentFileName);
        nameLabel.setPrefWidth(NAME_FIELD_LEN);
        Label sizeLabel = new Label(torrentFile.getTotalSize() / 1024 + " kB");
        sizeLabel.setPrefWidth(SIZE_FIELD_LEN);
        String statusStr = null;
        switch (status) {
            case DOWNLOADING -> statusStr = "downloading";
            case DISTRIBUTED -> statusStr = "distributing";
        }
        Label statusLabel = new Label(statusStr);
        statusLabel.setPrefWidth(STATUS_FIELD_LEN);
        fileSection.dSpeedLabel = new Label();
        fileSection.dSpeedLabel.setPrefWidth(D_SPEED_FIELD_LEN);
        fileSection.uSpeedLabel = new Label();
        fileSection.uSpeedLabel.setPrefWidth(U_SPEED_FIELD_LEN);
        Label barLabel = new Label();
        barLabel.setPrefWidth(BAR_FIELD_LEN);
        fileSection.labels.add(numLabel);
        fileSection.labels.add(nameLabel);
        fileSection.labels.add(sizeLabel);
        fileSection.labels.add(statusLabel);
        fileSection.labels.add(fileSection.dSpeedLabel);
        fileSection.labels.add(fileSection.uSpeedLabel);
        fileSection.labels.add(barLabel);
        fileSection.torrent = torrentFile;
        for (Label label : fileSection.labels) {
            label.setPrefHeight(LABEL_HEIGHT);
            label.setLayoutY(fileSection.y);
            label.setLayoutX(fileSection.x);
            label.setAlignment(Pos.CENTER);
            fileSection.x += label.getPrefWidth();
        }
        fileSection.buttonStatus = ButtonStatus.STOP;
        fileSection.stopResumeButton = new Button();
        fileSection.stopResumeButton.setLayoutY(fileSection.y);
        fileSection.stopResumeButton.setLayoutX(fileSection.x);
        fileSection.stopResumeButton.setPrefWidth(LABEL_HEIGHT);
        fileSection.stopResumeButton.setPrefHeight(LABEL_HEIGHT);
        if (status == Status.DISTRIBUTED) {
            return fileSection;
        }
        Rectangle image = makeIcon();
        image.setFill(TexturePack.stopButton);
        fileSection.stopResumeButton.setGraphic(image);
        fileSection.stopResumeButton.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            MainMenuController.stopResumeHandler(torrentFileName);
            Rectangle icon = makeIcon();
            if (fileSection.buttonStatus == ButtonStatus.RESUME) {
                icon.setFill(TexturePack.resumeBlackButton);
            } else {
                icon.setFill(TexturePack.stopBlackButton);
            }
            fileSection.stopResumeButton.setGraphic(icon);
        });
        fileSection.stopResumeButton.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            Rectangle icon = makeIcon();
            if (fileSection.buttonStatus == ButtonStatus.RESUME) {
                icon.setFill(TexturePack.resumeBlackButton);
            } else {
                icon.setFill(TexturePack.stopBlackButton);
            }
            fileSection.stopResumeButton.setGraphic(icon);
        });
        fileSection.stopResumeButton.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            Rectangle icon = makeIcon();
            if (fileSection.buttonStatus == ButtonStatus.RESUME) {
                icon.setFill(TexturePack.resumeButton);
            } else {
                icon.setFill(TexturePack.stopButton);
            }
            fileSection.stopResumeButton.setGraphic(icon);
        });
        return fileSection;
    }

    @Override
    public void renderFileSection(FileSection fileSection) {
        for (Label label : fileSection.labels) {
            rootPane.getChildren().add(label);
        }
        if (fileSection.stopResumeButton == null) {
            return;
        }
        rootPane.getChildren().add(fileSection.stopResumeButton);
    }

    @Override
    public void clearFileSection(FileSection fileSection, ClearType clearType) {
        Button button = fileSection.stopResumeButton;
        if (button != null && clearType.equals(ClearType.ONLY_D)) {
            rootPane.getChildren().remove(button);
            Label buttonLabel = new Label();
            buttonLabel.setPrefHeight(LABEL_HEIGHT);
            buttonLabel.setPrefWidth(LABEL_HEIGHT);
            buttonLabel.setLayoutX(button.getLayoutX());
            buttonLabel.setLayoutY(button.getLayoutY());
            rootPane.getChildren().add(buttonLabel);
        }
        switch (clearType) {
            case ALL -> {
                fileSection.uSpeedLabel.setText("");
                fileSection.dSpeedLabel.setText("");
            }
            case ONLY_D -> fileSection.dSpeedLabel.setText("");
            case ONLY_U -> fileSection.uSpeedLabel.setText("");
        }
    }

    @Override
    public void renderNewSegmentBar(FileSection fileSection) {
        double width = BAR_FIELD_LEN / fileSection.torrent.getPieces().size();
        double y = fileSection.y + TOP_OFFSET;
        double offset = MENU_LENGTH + LEFT_OFFSET + NUM_FIELD_LEN + NAME_FIELD_LEN + SIZE_FIELD_LEN +
                STATUS_FIELD_LEN + D_SPEED_FIELD_LEN + U_SPEED_FIELD_LEN;
        double x = offset + fileSection.sectionsCount++ * width;
        Rectangle segment = new Rectangle(x, y, width, TOP_OFFSET);
        segment.setFill(APP_COLOR);
        rootPane.getChildren().add(segment);
    }

    private void showHeaders() {
        Label numLabel = new Label("№");
        numLabel.setPrefWidth(NUM_FIELD_LEN);
        Label nameLabel = new Label("File name");
        nameLabel.setPrefWidth(NAME_FIELD_LEN);
        Label sizeLabel = new Label("Size");
        sizeLabel.setPrefWidth(SIZE_FIELD_LEN);
        Label statusLabel = new Label("Status");
        statusLabel.setPrefWidth(STATUS_FIELD_LEN);
        Label dSpeedLabel = new Label("Down kB/s");
        dSpeedLabel.setPrefWidth(D_SPEED_FIELD_LEN);
        Label uSpeedLabel = new Label("Up kB/s");
        uSpeedLabel.setPrefWidth(U_SPEED_FIELD_LEN);
        Label barLabel = new Label("Status bar");
        barLabel.setPrefWidth(BAR_FIELD_LEN);
        Label buttonLabel = new Label();
        buttonLabel.setPrefWidth(LABEL_HEIGHT);
        ArrayList<Label> allLabels = new ArrayList<>();
        allLabels.add(numLabel);
        allLabels.add(nameLabel);
        allLabels.add(sizeLabel);
        allLabels.add(statusLabel);
        allLabels.add(dSpeedLabel);
        allLabels.add(uSpeedLabel);
        allLabels.add(barLabel);
        allLabels.add(buttonLabel);
        double x = MENU_LENGTH + LEFT_OFFSET;
        Label borderLabel = new Label();
        borderLabel.setLayoutX(x);
        borderLabel.setLayoutY(TOP_OFFSET);
        borderLabel.setPrefHeight(SECTION_LEN);
        rootPane.getChildren().add(borderLabel);
        for (Label label : allLabels) {
            label.setPrefHeight(LABEL_HEIGHT);
            label.setLayoutY(TOP_OFFSET);
            label.setLayoutX(x);
            label.setAlignment(Pos.CENTER);
            x += label.getPrefWidth();
            rootPane.getChildren().add(label);
        }
        borderLabel.setPrefWidth(x - (MENU_LENGTH + LEFT_OFFSET));
    }

    private static Rectangle makeIcon() {
        Rectangle icon = new Rectangle();
        icon.setWidth(LABEL_HEIGHT / 3.5);
        icon.setHeight(LABEL_HEIGHT / 3.5);
        return icon;
    }
}
