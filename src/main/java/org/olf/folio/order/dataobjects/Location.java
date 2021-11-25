package org.olf.folio.order.dataobjects;

import org.json.JSONObject;
import org.olf.folio.order.MarcRecordMapping;

public class Location extends JsonDataObject {

  public static final String P_QUANTITY_PHYSICAL = "quantityPhysical";
  public static final String P_QUANTITY_ELECTRONIC = "quantityElectronic";
  public static final String P_LOCATION_ID = "locationId";

  public Location (JSONObject locationJson) {
    json = locationJson;
  }

  public Location () {

  }

  public Location putQuantityPhysical(int i) {
    return (Location) putInteger(P_QUANTITY_PHYSICAL, i);
  }

  public Location putQuantityElectronic(int i) {
    return (Location) putInteger(P_QUANTITY_ELECTRONIC, i);
  }

  public Location putLocationId (String locationId) {
    return (Location) putString(P_LOCATION_ID, locationId);
  }

  public static Location createLocation (MarcRecordMapping mappedMarc) {
    if (mappedMarc.electronic()) {
      return new Location().putQuantityElectronic(1);
    } else {
      return new Location().putQuantityPhysical(1);
    }
  }
}
