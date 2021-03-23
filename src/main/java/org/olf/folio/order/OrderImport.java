package org.olf.folio.order;


import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.ServletContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.marc4j.MarcJsonWriter;
import org.marc4j.MarcPermissiveStreamReader;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcTranslatedReader;
import org.marc4j.MarcWriter;
import org.marc4j.converter.impl.AnselToUnicode;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;
import org.olf.folio.order.services.ApiService;
import org.olf.folio.order.util.LookupUtil;
import org.olf.folio.order.util.MarcUtils; 
import org.apache.log4j.Logger;

public class OrderImport {
	
	private static final Logger logger = Logger.getLogger(OrderImport.class);
	private ServletContext myContext;
	private HashMap<String,String> lookupTable;
	private String tenant;
	
	private ApiService apiService;
	MarcUtils marcUtils = new MarcUtils();
	
	public  JSONArray  upload(String fileName) throws IOException, InterruptedException, Exception {

		logger.info("...starting...");
		JSONArray responseMessages = new JSONArray();
		
		//COLLECT VALUES FROM THE CONFIGURATION FILE
		String baseOkapEndpoint = (String) getMyContext().getAttribute("baseOkapEndpoint");
		String apiUsername = (String) getMyContext().getAttribute("okapi_username");
		String apiPassword = (String) getMyContext().getAttribute("okapi_password");
		tenant = (String) getMyContext().getAttribute("tenant");
		
		//String permLocationName = (String) getMyContext().getAttribute("permLocation");
		String permELocationName = (String) getMyContext().getAttribute("permELocation");
		String noteTypeName = (String) getMyContext().getAttribute("noteType");
		String materialTypeName = (String) getMyContext().getAttribute("materialType");
		
		//GET THE FOLIO TOKEN
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("username", apiUsername);
		jsonObject.put("password", apiPassword);
		jsonObject.put("tenant",tenant);
		
		this.apiService = new ApiService(tenant);
		logger.debug("get api token");
		String token = this.apiService.callApiAuth( baseOkapEndpoint + "authn/login",  jsonObject);
		
		//String token = callApiAuth( baseOkapEndpoint + "authn/login",  jsonObject);

		//TODO: REMOVE
		logger.debug("TOKEN: " + token);  
		
		//GET THE UPLOADED FILE
		String filePath = (String) myContext.getAttribute("uploadFilePath");
		InputStream in = null;		
		//MAKE SURE A FILE WAS UPLOADED
		InputStream is = null;
		if (fileName != null) {
			in = new FileInputStream(filePath + fileName);			
		} else {
			JSONObject responseMessage = new JSONObject();
			responseMessage.put("error", "no input file provided");
			responseMessage.put("PONumber", "~error~");
			responseMessages.put(responseMessage);
			return responseMessages;
		}
		
		//READ THE MARC RECORD FROM THE FILE AND VALIDATE IT
		//VALIDATES THE FUND CODE, TAG (OBJECT CODE
		MarcReader reader = new MarcStreamReader(in);
	    Record record = null;
	    
	   	JSONArray validateRequiredResult = validateRequiredValues(reader, token, baseOkapEndpoint);
	   	if (!validateRequiredResult.isEmpty()) return validateRequiredResult;
	   	
		//SAVE REFERENCE TABLE VALUES (JUST LOOKUP THEM UP ONCE)
	   	logger.debug("Get Lookup table");
		if (myContext.getAttribute(Constants.LOOKUP_TABLE) == null) {
			 LookupUtil lookupUtil = new LookupUtil();
			 lookupUtil.setBaseOkapEndpoint(baseOkapEndpoint);
			 lookupUtil.setApiService(apiService);
			 lookupUtil.load();
			 this.lookupTable = lookupUtil.getReferenceValues(token);
			 myContext.setAttribute(Constants.LOOKUP_TABLE, lookupTable);
			 logger.debug("put lookup table in context");
		} else {
			 this.lookupTable = (HashMap<String, String>) myContext.getAttribute(Constants.LOOKUP_TABLE);
			 logger.debug("got lookup table from context");
		}		 
		
		//READ THE MARC RECORD FROM THE FILE
		in = new FileInputStream(filePath + fileName);
		reader = new MarcStreamReader(in);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		MarcWriter w = new MarcStreamWriter(byteArrayOutputStream,"UTF-8");
		
		AnselToUnicode conv = new AnselToUnicode();
		w.setConverter(conv);
		
	    record = null;
		logger.debug("reading marc file");
		while (reader.hasNext()) {
			try {
				//INITIALIZE ELECTRONIC TO FALSE
				boolean electronic = false;
				
				record = reader.next();
				//GET THE 980, 981, 245, and  952 fields FROM THE MARC RECORD
				DataField twoFourFive = (DataField) record.getVariableField("245");
				DataField nineEighty = (DataField) record.getVariableField("980");
			    DataField nineFiveTwo = (DataField) record.getVariableField("952"); 
			    DataField nineEightyOne = (DataField) record.getVariableField("981");
			    
			    String title = marcUtils.getTitle(twoFourFive);
			    String fundCode = marcUtils.getFundCode(nineEighty);			    
			    String vendorCode =  marcUtils.getVendorCode(nineEighty);
			    String vendorItemId = marcUtils.getVendorItemId(nineEighty);
				String quantity =  marcUtils.getQuantiy(nineEighty);
				Integer quanityNo = 0; //INIT
			    if (quantity != null)  quanityNo = Integer.valueOf(quantity); 
				String price = marcUtils.getPrice(nineEighty, nineEightyOne);

				String notes =  marcUtils.getNotes(nineEighty);
				String electronicIndicator = marcUtils.getElectronicIndicator(nineEighty);
				if (electronicIndicator != null && electronicIndicator.equalsIgnoreCase("ELECTRONIC")) electronic = true;
				
				String permLocationName = marcUtils.getLocation(nineFiveTwo);
				logger.debug("permLocationName: "+ permLocationName);
				
			    // GENERATE UUIDS FOR OBJECTS
			    UUID snapshotId = UUID.randomUUID();
			    UUID recordTableId = UUID.randomUUID();
			    UUID orderUUID = UUID.randomUUID();
			    UUID orderLineUUID = UUID.randomUUID();
			    
			    //NOW WE CAN START CREATING THE PO!
			    //PULL TOGETHER THE ENTIRE TITLE
			    JSONObject responseMessage = new JSONObject();
			    
			    //PUT THE TITLE IN THE RESPONSE MESSAGE
			    responseMessage.put("title", title);

				//LOOK UP VENDOR
			    logger.debug("lookupVendor");
				String organizationEndpoint = baseOkapEndpoint + "organizations-storage/organizations?limit=30&offset=0&query=((code='" + vendorCode + "'))";
				String orgLookupResponse = apiService.callApiGet(organizationEndpoint,  token);
				JSONObject orgObject = new JSONObject(orgLookupResponse);
				String vendorId = (String) orgObject.getJSONArray("organizations").getJSONObject(0).get("id");
				
				//LOOK UP THE FUND
				logger.debug("lookup Fund");
				String fundEndpoint = baseOkapEndpoint + "finance/funds?limit=30&offset=0&query=((code='" + fundCode + "'))";
				String fundResponse = apiService.callApiGet(fundEndpoint, token);
				JSONObject fundsObject = new JSONObject(fundResponse);
				String fundId = (String) fundsObject.getJSONArray("funds").getJSONObject(0).get("id");
				
				//GET THE NEXT PO NUMBER
				logger.debug("get next PO number");
				String poNumber = apiService.callApiGet(baseOkapEndpoint + "orders/po-number", token);
				JSONObject poNumberObj = new JSONObject(poNumber);
				logger.debug("NEXT PO NUMBER: " + poNumberObj.get("poNumber"));
				
				// CREATING THE PURCHASE ORDER
				JSONObject order = new JSONObject();
				order.put("poNumber", poNumberObj.get("poNumber"));
				order.put("vendor", vendorId);
				order.put("orderType", "One-Time");
				order.put("reEncumber", false);
				order.put("id", orderUUID.toString());
				order.put("approved", true);
				order.put("workflowStatus","Open");
				
				// POST ORDER LINE
				//FOLIO WILL CREATE THE INSTANCE, HOLDINGS, ITEM (IF PHYSICAL ITEM)
				JSONObject orderLine = new JSONObject();
				JSONObject cost = new JSONObject();
				JSONObject location = new JSONObject();
				JSONArray locations = new JSONArray();
				JSONArray poLines = new JSONArray();
				if (electronic) {
					logger.trace("electronic=true");
					orderLine.put("orderFormat", "Electronic Resource");
					JSONObject eResource = new JSONObject();
					eResource.put("activated", false);
					eResource.put("createInventory", "Instance, Holding");
					eResource.put("trial", false);
					eResource.put("accessProvider", vendorId);
					orderLine.put("eresource",eResource);
					orderLine.put("orderFormat", "Electronic Resource");
					cost.put("quantityElectronic", 1);
					cost.put("listUnitPriceElectronic", price);
					location.put("quantityElectronic",quanityNo);
					location.put("locationId", lookupTable.get(permELocationName + "-location"));
					locations.put(location);
				} else {
					logger.trace("electronic=false");
					JSONObject physical = new JSONObject();
					physical.put("createInventory", "Instance, Holding, Item");
					physical.put("materialType", lookupTable.get(materialTypeName));
					orderLine.put("physical", physical);
					orderLine.put("orderFormat", "Physical Resource");
					cost.put("listUnitPrice", price);
					cost.put("quantityPhysical", 1);
					location.put("quantityPhysical",quanityNo);
					location.put("locationId",lookupTable.get(permLocationName + "-location"));
					locations.put(location);
				}
				
				//VENDOR REFERENCE NUMBER IF INCLUDED IN THE MARC RECORD:
				if (vendorItemId != null) {
					JSONObject vendorDetail = new JSONObject();
					vendorDetail.put("instructions", "");
					vendorDetail.put("refNumber", vendorItemId);
					vendorDetail.put("refNumberType", "Internal vendor number");
					vendorDetail.put("vendorAccount", "");
					orderLine.put("vendorDetail", vendorDetail);
				}				
				
				orderLine.put("id", orderLineUUID);
				orderLine.put("source", "User");
				cost.put("currency", "USD");
				orderLine.put("cost", cost);
				orderLine.put("locations", locations);
				orderLine.put("titleOrPackage",title);
				orderLine.put("acquisitionMethod", "Purchase");
				JSONArray funds = new JSONArray();
				JSONObject fundDist = new JSONObject();
				fundDist.put("distributionType", "percentage");
				fundDist.put("value", 100);
				fundDist.put("fundId", fundId);
				funds.put(fundDist);
				orderLine.put("fundDistribution", funds);
				orderLine.put("purchaseOrderId", orderUUID.toString());
				poLines.put(orderLine);
				order.put("compositePoLines", poLines);
				
				//POST THE ORDER AND LINE:
				String orderResponse = apiService.callApiPostWithUtf8(baseOkapEndpoint + "orders/composite-orders",order,token); 
				JSONObject approvedOrder = new JSONObject(orderResponse);
				logger.info(orderResponse);
				
				//INSERT THE NOTE IF THERE IS A NOTE IN THE MARC RECORD
				if (notes != null && !notes.equalsIgnoreCase("")) {
					logger.info("NOTE TYPE NAME: " + noteTypeName);
					logger.info(lookupTable);
					JSONObject noteAsJson = new JSONObject();
					JSONArray links = new JSONArray();
					JSONObject link = new JSONObject();
					link.put("type","poLine");
					link.put("id", orderLineUUID);
					links.put(link);
					noteAsJson.put("links", links);
					noteAsJson.put("typeId", lookupTable.get(noteTypeName));
					noteAsJson.put("domain", "orders");
					noteAsJson.put("content", notes);
					noteAsJson.put("title", notes);
					String noteResponse = apiService.callApiPostWithUtf8(baseOkapEndpoint + "/notes",noteAsJson,token); 
					logger.info(noteResponse);
				}
								
				//GET THE UPDATED PURCHASE ORDER FROM THE API AND PULL OUT THE ID FOR THE INSTANCE FOLIO CREATED:
				logger.debug("getUpdatedPurchaseOrder");
				String updatedPurchaseOrder = apiService.callApiGet(baseOkapEndpoint + "orders/composite-orders/" +orderUUID.toString() ,token); 
				JSONObject updatedPurchaseOrderJson = new JSONObject(updatedPurchaseOrder);
				String instanceId = updatedPurchaseOrderJson.getJSONArray("compositePoLines").getJSONObject(0).getString("instanceId");
				
				//GET THE INSTANCE RECORD FOLIO CREATED, SO WE CAN ADD BIB INFO TO IT:
				logger.debug("get InstanceResponse");
				String instanceResponse = apiService.callApiGet(baseOkapEndpoint + "inventory/instances/" + instanceId, token);
				JSONObject instanceAsJson = new JSONObject(instanceResponse);
				String hrid = instanceAsJson.getString("hrid");
				
				//PREPARING TO ADD THE MARC RECORD TO SOURCE RECORD STORAGE:
				//CONSTRUCTING THE 999 OF THE MARC RECORD for FOLIO: 
				DataField field = MarcFactory.newInstance().newDataField();
				field.setTag("999");
				field.setIndicator1('f');
				field.setIndicator2('f');
				Subfield one = MarcFactory.newInstance().newSubfield('i', instanceId);
				Subfield two = MarcFactory.newInstance().newSubfield('s',recordTableId.toString());
				field.addSubfield(one);
				field.addSubfield(two);
				record.addVariableField(field);
			    if (record.getControlNumberField() != null) {
			    	record.getControlNumberField().setData(hrid);
			    }
			    else {
			    	ControlField cf = MarcFactory.newInstance().newControlField("001");
			    	cf.setData(hrid);
			    	record.addVariableField(cf);
			    }
			    
				//TRANSFORM THE RECORD INTO JSON
				logger.info("MARC RECORD: " + record.toString());
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				MarcJsonWriter jsonWriter =  new MarcJsonWriter(baos);
				jsonWriter.setUnicodeNormalization(true);
				jsonWriter.write(record);
				jsonWriter.close();
				String jsonString = baos.toString();
				JSONObject mRecord = new JSONObject(jsonString);
				JSONObject content = new JSONObject();
				content.put("content",mRecord);
				logger.info("MARC TO JSON: " + mRecord.toString(3));

				//GET THE RAW MARC READY TO POST TO THE API
				ByteArrayOutputStream rawBaos = new ByteArrayOutputStream();
				MarcWriter writer = new MarcStreamWriter(rawBaos);
				writer.write(record);
				JSONObject jsonWithRaw = new JSONObject();
				jsonWithRaw.put("id", instanceId);
				jsonWithRaw.put("content",byteArrayOutputStream);
				
				//CREATING JOB EXECUTION?
				//TODO: I'M NOT ENTIRELY SURE IF THIS IS NECESSARY?
				//WHAT THE CONSEQUENCES OF THIS ARE?
				//TO POST TO SOURCE RECORD STORAGE, A SNAPSHOT ID
				//SEEMS TO BE REQUIRECD
				JSONObject jobExecution = new JSONObject();
				jobExecution.put("jobExecutionId", snapshotId.toString());
				jobExecution.put("status", "PARSING_IN_PROGRESS");
				String snapShotResponse = apiService.callApiPostWithUtf8(baseOkapEndpoint + "source-storage/snapshots",  jobExecution,token);
				
				//OBJECT FOR SOURCE RECORD STORAGE API CALL:
				JSONObject sourceRecordStorageObject = new JSONObject();
				sourceRecordStorageObject.put("recordType", "MARC");
				sourceRecordStorageObject.put("snapshotId",snapshotId.toString());
				sourceRecordStorageObject.put("matchedId", instanceId.toString());
				
				//LINK THE INSTANCE TO SOURCE RECORD STORAGE
				JSONObject externalId = new JSONObject();
				externalId.put("instanceId",instanceId);
				sourceRecordStorageObject.put("externalIdsHolder", externalId);
				
				//RAW RECORD
				JSONObject rawRecordObject = new JSONObject();
				rawRecordObject.put("id",instanceId);
				rawRecordObject.put("content",jsonWithRaw.toString());
				
				//PARSED RECORD
				JSONObject parsedRecord = new JSONObject();
				parsedRecord.put("id", instanceId);
				parsedRecord.put("content", mRecord);
				sourceRecordStorageObject.put("rawRecord", rawRecordObject);
				sourceRecordStorageObject.put("parsedRecord", parsedRecord);
				sourceRecordStorageObject.put("id", instanceId);
				
				//CALL SOURCE RECORD STORAGE POST
				String storageResponse = apiService.callApiPostWithUtf8(baseOkapEndpoint + "source-storage/records", sourceRecordStorageObject,token);
				
				
				//ADD IDENTIFIERS AND CONTRIBUTORS TO THE INSTANCE
				//*AND* CHANGE THE SOURCE TO 'MARC'
				//SO THE OPTION TO VIEW THE MARC RECORD SHOWS UP 
				//IN INVENTORY!
				JSONArray identifiers = buildIdentifiers(record, lookupTable);
				JSONArray contributors = buildContributors(record, lookupTable);
				instanceAsJson.put("title", title);
				instanceAsJson.put("source", "MARC");
				instanceAsJson.put("instanceTypeId", lookupTable.get("text"));
				instanceAsJson.put("identifiers", identifiers);
				instanceAsJson.put("contributors", contributors);
				instanceAsJson.put("discoverySuppress", false);
				
				
				//GET THE HOLDINGS RECORD FOLIO CREATED, SO WE CAN ADD URLs FROM THE 856 IN THE MARC RECORD
				String holdingResponse = apiService.callApiGet(baseOkapEndpoint + "holdings-storage/holdings?query=(instanceId==" + instanceId + ")", token);
				JSONObject holdingsAsJson = new JSONObject(holdingResponse);
				JSONObject holdingRecord = holdingsAsJson.getJSONArray("holdingsRecords").getJSONObject(0);
				
				JSONArray eResources = new JSONArray();
				String linkText = (String) getMyContext().getAttribute("textForElectronicResources");
				
				// TODO: clean this up...
				List urls =  record.getVariableFields("856");
				Iterator<DataField> iterator = urls.iterator();
				while (iterator.hasNext()) {
					DataField dataField = (DataField) iterator.next();
					if (dataField != null && dataField.getSubfield('u') != null) {
						String url = dataField.getSubfield('u').getData();
						if (dataField.getSubfield('z') != null) {
							linkText = dataField.getSubfield('z').getData();
						}
						JSONObject eResource = new JSONObject();
						eResource.put("uri", dataField.getSubfield('u').getData());
						//TODO - DO WE WANT TO CHANGE THE LINK TEXT?
						eResource.put("linkText", linkText);
						//I 'THINK' THESE RELATIONSHIP TYPES ARE HARDCODED INTO FOLIO
						//CANT BE LOOKED UP WITH AN API?
						//https://github.com/folio-org/mod-inventory-storage/blob/master/reference-data/electronic-access-relationships/resource.json
						eResource.put("relationshipId", "f5d0068e-6272-458e-8a81-b85e7b9a14aa");
						eResources.put(eResource);
					}
				}

				
				//UPDATE THE INSTANCE RECORD
				instanceAsJson.put("electronicAccess", eResources);
				instanceAsJson.put("natureOfContentTermIds", new JSONArray());
				instanceAsJson.put("precedingTitles", new JSONArray());
				instanceAsJson.put("succeedingTitles", new JSONArray());
				String instanceUpdateResponse = apiService.callApiPut(baseOkapEndpoint + "inventory/instances/" + instanceId,  instanceAsJson, token);
				
				//UPDATE THE HOLDINGS RECORD
				holdingRecord.put("electronicAccess", eResources);
				//IF THIS WAS AN ELECTRONIC RECORD, MARK THE HOLDING AS EHOLDING
				if (electronic) {
					holdingRecord.put("holdingsTypeId",this.lookupTable.get("Electronic"));
				}
				String createHoldingsResponse = apiService.callApiPut(baseOkapEndpoint + "holdings-storage/holdings/" + holdingRecord.getString("id"), holdingRecord,token);
				
				//SAVE THE PO NUMBER FOR THE RESPONSE
				responseMessage.put("PONumber", poNumberObj.get("poNumber"));
				responseMessage.put("theOne", hrid);
				responseMessage.put("location", permLocationName +" ("+ lookupTable.get(permELocationName + "-location") +")");
				
				responseMessages.put(responseMessage);
			}
			catch(Exception e) {
				logger.error(e.toString());
				JSONObject responseMessage = new JSONObject();
				responseMessage.put("error",e.toString());
				responseMessage.put("PONumber", "~error~");
				responseMessages.put(responseMessage);
				return responseMessages;
			}
		}
	    
		
		return responseMessages;

	}
	
	public JSONArray validateRequiredValues(MarcReader reader,String token, String baseOkapEndpoint ) {
		
	    Record record = null;
	    JSONArray responseMessages = new JSONArray();
		while(reader.hasNext()) {
			try {
		    	record = reader.next();    					    
		    	//GET THE 980, 981, 245, and 952 fields FROM THE MARC RECORD
			    DataField nineEighty = (DataField) record.getVariableField("980");
			    DataField nineEightyOne = (DataField) record.getVariableField("981");
			    DataField twoFourFive = (DataField) record.getVariableField("245");
			    DataField nineFiveTwo = (DataField) record.getVariableField("952");
			    
			    if (twoFourFive == null) {
					JSONObject responseMessage = new JSONObject();
					responseMessage.put("error", "Record is missing the 245 field");
					responseMessage.put("PONumber", "~error~");
					responseMessage.put("title", "~error~");
					//responseMessage.put("theOne", theOne);
					responseMessages.put(responseMessage);
					continue;
				}
			    
			    String title = marcUtils.getTitle(twoFourFive); 
			    
				if (nineEighty == null) {
					JSONObject responseMessage = new JSONObject();
					responseMessage.put("error", "Record is missing the 980 field");
					responseMessage.put("PONumber", "~error~");
					responseMessage.put("title", title);
					//responseMessage.put("theOne", theOne);
					responseMessages.put(responseMessage);
					continue;
				}
				
				String fundCode = marcUtils.getFundCode(nineEighty); 
				String vendorCode =  marcUtils.getVendorCode(nineEighty);
				String price = marcUtils.getPrice(nineEighty, nineEightyOne);
			    
				String quantity =  marcUtils.getQuantiy(nineEighty);
				Integer quanityNo = 0;
			    if (quantity != null)  quanityNo = Integer.valueOf(quantity);				
			    
				String electronicIndicator = marcUtils.getElectronicIndicator(nineEighty);
				String vendorItemId = marcUtils.getVendorItemId(nineEighty);
			    String notes =  marcUtils.getNotes(nineEighty);

			    String permLocationName = marcUtils.getLocation(nineFiveTwo);
			    
			    Map<String, String> requiredFields = new HashMap<String, String>(); 
			    requiredFields.put("Fund code", fundCode);
			    requiredFields.put("Vendor Code", vendorCode);
			    requiredFields.put("Price" , price);
			    requiredFields.put("PermLocation", permLocationName);
			    
			    // MAKE SURE EACH OF THE REQUIRED SUBFIELDS HAS DATA
		        for (Map.Entry<String,String> entry : requiredFields.entrySet())  {
		        	if (entry.getValue()==null) {
		        		JSONObject responseMessage = new JSONObject();
		        		responseMessage.put("title", title);
		        		//responseMessage.put("theOne", theOne);
					    responseMessage.put("error", entry.getKey() + " Missing");
						responseMessage.put("PONumber", "~error~");
						responseMessages.put(responseMessage);
		        	}
		        }
		        
		        if (!responseMessages.isEmpty()) return responseMessages;
		        
		        
			    //VALIDATE THE ORGANIZATION,  AND FUND
			    //STOP THE PROCESS IF AN ERRORS WERE FOUND
			    JSONObject orgValidationResult = validateOrganization(vendorCode, title, token, baseOkapEndpoint);
			    if (orgValidationResult != null) {
			    	logger.error("organization invalid: "+ vendorCode);
			    	logger.error(record.toString());
			    	responseMessages.put(orgValidationResult);				    
			    }
			    				    
			    JSONObject fundValidationResult = validateFund(fundCode, title, token, baseOkapEndpoint, price);
			    if (fundValidationResult != null) {
			    	logger.error("fundCode invalid: "+ fundCode + " (price: "+ price +")");
			    	logger.error(record.toString());
			    	responseMessages.put(fundValidationResult);
			    }
			    return responseMessages;
			    
			}  catch(Exception e) {
		    	logger.fatal(e.getMessage());
		    	JSONObject responseMessage = new JSONObject();
		    	responseMessage.put("error", e.getMessage());
		    	responseMessage.put("PONumber", "~error~");
		    	responseMessages.put(responseMessage);
		    }
		}
		return responseMessages;
		
	}
	
	
	//TODO - FIX THESE METHODS THAT GATHER DETAILS FROM THE MARC RECORD.
	//THEY WERE HURRILY CODED
	//JUST WANTED TO GET SOME DATA IN THE INSTANCE
	//FROM THE MARC RECORD FOR THIS POC
	public JSONArray buildContributors(Record record, HashMap<String,String> lookupTable) {
		JSONArray contributors = new JSONArray();
		String[] subfields = {"a","b","c","d","f","g","j","k","l","n","p","t","u"};
		
		List<DataField> fields = record.getDataFields();
		Iterator<DataField> fieldsIterator = fields.iterator();
		while (fieldsIterator.hasNext()) {
			DataField field = (DataField) fieldsIterator.next();
			if (field.getTag().equalsIgnoreCase("100") || field.getTag().equalsIgnoreCase("700")) {
				contributors.put(makeContributor(field,lookupTable, "Personal name", subfields));
			}
		}
		return contributors;
	}
	
	public JSONObject makeContributor( DataField field, HashMap<String,String> lookupTable, String name_type_id, String[] subfieldArray) {
		List<String> list = Arrays.asList(subfieldArray);
		JSONObject contributor = new JSONObject();
		contributor.put("name", "");
		contributor.put("contributorNameTypeId", lookupTable.get(name_type_id));
		List<Subfield> subfields =  field.getSubfields();
		Iterator<Subfield> subfieldIterator = subfields.iterator();
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
	
   
   public JSONArray buildIdentifiers(Record record, HashMap<String, String> lookupTable) {
		JSONArray identifiers = new JSONArray();
		
		List<DataField> fields = record.getDataFields();
		Iterator<DataField> fieldsIterator = fields.iterator();
		while (fieldsIterator.hasNext()) {
			DataField field = (DataField) fieldsIterator.next();
			System.out.println(field.getTag());
			List<Subfield> subfields =  field.getSubfields();
			Iterator<Subfield> subfieldIterator = subfields.iterator();
			while (subfieldIterator.hasNext()) {
				Subfield subfield = (Subfield) subfieldIterator.next();
				if (field.getTag().equalsIgnoreCase("020")) {
					if (subfield.getCode() == 'a') {
						JSONObject identifier = new JSONObject();
						String fullValue = subfield.getData();
						if (field.getSubfield('c') != null) fullValue += " "  + field.getSubfieldsAsString("c");
						if (field.getSubfield('q') != null) fullValue += " " + field.getSubfieldsAsString("q");
						identifier.put("value",fullValue);
						
						identifier.put("identifierTypeId", lookupTable.get("ISBN"));
						identifiers.put(identifier);
					}
					if (subfield.getCode() == 'z') {
						JSONObject identifier = new JSONObject();
						String fullValue = subfield.getData();
						if (field.getSubfield('c') != null) fullValue += " " + field.getSubfieldsAsString("c");
						if (field.getSubfield('q') != null) fullValue += " " + field.getSubfieldsAsString("q");
						identifier.put("value", fullValue);
						identifier.put("identifierTypeId", lookupTable.get("Invalid ISBN"));
						identifiers.put(identifier);
					}
				}
				if (field.getTag().equalsIgnoreCase("022")) {
					if (subfield.getCode() == 'a') {
						JSONObject identifier = new JSONObject();
						String fullValue = subfield.getData();
						if (field.getSubfield('c') != null) fullValue += " " + field.getSubfieldsAsString("c");
						if (field.getSubfield('q') != null) fullValue += " " + field.getSubfieldsAsString("q");
						identifier.put("value",fullValue);
						
						identifier.put("identifierTypeId", lookupTable.get("ISSN"));
						identifiers.put(identifier);
					} else if (subfield.getCode() == 'l') {
						JSONObject identifier = new JSONObject();
						String fullValue = subfield.getData();
						if (field.getSubfield('c') != null) fullValue += " " + field.getSubfieldsAsString("c");
						if (field.getSubfield('q') != null) fullValue += " " + field.getSubfieldsAsString("q");
						identifier.put("value", fullValue);
						identifier.put("identifierTypeId", lookupTable.get("Linking ISSN"));
						identifiers.put(identifier);
					} else {
						JSONObject identifier = new JSONObject();
						String fullValue = "";
						if (field.getSubfield('z') != null) fullValue += field.getSubfieldsAsString("z");
						if (field.getSubfield('y') != null) fullValue += " " +  field.getSubfieldsAsString("y");
						if (field.getSubfield('m') != null) fullValue += " " + field.getSubfieldsAsString("m");
						if (fullValue != "") {
							identifier.put("value", fullValue);
							identifier.put("identifierTypeId", lookupTable.get("Invalid ISSN"));
							identifiers.put(identifier);
						}
					}
				}
				
				
			}
			
		}
		return identifiers;
		
		
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
	

	//TODO 
	//THESE VALIDATION METHODS COULD
	//USE IMPROVEMENT
	public JSONObject validateFund(String fundCode, String title, String token, String baseOkapiEndpoint, String price ) throws IOException, InterruptedException, Exception {
		
		//GET CURRENT FISCAL YEAR
		String fiscalYearCode =  (String) getMyContext().getAttribute("fiscalYearCode");
		String fundEndpoint = baseOkapiEndpoint + "finance/funds?limit=30&offset=0&query=((code='" + fundCode + "'))";
		
		JSONObject responseMessage = new JSONObject();
		
		String fundResponse = apiService.callApiGet(fundEndpoint, token);
		JSONObject fundsObject = new JSONObject(fundResponse);
		//----------->VALIDATION #1: MAKE SURE THE FUND CODE EXISTS
		if (fundsObject.getJSONArray("funds").length() < 1) {
			responseMessage.put("error", "Fund code in file (" + fundCode + ") does not exist in FOLIO");
			responseMessage.put("PONumber", "~error~");
			return responseMessage;
		}
		String fundId = (String) fundsObject.getJSONArray("funds").getJSONObject(0).get("id");
		logger.debug("FUNDS: " + fundsObject.get("funds"));
		
		//----------->VALIDATION #2: MAKE SURE THE FUND CODE FOR THE CURRENT FISCAL HAS ENOUGH MONEY
		String fundBalanceQuery = baseOkapiEndpoint + "finance/budgets?query=(name=="  + fundCode + "-" + fiscalYearCode + ")";
		String fundBalanceResponse = apiService.callApiGet(fundBalanceQuery, token);
		JSONObject fundBalanceObject = new JSONObject(fundBalanceResponse);
		if (fundBalanceObject.getJSONArray("budgets").length() < 1) {
			responseMessage.put("error", "Fund code in file (" + fundCode + ") does not have a budget");
			responseMessage.put("title", title);
			responseMessage.put("PONumber", "~error~");
			return responseMessage;
		} 
		logger.info("funds are valid");
		return null;
	}
	
	
	
	public JSONObject validateOrganization(String orgCode, String title,  String token, String baseOkapiEndpoint ) throws IOException, InterruptedException, Exception {
		JSONObject responseMessage = new JSONObject();
	    //LOOK UP THE ORGANIZATION
	    String organizationEndpoint = baseOkapiEndpoint + "organizations-storage/organizations?limit=30&offset=0&query=((code='" + orgCode + "'))";
	    String orgLookupResponse = apiService.callApiGet(organizationEndpoint,  token);
		JSONObject orgObject = new JSONObject(orgLookupResponse);
		//---------->VALIDATION: MAKE SURE THE ORGANIZATION CODE EXISTS
		if (orgObject.getJSONArray("organizations").length() < 1) {
			logger.error(orgObject.toString(3));
			responseMessage.put("error", "Organization code in file (" + orgCode + ") does not exist in FOLIO");
			responseMessage.put("title", title);
			responseMessage.put("PONumber", "~error~");
			return responseMessage;
		}
		return null;
	}
	

}
