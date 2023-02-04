package com.aiwannafly.gui_torrent.torrent.client.downloader;

import com.aiwannafly.gui_torrent.torrent.client.messages.Message;

import java.util.ArrayList;

public class Response {
    public Status status;
    public int peerPort;
    public int pieceIdx;
    public boolean receivedKeepAlive = false;
    public long keepAliveTimeMillis = 0;
    public ArrayList<Integer> newAvailablePieces;
    public Message.MessageInfo messageInfo;

    public enum Status {
        RECEIVED, HAVE, GOT_KEEP_ALIVE, LOST, NOT_RESPONDS,
        GOT_CHOKE, GOT_UNCHOKE
    }

    public Response() {
    }

    public Response(Status status, int peerPort, int pieceIdx) {
        this.status = status;
        this.peerPort = peerPort;
        this.pieceIdx = pieceIdx;
    }
}
