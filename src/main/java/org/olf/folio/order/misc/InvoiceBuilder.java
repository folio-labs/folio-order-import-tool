package org.olf.folio.order.misc;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.olf.folio.order.Config;
import org.olf.folio.order.mapping.Constants;
import org.olf.folio.order.mapping.MarcToFolio;
import org.olf.folio.order.mapping.MarcMapLambda;
import org.olf.folio.order.folioapis.FolioData;

import java.util.UUID;

/**
 * Not actively used or actively tested as it is not sufficiently covering the requirements
 * for invoice imports.
 */
public class InvoiceBuilder {

  // Hard-coded values
  final static String BATCH_GROUP_ID = "2a2cb998-1437-41d1-88ad-01930aaeadd5"; // ='FOLIO', System default

  final static String SOURCE = "API";
  final static int INVOICE_LINE_QUANTITY = 1;

  // tbd
  final static String STATUS = "Open";
  final static String INVOICE_LINE_STATUS = "Open";
  final static boolean RELEASE_ENCUMBRANCE = true;

  public static JSONObject createInvoiceJson(String poNumber, MarcToFolio marc) throws Exception {
    JSONObject invoice = new JSONObject();
    invoice.put("id", UUID.randomUUID());
    invoice.put("poNumbers", (new JSONArray()).put(poNumber)); // optional
    invoice.put("batchGroupId", BATCH_GROUP_ID); // required
    invoice.put("currency", marc.currency()); // required
    invoice.put("invoiceDate", marc.invoiceDate()); // required
    invoice.put("paymentMethod", Config.paymentMethod); // required
    invoice.put("status", STATUS); // required
    invoice.put("source", SOURCE); // required
    invoice.put("vendorInvoiceNo", marc.vendorInvoiceNo()); // required
    invoice.put("vendorId", marc.vendorUuid()); // required
    return invoice;
  }

  public static JSONObject createInvoiceLineJson(UUID orderLineUUID, MarcToFolio marc, JSONObject invoice) {
    JSONObject invoiceLine = new JSONObject();
    invoiceLine.put("description", marc.title());  // required
    invoiceLine.put("invoiceId", UUID.fromString(invoice.get("id").toString())); // required
    invoiceLine.put("invoiceLineStatus", INVOICE_LINE_STATUS); // required
    invoiceLine.put("subTotal", marc.subTotal());  // required
    invoiceLine.put("quantity", INVOICE_LINE_QUANTITY); // required
    invoiceLine.put("releaseEncumbrance", RELEASE_ENCUMBRANCE);  // required
    invoiceLine.put("poLineId", orderLineUUID);
    return invoiceLine;
  }

  // Previous JSON builder - create the JSON is now done by BaseMapping and the entity objects in entities.
  public static JSONObject createCompositePoJson(MarcToFolio mappedMarc, Config config, Logger logger) throws Exception {
    JSONObject order = new JSONObject();
    order.put("poNumber", FolioData.getNextPoNumberFromOrders());
    logger.info("NEXT PO NUMBER: " + order.getString("poNumber"));
    order.put("vendor", mappedMarc.vendorUuid());
    order.put("orderType", "One-Time");
    order.put("reEncumber", true);
    order.put("id", UUID.randomUUID().toString());
    order.put("approved", true);
    order.put("workflowStatus","Open");

    if (mappedMarc.billToUuid() != null) order.put("billTo", mappedMarc.billToUuid());
    // POST ORDER LINE
    //FOLIO WILL CREATE THE INSTANCE, HOLDINGS, ITEM (IF PHYSICAL ITEM)
    JSONObject orderLine = new JSONObject();
    JSONObject cost = new JSONObject();
    JSONObject location = new JSONObject();
    JSONArray locations = new JSONArray();
    JSONObject orderLineDetails = new JSONObject();
    JSONArray poLines = new JSONArray();
    if (mappedMarc.electronic()) {
      orderLine.put("orderFormat", "Electronic Resource");
      orderLine.put("receiptStatus", "Receipt Not Required");
      JSONObject eResource = new JSONObject();
      eResource.put("activated", false);
      eResource.put("createInventory", "Instance, Holding");
      if (mappedMarc.hasUserLimit()) {
        eResource.put("userLimit", mappedMarc.userLimit());
      }
      eResource.put("trial", false);
      eResource.put("accessProvider", mappedMarc.accessProviderUUID());
      orderLine.put("eresource",eResource);
      cost.put("quantityElectronic", 1);
      cost.put("listUnitPriceElectronic", mappedMarc.price());
      location.put("quantityElectronic",1);
      location.put("locationId", FolioData.getLocationIdByName(Config.permELocationName));
    }	else {
      JSONObject physical = new JSONObject();
      physical.put("createInventory", "Instance, Holding, Item");
      physical.put("materialType", getMaterialTypeId(Config.materialType));
      orderLine.put("physical", physical);
      orderLine.put("orderFormat", "Physical Resource");
      cost.put("listUnitPrice", mappedMarc.price());
      cost.put("quantityPhysical", 1);
      location.put("quantityPhysical",1);
      location.put("locationId", FolioData.getLocationIdByName(Config.permLocationName));
    }
    locations.put(location);

    if (mappedMarc.hasReceivingNote()) {
      orderLineDetails.put("receivingNote", mappedMarc.receivingNote());
    }

    //VENDOR REFERENCE NUMBER IF INCLUDED IN THE MARC RECORD:
    if (mappedMarc.hasVendorItemId()) {
      JSONArray referenceNumbers = new JSONArray();
      JSONObject vendorDetail = new JSONObject();
      vendorDetail.put("instructions", "");
      vendorDetail.put("vendorAccount", ( mappedMarc.hasVendorAccount() ? mappedMarc.vendorAccount() : ""));
      JSONObject referenceNumber = new JSONObject();
      referenceNumber.put("refNumber", mappedMarc.vendorItemId());
      referenceNumber.put("refNumberType",
              ( mappedMarc.hasRefNumberType() ? mappedMarc.refNumberType() : "Vendor internal number"));
      referenceNumbers.put(referenceNumber);
      vendorDetail.put("referenceNumbers", referenceNumbers);
      orderLine.put("vendorDetail", vendorDetail);
    }
    if (mappedMarc instanceof MarcMapLambda) {
      // Tags
      JSONObject tags = new JSONObject();
      JSONArray tagList = new JSONArray();
      if (( (MarcMapLambda) mappedMarc ).hasObjectCode()) {
        tagList.put(( (MarcMapLambda) mappedMarc ).objectCode());
      }
      if (( (MarcMapLambda) mappedMarc ).hasProjectCode()) {
        tagList.put(( (MarcMapLambda) mappedMarc ).projectCode());
      }
      if (!tagList.isEmpty()) {
        tags.put("tagList", tagList);
        orderLine.put("tags", tags);
      }
    }
    // Order line
    orderLine.put("id", UUID.randomUUID().toString());
    orderLine.put("source", "User");
    cost.put("currency", mappedMarc.currency());
    orderLine.put("cost", cost);
    orderLine.put("locations", locations);
    orderLine.put("titleOrPackage", mappedMarc.title());
    orderLine.put("acquisitionMethod", mappedMarc.acquisitionMethodValue());
    orderLine.put("rush", mappedMarc.rush());
    if (mappedMarc.hasDescription())
      orderLine.put("description", mappedMarc.description());
    JSONArray funds = new JSONArray();
    JSONObject fundDist = new JSONObject();
    fundDist.put("distributionType", "percentage");
    fundDist.put("value", 100);
    fundDist.put("fundId", mappedMarc.fundId());
    fundDist.put("code", mappedMarc.fundCode());
    if (mappedMarc.hasExpenseClassCode())
      fundDist.put("expenseClassId", mappedMarc.expenseClassId());
    funds.put(fundDist);
    orderLine.put("fundDistribution", funds);
    orderLine.put("purchaseOrderId", order.getString("id"));
    poLines.put(orderLine);
    order.put("compositePoLines", poLines);
    if (mappedMarc.hasSelector())
      orderLine.put("selector", mappedMarc.selector());
    if (mappedMarc.hasDonor())
      orderLine.put("donor", mappedMarc.donor());

    orderLine.put("contributors", mappedMarc.getContributorsForOrderLine());
    if (!mappedMarc.productIdentifiers().isEmpty()) {
      orderLineDetails.put("productIds", mappedMarc.productIdentifiers());
    }
    if (!orderLineDetails.isEmpty())
      orderLine.put("details", orderLineDetails);

    if (mappedMarc.hasEdition()) {
      orderLine.put("edition", mappedMarc.edition());
    }

    if (mappedMarc.has260()) {
      if (mappedMarc.publisher("260") != null)
        orderLine.put("publisher", mappedMarc.publisher("260"));
      if (mappedMarc.publisher("260") != null)
        orderLine.put("publicationDate", mappedMarc.publicationDate("260"));
    } else if (mappedMarc.has264()) {
      if (mappedMarc.publisher("264") != null)
        orderLine.put("publisher", mappedMarc.publisher("264"));
      if (mappedMarc.publisher("264") != null)
        orderLine.put("publicationDate", mappedMarc.publicationDate("264"));
    }
    return order;
  }

  private static String getMaterialTypeId (String materialType) {
    return isUUID(materialType) ? materialType : Constants.MATERIAL_TYPES_MAP.get(materialType);
  }
  private static boolean isUUID(String str)
  {
    return ( str != null && Constants.UUID_PATTERN.matcher( str ).matches() );
  }

}
