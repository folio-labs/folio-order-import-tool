package org.olf.folio.order.entities;

import org.json.JSONArray;
import org.json.JSONObject;
import org.olf.folio.order.Constants;

public abstract class FolioEntity {

  public static String P_ID = "id";

  JSONObject json = new JSONObject();

  protected FolioEntity putString(String key, String value) {
    json.put(key, value);
    return this;
  }
  protected String getString(String key) {
    return json.getString(key);
  }
  protected FolioEntity putBoolean(String key, boolean value) {
    json.put(key, value);
    return this;
  }
  protected boolean getBoolean(String key) {
    return json.getBoolean(key);
  }
  protected FolioEntity putObject (String key, JSONObject value) {
    json.put(key, value);
    return this;
  }

  protected FolioEntity putInteger (String key, int value) {
    json.put(key, value);
    return this;
  }
  protected int getInteger(String key) {
    return json.getInt(key);
  }
  protected FolioEntity putArray (String key, JSONArray values) {
    json.put(key, values);
    return this;
  }

  protected JSONObject getJSONObject (String key) {
    return json.getJSONObject(key);
  }
  protected JSONArray getArray (String key) {
    return (json.has(key) ? json.getJSONArray(key) : null);
  }

  public JSONObject asJson() {
    return json;
  }

  public String getId() {
    return getString(P_ID);
  }

  protected static boolean present(String val) {
    return (val != null && !val.isEmpty());
  }
  protected boolean present(JSONArray array) {
    return (array != null && !array.isEmpty());
  }
  protected boolean present(JSONObject object) {
    return (object != null && !object.isEmpty());
  }
  protected static boolean isUUID(String str) {
    return ( str != null && Constants.UUID_PATTERN.matcher( str ).matches() );
  }

}
