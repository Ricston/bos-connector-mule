/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) Ricston Ltd  All rights reserved.  http://www.ricston.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package com.ricston.bonitasoft.connectors.mule;

import org.mule.api.MuleContext;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ow2.bonita.facade.APIAccessor;
import org.ow2.bonita.facade.QueryDefinitionAPI;
import org.ow2.bonita.facade.def.element.BusinessArchive;
import org.ow2.bonita.facade.impl.StandardAPIAccessorImpl;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;

/**
 * The MuleManager is used in order to manage Mule Instances. The Mule Manager allows
 * us to have the same mule instance being shared by specific processes. Due to the
 * nature of Bonita connectors, we cannot keep state in them and thus need a static
 * class to keep state, in our case, to keep a reference to the mule instances. The
 * Mule Manager will thus allow us to reuse a Mule instance per Process Definition
 * meaning that we do not have to start a mule server for each and every request. The
 * MuleManager is also responsible to startup mule using the configuraion found in
 * the mule-config.jar jar file embedded in the process.
 */
public class MuleManager
{
    private static volatile MuleManager instance = null;
    private Map<String, MuleContext> contextMap = new ConcurrentHashMap<String, MuleContext>();
    private Set<String> managedContexts = new HashSet<String>();

    public final String MULE_CONFIGURATION_ARCHIVE_DIR = "libs/";
    public final String MULE_CONFIGURATION_ARCHIVE_DEFAULT = MULE_CONFIGURATION_ARCHIVE_DIR+"mule-config.jar";
    public final String MULE_CONFIGURATION_ARCHIVE_SUFFIX = "-mule-config.jar";
    public final String MULE_CONFIGURATION_WORKING_DIRECTORY = ".mule/bonita/";

    protected static final Logger LOG = Logger.getLogger(MuleManager.class.getName());

    /**
     * This method is called in order to get the MuleContext to be used.
     * 
     * @param id The ID identifying the Mule instance. This is usually the process
     *            definition UUID
     * @return The MuleContext to be used. Might also return null if the MuleContext
     *         has not yet been started.
     */
    public MuleContext getContext(String id)
    {
        return contextMap.get(id);
    }

    /**
     * This method is called to start a MuleContext. This is synchronized allowing
     * only 1 Mule Context to get started at any one time. This allows us to ensure
     * that only 1 context per process definition is started
     * 
     * @param processDefId will be used to identify the mule context
     * @throws Exception
     */
    public synchronized void startContext(ProcessDefinitionUUID processDefId) throws Exception
    {
        LOG.info("Starting context for " + processDefId);
        
        if (contextMap.containsKey(processDefId.getValue()))
        {
            LOG.info("Context Key Already exists; some other thread started it for us or it is not managed by this Manager");
            return;
        }

        // Check for configuration files
        APIAccessor accessor = new StandardAPIAccessorImpl();
        QueryDefinitionAPI queryDefinitionAPI = accessor.getQueryDefinitionAPI();
        BusinessArchive businessArchive = queryDefinitionAPI.getBusinessArchive(processDefId);
        
        //Check 3 different names in order
        //processDefId-mule-config.jar
        //processDefIdWithoutVersion-mule-config.jar
        //mule-config.jar
        byte[] resource = businessArchive.getResource(MULE_CONFIGURATION_ARCHIVE_DIR+processDefId.getValue()+MULE_CONFIGURATION_ARCHIVE_SUFFIX);
        if(resource==null)
        {
            resource = businessArchive.getResource(MULE_CONFIGURATION_ARCHIVE_DIR+processDefId.getProcessName()+MULE_CONFIGURATION_ARCHIVE_SUFFIX);
        }
        if(resource==null)
        {
            resource = businessArchive.getResource(MULE_CONFIGURATION_ARCHIVE_DEFAULT);
        }
        
        //if (LOG.isLoggable(Level.FINE))
        if(true)
        {
            Map<String, byte[]> resources = businessArchive.getResources();
            for (String key : resources.keySet())
            {
                LOG.fine("ResourceKey=" + key);
            }
        }
        ArrayList<String> resourceEntries = new ArrayList<String>();
        ArrayList<String> resourceConfigs = new ArrayList<String>();

        if (resource != null)
        {
            FileUtils.writeByteArrayToFile(new File(MULE_CONFIGURATION_WORKING_DIRECTORY + processDefId
                                                    + ".jar"), resource);
            JarFile jar = new JarFile(MULE_CONFIGURATION_WORKING_DIRECTORY + processDefId + ".jar");

            Enumeration<JarEntry> entries = jar.entries();
            // List entries which are xml
            while (entries.hasMoreElements())
            {
                JarEntry entry = entries.nextElement();
                // ignore xml files which are not at the root level.
                if (entry.getName().endsWith(".xml")
                    && !(entry.getName().contains("/") || entry.getName().contains("\\")))
                {
                    resourceEntries.add(entry.getName());
                }
            }
            // Extract entries to file system
            for (String s : resourceEntries)
            {
                FileUtils.copyStreamToFile(jar.getInputStream(jar.getEntry(s)), new File(
                    MULE_CONFIGURATION_WORKING_DIRECTORY + processDefId + "-" + s));
                resourceConfigs.add(MULE_CONFIGURATION_WORKING_DIRECTORY + processDefId + "-" + s);
            }

        }
        DefaultMuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
        MuleContext context;
        if (resourceConfigs.size() > 0)
        {
            Iterator<String> iter = resourceConfigs.iterator();
            StringBuffer buffer = new StringBuffer(iter.next());
            while (iter.hasNext())
                buffer.append(",").append(iter.next());
            String configResources = buffer.toString();

            if (LOG.isLoggable(Level.FINE))
            {
                for (String s : resourceConfigs)
                {
                    LOG.fine("File " + s + " is:\r\n" + FileUtils.readFileToString(new File(s)));
                }
            }
            LOG.info("Config Resources are:" + configResources);

            context = muleContextFactory.createMuleContext(new SpringXmlConfigurationBuilder(configResources));
        }
        else
        {
            LOG.info("No resources found so starting default Mule");
            context = muleContextFactory.createMuleContext();
        }

        context.start();        
        
        contextMap.put(processDefId.getValue(), context);
        managedContexts.add(processDefId.getValue());
    }
    
    /**
     * This method is used by the mule-bos-transport to register Mule's Context so that the same Mule context is used
     */
    public void registerContext(String processName,MuleContext context){
        contextMap.put(processName, context);
    }
    
    public void unregisterContext(String processName){
        contextMap.remove(processName);
    }

    /**
     * This method is a helper method used during unit tests, making it easy to pass
     * the MuleConfiguration. Might also be used in the future if Bonita will start
     * supporting the attaching of files to a process.
     * 
     * @param processDefId Identifies the process for which the mule context is going to be started
     * @param muleResources contains the Mule configuration
     * @throws Exception
     */
    protected void startContext(String processDefId, String muleResources) throws Exception
    {

        DefaultMuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
        MuleContext context = muleContextFactory.createMuleContext(muleResources);
        context.start();
        contextMap.put(processDefId, context);
        managedContexts.add(processDefId);
    }
    
    protected void stopContext(String processDefId) throws Exception
    {
        MuleContext context=contextMap.get(processDefId);     
        if(context==null)
        {
            LOG.info("Not stopping context for "+processDefId+" since it is already stopped nor not managed by this Manager");
            return;
        }
        if(!managedContexts.contains(processDefId))
        {
            LOG.info("Not stopping context for "+processDefId+" since it is not managed by this Manager");
            return;
        }
        context.stop();
        context.dispose();
        contextMap.remove(processDefId);
        managedContexts.remove(processDefId);
    }
    
    protected void stopContext(ProcessDefinitionUUID processDefId) throws Exception
    {
        stopContext(processDefId.getValue());
    }

    // Singleton stuff

    protected MuleManager()
    {
    }

    public static MuleManager getInstance()
    {
        if (instance == null)
        {
            synchronized (MuleManager.class)
            {
                if (instance == null)
                {
                    instance = new MuleManager();
                }
            }
        }
        return instance;
    }
}
