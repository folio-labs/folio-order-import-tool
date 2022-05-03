package org.olf.folio.order.entities.inventory;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.json.JSONArray;
import org.json.JSONObject;
import org.olf.folio.order.entities.FolioEntity;

@CanIgnoreReturnValue
public class Instance extends FolioEntity {
  // Constant values
  public static final String V_FOLIO = "FOLIO";
  public static final String V_MARC = "MARC";
  public static final boolean DISCOVERY_SUPPRESS = false;

  public static final String P_ID = "id";
  public static final String P_HRID = "hrid";
  public static final String P_TITLE = "title";
  public static final String P_INDEX_TITLE = "indexTitle";
  public static final String P_SOURCE = "source";
  public static final String P_INSTANCE_TYPE_ID = "instanceTypeId";
  public static final String P_INSTANCE_FORMAT_IDS = "instanceFormatIds";
  public static final String P_LANGUAGES = "languages";
  public static final String P_EDITIONS = "editions";
  public static final String P_SERIES = "series";
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
  public Instance putIndexTitle(String indexTitle) {
    return (Instance) putString(P_INDEX_TITLE, indexTitle);
  }
  public Instance putSource(String source) {
    return (Instance) putString(P_SOURCE, source);
  }
  public Instance putInstanceTypeId(String instanceTypeId) {
    return (Instance) putString(P_INSTANCE_TYPE_ID, instanceTypeId);
  }
  public Instance putInstanceFormatIds(JSONArray instanceFormatIds) {
    return (Instance) putArray(P_INSTANCE_FORMAT_IDS, instanceFormatIds);
  }
  public Instance putLanguages(JSONArray languages) {
    return (Instance) putArray(P_LANGUAGES, languages);
  }
  public Instance putEdition (String edition) {
    JSONArray editions = new JSONArray();
    if (edition != null && ! edition.isEmpty()) {
      editions.put(edition);
    }
    return (Instance) putArray(P_EDITIONS, editions);
  }
  public Instance putSeries (JSONArray series) {
    return (Instance) putArray(P_SERIES,series);
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
  public Instance putAlternativeTitle (String alternativeTitle) {
    if (alternativeTitle != null && !alternativeTitle.isEmpty()) {

    }
    return this;
  }
  public String getSource () {
    return getString(P_SOURCE);
  }
}
