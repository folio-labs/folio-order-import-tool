package org.olf.folio.order.dataobjects;

import org.json.JSONObject;
import org.olf.folio.order.MarcRecordMapping;

public class PoLineLocation extends JsonDataObject {

  public static final String P_QUANTITY_PHYSICAL = "quantityPhysical";
  public static final String P_QUANTITY_ELECTRONIC = "quantityElectronic";
  public static final String P_LOCATION_ID = "locationId";
  
  public static PoLineLocation fromJson(JSONObject locationJson) {
    PoLineLocation loc = new PoLineLocation();
    loc.json = locationJson;
    return loc;
  }

  public static PoLineLocation fromMarcRecord(MarcRecordMapping mappedMarc) {
    if (mappedMarc.electronic()) {
      return new PoLineLocation().putQuantityElectronic(1);
    } else {
      return new PoLineLocation().putQuantityPhysical(1);
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
