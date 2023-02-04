package com.aiwannafly.gui_torrent.torrent.client.uploader;

import com.aiwannafly.gui_torrent.torrent.client.util.ByteOperations;
import com.aiwannafly.gui_torrent.torrent.client.util.ObservableList;
import com.aiwannafly.gui_torrent.torrent.client.util.torrent.Torrent;
import com.aiwannafly.gui_torrent.torrent.client.file_manager.FileManager;
import com.aiwannafly.gui_torrent.torrent.client.exceptions.BadMessageException;
import com.aiwannafly.gui_torrent.torrent.client.exceptions.DifferentHandshakesException;
import com.aiwannafly.gui_torrent.torrent.client.util.BitTorrentHandshake;
import com.aiwannafly.gui_torrent.torrent.client.messages.Message;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class UploadHandler implements Runnable {
    private final ServerSocketChannel serverSocketChannel;
    private final Torrent torrentFile;
    private final FileManager fileManager;
    private final String peerId;
    private final Map<SocketChannel, LeechInfo> leechesInfo = new HashMap<>();
    private Selector selector;
    private final ObservableList<Integer> myPieces;
    private final Set<Integer> announcedPieces = new HashSet<>();
    private final ObservableList<Integer> sentPieces;

    public static class LeechInfo {
        Long lastKeepAliveTime;
        Queue<String> myReplies = new ArrayDeque<>();
        Queue<Piece> pieces = new ArrayDeque<>();
    }

    public UploadHandler(Torrent torrentFile, FileManager fileManager, String peerId,
                         ServerSocketChannel serverSocket,
                         ObservableList<Integer> myPieces, ObservableList<Integer> sentPieces) {
        this.torrentFile = torrentFile;
        this.fileManager = fileManager;
        this.sentPieces = sentPieces;
        this.peerId = peerId;
        this.serverSocketChannel = serverSocket;
        this.myPieces = myPieces;
    }

    @Override
    public void run() {
        try {
            selector = Selector.open();
            int ops = serverSocketChannel.validOps();
            serverSocketChannel.register(selector, ops, null);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        KeepAliveHandler keepAliveHandler = new KeepAliveHandler(selector, leechesInfo);
        keepAliveHandler.start();
        while (true) {
            try {
                handleEvents(selector);
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
        keepAliveHandler.stop();
        try {
            selector.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleEvents(Selector selector) throws IOException {
        selector.select();
        synchronized (myPieces) {
            if (announcedPieces.size() < myPieces.size()) {
                for (Integer piece : myPieces) {
                    if (!announcedPieces.contains(piece)) {
                        announcePiece(piece);
                    }
                }
            }
        }
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        Iterator<SelectionKey> keysIterator = selectedKeys.iterator();
        while (keysIterator.hasNext()) {
            SelectionKey selectionKey = keysIterator.next();
            if (selectionKey.isAcceptable()) {
                SocketChannel client = serverSocketChannel.accept();
                try {
                    establishConnection(client);
                } catch (DifferentHandshakesException e) {
                    System.err.println(e.getMessage());
                }
                System.out.println("=== Connection accepted: " + client.getLocalAddress());
                sendBitfield(client);
            }
            if (selectionKey.isReadable()) {
                SocketChannel client = (SocketChannel) selectionKey.channel();
                Message.MessageInfo message;
                try {
                    message = Message.getMessage(client);
                } catch (BadMessageException e) {
                    System.out.println("=== Closed connection");
                    leechesInfo.remove(client);
                    selectionKey.cancel();
                    keysIterator.remove();
                    continue;
                }
                if (message.type == Message.KEEP_ALIVE) {
                    if (System.currentTimeMillis() - leechesInfo.get(client).lastKeepAliveTime == 0) {
                        System.out.println("=== Close connection");
                        leechesInfo.remove(client);
                        selectionKey.cancel();
                        keysIterator.remove();
                        continue;
                    }
                    leechesInfo.get(client).lastKeepAliveTime = System.currentTimeMillis();
                    keysIterator.remove();
                    continue;
                }
                Piece piece = null;
                try {
                    piece = makeReply(message);
                } catch (BadMessageException e) {
                    e.printStackTrace();
                }
                leechesInfo.get(client).pieces.add(piece);
                client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            }
            if (selectionKey.isWritable()) {
                SocketChannel client = (SocketChannel) selectionKey.channel();
                client.register(selector, SelectionKey.OP_READ);
                while (!leechesInfo.get(client).myReplies.isEmpty()) {
                    String message = leechesInfo.get(client).myReplies.remove();
                    client.write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));
                }
                while (!leechesInfo.get(client).pieces.isEmpty()) {
                    Piece piece = leechesInfo.get(client).pieces.remove();
                    client.write(piece.header);
                    client.write(piece.data);
                    sentPieces.add(1);
                }
            }
            keysIterator.remove();
        }
        if (leechesInfo.isEmpty()) {
            sentPieces.clear();
        }
    }

    private void establishConnection(SocketChannel client) throws DifferentHandshakesException,
            IOException {
        String myHandshake = new BitTorrentHandshake(torrentFile.getInfo_hash(), peerId).getMessage();
        ByteBuffer buf = ByteBuffer.allocate(myHandshake.length());
        client.read(buf);
        PrintWriter out = new PrintWriter(client.socket().getOutputStream(), true);
        out.print(myHandshake);
        out.flush();
        String leechHandshake = new String(buf.array());
        if (!myHandshake.equals(leechHandshake)) {
            throw new DifferentHandshakesException("=== Handshakes are different, reject connection");
        }
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
        LeechInfo leechInfo = new LeechInfo();
        leechInfo.lastKeepAliveTime = 0L;
        leechesInfo.put(client, leechInfo);
    }

    private Piece makeReply(Message.MessageInfo message) throws BadMessageException,
            IOException{
        if (message.type == Message.KEEP_ALIVE) {
            return null;
        }
        int type = message.type;
        if (type == Message.REQUEST) {
            Message.Piece piece = message.piece;
            String reply = ByteOperations.convertIntoBytes(9 + piece.length) + Message.PIECE +
                    ByteOperations.convertIntoBytes(piece.idx) + ByteOperations.convertIntoBytes(piece.begin);
            int offset = piece.idx * torrentFile.getPieceLength().intValue() + piece.begin;
            byte[] data = fileManager.readPiece(torrentFile.getName(), offset, piece.length);
            Piece p = new Piece();
            p.header = ByteBuffer.wrap(reply.getBytes(StandardCharsets.UTF_8));
            p.data = ByteBuffer.wrap(data);
            return p;
        } else if (type == Message.HAVE) {
            if (message.data.length() < 4) {
                throw new BadMessageException("=== Bad length");
            }
            return null;
        }
        throw new BadMessageException("=== Unknown message type: " + type);
    }

    private void sendBitfield(SocketChannel client) throws IOException {
        int totalPiecesCount = torrentFile.getPieces().size();
        int count = totalPiecesCount / 8;
        if (totalPiecesCount % 8 != 0) {
            count++;
        }
        byte[] data = new byte[count];
        for (int i = 0; i < totalPiecesCount; i++) {
            if (!myPieces.contains(i)) {
                continue;
            }
            int bitIdx = i % 8;
            int byteIdx = i / 8;
            data[byteIdx] |= 1 << bitIdx;
            announcedPieces.add(i);
        }
        String bitfieldMsg = ByteOperations.convertIntoBytes(1 + data.length) +
                Message.BITFIELD;
        client.write(ByteBuffer.wrap(bitfieldMsg.getBytes(StandardCharsets.UTF_8)));
        client.write(ByteBuffer.wrap(data));
    }

    private void announcePiece(Integer pieceIdx) throws IOException {
        String haveMsg = ByteOperations.convertIntoBytes(1 + 4) +
                Message.HAVE + ByteOperations.convertIntoBytes(pieceIdx);
        for (SocketChannel client : leechesInfo.keySet()) {
            leechesInfo.get(client).myReplies.add(haveMsg);
//            System.out.println("ANNOUNCE " + pieceIdx);
            client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        }
        announcedPieces.add(pieceIdx);
    }

    private static class Piece {
        ByteBuffer header;
        ByteBuffer data;
    }
}
