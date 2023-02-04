package com.aiwannafly.gui_torrent.view;

import com.aiwannafly.gui_torrent.ApplicationStarter;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;

import java.util.Objects;

public class TexturePack {
    public static ImagePattern getImagePattern(String imgName) {
        return new ImagePattern(new Image(Objects.requireNonNull(ApplicationStarter.class.getResource(
                imgName)).toString()));
    }

    public final static Image backgroundImage = new Image(Objects.requireNonNull(ApplicationStarter.class.getResource(
            "images/background.png")).toString());
    public final static Image iconImage = new Image(Objects.requireNonNull(ApplicationStarter.class.getResource(
            "images/icon.png")).toString());
    public final static ImagePattern stopButton = getImagePattern("images/stop.png");
    public final static ImagePattern resumeButton = getImagePattern("images/resume.png");
    public final static ImagePattern stopBlackButton = getImagePattern("images/stop_black.png");
    public final static ImagePattern resumeBlackButton = getImagePattern("images/resume_black.png");
}
