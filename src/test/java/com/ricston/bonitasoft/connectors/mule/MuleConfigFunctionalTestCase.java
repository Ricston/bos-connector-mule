/*
 * Copyright (c) Ricston Ltd.  All rights reserved.  http://www.ricston.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.ricston.bonitasoft.connectors.mule;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class MuleConfigFunctionalTestCase extends FunctionalTestCase {

	public void testBasic() throws Exception
	{
		MuleClient client=new MuleClient(muleContext);
		
		MuleMessage msg=client.send("vm://toSynchRequestResponse", "Stephen",null);
		assertNotNull(msg);
		assertEquals("Hello Stephen",msg.getPayload());
		
		client.dispatch("vm://toAsychService", "Stephen",null);
		msg=client.request("vm://fromAsychService",10000);
		assertNotNull(msg);
		assertEquals("Hello Stephen",msg.getPayload());
	}
	
	@Override
	protected String getConfigResources() {
		return "mule-config.xml";
	}

}
