package org.olf.folio.order.recordvalidation;

import org.apache.log4j.Logger;
import org.folio.isbn.IsbnUtil;
import org.json.JSONObject;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.marc.Record;
import org.olf.folio.order.Config;
import org.olf.folio.order.MarcRecordMapping;
import org.olf.folio.order.storage.FolioData;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class RecordChecker {

  final Config config;
  private static Logger logger = Logger.getLogger(RecordChecker.class);

  public RecordChecker (Config config) {
    this.config = config;
  }

  public JSONObject validateMarcRecords(String fileName) throws FileNotFoundException {
    InputStream in = new FileInputStream(config.uploadFilePath + fileName);
    MarcReader reader = new MarcStreamReader(in);
    Record record;
    ServiceResponse validationResults = new ServiceResponse (false);
    while(reader.hasNext()) {
      record = reader.next();
      MarcRecordMapping marc = new MarcRecordMapping(record);
      validateMarcRecord(marc, validationResults.nextResult());
    }
    return validationResults.toJson();
  }

  public void validateMarcRecord (MarcRecordMapping mappedMarc, RecordResult result) {
    try {
      result.setInputMarcData(mappedMarc);

      if (!mappedMarc.has980()) {
        result
                .addValidationMessageIfNotNull("Record is missing the 980 field")
                .markSkipped(config.onValidationErrorsSKipFailed);
        return;
      }

      if (mappedMarc.hasISBN() && !isValidIsbn(mappedMarc.getISBN())) {
        result
                .addValidationMessageIfNotNull("ISBN is invalid")
                .markSkipped(config.onValidationErrorsSKipFailed);
      }

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
          result.addValidationMessageIfNotNull("Mandatory data element " + entry.getKey() + " missing.")
                  .markSkipped(config.onValidationErrorsSKipFailed);
        }
      }

      //VALIDATE THE ORGANIZATION, OBJECT CODE AND FUND
      //STOP THE PROCESS IF ANY ERRORS WERE FOUND
      String orgValidationResult = FolioData.validateOrganization(mappedMarc.vendorCode(), mappedMarc.title());
      if (orgValidationResult != null) {
        result.addValidationMessageIfNotNull(orgValidationResult)
                .markSkipped(config.onValidationErrorsSKipFailed);
      }

      if (mappedMarc.hasObjectCode()) {
        result.addValidationMessageIfNotNull(
                FolioData.validateObjectCode(mappedMarc.objectCode(), mappedMarc.title()))
                .markSkipped(config.onValidationErrorsSKipFailed);
      }
      if (mappedMarc.hasProjectCode()) {
        result.addValidationMessageIfNotNull(
                FolioData.validateObjectCode(mappedMarc.projectCode(), mappedMarc.title()))
                .markSkipped(config.onValidationErrorsSKipFailed);
      }
      //result.addErrorMessageIfNotNull(FolioData.validateFund(mappedMarc.fundCode()));

      if (config.importInvoice) {
        result.addValidationMessageIfNotNull(
                FolioData.validateRequiredValuesForInvoice(mappedMarc.title(), mappedMarc.getRecord()))
                .markSkipped(config.onValidationErrorsSKipFailed);
      }

    }	catch(Exception e) {
      result.addValidationMessageIfNotNull("Got exception when validating MARC record: " + e.getMessage() + " " + e.getClass());
    }
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

  private String maybePlural (int count, String text) {
    return count == 1 ? (count + " " + text) : (count + " " + text + "s");
  }


}
