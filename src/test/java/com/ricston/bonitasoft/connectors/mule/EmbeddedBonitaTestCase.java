/*
 * Copyright (c) Ricston Ltd.  All rights reserved.  http://www.ricston.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.ricston.bonitasoft.connectors.mule;

import junit.framework.TestCase;
import org.mule.api.MuleContext;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.ow2.bonita.facade.ManagementAPI;
import org.ow2.bonita.facade.QueryRuntimeAPI;
import org.ow2.bonita.facade.RuntimeAPI;
import org.ow2.bonita.facade.def.element.BusinessArchive;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.runtime.*;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.util.AccessorUtil;
import org.ow2.bonita.util.BonitaConstants;
import org.ow2.bonita.util.BusinessArchiveFactory;
import org.ow2.bonita.util.SimpleCallbackHandler;

import javax.security.auth.login.LoginContext;
import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class EmbeddedBonitaTestCase extends TestCase {

    private static final String LOGIN = "admin";
    private static final String PASSWORD = "bpm";
    private static final String BAR_FILE_PATH = "src/test/resources/processes/HelloWorldMuleConnector--1.0.bar";
    private static final String JAAS_FILE_PATH = "src/test/resources/bonita/jaas-standard.cfg";
    private static final String BOS_ENV = "src/test/resources/bonita/bonita-environment.xml";
    private static final String MULE_CONFIG="src/test/resources/mule-config.xml";
    private static final String PROCESS_DEF_ID = "HelloWorldMuleConnector--1.0";

    final ManagementAPI managementAPI = AccessorUtil.getManagementAPI();
    final RuntimeAPI runtimeAPI = AccessorUtil.getRuntimeAPI();
    final QueryRuntimeAPI queryRuntimeAPI = AccessorUtil.getQueryRuntimeAPI();

    static
    {
        System.setProperty(BonitaConstants.JAAS_PROPERTY, JAAS_FILE_PATH);
        System.setProperty(BonitaConstants.ENVIRONMENT_PROPERTY, BOS_ENV);
    }

    
	public void testBasic() throws Exception
	{
	 // login
        LoginContext loginContext = new LoginContext("Bonita", new SimpleCallbackHandler(LOGIN, PASSWORD));
        loginContext.login();

        // clear everything, just in case there is any state left.
        managementAPI.deleteAllProcesses();
        
        // startup Mule and register the context. This way, now it is not the MuleManager that is managing the context
        DefaultMuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
	    MuleContext context;
	    context = muleContextFactory.createMuleContext(new SpringXmlConfigurationBuilder(MULE_CONFIG));
	    context.start();
	    
	    MuleManager.getInstance().registerContext(PROCESS_DEF_ID, context);
	    
	    // deploy the bar file
        final File barFile = new File(BAR_FILE_PATH);
        final BusinessArchive businessArchive = BusinessArchiveFactory.getBusinessArchive(barFile);
        final ProcessDefinition process = managementAPI.deploy(businessArchive);
        final ProcessDefinitionUUID processUUID = process.getUUID();
        
        //check that the context registered in the Mule Manager is identical to "our" context
        assertSame(context, MuleManager.getInstance().getContext(processUUID.getValue()));
        
        //check that Mule DOES NOT stop on disable
        managementAPI.disable(processUUID);
        assertTrue(context.isStarted());
        assertSame(context,MuleManager.getInstance().getContext(processUUID.getValue()));
        
        //check that Mule DOES NOT start on enable
        managementAPI.enable(processUUID);
        assertTrue(context.isStarted());
        assertSame(context,MuleManager.getInstance().getContext(processUUID.getValue()));
        
        //Check that Process/Connector works well
        //kick off process
        ProcessInstanceUUID processInstanceUUID=runtimeAPI.instantiateProcess(processUUID);
        ProcessInstance processInstance=queryRuntimeAPI.getProcessInstance(processInstanceUUID);
        Thread.sleep(1000);
        
        //do first task
        Set<TaskInstance> activitySet=processInstance.getTasks();
        assertEquals(1,activitySet.size());
        ActivityInstance enterNameActivity=activitySet.iterator().next();
        runtimeAPI.startTask(enterNameActivity.getUUID(), true);
        runtimeAPI.setVariable(enterNameActivity.getUUID(), "name", "Stephen Fenech");
        runtimeAPI.finishTask(enterNameActivity.getUUID(), true);
        
        //Sleep a bit for connector to do work
        Thread.sleep(5000);
        
        //get results and execute last task
        processInstance=queryRuntimeAPI.getProcessInstance(processInstanceUUID);
        activitySet=processInstance.getTasks();
        assertEquals(2,activitySet.size());
        Iterator<TaskInstance> iterator=activitySet.iterator();
        ActivityInstance showResultActivity=iterator.next();
        while(!ActivityState.READY.equals(showResultActivity.getState()))
        {
            showResultActivity=iterator.next();
        }
        runtimeAPI.startTask(showResultActivity.getUUID(), true);
        Map<String,Object> variables=processInstance.getLastKnownVariableValues();
        assertEquals(2,variables.size());
        assertEquals("Stephen Fenech",variables.get("name"));
        assertEquals("Hello Stephen Fenech",variables.get("result"));
        runtimeAPI.finishTask(showResultActivity.getUUID(), true);
        
        //check that process finished
        processInstance=queryRuntimeAPI.getProcessInstance(processInstanceUUID);
        assertEquals(InstanceState.FINISHED,processInstance.getInstanceState());
        
        //Undeploy Process and check that Mule is NOT shut down
        managementAPI.deleteProcess(processUUID);
        assertTrue(context.isStarted());
        assertEquals(context,MuleManager.getInstance().getContext(processUUID.getValue()));

        MuleManager.getInstance().unregisterContext(PROCESS_DEF_ID);
	    context.dispose();
        
	}
	
}
