package com.google.firebase.firestore.internal.cpp;

import java.util.HashMap;

public final class JniDemo {

    private final HashMap<Integer, Object> map = new HashMap<>();
    private int nextId = 0;

    public synchronized int put(Object obj) {
        if (obj == null) {
            throw new NullPointerException("obj==null");
        }
        int id = nextId++;
        map.put(id, obj);
        return id;
    }

    public synchronized Object get(int id) {
        return map.get(id);
    }

    public synchronized void remove(int id) {
        map.remove(id);
    }

    public synchronized int size() {
        return map.size();
    }

    public synchronized boolean isEmpty() {
        return map.isEmpty();
    }

}
