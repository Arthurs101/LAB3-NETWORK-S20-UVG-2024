package com.networks;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Main {
    private static void LaunchFloodingNetwork(JsonObject topoConfig, JsonObject namesConfig, String password){
        try (Scanner scan = new Scanner(System.in)) {
            Map<String, FloodingRoutingNode> nodes = new HashMap<>();
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
            String option = "";
            while (!option.equals("exit")){
                System.out.println("""
                nodes: ver los nodos
                msg: enviar un mensaje desde un nodo a otro
                exit: salir y terminar nodos
                """);
                option = scan.nextLine();
                switch (option) {
                    case "msg" ->{
                        System.out.println("Selecciona el nodo a usar: ");
                        nodes.forEach((k,v) -> {
                            System.out.println(k + " : " + v.getJid());
                        });
                        String emisor = scan.nextLine();
                        if (!nodes.containsKey(emisor)){
                            System.out.println("Error: " + emisor + " not found");
                        }else{
                            System.out.println("Selecciona el nodo a recibir el mensaje: ");
                            String recipt = scan.nextLine();
                            if (!nodes.containsKey(recipt)){
                                System.out.println("Error: " + recipt + " not found");
                            }else{
                                System.out.println("ingresa tu mensaje: ");
                                String message = scan.nextLine();
                                nodes.get(emisor).sendMessage(namesConfig.get(recipt).getAsString(),message);
                               
                            }
                            
                        }
                    }
                    case "nodes" -> {
                        nodes.forEach((k,v) -> {
                            System.out.println(k + " : " + v.getJid());
                        });
                    }
                    default -> System.out.println("unrecognized option");
                }
            }
            System.out.println("terminating the nodes...");
            nodes.forEach((node, instance) -> {
                System.out.println("Terminating node " + node);
                instance.terminateNode();
            });
        }
    }

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
                    costs.put(namesConfig.get(neighbor.getAsString()).getAsString(), 1);
                }

                // Create a LinkStateRoutingNode instance
                LinkStateRoutingNode routingNode = new LinkStateRoutingNode(jid, password, neighbors, costs, false);
                nodes.put(node, routingNode);

                // Start the node asynchronously and wait for it to be online
                return CompletableFuture.runAsync(() -> System.out.println("Node initialized: " + jid));
            });
        }

        return futureChain.thenApply(ignored -> nodes);
    }
    private static void LaunchLSRNetwork(JsonObject topoConfig, JsonObject namesConfig, String password){
        
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
                    Scanner scan = new Scanner(System.in);
                    System.out.println(" System is ready");
                    // Step 4: Simulate sending a message after stabilization
                    String option2 = "";
                while (!option2.equals("exit")){
                    System.out.println("""
                    nodes: ver los nodos
                    msg: enviar un mensaje desde un nodo a otro
                    exit: salir y terminar nodos
                    """);
                    option2 = scan.nextLine();
                    switch (option2) {
                        case "msg" ->{
                            System.out.println("Selecciona el nodo a usar: ");
                            nodes.forEach((k,v) -> {
                                System.out.println(k + " : " + v.getJid());
                            });
                            String emisor = scan.nextLine();
                            if (!nodes.containsKey(emisor)){
                                System.out.println("Error: " + emisor + " not found");
                            }else{
                                System.out.println("Selecciona el nodo a recibir el mensaje: ");
                                String recipt = scan.nextLine();
                                if (!nodes.containsKey(recipt)){
                                    System.out.println("Error: " + recipt + " not found");
                                }else{
                                    System.out.println("ingresa tu mensaje: ");
                                    String payload = scan.nextLine();
                                    MessageData message = new MessageData(
                                        "message",
                                        namesConfig.get(emisor).getAsString(),
                                        namesConfig.get(recipt).getAsString(),
                                        0,
                                        payload
                                    );
                                    nodes.get(emisor).sendMessage(message);
                                }
                                
                            }
                        }
                        case "nodes" -> {
                            nodes.forEach((k,v) -> {
                                System.out.println(k + " : " + v.getJid());
                            });
                        }
                        default -> System.out.println("unrecognized option");
                    }
                }
                System.out.println("terminating the nodes...");
                nodes.forEach((node, instance) -> {
                    System.out.println("Terminating node " + node);
                    instance.terminateNode();
                });
                   
                });
            }).join();  // Block the main thread until all async tasks are done
        
    }
    public static void main(String[] args) {
        String password = "prueba2024"; // all have the same
        Scanner scan = new Scanner(System.in);  // Create a Scanner object

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
        
        System.out.println("1. uso de flooding network");
        System.out.println("2. Uso de Linked State Routing Network");
        String input = scan.nextLine();
        switch (input) {
            case "1" -> {
                LaunchFloodingNetwork(topoConfig,namesConfig,password);
            }
            case "2" -> {
                LaunchLSRNetwork(topoConfig,namesConfig,password);
            }
            default -> System.out.println("not valid option, 1 or 2");
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

    }                 
}
