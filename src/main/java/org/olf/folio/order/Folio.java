package org.olf.folio.order;

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
import org.json.JSONArray;
import org.json.JSONObject;
import org.marc4j.MarcJsonWriter;
import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcWriter;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Folio {

  private static Config config;
  private static String token;
  private static Logger logger;


  /**
   * Capture configuration from import.properties, authenticate to FOLIO and retrieve reference data
   * @param config Static configuration from import.properties
   * @param logger Logger.
   * @throws Exception If FOLIO back-end request fails.
   */
  public static void initialize(Config config, Logger logger) throws Exception {
    //GET THE FOLIO TOKEN
    Folio.config = config;
    Folio.logger = logger;
    token = authenticate();
  }

  private static String authenticate()
          throws Exception {
    JSONObject authObject = new JSONObject();
    authObject.put("username", config.apiUsername);
    authObject.put("password", config.apiPassword);
    authObject.put("tenant", config.tenant);
    CloseableHttpClient client = HttpClients.custom().build();
    String authUrl = config.baseOkapiEndpoint + "authn/login";
    HttpUriRequest request = RequestBuilder.post()
            .setUri(authUrl)
            .setEntity(new StringEntity(authObject.toString()))
            .setHeader("x-okapi-tenant",config.tenant)
            .setHeader("Accept", "application/json").setVersion(HttpVersion.HTTP_1_1)
            .setHeader("content-type","application/json")
            .build();

    CloseableHttpResponse response = client.execute(request);
    HttpEntity entity = response.getEntity();
    String responseString = EntityUtils.toString(entity);
    int responseCode = response.getStatusLine().getStatusCode();

    logger.info("POST:");
    logger.info(authObject.remove("password").toString());
    logger.info(authUrl);
    logger.info(responseCode);
    logger.info(responseString);

    if (responseCode > 399) {
      throw new Exception(responseString);
    }

    Folio.token = response.getFirstHeader("x-okapi-token").getValue();
    return token;
  }

  public static String callApiGet(String url) throws Exception {
    CloseableHttpClient client = HttpClients.custom().build();
    HttpUriRequest request = RequestBuilder.get().setUri(config.baseOkapiEndpoint + url)
            .setHeader("x-okapi-tenant", config.tenant)
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

  public static String callApiPut(String url, JSONObject body)
          throws Exception {
    CloseableHttpClient client = HttpClients.custom().build();
    HttpUriRequest request = RequestBuilder.put()
            .setUri(config.baseOkapiEndpoint + url)
            .setCharset(Charset.defaultCharset())
            .setEntity(new StringEntity(body.toString(),"UTF8"))
            .setHeader("x-okapi-tenant", config.tenant)
            .setHeader("x-okapi-token", token)
            .setHeader("Accept", "application/json")
            .setHeader("Content-type","application/json")
            .build();

    //THE ORDERS-STORAGE ENDPOINT WANTS 'TEXT/PLAIN'
    //THE OTHER API CALL THAT USES PUT,
    //WANTS 'APPLICATION/JSON'
    if (url.contains("orders-storage") || url.contains("holdings-storage")) {
      request.setHeader("Accept","text/plain");
    }
    HttpResponse response = client.execute(request);
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

  public static String getNextPoNumberFromOrders() throws Exception {
    String jsonResponse = callApiGet("orders/po-number");
    return new JSONObject(jsonResponse).getString("poNumber");
  }

  //POST TO PO SEEMS TO WANT UTF8 (FOR SPECIAL CHARS)
  //IF UTF8 IS USED TO POST TO SOURCE RECORD STORAGE
  //SPECIAL CHARS DON'T LOOK CORRECT
  public static String callApiPostWithUtf8(String url, JSONObject body)
          throws Exception {
    CloseableHttpClient client = HttpClients.custom().build();
    HttpUriRequest request = RequestBuilder.post()
            .setUri(config.baseOkapiEndpoint + url)
            .setHeader("x-okapi-tenant", config.tenant)
            .setHeader("x-okapi-token", token)
            .setEntity(new StringEntity(body.toString(),"UTF-8"))
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


  public static Map<String,String> lookupReferenceValuesInFolio(List<String> lookupTables) throws Exception  {
    Map<String, String> lookUpTable = new HashMap<>();

    Iterator<String> lookupTablesIterator = lookupTables.iterator();
    while (lookupTablesIterator.hasNext()) {
      String endpoint = lookupTablesIterator.next();
      String response = callApiGet(endpoint);
      JSONObject jsonObject = new JSONObject(response);
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
        if (endpoint.contains("locations")) name = name + "-location";
        //SAVING ALL OF THE 'NAMES' SO THE UUIDs CAN BE LOOKED UP
        lookUpTable.put(name,id);
      }
    }
    return lookUpTable;
  }

  public static JSONObject validateFund(String fundCode, String title, String price ) throws Exception {

    JSONObject responseMessage = new JSONObject();

    //GET CURRENT FISCAL YEAR
    String fiscalYearCode = config.fiscalYearCode;
    String fiscalYearsEndpoint = "finance/fiscal-years?query=(code='"  + fiscalYearCode + "')";
    String fiscalYearResponse = callApiGet(fiscalYearsEndpoint);
    JSONObject fiscalYearsObject = new JSONObject(fiscalYearResponse);
    //----------->VALIDATION #1: MAKE SURE THE FISCAL YEAR CODE EXISTS
    if (fiscalYearsObject.getJSONArray("fiscalYears").length() < 1) {
      responseMessage.put("error", "Fiscal year code in file (" + fiscalYearCode + ") does not exist in FOLIO");
      responseMessage.put("PONumber", "~error~");
      return responseMessage;
    }
    String fiscalYearId = (String) new JSONObject(fiscalYearResponse).getJSONArray("fiscalYears").getJSONObject(0).get("id");

    // GET FUND
    String fundEndpoint = "finance/funds?limit=30&offset=0&query=((code='" + fundCode + "'))";
    String fundResponse = callApiGet(fundEndpoint);
    JSONObject fundsObject = new JSONObject(fundResponse);
    //----------->VALIDATION #2: MAKE SURE THE FUND CODE EXISTS
    if (fundsObject.getJSONArray("funds").length() < 1) {
      responseMessage.put("error", "Fund code in file (" + fundCode + ") does not exist in FOLIO");
      responseMessage.put("PONumber", "~error~");
      return responseMessage;
    }
    String fundId = (String) fundsObject.getJSONArray("funds").getJSONObject(0).get("id");
    logger.info("FUNDS: " + fundsObject.get("funds"));

    //----------->VALIDATION #3: MAKE SURE THE FUND CODE EXISTS FOR THE CURRENT FISCAL YEAR
    String fundBalanceQuery = "finance/budgets?query=(fundId==" + "%22" + fundId + "%22" + "+and+" + "fiscalYearId==" + "%22" + fiscalYearId + "%22)";
    String fundBalanceResponse = callApiGet(fundBalanceQuery);
    JSONObject fundBalanceObject = new JSONObject(fundBalanceResponse);
    if (fundBalanceObject.getJSONArray("budgets").length() < 1) {
      responseMessage.put("error", "Fund code in file (" + fundCode + ") does not have a budget for fiscal year code in file (" + fiscalYearCode +")");
      responseMessage.put("title", title);
      responseMessage.put("PONumber", "~error~");
      return responseMessage;
    }
    return null;
  }

  public static JSONObject validateObjectCode(String objectCode, String title) throws Exception {
    //---------->VALIDATION: MAKE SURE THE TAG (AKA OBJECT CODE) EXISTS
    JSONObject responseMessage = new JSONObject();
    String tagEndpoint = "tags?query=(label==" + objectCode + ")";
    String tagResponse = callApiGet(tagEndpoint);
    JSONObject tagObject = new JSONObject(tagResponse);
    if (tagObject.getJSONArray("tags").length() < 1) {
      responseMessage.put("error", "Object code in the record (" + objectCode + ") does not exist in FOLIO");
      responseMessage.put("title", title);
      responseMessage.put("PONumber", "~error~");
      return responseMessage;
    }
    return null;
  }

  public static JSONObject validateOrganization(String orgCode, String title, UuidMapping uuidMapping) throws Exception {
    JSONObject responseMessage = new JSONObject();
    if (uuidMapping.getOrganizationId(orgCode) == null) {
      responseMessage.put("error", "Organization code in file (" + orgCode + ") does not exist in FOLIO");
      responseMessage.put("title", title);
      responseMessage.put("PONumber", "~error~");
      return responseMessage;
    }
    return null;
  }

  // Use this?
  public static JSONObject validateAddress (String name, String title, UuidMapping uuidMapping) throws Exception {
    JSONObject responseMessage = new JSONObject();
    if (uuidMapping.getAddressIdByName(name) == null) {
      responseMessage.put("error", "Address with the (" + name + ") does not exist in FOLIO");
      responseMessage.put("title", title);
      responseMessage.put("PONumber", "~error~");
      return responseMessage;
    }
    return null;
  }

  public static JSONObject validateRequiredValuesForInvoice(String title, Record record) {

    DataField nineEighty = (DataField) record.getVariableField("980");
    String vendorInvoiceNo = nineEighty.getSubfieldsAsString("h");
    String invoiceDate = nineEighty.getSubfieldsAsString("i");

    if (vendorInvoiceNo == null && invoiceDate == null && config.failIfNoInvoiceData) {
      JSONObject responseMessage = new JSONObject();
      responseMessage.put("title", title);
      responseMessage.put("error", "Invoice data configured to be required and no Invoice data was found in MARC record");
      responseMessage.put("PONumber", "~error~");
      return responseMessage;
    } else if (vendorInvoiceNo != null || invoiceDate != null) { // if one of these is present both should be
      Map<String, String> requiredFields = new HashMap<>();
      requiredFields.put("Vendor invoice no", vendorInvoiceNo);
      requiredFields.put("Invoice date", invoiceDate);

      // MAKE SURE EACH OF THE REQUIRED SUBFIELDS HAS DATA
      for (Map.Entry<String, String> entry : requiredFields.entrySet()) {
        if (entry.getValue() == null) {
          JSONObject responseMessage = new JSONObject();
          responseMessage.put("title", title);
          responseMessage.put("error", entry.getKey() + " Missing");
          responseMessage.put("PONumber", "~error~");
          return responseMessage;
        }
      }
    }
    return null;
  }

  public static void storeMarcToSRS(Record record, ByteArrayOutputStream byteArrayOutputStream, UUID snapshotId, UUID recordTableId, String instanceId, String hrid ) throws Exception
  {
    logger.info("Storing MARC to SRS");
    //PREPARING TO ADD THE MARC RECORD TO SOURCE RECORD STORAGE:
    //CONSTRUCTING THE 999 OF THE MARC RECORD for FOLIO:
    DataField field = MarcFactory.newInstance().newDataField();
    field.setTag("999");
    field.setIndicator1('f');
    field.setIndicator2('f');
    Subfield one = MarcFactory.newInstance().newSubfield('i', instanceId );
    Subfield two = MarcFactory.newInstance().newSubfield('s', recordTableId.toString());
    field.addSubfield(one);
    field.addSubfield(two);
    record.addVariableField(field);
    if ( record.getControlNumberField() != null) {
      record.getControlNumberField().setData( hrid );
    }
    else {
      ControlField cf = MarcFactory.newInstance().newControlField("001");
      cf.setData( hrid );
      record.addVariableField(cf);
    }

    //TRANSFORM THE RECORD INTO JSON
    logger.info("MARC RECORD: " + record);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    MarcJsonWriter jsonWriter =  new MarcJsonWriter(baos);
    jsonWriter.setUnicodeNormalization(true);
    jsonWriter.write( record );
    jsonWriter.close();
    String jsonString = baos.toString();
    JSONObject mRecord = new JSONObject(jsonString);
    JSONObject content = new JSONObject();
    content.put("content",mRecord);
    logger.info("MARC TO JSON: " + mRecord);


    //GET THE RAW MARC READY TO POST TO THE API
    ByteArrayOutputStream rawBaos = new ByteArrayOutputStream();
    MarcWriter writer = new MarcStreamWriter(rawBaos);
    writer.write( record );
    JSONObject jsonWithRaw = new JSONObject();
    jsonWithRaw.put("id", instanceId );
    jsonWithRaw.put("content", byteArrayOutputStream );

    //CREATING JOB EXECUTION?
    //I'M NOT ENTIRELY SURE IF THIS IS NECESSARY?
    //WHAT THE CONSEQUENCES OF THIS ARE?
    //TO POST TO SOURCE RECORD STORAGE, A SNAPSHOT ID
    //SEEMS TO BE REQUIRED
    JSONObject jobExecution = new JSONObject();
    jobExecution.put("jobExecutionId", snapshotId.toString());
    jobExecution.put("status", "PARSING_IN_PROGRESS");
    Folio.callApiPostWithUtf8( "source-storage/snapshots",  jobExecution);

    //OBJECT FOR SOURCE RECORD STORAGE API CALL:
    JSONObject sourceRecordStorageObject = new JSONObject();
    sourceRecordStorageObject.put("recordType", "MARC");
    sourceRecordStorageObject.put("snapshotId", snapshotId.toString());
    sourceRecordStorageObject.put("matchedId", instanceId);
    //LINK THE INSTANCE TO SOURCE RECORD STORAGE
    JSONObject externalId = new JSONObject();
    externalId.put("instanceId", instanceId );
    sourceRecordStorageObject.put("externalIdsHolder", externalId);
    //RAW RECORD
    JSONObject rawRecordObject = new JSONObject();
    rawRecordObject.put("id", instanceId );
    rawRecordObject.put("content",jsonWithRaw.toString());
    //PARSED RECORD
    JSONObject parsedRecord = new JSONObject();
    parsedRecord.put("id", instanceId );
    parsedRecord.put("content", mRecord);
    sourceRecordStorageObject.put("rawRecord", rawRecordObject);
    sourceRecordStorageObject.put("parsedRecord", parsedRecord);
    sourceRecordStorageObject.put("id", instanceId );
    //CALL SOURCE RECORD STORAGE POST
    Folio.callApiPostWithUtf8( "source-storage/records", sourceRecordStorageObject);
  }

}
