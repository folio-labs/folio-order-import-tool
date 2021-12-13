package org.olf.folio.order.entities;

import org.json.JSONObject;

public class ProductIdentifier extends FolioEntity {
  public static final String P_PRODUCT_ID_TYPE = "productIdType";
  public static final String P_PRODUCT_ID = "productId";

  public static ProductIdentifier fromJson (JSONObject piJson) {
    ProductIdentifier pi = new ProductIdentifier();
    pi.json = piJson;
    return pi;
  }

  public ProductIdentifier putProductIdType (String productIdType) {
    putString(P_PRODUCT_ID_TYPE, productIdType);
    return this;
  }

  public ProductIdentifier putProductId (String productId) {
    putString(P_PRODUCT_ID, productId);
    return this;
  }


}
