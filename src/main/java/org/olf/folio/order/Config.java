package org.olf.folio.order;

import org.apache.log4j.Logger;

import javax.servlet.ServletContext;

public class Config {

  public String baseOkapiEndpoint;
  public String folioUiUrl;
  public String folioUiInventoryPath;
  public String folioUiOrdersPath;
  public String apiUsername;
  public String apiPassword;
  public String tenant;
  public String uploadFilePath;
  public String fiscalYearCode;
  public String permLocationName;
  public String permELocationName;
  public String noteTypeName;
  public String materialType;
  public String textForElectronicResource;
  public boolean objectCodeRequired;
  public boolean importInvoice;
  public boolean failIfNoInvoiceData;
  public String paymentMethod;
  public String permLocationWithInvoiceImport;
  public String permELocationWithInvoiceImport;
  public boolean importSRS;

  private static final Logger logger = Logger.getLogger(Config.class);

  public Config(ServletContext context) {
    baseOkapiEndpoint = (String) context.getAttribute("baseOkapiEndpoint");
    if (baseOkapiEndpoint == null || baseOkapiEndpoint.isEmpty()) {
      baseOkapiEndpoint = (String) context.getAttribute("baseOkapEndpoint"); //previous spelling
    }
    folioUiUrl = (String) context.getAttribute("folioUiUrl");
    folioUiInventoryPath = (String) context.getAttribute("folioUiInventoryPath");
    folioUiOrdersPath = (String) context.getAttribute("folioUiOrdersPath");
    apiUsername = (String) context.getAttribute("okapi_username");
    apiPassword = (String) context.getAttribute("okapi_password");
    tenant = (String) context.getAttribute("tenant");
    uploadFilePath = (String) context.getAttribute("uploadFilePath");
    permLocationName = (String) context.getAttribute("permLocation"); // Default, could change with invoice
    permELocationName = (String) context.getAttribute("permELocation"); // Default, could change with invoice
    fiscalYearCode = (String) context.getAttribute("fiscalYearCode");
    noteTypeName = (String) context.getAttribute("noteType");
    materialType = (String) context.getAttribute("materialType");
    textForElectronicResource = (String) context.getAttribute("textForElectronicResource");
    // objectCode is required unless objectCodeRequired is explicitly set to false in import.properties
    objectCodeRequired = ! ("false".equalsIgnoreCase((String) context.getAttribute("objectCodeRequired")));
    importInvoice = "true".equalsIgnoreCase((String) context.getAttribute("importInvoice"));
    failIfNoInvoiceData =  "true".equalsIgnoreCase((String) context.getAttribute("failIfNoInvoiceData"));
    paymentMethod = (String) context.getAttribute("paymentMethod");
    importSRS = "true".equalsIgnoreCase((String) context.getAttribute("importSRS"));

    permLocationWithInvoiceImport = (String) context.getAttribute("permLocationWithInvoiceImport");
    permELocationWithInvoiceImport = (String) context.getAttribute("permELocationWithInvoiceImport");
  }


}
