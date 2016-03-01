package com.kana.connect.server.receiver;

import java.io.File;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import net.brickst.connect.custom.webservices.JMSEndpoint;
import net.brickst.connect.custom.webservices.LogEndpoint;
import net.brickst.connect.custom.webservices.WebEndpoint;
import net.brickst.connect.custom.webservices.WebEndpoint.EndpointType;


public class TestSMSConfig 
{
	@Test
	public void testStaticConfig()
	{
		File configProps = new File("smskeyworddispatch.properties");
		SMSKeywordDispatchReplyHandler.loadConfig(configProps);
		
		// check match patterns
		Pattern[] patterns = SMSKeywordDispatchReplyHandler.getMatchPatterns();
		Assert.assertEquals(1, patterns.length);
		Pattern pattern = patterns[0];
		Assert.assertEquals(".", pattern.pattern());
		
		// check web endpoints
		WebEndpoint[] endpoints = SMSKeywordDispatchReplyHandler.getWebEndpoints();
		Assert.assertEquals(1, endpoints.length);
		WebEndpoint endpoint = endpoints[0];
		if (endpoint.getEndpointType() == EndpointType.JMS) {
		    JMSEndpoint wep = (JMSEndpoint) endpoints[0];
		    Assert.assertEquals("com.ibm.mq.jms.context.WMQInitialContextFactory", wep.getJndiInitialContextFactory());
		}
		else if (endpoint.getEndpointType() == EndpointType.LOG) {
		    LogEndpoint lep = (LogEndpoint) endpoints[0];
		    Assert.assertEquals(0.01, lep.getFailPercentage(), 0.01);		    
		}
		// check number mappings
		Integer wepNumber = SMSKeywordDispatchReplyHandler.getNumberMapping("16035151212");
		Assert.assertEquals(0, wepNumber.intValue());
	}
}
