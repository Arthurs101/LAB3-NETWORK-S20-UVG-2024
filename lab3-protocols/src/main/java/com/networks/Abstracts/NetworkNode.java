package com.networks.Abstracts;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.networks.MessageData;
import com.networks.XmppClient;

public abstract class NetworkNode {
    protected XmppClient client;
    protected String myJID;
    protected Set<String> seenMessages = new HashSet<>();
    protected Map<String, String> neighbors; // Node name to JID mapping

    public NetworkNode(String JID, String password, Map<String, String> neighbors) {
        try {
            this.client = new XmppClient(JID,password);
            this.myJID = JID;
            System.out.println("Node conected into network : " + JID);
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
                
                this.handleMessage(data);
            });
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    // Common method to send a message
    public void sendMessage(String to, String payload) {
        // Logic to send a message using XmppClient
        try {
            client.sendMessage(to, payload);
        } catch (Exception e) {
            System.err.println("Failed to send message to " + to + ": " + e.getMessage());
        }
    }
    public abstract void sendMessage(MessageData msg);
    // Abstract method to handle incoming messages
    public abstract void handleMessage(MessageData message);

    // Abstract method to broadcast messages
    public abstract void floodMessage(MessageData message, String from);

    /**
     * get the JID of the node
     * @return JID of the node
     */
    public String getJid(){
        return this.myJID;
 
    }

    /**
     * disconects the node from the server
     */
    public void terminateNode(){
        this.client.disconnect();
    }
}
