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
import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcWriter;
import org.marc4j.converter.impl.AnselToUnicode;
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
import org.olf.folio.order.storage.SRSStorage;

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
		ByteArrayOutputStream byteArrayOutputStreamForSRS = null;

		//GET THE UPLOADED FILE, EXIT IF NONE PROVIDED
		if (fileName == null) {
			JSONObject responseMessage = new JSONObject();
			responseMessage.put("error", "no input file provided");
			responseMessage.put("PONumber", "~error~");
			responseMessages.put(responseMessage);
			return responseMessages;
		}

		if (analyze) {
			RecordChecker check = new RecordChecker(config);
			return check.validateMarcRecords(fileName);
		}

		InputStream in = new FileInputStream(config.uploadFilePath + fileName);
		MarcReader reader = new MarcStreamReader(in);
		ProcessCounters counters = new ProcessCounters();
		while (reader.hasNext()) {
			JSONObject responseMessage = new JSONObject();
			try {
				Record record = reader.next();
				counters.recordsProcessed++;
				responseMessage.put("source", record.toString());
				MarcRecordMapping mappedMarc = new MarcRecordMapping(record);

				CompositePurchaseOrder compositePo = CompositePurchaseOrder.fromMarcRecord(mappedMarc);
				setPreconfiguredValues(compositePo, mappedMarc);

				//NOW WE CAN START CREATING THE PO!
				responseMessage.put("recNo", counters.recordsProcessed);
				responseMessage.put("title", mappedMarc.title());
	  		responseMessage.put("ISBN", mappedMarc.hasISBN() ? mappedMarc.getISBN() : "No ISBN in this record");

				// CREATE AND POST THE PURCHASE ORDER AND LINE
				FolioAccess.callApiPostWithUtf8(FolioData.COMPOSITE_ORDERS_PATH, compositePo);

				//INSERT THE NOTE IF THERE IS A NOTE IN THE MARC RECORD
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

				//GET THE UPDATED PURCHASE ORDER FROM THE API AND PULL OUT THE ID FOR THE INSTANCE FOLIO CREATED:
				CompositePurchaseOrder fetchedPo = CompositePurchaseOrder.fromJson(
								FolioAccess.callApiGetById(FolioData.COMPOSITE_ORDERS_PATH, compositePo.getId()));

				Instance fetchedInstance = Instance.fromJson(
												FolioAccess.callApiGetById(FolioData.INSTANCES_PATH, fetchedPo.getInstanceId()));

				if ( config.importSRS ) {
					if (byteArrayOutputStreamForSRS == null )
						byteArrayOutputStreamForSRS = getOutputStreamForSRS();
					SRSStorage.storeMarcToSRS(record,	byteArrayOutputStreamForSRS, fetchedPo.getInstanceId(),	fetchedInstance.getHrid());
				}

				fetchedInstance.putTitle(mappedMarc.title())
								.putSource(config.importSRS ? Instance.V_MARC : Instance.V_FOLIO)
								.putInstanceTypeId(FolioData.getInstanceTypeId("text"))
								.putIdentifiers(mappedMarc.getInstanceIdentifiers())
								.putContributors(mappedMarc.getContributorsForInstance())
								.putDiscoverySuppress(false)
								.putElectronicAccess(mappedMarc.getElectronicAccess())
								.putNatureOfContentTermIds(new JSONArray())
								.putPrecedingTitles(new JSONArray())
								.putSucceedingTitles(new JSONArray());

				FolioAccess.callApiPut( FolioData.INSTANCES_PATH,  fetchedInstance);

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

				if (!mappedMarc.electronic() && mappedMarc.hasDonor()) {
					//IF PHYSICAL RESOURCE WITH DONOR INFO, GET THE ITEM FOLIO CREATED, SO WE CAN ADD NOTE ABOUT DONOR
					Item fetchedItem = Item.fromJson(
									FolioAccess.callApiGetFirstObjectOfArray(
													FolioData.ITEMS_PATH + "?query=(holdingsRecordId==" + fetchedHoldingsRecord.getId() + ")", FolioData.ITEMS_ARRAY));
					fetchedItem.addBookplateNote(BookplateNote.createPhysicalBookplateNote(mappedMarc.donor()));
					FolioAccess.callApiPut( FolioData.ITEMS_PATH, fetchedItem);
				}

				if (config.importInvoice && mappedMarc.hasInvoice()) {
					importInvoice(
									fetchedPo.getPoNumber(),
									UUID.fromString(fetchedPo.getCompositePoLines().get(0).getId()),
									mappedMarc.vendorUuid(),
									mappedMarc,
									config);
				}

				//SAVE THE PO NUMBER FOR THE RESPONSE
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
		logger.info(counters.recordsProcessed + " record" + (counters.recordsProcessed == 1 ? "" : "s")
						+ " processed, "
						+ counters.recordsImported + " record" + (counters.recordsImported == 1 ? "" : "s")
		        + " imported, and "
						+ counters.recordsFailed + " record" + (counters.recordsFailed == 1 ? "" : "s")
						+ " failed.");
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


	public ServletContext getMyContext() {
		return myContext;
	}

	public void setMyContext(ServletContext myContext) {
		this.myContext = myContext;
	}

}
