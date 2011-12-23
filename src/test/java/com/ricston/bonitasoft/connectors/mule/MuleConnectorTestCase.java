/*
 * Copyright (c) Ricston Ltd.  All rights reserved.  http://www.ricston.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.ricston.bonitasoft.connectors.mule;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.ow2.bonita.connector.core.Connector;
import org.ow2.bonita.connector.core.ConnectorError;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.util.BonitaException;

import java.util.List;

public class MuleConnectorTestCase extends TestCase {

	public final String PROCESS_DEF_ID="myProcessId";
	
	public void testMuleManager() throws Exception{
	    MuleManager.getInstance().startContext(PROCESS_DEF_ID,"mule-config.xml");
	    
	    MuleContext ctx=MuleManager.getInstance().getContext(PROCESS_DEF_ID);
	    MuleClient client=new MuleClient(ctx);
	    
	    //test send
	    MuleMessage msg=client.send("vm://toSynchRequestResponse", "Stephen Fenech",null);
	    assertNotNull(msg);
	    assertNotNull(msg.getPayload());
	    assertEquals("Hello Stephen Fenech",msg.getPayloadAsString());
	    
	    //test dispatch
	    client.dispatch("vm://toAsychService", "Stephen Fenech",null);
        
	    //test request
	    msg=client.request("vm://fromAsychService", 10000);
	    assertNotNull(msg);
        assertNotNull(msg.getPayload());
        assertEquals("Hello Stephen Fenech",msg.getPayloadAsString());
        
        ctx.dispose();
	    
	}
	
	public void testConnector() throws Exception {
		MuleManager.getInstance().startContext(PROCESS_DEF_ID,"mule-config.xml");
		
		MuleConnector c = new MuleConnector(){
			@Override
			public ProcessDefinitionUUID getProcessDefinitionUUID() {
				return new ProcessDefinitionUUID(PROCESS_DEF_ID);
			}
		};
		
		assertNotNull(c.getProcessDefinitionUUID());
		
		//Synchronous Request Response		
		c.setEndpointAddress("vm://toSynchRequestResponse");
		c.setSynchronous(true);
		c.setRequestPayload("Stephen Fenech");
		work(c);
		assertEquals("Hello Stephen Fenech",c.getResult());
		assertEquals("Hello Stephen Fenech",c.getResultAsString());
		
		
		//Asynchronous Dispatch
		c = new MuleConnector(){
			@Override
			public ProcessDefinitionUUID getProcessDefinitionUUID() {
				return new ProcessDefinitionUUID(PROCESS_DEF_ID);
			}
		};
		c.setEndpointAddress("vm://toAsychService");
		c.setSynchronous(false);
		c.setRequestPayload("Stephen Fenech");
		work(c);
		assertNull(c.getResult());
		
		//Synchronous Request
		c = new MuleConnector(){
			@Override
			public ProcessDefinitionUUID getProcessDefinitionUUID() {
				return new ProcessDefinitionUUID(PROCESS_DEF_ID);
			}
		};
		c.setEndpointAddress("vm://fromAsychService");
		c.setSynchronous(true);
		work(c);
		assertEquals("Hello Stephen Fenech",c.getResult());
		
		//Stop Mule
		MuleManager.getInstance().stopContext(PROCESS_DEF_ID);
		
	}

	public void work(Connector connector) throws BonitaException {
		if (connector.containsErrors()) {
			containsErrors(connector, 1);
			fail(connector.getClass().getName() + " contains errors!");
		}
		try {
			connector.execute();
		} catch (Exception e) {
			e.printStackTrace();
			fail("The execution of " + connector.getClass().getName()
					+ " should work.");
		}
	}

	public void containsErrors(Connector connector, int errorNumber)
			throws BonitaException {
		List<ConnectorError> errors = connector.validate();
		for (ConnectorError error : errors) {
			System.out.println(error.getField() + " " + error.getError());
		}
		Assert.assertEquals(errorNumber, errors.size());
	}
}
