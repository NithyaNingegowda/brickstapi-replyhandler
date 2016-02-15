/*
 * Storage Class for Web Endpoint Configuration
 * 
 * Copyright (c) 2016 Brick Street Software, Inc.
 * 
 * This code is provided under the Apache License.
 * http://www.apache.org/licenses/
 */

package net.brickst.connect.custom.webservices;

import com.kana.connect.common.lib.Debug;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Contains configuration information and helper code for "web endpoints"
 * Currently supports JMS and REST.
 */
public class WebEndpoint {
	public enum EndpointType {
		JMS, REST, LOG
	}

	//
	// TOP LEVEL RETRY DIR (STATIC)
	//
	private static File topLevelRetryDir;
	private static Timer retryTimer;

	// start retry timer thread when loaded
	static {
		retryTimer = new Timer("WebEndpoint Delivery Retry", true);
	}

	//
	// INSTANCE VARS
	//

	private EndpointType endpointType;
	private File retryDir;
	private long retryIntervalMS;

	// retry timer task
	private Object retryTimerLock = new Object();
	private RetryTimerTask retryTimerTask;

	// JMS endpoint info
	private String jndiInitialContextFactory;
	private String jndiProviderUrl;
	private String jmsConnectionFactoryName;
	private String jmsConnectionUsername;
	private String jmsConnectionPassword;
	private String jmsSendQueueName;
	private JMSMessageFactory jmsMessageFactory;

	//
	// Design Note
	//
	// The JMS spec says that ConnectionFactory and Connection are
	// multithreaded objects that support concurrency. So we will
	// keep them in the endpoint object and allow them to be shared
	// across threads.
	//
	// On the other hand, Session and MessageProducer objects are
	// single-threaded so we will require callers to manage their own
	// instances of these objects.
	//
	// https://docs.oracle.com/javaee/5/api/javax/jms/ConnectionFactory.html
	// https://docs.oracle.com/javaee/5/api/javax/jms/Connection.html
	// https://docs.oracle.com/javaee/5/api/javax/jms/Session.html
	// https://docs.oracle.com/javaee/5/api/javax/jms/MessageProducer.html
	//

	// JMS connection state
	private ConnectionFactory jmsConnectionFactory;
	private Connection jmsConnection;
	private Queue jmsSendQueue;

	//
	// GETTERS and SETTERS
	//

	// static retry dir
	public static File getTopLevelRetryDir() {
		return topLevelRetryDir;
	}

	public static void setTopLevelRetryDir(File value) {
		if (!value.isDirectory()) {
			// mkdir ?
			if (!value.exists()) {
				// try mkdir
				boolean didCreate = false;
				try {
					didCreate = value.mkdirs();
				} catch (Exception x) {
					// TODO LOG EXCEPTION
				}
				if (!value.isDirectory()) {
					throw new IllegalArgumentException(
							"Unable to create retry directory: "
									+ value.getAbsolutePath());
				}
			} else {
				// exists and is not directory == invalid
				throw new IllegalArgumentException("Invalid retry directory: "
						+ value.getAbsolutePath());
			}
		}
		topLevelRetryDir = value;
	}

	// EndpointType
	public EndpointType getEndpointType() {
		return endpointType;
	}

	public void setEndpointType(EndpointType value) {
		endpointType = value;
	}

	// RetryIntervalMS
	public long getRetryIntervalMS() {
		return retryIntervalMS;
	}

	public void setRetryIntervalMS(long value) {
		retryIntervalMS = value;
	}

	// RetryDir
	public File getRetryDir() {
		return retryDir;
	}

	public void setRetryDir(File value) {
		if (!value.isDirectory()) {
			// mkdir ?
			if (!value.exists()) {
				// try mkdir
				boolean didCreate = false;
				try {
					didCreate = value.mkdirs();
				} catch (Exception x) {
					// TODO LOG EXCEPTION
				}
				if (!value.isDirectory()) {
					throw new IllegalArgumentException(
							"Unable to create retry directory: "
									+ value.getAbsolutePath());
				}
			} else {
				// exists and is not directory == invalid
				throw new IllegalArgumentException("Invalid retry directory: "
						+ value.getAbsolutePath());
			}
		}
		retryDir = value;
	}

	// JndiInitialContextFactory
	public String getJndiInitialContextFactory() {
		return jndiInitialContextFactory;
	}

	public void setJndiInitialContextFactory(String value) {
		jndiInitialContextFactory = value;
	}

	// JndiProviderUrl
	public String getJndiProviderUrl() {
		return jndiProviderUrl;
	}

	public void setJndiProviderUrl(String value) {
		jndiProviderUrl = value;
	}

	// JMS Connection Factory Name
	public String getJmsConnectionFactoryName() {
		return jmsConnectionFactoryName;
	}

	public void setJmsConnectionFactoryName(String value) {
		jmsConnectionFactoryName = value;
	}

	// JMS Connection Username
	public String getJmsConnectionUsername() {
		return jmsConnectionUsername;
	}

	public void setJmsConnectionUsername(String value) {
		jmsConnectionUsername = value;
	}

	// JMS Connection Password
	public String getJmsConnectionPassword() {
		return jmsConnectionPassword;
	}

	public void setJmsConnectionPassword(String value) {
		jmsConnectionPassword = value;
	}

	// JMS Send Queue Name
	public String getJmsSendQueueName() {
		return jmsSendQueueName;
	}

	public void setJmsSendQueueName(String value) {
		jmsSendQueueName = value;
	}

	// JMS connection factory (cannot set)
	public ConnectionFactory getJmsConnectionFactory() {
		return jmsConnectionFactory;
	}

	// JMS connection (cannot set)
	public Connection getJmsConnection() {
		return jmsConnection;
	}

	//
	// CONFIG FROM PROPERTIES METHODS
	//

	/**
	 * Initialize JMS config info from Properties Object The prefix argument
	 * will be prepended to all property names. e.g. prefix='endpoint_0',
	 * properties will be endpoint_0.jmsJndiClass=... endpoint_0.jmsJndiUrl=...
	 * endpoint_0.jmsConnectionFactoryName=... endpoint_0.jmsSendQueueName=...
	 */
	public void initJmsFromProperties(Properties props, String prefix) {
		String propName = null;
		String propVal = null;

		endpointType = EndpointType.JMS;

		if (prefix == null) {
			prefix = "";
		}

		// jndi class
		propName = prefix + "jmsJndiClass";
		propVal = props.getProperty(propName);
		if (propVal == null) {
			throw new IllegalArgumentException("Invalid Property: " + propName);
		}
		setJndiInitialContextFactory(propVal);

		// jndi provider url
		propName = prefix + "jmsJndiUrl";
		propVal = props.getProperty(propName);
		if (propVal == null) {
			throw new IllegalArgumentException("Invalid Property: " + propName);
		}
		setJndiProviderUrl(propVal);

		// jms connection factory name
		propName = prefix + "jmsConnectionFactoryName";
		propVal = props.getProperty(propName);
		if (propVal == null) {
			throw new IllegalArgumentException("Invalid Property: " + propName);
		}
		setJmsConnectionFactoryName(propVal);

		// jms send queue name
		propName = prefix + "jmsSendQueueName";
		propVal = props.getProperty(propName);
		if (propVal == null) {
			throw new IllegalArgumentException("Invalid Property: " + propName);
		}
		setJmsSendQueueName(propVal);

		//
		// connection username and password can be null
		//

		// jms connection username
		propName = prefix + "jmsConnectionUsername";
		propVal = props.getProperty(propName);
		setJmsConnectionUsername(propVal);

		// jms connection password
		propName = prefix + "jmsConnectionPassword";
		propVal = props.getProperty(propName);
		setJmsConnectionPassword(propVal);

		//
		// jms message factory
		//
		propName = prefix + "jmsMessageFactory";
		propVal = props.getProperty(propName);
		if (propVal == null) {
			jmsMessageFactory = new JMSTextMessageFactory();
		} else {
			try {
				jmsMessageFactory = (JMSMessageFactory) Class.forName(propVal)
						.newInstance();
			} catch (Throwable th) {
				throw new IllegalArgumentException(
						"Invalid JMS Message Factory Class: " + propVal, th);
			}
		}
	}

	/**
	 * Initialize REST config info from Properties Object The prefix argument
	 * will be prepended to all property names.
	 */
	public void initRestFromProperties(Properties props, String prefix) {
		throw new RuntimeException("NOT YET IMPLEMENTED");
	}
	
	/**
	 *  Initialize LOG config from Properties.
	 *  Log is a stub endpoint that writes the messages to the Connect log.
	 */
	public void initLogFromProperties(Properties props, String prefix) {
		endpointType = EndpointType.LOG;
	}

	//
	// JNDI / JMS Initialization
	//

	/**
	 * Creates InitialContext based on JNDI configuration info.
	 */
	public InitialContext getJndiInitialContext() throws NamingException {
		Hashtable env = new Hashtable();
		env.put(Context.INITIAL_CONTEXT_FACTORY, jndiInitialContextFactory);
		env.put(Context.PROVIDER_URL, jndiProviderUrl);
		return new InitialContext(env);
	}

	//
	// init
	//
	
	public void initNetworkResources()
	{
		if (endpointType == EndpointType.JMS) {
			try {
				jmsInit();
			}
			catch (Throwable th) {
				throw new RuntimeException(th);
			}
		}
	}
	
	/**
	 * Initializes Connection Factory and Connection using embedded JNDI info
	 */
	public void jmsInit() throws NamingException, JMSException {
		Context context = getJndiInitialContext();
		jmsInit(context);
	}

	/**
	 * Initializes Connection Factory and Connection using provided JNDI context
	 */
	public void jmsInit(Context context) throws NamingException, JMSException {
		if (endpointType != EndpointType.JMS) {
			throw new IllegalArgumentException("Endpoint type is not JMS");
		}

		// lookup connection factory
		ConnectionFactory cf = (ConnectionFactory) context
				.lookup(jmsConnectionFactoryName);
		if (cf == null) {
			throw new IllegalArgumentException(
					"Unable to find connection factory: "
							+ jmsConnectionFactoryName);
		}
		jmsConnectionFactory = cf;

		// lookup queue
		Queue queue = (Queue) context.lookup(jmsSendQueueName);
		if (queue == null) {
			throw new IllegalArgumentException("Unable to find send queue: "
					+ jmsSendQueueName);
		}
		jmsSendQueue = queue;

		// create jms connection
		Connection jmsConn = null;
		if (jmsConnectionUsername != null) {
			jmsConn = cf.createConnection(jmsConnectionUsername,
					jmsConnectionPassword);
		} else {
			jmsConn = cf.createConnection();
		}
	}

	/**
	 * Delivers Message to Endpoint Can be called by different threads so must
	 * handle it properly.
	 */
	public void deliverMessage(String content) throws JMSException, IOException {
		switch (endpointType) {
		case JMS:
			deliverMessageJMS(content);
			break;
		case REST:
			deliverMessageREST(content);
			break;
		case LOG:
			deliverMessageLOG(content);
			break;
		default:
			throw new IllegalArgumentException("Invalid Endpoint Type: "
					+ endpointType);
		}
	}

	public void deliverMessageLOG(String content) {
		if (Debug.SR.isEnabled()) {
			Debug.SR.println("WebEndpoint: LOG: \"" + content + "\"");
		}
	}
	
	/**
	 * Delivers Message to JMS Endpoint This can be called by multiple threads.
	 */
	public void deliverMessageJMS(String content) throws JMSException {
		//
		// get a session; note sessions are lightweight and single-threaded
		//
		// TODO: consider if we want to reuse sessions across calls; could have
		// a per-thread cache.
		//
		// NOTE: Session.CLIENT_ACKNOWLEDGE means that the message is not
		// "consumed" until we call Message.acknowledge()
		// If we crash before this is called, then the message will stay in
		// queue and be processed when we restart.
		// This param probably doesn't matter as long as we only send messages
		// and do not receive them.
		Session jmsSession = jmsConnection.createSession(false,
				Session.CLIENT_ACKNOWLEDGE);

		// run the rest of the message in a try block so that we can close the
		// session
		try {
			// create message
			Message jmsMessage = jmsMessageFactory.getMessage(jmsSession,
					content);

			// get message producer so we can send the message
			MessageProducer messageProducer = jmsSession
					.createProducer(jmsSendQueue);
			messageProducer.setDeliveryMode(DeliveryMode.PERSISTENT);

			// send message
			messageProducer.send(jmsMessage);
		} finally {
			if (jmsSession != null) {
				try {
					jmsSession.close();
				} catch (Exception x) {
					// Q: what do we do if we deliver the message but fail to
					// close?
					// A: log close exception but treat message delivered?
					;
				}
			}
		}
	}

	/**
	 * Delivers Message to REST Endpoint This can be called by multiple threads.
	 */
	public void deliverMessageREST(String content) throws IOException {
		throw new RuntimeException("NOT YET IMPLEMENTED");
	}

	//
	// RETRY
	//

	// TimerTask Inner Class
	public class RetryTimerTask extends TimerTask {
		WebEndpoint wep;

		public RetryTimerTask(WebEndpoint wep) {
			this.wep = wep;
		}

		public void run() {
			wep.doRetry();
		}
	}

	/**
	 * Saves a message for future retry
	 */
	public void scheduleRetry(String content) throws IOException {
		// assume this will generate unique filenames based on current timestamp
		File tempfile = null;
		File realfile = null;

		while (true) {
			long now = System.currentTimeMillis();
			String tempFilename = ".msg" + Long.toString(now) + ".txt";
			String filename = "msg" + Long.toString(now) + ".txt";
			tempfile = new File(getRetryDir(), tempFilename);
			realfile = new File(getRetryDir(), filename);
			if (!tempfile.exists() && !realfile.exists()) {
				break;
			}

			// filename collision; wait and retry
			double randomWait = Math.random() * 1000; // 0 - 1000 milliseconds
			try {
				Thread.sleep((long) randomWait);
			} catch (InterruptedException ix) {
				// don't care
			}
		}

		FileOutputStream fos = null;
		OutputStreamWriter osw = null;
		try {
			fos = new FileOutputStream(tempfile);
			osw = new OutputStreamWriter(fos, "UTF-8");
			osw.write(content);
		} finally {
			if (osw != null) {
				osw.close();
			}
			if (fos != null) {
				fos.close();
			}
		}

		// rename file so that retry can see it
		tempfile.renameTo(realfile);

		// schedule retry
		synchronized (retryTimerLock) {
			if (retryTimerTask == null) {
				retryTimerTask = new RetryTimerTask(this);
				retryTimer.schedule(retryTimerTask, retryIntervalMS,
						retryIntervalMS);
			}
		}
	}

	private String readFileContents(File file) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		FileInputStream fis = null;

		try {
			fis = new FileInputStream(file);
			byte[] buf = new byte[512];

			while (true) {
				int cc = fis.read(buf);
				if (cc == -1) {
					break;
				}
				baos.write(buf, 0, cc);
			}
		} finally {
			fis.close();
		}
		return baos.toString();
	}

	public void doRetry() {
		// get directory list
		String[] files = retryDir.list();
		Arrays.sort(files);

		int filecount = files.length;

		for (int i = 0; i < filecount; i++) {
			String retryFileName = files[i];
			File retryFile = new File(retryFileName);

			// check name
			String fname = retryFile.getName();
			if (!fname.startsWith("msg")) {
				continue;
			}

			// read file contents
			String content = null;

			if (!retryFile.exists()) {
				// skip non-existent file
				continue;
			}

			try {
				content = readFileContents(retryFile);
			} catch (Exception x) {
				// TODO LOG EXCEPTION
				// skip this file
				continue;
			}
			if (content == null || content.trim().length() == 0) {
				// TODO LOG ??
				continue;
			}

			// got content
			try {
				deliverMessage(content);
			} catch (Exception x) {
				// TODO LOG EXCEPTION
				continue;
			}

			// message delivered
			boolean didDelete = retryFile.delete();
			if (!didDelete) {
				// TODO LOG WEIRDNESS
			}
		}
	}
}
