package com.aiwannafly.gui_torrent.torrent.client.downloader;

import com.aiwannafly.gui_torrent.torrent.client.exceptions.BadMessageException;
import com.aiwannafly.gui_torrent.torrent.client.util.ByteOperations;
import com.aiwannafly.gui_torrent.torrent.client.util.torrent.Torrent;
import com.aiwannafly.gui_torrent.torrent.client.file_manager.FileManager;
import com.aiwannafly.gui_torrent.torrent.client.messages.Message;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Callable;

public class DownloadPieceTask implements Callable<Response> {
    private final Torrent torrentFile;
    private final FileManager fileManager;
    private final String fileName;
    private final int pieceIdx;
    private final int pieceLength;
    private final DownloadManager.PeerInfo peerInfo;
    private final int peerPort;
    private ArrayList<Integer> newAvailablePieces;

    public DownloadPieceTask(Torrent torrentFile, FileManager fileManager,
                             String fileName, int peerPort, int pieceLength,
                             DownloadManager.PeerInfo peerInfo) {
        this.fileManager = fileManager;
        this.peerPort = peerPort;
        this.torrentFile = torrentFile;
        this.fileName = fileName;
        this.pieceIdx = peerInfo.currentPieceIdx;
        this.pieceLength = pieceLength;
        this.peerInfo = peerInfo;
    }

    @Override
    public Response call() throws IOException, BadMessageException {
        Response result = new Response(Response.Status.RECEIVED, peerPort, pieceIdx);
        requestPiece(pieceIdx, 0, pieceLength);
        while (true) {
            Response.Status received = receivePiece();
            if (received == Response.Status.GOT_KEEP_ALIVE) {
//                System.out.println("GOT KA");
                result.receivedKeepAlive = true;
                result.keepAliveTimeMillis = System.currentTimeMillis();
            } else if (received == Response.Status.HAVE) {
//                System.out.println("GOT HAVE");
                result.newAvailablePieces = this.newAvailablePieces;
            } else if (received == Response.Status.LOST) {
//                System.out.println("GOT LOST");
                result.status = Response.Status.LOST;
                return result;
            } else if (received == Response.Status.GOT_CHOKE) {
//                System.out.println("GOT CHOKE");
                result.status = Response.Status.GOT_CHOKE;
                return result;
            }
            if (received == Response.Status.RECEIVED) {
//                System.out.println("GOT PIECE FROM " + peerPort);
                break;
            }
        }
        return result;
    }

    private void requestPiece(int index, int begin, int length) {
        String message = ByteOperations.convertIntoBytes(13) + Message.REQUEST +
                ByteOperations.convertIntoBytes(index) + ByteOperations.convertIntoBytes(begin) +
                ByteOperations.convertIntoBytes(length);
        peerInfo.out.print(message);
        peerInfo.out.flush();
    }

    private Response.Status receivePiece() throws IOException, BadMessageException {
        Selector selector = Selector.open();
        peerInfo.channel.configureBlocking(false);
        peerInfo.channel.register(selector, SelectionKey.OP_READ);
        long waitTime = 1000;
        int r = selector.select(waitTime);
        if (r == 0) {
            return Response.Status.LOST;
        }
        peerInfo.channel.keyFor(selector).cancel();
        peerInfo.channel.configureBlocking(true);
        Message.MessageInfo messageInfo = Message.getMessage(peerInfo.channel);
        if (messageInfo.type == Message.KEEP_ALIVE) {
            return Response.Status.GOT_KEEP_ALIVE;
        } else if (messageInfo.type == Message.CHOKE) {
            return Response.Status.GOT_CHOKE;
        }
        // piece: <len=0009+X><id=7><index><begin><block>
        if (messageInfo.type == Message.HAVE) {
            int idx = messageInfo.piece.idx;
            if (newAvailablePieces == null) {
                newAvailablePieces = new ArrayList<>();
            }
            newAvailablePieces.add(idx);
            // System.out.println("=== Received 'HAVE " + idx + "'");
            return Response.Status.HAVE;
        }
        if (messageInfo.type != Message.PIECE) {
            System.err.println("Bad type: " + messageInfo.type);
            return Response.Status.LOST;
        }
        Message.Piece piece = messageInfo.piece;
        String origHash = torrentFile.getPieces().get(piece.idx);
        String receivedHash;
        try {
            receivedHash = getSha1(piece.data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            return Response.Status.LOST;
        }
        if (!receivedHash.equals(origHash)) {
            System.err.println("=== Bad hash, r and o:");
            System.err.println(receivedHash);
            System.err.println(origHash);
            return Response.Status.LOST;
        }
        try {
            int offset;
            offset = piece.idx * torrentFile.getPieceLength().intValue() + piece.begin;
            fileManager.writePiece(fileName, offset, piece.data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return Response.Status.RECEIVED;
    }

    private String getSha1(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.reset();
        digest.update(bytes);
        return String.format("%040x", new BigInteger(1, digest.digest())).toUpperCase(Locale.ROOT);
    }
}
