package org.olf.folio.order.dataobjects;

import org.json.JSONArray;
import org.json.JSONObject;

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
}
