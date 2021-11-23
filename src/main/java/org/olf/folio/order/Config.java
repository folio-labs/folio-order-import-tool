package org.olf.folio.order;

import javax.servlet.ServletContext;

public class Config {

  public String baseOkapiEndpoint;
  public String apiUsername;
  public String apiPassword;
  public String tenant;
  public String fiscalYearCode;
  public String permLocationName;
  public String permELocationName;
  public String noteTypeName;
  public String materialType;
  public boolean objectCodeRequired;
  public boolean importInvoice;
  public boolean failIfNoInvoiceData;
  public boolean importSRS;
  public String permLocationWithInvoiceImport;
  public String permELocationWithInvoiceImport;

  public Config(ServletContext context) {
    //COLLECT VALUES FROM THE CONFIGURATION FILE
    baseOkapiEndpoint = (String) context.getAttribute("baseOkapiEndpoint");
    if (baseOkapiEndpoint == null || baseOkapiEndpoint.isEmpty())
    {
      // old spelling, to support old config files
      baseOkapiEndpoint = (String) context.getAttribute("baseOkapEndpoint");
    }
    apiUsername = (String) context.getAttribute("okapi_username");
    apiPassword = (String) context.getAttribute("okapi_password");
    tenant = (String) context.getAttribute("tenant");
    permLocationName = (String) context.getAttribute("permLocation"); // Default, could change with invoice
    permELocationName = (String) context.getAttribute("permELocation"); // Default, could change with invoice
    fiscalYearCode = (String) context.getAttribute("fiscalYearCode");
    noteTypeName = (String) context.getAttribute("noteType");
    materialType = (String) context.getAttribute("materialType");
    //String fiscalYearCode =  (String) context.getAttribute("fiscalYearCode");

    // UC extensions to import.properties
    // objectCode is required unless objectCodeRequired is explicitly set to false in import.properties
    objectCodeRequired = ! ("false".equalsIgnoreCase((String) context.getAttribute("objectCodeRequired")));
    importInvoice = "true".equalsIgnoreCase((String) context.getAttribute("importInvoice"));
    failIfNoInvoiceData =  "true".equalsIgnoreCase((String) context.getAttribute("failIfNoInvoiceData"));
    importSRS = "true".equalsIgnoreCase((String) context.getAttribute("importSRS"));

    permLocationWithInvoiceImport = (String) context.getAttribute("permLocationWithInvoiceImport");
    permELocationWithInvoiceImport = (String) context.getAttribute("permELocationWithInvoiceImport");
  }
}
