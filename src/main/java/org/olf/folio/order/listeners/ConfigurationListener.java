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
		try {
			String propertyFileName = "import.properties";
			String isProduction = sce.getServletContext().getInitParameter("environment-production");
			if (isProduction.equalsIgnoreCase("false")) propertyFileName = "import-test.properties";
			path = System.getProperty("user.home") + "/order/" +  propertyFileName;
			CompositeConfiguration config = new CompositeConfiguration();
			config.addConfiguration(new PropertiesConfiguration(path));
			System.out.println("----------------------------");
			System.out.println("initializing properties for: " + propertyFileName);
			System.out.println("----------------------------");
			Iterator<String> keys = config.getKeys();
			context.setAttribute("isProduction", isProduction);
			while (keys.hasNext()) {
				String key = keys.next();
				System.out.println(key);
				context.setAttribute(key, config.getProperty(key));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("FAILED TO START JETTY.  CANNOT FILE THE PROPERTY FILE: " + path);
			e.printStackTrace();
		} 

	}

	public void contextDestroyed(ServletContextEvent sce) {
		// TODO Auto-generated method stub

	}

}
