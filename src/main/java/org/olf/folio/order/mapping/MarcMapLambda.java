package org.olf.folio.order.mapping;

import org.marc4j.marc.Record;
import org.olf.folio.order.imports.RecordResult;
import org.olf.folio.order.storage.FolioData;

public class MarcMapLambda extends BaseMapping {
  protected static final String OBJECT_CODE           = "o";
  protected static final String PROJECT_CODE          = "r";

  public MarcMapLambda(Record record) {
    super(record);
  }

  /**
   * @return 980$o
   */
  public String objectCode() {
    return d980.getSubfieldsAsString(OBJECT_CODE);
  }

  public boolean hasObjectCode() {
    return objectCode() != null && !objectCode().isEmpty();
  }

  /**
   * @return 980$r
   */
  public String projectCode() {
    return d980.getSubfieldsAsString(PROJECT_CODE);
  }

  public boolean hasProjectCode() {
    return projectCode() != null && !projectCode().isEmpty();
  }

  public boolean validate(RecordResult outcome) throws Exception {
    super.validate(outcome);
    if (has980()) {
      if (!hasObjectCode()) {
        outcome.setFlagIfNotNull(
                "Object code is required with MARC mapping 'Lambda'" + " but no object code found in the record");
      } else {
        outcome.addValidationMessageIfAny(FolioData.validateObjectCode(objectCode()));
      }
      if (hasProjectCode()) {
        outcome.addValidationMessageIfAny(FolioData.validateObjectCode(projectCode()));
      }
    }
    return !outcome.failedValidation();
  }
}
