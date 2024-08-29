package com.networks;

import com.google.gson.Gson;

public class MessageData {
    private final String type;
    private final String from;
    private final String to;
    private final int hops;
    private final String payload;

    // Constructor
    public MessageData(String type, String from, String to, int hops, String payload) {
        this.type = type;
        this.from = from;
        this.to = to;
        this.hops = hops;
        this.payload = payload;
    }

    // Getters
    public String getType() {
        return type;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public int getHops() {
        return hops;
    }

    public String getPayload() {
        return payload;
    }

    // Convert object to JSON string
    public String toJson() {
        return new Gson().toJson(this);
    }

    // Parse JSON string to object
    public static MessageData fromJson(String json) {
        return new Gson().fromJson(json, MessageData.class);
    }
    @Override
    public String toString() {
        return  this.type+"-"+this.from+'-'+this.to+'-';
    }
}
