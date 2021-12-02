package org.olf.folio.order.recordvalidation;

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


  public static JSONObject validateMarcRecords(String fileName) throws FileNotFoundException {
    InputStream in = new FileInputStream(Config.uploadFilePath + fileName);
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

  public static void validateMarcRecord (MarcRecordMapping mappedMarc, RecordResult outcome) {
    try {
      outcome.setInputMarcData(mappedMarc);

      if (!mappedMarc.has980()) {
        outcome.addValidationMessageIfNotNull("Record is missing the 980 field")
                .markSkipped(Config.onValidationErrorsSKipFailed);
        return;
      }

      if (mappedMarc.hasISBN() && isInvalidIsbn(mappedMarc.getISBN())) {
        if (Config.V_ON_ISBN_INVALID_REMOVE_ISBN.equalsIgnoreCase(Config.onIsbnInvalid)) {
          outcome.setFlagIfNotNull(
                  String.format(
                          "ISBN %s is not valid. Will remove the ISBN to continue",
                          mappedMarc.getISBN())
          );
        } else if (Config.V_ON_ISBN_INVALID_REPORT_ERROR.equalsIgnoreCase(Config.onIsbnInvalid)) {
          outcome.addValidationMessageIfNotNull("ISBN is invalid")
                  .markSkipped(Config.onValidationErrorsSKipFailed);
        }
      } else if (!mappedMarc.hasISBN()) {
        outcome.setFlagIfNotNull("No ISBN found. " +
                "This order import will trigger creation of a new Instance in FOLIO.");
      }

      Map<String, String> requiredFields = new HashMap<>();
      if (Config.objectCodeRequired) {
        requiredFields.put("Object code", mappedMarc.objectCode());
      }
      requiredFields.put("Fund code", mappedMarc.fundCode());
      requiredFields.put("Vendor Code", mappedMarc.vendorCode());
      requiredFields.put("Price" , mappedMarc.price());

      // MAKE SURE EACH OF THE REQUIRED SUBFIELDS HAS DATA
      for (Map.Entry<String,String> entry : requiredFields.entrySet())  {
        if (entry.getValue()==null || entry.getValue().isEmpty()) {
          outcome.addValidationMessageIfNotNull("Mandatory data element " + entry.getKey() + " missing.")
                  .markSkipped(Config.onValidationErrorsSKipFailed);
        }
      }

      //VALIDATE THE ORGANIZATION, OBJECT CODE AND FUND
      //STOP THE PROCESS IF ANY ERRORS WERE FOUND
      String orgValidationResult = FolioData.validateOrganization(mappedMarc.vendorCode());
      if (orgValidationResult != null) {
System.out.println("Putting error on org validation");
        outcome.addValidationMessageIfNotNull(orgValidationResult)
                .markSkipped(Config.onValidationErrorsSKipFailed);
      }
System.out.println("Validate object code for outcome " + outcome.asJson());
      if (mappedMarc.hasObjectCode()) {
        System.out.println("Validating object code? ");
        outcome.addValidationMessageIfNotNull(
                FolioData.validateObjectCode(mappedMarc.objectCode()))
                .markSkipped(Config.onValidationErrorsSKipFailed);
      }
      if (mappedMarc.hasProjectCode()) {
        outcome.addValidationMessageIfNotNull(
                FolioData.validateObjectCode(mappedMarc.projectCode()))
                .markSkipped(Config.onValidationErrorsSKipFailed);
      }
      //result.addErrorMessageIfNotNull(FolioData.validateFund(mappedMarc.fundCode()));

      if (Config.importInvoice) {
        outcome.addValidationMessageIfNotNull(
                FolioData.validateRequiredValuesForInvoice(mappedMarc.title(), mappedMarc.getRecord()))
                .markSkipped(Config.onValidationErrorsSKipFailed);
      }

    }	catch(Exception e) {
      outcome.addValidationMessageIfNotNull("Got exception when validating MARC record: " + e.getMessage() + " " + e.getClass());
    }
  }

  public static boolean isInvalidIsbn (String isbn) {
    return !isValidIsbn(isbn);
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

}
