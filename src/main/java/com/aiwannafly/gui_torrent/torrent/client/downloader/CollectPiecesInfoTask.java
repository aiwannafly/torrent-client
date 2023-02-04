package com.aiwannafly.gui_torrent.torrent.client.downloader;

import com.aiwannafly.gui_torrent.torrent.client.messages.Message;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.concurrent.Callable;

public class CollectPiecesInfoTask implements Callable<Response> {
    private final DownloadManager.PeerInfo peerInfo;
    private final int peerPort;
    private final long waitTime;
    private long currentWaitTime;
    private final int waitedMsgType;


    CollectPiecesInfoTask(DownloadManager.PeerInfo peerInfo, int peerPort,
                          long waitTime, int waitedMsgType) {
        this.peerInfo = peerInfo;
        this.peerPort = peerPort;
        this.waitTime = waitTime;
        this.waitedMsgType = waitedMsgType;
        this.currentWaitTime = waitTime;
    }

    @Override
    public Response call() throws Exception {
        Response result = new Response();
        result.status = Response.Status.NOT_RESPONDS;
        result.peerPort = peerPort;
        Selector selector = Selector.open();
        peerInfo.channel.configureBlocking(false);
        peerInfo.channel.register(selector, SelectionKey.OP_READ);
        int type;
        long startTime = System.currentTimeMillis();
        do {
            int returnValue = selector.select(currentWaitTime);
            peerInfo.channel.keyFor(selector).cancel();
            if (returnValue == 0) {
                peerInfo.channel.configureBlocking(true);
                return result;
            }
            result.messageInfo = Message.getMessage(peerInfo.channel);
            if (result.messageInfo.type == Message.KEEP_ALIVE) {
                type = Message.KEEP_ALIVE;
                continue;
            }
            type = result.messageInfo.type;
            currentWaitTime = waitTime - (System.currentTimeMillis() - startTime);
        } while (type == Message.KEEP_ALIVE && currentWaitTime > 0);
        if (type == waitedMsgType) {
            if (type == Message.HAVE) {
                result.status = Response.Status.HAVE;
                result.pieceIdx = result.messageInfo.piece.idx;
                result.newAvailablePieces = new ArrayList<>();
                result.newAvailablePieces.add(result.pieceIdx);
            } else if (type == Message.UNCHOKE) {
                result.status = Response.Status.GOT_UNCHOKE;
            } else {
                result.status = Response.Status.RECEIVED;
            }
        }
        peerInfo.channel.configureBlocking(true);
        return result;
    }
}
