package com.networks;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Main {
    private static void MenuLoop(Map<String, FloodingRoutingNode> nodes){
        try (Scanner myObj = new Scanner(System.in)) {
            String action = "";  // Read user input
            while(!action.equals("quit")){
                System.out.println("Awaiting for action ");
                action = myObj.nextLine();
            }
        }
        nodes.forEach((ID,node) ->{
            node.terminateNode();
            System.out.println("Terminated node :" + ID + " - " + node.getJid());
        });
    }
    public static void main(String[] args) {
        Map<String, FloodingRoutingNode> nodes = new HashMap<>();
        String password = "prueba2024"; // all have the same, dont ask question k?


        /* 
        Reading the Name config json file usint Input strams
         */
        // Read the names.json file via InputStream
        InputStream namesStream = Main.class.getClassLoader().getResourceAsStream("names-priv.json");
        if (namesStream == null) {
            throw new RuntimeException("names.json not found in resources directory");
        }

        //Create a reader to get the raw date
        InputStreamReader namesReader = new InputStreamReader(namesStream, StandardCharsets.UTF_8);
        JsonObject namesJson = JsonParser.parseReader(namesReader).getAsJsonObject(); //parse into a json object

        
        /* 
            Reading the Topology config json file usint Input strams
        */

        // Read the topology.json file via a input stream
        InputStream topoStream = Main.class.getClassLoader().getResourceAsStream("topology.json");
        if (topoStream == null) {
            throw new RuntimeException("topology.json not found in resources directory");
        }

        //Create a reader to get the raw date
        InputStreamReader topoReader = new InputStreamReader(topoStream, StandardCharsets.UTF_8);
        JsonObject topoJson = JsonParser.parseReader(topoReader).getAsJsonObject(); //parse into a json object



        /**
         * Obtaining and creating the nodes for the configuration properties of both jsons
         */

        //the configs of the topology of neighbours 
        // node_name : ["node_name"] : dictionary of lists basiically
        JsonObject topoConfig = topoJson.getAsJsonObject("config");
        System.out.println(topoConfig); 

        //the configs of the topology of neighbours 
        // "node_ID" : "node_JID": dictionary of ID and lists of each node
        JsonObject namesConfig = namesJson.getAsJsonObject("config");
        System.out.println(namesConfig); 

        // Iterate over each node in the topology configuration
        for (String node : topoConfig.keySet()) {
            // Get the JID for the node from names.json
            String jid = namesConfig.get(node).getAsString();

            // Get the neighbors for the node from topo.json
            Map<String, String> neighbors = new HashMap<>();
            for (JsonElement neighbor : topoConfig.getAsJsonArray(node)) {
                // Fetch the neighbor's JID using its name from names.json
                // is a  hashmap of node_ID : Node_JID
                neighbors.put(neighbor.getAsString(), namesConfig.get(neighbor.getAsString()).getAsString());
            }

            // Create a FloodingRoutingNode instance
            FloodingRoutingNode floodingNode = new FloodingRoutingNode(jid, password, neighbors);
            nodes.put(node, floodingNode);
        }

        try {
            // Close file readers
            namesReader.close();
            topoReader.close();
        } catch (IOException e) {
            System.err.println("coulnd close readers, error: " + e.getMessage());
        }catch (Exception e){
            System.err.println(e.getMessage());
        }
        System.out.println("Nodes created: " + nodes.keySet());

        //test sending a message to the server using the node A to H
        System.out.println("Sending test message to the server");
        
        nodes.get("A").sendMessage("Hello my friend", namesConfig.get("H").getAsString());
        MenuLoop(nodes);
    }

    
}
