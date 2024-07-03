package org.olf.folio.order.entities.orders;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.json.JSONArray;
import org.json.JSONObject;
import org.olf.folio.order.entities.FolioEntity;
import org.olf.folio.order.mapping.MarcToFolio;
import org.olf.folio.order.folioapis.FolioData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@CanIgnoreReturnValue
public class CompositePurchaseOrder extends FolioEntity {
  // Constant values
  public static final String V_ONE_TIME = "One-Time";
  public static final String V_OPEN = "Open";
  public static final boolean V_RE_ENCUMBER = true;
  public static final boolean V_APPROVED = true;
  // Property names
  public static final String P_PO_NUMBER = "poNumber";
  public static final String P_VENDOR = "vendor";
  public static final String P_ORDER_TYPE = "orderType";
  public static final String P_RE_ENCUMBER = "reEncumber";
  public static final String P_ID = "id";
  public static final String P_APPROVED = "approved";
  public static final String P_WORKFLOW_STATUS = "workflowStatus";
  public static final String P_ASSIGNED_TO = "assignedTo";
  public static final String P_BILL_TO = "billTo";
  public static final String P_COMPOSITE_PO_LINES = "compositePoLines";

  public static CompositePurchaseOrder fromJson(JSONObject object) {
    CompositePurchaseOrder po = new CompositePurchaseOrder();
    po.json = object;
    return po;
  }

  public static CompositePurchaseOrder fromMarcRecord(MarcToFolio mappedMarc)
          throws Exception {

    UUID orderId = UUID.randomUUID();
    return new CompositePurchaseOrder()
                    .putPoNumber(FolioData.getNextPoNumberFromOrders())
                    .putVendor(mappedMarc.vendorUuid())
                    .putOrderType(V_ONE_TIME)
                    .putReEncumber(V_RE_ENCUMBER)
                    .putId(orderId)
                    .putApproved(V_APPROVED)
                    .putWorkflowStatus(V_OPEN)
                    .putAssignedTo(mappedMarc.assignedTo())
                    .putBillToIfPresent(mappedMarc.billToUuid())
                    .putCompositePoLines(
                            new JSONArray().put(PoLine.fromMarcRecord(orderId, mappedMarc).asJson())
                    );
  }

  public static CompositePurchaseOrder initiateEmptyOrder () throws Exception {
    UUID orderId = UUID.randomUUID();
    return new CompositePurchaseOrder()
            .putPoNumber(FolioData.getNextPoNumberFromOrders())
            .putOrderType(V_ONE_TIME)
            .putReEncumber(V_RE_ENCUMBER)
            .putId(orderId)
            .putApproved(V_APPROVED)
            .putWorkflowStatus(V_OPEN)
            .putCompositePoLines(new JSONArray());
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
  public CompositePurchaseOrder putAssignedTo(String username) {
    return (CompositePurchaseOrder) putString(P_ASSIGNED_TO, username);
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

  public CompositePurchaseOrder addPoLine (PoLine poLine) {
    if (!json.has(P_COMPOSITE_PO_LINES)) {
      json.put(P_COMPOSITE_PO_LINES, new JSONArray());
    }
    json.getJSONArray(P_COMPOSITE_PO_LINES).put(poLine.asJson());
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

  public String getFirstPoLineId () {
    if (hasPoLines()) {
      return getFirstPoLine().getId();
    } else {
      return null;
    }
  }

  public PoLine getFirstPoLine () {
    if (hasPoLines()) {
      return PoLine.fromJson(getCompositePoLinesJsonArray().getJSONObject(0));
    } else {
      return null;
    }
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

}
