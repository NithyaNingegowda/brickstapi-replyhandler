/*
 * Sample Email Reply Handler
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
import com.kana.connect.common.lib.MessageContext;
import com.kana.connect.server.receiver.EmailReplyHandler;
import com.kana.connect.server.receiver.ReceiverMessage;
import com.kana.connect.server.receiver.ReplyHandler;


public class EmailLoggingReplyHandler extends EmailReplyHandler
{
    public EmailLoggingReplyHandler() 
    {
	// The constructor can be used for handler-specific initialiation.
	// However, it should be careful to not throw exceptions.
    }
    
    public int handle(ReceiverMessage msg, TransactionManager tm)
    {
	// print log message to MailProcessor log under the "Mail Receiver" Diagnostic
	if (Debug.MR.isEnabled()) {
	    Debug.MR.println("EmailLoggingReplyHandler: attempting to handle: " + msg);
	}

	// extract info about the incoming message from the ReceiverMessage object
	String emailSource = msg.getFieldToMatch("from");
	String emailDest = msg.getFieldToMatch("to");
	String emailSubj = msg.getFieldToMatch("subject");
	String emailMessage = msg.getFieldToMatch("body");

	// MessageContext is the 3 number code that identifies a record in the CUSTOMER_QUEUE table.
	MessageContext msgContext = msg.getContext();
	long customerID = 0;
	if (msgContext != null) {
	    customerID = msgContext.getCustomerId();
	}
	    
	// attempt to find customer from the message
	CustomerRow cust = null;
	if (customerID > 0) {
	    cust = CustomerTable.getInstance().getCustomerByID(customerID);
	}
	if (cust == null) {
	    // customer not found, attempt to find by email
	    cust = CustomerTable.getInstance().getCustomerByEmail(emailSource);
	}
	
	if (Debug.MR.isEnabled()) {
	    if (cust != null) {
		Debug.MR.println("EmailLoggingReplyHandler: message from " + emailSource + " (customer:" + cust.getID() + ") to " + emailDest + " body:\"" + emailMessage + "\"");
	    }
	    else {
		Debug.MR.println("EmailLoggingReplyHandler: message from " + emailSource + " (customer: unknown) to " + emailDest + " body:\"" + emailMessage + "\"");
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