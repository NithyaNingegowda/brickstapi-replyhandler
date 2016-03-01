package net.brickst.connect.custom.webservices;

import javax.jms.JMSException;
import javax.naming.NamingException;

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
		
		// does not work yet
		// wep.jmsInit();
		
	}

}
