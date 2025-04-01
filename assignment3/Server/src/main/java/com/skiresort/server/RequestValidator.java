package com.skiresort.server;

import com.google.gson.JsonObject;

public class RequestValidator {

    public static boolean isUrlValid(String urlPath) {
        if (urlPath == null || urlPath.isEmpty()) {
            return false;
        }
        String[] parts = urlPath.split("/");
        return parts.length == 8 && "seasons".equals(parts[2]) && "days".equals(parts[4]) && "skiers".equals(parts[6]);
    }

    public static int extractSkierID(String urlPath) {
        return Integer.parseInt(urlPath.split("/")[7]);
    }

    public static String extractSeason(String urlPath) {
        return urlPath.split("/")[3];
    }
    public static int extractResortID(String urlPath) {
        return Integer.parseInt(urlPath.split("/")[1]);
    }

    public static String extractDay(String urlPath) {
        return urlPath.split("/")[5];
    }

    public static boolean isPayloadValid(JsonObject json) {
        return json.has("liftID") && json.has("time")
                && json.get("liftID").isJsonPrimitive()
                && json.get("time").isJsonPrimitive()
                && json.get("liftID").getAsInt() > 0
                && json.get("time").getAsInt() > 0 && json.get("time").getAsInt() <= 360;
    }

}

