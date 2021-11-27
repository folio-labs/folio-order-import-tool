package org.olf.folio.order.dataobjects;

import org.json.JSONObject;

public class Item extends JsonDataObject {
  public static Item fromJson (JSONObject itemJson) {
    Item item = new Item();
    item.json = itemJson;
    return item;
  }
}
