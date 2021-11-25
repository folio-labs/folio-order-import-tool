package org.olf.folio.order.dataobjects;

import org.json.JSONArray;
import org.olf.folio.order.MarcRecordMapping;

public class OrderLineDetails extends JsonDataObject{
  public static final String P_RECEIVING_NOTE = "receivingNote";
  public static final String P_PRODUCT_IDS = "productIds";

  public OrderLineDetails putReceivingNote (String receivingNote) {
    return (OrderLineDetails) putString(P_RECEIVING_NOTE, receivingNote);
  }

  public OrderLineDetails putProductIds (JSONArray productIds) {
    return (OrderLineDetails)  putArray(P_PRODUCT_IDS, productIds);
  }

  public static OrderLineDetails createOrderLineDetails (MarcRecordMapping mappedMarc) {
    OrderLineDetails details = new OrderLineDetails();
    if (mappedMarc.hasReceivingNote()) {
      details.putReceivingNote(mappedMarc.receivingNote());
    }
    if (! mappedMarc.getProductIdentifiers().isEmpty()) {
      details.putProductIds(mappedMarc.getProductIdentifiers());
    }
    return details;
  }
}
