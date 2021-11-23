package org.olf.folio.order;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.ServletContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcWriter;
import org.marc4j.converter.impl.AnselToUnicode;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.apache.log4j.Logger;
import org.marc4j.marc.VariableField;

public class OrderImport {

	private static final Logger logger = Logger.getLogger(OrderImport.class);
	private ServletContext myContext;
	private static Config config;

	public  JSONArray  upload(String fileName) throws Exception {

		logger.info("...starting...");
		if (config == null) {
			config = new Config(myContext);
		}
		Folio.initialize(config, logger);
		JSONArray responseMessages = new JSONArray();

		//GET THE UPLOADED FILE
		String filePath = (String) myContext.getAttribute("uploadFilePath");
		InputStream in;
		//MAKE SURE A FILE WAS UPLOADED
		if (fileName != null) {
			in = new FileInputStream(filePath + fileName);			
		}
		else {
			JSONObject responseMessage = new JSONObject();
			responseMessage.put("error", "no input file provided");
			responseMessage.put("PONumber", "~error~");
			responseMessages.put(responseMessage);
			return responseMessages;
		}

		//READ THE MARC RECORD FROM THE FILE AND VALIDATE IT
		//VALIDATES THE FUND CODE, TAG (OBJECT CODE
		MarcReader reader = new MarcStreamReader(in);
		Record record;

		JSONArray validateRequiredResult = validateRequiredValues(reader);
		if (!validateRequiredResult.isEmpty()) return validateRequiredResult;

		//READ THE MARC RECORD FROM THE FILE
		in = new FileInputStream(filePath + fileName);
		reader = new MarcStreamReader(in);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		MarcWriter w = new MarcStreamWriter(byteArrayOutputStream,"UTF-8");

		AnselToUnicode conv = new AnselToUnicode();
		w.setConverter(conv);

		while (reader.hasNext()) {
			try {
				record = reader.next();
				MarcRecord marc = new MarcRecord(record);

				config.permLocationName = (config.importInvoice && marc.hasInvoice() ? config.permLocationWithInvoiceImport : config.permLocationName);
				config.permELocationName = (config.importInvoice && marc.hasInvoice() ? config.permELocationWithInvoiceImport : config.permELocationName);

				//NOW WE CAN START CREATING THE PO!
				JSONObject responseMessage = new JSONObject();
				responseMessage.put("title", marc.title());

				// LOOK UP VENDOR
				String organizationEndpoint = "organizations-storage/organizations?limit=30&offset=0&query=((code='" + marc.vendorCode() + "'))";
				String orgLookupResponse = Folio.callApiGet(organizationEndpoint);
				JSONObject orgObject = new JSONObject(orgLookupResponse);
				String vendorId = (String) orgObject.getJSONArray("organizations").getJSONObject(0).get("id");
				// LOOK UP THE FUND
				String fundEndpoint =  "finance/funds?limit=30&offset=0&query=((code='" + marc.fundCode() + "'))";
				String fundResponse = Folio.callApiGet(fundEndpoint);
				JSONObject fundsObject = new JSONObject(fundResponse);
				String fundId = (String) fundsObject.getJSONArray("funds").getJSONObject(0).get("id");
				// LOOK UP ACCESS PROVIDER, FALL BACK to VENDOR
				organizationEndpoint = "organizations-storage/organizations?limit=30&offset=0&query=((code='" + marc.accessProviderCode() + "'))";
				orgLookupResponse = Folio.callApiGet(organizationEndpoint);
				orgObject = new JSONObject(orgLookupResponse);
				String accessProviderId;
				if (orgObject.getJSONArray("organizations") != null && !orgObject.getJSONArray("organizations").isEmpty()) {
					accessProviderId = (String) orgObject.getJSONArray("organizations").getJSONObject(0).get("id");
				} else {
					accessProviderId = vendorId;
				}
				String expenseClassId = null;
				if (marc.hasExpenseClassCode()) {
					String expenseClassEndpoint = "finance/expense-classes?limit=30&query=code='" + marc.expenseClassCode() + "'";
					String expenseClassResponse = Folio.callApiGet(expenseClassEndpoint);
					JSONObject expenseClassesObject = new JSONObject(expenseClassResponse);
					JSONArray expenseClasses = expenseClassesObject.getJSONArray("expenseClasses");
					if (expenseClasses != null && !expenseClasses.isEmpty()) {
						expenseClassId = (String) expenseClasses.getJSONObject(0).get("id");
					}
				}

				//GET THE NEXT PO NUMBER
				String poNumber = Folio.callApiGet("orders/po-number");
				JSONObject poNumberObj = new JSONObject(poNumber);
				logger.info("NEXT PO NUMBER: " + poNumberObj.get("poNumber"));

				// CREATING THE PURCHASE ORDER
				// GENERATE UUIDS FOR OBJECTS
				UUID orderUUID = UUID.randomUUID();
				JSONObject order = new JSONObject();
				order.put("poNumber", poNumberObj.get("poNumber"));
				order.put("vendor", vendorId);
				order.put("orderType", "One-Time");
				order.put("reEncumber", true); // Changed to true for UChicago
				order.put("id", orderUUID.toString());
				order.put("approved", true);
				order.put("workflowStatus","Open");

				// UC extension
				String addressId = (marc.hasBillTo() ? Folio.findAddressId(marc.billTo()) : null);
				if (addressId != null) order.put("billTo", addressId);

				// POST ORDER LINE
				//FOLIO WILL CREATE THE INSTANCE, HOLDINGS, ITEM (IF PHYSICAL ITEM)
				JSONObject orderLine = new JSONObject();
				JSONObject cost = new JSONObject();
				JSONObject location = new JSONObject();
				JSONArray locations = new JSONArray();
				JSONObject orderLineDetails = new JSONObject();
				JSONArray poLines = new JSONArray();
				if (marc.electronic()) {
					orderLine.put("orderFormat", "Electronic Resource");
					orderLine.put("receiptStatus", "Receipt Not Required");
					JSONObject eResource = new JSONObject();
					eResource.put("activated", false);
					eResource.put("createInventory", "Instance, Holding");
					if (marc.hasUserLimit())
						eResource.put("userLimit", marc.userLimit());
					eResource.put("trial", false);
					eResource.put("accessProvider", accessProviderId);
					orderLine.put("eresource",eResource);
					cost.put("quantityElectronic", 1);
					cost.put("listUnitPriceElectronic", marc.price());
					location.put("quantityElectronic",1);
					location.put("locationId", Folio.referenceDataByName.get(config.permELocationName + "-location"));
				}	else {
					JSONObject physical = new JSONObject();
					physical.put("createInventory", "Instance, Holding, Item");
					physical.put("materialType", getMaterialTypeId(config.materialType));
					orderLine.put("physical", physical);
					orderLine.put("orderFormat", "Physical Resource");
					cost.put("listUnitPrice", marc.price());
					cost.put("quantityPhysical", 1);
					location.put("quantityPhysical",1);
					location.put("locationId", Folio.referenceDataByName.get(config.permLocationName + "-location"));
				}
				locations.put(location);

				if (marc.hasReceivingNote()) {
					orderLineDetails.put("receivingNote", marc.receivingNote());
				}

				//VENDOR REFERENCE NUMBER IF INCLUDED IN THE MARC RECORD:
				if (marc.hasVendorItemId()) {
					JSONArray referenceNumbers = new JSONArray();
					JSONObject vendorDetail = new JSONObject();
					vendorDetail.put("instructions", "");
					vendorDetail.put("vendorAccount", (marc.hasVendorAccount() ? marc.vendorAccount() : ""));
					JSONObject referenceNumber = new JSONObject();
					referenceNumber.put("refNumber", marc.vendorItemId());
					referenceNumber.put("refNumberType",
									(marc.hasRefNumberType() ? marc.refNumberType() : "Vendor internal number"));
					referenceNumbers.put(referenceNumber);
					vendorDetail.put("referenceNumbers", referenceNumbers);
					orderLine.put("vendorDetail", vendorDetail);
				}
        // Tags
				JSONObject tags = new JSONObject();
				JSONArray tagList = new JSONArray();
				if (marc.hasObjectCode()) {
					tagList.put(marc.objectCode());
				}
				if (marc.hasProjectCode()) {
					tagList.put(marc.projectCode());
				}
				if (!tagList.isEmpty()) {
					tags.put("tagList", tagList);
					orderLine.put("tags", tags);
				}
        // Order line
				UUID orderLineUUID = UUID.randomUUID();
				orderLine.put("id", orderLineUUID);
				orderLine.put("source", "User");
				cost.put("currency", marc.currency());
				orderLine.put("cost", cost);
				orderLine.put("locations", locations);
				orderLine.put("titleOrPackage",marc.title());
				orderLine.put("acquisitionMethod", marc.acquisitionMethod());
				orderLine.put("rush", "RUSH".equalsIgnoreCase(marc.rush())); // UC extension
				if (marc.hasDescription())
					orderLine.put("description", marc.description()); // UC extension
				JSONArray funds = new JSONArray();
				JSONObject fundDist = new JSONObject();
				fundDist.put("distributionType", "percentage");
				fundDist.put("value", 100);
				fundDist.put("fundId", fundId);
				fundDist.put("code", marc.fundCode());
				if (expenseClassId != null)
					fundDist.put("expenseClassId", expenseClassId);
				funds.put(fundDist);
				orderLine.put("fundDistribution", funds);
				orderLine.put("purchaseOrderId", orderUUID.toString());
				poLines.put(orderLine);
				order.put("compositePoLines", poLines);

				if (marc.hasSelector())
					orderLine.put("selector", marc.selector());
				if (marc.hasDonor())
					orderLine.put("donor", marc.donor());

				orderLine.put("contributors", buildContributors(record, Folio.referenceDataByName, true));

				JSONArray productIds = Identifier.createProductIdentifiersJson( record, false,
						Constants.ISBN,
						Constants.ISSN,
						Constants.OTHER_STANDARD_IDENTIFIER,
						Constants.PUBLISHER_OR_DISTRIBUTOR_NUMBER );
				orderLineDetails.put("productIds", productIds);
				orderLine.put("details", orderLineDetails);

				DataField editionField = (DataField) record.getVariableField( "250" );
				if (editionField != null) {
					String edition = editionField.getSubfieldsAsString( "a" );
					if (edition != null && ! edition.isEmpty()) {
						orderLine.put("edition", edition);
					}
				}

				for (String tag : new String[] {"260", "264"} ) {
					if (record.getVariableField( tag ) != null) {
						DataField publishingField = (DataField) record.getVariableField( tag );
						String publisher = publishingField.getSubfieldsAsString( "b" );
						String publicationDate = publishingField.getSubfieldsAsString( "c" );
						if (publisher != null && ! publisher.isEmpty()) {
							orderLine.put("publisher", publisher);
						}
						if (publicationDate != null && !publicationDate.isEmpty()) {
							orderLine.put("publicationDate", publicationDate);
						}
						break;
					}
				}

				//POST THE ORDER AND LINE:
				String orderResponse = Folio.callApiPostWithUtf8("orders/composite-orders",order);
				JSONObject approvedOrder = new JSONObject(orderResponse);
				logger.info(orderResponse);

				//INSERT THE NOTE IF THERE IS A NOTE IN THE MARC RECORD
				if (marc.hasNotes()) {
					logger.info("NOTE TYPE NAME: " + config.noteTypeName);
					//logger.info(Folio.lookupTable);
					JSONObject noteAsJson = new JSONObject();
					JSONArray links = new JSONArray();
					JSONObject link = new JSONObject();
					link.put("type","poLine");
					link.put("id", orderLineUUID);
					links.put(link);
					noteAsJson.put("links", links);
					noteAsJson.put("typeId", Folio.referenceDataByName.get(config.noteTypeName));
					noteAsJson.put("domain", "orders");
					noteAsJson.put("content", marc.notes());
					noteAsJson.put("title", marc.notes());
					String noteResponse = Folio.callApiPostWithUtf8("/notes",noteAsJson);
					logger.info(noteResponse);
				}

				//GET THE UPDATED PURCHASE ORDER FROM THE API AND PULL OUT THE ID FOR THE INSTANCE FOLIO CREATED:
				String updatedPurchaseOrder = Folio.callApiGet("orders/composite-orders/" +orderUUID);
				JSONObject updatedPurchaseOrderJson = new JSONObject(updatedPurchaseOrder);
				String instanceId = updatedPurchaseOrderJson.getJSONArray("compositePoLines").getJSONObject(0).getString("instanceId");

				//GET THE INSTANCE RECORD FOLIO CREATED, SO WE CAN ADD BIB INFO TO IT:
				String instanceResponse = Folio.callApiGet("inventory/instances/" + instanceId);
				JSONObject instanceAsJson = new JSONObject(instanceResponse);
				String hrid = instanceAsJson.getString("hrid");

				// UChicago have asked that the MARC NOT be stored to SRS since this has implications for the ability to
				// batch update the instance record with the full cataloging when UChicago receive the invoice.
				if ( config.importSRS )
				{
					Folio.storeMarcToSRS(
							record,
							byteArrayOutputStream,
							UUID.randomUUID(), // snapshotId
							UUID.randomUUID(), // recordTableId
							instanceId,
							hrid );
				}

				//ADD IDENTIFIERS AND CONTRIBUTORS TO THE INSTANCE
				JSONArray identifiers = Identifier.createInstanceIdentifiersJson(record, true,
						Constants.ISBN,
						Constants.INVALID_ISBN,
						Constants.ISSN,
						Constants.INVALID_ISSN,
						Constants.LINKING_ISSN,
						Constants.OTHER_STANDARD_IDENTIFIER,
						Constants.PUBLISHER_OR_DISTRIBUTOR_NUMBER,
						Constants.SYSTEM_CONTROL_NUMBER);

				instanceAsJson.put("title", marc.title());
				instanceAsJson.put("source", config.importSRS ? "MARC" : "FOLIO");
				instanceAsJson.put("instanceTypeId", Folio.referenceDataByName.get("text"));
				instanceAsJson.put("identifiers", identifiers);
				instanceAsJson.put("contributors", buildContributors(record, Folio.referenceDataByName, false));
				instanceAsJson.put("discoverySuppress", false);

				//GET THE HOLDINGS RECORD FOLIO CREATED, SO WE CAN ADD URLs FROM THE 856 IN THE MARC RECORD
				String holdingResponse = Folio.callApiGet("holdings-storage/holdings?query=(instanceId==" + instanceId + ")");
				JSONObject holdingsAsJson = new JSONObject(holdingResponse);
				JSONObject holdingsRecord = holdingsAsJson.getJSONArray("holdingsRecords").getJSONObject(0);

				JSONArray eResources = new JSONArray();
				String linkText = (String) getMyContext().getAttribute("textForElectronicResources");
				List<VariableField> urls =  record.getVariableFields("856");
				Iterator<VariableField> iterator = urls.iterator();
				while (iterator.hasNext()) {
					DataField dataField = (DataField) iterator.next();
					if (dataField != null && dataField.getSubfield('u') != null) {
						String url = dataField.getSubfield('u').getData();
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
						eResource.put("relationshipId", "f5d0068e-6272-458e-8a81-b85e7b9a14aa");
						eResources.put(eResource);
					}
				}


				//UPDATE THE INSTANCE RECORD
				instanceAsJson.put("electronicAccess", eResources);
				instanceAsJson.put("natureOfContentTermIds", new JSONArray());
				instanceAsJson.put("precedingTitles", new JSONArray());
				instanceAsJson.put("succeedingTitles", new JSONArray());
				String instanceUpdateResponse = Folio.callApiPut( "inventory/instances/" + instanceId,  instanceAsJson);

				//UPDATE THE HOLDINGS RECORD
				holdingsRecord.put("electronicAccess", eResources);
				//IF THIS WAS AN ELECTRONIC RECORD, MARK THE HOLDING AS EHOLDING
				if (marc.electronic()) {
					holdingsRecord.put("holdingsTypeId", Folio.referenceDataByName.get("Electronic"));

					if (marc.hasDonor()) {
						JSONObject bookplateNote = new JSONObject();
						bookplateNote.put("holdingsNoteTypeId", Constants.HOLDINGS_NOTE_TYPE_ID_ELECTRONIC_BOOKPLATE);
						bookplateNote.put("note", marc.donor());
						bookplateNote.put("staffOnly", false);
						JSONArray holdingsNotes = (holdingsRecord.has("notes") ? holdingsRecord.getJSONArray("notes") : new JSONArray());
						holdingsNotes.put(bookplateNote);
						holdingsRecord.put("notes", holdingsNotes);
					}
				}
				Folio.callApiPut("holdings-storage/holdings/" + holdingsRecord.getString("id"), holdingsRecord);

				if (!marc.electronic() && marc.hasDonor()) {
					//IF PHYSICAL RESOURCE WITH DONOR INFO, GET THE ITEM FOLIO CREATED, SO WE CAN ADD NOTE ABOUT DONOR
					String itemsResponse = Folio.callApiGet("inventory/items?query=(holdingsRecordId==" + holdingsRecord.get("id") + ")");
					JSONObject itemsAsJson = new JSONObject(itemsResponse);
					JSONObject item = itemsAsJson.getJSONArray("items").getJSONObject(0);
					JSONObject bookplateNote = new JSONObject();
					bookplateNote.put("itemNoteTypeId", Constants.ITEM_NOTE_TYPE_ID_ELECTRONIC_BOOKPLATE);
					bookplateNote.put("note", marc.donor());
					bookplateNote.put("staffOnly", false);
					JSONArray itemNotes = (item.has("notes") ? item.getJSONArray("notes") : new JSONArray());
					itemNotes.put(bookplateNote);
					item.put("notes", itemNotes);
					//UPDATE THE ITEM
					Folio.callApiPut("inventory/items/" + item.getString("id"), item);
				}

				if (config.importInvoice && marc.hasInvoice()) {
					importInvoice(
							poNumberObj.getString("poNumber"), orderLineUUID, vendorId, marc);
				}

				//SAVE THE PO NUMBER FOR THE RESPONSE
				responseMessage.put("PONumber", poNumberObj.get("poNumber"));
				responseMessage.put("theOne", hrid);

				responseMessages.put(responseMessage);
			}
			catch(Exception e) {
				logger.fatal(e.toString());
				JSONObject responseMessage = new JSONObject();
				responseMessage.put("error",e.toString());
				responseMessage.put("PONumber", "~error~");
				responseMessages.put(responseMessage);
				return responseMessages;
			}
		}

		return responseMessages;

	}

	private void importInvoice(
							   String poNumber,
							   UUID orderLineUUID,
							   String vendorId,
							   MarcRecord marc) throws Exception {


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
		String invoiceResponse = Folio.callApiPostWithUtf8("invoice/invoices", invoice);
		logger.info(invoiceResponse);
		String invoiceLineResponse = Folio.callApiPostWithUtf8("invoice/invoice-lines", invoiceLine);
		logger.info(invoiceLineResponse);
	}

	public JSONArray validateRequiredValues(MarcReader reader) {

		Record record;
		JSONArray responseMessages = new JSONArray();
		while(reader.hasNext()) {
			try {
				record = reader.next();    					    
				//GET THE 980s FROM THE MARC RECORD
				DataField nineEighty = (DataField) record.getVariableField("980");

				DataField twoFourFive = (DataField) record.getVariableField("245");
				String title = twoFourFive.getSubfieldsAsString("a");
				//REMOVED - NOT NEEDED String theOne = ((ControlField) record.getVariableField("001")).getData();

				if (nineEighty == null) {
					JSONObject responseMessage = new JSONObject();
					responseMessage.put("error", "Record is missing the 980 field");
					responseMessage.put("PONumber", "~error~");
					responseMessage.put("title", title);
					responseMessages.put(responseMessage);
					continue;
				}

				String objectCode = nineEighty.getSubfieldsAsString("o");
				String projectCode = nineEighty.getSubfieldsAsString("r");
				String fundCode = nineEighty.getSubfieldsAsString("b");
				String vendorCode =  nineEighty.getSubfieldsAsString("v");
				String price = nineEighty.getSubfieldsAsString("m");

				Map<String, String> requiredFields = new HashMap<>();
				if (config.objectCodeRequired)  requiredFields.put("Object code",objectCode);
				requiredFields.put("Fund code",fundCode);
				requiredFields.put("Vendor Code",vendorCode);
				requiredFields.put("Price" , price);

				// MAKE SURE EACH OF THE REQUIRED SUBFIELDS HAS DATA
				for (Map.Entry<String,String> entry : requiredFields.entrySet())  {
					if (entry.getValue()==null || entry.getValue().isEmpty()) {
						JSONObject responseMessage = new JSONObject();
						responseMessage.put("title", title);
						responseMessage.put("error", entry.getKey() + " Missing");
						responseMessage.put("PONumber", "~error~");
						responseMessages.put(responseMessage);
					}
				}

				if (!responseMessages.isEmpty()) return responseMessages;

				//VALIDATE THE ORGANIZATION, OBJECT CODE AND FUND
				//STOP THE PROCESS IF AN ERRORS WERE FOUND
				JSONObject orgValidationResult = Folio.validateOrganization(vendorCode, title);
				if (orgValidationResult != null) responseMessages.put(orgValidationResult);

				if (objectCode != null && !objectCode.isEmpty()) {
					JSONObject objectValidationResult = Folio.validateObjectCode(objectCode, title);
					if (objectValidationResult != null) responseMessages.put(objectValidationResult);
				}
				//NEW FOR PROJECT CODE
				//PROJECT CODE IS NOT REQUIRED - BUT IF IT IS THERE, MAKE SURE IT'S A VALID CODE
				if (projectCode != null && !projectCode.isEmpty()) {
					JSONObject projectValidationResult = Folio.validateObjectCode(projectCode, title);
					if (projectValidationResult != null) responseMessages.put(projectValidationResult);
				}

				//END NEW
				JSONObject fundValidationResult = Folio.validateFund(fundCode, title, price);
				if (fundValidationResult != null) responseMessages.put(fundValidationResult);

				if (config.importInvoice) {
					JSONObject invoiceValidationResult = Folio.validateRequiredValuesForInvoice(title, record);
					if (invoiceValidationResult != null) responseMessages.put(invoiceValidationResult);
				}

				if (!responseMessages.isEmpty()) return responseMessages; //?
			}

			catch(Exception e) {
				logger.fatal(e.getMessage());
				JSONObject responseMessage = new JSONObject();
				responseMessage.put("error", e.getMessage());
				responseMessage.put("PONumber", "~error~");
				responseMessages.put(responseMessage);
			}
		}
		return responseMessages;

	}

	public JSONArray buildContributors(Record record, Map<String, String> lookupTable, boolean buildForOrderLine) {
		JSONArray contributors = new JSONArray();
		List<DataField> fields = record.getDataFields();
		Iterator fieldsIterator = fields.iterator();
		while (fieldsIterator.hasNext()) {
			DataField field = (DataField) fieldsIterator.next();
			if (field.getTag().equalsIgnoreCase("100") || field.getTag().equalsIgnoreCase("700")) {
				if (buildForOrderLine) {
					contributors.put( makeContributorForOrderLine(field, "Personal name"));
				} else {
					contributors.put( makeContributor(field, lookupTable, "Personal name",
							new String[] {"a", "b", "c", "d", "f", "g", "j", "k", "l", "n", "p", "t", "u"}));
				}
			} else if ((field.getTag().equals("110") || field.getTag().equals( "710" )) && buildForOrderLine) {
				contributors.put( makeContributorForOrderLine( field, "Corporate name"));
			} else if ((field.getTag().equals("111") || field.getTag().equals( "711" )) && buildForOrderLine) {
				contributors.put( makeContributorForOrderLine( field, "Meeting name"));
			}
		}
		return contributors;
	}

	public JSONObject makeContributorForOrderLine( DataField field, String contributorNameType) {
		Subfield subfield = field.getSubfield( 'a' );
		JSONObject contributor = new JSONObject();
		contributor.put("contributor", subfield.getData());
		contributor.put("contributorNameTypeId", Constants.CONTRIBUTOR_NAME_TYPES_MAP.get(contributorNameType));
		return contributor;
	}

	public JSONObject makeContributor( DataField field, Map<String,String> lookupTable, String name_type_id, String[] subfieldArray) {
		List<String> list = Arrays.asList(subfieldArray);
		JSONObject contributor = new JSONObject();
		contributor.put("name", "");
		contributor.put("contributorNameTypeId", lookupTable.get(name_type_id));
		List<Subfield> subfields =  field.getSubfields();
		Iterator subfieldIterator = subfields.iterator();
		String contributorName = "";
		while (subfieldIterator.hasNext()) {
			Subfield subfield = (Subfield) subfieldIterator.next();
			String subfieldAsString = String.valueOf(subfield.getCode());  
			if (subfield.getCode() == '4') {
				if (lookupTable.get(subfield.getData()) != null) {
					contributor.put("contributorTypeId", lookupTable.get(subfield.getData()));
				}
				else {
					contributor.put("contributorTypeId", lookupTable.get("bkp"));
				}
			}
			else if (subfield.getCode() == 'e') {
				contributor.put("contributorTypeText", subfield.getData());
			}
			else if (list.contains(subfieldAsString)) {
				if (!contributorName.isEmpty()) {
					contributorName += ", " + subfield.getData();
				}
				else {
					contributorName +=  subfield.getData();
				}
			}
		}
		contributor.put("name", contributorName);
		return contributor;
	}

	public ServletContext getMyContext() {
		return myContext;
	}

	public void setMyContext(ServletContext myContext) {
		this.myContext = myContext;
	}

	static String readFile(String path, Charset encoding)  throws IOException  {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	private static String getMaterialTypeId (String materialType) {
		return isUUID(materialType) ? materialType : Constants.MATERIAL_TYPES_MAP.get(materialType);
	}

	private static boolean isUUID(String str)
	{
		return ( str != null && Constants.UUID_PATTERN.matcher( str ).matches() );
	}
}
