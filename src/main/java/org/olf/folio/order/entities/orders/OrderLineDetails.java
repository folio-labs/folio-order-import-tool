package org.olf.folio.order.entities.orders;

import org.json.JSONArray;
import org.json.JSONObject;
import org.olf.folio.order.entities.FolioEntity;
import org.olf.folio.order.mapping.MarcToFolio;

import java.util.ArrayList;
import java.util.List;

public class OrderLineDetails extends FolioEntity {
  public static final String P_RECEIVING_NOTE = "receivingNote";
  public static final String P_PRODUCT_IDS = "productIds";

  public static OrderLineDetails fromMarcRecord(MarcToFolio mappedMarc) {
    return new OrderLineDetails()
            .putReceivingNoteIfPresent(mappedMarc.receivingNote())
            .putProductIdsIfPresent(mappedMarc.productIdentifiers());
  }

  public static OrderLineDetails fromJson (JSONObject json) {
    OrderLineDetails details = new OrderLineDetails();
    details.json = json;
    return details;
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
