package org.olf.folio.order.dataobjects;

import org.json.JSONArray;
import org.json.JSONObject;

public class Item extends JsonDataObject {
  public static final String P_NOTES = "notes";

  public static Item fromJson (JSONObject itemJson) {
    Item item = new Item();
    item.json = itemJson;
    return item;
  }

  public Item addNote (JSONObject note) {
    if (getNotes() == null) {
      json.put(P_NOTES, new JSONArray());
    }
    getNotes().put(note);
    return this;
  }

  public Item addBookplateNote (BookplateNote note) {
    return addNote(note.asJson());
  }

  public JSONArray getNotes () {
    return json.getJSONArray(P_NOTES);
  }

}
