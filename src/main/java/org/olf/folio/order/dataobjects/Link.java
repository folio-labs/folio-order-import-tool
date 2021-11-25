package org.olf.folio.order.dataobjects;

import org.json.JSONObject;

public class Link extends JsonDataObject {

  public static final String P_TYPE = "type";
  public static final String V_PO_LINE = "poLine";
  public static final String P_ID = "id";

  public Link putType(String type) {
    return (Link) putString(P_TYPE, type);
  }

  public Link putId(String orderLineUuid) {
    return (Link) putString(P_ID, orderLineUuid);
  }
}
