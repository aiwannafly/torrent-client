package com.aiwannafly.gui_torrent.torrent.client.uploader;

import com.aiwannafly.gui_torrent.torrent.client.Constants;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;

public class KeepAliveHandler {
    private final Timer keepAliveSendTimer = new Timer();
    private final Timer keepAliveReceiveTimer = new Timer();
    private final Map<SocketChannel, UploadHandler.LeechInfo> leechesInfo;
    private final ArrayList<SocketChannel> removalList = new ArrayList<>();
    private final Selector selector;

    public KeepAliveHandler(Selector selector, Map<SocketChannel, UploadHandler.LeechInfo> leechesInfo) {
        this.selector = selector;
        this.leechesInfo = leechesInfo;
    }

    public void start() {
        TimerTask sendKeepAliveTask = new SendKeepAliveTask();
        TimerTask receiveKeepAliveTask = new ReceiveKeepAliveTask();
        keepAliveSendTimer.schedule(sendKeepAliveTask, 0, Constants.KEEP_ALIVE_SEND_INTERVAL);
        keepAliveReceiveTimer.schedule(receiveKeepAliveTask, 0, Constants.MAX_KEEP_ALIVE_INTERVAL);
    }

    public void stop() {
        keepAliveReceiveTimer.cancel();
        keepAliveSendTimer.cancel();
    }

    private class SendKeepAliveTask extends TimerTask {
        @Override
        public void run() {
            for (SocketChannel client : leechesInfo.keySet()) {
                leechesInfo.get(client).myReplies.add(Constants.KEEP_ALIVE_MESSAGE);
                try {
                    client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                } catch (ClosedChannelException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class ReceiveKeepAliveTask extends TimerTask {
        @Override
        public void run() {
            removalList.clear();
            for (SocketChannel client : leechesInfo.keySet()) {
                if (getTimeFromLastKeepAlive(client) > Constants.MAX_KEEP_ALIVE_INTERVAL) {
                    // close connection
                    removalList.add(client);
                }
            }
            if (removalList.isEmpty()) {
                return;
            }
            for (SocketChannel client : removalList) {
                leechesInfo.remove(client);
            }
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keysIterator = selectedKeys.iterator();
            while (keysIterator.hasNext()) {
                SelectionKey myKey = keysIterator.next();
                if (removalList.contains((SocketChannel) myKey.channel())) {
                    myKey.cancel();
                }
                keysIterator.remove();
            }
        }
    }

    private long getTimeFromLastKeepAlive(SocketChannel client) {
        if (0 == leechesInfo.get(client).lastKeepAliveTime) {
            return 0;
        }
        return System.currentTimeMillis() - leechesInfo.get(client).lastKeepAliveTime;
    }
}
