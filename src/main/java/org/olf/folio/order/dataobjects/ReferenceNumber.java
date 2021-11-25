package org.olf.folio.order.dataobjects;

import org.json.JSONObject;
import org.olf.folio.order.MarcRecordMapping;

public class ReferenceNumber extends JsonDataObject {
  public static final String P_REF_NUMBER = "refNumber";
  public static final String P_REF_NUMBER_TYPE = "refNumberType";
  public static final String V_VENDOR_INTERNAL_NUMBER = "Vendor internal number";

  public ReferenceNumber putRefNumber (String refNumber) {
    return (ReferenceNumber) putString(P_REF_NUMBER, refNumber);
  }
  public ReferenceNumber putRefNumberType (String refNumberType) {
    return (ReferenceNumber) putString(P_REF_NUMBER_TYPE, refNumberType);
  }
  public static ReferenceNumber createReferenceNumber (MarcRecordMapping mappedMarc) {
    return new ReferenceNumber()
            .putRefNumber(mappedMarc.vendorItemId())
            .putRefNumberType(mappedMarc.hasRefNumberType()
                    ? mappedMarc.refNumberType()
                    : ReferenceNumber.V_VENDOR_INTERNAL_NUMBER);
  }
}
