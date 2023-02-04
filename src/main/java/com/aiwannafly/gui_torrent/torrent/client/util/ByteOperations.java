package com.aiwannafly.gui_torrent.torrent.client.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ByteOperations {
    private final static int INT_LEN = 32;
    private final static int BYTE_LEN = 8;
    private final static int BINARY_SYSTEM_MODE = 2;

    public static String convertIntoBytes(int number) {
        String bin = Integer.toBinaryString(number);
        String fullBinary = String.valueOf('0').repeat(INT_LEN - bin.length()) + bin;
        StringBuilder bytesView = new StringBuilder();
        for (int i = 0; i < INT_LEN / BYTE_LEN; i++) {
            try {
                int nextByte = Integer.parseInt(fullBinary.substring(BYTE_LEN * i, (i + 1) * BYTE_LEN),
                        BINARY_SYSTEM_MODE);
                bytesView.append((char) (nextByte));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return null;
            }
        }
        return bytesView.toString();
    }

    public static int convertFromBytes(String bytes) {
        assert bytes.length() >= 4;
        return ByteBuffer.wrap(bytes.substring(0, 4).getBytes(StandardCharsets.UTF_8)).getInt();
    }

    public static int convertFromBytes(byte[] bytes) {
        assert bytes.length >= 4;
        return ByteBuffer.wrap(bytes).getInt();
    }

    public static byte[] getBytesFromString(String str) {
        byte[] arr = new byte[str.length()];
        for (int i = 0; i < str.length(); i++) {
            arr[i] = (byte) str.charAt(i);
        }
        return arr;
    }
}
