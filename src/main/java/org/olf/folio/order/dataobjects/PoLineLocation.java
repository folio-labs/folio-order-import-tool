package org.olf.folio.order.dataobjects;

import org.json.JSONObject;
import org.olf.folio.order.Config;
import org.olf.folio.order.MarcRecordMapping;
import org.olf.folio.order.storage.FolioData;

public class PoLineLocation extends JsonDataObject {

  public static final String P_QUANTITY_PHYSICAL = "quantityPhysical";
  public static final String P_QUANTITY_ELECTRONIC = "quantityElectronic";
  public static final String P_LOCATION_ID = "locationId";
  
  public static PoLineLocation fromJson(JSONObject locationJson) {
    PoLineLocation loc = new PoLineLocation();
    loc.json = locationJson;
    return loc;
  }

  public static PoLineLocation fromMarcRecord(MarcRecordMapping mappedMarc) throws Exception {
    if (mappedMarc.electronic()) {
      return new PoLineLocation()
              .putQuantityElectronic(1)
              .putLocationId(getLocationId(mappedMarc));
    } else {
      return new PoLineLocation()
              .putQuantityPhysical(1)
              .putLocationId(getLocationId(mappedMarc));
    }
  }

  /**
   * Gets a location name from configuration based on whether resource is electronic or not
   * and whether this is an import with an invoice.
   * @param mappedMarc the MARC record, for determining if an invoice is present
   * @return  the name of the location from the startup configuration
   */
  private static String getLocationName (MarcRecordMapping mappedMarc)  {
    return (mappedMarc.electronic() ?
            (Config.importInvoice && mappedMarc.hasInvoice()) ?
                    Config.permELocationWithInvoiceImport : Config.permELocationName
            :
            (Config.importInvoice && mappedMarc.hasInvoice()) ?
                    Config.permLocationWithInvoiceImport : Config.permLocationName
    );
  }

  private static String getLocationId (MarcRecordMapping mappedMarc) throws Exception {
    return FolioData.getLocationIdByName(getLocationName(mappedMarc));
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
