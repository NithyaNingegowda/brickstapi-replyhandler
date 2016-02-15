/*
 * SMS Keyword Dispatch Reply Handler
 * 
 * Copyright (c) 2016 Brick Street Software, Inc.
 * 
 * This code is provided under the Apache License.
 * http://www.apache.org/licenses/
 */

/* 
 NOTE: In Connect 10r4, custom reply handlers must be in the com.kana.connect.server.receiver package
 because certain classes, like ReceiverMessage are not exported outside this package.
 */

package com.kana.connect.server.receiver;

import com.kana.connect.common.db.CustomerRow;
import com.kana.connect.common.db.CustomerTable;
import com.kana.connect.common.db.ReplyHandlerRow;
import com.kana.connect.common.db.TransactionManager;
import com.kana.connect.common.lib.Debug;
import com.kana.connect.server.receiver.ReplyHandler;
import com.kana.connect.server.receiver.SmppReceiverMessage;
import com.kana.connect.server.receiver.SmppReplyHandler;
import com.kana.connect.server.smpp.Address;
import com.kana.connect.server.smpp.message.SMPPRequest;

import net.brickst.connect.custom.content.XslContent;
import net.brickst.connect.custom.webservices.WebEndpoint;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

public class SMSKeywordDispatchReplyHandler extends SmppReplyHandler {
	// The mail processor may create a new handler object for each incoming
	// message.
	// On the other hand, the configuration for this handler is heavy and
	// expensive to set up.
	// As a result, we store the configuration in static variables so that it
	// can be
	// shared across multiple instances of the reply handler.
	//
	// If the config file changes, the mail processor needs to be restarted.
	// Our assumption is that this will happen rarely if ever.

	//
	// STATIC CONFIG VARS
	//
	private static Object configLock = new Object();
	private static Pattern[] matchPatterns;
	private static WebEndpoint[] webEndpoints;
	private static ConcurrentHashMap<String, Integer> numberMappings;
	private static XslContent contentTemplate;
	private static AtomicBoolean didInit;
	
	public SMSKeywordDispatchReplyHandler() {
		// The constructor can be used for handler-specific initialiation.
		// However, it should be careful to not throw exceptions.
	}

	public static Pattern[] getMatchPatterns() { return matchPatterns; }
	public static WebEndpoint[] getWebEndpoints() { return webEndpoints; }
	public static Integer getNumberMapping(String number) { return numberMappings.get(number); }
	
	protected static int getIntProperty(Properties props, String propName, int defaultVal)
	{
		String val = props.getProperty(propName);
		int ival;
		try {
			ival = Integer.parseInt(val);
			return ival;
		} catch (Exception x) {
			// log exception
			return defaultVal;
		}
	}

	public static boolean loadConfig(File configProps) 
	{
		synchronized (configLock) {

			if (matchPatterns != null || webEndpoints != null) {
				// assume config has already been loaded
				return false;
			}

			//
			// initialize configuration properties
			//
			Properties props = new Properties();

			// String handlerName = handlerRow.getName();
			FileInputStream propsInput = null;
			try {
				propsInput = new FileInputStream(configProps);
			} catch (Exception x) {
				// TODO LOG EXCEPTION
				throw new IllegalArgumentException("Unable to find "
						+ configProps, x);
			}
			try {
				props.load(propsInput);
			} catch (Exception x) {
				throw new RuntimeException(x);
			}

			//
			// get regexes
			//
			int regexCount = getIntProperty(props, "regex_count", 0);
			matchPatterns = new Pattern[regexCount];
			for (int i = 0; i < regexCount; i++) {
				String regex = props.getProperty("regex_" + i + ".pattern");
				int flags = getIntProperty(props, "regex_" + i + ".flags", 0);

				Pattern p = null;
				try {
					if (flags > 0) {
						p = Pattern.compile(regex, flags);
					} else {
						p = Pattern.compile(regex);
					}
				} catch (Throwable th) {
					throw new RuntimeException("Error compiling regex " + i, th);
				}
				matchPatterns[i] = p;
			}

			//
			// init endpoint retry dir
			//
			String endpointRetryDir = props.getProperty("endpoint_retrydir");
			if (endpointRetryDir == null) {
				endpointRetryDir = "smsretryqueue";
			}
			File retryTop = new File(endpointRetryDir);
			WebEndpoint.setTopLevelRetryDir(retryTop);

			//
			// load endpoints
			//
			int endpointCount = getIntProperty(props, "endpoint_count", 0);
			webEndpoints = new WebEndpoint[endpointCount];
			for (int i = 0; i < endpointCount; i++) {
				WebEndpoint wep = new WebEndpoint();
				String epPrefix = "endpoint_" + i + ".";
				String epName = epPrefix + "type";
				String epType = props.getProperty(epName);
				if ("JMS".equalsIgnoreCase(epType)) {
					wep.initJmsFromProperties(props, epPrefix);
				} 
				else if ("REST".equalsIgnoreCase(epType)) {
					wep.initRestFromProperties(props, epPrefix);
				}
				else if ("LOG".equalsIgnoreCase(epType)) {
					wep.initLogFromProperties(props, epPrefix);
				} 
				else {
					throw new IllegalArgumentException(
							"Invalid endpoint type: " + epType);
				}

				// init retry dir
				File retry = new File(WebEndpoint.getTopLevelRetryDir(),
						"endpoint_" + i);
				wep.setRetryDir(retry);

				// retry interval
				int retryIntervalSec = getIntProperty(props, epPrefix
						+ "retryIntervalSeconds", 300); // default 5 mins
				wep.setRetryIntervalMS(retryIntervalSec * 1000);

				webEndpoints[i] = wep;
			}

			//
			// mappings from destinations to endpoints
			//
			int mappingCount = getIntProperty(props, "mapping_count", 0);
			numberMappings = new ConcurrentHashMap<String, Integer>();
			for (int i = 0; i < mappingCount; i++) {
				String prefix = "mapping_" + i + ".";
				String number = props.getProperty(prefix + "number");
				int epNumber = getIntProperty(props, prefix + "endpoint", -1);
				if (epNumber < 0) {
					throw new IllegalArgumentException(
							"Invalid endpoint in mapping " + i);
				}
				numberMappings.put(number, Integer.valueOf(epNumber));
			}

			//
			// content
			//
			String contentType = props.getProperty("content.type");
			if ("XSL".equalsIgnoreCase(contentType)) {
				contentTemplate = new XslContent();
				contentTemplate.initFromPropsFile(props, "content.");
			}
			else {
				throw new IllegalArgumentException("Invalid Content Type: " + contentType);
			}
			
			// did load, need to init
			return true;
		}
	}

	public void initNetworkResources()
	{
		// should we init or let another thread do it???
		boolean doInit = didInit.compareAndSet(false, true);
		if (! doInit) {
			return;
		}
		// we win, init here
		
		int endpointCount = webEndpoints.length;
		for (int i = 0; i < endpointCount; i++) {
			WebEndpoint wep = new WebEndpoint();
			wep.initNetworkResources();
		}
		
		contentTemplate.initNetworkResources();
	}
	
	public void init(ReplyHandlerRow handlerRow) {
		// call superclass method
		super.init(handlerRow);

		String configFileName = handlerRow.getName() + ".properties";
		File configFile = new File(configFileName);

		// call static config loader
		loadConfig(configFile);
		initNetworkResources();
	}
	
	/**
	 * Helper method that transforms an SMPP Message into an XML document
	 */
	public String smppToXml(SmppReceiverMessage msg)
	{
		SMPPRequest smppReq = msg.getSmppRequest();
		
		StringBuffer buf = new StringBuffer();
		buf.append("<smpp>\n");

		// smpp header
		buf.append("<header>");
		buf.append("<command_id>").append(smppReq.getCommandId()).append("</command_id>");
		buf.append("<sequence_number>").append(smppReq.getSequenceNum()).append("</sequence_number>");
		buf.append("</header>\n");
		
		// smpp source
		Address smppSource = smppReq.getSource();
		buf.append("<source>");
		buf.append("<ton>").append(smppSource.getTON()).append("</ton>");
		buf.append("<npi>").append(smppSource.getNPI()).append("</npi>");
		buf.append("<address>").append(smppSource.getAddress()).append("</address>");
		buf.append("</source>\n");
		
		// smpp dest
		Address smppDest = smppReq.getDestination();
		buf.append("<destination>");
		buf.append("<ton>").append(smppDest.getTON()).append("</ton>");
		buf.append("<npi>").append(smppDest.getNPI()).append("</npi>");
		buf.append("<address>").append(smppDest.getAddress()).append("</address>");
		buf.append("</destination>\n");
		
		// smpp message
		buf.append("<message>");
		buf.append(smppReq.getMessageText());
		buf.append("</message>\n");

		buf.append("</smpp>\n");
		return buf.toString();
	}

	/**
	 * This method will be called for each incoming SMS message. It can examine
	 * the incoming message and take action based on the message content.
	 */
	public int handle(SmppReceiverMessage msg, TransactionManager tm) {
		// print log message to MailProcessor log under the "SMPP Receiver"
		// Diagnostic
		if (Debug.SR.isEnabled()) {
			Debug.SR.println("SMSKeywordDispatchReplyHandler: attempting to handle: "
					+ msg);
		}

		// extract info about the incoming message from the SmppReceiverMessage
		// object
		String smsSource = msg.getFieldToMatch("from");
		String smsDest = msg.getFieldToMatch("to");
		String smsMessage = msg.getFieldToMatch("body");

		//
		// MATCH INCOMING MESSAGE AGAINST REGEXES
		//

		// matchMatcher / matchIndex hold the result of the match
		Matcher matchMatcher = null;
		int matchIndex = -1;

		int regexCount = matchPatterns.length;
		for (int i = 0; i < regexCount; i++) {
			Pattern pattern = matchPatterns[i];
			Matcher matcher = pattern.matcher(smsMessage);
			if (matcher.matches()) {
				matchIndex = i;
				matchMatcher = matcher;
				break;
			}
		}

		// quit if no match
		if (matchIndex < 0) {
			return NOT_HANDLED; // NOT_HANDLED is inherited from parent class
		}

		//
		// IF REGEXES MATCH, FIND ASSOCIATED MAPPING FOR DEST NUMBERN
		//
		Integer targetEndpoint = numberMappings.get(smsDest);
		if (targetEndpoint == null) {
			// no match for destination number; log and give up???
			return NOT_HANDLED;
		}
		WebEndpoint wep = webEndpoints[targetEndpoint.intValue()];
		if (wep == null) {
			// no match for endpoint number; log and give up???
			return NOT_HANDLED;
		}

		//
		// CREATE CONTENT
		//
		// attempt to find customer from sms number
		// TODO incorporate this into the XML document
		CustomerRow cust = CustomerTable.getInstance().getCustomerBySMSNumber(
				smsSource);

		if (Debug.SR.isEnabled()) {
			if (cust != null) {
				Debug.SR.println("SMSLoggingReplyHandler: message from "
						+ smsSource + " (customer:" + cust.getID() + ") to "
						+ smsDest + " body:\"" + smsMessage + "\"");
			} else {
				Debug.SR.println("SMSLoggingReplyHandler: message from "
						+ smsSource + " (customer: unknown) to " + smsDest
						+ " body:\"" + smsMessage + "\"");
			}
		}

		// create xml doc
		String xmlContent = smppToXml(msg);
		String xslOutput = null;
		// do xsl transform 
		if (contentTemplate == null) {
			xslOutput = xmlContent;
		}
		else {
			try {
				xslOutput = contentTemplate.transformDocument(xmlContent);
			} 
			catch (Throwable th) {
				throw new RuntimeException(th);
			}
		}

		//
		// DELIVER MESSAGE TO ENDPOINT
		//
		try {
			wep.deliverMessage(xslOutput);
		} catch (Exception x) {
			//
			// TODO LOG EXCEPTION
			//
			try {
				wep.scheduleRetry(xslOutput);

				// to return HANDLED or HANDLED_CONTINUE...
				// each handler must set msg result codes
				msg.setHandlerID(getHandlerID());
				msg.setHandleType(getHandleType());
				msg.setHandleCode(HANDLED);
				return HANDLED; // or HANDLED_CONTINUE
			} catch (Throwable th) {
				// TODO log exception

				// If we cannot deliver the message and we cannot save to a
				// file,
				// we are seriously hosed and should crash the process.
				throw new RuntimeException(th);
			}
		}

		//
		// If the handler returns HANDLED, then other reply handlers WILL NOT
		// run.
		// If the handler returns HANDLED_CONTINUE, then other reply handler
		// WILL run.
		// Archiving and redirecting WILL occur if the handler returns HANDLED
		// or HANDLED_CONTINUE.
		//
		// If the handler returns NOT_HANDLED, then other reply handlers WILL
		// run.
		// Archiving and redirecting WILL NOT occur if the handler returns
		// NOT_HANDLED.
		//

		// to return HANDLED or HANDLED_CONTINUE...
		// each handler must set msg result codes
		msg.setHandlerID(getHandlerID());
		msg.setHandleType(getHandleType());
		msg.setHandleCode(HANDLED);
		return HANDLED; // or HANDLED_CONTINUE
	}

}