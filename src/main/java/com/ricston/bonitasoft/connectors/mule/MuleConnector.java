/*
 * Copyright (c) Ricston Ltd.  All rights reserved.  http://www.ricston.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.ricston.bonitasoft.connectors.mule;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.ow2.bonita.connector.core.ConnectorError;
import org.ow2.bonita.connector.core.ProcessConnector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This MuleConnector will allow Bonita Processes to Send, Dispatch and Request
 * Messages using Mule. It uses the MuleManager in order to get the MuleContext to be
 * used.
 */
public class MuleConnector extends ProcessConnector
{

    // Configuration fields
    private List<List<Object>> messageProperties;
    private String endpointAddress;
    private String requestPayload;
    private Boolean synchronous;

    // Result fields
    private MuleMessage resultMessage;

    protected static final Logger LOG = Logger.getLogger(MuleConnector.class.getName());

    @Override
    protected void executeConnector() throws Exception
    {
        if (LOG.isLoggable(Level.FINE))
        {
            LOG.fine("Executing Connector");
            LOG.fine("ProcessDefinitionUUID=" + getProcessDefinitionUUID());
            LOG.fine("ProcessDefinitionUUID value=" + getProcessDefinitionUUID().getValue());
            LOG.fine("MuleManager=" + MuleManager.getInstance());
        }

        // Get/Start then Get a MuleContext and initialise the Client.
        MuleContext ctx = MuleManager.getInstance().getContext(getProcessDefinitionUUID().getValue());
        if (ctx == null)
        {
            MuleManager.getInstance().startContext(getProcessDefinitionUUID());
        }
        MuleClient client = new MuleClient(MuleManager.getInstance().getContext(
            getProcessDefinitionUUID().getValue()));

        // Check if a request/send/dispatch is to be done
        if (requestPayload == null)
        {
            // we need to do a request since the payload is empty, i.e. nothing to
            // send
            LOG.fine("Doing a Request to endpoint " + endpointAddress);
            resultMessage = client.request(endpointAddress, 10000);
            LOG.fine("Received value of " + resultMessage.getPayloadAsString());
        }
        else
        {
            // we need to do a send or a dispatch
            if (synchronous)
            {
                // we need to do a send since synchronous
                LOG.fine("Doing a Send to endpoint " + endpointAddress + " with payload " + requestPayload);
                resultMessage = client.send(endpointAddress, requestPayload, getProperties(), 10000);
                LOG.fine("Received value of " + resultMessage.getPayloadAsString());
            }
            else
            {
                // we need to do a dispatch
                LOG.fine("Doing a Dispatch to endpoint " + endpointAddress + " with payload "
                         + requestPayload);
                client.dispatch(endpointAddress, requestPayload, getProperties());
            }
        }
    }

    @Override
    protected List<ConnectorError> validateValues()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Utility method to generate a new Map from the message properties configured on
     * the connector
     */
    private Map getProperties()
    {
        HashMap map = new HashMap();
        if (messageProperties != null)
        {
            for (List<Object> entry : messageProperties)
            {
                map.put(entry.get(0), entry.get(1));
            }
        }
        return map;
    }

    /**
     * Setter for input argument 'messageProperties'
     */
    public void setMessageProperties(List<List<Object>> messageProperties)
    {
        this.messageProperties = messageProperties;
    }

    /**
     * Setter for input argument 'endpointAddress'
     */
    public void setEndpointAddress(String endpointAddress)
    {
        this.endpointAddress = endpointAddress;
    }

    /**
     * Setter for input argument 'requestPayload'
     */
    public void setRequestPayload(String requestPayload)
    {
        this.requestPayload = requestPayload;
    }

    /**
     * Setter for input argument 'synchronous'
     */
    public void setSynchronous(Boolean synchronous)
    {
        this.synchronous = synchronous;
    }

    /**
     * Getter for output argument 'result'
     */
    public Object getResult()
    {
        return resultMessage!=null?resultMessage.getPayload():null;
    }

    /**
     * Getter for output argument 'resultAsString'
     * 
     * @throws Exception
     */
    public Object getResultAsString() throws Exception
    {
        return resultMessage!=null?resultMessage.getPayloadAsString():null;
    }

    /**
     * Getter for output argument resultMessageProperties
     */
    public Object getResultMessageProperties()
    {
        // Probably this method will be called by bonita, at most once... thus no
        // need to 'cache' result
        HashMap resultMessageProperties=null;
        if(resultMessage!=null)
        {
        
            resultMessageProperties = new HashMap();
            Set set = resultMessage.getPropertyNames();
            for (Object s : set)
            {
                resultMessageProperties.put(s, resultMessage.getProperty(s.toString()));
            }
        }
        return resultMessageProperties;
    }

    /**
     * Getter for output argument muleMessage
     */
    public Object getMuleMessage()
    {
        return resultMessage;
    }

}
