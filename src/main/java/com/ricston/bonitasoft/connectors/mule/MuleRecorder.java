/*
 * $Id: MuleRecorder.java 1663 2011-10-30 17:55:15Z claude.mamo $
 * --------------------------------------------------------------------------------------
 * Copyright (c) Ricston Ltd  All rights reserved.  http://www.ricston.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package com.ricston.bonitasoft.connectors.mule;

import java.util.Set;

import org.ow2.bonita.facade.def.InternalProcessDefinition;
import org.ow2.bonita.facade.runtime.ActivityInstance;
import org.ow2.bonita.facade.runtime.ActivityState;
import org.ow2.bonita.facade.runtime.Category;
import org.ow2.bonita.facade.runtime.impl.InternalProcessInstance;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.services.Recorder;
import org.ow2.bonita.type.lob.Lob;

public class MuleRecorder implements Recorder
{
    public void recordProcessDeployed(InternalProcessDefinition processDef, String userId)
    {
        try
        {
            MuleManager.getInstance().startContext(processDef.getUUID());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void recordProcessEnable(InternalProcessDefinition processDef)
    {
        try
        {
            MuleManager.getInstance().startContext(processDef.getUUID());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
    }

    public void recordProcessDisable(InternalProcessDefinition processDef)
    {
        try
        {
            MuleManager.getInstance().stopContext(processDef.getUUID());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
    }

    public void recordProcessArchive(InternalProcessDefinition processDef, String userId)
    {
        //Nothing to do here, you can only Archive a process that is disabled.
    }
    
    public void remove(InternalProcessDefinition processDef)
    {
        try
        {
            MuleManager.getInstance().stopContext(processDef.getUUID());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    

    public void recordEnterActivity(ActivityInstance activityInstance){}
    public void recordBodyStarted(ActivityInstanceUUID activityInstanceUUID){}
    public void recordBodyEnded(ActivityInstanceUUID activityInstanceUUID){}
    public void recordBodyAborted(ActivityInstanceUUID activityInstanceUUID){}
    public void recordBodyCancelled(ActivityInstanceUUID activityInstanceUUID){}
    public void recordInstanceStarted(InternalProcessInstance instance, String loggedInUserId){}
    public void recordInstanceEnded(ProcessInstanceUUID instanceUUID, String loggedInUserId){}
    public void recordInstanceAborted(ProcessInstanceUUID instanceUUID, String loggedInUserId){}
    public void recordInstanceCancelled(ProcessInstanceUUID instanceUUID, String loggedInUserId){}
    public void recordTaskReady(ActivityInstanceUUID taskUUID, Set<String> candidates, String userId){}
    public void recordTaskStarted(ActivityInstanceUUID taskUUID, String loggedInUserId){}
    public void recordTaskFinished(ActivityInstanceUUID taskUUID, String loggedInUserId){}
    public void recordTaskSuspended(ActivityInstanceUUID taskUUID, String loggedInUserId){}
    public void recordTaskResumed(ActivityInstanceUUID taskUUID, String loggedInUserId){}
    public void recordTaskAssigned(ActivityInstanceUUID taskUUID,ActivityState taskState,String loggedInUserId,Set<String> candidates,String assignedUserId){}
    public void recordInstanceVariableUpdated(String variableId,Object variableValue,ProcessInstanceUUID instanceUUID,String userId){}
    public void recordActivityVariableUpdated(String variableId,Object variableValue,ActivityInstanceUUID activityUUID,String userId){}
    public void recordActivityPriorityUpdated(ActivityInstanceUUID activityUUID, int priority){}
    public void remove(InternalProcessInstance processInst){}
    public void removeLob(Lob lob){}
	public void recordNewCategory(Category arg0) {}

}


