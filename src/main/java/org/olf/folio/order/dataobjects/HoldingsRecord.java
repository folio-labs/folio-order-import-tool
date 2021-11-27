package org.olf.folio.order.dataobjects;

import org.json.JSONObject;

public class HoldingsRecord extends JsonDataObject {

  public static HoldingsRecord fromJson(JSONObject holdingsJson) {
    HoldingsRecord holdingsRecord = new HoldingsRecord();
    holdingsRecord.json = holdingsJson;
    return holdingsRecord;
  }
}
