/*
 * Sample SMS Reply Handler
 * 
 * Copyright (c) 2015 Brick Street Software, Inc.
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
import com.kana.connect.common.db.TransactionManager;
import com.kana.connect.common.lib.Debug;
import com.kana.connect.server.receiver.ReplyHandler;
import com.kana.connect.server.receiver.SmppReceiverMessage;
import com.kana.connect.server.receiver.SmppReplyHandler;
import com.kana.connect.server.smpp.message.SMPPRequest;


public class SMSLoggingReplyHandler extends SmppReplyHandler
{
    public SMSLoggingReplyHandler() 
    {
	// The constructor can be used for handler-specific initialiation.
	// However, it should be careful to not throw exceptions.
    }
    
    /**
     * This method will be called for each incoming SMS message.
     * It can examine the incoming message and take action based on the message content.
     */
    public int handle(SmppReceiverMessage msg, TransactionManager tm)
    {
	// print log message to MailProcessor log under the "SMPP Receiver" Diagnostic
	if (Debug.SR.isEnabled()) {
	    Debug.SR.println("SMSLoggingReplyHandler: attempting to handle: " + msg);
	}

	// extract info about the incoming message from the SmppReceiverMessage object
	String smsSource = msg.getFieldToMatch("from");
	String smsDest = msg.getFieldToMatch("to");
	String smsMessage = msg.getFieldToMatch("body");
	
	// attempt to find customer from sms number
	CustomerRow cust = CustomerTable.getInstance().getCustomerBySMSNumber(smsSource);
	
	if (Debug.SR.isEnabled()) {
	    if (cust != null) {
		Debug.SR.println("SMSLoggingReplyHandler: message from " + smsSource + " (customer:" + cust.getID() + ") to " + smsDest + " body:\"" + smsMessage + "\"");
	    }
	    else {
		Debug.SR.println("SMSLoggingReplyHandler: message from " + smsSource + " (customer: unknown) to " + smsDest + " body:\"" + smsMessage + "\"");
	    }
	}
	
	//
	// If the handler returns HANDLED, then other reply handlers WILL NOT run.
	// If the handler returns HANDLED_CONTINUE, then other reply handler WILL run.
	// Archiving and redirecting WILL occur if the handler returns HANDLED or HANDLED_CONTINUE.
	//
	// If the handler returns NOT_HANDLED, then other reply handlers WILL run.
	// Archiving and redirecting WILL NOT occur if the handler returns NOT_HANDLED.
	//

	// to return HANDLED or HANDLED_CONTINUE...
        // each handler must set msg result codes
	/*
        msg.setHandlerID(getHandlerID());
        msg.setHandleType(getHandleType());
        msg.setHandleCode(ReplyHandler.HANDLED);
        return ReplyHandler.HANDLED;  //or Reply.Handler.HANDLED_CONTINUE
	*/

	// to return NOT_HANDLED...
        return ReplyHandler.NOT_HANDLED;
    }
}