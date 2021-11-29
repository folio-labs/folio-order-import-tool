package org.olf.folio.order;

import org.apache.log4j.Logger;
import org.folio.isbn.IsbnUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.marc.Record;
import org.olf.folio.order.storage.FolioData;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class RecordChecker {

  boolean foundErrors = false;
  final Config config;
  private static Logger logger = Logger.getLogger(RecordChecker.class);

  public RecordChecker (Config config) {
    this.config = config;
  }

  public JSONArray validateMarcRecords(String fileName) throws FileNotFoundException {
    InputStream in = new FileInputStream(config.uploadFilePath + fileName);
    MarcReader reader = new MarcStreamReader(in);
    Record record;
    JSONArray responseMessages = new JSONArray();
    int r = 0;
    while(reader.hasNext()) {
      record = reader.next();
      r++;
      MarcRecordMapping marc = new MarcRecordMapping(record);
      responseMessages.put(validateMarcRecord(marc, r));
    }
    return responseMessages;
  }

  public JSONObject validateMarcRecord (MarcRecordMapping mappedMarc, int recNo) {
    JSONObject msg = new JSONObject();
    try {
      msg.put("recNo", recNo);
      msg.put("controlNumber", mappedMarc.controlNumber());
      msg.put("title", mappedMarc.title());
      msg.put("source", mappedMarc.marcRecord.toString());

      if (!mappedMarc.has980()) {
        msg.put("isError", true);
        msg.put("error", String.format("Record #%s is missing the 980 field", recNo));
        msg.put("PONumber", "~error~");
        msg.put("title", mappedMarc.title());
        foundErrors = true;
        return msg;
      }

      if (!mappedMarc.hasISBN()) {
        msg.put("isError", true);
        msg.put("error", "No ISBN." + ( config.onValidationErrorsSKipFailed ? " Record skipped " : "" ));
      } else if (!isValidIsbn(mappedMarc.getISBN())) {
        foundErrors = true;
        msg.put("error", "Invalid ISBN. " + ( config.onValidationErrorsSKipFailed ? " Record skipped " : "" ));
        msg.put("isError", true);
      }
      msg.put("invalidIsbn", (mappedMarc.hasISBN() && !isValidIsbn(mappedMarc.getISBN())));
      msg.put("noIsbn", (!mappedMarc.hasISBN()));
      msg.put("ISBN", mappedMarc.hasISBN() ? mappedMarc.getISBN()  : "No ISBN");
      msg.put("productIdentifiers", mappedMarc.getProductIdentifiers().toString());
      msg.put("instanceIdentifiers", mappedMarc.getInstanceIdentifiers().toString());

      Map<String, String> requiredFields = new HashMap<>();
      if (config.objectCodeRequired) {
        requiredFields.put("Object code", mappedMarc.objectCode());
      }
      requiredFields.put("Fund code", mappedMarc.fundCode());
      requiredFields.put("Vendor Code", mappedMarc.vendorCode());
      requiredFields.put("Price" , mappedMarc.price());

      // MAKE SURE EACH OF THE REQUIRED SUBFIELDS HAS DATA
      for (Map.Entry<String,String> entry : requiredFields.entrySet())  {
        if (entry.getValue()==null || entry.getValue().isEmpty()) {
          msg.put("error", entry.getKey() + " Missing. " + ( config.onValidationErrorsSKipFailed ? " Record skipped." : "" ) );
          msg.put("isError", true);
          foundErrors = true;
        }
      }

      //VALIDATE THE ORGANIZATION, OBJECT CODE AND FUND
      //STOP THE PROCESS IF ANY ERRORS WERE FOUND
      JSONObject orgValidationResult = FolioData.validateOrganization(mappedMarc.vendorCode(), mappedMarc.title());
      if (orgValidationResult != null) {
        msg.put("error", msg.getString("error") + " " + orgValidationResult.getString("error"));
        msg.put("isError", true);
        foundErrors = true;
      }

      if (mappedMarc.hasObjectCode()) {
        JSONObject objectValidationResult = FolioData.validateObjectCode(mappedMarc.objectCode(), mappedMarc.title());
        if (objectValidationResult != null) {
          msg.put("error", msg.getString("error") + " " + objectValidationResult.getString("error"));
          msg.put("isError", true);
          foundErrors = true;
        }
      }
      if (mappedMarc.hasProjectCode()) {
        // TODO: Check this
        JSONObject projectValidationResult = FolioData.validateObjectCode(mappedMarc.projectCode(), mappedMarc.title());
        if (projectValidationResult != null) {
          msg.put("error", msg.getString("error") + " " + projectValidationResult.getString("error"));
          msg.put("isError", true);
          foundErrors = true;
        }
      }
      JSONObject fundValidationResult = FolioData.validateFund(mappedMarc.fundCode(), mappedMarc.title(), mappedMarc.price());
      if (fundValidationResult != null) {
        msg.put("error", msg.getString("error") + " " + fundValidationResult.getString("error"));
        msg.put("isError", true);
        foundErrors = true;
      }

      if (config.importInvoice) {
        JSONObject invoiceValidationResult = FolioData.validateRequiredValuesForInvoice(mappedMarc.title(), mappedMarc.marcRecord);
        if (invoiceValidationResult != null) {
          msg.put("error", msg.getString("error") + " " + invoiceValidationResult.getString("error"));
          msg.put("isError", true);
          foundErrors = true;
        }
      }

    }	catch(Exception e) {
      logger.error("Got exception when validating MARC record: " + e.getMessage() + " " + e.getClass());
      msg.put("isError", true);
      msg.put("error", e.getMessage());
    }
    logger.info("Validation result: " + msg);
    return msg;
  }

  public static boolean isValidIsbn (String isbn) {
    if (isbn.length() == 10) {
      return IsbnUtil.isValid10DigitNumber(isbn);
    } else if (isbn.length() == 13) {
      return IsbnUtil.isValid13DigitNumber(isbn);
    } else {
      return false;
    }
  }

  public boolean errorsFound () {
    return foundErrors;
  }

}
