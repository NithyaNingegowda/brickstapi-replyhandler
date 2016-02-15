package net.brickst.connect.custom.content;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.Assert;
import org.junit.Test;

public class TestXslContent 
{
	@Test
	public void testXslContent() throws MalformedURLException, TransformerConfigurationException, TransformerException
	{
		XslContent xc = new XslContent();
		xc.setXslLocation(new URL("http://content.brickst.net/temp1.xsl"));
		xc.initNetworkResources();
		
		String xml = "<SMS><Source><Number>16035551212</Number></Source>"
				+ "<Message><Product>abc123</Product></Message>"
				+ "</SMS>";
		
		String xform = xc.transformDocument(xml);
		Assert.assertTrue(xform.contains("abc123"));
	}
}
