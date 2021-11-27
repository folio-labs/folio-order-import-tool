package org.olf.folio.order;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.ServletContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcWriter;
import org.marc4j.converter.impl.AnselToUnicode;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.apache.log4j.Logger;
import org.marc4j.marc.VariableField;
import org.olf.folio.order.dataobjects.CompositePurchaseOrder;
import org.olf.folio.order.dataobjects.Instance;
import org.olf.folio.order.dataobjects.PoLineLocation;
import org.olf.folio.order.storage.FolioAccess;
import org.olf.folio.order.storage.FolioData;
import org.olf.folio.order.storage.SRSStorage;
import org.folio.isbn.IsbnUtil;

public class OrderImport {

	private static final Logger logger = Logger.getLogger("OrderImport");
	private ServletContext myContext;
	private static Config config;

	public  JSONArray upload(String fileName, boolean doImport) throws Exception {

		logger.info("...starting...");
		if (config == null) {
			config = new Config(myContext);
		}
		FolioAccess.initialize(config, logger);
		JSONArray responseMessages = new JSONArray();

		//GET THE UPLOADED FILE, EXIT IF NONE PROVIDED
		String filePath = (String) myContext.getAttribute("uploadFilePath");
		if (fileName == null) {
			JSONObject responseMessage = new JSONObject();
			responseMessage.put("error", "no input file provided");
			responseMessage.put("PONumber", "~error~");
			responseMessages.put(responseMessage);
			return responseMessages;
		}

		//READ THE MARC RECORD FROM THE FILE AND CHECK REQUIRED VALUES
		//EXIT IF ANY RECORD MISSES VALUES
		InputStream in = new FileInputStream(filePath + fileName);
		MarcReader reader = new MarcStreamReader(in);

		JSONArray validationResult = validateMarcRecords(reader);
		if (! doImport) {
			return validationResult;
		}

		//AGAIN, READ THE MARC RECORD FROM THE FILE
		in = new FileInputStream(filePath + fileName);
		reader = new MarcStreamReader(in);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		MarcWriter w = new MarcStreamWriter(byteArrayOutputStream,"UTF-8");

		AnselToUnicode conv = new AnselToUnicode();
		w.setConverter(conv);

		ProcessCounters counters = new ProcessCounters();
		while (reader.hasNext()) {
			JSONObject responseMessage = new JSONObject();
			try {
				Record record = reader.next();
				responseMessage.put("source", record.toString());
				counters.recordsProcessed++;

				MarcRecordMapping mappedMarc = new MarcRecordMapping(record);

				CompositePurchaseOrder newOrder = CompositePurchaseOrder.fromMarcRecord(mappedMarc);

				config.permLocationName = (config.importInvoice && mappedMarc.hasInvoice()
								? config.permLocationWithInvoiceImport : config.permLocationName);
				config.permELocationName = (config.importInvoice && mappedMarc.hasInvoice()
								? config.permELocationWithInvoiceImport : config.permELocationName);

				if (!newOrder.getCompositePoLines().isEmpty()
								&& !newOrder.getCompositePoLines().get(0).getLocations().isEmpty()) {
					PoLineLocation poLineLocation = newOrder.getCompositePoLines().get(0).getLocations().get(0);
					if (mappedMarc.electronic()) {
						poLineLocation.putLocationId(FolioData.getLocationIdByName(config.permELocationName));
					} else {
						poLineLocation.putLocationId(FolioData.getLocationIdByName(config.permLocationName));
					}
				}
				logger.info("Created CompositePurchaseOrder: " + newOrder.asJson().toString());

				//NOW WE CAN START CREATING THE PO!
				responseMessage.put("recNo", counters.recordsProcessed);
				responseMessage.put("title", mappedMarc.title());
	  		responseMessage.put("ISBN", mappedMarc.hasISBN() ? mappedMarc.getISBN() : "No ISBN in this record");

				// CREATING THE PURCHASE ORDER
				JSONObject order = createCompositePo(mappedMarc);

				//POST THE ORDER AND LINE:
				String orderResponse = FolioAccess.callApiPostWithUtf8("orders/composite-orders",order);
				JSONObject approvedOrder = new JSONObject(orderResponse);
				logger.info(orderResponse);

				//INSERT THE NOTE IF THERE IS A NOTE IN THE MARC RECORD
				if (mappedMarc.hasNotes()) {
					JSONObject noteAsJson = new JSONObject();
					JSONArray links = new JSONArray();
					JSONObject link = new JSONObject();
					link.put("type","poLine");
					link.put("id", ((JSONObject) (order.getJSONArray("compositePoLines").get(0))).getString("id"));
					links.put(link);
					noteAsJson.put("links", links);
					noteAsJson.put("typeId", FolioData.getNoteTypeIdByName(config.noteTypeName));
					noteAsJson.put("domain", "orders");
					noteAsJson.put("content", mappedMarc.notes());
					noteAsJson.put("title", mappedMarc.notes());
					String noteResponse = FolioAccess.callApiPostWithUtf8("/notes",noteAsJson);
					logger.info(noteResponse);
				}

				//GET THE UPDATED PURCHASE ORDER FROM THE API AND PULL OUT THE ID FOR THE INSTANCE FOLIO CREATED:
				JSONObject updatedPurchaseOrderJson = FolioAccess.callApiGet("orders/composite-orders/" +order.getString("id"));
				String instanceId =
								updatedPurchaseOrderJson.getJSONArray("compositePoLines")
												.getJSONObject(0).getString("instanceId");

				//GET THE INSTANCE RECORD FOLIO CREATED, SO WE CAN ADD BIB INFO TO IT:
				JSONObject instanceAsJson = FolioAccess.callApiGet("inventory/instances/" + instanceId);
				String hrid = instanceAsJson.getString("hrid");


				// UChicago have asked that the MARC NOT be stored to SRS since this has implications for the ability to
				// batch update the instance record with the full cataloging when UChicago receive the invoice.
				if ( config.importSRS ) {
					SRSStorage.storeMarcToSRS(record,	byteArrayOutputStream,
							UUID.randomUUID(), // snapshotId
							UUID.randomUUID(), // recordTableId
							instanceId,	hrid );
				}

				Instance instance = Instance.fromJson(instanceAsJson)
								.putTitle(mappedMarc.title())
								.putSource(config.importSRS ? Instance.V_MARC : Instance.V_FOLIO)
								.putInstanceTypeId(FolioData.getInstanceTypeId("text"))
								.putIdentifiers(mappedMarc.getInstanceIdentifiers())
								.putContributors(mappedMarc.getContributorsForInstance())
								.putDiscoverySuppress(false)
								.putElectronicAccess(mappedMarc.getElectronicAccess())
							  .putNatureOfContentTermIds(new JSONArray())
								.putPrecedingTitles(new JSONArray())
								.putSucceedingTitles(new JSONArray());

				logger.info("Two Instance objects, same HRID? : " + instance.getHrid().equalsIgnoreCase(hrid));

				instanceAsJson.put("title", mappedMarc.title());
				instanceAsJson.put("source", config.importSRS ? "MARC" : "FOLIO");
				instanceAsJson.put("instanceTypeId", FolioData.getInstanceTypeId("text"));
				instanceAsJson.put("identifiers", mappedMarc.getInstanceIdentifiers());
				instanceAsJson.put("contributors", mappedMarc.getContributorsForInstance());
				instanceAsJson.put("discoverySuppress", false);

				//GET THE HOLDINGS RECORD FOLIO CREATED, SO WE CAN ADD URLs FROM THE 856 IN THE MARC RECORD
				JSONObject holdingsAsJson = FolioAccess.callApiGet("holdings-storage/holdings?query=(instanceId==" + instanceId + ")");
				JSONObject holdingsRecord = holdingsAsJson.getJSONArray("holdingsRecords").getJSONObject(0);

				JSONArray eResources = new JSONArray();
				String linkText = (String) getMyContext().getAttribute("textForElectronicResources");
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

				logger.info("Created Instance object " + instance.asJson());

				//UPDATE THE INSTANCE RECORD
				instanceAsJson.put("electronicAccess", eResources);
				instanceAsJson.put("natureOfContentTermIds", new JSONArray());
				instanceAsJson.put("precedingTitles", new JSONArray());
				instanceAsJson.put("succeedingTitles", new JSONArray());
				String instanceUpdateResponse = FolioAccess.callApiPut( "inventory/instances/" + instanceId,  instanceAsJson);

				//UPDATE THE HOLDINGS RECORD
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
				FolioAccess.callApiPut("holdings-storage/holdings/" + holdingsRecord.getString("id"), holdingsRecord);

				if (!mappedMarc.electronic() && mappedMarc.hasDonor()) {
					//IF PHYSICAL RESOURCE WITH DONOR INFO, GET THE ITEM FOLIO CREATED, SO WE CAN ADD NOTE ABOUT DONOR
					JSONObject itemsAsJson = FolioAccess.callApiGet("inventory/items?query=(holdingsRecordId==" + holdingsRecord.get("id") + ")");
					JSONObject item = itemsAsJson.getJSONArray("items").getJSONObject(0);
					JSONObject bookplateNote = new JSONObject();
					bookplateNote.put("itemNoteTypeId", Constants.ITEM_NOTE_TYPE_ID_ELECTRONIC_BOOKPLATE);
					bookplateNote.put("note", mappedMarc.donor());
					bookplateNote.put("staffOnly", false);
					JSONArray itemNotes = (item.has("notes") ? item.getJSONArray("notes") : new JSONArray());
					itemNotes.put(bookplateNote);
					item.put("notes", itemNotes);
					//UPDATE THE ITEM
					FolioAccess.callApiPut("inventory/items/" + item.getString("id"), item);
				}

				if (config.importInvoice && mappedMarc.hasInvoice()) {
					importInvoice(
									order.getString("poNumber"),
									UUID.fromString(((JSONObject)(order.getJSONArray("compositePoLines").get(0))).getString("id")),
									mappedMarc.vendorUuid(),
									mappedMarc);
				}

				//SAVE THE PO NUMBER FOR THE RESPONSE
				responseMessage.put("PONumber", order.getString("poNumber"));
				responseMessage.put("theOne", hrid);

				responseMessages.put(responseMessage);
				counters.recordsImported++;
			}
			catch(Exception e) {
				logger.error(e.toString());
				counters.recordsFailed++;
				try {
					responseMessage.put("PONumber", "~error~");
					JSONObject msg = new JSONObject(e.getMessage());
					if (msg.has("errors") && msg.get("errors") instanceof JSONArray && !msg.getJSONArray("errors").isEmpty()) {
						responseMessage.put("error", ((JSONObject) (msg.getJSONArray("errors").get(0))).getString("message"));
					}
				} catch (JSONException | ClassCastException je) {
					// IGNORE
				}
				if (responseMessage.getString("error") == null || responseMessage.getString("error").isEmpty()) {
					responseMessage.put("error", e.getMessage());
				}
				responseMessages.put(responseMessage);
			}
		}
		logger.info(counters.recordsProcessed + " record" + (counters.recordsProcessed == 1 ? "" : "s")
						+ " processed, "
						+ counters.recordsImported + " record" + (counters.recordsImported == 1 ? "" : "s")
		        + " imported, and "
						+ counters.recordsFailed + " record" + (counters.recordsFailed == 1 ? "" : "s")
						+ " failed.");
		return responseMessages;
	}

	private JSONObject createCompositePo(MarcRecordMapping mappedMarc) throws Exception {
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

	private void importInvoice(
							   String poNumber,
							   UUID orderLineUUID,
							   String vendorId,
							   MarcRecordMapping marc) throws Exception {


		// Hard-coded values
		final String BATCH_GROUP_ID = "2a2cb998-1437-41d1-88ad-01930aaeadd5"; // ='FOLIO', System default

		final String SOURCE = "API";
		final int INVOICE_LINE_QUANTITY = 1;

		// Static config values:
		final String PAYMENT_METHOD_PROPERTY = "paymentMethod";
		final String PAYMENT_METHOD = (String) getMyContext().getAttribute(PAYMENT_METHOD_PROPERTY);

		// tbd
		final String STATUS = "Open";
		final String INVOICE_LINE_STATUS = "Open";
		final boolean RELEASE_ENCUMBRANCE = true;

		//CREATE INVOICE OBJECTS
		JSONObject invoice = new JSONObject();
		UUID invoiceUUID = UUID.randomUUID();

		invoice.put("id", invoiceUUID);
		invoice.put("poNumbers", (new JSONArray()).put(poNumber)); // optional
		invoice.put("batchGroupId", BATCH_GROUP_ID); // required
		invoice.put("currency", marc.currency()); // required
		invoice.put("invoiceDate", marc.invoiceDate()); // required
		invoice.put("paymentMethod", PAYMENT_METHOD); // required
		invoice.put("status", STATUS); // required
		invoice.put("source", SOURCE); // required
		invoice.put("vendorInvoiceNo", marc.vendorInvoiceNo()); // required
		invoice.put("vendorId", vendorId); // required

		JSONObject invoiceLine = new JSONObject();
		invoiceLine.put("description", marc.title());  // required
		invoiceLine.put("invoiceId", invoiceUUID); // required
		invoiceLine.put("invoiceLineStatus", INVOICE_LINE_STATUS); // required
		invoiceLine.put("subTotal", marc.subTotal());  // required
		invoiceLine.put("quantity", INVOICE_LINE_QUANTITY); // required
		invoiceLine.put("releaseEncumbrance", RELEASE_ENCUMBRANCE);  // required
		invoiceLine.put("poLineId", orderLineUUID);

		//POST INVOICE OBJECTS
		String invoiceResponse = FolioAccess.callApiPostWithUtf8("invoice/invoices", invoice);
		logger.info(invoiceResponse);
		String invoiceLineResponse = FolioAccess.callApiPostWithUtf8("invoice/invoice-lines", invoiceLine);
		logger.info(invoiceLineResponse);
	}

	public JSONArray validateMarcRecords(MarcReader reader) {

		Record record;
		JSONArray responseMessages = new JSONArray();
		int r = 0;
		while(reader.hasNext()) {
				record = reader.next();
				r++;
				MarcRecordMapping marc = new MarcRecordMapping(record);
				responseMessages.put(validateMarcRecord(marc, r));
		}
		return responseMessages;
	}

	private JSONObject validateMarcRecord (MarcRecordMapping mappedMarc, int recNo) {
		JSONObject msg = new JSONObject();
		try {
			msg.put("recNo", recNo);
			msg.put("controlNumber", mappedMarc.controlNumber());
			msg.put("title", mappedMarc.title());
			msg.put("source", mappedMarc.marcRecord.toString());

			if (!mappedMarc.has980()) {
				msg.put("error", String.format("Record #%s is missing the 980 field", recNo));
				msg.put("PONumber", "~error~");
				msg.put("title", mappedMarc.title());
				return msg;
			}

			if (!mappedMarc.hasISBN()) {
				msg.put("error", true);
			} else if (!isValidIsbn(mappedMarc.getISBN())) {
				msg.put("error", true);
			}
			msg.put("invalidIsbn", (mappedMarc.hasISBN() && !isValidIsbn(mappedMarc.getISBN())));
			msg.put("noIsbn", (!mappedMarc.hasISBN()));
			msg.put("ISBN", mappedMarc.hasISBN() ? mappedMarc.getISBN()  : "No ISBN");
			msg.put("productIdentifiers", mappedMarc.getProductIdentifiers().toString());
			msg.put("instanceIdentifiers", mappedMarc.getInstanceIdentifiers().toString());

			Map<String, String> requiredFields = new HashMap<>();
			if (config.objectCodeRequired) {
				requiredFields.put("Object code", mappedMarc.objectCode());
			}
			requiredFields.put("Fund code", mappedMarc.fundCode());
			requiredFields.put("Vendor Code", mappedMarc.vendorCode());
			requiredFields.put("Price" , mappedMarc.price());

			// MAKE SURE EACH OF THE REQUIRED SUBFIELDS HAS DATA
			for (Map.Entry<String,String> entry : requiredFields.entrySet())  {
				if (entry.getValue()==null || entry.getValue().isEmpty()) {
					msg.put("error", entry.getKey() + " Missing");
				}
			}

			//VALIDATE THE ORGANIZATION, OBJECT CODE AND FUND
			//STOP THE PROCESS IF ANY ERRORS WERE FOUND
			JSONObject orgValidationResult = FolioData.validateOrganization(mappedMarc.vendorCode(), mappedMarc.title());
			if (orgValidationResult != null) msg.put("error", msg.getString("error") + " " + orgValidationResult.getString("error"));

			if (mappedMarc.hasObjectCode()) {
				JSONObject objectValidationResult = FolioData.validateObjectCode(mappedMarc.objectCode(), mappedMarc.title());
				if (objectValidationResult != null) msg.put("error", msg.getString("error") + " " + objectValidationResult.getString("error"));
			}
			if (mappedMarc.hasProjectCode()) {
				// TODO: Check this
				JSONObject projectValidationResult = FolioData.validateObjectCode(mappedMarc.projectCode(), mappedMarc.title());
				if (projectValidationResult != null) msg.put("error", msg.getString("error") + " " + projectValidationResult.getString("error"));
			}
			JSONObject fundValidationResult = FolioData.validateFund(mappedMarc.fundCode(), mappedMarc.title(), mappedMarc.price());
			if (fundValidationResult != null) msg.put("error", msg.getString("error") + " " + fundValidationResult.getString("error"));

			if (config.importInvoice) {
				JSONObject invoiceValidationResult = FolioData.validateRequiredValuesForInvoice(mappedMarc.title(), mappedMarc.marcRecord);
				if (invoiceValidationResult != null) msg.put("error", msg.getString("error") + " " + invoiceValidationResult.getString("error"));
			}

		}	catch(Exception e) {
			logger.error("Got exception when validating MARC record: " + e.getMessage() + " " + e.getClass());
			msg.put("error", e.getMessage());
		}
		logger.info("Validation result: " + msg);
		return msg;
	}

	public ServletContext getMyContext() {
		return myContext;
	}

	public void setMyContext(ServletContext myContext) {
		this.myContext = myContext;
	}

	private static String getMaterialTypeId (String materialType) {
		return isUUID(materialType) ? materialType : Constants.MATERIAL_TYPES_MAP.get(materialType);
	}

	private static boolean isUUID(String str)
	{
		return ( str != null && Constants.UUID_PATTERN.matcher( str ).matches() );
	}

	private static boolean isValidIsbn (String isbn) {
		if (isbn.length() == 10) {
			return IsbnUtil.isValid10DigitNumber(isbn);
		} else if (isbn.length() == 13) {
			return IsbnUtil.isValid13DigitNumber(isbn);
		} else {
			return false;
		}

	}
}