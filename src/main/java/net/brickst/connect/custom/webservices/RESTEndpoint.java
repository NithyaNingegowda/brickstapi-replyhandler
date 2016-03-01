package net.brickst.connect.custom.webservices;

import java.util.Properties;

public class RESTEndpoint extends WebEndpoint
{
    public RESTEndpoint()
    {
        endpointType = EndpointType.REST;
    }

    @Override
    public void initFromProperties(Properties props, String prefix)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void deliverMessage(String content)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void initNetworkResources()
    {
        // TODO Auto-generated method stub
        
    }

}
