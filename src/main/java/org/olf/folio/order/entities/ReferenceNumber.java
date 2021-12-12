package org.olf.folio.order.entities;

import org.olf.folio.order.mapping.BaseMapping;

public class ReferenceNumber extends FolioEntity {
  public static final String P_REF_NUMBER = "refNumber";
  public static final String P_REF_NUMBER_TYPE = "refNumberType";
  public static final String V_VENDOR_INTERNAL_NUMBER = "Vendor internal number";

  public static ReferenceNumber fromMarcRecord(BaseMapping mappedMarc) {
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
