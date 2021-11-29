package org.olf.folio.order;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.UUID;
import javax.servlet.ServletContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.marc.Record;
import org.apache.log4j.Logger;
import org.olf.folio.order.dataobjects.BookplateNote;
import org.olf.folio.order.dataobjects.CompositePurchaseOrder;
import org.olf.folio.order.dataobjects.HoldingsRecord;
import org.olf.folio.order.dataobjects.Instance;
import org.olf.folio.order.dataobjects.Item;
import org.olf.folio.order.dataobjects.Link;
import org.olf.folio.order.dataobjects.Note;
import org.olf.folio.order.storage.FolioAccess;
import org.olf.folio.order.storage.FolioData;

public class OrderImport {

	private static final Logger logger = Logger.getLogger("OrderImport");
	private ServletContext myContext;
	public static Config config;

	public  JSONArray upload(String fileName, boolean analyze) throws Exception {

		logger.info("...starting...");
		if (config == null) {
			config = new Config(myContext);
		}
		FolioAccess.initialize(config, logger);
		JSONArray responseMessages = new JSONArray();

		//GET THE UPLOADED FILE, EXIT IF NONE PROVIDED
		if (fileName == null) {
			JSONObject responseMessage = new JSONObject();
			responseMessage.put("error", "no input file provided");
			responseMessage.put("PONumber", "~error~");
			responseMessages.put(responseMessage);
			return responseMessages;
		}

		RecordChecker check = new RecordChecker(config);
		if (analyze) {
			return check.validateMarcRecords(fileName);
		} else if (config.onValidationErrorsCancelAll) {
			JSONArray result = check.validateMarcRecords(fileName);
			if (check.errorsFound()) {
				JSONObject message = new JSONObject();
				message.put("isHeader", true);
				message.put("isCancelled", true);
				message.put("error", "The import was cancelled due to one or more validation errors");
				JSONArray response = new JSONArray();
				response.put(message);
				for (Object msg : result) {
					response.put(msg);
				}
				return response;
			}
		}

		InputStream in = new FileInputStream(config.uploadFilePath + fileName);
		MarcReader reader = new MarcStreamReader(in);
		ProcessCounters counters = new ProcessCounters();
		while (reader.hasNext()) {
			JSONObject responseMessage = new JSONObject();
			try {
				Record record = reader.next();
				counters.recordsProcessed++;
				MarcRecordMapping mappedMarc = new MarcRecordMapping(record);
				if (config.onValidationErrorsSKipFailed) {
					JSONObject result = check.validateMarcRecord(mappedMarc,counters.recordsProcessed);
					if (result.has("error")) {
						result.put("skipped", "record was not imported, due to validation errors");
						responseMessages.put(result);
						counters.recordsSkipped++;
						continue;
					}
				}
				responseMessage.put("source", record.toString());
				responseMessage.put("recNo", counters.recordsProcessed);
				responseMessage.put("title", mappedMarc.title());
	  		responseMessage.put("ISBN", mappedMarc.hasISBN() ? mappedMarc.getISBN() : "No ISBN in this record");

				// CREATE AND POST THE PURCHASE ORDER AND LINE
				CompositePurchaseOrder compositePo = CompositePurchaseOrder.fromMarcRecord(mappedMarc);
				setPreconfiguredValues(compositePo, mappedMarc);
				FolioAccess.callApiPostWithUtf8(FolioData.COMPOSITE_ORDERS_PATH, compositePo);

				//INSERT A NOTE IF THERE IS ONE IN THE MARC RECORD
				if (mappedMarc.hasNotes()
								&& compositePo.hasPoLines() && config.noteTypeName != null) {
					Note note = new Note()
									.addLink(new Link()
													.putType(Link.V_PO_LINE)
													.putId(compositePo.getCompositePoLines().get(0).getId()))
									.putTypeId(FolioData.getNoteTypeIdByName(config.noteTypeName))
									.putDomain(Note.V_ORDERS)
									.putContent(mappedMarc.notes())
									.putTitle(mappedMarc.notes());
					FolioAccess.callApiPostWithUtf8(FolioData.NOTES_PATH,note.asJson());
				}

				// GET THE UPDATED PURCHASE ORDER FROM FOLIO AND PULL OUT THE ID OF THE RELATED INSTANCE
				CompositePurchaseOrder fetchedPo = CompositePurchaseOrder.fromJson(
								FolioAccess.callApiGetById(FolioData.COMPOSITE_ORDERS_PATH, compositePo.getId()));

				// RETRIEVE, UPDATE, AND PUT THE RELATED INSTANCE
				Instance fetchedInstance = Instance.fromJson(
												FolioAccess.callApiGetById(FolioData.INSTANCES_PATH, fetchedPo.getInstanceId()));
				fetchedInstance.putTitle(mappedMarc.title())
								.putSource(Instance.V_FOLIO)
								.putInstanceTypeId(FolioData.getInstanceTypeId("text"))
								.putIdentifiers(mappedMarc.getInstanceIdentifiers())
								.putContributors(mappedMarc.getContributorsForInstance())
								.putDiscoverySuppress(false)
								.putElectronicAccess(mappedMarc.getElectronicAccess())
								.putNatureOfContentTermIds(new JSONArray())
								.putPrecedingTitles(new JSONArray())
								.putSucceedingTitles(new JSONArray());
				FolioAccess.callApiPut( FolioData.INSTANCES_PATH,  fetchedInstance);
        // END OF INSTANCE

				// RETRIEVE, UPDATE, AND PUT THE RELATED HOLDINGS RECORD
				HoldingsRecord fetchedHoldingsRecord = HoldingsRecord.fromJson(
								FolioAccess.callApiGetFirstObjectOfArray(
												FolioData.HOLDINGS_STORAGE_PATH
																+ "?query=(instanceId==" + fetchedInstance.getId() + ")", FolioData.HOLDINGS_RECORDS_ARRAY)	);
				fetchedHoldingsRecord.putElectronicAccess(mappedMarc.getElectronicAccess());
				if (mappedMarc.electronic()) {
					fetchedHoldingsRecord.putHoldingsTypeId(FolioData.getHoldingsTypeIdByName("Electronic"));
					if (mappedMarc.hasDonor()) {
						fetchedHoldingsRecord.addBookplateNote(BookplateNote.createElectronicBookplateNote(mappedMarc.donor()));
					}
				}
				FolioAccess.callApiPut(FolioData.HOLDINGS_STORAGE_PATH, fetchedHoldingsRecord);
        // END OF HOLDINGS RECORD

				// RETRIEVE, UPDATE, AND PUT THE RELATED ITEM IF THE MARC RECORD HAS A DONOR
				if (!mappedMarc.electronic() && mappedMarc.hasDonor()) {
					//IF PHYSICAL RESOURCE WITH DONOR INFO, GET THE ITEM FOLIO CREATED, SO WE CAN ADD NOTE ABOUT DONOR
					Item fetchedItem = Item.fromJson(
									FolioAccess.callApiGetFirstObjectOfArray(
													FolioData.ITEMS_PATH + "?query=(holdingsRecordId==" + fetchedHoldingsRecord.getId() + ")", FolioData.ITEMS_ARRAY));
					fetchedItem.addBookplateNote(BookplateNote.createPhysicalBookplateNote(mappedMarc.donor()));
					FolioAccess.callApiPut( FolioData.ITEMS_PATH, fetchedItem);
				}
        // END OF ITEM

				// IMPORT INVOICE
				if (config.importInvoice && mappedMarc.hasInvoice()) {
					importInvoice(
									fetchedPo.getPoNumber(),
									UUID.fromString(fetchedPo.getCompositePoLines().get(0).getId()),
									mappedMarc.vendorUuid(),
									mappedMarc,
									config);
				}

				// REPORT RESULTS TO THE CLIENT
				responseMessage.put("PONumber",  fetchedPo.getPoNumber());
				responseMessage.put("instanceHrid", fetchedInstance.getHrid());
				if (config.folioUiUrl != null) {
					if (config.folioUiInventoryPath != null) {
						responseMessage.put("inventoryUrl",
										config.folioUiUrl	+ config.folioUiInventoryPath	+ "/"
														+ fetchedInstance.getId()
														+ "?qindex=hrid&query="	+ fetchedInstance.getHrid()
														+ "&sort=title");
					}
					if (config.folioUiOrdersPath != null) {
						responseMessage.put("ordersUrl",
										config.folioUiUrl + config.folioUiOrdersPath + "/"
														+ fetchedPo.getId()
														+ "?qindex=poNumber&query=" + fetchedPo.getPoNumber());
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
		if (counters.recordsSkipped>0 || counters.recordsFailed>0) {
			JSONObject message = new JSONObject();
			message.put("isHeader", true);
			message.put("isError", true);
			message.put("error", counters.recordsSkipped
							+ " records where skipped due to validation errors. "
							+ counters.recordsFailed + " records failed during import. "
							+ counters.recordsImported + " records were imported ");
			JSONArray response = new JSONArray();
			response.put(message);
			for (Object msg : responseMessages) {
				response.put(msg);
			}
			responseMessages = response;
		}
		return responseMessages;
	}

	private void setPreconfiguredValues(
					CompositePurchaseOrder newCompositePurchaseOrder, MarcRecordMapping mappedMarc)
					throws Exception {

		String permLocationName = (config.importInvoice && mappedMarc.hasInvoice()
					? config.permLocationWithInvoiceImport : config.permLocationName);
		String permELocationName = (config.importInvoice && mappedMarc.hasInvoice()
					? config.permELocationWithInvoiceImport : config.permELocationName);
		String permLocationId = FolioData.getLocationIdByName(permLocationName);
		String permELocationId = FolioData.getLocationIdByName(permELocationName);

		if (mappedMarc.electronic()) {
			newCompositePurchaseOrder.setLocationIdOnPoLines(permELocationId);
		} else {
			newCompositePurchaseOrder.setLocationIdOnPoLines(permLocationId);
		}

		if (mappedMarc.physical()) {
			String materialTypeId = Constants.MATERIAL_TYPES_MAP.get(config.materialType);
			newCompositePurchaseOrder.setMaterialTypeOnPoLines(materialTypeId);
		}
	}

	public static void importInvoice(
							   String poNumber,
							   UUID orderLineUUID,
							   String vendorId,
							   MarcRecordMapping marc,
	               Config config) throws Exception {

		JSONObject invoice = JsonObjectBuilder.createInvoiceJson(poNumber, vendorId, marc, config);
		JSONObject invoiceLine = JsonObjectBuilder.createInvoiceLineJson(orderLineUUID, marc, invoice);
		logger.info(
						FolioAccess.callApiPostWithUtf8("invoice/invoices", invoice)
										.toString());
		logger.info(
						FolioAccess.callApiPostWithUtf8("invoice/invoice-lines", invoiceLine)
										.toString());
	}

	public void setMyContext(ServletContext myContext) {
		this.myContext = myContext;
	}

}
