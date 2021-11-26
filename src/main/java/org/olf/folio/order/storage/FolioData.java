package org.olf.folio.order.storage;

import org.json.JSONArray;
import org.json.JSONObject;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
  public static final Map<String,String> fiscalYearCodeToUuid = new HashMap<>();
  public static final Map<String,String> organizationCodeToUuid = new HashMap<>();
  public static final Map<String,String> fundCodeToUuid = new HashMap<>();
  public static final Map<String,String> expenseClassCodeToUuid = new HashMap<>();
  public static final Map<String,String> addressNameToUuid = new HashMap<>();
  public static final Map<String,String> locationNameToUuid = new HashMap<>();
  public static  Map<String, String> referenceDataByName = new HashMap<>();

  public static String getNextPoNumberFromOrders() throws Exception {
    return (callApiGet("orders/po-number")).getString("poNumber");
  }

  public static Map<String,String> lookupReferenceValuesInFolio(List<String> lookupTables) throws Exception  {
    Map<String, String> lookUpTable = new HashMap<>();

    Iterator<String> lookupTablesIterator = lookupTables.iterator();
    while (lookupTablesIterator.hasNext()) {
      String endpoint = lookupTablesIterator.next();
      JSONObject jsonObject = callApiGet(endpoint);
      //(0) IS THE NUMBER OF ITEMS FOUND
      //(1) IS THE ARRAY OF ITEMS
      //NOT A GOOD APPROACH
      String elementName = (String) jsonObject.names().get(1);
      JSONArray elements = jsonObject.getJSONArray(elementName);
      Iterator elementsIterator = elements.iterator();
      while (elementsIterator.hasNext()) {
        JSONObject element = (JSONObject) elementsIterator.next();
        String id = element.getString("id");
        String name = element.getString("name");
        if (endpoint.contains("locations")) name = name + "-location";
        //SAVING ALL OF THE 'NAMES' SO THE UUIDs CAN BE LOOKED UP
        lookUpTable.put(name,id);
      }
    }
    return lookUpTable;
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

  private static String encode(String str) {
    return URLEncoder.encode(str, StandardCharsets.UTF_8);
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

  /**
   * Finds FOLIO's UUID for the provided fund code
   * @param fundCode The code to map to a UUID
   * @return the fund UUID if found, otherwise null
   * @throws Exception if the look-up request to FOLIO failed
   */
  public static String getFundId(String fundCode) throws Exception {
    return getIdByKey(
            fundCode,
            "finance/funds?query=((code='" + fundCode + "'))",
            FUNDS_ARRAY,
            fundCodeToUuid);
  }

  /**
   * Finds FOLIO's UUID for the provided expense class code
   * @param expenseClassCode The code to map to a UUID
   * @return the expense class UUID if found, otherwise null
   * @throws Exception if the look-up request to FOLIO failed
   */
  public static String getExpenseClassId(String expenseClassCode) throws Exception {
    return getIdByKey(
            expenseClassCode,
            "finance/expense-classes?query=code='" + expenseClassCode + "'",
            EXPENSE_CLASSES_ARRAY,
            expenseClassCodeToUuid);
  }

  public static String getRefUuidByName (String referenceName) throws Exception {
    if (referenceDataByName.isEmpty()) {
      createReferenceDataMap();
    }
    return referenceDataByName.get(referenceName);
  }

  public static void createReferenceDataMap() throws Exception {
    //IMPROVE THIS - 'text' is repeated (it is a 'name' in more than one reference table)
    List<String> referenceTables = new ArrayList<>();
    referenceTables.add("identifier-types?limit=1000");
    referenceTables.add("classification-types?limit=1000");
    referenceTables.add("contributor-types?limit=1000");
    referenceTables.add("contributor-name-types?limit=1000");
    referenceTables.add("locations?limit=10000");
    referenceTables.add("loan-types?limit=1000");
    referenceTables.add("note-types?limit=1000");
    referenceTables.add("material-types?limit=1000");
    referenceTables.add("instance-types?limit=1000");
    referenceTables.add("holdings-types?limit=1000");

    //SAVE REFERENCE TABLE VALUES (JUST LOOKUP THEM UP ONCE)
    if ( referenceDataByName == null || referenceDataByName.isEmpty()) {
      referenceDataByName = lookupReferenceValuesInFolio(referenceTables);
    }
  }

  private static String getFirstId(JSONArray array) {
    if (array == null || array.isEmpty()) {
      return null;
    } else {
      return (String) array.getJSONObject(0).get("id");
    }
  }

  private static String getFirstId(JSONObject json, String nameOfArray) {
    if (json == null || json.isEmpty()) {
      return null;
    } else if (nameOfArray == null || nameOfArray.isEmpty()) {
      return null;
    } else if (! json.has(nameOfArray) || ! (json.get(nameOfArray) instanceof JSONArray)) {
      return null;
    } else {
      return getFirstId(json.getJSONArray(nameOfArray));
    }
  }
}
