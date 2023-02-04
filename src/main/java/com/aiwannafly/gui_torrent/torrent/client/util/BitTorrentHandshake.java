package com.aiwannafly.gui_torrent.torrent.client.util;

public class BitTorrentHandshake implements Handshake {
    private static final String PSTR = "BitTorrent protocol";
    private static final int PSTRLEN = 19;
    private static final int RESERVED_LEN = 8;
    private final String string;

    public BitTorrentHandshake(String string) {
        this.string = string;
    }

    public BitTorrentHandshake(String infoHash, String peerId) {
        string = (char) PSTRLEN +
                PSTR +
                String.valueOf((char) 0).repeat(RESERVED_LEN) +
                infoHash +
                peerId;
    }

    public String getMessage() {
        return string;
    }

    public String getInfoHash() {
        return string.substring(20 + RESERVED_LEN, 20 + RESERVED_LEN + 40);
    }

    public String getPeerId() {
        return string.substring(20 + RESERVED_LEN + 40);
    }
}
