package net.brickst.connect.custom.webservices;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.NamingException;

import junit.framework.Assert;

import org.junit.Test;

public class TestJMSEndpoint
{
	@Test
	public void testJMSEndpoint() throws NamingException, JMSException
	{
		JMSEndpoint wep = new JMSEndpoint();
		wep.setJndiInitialContextFactory("com.ibm.websphere.naming.WsnInitialContextFactory");
		wep.setJndiProviderUrl("iiop://10.101.0.101:2809");

		wep.setJmsConnectionFactoryName("jms/cellQueueConnectionFactory");
		wep.setJmsSendQueueName("jms/cellRequestQueue");
		
		wep.jmsInit();

        Connection jmsConn = wep.getJmsConnection();
        
        // drain any queue messages that might be hanging around
        {
            Session jmsSess = jmsConn.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            MessageConsumer jmsConsumer = jmsSess.createConsumer(wep.getJmsQueue());
        
            while (true) {
                Message jmsMsg = jmsConsumer.receiveNoWait();   
                if (jmsMsg == null) {
                    break;
                }
                jmsMsg.acknowledge();
            }
        }
        
		// send messages to queue
		int count = 5;
		for (int i = 0; i < count; i++) {
		    String msg = Integer.toString(i);
		    wep.deliverMessage(msg);
		}
		
		// receive messages to make sure we get them all
		
		Session jmsSess = jmsConn.createSession(false, Session.CLIENT_ACKNOWLEDGE);
		MessageConsumer jmsConsumer = jmsSess.createConsumer(wep.getJmsQueue());
		
		boolean[] msgAcks = new boolean[count];
		int unacked = count;
		while (true) {
		    Message jmsMsg = jmsConsumer.receive();   
		    if (jmsMsg == null) {
		        break;
		    }
		    if (jmsMsg instanceof TextMessage) {
		        TextMessage txtMsg = (TextMessage) jmsMsg;
		        String msgText = txtMsg.getText();
		        int msgNum = Integer.parseInt(msgText);
		        if (msgNum >= 0 && msgNum < count) {
		            msgAcks[msgNum] = true;
		            unacked--;
		        }
		    }
            // tell queue we have consumed the message
		    jmsMsg.acknowledge();
		    if (unacked == 0) {
		        break;
		    }
		}
		Assert.assertTrue(unacked == 0);
		int totalAcks = 0;
		for (int i = 0; i < count; i++) {
		    if (msgAcks[i]) {
		        totalAcks++;
		    }
		}
		Assert.assertTrue(totalAcks == count);
		
		jmsSess.close();
	}

}
