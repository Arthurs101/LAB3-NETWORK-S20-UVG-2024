package com.networks;

import java.io.IOException;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.impl.JidCreate;
/**
 * Class for handling XMPP connection and messaging process
 */
public class XmppClient {
    private XMPPTCPConnection connection = null;

    /**
     * 
     * @param JID the JID composed of username@domain
     * @param password the pwword
     * @throws SmackException
     * @throws IOException
     * @throws XMPPException 
     * @throws InterruptedException
     */
    public XmppClient (String JID, String password) throws SmackException, IOException, XMPPException, InterruptedException { 
        String domain = JID.split(":")[1];
        XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
            .setUsernameAndPassword(JID, password)
            .setXmppDomain(domain)
            .setHost(domain)
            .setPort(5222)
            .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
            .build();

            this.connection = new XMPPTCPConnection(config);
            this.connection.connect();
            this.connection.login();
    }

    public void disconnect() {
        if (this.connection != null && connection.isConnected()) {
            this.connection.disconnect();
        }
    }
     public void sendMessage(String to, String messageBody) throws Exception {
        if (connection != null && connection.isAuthenticated()) {
            ChatManager chatManager = ChatManager.getInstanceFor(this.connection);
            Chat chat = chatManager.chatWith(JidCreate.entityBareFrom(to));
            chat.send(messageBody);
        } else {
            throw new Exception("is not conected");
        }
    }
}
