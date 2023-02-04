module com.aiwannafly.gui_torrent {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.commons.io;

    opens com.aiwannafly.gui_torrent to javafx.fxml;
    exports com.aiwannafly.gui_torrent;
    exports com.aiwannafly.gui_torrent.controller;
    opens com.aiwannafly.gui_torrent.controller to javafx.fxml;
    exports com.aiwannafly.gui_torrent.view;
    opens com.aiwannafly.gui_torrent.view to javafx.fxml;
    exports com.aiwannafly.gui_torrent.torrent.client.tracker;
    opens com.aiwannafly.gui_torrent.torrent.client.tracker to javafx.fxml;
    exports com.aiwannafly.gui_torrent.torrent.client;
    opens com.aiwannafly.gui_torrent.torrent.client to javafx.fxml;
}