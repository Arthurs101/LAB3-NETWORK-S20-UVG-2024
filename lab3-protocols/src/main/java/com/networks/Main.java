package com.networks;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Main {
    private static CompletableFuture<Map<String, LinkStateRoutingNode>> initializeNodesSequentially(JsonObject topoConfig, JsonObject namesConfig, String password) {
        Map<String, LinkStateRoutingNode> nodes = new HashMap<>();
        CompletableFuture<Void> futureChain = CompletableFuture.completedFuture(null);

        // Iterate over each node in the topology configuration
        for (String node : topoConfig.keySet()) {
            futureChain = futureChain.thenComposeAsync(ignored -> {
                String jid = namesConfig.get(node).getAsString();

                // Get the neighbors for the node from topo.json
                Map<String, String> neighbors = new HashMap<>();
                Map<String, Integer> costs = new HashMap<>();
                for (JsonElement neighbor : topoConfig.getAsJsonArray(node)) {
                    // Fetch the neighbor's JID using its name from names.json
                    neighbors.put(neighbor.getAsString(), namesConfig.get(neighbor.getAsString()).getAsString());
                    costs.put(neighbor.getAsString(), 1);
                }

                // Create a LinkStateRoutingNode instance
                LinkStateRoutingNode routingNode = new LinkStateRoutingNode(jid, password, neighbors, costs, true);
                nodes.put(node, routingNode);

                // Start the node asynchronously and wait for it to be online
                return CompletableFuture.runAsync(() -> System.out.println("Node initialized: " + jid));
            });
        }

        return futureChain.thenApply(ignored -> nodes);
    }
    public static void main(String[] args) {
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

        // Initialize nodes sequentially
            initializeNodesSequentially(topoConfig, namesConfig, password).thenComposeAsync(nodes -> {
                System.out.println("All nodes are online. Starting LSR propagation...");

                // Step 2: Share LSR propagation for all nodes asynchronously
                CompletableFuture<Void> shareFuture = CompletableFuture.allOf(
                        nodes.values().stream()
                                .map(node -> CompletableFuture.runAsync(node::shareLinkState))
                                .toArray(CompletableFuture[]::new)
                );

                // Step 3: After LSR propagation, wait for routing tables to stabilize
                return shareFuture.thenRunAsync(() -> {
                    try {
                        Thread.sleep(10000); // Wait for 10 seconds for stabilization
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    for (LinkStateRoutingNode node : nodes.values()) {
                        node.logNetworkState();
                    }
                }).thenRunAsync(() -> {
                    // Step 4: Simulate sending a message after stabilization
                    MessageData message = new MessageData(
                        "message",
                        namesConfig.get("F").getAsString(),
                        namesConfig.get("E").getAsString(),
                        0,
                        "Hello my friend"
                    );
                    
                    nodes.get("F").sendMessage(message);
                });
            }).join();  // Block the main thread until all async tasks are done

        try {
            // Close file readers
            namesReader.close();
            topoReader.close();
        } catch (IOException e) {
            System.err.println("coulnd close readers, error: " + e.getMessage());
        }catch (Exception e){
            System.err.println(e.getMessage());
        }

    }                 
}
