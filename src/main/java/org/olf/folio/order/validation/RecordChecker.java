package org.olf.folio.order.validation;

import org.json.JSONObject;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.marc.Record;
import org.olf.folio.order.Config;
import org.olf.folio.order.MarcRecordMapping;
import org.olf.folio.order.dataobjects.Instance;
import org.olf.folio.order.imports.FileStorageHelper;
import org.olf.folio.order.imports.RecordResult;
import org.olf.folio.order.imports.Results;
import org.olf.folio.order.storage.FolioData;
import org.olf.folio.order.utils.Utils;

import java.io.FileNotFoundException;

public class RecordChecker {


  public static Results validateMarcRecords(FileStorageHelper fileStore) throws FileNotFoundException {
    MarcReader reader = new MarcStreamReader(fileStore.getMarcInputStream());
    Record record;
    Results validationResults = new Results(false, fileStore);
    int recordCount = 0;
    while(reader.hasNext()) {
      recordCount++;
      record = reader.next();
      MarcRecordMapping marc = new MarcRecordMapping(record);
      validateMarcRecord(marc, validationResults.nextResult());
    }
    validationResults.setMarcRecordCount(recordCount).markDone();
    return validationResults;
  }

  public static void validateMarcRecord (MarcRecordMapping mappedMarc, RecordResult outcome) {
    try {
      outcome.setInputMarcData(mappedMarc);

      // Sanity check
      if (!mappedMarc.has980()) {
        outcome.addValidationMessage("Record is missing the 980 field")
                .markSkipped(Config.onValidationErrorsSKipFailed);
        return;
      }

      // Check for mandatory fields. Check that mappings to FOLIO IDs resolve.
      if (!mappedMarc.hasFundCode()) {
        outcome.addValidationMessage("Record is missing required fund code (908$b)")
                .markSkipped(Config.onValidationErrorsSKipFailed);
      } else {
        String failMessage = FolioData.validateFund(mappedMarc.fundCode());
        if (failMessage != null) {
          outcome.addValidationMessage(failMessage)
                  .markSkipped(Config.onValidationErrorsSKipFailed);
        }
      }
      if (! mappedMarc.hasVendorCode()) {
        outcome.addValidationMessage("Record is missing required vendor code")
                .markSkipped(Config.onValidationErrorsSKipFailed);
      } else {
        String orgValidationResult = FolioData.validateOrganization(mappedMarc.vendorCode());
        if (orgValidationResult != null) {
          outcome.addValidationMessage(orgValidationResult)
                  .markSkipped(Config.onValidationErrorsSKipFailed);
        }
      }
      if (!mappedMarc.hasPrice()) {
        outcome.addValidationMessage("Record is missing required price info (980$m)")
                .markSkipped(Config.onValidationErrorsSKipFailed);
      }
      if (Config.objectCodeRequired) {
        if (!mappedMarc.hasObjectCode()) {
          outcome.setFlagIfNotNull(
                          "Object code is required according to the startup config" +
                                  " but no object code found in the record")
                  .markSkipped(Config.onValidationErrorsSKipFailed);
        }
      }

      // Validate optional properties
      if (mappedMarc.hasObjectCode()) {
        String failMessage = FolioData.validateObjectCode(mappedMarc.objectCode());
        if (failMessage != null) {
          outcome.addValidationMessage(failMessage)
                  .markSkipped(Config.onValidationErrorsSKipFailed);
        }
      }

      if (mappedMarc.hasProjectCode()) {
        String failMessage = FolioData.validateObjectCode(mappedMarc.projectCode());
        if (failMessage != null ) {
          outcome.addValidationMessage(failMessage)
                  .markSkipped(Config.onValidationErrorsSKipFailed);
        }
      }

      if (mappedMarc.hasBillTo()) {
        String failMessage = FolioData.validateAddress(mappedMarc.billTo());
        if (failMessage != null) {
          outcome.addValidationMessage(failMessage)
                  .markSkipped(Config.onValidationErrorsSKipFailed);
        }
      }

      if (mappedMarc.hasExpenseClassCode()) {
        if (FolioData.getExpenseClassId(mappedMarc.expenseClassCode())==null) {
          outcome.addValidationMessage(
                          "No expense class with the code ("
                                  + mappedMarc.expenseClassCode()
                                  + ") found in FOLIO.")
                  .markSkipped(Config.onValidationErrorsSKipFailed);
        } else {
          if (mappedMarc.hasBudgetId()) {
            String budgetExpClassId = FolioData.getBudgetExpenseClassId(
                    mappedMarc.budgetId(), mappedMarc.expenseClassId());
            if (budgetExpClassId == null) {
              outcome.addValidationMessage(
                      String.format("No budget expense class found for fund code (%s) and expense class (%s).",
                               mappedMarc.fundCode(), mappedMarc.expenseClassCode()))
                      .markSkipped(Config.onValidationErrorsSKipFailed);

            }
          }
        }
      }

      if (mappedMarc.hasAcquisitionMethod()) {
        try {
          if (FolioData.getAcquisitionMethodId(mappedMarc.acquisitionMethodValue()) == null) {
            outcome.addValidationMessage(
                    "No acquisition method with the value (" + mappedMarc.acquisitionMethodValue() + ") found in FOLIO.").markSkipped(
                    Config.onValidationErrorsSKipFailed);
          }
        } catch (Exception e) {
          // If the error is this, then we are probably using a version of the API
          // where this check cannot yet be performed.
          if (!e.getMessage().contains("No suitable module found for path")) {
            throw e;
          }
        }
      }

      if (Config.importInvoice) {
        outcome.addValidationMessage(
                        FolioData.validateRequiredValuesForInvoice(mappedMarc.title(), mappedMarc.getRecord()))
                .markSkipped(Config.onValidationErrorsSKipFailed);
      }

      // Check for valid ISBNs and other identifiers
      if (mappedMarc.hasISBN() && Utils.isInvalidIsbn(mappedMarc.getISBN())) {
        if (Config.V_ON_ISBN_INVALID_REMOVE_ISBN.equalsIgnoreCase(Config.onIsbnInvalid)) {
          outcome.setFlagIfNotNull(
                  String.format(
                          "ISBN %s is not valid. Will remove the ISBN to continue.",
                          mappedMarc.getISBN())
          );
        } else if (Config.V_ON_ISBN_INVALID_REPORT_ERROR.equalsIgnoreCase(Config.onIsbnInvalid)) {
          outcome.addValidationMessage("ISBN is invalid")
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
          existingInstancesMessage = "Found no existing Instances with that exact title.";
        }
        outcome.setFlagIfNotNull(
                "No ISBN, ISSN, Publisher number or distributor number, system control number," +
                        " or other standard identifier found in the record." +
                        " This order import might trigger the creation of a new Instance in FOLIO."
                + existingInstancesMessage);
      }

    }	catch(Exception e) {
      outcome.addValidationMessage("Got exception when validating MARC record: " + e.getMessage() + " " + e.getClass());
    }
  }

}
