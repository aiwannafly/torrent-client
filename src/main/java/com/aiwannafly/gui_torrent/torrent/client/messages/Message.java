package com.aiwannafly.gui_torrent.torrent.client.messages;

import com.aiwannafly.gui_torrent.torrent.client.exceptions.BadMessageException;
import com.aiwannafly.gui_torrent.torrent.client.util.ByteOperations;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Message {
    public static final int CHOKE = 0;
    public static final int UNCHOKE = 1;
    public static final int INTERESTED = 2;
    public static final int NOT_INTERESTED = 3;
    public static final int HAVE = 4;
    public static final int BITFIELD = 5;
    public static final int REQUEST = 6;
    public static final int PIECE = 7;
    public static final int CANCEL = 8;
    public static final int KEEP_ALIVE = 9;

    public static class MessageInfo {
        public int length;
        public int type;
        public String data;
        public Piece piece;
        public byte[] bitfield;
    }

    public static class Piece {
        public byte[] data;
        public int idx;
        public int begin;
        public int length;
    }

    public static MessageInfo getMessage(SocketChannel client) throws IOException, BadMessageException {
        ByteBuffer lengthBuf = ByteBuffer.allocate(4);
        try {
            client.read(lengthBuf);
        } catch (SocketException e) {
            throw new BadMessageException(e.getMessage());
        }
        MessageInfo message = new MessageInfo();
        String lengthStr = new String(lengthBuf.array());
        message.length = ByteOperations.convertFromBytes(lengthStr);
        if (message.length == 0) {
            message.type = KEEP_ALIVE;
            return message;
        }
        if (message.length < 0) {
            throw new BadMessageException("=== Bad length");
        }
        ByteBuffer typeBuf = ByteBuffer.allocate(1);
        client.read(typeBuf);
        String typeStr = new String(typeBuf.array());
        message.type = Integer.parseInt(typeStr);
        message.length--;
        if (message.type == CHOKE || message.type == UNCHOKE ||
                message.type == INTERESTED || message.type == NOT_INTERESTED) {
            return message;
        }
        if (message.type == HAVE || message.type == BITFIELD) {
            ByteBuffer messageBuf = ByteBuffer.allocate(message.length);
            int count = client.read(messageBuf);
            if (count != message.length) {
                System.err.println("Read just " + count + " / " + message.length + " bytes.");
            }
            if (message.type == HAVE) {
                message.piece = new Piece();
                message.piece.idx = ByteOperations.convertFromBytes(new String(messageBuf.array()));
                return message;
            }
            message.bitfield = messageBuf.array();
            return message;
        }
        if (message.type == PIECE || message.type == CANCEL || message.type == REQUEST) {
            Piece piece = new Piece();
            message.piece = piece;
            ByteBuffer buf = ByteBuffer.allocate(4);
            client.read(buf);
            message.length -= 4;
            piece.idx = ByteOperations.convertFromBytes(new String(buf.array()));
            buf.clear();
            client.read(buf);
            message.length -= 4;
            piece.begin = ByteOperations.convertFromBytes(new String(buf.array()));
            if (message.type == CANCEL || message.type == REQUEST) {
                buf.clear();
                client.read(buf);
                message.length -= 4;
                piece.length = ByteOperations.convertFromBytes(new String(buf.array()));
                return message;
            }
            InputStream in = client.socket().getInputStream();
            piece.data = new byte[message.length];
            int count = in.read(piece.data);
            while (count != message.length) {
//                System.out.println("count: " + count + " / " + message.length);
                int return_value = in.read(piece.data, count, message.length - count);
                count += return_value;
            }
//            System.out.println("count: " + count + " / " + message.length);
            piece.length = count;
            return message;
        }
        throw new BadMessageException("=== Unknown type: " + message.type);
    }

    public static MessageInfo getRMessage(SocketChannel client) throws IOException, BadMessageException {
        InputStream in = client.socket().getInputStream();
        byte[] lengthBuf = new byte[4];
        try {
            int count = in.read(lengthBuf);
        } catch (SocketException e) {
            throw new BadMessageException(e.getMessage());
        }
        MessageInfo message = new MessageInfo();
        message.length = ByteOperations.convertFromBytes(lengthBuf);
        System.out.println("MESSAGE LENGTH " + message.length);
        if (message.length == 0) {
            message.type = KEEP_ALIVE;
            return message;
        }
        System.out.println(Integer.toBinaryString(message.length));
        if (message.length < 0) {
            throw new BadMessageException("=== Bad length: " + message.length);
        }
        byte[] typeBuf = new byte[1];
        int count = in.read(typeBuf);
        String typeStr = String.valueOf((char) typeBuf[0]);
        try {
            message.type = Integer.parseInt(typeStr);
        } catch (NumberFormatException e) {
            throw new BadMessageException("=== Bad type, not int. Length: " + message.length);
        }
        System.out.println("MESSAGE TYPE: " + message.type);
        message.length--;
        if (message.type == CHOKE || message.type == UNCHOKE ||
            message.type == INTERESTED || message.type == NOT_INTERESTED) {
            return message;
        }
        if (message.type == HAVE || message.type == BITFIELD) {
            byte[] messageBuf = new byte[message.length];
            count = in.read(messageBuf);
            if (count != message.length) {
                System.err.println("Read just " + count + " / " + message.length + " bytes.");
            }
            if (message.type == HAVE) {
                message.piece = new Piece();
                message.piece.idx = ByteOperations.convertFromBytes(messageBuf);
                return message;
            }
            message.bitfield = messageBuf;
            return message;
        }
        if (message.type == PIECE || message.type == CANCEL || message.type == REQUEST) {
            Piece piece = new Piece();
            message.piece = piece;
            byte[] buf = new byte[4];
            count = in.read(buf);
            message.length -= 4;
            piece.idx = ByteOperations.convertFromBytes(buf);
            count = in.read(buf);
            message.length -= 4;
            piece.begin = ByteOperations.convertFromBytes(buf);
            if (message.type == CANCEL || message.type == REQUEST) {
                count = in.read(buf);
                message.length -= 4;
                piece.length = ByteOperations.convertFromBytes(buf);
                return message;
            }
            piece.data = new byte[message.length];
            count = in.read(piece.data);
            while (count != message.length) {
                int return_value = in.read(piece.data, count, message.length - count);
                count += return_value;
            }
            piece.length = count;
            return message;
        }
        throw new BadMessageException("=== Unknown type: " + message.type);
    }
}
