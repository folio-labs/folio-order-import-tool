package org.olf.folio.order.dataobjects;

import org.json.JSONArray;
import org.olf.folio.order.MarcRecordMapping;

public class OrderLineDetails extends JsonDataObject{
  public static final String P_RECEIVING_NOTE = "receivingNote";
  public static final String P_PRODUCT_IDS = "productIds";

  public static OrderLineDetails fromMarcRecord(MarcRecordMapping mappedMarc) {
    return new OrderLineDetails()
            .putReceivingNoteIfPresent(mappedMarc.receivingNote())
            .putProductIdsIfPresent(mappedMarc.getProductIdentifiers());
  }

  public OrderLineDetails putReceivingNote (String receivingNote) {
    return (OrderLineDetails) putString(P_RECEIVING_NOTE, receivingNote);
  }

  public OrderLineDetails putReceivingNoteIfPresent (String receivingNote) {
    return present(receivingNote) ? putReceivingNote(receivingNote) : this;
  }
  public OrderLineDetails putProductIds (JSONArray productIds) {
    return (OrderLineDetails)  putArray(P_PRODUCT_IDS, productIds);
  }
  public OrderLineDetails putProductIdsIfPresent (JSONArray productIds) {
    return present(productIds) ? putProductIds(productIds) : this;
  }

}
