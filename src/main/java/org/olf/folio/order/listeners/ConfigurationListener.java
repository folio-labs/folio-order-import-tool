package org.olf.folio.order.listeners;

import java.util.Iterator;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener; 
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils; 

public class ConfigurationListener implements ServletContextListener {

	public void contextInitialized(ServletContextEvent sce) {
		ServletContext context = sce.getServletContext();
		
		try {
			CompositeConfiguration config = new CompositeConfiguration();
			config.setListDelimiter('|');
			String use_env = System.getenv("USE_SYSTEM_ENV");
			if (StringUtils.isNotEmpty(use_env) && StringUtils.equals(use_env, "true")) {
				config.setProperty("baseOkapEndpoint",  System.getenv("baseOkapEndpoint"));
				config.setProperty("okapi_username", System.getenv("okapi_username"));
				config.setProperty("okapi_password", System.getenv("okapi_password"));
				config.setProperty("tenant", System.getenv("tenant"));
		 
				config.setProperty("permELocation", System.getenv("permELocation"));
				config.setProperty("permLocation", System.getenv("permLocation"));
				config.setProperty("fiscalYearCode", System.getenv("fiscalYearCode"));
				config.setProperty("loanType", System.getenv("loanType"));
				config.setProperty("textForElectronicResources", System.getenv("textForElectronicResources"));
				config.setProperty("noteType", System.getenv("noteType"));
				config.setProperty("materialType", System.getenv("materialType"));
			 	
			} else {
			   PropertiesConfiguration props = new PropertiesConfiguration();
			   try {
			      props.load(context.getClassLoader().getResourceAsStream("application.properties"));
			    
			   } catch (ConfigurationException e) {
			      throw new RuntimeException(e);
			   }
			   config.addConfiguration(props);
			}
			
			System.out.println("----------------------------");
			System.out.println("initializing properties");
			System.out.println("----------------------------");
			Iterator<String> keys = config.getKeys();
			while (keys.hasNext()) {
				String key = keys.next();
				System.out.println(key +": "+ config.getProperty(key));
				context.setAttribute(key, config.getProperty(key));
			}            
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("FAILED TO START JETTY.  Cannot find application.properties ");
			e.printStackTrace();
		} 
		
	}

	public void contextDestroyed(ServletContextEvent sce) {
		// TODO Auto-generated method stub
		
	}

}
