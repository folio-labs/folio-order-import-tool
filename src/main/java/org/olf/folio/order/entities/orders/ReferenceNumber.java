package org.olf.folio.order.entities.orders;

import org.olf.folio.order.entities.FolioEntity;
import org.olf.folio.order.mapping.MarcToFolio;

public class ReferenceNumber extends FolioEntity {
  // Constant value
  public static final String V_VENDOR_INTERNAL_NUMBER = "Vendor internal number";
  // Property names
  public static final String P_REF_NUMBER = "refNumber";
  public static final String P_REF_NUMBER_TYPE = "refNumberType";

  public static ReferenceNumber fromMarcRecord(MarcToFolio mappedMarc) {
    return new ReferenceNumber()
            .putRefNumber(mappedMarc.vendorItemId())
            .putRefNumberType(mappedMarc.hasRefNumberType()
                    ? mappedMarc.refNumberType()
                    : ReferenceNumber.V_VENDOR_INTERNAL_NUMBER);
  }

  public ReferenceNumber putRefNumber (String refNumber) {
    return (ReferenceNumber) putString(P_REF_NUMBER, refNumber);
  }
  public ReferenceNumber putRefNumberType (String refNumberType) {
    return (ReferenceNumber) putString(P_REF_NUMBER_TYPE, refNumberType);
  }
}
