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
