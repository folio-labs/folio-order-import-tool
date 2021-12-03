package org.olf.folio.order.validation;

import org.json.JSONObject;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.marc.Record;
import org.olf.folio.order.Config;
import org.olf.folio.order.MarcRecordMapping;
import org.olf.folio.order.dataobjects.Instance;
import org.olf.folio.order.imports.RecordResult;
import org.olf.folio.order.imports.Results;
import org.olf.folio.order.storage.FolioData;
import org.olf.folio.order.utils.Utils;

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
    Results validationResults = new Results(false);
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

      if (mappedMarc.hasISBN() && Utils.isInvalidIsbn(mappedMarc.getISBN())) {
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
      } else if (!mappedMarc.hasISBN() &&
                 !mappedMarc.hasISSN() &&
                 !mappedMarc.hasPublisherOrDistributorNumber() &&
                 !mappedMarc.hasSystemControlNumber() &&
                 !mappedMarc.hasOtherStandardIdentifier()) {
        String existingInstancesMessage;
        JSONObject instances = FolioData.getInstancesByQuery("title=\"" + mappedMarc.title() + "\"");
        int totalRecords = instances.getInt("totalRecords");
        if (totalRecords > 0) {
          Instance firstExistingInstance  = Instance.fromJson((JSONObject) instances.getJSONArray(FolioData.INSTANCES_ARRAY).get(0));
          existingInstancesMessage = String.format(
                  "%s in Inventory with the same title%sHRID %s",
                  (totalRecords>1 ? " There are already " + totalRecords + " instances" : " There is already an Instance"),
                  (totalRecords>1 ? ", for example one with " : " and "),
                  firstExistingInstance.getHrid());
        } else {
          existingInstancesMessage = "Found no existing Instances with that exact title";
        }
        outcome.setFlagIfNotNull(
                "No ISBN, ISSN, Publisher number or distributor number, system control number," +
                        " or other standard identifier found in the record." +
                        " This order import might trigger the creation of a new Instance in FOLIO."
                + existingInstancesMessage);
      }

      Map<String, String> requiredFields = new HashMap<>();
      if (Config.objectCodeRequired) {
        requiredFields.put("Object code", mappedMarc.objectCode());
      }
      requiredFields.put(MarcRecordMapping.FUND_CODE_LABEL, mappedMarc.fundCode());
      requiredFields.put(MarcRecordMapping.VENDOR_CODE_LABEL, mappedMarc.vendorCode());
      requiredFields.put(MarcRecordMapping.PRICE_LABEL, mappedMarc.price());

      // MAKE SURE EACH OF THE REQUIRED SUBFIELDS HAS DATA
      for (Map.Entry<String,String> entry : requiredFields.entrySet())  {
        if (entry.getValue()==null || entry.getValue().isEmpty()) {
          String message = String.format("Mandatory data element %s missing (looked in %s).",
                  entry.getKey(), MarcRecordMapping.FOLIO_TO_MARC_FIELD_MAP.get(entry.getKey()));
          outcome.addValidationMessageIfNotNull(message)
                  .markSkipped(Config.onValidationErrorsSKipFailed);
        }
      }

      //VALIDATE THE ORGANIZATION, OBJECT CODE AND FUND
      //STOP THE PROCESS IF ANY ERRORS WERE FOUND
      String orgValidationResult = FolioData.validateOrganization(mappedMarc.vendorCode());
      if (orgValidationResult != null) {
        outcome.addValidationMessageIfNotNull(orgValidationResult)
                .markSkipped(Config.onValidationErrorsSKipFailed);
      }
      if (mappedMarc.hasObjectCode()) {
        outcome.addValidationMessageIfNotNull(
                FolioData.validateObjectCode(mappedMarc.objectCode()))
                .markSkipped(Config.onValidationErrorsSKipFailed);
      }
      if (mappedMarc.hasProjectCode()) {
        outcome.addValidationMessageIfNotNull(
                FolioData.validateObjectCode(mappedMarc.projectCode()))
                .markSkipped(Config.onValidationErrorsSKipFailed);
      }

      if (Config.importInvoice) {
        outcome.addValidationMessageIfNotNull(
                FolioData.validateRequiredValuesForInvoice(mappedMarc.title(), mappedMarc.getRecord()))
                .markSkipped(Config.onValidationErrorsSKipFailed);
      }

    }	catch(Exception e) {
      outcome.addValidationMessageIfNotNull("Got exception when validating MARC record: " + e.getMessage() + " " + e.getClass());
    }
  }

}
