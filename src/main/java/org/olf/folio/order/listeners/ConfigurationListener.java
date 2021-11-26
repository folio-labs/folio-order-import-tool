package org.olf.folio.order.listeners;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

public class ConfigurationListener implements ServletContextListener {

	private static final Logger logger = Logger.getLogger(ConfigurationListener.class);
	public void contextInitialized(ServletContextEvent sce) {
		ServletContext context = sce.getServletContext();
		String path = "file://";
		String userHomeSysProp = System.getProperty( "user.home" );
		String configFileSysProp = System.getProperty( "configFile");
		if (configFileSysProp == null || configFileSysProp.isEmpty()) configFileSysProp =
						userHomeSysProp + "/order/import.properties";
		try {
			path += (configFileSysProp.startsWith( "/" ) ?
							configFileSysProp : userHomeSysProp + "/" + configFileSysProp);
			CompositeConfiguration config = new CompositeConfiguration();
			config.addConfiguration( new PropertiesConfiguration( path ) );
			logger.info("Initializing properties using " + path);
			Iterator<String> keys = config.getKeys();
			while (keys.hasNext()) {
				String key = keys.next();
				if (!key.contains("password")) {
					logger.info(String.format("%-31s: %-23s", key, config.getProperty(key)));
				}
				context.setAttribute(key, config.getProperty(key));
			}
			ConfigurationCheck check = new ConfigurationCheck(config, context);
			boolean passed = check.validateConfiguration();
			if (!passed) {
				check.report();
				throw new ConfigurationException("SOME CONFIGURATION PROBLEMS ENCOUNTERED - SEE PREVIOUS LOG LINES");
			}

		} catch (ConfigurationException e) {
			logger.error("FAILED TO START SERVICE: " + e.getMessage() + System.lineSeparator());
			System.exit(-1);
		}
	}

	public void contextDestroyed(ServletContextEvent sce) {
		System.out.println("CONTEXT DESTROYED");
	}

}
