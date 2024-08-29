package com.networks;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
/**
 * Node for a flooding algorithm
 */
public class FloodingRoutingNode {
    private XmppClient client;
    private final Map<String, String> neighbors;
    private String myJID;
    private Set<String> seenMessages = new HashSet<>();

    /**
     * @param neighbours Map of neighbours, where is ID:JID the keypairs
     * @param JID the jid for the node to connect to the server
     * @param pwrd the password for the node
    */
    public FloodingRoutingNode(String JID, String pwrd, Map<String, String> neighbors) {
        try {
            this.client = new XmppClient(JID,pwrd);
            this.myJID = JID;
            System.out.println("Node conected into network" + JID);
        } catch (Exception e) {
            System.err.println("Failed to connect node into the network " + JID + ": " + e.getMessage());
        }
        this.neighbors = neighbors;
        //set up a listener for all the messages received
        try{
            this.client.getChatManagerListener().addIncomingListener((from, message, chat) -> {
                String fromJID =  from.toString();
                String rawMessage = message.getBody();
                MessageData data = MessageData.fromJson(rawMessage);
                this.MessageHandler(data);
            });
        }
        catch(Exception e){
            e.printStackTrace();
        }

    }
    
    /**
     * disconects the node from the server
     */
    public void terminateNode(){
        this.client.disconnect();
    }

    /**
     * get the JID of the node
     * @return JID of the node
     */
    public String getJid(){
        return this.myJID;
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

    /**
     * Method hangle messages received from
     * @param  MessageData message instance of the message received
     */
    public void MessageHandler(MessageData message){
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
    public void sendMessage(String message, String to) {
        try {
            MessageData forwardedMessage = new MessageData(
                "message", // type of the message
                this.myJID, // the from address
                to, // the to address
                0, // the amount of hops the message had to be forwarded
                message// the payload ( the message )
            );
            MessageHandler(forwardedMessage);
        } catch (Exception e) {
            System.err.print(e.getMessage());
        }
    }
}
