package org.olf.folio.order;

import java.io.FileNotFoundException;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.marc.Record;
import org.apache.log4j.Logger;
import org.olf.folio.order.entities.orders.CompositePurchaseOrder;
import org.olf.folio.order.entities.inventory.HoldingsRecord;
import org.olf.folio.order.entities.inventory.Instance;
import org.olf.folio.order.entities.inventory.Item;
import org.olf.folio.order.entities.orders.Link;
import org.olf.folio.order.entities.orders.Note;
import org.olf.folio.order.importhistory.FileStorageHelper;
import org.olf.folio.order.mapping.MarcToFolio;
import org.olf.folio.order.misc.InvoiceBuilder;
import org.olf.folio.order.validation.RecordChecker;
import org.olf.folio.order.importhistory.RecordResult;
import org.olf.folio.order.importhistory.Results;
import org.olf.folio.order.folioapis.FolioAccess;
import org.olf.folio.order.folioapis.FolioData;

public class OrderImport {

	private static final Logger logger = Logger.getLogger("OrderImport");

	public Results runAnalyzeJob(FileStorageHelper fileStore) throws Exception {
		logger.info("...starting...");
		FolioAccess.initialize();

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
		FolioAccess.initialize();

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
				MarcToFolio mappedMarc = Config.getMarcMapping(record);
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

	private CompositePurchaseOrder importPurchaseOrderAndNote(MarcToFolio mappedMarc, RecordResult outcome)
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

	private void updateInventory(String instanceId, MarcToFolio mappedMarc, RecordResult outcome)
					throws Exception {
		// RETRIEVE, UPDATE, AND PUT THE RELATED INSTANCE
		Instance instance = Instance.fromJson(
						FolioAccess.callApiGetById(FolioData.INSTANCES_PATH, instanceId));
		if (instance.getSource().equals(Instance.V_MARC)) {
			String feedback =
							String.format("Purchase order and line were created and linked to records in " +
											"Inventory but cannot update the Instance %s because it has 'source' set to %s",
							        instance.getHrid(), instance.getSource());
			outcome.setFlagIfNotNull(feedback)
							.setInstanceHrid(instance.getHrid())
							.setInstanceUiUrl(Config.folioUiUrl, Config.folioUiInventoryPath,
											instance.getId(), instance.getHrid());
		} else {
			mappedMarc.populateInstance(instance);
			FolioAccess.callApiPut(FolioData.INSTANCES_PATH, instance);
			outcome.setInstanceHrid(instance.getHrid()).setInstanceUiUrl(Config.folioUiUrl, Config.folioUiInventoryPath, instance.getId(), instance.getHrid());
		}

		// RETRIEVE, UPDATE, AND PUT THE RELATED HOLDINGS RECORD
		HoldingsRecord holdingsRecord = HoldingsRecord.fromJson(
						FolioAccess.callApiGetFirstObjectOfArray(
										FolioData.HOLDINGS_STORAGE_PATH + "?query=(instanceId==" + instance.getId() + ")",
										FolioData.HOLDINGS_RECORDS_ARRAY));
    mappedMarc.populateHoldingsRecord(holdingsRecord);
		FolioAccess.callApiPut(FolioData.HOLDINGS_STORAGE_PATH, holdingsRecord);

		if (mappedMarc.updateItem()) {
			Item item =Item.fromJson(
											FolioAccess.callApiGetFirstObjectOfArray(
															FolioData.ITEMS_PATH + "?query=(holdingsRecordId=="
																			+ holdingsRecord.getId() + ")",
															FolioData.ITEMS_ARRAY));
			mappedMarc.populateItem(item);
			FolioAccess.callApiPut(FolioData.ITEMS_PATH, item);
		}
	}

	public static void maybeImportInvoice(
							   CompositePurchaseOrder po,
							   MarcToFolio marc) throws Exception {

		if (Config.importInvoice && marc.hasInvoice()) {
			UUID orderLineUUID = UUID.fromString(po.getCompositePoLines().get(0).getId());
			JSONObject invoice = InvoiceBuilder.createInvoiceJson(po.getPoNumber(), marc);
			JSONObject invoiceLine = InvoiceBuilder.createInvoiceLineJson(orderLineUUID, marc, invoice);
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
