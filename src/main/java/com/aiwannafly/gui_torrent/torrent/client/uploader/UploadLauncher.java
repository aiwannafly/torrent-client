package com.aiwannafly.gui_torrent.torrent.client.uploader;

import com.aiwannafly.gui_torrent.torrent.client.tracker_communicator.TrackerCommunicator;
import com.aiwannafly.gui_torrent.torrent.client.util.ObservableList;
import com.aiwannafly.gui_torrent.torrent.client.util.torrent.Torrent;
import com.aiwannafly.gui_torrent.torrent.client.Constants;
import com.aiwannafly.gui_torrent.torrent.client.file_manager.FileManager;
import com.aiwannafly.gui_torrent.torrent.client.tracker.TrackerCommandHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;

public class UploadLauncher implements Uploader {
    private ServerSocketChannel serverSocketChannel;
    private Thread connectionsHandlerThread;
    private final Torrent torrentFile;
    private final FileManager fileManager;
    private final String peerId;
    private final ObservableList<Integer> availablePieces;
    private final TrackerCommunicator trackerComm;
    private final ObservableList<Integer> sentPieces;

    public UploadLauncher(Torrent torrentFile, FileManager fileManager, String peerId,
                          ObservableList<Integer> availablePieces, TrackerCommunicator trackerComm,
                          ObservableList<Integer> sentPieces) {
        this.torrentFile = torrentFile;
        this.fileManager = fileManager;
        this.sentPieces = sentPieces;
        this.availablePieces = availablePieces;
        this.trackerComm = trackerComm;
        this.peerId = peerId;
        try {
            this.serverSocketChannel = ServerSocketChannel.open();
            InetSocketAddress address = new InetSocketAddress("localhost", 0);
            this.serverSocketChannel.bind(address);
            this.serverSocketChannel.configureBlocking(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void launchDistribution() {
        // listen-port 5000 fileName
        String torrentFileName = torrentFile.getName() + Constants.POSTFIX;
        String command = TrackerCommandHandler.SET_LISTENING_SOCKET + " " +
                getListeningPort() + " " + torrentFileName;
        trackerComm.sendToTracker(command);
        trackerComm.receiveFromTracker();
        connectionsHandlerThread = new Thread(new UploadHandler(this.torrentFile,
                this.fileManager, this.peerId, this.serverSocketChannel, this.availablePieces,
                this.sentPieces));
        connectionsHandlerThread.setName(Constants.CONNECTIONS_THREAD_NAME);
        connectionsHandlerThread.setDaemon(true);
        connectionsHandlerThread.start();
    }

    @Override
    public int getListeningPort() {
        String addr;
        try {
            addr = serverSocketChannel.getLocalAddress().toString();
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
        int port_idx = 0;
        for (int i = 0; i < addr.length(); i++) {
            if (addr.charAt(i) == ':') {
                port_idx = i + 1;
                break;
            }
        }
        return Integer.parseInt(addr.substring(port_idx));
    }

    @Override
    public void shutdown() {
        connectionsHandlerThread.interrupt();
        try {
            serverSocketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
