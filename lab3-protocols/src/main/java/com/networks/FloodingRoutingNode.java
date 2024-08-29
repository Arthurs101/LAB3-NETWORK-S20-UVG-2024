package com.networks;

import java.util.Map;
/**
 * Node for a flooding algorithm
 */
public class FloodingRoutingNode {
    private XmppClient client;
    private Map<String, String> neighbors;
    private String myJID;

    /**
     * @param neighbours Map of neighbours, where is ID:JID the keypairs
     * @param JID the jid for the node to connect to the server
     * @param pwrd the password for the node
    */
    public FloodingRoutingNode(String JID, String pwrd, Map<String, String> neighbors) {
        try {
            this.client = new XmppClient(JID,pwrd );
        } catch (Exception e) {
            System.err.println("Failed to connect node into the network " + JID + ": " + e.getMessage());
        }
        this.neighbors = neighbors;
    }

    /**
     * Method to flood a message to all neighbors except the sender
     * @param  MessageData message instance of the message to send
     * @param String from , the user that sent the original message
     */
    public void floodMessage(MessageData message, String from) {
        neighbors.values().forEach(neighborJid -> {
            if (!neighborJid.equals(from)) {
                try {
                    MessageData forwardedMessage = new MessageData(
                        message.getType(),
                        message.getFrom(),
                        message.getTo(),
                        message.getHops() + 1,
                        message.getPayload()
                    );
                    client.sendMessage(neighborJid, forwardedMessage.toJson());
                } catch (Exception e) {
                    System.err.print(e.getMessage());
                }
            }
        });
    }
}
