package org.olf.folio.order.listeners;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.log4j.Logger;
import org.olf.folio.order.Config;
import org.olf.folio.order.Constants;
import org.olf.folio.order.storage.FolioAccess;
import org.olf.folio.order.storage.FolioData;

import javax.servlet.ServletContext;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigurationCheck {

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
  private boolean authenticationPassed = true;
  private boolean urlPassed = true;
  private boolean codesAndNamesExist = true;

  public boolean validateConfiguration () {
    boolean validOnAnalysisErrorsSetting;
    allMandatoryPresent = checkMissingMandatoryProperties();
    urlPassed = checkBaseOkapiEndpointUrl();
    if (allMandatoryPresent && urlPassed) {
      authenticationPassed = checkAccess ();
      if (authenticationPassed) {
        codesAndNamesExist = validateCodesAndNames ();
      }
    }
    validOnAnalysisErrorsSetting = configOnValidationErrorsIsValid();
    return (allMandatoryPresent && authenticationPassed && urlPassed && validOnAnalysisErrorsSetting);
  }

  public boolean configOnValidationErrorsIsValid() {
    if (config.containsKey(Config.P_ON_VALIDATION_ERRORS)) {
      List<String> validValues = Arrays.asList(
              Config.V_ON_VALIDATION_ERRORS_CANCEL_ALL,
              Config.V_ON_VALIDATION_ERRORS_SKIP_FAILED,
              Config.V_ON_VALIDATION_ERRORS_ATTEMPT_IMPORT);
      String val = config.getString(Config.P_ON_VALIDATION_ERRORS);
      if (validValues.contains(val)) {
        return true;
      } else {
        addPropertyError(Config.P_ON_VALIDATION_ERRORS,
                "The value [" + val + "] not valid for this config property, " +
                        "should be one of " + validValues);
        return false;
      }
    }
    return true;
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

  public boolean resolvedCodesAndNames() {
    return codesAndNamesExist;
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
    }
    for (String key: getPropertyErrors().keySet()) {
      logger.error(key + ": " + getPropertyErrors().get(key));
    }
  }

  private boolean checkMissingMandatoryProperties () {
    boolean passed = true;
    for (String prop : Arrays.asList(
            Config.P_OKAPI_USERNAME,
            Config.P_OKAPI_PASSWORD,
            Config.P_TENANT,
            Config.P_FISCAL_YEAR_CODE,
            Config.P_NOTE_TYPE)) {
      if (!config.containsKey(prop)) {
        passed = false;
        addMissingMandatoryProperty(prop);
      }
    }
    if (!config.containsKey(Config.P_BASE_OKAPI_ENDPOINT) && !config.containsKey(
            Config.P_BASE_OKAP_ENDPOINT)) {
      passed = false;
      addMissingMandatoryProperty(Config.P_BASE_OKAPI_ENDPOINT);
    }
    if (passed) {
      logger.info(" ");
      logger.info("All required configuration parameters present");
    }
    return passed;
  }

  private boolean checkBaseOkapiEndpointUrl () {
    if (config.containsKey(Config.P_BASE_OKAPI_ENDPOINT) || config.containsKey(
            Config.P_BASE_OKAP_ENDPOINT)) {
      String baseOkapiEndpoint = config.containsKey(Config.P_BASE_OKAPI_ENDPOINT)
              ? config.getString(Config.P_BASE_OKAPI_ENDPOINT)
              : config.getString(Config.P_BASE_OKAP_ENDPOINT);
      try {
        new URL(baseOkapiEndpoint);
      } catch (MalformedURLException mue) {
        addPropertyError(Config.P_BASE_OKAPI_ENDPOINT, mue.getMessage());
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
    try {
      FolioAccess.initialize(logger);
    } catch (Exception e) {
      accessErrors.add(e.getMessage());
      return false;
    }
    logger.info(" ");
    logger.info("Access to FOLIO works with the provided authentication configuration");
    return true;
  }

  private boolean validateCodesAndNames () {
    boolean allCodesAndNamesResolved = true;
    try {
      String fiscalYearCode = config.getString(Config.P_FISCAL_YEAR_CODE);
      String fiscalYearId = FolioData.getFiscalYearId(fiscalYearCode);
      logger.info(" ");
      if (fiscalYearId == null) {
        addPropertyError(Config.P_FISCAL_YEAR_CODE, "Could not find fiscal year record for code [" + fiscalYearCode + "]");
        allCodesAndNamesResolved = false;
      } else {
        logger.info("Found ID [" + fiscalYearId + "] for fiscal year code [" + fiscalYearCode + "]");
      }
      String permLocation = config.getString(Config.P_PERM_LOCATION);
      if (permLocation != null && !permLocation.equalsIgnoreCase("NA")) {
        String permLocationId = FolioData.getLocationIdByName(permLocation);
        if (permLocationId == null) {
          addPropertyError(Config.P_PERM_LOCATION, "Could not find location by the name [" + permLocation + "]");
          allCodesAndNamesResolved = false;
        } else {
          logger.info("Found ID [" + permLocationId + "] for location name [" + permLocation + "]");
        }
      }
      String permELocation = config.getString(Config.P_PERM_E_LOCATION);
      if (permELocation != null && !permELocation.equalsIgnoreCase("NA")) {
        String permELocationId = FolioData.getLocationIdByName(permELocation);
        if (permELocationId == null) {
          addPropertyError(Config.P_PERM_E_LOCATION, "Could not find location by the name [" + permELocation + "]");
          allCodesAndNamesResolved = false;
        } else {
          logger.info("Found ID [" + permELocationId + "] for location name [" + permLocation + "]");
        }
      }
      String noteType = config.getString(Config.P_NOTE_TYPE);
      if (noteType != null && !noteType.isEmpty()) {
        String noteTypeId = FolioData.getNoteTypeIdByName(noteType);
        if (noteTypeId == null) {
          addPropertyError(Config.P_NOTE_TYPE, "Could not find note type by the name [" + noteType + "]");
          allCodesAndNamesResolved = false;
        } else {
          logger.info("Found ID [" + noteTypeId + "] for note type name [" + noteType + "]");
        }
      }
      String materialType = config.getString(Config.P_MATERIAL_TYPE);
      if (materialType != null && !materialType.isEmpty() && !materialType.equalsIgnoreCase("NA")) {
        String materialTypeId = Constants.MATERIAL_TYPES_MAP.get(materialType);
        if (materialTypeId == null) {
          addPropertyError(Config.P_MATERIAL_TYPE, "Could not find material type by the name [" + materialType + "]");
          allCodesAndNamesResolved = false;
        } else {
          logger.info("Found ID [" + materialTypeId + "] for material type name [" + materialType + "]");
        }
      }
    } catch (Exception e) {
      logger.error("There was an API error looking up IDs by names/codes " + e.getMessage());
      return false;
    }
    return allCodesAndNamesResolved;
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
