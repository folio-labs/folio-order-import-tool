package org.olf.folio.order.entities;

import org.json.JSONArray;
import org.json.JSONObject;

public class Instance extends FolioEntity {
  public static final String P_ID = "id";
  public static final String P_HRID = "hrid";
  public static final String P_TITLE = "title";
  public static final String P_SOURCE = "source";
  public static final String V_MARC = "MARC";
  public static final String V_FOLIO = "FOLIO";
  public static final String P_INSTANCE_TYPE_ID = "instanceTypeId";
  public static final String P_IDENTIFIERS = "identifiers";
  public static final String P_CONTRIBUTORS = "contributors";
  public static final String P_DISCOVERY_SUPPRESS = "discoverySuppress";
  public static final String P_ELECTRONIC_ACCESS = "electronicAccess";
  public static final String P_NATURE_OF_CONTENT_TERM_IDS = "natureOfContentTermIds";
  public static final String P_PRECEDING_TITLES = "precedingTitles";
  public static final String P_SUCCEEDING_TITLES = "succeedingTitles";

  public static Instance fromJson(JSONObject instanceJson) {
    Instance instance = new Instance();
    instance.json = instanceJson;
    return instance;
  }

  public String getHrid () {
    return getString(P_HRID);
  }
  public Instance putTitle(String title) {
    return (Instance) putString(P_TITLE, title);
  }
  public Instance putSource(String source) {
    return (Instance) putString(P_SOURCE, source);
  }
  public Instance putInstanceTypeId(String instanceTypeId) {
    return (Instance) putString(P_INSTANCE_TYPE_ID, instanceTypeId);
  }
  public Instance putIdentifiers(JSONArray identifiers) {
    return (Instance) putArray(P_IDENTIFIERS, identifiers);
  }
  public Instance putContributors(JSONArray contributors) {
    return (Instance)  putArray(P_CONTRIBUTORS, contributors);
  }
  public Instance putDiscoverySuppress (boolean suppress) {
    return (Instance) putBoolean(P_DISCOVERY_SUPPRESS, suppress);
  }
  public Instance putElectronicAccess (JSONArray electronicAccess) {
    return (Instance) putArray(P_ELECTRONIC_ACCESS, electronicAccess);
  }
  public Instance putNatureOfContentTermIds (JSONArray natureOfContentTemIds) {
    return (Instance) putArray(P_NATURE_OF_CONTENT_TERM_IDS, natureOfContentTemIds);
  }
  public Instance putPrecedingTitles (JSONArray precedingTitles) {
    return (Instance) putArray(P_PRECEDING_TITLES, precedingTitles);
  }
  public Instance putSucceedingTitles (JSONArray succeedingTitles) {
    return (Instance) putArray(P_SUCCEEDING_TITLES, succeedingTitles);
  }
  public String getSource () {
    return getString(P_SOURCE);
  }
}
