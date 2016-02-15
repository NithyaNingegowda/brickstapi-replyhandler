package com.kana.connect.server.receiver;

import java.io.File;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import net.brickst.connect.custom.webservices.WebEndpoint;


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
		WebEndpoint wep = endpoints[0];
		Assert.assertEquals("com.ibm.mq.jms.context.WMQInitialContextFactory", wep.getJndiInitialContextFactory());
		
		// check number mappings
		Integer wepNumber = SMSKeywordDispatchReplyHandler.getNumberMapping("16035151212");
		Assert.assertEquals(0, wepNumber.intValue());
	}
}
