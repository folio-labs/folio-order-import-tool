package org.olf.folio.order.entities.orders;

import org.json.JSONObject;
import org.olf.folio.order.entities.FolioEntity;
import org.olf.folio.order.mapping.MarcToFolio;

public class PoLineLocation extends FolioEntity {

  public static final String P_QUANTITY_PHYSICAL = "quantityPhysical";
  public static final String P_QUANTITY_ELECTRONIC = "quantityElectronic";
  public static final String P_LOCATION_ID = "locationId";
  
  public static PoLineLocation fromJson(JSONObject locationJson) {
    PoLineLocation loc = new PoLineLocation();
    loc.json = locationJson;
    return loc;
  }

  public static PoLineLocation fromMarcRecord(MarcToFolio mappedMarc) throws Exception {
    if (mappedMarc.electronic()) {
      return new PoLineLocation()
              .putQuantityElectronic(mappedMarc.quantity())
              .putLocationId(mappedMarc.locationId());
    } else {
      return new PoLineLocation()
              .putQuantityPhysical(mappedMarc.quantity())
              .putLocationId(mappedMarc.locationId());
    }
  }

  public PoLineLocation putQuantityPhysical(int i) {
    return (PoLineLocation) putInteger(P_QUANTITY_PHYSICAL, i);
  }

  public PoLineLocation putQuantityElectronic(int i) {
    return (PoLineLocation) putInteger(P_QUANTITY_ELECTRONIC, i);
  }

  public PoLineLocation putLocationId (String locationId) {
    return (PoLineLocation) putString(P_LOCATION_ID, locationId);
  }

}
