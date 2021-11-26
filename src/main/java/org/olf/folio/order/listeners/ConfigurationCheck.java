package org.olf.folio.order.listeners;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.log4j.Logger;
import org.olf.folio.order.Config;
import org.olf.folio.order.Folio;

import javax.servlet.ServletContext;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigurationCheck {

  private static final String P_BASE_OKAPI_ENDPOINT = "baseOkapiEndpoint";
  private static final String P_BASE_OKAP_ENDPOINT = "baseOkapEndpoint"; // previous spelling
  private static final String P_TENANT = "tenant";
  private static final String P_OKAPI_USERNAME = "okapi_username";
  private static final String P_OKAPI_PASSWORD = "okapi_password";
  private static final String P_FISCAL_YEAR_CODE = "fiscalYearCode";
  private static final String P_LOAN_TYPE = "loanType";
  private static final String P_NOTE_TYPE = "noteType";
  private static final String P_MATERIAL_TYPE = "materialType";

  private final CompositeConfiguration config;
  private final ServletContext context;
  private final Map<String,String> propertyErrors = new HashMap<>();
  private final List<String> missingMandatoryProperties = new ArrayList<>();
  private final List<String> accessErrors = new ArrayList<>();

  private static final Logger logger = Logger.getLogger(ConfigurationCheck.class);

  public ConfigurationCheck (CompositeConfiguration config, ServletContext context) {
    this.config = config;
    this.context = context;
  }
  private boolean allMandatoryPresent = true;
  private boolean authenticationPassed = false;
  private boolean urlPassed = true;

  public boolean validateConfiguration () {
    allMandatoryPresent = checkMissingMandatoryProperties();
    urlPassed = checkBaseOkapiEndpointUrl();
    if (allMandatoryPresent && urlPassed) {
      authenticationPassed = checkAccess ();
    }
    return (allMandatoryPresent && authenticationPassed && urlPassed);
  }

  public List<String> getMissingMandatoryProperties () {
    return missingMandatoryProperties;
  }
  public List<String> getAccessErrors () {
    return accessErrors;
  }
  public Map<String,String> getPropertyErrors () {
    return propertyErrors;
  }

  public boolean passed () {
    return allMandatoryPresent && authenticationPassed && urlPassed;
  }

  public void report () {
    if (!allMandatoryPresent) {
      logger.error(" ");
      logger.error ("One or more missing, mandatory properties: ");
      for (String prop : getMissingMandatoryProperties()) {
        logger.error(prop);
      }
      logger.error(" ");
    } else if (!urlPassed) {
      logger.error(" ");
      for (String key: getPropertyErrors().keySet()) {
        logger.error(key + ": " + getPropertyErrors().get(key));
      }
      logger.error(" ");
    } else if (!authenticationPassed) {
      logger.error(" ");
      logger.error("Authentication to FOLIO failed for the provided access configuration");
      for (String error : getAccessErrors()) {
        logger.error(error);
      }
      logger.error(" ");
    } else {
      for (String key: getPropertyErrors().keySet()) {
        logger.error(key + ": " + getPropertyErrors().get(key));
      }
    }
  }

  private boolean checkMissingMandatoryProperties () {
    boolean passed = true;
    for (String prop : Arrays.asList(
            P_OKAPI_USERNAME,
            P_OKAPI_PASSWORD,
            P_TENANT,
            P_FISCAL_YEAR_CODE,
            P_LOAN_TYPE,
            P_NOTE_TYPE)) {
      if (!config.containsKey(prop)) {
        passed = false;
        addMissingMandatoryProperty(prop);
      }
    }
    if (!config.containsKey(P_BASE_OKAPI_ENDPOINT) && !config.containsKey(P_BASE_OKAP_ENDPOINT)) {
      passed = false;
      addMissingMandatoryProperty(P_BASE_OKAPI_ENDPOINT);
    }
    if (passed) {
      logger.info(" ");
      logger.info("All required configuration parameters present");
    }
    return passed;
  }

  private boolean checkBaseOkapiEndpointUrl () {
    if (config.containsKey(P_BASE_OKAPI_ENDPOINT) || config.containsKey(P_BASE_OKAP_ENDPOINT)) {
      String baseOkapiEndpoint = config.containsKey(P_BASE_OKAPI_ENDPOINT)
              ? config.getString(P_BASE_OKAPI_ENDPOINT)
              : config.getString(P_BASE_OKAP_ENDPOINT);
      try {
        new URL(baseOkapiEndpoint);
      } catch (MalformedURLException mue) {
        addPropertyError(P_BASE_OKAPI_ENDPOINT, mue.getMessage());
        return false;
      }
      logger.info(" ");
      logger.info("The URL for Okapi is valid");
      return true;
    } else {
      return false;
    }
  }

  private boolean checkAccess () {
    Config conf = new Config(context);
    try {
      Folio.initialize(conf, logger);
    } catch (Exception e) {
      accessErrors.add(e.getMessage());
      return false;
    }
    logger.info(" ");
    logger.info("Access to FOLIO works with the provided authentication configuration");
    return true;
  }

  public void addPropertyError(String property, String error) {
    if (propertyErrors.containsKey(property)) {
      propertyErrors.put(property, propertyErrors.get(property) + " and " + error);
    } else {
      propertyErrors.put(property, error);
    }
  }

  private void addMissingMandatoryProperty (String property) {
    missingMandatoryProperties.add(property);
  }

}
