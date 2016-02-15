/*
 * XSL Content Object
 * 
 * Copyright (c) 2016 Brick Street Software, Inc.
 * 
 * This code is provided under the Apache License.
 * http://www.apache.org/licenses/
 */

package net.brickst.connect.custom.content;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Manages an XSL Content Object
 */
public class XslContent {
	private URL xslLocation;

	// Transformer objects are single-threaded, so we keep a master
	// object and clone it for different threads.
	private Templates xslTemplates;

	//
	// GETTERS AND SETTERS
	//

	public URL getXslLocation() {
		return xslLocation;
	}

	public void setXslLocation(URL value) {
		xslLocation = value;
	}

	public static Templates getTransformerTemplates(URL xsltUrl)
			throws IOException, TransformerConfigurationException {
		TransformerFactory transformerFactory = TransformerFactory
				.newInstance();

		// fetch xsl content
		InputStream xslStream = getUrlContentStream(xsltUrl);
		try {
			StreamSource ssource = new StreamSource(xslStream);
			Templates templ = transformerFactory.newTemplates(ssource);
			return templ;
		} finally {
			if (xslStream != null) {
				try {
					xslStream.close();
				} catch (Exception x) { /* dont care */
				}
			}
		}
	}

	/**
	 * Reads the contents of a URL to a string.
	 */
	public static String getUrlContent(URL url) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		InputStream is = null;

		try {
			is = getUrlContentStream(url);

			// read loop
			byte[] buf = new byte[512];
			while (true) {
				int cc = is.read(buf);
				if (cc == -1) {
					break;
				}
				baos.write(buf, 0, cc);
			}
			return baos.toString();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception x) { /* don't care */
				}
			}
		}
	}

	public static InputStream getUrlContentStream(URL url) throws IOException {
		URLConnection urlconn = url.openConnection();
		InputStream is = urlconn.getInputStream();
		return is;
	}

	//
	// init from props file
	//
	public void initFromPropsFile(Properties props, String prefix) {
		if (prefix == null) {
			prefix = "";
		}

		String propName = prefix + "url";
		String propVal = props.getProperty(propName);
		URL loc = null;
		try {
			loc = new URL(propVal);
		} catch (Throwable th) {
			throw new IllegalArgumentException("Invalid URL: " + propVal);
		}

		xslLocation = loc;
	}

	/**
	 * Init method called after all config info has been read and before
	 * processing incoming messages.
	 */
	public Templates initXslContent() throws IOException,
			TransformerConfigurationException {
		xslTemplates = getTransformerTemplates(xslLocation);
		return xslTemplates;
	}

	public void initNetworkResources()
	{
		try {
			initXslContent();
		}
		catch (Exception x) {
			throw new RuntimeException(x);
		}
	}
	//
	// Get a transformer
	//
	// TODO object pool ???
	public Transformer getXsltTransformer()
			throws TransformerConfigurationException {
		Transformer trans = xslTemplates.newTransformer();
		return trans;
	}

	/**
	 * Calls transformer on an input document
	 */
	public String transformDocument(String content)
			throws TransformerConfigurationException, TransformerException {
		Transformer cloneTrans = getXsltTransformer();

		// create source and result objects
		StreamSource ssource = new StreamSource(new StringReader(content));
		StringWriter swriter = new StringWriter();
		StreamResult sresult = new StreamResult(swriter);

		cloneTrans.transform(ssource, sresult);
		String result = swriter.toString();
		return result;
	}
}
