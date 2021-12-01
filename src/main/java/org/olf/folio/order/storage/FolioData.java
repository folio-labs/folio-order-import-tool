package org.olf.folio.order.storage;

import org.json.JSONArray;
import org.json.JSONObject;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.olf.folio.order.Config;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class FolioData extends FolioAccess {

  public static final String HOLDINGS_STORAGE_PATH = "holdings-storage/holdings";
  public static final String INSTANCES_PATH = "inventory/instances";
  public static final String ITEMS_PATH = "inventory/items";
  public static final String COMPOSITE_ORDERS_PATH = "orders/composite-orders";
  public static final String NOTES_PATH = "notes";

  public static final String INSTANCES_ARRAY = "instances";
  public static final String HOLDINGS_RECORDS_ARRAY = "holdingsRecords";
  public static final String ITEMS_ARRAY = "items";
  public static final String ORGANIZATIONS_ARRAY = "organizations";
  public static final String FUNDS_ARRAY = "funds";
  public static final String EXPENSE_CLASSES_ARRAY = "expenseClasses";
  public static final String LOCATIONS_ARRAY = "locations";
  public static final String BUDGETS_ARRAY = "budgets";
  public static final String FISCAL_YEARS_ARRAY = "fiscalYears";
  public static final String INSTANCE_TYPES_ARRAY = "instanceTypes";
  public static final String CONTRIBUTOR_TYPES_ARRAY = "contributorTypes";
  public static final String HOLDINGS_TYPES_ARRAY = "holdingsTypes";
  public static final String NOTE_TYPES_ARRAY = "noteTypes";
  public static final String CONFIGS_ARRAY = "configs";
  public static final String TAGS_ARRAY = "tags";

  public static final Map<String,String> organizationCodeToUuid = new HashMap<>();
  public static final Map<String,String> expenseClassCodeToUuid = new HashMap<>();
  public static final Map<String,String> addressNameToUuid = new HashMap<>();
  public static final Map<String,String> locationNameToUuid = new HashMap<>();
  public static final Map<String,String> instanceTypeNameToUuid = new HashMap<>();
  public static final Map<String,String> contributorTypeNameToUuid = new HashMap<>();
  public static final Map<String,String> contributorTypeCodeToUuid = new HashMap<>();
  public static final Map<String,String> holdingsTypeNameToUuid = new HashMap<>();
  public static final Map<String,String> noteTypeNameToUuid = new HashMap<>();
  public static final Map<String,String> fiscalYearCodeToUuid = new HashMap<>();
  public static final Map<String,String> fundCodeToUuid = new HashMap<>();
  public static final Map<String,String> fundIdAndFiscalYearIdToBudgetId = new HashMap<>();

  public static String getNextPoNumberFromOrders() throws Exception {
    return (callApiGet("orders/po-number")).getString("poNumber");
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
        JSONArray addresses = callApiGetArray(configsEndpoint, CONFIGS_ARRAY);
        if (addresses != null && !addresses.isEmpty()) {
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

  public static String getFiscalYearId (String fiscalYearCode) throws Exception {
    return getIdByKey (
            fiscalYearCode,
            "finance/fiscal-years?query=(code='" + fiscalYearCode + "')",
            FISCAL_YEARS_ARRAY,
            fiscalYearCodeToUuid);
  }

  public static String getBudgetId(String fundId, String fiscalYearId) throws Exception {
    String comboKey = fundId + "/" + fiscalYearId;
    if (! fundIdAndFiscalYearIdToBudgetId.containsKey(comboKey)) {
      String fundBalanceQuery = "finance/budgets?query=(fundId==" + "%22" + fundId + "%22" + "+and+" + "fiscalYearId==" + "%22" + fiscalYearId + "%22)";
      String budgetId = getFirstId(callApiGetArray(fundBalanceQuery, BUDGETS_ARRAY));
      fundIdAndFiscalYearIdToBudgetId.put(comboKey, budgetId);
    }
    return fundIdAndFiscalYearIdToBudgetId.get(comboKey);
  }

  public static JSONArray getTags (String objectCode) throws Exception {
    String tagEndpoint = "tags?query=(label==" + objectCode + ")";
    return callApiGetArray(tagEndpoint,TAGS_ARRAY);
  }

  private static String getIdByKey (String key, String url, String nameOfArray, Map<String,String> map)
          throws Exception {
    if (key == null || key.isEmpty()) return null;
    if (!map.containsKey(key)) {
      map.put(key, getFirstId(callApiGetArray(url,nameOfArray)));
    }
    return map.get(key);
  }

  public static String validateFund(String fundCode) throws Exception {
    //VALIDATE FISCAL YEAR CODE
    String fiscalYearId = getFiscalYearId(Config.fiscalYearCode);
    if (fiscalYearId == null) {
      return "Fiscal year code in file (" + Config.fiscalYearCode + ") does not exist in FOLIO";
    }
     //VALIDATE FUND CODE
    String fundId = getFundId(fundCode);
    if (fundId == null) {
      return "Fund code in file (" + fundCode + ") does not exist in FOLIO";
    }
    //MAKE SURE THE FUND CODE EXISTS FOR THE CURRENT FISCAL YEAR
    if (getBudgetId(fundId, fiscalYearId) == null) {
      return "Fund code in file (" + fundCode + ") does not have a budget for fiscal year code in file (" + Config.fiscalYearCode +")";
    }
    return null;
  }

  public static String validateObjectCode(String objectCode, String title) throws Exception {
    if (getTags(objectCode).length() < 1) {
      return "Object code in the record (" + objectCode + ") does not exist in FOLIO";
    }
    return null;
  }

  public static String validateOrganization(String orgCode, String title) throws Exception {
    JSONObject responseMessage = new JSONObject();
    if (getOrganizationId(orgCode) == null) {
      return "Organization code in file (" + orgCode + ") does not exist in FOLIO";
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
