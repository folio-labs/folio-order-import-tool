package org.olf.folio.order.entities.orders;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.json.JSONObject;
import org.olf.folio.order.entities.FolioEntity;

@CanIgnoreReturnValue
public class PoLineLocation extends FolioEntity {

  public static final String P_QUANTITY_PHYSICAL = "quantityPhysical";
  public static final String P_QUANTITY_ELECTRONIC = "quantityElectronic";
  public static final String P_LOCATION_ID = "locationId";
  
  public static PoLineLocation fromJson(JSONObject locationJson) {
    PoLineLocation loc = new PoLineLocation();
    loc.json = locationJson;
    return loc;
  }

  public PoLineLocation putQuantityPhysical(int qp) {
    return (PoLineLocation) putInteger(P_QUANTITY_PHYSICAL, qp);
  }

  public PoLineLocation putQuantityElectronic(int qe) {
    return (PoLineLocation) putInteger(P_QUANTITY_ELECTRONIC, qe);
  }

  public PoLineLocation putLocationId (String locationId) {
    return (PoLineLocation) putString(P_LOCATION_ID, locationId);
  }

}
