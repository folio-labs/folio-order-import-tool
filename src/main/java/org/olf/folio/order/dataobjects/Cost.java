package org.olf.folio.order.dataobjects;

import org.olf.folio.order.mapping.BaseMapping;

public class Cost extends JsonDataObject {
  public static final String P_QUANTITY_PHYSICAL = "quantityPhysical";
  public static final String P_LIST_UNIT_PRICE = "listUnitPrice";
  public static final String P_QUANTITY_ELECTRONIC = "quantityElectronic";
  public static final String P_LIST_UNIT_PRICE_ELECTRONIC = "listUnitPriceElectronic";
  public static final String P_CURRENCY = "currency";

  public static Cost fromMarcRecord(BaseMapping mappedMarc) {
    Cost cost = new Cost();
    if (mappedMarc.electronic()) {
      cost.putQuantityElectronic(1);
      cost.putListUnitPriceElectronic(mappedMarc.price());
    } else {
      cost.putQuantityPhysical(1);
      cost.putListUnitPrice(mappedMarc.price());
    }
    cost.putCurrency(mappedMarc.currency());
    return cost;
  }

  public Cost putQuantityElectronic (int i) {
    return (Cost) putInteger(P_QUANTITY_ELECTRONIC, i);
  }
  public Cost putListUnitPriceElectronic (String price) {
    return (Cost) putString(P_LIST_UNIT_PRICE_ELECTRONIC, price);
  }
  public Cost putListUnitPrice (String price) {
    return (Cost) putString(P_LIST_UNIT_PRICE, price);
  }
  public Cost putQuantityPhysical (int i) {
    return (Cost) putInteger (P_QUANTITY_PHYSICAL, i);
  }
  public Cost putCurrency (String currency) {
    return (Cost) putString(P_CURRENCY, currency);
  }


}
