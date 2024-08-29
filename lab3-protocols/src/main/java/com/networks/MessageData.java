package com.networks;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

public class MessageData {
    private final String type;
    private final String from;
    private final String to;
    private final int hops;
    private final String payload;
    private List<String> path;
    // Constructor
    public MessageData(String type, String from, String to, int hops, String payload) {
        this.type = type;
        this.from = from;
        this.to = to;
        this.hops = hops;
        this.payload = payload;
        this.path = new ArrayList<>();
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
    
    public List<String> getPath(){
        return this.path;
    }
    
    //Setters
    public void setPath(List<String> path) {
        this.path = path;
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
