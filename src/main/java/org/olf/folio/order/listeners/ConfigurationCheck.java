package org.olf.folio.order.listeners;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.log4j.Logger;
import org.olf.folio.order.Config;
import org.olf.folio.order.mapping.Constants;
import org.olf.folio.order.folioapis.FolioAccess;
import org.olf.folio.order.folioapis.FolioData;

import javax.servlet.ServletContext;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.olf.folio.order.Config.*;

public class ConfigurationCheck {

  private final CompositeConfiguration compositeConfiguration;
  private final Map<String,String> propertyErrors = new HashMap<>();
  private final List<String> missingMandatoryProperties = new ArrayList<>();
  private final List<String> accessErrors = new ArrayList<>();

  private static final Logger logger = Logger.getLogger(ConfigurationCheck.class);

  public ConfigurationCheck (CompositeConfiguration compositeConfiguration, ServletContext context) {
    this.compositeConfiguration = compositeConfiguration;
  }
  private boolean allMandatoryPresent = true;
  private boolean authenticationPassed = true;
  private boolean urlPassed = true;
  private boolean codesAndNamesExist = true;
  private boolean fileSystemPathIsWriteable = true;

  public boolean validateConfiguration () {
    boolean validOnAnalysisErrorsSetting;
    allMandatoryPresent = checkMissingMandatoryProperties();
    urlPassed = checkBaseOkapiEndpointUrl();
    boolean validMarcMappingChoice = specifiedMarcMappingExists();
    if (allMandatoryPresent && urlPassed) {
      authenticationPassed = checkFolioAccess();
      if (authenticationPassed) {
        codesAndNamesExist = validateCodesAndNames ();
      }
      fileSystemPathIsWriteable = checkUploadFilePath();
    }
    boolean validUnitOfPurchaseOrderImport = purchaseOrderUnitIsValid();
    boolean timeZoneValid = checkTimeZone();
    validOnAnalysisErrorsSetting = configOnValidationErrorsIsValid();
    // temporary check for the presence of an API:
    checkForAcquisitionMethodsEndpoint();
    //
    return (allMandatoryPresent
            && authenticationPassed
            && urlPassed
            && validMarcMappingChoice
            && validOnAnalysisErrorsSetting
            && fileSystemPathIsWriteable
            && timeZoneValid
            && validUnitOfPurchaseOrderImport);
  }

  public boolean configOnValidationErrorsIsValid() {
    if (compositeConfiguration.containsKey(Config.P_ON_VALIDATION_ERRORS)) {
      List<String> validValues = Arrays.asList(
              Config.V_ON_VALIDATION_ERRORS_CANCEL_ALL,
              Config.V_ON_VALIDATION_ERRORS_SKIP_FAILED,
              Config.V_ON_VALIDATION_ERRORS_ATTEMPT_IMPORT);
      if (validValues.contains(Config.onValidationErrors)) {
        return true;
      } else {
        addPropertyError(Config.P_ON_VALIDATION_ERRORS,
                "The value [" + Config.onValidationErrors + "] not valid for this config property, " +
                        "should be one of " + validValues);
        return false;
      }
    }
    return true;
  }

  public boolean purchaseOrderUnitIsValid() {
    if  ((!compositeConfiguration.containsKey(Config.P_PURCHASE_ORDER_UNIT)) ||
      Arrays.asList(V_PURCHASE_ORDER_UNIT_FILE,V_PURCHASE_ORDER_UNIT_RECORD).contains(compositeConfiguration.getString(P_PURCHASE_ORDER_UNIT).toLowerCase())) {
      return true;
    } else {
      addPropertyError(P_PURCHASE_ORDER_UNIT,
              String.format("[%s] not a valid unit of purchase order import. Valid options are one purchase order per [%s] or one purchase order per [%s]",
                      compositeConfiguration.getString(P_PURCHASE_ORDER_UNIT),
                      V_PURCHASE_ORDER_UNIT_FILE,
                      V_PURCHASE_ORDER_UNIT_RECORD));
      return false;
    }
  }

  public boolean specifiedMarcMappingExists() {
    if (compositeConfiguration.containsKey(Config.P_MARC_MAPPING)) {
      if (Arrays.asList(
              Config.V_MARC_MAPPING_CHI,
              Config.V_MARC_MAPPING_LAMBDA,
              Config.V_MARC_MAPPING_SIGMA).contains(compositeConfiguration.getString(Config.P_MARC_MAPPING).toLowerCase())) {
        return true;
      } else {
        addPropertyError(Config.P_MARC_MAPPING,
                String.format("Unknown MARC mapping option. Options are %s, %s, and %s.",
                        Config.V_MARC_MAPPING_CHI, Config.V_MARC_MAPPING_LAMBDA, Config.V_MARC_MAPPING_SIGMA));
        return false;
      }
    } else {
      return true;
    }
  }

  public boolean checkTimeZone () {
    if (compositeConfiguration.containsKey(Config.P_TZ_TIME_ZONE)) {
      try {
        ZoneId.of(compositeConfiguration.getString(Config.P_TZ_TIME_ZONE));
      } catch (ZoneRulesException zre) {
        addPropertyError(Config.P_TZ_TIME_ZONE, zre.getMessage()
                + "  (https://en.wikipedia.org/wiki/List_of_tz_database_time_zones)");
        return false;
      }
    }
    return true;
  }

  public List<String> getMissingMandatoryProperties () {
    return missingMandatoryProperties;
  }
  public boolean isMissingMandatoryProperties () {
    return !allMandatoryPresent;
  }

  public List<String> getAccessErrors () {
    return accessErrors;
  }
  public boolean authenticationFailed () {
    return !authenticationPassed;
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
        logger.error("   " + prop);
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
      if (!compositeConfiguration.containsKey(prop)) {
        passed = false;
        addMissingMandatoryProperty(prop);
      }
    }
    if (!compositeConfiguration.containsKey(Config.P_BASE_OKAPI_ENDPOINT) && !compositeConfiguration.containsKey(
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
    if (compositeConfiguration.containsKey(Config.P_BASE_OKAPI_ENDPOINT) || compositeConfiguration.containsKey(
            Config.P_BASE_OKAP_ENDPOINT)) {
      String baseOkapiEndpoint = compositeConfiguration.containsKey(Config.P_BASE_OKAPI_ENDPOINT)
              ? compositeConfiguration.getString(Config.P_BASE_OKAPI_ENDPOINT)
              : compositeConfiguration.getString(Config.P_BASE_OKAP_ENDPOINT);
      try {
        new URL(baseOkapiEndpoint);
      } catch (MalformedURLException mue) {
        addPropertyError(Config.P_BASE_OKAPI_ENDPOINT, mue.getMessage());
        return false;
      }
      logger.info(" ");
      logger.info(String.format("The URL for Okapi [%s] is a valid URL", baseOkapiEndpoint));
      return true;
    } else {
      return false;
    }
  }

  private boolean checkFolioAccess() {
    try {
      FolioAccess.initialize();
    } catch (Exception e) {
      accessErrors.add(e.getMessage());
      return false;
    }
    logger.info(" ");
    logger.info("Access to FOLIO works with the provided authentication configuration");
    return true;
  }

  private void checkForAcquisitionMethodsEndpoint () {
    try {
      FolioData.callApiGet("orders-storage/acquisition-methods?limit=1");
      logger.info("The present version of Orders has the (/orders-storage/acquisition-methods) API.");
      Config.acquisitionMethodsApiPresent = true;
    } catch (Exception e) {
      if (e.getMessage().contains("No suitable module found for path")) {
        logger.info("The present version of Orders does not provide the (/orders-storage/acquisition-methods) API.");
        Config.acquisitionMethodsApiPresent = false;
      }
    }
  }

  private boolean checkUploadFilePath () {
    if (compositeConfiguration.containsKey(Config.P_UPLOAD_FILE_PATH)) {
      String pathString = compositeConfiguration.getString(Config.P_UPLOAD_FILE_PATH);
      Path path = Path.of(pathString);
      if (Files.exists(path)) {
        if (Files.isWritable(path)) {
          return true;
        } else {
          addPropertyError(Config.P_UPLOAD_FILE_PATH,
                  String.format("Path '%s' exists but is not writeable.", pathString));
          return false;
        }
      } else if (Files.exists(path.getParent())) {
        boolean directoryCreated = new File(pathString).mkdir();
        if (directoryCreated) {
          logger.info(String.format("File upload path '%s' was created. ", pathString));
          return true;
        } else {
          addPropertyError(Config.P_UPLOAD_FILE_PATH,
                  String.format("The directory '%s' does not exist and could not be created.", pathString));
          return false;
        }
      } else {
        addPropertyError(Config.P_UPLOAD_FILE_PATH,
                String.format("Neither the directory '%s' or the parent '%s' exists on this server.", pathString, path.getParent()));
        return false;
      }
    } else {
      return true;
    }
  }

  private boolean validateCodesAndNames () {
    boolean allCodesAndNamesResolved = true;
    try {
      String fiscalYearCode = compositeConfiguration.getString(Config.P_FISCAL_YEAR_CODE);
      String fiscalYearId = FolioData.getFiscalYearId(fiscalYearCode);
      logger.info(" ");
      if (fiscalYearId == null) {
        addPropertyError(Config.P_FISCAL_YEAR_CODE, "Could not find fiscal year record for code [" + fiscalYearCode + "]");
        allCodesAndNamesResolved = false;
      } else {
        logger.info("Found ID [" + fiscalYearId + "] for fiscal year code [" + fiscalYearCode + "]");
      }
      String permLocation = compositeConfiguration.getString(Config.P_PERM_LOCATION);
      if (permLocation != null && !permLocation.equalsIgnoreCase("NA")) {
        String permLocationId = FolioData.getLocationIdByName(permLocation);
        if (permLocationId == null) {
          addPropertyError(Config.P_PERM_LOCATION, "Could not find location by the name [" + permLocation + "]");
          allCodesAndNamesResolved = false;
        } else {
          logger.info("Found ID [" + permLocationId + "] for location name [" + permLocation + "]");
        }
      }
      String permELocation = compositeConfiguration.getString(Config.P_PERM_E_LOCATION);
      if (permELocation != null && !permELocation.equalsIgnoreCase("NA")) {
        String permELocationId = FolioData.getLocationIdByName(permELocation);
        if (permELocationId == null) {
          addPropertyError(Config.P_PERM_E_LOCATION, "Could not find location by the name [" + permELocation + "]");
          allCodesAndNamesResolved = false;
        } else {
          logger.info("Found ID [" + permELocationId + "] for location name [" + permLocation + "]");
        }
      }
      String noteType = compositeConfiguration.getString(Config.P_NOTE_TYPE);
      if (noteType != null && !noteType.isEmpty()) {
        String noteTypeId = FolioData.getNoteTypeIdByName(noteType);
        if (noteTypeId == null) {
          addPropertyError(Config.P_NOTE_TYPE, "Could not find note type by the name [" + noteType + "]");
          allCodesAndNamesResolved = false;
        } else {
          logger.info("Found ID [" + noteTypeId + "] for note type name [" + noteType + "]");
        }
      }
      String materialType = compositeConfiguration.getString(Config.P_MATERIAL_TYPE);
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
