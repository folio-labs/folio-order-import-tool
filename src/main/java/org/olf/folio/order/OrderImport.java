package org.olf.folio.order;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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
import org.olf.folio.order.entities.orders.PoLine;
import org.olf.folio.order.importhistory.FileStorageHelper;
import org.olf.folio.order.mapping.MarcToFolio;
import org.olf.folio.order.misc.InvoiceBuilder;
import org.olf.folio.order.validation.RecordChecker;
import org.olf.folio.order.importhistory.RecordResult;
import org.olf.folio.order.importhistory.Results;
import org.olf.folio.order.folioapis.FolioAccess;
import org.olf.folio.order.folioapis.FolioData;


public class OrderImport {

	private static final Logger logger = Logger.getLogger(OrderImport.class);

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

		if (Config.createOnePurchaseOrderPerFile) {
			CompositePurchaseOrder multilinePo = CompositePurchaseOrder.initiateEmptyOrder();
			ImportDataPark dataPark = new ImportDataPark(multilinePo.getId());
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
						dataPark.addLine(poLine.getId(), mappedMarc, outcome);
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
			if (!results.hasValidationErrors()) {
				CompositePurchaseOrder importedPo;
				try {
					importedPo = importPurchaseOrder(multilinePo);
				}
				catch (Exception e) {
					results.setFatalError(String.format("Failed to import purchase order: %s", e.getMessage()));
					return results.markEndedWithError();
				}

				try {
					for (PoLine line : importedPo.getCompositePoLines()) {
						dataPark.putInstanceId(line.getId(), line.getInstanceId());
					}
				}
				catch (NullPointerException npe) {
					results.setFatalError(String.format("Internal error in the import tool: %s", npe.getMessage()));
					npe.printStackTrace();
					return results.markEndedWithError();
				}

				for (ImportDataPark.PoLineData data : dataPark.getDataLines()) {
					try {
						updateInventory(data.instanceId, data.mappedMarc, data.recordResult);
						importNotesIfAny(data.mappedMarc, data.poLineId);
						data.recordResult.setPoNumber(importedPo.getPoNumber()).setPoUiUrl(Config.folioUiUrl,
										Config.folioUiOrdersPath, importedPo.getId(), importedPo.getPoNumber());
					}
					catch (JSONException je) {
						data.recordResult.setImportError(
										"Application error. Unexpected error occurred in the MARC parsing logic: " + je.getMessage());
					}
					catch (NullPointerException npe) {
						data.recordResult.setImportError(
										"Application error. Null pointer encountered. " + npe.getMessage() + Arrays.toString(
														npe.getStackTrace()));
					}
					catch (Exception e) {
						data.recordResult.setImportError(e.getMessage() + ( e.getCause() != null ? " " + e.getCause() : "" ));
					}
				}
			}
			fileStore.storeResults(results);
		} else {
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
						updateInventory(importedPo.getInstanceId(), mappedMarc, outcome);

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
		return results.markDone();
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
			if (item.isEmpty()) {
				outcome.setImportError(
								String.format("Warning: The tool attempted to find and update an Item " +
												"but no Item was found for holdingsRecordId (%s)", holdingsRecord.getId()));
			} else {
				mappedMarc.populateItem(item);
				FolioAccess.callApiPut(FolioData.ITEMS_PATH, item);
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

	static class ImportDataPark {
		String poId;
		Map<String,PoLineData> map = new HashMap<>();

		ImportDataPark(String purchaseOrderId) {
			poId = purchaseOrderId;
		}

		void addLine(String poLineId, MarcToFolio mappedMarc, RecordResult outcome) {
			map.put(poLineId, new PoLineData(poLineId, mappedMarc, outcome));
		}

		void putInstanceId(String poLineId, String instanceId) {
			if (map.containsKey(poLineId)) {
				map.get(poLineId).putInstanceId(instanceId);
			} else {
				logger.error(String.format("ImportDataPark has no entry for PO line ID [%s]: %s", poLineId, map.keySet()));
			}
		}

		Collection<PoLineData> getDataLines () {
			return map.values();
		}

		static class PoLineData {
			String poLineId;
			MarcToFolio mappedMarc;
			RecordResult recordResult;
			String instanceId;

			PoLineData (String poLineId, MarcToFolio mappedMarc, RecordResult recordResult) {
				this.poLineId = poLineId;
				this.mappedMarc = mappedMarc;
				this.recordResult = recordResult;
			}

			void putInstanceId (String instanceId) {
				this.instanceId = instanceId;
			}
		}
	}
}
