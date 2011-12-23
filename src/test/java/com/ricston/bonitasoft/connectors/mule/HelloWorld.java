 /*
 * Copyright (c) Ricston Ltd.  All rights reserved.  http://www.ricston.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
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
