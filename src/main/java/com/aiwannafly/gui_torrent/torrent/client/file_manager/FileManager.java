package com.aiwannafly.gui_torrent.torrent.client.file_manager;

import java.io.IOException;

public interface FileManager extends AutoCloseable {

    /** Reads a piece of a file
     @param offset - offset in the file
     @param length - length of a piece in bytes
     @return a piece in this location presented as a byte array
     */
    byte[] readPiece(String fileName, int offset, int length) throws IOException;

    /** Writes a piece into a file
     @param offset - offset in the file
     @param piece -  a piece in this location presented as a byte array
     */
    void writePiece(String fileName, int offset, byte[] piece) throws IOException;

}
