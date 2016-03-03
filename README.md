# brickstapi-replyhandler

This project requires Gradle to build.  See https://gradle.org/ for instructions on how to download and configure gradle.


To Build:
=========

1. See libs/README.txt to see which jar files need to be copied from the Connect distribution.

2. Build using this gradle command:

	gradle jar

   This will produce connect.replyhandler-0.1.jar in build/libs.

   Alternatively, you can build using maven:
        mvn package

	To build without tests:

	mvn -Dmaven.test.skip=true package

	This will build a jar in the target directory.


To install:
===========
      
1. Add reply handler record:

    insert into reply_handler_master
    (
    reply_handler_id, company_id, name, 
    class_name, 
    handle_type_code, source_code, status_code, 
    scope_code, handle_order, 
    insert_datetime, insert_process, insert_user, 
    update_datetime, update_process, update_user, 
    apply_to)
    values
    (    
    seq_reply_handler_id.nextval, 100, 'smskeyworddispatch',
    'com.kana.connect.server.receiver.SMSKeywordDispatchReplyHandler',
    101, 0, 0,
    2, 3, 
    sysdate, 'CE', 100, 
    sysdate, 'CE', 100,
    3);

* handle_type_code can be anything
* source_code and status_code are unused and should be 0
* scope_code should be 2 for global
* handle_order can be any numbers; handlers are executed in handle_order
* apply_to can be 1 (all), 2 (email), or 3 (sms)


2. Install jar on inside node

Copy the jar file to the kc/import directory on the inside node(s).

Edit the crm.env file on the inside node(s) to add the jar file to the classpath.

e.g. Add this to end of CLASSPATH: ':${KCHOME}/import/ConnectReplyHandlers-1.0-SNAPSHOT.jar'

3. Create kc/smskeyworddispatch.properties file.

Restart the Connect service to force the new classpath to be used.
   

