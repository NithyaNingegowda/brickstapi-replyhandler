/*
 * JMS Message Factory -- transforms input data to a JMS Message
 * 
 * Copyright (c) 2016 Brick Street Software, Inc.
 * 
 * This code is provided under the Apache License.
 * http://www.apache.org/licenses/
 */

package net.brickst.connect.custom.webservices;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

/**
 * Creates a JMS Text Message
 * This is the simplest default implementation.
 */
public class JMSTextMessageFactory implements JMSMessageFactory
{
    public Message getMessage(Session session, String content) throws JMSException
    {
	TextMessage textMsg = session.createTextMessage(content);
	return textMsg;
    }
}
