package org.olf.folio.order.listeners;

import java.util.Iterator;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.olf.folio.order.Config;

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
				context.setAttribute(key, config.getProperty(key));
			}
			ConfigurationCheck check = new ConfigurationCheck(config, context);
			// Load to static configuration object used throughout the app
			Config.load(context);
			boolean passed = check.validateConfiguration();
			keys = config.getKeys();
			logger.info(" ");
			while (keys.hasNext()) {
				String key = keys.next();
				if (!key.contains("password")) {
					logger.info(String.format("%-31s: %-23s", key, config.getProperty(key)));
				}
			}
			logger.info(" ");
			check.report();
			if (!passed) {
				if ("true".equalsIgnoreCase(config.getString(Config.P_EXIT_ON_CONFIG_ERRORS))) {
					throw new ConfigurationException(
									"SOME CONFIGURATION PROBLEMS ENCOUNTERED - SEE PREVIOUS LOG LINES");
				}
			}
			if (!check.resolvedCodesAndNames()) {
				if ("true".equalsIgnoreCase(config.getString(Config.P_EXIT_ON_FAILED_ID_LOOKUPS))) {
					throw new ConfigurationException(
									"ONE OR MORE CODES/NAMES WHERE NOT FOUND"
					);
				}
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
