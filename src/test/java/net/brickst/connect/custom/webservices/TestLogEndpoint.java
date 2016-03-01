package net.brickst.connect.custom.webservices;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

/*
 * Unit Test for the Log Endpoint.
 * Tests sending with delay and failures.
 */
public class TestLogEndpoint
{
    private File retryDir;
    
    @Before
    public void setup() throws IOException
    {
        File retryTop = new File(".");
        WebEndpoint.setTopLevelRetryDir(retryTop);
        System.out.println("Top Level: " + retryTop.getCanonicalPath());
        
        retryDir = new File(retryTop, "logRetry");
        if (retryDir.isDirectory()) {
            // clear any files in retry dir
            File[] retryDirList = retryDir.listFiles();
            if (retryDirList != null && retryDirList.length > 0) {
                for (int i = 0; i < retryDirList.length; i++) {
                    File retry = retryDirList[i];
                    if (retry.isFile()) {
                        retry.delete();
                    }
                }
            }
        }
    }
    
    @Test
    public void testLogEndpoint1()
    {
        // retry dir
        if (retryDir.isDirectory()) {
            File[] newRetryDirList = retryDir.listFiles();
            Assert.assertEquals(0, newRetryDirList.length);
        }

        // init props object
        Properties props = new Properties();
        props.setProperty("test.failPercentage", "0.0");
        
        LogEndpoint lep = new LogEndpoint();
        lep.setRetryDir(retryDir);
        lep.initFromProperties(props, "test.");
        // note: we do not start the retry task
        
        // send 1000 messages
	int count = 1000;
        int sendCount = 0;
        int errorCount = 0;
        for (int i = 0; i < count; i++) {
            String msg = "Message " + i;
        
            try {
                lep.deliverMessage(msg);
                sendCount++;
            }
            catch (Throwable th) {
                errorCount++;
            }
        }
        System.out.println("Trials: " + count + ", Send: " + sendCount + " Error: " + errorCount);
        Assert.assertEquals(count, sendCount);
        Assert.assertEquals(0, errorCount);
    }

    @Test
    public void testLogEndpointFail1()
    {
        // retry dir
        if (retryDir.isDirectory()) {
            File[] newRetryDirList = retryDir.listFiles();
            Assert.assertEquals("Retry Dir Not Empty", 0, newRetryDirList.length);
        }

        // init props object
        Properties props = new Properties();
        props.setProperty("test.failPercentage", "0.2");
        
        LogEndpoint lep = new LogEndpoint();
        lep.setRetryDir(retryDir);
        lep.initFromProperties(props, "test.");
        // note: we do not start the retry task
        
        // send 1000 messages
	int count = 1000;
        int sendCount = 0;
        int errorCount = 0;
        for (int i = 0; i < count; i++) {
            String msg = "Message " + i;
        
            try {
                lep.deliverMessage(msg);
                sendCount++;
            }
            catch (Throwable th) {
                errorCount++;
            }
        }
        System.out.println("Trials: " + count + ", Send: " + sendCount + " Error: " + errorCount);
	// TODO compute ranges as a function of count
        Assert.assertTrue("Send Count Not Between 700 and 900", (700 <= sendCount) && (sendCount <= 900));      
        Assert.assertTrue("Error Count Not Between 100 and 300", (100 <= errorCount) && (errorCount <= 300));
    }

    @Test
    public void testLogEndpointDelay1()
    {
        // retry dir
        if (retryDir.isDirectory()) {
            File[] newRetryDirList = retryDir.listFiles();
            Assert.assertEquals("Retry Dir Not Empty", 0, newRetryDirList.length);
        }

        // init props object
        Properties props = new Properties();
        props.setProperty("test.failPercentage", "0.0");
        props.setProperty("test.delayMean", "100");
        props.setProperty("test.delayStdDeviation", "50");
        
        LogEndpoint lep = new LogEndpoint();
        lep.setRetryDir(retryDir);
        lep.initFromProperties(props, "test.");
        // note: we do not start the retry task
        
        // send 100 messages
        int count = 100;
        long[] samples = new long[count];
        long totalTime = 0;
        int sendCount = 0;
        int errorCount = 0;
        for (int i = 0; i < count; i++) {
            String msg = "Message " + i;
        
            try {
                long start = System.currentTimeMillis();
                lep.deliverMessage(msg);
                
                long end = System.currentTimeMillis();
                long elapsed = end - start;
                samples[i] = elapsed;
                totalTime += elapsed;
                
                sendCount++;
            }
            catch (Throwable th) {
                errorCount++;
            }
        }
        System.out.println("Trials: " + count + " Send: " + sendCount + " Error: " + errorCount);
        Assert.assertTrue(sendCount == count);      
        Assert.assertTrue(errorCount == 0);
        
        // compute mean
        double mean = totalTime;
        mean = mean / count;
        System.out.println("Mean Delay: " + mean);
        Assert.assertEquals(100.0, mean, 50.0);
    }

    
}
