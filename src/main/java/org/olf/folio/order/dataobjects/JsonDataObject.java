package org.olf.folio.order.dataobjects;

import org.json.JSONArray;
import org.json.JSONObject;
import org.olf.folio.order.Constants;

public abstract class JsonDataObject {

  JSONObject json = new JSONObject();

  protected JsonDataObject putString(String key, String value) {
    json.put(key, value);
    return this;
  }
  protected String getString(String key) {
    return json.getString(key);
  }
  protected JsonDataObject putBoolean(String key, boolean value) {
    json.put(key, value);
    return this;
  }
  protected boolean getBoolean(String key) {
    return json.getBoolean(key);
  }
  protected JsonDataObject putObject (String key, JSONObject value) {
    json.put(key, value);
    return this;
  }
  protected JSONObject getObject (String key) {
    return json.getJSONObject(key);
  }

  protected JsonDataObject putInteger (String key, int value) {
    json.put(key, value);
    return this;
  }
  protected int getInteger(String key) {
    return json.getInt(key);
  }
  protected JsonDataObject putArray (String key, JSONArray values) {
    json.put(key, values);
    return this;
  }

  protected JSONArray getArray (String key) {
    return json.getJSONArray(key);
  }

  public JSONObject asJson() {
    return json;
  }

  protected boolean present(String val) {
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
