package com.aiwannafly.gui_torrent.torrent.client.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class TorrentFileCreator {
    public static final int PIECE_SIZE = 512 * 1024;

    private static void encodeObject(Object o, OutputStream out) throws IOException {
        if (o instanceof String) {
            encodeString((String) o, out);
        } else if (o instanceof Map) {
            encodeMap((Map) o, out);
        } else if (o instanceof byte[]) {
            encodeBytes((byte[]) o, out);
        } else if (o instanceof Number) {
            encodeLong(((Number) o).longValue(), out);
        } else {
            throw new Error("Unencodable type");
        }
    }

    private static void encodeLong(long value, OutputStream out) throws IOException {
        out.write('i');
        out.write(Long.toString(value).getBytes(StandardCharsets.US_ASCII));
        out.write('e');
    }

    private static void encodeBytes(byte[] bytes, OutputStream out) throws IOException {
        out.write(Integer.toString(bytes.length).getBytes(StandardCharsets.US_ASCII));
        out.write(':');
        out.write(bytes);
    }

    private static void encodeString(String str, OutputStream out) throws IOException {
        encodeBytes(str.getBytes(StandardCharsets.UTF_8), out);
    }

    private static void encodeMap(Map<String, Object> map, OutputStream out) throws IOException {
        // Sort the map. A generic encoder should sort by key bytes
        SortedMap<String, Object> sortedMap = new TreeMap<String, Object>(map);
        out.write('d');
        for (Map.Entry<String, Object> e : sortedMap.entrySet()) {
            encodeString(e.getKey(), out);
            encodeObject(e.getValue(), out);
        }
        out.write('e');
    }

    private static byte[] hashPieces(File file, int pieceLength) throws IOException {
        MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            throw new Error("SHA1 not supported");
        }
        InputStream in = new FileInputStream(file);
        ByteArrayOutputStream pieces = new ByteArrayOutputStream();
        byte[] bytes = new byte[pieceLength];
        int pieceByteCount = 0, readCount = in.read(bytes, 0, pieceLength);
        while (readCount != -1) {
            pieceByteCount += readCount;
            sha1.update(bytes, 0, readCount);
            if (pieceByteCount == pieceLength) {
                pieceByteCount = 0;
                pieces.write(sha1.digest());
            }
            readCount = in.read(bytes, 0, pieceLength - pieceByteCount);
        }
        in.close();
        if (pieceByteCount > 0)
            pieces.write(sha1.digest());
        return pieces.toByteArray();
    }

    public static void createTorrent(File file, File sharedFile, String announceURL) throws IOException {
        final int pieceLength = PIECE_SIZE;
        Map<String, Object> info = new HashMap<>();
        info.put("name", sharedFile.getName());
        info.put("length", sharedFile.length());
        info.put("piece length", pieceLength);
        info.put("pieces", hashPieces(sharedFile, pieceLength));
        Map<String, Object> metaInfo = new HashMap<>();
        metaInfo.put("announce", announceURL);
        metaInfo.put("info", info);
        OutputStream out = new FileOutputStream(file);
        encodeMap(metaInfo, out);
        out.close();
    }
}
