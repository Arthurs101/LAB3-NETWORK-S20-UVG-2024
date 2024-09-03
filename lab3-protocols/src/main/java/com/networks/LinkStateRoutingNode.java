package com.networks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.networks.Abstracts.NetworkNode;

/**
 * Class for implementing the Link State Routing functionality.
 */
public class LinkStateRoutingNode extends NetworkNode {
    private Map<String, Map<String, Integer>> linkStateDB;
    private Map<String, List<Object>> routingTable;
    private Map<String, Integer> costs;
    private int sequenceNumber;
    private boolean verbose;

    public LinkStateRoutingNode(String JID, String password, Map<String, String> neighbors, Map<String, Integer> costs, boolean verbose) {
        super(JID, password, neighbors);
        this.costs = costs;
        this.linkStateDB = new HashMap<>();
        this.routingTable = new HashMap<>();
        this.sequenceNumber = 0;
        this.verbose = verbose;

        // Initialize link state DB with the node's own costs
        this.linkStateDB.put(JID, new HashMap<>(costs));
    }

    @Override
    public void sendMessage(MessageData msg) {
        String nextHop = getNextHop(msg.getTo());
        if (nextHop != null) {
            msg.getPath().add(this.myJID);
            msg = new MessageData(msg.getType(), msg.getFrom(), msg.getTo(), msg.getHops() + 1, msg.getPayload());
            sendMessage(nextHop, msg.toJson());
            log("IMPORTANT", "Forwarded message to " + msg.getTo() + " via " + nextHop);
        } else {
            log("ERROR", "No route to " + msg.getTo());
        }
    }

    @Override
    public void handleMessage(MessageData message) {
        if ("info".equals(message.getType())) {
            floodMessage(message, message.getFrom());
        } else {
            log("IMPORTANT", "Received a message from " + message.getFrom() + ": " + message.toString());
            if (message.getTo().equals(this.myJID)) {
                log("IMPORTANT", "Message reached received " + message.getPayload());
                log("IMPORTANT", "Path taken: " + String.join(" -> ", message.getPath()));
                log("IMPORTANT", "Number of hops: " + message.getHops());
            } else {
                sendMessage(message);
            }
        }
    }

    @Override
    public void floodMessage(MessageData message, String from) {
        if (seenMessages.contains(message.toString())) {
            return;
        }
        seenMessages.add(message.toString());

        // Update the link state database
        updateLinkStateDB(message.getFrom(), message.getPayload());

        // Flood to all neighbors except the sender
        for (String neighbor : this.neighbors.values()) {
            if (!neighbor.equals(from)) {
                sendMessage(neighbor, message.toJson());
            }
        }

        computeRoutingTable();
    }

    public void shareLinkState() {
        this.sequenceNumber += 1;
        MessageData message = new MessageData("info", this.myJID, "all", 0, new Gson().toJson(this.costs));
        for (String neighbor : neighbors.values()) {
            sendMessage(neighbor, message.toJson());
        }
        System.out.println(this.myJID + " has finished sharing state");
    }

    private void updateLinkStateDB(String sourceJid, String costsJson) {
        Map<String, Integer> costs = new Gson().fromJson(costsJson, new com.google.gson.reflect.TypeToken<Map<String, Integer>>() {}.getType());
        this.linkStateDB.put(sourceJid, costs);
        log("INFO", "Updated Link State DB for " + sourceJid);
    }

    private String getNextHop(String destination) {
        if (routingTable.containsKey(destination)) {
            return (String) routingTable.get(destination).get(0);
        }
        return null;
    }

    private void computeRoutingTable() {
        this.routingTable.clear();
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        Set<String> nodes = new HashSet<>(this.linkStateDB.keySet());

        // Initialize distances and add all nodes to the set
        for (String node : nodes) {
            distances.put(node, node.equals(this.myJID) ? 0 : Integer.MAX_VALUE);
            previous.put(node, null);
        }

        while (!nodes.isEmpty()) {
            // Find the node with the minimum distance
            String minNode = null;
            for (String node : nodes) {
                if (minNode == null || distances.get(node) < distances.get(minNode)) {
                    minNode = node;
                }
            }
            if (minNode == null) {
                break;
            }
    
            // Remove the minimum node from the unvisited set
            nodes.remove(minNode);

            // Update distances to neighbors
            Map<String, Integer> neighbors = this.linkStateDB.get(minNode);
            if (neighbors != null) {
                for (Map.Entry<String, Integer> entry : neighbors.entrySet()) {
                    Integer currentDistance = distances.get(minNode);
                    if (currentDistance == null) {
                        currentDistance = Integer.MAX_VALUE;
                    }
                    int alt = (int)currentDistance + (int)entry.getValue();
                    String neighbor = entry.getKey();
                    if (distances.get(neighbor) == null) {
                        distances.put(neighbor,Integer.MAX_VALUE);
                    }
                    if (alt < distances.get(neighbor)) {
                        distances.put(neighbor, alt);
                        previous.put(neighbor, minNode);
                    }
                }
            }
        }

        // Build the routing table
        for (String node : distances.keySet()) {
            if (!node.equals(this.myJID)) {
                List<String> path = new ArrayList<>();
                String current = node;
                while (current != null) {
                    path.add(0, current);
                    current = previous.get(current);
                }
                if (path.size() > 1) {
                    this.routingTable.put(node, Arrays.asList(path.get(1), distances.get(node)));
                }
            }
        }
    }
    public void logNetworkState(){
        System.out.println("--- Network State for" + this.myJID  +"---");
        System.out.println("Neighbors:");
        neighbors.forEach((k,v) -> {
            System.out.println(k + " :" + v);
        });
        System.out.println("Costos:");
        costs.forEach((k,v) -> {
            System.out.println(k + " :" + v);
        });
        System.out.println("Link state DB:");
        linkStateDB.forEach((k,v) -> {
            System.out.println(k);
            v.forEach((t,z) -> {System.out.println(t + " : " + z);});
        });
        System.out.println("Routing Table:");
        this.routingTable.forEach((k,v) -> {
            System.out.println(k + " : " + v.toString());});
        System.out.println("-----------------------------\n");
    }
    private void log(String level, String message) {
        if (verbose || "IMPORTANT".equals(level) || "ERROR".equals(level)) {
            System.out.println(this.myJID + " - " + level + " - " + message);
        }
    }
}
