package org.olf.folio.order.dataobjects;

import org.json.JSONArray;
import org.olf.folio.order.MarcRecordMapping;

public class VendorDetail extends JsonDataObject {

  public static final String P_INSTRUCTIONS = "instructions";
  public static final String P_VENDOR_ACCOUNT = "vendorAccount";
  public static final String P_REFERENCE_NUMBERS = "referenceNumbers";

  public VendorDetail putInstructions(String instructions) {
    return (VendorDetail) putString(P_INSTRUCTIONS, instructions);
  }
  public VendorDetail putVendorAccount(String vendorAccount) {
    return (VendorDetail) putString(P_VENDOR_ACCOUNT, vendorAccount);
  }
  public VendorDetail putReferenceNumbers(JSONArray referenceNumbers) {
    return (VendorDetail) putArray(P_REFERENCE_NUMBERS, referenceNumbers);
  }
  public static VendorDetail createVendorDetail(MarcRecordMapping mappedMarc) {
    return new VendorDetail()
            .putInstructions("")
            .putVendorAccount(
                    (mappedMarc.hasVendorAccount() ? mappedMarc.vendorAccount(): ""))
            .putReferenceNumbers(
                    new JSONArray().put(ReferenceNumber.createReferenceNumber(mappedMarc).asJson()));
  }
}
