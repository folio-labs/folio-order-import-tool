package org.olf.folio.order;

import javax.servlet.ServletContext;
import java.util.Arrays;
import java.util.List;

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
  public static final String P_PERM_LOCATION_WITH_INVOICE_IMPORT = "permLocationWithInvoiceImport";
  public static final String P_PERM_E_LOCATION_WITH_INVOICE_IMPORT = "permELocationWithInvoiceImport";
  public static final String P_TEXT_FOR_ELECTRONIC_RESOURCES = "textForElectronicResources";
  public static final String P_PAYMENT_METHOD = "paymentMethod";
  // PROCESSING INSTRUCTIONS
  public static final String P_OBJECT_CODE_REQUIRED = "objectCodeRequired";
  public static final String P_IMPORT_INVOICE = "importInvoice";
  public static final String P_FAIL_IF_NO_INVOICE_DATA = "failIfNoInvoiceData";
  public static final String P_EXIT_ON_CONFIG_ERRORS = "exitOnConfigErrors";
  public static final String P_EXIT_ON_ACCESS_ERRORS = "exitOnAccessErrors";
  public static final String P_EXIT_ON_FAILED_ID_LOOKUPS = "exitOnFailedIdLookups";
  public static final String P_ON_VALIDATION_ERRORS = "onValidationErrors";
  public static final String V_ON_VALIDATION_ERRORS_CANCEL_ALL = "cancelAll";
  public static final String V_ON_VALIDATION_ERRORS_SKIP_FAILED = "skipFailed";
  public static final String V_ON_VALIDATION_ERRORS_ATTEMPT_IMPORT = "attemptImport";
  public static final String P_ON_ISBN_INVALID = "onIsbnInvalid";
  public static final String V_ON_ISBN_INVALID_REPORT_ERROR = "reportError";
  public static final String V_ON_ISBN_INVALID_REMOVE_ISBN = "removeIsbn";
  public static final String V_ON_ISBN_INVALID_DO_NOTHING = "doNothing";
  // OPTIONAL FEEDBACK SETTINGS
  public static final String P_FOLIO_UI_URL = "folioUiUrl";
  public static final String P_FOLIO_UI_INVENTORY_PATH = "folioUiInventoryPath";
  public static final String P_FOLIO_UI_ORDERS_PATH = "folioUiOrdersPath";

  public static final List<String> KNOWN_PROPERTIES = Arrays.asList(
          P_BASE_OKAP_ENDPOINT,
          P_BASE_OKAPI_ENDPOINT,
          P_EXIT_ON_ACCESS_ERRORS,
          P_EXIT_ON_CONFIG_ERRORS,
          P_EXIT_ON_FAILED_ID_LOOKUPS,
          P_FAIL_IF_NO_INVOICE_DATA,
          P_FISCAL_YEAR_CODE,
          P_FOLIO_UI_INVENTORY_PATH,
          P_FOLIO_UI_ORDERS_PATH,
          P_FOLIO_UI_URL,
          P_IMPORT_INVOICE,
          P_MATERIAL_TYPE,
          P_NOTE_TYPE,
          P_OBJECT_CODE_REQUIRED,
          P_OKAPI_PASSWORD,
          P_OKAPI_USERNAME,
          P_ON_ISBN_INVALID,
          P_ON_VALIDATION_ERRORS,
          P_PAYMENT_METHOD,
          P_PERM_E_LOCATION,
          P_PERM_LOCATION,
          P_PERM_E_LOCATION_WITH_INVOICE_IMPORT,
          P_PERM_LOCATION_WITH_INVOICE_IMPORT,
          P_TENANT,
          P_TEXT_FOR_ELECTRONIC_RESOURCES,
          P_UPLOAD_FILE_PATH
  );

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
  public static boolean exitOnAccessErrors;
  public static boolean exitOnFailedIdLookups;
  public static String onValidationErrors;
  public static String onIsbnInvalid;
  public static boolean onIsbnInvalidFlagError;
  public static boolean onIsbnInvalidRemoveIsbn;
  public static boolean onIsbnInvalidDoNothing;
  public static boolean onValidationErrorsCancelAll;
  public static boolean onValidationErrorsSKipFailed;
  public static boolean onValidationErrorsAttemptImport;

  private static final String EMPTY = "";
  private static final String NOT_APPLICABLE = "NA";
  private static ServletContext context;

  private static final String V_DEFAULT_UI_INVENTORY_PATH = "inventory/view";
  private static final String V_DEFAULT_UI_ORDERS_PATH = "orders/view";
  private static final String V_DEFAULT_MATERIAL_TYPE = "unspecified";

  public static void load (ServletContext servletContext) {
    if (context == null) {
      context = servletContext;
      // FOLIO access
      baseOkapiEndpoint = getText(P_BASE_OKAPI_ENDPOINT);
      if (baseOkapiEndpoint.isEmpty()) {
        baseOkapiEndpoint = getText(P_BASE_OKAP_ENDPOINT); //previous spelling
      }
      if (!baseOkapiEndpoint.endsWith("/")) {
        baseOkapiEndpoint = baseOkapiEndpoint + "/";
      }
      folioUiUrl = getText(P_FOLIO_UI_URL);
      folioUiInventoryPath = getText(P_FOLIO_UI_INVENTORY_PATH,V_DEFAULT_UI_INVENTORY_PATH);
      folioUiOrdersPath = getText(P_FOLIO_UI_ORDERS_PATH, V_DEFAULT_UI_ORDERS_PATH);
      apiUsername = getText(P_OKAPI_USERNAME);
      apiPassword = getText(P_OKAPI_PASSWORD);
      tenant = getText(P_TENANT);
      // Operations
      uploadFilePath = getText(P_UPLOAD_FILE_PATH);
      // Default values
      permLocationName = getText(P_PERM_LOCATION); // Default, could change with invoice
      permELocationName = getText(P_PERM_E_LOCATION); // Default, could change with invoice
      fiscalYearCode = getText(P_FISCAL_YEAR_CODE);
      noteTypeName = getText(P_NOTE_TYPE);
      materialType = getText(P_MATERIAL_TYPE, Constants.MATERIAL_TYPES_MAP.get(V_DEFAULT_MATERIAL_TYPE));
      paymentMethod = getText(P_PAYMENT_METHOD);
      textForElectronicResources = getText(P_TEXT_FOR_ELECTRONIC_RESOURCES);
      // Processing instructions
      exitOnConfigErrors = getBoolean(P_EXIT_ON_CONFIG_ERRORS,true);
      exitOnAccessErrors = getBoolean(P_EXIT_ON_ACCESS_ERRORS, true);
      exitOnFailedIdLookups = getBoolean(P_EXIT_ON_FAILED_ID_LOOKUPS,true);
      objectCodeRequired = getBoolean(P_OBJECT_CODE_REQUIRED, true);
      importInvoice = getBoolean(P_IMPORT_INVOICE,false);
      failIfNoInvoiceData = getBoolean(P_FAIL_IF_NO_INVOICE_DATA, importInvoice);
      onValidationErrors = getText(P_ON_VALIDATION_ERRORS, V_ON_VALIDATION_ERRORS_CANCEL_ALL);
      onValidationErrorsCancelAll = V_ON_VALIDATION_ERRORS_CANCEL_ALL.equalsIgnoreCase(onValidationErrors);
      onValidationErrorsAttemptImport = V_ON_VALIDATION_ERRORS_ATTEMPT_IMPORT.equalsIgnoreCase(onValidationErrors);
      onValidationErrorsSKipFailed = V_ON_VALIDATION_ERRORS_SKIP_FAILED.equalsIgnoreCase(onValidationErrors);
      onIsbnInvalid = getText(P_ON_ISBN_INVALID, V_ON_ISBN_INVALID_REPORT_ERROR);
      onIsbnInvalidFlagError = V_ON_ISBN_INVALID_REPORT_ERROR.equalsIgnoreCase(onIsbnInvalid);
      onIsbnInvalidRemoveIsbn = V_ON_ISBN_INVALID_REMOVE_ISBN.equalsIgnoreCase(onIsbnInvalid);
      onIsbnInvalidDoNothing = V_ON_ISBN_INVALID_DO_NOTHING.equalsIgnoreCase(onIsbnInvalid);

      permLocationWithInvoiceImport = getText(P_PERM_LOCATION_WITH_INVOICE_IMPORT,
              (importInvoice ? EMPTY : NOT_APPLICABLE));
      permELocationWithInvoiceImport = getText(P_PERM_E_LOCATION_WITH_INVOICE_IMPORT,
              (importInvoice ? EMPTY : NOT_APPLICABLE));
    }
  }

  public static String getText (String key, String defaultStr) {
    String prop = (String) context.getAttribute(key);
    if (prop == null || prop.isEmpty()) {
      return defaultStr;
    } else {
      return prop;
    }
  }

  public static String getText (String key) {
    String prop = (String) context.getAttribute(key);
    return prop == null ? EMPTY : prop;
  }

  public static boolean getBoolean (String key, boolean defaultsTo) {
    List<String> trueStrings = Arrays.asList("true","TRUE","1","yes","YES","Y");
    List<String> falseStrings = Arrays.asList("false","FALSE","0","NO","no");
    String prop = (String) context.getAttribute(key);
    if (trueStrings.contains(prop)) {
      return true;
    } else if (falseStrings.contains(prop)) {
      return false;
    } else {
      return defaultsTo;
    }

  }

}
