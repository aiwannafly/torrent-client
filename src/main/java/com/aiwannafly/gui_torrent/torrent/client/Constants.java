package com.aiwannafly.gui_torrent.torrent.client;

public class Constants {
    public static final int DOWNLOAD_MAX_THREADS_COUNT = 8;
    public static final String TRACKER_URL = "127.0.0.1";
    public static final String STOP_COMMAND = "exit";
    public static final String PREFIX = "[torrent]";
    public static final String POSTFIX = ".torrent";
    public static final String DOWNLOAD_PATH = "downloads\\";
    public static final String UPLOAD_PATH = "uploads\\";
    public static final String TORRENT_PATH = "torrents\\";
    public static final String CONNECTIONS_THREAD_NAME = "Connection handler thread";
    public static final int MAX_KEEP_ALIVE_INTERVAL = 2 * 60 * 1000;
    public static final String KEEP_ALIVE_MESSAGE = "\0\0\0\0";
    public static final int KEEP_ALIVE_SEND_INTERVAL = 5 * 1000;
    public static final char PATH_DIVIDER = '\\';
}
