package org.olf.folio.order;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Looks up UUIDs for organization, fund, or expense class codes, and caches the result.
 */
public class UuidMapping {

  private static final String ORGANIZATIONS = "organizations";
  private static final String FUNDS = "funds";
  private static final String EXPENSE_CLASSES = "expenseClasses";

  private final Map<String,String> organizationCodeToUuid = new HashMap<>();
  private final Map<String,String> fundCodeToUuid = new HashMap<>();
  private final Map<String,String> expenseClassCodeToUuid = new HashMap<>();
  private final Map<String,String> addressNameToUuid = new HashMap<>();
  public static Map<String, String> referenceDataByName = new HashMap<>();


  /**
   * Finds FOLIO's UUID for the provided organization code
   * @param organizationCode The code to map to a UUID
   * @return the organization UUID if found, otherwise null
   * @throws Exception if the look-up request to FOLIO failed
   */
  public String getOrganizationId(String organizationCode) throws Exception {
    if (organizationCode == null) {
      return null;
    } else {
      if ( !organizationCodeToUuid.containsKey(organizationCode) )
      {
        String organizationEndpoint = "organizations-storage/organizations?query=((code='" + organizationCode + "'))";
        String orgLookupResponse = Folio.callApiGet(organizationEndpoint);
        JSONArray organizations = new JSONObject(orgLookupResponse).getJSONArray(ORGANIZATIONS);
        organizationCodeToUuid.put(organizationCode, getIdOfFirstRecordIfAny(organizations));
      }
      return organizationCodeToUuid.get(organizationCode);
    }
  }

  public String getAddressIdByName (String addressName) throws Exception {
    if (addressName == null || addressName.isEmpty()) {
      return null;
    } else {
      if (addressNameToUuid.isEmpty()) {
        String configsEndpoint = "configurations/entries?limit=1000&query=%28module%3DTENANT%20and%20configName%3Dtenant.addresses%29";
        String configsResponse = Folio.callApiGet(configsEndpoint);
        JSONObject configsObject = new JSONObject(configsResponse);
        if (configsObject.has("configs")) {
          JSONArray addresses = configsObject.getJSONArray("configs");
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
  public String getFundId(String fundCode) throws Exception {
    if (fundCode == null) {
      return null;
    } else {
      if (!fundCodeToUuid.containsKey(fundCode)) {
        String fundEndpoint = "finance/funds?query=((code='" + fundCode + "'))";
        String fundResponse = Folio.callApiGet(fundEndpoint);
        JSONArray funds = new JSONObject(fundResponse).getJSONArray(FUNDS);
        fundCodeToUuid.put(fundCode, getIdOfFirstRecordIfAny(funds));
      }
      return fundCodeToUuid.get(fundCode);
    }
  }

  /**
   * Finds FOLIO's UUID for the provided expense class code
   * @param expenseClassCode The code to map to a UUID
   * @return the expense class UUID if found, otherwise null
   * @throws Exception if the look-up request to FOLIO failed
   */
  public String getExpenseClassId(String expenseClassCode) throws Exception {
    if (expenseClassCode == null) {
      return null;
    } else {
      if (!expenseClassCodeToUuid.containsKey(expenseClassCode)) {
        String expenseClassEndpoint = "finance/expense-classes?query=code='" + expenseClassCode + "'";
        String expenseClassResponse = Folio.callApiGet(expenseClassEndpoint);
        JSONArray expenseClasses = new JSONObject(expenseClassResponse).getJSONArray(EXPENSE_CLASSES);
        expenseClassCodeToUuid.put(expenseClassCode, getIdOfFirstRecordIfAny(expenseClasses));
      }
      return expenseClassCodeToUuid.get(expenseClassCode);
    }
  }

  public String getRefUuidByName (String referenceName) throws Exception {
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
    if ( referenceDataByName == null) {
      referenceDataByName = Folio.lookupReferenceValuesInFolio(referenceTables);
    }
  }


  private String getIdOfFirstRecordIfAny (JSONArray array) {
    if (array == null || array.isEmpty()) {
      return null;
    } else {
      return (String) array.getJSONObject(0).get("id");
    }
  }

}
