package org.olf.folio.order.validation;

import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.marc.Record;
import org.olf.folio.order.Config;
import org.olf.folio.order.importhistory.FileStorageHelper;
import org.olf.folio.order.importhistory.RecordResult;
import org.olf.folio.order.importhistory.Results;
import org.olf.folio.order.mapping.MarcToFolio;

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
      validateMarcRecord(Config.getMarcMapping(record), validationResults.nextResult());
    }
    validationResults.setMarcRecordCount(recordCount).markDone();
    return validationResults;
  }

  public static void validateMarcRecord (MarcToFolio mappedMarc, RecordResult outcome) {
    try {
      outcome.setInputMarcData(mappedMarc);
      mappedMarc.validate(outcome);
      if (outcome.failedValidation()) {
        outcome.markSkipped(Config.onValidationErrorsSKipFailed);
      }
    }	catch(Exception e) {
      outcome.addValidationMessageIfAny("Got exception when validating MARC record: " + e.getMessage() + " " + e.getClass() );
    }
  }

}
