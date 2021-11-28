package org.olf.folio.order;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.VariableField;
import org.olf.folio.order.storage.FolioData;

import java.util.List;
import java.util.UUID;

public class JsonObjectBuilder {

  public static JSONObject createCompositePoJson(MarcRecordMapping mappedMarc, Config config, Logger logger) throws Exception {
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
      location.put("locationId", FolioData.getLocationIdByName(config.permELocationName));
    }	else {
      JSONObject physical = new JSONObject();
      physical.put("createInventory", "Instance, Holding, Item");
      physical.put("materialType", getMaterialTypeId(config.materialType));
      orderLine.put("physical", physical);
      orderLine.put("orderFormat", "Physical Resource");
      cost.put("listUnitPrice", mappedMarc.price());
      cost.put("quantityPhysical", 1);
      location.put("quantityPhysical",1);
      location.put("locationId", FolioData.getLocationIdByName(config.permLocationName));
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
    // Tags
    JSONObject tags = new JSONObject();
    JSONArray tagList = new JSONArray();
    if (mappedMarc.hasObjectCode()) {
      tagList.put(mappedMarc.objectCode());
    }
    if (mappedMarc.hasProjectCode()) {
      tagList.put(mappedMarc.projectCode());
    }
    if (!tagList.isEmpty()) {
      tags.put("tagList", tagList);
      orderLine.put("tags", tags);
    }
    // Order line
    orderLine.put("id", UUID.randomUUID().toString());
    orderLine.put("source", "User");
    cost.put("currency", mappedMarc.currency());
    orderLine.put("cost", cost);
    orderLine.put("locations", locations);
    orderLine.put("titleOrPackage", mappedMarc.title());
    orderLine.put("acquisitionMethod", mappedMarc.acquisitionMethod());
    orderLine.put("rush", mappedMarc.rush());
    if (mappedMarc.hasDescription())
      orderLine.put("description", mappedMarc.description());
    JSONArray funds = new JSONArray();
    JSONObject fundDist = new JSONObject();
    fundDist.put("distributionType", "percentage");
    fundDist.put("value", 100);
    fundDist.put("fundId", mappedMarc.fundUUID());
    fundDist.put("code", mappedMarc.fundCode());
    if (mappedMarc.hasExpenseClassCode())
      fundDist.put("expenseClassId", mappedMarc.getExpenseClassUUID());
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
    if (!mappedMarc.getProductIdentifiers().isEmpty()) {
      orderLineDetails.put("productIds", mappedMarc.getProductIdentifiers());
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

  static JSONObject createNoteJson(MarcRecordMapping mappedMarc, JSONObject order, Config config) throws Exception {
    JSONObject noteAsJson = new JSONObject();
    JSONArray links = new JSONArray();
    JSONObject link = new JSONObject();
    link.put("type","poLine");
    link.put("id", ((JSONObject) ( order.getJSONArray("compositePoLines").get(0))).getString("id"));
    links.put(link);
    noteAsJson.put("links", links);
    noteAsJson.put("typeId", FolioData.getNoteTypeIdByName(config.noteTypeName));
    noteAsJson.put("domain", "orders");
    noteAsJson.put("content", mappedMarc.notes());
    noteAsJson.put("title", mappedMarc.notes());
    return noteAsJson;
  }

  // Hard-coded values
  final static String BATCH_GROUP_ID = "2a2cb998-1437-41d1-88ad-01930aaeadd5"; // ='FOLIO', System default

  final static String SOURCE = "API";
  final static int INVOICE_LINE_QUANTITY = 1;

  // tbd
  final static String STATUS = "Open";
  final static String INVOICE_LINE_STATUS = "Open";
  final static boolean RELEASE_ENCUMBRANCE = true;

  static JSONObject createInvoiceJson(String poNumber, String vendorId, MarcRecordMapping marc, Config config) {
    JSONObject invoice = new JSONObject();
    invoice.put("id", UUID.randomUUID());
    invoice.put("poNumbers", (new JSONArray()).put(poNumber)); // optional
    invoice.put("batchGroupId", BATCH_GROUP_ID); // required
    invoice.put("currency", marc.currency()); // required
    invoice.put("invoiceDate", marc.invoiceDate()); // required
    invoice.put("paymentMethod", config.paymentMethod); // required
    invoice.put("status", STATUS); // required
    invoice.put("source", SOURCE); // required
    invoice.put("vendorInvoiceNo", marc.vendorInvoiceNo()); // required
    invoice.put("vendorId", vendorId); // required
    return invoice;
  }

  static JSONObject createInvoiceLineJson(UUID orderLineUUID, MarcRecordMapping marc, JSONObject invoice) {
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

  public static JSONArray createElectronicAccessJson(Record record, Config config) {
    JSONArray eResources = new JSONArray();
    String linkText = config.textForElectronicResource;
    List<VariableField> urls =  record.getVariableFields("856");
    for (VariableField url : urls) {
      DataField dataField = (DataField) url;
      if (dataField != null && dataField.getSubfield('u') != null) {
        if (dataField.getSubfield('y') != null) {
          linkText = dataField.getSubfield('y').getData();
        }
        String licenseNote = dataField.getSubfield('z').getData();
        JSONObject eResource = new JSONObject();
        eResource.put("uri", dataField.getSubfield('u').getData());
        //DO WE WANT TO CHANGE THE LINK TEXT?
        eResource.put("linkText", linkText);
        if (licenseNote != null) eResource.put("publicNote", licenseNote);
        //THIS RELATIONSHIP (UUID) IS BUILT INTO FOLIO
        //IMPLEMENTER
        eResource.put("relationshipId", Constants.ELECTRONIC_ACCESS_RELATIONSHIP_TYPE_RESOURCE);
        eResources.put(eResource);
      }
    }
    return eResources;
  }

  public static JSONObject updateItemJsonWithBookplateNote(MarcRecordMapping mappedMarc, JSONObject itemJson) {
    JSONObject bookplateNote = new JSONObject();
    bookplateNote.put("itemNoteTypeId", Constants.ITEM_NOTE_TYPE_ID_ELECTRONIC_BOOKPLATE);
    bookplateNote.put("note", mappedMarc.donor());
    bookplateNote.put("staffOnly", false);
    JSONArray itemNotes = (itemJson.has("notes") ? itemJson.getJSONArray("notes") : new JSONArray());
    itemNotes.put(bookplateNote);
    itemJson.put("notes", itemNotes);
    return itemJson;
  }

  public static JSONObject updateHoldingsRecordJson(JSONObject holdingsRecord, MarcRecordMapping mappedMarc, JSONArray eResources) throws Exception {
    //UPDATE THE HOLDINGS RECORD
    //GET THE HOLDINGS RECORD FOLIO CREATED, SO WE CAN ADD URLs FROM THE 856 IN THE MARC RECORD
    holdingsRecord.put("electronicAccess", eResources);
    //IF THIS WAS AN ELECTRONIC RECORD, MARK THE HOLDING AS E-HOLDING
    if (mappedMarc.electronic()) {
      holdingsRecord.put("holdingsTypeId", FolioData.getHoldingsTypeIdByName("Electronic"));

      if (mappedMarc.hasDonor()) {
        JSONObject bookplateNote = new JSONObject();
        bookplateNote.put("holdingsNoteTypeId", Constants.HOLDINGS_NOTE_TYPE_ID_ELECTRONIC_BOOKPLATE);
        bookplateNote.put("note", mappedMarc.donor());
        bookplateNote.put("staffOnly", false);
        JSONArray holdingsNotes = (holdingsRecord.has("notes") ? holdingsRecord.getJSONArray("notes") : new JSONArray());
        holdingsNotes.put(bookplateNote);
        holdingsRecord.put("notes", holdingsNotes);
      }
    }
    return holdingsRecord;
  }

  public static JSONObject updateInstanceJson(MarcRecordMapping mappedMarc, JSONObject instanceAsJson, JSONArray eResources) throws Exception {
    //UPDATE THE INSTANCE RECORD
    instanceAsJson.put("title", mappedMarc.title());
    instanceAsJson.put("source", OrderImport.config.importSRS ? "MARC" : "FOLIO");
    instanceAsJson.put("instanceTypeId", FolioData.getInstanceTypeId("text"));
    instanceAsJson.put("identifiers", mappedMarc.getInstanceIdentifiers());
    instanceAsJson.put("contributors", mappedMarc.getContributorsForInstance());
    instanceAsJson.put("discoverySuppress", false);
    instanceAsJson.put("electronicAccess", eResources);
    instanceAsJson.put("natureOfContentTermIds", new JSONArray());
    instanceAsJson.put("precedingTitles", new JSONArray());
    instanceAsJson.put("succeedingTitles", new JSONArray());
    return instanceAsJson;
  }
}
