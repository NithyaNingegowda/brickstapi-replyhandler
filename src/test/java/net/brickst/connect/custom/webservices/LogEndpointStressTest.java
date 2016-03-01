package net.brickst.connect.custom.webservices;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

public class LogEndpointStressTest implements LogEndpoint.LogObserver
{
    private File retryDir;
    
    // LogMonitor tracks events for an individual log message
    public class LogMonitor
    {
        long start = 0;
        long end = 0;
        int failCount = 0;
                
        public void callDeliver()
        {
            if (start == 0) {
                start = System.currentTimeMillis();
            }
        }

        public void failDeliver()
        {
            failCount++;            
        }

        public void didDeliver()
        {
            end = System.currentTimeMillis();
        }   
    }
    
    private ConcurrentHashMap<String,LogMonitor> monitorTable;
    
    @Override
    public void callDeliver(String content)
    {
        LogMonitor m = monitorTable.get(content);
        if (m == null) {
            m = new LogMonitor();
            monitorTable.put(content, m);
        }
        m.callDeliver();
    }

    @Override
    public void failDeliver(String content)
    {
        LogMonitor m = monitorTable.get(content);
        if (m == null) {
            m = new LogMonitor();
            monitorTable.put(content, m);
        }
        m.failDeliver();        
    }

    @Override
    public void didDeliver(String content)
    {
        LogMonitor m = monitorTable.get(content);
        if (m == null) {
            m = new LogMonitor();
            monitorTable.put(content, m);
        }
        m.didDeliver();        
    }
    
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
        
        monitorTable = new ConcurrentHashMap<String,LogMonitor>();
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
        props.setProperty("test.failPercentage", "0.5");
        props.setProperty("test.delayMean", "100");
        props.setProperty("test.delayStdDeviation", "50");
        
        LogEndpoint lep = new LogEndpoint();
        lep.setRetryDir(retryDir);
        lep.initFromProperties(props, "test.");
        
        // setup retry
        lep.setRetryIntervalMS(5000);
        lep.startRetryTask();
        
        lep.setObserver(this);
        
        // send 100 messages
        int count = 100;
        long[] samples = new long[count];
        long totalTime = 0;
        int sendCount = 0;
        int errorCount = 0;
        for (int i = 0; i < count; i++) {
            String msg = "Message " + i;
            // init monitor for less skew in performance data
            LogMonitor m = new LogMonitor();
            monitorTable.put(msg, m);
        
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
                try
                {
                    lep.scheduleRetry(msg);
                }
                catch (IOException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Trials: " + count + " Send: " + sendCount + " Error: " + errorCount);
        
        // wait for retries to complete
        while (true) {
            File[] newRetryDirList = retryDir.listFiles();
            if (newRetryDirList.length > 0) {
                System.out.println("Waiting for retries to complete...");
                try {
                    Thread.sleep(5000);
                }
                catch (Exception x) {
                    ; // don't care
                }
                continue;
            }
            else {
                break;
            }
        }
        
        // compute mean
        double mean = totalTime;
        mean = mean / count;
        System.out.println("Mean Delay: " + mean);
        Assert.assertEquals(100.0, mean, 50.0);
        
        // scan monitorTable to ensure all msgs were delivered
        double meanSuccess = 0;
        int successCount = 0;
        double meanFailure = 0;
        int maxRetries = 0;
        int failureCount = 0;
        int deliveryFailure = 0;
        Iterator<String> msgs = monitorTable.keySet().iterator();
        while (msgs.hasNext()) {
            String msg = msgs.next();
            LogMonitor m = monitorTable.get(msg);

            if (m.end == 0) {
                deliveryFailure++;
                continue;
            }
            
            long elapsed = m.end - m.start;
            if (m.failCount == 0) {
                // success
                successCount++;
                meanSuccess += elapsed;
            }
            else {
                if (m.failCount > maxRetries) {
                    maxRetries = m.failCount;
                }
                failureCount++;
                meanFailure += elapsed;
            }
        }
        System.out.println("Success Count: " + successCount);
        System.out.println("Mean Success Time: " + (meanSuccess / successCount));
        System.out.println("Fail Count: " + failureCount);
        System.out.println("Mean Failure Time: " + (meanFailure/ failureCount));
        System.out.println("Max Retry Count: " + maxRetries);
    }


    
}
