package org.olf.folio.order;

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import org.json.JSONArray;
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
import org.olf.folio.order.entities.orders.PoLine;
import org.olf.folio.order.importhistory.FileStorageHelper;
import org.olf.folio.order.mapping.MarcToFolio;
import org.olf.folio.order.misc.InvoiceBuilder;
import org.olf.folio.order.validation.RecordChecker;
import org.olf.folio.order.importhistory.RecordResult;
import org.olf.folio.order.importhistory.Results;
import org.olf.folio.order.folioapis.FolioAccess;
import org.olf.folio.order.folioapis.FolioData;

import static org.olf.folio.order.folioapis.FolioData.encode;


public class OrderImport {

	private static final Logger logger = Logger.getLogger(OrderImport.class);
	private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	static {
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

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

		MarcReader marcReader = new MarcStreamReader(fileStore.getMarcInputStream());
		results.markPartial();

		if (Config.createOnePurchaseOrderPerFile) {
			importOnePurchaseOrderForEntireFile(marcReader, results, fileStore);
		} else {
			importOnePurchaseOrderPerMarcRecord(marcReader, results, fileStore);
		}
		return results.markDone();
	}

	private void importOnePurchaseOrderPerMarcRecord(MarcReader reader, Results results, FileStorageHelper fileStore) throws Exception {
		while (reader.hasNext()) {
			RecordResult outcome = results.nextResult();
			try {
				Record record = reader.next();
				MarcToFolio mappedMarc = Config.getMarcMapping(record);
				RecordChecker.validateMarcRecord(mappedMarc, outcome);
				if (!outcome.isSkipped()) {
					// RECORD VALIDATION PASSED OR SERVICE IS CONFIGURED TO ATTEMPT IMPORT IN ANY CASE

					// CREATE PURCHASE ORDER AND LINE
					CompositePurchaseOrder compositePo = CompositePurchaseOrder.fromMarcRecord(mappedMarc);
					// IMPORT THE CREATED PO
					CompositePurchaseOrder importedPo = importPurchaseOrder(compositePo);

					//INSERT A NOTE IF THERE IS ONE IN THE MARC RECORD
					String poLineId = compositePo.getFirstPoLineId();
					importNotesIfAny(mappedMarc, poLineId);

					outcome.setPoNumber(importedPo.getPoNumber())
								.setPoUiUrl(Config.folioUiUrl, Config.folioUiOrdersPath,
												importedPo.getId(), importedPo.getPoNumber());

					// FETCH, UPDATE, AND PUT SELECT INVENTORY INSTANCE/HOLDINGS RECORD/ITEM
					updateInventory(importedPo.getFirstPoLine(), mappedMarc, outcome);

					// IMPORT INVOICE IF CONFIGURED FOR IT AND PRESENT
					maybeImportInvoice(importedPo, mappedMarc);
				}

			}
			catch (JSONException je) {
				outcome.setImportError(
								"Application error. Unexpected error occurred in the MARC parsing logic: " + je.getMessage());
			}
			catch (NullPointerException npe) {
				outcome.setImportError(
								"Application error. Null pointer encountered. " + npe.getMessage() + Arrays.toString(
												npe.getStackTrace()));
			}
			catch (Exception e) {
				outcome.setImportError(e.getMessage() + ( e.getCause() != null ? " " + e.getCause() : "" ));
			}
			fileStore.storeResults(results);
		}
	}

	private void importOnePurchaseOrderForEntireFile(MarcReader reader, Results results, FileStorageHelper fileStore) throws Exception {
		CompositePurchaseOrder multilinePo = CompositePurchaseOrder.initiateEmptyOrder();
		PurchaseOrderDataShelf poDataShelf = new PurchaseOrderDataShelf(multilinePo.getId());
		while (reader.hasNext()) {
			RecordResult outcome = results.nextResult();
			Record record = reader.next();
			try {
				MarcToFolio mappedMarc = Config.getMarcMapping(record);
				RecordChecker.validateMarcRecord(mappedMarc, outcome);
				if (!outcome.isSkipped()) {
					if (!multilinePo.hasPoLines()) {
						// Set PO vendor from first PO line
						multilinePo.putVendor(mappedMarc.vendorUuid());
					}
					PoLine poLine = PoLine.fromMarcRecord(multilinePo.getId(), mappedMarc);
					multilinePo.addPoLine(poLine);
					poDataShelf.putPoLineFolderOnShelf(poLine.getId(), mappedMarc, outcome);
				}
			} catch (JSONException je) {
				outcome.setImportError(
								"Application error. Unexpected error occurred in the MARC parsing logic: " + je.getMessage());
			}
			catch (NullPointerException npe) {
				outcome.setImportError(
								"Application error. Null pointer encountered. " + npe.getMessage() + Arrays.toString(
												npe.getStackTrace()));
			}
			catch (Exception e) {
				outcome.setImportError(e.getMessage() + ( e.getCause() != null ? " " + e.getCause() : "" ));
			}
		}

		CompositePurchaseOrder importedPo = null;
		if (!results.hasValidationErrors()) {
			try {
				importedPo = importPurchaseOrder(multilinePo);
			}
			catch (Exception e) {
				results.setFatalError(String.format("Failed to import purchase order: %s", e.getMessage()));
				results.markEndedWithError();
			}

			if (! (importedPo == null)) {
				try {
					for (PoLine line : importedPo.getCompositePoLines()) {
						poDataShelf.putPoLineResponseInFolder(line.getId(), line);
					}
				}
				catch (NullPointerException npe) {
					results.setFatalError(String.format("Internal error in the import tool: %s", npe.getMessage()));
					npe.printStackTrace();
					results.markEndedWithError();
				}

				if (!results.hasFatalError()) {
					for (PurchaseOrderDataShelf.PoLineFolder poLineFolder : poDataShelf.getPoLineFolder()) {
						try {
							updateInventory(poLineFolder.poLine, poLineFolder.mappedMarc, poLineFolder.recordResult);
							importNotesIfAny(poLineFolder.mappedMarc, poLineFolder.poLineId);
							poLineFolder.recordResult.setPoNumber(importedPo.getPoNumber()).setPoUiUrl(Config.folioUiUrl,
											Config.folioUiOrdersPath, importedPo.getId(), importedPo.getPoNumber());
						}
						catch (JSONException je) {
							poLineFolder.recordResult.setImportError(
											"Application error. Unexpected error occurred in the MARC parsing logic: " + je.getMessage());
						}
						catch (NullPointerException npe) {
							poLineFolder.recordResult.setImportError("Application error. Null pointer encountered. " + npe.getMessage() + Arrays.toString(
											npe.getStackTrace()));
						}
						catch (Exception e) {
							poLineFolder.recordResult.setImportError(e.getMessage() + ( e.getCause() != null ? " " + e.getCause() : "" ));
						}
					}
				}
			}
		}
		fileStore.storeResults(results);
	}

	private CompositePurchaseOrder importPurchaseOrder(CompositePurchaseOrder compositePurchaseOrder)
					throws Exception {
		logger.debug(String.format("POSTing purchase order [%s]", compositePurchaseOrder.asJson().toString(2)));
		JSONObject persistedPo = FolioAccess.callApiPostWithUtf8(FolioData.COMPOSITE_ORDERS_PATH, compositePurchaseOrder);
		logger.debug(String.format("Response on POSTing of purchase order: [%s]", persistedPo.toString(2)));
		return CompositePurchaseOrder.fromJson(persistedPo);
	}

	private void importNotesIfAny(MarcToFolio mappedMarc, String poLineId) throws Exception {
		if (mappedMarc.hasNotes() && Config.noteTypeName != null && poLineId != null) {
			Note note = new Note().addLink(new Link().putType(Link.V_PO_LINE).putId(poLineId)).putTypeId(
							FolioData.getNoteTypeIdByName(Config.noteTypeName)).putDomain(Note.V_ORDERS).putContent(mappedMarc.notes()).putTitle(mappedMarc.notes());
			FolioAccess.callApiPostWithUtf8(FolioData.NOTES_PATH, note.asJson());
		}
	}

	private void updateInventory(PoLine poLine, MarcToFolio mappedMarc, RecordResult outcome)
					throws Exception {
		// RETRIEVE, UPDATE, AND PUT THE RELATED INSTANCE
		Instance instance = Instance.fromJson(
						FolioAccess.callApiGetById(FolioData.INSTANCES_PATH, poLine.getInstanceId()));
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

		List<String> generatedHoldings = poLine.getHoldingsRecordIds();
		for (String holdingsRecordId : generatedHoldings) {
			HoldingsRecord holdingsRecord = HoldingsRecord.fromJson(FolioAccess.callApiGetById(FolioData.HOLDINGS_STORAGE_PATH,holdingsRecordId));
			mappedMarc.populateHoldingsRecord(holdingsRecord);
			FolioAccess.callApiPut(FolioData.HOLDINGS_STORAGE_PATH, holdingsRecord);
			if (mappedMarc.updateItem()) {
				String query = String.format("(holdingsRecordId==\"%s\" AND metadata.createdDate>=\"%s\")",
								holdingsRecord.getId(),
								getFormattedTimeSomeMinutesAgo(5)
								);
				JSONArray items = FolioAccess.callApiGetArray(FolioData.ITEMS_PATH + "?query="+encode(query),	FolioData.ITEMS_ARRAY);
				logger.debug("Found following recently created items on the holdings record: " + items.toString(2));
				for (Object itemObject : items) {
					Item item = Item.fromJson((JSONObject) itemObject);
					if (item.isEmpty()) {
						outcome.setImportError(String.format(
										"Warning: The tool attempted to find and update an Item but no recently created items were found for holdingsRecordId (%s)",
										holdingsRecord.getId()));
					} else {
						mappedMarc.populateItem(item);
						FolioAccess.callApiPut(FolioData.ITEMS_PATH, item);
					}
				}
			} else {
				logger.debug("Not updating item; does not apply.");
			}
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

	/**
	 * Helper classes for caching import data: A 'shelf' for the entire purchase order,
	 * holding a 'folder' for each PO line, containing the source data, the imported data,
	 * and the result feedback.
	 */
	static class PurchaseOrderDataShelf {
		String poId;
		Map<String, PoLineFolder> map = new HashMap<>();

		PurchaseOrderDataShelf(String purchaseOrderId) {
			poId = purchaseOrderId;
		}

		void putPoLineFolderOnShelf(String poLineId, MarcToFolio mappedMarc, RecordResult outcome) {
			map.put(poLineId, new PoLineFolder(poLineId, mappedMarc, outcome));
		}

		void putPoLineResponseInFolder(String poLineId, PoLine poLine) {
			if (map.containsKey(poLineId)) {
				map.get(poLineId).putPoLine(poLine);
			} else {
				logger.error(String.format("ImportDataPark has no entry for PO line ID [%s]: %s", poLineId, map.keySet()));
			}
		}

		Collection<PoLineFolder> getPoLineFolder() {
			return map.values();
		}

		static class PoLineFolder {
			String poLineId;
			MarcToFolio mappedMarc;
			RecordResult recordResult;
			PoLine poLine;

			PoLineFolder(String poLineId, MarcToFolio mappedMarc, RecordResult recordResult) {
				this.poLineId = poLineId;
				this.mappedMarc = mappedMarc;
				this.recordResult = recordResult;
			}

			void putPoLine (PoLine poLine) {
				this.poLine = poLine;
			}
		}
	}

	private static String getFormattedTimeSomeMinutesAgo(int minutesAgo) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.MINUTE,-minutesAgo);
		return dateFormatter.format(calendar.getTime());
	}


}
