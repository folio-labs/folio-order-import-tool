package org.olf.folio.order.storage;

import org.json.JSONArray;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.olf.folio.order.Config;

import java.util.HashMap;
import java.util.Map;

public class ValidationLookups extends FolioData {

  public static String validateFund(String fundCode) throws Exception {
    //VALIDATE FISCAL YEAR CODE
    String fiscalYearId = getFiscalYearId(Config.fiscalYearCode);
    if (fiscalYearId == null) {
      return "No fiscal year with the code (" + Config.fiscalYearCode + ") found in FOLIO";
    }
     //VALIDATE FUND CODE
    String fundId = getFundId(fundCode);
    if (fundId == null) {
      return "No fund with the code (" + fundCode + ") found in FOLIO.";
    }
    //MAKE SURE THE FUND CODE EXISTS FOR THE CURRENT FISCAL YEAR
    if (getBudgetId(fundId, fiscalYearId) == null) {
      return "The fund code in the record (" + fundCode + ") does not have a budget for the configured fiscal year (" + Config.fiscalYearCode +")";
    }
    return null;
  }

  public static String validateTag(String objectCode) throws Exception {
    if (getTags(objectCode).length() < 1) {
      return "No tag (" + objectCode + ") found in FOLIO";
    }
    return null;
  }

  public static String validateLocationName (String locationName) throws Exception {
    if (getLocationIdByName(locationName) == null) {
      return String.format("Not found. No location with name (%s) found in FOLIO.", locationName);
    } else {
      return null;
    }
  }

  public static String validateOrganization(String orgCode) throws Exception {
    if (getOrganizationId(orgCode) == null) {
      return "No organization with the code (" + orgCode + ") found in FOLIO";
    }
    return null;
  }

  public static String validateAddress (String name) throws Exception {
    if (getAddressIdByName(name) == null) {
      return "No address with the name (" + name + ") found in FOLIO.";
    }
    return null;
  }

  public static String validateMaterialTypeName (String name) throws  Exception {
    if (getMaterialTypeId(name) == null) {
      return String.format("Not found. No material type by name %s found in FOLIO.", name);
    } else {
      return null;
    }
  }

  public static String validateRequiredValuesForInvoice(String title, Record record) {
    DataField nineEighty = (DataField) record.getVariableField("980");
    String vendorInvoiceNo = nineEighty.getSubfieldsAsString("h");
    String invoiceDate = nineEighty.getSubfieldsAsString("i");

    if (vendorInvoiceNo == null && invoiceDate == null && Config.failIfNoInvoiceData) {
      return "Invoice data configured to be required and no Invoice data was found in MARC record";
    } else if (vendorInvoiceNo != null || invoiceDate != null) { // if one of these is present both should be
      Map<String, String> requiredFields = new HashMap<>();
      requiredFields.put("Vendor invoice no", vendorInvoiceNo);
      requiredFields.put("Invoice date", invoiceDate);

      // MAKE SURE EACH OF THE REQUIRED SUBFIELDS HAS DATA
      for (Map.Entry<String, String> entry : requiredFields.entrySet()) {
        if (entry.getValue() == null) {
          return entry.getKey() + " Missing";
        }
      }
    }
    return null;
  }

  public static String validateBudgetExpenseClass(String budgetId, String expenseClassId) throws Exception {
    if (FolioData.getBudgetExpenseClassId(budgetId, expenseClassId)==null) {
      return "Not found: budget expense class not found for given fund and expense class.";
    } else {
      return null;
    }
  }

  public static String validateBarcodeUniqueness(String barcode) throws Exception {
    JSONArray items = FolioAccess.callApiGetArray(ITEMS_PATH + "?query=(barcode=="+ encode(barcode) + ")", ITEMS_ARRAY);
    if (items.isEmpty()) {
      return null;
    } else {
      return String.format("Barcode (%s) already exists.", barcode);
    }
  }
}
