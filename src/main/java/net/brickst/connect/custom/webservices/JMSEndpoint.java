package net.brickst.connect.custom.webservices;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;

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

public class JMSEndpoint extends WebEndpoint
{
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

    public JMSEndpoint()
    {
        endpointType = EndpointType.JMS;
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

    /**
     * Initialize JMS config info from Properties Object The prefix argument
     * will be prepended to all property names. e.g. prefix='endpoint_0',
     * properties will be endpoint_0.jmsJndiClass=... endpoint_0.jmsJndiUrl=...
     * endpoint_0.jmsConnectionFactoryName=... endpoint_0.jmsSendQueueName=...
     */
    public void initFromProperties(Properties props, String prefix) {
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

    /**
     * Init method called before the endpoint is used
     */
    public void initNetworkResources()
    {
        try {
            jmsInit();
        }
        catch (Throwable th) {
            throw new RuntimeException(th);
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
    public void deliverMessage(String content) 
    {
        try {
            deliverMessageJMS(content);
        }
        catch (JMSException x) {
            // throw exception; caller will schedule retry
            throw new RuntimeException(x);
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
    
}
