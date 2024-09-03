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
    private static void LaunchFloodingNetwork(JsonObject topoConfig, JsonObject namesConfig, String password, String NodeTOUse) {
        try (Scanner scan = new Scanner(System.in)) {
            Map<String, FloodingRoutingNode> nodes = new HashMap<>();
            if (topoConfig.has(NodeTOUse)) {
                // Get the JID for the node from names.json
                String jid = namesConfig.get(NodeTOUse).getAsString();

                // Get the neighbors for the selected node from topo.json
                Map<String, String> neighbors = new HashMap<>();
                for (JsonElement neighbor : topoConfig.getAsJsonArray(NodeTOUse)) {
                    // Fetch the neighbor's JID using its name from names.json
                    neighbors.put(neighbor.getAsString(), namesConfig.get(neighbor.getAsString()).getAsString());
                }

                // Initialize only the selected node
                FloodingRoutingNode floodingNode = new FloodingRoutingNode(jid, password, neighbors);
                nodes.put(NodeTOUse, floodingNode);
            } else {
                System.out.println("Node " + NodeTOUse + " is not found in the topology.");
                return;
            }

            String option = "";
            while (!option.equals("exit")) {
                System.out.println("""
                msg: enviar un mensaje a otro nodo
                exit: salir
                """);
                option = scan.nextLine();
                switch (option) {
                    case "msg" -> {
                        System.out.println("Selecciona el nodo a recibir: ");
                        String recipt = scan.nextLine();
                        System.out.println("ingresa tu mensaje: ");
                        String message = scan.nextLine();
                        nodes.get(NodeTOUse).sendMessage(namesConfig.get(recipt).getAsString(), message);
                        
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

    private static CompletableFuture<Map<String, LinkStateRoutingNode>> initializeNodesSequentially(JsonObject topoConfig, JsonObject namesConfig, String password, String nodeToUse) {
        Map<String, LinkStateRoutingNode> nodes = new HashMap<>();
        CompletableFuture<Void> futureChain = CompletableFuture.completedFuture(null);
        
        if (topoConfig.has(nodeToUse)) {
            futureChain = futureChain.thenComposeAsync(ignored -> {
                String jid = namesConfig.get(nodeToUse).getAsString();

                // Get the neighbors for the node from topo.json
                Map<String, String> neighbors = new HashMap<>();
                Map<String, Integer> costs = new HashMap<>();
                for (JsonElement neighbor : topoConfig.getAsJsonArray(nodeToUse)) {
                    // Fetch the neighbor's JID using its name from names.json
                    neighbors.put(neighbor.getAsString(), namesConfig.get(neighbor.getAsString()).getAsString());
                    costs.put(namesConfig.get(neighbor.getAsString()).getAsString(), 1);
                }

                // Create a LinkStateRoutingNode instance
                LinkStateRoutingNode routingNode = new LinkStateRoutingNode(jid, password, neighbors, costs, false);
                nodes.put(nodeToUse, routingNode);

                // Start the node asynchronously and wait for it to be online
                return CompletableFuture.runAsync(() -> System.out.println("Node initialized: " + jid));
            });
        } else {
            System.out.println("Node " + nodeToUse + " is not found in the topology.");
            return CompletableFuture.completedFuture(nodes);
        }

        return futureChain.thenApply(ignored -> nodes);
    }

    private static void LaunchLSRNetwork(JsonObject topoConfig, JsonObject namesConfig, String password, String nodeToUse) {
        initializeNodesSequentially(topoConfig, namesConfig, password, nodeToUse).thenComposeAsync(nodes -> {
            if (nodes.isEmpty()) {
                System.out.println("No nodes were initialized.");
                return CompletableFuture.completedFuture(null);
            }
            System.out.println("Is everyone connected? (yes/no)");
            Scanner scan1 = new Scanner(System.in);
            String inputs = "";
            while(!"yes".equals(inputs)) {
                inputs = scan1.nextLine();
            }
            System.out.println("Node is online. Starting LSR propagation...");

            CompletableFuture<Void> shareFuture = CompletableFuture.runAsync(nodes.get(nodeToUse)::shareLinkState);

            return shareFuture.thenRunAsync(() -> {
                nodes.get(nodeToUse).logNetworkState();
                System.out.println("System is ready");

                try (Scanner scan = new Scanner(System.in)) {
                    String option2 = "";
                    while (!option2.equals("exit")) {
                        System.out.println("""
                        msg: enviar un mensaje desde un nodo a otro
                        exit: salir 
                        """);
                        option2 = scan.nextLine();
                        switch (option2) {
                            case "msg" -> {
                                System.out.println("nodo a recibir el mensaje: ");
                                String recipt = scan.nextLine();
                                System.out.println("ingresa tu mensaje: ");
                                String payload = scan.nextLine();
                                MessageData message = new MessageData(
                                        "message",
                                        namesConfig.get(nodeToUse).getAsString(),
                                        namesConfig.get(recipt).getAsString(),
                                        0,
                                        payload
                                );
                                nodes.get(nodeToUse).sendMessage(message);
                                
                            }
                        }
                    }
                    System.out.println("terminating the nodes...");
                    nodes.forEach((node, instance) -> {
                        System.out.println("Terminating node " + node);
                        instance.terminateNode();
                    });
                }
            });
        }).join();  // Block the main thread until all async tasks are done
    }

    public static void main(String[] args) {
        String password = "prueba2024";
        Scanner scan = new Scanner(System.in);

        // Reading the Name config json file
        InputStream namesStream = Main.class.getClassLoader().getResourceAsStream("names-priv.json");
        if (namesStream == null) {
            throw new RuntimeException("names.json not found in resources directory");
        }
        InputStreamReader namesReader = new InputStreamReader(namesStream, StandardCharsets.UTF_8);
        JsonObject namesJson = JsonParser.parseReader(namesReader).getAsJsonObject();

        // Reading the Topology config json file
        InputStream topoStream = Main.class.getClassLoader().getResourceAsStream("topology.json");
        if (topoStream == null) {
            throw new RuntimeException("topology.json not found in resources directory");
        }
        InputStreamReader topoReader = new InputStreamReader(topoStream, StandardCharsets.UTF_8);
        JsonObject topoJson = JsonParser.parseReader(topoReader).getAsJsonObject();

        JsonObject topoConfig = topoJson.getAsJsonObject("config");
        JsonObject namesConfig = namesJson.getAsJsonObject("config");

        System.out.println("Ingrese el nodo a emplear: ");
        String NodeTOUse = scan.nextLine();
        System.out.println("1. Uso de Flooding Network");
        System.out.println("2. Uso de Linked State Routing Network");
        String input = scan.nextLine();
        switch (input) {
            case "1" -> LaunchFloodingNetwork(topoConfig, namesConfig, password, NodeTOUse);
            case "2" -> LaunchLSRNetwork(topoConfig, namesConfig, password, NodeTOUse);
            default -> System.out.println("not valid option, 1 or 2");
        }

        try {
            namesReader.close();
            topoReader.close();
        } catch (IOException e) {
            System.err.println("Could not close readers, error: " + e.getMessage());
        }
    }
}
