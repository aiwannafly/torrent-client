package com.aiwannafly.gui_torrent.torrent.client.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Flow;

public class ObservableList<T> extends ArrayList<T> implements Flow.Publisher<Boolean> {
    private final Set<Flow.Subscriber<? super Boolean>> subscribers = new HashSet<>();

    @Override
    public boolean add(T elem) {
        notifyObservers();
        return super.add(elem);
    }

    @Override
    public T remove(int idx) {
        notifyObservers();
        return super.remove(idx);
    }

    @Override
    public void clear() {
        notifyObservers();
        super.clear();
    }

    @Override
    public void subscribe(Flow.Subscriber<? super Boolean> subscriber) {
        subscribers.add(subscriber);
    }

    private void notifyObservers() {
        for (Flow.Subscriber<? super Boolean> subscriber: subscribers) {
            subscriber.onNext(true);
        }
    }
}