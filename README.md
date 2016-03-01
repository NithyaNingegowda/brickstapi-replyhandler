# brickstapi-replyhandler

This project requires Gradle to build.  See https://gradle.org/ for instructions on how to download and configure gradle.


To Build:

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

Copy the jar file to the kc/import directory.

Edit the connect.properties file to add the jar file to the classpath.


If on a deployed node, restart the Connect service to force the new classpath to be used.


Execute the following SQL Statement to register the reply handler:

INSERT INTO REPLY_HANDLER_MASTER ...
