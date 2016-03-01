package net.brickst.connect.custom.webservices;

import java.util.Properties;
import java.util.Random;

import com.kana.connect.common.lib.Debug;

public class LogEndpoint extends WebEndpoint
{
    //
    // TEST / DEBUG SUPPORT
    //
    
    // Log can be configured to fail a certain percentage of messages
    // This should be a number between 0 and 1. Default is 0;
    private double failPercentage = 0;
    
    // Log can be configured to delay deliver.
    // Specify a mean and standard deviation in milliseconds.
    // We use Random.nextGaussian to get a normal distribution of delay values.
    private long delayMean = 0;
    private long delayStdDeviation = 0;
    
    private Random random;
    
    //
    // Log Observer -- the log observer is used to provide a
    // a callback mechanism for fail and retry events.  It is
    // intended for unit testing of the fail/retry mechanism
    // See LogEndpointStressTest for usage.
    //
    
    public interface LogObserver {
        public void callDeliver(String content);
        public void failDeliver(String content);
        public void didDeliver(String content);
    }
    
    private LogObserver observer;
    
    //
    // GETTERS / SETTERS
    //
    
    public double getFailPercentage() { return failPercentage; }
    public void setFailPercentage(double val) { failPercentage = val; }
    
    public long getDelayMean() { return delayMean; }
    public void setDelayMean(long val) { delayMean = val; }
    
    public long getDelayStdDeviation() { return delayStdDeviation; }
    public void setDelayStdDeviation(long val) { delayStdDeviation = val; }
    
    public LogObserver getObserver() { return observer; }
    public void setObserver(LogObserver val) { observer = val; }
    
    public LogEndpoint()
    {
        endpointType = EndpointType.LOG;
    }
    
    @Override
    public void initFromProperties(Properties props, String prefix)
    {
        String propName;
        String propVal;
        
        propName = prefix + "failPercentage";
        propVal = props.getProperty(propName);
        if (propVal != null) {
            try {
                double dval = Double.parseDouble(propVal);
                if (dval < 0 || dval > 1) {
                    throw new IllegalArgumentException("failPercentage must be between 0 and 1");
                }
                failPercentage = dval;
            }
            catch (Exception x) {
                throw new IllegalArgumentException("invalid failPercentage: " + propVal);
            }
        }
        
        propName = prefix + "delayMean";
        propVal = props.getProperty(propName);
        if (propVal != null) {
            try {
                long lval = Long.parseLong(propVal);
                delayMean = lval;
            }
            catch (Exception x) {
                throw new IllegalArgumentException("invalid delayMean: " + propVal);
            }
        }
        
        propName = prefix + "delayStdDeviation";
        propVal = props.getProperty(propName);
        if (propVal != null) {
            try {
                long lval = Long.parseLong(propVal);
                delayStdDeviation = lval;
            }
            catch (Exception x) {
                throw new IllegalArgumentException("invalid delayStdDeviation: " + propVal);
            }
        }

        // specify an optional random seed
        propName = prefix + "randomSeed";
        propVal = props.getProperty(propName);
        if (propVal != null) {
            try {
                long lval = Long.parseLong(propVal);
                random = new Random(lval);
            }
            catch (Exception x) {
                throw new IllegalArgumentException("invalid randomSeed: " + propVal);
            }
        }
        else {
            random = new Random();
        }
    }

    @Override
    public void deliverMessage(String content)
    {
        if (observer != null) {
            observer.callDeliver(content);
        }
        
        // handle delay
        if (delayMean > 0) {
            double delayRandom = random.nextGaussian() * delayStdDeviation + delayMean;
            long delayLong = (long) delayRandom;
            try {
                Thread.sleep(delayLong);
            }
            catch (Exception x) {
                // don't care
            }
        }
        
        // check for fail
        if (failPercentage > 0) {
            double failRandom = random.nextDouble();
            if (failRandom < failPercentage) {
                if (observer != null) {
                    observer.failDeliver(content);
                }
                throw new RuntimeException("RANDOM FAILURE");
            }
        }
        
        // log message
        if (observer != null) {
            observer.didDeliver(content);
        }
        Debug.MR.println(content);
    }

    @Override
    public void initNetworkResources()
    {
        // TODO Auto-generated method stub
        
    }
}
