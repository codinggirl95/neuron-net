package com.myapp.neuron.net.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryCache {
    private final Map<String, String> store = new ConcurrentHashMap<>();

    public boolean putIfAbsent(String key, String value) {
        return store.putIfAbsent(key, value) == null;
    }

    public void put(String key, String value) {
        store.put(key, value);
    }

    public String get(String key) {
        return store.get(key);
    }

    public boolean exists(String key) {
        return store.containsKey(key);
    }

    public boolean delete(String key) {
        return store.remove(key) != null;
    }
}
