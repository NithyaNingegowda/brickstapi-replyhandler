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

public interface JMSMessageFactory
{
    public Message getMessage(Session session, String content) throws JMSException;
}
