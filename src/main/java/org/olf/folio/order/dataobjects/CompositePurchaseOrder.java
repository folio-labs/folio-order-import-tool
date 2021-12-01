package org.olf.folio.order.dataobjects;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.olf.folio.order.MarcRecordMapping;
import org.olf.folio.order.storage.FolioData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CompositePurchaseOrder extends JsonDataObject {
  public static final String P_PO_NUMBER = "poNumber";
  public static final String P_VENDOR = "vendor";
  public static final String P_ORDER_TYPE = "orderType";
  public static final String V_ONE_TIME = "One-Time";
  public static final String P_RE_ENCUMBER = "reEncumber";
  public static final String P_ID = "id";
  public static final String P_APPROVED = "approved";
  public static final String P_WORKFLOW_STATUS = "workflowStatus";
  public static final String V_OPEN = "Open";
  public static final String P_BILL_TO = "billTo";
  public static final String P_COMPOSITE_PO_LINES = "compositePoLines";

  private static final Logger logger = Logger.getLogger(CompositePurchaseOrder.class);

  public static CompositePurchaseOrder fromJson(JSONObject object) {
    CompositePurchaseOrder po = new CompositePurchaseOrder();
    po.json = object;
    return po;
  }

  public static CompositePurchaseOrder fromMarcRecord(MarcRecordMapping mappedMarc)
          throws Exception {

    UUID orderId = UUID.randomUUID();
    return new CompositePurchaseOrder()
                    .putPoNumber(FolioData.getNextPoNumberFromOrders())
                    .putVendor(mappedMarc.vendorUuid())
                    .putOrderType(V_ONE_TIME)
                    .putReEncumber(true)
                    .putId(orderId)
                    .putApproved(true)
                    .putWorkflowStatus(V_OPEN)
                    .putBillToIfPresent(mappedMarc.billToUuid())
                    .putCompositePoLines(
                            new JSONArray().put(PoLine.fromMarcRecord(orderId, mappedMarc).asJson())
                    );
  }

  public CompositePurchaseOrder putId(UUID id) {
    return (CompositePurchaseOrder) putString(P_ID, id.toString());
  }
  public CompositePurchaseOrder putPoNumber(String poNumber) {
    return (CompositePurchaseOrder) putString(P_PO_NUMBER, poNumber);
  }
  public CompositePurchaseOrder putVendor (String vendorUuid) {
    return (CompositePurchaseOrder) putString(P_VENDOR, vendorUuid);
  }
  public CompositePurchaseOrder putOrderType (String orderType) {
    return (CompositePurchaseOrder) putString(P_ORDER_TYPE, orderType);
  }
  public CompositePurchaseOrder putReEncumber(boolean value) {
    return (CompositePurchaseOrder)  putBoolean(P_RE_ENCUMBER, value);
  }
  public CompositePurchaseOrder putApproved(boolean value) {
    return (CompositePurchaseOrder)  putBoolean(P_APPROVED, value);
  }
  public CompositePurchaseOrder putWorkflowStatus(String workflowStatus) {
    return (CompositePurchaseOrder) putString(P_WORKFLOW_STATUS, workflowStatus);
  }
  public CompositePurchaseOrder putBillTo(String billTo) {
    return (CompositePurchaseOrder) putString(P_BILL_TO, billTo);
  }
  public CompositePurchaseOrder putBillToIfPresent(String billTo) {
    return present(billTo) ? putBillTo(billTo) : this;
  }

  public CompositePurchaseOrder putCompositePoLines (JSONArray poLines) {
    return (CompositePurchaseOrder) putArray(P_COMPOSITE_PO_LINES, poLines);
  }

  public CompositePurchaseOrder putCompositePoLines (List<PoLine> poLines) {
    if (getCompositePoLinesJsonArray() == null) {
      putCompositePoLines(new JSONArray());
    }
    for (PoLine line : poLines) {
      getCompositePoLinesJsonArray().put(line.asJson());
    }
    return this;
  }

  public String getPoNumber() {
    return getString(P_PO_NUMBER);
  }
  public String getInstanceId () {
    if (hasPoLines()) {
      return getCompositePoLines().get(0).getInstanceId();
    } else {
      return null;
    }
  }

  public String getVendor() {
    return getString(P_VENDOR);
  }
  public String getOrderType() {
    return getString(P_ORDER_TYPE);
  }
  public boolean getReEncumber() {
    return getBoolean(P_RE_ENCUMBER);
  }
  public boolean getApproved() {
    return getBoolean(P_APPROVED);
  }
  public String getBillTo() {
    return getString(P_BILL_TO);
  }
  public JSONArray getCompositePoLinesJsonArray() {
    return getArray(P_COMPOSITE_PO_LINES);
  }

  public boolean hasPoLines () {
    return ! getArray(P_COMPOSITE_PO_LINES).isEmpty();
  }

  public List<PoLine> getCompositePoLines () {
    List<PoLine> poLines = new ArrayList<>();
    for (Object pol : getCompositePoLinesJsonArray()) {
      JSONObject poLine = (JSONObject) pol;
      poLines.add(PoLine.fromJson(poLine));
    }
    return poLines;
  }

  public CompositePurchaseOrder setMaterialTypeOnPoLines (String materialType) {
    for (PoLine line : getCompositePoLines()) {
      line.getPhysical().putMaterialType(materialType);
    }
    return this;
  }

  public CompositePurchaseOrder setLocationIdOnPoLines (String locationId) {
    for (PoLine line : getCompositePoLines()) {
      for (PoLineLocation location : line.getLocations()) {
        location.putLocationId(locationId);
      }
    }
    return this;
  }

}
