package org.olf.folio.order.storage;

import org.json.JSONArray;
import org.json.JSONObject;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FolioData extends FolioAccess {


  public static final String ORGANIZATIONS_ARRAY = "organizations";
  public static final String FUNDS_ARRAY = "funds";
  public static final String EXPENSE_CLASSES_ARRAY = "expenseClasses";
  public static final String LOCATIONS_ARRAY = "locations";
  public static final String ARR_BUDGETS = "budgets";
  public static final String FISCAL_YEARS_ARRAY = "fiscalYears";
  public static final String INSTANCE_TYPES_ARRAY = "instanceTypes";
  public static final String CONTRIBUTOR_TYPES_ARRAY = "contributorTypes";
  public static final String HOLDINGS_TYPES_ARRAY = "holdingsTypes";
  public static final String NOTE_TYPES_ARRAY = "noteTypes";
  public static final Map<String,String> fiscalYearCodeToUuid = new HashMap<>();
  public static final Map<String,String> organizationCodeToUuid = new HashMap<>();
  public static final Map<String,String> fundCodeToUuid = new HashMap<>();
  public static final Map<String,String> expenseClassCodeToUuid = new HashMap<>();
  public static final Map<String,String> addressNameToUuid = new HashMap<>();
  public static final Map<String,String> locationNameToUuid = new HashMap<>();
  public static final Map<String,String> instanceTypeNameToUuid = new HashMap<>();
  public static final Map<String,String> contributorTypeNameToUuid = new HashMap<>();
  public static final Map<String,String> contributorTypeCodeToUuid = new HashMap<>();
  public static final Map<String,String> holdingsTypeNameToUuid = new HashMap<>();
  public static final Map<String,String> noteTypeNameToUuid = new HashMap<>();



  public static String getNextPoNumberFromOrders() throws Exception {
    return (callApiGet("orders/po-number")).getString("poNumber");
  }

  public static String getFiscalYearId (String fiscalYearCode) throws Exception {
    return getIdByKey (
            fiscalYearCode,
            "finance/fiscal-years?query=(code='" + fiscalYearCode + "')",
            FISCAL_YEARS_ARRAY,
            fiscalYearCodeToUuid);
  }

  public static String getLocationIdByName(String locationName) throws Exception {
    return getIdByKey (
            locationName,
            "locations?query=(name==%22" + encode(locationName) + "%22)",
            LOCATIONS_ARRAY,
            locationNameToUuid);
  }

  public static String getOrganizationId(String organizationCode) throws Exception {
    return getIdByKey(
            organizationCode,
            "organizations-storage/organizations?query=(code==%22" + organizationCode + "%22)",
            ORGANIZATIONS_ARRAY,
            organizationCodeToUuid);
  }

  public static String getExpenseClassId(String expenseClassCode) throws Exception {
    return getIdByKey(
            expenseClassCode,
            "finance/expense-classes?query=(code==%22" + expenseClassCode + "%22)",
            EXPENSE_CLASSES_ARRAY,
            expenseClassCodeToUuid);
  }

  public static String getInstanceTypeId(String instanceTypeName) throws Exception {
    return getIdByKey(
            instanceTypeName,
            "instance-types?query=(name==%22" + instanceTypeName + "%22)",
            INSTANCE_TYPES_ARRAY,
            instanceTypeNameToUuid);
  }

  public static String getContributorTypeIdByName (String contributorTypeName) throws Exception {
    return getIdByKey(
            contributorTypeName,
            "contributor-types?query=(name==%22" + contributorTypeName + "%22)",
            CONTRIBUTOR_TYPES_ARRAY,
            contributorTypeNameToUuid);
  }

  public static String getContributorTypeIdByCode (String contributorTypeCode) throws Exception {
    return getIdByKey(
            contributorTypeCode,
            "contributor-types?query=(code==%22" + contributorTypeCode + "%22)",
            CONTRIBUTOR_TYPES_ARRAY,
            contributorTypeCodeToUuid);
  }

  public static String getHoldingsTypeIdByName (String holdingsTypeName) throws Exception {
    return getIdByKey(
            holdingsTypeName,
            "holdings-types?query=(name==%22" + holdingsTypeName + "%22)",
            HOLDINGS_TYPES_ARRAY,
            holdingsTypeNameToUuid);
  }

  public static String getNoteTypeIdByName (String noteTypeName) throws Exception {
    return getIdByKey(
            noteTypeName,
            "note-types?query=(name==%22" + encode(noteTypeName) + "%22)",
            NOTE_TYPES_ARRAY,
            noteTypeNameToUuid);
  }

  public static String getAddressIdByName (String addressName) throws Exception {
    if (addressName == null || addressName.isEmpty()) {
      return null;
    } else {
      if (addressNameToUuid.isEmpty()) {
        String configsEndpoint = "configurations/entries?limit=1000&query=%28module%3DTENANT%20and%20configName%3Dtenant.addresses%29";
        JSONArray addresses = callApiGet(configsEndpoint).getJSONArray("configs");
        if (addresses.length()>0) {
          for (int i = 0; i < addresses.length(); i++) {
            JSONObject address = addresses.getJSONObject(i);
            addressNameToUuid.put(new JSONObject(address.getString("value")).getString("name"),
                    address.getString("id"));
          }
        }
      }
      return addressNameToUuid.get(addressName);
    }
  }

  public static String getFundId(String fundCode) throws Exception {
    return getIdByKey(
            fundCode,
            "finance/funds?query=(code='" + fundCode + "')",
            FUNDS_ARRAY,
            fundCodeToUuid);
  }

  public static JSONArray getFundBalances (String fundId, String fiscalYearId) throws Exception {
    String fundBalanceQuery = "finance/budgets?query=(fundId==" + "%22" + fundId + "%22" + "+and+" + "fiscalYearId==" + "%22" + fiscalYearId + "%22)";
    return callApiGet(fundBalanceQuery).getJSONArray(ARR_BUDGETS);
  }

  public static JSONArray getTags (String objectCode) throws Exception {
    String tagEndpoint = "tags?query=(label==" + objectCode + ")";
    return callApiGet(tagEndpoint).getJSONArray("tags");
  }

  private static String getIdByKey (String key, String url, String nameOfArray, Map<String,String> map)
          throws Exception {
    if (key == null || key.isEmpty()) return null;
    if (!map.containsKey(key)) {
      map.put(key, getFirstId(callApiGetArray(url,nameOfArray)));
    }
    return map.get(key);
  }

  public static JSONObject validateFund(String fundCode, String title, String price ) throws Exception {

    JSONObject responseMessage = new JSONObject();

    //VALIDATE FISCAL YEAR CODE
    String fiscalYearId = getFiscalYearId(config.fiscalYearCode);
    if (fiscalYearId == null) {
      responseMessage.put("error", "Fiscal year code in file (" + config.fiscalYearCode + ") does not exist in FOLIO");
      responseMessage.put("PONumber", "~error~");
      return responseMessage;
    }

     //VALIDATE FUND CODE
    String fundId = getFundId(fundCode);
    if (fundId == null) {
      responseMessage.put("error", "Fund code in file (" + fundCode + ") does not exist in FOLIO");
      responseMessage.put("PONumber", "~error~");
      return responseMessage;
    }

    //MAKE SURE THE FUND CODE EXISTS FOR THE CURRENT FISCAL YEAR
    if (getFundBalances(fundId, fiscalYearId).length() < 1) {
      responseMessage.put("error", "Fund code in file (" + fundCode + ") does not have a budget for fiscal year code in file (" + config.fiscalYearCode +")");
      responseMessage.put("title", title);
      responseMessage.put("PONumber", "~error~");
      return responseMessage;
    }
    return null;
  }

  public static JSONObject validateObjectCode(String objectCode, String title) throws Exception {
    //---------->VALIDATION: MAKE SURE THE TAG (AKA OBJECT CODE) EXISTS
    JSONObject responseMessage = new JSONObject();
    if (getTags(objectCode).length() < 1) {
      responseMessage.put("error", "Object code in the record (" + objectCode + ") does not exist in FOLIO");
      responseMessage.put("title", title);
      responseMessage.put("PONumber", "~error~");
      return responseMessage;
    }
    return null;
  }

  public static JSONObject validateOrganization(String orgCode, String title) throws Exception {
    JSONObject responseMessage = new JSONObject();
    if (getOrganizationId(orgCode) == null) {
      responseMessage.put("error", "Organization code in file (" + orgCode + ") does not exist in FOLIO");
      responseMessage.put("title", title);
      responseMessage.put("PONumber", "~error~");
      return responseMessage;
    }
    return null;
  }

  // Use this?
  public static JSONObject validateAddress (String name, String title) throws Exception {
    JSONObject responseMessage = new JSONObject();
    if (getAddressIdByName(name) == null) {
      responseMessage.put("error", "Address with the (" + name + ") does not exist in FOLIO");
      responseMessage.put("title", title);
      responseMessage.put("PONumber", "~error~");
      return responseMessage;
    }
    return null;
  }

  public static JSONObject validateRequiredValuesForInvoice(String title, Record record) {

    DataField nineEighty = (DataField) record.getVariableField("980");
    String vendorInvoiceNo = nineEighty.getSubfieldsAsString("h");
    String invoiceDate = nineEighty.getSubfieldsAsString("i");

    if (vendorInvoiceNo == null && invoiceDate == null && config.failIfNoInvoiceData) {
      JSONObject responseMessage = new JSONObject();
      responseMessage.put("title", title);
      responseMessage.put("error", "Invoice data configured to be required and no Invoice data was found in MARC record");
      responseMessage.put("PONumber", "~error~");
      return responseMessage;
    } else if (vendorInvoiceNo != null || invoiceDate != null) { // if one of these is present both should be
      Map<String, String> requiredFields = new HashMap<>();
      requiredFields.put("Vendor invoice no", vendorInvoiceNo);
      requiredFields.put("Invoice date", invoiceDate);

      // MAKE SURE EACH OF THE REQUIRED SUBFIELDS HAS DATA
      for (Map.Entry<String, String> entry : requiredFields.entrySet()) {
        if (entry.getValue() == null) {
          JSONObject responseMessage = new JSONObject();
          responseMessage.put("title", title);
          responseMessage.put("error", entry.getKey() + " Missing");
          responseMessage.put("PONumber", "~error~");
          return responseMessage;
        }
      }
    }
    return null;
  }

  private static String getFirstId(JSONArray array) {
    if (array == null || array.isEmpty()) {
      return null;
    } else {
      return (String) array.getJSONObject(0).get("id");
    }
  }

  private static String encode(String str) {
    return URLEncoder.encode(str, StandardCharsets.UTF_8);
  }

}
