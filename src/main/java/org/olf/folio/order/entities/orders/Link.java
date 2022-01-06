package org.olf.folio.order.entities.orders;

import org.olf.folio.order.entities.FolioEntity;

public class Link extends FolioEntity {
  // Constant value
  public static final String V_PO_LINE = "poLine";
  // Property names
  public static final String P_TYPE = "type";
  public static final String P_ID = "id";

  public Link putType(String type) {
    return (Link) putString(P_TYPE, type);
  }
  public Link putId(String orderLineUuid) {
    return (Link) putString(P_ID, orderLineUuid);
  }
}
