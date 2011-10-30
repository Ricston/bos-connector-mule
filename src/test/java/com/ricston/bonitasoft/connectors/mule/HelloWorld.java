
package com.ricston.bonitasoft.connectors.mule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HelloWorld
{

    private static Log logger = LogFactory.getLog(HelloWorld.class);

    public String sayHello(String name)
    {
        logger.info("sayHello is being executed with payload "+name);
        return "Hello " + name;
    }
}
