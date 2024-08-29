package com.networks;

import java.util.Map;

import com.networks.Abstracts.NetworkNode;
/**
 * Node for a flooding algorithm
 */
public class FloodingRoutingNode extends NetworkNode {    /**
     * @param neighbours Map of neighbours, where is ID:JID the keypairs
     * @param JID the jid for the node to connect to the server
     * @param pwrd the password for the node
    */
    public FloodingRoutingNode(String JID, String pwrd, Map<String, String> neighbors)  {
        super(JID, pwrd, neighbors);
    }
    
    /**
     * disconects the node from the server
     */
    public void terminateNode(){
        this.client.disconnect();
    }


    /**
     * Method to flood a message to all neighbors except the sender
     * @param  MessageData message instance of the message to send
     * @param String from , the user that sent the original message
     */
    @Override
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

    /**
     * Method hangle messages received from
     * @param  MessageData message instance of the message received
     */
    @Override
    public void handleMessage(MessageData message){
        String jsonMessage = message.toString();
        if(!this.seenMessages.contains(jsonMessage)){
           //proceed the message handling
            this.seenMessages.add(jsonMessage);
            switch (message.getType()) {
                case "hello" -> { 
                    if (message.getTo().equals(this.myJID)){ // is a node saluting this node
                        System.out.println(this.myJID + " Received Hello from " + message.getFrom());
                        System.out.println("Raw maessage: " + message.toJson());
                        break;
                    } else{ //forward that message
                        floodMessage(message, message.getFrom());
                    }
                }
                case "message" -> {
                    if (message.getTo().equals(this.myJID)){ // is a node saluting this node
                        System.out.println(this.myJID +" got messsage " + message.getPayload() + " From " + message.getFrom());
                        System.out.println("Raw maessage: " + message.toJson());
                    } else{ //forward that message
                        floodMessage(message, message.getFrom());
                    }
                }
                default -> throw new AssertionError();
            }
        }
    }
    /**
     * Method to send a direct message to another node calls the flooding method
     * @param message String to send
     * @param to recipient
     */
    @Override
    public void sendMessage(String message, String to) {
        try {
            MessageData forwardedMessage = new MessageData(
                "message", // type of the message
                this.myJID, // the from address
                to, // the to address
                0, // the amount of hops the message had to be forwarded
                message// the payload ( the message )
            );
            handleMessage(forwardedMessage);
        } catch (Exception e) {
            System.err.print(e.getMessage());
        }
    }
}
