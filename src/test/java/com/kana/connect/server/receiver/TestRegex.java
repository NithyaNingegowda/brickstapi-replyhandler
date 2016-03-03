package com.kana.connect.server.receiver;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.junit.Test;

public class TestRegex
{
    @Test
    public void testSimpleRegex()
    {
        //Properties props = new Properties();
        //props.setProperty("regex_0.pattern", ".");
        String smsMessage = "Hello from SMPPSim 2";
        
        Pattern p = Pattern.compile(".*");
        Matcher matcher = p.matcher(smsMessage);
        boolean isMatch = matcher.matches();
        Assert.assertTrue(isMatch);
    }
}
