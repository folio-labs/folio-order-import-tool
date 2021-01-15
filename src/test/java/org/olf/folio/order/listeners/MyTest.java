package org.olf.folio.order.listeners;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.After;
import org.junit.jupiter.api.Disabled; 

 
public class MyTest {
    
	@Test
	public void nullTest() {
		//
	} 
	@Test
	public void testConfig() { 
		try { 
			CompositeConfiguration config = new CompositeConfiguration();
			
			 
			PropertiesConfiguration props = new PropertiesConfiguration();
			try {
			    props.load(ClassLoader.getSystemResourceAsStream("application.properties"));
			} catch (ConfigurationException e) {
			    throw new RuntimeException(e);
			}
			
			// config.addConfiguration(new PropertiesConfiguration(path));
			
			config.addConfiguration(props);
			System.out.println("----------------------------");
			System.out.println("initializing properties");
			System.out.println("----------------------------");
			Iterator<String> keys = config.getKeys();
			while (keys.hasNext()) {
				String key = keys.next();
				System.out.println(key +": "+ config.getProperty(key)); 
			}            
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("FAILED TO START JETTY.  CANNOT FILE THE PROPERTY FILE: ");
			e.printStackTrace();
		} 
	}
    

}
