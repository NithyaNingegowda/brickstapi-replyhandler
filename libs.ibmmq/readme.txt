-create  import.ibm dir next to import dir
-copy these files into import.ibm
-edit crm.env and add this at the end of CLASSPATH= line:

:${KCHOME}/import.ibm/com.ibm.jaxws.thinclient_7.0.0.jar:${KCHOME}/import.ibm/com.ibm.mq.commonservices.jar:${KCHOME}/import.ibm/com.ibm.mq.headers.jar:${KCHOME}/import.ibm/com.ibm.mq.jar:${KCHOME}/import.ibm/com.ibm.mq.jmqi.jar:${KCHOME}/import.ibm/com.ibm.mq.jmqi.remote.jar:${KCHOME}/import.ibm/com.ibm.mq.jmqi.system.jar:${KCHOME}/import.ibm/com.ibm.mqjms.jar:${KCHOME}/import.ibm/com.ibm.msg.client.commonservices.j2se.jar:${KCHOME}/import.ibm/com.ibm.msg.client.commonservices.jar:${KCHOME}/import.ibm/com.ibm.msg.client.jms.internal.jar:${KCHOME}/import.ibm/com.ibm.msg.client.jms.jar:${KCHOME}/import.ibm/com.ibm.msg.client.provider.jar:${KCHOME}/import.ibm/com.ibm.msg.client.wmq.common.jar:${KCHOME}/import.ibm/com.ibm.msg.client.wmq.factories.jar:${KCHOME}/import.ibm/com.ibm.msg.client.wmq.jar:${KCHOME}/import.ibm/com.ibm.ws.admin.client_7.0.0.jar:${KCHOME}/import.ibm/com.ibm.ws.ejb.thinclient_7.0.0.jar:${KCHOME}/import.ibm/com.ibm.ws.orb_7.0.0.jar:${KCHOME}/import.ibm/com.ibm.ws.runtime.jar:${KCHOME}/import.ibm/com.ibm.ws.sib.client.thin.jms_7.0.0.jar:${KCHOME}/import.ibm/com.ibm.ws.sib.server.jar:${KCHOME}/import.ibm/dhbcore.jar:${KCHOME}/import.ibm/ibmkeycert.jar:${KCHOME}/import.ibm/ibmpkcs.jar


IBM MQ
Required libs from IBM MQ 7/7.5:

\IBM\WebSphere MQ\java\lib\jca\wmq.jmsra.rar\
com.ibm.mq.commonservices.jar
com.ibm.mq.headers.jar
com.ibm.mq.jar
com.ibm.mq.jmqi.jar
com.ibm.mq.jmqi.remote.jar
com.ibm.mq.jmqi.system.jar
com.ibm.mqjms.jar
com.ibm.msg.client.commonservices.j2se.jar
com.ibm.msg.client.commonservices.jar
com.ibm.msg.client.jms.internal.jar
com.ibm.msg.client.jms.jar
com.ibm.msg.client.provider.jar
com.ibm.msg.client.wmq.common.jar
com.ibm.msg.client.wmq.factories.jar
com.ibm.msg.client.wmq.jar
dhbcore.jar
\IBM\WebSphere MQ\java\jre\lib\
ibmpkcs.jar
\IBM\WebSphere MQ\java\jre\lib\ext\
ibmkeycert.jar

IBM WAS
Required libs from WAS 7

\IBM\WebSphere\AppServer\plugins\
com.ibm.ws.runtime.jar

\IBM\WebSphere\AppServer\runtimes\
com.ibm.jaxws.thinclient_7.0.0.jar
com.ibm.ws.admin.client_7.0.0.jar
com.ibm.ws.orb_7.0.0.jar

Other IBM WAS
\IBM\WebSphere\AppServer\runtimes\
com.ibm.ws.ejb.thinclient_7.0.0.jar
com.ibm.ws.sib.server.jar
com.ibm.ws.sib.client.thin.jms_7.0.0.jar
