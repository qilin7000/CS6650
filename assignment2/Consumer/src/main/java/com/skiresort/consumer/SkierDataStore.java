package com.skiresort.consumer;

import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SkierDataStore {
    private static final Map<Integer, List<JsonObject>> skierRecords = new ConcurrentHashMap<>();

    public static void addLiftRide(int skierID, JsonObject liftRide) {
        skierRecords.computeIfAbsent(skierID, k -> new CopyOnWriteArrayList<>()).add(liftRide);
    }

    public static List<JsonObject> getLiftRides(int skierID) {
        return skierRecords.getOrDefault(skierID, List.of());
    }
}

