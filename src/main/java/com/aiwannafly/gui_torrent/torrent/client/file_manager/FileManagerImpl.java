package com.aiwannafly.gui_torrent.torrent.client.file_manager;

import com.aiwannafly.gui_torrent.torrent.client.Constants;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileManagerImpl implements FileManager {
    private final Map<String, RandomAccessFile> files = new HashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public synchronized byte[] readPiece(String fileName, int offset, int length) throws IOException {
        if (!files.containsKey(fileName)) {
            RandomAccessFile randomAccessFile = new RandomAccessFile(Constants.UPLOAD_PATH + fileName, "r");
            files.put(fileName, randomAccessFile);
        }
        RandomAccessFile file = files.get(fileName);
        byte[] piece = new byte[length];
        try {
            file.seek(offset);
            int readBytes = file.read(piece);
            if (readBytes != length) {
                System.err.println("Read just " + readBytes + " / " + length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return piece;
    }

    @Override
    public synchronized void writePiece(String fileName, int offset, byte[] piece) throws IOException {
        if (!files.containsKey(fileName)) {
            RandomAccessFile randomAccessFile = new RandomAccessFile(Constants.DOWNLOAD_PATH + fileName, "rw");
            files.put(fileName, randomAccessFile);
        }
        RandomAccessFile file = files.get(fileName);
        executor.execute((() -> {
            try {
                file.seek(offset);
                file.write(piece);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }

    @Override
    public void close() throws IOException {
        executor.shutdown();
        for (RandomAccessFile file: files.values()) {
            file.close();
        }
    }
}
