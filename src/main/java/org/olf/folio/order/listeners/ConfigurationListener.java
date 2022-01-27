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
		String configFileSysProp = System.getProperty("config") == null ?
						System.getProperty( "configFile") : System.getProperty("config");
		if (configFileSysProp == null || configFileSysProp.isEmpty()) {
			configFileSysProp = (String)context.getAttribute("config");
		}
		if (configFileSysProp == null || configFileSysProp.isEmpty()) configFileSysProp =
						userHomeSysProp + "/order/import.properties";
		try {
			path += (configFileSysProp.startsWith( "/" ) ?
							configFileSysProp : userHomeSysProp + "/" + configFileSysProp);
			CompositeConfiguration compositeConfiguration = new CompositeConfiguration();
			compositeConfiguration.addConfiguration( new PropertiesConfiguration( path ) );
			logger.info("Initializing properties using " + path);
			Iterator<String> keys = compositeConfiguration.getKeys();
			while (keys.hasNext()) {
				String key = keys.next();
				context.setAttribute(key, compositeConfiguration.getProperty(key));
			}
			ConfigurationCheck check = new ConfigurationCheck(compositeConfiguration, context);
			// Load to static configuration object used throughout the app
			Config.load(context);
			boolean passed = check.validateConfiguration();
			keys = compositeConfiguration.getKeys();
			logger.info(" ");
			while (keys.hasNext()) {
				String key = keys.next();
				if (!key.contains("password")) {
					if (Config.KNOWN_PROPERTIES.contains(key)) {
						logger.info(String.format("%-31s: %-23s", key, compositeConfiguration.getProperty(key)));
					} else {
						logger.info(String.format( "%-31s: %-40s   <-- UNRECOGNIZED SETTING", key, compositeConfiguration.getProperty(key)));
					}
				}
			}
			logger.info(" ");
			check.report();
			if (!passed) {
				if (check.isMissingMandatoryProperties() && Config.exitOnConfigErrors) {
					throw new ConfigurationException(
									"MISSING MANDATORY PROPERTIES: " + check.getMissingMandatoryProperties());
				} else if (check.authenticationFailed() && Config.exitOnAccessErrors) {
					throw new ConfigurationException(
									String.format("COULD NOT AUTHENTICATE [%s] TO FOLIO.", Config.apiUsername));
				}	else if (Config.exitOnConfigErrors) {
					throw new ConfigurationException(
									"FOUND PROBLEMS WITH ONE OR MORE CONFIGURATION SETTINGS.");
				}
			}
			if (!check.resolvedCodesAndNames()) {
				if (Config.exitOnFailedIdLookups) {
					throw new ConfigurationException(
									"ONE OR MORE LOOKUPS OF UUIDS FOR CODES/NAMES FAILED."
					);
				}
			}

		} catch (ConfigurationException e) {
			logger.error(String.format("FAILED TO START SERVICE: %s%n", e.getMessage()));
			System.exit(-1);
		}
	}

	public void contextDestroyed(ServletContextEvent sce) {
		System.out.println("CONTEXT DESTROYED");
	}

}
