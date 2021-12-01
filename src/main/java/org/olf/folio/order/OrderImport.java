package org.olf.folio.order;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.UUID;
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
import org.olf.folio.order.dataobjects.InstanceIdentifier;
import org.olf.folio.order.dataobjects.Item;
import org.olf.folio.order.dataobjects.Link;
import org.olf.folio.order.dataobjects.Note;
import org.olf.folio.order.recordvalidation.RecordChecker;
import org.olf.folio.order.recordvalidation.RecordResult;
import org.olf.folio.order.recordvalidation.ServiceResponse;
import org.olf.folio.order.storage.FolioAccess;
import org.olf.folio.order.storage.FolioData;

public class OrderImport {

	private static final Logger logger = Logger.getLogger("OrderImport");

	public  JSONObject upload(String fileName, boolean analyze) throws Exception {

		logger.info("...starting...");
		FolioAccess.initialize(logger);

		//GET THE UPLOADED FILE, EXIT IF NONE PROVIDED
		if (fileName == null) {
			JSONObject responseMessage = new JSONObject();
			responseMessage.put("error", "no input file provided");
			responseMessage.put("PONumber", "~error~");
			return responseMessage;
		}

		// If analyze-only or analyze-first
		if (analyze || Config.onValidationErrorsCancelAll) {
			return RecordChecker.validateMarcRecords(fileName);
		}

		InputStream in = new FileInputStream(Config.uploadFilePath + fileName);
		MarcReader reader = new MarcStreamReader(in);
		ServiceResponse validationAndImportOutcomes =  new ServiceResponse(true);
		while (reader.hasNext()) {
			RecordResult outcome = validationAndImportOutcomes.nextResult();
			try {
				Record record = reader.next();
				MarcRecordMapping mappedMarc = new MarcRecordMapping(record);
  			RecordChecker.validateMarcRecord(mappedMarc, outcome);
				if (!outcome.isSkipped()) {
					// RECORD VALIDATION PASSED OR SERVICE IS CONFIGURED TO ATTEMPT IMPORT IN ANY CASE

					// CREATE AND POST THE PURCHASE ORDER AND LINE
					CompositePurchaseOrder importedPo = importPurchaseOrderAndNote(mappedMarc, outcome);

					// FETCH, UPDATE, AND PUT SELECT INVENTORY INSTANCE/HOLDINGS RECORD/ITEM
					updateInventory(importedPo.getInstanceId(), mappedMarc, outcome);

					// IMPORT INVOICE IF CONFIGURED FOR IT AND PRESENT
					maybeImportInvoice(importedPo, mappedMarc);
				}
			} catch (JSONException je) {
				outcome.setImportError("Application error. Unexpected error occurred in the MARC parsing logic: " + je.getMessage());
			} catch (NullPointerException npe) {
				outcome.setImportError("Application error. Null pointer encountered. " + npe.getMessage());
			}	catch(Exception e) {
				outcome.setImportError(e.getMessage() + " " + e.getCause());
			}
		}
		return validationAndImportOutcomes.toJson();
	}

	private CompositePurchaseOrder importPurchaseOrderAndNote(MarcRecordMapping mappedMarc, RecordResult outcome)
					throws Exception {
		CompositePurchaseOrder compositePo = CompositePurchaseOrder.fromMarcRecord(mappedMarc);
		setPreconfiguredValues(compositePo, mappedMarc);
		logger.info("We created the Purchase Order JSON from MARC etc.");

		FolioAccess.callApiPostWithUtf8(FolioData.COMPOSITE_ORDERS_PATH, compositePo);
		logger.info("We posted the Purchase Order JSON to FOLIO.");
		outcome.setPoNumber(compositePo.getPoNumber())
						.setPoUiUrl(Config.folioUiUrl, Config.folioUiOrdersPath,
						compositePo.getId(), compositePo.getPoNumber());

		//INSERT A NOTE IF THERE IS ONE IN THE MARC RECORD
		if (mappedMarc.hasNotes() && compositePo.hasPoLines() && Config.noteTypeName != null) {
			Note note = new Note().addLink(new Link().putType(Link.V_PO_LINE)
							.putId(compositePo.getCompositePoLines().get(0).getId()))
							.putTypeId(FolioData.getNoteTypeIdByName(Config.noteTypeName))
							.putDomain(Note.V_ORDERS)
							.putContent(mappedMarc.notes())
							.putTitle(mappedMarc.notes());
			logger.info("We created a Note JSON.");
			FolioAccess.callApiPostWithUtf8(FolioData.NOTES_PATH, note.asJson());
			logger.info("We posted the Note JSON to FOLIO.");
		}

		// GET THE UPDATED PURCHASE ORDER FROM FOLIO (TO RETRIEVE THE INSTANCE ID FOR INVENTORY UPDATES)
		return CompositePurchaseOrder.fromJson(
						FolioAccess.callApiGetById(FolioData.COMPOSITE_ORDERS_PATH, compositePo.getId()));
	}

	private void setPreconfiguredValues(CompositePurchaseOrder compositePo, MarcRecordMapping mappedMarc)
					throws Exception {

		String permLocationName = (Config.importInvoice && mappedMarc.hasInvoice()
					? Config.permLocationWithInvoiceImport : Config.permLocationName);
		String permELocationName = (Config.importInvoice && mappedMarc.hasInvoice()
					? Config.permELocationWithInvoiceImport : Config.permELocationName);
		String permLocationId = FolioData.getLocationIdByName(permLocationName);
		String permELocationId = FolioData.getLocationIdByName(permELocationName);

		if (mappedMarc.electronic()) {
			compositePo.setLocationIdOnPoLines(permELocationId);
		} else {
			compositePo.setLocationIdOnPoLines(permLocationId);
		}

		if (mappedMarc.physical()) {
			String materialTypeId = Constants.MATERIAL_TYPES_MAP.get(Config.materialType);
			compositePo.setMaterialTypeOnPoLines(materialTypeId);
		}
	}

	private void updateInventory(String instanceId, MarcRecordMapping mappedMarc, RecordResult outcome)
					throws Exception {
		// RETRIEVE, UPDATE, AND PUT THE RELATED INSTANCE
		Instance fetchedInstance = Instance.fromJson(
						FolioAccess.callApiGetById(FolioData.INSTANCES_PATH, instanceId));
		logger.info("We fetched the linked Instance from FOLIO.");
		fetchedInstance.putTitle(mappedMarc.title())
						.putSource(Instance.V_FOLIO)
						.putInstanceTypeId(FolioData.getInstanceTypeId("text"))
						.putIdentifiers(InstanceIdentifier.createInstanceIdentifiersFromMarc(mappedMarc))
						.putContributors(mappedMarc.getContributorsForInstance())
						.putDiscoverySuppress(false)
						.putElectronicAccess(mappedMarc.getElectronicAccess(Config.textForElectronicResources))
						.putNatureOfContentTermIds(new JSONArray())
						.putPrecedingTitles(new JSONArray())
						.putSucceedingTitles(new JSONArray());
		logger.info("We updated the fetched Instance JSON.");
		FolioAccess.callApiPut(FolioData.INSTANCES_PATH, fetchedInstance);
		logger.info("We posted the updated Instance JSON to FOLIO.");
		// END OF INSTANCE
		outcome.setInstanceHrid(fetchedInstance.getHrid())
						.setInstanceUiUrl(Config.folioUiUrl, Config.folioUiInventoryPath,
										fetchedInstance.getId(), fetchedInstance.getHrid());

		// RETRIEVE, UPDATE, AND PUT THE RELATED HOLDINGS RECORD
		HoldingsRecord fetchedHoldingsRecord = HoldingsRecord.fromJson(
						FolioAccess.callApiGetFirstObjectOfArray(
										FolioData.HOLDINGS_STORAGE_PATH + "?query=(instanceId==" + fetchedInstance.getId() + ")",
										FolioData.HOLDINGS_RECORDS_ARRAY));
		logger.info("We fetched the related Holdings Record from FOLIO.");
		fetchedHoldingsRecord.putElectronicAccess(mappedMarc.getElectronicAccess(Config.textForElectronicResources));
		if (mappedMarc.electronic()) {
			fetchedHoldingsRecord.putHoldingsTypeId(FolioData.getHoldingsTypeIdByName("Electronic"));
			if (mappedMarc.hasDonor()) {
				fetchedHoldingsRecord.addBookplateNote(
								BookplateNote.createElectronicBookplateNote(mappedMarc.donor()));
			}
		}
		logger.info("We updated the fetched Holdings Record JSON.");
		FolioAccess.callApiPut(FolioData.HOLDINGS_STORAGE_PATH, fetchedHoldingsRecord);
		logger.info("We put the updated Holdings Record JSON to FOLIO.");
		// END OF HOLDINGS RECORD

		// RETRIEVE, UPDATE, AND PUT THE RELATED ITEM IF THE MARC RECORD HAS A DONOR
		if (!mappedMarc.electronic() && mappedMarc.hasDonor()) {
			//IF PHYSICAL RESOURCE WITH DONOR INFO, GET THE ITEM FOLIO CREATED, SO WE CAN ADD NOTE ABOUT DONOR
			Item fetchedItem =
							Item.fromJson(
											FolioAccess.callApiGetFirstObjectOfArray(
															FolioData.ITEMS_PATH + "?query=(holdingsRecordId=="
																			+ fetchedHoldingsRecord.getId() + ")",
															FolioData.ITEMS_ARRAY));
			logger.info("We fetched the related Item from FOLIO.");
			fetchedItem.addBookplateNote(BookplateNote.createPhysicalBookplateNote(mappedMarc.donor()));
			logger.info("We updated the fetched Item JSON.");
			FolioAccess.callApiPut(FolioData.ITEMS_PATH, fetchedItem);
			logger.info("We put the updated Item JSON to FOLIO.");
		}
	}

	public static void maybeImportInvoice(
							   CompositePurchaseOrder po,
							   MarcRecordMapping marc) throws Exception {

		if (Config.importInvoice && marc.hasInvoice()) {
			UUID orderLineUUID = UUID.fromString(po.getCompositePoLines().get(0).getId());
			JSONObject invoice = JsonObjectBuilder.createInvoiceJson(po.getPoNumber(), marc);
			JSONObject invoiceLine = JsonObjectBuilder.createInvoiceLineJson(orderLineUUID, marc, invoice);
			logger.info(FolioAccess.callApiPostWithUtf8("invoice/invoices", invoice).toString());
			logger.info(FolioAccess.callApiPostWithUtf8("invoice/invoice-lines", invoiceLine).toString());
		}
	}

}
