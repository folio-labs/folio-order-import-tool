package org.olf.folio.order.folioapis;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class FolioData extends FolioAccess {

  // FOLIO APIS PATHS
  public static final String HOLDINGS_STORAGE_PATH = "holdings-storage/holdings";
  public static final String INSTANCES_PATH = "inventory/instances";
  public static final String ITEMS_PATH = "inventory/items";
  public static final String COMPOSITE_ORDERS_PATH = "orders/composite-orders";
  public static final String NOTES_PATH = "notes";
  public static final String LOCATIONS_PATH = "locations";
  public static final String ORDERS_PO_NUMBER_PATH = "orders/po-number";
  public static final String ORGANIZATIONS_PATH = "organizations-storage/organizations";
  public static final String EXPENSE_CLASSES_PATH = "finance/expense-classes";
  public static final String ACQUISITION_METHODS_PATH = "orders/acquisition-methods";
  public static final String INSTANCE_TYPES_PATH = "instance-types";
  public static final String MATERIAL_TYPES_PATH = "material-types";
  public static final String LOAN_TYPES_PATH = "loan-types";
  public static final String CONTRIBUTOR_TYPES_PATH = "contributor-types";
  public static final String HOLDINGS_TYPES_PATH = "holdings-types";
  public static final String NOTE_TYPES_PATH = "note-types";
  public static final String CONFIGURATIONS_ENTRIES_PATH = "configurations/entries";
  public static final String FUNDS_PATH = "finance/funds";
  public static final String FISCAL_YEARS_PATH = "finance/fiscal-years";
  public static final String BUDGETS_PATH = "finance/budgets";
  public static final String BUDGET_EXPENSE_CLASSES_PATH = "finance-storage/budget-expense-classes";
  public static final String TAGS_PATH = "tags";

  // NAMES OF ENTITY ARRAYS IN RESPONSE JSON FROM THE APIS
  public static final String INSTANCES_ARRAY = "instances";
  public static final String HOLDINGS_RECORDS_ARRAY = "holdingsRecords";
  public static final String ITEMS_ARRAY = "items";
  public static final String MATERIAL_TYPES_ARRAY = "mtypes";
  public static final String ORGANIZATIONS_ARRAY = "organizations";
  public static final String FUNDS_ARRAY = "funds";
  public static final String EXPENSE_CLASSES_ARRAY = "expenseClasses";
  public static final String BUDGETS_ARRAY = "budgets";
  public static final String BUDGET_EXPENSE_CLASSES_ARRAY = "budgetExpenseClasses";
  public static final String ACQUISITION_METHODS_ARRAY = "acquisitionMethods";
  public static final String LOCATIONS_ARRAY = "locations";
  public static final String LOAN_TYPES_ARRAY = "loantypes";
  public static final String FISCAL_YEARS_ARRAY = "fiscalYears";
  public static final String INSTANCE_TYPES_ARRAY = "instanceTypes";
  public static final String CONTRIBUTOR_TYPES_ARRAY = "contributorTypes";
  public static final String HOLDINGS_TYPES_ARRAY = "holdingsTypes";
  public static final String NOTE_TYPES_ARRAY = "noteTypes";
  public static final String CONFIGS_ARRAY = "configs";
  public static final String TAGS_ARRAY = "tags";

  // CACHING LOOKED UP VALUES
  public static final Map<String,String> organizationCodeToUuid = new HashMap<>();
  public static final Map<String,String> expenseClassCodeToUuid = new HashMap<>();
  public static final Map<String,String> addressNameToUuid = new HashMap<>();
  public static final Map<String,String> locationNameToUuid = new HashMap<>();
  public static final Map<String,String> instanceTypeNameToUuid = new HashMap<>();
  public static final Map<String,String> materialTypeNameToUuid = new HashMap<>();
  public static final Map<String,String> loanTypeNameToUuid = new HashMap<>();
  public static final Map<String,String> contributorTypeNameToUuid = new HashMap<>();
  public static final Map<String,String> contributorTypeCodeToUuid = new HashMap<>();
  public static final Map<String,String> holdingsTypeNameToUuid = new HashMap<>();
  public static final Map<String,String> noteTypeNameToUuid = new HashMap<>();
  public static final Map<String,String> fiscalYearCodeToUuid = new HashMap<>();
  public static final Map<String,String> fundCodeToUuid = new HashMap<>();
  public static final Map<String,String> fundIdAndFiscalYearIdToBudgetId = new HashMap<>();
  public static final Map<String,String> acquisitionMethodValueToUuid = new HashMap<>();
  public static final Map<String,String> budgetAndExpClassToBudgetExpClassId = new HashMap<>();

  public static String getNextPoNumberFromOrders() throws Exception {
    return (callApiGet(ORDERS_PO_NUMBER_PATH)).getString("poNumber");
  }

  public static String getLocationIdByName(String locationName) throws Exception {
    return getIdByKey (
            locationName,
            LOCATIONS_PATH +"?query=(name==%22" + encode(locationName) + "%22)",
            LOCATIONS_ARRAY,
            locationNameToUuid);
  }

  public static String getOrganizationId(String organizationCode) throws Exception {
    return getIdByKey(
            organizationCode,
            ORGANIZATIONS_PATH +"?query=(code==%22" + encode(organizationCode) + "%22)",
            ORGANIZATIONS_ARRAY,
            organizationCodeToUuid);
  }

  public static String getExpenseClassId(String expenseClassCode) throws Exception {
    return getIdByKey(
            expenseClassCode,
            EXPENSE_CLASSES_PATH + "?query=(code==%22" + encode(expenseClassCode) + "%22)",
            EXPENSE_CLASSES_ARRAY,
            expenseClassCodeToUuid);
  }

  public static String getAcquisitionMethodId (String acquisitionMethodValue) throws Exception {
    return getIdByKey(
            acquisitionMethodValue,
            ACQUISITION_METHODS_PATH + "?query=(value==%22" + encode(acquisitionMethodValue) +"%22)",
            ACQUISITION_METHODS_ARRAY,
            acquisitionMethodValueToUuid);
  }

  public static String getInstanceTypeId(String instanceTypeName) throws Exception {
    return getIdByKey(
            instanceTypeName,
            INSTANCE_TYPES_PATH + "?query=(name==%22" + encode(instanceTypeName) + "%22)",
            INSTANCE_TYPES_ARRAY,
            instanceTypeNameToUuid);
  }

  public static String getMaterialTypeId (String materialTypeName) throws Exception {
    return getIdByKey(
            materialTypeName,
            MATERIAL_TYPES_PATH + "?query=(name==%22" + encode(materialTypeName) + "%22)",
            MATERIAL_TYPES_ARRAY,
            materialTypeNameToUuid);

  }

  public static String getLoanTypeId (String loanTypeName) throws Exception {
    return getIdByKey(
            loanTypeName,
            LOAN_TYPES_PATH + "?query=(name==%22" + encode(loanTypeName) + "%22)",
            LOAN_TYPES_ARRAY,
            loanTypeNameToUuid
    );
  }

  public static String getContributorTypeIdByName (String contributorTypeName) throws Exception {
    return getIdByKey(
            contributorTypeName,
            CONTRIBUTOR_TYPES_PATH + "?query=(name==%22" + encode(contributorTypeName) + "%22)",
            CONTRIBUTOR_TYPES_ARRAY,
            contributorTypeNameToUuid);
  }

  public static String getContributorTypeIdByCode (String contributorTypeCode) throws Exception {
    return getIdByKey(
            contributorTypeCode,
            CONTRIBUTOR_TYPES_PATH + "?query=(code==%22" + encode(contributorTypeCode) + "%22)",
            CONTRIBUTOR_TYPES_ARRAY,
            contributorTypeCodeToUuid);
  }

  public static String getHoldingsTypeIdByName (String holdingsTypeName) throws Exception {
    return getIdByKey(
            holdingsTypeName,
            HOLDINGS_TYPES_PATH + "?query=(name==%22" + encode(holdingsTypeName) + "%22)",
            HOLDINGS_TYPES_ARRAY,
            holdingsTypeNameToUuid);
  }

  public static String getNoteTypeIdByName (String noteTypeName) throws Exception {
    return getIdByKey(
            noteTypeName,
            NOTE_TYPES_PATH + "?query=(name==%22" + encode(noteTypeName) + "%22)",
            NOTE_TYPES_ARRAY,
            noteTypeNameToUuid);
  }

  public static String getAddressIdByName (String addressName) throws Exception {
    if (addressName == null || addressName.isEmpty()) {
      return null;
    } else {
      if (addressNameToUuid.isEmpty()) {
        String configsEndpoint = CONFIGURATIONS_ENTRIES_PATH + "?limit=1000&query=%28module%3DTENANT%20and%20configName%3Dtenant.addresses%29";
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
            FUNDS_PATH + "?query=(code='" + encode(fundCode) + "')",
            FUNDS_ARRAY,
            fundCodeToUuid);
  }

  public static String getFiscalYearId (String fiscalYearCode) throws Exception {
    return getIdByKey (
            fiscalYearCode,
            FISCAL_YEARS_PATH + "?query=(code='" + encode(fiscalYearCode) + "')",
            FISCAL_YEARS_ARRAY,
            fiscalYearCodeToUuid);
  }

  public static String getBudgetId(String fundId, String fiscalYearId) throws Exception {
    String comboKey = fundId + "/" + fiscalYearId;
    if (! fundIdAndFiscalYearIdToBudgetId.containsKey(comboKey)) {
      String fundBalanceQuery = BUDGETS_PATH + "?query=(fundId==" + "%22" + fundId + "%22" + "+and+" + "fiscalYearId==" + "%22" + fiscalYearId + "%22)";
      String budgetId = getFirstId(callApiGetArray(fundBalanceQuery, BUDGETS_ARRAY));
      fundIdAndFiscalYearIdToBudgetId.put(comboKey, budgetId);
    }
    return fundIdAndFiscalYearIdToBudgetId.get(comboKey);
  }

  public static String getBudgetExpenseClassId(String budgetId, String expenseClassId) throws Exception {
    String comboKey = budgetId + "/" + expenseClassId;
    if (! budgetAndExpClassToBudgetExpClassId.containsKey(comboKey) ) {
      String budgetExpenseClassQuery = BUDGET_EXPENSE_CLASSES_PATH + "?query=(budgetId==" + "%22" + budgetId + "%22" + "+and+" + "expenseClassId==" + "%22" + expenseClassId + "%22)";
      String budgetExpenseClassId = getFirstId(callApiGetArray(budgetExpenseClassQuery, BUDGET_EXPENSE_CLASSES_ARRAY));
      budgetAndExpClassToBudgetExpClassId.put(comboKey, budgetExpenseClassId);
    }
    return budgetAndExpClassToBudgetExpClassId.get(comboKey);
  }

  public static JSONArray getTags (String objectCode) throws Exception {
    String tagEndpoint = TAGS_PATH + "?query=(label==" + encode(objectCode) + ")";
    JSONObject tagResponse = callApiGet(tagEndpoint);
    if (tagResponse.has(TAGS_ARRAY)) {
      return tagResponse.getJSONArray(TAGS_ARRAY);
    } else {
      return new JSONArray();
    }
  }

  private static String getIdByKey (String key, String url, String nameOfArray, Map<String,String> map)
          throws Exception {
    if (key == null || key.isEmpty()) return null;
    if (!map.containsKey(key)) {
      map.put(key, getFirstId(callApiGetArray(url, nameOfArray)));
    }
    return map.get(key);
  }

  public static JSONObject getInstancesByQuery (String query) throws Exception {
    return callApiGet(INSTANCES_PATH + "?query=(" +encode(query) + ")");
  }

  private static String getFirstId(JSONArray array) {
    if (array == null || array.isEmpty()) {
      return null;
    } else {
      return (String) array.getJSONObject(0).get("id");
    }
  }

  protected static String encode(String str) {
    return URLEncoder.encode(str, StandardCharsets.UTF_8);
  }

}
