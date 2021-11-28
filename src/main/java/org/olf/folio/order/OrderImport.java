package org.olf.folio.order;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
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
import org.marc4j.marc.Record;
import org.apache.log4j.Logger;
import org.olf.folio.order.dataobjects.CompositePurchaseOrder;
import org.olf.folio.order.dataobjects.HoldingsRecord;
import org.olf.folio.order.dataobjects.Instance;
import org.olf.folio.order.dataobjects.Item;
import org.olf.folio.order.dataobjects.PoLineLocation;
import org.olf.folio.order.storage.FolioAccess;
import org.olf.folio.order.storage.FolioData;
import org.olf.folio.order.storage.SRSStorage;
import org.folio.isbn.IsbnUtil;

public class OrderImport {

	private static final Logger logger = Logger.getLogger("OrderImport");
	private ServletContext myContext;
	public static Config config;

	public  JSONArray upload(String fileName, boolean doImport) throws Exception {

		logger.info("...starting...");
		if (config == null) {
			config = new Config(myContext);
		}
		FolioAccess.initialize(config, logger);
		JSONArray responseMessages = new JSONArray();
		ByteArrayOutputStream byteArrayOutputStreamForSRS = null;

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
		ProcessCounters counters = new ProcessCounters();
		while (reader.hasNext()) {
			JSONObject responseMessage = new JSONObject();
			try {
				Record record = reader.next();
				responseMessage.put("source", record.toString());
				counters.recordsProcessed++;

				MarcRecordMapping mappedMarc = new MarcRecordMapping(record);

				// New JSON builder
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
				// End of new JSON builder

				//NOW WE CAN START CREATING THE PO!
				responseMessage.put("recNo", counters.recordsProcessed);
				responseMessage.put("title", mappedMarc.title());
	  		responseMessage.put("ISBN", mappedMarc.hasISBN() ? mappedMarc.getISBN() : "No ISBN in this record");

				// CREATING AND POST THE PURCHASE ORDER AND LINE
				JSONObject createdCompositePo = JsonObjectBuilder.createCompositePoJson(mappedMarc, config, logger);
				logger.info(
								FolioAccess.callApiPostWithUtf8("orders/composite-orders",createdCompositePo).toString()
				);

				//INSERT THE NOTE IF THERE IS A NOTE IN THE MARC RECORD
				if (mappedMarc.hasNotes()) {
					JSONObject noteAsJson = JsonObjectBuilder.createNoteJson(mappedMarc, createdCompositePo, config);
					logger.info(
									FolioAccess.callApiPostWithUtf8("/notes",noteAsJson).toString()
					);
				}

				//GET THE UPDATED PURCHASE ORDER FROM THE API AND PULL OUT THE ID FOR THE INSTANCE FOLIO CREATED:
				JSONObject fetchedCompositePo = FolioAccess.callApiGet("orders/composite-orders/" +createdCompositePo.getString("id"));
				String instanceId =
								fetchedCompositePo.getJSONArray("compositePoLines")
												.getJSONObject(0).getString("instanceId");


				//GET THE INSTANCE RECORD FOLIO CREATED, SO WE CAN ADD BIB INFO TO IT:
				JSONObject fetchedInstance = FolioAccess.callApiGet("inventory/instances/" + instanceId);
				//String instanceHrid = fetchedInstance.getString("hrid");

				// UChicago have asked that the MARC NOT be stored to SRS since this has implications for the ability to
				// batch update the instance record with the full cataloging when UChicago receive the invoice.
				if ( config.importSRS ) {
					if (byteArrayOutputStreamForSRS == null )
						byteArrayOutputStreamForSRS = getOutputStreamForSRS();
					SRSStorage.storeMarcToSRS(record,	byteArrayOutputStreamForSRS, instanceId,	fetchedInstance.getString("hrid") );
				}

				// new logic not in use
				Instance instance_new_logic = DataObjectBuilder.createAndUpdateInstanceFromJson(fetchedInstance, mappedMarc, config);
				logger.info("JSON from newCreated Instance object with data object logic: " + instance_new_logic.asJson());
        //

				JSONArray createdElectronicAccess = JsonObjectBuilder.createElectronicAccessJson(record, config);

				JSONObject updatedInstance = JsonObjectBuilder.updateInstanceJson(mappedMarc, fetchedInstance, createdElectronicAccess);

				if (!(instance_new_logic.asJson().toString()).equalsIgnoreCase(updatedInstance.toString())) {
					logger.info("Instance JSON was not exactly identical from old and new JSON creation logic: ");
					logger.info("String length old: "
									+ instance_new_logic.toString().length()
									+ ", String length new: "
									+ updatedInstance.toString().length());
					logger.info("Instance JSON, new logic: " + instance_new_logic.asJson().toString());
					logger.info("Instance JSON, old logic: " + updatedInstance);
				}

				FolioAccess.callApiPut( "inventory/instances/" + instanceId,  updatedInstance);

				JSONObject fetchedHoldingsRecord = FolioAccess.callApiGetFirstObjectOfArray("holdings-storage/holdings?query=(instanceId==" + instanceId + ")", "holdingsRecords");
				if (fetchedHoldingsRecord == null) {
					throw new Exception("Failed to retrieve holdings record that was expected to be created on uploading the order");
				}
				HoldingsRecord holdingsRecord_new_logic =
								DataObjectBuilder.createAndUpdateHoldingsRecordFromJson(fetchedHoldingsRecord, mappedMarc);

				JSONObject updatedHoldingsRecord = JsonObjectBuilder.updateHoldingsRecordJson(
								fetchedHoldingsRecord, mappedMarc, createdElectronicAccess);

				if (!(holdingsRecord_new_logic.asJson().toString()).equalsIgnoreCase(updatedHoldingsRecord.toString())) {
					logger.info("HoldingsRecord JSON was not exactly identical from old and new JSON creation logic: ");
					logger.info("String length old: "
									+ holdingsRecord_new_logic.toString().length()
									+ ", String length new: "
									+ updatedHoldingsRecord.toString().length());
					logger.info("HoldingsRecord JSON, new logic: " + holdingsRecord_new_logic.asJson().toString());
					logger.info("HoldingsRecord JSON, old logic: " + updatedHoldingsRecord);
				}

				FolioAccess.callApiPut("holdings-storage/holdings/" + updatedHoldingsRecord.getString("id"),
								updatedHoldingsRecord);

				if (!mappedMarc.electronic() && mappedMarc.hasDonor()) {
					//IF PHYSICAL RESOURCE WITH DONOR INFO, GET THE ITEM FOLIO CREATED, SO WE CAN ADD NOTE ABOUT DONOR
					JSONObject fetchedItem = FolioAccess.callApiGetFirstObjectOfArray("inventory/items?query=(holdingsRecordId==" + updatedHoldingsRecord.get("id") + ")", "items");
					Item item_new_logic = DataObjectBuilder.createAndUpdateItemFromJson(fetchedItem,mappedMarc);
					if (fetchedItem == null) {
						throw new Exception("Failed to retrieve Item that was expected to be created on uploading the order");
					}
					JSONObject updatedItem = JsonObjectBuilder.updateItemJsonWithBookplateNote(mappedMarc, fetchedItem);
					if (!(item_new_logic.asJson().toString()).equalsIgnoreCase(updatedItem.toString())) {
						logger.info("Item JSON was not exactly identical from old and new JSON creation logic: ");
						logger.info("String length old: "
										+ item_new_logic.toString().length()
										+ ", String length new: "
										+ updatedItem.toString().length());
						logger.info("Item JSON, new logic: " + item_new_logic.asJson().toString());
						logger.info("Item JSON, old logic: " + updatedItem);
					}
					FolioAccess.callApiPut("inventory/items/" + updatedItem.getString("id"), updatedItem);
				}

				if (config.importInvoice && mappedMarc.hasInvoice()) {
					importInvoice(
									createdCompositePo.getString("poNumber"),
									UUID.fromString(((JSONObject)(createdCompositePo.getJSONArray("compositePoLines").get(0))).getString("id")),
									mappedMarc.vendorUuid(),
									mappedMarc,
									config);
				}

				//SAVE THE PO NUMBER FOR THE RESPONSE
				responseMessage.put("PONumber",  createdCompositePo.getString("poNumber"));
				responseMessage.put("instanceHrid", fetchedInstance.getString("hrid"));
				if (config.folioUiUrl != null) {
					if (config.folioUiInventoryPath != null) {
						responseMessage.put("inventoryUrl",
										config.folioUiUrl	+ config.folioUiInventoryPath	+ "/"
														+ fetchedInstance.getString("id")
														+ "?qindex=hrid&query="	+ fetchedInstance.getString("hrid")
														+ "&sort=title");
					}
					if (config.folioUiOrdersPath != null) {
						responseMessage.put("ordersUrl",
										config.folioUiUrl + config.folioUiOrdersPath + "/"
														+ fetchedCompositePo.getString("id")
														+ "?qindex=poNumber&query=" + fetchedCompositePo.getString("poNumber"));
					}
				}

				responseMessages.put(responseMessage);
				counters.recordsImported++;

			}	catch(Exception e) {
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

	private ByteArrayOutputStream getOutputStreamForSRS() {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		MarcWriter w = new MarcStreamWriter(byteArrayOutputStream,"UTF-8");
		AnselToUnicode conv = new AnselToUnicode();
		w.setConverter(conv);
		return byteArrayOutputStream;
	}

	public static void importInvoice(
							   String poNumber,
							   UUID orderLineUUID,
							   String vendorId,
							   MarcRecordMapping marc,
	               Config config) throws Exception {

		//CREATE INVOICE OBJECTS
		JSONObject invoice = JsonObjectBuilder.createInvoiceJson(poNumber, vendorId, marc, config);
		JSONObject invoiceLine = JsonObjectBuilder.createInvoiceLineJson(orderLineUUID, marc, invoice);
		//POST INVOICE OBJECTS
		logger.info(
						FolioAccess.callApiPostWithUtf8("invoice/invoices", invoice)
										.toString());
		logger.info(
						FolioAccess.callApiPostWithUtf8("invoice/invoice-lines", invoiceLine)
										.toString());
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
