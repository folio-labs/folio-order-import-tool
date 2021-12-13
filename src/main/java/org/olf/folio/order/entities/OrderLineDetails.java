package org.olf.folio.order.entities;

import org.json.JSONArray;
import org.json.JSONObject;
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

  public boolean hasProductIdentifiers () {
    return json.has(P_PRODUCT_IDS);
  }

  public List<ProductIdentifier> getProductIdentifiers () {
    List<ProductIdentifier> list = new ArrayList<>();
    JSONArray ids = getArray(P_PRODUCT_IDS);
    if (ids != null) {
      for (Object o : ids) {
        list.add(ProductIdentifier.fromJson((JSONObject) o));
      }
    }
    return list;
  }
  public JSONArray getProductIdentifiersAsJson () {
    return getArray(P_PRODUCT_IDS);
  }

}
