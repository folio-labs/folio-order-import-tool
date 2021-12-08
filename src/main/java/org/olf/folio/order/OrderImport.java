package org.olf.folio.order;

import java.io.FileNotFoundException;
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
import org.olf.folio.order.imports.FileStorageHelper;
import org.olf.folio.order.validation.RecordChecker;
import org.olf.folio.order.imports.RecordResult;
import org.olf.folio.order.imports.Results;
import org.olf.folio.order.storage.FolioAccess;
import org.olf.folio.order.storage.FolioData;

public class OrderImport {

	private static final Logger logger = Logger.getLogger("OrderImport");

	public Results runAnalyzeJob(FileStorageHelper fileStore) throws Exception {
		logger.info("...starting...");
		FolioAccess.initialize(logger);

		//GET THE UPLOADED FILE, EXIT IF NONE PROVIDED
		if (fileStore.fullPathToMarcFile() == null) {
			return new Results(false, fileStore)
							.setFatalError("No input MARC file provided")
							.markEndedWithError();
		}
		return RecordChecker.validateMarcRecords(fileStore);
	}

	public Results runImportJob(FileStorageHelper fileStore, Results results) throws Exception {

		logger.info("...starting...");
		FolioAccess.initialize(logger);

		//GET THE UPLOADED FILE, EXIT IF NONE PROVIDED
		if (fileStore.fullPathToMarcFile() == null) {
			return new Results( true, fileStore)
							.setFatalError("No input MARC file provided")
							.markEndedWithError();
		}

		// If analyze-only or analyze-first
		if (Config.onValidationErrorsCancelAll) {
			Results validationResults = RecordChecker.validateMarcRecords(fileStore);
			if (validationResults.hasValidationErrors()) {
				return validationResults;
			}
		}

		MarcReader reader = new MarcStreamReader(fileStore.getMarcInputStream());
		results.markPartial();
		while (reader.hasNext()) {
			RecordResult outcome = results.nextResult();
			try {
				Record record = reader.next();
				MarcRecordMapping mappedMarc = new MarcRecordMapping(record);
  			RecordChecker.validateMarcRecord(mappedMarc, outcome);
				if (!outcome.isSkipped()) {
					// RECORD VALIDATION PASSED OR SERVICE IS CONFIGURED TO ATTEMPT IMPORT IN ANY CASE

					// CREATE AND POST THE PURCHASE ORDER AND LINE
					CompositePurchaseOrder importedPo = importPurchaseOrderAndNote(mappedMarc, outcome);

					outcome.setPoNumber(importedPo.getPoNumber())
									.setPoUiUrl(Config.folioUiUrl, Config.folioUiOrdersPath,
													importedPo.getId(), importedPo.getPoNumber());

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
				outcome.setImportError(e.getMessage() + (e.getCause() != null ? " " + e.getCause() : ""));
			}
			fileStore.storeResults(results);
		}
		return results.markDone();
	}

	private CompositePurchaseOrder importPurchaseOrderAndNote(MarcRecordMapping mappedMarc, RecordResult outcome)
					throws Exception {
		CompositePurchaseOrder compositePo = CompositePurchaseOrder.fromMarcRecord(mappedMarc);

		JSONObject persistedPo = FolioAccess.callApiPostWithUtf8(FolioData.COMPOSITE_ORDERS_PATH, compositePo);

		//INSERT A NOTE IF THERE IS ONE IN THE MARC RECORD
		if (mappedMarc.hasNotes() && compositePo.hasPoLines() && Config.noteTypeName != null) {
			Note note = new Note().addLink(new Link().putType(Link.V_PO_LINE)
							.putId(compositePo.getCompositePoLines().get(0).getId()))
							.putTypeId(FolioData.getNoteTypeIdByName(Config.noteTypeName))
							.putDomain(Note.V_ORDERS)
							.putContent(mappedMarc.notes())
							.putTitle(mappedMarc.notes());
			FolioAccess.callApiPostWithUtf8(FolioData.NOTES_PATH, note.asJson());
		}

		return CompositePurchaseOrder.fromJson(persistedPo);
	}

	private void updateInventory(String instanceId, MarcRecordMapping mappedMarc, RecordResult outcome)
					throws Exception {
		// RETRIEVE, UPDATE, AND PUT THE RELATED INSTANCE
		Instance fetchedInstance = Instance.fromJson(
						FolioAccess.callApiGetById(FolioData.INSTANCES_PATH, instanceId));
		if (fetchedInstance.getSource().equals(Instance.V_MARC)) {
			String feedback = String.format("Purchase order and line were created and linked to records in " +
											"Inventory but cannot update the Instance %s because it has 'source' set to %s",
							fetchedInstance.getHrid(), fetchedInstance.getSource());

			outcome.setFlagIfNotNull(feedback)
							.setInstanceHrid(fetchedInstance.getHrid())
							.setInstanceUiUrl(Config.folioUiUrl, Config.folioUiInventoryPath,
											fetchedInstance.getId(), fetchedInstance.getHrid());

			return;
		}

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
		FolioAccess.callApiPut(FolioData.INSTANCES_PATH, fetchedInstance);
		// END OF INSTANCE
		outcome.setInstanceHrid(fetchedInstance.getHrid())
						.setInstanceUiUrl(Config.folioUiUrl, Config.folioUiInventoryPath,
										fetchedInstance.getId(), fetchedInstance.getHrid());

		// RETRIEVE, UPDATE, AND PUT THE RELATED HOLDINGS RECORD
		HoldingsRecord fetchedHoldingsRecord = HoldingsRecord.fromJson(
						FolioAccess.callApiGetFirstObjectOfArray(
										FolioData.HOLDINGS_STORAGE_PATH + "?query=(instanceId==" + fetchedInstance.getId() + ")",
										FolioData.HOLDINGS_RECORDS_ARRAY));
		fetchedHoldingsRecord.putElectronicAccess(mappedMarc.getElectronicAccess(Config.textForElectronicResources));
		if (mappedMarc.electronic()) {
			fetchedHoldingsRecord.putHoldingsTypeId(FolioData.getHoldingsTypeIdByName("Electronic"));
			if (mappedMarc.hasDonor()) {
				fetchedHoldingsRecord.addBookplateNote(
								BookplateNote.createElectronicBookplateNote(mappedMarc.donor()));
			}
		}
		FolioAccess.callApiPut(FolioData.HOLDINGS_STORAGE_PATH, fetchedHoldingsRecord);
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
			fetchedItem.addBookplateNote(BookplateNote.createPhysicalBookplateNote(mappedMarc.donor()));
			FolioAccess.callApiPut(FolioData.ITEMS_PATH, fetchedItem);
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

	public static int countMarcRecords (FileStorageHelper store) throws FileNotFoundException {
		MarcStreamReader reader = new MarcStreamReader(store.getMarcInputStream());
		int records = 0;
		while (reader.hasNext()) {
			reader.next();
			records++;
		}
		return records;
	}
}
