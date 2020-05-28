package org.olf.folio.order;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletContext;

import org.json.JSONArray;
import org.json.JSONObject;
import org.marc4j.MarcJsonWriter;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcWriter;
import org.marc4j.marc.DataField;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

public class TestImport {
	
	private static final Logger logger = Logger.getLogger(TestImport.class);
	private ServletContext myContext;
	private HashMap<String,String> lookupTable;
	private String tenant;
	
	public  JSONObject  upload(String fileName) throws IOException, InterruptedException, Exception {

		logger.info("...starting...");
		JSONObject responseMessage = new JSONObject();
		//COLLECT VALUES FROM THE CONFIGURATION FILE
		String baseOkapEndpoint = (String) getMyContext().getAttribute("baseOkapEndpoint");
		String apiUsername = (String) getMyContext().getAttribute("okapi_username");
		String apiPassword = (String) getMyContext().getAttribute("okapi_password");
		tenant = (String) getMyContext().getAttribute("tenant");
		String permLocationName = (String) getMyContext().getAttribute("permLocation");
		String noteTypeName = (String) getMyContext().getAttribute("noteType");
		String materialTypeName = (String) getMyContext().getAttribute("materialType");
		
		//GET THE FOLIO TOKEN
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("username", apiUsername);
		jsonObject.put("password", apiPassword);
		jsonObject.put("tenant",tenant);
		String token = callApiAuth( baseOkapEndpoint + "authn/login",  jsonObject);
		
		//TODO: REMOVE
		logger.info("TOKEN: " + token); 
		
		// GENERATE UUIDS FOR OBJECTS
		UUID parsedRecordId = UUID.randomUUID();
		UUID rawRecordId = UUID.randomUUID();
		UUID matchedId = UUID.randomUUID();
		UUID snapshotId = UUID.randomUUID();
		UUID recordTableId = UUID.randomUUID();
		UUID orderUUID = UUID.randomUUID();
		UUID orderLineUUID = UUID.randomUUID();
		
		//LOOKUP REFERENCE TABLES 
		//TODO
		//IMPROVE THIS - 'text' is repeated (it is a 'name' in more than one reference table)
		List<String> referenceTables = new ArrayList<String>(); 
		referenceTables.add(baseOkapEndpoint +"identifier-types?limit=1000");
		referenceTables.add(baseOkapEndpoint + "contributor-types?limit=1000");
		referenceTables.add(baseOkapEndpoint + "classification-types?limit=1000");
		referenceTables.add(baseOkapEndpoint + "contributor-types?limit=1000");
		referenceTables.add(baseOkapEndpoint + "contributor-name-types?limit=1000");
		referenceTables.add(baseOkapEndpoint + "locations?limit=10000");
		referenceTables.add(baseOkapEndpoint + "loan-types?limit=1000");
		referenceTables.add(baseOkapEndpoint + "note-types?limit=1000");
		referenceTables.add(baseOkapEndpoint + "material-types?limit=1000");
		referenceTables.add(baseOkapEndpoint + "instance-types?limit=1000");
		 
		 //SAVE REFERENCE TABLE VALUES (JUST LOOKUP THEM UP ONCE)
		 if (myContext.getAttribute(Constants.LOOKUP_TABLE) == null) {
				this.lookupTable = lookupReferenceValues(referenceTables,token);
				myContext.setAttribute(Constants.LOOKUP_TABLE, lookupTable);
			}
		else {
				this.lookupTable = (HashMap<String, String>) myContext.getAttribute(Constants.LOOKUP_TABLE);
		}
			
		//INITIALIZE ELECTRONIC TO FALSE
		boolean electronic = false;

		//GET THE UPLOADED FILE
		InputStream in = null;
		if (fileName != null) {
			//TODO
			String filePath = (String) myContext.getAttribute("uploadFilePath");
			in = new FileInputStream(filePath + fileName);
		}
		else {
			responseMessage.put("error", "no input file provided");
			return responseMessage;
		}
		
		//SAVE FILE NAME FOR RESPONSE MESSAGE
		responseMessage.put("fileName", fileName);
		
		//READ THE MARC RECORD FROM THE FILE
		MarcReader reader = new MarcStreamReader(in);
	    Record record = null;
	    
	    try {
	    	reader.hasNext();
	    	record = reader.next();
	    }
	    catch(Exception e) {
	    	logger.fatal(e.getMessage());
	    	responseMessage.put("error", e.getMessage());
	    }
	    
	    //PULL TOGETHER THE TITLE
	    DataField twoFourFive = (DataField) record.getVariableField("245");
	    String title = twoFourFive.getSubfieldsAsString("a");
	    String titleTwo = twoFourFive.getSubfieldsAsString("b");
	    String titleThree = twoFourFive.getSubfieldsAsString("c");
	    
	    if (titleTwo != null) title += " " + titleTwo;
	    if (titleThree != null) title += " " + titleThree;
	    
	    //SAVE THE TITLE FOR THE RESPONSE MESSAGE
	    responseMessage.put("title", title);
	    
	    //GET THE 980s FROM THE MARC RECORD
	    DataField nineEighty = (DataField) record.getVariableField("980");
	    String fundCode = nineEighty.getSubfieldsAsString("b");
	    String vendorCode =  nineEighty.getSubfieldsAsString("v");
	    String notes =  nineEighty.getSubfieldsAsString("n");
	    String quantity =  nineEighty.getSubfieldsAsString("q");
	    String price = nineEighty.getSubfieldsAsString("m");
	    String electronicIndicator = nineEighty.getSubfieldsAsString("z");
	    String vendorItemId = nineEighty.getSubfieldsAsString("c");
	    Integer quanityNo = 0;
	    if (quantity != null)  quanityNo = Integer.valueOf(quantity);
	    
	    //IS THIS AN ELECTRONIC RESOURCE?
	    if (electronicIndicator != null && electronicIndicator.equalsIgnoreCase("ELECTRONIC")) electronic = true;
	    
	    //LOOK UP THE FUND AND THE VENDOR
	    String organizationEndpoint = baseOkapEndpoint + "organizations-storage/organizations?limit=30&offset=0&query=((code='" + vendorCode + "'))";
	    String fundEndpoint = baseOkapEndpoint + "finance/funds?limit=30&offset=0&query=((code='" + fundCode + "'))";
	    
	    String orgLookupResponse = callApiGet(organizationEndpoint,  token);
		JSONObject orgObject = new JSONObject(orgLookupResponse);
		String vendorId = (String) orgObject.getJSONArray("organizations").getJSONObject(0).get("id");
		logger.info("ORGANIZATIONS: " + orgObject.get("organizations"));
		
		String fundResponse = callApiGet(fundEndpoint, token);
		JSONObject fundsObject = new JSONObject(fundResponse);
		String fundId = (String) fundsObject.getJSONArray("funds").getJSONObject(0).get("id");
		logger.info("FUNDS: " + fundsObject.get("funds"));
		
		
		//INSERT THE ORDER AND ORDER LINE:
		
		 //GET THE NEXT PO NUMBER
		String poNumber = callApiGet(baseOkapEndpoint + "orders/po-number", token);
		JSONObject poNumberObj = new JSONObject(poNumber);
		logger.info("NEXT PO NUMBER: " + poNumberObj.get("poNumber"));
		
		
		// POST TO COMPOSITE ORDERS
		JSONObject order = new JSONObject();
		order.put("poNumber", poNumberObj.get("poNumber"));
		order.put("vendor", vendorId);
		order.put("orderType", "One-Time");
		order.put("reEncumber", false);
		order.put("id", orderUUID.toString());
		order.put("approved", true);
		String orderResponse = callApiPost(baseOkapEndpoint + "orders/composite-orders",order,token); 
		JSONObject approvedOrder = new JSONObject(orderResponse);
		logger.info(orderResponse);

		
		// POST ORDER LINE
		//FOLIO WILL CREATE THE INSTANCE, HOLDINGS, ITEM
		JSONObject orderLine = new JSONObject();
		JSONObject cost = new JSONObject();
		JSONObject location = new JSONObject();
		JSONArray locations = new JSONArray();
		if (electronic) {
			orderLine.put("orderFormat", "Electronic Resource");
			JSONObject eResource = new JSONObject();
			eResource.put("activated", false);
			eResource.put("createInventory", "Instance, Holding");
			eResource.put("trial", false);
			eResource.put("accessProvider", vendorId);
			orderLine.put("eresource",eResource);
			orderLine.put("orderFormat", "Electronic Resource");
			cost.put("quantityElectronic", quanityNo);
			cost.put("listUnitPriceElectronic", price);
		}
		else {
			JSONObject physical = new JSONObject();
			physical.put("createInventory", "Instance, Holding, Item");
			physical.put("materialType", lookupTable.get(materialTypeName));
			orderLine.put("physical", physical);
			orderLine.put("orderFormat", "Physical Resource");
			cost.put("listUnitPrice", price);
			cost.put("quantityPhysical", 1);
			location.put("quantityPhysical",quanityNo);
			location.put("locationId",lookupTable.get(permLocationName));
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
		//TODO:
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
		
		String lineResponse = callApiPost(baseOkapEndpoint + "orders/order-lines",orderLine,token); 
		logger.info(lineResponse);
		
		
		//INSERT THE NOTE IF THERE IS A NOTE IN THE MARC RECORD
		if (notes != null && !notes.equalsIgnoreCase("")) {
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
			String noteResponse = callApiPost(baseOkapEndpoint + "/notes",noteAsJson,token); 
			logger.info(noteResponse);
		}

		

		
		//OPEN THE ORDER - ENCUMBERS THE MONEY, CREATES THE INSTANCE, HOLDING & ITEM-IF PHYSICAL
		approvedOrder.put("workflowStatus","Open");
		String openResponse = callApiPut(baseOkapEndpoint + "orders/composite-orders/" +orderUUID.toString() ,approvedOrder,token); 
		logger.info(openResponse);

		
		//GET THE UPDATED PURCHASE ORDER FROM THE API AND PULL OUT THE ID FOR THE INSTANCE FOLIO CREATED:
		String updatedPurchaseOrder = callApiGet(baseOkapEndpoint + "orders/composite-orders/" +orderUUID.toString() ,token); 
		JSONObject updatedPurchaseOrderJson = new JSONObject(updatedPurchaseOrder);
		String instanceId = updatedPurchaseOrderJson.getJSONArray("compositePoLines").getJSONObject(0).getString("instanceId");

		
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
	    
	    
	    //TRANSFORM THE RECORD INTO JSON
	    logger.info("MARC RECORD: " + record.toString());
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    MarcJsonWriter jsonWriter =  new MarcJsonWriter(baos);
	    jsonWriter.write(record);
	    jsonWriter.close();
	    String jsonString = baos.toString();
	    JSONObject mRecord = new JSONObject(jsonString);
	    JSONObject content = new JSONObject();
	    content.put("content",mRecord);
	    logger.info("MARC TO JSON: " + mRecord);
	    

		//GET THE RAW MARC READY TO POST TO THE API
	    ByteArrayOutputStream rawBaos = new ByteArrayOutputStream();
	    MarcWriter writer = new MarcStreamWriter(rawBaos,"UTF-8");
	    writer.write(record);
		JSONObject jsonWithRaw = new JSONObject();
		jsonWithRaw.put("id", rawRecordId);
		jsonWithRaw.put("content",rawBaos.toString("UTF8"));
		
		//CREATING JOB EXECUTION?
		//TODO: I'M NOT ENTIRELY SURE IF THIS IS NECESSARY?
		//WHAT THE CONSEQUENCES OF THIS ARE?
		//TO POST TO SOURCE RECORD STORAGE, A SNAPSHOT ID
		//SEEMS TO BE REQUIRECD
		JSONObject jobExecution = new JSONObject();
		jobExecution.put("jobExecutionId", snapshotId.toString());
		jobExecution.put("status", "PARSING_IN_PROGRESS");
		String snapShotResponse = callApiPost(baseOkapEndpoint + "source-storage/snapshots",  jobExecution,token);
		
		//OBJECT FOR SOURCE RECORD STORAGE API CALL:
		JSONObject sourceRecordStorageObject = new JSONObject();
		sourceRecordStorageObject.put("recordType", "MARC");
		sourceRecordStorageObject.put("snapshotId",snapshotId.toString());
		sourceRecordStorageObject.put("matchedId", matchedId.toString());
		//LINK THE INSTANCE TO SOURCE RECORD STORAGE
		JSONObject externalId = new JSONObject();
		externalId.put("instanceId",instanceId);
		sourceRecordStorageObject.put("externalIdsHolder", externalId);
		//RAW RECORD
		JSONObject rawRecordObject = new JSONObject();
		rawRecordObject.put("id",rawRecordId.toString());
		rawRecordObject.put("content",jsonWithRaw.toString());
		//PARSED RECORD
		JSONObject parsedRecord = new JSONObject();
		parsedRecord.put("id", parsedRecordId.toString());
		parsedRecord.put("content", mRecord);
		sourceRecordStorageObject.put("rawRecord", rawRecordObject);
		sourceRecordStorageObject.put("parsedRecord", parsedRecord);
		
		String storageResponse = callApiPost(baseOkapEndpoint + "source-storage/records", sourceRecordStorageObject,token);
		
		//GET THE INSTANCE RECORD FOLIO CREATED, SO WE CAN ADD BIB INFO TO IT:
		String instanceResponse = callApiGet(baseOkapEndpoint + "inventory/instances/" + instanceId, token);
		JSONObject instanceAsJson = new JSONObject(instanceResponse);

		//ADD IDENTIFIERS AND CONTRIBUTORS TO THE INSTANCE
		//*AND* CHANGE THE SOURCE TO 'MARC'
		//SO THE OPTION TO VIEW THE MARC RECORD SHOWS UP 
		//IN INVENTORY!
		JSONArray identifiers = buildIdentifiers(record,lookupTable);
		JSONArray contributors = buildContributors(record, lookupTable);
		instanceAsJson.put("title", title);
		instanceAsJson.put("source", "MARC");
		instanceAsJson.put("instanceTypeId", lookupTable.get("text"));
		instanceAsJson.put("identifiers", identifiers);
		instanceAsJson.put("contributors", contributors);
		instanceAsJson.put("discoverySuppress", true);
		String instanceUpdateResponse = callApiPut(baseOkapEndpoint + "inventory/instances/" + instanceId,  instanceAsJson,token);
		
		//GET THE HOLDINGS RECORD FOLIO CREATED, SO WE CAN ADD URLs FROM THE 856 IN THE MARC RECORD
		String holdingResponse = callApiGet(baseOkapEndpoint + "holdings-storage/holdings?query=(instanceId==" + instanceId + ")", token);
		JSONObject holdingsAsJson = new JSONObject(holdingResponse);
		JSONObject holdingRecord = holdingsAsJson.getJSONArray("holdingsRecords").getJSONObject(0);
		
		JSONArray eResources = new JSONArray();
		String linkText = (String) getMyContext().getAttribute("textForElectronicResources");
		List urls =  record.getVariableFields("856");
		Iterator<DataField> iterator = urls.iterator();
		while (iterator.hasNext()) {
			DataField dataField = (DataField) iterator.next();
			if (dataField != null && dataField.getSubfield('u') != null) {
				String url = dataField.getSubfield('u').getData();
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
		//UPDATE THE HOLDINGS RECORD
		holdingRecord.put("electronicAccess", eResources);
		String createHoldingsResponse = callApiPut(baseOkapEndpoint + "holdings-storage/holdings/" + holdingRecord.getString("id"), holdingRecord,token);

		
		//SAVE THE PO NUMBER FOR THE RESPONSE
		responseMessage.put("PONumber", poNumberObj.get("poNumber"));
		return responseMessage;
	}
	
	
	public  HashMap<String,String> lookupReferenceValues(List<String> lookupTables,String token) throws IOException, InterruptedException, Exception  {
		Map<String, String> lookUpTable = new HashMap<String,String>();

		Iterator<String> lookupTablesIterator = lookupTables.iterator();
		while (lookupTablesIterator.hasNext()) {
			String endpoint = lookupTablesIterator.next();
			String response = callApiGet(endpoint,token);
			JSONObject jsonObject = new JSONObject(response);
			//TODO - IMPROVE THIS
			//(0) IS THE NUMBER OF ITEMS FOUND
			//(1) IS THE ARRAY OF ITEMS
			//NOT A GOOD APPROACH
			String elementName = (String) jsonObject.names().get(1);	
			JSONArray elements = jsonObject.getJSONArray(elementName);
			Iterator elementsIterator = elements.iterator();
			while (elementsIterator.hasNext()) {
				JSONObject element = (JSONObject) elementsIterator.next();
				String id = element.getString("id");
				String name = element.getString("name");
				//SAVING ALL OF THE 'NAMES' SO THE UUIDs CAN BE LOOKED UP
				lookUpTable.put(name,id);		
			}
		}

		return (HashMap<String, String>) lookUpTable;
	}
	
	
	//TODO - FIX THESE METHODS THAT GATHER DETAILS FROM THE MARC RECORD.
	//THEY WERE HURRILY CODED
	//JUST WANTED TO GET SOME DATA IN THE INSTANCE
	//FROM THE MARC RECORD FOR THIS POC
	public JSONArray buildContributors(Record record,HashMap<String,String> lookupTable) {
		JSONArray contributors = new JSONArray();
		List fields = record.getDataFields();
		Iterator fieldsIterator = fields.iterator();
		while (fieldsIterator.hasNext()) {
			DataField field = (DataField) fieldsIterator.next();
			if (field.getTag().equalsIgnoreCase("100") || field.getTag().equalsIgnoreCase("700")) {
				contributors.put(makeContributor(field,lookupTable,"Personal name", new String[]{"a","b","c","d","f","g","j","k","l","n","p","t","u"}));
			}
		}
		return contributors;
	}
	
	public JSONObject makeContributor( DataField field, HashMap<String,String> lookupTable, String name_type_id, String[] subfieldArray) {
		List<String> list = Arrays.asList(subfieldArray);
		//type_text_field='e'
		JSONObject contributor = new JSONObject();
		contributor.put("name", "");
		contributor.put("contributorNameTypeId", lookupTable.get(name_type_id));
		List subfields =  field.getSubfields();
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
	
	
   public JSONArray buildIdentifiers(Record record,HashMap<String,String> lookupTable) {
		JSONArray identifiers = new JSONArray();
		
		List fields = record.getDataFields();
		Iterator fieldsIterator = fields.iterator();
		while (fieldsIterator.hasNext()) {
			DataField field = (DataField) fieldsIterator.next();
			System.out.println(field.getTag());
			List subfields =  field.getSubfields();
			Iterator subfieldIterator = subfields.iterator();
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
						//JSONObject identifier = new JSONObject();
						//identifier.put(lookupTable.get("ISBN"), subfield.getData());
						identifiers.put(identifier);
					}
					if (subfield.getCode() == 'z') {
						JSONObject identifier = new JSONObject();
						String fullValue = subfield.getData();
						if (field.getSubfield('c') != null) fullValue += " " + field.getSubfieldsAsString("c");
						if (field.getSubfield('q') != null) fullValue += " " + field.getSubfieldsAsString("q");
						identifier.put("value", fullValue);
						identifier.put("identifierTypeId", lookupTable.get("Invalid ISBN"));
						//JSONObject identifier = new JSONObject();
						//identifier.put(lookupTable.get("ISBN"), subfield.getData());
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
						//JSONObject identifier = new JSONObject();
						//identifier.put(lookupTable.get("ISBN"), subfield.getData());
						identifiers.put(identifier);
					}
					else if (subfield.getCode() == 'l') {
						JSONObject identifier = new JSONObject();
						String fullValue = subfield.getData();
						if (field.getSubfield('c') != null) fullValue += " " + field.getSubfieldsAsString("c");
						if (field.getSubfield('q') != null) fullValue += " " + field.getSubfieldsAsString("q");
						identifier.put("value", fullValue);
						identifier.put("identifierTypeId", lookupTable.get("Linking ISSN"));
						//JSONObject identifier = new JSONObject();
						//identifier.put(lookupTable.get("ISBN"), subfield.getData());
						identifiers.put(identifier);
					}
					else {
						JSONObject identifier = new JSONObject();
						String fullValue = "";
						if (field.getSubfield('z') != null) fullValue += field.getSubfieldsAsString("z");
						if (field.getSubfield('y') != null) fullValue += " " +  field.getSubfieldsAsString("y");
						if (field.getSubfield('m') != null) fullValue += " " + field.getSubfieldsAsString("m");
						if (fullValue != "") {
							identifier.put("value", fullValue);
							identifier.put("identifierTypeId", lookupTable.get("Invalid ISSN"));
							//JSONObject identifier = new JSONObject();
							//identifier.put(lookupTable.get("ISBN"), subfield.getData());
							identifiers.put(identifier);
						}
					}
				}
				
				
			}
			
		}
		return identifiers;
		
		
	}
	
	
	
	public String callApiGet(String url, String token) throws Exception, IOException, InterruptedException {
				CloseableHttpClient client = HttpClients.custom().build();
				HttpUriRequest request = RequestBuilder.get().setUri(url)
				.setHeader("x-okapi-tenant", tenant)
				.setHeader("x-okapi-token", token)
				.setHeader("Accept", "application/json")
				.setHeader("content-type","application/json")
				.build();


				HttpResponse response = client.execute(request);
				HttpEntity entity = response.getEntity();
				String responseString = EntityUtils.toString(entity, "UTF-8");
				int responseCode = response.getStatusLine().getStatusCode();

				logger.info("GET:");
				logger.info(url);
				logger.info(responseCode);
				logger.info(responseString);

				if (responseCode > 399) {
					throw new Exception(responseString);
				}

				return responseString;

	}

	public String callApiPost(String url, JSONObject body, String token)
			throws Exception, IOException, InterruptedException {
		CloseableHttpClient client = HttpClients.custom().build();
		HttpUriRequest request = RequestBuilder.post()
				.setUri(url)
				.setHeader("x-okapi-tenant", tenant)
				.setHeader("x-okapi-token", token)
				.setEntity(new StringEntity(body.toString()))
				.setHeader("Accept", "application/json")
				.setHeader("content-type","application/json")
				.build();

		HttpResponse response = client.execute(request);
		HttpEntity entity = response.getEntity();
		String responseString = EntityUtils.toString(entity, "UTF-8");
		int responseCode = response.getStatusLine().getStatusCode();

		logger.info("POST:");
		logger.info(body.toString());
		logger.info(url);
		logger.info(responseCode);
		logger.info(responseString);

		if (responseCode > 399) {
			throw new Exception(responseString);
		}

		return responseString;

	}
	
	public String callApiPut(String url, JSONObject body, String token)
			throws Exception, IOException, InterruptedException {
		CloseableHttpClient client = HttpClients.custom().build();
		HttpUriRequest request = RequestBuilder.put()
				.setUri(url)
				.setEntity(new StringEntity(body.toString()))
				.setHeader("x-okapi-tenant", tenant)
				.setHeader("x-okapi-token", token)
				.setHeader("Accept", "application/json")
				.setHeader("Content-type","application/json")
				.build();
		
		//TODO
		//UGLY WORK-AROUND
		//THE ORDERS-STORAGE ENDOINT WANTS 'TEXT/PLAIN'
		//THE OTHER API CALL THAT USES PUT,
		//WANTS 'APPLICATION/JSON'
		if (url.contains("orders-storage") || url.contains("holdings-storage")) {
			request.setHeader("Accept","text/plain");
		}
		HttpResponse response = client.execute(request);
		HttpEntity entity = response.getEntity();
		int responseCode = response.getStatusLine().getStatusCode();

		logger.info("PUT:");
		logger.info(body.toString());
		logger.info(url);
		logger.info(responseCode);
		//logger.info(responseString);

		if (responseCode > 399) {
			throw new Exception("Response: " + responseCode);
		}

		return "ok";

	}
	
	public  String callApiAuth(String url,  JSONObject  body)
			throws Exception, IOException, InterruptedException {
		    CloseableHttpClient client = HttpClients.custom().build();
		    HttpUriRequest request = RequestBuilder.post()
		    		.setUri(url)
		    		.setEntity(new StringEntity(body.toString()))
					.setHeader("x-okapi-tenant",tenant)
					.setHeader("Accept", "application/json").setVersion(HttpVersion.HTTP_1_1)
					.setHeader("content-type","application/json")
					.build();

		    CloseableHttpResponse response = client.execute(request);
			HttpEntity entity = response.getEntity();
			String responseString = EntityUtils.toString(entity, "UTF-8");
			int responseCode = response.getStatusLine().getStatusCode();

			logger.info("POST:");
			logger.info(body.toString());
			logger.info(url);
			logger.info(responseCode);
			logger.info(responseString);

			if (responseCode > 399) {
				//String responseBody = processErrorResponse(responseString);
				throw new Exception(responseString);
			}

			
			String token = response.getFirstHeader("x-okapi-token").getValue();
			return token;

	}


	public ServletContext getMyContext() {
		return myContext;
	}


	public void setMyContext(ServletContext myContext) {
		this.myContext = myContext;
	}
	
	
	


	

}
