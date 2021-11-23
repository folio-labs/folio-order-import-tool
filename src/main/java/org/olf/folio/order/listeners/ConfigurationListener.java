package org.olf.folio.order.listeners;

import java.util.Iterator;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;



public class ConfigurationListener implements ServletContextListener {

	public void contextInitialized(ServletContextEvent sce) {
		ServletContext context = sce.getServletContext();
		String path = "";
		String userHomeSysProp = System.getProperty( "user.home" );
		String configFileSysProp = System.getProperty( "configFile");
		if (configFileSysProp == null || configFileSysProp.isEmpty()) configFileSysProp =  userHomeSysProp + "/order/import.properties";
		try {
			path = (configFileSysProp.startsWith( "/" ) ? configFileSysProp : userHomeSysProp + "/" + configFileSysProp);
			CompositeConfiguration config = new CompositeConfiguration();
			config.addConfiguration( new PropertiesConfiguration( path ) );
			System.out.println("----------------------------");
			System.out.println("initializing properties using " + path);
			System.out.println("----------------------------");
			Iterator<String> keys = config.getKeys();
			while (keys.hasNext()) {
				String key = keys.next();
				if (!key.contains("password")) {
					System.out.printf("%-31s: %-23s%n", key, config.getProperty(key));
				}
				context.setAttribute(key, config.getProperty(key));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("FAILED TO START JETTY.  CANNOT FIND THE PROPERTY FILE: " + path);
			e.printStackTrace();
			System.exit(-1);
		} 

	}

	public void contextDestroyed(ServletContextEvent sce) {
		// TODO Auto-generated method stub

	}

}
