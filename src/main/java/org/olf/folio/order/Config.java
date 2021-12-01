package org.olf.folio.order;

import org.apache.log4j.Logger;

import javax.servlet.ServletContext;

public class Config {

  // FOLIO ACCESS
  public static final String P_BASE_OKAPI_ENDPOINT = "baseOkapiEndpoint";
  public static final String P_BASE_OKAP_ENDPOINT = "baseOkapEndpoint"; // previous spelling
  public static final String P_TENANT = "tenant";
  public static final String P_OKAPI_USERNAME = "okapi_username";
  public static final String P_OKAPI_PASSWORD = "okapi_password";
  // OPERATIONS
  public static final String P_UPLOAD_FILE_PATH = "uploadFilePath";
  // STATIC VALUES
  public static final String P_FISCAL_YEAR_CODE = "fiscalYearCode";
  public static final String P_NOTE_TYPE = "noteType";
  public static final String P_MATERIAL_TYPE = "materialType";
  public static final String P_PERM_LOCATION = "permLocation";
  public static final String P_PERM_E_LOCATION = "permELocation";
  public static final String P_PERM_LOCATION_WITH_INVOICE_IMPORT = "permLocationWithInvoice";
  public static final String P_PERM_E_LOCATION_WITH_INVOICE_IMPORT = "permELocationWithInvoiceImport";
  public static final String P_TEXT_FOR_ELECTRONIC_RESOURCES = "textForElectronicResources";
  public static final String P_PAYMENT_METHOD = "paymentMethod";
  // PROCESSING INSTRUCTIONS
  public static final String P_OBJECT_CODE_REQUIRED = "objectCodeRequired";
  public static final String P_IMPORT_INVOICE = "importInvoice";
  public static final String P_FAIL_IF_NO_INVOICE_DATA = "failIfNoInvoiceData";
  public static final String P_EXIT_ON_CONFIG_ERRORS = "exitOnConfigErrors";
  public static final String P_EXIT_ON_FAILED_ID_LOOKUPS = "exitOnFailedIdLookups";
  public static final String P_ON_VALIDATION_ERRORS = "onValidationErrors";
  public static final String V_ON_VALIDATION_ERRORS_CANCEL_ALL = "cancelAll";
  public static final String V_ON_VALIDATION_ERRORS_SKIP_FAILED = "skipFailed";
  public static final String V_ON_VALIDATION_ERRORS_ATTEMPT_IMPORT = "attemptImport";
  public static final String P_ON_ISBN_INVALID = "onIsbnInvalid";
  public static final String V_ON_ISBN_INVALID_FLAG_ERROR = "flagError";
  public static final String V_ON_ISBN_INVALID_REMOVE_ISBN = "removeIsbn";
  public static final String V_ON_ISBN_INVALID_DO_NOTHING = "doNothing";
  // OPTIONAL FEEDBACK SETTINGS
  public static final String P_FOLIO_UI_URL = "folioUiUrl";
  public static final String P_FOLIO_UI_INVENTORY_PATH = "folioUiInventoryPath";
  public static final String P_FOLIO_UI_ORDERS_PATH = "folioUiOrdersPath";

  public static String baseOkapiEndpoint;
  public static String folioUiUrl;
  public static String folioUiInventoryPath;
  public static String folioUiOrdersPath;
  public static String apiUsername;
  public static String apiPassword;
  public static String tenant;
  public static String uploadFilePath;
  public static String fiscalYearCode;
  public static String permLocationName;
  public static String permELocationName;
  public static String noteTypeName;
  public static String materialType;
  public static String textForElectronicResources;
  public static boolean objectCodeRequired;
  public static boolean importInvoice;
  public static boolean failIfNoInvoiceData;
  public static String paymentMethod;
  public static String permLocationWithInvoiceImport;
  public static String permELocationWithInvoiceImport;
  public static boolean exitOnConfigErrors;
  public static boolean exitOnFailedIdLookups;
  public static String onValidationErrors;
  public static String onIsbnInvalid;
  public static boolean onIsbnInvalidFlagError;
  public static boolean onIsbnInvalidRemoveIsbn;
  public static boolean onIsbnInvalidDoNothing;
  public static boolean onValidationErrorsCancelAll;
  public static boolean onValidationErrorsSKipFailed;
  public static boolean onValidationErrorsAttemptImport;

  private static boolean loaded = false;

  public static void load (ServletContext context) {
    if (!loaded) {
      loaded = true;
      // FOLIO access
      baseOkapiEndpoint = (String) context.getAttribute(P_BASE_OKAPI_ENDPOINT);
      if (baseOkapiEndpoint == null || baseOkapiEndpoint.isEmpty()) {
        baseOkapiEndpoint = (String) context.getAttribute(P_BASE_OKAP_ENDPOINT); //previous spelling
      }
      folioUiUrl = (String) context.getAttribute(P_FOLIO_UI_URL);
      folioUiInventoryPath = (String) context.getAttribute(P_FOLIO_UI_INVENTORY_PATH);
      folioUiOrdersPath = (String) context.getAttribute(P_FOLIO_UI_ORDERS_PATH);
      apiUsername = (String) context.getAttribute(P_OKAPI_USERNAME);
      apiPassword = (String) context.getAttribute(P_OKAPI_PASSWORD);
      tenant = (String) context.getAttribute(P_TENANT);
      // Operations
      uploadFilePath = (String) context.getAttribute(P_UPLOAD_FILE_PATH);
      // Default values
      permLocationName = (String) context.getAttribute(P_PERM_LOCATION); // Default, could change with invoice
      permELocationName = (String) context.getAttribute(P_PERM_E_LOCATION); // Default, could change with invoice
      fiscalYearCode = (String) context.getAttribute(P_FISCAL_YEAR_CODE);
      noteTypeName = (String) context.getAttribute(P_NOTE_TYPE);
      materialType = (String) context.getAttribute(P_MATERIAL_TYPE);
      paymentMethod = (String) context.getAttribute(P_PAYMENT_METHOD);
      textForElectronicResources = (String) context.getAttribute(P_TEXT_FOR_ELECTRONIC_RESOURCES);
      // Processing instructions
      // objectCode is required unless objectCodeRequired is explicitly set to false in import.properties
      exitOnConfigErrors = "true".equalsIgnoreCase((String) context.getAttribute(P_EXIT_ON_CONFIG_ERRORS));
      exitOnFailedIdLookups = "true".equalsIgnoreCase((String) context.getAttribute(P_EXIT_ON_FAILED_ID_LOOKUPS));
      objectCodeRequired = !( "false".equalsIgnoreCase(
              (String) context.getAttribute(P_OBJECT_CODE_REQUIRED)) );
      failIfNoInvoiceData = "true".equalsIgnoreCase((String) context.getAttribute(P_FAIL_IF_NO_INVOICE_DATA));
      importInvoice = "true".equalsIgnoreCase((String) context.getAttribute(P_IMPORT_INVOICE));
      onValidationErrors = (String) context.getAttribute(P_ON_VALIDATION_ERRORS);
      onValidationErrorsCancelAll = onValidationErrors.equalsIgnoreCase(
              V_ON_VALIDATION_ERRORS_CANCEL_ALL);
      onValidationErrorsAttemptImport = onValidationErrors.equalsIgnoreCase(
              V_ON_VALIDATION_ERRORS_ATTEMPT_IMPORT);
      onValidationErrorsSKipFailed = onValidationErrors.equalsIgnoreCase(
              V_ON_VALIDATION_ERRORS_SKIP_FAILED);
      onIsbnInvalid = (String) context.getAttribute(P_ON_ISBN_INVALID);
      onIsbnInvalidFlagError = V_ON_ISBN_INVALID_FLAG_ERROR.equalsIgnoreCase(onIsbnInvalid);
      onIsbnInvalidRemoveIsbn = V_ON_ISBN_INVALID_REMOVE_ISBN.equalsIgnoreCase(onIsbnInvalid);
      onIsbnInvalidDoNothing = V_ON_ISBN_INVALID_DO_NOTHING.equalsIgnoreCase(onIsbnInvalid);

      permLocationWithInvoiceImport = (String) context.getAttribute(
              P_PERM_LOCATION_WITH_INVOICE_IMPORT);
      permELocationWithInvoiceImport = (String) context.getAttribute(
              P_PERM_E_LOCATION_WITH_INVOICE_IMPORT);
    }
  }

}
